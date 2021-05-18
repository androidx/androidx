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

package androidx.work.impl.utils

import android.app.Notification
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.work.Constraints
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.impl.WorkDatabase
import androidx.work.impl.foreground.ForegroundProcessor
import androidx.work.impl.model.WorkSpecDao
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import androidx.work.worker.TestWorker
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import java.util.UUID

@RunWith(AndroidJUnit4::class)

public class WorkForegroundUpdaterTest {

    private lateinit var mContext: Context
    private lateinit var mDatabase: WorkDatabase
    private lateinit var mWorkSpecDao: WorkSpecDao
    private lateinit var mTaskExecutor: TaskExecutor
    private lateinit var mForegroundProcessor: ForegroundProcessor
    private lateinit var mForegroundInfo: ForegroundInfo

    @Before
    public fun setUp() {
        mContext = mock(Context::class.java)
        mDatabase = mock(WorkDatabase::class.java)
        mWorkSpecDao = mock(WorkSpecDao::class.java)
        `when`(mDatabase.workSpecDao()).thenReturn(mWorkSpecDao)
        mTaskExecutor = InstantWorkTaskExecutor()
        mForegroundProcessor = mock(ForegroundProcessor::class.java)
        val notification = mock(Notification::class.java)
        mForegroundInfo = ForegroundInfo(1, notification)
    }

    @Test(expected = IllegalStateException::class)
    @MediumTest
    // Mockito tries to class load android.os.CancellationSignal which is only available on API >= 16
    @SdkSuppress(minSdkVersion = 16, maxSdkVersion = 29)
    public fun setForeground_whenWorkReplaced() {
        val foregroundUpdater =
            WorkForegroundUpdater(mDatabase, mForegroundProcessor, mTaskExecutor)
        val uuid = UUID.randomUUID()
        try {
            foregroundUpdater.setForegroundAsync(mContext, uuid, mForegroundInfo).get()
        } catch (exception: Throwable) {
            throw exception.cause ?: exception
        }
    }

    @Test(expected = IllegalStateException::class)
    @MediumTest
    // Mockito tries to class load android.os.CancellationSignal which is only available on API >= 16
    @SdkSuppress(minSdkVersion = 16, maxSdkVersion = 29)
    public fun setForeground_whenWorkFinished() {
        `when`(mWorkSpecDao.getState(anyString())).thenReturn(WorkInfo.State.SUCCEEDED)
        val foregroundUpdater =
            WorkForegroundUpdater(mDatabase, mForegroundProcessor, mTaskExecutor)
        val uuid = UUID.randomUUID()
        try {
            foregroundUpdater.setForegroundAsync(mContext, uuid, mForegroundInfo).get()
        } catch (exception: Throwable) {
            throw exception.cause ?: exception
        }
    }

    @Test
    @MediumTest
    public fun setForeground_throwsException() {
        `when`(mWorkSpecDao.getState(anyString())).thenReturn(WorkInfo.State.RUNNING)
        `when`(mForegroundProcessor.startForeground(anyString(), ArgumentMatchers.any()))
            .thenThrow(IllegalStateException("Subject to foreground service restrictions."))
        val request = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).build()
        val notificationId = 1
        val notification = mock(Notification::class.java)
        val metadata = ForegroundInfo(notificationId, notification)
        val foregroundUpdater =
            WorkForegroundUpdater(mDatabase, mForegroundProcessor, mTaskExecutor)
        var exception: Throwable? = null
        try {
            val future = foregroundUpdater.setForegroundAsync(mContext, request.id, metadata)
            future.get()
        } catch (throwable: Throwable) {
            exception = throwable
        }
        assertNotNull("Exception cannot be null", exception)
    }
}
