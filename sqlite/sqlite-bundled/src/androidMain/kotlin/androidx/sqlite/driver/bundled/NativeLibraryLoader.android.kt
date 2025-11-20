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

package androidx.sqlite.driver.bundled

import java.io.File

/** Helper class to load native libraries based on the host platform. */
internal actual object NativeLibraryLoader {

    private const val LIB_PATH_PROPERTY_NAME = "androidx.sqlite.driver.bundled.path"
    private const val LIB_NAME_PROPERTY_NAME = "androidx.sqlite.driver.bundled.name"

    actual fun loadLibrary(name: String): Unit =
        synchronized(this) {
            // Load from configured property path
            val libraryPath = System.getProperty(LIB_PATH_PROPERTY_NAME)
            val libraryName = System.getProperty(LIB_NAME_PROPERTY_NAME)
            if (libraryPath != null && libraryName != null) {
                val libFile = File(libraryPath, libraryName)
                check(libFile.exists()) {
                    "Cannot find a suitable SQLite binary at the configured path" +
                        "($LIB_PATH_PROPERTY_NAME = $libraryPath). " +
                        "File $libFile does not exist."
                }
                @Suppress("UnsafeDynamicallyLoadedCode") System.load(libFile.absolutePath)
                return
            }

            // Load from APK natives
            System.loadLibrary(name)
        }
}
