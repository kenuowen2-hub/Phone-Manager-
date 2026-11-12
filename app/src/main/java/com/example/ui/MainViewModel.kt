package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiManager
import com.example.audio.AudioRecorderHelper
import com.example.data.local.AppDatabase
import com.example.data.local.AppLockConfig
import com.example.data.local.BlockedRule
import com.example.data.local.LockSchedule
import com.example.data.local.SearchBlockedLog
import com.example.data.local.SecurityPreferences
import com.example.data.local.SecuritySettingsState
import com.example.data.local.TranscriptionItem
import com.example.device.AppItem
import com.example.device.AppSortOption
import com.example.device.DeviceDashboardState
import com.example.device.DeviceStatsManager
import com.example.device.InstalledAppsManager
import com.example.filter.FilterCheckResult
import com.example.filter.SearchFilterEngine
import com.example.storage.SharedFileItem
import com.example.storage.StorageManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiThinkingState {
    data object Idle : AiThinkingState
    data object Thinking : AiThinkingState
    data class Success(val response: String) : AiThinkingState
    data class Error(val message: String) : AiThinkingState
}

sealed interface AudioTranscribeState {
    data object Idle : AudioTranscribeState
    data object Recording : AudioTranscribeState
    data object Transcribing : AudioTranscribeState
    data class Success(val text: String, val durationSec: Long) : AudioTranscribeState
    data class Error(val message: String) : AudioTranscribeState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val securityPrefs = SecurityPreferences(application)
    val deviceStatsManager = DeviceStatsManager(application)
    val appsManager = InstalledAppsManager(application)
    val storageHelper = StorageManagerHelper(application)
    val audioRecorder = AudioRecorderHelper(application)
    val aiManager = AiManager()

    // State flows
    val dashboardState: StateFlow<DeviceDashboardState> = deviceStatsManager.dashboardState
    val securitySettings: StateFlow<SecuritySettingsState> = securityPrefs.settings

    val blockedRules: StateFlow<List<BlockedRule>> = db.blockedRuleDao().getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedLogs: StateFlow<List<SearchBlockedLog>> = db.searchBlockedLogDao().getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lockedAppsConfig: StateFlow<List<AppLockConfig>> = db.appLockConfigDao().getAllLockedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transcriptions: StateFlow<List<TranscriptionItem>> = db.transcriptionDao().getAllTranscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Installed apps state
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _appSearchQuery = MutableStateFlow("")
    val appSearchQuery: StateFlow<String> = _appSearchQuery.asStateFlow()

    private val _appSortOption = MutableStateFlow(AppSortOption.NAME_ASC)
    val appSortOption: StateFlow<AppSortOption> = _appSortOption.asStateFlow()

    // In-app search & filter state
    private val _searchFilterQuery = MutableStateFlow("")
    val searchFilterQuery: StateFlow<String> = _searchFilterQuery.asStateFlow()

    private val _currentActiveUrl = MutableStateFlow("https://html.duckduckgo.com")
    val currentActiveUrl: StateFlow<String> = _currentActiveUrl.asStateFlow()

    private val _lastFilterResult = MutableStateFlow<FilterCheckResult?>(null)
    val lastFilterResult: StateFlow<FilterCheckResult?> = _lastFilterResult.asStateFlow()

    // Shared files state
    private val _sharedFiles = MutableStateFlow<List<SharedFileItem>>(emptyList())
    val sharedFiles: StateFlow<List<SharedFileItem>> = _sharedFiles.asStateFlow()

    private val _appCacheSizeBytes = MutableStateFlow(0L)
    val appCacheSizeBytes: StateFlow<Long> = _appCacheSizeBytes.asStateFlow()

    // AI States
    private val _aiThinkingState = MutableStateFlow<AiThinkingState>(AiThinkingState.Idle)
    val aiThinkingState: StateFlow<AiThinkingState> = _aiThinkingState.asStateFlow()

    private val _audioTranscribeState = MutableStateFlow<AudioTranscribeState>(AudioTranscribeState.Idle)
    val audioTranscribeState: StateFlow<AudioTranscribeState> = _audioTranscribeState.asStateFlow()

    // App lock simulation/test gate
    private val _testLockApp = MutableStateFlow<AppItem?>(null)
    val testLockApp: StateFlow<AppItem?> = _testLockApp.asStateFlow()

    private val _isLockGateOpen = MutableStateFlow(false)
    val isLockGateOpen: StateFlow<Boolean> = _isLockGateOpen.asStateFlow()

    init {
        refreshDashboard()
        refreshInstalledApps()
        refreshAppCache()
        seedDefaultBlockedRulesIfEmpty()
    }

    private fun seedDefaultBlockedRulesIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.blockedRuleDao().getAllRulesDirect()
            if (existing.isEmpty()) {
                db.blockedRuleDao().insertRule(
                    BlockedRule(
                        pattern = "phishing",
                        isWholeWord = true,
                        description = "Protects against phishing search attempts"
                    )
                )
                db.blockedRuleDao().insertRule(
                    BlockedRule(
                        pattern = "malware download",
                        isWholeWord = false,
                        description = "Blocks malware download queries"
                    )
                )
                db.blockedRuleDao().insertRule(
                    BlockedRule(
                        pattern = "free gift card scam",
                        isWholeWord = false,
                        description = "Known deceptive search phrase"
                    )
                )
            }
        }
    }

    fun refreshDashboard() {
        deviceStatsManager.refreshAll()
    }

    private var appsJob: kotlinx.coroutines.Job? = null

    fun refreshInstalledApps() {
        appsJob?.cancel()
        appsJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoadingApps.value = true
            db.appLockConfigDao().getAllLockedApps().collect { lockedList ->
                val lockedSet = lockedList.filter { it.isLocked }.map { it.packageName }.toSet()
                _installedApps.value = appsManager.getInstalledApps(lockedSet)
                _isLoadingApps.value = false
            }
        }
    }

    fun setAppSearchQuery(query: String) {
        _appSearchQuery.value = query
    }

    fun setAppSortOption(option: AppSortOption) {
        _appSortOption.value = option
    }

    fun toggleAppLock(app: AppItem) {
        if (app.isCriticalProtected) return
        viewModelScope.launch {
            val willBeLocked = !app.isLocked
            if (willBeLocked) {
                db.appLockConfigDao().setLock(
                    AppLockConfig(packageName = app.packageName, appName = app.appName, isLocked = true)
                )
            } else {
                db.appLockConfigDao().removeLock(app.packageName)
            }
            // Update local list
            _installedApps.value = _installedApps.value.map {
                if (it.packageName == app.packageName) it.copy(isLocked = willBeLocked) else it
            }
        }
    }

    // App Lock PIN & Settings
    fun setPin(pin: String): Boolean {
        return securityPrefs.setPin(pin)
    }

    fun verifyPin(pin: String): Boolean {
        return securityPrefs.verifyPin(pin)
    }

    fun recordBiometricSuccess() {
        securityPrefs.recordBiometricSuccess()
    }

    fun setAppLockMasterEnabled(enabled: Boolean) {
        securityPrefs.setAppLockEnabled(enabled)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityPrefs.setBiometricEnabled(enabled)
    }

    fun setLockSchedule(schedule: LockSchedule, startHour: Int = 22, endHour: Int = 7) {
        securityPrefs.setLockSchedule(schedule, startHour, endHour)
    }

    fun setThemeMode(mode: String) {
        securityPrefs.setThemeMode(mode)
    }

    // Lock gate simulation
    fun openLockGateForApp(app: AppItem) {
        _testLockApp.value = app
        if (app.isLocked && securityPrefs.shouldCurrentlyLock()) {
            _isLockGateOpen.value = true
        } else {
            // Already unlocked or not locked -> launch directly
            appsManager.launchApp(app.packageName)
        }
    }

    fun dismissLockGate() {
        _isLockGateOpen.value = false
        _testLockApp.value = null
    }

    fun onLockGateUnlocked() {
        val app = _testLockApp.value
        _isLockGateOpen.value = false
        if (app != null) {
            appsManager.launchApp(app.packageName)
        }
        _testLockApp.value = null
    }

    // Search filter rules
    fun addBlockedRule(pattern: String, isWholeWord: Boolean, description: String) {
        viewModelScope.launch {
            db.blockedRuleDao().insertRule(
                BlockedRule(
                    pattern = pattern.trim(),
                    isWholeWord = isWholeWord,
                    description = description.trim()
                )
            )
        }
    }

    fun updateBlockedRule(rule: BlockedRule) {
        viewModelScope.launch {
            db.blockedRuleDao().updateRule(rule)
        }
    }

    fun deleteBlockedRule(rule: BlockedRule) {
        viewModelScope.launch {
            db.blockedRuleDao().deleteRule(rule)
        }
    }

    fun clearBlockedLogs() {
        viewModelScope.launch {
            db.searchBlockedLogDao().clearAll()
        }
    }

    // In-app search check
    fun executeSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _searchFilterQuery.value = trimmed

        viewModelScope.launch {
            val rules = db.blockedRuleDao().getAllRulesDirect()
            val result = SearchFilterEngine.checkQuery(trimmed, rules)
            _lastFilterResult.value = result

            if (result.isBlocked) {
                // Log violation
                result.matchedRule?.let { rule ->
                    db.blockedRuleDao().incrementMatchCount(rule.id)
                    db.searchBlockedLogDao().insertLog(
                        SearchBlockedLog(
                            query = trimmed,
                            matchedRule = rule.pattern
                        )
                    )
                }
            } else {
                // Safe query -> navigate in webview
                val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
                _currentActiveUrl.value = "https://html.duckduckgo.com/html/?q=$encoded"
            }
        }
    }

    fun dismissFilterResult() {
        _lastFilterResult.value = null
    }

    // Shared files
    fun addSharedFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val resolved = uris.mapNotNull { storageHelper.resolveFileInfo(it) }
            _sharedFiles.value = (_sharedFiles.value + resolved).distinctBy { it.uri }
        }
    }

    fun deleteSharedFile(item: SharedFileItem, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = storageHelper.deleteSharedFile(item.uri)
            if (success) {
                _sharedFiles.value = _sharedFiles.value.filter { it.uri != item.uri }
            }
            onResult(success)
        }
    }

    fun removeFileFromList(item: SharedFileItem) {
        _sharedFiles.value = _sharedFiles.value.filter { it.uri != item.uri }
    }

    fun refreshAppCache() {
        _appCacheSizeBytes.value = storageHelper.getAppCacheSize()
    }

    fun clearAppCache() {
        viewModelScope.launch {
            storageHelper.clearAppCache()
            refreshAppCache()
        }
    }

    // AI Thinking Mode (gemini-4-carbon with ThinkingLevel.HIGH)
    fun askThinkingAdvisor(prompt: String) {
        if (prompt.isBlank()) return
        _aiThinkingState.value = AiThinkingState.Thinking

        viewModelScope.launch {
            try {
                // Provide context from dashboard to assist in real device advice
                val dash = dashboardState.value
                val enrichedPrompt = """
                    You are the Smart Phone Manager AI Security & Diagnostics Advisor.
                    The user asks: "$prompt"
                    
                    Current Device Context:
                    - OS: ${dash.osVersion}
                    - Model: ${dash.deviceModel}
                    - Battery: ${dash.battery.levelPercent}% (${dash.battery.chargingSource}, Health: ${dash.battery.health}, Temp: ${dash.battery.temperatureCelsius}°C)
                    - Storage: ${dash.storage.usedGbFormatted} used of ${dash.storage.totalGbFormatted} (${dash.storage.freeGbFormatted} available)
                    - RAM: ${dash.memory.usedPercent}% used
                    - Usage Stats Access: ${if (dash.hasUsageStatsPermission) "Granted" else "Not granted"}
                    
                    Provide an in-depth, scientifically accurate, and helpful analysis. Never suggest fake cleaning, fake RAM boost, or snake-oil utilities. Explain how Android actually handles permissions, memory, and security.
                """.trimIndent()

                val result = aiManager.runThinkingAnalysis(enrichedPrompt)
                _aiThinkingState.value = AiThinkingState.Success(result)
            } catch (e: Exception) {
                _aiThinkingState.value = AiThinkingState.Error(e.localizedMessage ?: "Thinking analysis failed.")
            }
        }
    }

    fun resetAiThinkingState() {
        _aiThinkingState.value = AiThinkingState.Idle
    }

    // Audio input transcription (gemini-3.5-transcribe)
    fun startAudioRecording(): Boolean {
        val started = audioRecorder.startRecording()
        if (started) {
            _audioTranscribeState.value = AudioTranscribeState.Recording
        }
        return started
    }

    fun stopAudioRecordingAndTranscribe() {
        if (!audioRecorder.isRecording) return
        _audioTranscribeState.value = AudioTranscribeState.Transcribing

        viewModelScope.launch {
            try {
                val recordData = audioRecorder.stopRecording()
                if (recordData != null) {
                    val (audioBytes, duration) = recordData
                    val text = aiManager.transcribeAudio(audioBytes, "audio/mp4")
                    _audioTranscribeState.value = AudioTranscribeState.Success(text, duration)

                    // Save to history
                    db.transcriptionDao().insertTranscription(
                        TranscriptionItem(
                            transcribedText = text,
                            durationSeconds = duration
                        )
                    )
                } else {
                    _audioTranscribeState.value = AudioTranscribeState.Error("No audio recorded or empty audio buffer.")
                }
            } catch (e: Exception) {
                _audioTranscribeState.value = AudioTranscribeState.Error(e.localizedMessage ?: "Audio transcription failed.")
            }
        }
    }

    fun cancelAudioRecording() {
        audioRecorder.cancelRecording()
        _audioTranscribeState.value = AudioTranscribeState.Idle
    }

    fun clearTranscriptionHistory() {
        viewModelScope.launch {
            db.transcriptionDao().clearAll()
        }
    }

    fun deleteTranscriptionItem(item: TranscriptionItem) {
        viewModelScope.launch {
            db.transcriptionDao().deleteTranscription(item)
        }
    }
}
