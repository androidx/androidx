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

package androidx.camera.camera2.pipe.graph

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphBase.Companion.subscribeToLatestFrameResult
import androidx.camera.camera2.pipe.CameraGraphBase.Companion.subscribeToLatestFrameResults
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.LatestFrameMetadata
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestListeners
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.testing.CameraGraphSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class LatestFrameMetadataAggregatorTest {

    private val fakeRequestMetadata = FakeRequestMetadata()
    private val keyX = CaptureResult.SENSOR_EXPOSURE_TIME
    private val keyY = CaptureResult.SENSOR_SENSITIVITY
    private val metaKeyZ = Metadata.Key.create<Int>("test.key.z")

    // Simulator test helpers
    private val testScope = TestScope()
    private val metadata =
        FakeCameraMetadata.fromTemplate(
            template = HighEndDeviceTemplate,
            lensFacing = CameraCharacteristics.LENS_FACING_FRONT,
        )
    private val streamConfig = CameraStream.Config.create(Size(640, 480), StreamFormat.YUV_420_888)
    private val graphConfig =
        CameraGraph.Config(camera = metadata.camera, streams = listOf(streamConfig))
    private val context = ApplicationProvider.getApplicationContext() as Context
    private var simulator: CameraGraphSimulator? = null

    @After
    fun tearDown() {
        simulator?.close()
    }

    private suspend fun TestScope.startSimulator(): CameraGraphSimulator {
        val sim = CameraGraphSimulator.create(testScope, context, metadata, graphConfig)
        simulator = sim
        val stream = checkNotNull(sim.streams[streamConfig])
        val request = Request(streams = listOf(stream.id))
        sim.acquireSession().use { it.startRepeating(request) }
        sim.start()
        sim.initializeSurfaces()
        sim.simulateCameraStarted()
        advanceUntilIdle()
        return sim
    }

    // --- Unit Tests for LatestFrameMetadataAggregator ---

    @Test
    fun testSlidingWindowLogic() {
        var latestParams: LatestFrameMetadata? = null
        val aggregator =
            LatestFrameMetadataAggregator(
                captureResultKeys = setOf(keyX, keyY),
                metadataKeys = emptySet(),
                maxWindowSize = 3,
            ) {
                latestParams = it
            }

        val frame1Metadata =
            FakeFrameMetadata(frameNumber = FrameNumber(1), resultMetadata = mapOf(keyX to 10L))
        val frame1Info =
            FakeFrameInfo(metadata = frame1Metadata, requestMetadata = fakeRequestMetadata)

        val frame2Metadata =
            FakeFrameMetadata(frameNumber = FrameNumber(2), resultMetadata = mapOf(keyX to 20L))

        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(1), frame1Metadata)
        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(2), frame2Metadata)

        // Resolves exp time to F2 exposure time
        assertThat(latestParams?.get(keyX)).isEqualTo(20L)

        aggregator.onTotalCaptureResult(fakeRequestMetadata, FrameNumber(1), frame1Info)

        assertThat(latestParams?.get(keyX))
            .isEqualTo(20L) // Still resolved to F2 since it is the highest frame number in window

        val frame3Metadata =
            FakeFrameMetadata(frameNumber = FrameNumber(3), resultMetadata = mapOf(keyY to 100))
        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(3), frame3Metadata)

        assertThat(latestParams?.get(keyX)).isEqualTo(20L)
        assertThat(latestParams?.get(keyY)).isEqualTo(100)
    }

    @Test
    fun testValuesStopBeingReported() {
        var latestParams: LatestFrameMetadata? = null
        val aggregator =
            LatestFrameMetadataAggregator(
                captureResultKeys = setOf(keyX, keyY),
                metadataKeys = emptySet(),
                maxWindowSize = 3,
            ) {
                latestParams = it
            }

        val frame1Metadata =
            FakeFrameMetadata(frameNumber = FrameNumber(1), resultMetadata = mapOf(keyX to 10L))
        val frame2Metadata =
            FakeFrameMetadata(
                frameNumber = FrameNumber(2),
                resultMetadata = mapOf(keyY to 100),
            ) // No keyX

        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(1), frame1Metadata)
        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(2), frame2Metadata)

        assertThat(latestParams?.get(keyX)).isEqualTo(10L) // Found in F1 since F2 is not completed
        assertThat(latestParams?.get(keyY)).isEqualTo(100)

        val frame2Info =
            FakeFrameInfo(metadata = frame2Metadata, requestMetadata = fakeRequestMetadata)
        aggregator.onTotalCaptureResult(fakeRequestMetadata, FrameNumber(2), frame2Info)

        assertThat(latestParams?.get(keyX))
            .isNull() // F2 total stops search, and F2 doesn't have keyX
        assertThat(latestParams?.get(keyY)).isEqualTo(100)
    }

    @Test
    fun testRepeatedUpdatesDoNotFireMultipleEvents() {
        val updates = mutableListOf<LatestFrameMetadata>()
        val aggregator =
            LatestFrameMetadataAggregator(
                captureResultKeys = setOf(keyX),
                metadataKeys = emptySet(),
            ) {
                updates.add(it)
            }

        val frame1Metadata =
            FakeFrameMetadata(frameNumber = FrameNumber(1), resultMetadata = mapOf(keyX to 10L))
        val frame1Info =
            FakeFrameInfo(metadata = frame1Metadata, requestMetadata = fakeRequestMetadata)

        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(1), frame1Metadata)
        assertThat(updates).hasSize(1)

        // Sending the total capture result with the same value should not fire another update
        aggregator.onTotalCaptureResult(fakeRequestMetadata, FrameNumber(1), frame1Info)
        assertThat(updates).hasSize(1)

        // Sending F2 partial with same value should not fire another update
        val frame2Metadata =
            FakeFrameMetadata(frameNumber = FrameNumber(2), resultMetadata = mapOf(keyX to 10L))
        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(2), frame2Metadata)
        assertThat(updates).hasSize(1)

        // Sending F2 partial with different value SHOULD fire an update
        val frame2MetadataNew =
            FakeFrameMetadata(frameNumber = FrameNumber(2), resultMetadata = mapOf(keyX to 20L))
        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(2), frame2MetadataNew)
        assertThat(updates).hasSize(2)
    }

    @Test
    fun testKeyTransitioningToAbsentInTotalCaptureResult() {
        val updates = mutableListOf<LatestFrameMetadata>()
        val aggregator =
            LatestFrameMetadataAggregator(
                captureResultKeys = setOf(keyX),
                metadataKeys = emptySet(),
            ) {
                updates.add(it)
            }

        // F1 partial reports keyX = 10L
        val frame1Metadata =
            FakeFrameMetadata(frameNumber = FrameNumber(1), resultMetadata = mapOf(keyX to 10L))
        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(1), frame1Metadata)

        assertThat(updates).hasSize(1)
        assertThat(updates.last()[keyX]).isEqualTo(10L)

        // F2 total arrives without keyX (absent)
        val frame2Metadata = FakeFrameMetadata(frameNumber = FrameNumber(2))
        val frame2Info =
            FakeFrameInfo(metadata = frame2Metadata, requestMetadata = fakeRequestMetadata)

        aggregator.onTotalCaptureResult(fakeRequestMetadata, FrameNumber(2), frame2Info)

        assertThat(updates).hasSize(2)
        assertThat(updates.last()[keyX]).isNull()
    }

    @Test
    fun testOlderFrameUpdateDoesNotOverrideNewerFrame() {
        val updates = mutableListOf<LatestFrameMetadata>()
        val aggregator =
            LatestFrameMetadataAggregator(
                captureResultKeys = setOf(keyX),
                metadataKeys = emptySet(),
            ) {
                updates.add(it)
            }

        val f1Partial1 =
            FakeFrameMetadata(frameNumber = FrameNumber(1), resultMetadata = mapOf(keyX to 10L))
        val f2Partial1 =
            FakeFrameMetadata(frameNumber = FrameNumber(2), resultMetadata = mapOf(keyX to 20L))
        val f1Partial2 =
            FakeFrameMetadata(frameNumber = FrameNumber(1), resultMetadata = mapOf(keyX to 10L))
        val f1Total =
            FakeFrameInfo(
                FakeFrameMetadata(
                    frameNumber = FrameNumber(1),
                    resultMetadata = mapOf(keyX to 10L),
                ),
                fakeRequestMetadata,
            )

        // Frame #1 fires partial result #1 with [keyX, 10L]
        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(1), f1Partial1)
        assertThat(updates).hasSize(1)
        assertThat(updates.last()[keyX]).isEqualTo(10L)

        // Frame #2 fires partial result #1 with [keyX, 20L]
        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(2), f2Partial1)
        assertThat(updates).hasSize(2)
        assertThat(updates.last()[keyX]).isEqualTo(20L)

        // Frame #1 fires partial result #2 with [keyX, 10L]
        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(1), f1Partial2)
        // State should remain [keyX, 20L], and no new update should fire
        assertThat(updates).hasSize(2)
        assertThat(updates.last()[keyX]).isEqualTo(20L)

        // Frame #1 fires total result #1 with [keyX, 10L]
        aggregator.onTotalCaptureResult(fakeRequestMetadata, FrameNumber(1), f1Total)
        // State should remain [keyX, 20L], and no new update should fire
        assertThat(updates).hasSize(2)
        assertThat(updates.last()[keyX]).isEqualTo(20L)
    }

    @Test
    fun testPartialResultComingAfterTotalResultIsIgnored() {
        val updates = mutableListOf<LatestFrameMetadata>()
        val aggregator =
            LatestFrameMetadataAggregator(
                captureResultKeys = setOf(keyX),
                metadataKeys = emptySet(),
            ) {
                updates.add(it)
            }

        val f1Total =
            FakeFrameInfo(
                FakeFrameMetadata(
                    frameNumber = FrameNumber(1),
                    resultMetadata = mapOf(keyX to 10L),
                ),
                fakeRequestMetadata,
            )
        val f1Partial =
            FakeFrameMetadata(frameNumber = FrameNumber(1), resultMetadata = mapOf(keyX to 20L))

        aggregator.onTotalCaptureResult(fakeRequestMetadata, FrameNumber(1), f1Total)
        assertThat(updates).hasSize(1)
        assertThat(updates.last()[keyX]).isEqualTo(10L)

        // Delayed partial for F1 arriving after F1 total should be ignored
        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(1), f1Partial)
        assertThat(updates).hasSize(1)
        assertThat(updates.last()[keyX]).isEqualTo(10L)
    }

    @Test
    fun testParameterTransitioningToNullAndBackToRealValue() {
        val updates = mutableListOf<LatestFrameMetadata>()
        val aggregator =
            LatestFrameMetadataAggregator(
                captureResultKeys = setOf(keyX),
                metadataKeys = emptySet(),
            ) {
                updates.add(it)
            }

        // Frame #1 Total has a real value (10L)
        val f1Total =
            FakeFrameInfo(
                FakeFrameMetadata(
                    frameNumber = FrameNumber(1),
                    resultMetadata = mapOf(keyX to 10L),
                ),
                fakeRequestMetadata,
            )
        aggregator.onTotalCaptureResult(fakeRequestMetadata, FrameNumber(1), f1Total)

        assertThat(updates).hasSize(1)
        assertThat(updates.last()[keyX]).isEqualTo(10L)

        // Frame #2 Total has null (absent)
        val f2Total =
            FakeFrameInfo(FakeFrameMetadata(frameNumber = FrameNumber(2)), fakeRequestMetadata)
        aggregator.onTotalCaptureResult(fakeRequestMetadata, FrameNumber(2), f2Total)

        assertThat(updates).hasSize(2)
        assertThat(updates.last()[keyX]).isNull()

        // Frame #3 Total has a real value (20L)
        val f3Total =
            FakeFrameInfo(
                FakeFrameMetadata(
                    frameNumber = FrameNumber(3),
                    resultMetadata = mapOf(keyX to 20L),
                ),
                fakeRequestMetadata,
            )
        aggregator.onTotalCaptureResult(fakeRequestMetadata, FrameNumber(3), f3Total)

        assertThat(updates).hasSize(3)
        assertThat(updates.last()[keyX]).isEqualTo(20L)
    }

    // --- Integration Tests with CameraGraphSimulator ---

    @Test
    fun simulatorCanSimulateCameraParametersListener() =
        testScope.runTest {
            val sim = startSimulator()

            var latestParams: LatestFrameMetadata? = null
            val listener =
                RequestListeners.createLatestFrameMetadataListener(
                    captureResultKeys = setOf(keyX)
                ) {
                    latestParams = it
                }
            sim.listeners.add(listener)

            advanceUntilIdle()
            sim.simulateNextFrame() // drain repeating

            val frame = sim.simulateNextFrame()
            assertThat(latestParams?.get(keyX)).isNull()

            val resultMetadata = mutableMapOf<CaptureResult.Key<*>, Any>(keyX to 10L)
            frame.simulatePartialCaptureResult(resultMetadata)
            advanceUntilIdle()

            assertThat(latestParams?.get(keyX)).isEqualTo(10L)

            sim.listeners.remove(listener)
        }

    @Test
    fun simulatorCanSimulateCameraParametersFlow() =
        testScope.runTest {
            val sim = startSimulator()

            var latestParams: LatestFrameMetadata? = null
            val job = launch {
                sim.subscribeToLatestFrameResults(captureResultKeys = setOf(keyX)).collect {
                    latestParams = it
                }
            }

            advanceUntilIdle()
            sim.simulateNextFrame() // drain

            val frame = sim.simulateNextFrame()
            assertThat(latestParams?.get(keyX)).isNull()

            val resultMetadata = mutableMapOf<CaptureResult.Key<*>, Any>(keyX to 10L)
            frame.simulatePartialCaptureResult(resultMetadata)
            advanceUntilIdle()

            assertThat(latestParams?.get(keyX)).isEqualTo(10L)

            job.cancel()
        }

    @Test
    fun simulatorCanSimulateSingleKeySubscriptionListener() =
        testScope.runTest {
            val sim = startSimulator()

            var latestValue: Long? = null
            val listener =
                RequestListeners.createLatestFrameMetadataListener(
                    captureResultKeys = setOf(keyX)
                ) {
                    latestValue = it[keyX]
                }
            sim.listeners.add(listener)

            advanceUntilIdle()
            sim.simulateNextFrame() // drain

            val frame = sim.simulateNextFrame()
            assertThat(latestValue).isNull()

            val resultMetadata = mutableMapOf<CaptureResult.Key<*>, Any>(keyX to 20L)
            frame.simulatePartialCaptureResult(resultMetadata)
            advanceUntilIdle()

            assertThat(latestValue).isEqualTo(20L)

            sim.listeners.remove(listener)
        }

    @Test
    fun simulatorCanSimulateSingleKeySubscriptionFlow() =
        testScope.runTest {
            val sim = startSimulator()

            var latestValue: Long? = null
            val job = launch { sim.subscribeToLatestFrameResult(keyX).collect { latestValue = it } }

            advanceUntilIdle()
            sim.simulateNextFrame() // drain

            val frame = sim.simulateNextFrame()
            assertThat(latestValue).isNull()

            val resultMetadata = mutableMapOf<CaptureResult.Key<*>, Any>(keyX to 30L)
            frame.simulatePartialCaptureResult(resultMetadata)
            advanceUntilIdle()

            assertThat(latestValue).isEqualTo(30L)

            job.cancel()
        }

    @Test
    fun simulatorCanSimulateSingleMetadataKeySubscriptionListener() =
        testScope.runTest {
            val sim = startSimulator()

            var latestValue: Int? = null
            val listener =
                RequestListeners.createLatestFrameMetadataListener(metadataKeys = setOf(metaKeyZ)) {
                    latestValue = it[metaKeyZ]
                }
            sim.listeners.add(listener)

            advanceUntilIdle()
            sim.simulateNextFrame() // drain

            val frame = sim.simulateNextFrame()
            assertThat(latestValue).isNull()

            frame.simulateTotalCaptureResult(emptyMap(), extraResultMetadata = mapOf(metaKeyZ to 5))
            advanceUntilIdle()

            assertThat(latestValue).isEqualTo(5)

            sim.listeners.remove(listener)
        }

    @Test
    fun simulatorCanSimulateSingleMetadataKeySubscriptionFlow() =
        testScope.runTest {
            val sim = startSimulator()

            var latestValue: Int? = null
            val job = launch {
                sim.subscribeToLatestFrameResult(metaKeyZ).collect { latestValue = it }
            }

            advanceUntilIdle()
            sim.simulateNextFrame() // drain

            val frame = sim.simulateNextFrame()
            assertThat(latestValue).isNull()

            frame.simulateTotalCaptureResult(emptyMap(), extraResultMetadata = mapOf(metaKeyZ to 6))
            advanceUntilIdle()

            assertThat(latestValue).isEqualTo(6)

            job.cancel()
        }

    @Test
    fun simulatorCanSimulateMultipleKeysOrderingAndFiltering() =
        testScope.runTest {
            val sim = startSimulator()

            var params: LatestFrameMetadata? = null
            val listener =
                RequestListeners.createLatestFrameMetadataListener(
                    captureResultKeys = setOf(keyX, keyY),
                    metadataKeys = setOf(metaKeyZ),
                ) {
                    params = it
                }
            sim.listeners.add(listener)

            advanceUntilIdle()
            sim.simulateNextFrame() // drain

            // Frame 1 Partial & Total
            val frame1 = sim.simulateNextFrame()
            frame1.simulatePartialCaptureResult(
                mapOf<CaptureResult.Key<*>, Any>(keyX to 10L, keyY to 100)
            )
            frame1.simulateTotalCaptureResult(
                emptyMap(),
                extraResultMetadata = mapOf(metaKeyZ to 1),
            )
            advanceUntilIdle()

            assertThat(params?.get(keyX)).isEqualTo(10L)
            assertThat(params?.get(keyY)).isEqualTo(100)
            assertThat(params?.get(metaKeyZ)).isEqualTo(1)
            assertThat(params?.keys).containsExactly(keyX, keyY)
            assertThat(params?.metadataKeys).containsExactly(metaKeyZ)

            // Frame 2 Partial with A, B
            val frame2 = sim.simulateNextFrame()
            frame2.simulatePartialCaptureResult(
                mapOf<CaptureResult.Key<*>, Any>(keyX to 20L, keyY to 200)
            )
            advanceUntilIdle()

            // Frame 3 Partial with A
            val frame3 = sim.simulateNextFrame()
            frame3.simulatePartialCaptureResult(mapOf(keyX to 30L))
            advanceUntilIdle()

            // Assert A comes from F3 partial (30) and B comes from F2 partial (200)
            assertThat(params?.get(keyX)).isEqualTo(30L)
            assertThat(params?.get(keyY)).isEqualTo(200)

            // Total for Frame 2
            frame2.simulateTotalCaptureResult(emptyMap())
            advanceUntilIdle()

            // Assert A still comes from F3 partial (30)
            assertThat(params?.get(keyX)).isEqualTo(30L)
            assertThat(params?.get(keyY)).isEqualTo(200)

            sim.listeners.remove(listener)
        }

    @Test
    fun testFrameNumberPropagation() {
        var latestParams: LatestFrameMetadata? = null
        val aggregator =
            LatestFrameMetadataAggregator(
                captureResultKeys = setOf(keyX),
                metadataKeys = emptySet(),
                maxWindowSize = 3,
            ) {
                latestParams = it
            }

        // F1 reports keyX = 10L
        val frame1Metadata =
            FakeFrameMetadata(frameNumber = FrameNumber(1), resultMetadata = mapOf(keyX to 10L))
        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(1), frame1Metadata)

        assertThat(latestParams?.get(keyX)).isEqualTo(10L)
        assertThat(latestParams?.getFrameNumber(keyX)).isEqualTo(FrameNumber(1))

        // F2 reports keyX = 20L
        val frame2Metadata =
            FakeFrameMetadata(frameNumber = FrameNumber(2), resultMetadata = mapOf(keyX to 20L))
        aggregator.onPartialCaptureResult(fakeRequestMetadata, FrameNumber(2), frame2Metadata)

        assertThat(latestParams?.get(keyX)).isEqualTo(20L)
        assertThat(latestParams?.getFrameNumber(keyX)).isEqualTo(FrameNumber(2))
    }

    @Test
    fun testExcludeFilter() {
        var latestParams: LatestFrameMetadata? = null
        val targetRequest = Request(streams = emptyList())
        val excludeRequest = Request(streams = emptyList())

        val aggregator =
            LatestFrameMetadataAggregator(
                captureResultKeys = setOf(keyX),
                metadataKeys = emptySet(),
                filter = { requestMetadata -> requestMetadata.request == excludeRequest },
            ) {
                latestParams = it
            }

        // Send updates for targetRequest -> should update latestParams
        val metadata1 =
            FakeFrameMetadata(frameNumber = FrameNumber(1), resultMetadata = mapOf(keyX to 10L))
        aggregator.onPartialCaptureResult(
            FakeRequestMetadata(request = targetRequest),
            FrameNumber(1),
            metadata1,
        )
        assertThat(latestParams?.get(keyX)).isEqualTo(10L)

        // Send updates for excludeRequest -> should be ignored, latestParams remains 10L
        val metadata2 =
            FakeFrameMetadata(frameNumber = FrameNumber(2), resultMetadata = mapOf(keyX to 20L))
        aggregator.onPartialCaptureResult(
            FakeRequestMetadata(request = excludeRequest),
            FrameNumber(2),
            metadata2,
        )
        assertThat(latestParams?.get(keyX)).isEqualTo(10L) // Still 10L
    }

    @Test
    fun simulatorCanFilterRequestsUsingRequestFilter() =
        testScope.runTest {
            val sim = startSimulator()

            var latestParams: LatestFrameMetadata? = null
            val targetRequest = Request(streams = sim.streams.streams.map { it.id })
            val excludeRequest = Request(streams = sim.streams.streams.map { it.id })

            val listener =
                RequestListeners.createLatestFrameMetadataListener(
                    captureResultKeys = setOf(keyX),
                    filter = { requestMetadata -> requestMetadata.request == excludeRequest },
                ) {
                    latestParams = it
                }
            sim.listeners.add(listener)

            advanceUntilIdle()
            sim.simulateNextFrame() // Drain initial repeating request (without listener)

            // 1. Simulate frame with targetRequest (repeating)
            val frame1 = sim.simulateNextFrame()
            frame1.simulatePartialCaptureResult(mapOf(keyX to 10L))
            frame1.simulateTotalCaptureResult(emptyMap())
            advanceUntilIdle()
            assertThat(latestParams?.get(keyX)).isEqualTo(10L)

            // 2. Submit single request to exclude and simulate it
            sim.acquireSession().use { it.submit(excludeRequest) }
            advanceUntilIdle() // Wait for request to be processed
            val frame2 = sim.simulateNextFrame()
            frame2.simulatePartialCaptureResult(mapOf(keyX to 20L))
            frame2.simulateTotalCaptureResult(emptyMap())
            advanceUntilIdle()
            assertThat(latestParams?.get(keyX)).isEqualTo(10L) // Still 10L (excluded!)

            // 3. Simulate another target frame to verify it resumes updating
            val frame3 = sim.simulateNextFrame()
            frame3.simulatePartialCaptureResult(mapOf(keyX to 30L))
            frame3.simulateTotalCaptureResult(emptyMap())
            advanceUntilIdle()
            assertThat(latestParams?.get(keyX)).isEqualTo(30L) // Resumed!

            sim.listeners.remove(listener)
        }

    @Test
    fun testTotalCaptureResultWithNullValueOverwritesPreviousValue() {
        var latestParams: LatestFrameMetadata? = null
        val aggregator =
            LatestFrameMetadataAggregator(
                captureResultKeys = setOf(keyX),
                metadataKeys = emptySet(),
                maxWindowSize = 3,
            ) {
                latestParams = it
            }

        // F1 reports KeyX = 10L (Total Result)
        val frame1Metadata =
            FakeFrameMetadata(frameNumber = FrameNumber(1), resultMetadata = mapOf(keyX to 10L))
        aggregator.onTotalCaptureResult(
            fakeRequestMetadata,
            FrameNumber(1),
            FakeFrameInfo(frame1Metadata, fakeRequestMetadata),
        )

        assertThat(latestParams?.get(keyX)).isEqualTo(10L)

        // F2 is a Total Capture Result but does NOT report KeyX (value is null)
        val frame2Metadata =
            FakeFrameMetadata(frameNumber = FrameNumber(2), resultMetadata = emptyMap())
        aggregator.onTotalCaptureResult(
            fakeRequestMetadata,
            FrameNumber(2),
            FakeFrameInfo(frame2Metadata, fakeRequestMetadata),
        )

        // Snapshot should now return null for KeyX
        assertThat(latestParams?.get(keyX)).isNull()
    }
}
