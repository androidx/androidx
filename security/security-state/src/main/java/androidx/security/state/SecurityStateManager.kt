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

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.system.Os
import androidx.annotation.RequiresApi
import androidx.webkit.WebViewCompat
import java.util.regex.Pattern

/**
 * This class is a wrapper around AOSP [android.os.SecurityStateManager] service API added in
 * SDK 35. Support for features on older SDKs is provided on a best effort basis.
 *
 * Manages the retrieval and storage of security patch levels and module information for an Android
 * device. This class provides methods to fetch the current security state of the system, including
 * patch levels for the system, vendor, and kernel as well as module updates available through
 * Android's update system.
 *
 * It utilizes Android's PackageManager and other system services to retrieve detailed
 * security-related information, which is crucial for maintaining the security integrity of the
 * device.
 */
public open class SecurityStateManager(private val context: Context) {

    public companion object {
        private const val TAG = "SecurityStateManager"
        private val kernelReleasePattern: Pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+)(.*)")

        private const val VENDOR_SECURITY_PATCH_PROPERTY_KEY: String =
            "ro.vendor.build.security_patch"
        private const val ANDROID_MODULE_METADATA_PROVIDER: String = "com.android.modulemetadata"

        /**
         * The system SPL key returned as part of the {@code Bundle} from {@code
         * getGlobalSecurityState}.
         */
        public const val KEY_SYSTEM_SPL: String = "system_spl"

        /**
         * The vendor SPL key returned as part of the {@code Bundle} from {@code
         * getGlobalSecurityState}.
         */
        public const val KEY_VENDOR_SPL: String = "vendor_spl"

        /**
         * The kernel version key returned as part of the {@code Bundle} from {@code
         * getGlobalSecurityState}.
         */
        public const val KEY_KERNEL_VERSION: String = "kernel_version"
    }

    private val packageManager: PackageManager = context.packageManager

    /**
     * Retrieves the global security state of the device, compiling various security patch levels
     * and module information into a Bundle. This method can optionally use Google's module metadata
     * providers to enhance the data returned.
     *
     * @param moduleMetadataProvider Specifies package name for system modules metadata.
     * @return A Bundle containing keys and values representing the security state of the system,
     *   vendor, and kernel.
     */
    @SuppressLint("NewApi") // Lint does not detect version check below.
    public open fun getGlobalSecurityState(moduleMetadataProvider: String? = null): Bundle {
        if (getAndroidSdkInt() >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return getGlobalSecurityStateFromService()
        }
        return Bundle().apply {
            if (getAndroidSdkInt() >= Build.VERSION_CODES.M) {
                putString(KEY_SYSTEM_SPL, Build.VERSION.SECURITY_PATCH)

                val vendorSpl = getVendorSpl()
                if (vendorSpl.isNotEmpty()) {
                    putString(KEY_VENDOR_SPL, vendorSpl)
                } else {
                    // Assume vendor SPL == system SPL
                    putString(KEY_VENDOR_SPL, Build.VERSION.SECURITY_PATCH)
                }
            }
            if (getAndroidSdkInt() >= Build.VERSION_CODES.Q) {
                val moduleMetadataProviderPackageName =
                    moduleMetadataProvider ?: ANDROID_MODULE_METADATA_PROVIDER

                if (moduleMetadataProviderPackageName.isNotEmpty()) {
                    putString(
                        moduleMetadataProviderPackageName,
                        getPackageVersion(moduleMetadataProviderPackageName)
                    )
                }
            }
            val kernelVersion = getKernelVersion()
            if (kernelVersion.isNotEmpty()) {
                putString(KEY_KERNEL_VERSION, kernelVersion)
            }
            addWebViewPackages(this)
        }
    }

    /**
     * Returns the current global security state from the system service on SDK 35+.
     *
     * @return A [Bundle] that contains the global security state information as string-to-string
     *   key-value pairs.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint("WrongConstant")
    private fun getGlobalSecurityStateFromService(): Bundle {
        val securityStateManagerService =
            context.getSystemService(Context.SECURITY_STATE_SERVICE)
                as android.os.SecurityStateManager
        val globalSecurityState = securityStateManagerService.globalSecurityState
        val vendorSpl = globalSecurityState.getString(KEY_VENDOR_SPL, "")
        if (vendorSpl.isEmpty()) {
            // Assume vendor SPL == system SPL
            globalSecurityState.putString(
                KEY_VENDOR_SPL,
                globalSecurityState.getString(KEY_SYSTEM_SPL)
            )
        }
        return globalSecurityState
    }

    /**
     * Fetches the security patch level (SPL) for a specific package by its package name. This is
     * typically used to get version information for modules that may have their own update cycles
     * independent of the system OS.
     *
     * @param packageName The package name for which to retrieve the SPL.
     * @return A string representing the version, or an empty string if not available or an error
     *   occurs.
     * @throws PackageManager.NameNotFoundException if the package name provided does not exist.
     */
    internal open fun getPackageVersion(packageName: String): String {
        if (packageName.isNotEmpty()) {
            return try {
                packageManager.getPackageInfo(packageName, 0).versionName ?: ""
            } catch (e: PackageManager.NameNotFoundException) {
                ""
            }
        }
        return ""
    }

    /**
     * Adds information about the webview packages to a bundle. This is used to track updates and
     * versions for the webview, which can have security implications.
     *
     * @param bundle The bundle to which the webview package information will be added.
     */
    @RequiresApi(26)
    private fun addWebViewPackages(bundle: Bundle) {
        if (getAndroidSdkInt() < Build.VERSION_CODES.O) {
            return
        }
        val packageName = getCurrentWebViewPackageName()
        if (packageName.isNotEmpty()) {
            bundle.putString(packageName, getPackageVersion(packageName))
        }
    }

    /**
     * Retrieves the current webview package name used by the system. This method checks the
     * system's default webview provider and extracts the package name, which is essential for
     * managing updates and security patches for components relying on webview.
     *
     * @return A string representing the current webview package name, or an empty string if it
     *   cannot be determined.
     */
    private fun getCurrentWebViewPackageName(): String {
        val webViewPackageInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WebViewCompat.getCurrentWebViewPackage(context)
            } else {
                null
            }
        return webViewPackageInfo?.packageName ?: ""
    }

    /**
     * Retrieves the SDK version of the current Android system.
     *
     * @return the SDK version as an integer.
     */
    internal open fun getAndroidSdkInt(): Int {
        return Build.VERSION.SDK_INT
    }

    /**
     * Safely retrieves the current security patch level of the device's operating system. This
     * method ensures compatibility by checking the Android version before attempting to access APIs
     * that are not available on older versions.
     *
     * @return A string representing the current security patch level, or empty string if it cannot
     *   be retrieved.
     */
    @SuppressLint("NewApi") // Lint does not detect version check below.
    internal fun getSecurityPatchLevelSafe(): String {
        return if (getAndroidSdkInt() >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else {
            ""
        }
    }

    /**
     * Attempts to retrieve the kernel version of the device using the system's uname() command.
     * This method is used to determine the current kernel version which is a part of the security
     * state assessment.
     *
     * @return A string containing the kernel version, or an empty string if an error occurs or the
     *   data cannot be reliably parsed.
     */
    private fun getKernelVersion(): String {
        try {
            val matcher = kernelReleasePattern.matcher(Os.uname().release)
            return if (matcher.matches()) matcher.group(1)!! else ""
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * Uses reflection to access Android's SystemProperties to retrieve the vendor security patch
     * level. This approach is necessary on Android versions where the vendor patch level is not
     * directly accessible via the public API.
     *
     * @return A string representing the vendor's security patch level, or an empty string if it
     *   cannot be retrieved.
     */
    @Suppress("BanUncheckedReflection") // For accessing vendor SPL on SDK older than 35.
    private fun getVendorSpl(): String {
        try {
            // This is the only way to get vendor SPL from public API level on Android 14 or older
            // devices.
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod =
                systemProperties.getMethod("get", String::class.java, String::class.java)
            return getMethod.invoke(systemProperties, VENDOR_SECURITY_PATCH_PROPERTY_KEY, "")
                as String
        } catch (e: Exception) {
            return ""
        }
    }
}
