/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.nativeloader

import androidx.annotation.RestrictTo
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Native code loader for JVM. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
actual public object NativeLoader {
    actual public fun load() {
        // On the JVM we need to find the correct libink library file in the JAR resources, copy it
        // out to a tempfile, and load it directly.
        //
        // See
        // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:sqlite/sqlite-bundled/src/jvmMain/kotlin/androidx/sqlite/driver/bundled/NativeLibraryLoader.jvm.kt;l=24;drc=a70f25f13b00141438854309a0de47d537904522
        // for a similar system.
        val tempFile = Files.createTempFile("libink.so", null).apply { toFile().deleteOnExit() }
        NativeLoader::class
            .java
            .classLoader!!
            // If additional architectures need to be supported, construct the correct resource
            // prefix for that platform.
            .getResourceAsStream("linux-x86_64/libink.so")
            .use { resourceStream ->
                Files.copy(resourceStream, tempFile, StandardCopyOption.REPLACE_EXISTING)
            }
        System.load(tempFile.toFile().canonicalPath)
    }
}
