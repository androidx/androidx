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

package androidx.build.clang

import androidx.build.ProjectLayoutType
import java.io.File
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * Represents a C compilation for a single [konanTarget].
 *
 * @param konanTarget Target host for the compilation.
 * @param compileTask The task that compiles the sources and build .o file for each source file.
 * @param archiveTask The task that will archive the output of the [compileTask] into a single .a
 *   file.
 * @param sharedLibTask The task that will created a shared library from the output of [compileTask]
 *   that also optionally links with [linkedObjects]
 * @param sources List of source files for the compilation.
 * @param includes List of include directories containing .h files for the compilation.
 * @param linkedObjects List of object files that should be dynamically linked in the final shared
 *   object output.
 * @param linkerArgs Arguments that will be passed into linker when creating a shared library.
 * @param freeArgs Arguments that will be passed into clang for compilation.
 */
class NativeTargetCompilation
internal constructor(
    val project: Project,
    val konanTarget: KonanTarget,
    internal val compileTask: TaskProvider<ClangCompileTask>,
    internal val archiveTask: TaskProvider<ClangArchiveTask>,
    internal val sharedLibTask: TaskProvider<ClangSharedLibraryTask>,
    val sources: ConfigurableFileCollection,
    val includes: ConfigurableFileCollection,
    val linkedObjects: ConfigurableFileCollection,
    @Suppress("unused") // used via build.gradle
    val linkerArgs: ListProperty<String>,
    @Suppress("unused") // used via build.gradle
    val freeArgs: ListProperty<String>
) : Named {
    override fun getName(): String = konanTarget.name

    /**
     * Dynamically links the shared library output of this target with the given [dependency]'s
     * object library output.
     */
    @Suppress("unused") // used from build.gradle
    fun linkWith(dependency: MultiTargetNativeCompilation) {
        linkedObjects.from(dependency.sharedObjectOutputFor(konanTarget))
    }

    /**
     * Statically include the shared library output of this target with the given [dependency]'s
     * archive library output.
     */
    @Suppress("unused") // used from build.gradle
    fun include(dependency: MultiTargetNativeCompilation) {
        linkedObjects.from(dependency.sharedArchiveOutputFor(konanTarget))
    }

    /** Convenience method to add jni headers to the compilation. */
    @Suppress("unused") // used from build.gradle
    fun addJniHeaders() {
        if (konanTarget.family == Family.ANDROID) {
            // android already has JNI
            return
        }

        includes.from(project.provider { findJniHeaderDirectories() })
    }

    private fun findJniHeaderDirectories(): List<File> {
        // TODO b/306669673 add support for GitHub builds.
        // we need to find 2 jni header files
        // jni.h -> This is the same across all platforms
        // jni_md.h -> Includes machine dependant definitions.
        // Internal Devs: You can read more about it here:  http://go/androidx-jni-cross-compilation
        val javaHome = File(System.getProperty("java.home"))
        if (ProjectLayoutType.isPlayground(project)) {
            return findJniHeadersInPlayground(javaHome)
        }
        // for jni_md, we need to find the prebuilts because each jdk ships with jni_md only for
        // its own target family.
        val jdkPrebuiltsRoot = javaHome.parentFile

        val relativeHeaderPaths =
            when (konanTarget.family) {
                Family.MINGW -> {
                    listOf("windows-x86/include", "windows-x86/include/win32")
                }
                Family.OSX -> {
                    // it is OK that we are using x86 here, they are the same files (openjdk only
                    // distinguishes between unix and windows).
                    listOf("darwin-x86/include", "darwin-x86/include/darwin")
                }
                Family.LINUX -> {
                    listOf(
                        "linux-x86/include",
                        "linux-x86/include/linux",
                    )
                }
                else -> error("unsupported family ($konanTarget) for JNI compilation")
            }
        return relativeHeaderPaths
            .map { jdkPrebuiltsRoot.resolve(it) }
            .onEach {
                check(it.exists()) {
                    "Cannot find header directory (${it.name}) in ${it.canonicalPath}"
                }
            }
    }

    /**
     * JDK ships with JNI headers only for the current platform. As a result, we don't have access
     * to cross-platform jni headers. They are mostly the same and we don't ship cross compiled code
     * from GitHub so it is acceptable to use local JNI headers for cross platform compilation on
     * GitHub.
     */
    private fun findJniHeadersInPlayground(javaHome: File): List<File> {
        val include = File(javaHome, "include")
        if (!include.exists()) {
            error("Cannot find header directory in $javaHome")
        }
        return listOf(
                include,
                File(include, "darwin"),
                File(include, "linux"),
                File(include, "win32"),
            )
            .filter { it.exists() }
    }
}
