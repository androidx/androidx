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

import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.impl.CAMERAX_TAG_BUNDLE
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.integration.testing.FakeSurface
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.TagBundle
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import java.util.concurrent.Executors

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class CaptureConfigAdapterTest {

    @Test
    fun shouldFail_whenCaptureConfigHasNoSurfaces() {
        // Arrange
        val captureConfig = CaptureConfig.defaultEmptyCaptureConfig()
        val configAdapter = CaptureConfigAdapter(
            surfaceToStreamMap = emptyMap(),
            callbackExecutor = Executors.newSingleThreadExecutor()
        )
        val sessionConfigOptions = Camera2ImplConfig.Builder().build()

        // Act/Assert
        assertThrows<IllegalStateException> {
            configAdapter.mapToRequest(captureConfig, sessionConfigOptions)
        }
    }

    @Test
    fun shouldFail_whenCaptureConfigSurfaceNotRecognized() {
        // Arrange
        val captureConfig = CaptureConfig.Builder()
            .apply { addSurface(FakeSurface()) }
            .build()
        val configAdapter = CaptureConfigAdapter(
            surfaceToStreamMap = emptyMap(),
            callbackExecutor = Executors.newSingleThreadExecutor()
        )
        val sessionConfigOptions = Camera2ImplConfig.Builder().build()

        // Act/Assert
        assertThrows<IllegalStateException> {
            configAdapter.mapToRequest(captureConfig, sessionConfigOptions)
        }
    }

    @Test
    fun shouldReturnRequestThatIncludesCaptureCallbacks() {
        // Arrange
        val surface = FakeSurface()
        val configAdapter = CaptureConfigAdapter(
            surfaceToStreamMap = mapOf(surface to StreamId(0)),
            callbackExecutor = Executors.newSingleThreadExecutor()
        )

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
        val request = configAdapter.mapToRequest(captureConfig, sessionConfigOptions)
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
        val surface = FakeSurface()
        val configAdapter = CaptureConfigAdapter(
            surfaceToStreamMap = mapOf(surface to StreamId(0)),
            callbackExecutor = Executors.newSingleThreadExecutor()
        )

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
        val request = configAdapter.mapToRequest(captureConfig, sessionConfigOptions)

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
        val surface = FakeSurface()
        val configAdapter = CaptureConfigAdapter(
            surfaceToStreamMap = mapOf(surface to StreamId(0)),
            callbackExecutor = Executors.newSingleThreadExecutor()
        )

        val tagKey = "testTagKey"
        val tagValue = "testTagValue"
        val captureConfig = CaptureConfig.Builder().apply {
            addSurface(surface)
            addTag(tagKey, tagValue)
        }.build()
        val sessionConfigOptions = Camera2ImplConfig.Builder().build()

        // Act
        val request = configAdapter.mapToRequest(captureConfig, sessionConfigOptions)

        // Assert
        assertThat(request.extras).containsKey(CAMERAX_TAG_BUNDLE)
        val tagBundle = request.extras[CAMERAX_TAG_BUNDLE] as TagBundle
        assertThat(tagBundle.getTag(tagKey)).isEqualTo(tagValue)
    }

    @Test
    fun captureOptionsMergeConflict_singleCaptureOptionShouldKeep() {
        // Arrange
        val surface = FakeSurface()
        val configAdapter = CaptureConfigAdapter(
            surfaceToStreamMap = mapOf(surface to StreamId(0)),
            callbackExecutor = Executors.newSingleThreadExecutor()
        )

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
        val request = configAdapter.mapToRequest(captureConfig, sessionConfigOptions)

        // Assert, the options of the single capture should have higher priority.
        val rotation = request.parameters[CaptureRequest.JPEG_ORIENTATION]
        assertThat(rotation).isEqualTo(90)
    }
}