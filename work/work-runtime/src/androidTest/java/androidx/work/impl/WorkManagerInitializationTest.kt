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

import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.work.Configuration
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkManagerInitializationTest {
    private val executor = SynchronousExecutor()
    private val configuration = Configuration.Builder()
        .setExecutor(executor)
        .setTaskExecutor(executor)
        .build()
    private val taskExecutor = InstantWorkTaskExecutor()

    @Test(expected = IllegalStateException::class)
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    fun directBootTest() {
        val context = DeviceProtectedStoreContext(true)
        TestWorkManagerImpl(context, configuration, taskExecutor)
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    fun credentialBackedStorageTest() {
        val context = DeviceProtectedStoreContext(false)
        val workManager = TestWorkManagerImpl(context, configuration, taskExecutor)
        assertNotNull(workManager)
    }
}

private class DeviceProtectedStoreContext(
    val deviceProtectedStorage: Boolean
) : ContextWrapper(ApplicationProvider.getApplicationContext()) {
    override fun isDeviceProtectedStorage() = deviceProtectedStorage

    override fun getApplicationContext() = this
}
