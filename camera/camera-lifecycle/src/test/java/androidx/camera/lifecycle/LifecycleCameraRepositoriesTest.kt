/*
 * Copyright 2025 The Android Open Source Project
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

import android.content.Context
import android.os.Build
import androidx.camera.core.LegacySessionConfig
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.StreamSpecsCalculatorImpl
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class LifecycleCameraRepositoriesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        // Clear the singleton's state after each test to ensure test isolation.
        LifecycleCameraRepositories.clear()
    }

    @Test
    fun getInstance_withoutDeviceId_ReturnsSameDefaultInstance() {
        // Act.
        val instance1 = LifecycleCameraRepositories.getInstance()
        val instance2 = LifecycleCameraRepositories.getInstance()

        // Assert.
        assertThat(instance1).isNotNull()
        assertThat(instance1).isSameInstanceAs(instance2)
    }

    @Test
    fun getInstance_withSameDeviceId_returnsSameInstance() {
        // Arrange.
        val deviceId = 1

        // Act: Create two instances with the same device ID.
        val instance1 = LifecycleCameraRepositories.getInstance(deviceId)
        val instance2 = LifecycleCameraRepositories.getInstance(deviceId)

        // Assert.
        assertThat(instance1).isSameInstanceAs(instance2)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun getInstance_withDifferentDeviceIds_returnsDifferentInstances() {
        // Act.
        val instance1 = LifecycleCameraRepositories.getInstance(1)
        val instance2 = LifecycleCameraRepositories.getInstance(2)
        val defaultInstance = LifecycleCameraRepositories.getInstance()

        // Assert.
        assertThat(instance1).isNotSameInstanceAs(instance2)
        assertThat(instance1).isNotSameInstanceAs(defaultInstance)
        assertThat(instance2).isNotSameInstanceAs(defaultInstance)
    }

    @Test
    fun getInstance_isThreadSafe() {
        // Arrange.
        val threadCount = 20
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val results = Collections.synchronizedList(mutableListOf<LifecycleCameraRepository>())

        // Act.
        repeat(threadCount) {
            executor.submit {
                // All threads request the default repository instance.
                val repo = LifecycleCameraRepositories.getInstance()
                results.add(repo)
                latch.countDown()
            }
        }

        // Wait for all threads to complete.
        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        // Assert.
        assertThat(results).hasSize(threadCount)
        // Get the first instance as the reference.
        val firstInstance = results.first()
        // All other instances in the list should be the exact same object.
        results.forEach { assertThat(it).isSameInstanceAs(firstInstance) }
    }

    @Test
    fun lifecycleCamerasActive_bindToRepositoriesWithDifferentDeviceId() {
        // Act.
        val repository0 = LifecycleCameraRepositories.getInstance(0)
        val lifecycle0 = FakeLifecycleOwner()
        val lifecycleCamera0 = repository0.createFakeLifecycleCamera(lifecycle0)
        lifecycle0.start()
        repository0.lazyBindToLifecycleCamera(lifecycleCamera0)

        // Create the second repository with a different device ID.
        val repository1 = LifecycleCameraRepositories.getInstance(1)
        val lifecycle1 = FakeLifecycleOwner()
        val lifecycleCamera1 = repository1.createFakeLifecycleCamera(lifecycle1)
        lifecycle1.start()
        repository1.lazyBindToLifecycleCamera(lifecycleCamera1)

        // Assert: Both lifecycle camera should be active.
        assertThat(lifecycleCamera0.isActive).isTrue()
        assertThat(lifecycleCamera1.isActive).isTrue()
    }

    @Test
    fun lifecycleCameraOf1stActiveLifecycleInactive_bindToRepositoriesWithSameDeviceId() {
        // Arrange.
        val deviceId = 1

        // Act.
        val repository0 = LifecycleCameraRepositories.getInstance(deviceId)
        val lifecycle0 = FakeLifecycleOwner()
        val lifecycleCamera0 = repository0.createFakeLifecycleCamera(lifecycle0)
        lifecycle0.start()
        repository0.lazyBindToLifecycleCamera(lifecycleCamera0)

        // Create the second repository with the same device ID.
        val repository1 = LifecycleCameraRepositories.getInstance(deviceId)
        val lifecycle1 = FakeLifecycleOwner()
        val lifecycleCamera1 = repository1.createFakeLifecycleCamera(lifecycle1)
        lifecycle1.start()
        repository1.lazyBindToLifecycleCamera(lifecycleCamera1)

        // Assert.
        // The previous LifecycleCamera becomes inactive after new LifecycleCamera becomes active.
        assertThat(lifecycleCamera0.isActive).isFalse()
        // New LifecycleCamera becomes active after binding use case to it.
        assertThat(lifecycleCamera1.isActive).isTrue()
    }

    private fun LifecycleCameraRepository.createFakeLifecycleCamera(
        lifecycle: LifecycleOwner
    ): LifecycleCamera =
        createLifecycleCamera(
            lifecycle,
            CameraUseCaseAdapter(
                FakeCamera(),
                FakeCameraCoordinator(),
                StreamSpecsCalculatorImpl(
                    FakeUseCaseConfigFactory(),
                    FakeCameraDeviceSurfaceManager(),
                ),
                FakeUseCaseConfigFactory(),
            ),
        )

    private fun LifecycleCameraRepository.lazyBindToLifecycleCamera(
        lifecycleCamera: LifecycleCamera
    ) =
        bindToLifecycleCamera(
            lifecycleCamera,
            LegacySessionConfig(listOf(FakeUseCase()), null, emptyList()),
            FakeCameraCoordinator(),
        )
}
