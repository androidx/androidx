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

package androidx.stableaidl.tasks

import androidx.stableaidl.internal.fixtures.FakeNoOpWorkAction
import androidx.stableaidl.tasks.StableAidlCompile.Companion.aidlCompileDelegate
import com.android.build.gradle.internal.fixtures.FakeGradleExecOperations
import com.android.build.gradle.internal.fixtures.FakeGradleWorkExecutor
import com.android.build.gradle.internal.fixtures.FakeInjectableService
import com.google.common.truth.Truth
import kotlin.reflect.jvm.javaMethod
import org.gradle.api.DefaultTask
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.workers.WorkerExecutor
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StableAidlCompileTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val execOperations = FakeGradleExecOperations()

    private lateinit var workers: WorkerExecutor
    private lateinit var instantiatorTask: DefaultTask

    @Before
    fun setup() {
        with(ProjectBuilder.builder().withProjectDir(temporaryFolder.newFolder()).build()) {
            workers = FakeGradleWorkExecutor(
                objects, temporaryFolder.newFolder(), listOf(
                    FakeInjectableService(
                        FakeNoOpWorkAction::execOperations.getter.javaMethod!!,
                        execOperations
                    )
                )
            )
            instantiatorTask = tasks.create("task", DefaultTask::class.java)
        }
    }

    @Test
    fun testStableAidlCompileRunnable() {
        val sourceFolder = temporaryFolder.newFolder()
        val file1 = createFile("1.aidl", sourceFolder)
        val file2 = createFile("2.aidl", sourceFolder)
        val file3 = createFile("3.aidl", sourceFolder)
        val noise = createFile("noise.txt", sourceFolder)

        val outputDir = temporaryFolder.newFolder("outputDir")

        val fakeExe = temporaryFolder.newFile("fake.exe")

        val fakeFramework = temporaryFolder.newFolder("fakeFramework")

        aidlCompileDelegate(
            workers,
            fakeExe,
            fakeFramework,
            outputDir,
            null,
            listOf("-x"),
            listOf(sourceFolder),
            listOf(),
            listOf()
        )

        // Check that executable only runs for aidl files, and properly locates the framework
        // and output dir
        Truth.assertThat(execOperations.capturedExecutions).hasSize(3)
        for (processInfo in execOperations.capturedExecutions) {
            Truth.assertThat(processInfo.executable).isEqualTo(fakeExe.canonicalPath)

            Truth.assertThat(processInfo.args).containsAtLeast(
                // TODO: Remove when the framework has been fully annotated.
                // "-p" + fakeFramework.canonicalPath,
                "-o" + outputDir.absolutePath,
                "-x"
            )

            Truth.assertThat(processInfo.args).containsAnyOf(
                file1.absolutePath,
                file2.absolutePath,
                file3.absolutePath
            )

            Truth.assertThat(processInfo.args).doesNotContain(noise.absolutePath)
        }
    }
}
