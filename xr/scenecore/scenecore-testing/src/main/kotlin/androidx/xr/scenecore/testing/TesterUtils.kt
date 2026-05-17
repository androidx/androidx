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

package androidx.xr.scenecore.testing

import android.annotation.SuppressLint
import androidx.xr.scenecore.AnchorEntity.State
import androidx.xr.scenecore.HitTestResult
import androidx.xr.scenecore.HitTestResult.SurfaceType
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.PerceivedResolutionResult
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.PointerCaptureComponent.PointerCaptureState
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.ResizeEvent.ResizeState
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.SpatialVisibility
import androidx.xr.scenecore.SpatializerConstants
import androidx.xr.scenecore.SurfaceEntity.EdgeFeatheringParams
import androidx.xr.scenecore.SurfaceEntity.Shape
import androidx.xr.scenecore.runtime.AnchorEntity.State as RtState
import androidx.xr.scenecore.runtime.Dimensions as RtDimensions
import androidx.xr.scenecore.runtime.HitTestResult as RtHitTestResult
import androidx.xr.scenecore.runtime.HitTestResult.HitTestSurfaceType as RtHitTestSurfaceType
import androidx.xr.scenecore.runtime.InputEvent as RtInputEvent
import androidx.xr.scenecore.runtime.InputEvent.HitInfo as RtHitInfo
import androidx.xr.scenecore.runtime.PerceivedResolutionResult as RtPerceivedResolutionResult
import androidx.xr.scenecore.runtime.PointSourceParams as RtPointSourceParams
import androidx.xr.scenecore.runtime.PointerCaptureComponent.PointerCaptureState as RtPointerCaptureState
import androidx.xr.scenecore.runtime.ResizeEvent as RtResizeEvent
import androidx.xr.scenecore.runtime.ResizeEvent.ResizeState as RtResizeState
import androidx.xr.scenecore.runtime.SoundFieldAttributes as RtSoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialVisibility as RtSpatialVisibility
import androidx.xr.scenecore.runtime.SpatializerConstants as RtSpatializerConstants
import androidx.xr.scenecore.runtime.SurfaceEntity.EdgeFeather as RtEdgeFeather
import androidx.xr.scenecore.runtime.SurfaceEntity.Shape as RtShape
import androidx.xr.scenecore.toRtPixelDimensions

// region Dimensions, Resolution & Resizing

/**
 * Extension function that converts a [PerceivedResolutionResult] to [RtPerceivedResolutionResult].
 */
internal fun PerceivedResolutionResult.toRtPerceivedResolutionResult():
    RtPerceivedResolutionResult {
    return when (this) {
        is PerceivedResolutionResult.Success ->
            RtPerceivedResolutionResult.Success(this.perceivedResolution.toRtPixelDimensions())
        is PerceivedResolutionResult.EntityTooClose -> RtPerceivedResolutionResult.EntityTooClose()
        is PerceivedResolutionResult.InvalidRenderViewpoint ->
            RtPerceivedResolutionResult.InvalidRenderViewpoint()
        else ->
            throw IllegalArgumentException(
                "Unknown PerceivedResolutionResult type: ${this::class.java}"
            )
    }
}

/** Extension function that converts a [ResizeState] to a [RtResizeState]. */
internal fun ResizeState.toRtResizeState(): Int {
    return when (this) {
        ResizeState.UNKNOWN -> RtResizeEvent.RESIZE_STATE_UNKNOWN
        ResizeState.START -> RtResizeEvent.RESIZE_STATE_START
        ResizeState.ONGOING -> RtResizeEvent.RESIZE_STATE_ONGOING
        ResizeState.END -> RtResizeEvent.RESIZE_STATE_END
        else -> throw IllegalArgumentException("Unknown resize state")
    }
}

/** Extension function that converts a [ResizeEvent] to a [RtResizeEvent]. */
internal fun ResizeEvent.toRtResizeEvent(): RtResizeEvent {
    return RtResizeEvent(
        this.resizeState.toRtResizeState(),
        RtDimensions(this.newSize.width, this.newSize.height, this.newSize.depth),
    )
}

// endregion

// region Entity Anchors & State

/** Extension function that converts a [State] to a [RtState]. */
@SuppressLint("WrongConstant")
internal fun State.toRtState(): Int {
    return when (this) {
        State.ANCHORED -> RtState.ANCHORED
        State.UNANCHORED -> RtState.UNANCHORED
        State.TIMED_OUT -> RtState.TIMED_OUT
        else -> RtState.ERROR
    }
}

/** Extension function that converts a [RtState] to a [State]. */
@SuppressLint("WrongConstant")
internal fun Int.toState(): State {
    return when (this) {
        RtState.ANCHORED -> State.ANCHORED
        RtState.UNANCHORED -> State.UNANCHORED
        RtState.TIMED_OUT -> State.TIMED_OUT
        else -> State.ERROR
    }
}

// endregion

// region Geometry (Shapes & Edges)
/** Extension function that converts a [Shape] to a [RtShape]. */
internal fun Shape.toRtShape(): RtShape {
    return when (this) {
        is Shape.Quad -> RtShape.Quad(extents = this.extents, cornerRadius = this.cornerRadius)
        is Shape.Sphere -> RtShape.Sphere(radius = this.radius)
        is Shape.Hemisphere -> RtShape.Hemisphere(radius = this.radius)
        else -> throw IllegalArgumentException("Unknown Shape type: ${this::class.java}")
    }
}

/** Extension function that converts a [RtShape] to a [Shape]. */
internal fun RtShape.toShape(): Shape {
    return when (this) {
        is RtShape.Quad -> Shape.Quad(extents = this.extents, cornerRadius = this.cornerRadius)
        is RtShape.Sphere -> Shape.Sphere(radius = this.radius)
        is RtShape.Hemisphere -> Shape.Hemisphere(radius = this.radius)
        else -> throw IllegalArgumentException("Unknown RtShape type: ${this::class.java}")
    }
}

/** Extension function that converts a [EdgeFeatheringParams] to a [RtEdgeFeather]. */
internal fun EdgeFeatheringParams.toRtEdgeFeather(): RtEdgeFeather {
    return when (this) {
        is EdgeFeatheringParams.RectangleFeather ->
            RtEdgeFeather.RectangleFeather(leftRight = this.leftRight, topBottom = this.topBottom)
        is EdgeFeatheringParams.NoFeathering -> RtEdgeFeather.NoFeathering()
        else ->
            throw IllegalArgumentException("Unknown EdgeFeatheringParams type: ${this::class.java}")
    }
}

/** Extension function that converts a [RtEdgeFeather] to a [EdgeFeatheringParams]. */
internal fun RtEdgeFeather.toEdgeFeatheringParams(): EdgeFeatheringParams {
    return when (this) {
        is RtEdgeFeather.RectangleFeather ->
            EdgeFeatheringParams.RectangleFeather(
                leftRight = this.leftRight,
                topBottom = this.topBottom,
            )
        is RtEdgeFeather.NoFeathering -> EdgeFeatheringParams.NoFeathering()
        else -> throw IllegalArgumentException("Unknown RtEdgeFeather type: ${this::class.java}")
    }
}

// endregion

// region Hit Testing
/** Extension function that converts a [HitTestResult.SurfaceType] to a [RtHitTestSurfaceType]. */
internal fun Int.toRtHitTestSurfaceType(): Int {
    return when (this) {
        SurfaceType.UNKNOWN -> RtHitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN
        SurfaceType.PLANE -> RtHitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE
        SurfaceType.OBJECT -> RtHitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_OBJECT
        else -> RtHitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN
    }
}

/** Extension function that converts a [HitTestResult] to a [RtHitTestResult]. */
internal fun HitTestResult.toRtHitTestResult(): RtHitTestResult {
    return RtHitTestResult(
        hitPosition,
        surfaceNormal,
        surfaceType.toRtHitTestSurfaceType(),
        distance,
    )
}

// endregion

// region Input Events & Interactions
/** Extension function that converts an [InputEvent] to a [RtInputEvent]. */
internal fun InputEvent.toRtInputEvent(): RtInputEvent {
    val rtHitInfos = hitInfoList.map { it.toRtHitInfo() }
    return RtInputEvent(
        source = source.toRtInputEventSource(),
        pointerType = pointerType.toRtInputEventPointer(),
        timestamp = timestamp,
        origin = origin,
        direction = direction,
        action = action.toRtInputEventAction(),
        hitInfoList = rtHitInfos,
    )
}

/**
 * Extension function that converts a [InputEvent.HitInfo] to a [RtHitInfo] (aka
 * RtInputEvent.HitInfo).
 */
internal fun InputEvent.HitInfo.toRtHitInfo(): RtHitInfo {
    return RtHitInfo(
        inputEntity = inputEntity.rtEntity,
        hitPosition = hitPosition,
        transform = transform,
    )
}

/** Extension function that converts a [InputEvent.Source] to [Int] (RtInputEvent.Source). */
internal fun InputEvent.Source.toRtInputEventSource(): Int {
    return when (this) {
        InputEvent.Source.UNKNOWN -> RtInputEvent.Source.UNKNOWN
        InputEvent.Source.HEAD -> RtInputEvent.Source.HEAD
        InputEvent.Source.CONTROLLER -> RtInputEvent.Source.CONTROLLER
        InputEvent.Source.HANDS -> RtInputEvent.Source.HANDS
        InputEvent.Source.MOUSE -> RtInputEvent.Source.MOUSE
        InputEvent.Source.GAZE_AND_GESTURE -> RtInputEvent.Source.GAZE_AND_GESTURE
        else -> error("Unknown Input Event Source: $this")
    }
}

/** Extension function that converts a [InputEvent.Pointer] to [Int] (RtInputEvent.Pointer). */
internal fun InputEvent.Pointer.toRtInputEventPointer(): Int {
    return when (this) {
        InputEvent.Pointer.DEFAULT -> RtInputEvent.Pointer.DEFAULT
        InputEvent.Pointer.LEFT -> RtInputEvent.Pointer.LEFT
        InputEvent.Pointer.RIGHT -> RtInputEvent.Pointer.RIGHT
        else -> error("Unknown Input Event Pointer Type: $this")
    }
}

/** Extension function that converts a [InputEvent.Action] to [Int] (RtInputEvent.Action). */
internal fun InputEvent.Action.toRtInputEventAction(): Int {
    return when (this) {
        InputEvent.Action.DOWN -> RtInputEvent.Action.DOWN
        InputEvent.Action.UP -> RtInputEvent.Action.UP
        InputEvent.Action.MOVE -> RtInputEvent.Action.MOVE
        InputEvent.Action.CANCEL -> RtInputEvent.Action.CANCEL
        InputEvent.Action.HOVER_MOVE -> RtInputEvent.Action.HOVER_MOVE
        InputEvent.Action.HOVER_ENTER -> RtInputEvent.Action.HOVER_ENTER
        InputEvent.Action.HOVER_EXIT -> RtInputEvent.Action.HOVER_EXIT
        else -> error("Unknown Input Event Action: $this")
    }
}

/** Extension function that converts a [PointerCaptureState] to a [RtPointerCaptureState]. */
internal fun PointerCaptureState.toRtPointerCaptureState(): Int {
    return when (this) {
        PointerCaptureState.PAUSED -> RtPointerCaptureState.POINTER_CAPTURE_STATE_PAUSED
        PointerCaptureState.ACTIVE -> RtPointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE
        PointerCaptureState.STOPPED -> RtPointerCaptureState.POINTER_CAPTURE_STATE_STOPPED
        else -> throw IllegalStateException("Invalid state received for pointer capture")
    }
}

/** Extension function that converts a [RtPointerCaptureState] to a [PointerCaptureState]. */
internal fun Int.toPointerCaptureState(): PointerCaptureState {
    return when (this) {
        RtPointerCaptureState.POINTER_CAPTURE_STATE_PAUSED -> PointerCaptureState.PAUSED
        RtPointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE -> PointerCaptureState.ACTIVE
        RtPointerCaptureState.POINTER_CAPTURE_STATE_STOPPED -> PointerCaptureState.STOPPED
        else -> throw IllegalStateException("Invalid state received for pointer capture")
    }
}

// endregion

// region Spatial Audio
/** Extension function that converts a [RtPointSourceParams] to a [PointSourceParams]. */
internal fun RtPointSourceParams.toPointSourceParams(): PointSourceParams {
    val pointSourceParams = PointSourceParams()
    pointSourceParams.rtPointSourceParams = this
    return pointSourceParams
}

/**
 * Extension function that converts a [SpatializerConstants.SourceType] to a
 * [RtSpatializerConstants.SourceType].
 */
internal fun SpatializerConstants.SourceType.toRtSourceType(): Int {
    return when (this) {
        SpatializerConstants.SourceType.POINT_SOURCE ->
            RtSpatializerConstants.SourceType.SOURCE_TYPE_POINT_SOURCE
        SpatializerConstants.SourceType.SOUND_FIELD ->
            RtSpatializerConstants.SourceType.SOURCE_TYPE_SOUND_FIELD
        else -> RtSpatializerConstants.SourceType.SOURCE_TYPE_BYPASS
    }
}

/**
 * Extension function that converts a [RtSpatializerConstants.SourceType] to a
 * [SpatializerConstants.SourceType].
 */
internal fun Int.toSourceType(): SpatializerConstants.SourceType {
    return when (this) {
        RtSpatializerConstants.SourceType.SOURCE_TYPE_POINT_SOURCE ->
            SpatializerConstants.SourceType.POINT_SOURCE
        RtSpatializerConstants.SourceType.SOURCE_TYPE_SOUND_FIELD ->
            SpatializerConstants.SourceType.SOUND_FIELD
        else -> SpatializerConstants.SourceType.DEFAULT
    }
}

/**
 * Extension function that converts a [SpatializerConstants.AmbisonicsOrder] to a
 * [RtSpatializerConstants.AmbisonicsOrder].
 */
internal fun SpatializerConstants.AmbisonicsOrder.toRtAmbisonicsOrder(): Int {
    return when (this) {
        SpatializerConstants.AmbisonicsOrder.FIRST_ORDER ->
            RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_FIRST_ORDER
        SpatializerConstants.AmbisonicsOrder.SECOND_ORDER ->
            RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_SECOND_ORDER
        SpatializerConstants.AmbisonicsOrder.THIRD_ORDER ->
            RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_THIRD_ORDER
        else -> RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_FIRST_ORDER
    }
}

/**
 * Extension function that converts a [RtSpatializerConstants.AmbisonicsOrder] to a
 * [SpatializerConstants.AmbisonicsOrder].
 */
internal fun Int.toAmbisonicsOrder(): SpatializerConstants.AmbisonicsOrder {
    return when (this) {
        RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_FIRST_ORDER ->
            SpatializerConstants.AmbisonicsOrder.FIRST_ORDER
        RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_SECOND_ORDER ->
            SpatializerConstants.AmbisonicsOrder.SECOND_ORDER
        RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_THIRD_ORDER ->
            SpatializerConstants.AmbisonicsOrder.THIRD_ORDER
        else -> SpatializerConstants.AmbisonicsOrder.FIRST_ORDER
    }
}

/** Extension function that converts a [SoundFieldAttributes] to a [RtSoundFieldAttributes]. */
internal fun SoundFieldAttributes.toRtSoundFieldAttributes(): RtSoundFieldAttributes {
    return RtSoundFieldAttributes(ambisonicsOrder = this.order.toRtAmbisonicsOrder())
}

/** Extension function that converts a [RtSoundFieldAttributes] to a [SoundFieldAttributes]. */
internal fun RtSoundFieldAttributes.toSoundFieldAttributes(): SoundFieldAttributes {
    return SoundFieldAttributes(order = this.ambisonicsOrder.toAmbisonicsOrder())
}

// endregion

/**
 * Extension function that converts a Set of [SpatialCapability] to a
 * [androidx.xr.scenecore.runtime.SpatialCapabilities] (RtSpatialCapabilities).
 */
internal fun Set<SpatialCapability>.toRtSpatialCapabilities(): RtSpatialCapabilities {
    var capabilitiesMask = 0
    with(RtSpatialCapabilities) {
        if (contains(SpatialCapability.SPATIAL_3D_CONTENT)) {
            capabilitiesMask = capabilitiesMask or SPATIAL_CAPABILITY_3D_CONTENT
        }
        if (contains(SpatialCapability.APP_ENVIRONMENT)) {
            capabilitiesMask = capabilitiesMask or SPATIAL_CAPABILITY_APP_ENVIRONMENT
        }
        if (contains(SpatialCapability.EMBED_ACTIVITY)) {
            capabilitiesMask = capabilitiesMask or SPATIAL_CAPABILITY_EMBED_ACTIVITY
        }
        if (contains(SpatialCapability.PASSTHROUGH_CONTROL)) {
            capabilitiesMask = capabilitiesMask or SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL
        }
        if (contains(SpatialCapability.SPATIAL_AUDIO)) {
            capabilitiesMask = capabilitiesMask or SPATIAL_CAPABILITY_SPATIAL_AUDIO
        }
        if (contains(SpatialCapability.SPATIAL_UI)) {
            capabilitiesMask = capabilitiesMask or SPATIAL_CAPABILITY_UI
        }
    }
    return RtSpatialCapabilities(capabilitiesMask)
}

/** Extension function that converts a [SpatialVisibility] to a [RtSpatialVisibility]. */
internal fun SpatialVisibility.toRtSpatialVisibility(): RtSpatialVisibility {
    val rtValue =
        when (this) {
            SpatialVisibility.UNKNOWN -> RtSpatialVisibility.UNKNOWN
            SpatialVisibility.OUTSIDE_FIELD_OF_VIEW -> RtSpatialVisibility.OUTSIDE_FOV
            SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW ->
                RtSpatialVisibility.PARTIALLY_WITHIN_FOV
            SpatialVisibility.WITHIN_FIELD_OF_VIEW -> RtSpatialVisibility.WITHIN_FOV
            else -> RtSpatialVisibility.UNKNOWN
        }
    return RtSpatialVisibility(rtValue)
}
