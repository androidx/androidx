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

package androidx.work.impl

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.Configuration
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.concurrent.Executor

@RunWith(AndroidJUnit4::class)
class WorkManagerInitializationTest {
    private lateinit var mContext: Context
    private lateinit var mConfiguration: Configuration
    private lateinit var mExecutor: Executor
    private lateinit var mTaskExecutor: TaskExecutor

    @Before
    fun setUp() {
        mContext = mock(Context::class.java)
        `when`(mContext.applicationContext).thenReturn(mContext)
        mExecutor = mock(Executor::class.java)
        mConfiguration = Configuration.Builder()
            .setExecutor(mExecutor)
            .setTaskExecutor(mExecutor)
            .build()
        mTaskExecutor = mock(TaskExecutor::class.java)
    }

    @Test(expected = IllegalStateException::class)
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    fun directBootTest() {
        `when`(mContext.isDeviceProtectedStorage).thenReturn(true)
        WorkManagerImpl(mContext, mConfiguration, mTaskExecutor, true)
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    fun credentialBackedStorageTest() {
        `when`(mContext.isDeviceProtectedStorage).thenReturn(false)
        val workManager = WorkManagerImpl(mContext, mConfiguration, mTaskExecutor, true)
        assertNotNull(workManager)
    }
}
