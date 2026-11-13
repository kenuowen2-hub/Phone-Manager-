package com.example.security

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

sealed interface BiometricStatus {
    data object Available : BiometricStatus
    data class Unavailable(val reason: String) : BiometricStatus
}

object BiometricHelper {

    fun checkStatus(context: Context): BiometricStatus {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return BiometricStatus.Unavailable(
                "System BiometricPrompt requires Android 9 (API 28) or higher. Use your secure PIN."
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = appContext.getSystemService(BiometricManager::class.java)
                ?: return BiometricStatus.Unavailable("Biometric service is unavailable on this device.")

            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            } else {
                @Suppress("DEPRECATION")
                manager.canAuthenticate()
            }

            return when (result) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                    BiometricStatus.Unavailable("No biometric sensor detected on this device.")
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                    BiometricStatus.Unavailable("Biometric sensor is currently unavailable.")
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                    BiometricStatus.Unavailable("No fingerprint or face biometric enrolled in Android Settings.")
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                    BiometricStatus.Unavailable("A system security update is required for biometric authentication.")
                else -> BiometricStatus.Unavailable("Biometric authentication is not ready on this device.")
            }
        }

        // Android 9 (API 28): check hardware feature + secure lock screen
        val hasFingerprint = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val keyguard = appContext.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isSecure = keyguard?.isDeviceSecure == true
        return if (hasFingerprint && isSecure) {
            BiometricStatus.Available
        } else {
            BiometricStatus.Unavailable("Fingerprint hardware or secure lock screen is not configured.")
        }
    }

    fun authenticate(
        context: Context,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onError("Biometric authentication requires Android 9 or higher. Please enter your PIN.")
            return
        }

        val status = checkStatus(context)
        if (status is BiometricStatus.Unavailable) {
            onError(status.reason)
            return
        }

        try {
            val executor = context.mainExecutor
            val cancellationSignal = CancellationSignal()
            val prompt = BiometricPrompt.Builder(context)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButton("Use PIN", executor) { _, _ ->
                    onError("Cancelled. Enter your PIN to continue.")
                }
                .build()

            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errString?.toString() ?: "Biometric authentication failed.")
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // System prompt remains open for retry; no immediate failure needed
                    }
                }
            )
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Unable to launch biometric prompt.")
        }
    }
}
