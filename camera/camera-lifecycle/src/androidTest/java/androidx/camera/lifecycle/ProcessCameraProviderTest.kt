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
import androidx.annotation.experimental.UseExperimental
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

@SmallTest
class ProcessCameraProviderTest {

    private val context = ApplicationProvider.getApplicationContext() as android.content.Context
    private val lifecycleOwner0 = FakeLifecycleOwner()
    private val lifecycleOwner1 = FakeLifecycleOwner()

    private var provider: ProcessCameraProvider? = null

    @After
    fun tearDown() {
        runBlocking {
            try {
                val provider = ProcessCameraProvider.getInstance(context).await()
                provider.shutdown().await()
            } catch (e: IllegalStateException) {
                // ProcessCameraProvider may not be configured. Ignore.
            }
        }
    }

    @Test
    fun uninitializedGetInstance_throwsISE() {
        runBlocking {
            assertThrows<IllegalStateException> {
                ProcessCameraProvider.getInstance(context).await()
            }
        }
    }

    @UseExperimental(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun multipleConfigureInstance_throwsISE() {
        val config = FakeAppConfig.create()
        ProcessCameraProvider.configureInstance(config)
        assertThrows<IllegalStateException> {
            ProcessCameraProvider.configureInstance(config)
        }
    }

    @UseExperimental(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun configuredGetInstance_returnsProvider() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking {
            provider = ProcessCameraProvider.getInstance(context).await()
            assertThat(provider).isNotNull()
        }
    }

    @UseExperimental(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun configuredGetInstance_usesConfiguredExecutor() {
        var executeCalled = false
        val config =
            CameraXConfig.Builder.fromConfig(FakeAppConfig.create()).setCameraExecutor { runnable ->
                run {
                    executeCalled = true
                    Dispatchers.Default.asExecutor().execute(runnable)
                }
            }.build()
        ProcessCameraProvider.configureInstance(config)
        runBlocking {
            ProcessCameraProvider.getInstance(context).await()
            assertThat(executeCalled).isTrue()
        }
    }

    @UseExperimental(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun canRetrieveCamera_withZeroUseCases() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val camera =
                provider!!.bindToLifecycle(lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA)
            assertThat(camera).isNotNull()
        }
    }

    @Test
    fun bindUseCase_isBound() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            provider!!.bindToLifecycle(
                lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA,
                useCase
            )

            assertThat(provider!!.isBound(useCase)).isTrue()
        }
    }

    @Test
    fun bindSecondUseCaseToDifferentLifecycle_firstUseCaseStillBound() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val useCase1 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            provider!!.bindToLifecycle(
                lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA,
                useCase0
            )
            provider!!.bindToLifecycle(
                lifecycleOwner1, CameraSelector.DEFAULT_BACK_CAMERA,
                useCase1
            )

            // TODO(b/158595693) Add check on whether or not camera for fakeUseCase0 should be
            //  exist or not
            // assertThat(fakeUseCase0.camera).isNotNull() (or isNull()?)
            assertThat(provider!!.isBound(useCase0)).isTrue()
            assertThat(useCase1.camera).isNotNull()
            assertThat(provider!!.isBound(useCase1)).isTrue()
        }
    }

    @Test
    fun isNotBound_afterUnbind() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            
            provider!!.bindToLifecycle(
                lifecycleOwner0,
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCase
            )

            provider!!.unbind(useCase)

            assertThat(provider!!.isBound(useCase)).isFalse()
        }

    }
}