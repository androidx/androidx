/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work.inspection

import androidx.inspection.InspectorEnvironment
import androidx.inspection.testing.DefaultTestInspectorEnvironment
import androidx.inspection.testing.InspectorTester
import androidx.inspection.testing.TestInspectorExecutors
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

private const val WORK_MANAGER_INSPECTOR_ID = "androidx.work.inspection"

class WorkManagerInspectorTestEnvironment : ExternalResource() {
    private lateinit var inspectorTester: InspectorTester
    private lateinit var environment: FakeInspectorEnvironment
    private val job = Job()

    override fun before() {
        environment = FakeInspectorEnvironment(job)
        inspectorTester = runBlocking {
            InspectorTester(
                inspectorId = WORK_MANAGER_INSPECTOR_ID,
                environment = environment
            )
        }
    }

    override fun after() {
        runBlocking {
            job.cancelAndJoin()
        }
        inspectorTester.dispose()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun assertNoQueuedEvents() {
        assertThat(inspectorTester.channel.isEmpty).isTrue()
    }
}

/**
 * Empty Fake inspector environment.
 */
private class FakeInspectorEnvironment(
    job: Job
) : DefaultTestInspectorEnvironment(TestInspectorExecutors(job)) {
    override fun <T : Any?> findInstances(clazz: Class<T>): MutableList<T> {
        TODO("not implemented")
    }

    override fun registerEntryHook(
        originClass: Class<*>,
        originMethod: String,
        entryHook: InspectorEnvironment.EntryHook
    ) {
        TODO("not implemented")
    }

    override fun <T : Any?> registerExitHook(
        originClass: Class<*>,
        originMethod: String,
        exitHook: InspectorEnvironment.ExitHook<T>
    ) {
        TODO("not implemented")
    }
}
