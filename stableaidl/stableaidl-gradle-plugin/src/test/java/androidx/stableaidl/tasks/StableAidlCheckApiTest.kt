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
class StableAidlCheckApiTest {
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
    fun testStableAidlCheckApiRunnable() {
        val expectedApiDir = temporaryFolder.newFolder()
        createFile("1.aidl", expectedApiDir)
        createFile("2.aidl", expectedApiDir)
        createFile("3.aidl", expectedApiDir)

        val actualApiDir = temporaryFolder.newFolder()
        createFile("1.aidl", actualApiDir)

        val fakeExe = temporaryFolder.newFile("fake.exe")

        val fakeFramework = temporaryFolder.newFolder("fakeFramework")

        StableAidlCheckApi.aidlCheckApiDelegate(
            workers,
            fakeExe,
            fakeFramework,
            listOf(
                "--structured",
                "--checkapi=equal",
                expectedApiDir.absolutePath,
                actualApiDir.absolutePath
            ),
            listOf(),
            listOf()
        )

        // Check that executable only runs once and arguments are intact.
        Truth.assertThat(execOperations.capturedExecutions).hasSize(1)
        for (processInfo in execOperations.capturedExecutions) {
            Truth.assertThat(processInfo.executable).isEqualTo(fakeExe.canonicalPath)

            Truth.assertThat(processInfo.args).containsAtLeast(
                // TODO: Remove when the framework has been fully annotated.
                // "-p" + fakeFramework.canonicalPath,
                "--structured",
                "--checkapi=equal",
                expectedApiDir.absolutePath,
                actualApiDir.absolutePath
            )
        }
    }
}
