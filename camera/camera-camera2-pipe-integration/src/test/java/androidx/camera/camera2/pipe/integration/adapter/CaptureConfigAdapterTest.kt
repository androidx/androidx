/*
 * Copyright 2021 The Android Open Source Project
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

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.impl.CAMERAX_TAG_BUNDLE
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraph
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeSurface
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.TagBundle
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class CaptureConfigAdapterTest {
    private val fakeUseCaseThreads by lazy {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val cameraScope = CoroutineScope(Job() + dispatcher)

        UseCaseThreads(
            cameraScope,
            executor,
            dispatcher,
        )
    }
    private val fakeCameraProperties = FakeCameraProperties()
    private val surface = FakeSurface()
    private val configAdapter = CaptureConfigAdapter(
        useCaseGraphConfig = UseCaseGraphConfig(
            graph = FakeCameraGraph(),
            surfaceToStreamMap = mapOf(surface to StreamId(0)),
            cameraStateAdapter = CameraStateAdapter(),
        ),
        cameraProperties = fakeCameraProperties,
        threads = fakeUseCaseThreads,
    )

    @After
    fun tearDown() {
        surface.close()
    }

    @Test
    fun shouldFail_whenCaptureConfigHasNoSurfaces() {
        // Arrange
        val captureConfig = CaptureConfig.defaultEmptyCaptureConfig()
        val sessionConfigOptions = Camera2ImplConfig.Builder().build()

        // Act/Assert
        assertThrows<IllegalStateException> {
            configAdapter.mapToRequest(
                captureConfig,
                RequestTemplate(CameraDevice.TEMPLATE_PREVIEW),
                sessionConfigOptions
            )
        }
    }

    @Test
    fun shouldFail_whenCaptureConfigSurfaceNotRecognized() {
        // Arrange
        val fakeSurface = FakeSurface()
        val captureConfig = CaptureConfig.Builder()
            .apply { addSurface(fakeSurface) }
            .build()
        val sessionConfigOptions = Camera2ImplConfig.Builder().build()

        // Act/Assert
        assertThrows<IllegalStateException> {
            configAdapter.mapToRequest(
                captureConfig,
                RequestTemplate(CameraDevice.TEMPLATE_PREVIEW),
                sessionConfigOptions
            )
        }

        // Clean up
        fakeSurface.close()
    }

    @Test
    fun shouldReturnRequestThatIncludesCaptureCallbacks() {
        // Arrange
        val callbackAborted = CompletableDeferred<Unit>()
        val captureCallback = object : CameraCaptureCallback() {
            override fun onCaptureCancelled() {
                callbackAborted.complete(Unit)
            }
        }
        val captureConfig = CaptureConfig.Builder()
            .apply {
                addSurface(surface)
                addCameraCaptureCallback(captureCallback)
            }
            .build()
        val sessionConfigOptions = Camera2ImplConfig.Builder().build()

        // Act
        val request = configAdapter.mapToRequest(
            captureConfig,
            RequestTemplate(CameraDevice.TEMPLATE_PREVIEW),
            sessionConfigOptions
        )
        request.listeners.forEach { listener ->
            listener.onAborted(request)
        }

        // Assert
        runBlocking {
            callbackAborted.await()
        }
    }

    @Test
    fun shouldReturnRequestThatIncludesCaptureOptions() {
        // Arrange
        val captureConfig = CaptureConfig.Builder()
            .apply {
                addSurface(surface)
                addImplementationOption(CaptureConfig.OPTION_ROTATION, 90)
                addImplementationOption(CaptureConfig.OPTION_JPEG_QUALITY, 100)
            }
            .build()
        val sessionConfigOptions = Camera2ImplConfig.Builder().apply {
            setCaptureRequestOption(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
        }.build()

        // Act
        val request = configAdapter.mapToRequest(
            captureConfig,
            RequestTemplate(CameraDevice.TEMPLATE_PREVIEW),
            sessionConfigOptions
        )

        // Assert
        val rotation = request.parameters[CaptureRequest.JPEG_ORIENTATION]
        assertThat(rotation).isEqualTo(90)
        val quality = request.parameters[CaptureRequest.JPEG_QUALITY]
        assertThat(quality).isEqualTo(100)
        val flashMode = request.parameters[CaptureRequest.FLASH_MODE]
        assertThat(flashMode).isEqualTo(CaptureRequest.FLASH_MODE_OFF)
    }

    @Test
    fun shouldSetTagBundleToTheRequest() {
        // Arrange
        val tagKey = "testTagKey"
        val tagValue = "testTagValue"
        val captureConfig = CaptureConfig.Builder().apply {
            addSurface(surface)
            addTag(tagKey, tagValue)
        }.build()
        val sessionConfigOptions = Camera2ImplConfig.Builder().build()

        // Act
        val request = configAdapter.mapToRequest(
            captureConfig,
            RequestTemplate(CameraDevice.TEMPLATE_PREVIEW),
            sessionConfigOptions
        )

        // Assert
        assertThat(request.extras).containsKey(CAMERAX_TAG_BUNDLE)
        val tagBundle = request.extras[CAMERAX_TAG_BUNDLE] as TagBundle
        assertThat(tagBundle.getTag(tagKey)).isEqualTo(tagValue)
    }

    @Test
    fun captureOptionsMergeConflict_singleCaptureOptionShouldKeep() {
        // Arrange
        val captureConfig = CaptureConfig.Builder()
            .apply {
                addSurface(surface)
                addImplementationOption(CaptureConfig.OPTION_ROTATION, 90)
            }
            .build()

        // Create a session config that includes an option that conflicts with the capture config.
        val sessionConfigOptions = Camera2ImplConfig.Builder().apply {
            setCaptureRequestOption(CaptureRequest.JPEG_ORIENTATION, 100)
        }.build()

        // Act
        val request = configAdapter.mapToRequest(
            captureConfig,
            RequestTemplate(CameraDevice.TEMPLATE_PREVIEW),
            sessionConfigOptions
        )

        // Assert, the options of the single capture should have higher priority.
        val rotation = request.parameters[CaptureRequest.JPEG_ORIENTATION]
        assertThat(rotation).isEqualTo(90)
    }

    @Test
    fun submitStillCaptureRequests_withTemplate_templateSent(): Unit = runBlocking {
        // Arrange.
        val imageCaptureConfig = CaptureConfig.Builder().let {
            it.addSurface(surface)
            it.templateType = CameraDevice.TEMPLATE_MANUAL
            it.build()
        }
        val request = configAdapter.mapToRequest(
            imageCaptureConfig,
            RequestTemplate(CameraDevice.TEMPLATE_PREVIEW),
            Camera2ImplConfig.Builder().build(),
        )

        // Assert.
        val template = request.template
        assertThat(template).isEqualTo(RequestTemplate(CameraDevice.TEMPLATE_MANUAL))
    }

    @Test
    fun submitStillCaptureRequests_withNoTemplate_templateStillCaptureSent(): Unit = runBlocking {
        // Arrange.
        val imageCaptureConfig = CaptureConfig.Builder().apply {
            addSurface(surface)
        }.build()

        // Act.
        val request = configAdapter.mapToRequest(
            imageCaptureConfig,
            RequestTemplate(CameraDevice.TEMPLATE_PREVIEW),
            Camera2ImplConfig.Builder().build(),
        )

        // Assert.
        val template = request.template
        assertThat(template).isEqualTo(RequestTemplate(CameraDevice.TEMPLATE_STILL_CAPTURE))
    }

    @Test
    fun submitStillCaptureRequests_withTemplateRecord_templateVideoSnapshotSent(): Unit =
        runBlocking {
            // Arrange.
            val imageCaptureConfig = CaptureConfig.Builder().apply {
                templateType = CameraDevice.TEMPLATE_STILL_CAPTURE
                addSurface(surface)
            }.build()

            // Act.
            val request = configAdapter.mapToRequest(
                imageCaptureConfig,
                // With session template in TEMPLATE_RECORD
                RequestTemplate(CameraDevice.TEMPLATE_RECORD),
                Camera2ImplConfig.Builder().build(),
            )

            // Assert.
            val template = request.template
            assertThat(template).isEqualTo(RequestTemplate(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT))
        }
}
