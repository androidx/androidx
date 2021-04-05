/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.util

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.config.JvmTarget
import java.io.File
import java.io.OutputStream
import java.net.URLClassLoader

/**
 * Helper class for Kotlin Compile Testing library to have common setup for room.
 */
internal object KotlinCompilationUtil {
    fun prepareCompilation(
        sources: List<Source>,
        outputStream: OutputStream,
        classpaths: List<File> = emptyList()
    ): KotlinCompilation {
        val compilation = KotlinCompilation()
        val srcRoot = compilation.workingDir.resolve("ksp/srcInput")
        val javaSrcRoot = srcRoot.resolve("java")
        val kotlinSrcRoot = srcRoot.resolve("kotlin")
        compilation.sources = sources.map {
            when (it) {
                is Source.JavaSource -> it.toKotlinSourceFile(javaSrcRoot)
                is Source.KotlinSource -> it.toKotlinSourceFile(kotlinSrcRoot)
            }
        }
        // workaround for https://github.com/tschuchortdev/kotlin-compile-testing/issues/105
        compilation.kotlincArguments += "-Xjava-source-roots=${javaSrcRoot.absolutePath}"
        compilation.jvmDefault = "enable"
        compilation.jvmTarget = JvmTarget.JVM_1_8.description
        compilation.inheritClassPath = false
        compilation.verbose = false
        compilation.classpaths = Classpaths.inheritedClasspath + classpaths
        compilation.messageOutputStream = outputStream
        compilation.kotlinStdLibJar = Classpaths.kotlinStdLibJar
        compilation.kotlinStdLibCommonJar = Classpaths.kotlinStdLibCommonJar
        compilation.kotlinStdLibJdkJar = Classpaths.kotlinStdLibJdkJar
        compilation.kotlinReflectJar = Classpaths.kotlinReflectJar
        compilation.kotlinScriptRuntimeJar = Classpaths.kotlinScriptRuntimeJar
        return compilation
    }

    /**
     * Helper object to persist common classpaths resolved by KCT to make sure it does not
     * re-resolve host classpath repeatedly and also runs compilation with a smaller classpath.
     * see: https://github.com/tschuchortdev/kotlin-compile-testing/issues/113
     */
    private object Classpaths {

        val inheritedClasspath: List<File>

        /**
         * These jars are files that Kotlin Compile Testing discovers from classpath. It uses a
         * rather expensive way of discovering these so we cache them here for now.
         *
         * We can remove this cache once we update to a version that includes the fix in KCT:
         * https://github.com/tschuchortdev/kotlin-compile-testing/pull/114
         */
        val kotlinStdLibJar: File?
        val kotlinStdLibCommonJar: File?
        val kotlinStdLibJdkJar: File?
        val kotlinReflectJar: File?
        val kotlinScriptRuntimeJar: File?

        init {
            // create a KotlinCompilation to resolve common jars
            val compilation = KotlinCompilation()
            kotlinStdLibJar = compilation.kotlinStdLibJar
            kotlinStdLibCommonJar = compilation.kotlinStdLibCommonJar
            kotlinStdLibJdkJar = compilation.kotlinStdLibJdkJar
            kotlinReflectJar = compilation.kotlinReflectJar
            kotlinScriptRuntimeJar = compilation.kotlinScriptRuntimeJar

            inheritedClasspath = getClasspathFromClassloader(
                KotlinCompilationUtil::class.java.classLoader
            )
        }
    }

    // ported from https://github.com/google/compile-testing/blob/master/src/main/java/com
    // /google/testing/compile/Compiler.java#L231
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
                """Classpath for compilation could not be extracted
                since $currentClassloader is not an instance of URLClassloader
                """.trimIndent()
            }
            // We only know how to extract classpaths from URLClassloaders.
            currentClassloader.urLs.forEach { url ->
                check(url.protocol == "file") {
                    """Given classloader consists of classpaths which are unsupported for
                    compilation.
                    """.trimIndent()
                }
                classpaths.add(url.path)
            }
            currentClassloader = currentClassloader.parent
        }
        return classpaths.map { File(it) }.filter { it.exists() }
    }
}
