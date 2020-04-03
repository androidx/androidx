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

package androidx.camera.lifecycle

import androidx.camera.core.CameraSelector
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
import org.junit.Test

@SmallTest
class ProcessCameraProviderTest {

    private val context = ApplicationProvider.getApplicationContext() as android.content.Context
    private val lifecycleOwner = FakeLifecycleOwner()

    @Test
    fun uninitializedGetInstance_throwsISE() {
        runBlocking {
            assertThrows<IllegalStateException> {
                ProcessCameraProvider.getInstance(context).await()
            }
        }
    }

    @Test
    fun initializedGetInstance_returnsProvider() {
        ProcessCameraProvider.initializeInstance(context, FakeAppConfig.create())
        runBlocking {
            val provider = ProcessCameraProvider.getInstance(context).await()
            assertThat(provider).isNotNull()
            provider.shutdown().await()
        }
    }

    @Test
    fun canRetrieveCamera_withZeroUseCases() {
        ProcessCameraProvider.initializeInstance(context, FakeAppConfig.create())
        runBlocking(MainScope().coroutineContext) {
            val provider = ProcessCameraProvider.getInstance(context).await()
            val camera =
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA)
            assertThat(camera).isNotNull()
            provider.shutdown().await()
        }
    }
}