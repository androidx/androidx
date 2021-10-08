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
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
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
internal class Controller3ASubmit3ATest {
    private val graphState3A = GraphState3A()
    private val graphProcessor = FakeGraphProcessor(graphState3A = graphState3A)
    private val requestProcessor = FakeRequestProcessor()
    private val listener3A = Listener3A()
    private val controller3A = Controller3A(graphProcessor, graphState3A, listener3A)

    @Test
    fun testSubmit3ADoesNotUpdateState3A(): Unit = runBlocking {
        initGraphProcessor()

        val result = controller3A.submit3A(afMode = AfMode.OFF)
        assertThat(graphState3A.afMode?.value).isNotEqualTo(CaptureRequest.CONTROL_AF_MODE_OFF)
        assertThat(result.isCompleted).isFalse()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAfModeSubmit(): Unit = runBlocking {
        initGraphProcessor()

        val result = controller3A.submit3A(afMode = AfMode.OFF)
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
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAeModeSubmit(): Unit = runBlocking {
        initGraphProcessor()

        val result = controller3A.submit3A(aeMode = AeMode.ON_ALWAYS_FLASH)
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
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAwbModeSubmit(): Unit = runBlocking {
        initGraphProcessor()

        val result = controller3A.submit3A(awbMode = AwbMode.CLOUDY_DAYLIGHT)
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
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAfRegionsSubmit(): Unit = runBlocking {
        initGraphProcessor()

        val result = controller3A.submit3A(afRegions = listOf(MeteringRectangle(1, 1, 100, 100, 2)))
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
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAeRegionsSubmit(): Unit = runBlocking {
        initGraphProcessor()

        val result = controller3A.submit3A(aeRegions = listOf(MeteringRectangle(1, 1, 100, 100, 2)))
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
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testAwbRegionsSubmit(): Unit = runBlocking {
        initGraphProcessor()

        val result = controller3A.submit3A(
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
        assertThat(result3A.frameMetadata!!.frameNumber.value).isEqualTo(101L)
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
    }

    @Test
    fun testWithGraphProcessorFailure(): Unit = runBlocking {
        // There are different conditions that can lead to the request processor not being able
        // to successfully submit the desired request. For this test we are closing the processor.
        graphProcessor.close()

        // Since the request processor is closed the submit3A method call will fail.
        val result = controller3A.submit3A(aeMode = AeMode.ON_ALWAYS_FLASH).await()
        assertThat(result.frameMetadata).isNull()
        assertThat(result.status).isEqualTo(Result3A.Status.SUBMIT_FAILED)
    }

    private fun initGraphProcessor() {
        graphProcessor.onGraphStarted(requestProcessor)
        graphProcessor.startRepeating(Request(streams = listOf(StreamId(1))))
    }
}