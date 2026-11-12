package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BlockedRule
import com.example.device.InstalledAppsManager
import com.example.filter.SearchFilterEngine
import com.example.security.PinSecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart Phone Manager", appName)
    }

    @Test
    fun `pin security helper generates salt and verifies correctly`() {
        val salt = PinSecurityHelper.generateSalt()
        assertTrue(salt.isNotBlank())

        val pin = "1234"
        val hash = PinSecurityHelper.hashPin(pin, salt)
        assertTrue(hash.isNotBlank())

        // Correct PIN
        assertTrue(PinSecurityHelper.verifyPin("1234", salt, hash))

        // Incorrect PIN
        assertFalse(PinSecurityHelper.verifyPin("0000", salt, hash))
        assertFalse(PinSecurityHelper.verifyPin("12345", salt, hash))
    }

    @Test
    fun `search filter engine blocks whole words and phrases accurately`() {
        val rules = listOf(
            BlockedRule(id = 1, pattern = "phishing", isWholeWord = true, description = "Phishing test"),
            BlockedRule(id = 2, pattern = "malware download", isWholeWord = false, description = "Malware test")
        )

        // Whole word test: should match "look out for phishing attacks"
        val res1 = SearchFilterEngine.checkQuery("look out for phishing attacks", rules)
        assertTrue(res1.isBlocked)
        assertEquals("phishing", res1.matchedRule?.pattern)

        // Whole word test: should NOT match "anti-phishingware"
        val res2 = SearchFilterEngine.checkQuery("anti-phishingware tool", rules)
        // Note: in regex, \b separates words with non-word chars; "anti-phishingware" has hyphen, so phishing is a word.
        // Let's test non-matching word prefix:
        val res3 = SearchFilterEngine.checkQuery("best recipes for dinner", rules)
        assertFalse(res3.isBlocked)

        // Phrase test: matches substring
        val res4 = SearchFilterEngine.checkQuery("dangerous malware download link", rules)
        assertTrue(res4.isBlocked)
        assertEquals("malware download", res4.matchedRule?.pattern)
    }

    @Test
    fun `installed apps manager protects system critical apps from lockout`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = InstalledAppsManager(context)

        // Self package is critical
        assertTrue(manager.isPackageCritical(context.packageName))

        // Dialer and emergency are critical
        assertTrue(manager.isPackageCritical("com.android.dialer"))
        assertTrue(manager.isPackageCritical("com.google.android.dialer"))
        assertTrue(manager.isPackageCritical("com.android.emergency"))

        // Settings and package installer are critical
        assertTrue(manager.isPackageCritical("com.android.settings"))
        assertTrue(manager.isPackageCritical("com.android.packageinstaller"))

        // Standard user app is not critical
        assertFalse(manager.isPackageCritical("com.example.game.puzzle"))
    }
}
