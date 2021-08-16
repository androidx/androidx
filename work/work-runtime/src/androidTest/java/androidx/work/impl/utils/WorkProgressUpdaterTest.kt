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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.impl.WorkDatabase
import androidx.work.impl.model.WorkSpecDao
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.UUID

@RunWith(AndroidJUnit4::class)
// Mockito tries to class load android.os.CancellationSignal which is only available on API >= 16
@SdkSuppress(minSdkVersion = 16)
class WorkProgressUpdaterTest {
    private lateinit var mContext: Context
    private lateinit var mDatabase: WorkDatabase
    private lateinit var mWorkSpecDao: WorkSpecDao
    private lateinit var mTaskExecutor: TaskExecutor
    private lateinit var mProgressUpdater: WorkProgressUpdater

    @Before
    fun setUp() {
        mContext = Mockito.mock(Context::class.java)
        mDatabase = Mockito.mock(WorkDatabase::class.java)
        mWorkSpecDao = Mockito.mock(WorkSpecDao::class.java)
        `when`(mDatabase.workSpecDao()).thenReturn(mWorkSpecDao)
        mTaskExecutor = InstantWorkTaskExecutor()
        mProgressUpdater = WorkProgressUpdater(mDatabase, mTaskExecutor)
    }

    @Test(expected = IllegalStateException::class)
    @MediumTest
    fun updateProgress_whenWorkFinished() {
        `when`(mWorkSpecDao.getState(anyString())).thenReturn(WorkInfo.State.SUCCEEDED)
        val uuid = UUID.randomUUID()
        try {
            val data = Data.Builder().build()
            mProgressUpdater.updateProgress(mContext, uuid, data).get()
        } catch (exception: Throwable) {
            throw exception.cause ?: exception
        }
    }
}
