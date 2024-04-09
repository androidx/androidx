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

import com.google.common.truth.Truth.assertThat
import org.gradle.api.file.ConfigurableFileCollection
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.AssumptionViolatedException
import org.junit.Test

class AndroidXClangTest : BaseClangTest() {

    @Test
    fun addJniHeaders() {
        val multiTargetNativeCompilation = clangExtension.createNativeCompilation(
            "mylib"
        ) {
            it.configureEachTarget {
                it.addJniHeaders()
            }
        }
        multiTargetNativeCompilation.configureTargets(
            listOf(KonanTarget.LINUX_X64, KonanTarget.ANDROID_X64)
        )
        // trigger configuration
        multiTargetNativeCompilation.targetProvider(KonanTarget.LINUX_X64).get()
        multiTargetNativeCompilation.targetProvider(KonanTarget.ANDROID_X64).get()
        val compileTasks = project.tasks.withType(ClangCompileTask::class.java).toList()
        val linuxCompileTask = compileTasks.first {
            it.clangParameters.konanTarget.get().asKonanTarget == KonanTarget.LINUX_X64
        }
        // make sure it includes linux header
        assertThat(
            linuxCompileTask.clangParameters.includes.regularFileNames()
        ).contains("jni.h")
        val androidCompileTask = compileTasks.first {
            it.clangParameters.konanTarget.get().asKonanTarget == KonanTarget.ANDROID_X64
        }
        // android has jni in sysroots, hence we shouldn't add that
        assertThat(
            androidCompileTask.clangParameters.includes.regularFileNames()
        ).doesNotContain("jni.h")
    }

    @Test
    fun configureTargets() {
        val commonSourceFolders = tmpFolder.newFolder("src").also {
            it.resolve("src1.c").writeText("")
            it.resolve("src2.c").writeText("")
        }
        val commonIncludeFolders = listOf(
            tmpFolder.newFolder("include1"),
            tmpFolder.newFolder("include2"),
        )
        val linuxSrcFolder = tmpFolder.newFolder("linuxOnlySrc").also {
            it.resolve("linuxSrc1.c").writeText("")
            it.resolve("linuxSrc2.c").writeText("")
        }
        val androidIncludeFolders = listOf(
            tmpFolder.newFolder("androidInclude1"),
            tmpFolder.newFolder("androidInclude2"),
        )
        val multiTargetNativeCompilation = clangExtension.createNativeCompilation(
            "mylib"
        ) {
            it.configureEachTarget {
                it.sources.from(commonSourceFolders)
                it.includes.from(commonIncludeFolders)
                it.freeArgs.addAll("commonArg1", "commonArg2")
                if (it.konanTarget == KonanTarget.LINUX_X64) {
                    it.freeArgs.addAll("linuxArg1")
                }
                if (it.konanTarget == KonanTarget.ANDROID_X64) {
                    it.freeArgs.addAll("androidArg1")
                }
            }
        }
        multiTargetNativeCompilation.configureTarget(KonanTarget.LINUX_X64) {
            it.sources.from(linuxSrcFolder)
        }
        // multiple configure calls on the target
        multiTargetNativeCompilation.configureTarget(KonanTarget.LINUX_X64) {
            it.freeArgs.addAll("linuxArg2")
        }
        multiTargetNativeCompilation.configureTarget(KonanTarget.ANDROID_X64) {
            it.includes.from(androidIncludeFolders)
            it.freeArgs.addAll("androidArg2")
        }

        // Add this check if we can re-enable lazy evaluation b/325518502
//        assertThat(project.tasks.withType(
//            ClangCompileTask::class.java
//        ).toList()).isEmpty()

        // trigger configuration of targets
        multiTargetNativeCompilation.targetProvider(KonanTarget.LINUX_X64).get()
        multiTargetNativeCompilation.targetProvider(KonanTarget.ANDROID_X64).get()

        // make sure it created tasks for it
        project.tasks.withType(ClangCompileTask::class.java).let { compileTasks ->
            // 2 compile tasks, 1 for linux, 1 for android
            assertThat(compileTasks).hasSize(2)
            val linuxTask = compileTasks.first {
                it.clangParameters.konanTarget.get().asKonanTarget == KonanTarget.LINUX_X64
            }
            assertThat(
                linuxTask.clangParameters.sources.regularFileNames()
            ).containsExactly("src1.c", "src2.c", "linuxSrc1.c", "linuxSrc2.c")
            assertThat(
                linuxTask.clangParameters.includes.directoryNames()
            ).containsExactly("include1", "include2")
            assertThat(
                linuxTask.clangParameters.freeArgs.get()
            ).containsExactly("commonArg1", "commonArg2", "linuxArg1", "linuxArg2")

            val androidTask = compileTasks.first {
                it.clangParameters.konanTarget.get().asKonanTarget == KonanTarget.ANDROID_X64
            }
            assertThat(
                androidTask.clangParameters.sources.regularFileNames()
            ).containsExactly("src1.c", "src2.c")
            assertThat(
                androidTask.clangParameters.includes.directoryNames()
            ).containsExactly(
                "androidInclude1", "androidInclude2", "include1", "include2"
            )
            assertThat(
                androidTask.clangParameters.freeArgs.get()
            ).containsExactly("commonArg1", "commonArg2", "androidArg1", "androidArg2")
        }
        // 2 archive tasks, 1 for each target
        project.tasks.withType(ClangArchiveTask::class.java).let { archiveTasks ->
            assertThat(archiveTasks).hasSize(2)
            assertThat(
                archiveTasks.map { it.llvmArchiveParameters.konanTarget.get().asKonanTarget }
            ).containsExactly(
                KonanTarget.LINUX_X64,
                KonanTarget.ANDROID_X64
            )
            archiveTasks.forEach { archiveTask ->
                assertThat(
                    archiveTask.llvmArchiveParameters.outputFile.get().asFile.name
                ).isEqualTo(
                    "libmylib.a"
                )
            }
        }

        // 2 shared library tasks, 1 for each target
        project.tasks.withType(ClangSharedLibraryTask::class.java).let { soTasks ->
            assertThat(
                soTasks.map { it.clangParameters.konanTarget.get().asKonanTarget }
            ).containsExactly(
                KonanTarget.LINUX_X64,
                KonanTarget.ANDROID_X64
            )
            soTasks.forEach {
                assertThat(
                    it.clangParameters.outputFile.get().asFile.name
                ).isEqualTo("libmylib.so")
            }
        }
    }

    @Test
    fun configureDisabledTarget() {
        if (HostManager.hostIsMac) {
            throw AssumptionViolatedException(
                """
                All targets are enabled on mac, hence we cannot end-to-end test disabled targets.
            """.trimIndent()
            )
        }
        val multiTargetNativeCompilation = clangExtension.createNativeCompilation(
            "mylib"
        ) {
            it.configureEachTarget {
                it.sources.from(tmpFolder.newFolder())
            }
        }
        multiTargetNativeCompilation.configureTarget(KonanTarget.LINUX_X64)
        multiTargetNativeCompilation.configureTarget(KonanTarget.MACOS_ARM64)
        assertThat(multiTargetNativeCompilation.hasTarget(
            KonanTarget.LINUX_X64
        )).isTrue()
        assertThat(multiTargetNativeCompilation.hasTarget(
            KonanTarget.MACOS_ARM64
        )).isFalse()
    }

    @Test
    fun linking() {
        val lib1Sources = tmpFolder.newFolder().also {
            it.resolve("src1.c").writeText("")
        }
        val lib2Sources = tmpFolder.newFolder().also {
            it.resolve("src2.c").writeText("")
        }
        val compilation1 = clangExtension.createNativeCompilation(
            "lib1"
        ) {
            it.configureEachTarget {
                it.sources.from(lib1Sources)
            }
        }
        compilation1.configureTargets(listOf(KonanTarget.LINUX_X64, KonanTarget.ANDROID_X64))
        val compilation2 = clangExtension.createNativeCompilation(
            "lib2"
        ) {
            it.configureEachTarget {
                it.sources.from(lib2Sources)
                it.linkWith(compilation1)
            }
        }
        compilation2.configureTargets(listOf(KonanTarget.LINUX_X64, KonanTarget.ANDROID_X64))
        // trigger configuration
        compilation2.targetProvider(KonanTarget.LINUX_X64).get()
        compilation2.targetProvider(KonanTarget.ANDROID_X64).get()
        val sharedLibrariesTasks = project.tasks.withType(
            ClangSharedLibraryTask::class.java
        ).toList().filter {
            it.name.contains("lib2", ignoreCase = true)
        }
        assertThat(sharedLibrariesTasks).hasSize(2)
        sharedLibrariesTasks.forEach {
            assertThat(
                it.clangParameters.linkedObjects.files.map { it.name }
            ).containsExactly("liblib1.so")
        }
    }

    private fun ConfigurableFileCollection.regularFileNames() = asFileTree.files.map {
        it.name
    }

    private fun ConfigurableFileCollection.directoryNames() = files.flatMap {
        it.walkTopDown()
    }.filter { it.isDirectory }.map { it.name }
}
