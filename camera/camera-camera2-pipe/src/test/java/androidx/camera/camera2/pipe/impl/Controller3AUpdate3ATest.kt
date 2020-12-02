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

package androidx.camera.camera2.pipe.impl

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Status3A
import androidx.camera.camera2.pipe.testing.CameraPipeRobolectricTestRunner
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(CameraPipeRobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class Controller3AUpdate3ATest {
    private val graphProcessor = FakeGraphProcessor()
    private val graphState3A = GraphState3A()
    private val listener3A = Listener3A()
    private val controller3A = Controller3A(graphProcessor, graphState3A, listener3A)

    @Test
    fun testUpdate3AUpdatesState3A() {
        val result = controller3A.update3A(afMode = AfMode.OFF)
        assertThat(graphState3A.readState()[CaptureRequest.CONTROL_AF_MODE]).isEqualTo(
            CaptureRequest.CONTROL_AE_MODE_OFF
        )
        assertThat(result.isCompleted).isFalse()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testUpdate3ACancelsPreviousInProgressUpdate() {
        val result = controller3A.update3A(afMode = AfMode.OFF)
        // Invoking update3A before the previous one is complete will cancel the result of the
        // previous call.
        controller3A.update3A(afMode = AfMode.CONTINUOUS_PICTURE)
        assertThat(result.getCompletionExceptionOrNull() is CancellationException)
    }

    @Test
    fun testAfModeUpdate(): Unit = runBlocking {
        val result = controller3A.update3A(afMode = AfMode.OFF)
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
                        CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_OFF
                    )
                )
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Status3A.OK)
    }

    @Test
    fun testAeModeUpdate(): Unit = runBlocking {
        val result = controller3A.update3A(aeMode = AeMode.ON_ALWAYS_FLASH)
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
                        CaptureResult.CONTROL_AE_MODE to
                            CaptureResult.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                    )
                )
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Status3A.OK)
    }

    @Test
    fun testAwbModeUpdate(): Unit = runBlocking {
        val result = controller3A.update3A(awbMode = AwbMode.CLOUDY_DAYLIGHT)
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
                        CaptureResult.CONTROL_AWB_MODE to
                            CaptureResult.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                    )
                )
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Status3A.OK)
    }

    @Test
    fun testAfRegionsUpdate(): Unit = runBlocking {
        val result = controller3A.update3A(afRegions = listOf(MeteringRectangle(1, 1, 100, 100, 2)))
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
                        CaptureResult.CONTROL_AF_REGIONS to
                            Array(1) { MeteringRectangle(1, 1, 99, 99, 2) }
                    )
                )
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Status3A.OK)
    }

    @Test
    fun testAeRegionsUpdate(): Unit = runBlocking {
        val result = controller3A.update3A(aeRegions = listOf(MeteringRectangle(1, 1, 100, 100, 2)))
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
                        CaptureResult.CONTROL_AE_REGIONS to
                            Array(1) { MeteringRectangle(1, 1, 99, 99, 2) }
                    )
                )
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Status3A.OK)
    }

    @Test
    fun testAwbRegionsUpdate(): Unit = runBlocking {
        val result = controller3A.update3A(
            awbRegions = listOf(
                MeteringRectangle(1, 1, 100, 100, 2)
            )
        )
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
                        CaptureResult.CONTROL_AWB_REGIONS to
                            Array(1) { MeteringRectangle(1, 1, 99, 99, 2) }
                    )
                )
            )
        }
        val result3A = result.await()
        assertThat(result3A.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Status3A.OK)
    }
}