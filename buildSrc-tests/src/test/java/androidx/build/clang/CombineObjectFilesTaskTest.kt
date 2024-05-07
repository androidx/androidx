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
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Test

class CombineObjectFilesTaskTest : BaseClangTest() {
    @Test
    fun configureAndExecute() {
        val multiTargetNativeCompilation = clangExtension.createNativeCompilation(
            "code"
        ) {
        }
        val allTargets = listOf(
            KonanTarget.LINUX_X64, KonanTarget.ANDROID_X64, KonanTarget.ANDROID_X86,
            KonanTarget.ANDROID_ARM64
        )
        val chosenTargets = allTargets - KonanTarget.ANDROID_X64
        multiTargetNativeCompilation.configureTargets(allTargets)
        val taskOutputDir = tmpFolder.newFolder()
        val taskProvider = project.tasks.register(
            "combineTask",
            CombineObjectFilesTask::class.java
        ) {
            it.outputDirectory.set(taskOutputDir)
        }
        taskProvider.configureFrom(multiTargetNativeCompilation) {
            it in chosenTargets
        }
        // create the input files so that we can run the task
        // we'll only create it for targets we've chosen to ensure the task doesn't add more
        // than it needs to
        chosenTargets.forEach { chosenTarget ->
            multiTargetNativeCompilation.sharedObjectOutputFor(
                chosenTarget
            ).get().asFile.also {
                it.parentFile.mkdirs()
                // write target name to the so file so that we can assert task outputs
                it.writeText(chosenTarget.name)
            }
        }

        // execute the task action
        taskProvider.get().combineLibraries()
        val outputContents = taskOutputDir.walkTopDown()
            .filter { it.isFile }
            .map {
                it.relativeTo(taskOutputDir).path to it.readText()
            }.toList()
        assertThat(outputContents).containsExactly(
            "natives/linux_x64/libcode.so" to KonanTarget.LINUX_X64.name,
            "x86/libcode.so" to KonanTarget.ANDROID_X86.name,
            "arm64-v8a/libcode.so" to KonanTarget.ANDROID_ARM64.name
        )
    }
}
