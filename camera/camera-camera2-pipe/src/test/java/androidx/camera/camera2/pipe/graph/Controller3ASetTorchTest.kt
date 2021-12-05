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

package androidx.camera.camera2.pipe.graph

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.TorchState
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestProcessor
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class Controller3ASetTorchTest {
    private val graphState3A = GraphState3A()
    private val graphProcessor = FakeGraphProcessor(graphState3A = graphState3A)
    private val requestProcessor = FakeRequestProcessor()
    private val listener3A = Listener3A()
    private val controller3A = Controller3A(
        graphProcessor,
        FakeCameraMetadata(),
        graphState3A,
        listener3A
    )

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testSetTorchOn() = runBlocking {
        initGraphProcessor()

        val result = controller3A.setTorch(TorchState.ON)
        assertThat(graphState3A.aeMode!!.value).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
        assertThat(graphState3A.flashMode!!.value).isEqualTo(CaptureRequest.FLASH_MODE_TORCH)
        assertThat(result.isCompleted).isFalse()

        GlobalScope.launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AE_MODE to CaptureResult.CONTROL_AE_MODE_ON,
                        CaptureResult.FLASH_MODE to CaptureResult.FLASH_MODE_TORCH
                    )
                )
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testSetTorchOff() = runBlocking {
        initGraphProcessor()

        val result = controller3A.setTorch(TorchState.OFF)
        assertThat(graphState3A.aeMode!!.value).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
        assertThat(graphState3A.flashMode!!.value).isEqualTo(CaptureRequest.FLASH_MODE_OFF)
        assertThat(result.isCompleted).isFalse()

        GlobalScope.launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AE_MODE to CaptureResult.CONTROL_AE_MODE_ON,
                        CaptureResult.FLASH_MODE to CaptureResult.FLASH_MODE_OFF
                    )
                )
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testSetTorchDoesNotChangeAeModeIfNotNeeded() = runBlocking {
        initGraphProcessor()

        graphState3A.update(aeMode = AeMode.OFF)

        val result = controller3A.setTorch(TorchState.ON)
        assertThat(graphState3A.aeMode!!.value).isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
        assertThat(graphState3A.flashMode!!.value).isEqualTo(
            CaptureRequest.FLASH_MODE_TORCH
        )
        assertThat(result.isCompleted).isFalse()

        GlobalScope.launch {
            listener3A.onRequestSequenceCreated(
                FakeRequestMetadata(
                    requestNumber = RequestNumber(1)
                )
            )
            listener3A.onPartialCaptureResult(
                FakeRequestMetadata(requestNumber = RequestNumber(1)),
                FrameNumber(101L),
                FakeFrameMetadata(
                    frameNumber = FrameNumber(101L),
                    resultMetadata = mapOf(
                        CaptureResult.CONTROL_AE_MODE to CaptureResult.CONTROL_AE_MODE_OFF,
                        CaptureResult.FLASH_MODE to CaptureResult.FLASH_MODE_TORCH
                    )
                )
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    private fun initGraphProcessor() {
        graphProcessor.onGraphStarted(requestProcessor)
        graphProcessor.startRepeating(Request(streams = listOf(StreamId(1))))
    }
}
