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

package androidx.camera.core

import android.content.Context
import android.os.Build
import android.os.Looper
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.junit.Assert

@RunWith(RobolectricTestRunner::class)
@Suppress("DEPRECATION")
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP
)
class VideoCaptureTest {
    @Before
    fun setUp() {
        val camera = FakeCamera()

        val cameraFactoryProvider =
            CameraFactory.Provider { _, _, _ ->
                val cameraFactory = FakeCameraFactory()
                cameraFactory.insertDefaultBackCamera(camera.cameraInfoInternal.cameraId) {
                    camera
                }
                cameraFactory
            }
        val cameraXConfig = CameraXConfig.Builder.fromConfig(FakeAppConfig.create())
            .setCameraFactoryProvider(cameraFactoryProvider)
            .build()
        val context = ApplicationProvider.getApplicationContext<Context>()
        CameraXUtil.initialize(context, cameraXConfig).get()
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown().get()
    }

    @Test
    fun startRecording_beforeUseCaseIsBound() {
        val videoCapture = VideoCapture.Builder().build()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val outputFileOptions = VideoCapture.OutputFileOptions.Builder(file).build()
        val callback = mock(VideoCapture.OnVideoSavedCallback::class.java)
        videoCapture.startRecording(
            outputFileOptions,
            CameraXExecutors.mainThreadExecutor(),
            callback
        )
        shadowOf(Looper.getMainLooper()).idle()

        verify(callback).onError(eq(VideoCapture.ERROR_INVALID_CAMERA), anyString(), any())
    }

    @Test
    fun throwException_whenSetBothTargetResolutionAndAspectRatio() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            VideoCapture.Builder().setTargetResolution(SizeUtil.RESOLUTION_VGA)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
        }
    }

    @Test
    fun throwException_whenSetTargetResolutionWithResolutionSelector() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            VideoCapture.Builder().setTargetResolution(SizeUtil.RESOLUTION_VGA)
                .setResolutionSelector(ResolutionSelector.Builder().build())
                .build()
        }
    }

    @Test
    fun throwException_whenSetTargetAspectRatioWithResolutionSelector() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            VideoCapture.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setResolutionSelector(ResolutionSelector.Builder().build())
                .build()
        }
    }
}