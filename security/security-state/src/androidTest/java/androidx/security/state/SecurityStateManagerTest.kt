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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SecurityStateManagerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var securityStateManager: SecurityStateManager

    @Before
    fun setup() {
        securityStateManager = SecurityStateManager(context)
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
                        value.matches(versionRegexWebView.toRegex())
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testGetGlobalSecurityState_sdkAbove29() {
        val bundle = securityStateManager.getGlobalSecurityState()
        assertTrue(matchesDateFormat(bundle.getString("system_spl")!!))
        assertTrue(matchesDateFormat(bundle.getString("vendor_spl")!!))
        assertTrue(matchesKernelFormat(bundle.getString("kernel_version")!!))
        assertTrue(containsModuleMetadataPackage(bundle))
        assertTrue(containsWebViewPackage(bundle))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O, maxSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun testGetGlobalSecurityState_sdkAbove25Below29_doesNotContainModuleMetadata() {
        val bundle = securityStateManager.getGlobalSecurityState()
        assertTrue(matchesDateFormat(bundle.getString("system_spl")!!))
        assertTrue(matchesDateFormat(bundle.getString("vendor_spl")!!))
        assertTrue(matchesKernelFormat(bundle.getString("kernel_version")!!))
        assertTrue(containsWebViewPackage(bundle))
        assertFalse(containsModuleMetadataPackage(bundle))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M, maxSdkVersion = Build.VERSION_CODES.N_MR1)
    @Test
    fun testGetGlobalSecurityState_sdkAbove22Below26_doesNotContainModuleMetadataOrWebView() {
        val bundle = securityStateManager.getGlobalSecurityState()
        assertTrue(matchesDateFormat(bundle.getString("system_spl")!!))
        assertTrue(matchesDateFormat(bundle.getString("vendor_spl")!!))
        assertTrue(matchesKernelFormat(bundle.getString("kernel_version")!!))
        assertFalse(containsModuleMetadataPackage(bundle))
        assertFalse(containsWebViewPackage(bundle))
    }

    @SdkSuppress(
        minSdkVersion = Build.VERSION_CODES.LOLLIPOP,
        maxSdkVersion = Build.VERSION_CODES.LOLLIPOP_MR1
    )
    @Test
    fun testGetGlobalSecurityState_sdkBelow23_containsOnlyKernel() {
        val bundle = securityStateManager.getGlobalSecurityState()
        assertTrue(matchesKernelFormat(bundle.getString("kernel_version")!!))
        assertFalse(bundle.containsKey("system_spl"))
        assertFalse(bundle.containsKey("vendor_spl"))
        assertFalse(containsModuleMetadataPackage(bundle))
        assertFalse(containsWebViewPackage(bundle))
    }
}
