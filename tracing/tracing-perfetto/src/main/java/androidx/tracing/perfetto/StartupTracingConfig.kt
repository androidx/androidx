/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tracing.perfetto

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import java.io.File
import java.util.Properties

/**
 * Config for enabling tracing at app startup
 *
 * @param libFilePath Path to the optionally sideloaded `libtracing_perfetto.so` file
 * @param isPersistent Determines whether tracing should remain enabled (sticky) between app runs
 */
internal data class StartupTracingConfig(val libFilePath: String?, val isPersistent: Boolean)

/**
 * Hack used by [StartupTracingConfigStore] to perform a fast check whether there is
 * a [StartupTracingConfig] present. Relies on [PackageManager.getComponentEnabledSetting] and a
 * dummy [BroadcastReceiver] component.
 */
private abstract class StartupTracingConfigStoreIsEnabledGate : BroadcastReceiver() {
    companion object {
        fun enable(context: Context) = setEnabledSetting(context, true)

        fun disable(context: Context) = setEnabledSetting(context, false)

        private fun setEnabledSetting(context: Context, enabled: Boolean) {
            context.packageManager.setComponentEnabledSetting(
                context.componentName,
                if (enabled) COMPONENT_ENABLED_STATE_ENABLED else COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        fun isEnabled(context: Context): Boolean =
            context.packageManager.getComponentEnabledSetting(context.componentName) ==
                COMPONENT_ENABLED_STATE_ENABLED

        private val Context.componentName
            get() = ComponentName(
                this,
                StartupTracingConfigStoreIsEnabledGate::class.java.name
            )
    }
}

internal object StartupTracingConfigStore {
    private const val KEY_IS_PERSISTENT = "isPersistent"
    private const val KEY_LIB_FILE_PATH = "libtracingPerfettoFilePath"
    private const val STARTUP_CONFIG_FILE_NAME = "libtracing_perfetto_startup.properties"

    private fun startupConfigFileForPackageName(packageName: String): File =
        File("/sdcard/Android/media/$packageName/$STARTUP_CONFIG_FILE_NAME")

    /** Loads the config */
    fun load(context: Context): StartupTracingConfig? {
        // use the fast-check-gate value
        if (!StartupTracingConfigStoreIsEnabledGate.isEnabled(context)) return null

        // read the config from file
        val propertiesFile = startupConfigFileForPackageName(context.packageName)
        if (!propertiesFile.exists()) return null
        val properties = Properties()
        propertiesFile.reader().use { properties.load(it) }
        return StartupTracingConfig(
            properties.getProperty(KEY_LIB_FILE_PATH),
            properties.getProperty(KEY_IS_PERSISTENT).toBoolean()
        )
    }

    /** Stores the config */
    fun StartupTracingConfig.store(context: Context) {
        startupConfigFileForPackageName(context.packageName)
            .bufferedWriter()
            .use { writer ->
                Properties().also {
                    it.setProperty(KEY_LIB_FILE_PATH, libFilePath)
                    it.setProperty(KEY_IS_PERSISTENT, isPersistent.toString())
                }.store(writer, null)
            }
        StartupTracingConfigStoreIsEnabledGate.enable(context) // update the fast-check-gate value
    }

    /** Deletes the config */
    fun clear(context: Context) {
        StartupTracingConfigStoreIsEnabledGate.disable(context) // update the fast-check-gate value
        startupConfigFileForPackageName(context.packageName).delete()
    }
}
