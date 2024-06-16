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

package androidx.work.impl.background.systemjob

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobScheduler.RESULT_SUCCESS
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context.JOB_SCHEDULER_SERVICE
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequest
import androidx.work.testutils.TestEnv
import androidx.work.worker.TestWorker
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RequiresApi(34)
@RunWith(AndroidJUnit4::class)
class JobSchedulerNamespaceTest {
    private val env = TestEnv(Configuration.Builder().build())
    private val systemJobScheduler = with(env) { SystemJobScheduler(context, db, configuration) }
    private val globalJobScheduler =
        env.context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler

    @Before
    fun setup() {
        // making sure we start at the clean state
        globalJobScheduler.cancelInAllNamespaces()
        assertThat(globalJobScheduler.pendingJobsInAllNamespaces.size).isEqualTo(0)
    }

    @SdkSuppress(minSdkVersion = 34)
    @MediumTest
    @Test
    fun checkNamespaces() {
        val request =
            OneTimeWorkRequest.Builder(TestWorker::class.java)
                .setInitialDelay(365, TimeUnit.DAYS)
                .build()
        env.db.workSpecDao().insertWorkSpec(request.workSpec)
        systemJobScheduler.schedule(request.workSpec)
        val pendingJobs = globalJobScheduler.pendingJobsInAllNamespaces
        assertThat(pendingJobs[null]).isNull()
        val workManagerJobs = pendingJobs[WORKMANAGER_NAMESPACE]
        assertThat(workManagerJobs).isNotNull()
        assertThat(workManagerJobs).hasSize(1)
    }

    @SdkSuppress(minSdkVersion = 34)
    @MediumTest
    @Test
    fun cancelAllInAllNamespaces() {
        val componentName = ComponentName(env.context, WhateverJobService::class.java)
        val jobInfo =
            JobInfo.Builder(10, componentName)
                .setMinimumLatency(TimeUnit.DAYS.toMillis(365))
                .build()
        assertThat(globalJobScheduler.schedule(jobInfo)).isEqualTo(RESULT_SUCCESS)

        val workManagerComponent = ComponentName(env.context, SystemJobService::class.java)
        val oldWorkManagerInfo =
            JobInfo.Builder(12, workManagerComponent)
                .setMinimumLatency(TimeUnit.DAYS.toMillis(365))
                .build()
        assertThat(globalJobScheduler.schedule(oldWorkManagerInfo)).isEqualTo(RESULT_SUCCESS)

        val request = prepareWorkSpec()
        systemJobScheduler.schedule(request.workSpec)
        val pendingJobsPreCancellation = globalJobScheduler.pendingJobsInAllNamespaces
        assertThat(pendingJobsPreCancellation).hasSize(2)
        assertThat(pendingJobsPreCancellation[null]).hasSize(2)
        SystemJobScheduler.cancelAllInAllNamespaces(env.context)
        val pendingJobsPostCancellation = globalJobScheduler.pendingJobsInAllNamespaces
        assertThat(pendingJobsPostCancellation).hasSize(1)
        assertThat(pendingJobsPostCancellation[null]).hasSize(1)
    }

    @SdkSuppress(minSdkVersion = 34)
    @MediumTest
    @Test
    fun reconcileTest() {
        val request = prepareWorkSpec()
        systemJobScheduler.schedule(request.workSpec)
        val workManagerComponent = ComponentName(env.context, SystemJobService::class.java)
        val unknownWorkInfo =
            JobInfo.Builder(4000, workManagerComponent)
                .setMinimumLatency(TimeUnit.DAYS.toMillis(365))
                .build()
        env.context.wmJobScheduler.schedule(unknownWorkInfo)
        globalJobScheduler.schedule(unknownWorkInfo)
        val preReconcile = globalJobScheduler.pendingJobsInAllNamespaces
        assertThat(preReconcile[WORKMANAGER_NAMESPACE]).hasSize(2)
        assertThat(preReconcile[null]).hasSize(1)
        SystemJobScheduler.reconcileJobs(env.context, env.db)
        val postReconcile = globalJobScheduler.pendingJobsInAllNamespaces
        assertThat(postReconcile).hasSize(2)
        assertThat(postReconcile[WORKMANAGER_NAMESPACE]).hasSize(1)
        assertThat(preReconcile[null]).hasSize(1)
    }

    @After
    fun tearDown() {
        globalJobScheduler.cancelInAllNamespaces()
    }

    private fun prepareWorkSpec(): OneTimeWorkRequest {
        val request =
            OneTimeWorkRequest.Builder(TestWorker::class.java)
                .setInitialDelay(365, TimeUnit.DAYS)
                .build()
        env.db.workSpecDao().insertWorkSpec(request.workSpec)
        return request
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class WhateverJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        throw UnsupportedOperationException()
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        throw UnsupportedOperationException()
    }
}
