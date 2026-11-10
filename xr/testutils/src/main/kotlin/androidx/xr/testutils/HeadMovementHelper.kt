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

@file:JvmName("HeadMovementHelper")

package androidx.xr.testutils

import android.app.UiAutomation
import android.graphics.Rect
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import java.util.Locale
import kotlin.math.abs

/** Default settle delay in milliseconds (~3 frames at 60Hz) after updating the head pose. */
public const val DEFAULT_HEAD_SETTLE_DELAY_MS: Long = 50L

/** Default duration in milliseconds for [HeadMovementScope.animate]. */
public const val DEFAULT_HEAD_MOVEMENT_DURATION_MS: Long = 500L

/** System property specifying the simulated head position (`x,y,z` in meters). */
internal const val PROP_FAKE_HET_POSITION: String = "cpm.fake_het_position"

/** System property specifying the simulated head orientation (`qx,qy,qz,qw`). */
internal const val PROP_FAKE_HET_ORIENTATION: String = "cpm.fake_het_orientation"

/** Interval in milliseconds between interpolated frames (~36 frames per second). */
internal const val HEAD_MOVEMENT_FRAME_INTERVAL_MS: Long = 28L

/**
 * Receiver scope for simulated head movements executed by
 * [FakeHeadTrackingRule.performHeadMovement].
 *
 * Drives the user's simulated 6DoF head pose through CPM fake head tracking, which moves the
 * viewpoint that the whole scene is rendered from. This differs from [performSpatialInteraction]
 * with [SpatialDeviceType.Head], which injects a head-gaze pointer ray for input without moving the
 * viewpoint. Use both together to, for example, walk up to a panel and then select it.
 *
 * Like [SpatialInteractionScope], every movement applies immediately, and [animate] drives timed
 * transitions by calling movements with an interpolated fraction:
 * ```kotlin
 * fakeHeadTrackingRule.performHeadMovement {
 *     val start = currentPosition
 *     animate(durationMs = 600L) { fraction ->
 *         moveHeadTo(Vector3.lerp(start, Vector3(0.5f, 0f, -0.5f), fraction))
 *     }
 *     lookAt(panelNode)
 * }
 * ```
 *
 * All coordinates operate in OpenXR world space (`+X` right, `+Y` up, `-Z` forward), the same space
 * as [SpatialNode.transformPointToWorld].
 *
 * Position and orientation are written as separate system properties, and only the values that
 * changed are written. A frame that changes both can briefly expose the new position with the
 * previous orientation; the final pose of every movement is always consistent.
 */
public interface HeadMovementScope {

    // =========================================================================================
    // Abstract state & primitive actions (must be provided by implementations)
    // =========================================================================================

    /** Current simulated head [Pose] in OpenXR world space. */
    public val currentPose: Pose

    /**
     * Immediately moves the simulated head to [pose], updating position and rotation together.
     *
     * @param pose target head [Pose] in OpenXR world space
     * @throws IllegalArgumentException if [pose] is not finite
     */
    public fun poseHeadTo(pose: Pose)

    /**
     * Drives an animation loop over [durationMs] using [easing], passing the normalized progression
     * [fraction] (`[0.0, 1.0]`) to [block] at each frame.
     *
     * Movements issued from [block] are applied immediately, and the final frame always runs with a
     * [fraction] of exactly `1.0`. The settle delay is applied once after the final frame. Use
     * [fraction] to interpolate absolute targets captured before the animation, for example:
     * ```kotlin
     * val start = currentPose
     * animate { fraction -> poseHeadTo(Pose.lerp(start, end, fraction)) }
     * ```
     *
     * @param durationMs total duration of the animation in milliseconds (default:
     *   [DEFAULT_HEAD_MOVEMENT_DURATION_MS])
     * @param easing motion curve applied to the interpolation (default: [FastOutSlowInEasing])
     * @param block receiver block run at each animated frame with the current [fraction]
     * @throws IllegalArgumentException if [durationMs] is negative
     */
    public fun animate(
        durationMs: Long = DEFAULT_HEAD_MOVEMENT_DURATION_MS,
        easing: Easing = FastOutSlowInEasing,
        block: HeadMovementScope.(fraction: Float) -> Unit,
    )

    // =========================================================================================
    // Derived properties & default convenience methods
    // =========================================================================================

    /** Current simulated head position in meters in OpenXR world space. */
    public val currentPosition: Vector3
        get() = currentPose.translation

    /** Current simulated head orientation in OpenXR world space. */
    public val currentRotation: Quaternion
        get() = currentPose.rotation

    /**
     * Immediately applies [delta] relative to the head, composing it onto [currentPose] like
     * [Pose.compose].
     *
     * [delta]'s translation is expressed in the head's local frame (`-Z` is the facing direction),
     * so `poseHeadBy(Pose(Vector3(0f, 0f, -0.5f), Quaternion.Identity))` steps 0.5 meters forward
     * along the current gaze.
     *
     * @param delta relative [Pose] in the head's local frame
     * @throws IllegalArgumentException if [delta] is not finite
     */
    public fun poseHeadBy(delta: Pose) {
        poseHeadTo(currentPose.compose(delta))
    }

    /**
     * Immediately moves the simulated head to [position], preserving [currentRotation].
     *
     * @param position target head position in meters in OpenXR world space
     * @throws IllegalArgumentException if [position] is not finite
     */
    public fun moveHeadTo(position: Vector3) {
        poseHeadTo(Pose(position, currentRotation))
    }

    /**
     * Immediately displaces the simulated head by [delta] in OpenXR world space, preserving
     * [currentRotation], like [Pose.translate].
     *
     * To move relative to the head's facing direction, use [poseHeadBy] instead.
     *
     * @param delta translation vector in meters in OpenXR world space
     * @throws IllegalArgumentException if [delta] is not finite
     */
    public fun moveHeadBy(delta: Vector3) {
        poseHeadTo(currentPose.translate(delta))
    }

    /**
     * Immediately rotates the simulated head to [rotation], preserving [currentPosition].
     *
     * Combine with [animate] for continuous sweeps of any angle. For example, a 360-degree room
     * scan around world [Vector3.Up] that keeps the starting pitch:
     * ```kotlin
     * val start = currentRotation
     * animate(durationMs = 2_000L, easing = LinearEasing) { fraction ->
     *     rotateHeadTo(Quaternion.fromAxisAngle(Vector3.Up, 360f * fraction) * start)
     * }
     * ```
     *
     * Multiplying on the right instead (`start * Quaternion.fromAxisAngle(...)`) rotates around the
     * head's local axis.
     *
     * @param rotation target head orientation in OpenXR world space
     * @throws IllegalArgumentException if [rotation] is not finite
     */
    public fun rotateHeadTo(rotation: Quaternion) {
        poseHeadTo(Pose(currentPosition, rotation))
    }

    /**
     * Immediately rotates the simulated head by [delta] around the head's local axes, preserving
     * [currentPosition], like [Pose.rotate] (`currentRotation * delta`).
     *
     * @param delta relative rotation in the head's local frame
     * @throws IllegalArgumentException if [delta] is not finite
     */
    public fun rotateHeadBy(delta: Quaternion) {
        poseHeadTo(currentPose.rotate(delta))
    }

    /**
     * Immediately rotates the simulated head at [currentPosition] to gaze at [targetPoint] while
     * staying gravity-aligned (zero roll).
     *
     * Keeps [currentRotation] when [targetPoint] coincides with [currentPosition], and keeps its
     * horizontal heading when gazing straight up or straight down.
     *
     * @param targetPoint 3D point in meters in OpenXR world space
     * @throws IllegalArgumentException if [targetPoint] is not finite
     */
    public fun lookAt(targetPoint: Vector3) {
        rotateHeadTo(gravityAlignedLookRotation(currentPosition, targetPoint, currentRotation))
    }

    /**
     * Immediately rotates the simulated head to gaze at an aligned point on [targetNode] while
     * staying gravity-aligned (zero roll).
     *
     * Uses the same [offset] and [alignment] conventions as [SpatialInteractionScope.pointAt].
     *
     * @param targetNode the target [SpatialNode]
     * @param offset translation offset in meters in [targetNode]-local space (default:
     *   [Vector3.Zero])
     * @param alignment normalized alignment within [targetNode]'s half-extents (default:
     *   [SpatialAlignment.Center])
     * @throws IllegalArgumentException if the resolved target point is not finite
     */
    public fun lookAt(
        targetNode: SpatialNode,
        offset: Vector3 = Vector3.Zero,
        alignment: SpatialAlignment = SpatialAlignment.Center,
    ) {
        lookAt(calculateAlignedPoint(targetNode, alignment, offset))
    }

    /**
     * Immediately rotates the simulated head to gaze at the center of a 2D View / UIAutomator
     * [boundsInWindow] on [targetNode] while staying gravity-aligned (zero roll).
     *
     * @param targetNode the panel or window [SpatialNode] hosting the 2D content
     * @param boundsInWindow screen/window pixel bounding rectangle
     */
    public fun lookAt(targetNode: SpatialNode, boundsInWindow: Rect) {
        lookAt(targetNode, offset = calculateWindowBoundsOffset(targetNode, boundsInWindow))
    }
}

/**
 * Computes a gravity-aligned head orientation (zero roll relative to [Vector3.Up]) that points the
 * head's `-Z` forward gaze from [headPosition] toward [targetPoint].
 *
 * Returns [fallbackRotation] when [headPosition] and [targetPoint] coincide. Gazes within about
 * 0.06 degrees of vertical have no usable horizontal heading and keep the heading of
 * [fallbackRotation]; every other gaze has exactly zero roll.
 *
 * @param headPosition head position in meters
 * @param targetPoint target point in meters to gaze toward
 * @param fallbackRotation orientation used when [headPosition] equals [targetPoint] and to resolve
 *   the heading at vertical poles
 * @return gravity-aligned head orientation
 * @throws IllegalArgumentException if any input component is not finite
 */
internal fun gravityAlignedLookRotation(
    headPosition: Vector3,
    targetPoint: Vector3,
    fallbackRotation: Quaternion = Quaternion.Identity,
): Quaternion {
    requireFiniteVector(headPosition, "headPosition")
    requireFiniteVector(targetPoint, "targetPoint")
    requireFiniteQuaternion(fallbackRotation, "fallbackRotation")

    val gaze = targetPoint - headPosition
    if (gaze.lengthSquared < 1e-8f) {
        return fallbackRotation
    }
    // Pose.forward is local -Z, while Quaternion.fromLookTowards aligns local +Z (Backward).
    val backward = -(gaze.toNormalized())
    // Matches the degenerate-cross-product threshold of Quaternion.fromLookTowards, so only a gaze
    // within about 0.06 degrees of vertical, which has no usable heading, uses the fallback.
    if (backward.x * backward.x + backward.z * backward.z < 1e-6f) {
        val fallbackRight = fallbackRotation * Vector3.Right
        val horizontalRight = Vector3(fallbackRight.x, 0f, fallbackRight.z)
        val right =
            if (horizontalRight.lengthSquared > 1e-6f) {
                horizontalRight.toNormalized()
            } else {
                Vector3.Right
            }
        val poleUp = (backward cross right).toNormalized()
        return Quaternion.fromLookTowards(backward, poleUp)
    }
    return Quaternion.fromLookTowards(backward, Vector3.Up)
}

// =========================================================================================
// Testable internal implementations
// =========================================================================================

@VisibleForTesting
internal fun readSimulatedHeadPoseInternal(shellExecutor: (String) -> String): Pose {
    val (position, orientation) = readSimulatedHeadProperties(shellExecutor)
    return Pose(position ?: Vector3.Zero, orientation ?: Quaternion.Identity)
}

/** Reads the simulated head position and orientation, or `null` for unset or malformed values. */
private fun readSimulatedHeadProperties(
    shellExecutor: (String) -> String
): Pair<Vector3?, Quaternion?> =
    parsePositionProperty(readSystemProperty(shellExecutor, PROP_FAKE_HET_POSITION)) to
        parseOrientationProperty(readSystemProperty(shellExecutor, PROP_FAKE_HET_ORIENTATION))

@VisibleForTesting
internal fun resetHeadMovementInternal(
    settleDelayMs: Long,
    shellExecutor: (String) -> String,
    sleeper: (Long) -> Unit,
) {
    requireNonNegativeSettleDelay(settleDelayMs)
    shellExecutor("setprop $PROP_FAKE_HET_POSITION ${formatPosition(Vector3.Zero)}")
    shellExecutor("setprop $PROP_FAKE_HET_ORIENTATION ${formatOrientation(Quaternion.Identity)}")
    if (settleDelayMs > 0L) {
        sleeper(settleDelayMs)
    }
}

@VisibleForTesting
internal fun performHeadMovementInternal(
    resetOnClose: Boolean,
    settleDelayMs: Long,
    shellExecutor: (String) -> String,
    sleeper: (Long) -> Unit,
    clockMs: () -> Long,
    block: HeadMovementScope.() -> Unit,
) {
    requireNonNegativeSettleDelay(settleDelayMs)
    val (devicePosition, deviceOrientation) = readSimulatedHeadProperties(shellExecutor)
    val scope =
        HeadMovementScopeImpl(
            initialPose =
                Pose(devicePosition ?: Vector3.Zero, deviceOrientation ?: Quaternion.Identity),
            // Values already on the device are not rewritten; unset or malformed ones are.
            initialPositionValue = devicePosition?.let(::formatPosition),
            initialOrientationValue = deviceOrientation?.let(::formatOrientation),
            settleDelayMs = settleDelayMs,
            shellExecutor = shellExecutor,
            sleeper = sleeper,
            clockMs = clockMs,
        )
    try {
        scope.block()
    } catch (t: Throwable) {
        if (resetOnClose) {
            // Preserve the original failure if the cleanup shell command also fails.
            runCatching { resetHeadMovementInternal(settleDelayMs, shellExecutor, sleeper) }
                .exceptionOrNull()
                ?.let(t::addSuppressed)
        }
        throw t
    }
    if (resetOnClose) {
        resetHeadMovementInternal(settleDelayMs, shellExecutor, sleeper)
    }
}

/** Default internal implementation of [HeadMovementScope]. */
private class HeadMovementScopeImpl(
    initialPose: Pose,
    initialPositionValue: String?,
    initialOrientationValue: String?,
    private val settleDelayMs: Long,
    private val shellExecutor: (String) -> String,
    private val sleeper: (Long) -> Unit,
    private val clockMs: () -> Long,
) : HeadMovementScope {

    override var currentPose: Pose = initialPose
        private set

    private var lastPositionValue: String? = initialPositionValue
    private var lastOrientationValue: String? = initialOrientationValue
    private var animationDepth: Int = 0

    override fun poseHeadTo(pose: Pose) {
        requireFinitePose(pose, "pose")
        writePose(pose)
        settleIfIdle()
    }

    override fun animate(
        durationMs: Long,
        easing: Easing,
        block: HeadMovementScope.(fraction: Float) -> Unit,
    ) {
        require(durationMs >= 0L) { "durationMs must be non-negative, was $durationMs." }
        animationDepth++
        try {
            if (durationMs > 0L) {
                // Sample frames on a fixed time grid and skip slots that were missed, so shell
                // latency does not stretch the overall duration.
                val startMs = clockMs()
                val endMs = startMs + durationMs
                var nowMs = startMs
                var nextMs = startMs
                while (nowMs < endMs) {
                    block(easing.transform((nowMs - startMs).toFloat() / durationMs))
                    nowMs = clockMs()
                    while (nextMs <= nowMs) nextMs += HEAD_MOVEMENT_FRAME_INTERVAL_MS
                    val waitMs = minOf(nextMs, endMs) - nowMs
                    if (waitMs > 0L) {
                        sleeper(waitMs)
                        nowMs = clockMs()
                    }
                }
            }
            block(1f)
        } finally {
            animationDepth--
        }
        settleIfIdle()
    }

    private fun writePose(pose: Pose) {
        val positionValue = formatPosition(pose.translation)
        val orientationValue = formatOrientation(pose.rotation)
        // Skip redundant setprop calls to keep per-frame shell latency low.
        if (positionValue != lastPositionValue) {
            shellExecutor("setprop $PROP_FAKE_HET_POSITION $positionValue")
            lastPositionValue = positionValue
        }
        if (orientationValue != lastOrientationValue) {
            shellExecutor("setprop $PROP_FAKE_HET_ORIENTATION $orientationValue")
            lastOrientationValue = orientationValue
        }
        currentPose = pose
    }

    /** Settles after standalone movements, but not after each frame inside [animate]. */
    private fun settleIfIdle() {
        if (animationDepth == 0 && settleDelayMs > 0L) {
            sleeper(settleDelayMs)
        }
    }
}

// =========================================================================================
// Private geometry & property formatting helpers
// =========================================================================================

internal fun readSystemProperty(shellExecutor: (String) -> String, propertyName: String): String =
    shellExecutor("getprop $propertyName")
        .lineSequence()
        .map { it.trim() }
        .lastOrNull { it.isNotEmpty() && !it.startsWith("WARNING:") }
        .orEmpty()

private fun parsePositionProperty(raw: String): Vector3? {
    val values = parseFloatArray(raw, expectedSize = 3) ?: return null
    return Vector3(values[0], values[1], values[2])
}

private fun parseOrientationProperty(raw: String): Quaternion? {
    val values = parseFloatArray(raw, expectedSize = 4) ?: return null
    return Quaternion(values[0], values[1], values[2], values[3])
}

private fun parseFloatArray(raw: String, expectedSize: Int): FloatArray? {
    if (raw.isBlank()) return null
    val parts = raw.split(',')
    if (parts.size != expectedSize) return null
    val values = FloatArray(expectedSize)
    for (i in parts.indices) {
        values[i] = parts[i].trim().toFloatOrNull()?.takeIf(Float::isFinite) ?: return null
    }
    return values
}

private fun formatPosition(position: Vector3): String =
    String.format(
        Locale.US,
        "%.5f,%.5f,%.5f",
        normalizeNegativeZero(position.x),
        normalizeNegativeZero(position.y),
        normalizeNegativeZero(position.z),
    )

private fun formatOrientation(rotation: Quaternion): String =
    String.format(
        Locale.US,
        "%.5f,%.5f,%.5f,%.5f",
        normalizeNegativeZero(rotation.x),
        normalizeNegativeZero(rotation.y),
        normalizeNegativeZero(rotation.z),
        normalizeNegativeZero(rotation.w),
    )

private fun requireFinitePose(pose: Pose, name: String) {
    requireFiniteVector(pose.translation, "$name.translation")
    requireFiniteQuaternion(pose.rotation, "$name.rotation")
}

private fun requireFiniteVector(vector: Vector3, name: String) {
    requireFinite(vector.x, "$name.x")
    requireFinite(vector.y, "$name.y")
    requireFinite(vector.z, "$name.z")
}

private fun requireFiniteQuaternion(quaternion: Quaternion, name: String) {
    requireFinite(quaternion.x, "$name.x")
    requireFinite(quaternion.y, "$name.y")
    requireFinite(quaternion.z, "$name.z")
    requireFinite(quaternion.w, "$name.w")
}

private fun requireFinite(value: Float, name: String) {
    require(value.isFinite()) { "$name must be finite, was $value." }
}

/** Maps values that would format as `-0.00000` (including `-0f`) to `0f`. */
private fun normalizeNegativeZero(value: Float): Float = if (abs(value) < 5e-6f) 0f else value

private fun requireNonNegativeSettleDelay(settleDelayMs: Long) {
    require(settleDelayMs >= 0L) { "settleDelayMs must be non-negative, was $settleDelayMs." }
}

/**
 * Runs [command] via [uiAutomation] and returns its stdout.
 *
 * Unlike [executeShellCommand], failures are never swallowed: `setprop` reports errors such as
 * SELinux denials only on stderr, so any non-warning stderr output is treated as a failure.
 *
 * @throws IllegalStateException if the command writes an error to stderr
 * @throws UnsupportedOperationException if the device runs below API 34
 */
internal fun runShellCommandOrThrow(command: String, uiAutomation: UiAutomation): String {
    val (stdout, stdin, stderr) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            uiAutomation.executeShellCommandRwe(command)
        } else {
            throw UnsupportedOperationException("Head movement simulation requires API 34+.")
        }
    // Wrap every stream up front so all file descriptors close even if an earlier step throws.
    val stdoutStream = ParcelFileDescriptor.AutoCloseInputStream(stdout)
    val stderrStream = ParcelFileDescriptor.AutoCloseInputStream(stderr)
    val (output, error) =
        stdoutStream.use {
            stderrStream.use {
                stdin.close()
                stdoutStream.bufferedReader().readText() to stderrStream.bufferedReader().readText()
            }
        }
    requireNoShellError(command, error)
    return output
}

/**
 * Throws if [stderr] of [command] contains anything other than blank lines or `WARNING:` lines
 * (such as linker warnings).
 */
@VisibleForTesting
internal fun requireNoShellError(command: String, stderr: String) {
    val error =
        stderr
            .lineSequence()
            .map { it.trim() }
            .filterNot { it.isEmpty() || it.startsWith("WARNING:") }
            .joinToString("\n")
    check(error.isEmpty()) { "Shell command '$command' failed: $error" }
}

internal fun monotonicClockMs(): Long =
    try {
        SystemClock.uptimeMillis()
    } catch (_: RuntimeException) {
        System.nanoTime() / 1_000_000L
    }
