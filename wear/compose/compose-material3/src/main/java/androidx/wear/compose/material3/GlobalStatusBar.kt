/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalContext

/** [CompositionLocal] to determine if the global status bar is enabled. */
public val LocalGlobalStatusBarEnabled: ProvidableCompositionLocal<Boolean> =
    compositionLocalWithComputedDefaultOf {
        val context = LocalContext.currentValue.applicationContext
        val checker = GlobalStatusBarChecker()
        checker.isEnabled(context)
    }

/** Global constants registry containing system keys and tag names for GSB enablement. */
internal object GlobalStatusBarDefaults {
    /**
     * System property key checked on the host platform to determine if the GSB integration rollout
     * is active.
     */
    const val SYSTEM_PROPERTY_KEY = "persist.global_status_bar_opt_in"

    /** Manifest metadata key used to configure the application-level GSB opt-in/opt-out status. */
    const val METADATA_KEY = "com.google.wear.ENABLE_GLOBAL_STATUS_BAR"
}

/** Interface responsible for reading system properties from the Android environment. */
internal interface SystemPropertyReader {
    /**
     * Reads a boolean system property.
     *
     * @param name The name of the system property.
     * @param defaultValue The fallback value if resolution fails or the key is absent.
     * @return The boolean value of the system property.
     */
    fun getBoolean(name: String, defaultValue: Boolean): Boolean
}

/** Interface responsible for querying manifest metadata declarations. */
internal interface ManifestMetadataChecker {
    /**
     * Checks if a specific boolean key is declared and true in the app's merged manifest metadata.
     *
     * @param context The application context.
     * @param key The metadata tag key string.
     * @return `true` if the key is present and set to true.
     */
    fun hasMetadata(context: Context, key: String): Boolean
}

/**
 * Production implementation of [SystemPropertyReader] utilizing reflection to read system
 * properties.
 */
internal class DefaultSystemPropertyReader : SystemPropertyReader {
    @Suppress("BanUncheckedReflection")
    override fun getBoolean(name: String, defaultValue: Boolean): Boolean {
        return try {
            val systemPropertiesClazz = Class.forName("android.os.SystemProperties")
            val getBooleanMethod =
                systemPropertiesClazz.getMethod(
                    "getBoolean",
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                )
            getBooleanMethod.invoke(null, name, defaultValue) as Boolean
        } catch (e: Exception) {
            defaultValue
        }
    }
}

/** Production implementation of [ManifestMetadataChecker] querying app merged manifest entries. */
internal class DefaultManifestMetadataChecker : ManifestMetadataChecker {
    override fun hasMetadata(context: Context, key: String): Boolean {
        return try {
            // Handle API 33+ GET_META_DATA deprecation
            val appInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getApplicationInfo(
                        context.packageName,
                        PackageManager.ApplicationInfoFlags.of(
                            PackageManager.GET_META_DATA.toLong()
                        ),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getApplicationInfo(
                        context.packageName,
                        PackageManager.GET_META_DATA,
                    )
                }
            appInfo.metaData?.getBoolean(key, false) ?: false
        } catch (e: Exception) {
            false
        }
    }
}

/** Helper responsible for checking the enablement status of the global status bar. */
internal class GlobalStatusBarChecker(
    private val sysPropReader: SystemPropertyReader = DefaultSystemPropertyReader(),
    private val metadataChecker: ManifestMetadataChecker = DefaultManifestMetadataChecker(),
) {
    fun isEnabled(context: Context): Boolean {
        // 1. Check if the system property is enabled via reflection (to bypass compile stub
        // restrictions)
        val isSystemPropEnabled =
            sysPropReader.getBoolean(GlobalStatusBarDefaults.SYSTEM_PROPERTY_KEY, false)

        // 2. If the system property is active, check if this app opted in (automatically or
        // manually)
        if (isSystemPropEnabled) {
            return metadataChecker.hasMetadata(context, GlobalStatusBarDefaults.METADATA_KEY)
        }
        return false
    }
}
