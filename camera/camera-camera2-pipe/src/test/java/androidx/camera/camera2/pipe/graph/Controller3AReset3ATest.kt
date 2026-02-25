/*
 * Copyright 2025 The Android Open Source Project
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

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.requiredParameters
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class Controller3AReset3ATest {
    val testScope = TestScope()
    private val graphTestContext = GraphTestContext()
    private val graphState3A = GraphState3A()
    private val graphProcessor = graphTestContext.graphProcessor
    private val captureSequenceProcessor = graphTestContext.captureSequenceProcessor

    private val listener3A = Listener3A()
    private val fakeMetadata =
        FakeCameraMetadata(
            mapOf(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                    intArrayOf(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE),
                CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE to 0.1f,
            )
        )
    private val controller3A = Controller3A(graphProcessor, fakeMetadata, graphState3A, listener3A)

    @Test
    fun reset3A_afterUpdate3A__resetsAfRegions() =
        testScope.runTest {
            val snapshot = controller3A.state3ASnapshot()

            val meteringRegions = listOf(MeteringRectangle(1, 1, 100, 100, 2))
            controller3A.update3A(afRegions = meteringRegions)
            advanceUntilIdle()

            controller3A.reset3A(snapshot)
            advanceUntilIdle()

            val updateParams = captureSequenceProcessor.nextEvent().requiredParameters
            val regionsForUpdate = updateParams[CaptureRequest.CONTROL_AF_REGIONS] as Array<*>
            assertThat(regionsForUpdate[0]).isEqualTo(meteringRegions[0])

            val resetParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(resetParams[CaptureRequest.CONTROL_AF_REGIONS]).isNull()
        }

    @Test
    fun reset3A_afterUpdate3A_resetsAeRegions() =
        testScope.runTest {
            val snapshot = controller3A.state3ASnapshot()

            val meteringRegions = listOf(MeteringRectangle(1, 1, 100, 100, 2))
            controller3A.update3A(aeRegions = meteringRegions)
            advanceUntilIdle()

            val updateParams = captureSequenceProcessor.nextEvent().requiredParameters
            val regionsForUpdate = updateParams[CaptureRequest.CONTROL_AE_REGIONS] as Array<*>
            assertThat(regionsForUpdate[0]).isEqualTo(meteringRegions[0])

            controller3A.reset3A(snapshot)
            advanceUntilIdle()

            val resetParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(resetParams[CaptureRequest.CONTROL_AE_REGIONS]).isNull()
        }

    @Test
    fun reset3a_afterUpdate3A_resetsAwbRegions() =
        testScope.runTest {
            val snapshot = controller3A.state3ASnapshot()

            val meteringRegions = listOf(MeteringRectangle(1, 1, 100, 100, 2))
            controller3A.update3A(awbRegions = meteringRegions)
            advanceUntilIdle()

            val updateParams = captureSequenceProcessor.nextEvent().requiredParameters
            val regionsForUpdate = updateParams[CaptureRequest.CONTROL_AWB_REGIONS] as Array<*>
            assertThat(regionsForUpdate[0]).isEqualTo(meteringRegions[0])

            controller3A.reset3A(snapshot)
            advanceUntilIdle()

            val resetParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(resetParams[CaptureRequest.CONTROL_AWB_REGIONS]).isNull()
        }

    @Test
    fun reset3A_afterUpdate3A_resets3ARegions() =
        testScope.runTest {
            val snapshot = controller3A.state3ASnapshot()

            val meteringRegions = listOf(MeteringRectangle(1, 1, 100, 100, 2))
            controller3A.update3A(
                aeRegions = meteringRegions,
                afRegions = meteringRegions,
                awbRegions = meteringRegions,
            )
            advanceUntilIdle()

            val updateParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat((updateParams[CaptureRequest.CONTROL_AE_REGIONS] as Array<*>)[0])
                .isEqualTo(meteringRegions[0])
            assertThat((updateParams[CaptureRequest.CONTROL_AF_REGIONS] as Array<*>)[0])
                .isEqualTo(meteringRegions[0])
            assertThat((updateParams[CaptureRequest.CONTROL_AWB_REGIONS] as Array<*>)[0])
                .isEqualTo(meteringRegions[0])

            controller3A.reset3A(snapshot)
            advanceUntilIdle()

            val resetParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(resetParams[CaptureRequest.CONTROL_AE_REGIONS]).isNull()
            assertThat(resetParams[CaptureRequest.CONTROL_AF_REGIONS]).isNull()
            assertThat(resetParams[CaptureRequest.CONTROL_AWB_REGIONS]).isNull()
        }

    @Test
    fun reset3A_afterUpdate3a_resetsModesAndRegionsToBefore() =
        testScope.runTest {
            val persistentAeRegions = listOf(MeteringRectangle(10, 10, 100, 100, 10))
            val persistentAfRegions = listOf(MeteringRectangle(20, 20, 100, 100, 20))
            val persistentAwbRegions = listOf(MeteringRectangle(30, 30, 100, 100, 30))
            controller3A.update3A(
                aeMode = AeMode.ON,
                afMode = AfMode.CONTINUOUS_PICTURE,
                awbMode = AwbMode.AUTO,
                aeRegions = persistentAeRegions,
                afRegions = persistentAfRegions,
                awbRegions = persistentAwbRegions,
            )
            advanceUntilIdle()
            captureSequenceProcessor.nextEvent()

            val snapshot = controller3A.state3ASnapshot()

            val sessionAeRegions = listOf(MeteringRectangle(1, 1, 50, 50, 1))
            val sessionAfRegions = listOf(MeteringRectangle(2, 2, 50, 50, 2))
            val sessionAwbRegions = listOf(MeteringRectangle(3, 3, 50, 50, 3))
            controller3A.update3A(
                aeMode = AeMode.OFF,
                afMode = AfMode.AUTO,
                awbMode = AwbMode.OFF,
                aeRegions = sessionAeRegions,
                afRegions = sessionAfRegions,
                awbRegions = sessionAwbRegions,
            )
            advanceUntilIdle()

            val updateParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(updateParams[CaptureRequest.CONTROL_AE_MODE]).isEqualTo(AeMode.OFF.value)
            assertThat(updateParams[CaptureRequest.CONTROL_AF_MODE]).isEqualTo(AfMode.AUTO.value)
            assertThat(updateParams[CaptureRequest.CONTROL_AWB_MODE]).isEqualTo(AwbMode.OFF.value)
            assertThat((updateParams[CaptureRequest.CONTROL_AE_REGIONS] as Array<*>)[0])
                .isEqualTo(sessionAeRegions[0])
            assertThat((updateParams[CaptureRequest.CONTROL_AF_REGIONS] as Array<*>)[0])
                .isEqualTo(sessionAfRegions[0])
            assertThat((updateParams[CaptureRequest.CONTROL_AWB_REGIONS] as Array<*>)[0])
                .isEqualTo(sessionAwbRegions[0])

            controller3A.reset3A(snapshot)
            advanceUntilIdle()

            val resetParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(resetParams[CaptureRequest.CONTROL_AE_MODE]).isEqualTo(AeMode.ON.value)
            assertThat(resetParams[CaptureRequest.CONTROL_AF_MODE])
                .isEqualTo(AfMode.CONTINUOUS_PICTURE.value)
            assertThat(resetParams[CaptureRequest.CONTROL_AWB_MODE]).isEqualTo(AwbMode.AUTO.value)
            assertThat((resetParams[CaptureRequest.CONTROL_AE_REGIONS] as Array<*>)[0])
                .isEqualTo(persistentAeRegions[0])
            assertThat((resetParams[CaptureRequest.CONTROL_AF_REGIONS] as Array<*>)[0])
                .isEqualTo(persistentAfRegions[0])
            assertThat((resetParams[CaptureRequest.CONTROL_AWB_REGIONS] as Array<*>)[0])
                .isEqualTo(persistentAwbRegions[0])
        }

    @Test
    fun reset3A_afterLockAe_unlocksAe() =
        testScope.runTest {
            val snapshot = controller3A.state3ASnapshot()

            controller3A.lock3A(aeLockBehavior = Lock3ABehavior.IMMEDIATE)
            advanceUntilIdle()
            captureSequenceProcessor.nextEvent()

            val lockParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(lockParams[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(true)

            controller3A.reset3A(snapshot)
            advanceUntilIdle()

            val resetParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(resetParams[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(null)
        }

    @Test
    fun reset3A_afterUnlockAe_noOp() =
        testScope.runTest {
            val snapshot = controller3A.state3ASnapshot()

            controller3A.unlock3A(ae = true)
            advanceUntilIdle()

            val lockParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(lockParams[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(false)

            controller3A.reset3A(snapshot)
            advanceUntilIdle()

            assertThat(captureSequenceProcessor.events).hasSize(2)
        }

    @Test
    fun reset3A_afterUnlockAe_locksToBefore() =
        testScope.runTest {
            controller3A.lock3A(aeLockBehavior = Lock3ABehavior.IMMEDIATE)
            advanceUntilIdle()
            captureSequenceProcessor.nextEvent()
            captureSequenceProcessor.nextEvent()

            val snapshot = controller3A.state3ASnapshot()

            controller3A.unlock3A(ae = true)
            advanceUntilIdle()

            val lockParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(lockParams[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(false)

            controller3A.reset3A(snapshot)
            advanceUntilIdle()

            val resetParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(resetParams[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(true)
        }

    @Test
    fun reset3A_afterUnlockAeAwb_locksToBefore() =
        testScope.runTest {
            controller3A.lock3A(
                aeLockBehavior = Lock3ABehavior.IMMEDIATE,
                awbLockBehavior = Lock3ABehavior.IMMEDIATE,
            )
            advanceUntilIdle()
            captureSequenceProcessor.nextEvent()
            captureSequenceProcessor.nextEvent()

            val snapshot = controller3A.state3ASnapshot()

            controller3A.unlock3A(ae = true, awb = true)
            advanceUntilIdle()

            val lockParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(lockParams[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(false)
            assertThat(lockParams[CaptureRequest.CONTROL_AWB_LOCK]).isEqualTo(false)

            controller3A.reset3A(snapshot)
            advanceUntilIdle()

            val resetParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(resetParams[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(true)
            assertThat(resetParams[CaptureRequest.CONTROL_AWB_LOCK]).isEqualTo(true)
        }

    @Test
    fun reset3A_afterLockAf_unlocksAf() =
        testScope.runTest {
            val snapshot = controller3A.state3ASnapshot()

            controller3A.lock3A(afLockBehavior = Lock3ABehavior.IMMEDIATE)
            advanceUntilIdle()

            captureSequenceProcessor.nextEvent()
            captureSequenceProcessor.nextEvent()
            val lockParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(lockParams[CaptureRequest.CONTROL_AF_TRIGGER])
                .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_START)

            controller3A.reset3A(snapshot)
            advanceUntilIdle()

            captureSequenceProcessor.nextEvent()
            val resetParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(resetParams[CaptureRequest.CONTROL_AF_TRIGGER])
                .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_CANCEL)
        }

    @Test
    fun reset3A_afterUnlockAf_locksToBefore() =
        testScope.runTest {
            controller3A.lock3A(afLockBehavior = Lock3ABehavior.IMMEDIATE)
            advanceUntilIdle()
            captureSequenceProcessor.nextEvent()
            captureSequenceProcessor.nextEvent()
            captureSequenceProcessor.nextEvent()

            val snapshot = controller3A.state3ASnapshot()

            controller3A.unlock3A(af = true)
            advanceUntilIdle()

            val unlockParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(unlockParams[CaptureRequest.CONTROL_AF_TRIGGER])
                .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_CANCEL)
            captureSequenceProcessor.nextEvent()

            controller3A.reset3A(snapshot)
            advanceUntilIdle()

            captureSequenceProcessor.nextEvent()
            val resetParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(resetParams[CaptureRequest.CONTROL_AF_TRIGGER])
                .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_START)
        }

    @Test
    fun reset3A_afterUnlockAeAfAwb_locksToBefore() =
        testScope.runTest {
            controller3A.lock3A(
                aeLockBehavior = Lock3ABehavior.IMMEDIATE,
                afLockBehavior = Lock3ABehavior.IMMEDIATE,
                awbLockBehavior = Lock3ABehavior.IMMEDIATE,
            )
            advanceUntilIdle()
            captureSequenceProcessor.nextEvent()
            captureSequenceProcessor.nextEvent()
            captureSequenceProcessor.nextEvent()

            val snapshot = controller3A.state3ASnapshot()

            controller3A.unlock3A(ae = true, af = true, awb = true)
            advanceUntilIdle()

            val unlockSingleRequestParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(unlockSingleRequestParams[CaptureRequest.CONTROL_AF_TRIGGER])
                .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_CANCEL)

            val unlockRepeatingRepeatingParams =
                captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(unlockRepeatingRepeatingParams[CaptureRequest.CONTROL_AE_LOCK])
                .isEqualTo(false)
            assertThat(unlockRepeatingRepeatingParams[CaptureRequest.CONTROL_AWB_LOCK])
                .isEqualTo(false)

            controller3A.reset3A(snapshot)
            advanceUntilIdle()

            captureSequenceProcessor.nextEvent()
            val resetParams = captureSequenceProcessor.nextEvent().requiredParameters
            assertThat(resetParams[CaptureRequest.CONTROL_AE_LOCK]).isEqualTo(true)
            assertThat(resetParams[CaptureRequest.CONTROL_AF_TRIGGER])
                .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_START)
            assertThat(resetParams[CaptureRequest.CONTROL_AWB_LOCK]).isEqualTo(true)
        }

    @After
    fun tearDown() {
        graphTestContext.close()
    }
}
