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

package androidx.camera.camera2.pipe.integration.interop

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.compat.Camera2CameraControlCompatImpl
import androidx.camera.camera2.pipe.integration.impl.CAMERAX_TAG_BUNDLE
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.impl.toParameters
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraRequestControl
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.MutableTagBundle
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalCamera2Interop::class)
class Camera2CameraControlTest {

    private val fakeUseCaseThreads by lazy {
        val executor = MoreExecutors.directExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val cameraScope = CoroutineScope(Job() + dispatcher)

        UseCaseThreads(
            cameraScope,
            executor,
            dispatcher,
        )
    }
    private val comboRequestListener = ComboRequestListener()
    private val fakeRequestControl = FakeUseCaseCameraRequestControl()
    private val camera2CameraControlCompatImpl = Camera2CameraControlCompatImpl()
    private lateinit var camera2CameraControl: Camera2CameraControl

    @Before
    fun setUp() {
        camera2CameraControl =
            Camera2CameraControl.create(
                compat = camera2CameraControlCompatImpl,
                threads = fakeUseCaseThreads,
                requestListener = comboRequestListener,
            )
        camera2CameraControl.requestControl = fakeRequestControl
    }

    @Test
    fun useCaseCameraUpdated_setRequestOptionResultShouldPropagate(): Unit = runBlocking {
        // Arrange.
        val completeDeferred = CompletableDeferred<Unit>()
        val fakeRequestControl =
            FakeUseCaseCameraRequestControl().apply { setConfigResult = completeDeferred }

        val resultFuture =
            camera2CameraControl.setCaptureRequestOptions(
                CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_OFF
                    )
                    .build()
            )

        // Act. Simulate the UseCaseCamera is recreated.
        camera2CameraControl.requestControl = fakeRequestControl
        // Simulate setRequestOption is completed in the recreated UseCaseCamera
        completeDeferred.complete(Unit)
        val requestsToCamera =
            fakeRequestControl.setConfigCalls
                .filter { it.type == UseCaseCameraRequestControl.Type.CAMERA2_CAMERA_CONTROL }
                .onEach { request ->
                    comboRequestListener.simulateRepeatingResult(
                        requests = request.config?.toParameters() ?: emptyMap(),
                        tags = request.tags,
                    )
                }

        // Assert. The setRequestOption task should be completed.
        assertThat(requestsToCamera).isNotEmpty()
        assertThat(resultFuture.get(3, TimeUnit.SECONDS)).isNull()
    }

    @Test
    fun useCaseCameraUpdated_onlyCompleteLatestRequest(): Unit = runBlocking {
        // Arrange.
        val completeDeferred = CompletableDeferred<Unit>()
        val fakeRequestControl =
            FakeUseCaseCameraRequestControl().apply { setConfigResult = completeDeferred }

        val resultFuture =
            camera2CameraControl.setCaptureRequestOptions(
                CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_OFF
                    )
                    .build()
            )

        // Act. Simulate the UseCaseCamera is recreated.
        camera2CameraControl.requestControl = fakeRequestControl
        // Act. Submit a new request option.
        val resultFuture2 =
            camera2CameraControl.setCaptureRequestOptions(
                CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                    )
                    .build()
            )
        // Simulate setRequestOption is completed in the recreated UseCaseCamera
        completeDeferred.complete(Unit)
        val requestsToCamera =
            fakeRequestControl.setConfigCalls
                .filter { it.type == UseCaseCameraRequestControl.Type.CAMERA2_CAMERA_CONTROL }
                .onEach { request ->
                    comboRequestListener.simulateRepeatingResult(
                        requests = request.config?.toParameters() ?: emptyMap(),
                        tags = request.tags,
                    )
                }

        // Assert. The first request should be cancelled., the latest setRequestOption
        // task should be completed.
        assertThat(requestsToCamera).isNotEmpty()
        assertThrows<ExecutionException> { resultFuture.get(3, TimeUnit.SECONDS) }
        assertThat(resultFuture2.get(3, TimeUnit.SECONDS)).isNull()
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromCameraControlThrows_whenNotCamera2Impl() {
        val wrongCameraControl = CameraControlInternal.DEFAULT_EMPTY_INSTANCE
        Camera2CameraControl.from(wrongCameraControl)
    }

    private fun ComboRequestListener.simulateRepeatingResult(
        requests: Map<CaptureRequest.Key<*>, Any> = emptyMap(),
        tags: Map<String, Any> = emptyMap(),
        results: Map<CaptureResult.Key<*>, Any> = emptyMap(),
        frameNumber: FrameNumber = FrameNumber(101L),
    ) {
        val requestMetadata =
            FakeRequestMetadata(
                requestParameters = requests,
                metadata =
                    mapOf(
                        CAMERAX_TAG_BUNDLE to
                            MutableTagBundle.create().also { tagBundle ->
                                tags.forEach { (tagKey, tagValue) ->
                                    tagBundle.putTag(tagKey, tagValue)
                                }
                            }
                    ),
                requestNumber = RequestNumber(1)
            )
        val resultMetaData =
            FakeFrameMetadata(
                resultMetadata = results,
                frameNumber = frameNumber,
            )
        fakeUseCaseThreads.sequentialExecutor.execute {
            onComplete(
                requestMetadata,
                frameNumber,
                FakeFrameInfo(
                    metadata = resultMetaData,
                    requestMetadata = requestMetadata,
                )
            )
        }
    }
}
