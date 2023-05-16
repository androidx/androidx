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

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import java.io.File
import java.io.Writer
import java.util.Properties

/**
 * Config for enabling tracing at app startup
 *
 * @param libFilePath Path to the optionally sideloaded `libtracing_perfetto.so` file
 * @param isPersistent Determines whether tracing should remain enabled (sticky) between app runs
 */
@RestrictTo(LIBRARY_GROUP)
internal data class StartupTracingConfig(val libFilePath: String?, val isPersistent: Boolean)

@RestrictTo(LIBRARY_GROUP)
internal object StartupTracingConfigStore {
    private const val KEY_IS_PERSISTENT = "isPersistent"
    private const val KEY_LIB_FILE_PATH = "libtracingPerfettoFilePath"
    private const val STARTUP_CONFIG_FILE_NAME = "libtracing_perfetto_startup.properties"

    private fun startupConfigFileForPackageName(packageName: String): File =
        File("/sdcard/Android/media/$packageName/$STARTUP_CONFIG_FILE_NAME")

    /** Loads the config */
    fun load(packageName: String): StartupTracingConfig? {
        // read the config from file
        val propertiesFile = startupConfigFileForPackageName(packageName)
        if (!propertiesFile.exists()) return null
        val properties = Properties()
        propertiesFile.reader().use { properties.load(it) }
        return StartupTracingConfig(
            properties.getProperty(KEY_LIB_FILE_PATH),
            properties.getProperty(KEY_IS_PERSISTENT).toBoolean()
        )
    }

    /** Stores the config */
    fun StartupTracingConfig.store(packageName: String): Unit =
        startupConfigFileForPackageName(packageName)
            .bufferedWriter()
            .use { store(it) }

    /**
     * Stores the config as a [Properties] string
     *
     * The caller is responsible for closing the passed-in [Writer]
     */
    private fun StartupTracingConfig.store(writer: Writer) =
        Properties().also {
            it.setProperty(KEY_LIB_FILE_PATH, libFilePath)
            it.setProperty(KEY_IS_PERSISTENT, isPersistent.toString())
        }.store(writer, null)

    /** Deletes the config */
    fun clear(packageName: String) {
        startupConfigFileForPackageName(packageName).delete()
    }
}