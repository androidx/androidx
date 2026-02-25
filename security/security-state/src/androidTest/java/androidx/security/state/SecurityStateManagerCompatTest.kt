/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.security.state

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.security.state.SecurityPatchState.DateBasedSecurityPatchLevel
import androidx.security.state.SecurityStateManagerCompat.Companion.KEY_SYSTEM_SUPPLEMENTAL_PATCHES
import androidx.security.state.SecurityStateManagerCompat.Companion.KEY_VENDOR_SUPPLEMENTAL_PATCHES
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SecurityStateManagerCompatTest {

    private companion object {
        private const val TAG = "SecurityStateManagerCompatTest"
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var securityStateManagerCompat: SecurityStateManagerCompat

    private val SYSTEM_SUPPLEMENTAL_PATCH_CONFIG_FILE =
        "/system/etc/security/supplemental_security_patches.xml"
    private val VENDOR_SUPPLEMENTAL_PATCH_CONFIG_FILE =
        "/vendor/etc/security/supplemental_security_patches.xml"

    @Before
    fun setup() {
        securityStateManagerCompat = SecurityStateManagerCompat(context)
    }

    /** Returns `true` if [date] is in the format "YYYY-MM-DD". */
    private fun matchesDateFormat(date: String): Boolean {
        val dateRegex = "^\\d{4}-\\d{2}-\\d{2}$"
        return date.matches(dateRegex.toRegex())
    }

    /** Returns `true` if [kernel] is in the format "X.X.XX". */
    private fun matchesKernelFormat(kernel: String): Boolean {
        val versionRegex = "^\\d+\\.\\d+\\.\\d+$"
        return kernel.matches(versionRegex.toRegex())
    }

    /** Returns `true` if a key for a WebView package exists in [bundle]. */
    private fun containsWebViewPackage(bundle: Bundle): Boolean {
        var foundWebView = false
        // https://chromium.googlesource.com/chromium/src/+/HEAD/android_webview/docs/webview-providers.md#package-name
        val nameRegex =
            "^(?:com\\.google\\.android\\.apps\\.chrome|com\\.google\\.android\\.webview|com\\.android\\.(?:chrome|webview)|com\\.chrome)(?:\\.(?:beta|dev|canary|debug))?\$"
        val versionRegexWebView = "^\\d+\\.\\d+\\.\\d+\\.\\d+$"
        for (key in bundle.keySet()) {
            if (key.matches(nameRegex.toRegex())) {
                foundWebView = true
                val value = bundle.getString(key)
                if (value!!.isNotEmpty()) {
                    assertTrue(
                        "WebView version format incorrect for $key: $value",
                        value.matches(versionRegexWebView.toRegex()),
                    )
                    break
                }
            }
        }
        return foundWebView
    }

    /** Returns `true` if a key for the module metadata package name exists in [bundle]. */
    private fun containsModuleMetadataPackage(bundle: Bundle): Boolean {
        return bundle.keySet().any { it.contains("modulemetadata") }
    }

    @Throws(Exception::class)
    private fun matchesCveIdsFormat(bundle: Bundle, key: String, configFilePath: String): Boolean {
        val expectedCveIds = getExpectedCveIds(configFilePath)

        val actualCveIds = bundle.getStringArray(key) ?: emptyArray()
        expectedCveIds.sort()
        actualCveIds.sort()

        return expectedCveIds.contentEquals(actualCveIds)
    }

    private fun getExpectedCveIds(configFilePath: String): Array<String> {
        val configFile = File(configFilePath)
        // skip the test if supplemental_security_patches.xml is not present.
        assumeTrue(configFile.exists())

        return try {
            FileInputStream(configFile).use { inputStream ->
                // Call the static read method. Handle potential null return.
                val securityPatches: SecurityPatchesXmlParser? =
                    SecurityPatchesXmlParser.read(inputStream)

                if (securityPatches != null) {
                    // Use the getPatch() method, not property access
                    val patches = securityPatches.getPatch()
                    patches.mapNotNull { patch -> patch.id }.toTypedArray()
                } else {
                    Log.e(TAG, "Failed to parse XML, read returned null for: $configFilePath")
                    emptyArray()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read supplemental security patch file: $configFilePath", e)
            emptyArray()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse supplemental security patch file: $configFilePath", e)
            emptyArray()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testGetGlobalSecurityState_sdkAbove29() {
        val bundle = securityStateManagerCompat.getGlobalSecurityState()
        assertTrue(matchesDateFormat(bundle.getString("system_spl")!!))
        assertTrue(matchesKernelFormat(bundle.getString("kernel_version")!!))
        assertTrue(containsModuleMetadataPackage(bundle))
        assertTrue(containsWebViewPackage(bundle))
        assertTrue(
            matchesCveIdsFormat(
                bundle,
                KEY_SYSTEM_SUPPLEMENTAL_PATCHES,
                SYSTEM_SUPPLEMENTAL_PATCH_CONFIG_FILE,
            )
        )
        assertTrue(
            matchesCveIdsFormat(
                bundle,
                KEY_VENDOR_SUPPLEMENTAL_PATCHES,
                VENDOR_SUPPLEMENTAL_PATCH_CONFIG_FILE,
            )
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O, maxSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun testGetGlobalSecurityState_sdkAbove25Below29_doesNotContainModuleMetadata() {
        val bundle = securityStateManagerCompat.getGlobalSecurityState()
        assertTrue(matchesDateFormat(bundle.getString("system_spl")!!))
        assertTrue(matchesKernelFormat(bundle.getString("kernel_version")!!))
        assertTrue(containsWebViewPackage(bundle))
        assertFalse(containsModuleMetadataPackage(bundle))
        assertTrue(
            matchesCveIdsFormat(
                bundle,
                KEY_SYSTEM_SUPPLEMENTAL_PATCHES,
                SYSTEM_SUPPLEMENTAL_PATCH_CONFIG_FILE,
            )
        )
        assertTrue(
            matchesCveIdsFormat(
                bundle,
                KEY_VENDOR_SUPPLEMENTAL_PATCHES,
                VENDOR_SUPPLEMENTAL_PATCH_CONFIG_FILE,
            )
        )
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    @Test
    fun testGetGlobalSecurityState_sdkAbove22Below26_doesNotContainModuleMetadataOrWebView() {
        val bundle = securityStateManagerCompat.getGlobalSecurityState()
        assertTrue(matchesDateFormat(bundle.getString("system_spl")!!))
        assertTrue(matchesKernelFormat(bundle.getString("kernel_version")!!))
        assertFalse(containsModuleMetadataPackage(bundle))
        assertFalse(containsWebViewPackage(bundle))
        assertTrue(
            matchesCveIdsFormat(
                bundle,
                KEY_SYSTEM_SUPPLEMENTAL_PATCHES,
                SYSTEM_SUPPLEMENTAL_PATCH_CONFIG_FILE,
            )
        )
        assertTrue(
            matchesCveIdsFormat(
                bundle,
                KEY_VENDOR_SUPPLEMENTAL_PATCHES,
                VENDOR_SUPPLEMENTAL_PATCH_CONFIG_FILE,
            )
        )
    }

    @Test
    fun testGetGlobalSecurityState_whenVendorIsEnabled_containsVendorSpl() {
        SecurityPatchState.Companion.USE_VENDOR_SPL = true
        val bundle = securityStateManagerCompat.getGlobalSecurityState()
        assertTrue(bundle.containsKey("vendor_spl"))
    }

    @Test
    fun testGetGlobalSecurityState_whenVendorIsDisabled_doesNotContainVendorSpl() {
        SecurityPatchState.Companion.USE_VENDOR_SPL = false
        val bundle = securityStateManagerCompat.getGlobalSecurityState()
        assertFalse(bundle.containsKey("vendor_spl"))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testGetGlobalSecurityState_withGoogleModules_doesNotThrow() {
        if (!Build.BRAND.equals("Google", ignoreCase = true)) {
            return // Skip this test on non-Google devices.
        }
        val bundle =
            securityStateManagerCompat.getGlobalSecurityState("com.google.android.modulemetadata")
        DateBasedSecurityPatchLevel.fromString(
            bundle.getString("com.google.android.modulemetadata")!!
        )
    }
}
