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

package androidx.work.multiprocess

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_CLASS_NAME
import androidx.work.multiprocess.RemoteListenableWorker.ARGUMENT_PACKAGE_NAME
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkerinRemoteProcessTest {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val configuration = Configuration.Builder()
        .setMinimumLoggingLevel(Log.VERBOSE)
        .build()
    val taskExecutor = WorkManagerTaskExecutor(configuration.taskExecutor)
    val workManager = WorkManagerImpl(
        context = context,
        configuration = configuration,
        workTaskExecutor = taskExecutor,
    )

    init {
        WorkManagerImpl.setDelegate(workManager)
    }

    @SdkSuppress(minSdkVersion = 27)
    @MediumTest
    @Test
    fun runWorker() = runBlocking {
        val componentName = ComponentName("androidx.work.multiprocess.test",
            RemoteWorkerService2::class.java.canonicalName!!)
        val workRequest = OneTimeWorkRequestBuilder<RemoteSuccessWorker>()
            .setInputData(
                workDataOf(
                    ARGUMENT_PACKAGE_NAME to componentName.packageName,
                    ARGUMENT_CLASS_NAME to componentName.className,
                )
            )
            .build()
        workManager.enqueue(workRequest)
        val finished = workManager.getWorkInfoByIdFlow(workRequest.id).filter {
            it?.state?.isFinished ?: false
        }.first()
        assertThat(finished!!.state).isEqualTo(WorkInfo.State.SUCCEEDED)
    }
}
