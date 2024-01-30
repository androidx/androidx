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

package androidx.work

import android.content.Context
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.work.Configuration.Companion.MIN_SCHEDULER_LIMIT
import androidx.work.Constraints.ContentUriTrigger
import androidx.work.impl.Scheduler
import androidx.work.impl.WorkDatabase
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.schedulers
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.worker.TestWorker
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 24)
@MediumTest
@RunWith(AndroidJUnit4::class)
class ContentUriTriggerWorkersTest {
    val testLimit = 4
    val context = ApplicationProvider.getApplicationContext<Context>()
    val configuration = Configuration.Builder()
        .setMaxSchedulerLimit(MIN_SCHEDULER_LIMIT)
        .setContentUriTriggerWorkersLimit(testLimit)
        .build()
    val executor = Executors.newSingleThreadExecutor()
    val taskExecutor = WorkManagerTaskExecutor(executor)
    internal val testScheduler = TestScheduler()
    val workManager = WorkManagerImpl(
        context = context,
        configuration = configuration,
        workTaskExecutor = taskExecutor,
        workDatabase = WorkDatabase.create(context, executor, configuration.clock, true),
        schedulersCreator = schedulers(testScheduler)
    )

    @Test
    fun maxSchedulerLimitNotApplicable() {
        repeat(MIN_SCHEDULER_LIMIT) {
            workManager.enqueue(OneTimeWorkRequest.from(TestWorker::class.java)).result.get()
        }
        assertThat(testScheduler.mutableIds.size).isEqualTo(MIN_SCHEDULER_LIMIT)
        workManager.enqueue(OneTimeWorkRequest.from(TestWorker::class.java)).result.get()
        // not scheduled in scheduler, because it is goes over limit
        assertThat(testScheduler.mutableIds.size).isEqualTo(MIN_SCHEDULER_LIMIT)
        val triggers = setOf(ContentUriTrigger(EXTERNAL_CONTENT_URI, false))
        val requestWithUris = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(contentUriTriggers = triggers)).build()
        workManager.enqueue(requestWithUris).result.get()
        assertThat(testScheduler.mutableIds.last()).isEqualTo(requestWithUris.stringId)
    }

    @Test
    fun contentUriTriggerWorkersLimitApplicable() {
        val triggers = setOf(ContentUriTrigger(EXTERNAL_CONTENT_URI, false))
        repeat(testLimit) {
            val requestWithUris = OneTimeWorkRequest.Builder(TestWorker::class.java)
                .setConstraints(Constraints(contentUriTriggers = triggers)).build()
            workManager.enqueue(requestWithUris).result.get()
        }
        val requestWithUris = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(Constraints(contentUriTriggers = triggers)).build()
        try {
            workManager.enqueue(requestWithUris).result.get()
            throw AssertionError("workManager.enqueue expected to fail")
        } catch (e: ExecutionException) {
            // expected exception
            assertThat(e.cause).isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}

internal class TestScheduler : Scheduler {
    val mutableIds = mutableListOf<String>()

    override fun schedule(vararg workSpecs: WorkSpec) {
        mutableIds.addAll(workSpecs.map { it.id })
    }

    override fun cancel(workSpecId: String) {
    }

    override fun hasLimitedSchedulingSlots() = true
}
