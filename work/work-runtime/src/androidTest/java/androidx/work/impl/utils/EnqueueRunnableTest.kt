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

package androidx.work.impl.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.impl.Scheduler
import androidx.work.impl.WorkContinuationImpl
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.model.WorkSpec
import androidx.work.testutils.TestEnv
import androidx.work.testutils.WorkManager
import androidx.work.worker.TestWorker
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class EnqueueRunnableTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var scheduled = false
    private val trackingScheduler =
        object : Scheduler {
            override fun schedule(vararg workSpecs: WorkSpec?) {
                scheduled = true
            }

            override fun cancel(workSpecId: String) {}

            override fun hasLimitedSchedulingSlots() = false
        }
    private val env = TestEnv(Configuration.Builder().build())
    private val wm =
        WorkManager(
            env = env,
            schedulers = listOf(trackingScheduler),
            Trackers(context, env.taskExecutor)
        )

    @Test
    fun testCheckScheduling() {
        val request1 = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val impl1 = WorkContinuationImpl(wm, "name", ExistingWorkPolicy.KEEP, listOf(request1))
        EnqueueRunnable.enqueue(impl1)
        assertThat(wm.getWorkInfoById(request1.id).get()).isNotNull()
        // it is first time enqueued and show be requested to scheduled
        assertThat(scheduled).isTrue()
        scheduled = false
        val request2 = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        // there is already a work with this name + policy keep, it shouldn't be scheduled
        val impl2 = WorkContinuationImpl(wm, "name", ExistingWorkPolicy.KEEP, listOf(request2))
        EnqueueRunnable.enqueue(impl2)
        assertThat(scheduled).isFalse()
    }
}
