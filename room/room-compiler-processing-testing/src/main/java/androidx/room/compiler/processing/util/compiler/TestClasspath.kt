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

package androidx.room.compiler.processing.util.compiler

import androidx.room.compiler.processing.util.getSystemClasspaths
import java.io.File
import java.net.URLClassLoader

/** Test runtime classpath helper. */
internal object TestClasspath {
    internal val inheritedClasspath by
        lazy(LazyThreadSafetyMode.NONE) {
            getClasspathFromClassloader(KotlinCliRunner::class.java.classLoader)
        }

    // Ported from
    // https://github.com/google/compile-testing/blob/master/src/main/java/com/google/testing/compile/Compiler.java#L231
    private fun getClasspathFromClassloader(referenceClassLoader: ClassLoader): List<File> {
        val platformClassLoader: ClassLoader = ClassLoader.getPlatformClassLoader()
        var currentClassloader = referenceClassLoader
        val systemClassLoader = ClassLoader.getSystemClassLoader()

        // Concatenate search paths from all classloaders in the hierarchy
        // 'till the system classloader.
        val classpaths: MutableSet<String> = LinkedHashSet()
        while (true) {
            if (currentClassloader === systemClassLoader) {
                classpaths.addAll(getSystemClasspaths())
                break
            }
            if (currentClassloader === platformClassLoader) {
                break
            }
            check(currentClassloader is URLClassLoader) {
                "Classpath for compilation could not be extracted since $currentClassloader " +
                    "is not an instance of URLClassloader"
            }

            // We only know how to extract classpaths from URLClassloaders.
            currentClassloader.urLs.forEach { url ->
                check(url.protocol == "file") {
                    "Given classloader consists of classpaths which are unsupported for " +
                        "compilation."
                }
                classpaths.add(url.path)
            }
            currentClassloader = currentClassloader.parent
        }
        return classpaths.map { File(it) }.filter { it.exists() }
    }
}
