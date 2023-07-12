/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.camera2.pipe.integration.impl.ZoomControl
import androidx.camera.camera2.pipe.integration.testing.FakeCameraInfoAdapterCreator.createCameraInfoAdapter
import androidx.camera.camera2.pipe.integration.testing.FakeCameraInfoAdapterCreator.useCaseThreads
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCamera
import androidx.camera.camera2.pipe.integration.testing.FakeZoomCompat
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.ZoomState
import androidx.camera.core.impl.ImageFormatConstants
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraInfoAdapterTest {
    private val zoomControl = ZoomControl(useCaseThreads, FakeZoomCompat())
    private val cameraInfoAdapter = createCameraInfoAdapter(zoomControl = zoomControl)

    @get:Rule
    val dispatcherRule = MainDispatcherRule(MoreExecutors.directExecutor().asCoroutineDispatcher())

    @Test
    fun getSupportedResolutions() {
        // Act.
        val resolutions: List<Size> = cameraInfoAdapter.getSupportedResolutions(
            ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
        )

        // Assert.
        assertThat(resolutions).containsExactly(
            Size(1920, 1080),
            Size(1280, 720),
            Size(640, 480)
        )
    }

    @Test
    fun getSupportedFpsRanges() {
        // Act.
        val ranges: Set<Range<Int>> = cameraInfoAdapter.supportedFrameRateRanges

        // Assert.
        assertThat(ranges).containsExactly(
            Range(12, 30),
            Range(24, 24),
            Range(30, 30),
            Range(60, 60)
        )
    }

    @Test
    fun canReturnIsFocusMeteringSupported() {
        val factory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
        val action = FocusMeteringAction.Builder(
            factory.createPoint(0.5f, 0.5f)
        ).build()

        assertWithMessage("isFocusMeteringSupported() method did not return successfully")
            .that(cameraInfoAdapter.isFocusMeteringSupported(action))
            .isAnyOf(true, false)
    }

    @Test
    fun canReturnDefaultZoomState() {
        // make new ZoomControl to test first-time initialization scenario
        val zoomControl = ZoomControl(useCaseThreads, FakeZoomCompat())
        val cameraInfoAdapter = createCameraInfoAdapter(zoomControl = zoomControl)

        assertWithMessage("zoomState did not return default zoom ratio successfully")
            .that(cameraInfoAdapter.zoomState.value)
            .isEqualTo(zoomControl.defaultZoomState)
    }

    @Test
    fun canObserveZoomStateUpdate(): Unit = runBlocking {
        var currentZoomState: ZoomState = ZoomValue(-1.0f, -1.0f, -1.0f)
        cameraInfoAdapter.zoomState.observeForever {
            currentZoomState = it
        }

        // if useCaseCamera is null, zoom setting operation will be cancelled
        zoomControl.useCaseCamera = FakeUseCaseCamera()

        val expectedZoomState = ZoomValue(3.0f, 1.0f, 10.0f)
        zoomControl.applyZoomState(expectedZoomState)[3, TimeUnit.SECONDS]

        assertWithMessage("zoomState did not return the correct zoom state successfully")
            .that(currentZoomState)
            .isEqualTo(expectedZoomState)
    }

    @Test
    fun canObserveZoomStateReset(): Unit = runBlocking {
        var currentZoomState: ZoomState = ZoomValue(-1.0f, -1.0f, -1.0f)
        cameraInfoAdapter.zoomState.observeForever {
            currentZoomState = it
        }

        // if useCaseCamera is null, zoom setting operation will be cancelled
        zoomControl.useCaseCamera = FakeUseCaseCamera()

        zoomControl.reset()

        // minZoom and maxZoom will be set as 0 due to FakeZoomCompat using those values
        assertWithMessage("zoomState did not return default zoom state successfully")
            .that(currentZoomState)
            .isEqualTo(zoomControl.defaultZoomState)
    }
}
