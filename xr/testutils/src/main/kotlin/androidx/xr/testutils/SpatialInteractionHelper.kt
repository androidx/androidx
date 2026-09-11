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

@file:JvmName("SpatialInteractionHelper")

package androidx.xr.testutils

import android.app.Instrumentation
import android.app.UiAutomation
import android.graphics.Rect
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.math.abs

public const val DEFAULT_RAY_DISTANCE: Float = 2.0f
public const val DEFAULT_DELAY_MS: Long = 100L
public const val DEFAULT_DRAG_DURATION_MS: Long = 500L

/** Default distance in meters outside the panel outer edge for the move affordance. */
public const val DEFAULT_MOVE_HANDLE_MARGIN: Float = 0.05f

/** Default binary name for the XR raycast injection CLI. */
public const val CLI_BINARY_NAME: String = "xr_inject_input_cli"

/** Default system path for the XR raycast injection CLI. */
public const val CLI_SYSTEM_PATH: String = "/system/bin/xr_inject_input_cli"

/** Temporary install path in `/data/local/tmp` for emulator or fallback execution. */
public const val CLI_TMP_PATH: String = "/data/local/tmp/xr_inject_input_cli"

@Volatile private var resolvedCliPath: String? = null

/**
 * Performs a series of spatial raycast interactions targeting this [SpatialNode] within a scoped
 * [SpatialInteractionScope].
 *
 * Scopes all relative coordinates, ray origins, targets, and displacements directly within this
 * [SpatialNode]'s local coordinate frame.
 *
 * Automatically dispatches a system input reset upon scope completion or failure, ensuring clean
 * state isolation across tests.
 *
 * @param deviceType The [SpatialDeviceType] for this interaction (default:
 *   [SpatialDeviceType.Controller]).
 * @param pointerType The [SpatialPointerType] for this interaction (default:
 *   [SpatialPointerType.Left]).
 * @param uiAutomation The [UiAutomation] instance to execute shell commands with.
 * @param block The interaction lambda executed in [SpatialInteractionScope].
 */
public fun SpatialNode.performSpatialInteraction(
    deviceType: SpatialDeviceType = SpatialDeviceType.Controller,
    pointerType: SpatialPointerType = SpatialPointerType.Left,
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    block: SpatialInteractionScope.() -> Unit,
) {
    SpatialInteractionScopeImpl(
            node = this,
            deviceType = deviceType,
            pointerType = pointerType,
            uiAutomation = uiAutomation,
        )
        .use { it.block() }
}

/**
 * Performs a series of spatial raycast interactions within this [SpatialTask]'s activity space.
 *
 * Automatically scopes all coordinates and ray operations relative to this task's
 * [SpatialTask.activitySpaceRoot].
 *
 * @param deviceType The [SpatialDeviceType] for this interaction (default:
 *   [SpatialDeviceType.Controller]).
 * @param pointerType The [SpatialPointerType] for this interaction (default:
 *   [SpatialPointerType.Left]).
 * @param uiAutomation The [UiAutomation] instance to execute shell commands with.
 * @param block The interaction lambda executed in [SpatialInteractionScope].
 */
public fun SpatialTask.performSpatialInteraction(
    deviceType: SpatialDeviceType = SpatialDeviceType.Controller,
    pointerType: SpatialPointerType = SpatialPointerType.Left,
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    block: SpatialInteractionScope.() -> Unit,
) {
    SpatialInteractionScopeImpl(
            node = activitySpaceRoot,
            deviceType = deviceType,
            pointerType = pointerType,
            uiAutomation = uiAutomation,
        )
        .use { it.block() }
}

/**
 * Performs a single discrete ray action event defined by [ray] via [uiAutomation].
 *
 * @param ray The [Ray] defining origin and direction.
 * @param action Input action phase.
 * @param deviceType Device type.
 * @param pointerType Pointer type.
 * @param uiAutomation The [UiAutomation] instance to execute shell commands with.
 */
@JvmOverloads
public fun performRayAction(
    ray: Ray,
    action: SpatialRayAction = SpatialRayAction.Up,
    deviceType: SpatialDeviceType = SpatialDeviceType.Hands,
    pointerType: SpatialPointerType = SpatialPointerType.Left,
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
): String {
    val cliPath = getOrInstallCli(uiAutomation)
    val endPoint = ray.origin + ray.direction
    val command =
        "$cliPath ${ray.origin.x} ${ray.origin.y} ${ray.origin.z} " +
            "${endPoint.x} ${endPoint.y} ${endPoint.z} " +
            "${action.value} ${deviceType.value} ${pointerType.value}"
    return executeShellCommand(command, uiAutomation)
}

/**
 * Resets any active or overridden injected raycast state in the XR input subsystem via
 * [uiAutomation].
 *
 * Invokes the XR raycast injection CLI with the `stop` argument to clear active injected raycasts
 * and restore standard input handling.
 *
 * @param uiAutomation The [UiAutomation] instance to execute shell commands with.
 * @return The output string returned by the CLI command.
 */
@JvmOverloads
public fun resetSpatialInteraction(
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
): String {
    return try {
        val cliPath = getOrInstallCli(uiAutomation)
        val command = "$cliPath stop"
        executeShellCommand(command, uiAutomation)
    } catch (e: Exception) {
        Log.w("SpatialInteractionHelper", "Could not execute CLI reset: ${e.message}")
        ""
    }
}

/**
 * Represents the action phase for spatial raycast input events (down/press vs up/release).
 *
 * @property value The raw integer code transmitted to the underlying input injection binary.
 */
public enum class SpatialRayAction(public val value: Int) {
    /** Release / Hover phase (corresponds to MotionEvent.ACTION_UP / 0). */
    Up(0),

    /** Press / Pinch phase (corresponds to MotionEvent.ACTION_DOWN / 1). */
    Down(1),
}

/**
 * Represents the type of XR device or tracking source used for spatial raycast input events.
 *
 * @property value The raw integer code transmitted to the underlying input injection binary.
 */
public enum class SpatialDeviceType(public val value: Int) {
    /** Unspecified or unknown device type (falls back to system default routing). */
    Unknown(0),

    /** 6DoF Motion Controller input source. */
    Controller(1),

    /** Eye tracking / gaze input source. */
    Eyes(2),

    /** Hand tracking / pinch raycast input source. */
    Hands(3),

    /** Head pose / head gaze input source. */
    Head(4),
}

/**
 * Represents the handedness / pointer side for spatial raycast input events.
 *
 * @property value The raw integer code transmitted to the underlying input injection binary.
 */
public enum class SpatialPointerType(public val value: Int) {
    /** Left hand / pointer side. */
    Left(0),

    /** Right hand / pointer side. */
    Right(1),
}

/**
 * Represents 2D/3D spatial alignment relative to a content bounding box using normalized bias
 * [-1.0, 1.0].
 *
 * (-1, -1) is bottom-left, (0, 0) is center, and (1, 1) is top-right.
 */
public data class SpatialAlignment
@JvmOverloads
constructor(public val x: Float = 0f, public val y: Float = 0f, public val z: Float = 0f) {
    public companion object {
        @JvmField public val Center: SpatialAlignment = SpatialAlignment(0f, 0f, 0f)
        @JvmField public val TopCenter: SpatialAlignment = SpatialAlignment(0f, 1f, 0f)
        @JvmField public val BottomCenter: SpatialAlignment = SpatialAlignment(0f, -1f, 0f)
        @JvmField public val CenterLeft: SpatialAlignment = SpatialAlignment(-1f, 0f, 0f)
        @JvmField public val CenterRight: SpatialAlignment = SpatialAlignment(1f, 0f, 0f)
        @JvmField public val TopLeft: SpatialAlignment = SpatialAlignment(-1f, 1f, 0f)
        @JvmField public val TopRight: SpatialAlignment = SpatialAlignment(1f, 1f, 0f)
        @JvmField public val BottomLeft: SpatialAlignment = SpatialAlignment(-1f, -1f, 0f)
        @JvmField public val BottomRight: SpatialAlignment = SpatialAlignment(1f, -1f, 0f)
    }
}

/**
 * Receiver scope for builder blocks passed to [performSpatialInteraction].
 *
 * All operations and coordinates within the scope (such as [currentOrigin], [currentTarget],
 * [moveOriginTo], [pointAt], and [movePointerBy]) operate within [node]'s local coordinate frame
 * (where $(0, 0, 0)$ is [node]'s origin, $+X$ is right, $+Y$ is up, and $+Z$ points along [node]'s
 * forward normal towards the user).
 *
 * Encapsulates sequential 3D pointer aiming, discrete trigger/press operations, and continuous
 * spatial trajectories across XR devices (Controllers, Hand Tracking, Eye Gaze).
 */
public interface SpatialInteractionScope {

    // =========================================================================================
    // Abstract state & primitive actions (must be provided by implementations)
    // =========================================================================================

    /**
     * The [SpatialNode] to which this interaction scope is anchored. All coordinates in this scope
     * operate relative to this node's local coordinate frame.
     */
    public val node: SpatialNode

    /**
     * Current position of the pointer source (controller, hand, or eye origin) in [node]-local
     * space.
     */
    public val currentOrigin: Vector3

    /** Current 3D point in [node]-local space being targeted by the pointer. */
    public val currentTarget: Vector3

    /** Whether the primary selection (trigger/pinch) is currently engaged. */
    public val isPressed: Boolean

    /** Fixed device type for this interaction scope. */
    public val deviceType: SpatialDeviceType

    /** Fixed pointer handedness/stream for this interaction scope. */
    public val pointerType: SpatialPointerType

    /**
     * Immediately moves the pointer source position (e.g. controller or hand) to [position] in
     * [node]'s local coordinate frame.
     *
     * @param position 3D coordinates in meters relative to [node].
     */
    public fun moveOriginTo(position: Vector3)

    /**
     * Immediately aims the pointer ray at an aligned point on [node], with optional local metric
     * [offset] in [node]'s coordinate frame.
     *
     * If [isPressed] is true, drags the pointer to the target location on [node].
     *
     * @param offset Metric translation offset in [node]-local space (default: [Vector3.Zero]).
     * @param alignment Normalized alignment within [node]'s half-extents (default:
     *   [SpatialAlignment.Center]).
     * @param targetNode The target [SpatialNode] (default: this scope's [node]).
     */
    public fun pointAt(
        offset: Vector3 = Vector3.Zero,
        alignment: SpatialAlignment = SpatialAlignment.Center,
        targetNode: SpatialNode = node,
    )

    /**
     * Engages primary selection (trigger press / pinch down) at the current ray target. Immediately
     * sets [isPressed] to `true` without artificial hold delays.
     */
    public fun press()

    /**
     * Disengages primary selection (trigger release / pinch up) at the current ray target.
     * Immediately sets [isPressed] to `false` without artificial delays.
     */
    public fun release()

    // =========================================================================================
    // Derived properties & default convenience methods
    // =========================================================================================

    /** Current position of the pointer source in OpenXR world space. */
    public val currentWorldOrigin: Vector3
        get() = node.transformPointToWorld(currentOrigin)

    /** Current 3D point in OpenXR world space being targeted by the pointer. */
    public val currentWorldTarget: Vector3
        get() = node.transformPointToWorld(currentTarget)

    /**
     * Performs a complete discrete selection sequence (Press -> Release) at the current target.
     *
     * @param holdDurationMs Optional hold duration between press and release (default: 100ms).
     */
    public fun select(holdDurationMs: Long = DEFAULT_DELAY_MS) {
        press()
        if (holdDurationMs > DEFAULT_DELAY_MS) {
            sleep(holdDurationMs - DEFAULT_DELAY_MS)
        }
        release()
    }

    /**
     * Immediately displaces the pointer source position by [delta] in [node]'s local coordinate
     * frame relative to [currentOrigin].
     *
     * @param delta Translation vector in meters in [node]-local coordinates.
     */
    public fun moveOriginBy(delta: Vector3) {
        moveOriginTo(currentOrigin + delta)
    }

    /**
     * Immediately moves the pointer target by [delta] in [node]'s local coordinate frame.
     *
     * If [isPressed] is true, drags the pointer by [delta].
     *
     * @param delta Translation vector in meters in [node]-local coordinates.
     */
    public fun movePointerBy(delta: Vector3) {
        pointAt(offset = currentTarget + delta, targetNode = node)
    }

    /**
     * Immediately aims the pointer ray at the center of a 2D View / UIAutomator [boundsInWindow] on
     * [targetNode].
     *
     * @param boundsInWindow Screen/window pixel bounding rectangle.
     * @param targetNode The panel or window [SpatialNode] hosting the 2D content (default: this
     *   scope's [node]).
     */
    public fun pointAt(
        boundsInWindow: Rect,
        targetNode: SpatialNode = node,
    ) {
        val offset = calculateWindowBoundsOffset(targetNode, boundsInWindow)
        pointAt(offset = offset, alignment = SpatialAlignment.Center, targetNode = targetNode)
    }

    /**
     * Calculates the 3D coordinates in [node]'s local coordinate frame corresponding to an aligned
     * point on [targetNode] with an optional metric [offset].
     *
     * @param targetNode The target [SpatialNode].
     * @param offset Optional translation offset in meters in [targetNode]-local space.
     * @param alignment Normalized bias alignment within [targetNode]'s half-extents (default:
     *   [SpatialAlignment.Center]).
     * @return The 3D position in [node]'s local space.
     */
    public fun localPointOf(
        targetNode: SpatialNode,
        offset: Vector3 = Vector3.Zero,
        alignment: SpatialAlignment = SpatialAlignment.Center,
    ): Vector3 {
        val targetWorldPoint = calculateAlignedPoint(targetNode, alignment, offset)
        return node.transformPointFromWorld(targetWorldPoint)
    }

    /**
     * Calculates the 3D coordinates in [node]'s local coordinate frame corresponding to the center
     * of a 2D View / UIAutomator [boundsInWindow] on [targetNode].
     *
     * @param targetNode The panel or window [SpatialNode] hosting the 2D content.
     * @param boundsInWindow Screen/window pixel bounding rectangle.
     * @return The 3D position in [node]'s local space.
     */
    public fun localPointOf(
        targetNode: SpatialNode,
        boundsInWindow: Rect,
    ): Vector3 {
        val offset = calculateWindowBoundsOffset(targetNode, boundsInWindow)
        return localPointOf(targetNode, offset, SpatialAlignment.Center)
    }

    /**
     * Drives an animation loop over [durationMs] using [easing], passing the normalized progression
     * [fraction] (`[0.0, 1.0]`) to [block] at each step.
     *
     * The [fraction] is computed by evaluating [easing] over the normalized time progression.
     * Callers can use this [fraction] to interpolate positions (e.g. using [Vector3.lerp]) or
     * offsets across keyframes.
     *
     * @param durationMs Total duration of the animation in milliseconds (default:
     *   [DEFAULT_DRAG_DURATION_MS]).
     * @param easing Motion curve applied to the motion interpolation (default:
     *   [FastOutSlowInEasing]).
     * @param block The receiver block executed at each animated step with the current [fraction].
     */
    public fun animate(
        durationMs: Long = DEFAULT_DRAG_DURATION_MS,
        easing: Easing = FastOutSlowInEasing,
        block: SpatialInteractionScope.(fraction: Float) -> Unit,
    ) {
        if (durationMs <= 0L) {
            block(1.0f)
            return
        }
        val steps = maxOf(2, (durationMs / DEFAULT_DELAY_MS).toInt())
        val stepDelay = maxOf(20L, durationMs / steps)
        for (i in 0..steps) {
            val progress = (i.toFloat() / steps).coerceIn(0f, 1f)
            val fraction = easing.transform(progress)
            block(fraction)
            if (i < steps) {
                sleep(stepDelay)
            }
        }
    }

    /**
     * Aims the pointer ray at [node]'s move affordance (`DragBar` from
     * [SpatialNode.moveAffordanceNodes] if present), falling back to an outer edge position at
     * [alignment] with [marginMeters], with an optional [offset] in [node]'s local coordinate
     * frame.
     *
     * @param offset Translation offset in meters in [node]-local space relative to the move
     *   affordance center.
     * @param alignment Fallback edge alignment if no explicit `DragBar` node is present.
     * @param marginMeters Distance outside the outer edge in meters for the fallback handle.
     */
    public fun pointAtMoveAffordance(
        offset: Vector3 = Vector3.Zero,
        alignment: SpatialAlignment = SpatialAlignment.BottomCenter,
        marginMeters: Float = DEFAULT_MOVE_HANDLE_MARGIN,
    ) {
        val affordance =
            node.moveAffordanceNodes.firstOrNull { it.isVisible }
                ?: node.moveAffordanceNodes.firstOrNull()
        val affordanceLocalOffset =
            if (affordance != null && affordance.worldExtents != null) {
                node.transformPointFromWorld(affordance.position)
            } else {
                val halfExtents = node.worldHalfExtents ?: Vector3.Zero
                val sx = if (abs(node.scale.x) > 1e-6f) node.scale.x else 1.0f
                val sy = if (abs(node.scale.y) > 1e-6f) node.scale.y else 1.0f
                val sz = if (abs(node.scale.z) > 1e-6f) node.scale.z else 1.0f
                Vector3(
                    x = ((alignment.x * halfExtents.x) + (alignment.x * marginMeters)) / sx,
                    y = ((alignment.y * halfExtents.y) + (alignment.y * marginMeters)) / sy,
                    z = ((alignment.z * halfExtents.z) + (alignment.z * marginMeters)) / sz,
                )
            }
        pointAt(offset = affordanceLocalOffset + offset)
    }

    /**
     * Aims the pointer ray at [node]'s resize affordance (`Resize*` from
     * [SpatialNode.resizeAffordanceNodes] if present), falling back to an outer edge/corner
     * position at [alignment] with [marginMeters], with an optional [offset] in [node]'s local
     * coordinate frame.
     *
     * @param offset Translation offset in meters in [node]-local space relative to the resize
     *   affordance center.
     * @param alignment Target corner/edge alignment (default: [SpatialAlignment.BottomRight]).
     * @param marginMeters Distance outside the outer edge in meters for the fallback handle.
     */
    public fun pointAtResizeAffordance(
        offset: Vector3 = Vector3.Zero,
        alignment: SpatialAlignment = SpatialAlignment.BottomRight,
        marginMeters: Float = DEFAULT_MOVE_HANDLE_MARGIN,
    ) {
        val cornerKeyword =
            when (alignment) {
                SpatialAlignment.BottomRight -> "BottomRight"
                SpatialAlignment.BottomLeft -> "BottomLeft"
                SpatialAlignment.TopRight -> "TopRight"
                SpatialAlignment.TopLeft -> "TopLeft"
                SpatialAlignment.CenterRight -> "Right"
                SpatialAlignment.CenterLeft -> "Left"
                SpatialAlignment.TopCenter -> "Top"
                SpatialAlignment.BottomCenter -> "Bottom"
                else -> null
            }
        val matchingAffordance =
            node.resizeAffordanceNodes.firstOrNull {
                cornerKeyword != null && it.name.contains(cornerKeyword, ignoreCase = true)
            }
        val affordanceLocalOffset =
            if (matchingAffordance != null && matchingAffordance.worldExtents != null) {
                node.transformPointFromWorld(matchingAffordance.position)
            } else {
                val halfExtents = node.worldHalfExtents ?: Vector3.Zero
                val sx = if (abs(node.scale.x) > 1e-6f) node.scale.x else 1.0f
                val sy = if (abs(node.scale.y) > 1e-6f) node.scale.y else 1.0f
                val sz = if (abs(node.scale.z) > 1e-6f) node.scale.z else 1.0f
                Vector3(
                    x = ((alignment.x * halfExtents.x) + (alignment.x * marginMeters)) / sx,
                    y = ((alignment.y * halfExtents.y) + (alignment.y * marginMeters)) / sy,
                    z = ((alignment.z * halfExtents.z) + (alignment.z * marginMeters)) / sz,
                )
            }
        pointAt(offset = affordanceLocalOffset + offset)
    }

    /**
     * Moves [node] by [delta] in [node]'s local coordinate frame by grabbing its move affordance
     * and dragging it.
     *
     * For custom animation timing or easing, compose [pointAtMoveAffordance], [press], [animate],
     * and [release] directly.
     *
     * @param delta Translation vector in meters in [node]-local space (e.g. +X moves right, +Y
     *   moves up relative to [node]'s orientation).
     * @param alignment Fallback edge alignment if no explicit `DragBar` node is present.
     * @param marginMeters Distance outside the outer edge in meters for the fallback handle.
     */
    public fun moveNodeBy(
        delta: Vector3,
        alignment: SpatialAlignment = SpatialAlignment.BottomCenter,
        marginMeters: Float = DEFAULT_MOVE_HANDLE_MARGIN,
    ) {
        pointAtMoveAffordance(alignment = alignment, marginMeters = marginMeters)
        press()
        animate { fraction ->
            pointAtMoveAffordance(
                offset = delta * fraction,
                alignment = alignment,
                marginMeters = marginMeters,
            )
        }
        release()
    }

    /**
     * Resizes [node] by dragging its resize affordance at [alignment] by [delta] in [node]'s local
     * coordinate frame.
     *
     * For custom animation timing or easing, compose [pointAtResizeAffordance], [press], [animate],
     * and [release] directly.
     *
     * @param delta Drag displacement vector in meters in [node]-local space (e.g. +X expands right,
     *   -Y expands downward relative to [node]'s orientation).
     * @param alignment Corner/edge alignment to drag (default: [SpatialAlignment.BottomRight]).
     * @param marginMeters Distance outside the outer edge in meters for the fallback handle.
     */
    public fun resizeNodeBy(
        delta: Vector3,
        alignment: SpatialAlignment = SpatialAlignment.BottomRight,
        marginMeters: Float = DEFAULT_MOVE_HANDLE_MARGIN,
    ) {
        pointAtResizeAffordance(alignment = alignment, marginMeters = marginMeters)
        press()
        animate { fraction ->
            pointAtResizeAffordance(
                offset = delta * fraction,
                alignment = alignment,
                marginMeters = marginMeters,
            )
        }
        release()
    }
}

/** Default internal implementation of [SpatialInteractionScope]. */
@PublishedApi
internal class SpatialInteractionScopeImpl(
    override val node: SpatialNode,
    override val deviceType: SpatialDeviceType,
    override val pointerType: SpatialPointerType,
    private val uiAutomation: UiAutomation? = null,
) : SpatialInteractionScope, AutoCloseable {

    override var currentOrigin: Vector3 = Vector3.Zero
        private set

    override var currentTarget: Vector3 = Vector3.Zero
        private set

    private var hasExplicitOrigin: Boolean = false
    private var hasTarget: Boolean = false

    override var isPressed: Boolean = false
        private set

    override fun moveOriginTo(position: Vector3) {
        currentOrigin = position
        hasExplicitOrigin = true
        dispatchCurrentState()
    }

    override fun pointAt(
        offset: Vector3,
        alignment: SpatialAlignment,
        targetNode: SpatialNode,
    ) {
        val targetWorldPoint = calculateAlignedPoint(targetNode, alignment, offset)
        currentTarget = node.transformPointFromWorld(targetWorldPoint)
        if (!hasExplicitOrigin) {
            val normal = (targetNode.rotation * Vector3(0f, 0f, 1f)).toNormalized()
            val originWorldPoint = targetWorldPoint + (normal * DEFAULT_RAY_DISTANCE)
            currentOrigin = node.transformPointFromWorld(originWorldPoint)
        }
        hasTarget = true
        dispatchCurrentState()
    }

    override fun press() {
        if (isPressed) {
            release()
        } else {
            // Ensure the unpressed hover state is dispatched and given time to register in the
            // XR input pipeline before engaging the press-down transition.
            dispatchCurrentState()
            sleep(DEFAULT_DELAY_MS)
        }
        isPressed = true
        dispatchCurrentState()
        sleep(DEFAULT_DELAY_MS)
    }

    override fun release() {
        isPressed = false
        dispatchCurrentState()
        sleep(DEFAULT_DELAY_MS)
        try {
            uiAutomation?.waitForIdle(50L, 500L)
        } catch (_: Exception) {}
    }

    private fun dispatchCurrentState() {
        val automation = uiAutomation ?: return
        performRayAction(
            ray = currentRay,
            action = if (isPressed) SpatialRayAction.Down else SpatialRayAction.Up,
            deviceType = deviceType,
            pointerType = pointerType,
            uiAutomation = automation,
        )
    }

    override fun close() {
        val automation = uiAutomation
        if (automation == null) {
            isPressed = false
            return
        }
        try {
            resetSpatialInteraction(automation)
        } catch (ignored: Exception) {
            if (isPressed) {
                try {
                    performRayAction(
                        ray = currentRay,
                        action = SpatialRayAction.Up,
                        deviceType = deviceType,
                        pointerType = pointerType,
                        uiAutomation = automation,
                    )
                } catch (_: Exception) {}
            }
        } finally {
            isPressed = false
            try {
                automation.waitForIdle(50L, 500L)
            } catch (_: Exception) {}
        }
    }

    private val currentRay: Ray
        get() {
            val worldOrigin = currentWorldOrigin
            val worldTarget = currentWorldTarget
            val direction =
                if (hasTarget && (worldTarget - worldOrigin).length > 0.0001f) {
                    (worldTarget - worldOrigin).toNormalized() * (DEFAULT_RAY_DISTANCE * 2.0f)
                } else {
                    (node.rotation * Vector3(0f, 0f, -1f)).toNormalized() *
                        (DEFAULT_RAY_DISTANCE * 2.0f)
                }
            return Ray(origin = worldOrigin, direction = direction)
        }
}

private fun calculateWindowBoundsOffset(node: SpatialNode, bounds: Rect): Vector3 =
    calculateWindowPixelOffset(
        node = node,
        pixelX = (bounds.left + bounds.right) / 2.0f,
        pixelY = (bounds.top + bounds.bottom) / 2.0f,
    )

private fun calculateWindowPixelOffset(
    node: SpatialNode,
    pixelX: Float,
    pixelY: Float,
): Vector3 {
    val halfExtents = node.worldHalfExtents ?: Vector3.Zero
    val pixelBounds = node.effectiveSurfaceBoundsInPixels ?: node.surfaceBoundsInPixels
    val windowWidthPx = pixelBounds?.x ?: 1.0f
    val windowHeightPx = pixelBounds?.y ?: 1.0f

    val normX = if (windowWidthPx > 0f) (2.0f * pixelX / windowWidthPx) - 1.0f else 0.0f
    val normY = if (windowHeightPx > 0f) 1.0f - (2.0f * pixelY / windowHeightPx) else 0.0f

    val sx = if (abs(node.scale.x) > 1e-6f) node.scale.x else 1.0f
    val sy = if (abs(node.scale.y) > 1e-6f) node.scale.y else 1.0f
    return Vector3(x = normX * halfExtents.x / sx, y = normY * halfExtents.y / sy, z = 0f)
}

/**
 * Calculates the world-space coordinate of an aligned point on or relative to [node], with an
 * optional metric [offset] in meters.
 *
 * @param node The spatial panel or window node.
 * @param alignment Normalized bias alignment within the node bounds (e.g.
 *   [SpatialAlignment.BottomCenter], [SpatialAlignment.BottomRight]).
 * @param offset Translation offset in meters applied relative to the aligned point in node-local
 *   coordinates.
 */
private fun calculateAlignedPoint(
    node: SpatialNode,
    alignment: SpatialAlignment = SpatialAlignment.Center,
    offset: Vector3 = Vector3.Zero,
): Vector3 {
    val worldHalfExtents = node.worldHalfExtents ?: Vector3.Zero
    val localHalfExtents =
        Vector3(
            x = if (node.scale.x != 0f) worldHalfExtents.x / node.scale.x else worldHalfExtents.x,
            y = if (node.scale.y != 0f) worldHalfExtents.y / node.scale.y else worldHalfExtents.y,
            z = if (node.scale.z != 0f) worldHalfExtents.z / node.scale.z else worldHalfExtents.z,
        )
    val localPoint =
        Vector3(
            x = (alignment.x * localHalfExtents.x) + offset.x,
            y = (alignment.y * localHalfExtents.y) + offset.y,
            z = (alignment.z * localHalfExtents.z) + offset.z,
        )
    return node.transformPointToWorld(localPoint)
}

/**
 * Resolves the executable path for the raycast injection CLI.
 *
 * If `xr_inject_input_cli` is found on the device (e.g. in `/system/bin/xr_inject_input_cli`,
 * standard binary directories, or `/data/local/tmp/xr_inject_input_cli`), returns that path. If not
 * found on the device, attempts to copy the prebuilt binary/script from library assets to
 * `/data/local/tmp/xr_inject_input_cli`, makes it executable, and returns
 * `"/data/local/tmp/xr_inject_input_cli"`.
 *
 * @throws IllegalStateException if the CLI is not found on the device and cannot be installed from
 *   assets.
 */
private fun getOrInstallCli(
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
): String {
    resolvedCliPath?.let {
        return it
    }

    // 1. Check if an existing valid non-empty binary is already present at /data/local/tmp
    val tmpCheck =
        executeShellCommand("test -s $CLI_TMP_PATH && echo VALID || echo INVALID", uiAutomation)
            .trim()
    if (tmpCheck == "VALID") {
        resolvedCliPath = CLI_TMP_PATH
        return CLI_TMP_PATH
    }

    // 2. Prioritize extracting the bundled binary from assets directly to /data/local/tmp
    if (installCliFromAssets(uiAutomation)) {
        resolvedCliPath = CLI_TMP_PATH
        return CLI_TMP_PATH
    }

    // 3. Fall back to which CLI_BINARY_NAME in PATH
    val whichCheck = executeShellCommand("which $CLI_BINARY_NAME", uiAutomation).trim()
    if (
        whichCheck.isNotEmpty() &&
            !whichCheck.contains("not found") &&
            !whichCheck.contains("No such") &&
            !whichCheck.contains("[TIMED_OUT]")
    ) {
        resolvedCliPath = whichCheck
        return whichCheck
    }

    // 4. Fall back to standard executable locations on system image
    val candidatePaths =
        listOf(
            CLI_SYSTEM_PATH,
            "/vendor/bin/$CLI_BINARY_NAME",
            "/product/bin/$CLI_BINARY_NAME",
            "/system/xbin/$CLI_BINARY_NAME",
        )
    for (candidatePath in candidatePaths) {
        val check = executeShellCommand("ls -d $candidatePath", uiAutomation).trim()
        if (
            check.contains(candidatePath) &&
                !check.contains("No such") &&
                !check.contains("not found") &&
                !check.contains("[TIMED_OUT]")
        ) {
            resolvedCliPath = candidatePath
            return candidatePath
        }
    }

    throw IllegalStateException(
        "XR raycast injection CLI '$CLI_BINARY_NAME' could not be installed from assets " +
            "and was not found on the device (checked $CLI_TMP_PATH, $candidatePaths)."
    )
}

private val commandExecutor = java.util.concurrent.Executors.newCachedThreadPool()

/** Executes a shell command synchronously via [uiAutomation] and returns stdout as a string. */
internal fun executeShellCommand(
    command: String,
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    timeoutMs: Long = 3000L,
): String {
    Log.i("SpatialInteractionHelper", "Executing shell command: $command")
    val pfd =
        try {
            uiAutomation.executeShellCommand(command)
        } catch (e: Exception) {
            Log.w("SpatialInteractionHelper", "Failed to execute command: $command", e)
            return ""
        }

    return try {
        val future =
            commandExecutor.submit<String> {
                try {
                    ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use {
                        it.readText()
                    }
                } catch (e: Exception) {
                    ""
                }
            }
        try {
            val output = future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            Log.i("SpatialInteractionHelper", "Shell command output: '$output'")
            output
        } catch (e: java.util.concurrent.TimeoutException) {
            Log.w(
                "SpatialInteractionHelper",
                "Command timed out after ${timeoutMs}ms: $command",
            )
            future.cancel(true)
            try {
                pfd.close()
            } catch (_: Exception) {}
            "[TIMED_OUT]"
        }
    } catch (e: Exception) {
        Log.w("SpatialInteractionHelper", "Error reading command output: $command", e)
        ""
    }
}

/**
 * Copies the `xr_inject_input_cli` binary from library / test assets to `/data/local/tmp/` and
 * marks it executable.
 */
private fun installCliFromAssets(uiAutomation: UiAutomation): Boolean {
    val instrumentation =
        try {
            InstrumentationRegistry.getInstrumentation()
        } catch (e: Exception) {
            return false
        }

    openCliAssetInputStream(instrumentation)?.close() ?: return false

    return try {
        val context = instrumentation.targetContext ?: instrumentation.context
        val candidateDirs =
            listOfNotNull(
                File("/sdcard/Download"),
                File("/sdcard"),
                context.getExternalFilesDir(null),
                context.externalCacheDir,
                context.cacheDir,
            )

        var copied = false
        for (dir in candidateDirs) {
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val tempFile = File(dir, "xr_inject_input_cli_staged")
            try {
                val stream = openCliAssetInputStream(instrumentation) ?: break
                tempFile.outputStream().use { out -> stream.use { it.copyTo(out) } }
                tempFile.setReadable(true, false)
                tempFile.setExecutable(true, false)

                executeShellCommand("cp ${tempFile.absolutePath} $CLI_TMP_PATH", uiAutomation)
                executeShellCommand("chmod 755 $CLI_TMP_PATH", uiAutomation)

                val check = executeShellCommand("ls -l $CLI_TMP_PATH", uiAutomation).trim()
                Log.i(
                    "SpatialInteractionHelper",
                    "Checked $CLI_TMP_PATH after copy from ${tempFile.absolutePath}: '$check'",
                )
                if (
                    check.contains(CLI_BINARY_NAME) &&
                        !check.contains("No such") &&
                        !check.contains("not found")
                ) {
                    copied = true
                    break
                }
            } catch (e: Exception) {
                Log.w("SpatialInteractionHelper", "Failed staging via ${dir.absolutePath}", e)
            } finally {
                try {
                    tempFile.delete()
                } catch (_: Exception) {}
            }
        }
        copied
    } catch (e: Exception) {
        Log.e(
            "SpatialInteractionHelper",
            "Failed to install $CLI_BINARY_NAME to $CLI_TMP_PATH",
            e,
        )
        false
    }
}

private fun openCliAssetInputStream(instrumentation: Instrumentation): InputStream? {
    val contexts = listOfNotNull(instrumentation.context, instrumentation.targetContext)
    val candidatePaths =
        Build.SUPPORTED_ABIS.flatMap { abi ->
            listOf(
                "cli/$abi/$CLI_BINARY_NAME",
                "$abi/$CLI_BINARY_NAME",
                "androidx/xr/cli/$abi/$CLI_BINARY_NAME",
            )
        } + listOf("cli/$CLI_BINARY_NAME", CLI_BINARY_NAME, "androidx/xr/cli/$CLI_BINARY_NAME")

    for (ctx in contexts) {
        val assetManager = ctx.assets ?: continue
        for (path in candidatePaths) {
            try {
                val stream = assetManager.open(path)
                Log.i(
                    "SpatialInteractionHelper",
                    "Found CLI asset at: $path in context ${ctx.packageName}",
                )
                return stream
            } catch (_: IOException) {}
        }

        fun findInAssetDir(dir: String): InputStream? {
            val list =
                try {
                    assetManager.list(dir) ?: emptyArray()
                } catch (_: IOException) {
                    emptyArray()
                }
            for (entry in list) {
                val entryPath = if (dir.isEmpty()) entry else "$dir/$entry"
                if (entry == CLI_BINARY_NAME) {
                    try {
                        val stream = assetManager.open(entryPath)
                        Log.i(
                            "SpatialInteractionHelper",
                            "Found CLI asset via recursive scan at: $entryPath in context " +
                                ctx.packageName,
                        )
                        return stream
                    } catch (_: IOException) {}
                }
                val sub = findInAssetDir(entryPath)
                if (sub != null) return sub
            }
            return null
        }

        val recursiveStream = findInAssetDir("")
        if (recursiveStream != null) return recursiveStream
    }

    return null
}

/**
 * Synchronizes with the main thread and compositor queues to ensure pending UI and input tasks are
 * finished executing.
 */
private fun syncIdle(
    uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
) {
    try {
        uiAutomation.waitForIdle(50L, 500L)
    } catch (ignored: Exception) {
        // Ignored if called off-instrumentation or when idle sync is not available / times out
    }
}

@Suppress("BanThreadSleep")
internal fun sleep(ms: Long) {
    if (ms > 0L) {
        try {
            SystemClock.sleep(ms)
        } catch (_: RuntimeException) {
            try {
                Thread.sleep(ms)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
