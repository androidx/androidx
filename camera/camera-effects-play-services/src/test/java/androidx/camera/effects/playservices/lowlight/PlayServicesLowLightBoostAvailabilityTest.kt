/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.effects.playservices.lowlight

import android.content.Context
import android.os.Build
import android.os.Looper.getMainLooper
import androidx.camera.core.CameraInfo
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PlayServicesLowLightBoostAvailabilityTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val camera0 = FakeCameraInfoInternal("0")
    private val client = FakeLowLightBoostClient()

    @Before
    fun setUp() {
        PlayServicesLowLightBoostAvailability.reset()
        PlayServicesLowLightBoostAvailability.clientFactoryForTesting = { client }
    }

    @After
    fun tearDown() {
        PlayServicesLowLightBoostAvailability.reset()
    }

    @Test
    fun supported_isCachedAndNotQueriedAgain() {
        // Act.
        val first = isSupported(camera0)
        val second = PlayServicesLowLightBoostAvailability.isSupportedAsync(context, camera0)

        // Assert.
        assertThat(first).isTrue()
        assertThat(second.isDone).isTrue()
        assertThat(second.get()).isTrue()
        assertThat(client.deviceSupportedCount).isEqualTo(1)
        assertThat(client.cameraSupportedCount).isEqualTo(1)
        assertThat(PlayServicesLowLightBoostAvailability.getCachedDeviceSupported()).isTrue()
        assertThat(PlayServicesLowLightBoostAvailability.getCachedCameraSupported("0")).isTrue()
    }

    @Test
    fun deviceUnsupported_doesNotQueryCamera() {
        // Arrange.
        client.deviceSupportedTask = { Tasks.forResult(false) }

        // Act & Assert.
        assertThat(isSupported(camera0)).isFalse()
        assertThat(client.cameraSupportedCount).isEqualTo(0)
        assertThat(PlayServicesLowLightBoostAvailability.getCachedDeviceSupported()).isFalse()
    }

    @Test
    fun cameraSupport_isCachedPerCamera() {
        // Arrange.
        client.cameraSupportedTask = { cameraId -> Tasks.forResult(cameraId == "0") }

        // Act & Assert.
        assertThat(isSupported(camera0)).isTrue()
        assertThat(isSupported(FakeCameraInfoInternal("1"))).isFalse()
        assertThat(PlayServicesLowLightBoostAvailability.getCachedCameraSupported("0")).isTrue()
        assertThat(PlayServicesLowLightBoostAvailability.getCachedCameraSupported("1")).isFalse()
    }

    @Test
    fun failedQuery_resolvesToFalseAndIsNotCached() {
        // Arrange.
        client.deviceSupportedTask = { Tasks.forException(IllegalStateException("Failed")) }

        // Act.
        val first = isSupported(camera0)
        client.deviceSupportedTask = { Tasks.forResult(true) }
        val second = isSupported(camera0)

        // Assert: the failure isn't cached, so the next query tries again.
        assertThat(first).isFalse()
        assertThat(second).isTrue()
        assertThat(client.deviceSupportedCount).isEqualTo(2)
    }

    @Test
    fun queryThrows_resolvesToFalse() {
        // Arrange.
        client.deviceSupportedTask = { throw IllegalStateException("Failed") }

        // Act & Assert.
        assertThat(isSupported(camera0)).isFalse()
        assertThat(PlayServicesLowLightBoostAvailability.getCachedDeviceSupported()).isNull()
    }

    @Test
    fun timeout_resolvesToFalseAndLateResultIsCached() {
        // Arrange: a device query that doesn't complete in time.
        val pendingResult = TaskCompletionSource<Boolean>()
        client.deviceSupportedTask = { pendingResult.task }
        PlayServicesLowLightBoostAvailability.timeoutMs = 100L

        // Act: the timeout fires on a background thread, so it doesn't need the main thread.
        val future = PlayServicesLowLightBoostAvailability.isSupportedAsync(context, camera0)

        // Assert: the timeout resolves to false, and a later result is still cached.
        assertThat(future.get(5, TimeUnit.SECONDS)).isFalse()
        pendingResult.setResult(true)
        assertThat(PlayServicesLowLightBoostAvailability.getCachedDeviceSupported()).isTrue()
    }

    @Test
    fun cachedResult_isUsedWithoutQuery() {
        // Arrange.
        PlayServicesLowLightBoostAvailability.setDeviceSupported(true)
        PlayServicesLowLightBoostAvailability.setCameraSupported("0", false)

        // Act & Assert.
        assertThat(isSupported(camera0)).isFalse()
        assertThat(client.deviceSupportedCount).isEqualTo(0)
        assertThat(client.cameraSupportedCount).isEqualTo(0)
    }

    @Test
    fun nonInternalCameraInfo_isNotSupported() {
        // Arrange.
        val cameraInfo = object : CameraInfo by camera0 {}

        // Act & Assert.
        assertThat(isSupported(cameraInfo)).isFalse()
        assertThat(client.deviceSupportedCount).isEqualTo(0)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun belowApi30_isNotSupportedWithoutQuery() {
        // Act & Assert.
        assertThat(isSupported(camera0)).isFalse()
        assertThat(client.deviceSupportedCount).isEqualTo(0)
    }

    private fun isSupported(cameraInfo: CameraInfo): Boolean {
        val future: ListenableFuture<Boolean> =
            PlayServicesLowLightBoostAvailability.isSupportedAsync(context, cameraInfo)
        shadowOf(getMainLooper()).idle()
        assertThat(future.isDone).isTrue()
        return future.get()
    }
}
