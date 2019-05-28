/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.impl.background.gcm

import android.content.Context
import android.util.Log
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.Configuration
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.utils.SynchronousExecutor
import com.google.android.gms.gcm.GcmNetworkManager
import com.google.android.gms.gcm.TaskParams
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.concurrent.Executor

@RunWith(AndroidJUnit4::class)
@SmallTest
class WorkManagerGcmDispatcherTest {
    lateinit var mContext: Context
    lateinit var mExecutor: Executor
    lateinit var mWorkManager: WorkManagerImpl
    lateinit var mDispatcher: WorkManagerGcmDispatcher

    @Before
    fun setUp() {
        mContext = ApplicationProvider.getApplicationContext()
        mExecutor = SynchronousExecutor()
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                runnable.run()
            }

            override fun isMainThread(): Boolean {
                return true
            }

            override fun postToMainThread(runnable: Runnable) {
                runnable.run()
            }
        })

        val workTaskExecutor: androidx.work.impl.utils.taskexecutor.TaskExecutor =
            object : androidx.work.impl.utils.taskexecutor.TaskExecutor {
                override fun postToMainThread(runnable: Runnable?) {
                    mExecutor.execute(runnable)
                }

                override fun getMainThreadExecutor(): Executor {
                    return mExecutor
                }

                override fun executeOnBackgroundThread(runnable: Runnable?) {
                    mExecutor.execute(runnable)
                }

                override fun getBackgroundExecutorThread(): Thread {
                    return Thread.currentThread()
                }

                override fun getBackgroundExecutor(): Executor {
                    return mExecutor
                }
            }

        val configuration = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(mExecutor)
            .build()

        mWorkManager = WorkManagerImpl(mContext, configuration, workTaskExecutor, true)
        WorkManagerImpl.setDelegate(mWorkManager)
        mDispatcher = WorkManagerGcmDispatcher(mContext)
    }

    @Test
    fun testNullWorkSpecId() {
        val taskParams = mock(TaskParams::class.java)
        `when`(taskParams.tag).thenReturn(null)
        val result = mDispatcher.onRunTask(taskParams)
        assert(result == GcmNetworkManager.RESULT_FAILURE)
    }

    @Test
    fun testWorkSpecIdThatDoesNotExit() {
        val taskParams = mock(TaskParams::class.java)
        `when`(taskParams.tag).thenReturn("InvalidWorkSpecId")
        val result = mDispatcher.onRunTask(taskParams)
        assert(result == GcmNetworkManager.RESULT_FAILURE)
    }
}
