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

package androidx.xr.testutils

import android.graphics.Rect
import androidx.compose.animation.core.LinearEasing
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HeadMovementHelperTest {

    @Test
    fun readSimulatedHeadPoseInternal_parsesValidPropertiesAndDefaultsWhenUnset() {
        assertEquals(Pose.Identity, readSimulatedHeadPoseInternal { "" })

        val configuredPose = readSimulatedHeadPoseInternal { cmd ->
            when (cmd) {
                "getprop $PROP_FAKE_HET_POSITION" -> "0.25000,-0.10000,-1.50000"
                "getprop $PROP_FAKE_HET_ORIENTATION" -> "0.00000,0.70711,0.00000,0.70711"
                else -> ""
            }
        }
        assertVectorApproxEquals(Vector3(0.25f, -0.10f, -1.50f), configuredPose.translation)
        assertTrue(
            Quaternion.angle(Quaternion.fromAxisAngle(Vector3.Up, 90f), configuredPose.rotation) <
                0.05f
        )

        val malformedPose = readSimulatedHeadPoseInternal { cmd ->
            if (cmd == "getprop $PROP_FAKE_HET_POSITION") "1.0,NaN,2.0" else "1,2,3"
        }
        assertEquals(Pose.Identity, malformedPose)
    }

    @Test
    fun resetHeadMovementInternal_resetsToIdentityPoseAndSettles() {
        val device = FakeDevice()

        resetHeadMovementInternal(settleDelayMs = 50L, device.shellExecutor, device.sleeper)

        assertEquals(
            listOf(
                "setprop cpm.fake_het_position 0.00000,0.00000,0.00000",
                "setprop cpm.fake_het_orientation 0.00000,0.00000,0.00000,1.00000",
            ),
            device.setprops,
        )
        assertEquals(listOf(50L), device.sleeps)
    }

    @Test
    fun poseHeadTo_formatsWithUsLocaleNormalizesNegativeZeroAndSettles() {
        val device = FakeDevice()
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.FRANCE)
        try {
            device.perform(settleDelayMs = 75L) {
                poseHeadTo(
                    Pose(Vector3(-0.0f, 1.25f, -0.5f), Quaternion.fromAxisAngle(Vector3.Up, 90f))
                )
            }
        } finally {
            Locale.setDefault(previousLocale)
        }

        assertEquals(
            listOf(
                "setprop cpm.fake_het_position 0.00000,1.25000,-0.50000",
                "setprop cpm.fake_het_orientation 0.00000,0.70711,0.00000,0.70711",
            ),
            device.setprops,
        )
        assertEquals(listOf(75L), device.sleeps)
    }

    @Test
    fun animate_poseLerp_pacesFramesWithClockSettlesOnceAndRetainsFinalPose() {
        val device = FakeDevice(setpropLatencyMs = 5L)
        val endPose = Pose(Vector3(1f, 0f, -0.5f), Quaternion.fromAxisAngle(Vector3.Up, 90f))

        device.perform(settleDelayMs = 60L) {
            val start = currentPose
            assertEquals(Pose.Identity, start)
            // 112ms / 28ms frame interval -> 4 intervals -> 5 frames (0, 0.25, 0.5, 0.75, 1.0).
            animate(durationMs = 112L, easing = LinearEasing) { fraction ->
                poseHeadTo(Pose.lerp(start, endPose, fraction))
            }
            assertVectorApproxEquals(endPose.translation, currentPosition)
            assertTrue(Quaternion.angle(endPose.rotation, currentRotation) < 0.05f)
        }

        // 5 frames * 2 properties, and no reset since resetOnClose defaults to false.
        assertEquals(10, device.setprops.size)
        assertEquals("setprop cpm.fake_het_position 0.50000,0.00000,-0.25000", device.setprops[4])
        assertEquals("setprop cpm.fake_het_position 1.00000,0.00000,-0.50000", device.setprops[8])
        assertEquals(
            "setprop cpm.fake_het_orientation 0.00000,0.70711,0.00000,0.70711",
            device.setprops[9],
        )
        // Each 28ms interval sleeps 18ms (28ms - 10ms setprop latency), then a single 60ms settle
        // after the final frame instead of one per frame.
        assertEquals(listOf(18L, 18L, 18L, 18L, 60L), device.sleeps)
    }

    @Test
    fun animate_frameOverrunsInterval_skipsMissedFramesWithoutStretchingDuration() {
        // 2 setprops * 20ms = 40ms per frame, which overruns the 28ms frame interval.
        val device = FakeDevice(setpropLatencyMs = 20L)
        val endPose = Pose(Vector3(1f, 0f, 0f), Quaternion.fromAxisAngle(Vector3.Up, 90f))

        device.perform {
            val start = currentPose
            animate(durationMs = 112L, easing = LinearEasing) { fraction ->
                poseHeadTo(Pose.lerp(start, endPose, fraction))
            }
        }

        // Frames at t = 0ms and t = 56ms; the slots at 28ms and 84ms are skipped, then the final
        // frame is written at t = 112ms instead of stepping through all 5 fixed frames.
        assertEquals(3, device.recordedPoses.size)
        assertVectorApproxEquals(Vector3.Zero, device.recordedPoses[0].translation)
        assertVectorApproxEquals(Vector3(0.5f, 0f, 0f), device.recordedPoses[1].translation)
        assertVectorApproxEquals(Vector3(1f, 0f, 0f), device.recordedPoses[2].translation)
        // Each frame ends at +40ms and waits 16ms for the next 28ms grid slot.
        assertEquals(listOf(16L, 16L), device.sleeps)
    }

    @Test
    fun sequentialMovements_chainFromExistingDevicePoseAndSkipRedundantSetprops() {
        val device =
            FakeDevice(
                initialPosition = "0.20000,1.00000,0.00000",
                initialOrientation = "0.00000,0.00000,0.00000,1.00000",
            )

        device.perform(settleDelayMs = 25L) {
            assertVectorApproxEquals(Vector3(0.2f, 1.0f, 0f), currentPosition)
            val start = currentPosition
            val end = start + Vector3(0.8f, 0f, 0f)
            // 56ms / 28ms -> 2 intervals -> 3 frames: x = 0.2, 0.6, 1.0.
            animate(durationMs = 56L, easing = LinearEasing) { fraction ->
                moveHeadTo(Vector3.lerp(start, end, fraction))
            }
            // Already gazing along -Z, so the orientation does not change.
            lookAt(Vector3(1.0f, 1.0f, -2.0f))
        }

        // Frame 0 matches the pose already on the device, so only frames 0.5 and 1.0 call setprop.
        assertEquals(
            listOf(
                "setprop cpm.fake_het_position 0.60000,1.00000,0.00000",
                "setprop cpm.fake_het_position 1.00000,1.00000,0.00000",
            ),
            device.setprops,
        )
        // 2 frame intervals + the animate settle, then the lookAt settle.
        assertEquals(listOf(28L, 28L, 25L, 25L), device.sleeps)
    }

    @Test
    fun poseHeadBy_composesDeltaInHeadLocalFrame() {
        val device = FakeDevice()

        device.perform {
            poseHeadTo(Pose(Vector3(0f, 1f, 0f), Quaternion.fromAxisAngle(Vector3.Up, 90f)))
            // Facing -X, so stepping 1 meter along local -Z moves along world -X.
            poseHeadBy(Pose(Vector3(0f, 0f, -1f), Quaternion.Identity))
            assertVectorApproxEquals(Vector3(-1f, 1f, 0f), currentPosition)
            assertVectorApproxEquals(Vector3.Left, currentPose.forward)

            // A local yaw of 90 degrees turns from -X to +Z (looking back toward the origin).
            poseHeadBy(Pose(Vector3.Zero, Quaternion.fromAxisAngle(Vector3.Up, 90f)))
            assertVectorApproxEquals(Vector3(-1f, 1f, 0f), currentPosition)
            assertVectorApproxEquals(Vector3.Backward, currentPose.forward, 1e-3f)
        }
    }

    @Test
    fun moveHeadToAndMoveHeadBy_useWorldFrameAndPreserveRotation() {
        val device = FakeDevice()
        val yaw90 = Quaternion.fromAxisAngle(Vector3.Up, 90f)

        device.perform {
            rotateHeadTo(yaw90)
            moveHeadTo(Vector3(0.5f, 1f, 0f))
            assertVectorApproxEquals(Vector3(0.5f, 1f, 0f), currentPosition)

            // Unlike poseHeadBy, moveHeadBy ignores the facing direction.
            moveHeadBy(Vector3(0f, 0f, -1f))
            assertVectorApproxEquals(Vector3(0.5f, 1f, -1f), currentPosition)
            assertTrue(Quaternion.angle(yaw90, currentRotation) < 0.01f)
        }
    }

    @Test
    fun rotateHeadToAndRotateHeadBy_preservePositionAndRotateInLocalFrame() {
        val device = FakeDevice()

        device.perform {
            moveHeadTo(Vector3(0.5f, 1.0f, 0f))
            rotateHeadTo(Quaternion.fromAxisAngle(Vector3.Up, 90f))
            assertVectorApproxEquals(Vector3(0.5f, 1.0f, 0f), currentPosition)
            assertVectorApproxEquals(Vector3.Left, currentPose.forward)

            // A local pitch around the head's right axis looks up without adding roll.
            rotateHeadBy(Quaternion.fromAxisAngle(Vector3.Right, 30f))
            assertVectorApproxEquals(Vector3(0.5f, 1.0f, 0f), currentPosition)
            assertApproxEquals(0f, currentPose.right.y)
            assertApproxEquals(0.5f, currentPose.forward.y, tolerance = 1e-3f)
        }
    }

    @Test
    fun rotateHeadTo_yaw180_neverFormatsNegativeZero() {
        val device = FakeDevice()

        device.perform { rotateHeadTo(Quaternion.fromAxisAngle(Vector3.Up, 180f)) }

        assertTrue(device.setprops.none { it.contains("-0.00000") }, "${device.setprops}")
        assertVectorApproxEquals(Vector3.Backward, device.recordedPoses.last().forward, 1e-3f)
    }

    @Test
    fun animate_rotateHeadToFull360Sweep_rotatesThroughAllQuadrants() {
        val device = FakeDevice()

        device.perform {
            val start = currentRotation
            // 112ms / 28ms -> 4 intervals -> 0, 90, 180, 270, 360 degrees.
            animate(durationMs = 112L, easing = LinearEasing) { fraction ->
                rotateHeadTo(Quaternion.fromAxisAngle(Vector3.Up, 360f * fraction) * start)
            }
        }

        val forwards = device.recordedPoses.map { it.forward }
        assertEquals(5, forwards.size)
        assertVectorApproxEquals(Vector3.Forward, forwards[0], tolerance = 1e-3f)
        assertVectorApproxEquals(Vector3.Left, forwards[1], tolerance = 1e-3f)
        assertVectorApproxEquals(Vector3.Backward, forwards[2], tolerance = 1e-3f)
        assertVectorApproxEquals(Vector3.Right, forwards[3], tolerance = 1e-3f)
        assertVectorApproxEquals(Vector3.Forward, forwards[4], tolerance = 1e-3f)
    }

    @Test
    fun lookAt_fromRolledHead_gazesAtTargetWithZeroRoll() {
        val device = FakeDevice()
        // Yaw 90 degrees (left) and pitch 45 degrees (up) from the origin.
        val target = Vector3(-1f, 1f, 0f)

        device.perform {
            rotateHeadBy(Quaternion.fromAxisAngle(Vector3.Backward, 30f))
            assertApproxEquals(0.5f, currentPose.right.y, tolerance = 1e-3f)

            lookAt(target)
            assertVectorApproxEquals(target.toNormalized(), currentPose.forward, 1e-3f)
            assertApproxEquals(0f, currentPose.right.y)
        }
    }

    @Test
    fun lookAt_targetAtHeadPosition_keepsCurrentRotation() {
        val device = FakeDevice()
        lateinit var startRotation: Quaternion

        device.perform {
            moveHeadTo(Vector3(0.5f, 1f, 0f))
            rotateHeadBy(Quaternion.fromAxisAngle(Vector3.Backward, 30f))
            startRotation = currentRotation
            // A target at the head position has no gaze direction, so the rotation is kept.
            lookAt(currentPosition)
            assertEquals(startRotation, currentRotation)
        }
    }

    @Test
    fun lookAt_straightUp_keepsCurrentHeading() {
        val device = FakeDevice()
        val yaw90 = Quaternion.fromAxisAngle(Vector3.Up, 90f)

        device.perform {
            rotateHeadTo(yaw90)
            lookAt(currentPosition + Vector3(0f, 3f, 0f))
            assertVectorApproxEquals(Vector3.Up, currentPose.forward)
            assertVectorApproxEquals(yaw90 * Vector3.Right, currentPose.right)
        }
    }

    @Test
    fun lookAt_spatialNodeAlignment_gazesAtAlignedPointOnNode() {
        val device = FakeDevice()
        // A 1m x 0.9m panel centered 2 meters in front of the origin.
        val panel = panelNode(position = Vector3(0f, 0f, -2f))

        device.perform {
            lookAt(panel)
            assertVectorApproxEquals(Vector3.Forward, currentPose.forward)

            // The top-right corner sits at (0.5, 0.45, -2) in world space.
            lookAt(panel, alignment = SpatialAlignment.TopRight)
            assertVectorApproxEquals(
                Vector3(0.5f, 0.45f, -2f).toNormalized(),
                currentPose.forward,
                1e-3f,
            )
            assertApproxEquals(0f, currentPose.right.y)

            lookAt(panel, offset = Vector3(-0.5f, 0f, 0f))
            assertVectorApproxEquals(Vector3(-0.5f, 0f, -2f).toNormalized(), currentPose.forward)
        }
    }

    @Test
    fun lookAt_spatialNodeBoundsInWindow_gazesAtBoundsCenter() {
        val device = FakeDevice()
        val panel =
            panelNode(
                position = Vector3(0f, 0f, -2f),
                surfaceBoundsInPixels = floatArrayOf(1000f, 900f),
            )

        device.perform {
            // The right half of the window is centered at x = 750px, i.e. 0.25m right of center.
            // Rect's constructor is a no-op stub in JVM unit tests, so assign the fields directly.
            val rightHalf =
                Rect().apply {
                    left = 500
                    top = 0
                    right = 1000
                    bottom = 900
                }
            lookAt(panel, rightHalf)
            assertVectorApproxEquals(Vector3(0.25f, 0f, -2f).toNormalized(), currentPose.forward)
        }
    }

    @Test
    fun animate_composingMoveHeadToAndLookAt_locksGravityAlignedGazeEveryFrame() {
        val device = FakeDevice()
        val target = Vector3(0f, 1.2f, -1.5f)

        device.perform {
            moveHeadTo(Vector3(-1.0f, 1.2f, 0f))
            device.recordedPoses.clear()
            val start = currentPosition
            val end = Vector3(1.0f, 1.2f, 0f)
            animate(durationMs = 112L) { fraction ->
                moveHeadTo(Vector3.lerp(start, end, fraction))
                lookAt(target)
            }
        }

        assertEquals(5, device.recordedPoses.size)
        for (framePose in device.recordedPoses) {
            val expectedForward = (target - framePose.translation).toNormalized()
            assertVectorApproxEquals(expectedForward, framePose.forward, tolerance = 1e-3f)
            assertApproxEquals(0f, framePose.right.y)
        }
    }

    @Test
    fun animate_lookAtLerpedTarget_keepsEveryFrameGravityAligned() {
        val device = FakeDevice()

        device.perform {
            rotateHeadBy(Quaternion.fromAxisAngle(Vector3.Backward, 30f))
            device.recordedPoses.clear()
            val startTarget = currentPosition + currentPose.forward
            val endTarget = Vector3(-1f, 1f, 0f)
            animate(durationMs = 112L, easing = LinearEasing) { fraction ->
                lookAt(Vector3.lerp(startTarget, endTarget, fraction))
            }
        }

        // Every frame passes through lookAt, so the starting roll is discarded immediately.
        assertEquals(5, device.recordedPoses.size)
        for (framePose in device.recordedPoses) {
            assertApproxEquals(0f, framePose.right.y, tolerance = 1e-3f)
        }
        assertVectorApproxEquals(
            Vector3(-1f, 1f, 0f).toNormalized(),
            device.recordedPoses.last().forward,
            1e-3f,
        )
    }

    @Test
    fun resetOnCloseTrue_whenBlockAndCleanupThrow_attachesCleanupFailureAsSuppressed() {
        val cleanupFailure = IllegalStateException("Failed to set property")
        val failure = IllegalStateException("Simulated assertion failure")

        val thrown =
            assertFailsWith<IllegalStateException> {
                performHeadMovementInternal(
                    resetOnClose = true,
                    settleDelayMs = 0L,
                    shellExecutor = { cmd ->
                        if (cmd.startsWith("setprop ")) throw cleanupFailure else ""
                    },
                    sleeper = {},
                    clockMs = { 0L },
                    block = { throw failure },
                )
            }

        assertSame(failure, thrown)
        assertSame(cleanupFailure, thrown.suppressed.single())
    }

    @Test
    fun resetOnCloseTrue_resetsAfterCompletionAndPropagatesOriginalFailure() {
        val device = FakeDevice()

        device.perform(resetOnClose = true) { moveHeadTo(Vector3(0.5f, 0.2f, -0.3f)) }

        assertEquals(
            listOf(
                "setprop cpm.fake_het_position 0.50000,0.20000,-0.30000",
                "setprop cpm.fake_het_orientation 0.00000,0.00000,0.00000,1.00000",
                "setprop cpm.fake_het_position 0.00000,0.00000,0.00000",
                "setprop cpm.fake_het_orientation 0.00000,0.00000,0.00000,1.00000",
            ),
            device.setprops,
        )

        val failingDevice = FakeDevice()
        val failure = IllegalStateException("Simulated assertion failure")
        val thrown =
            assertFailsWith<IllegalStateException> {
                failingDevice.perform(resetOnClose = true) {
                    moveHeadTo(Vector3(1f, 0f, 0f))
                    throw failure
                }
            }
        assertSame(failure, thrown)
        assertEquals(
            "setprop cpm.fake_het_position 0.00000,0.00000,0.00000",
            failingDevice.setprops[2],
        )
    }

    @Test
    fun requireNoShellError_ignoresBlankAndWarningLinesAndThrowsOnErrors() {
        requireNoShellError("getprop foo", "")
        requireNoShellError("getprop foo", "\nWARNING: linker: some message\n")

        val thrown =
            assertFailsWith<IllegalStateException> {
                requireNoShellError(
                    "setprop foo bar",
                    "WARNING: linker: some message\nFailed to set property 'foo' to 'bar'.\n",
                )
            }
        assertTrue(thrown.message!!.contains("setprop foo bar"))
        assertTrue(thrown.message!!.contains("Failed to set property 'foo' to 'bar'."))
        assertFalse(thrown.message!!.contains("WARNING"))
    }

    @Test
    fun gravityAlignedLookRotation_pointsMinusZAtTargetWithZeroRoll() {
        val forwardRotation = gravityAlignedLookRotation(Vector3.Zero, Vector3(0f, 0f, -2f))
        assertTrue(Quaternion.angle(Quaternion.Identity, forwardRotation) < 0.01f)

        val headPosition = Vector3(-0.5f, 1.2f, 0.4f)
        val targetPoint = Vector3(1.1f, 1.9f, -1.3f)
        val diagonalPose = Pose(headPosition, gravityAlignedLookRotation(headPosition, targetPoint))

        assertVectorApproxEquals(
            (targetPoint - headPosition).toNormalized(),
            diagonalPose.forward,
        )
        assertApproxEquals(0f, diagonalPose.right.y)
        assertTrue(diagonalPose.up.y > 0f)
    }

    @Test
    fun gravityAlignedLookRotation_nearVerticalGaze_keepsZeroRollAndTargetHeading() {
        // 88 degrees up toward -X, with a fallback heading that faces -Z.
        val pitch = Math.toRadians(88.0)
        val gaze = Vector3(-cos(pitch).toFloat(), sin(pitch).toFloat(), 0f)

        val pose =
            Pose(Vector3.Zero, gravityAlignedLookRotation(Vector3.Zero, gaze, Quaternion.Identity))

        assertApproxEquals(0f, pose.right.y, tolerance = 1e-5f)
        assertVectorApproxEquals(gaze, pose.forward, tolerance = 1e-4f)
    }

    @Test
    fun gravityAlignedLookRotation_coincidentAndVerticalPoleCases_preserveHeading() {
        val fallbackYaw90 = Quaternion.fromAxisAngle(Vector3.Up, 90f)

        assertEquals(
            fallbackYaw90,
            gravityAlignedLookRotation(Vector3(1f, 2f, 3f), Vector3(1f, 2f, 3f), fallbackYaw90),
        )

        val downPose =
            Pose(
                Vector3.Zero,
                gravityAlignedLookRotation(Vector3.Zero, Vector3(0f, -3f, 0f), fallbackYaw90),
            )
        assertVectorApproxEquals(Vector3.Down, downPose.forward)
        assertVectorApproxEquals(fallbackYaw90 * Vector3.Right, downPose.right)
    }

    @Test
    fun validation_invalidInputs_throwIllegalArgumentException() {
        val device = FakeDevice()
        assertFailsWith<IllegalArgumentException> {
            device.perform { moveHeadTo(Vector3(Float.NaN, 0f, 0f)) }
        }
        assertFailsWith<IllegalArgumentException> {
            device.perform { moveHeadBy(Vector3(0f, Float.POSITIVE_INFINITY, 0f)) }
        }
        assertFailsWith<IllegalArgumentException> {
            device.perform { poseHeadBy(Pose(Vector3(0f, 0f, Float.NaN), Quaternion.Identity)) }
        }
        assertFailsWith<IllegalArgumentException> {
            device.perform { lookAt(Vector3(Float.NaN, 0f, 0f)) }
        }
        assertFailsWith<IllegalArgumentException> {
            device.perform {
                lookAt(panelNode(Vector3(0f, 0f, -2f)), offset = Vector3.One * Float.NaN)
            }
        }
        assertFailsWith<IllegalArgumentException> { device.perform(settleDelayMs = -1L) {} }
        assertFailsWith<IllegalArgumentException> {
            resetHeadMovementInternal(settleDelayMs = -1L, device.shellExecutor, device.sleeper)
        }
        assertFailsWith<IllegalArgumentException> { device.perform { animate(-1L) {} } }
        assertTrue(device.setprops.isEmpty(), "${device.setprops}")
    }

    /** Creates an unrotated 1m x 0.9m panel [SpatialNode] centered at [position]. */
    private fun panelNode(
        position: Vector3,
        surfaceBoundsInPixels: FloatArray? = null,
    ): SpatialNode =
        SpatialNode(
            name = "TestPanel",
            attributes =
                buildMap {
                    put("ImpNodeWorldPosition", floatArrayOf(position.x, position.y, position.z))
                    put("ImpNodeWorldRotation", floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f))
                    put("ImpNodeWorldScale", floatArrayOf(1.0f, 1.0f, 1.0f))
                    put("root2DSurfaceBoundsInMeters", floatArrayOf(1.0f, 0.9f))
                    if (surfaceBoundsInPixels != null) {
                        put("root2DSurfaceBoundsInPixels", surfaceBoundsInPixels)
                    }
                },
        )

    /**
     * Fake device that serves `getprop` from initial values, records `setprop` commands, and
     * advances a fake clock for sleeps and simulated shell latency.
     */
    private class FakeDevice(
        private val initialPosition: String = "",
        private val initialOrientation: String = "",
        private val setpropLatencyMs: Long = 0L,
    ) {
        val setprops = mutableListOf<String>()
        val sleeps = mutableListOf<Long>()
        val recordedPoses = mutableListOf<Pose>()
        private var clockMs = 1_000L
        private var latestPosition = Vector3.Zero

        val shellExecutor: (String) -> String = { cmd ->
            assertNoShellMetacharacters(cmd)
            when {
                cmd == "getprop $PROP_FAKE_HET_POSITION" -> initialPosition
                cmd == "getprop $PROP_FAKE_HET_ORIENTATION" -> initialOrientation
                cmd.startsWith("setprop ") -> {
                    setprops.add(cmd)
                    clockMs += setpropLatencyMs
                    recordPose(cmd)
                    ""
                }
                else -> ""
            }
        }

        val sleeper: (Long) -> Unit = { ms ->
            sleeps.add(ms)
            clockMs += ms
        }

        fun perform(
            resetOnClose: Boolean = false,
            settleDelayMs: Long = 0L,
            block: HeadMovementScope.() -> Unit,
        ) {
            performHeadMovementInternal(
                resetOnClose = resetOnClose,
                settleDelayMs = settleDelayMs,
                shellExecutor = shellExecutor,
                sleeper = sleeper,
                clockMs = { clockMs },
                block = block,
            )
        }

        /** Records a pose snapshot whenever the orientation property is written. */
        private fun recordPose(cmd: String) {
            val values = cmd.substringAfterLast(' ').split(',').map(String::toFloat)
            if (cmd.startsWith("setprop $PROP_FAKE_HET_POSITION ")) {
                latestPosition = Vector3(values[0], values[1], values[2])
            } else if (cmd.startsWith("setprop $PROP_FAKE_HET_ORIENTATION ")) {
                val rotation = Quaternion(values[0], values[1], values[2], values[3])
                recordedPoses.add(Pose(latestPosition, rotation))
            }
        }

        private fun assertNoShellMetacharacters(cmd: String) {
            // UiAutomation.executeShellCommand runs commands without a shell, so commands must
            // never rely on shell chaining, subshells, or quoting.
            assertFalse(cmd.contains(';'), "Command must not contain ';': $cmd")
            assertFalse(cmd.contains("$("), "Command must not contain '$(': $cmd")
            assertFalse(cmd.contains('"'), "Command must not contain quotes: $cmd")
        }
    }

    private fun assertApproxEquals(expected: Float, actual: Float, tolerance: Float = 1e-4f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "Expected $expected (+/- $tolerance), actual $actual",
        )
    }

    private fun assertVectorApproxEquals(
        expected: Vector3,
        actual: Vector3,
        tolerance: Float = 1e-4f,
    ) {
        assertTrue(
            Vector3.distance(expected, actual) <= tolerance,
            "Expected $expected (+/- $tolerance), actual $actual",
        )
    }
}
