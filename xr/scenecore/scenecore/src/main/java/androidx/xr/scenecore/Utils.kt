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

package androidx.xr.scenecore

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.xr.arcore.Plane
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Ray
import androidx.xr.scenecore.HitTestResult.SurfaceType
import androidx.xr.scenecore.InputEvent.HitInfo
import androidx.xr.scenecore.ScenePose.HitTestFilter
import androidx.xr.scenecore.SurfaceEntity.Shape.TriangleMesh
import androidx.xr.scenecore.runtime.AnchorEntity as RtAnchorEntity
import androidx.xr.scenecore.runtime.AnchorPlacement as RtAnchorPlacement
import androidx.xr.scenecore.runtime.Dimensions as RtDimensions
import androidx.xr.scenecore.runtime.HitTestResult as RtHitTestResult
import androidx.xr.scenecore.runtime.HitTestResult.HitTestSurfaceType as RtHitTestSurfaceType
import androidx.xr.scenecore.runtime.InputEvent as RtInputEvent
import androidx.xr.scenecore.runtime.InputEvent.HitInfo as RtHitInfo
import androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec as RtKhronosPbrMaterialSpec
import androidx.xr.scenecore.runtime.MoveEvent as RtMoveEvent
import androidx.xr.scenecore.runtime.PerceivedResolutionResult as RtPerceivedResolutionResult
import androidx.xr.scenecore.runtime.PixelDimensions as RtPixelDimensions
import androidx.xr.scenecore.runtime.PlaneSemantic as RtPlaneSemantic
import androidx.xr.scenecore.runtime.PlaneType as RtPlaneType
import androidx.xr.scenecore.runtime.ResizeEvent as RtResizeEvent
import androidx.xr.scenecore.runtime.ScenePose.HitTestFilter as RtHitTestFilter
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.Space as RtSpace
import androidx.xr.scenecore.runtime.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialPointerIcon as RtSpatialPointerIcon
import androidx.xr.scenecore.runtime.SpatialPointerIconType as RtSpatialPointerIconType
import androidx.xr.scenecore.runtime.SpatialVisibility as RtSpatialVisibility
import androidx.xr.scenecore.runtime.SurfaceEntity.Shape.TriangleMesh as RtTriangleMesh
import androidx.xr.scenecore.runtime.TextureSampler as RtTextureSampler
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job

internal class HandlerExecutor(val handler: Handler) : Executor {
    override fun execute(command: Runnable) {
        handler.post(command)
    }

    companion object {
        val mainThreadExecutor: Executor = HandlerExecutor(Handler(Looper.getMainLooper()))
    }
}

/**
 * Extension function that converts a [androidx.xr.runtime.math.FloatSize3d] to
 * [androidx.xr.scenecore.runtime.Dimensions].
 */
internal fun FloatSize3d.toRtDimensions(): RtDimensions {
    return RtDimensions(width, height, depth)
}

/**
 * Extension function that converts a [androidx.xr.runtime.math.FloatSize2d] to
 * [androidx.xr.scenecore.runtime.Dimensions], setting the `depth` field to 0.
 */
internal fun FloatSize2d.toRtDimensions(): RtDimensions {
    return RtDimensions(width, height, 0f)
}

/**
 * Extension function that converts a [androidx.xr.scenecore.runtime.Dimensions] to [FloatSize3d].
 */
internal fun RtDimensions.toFloatSize3d(): FloatSize3d {
    return FloatSize3d(width, height, depth)
}

/**
 * Extension function that converts a [androidx.xr.scenecore.runtime.Dimensions] to [FloatSize3d].
 */
internal fun RtDimensions.toFloatSize2d(): FloatSize2d {
    return FloatSize2d(width, height)
}

/**
 * Extension function that converts a [androidx.xr.runtime.math.IntSize2d] to
 * [androidx.xr.scenecore.runtime.PixelDimensions].
 */
internal fun IntSize2d.toRtPixelDimensions(): RtPixelDimensions {
    return RtPixelDimensions(width, height)
}

/**
 * Extension function that converts a [androidx.xr.scenecore.runtime.PixelDimensions] to
 * [IntSize2d].
 */
internal fun RtPixelDimensions.toIntSize2d(): IntSize2d {
    return IntSize2d(width, height)
}

/**
 * Extension function that converts [PlaneOrientation] to [androidx.xr.scenecore.runtime.PlaneType].
 */
internal fun Int.toRtPlaneType(): RtPlaneType {
    return when (this) {
        PlaneOrientation.HORIZONTAL -> RtPlaneType.HORIZONTAL
        PlaneOrientation.VERTICAL -> RtPlaneType.VERTICAL
        PlaneOrientation.ANY -> RtPlaneType.ANY
        else -> error("Unknown Plane Type: $PlaneOrientation")
    }
}

/**
 * Extension function that converts [PlaneSemanticType] to
 * [androidx.xr.scenecore.runtime.PlaneSemantic].
 */
internal fun Int.toRtPlaneSemantic(): RtPlaneSemantic {
    return when (this) {
        PlaneSemanticType.WALL -> RtPlaneSemantic.WALL
        PlaneSemanticType.FLOOR -> RtPlaneSemantic.FLOOR
        PlaneSemanticType.CEILING -> RtPlaneSemantic.CEILING
        PlaneSemanticType.TABLE -> RtPlaneSemantic.TABLE
        PlaneSemanticType.ANY -> RtPlaneSemantic.ANY
        else -> error("Unknown Plane Semantic: $PlaneSemanticType")
    }
}

/**
 * Extension function that converts [Space] value to [androidx.xr.scenecore.runtime.Space] value.
 */
internal fun Space.toRtSpace(): Int {
    return when (this) {
        Space.PARENT -> RtSpace.PARENT
        Space.ACTIVITY -> RtSpace.ACTIVITY
        Space.REAL_WORLD -> RtSpace.REAL_WORLD
        else -> error("Unknown Space Value: $this")
    }
}

/**
 * Extension function that converts a [androidx.xr.scenecore.runtime.MoveEvent] to a [MoveEvent].
 */
internal fun RtMoveEvent.toMoveEvent(entityManager: EntityManager): MoveEvent {

    disposedEntity?.let { entityManager.removeEntity(it) }
    return MoveEvent(
        moveState.toMoveState(),
        Ray(initialInputRay.origin, initialInputRay.direction),
        Ray(currentInputRay.origin, currentInputRay.direction),
        previousPose,
        currentPose,
        previousScale.x,
        currentScale.x,
        entityManager.getEntityForRtEntity(initialParent)!!,
        updatedParent?.let {
            entityManager.getEntityForRtEntity(it)
                ?: AnchorEntity.create(it as RtAnchorEntity, entityManager)
        },
    )
}

/** Extension function that converts a [RtHitInfo] to a [HitInfo]. */
internal fun RtHitInfo.toHitInfo(entityManager: EntityManager): HitInfo? {
    // TODO: b/377541143 - Replace instance equality check in EntityManager.
    val hitEntity = entityManager.getEntityForRtEntity(inputEntity)
    return if (hitEntity == null) {
        null
    } else {
        HitInfo(inputEntity = hitEntity, hitPosition = hitPosition, transform = transform)
    }
}

/**
 * Extension function that converts a [androidx.xr.scenecore.runtime.InputEvent] to a [InputEvent].
 */
internal fun RtInputEvent.toInputEvent(entityManager: EntityManager): InputEvent {
    val hitInfos = mutableListOf<HitInfo>()
    hitInfoList.forEach { it.toHitInfo(entityManager)?.let { element -> hitInfos.add(element) } }
    return InputEvent(
        source.toInputEventSource(),
        pointerType.toInputEventPointer(),
        timestamp,
        origin,
        direction,
        action.toInputEventAction(),
        hitInfos,
    )
}

private fun checkBitfield(value: Int, mask: Int): Boolean = ((value and mask) == mask)

/**
 * Extension function that converts a [androidx.xr.scenecore.runtime.SpatialCapabilities] to a
 * [SpatialCapability].
 */
internal fun RtSpatialCapabilities.toSpatialCapabilities(): Set<SpatialCapability> {
    val caps = HashSet<SpatialCapability>()
    with(RtSpatialCapabilities) {
        if (checkBitfield(capabilities, SPATIAL_CAPABILITY_3D_CONTENT)) {
            caps.add(SpatialCapability.SPATIAL_3D_CONTENT)
        }
        if (checkBitfield(capabilities, SPATIAL_CAPABILITY_APP_ENVIRONMENT)) {
            caps.add(SpatialCapability.APP_ENVIRONMENT)
        }
        if (checkBitfield(capabilities, SPATIAL_CAPABILITY_EMBED_ACTIVITY)) {
            caps.add(SpatialCapability.EMBED_ACTIVITY)
        }
        if (checkBitfield(capabilities, SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL)) {
            caps.add(SpatialCapability.PASSTHROUGH_CONTROL)
        }
        if (checkBitfield(capabilities, SPATIAL_CAPABILITY_SPATIAL_AUDIO)) {
            caps.add(SpatialCapability.SPATIAL_AUDIO)
        }
        if (checkBitfield(capabilities, SPATIAL_CAPABILITY_UI)) {
            caps.add(SpatialCapability.SPATIAL_UI)
        }
    }
    return caps.toSet()
}

/**
 * Extension function that converts a [androidx.xr.scenecore.runtime.SpatialVisibility] to a
 * [SpatialVisibility] constant.
 */
internal fun RtSpatialVisibility.toSpatialVisibility(): SpatialVisibility {
    return visibility.toSpatialVisibilityValue()
}

/**
 * Extension function that converts a [androidx.xr.scenecore.runtime.ResizeEvent] to a
 * [ResizeEvent].
 */
internal fun RtResizeEvent.toResizeEvent(entity: Entity): ResizeEvent {
    return ResizeEvent(entity, resizeState.toResizeState(), newSize.toFloatSize3d())
}

/**
 * Extension function that converts a [Set] of [AnchorPlacement] to a [Set] of
 * [androidx.xr.scenecore.runtime.AnchorPlacement].
 */
internal fun Set<AnchorPlacement>.toRtAnchorPlacement(
    sceneRuntime: SceneRuntime
): Set<RtAnchorPlacement> {
    val rtAnchorPlacementSet = HashSet<RtAnchorPlacement>()
    for (placement in this) {
        val planeOrientationFilter =
            placement.anchorablePlaneOrientations.map { it.toRtPlaneType() }.toMutableSet()
        val planeSemanticFilter =
            placement.anchorablePlaneSemanticTypes.map { it.toRtPlaneSemantic() }.toMutableSet()

        val rtAnchorPlacement =
            sceneRuntime.createAnchorPlacementForPlanes(planeOrientationFilter, planeSemanticFilter)
        rtAnchorPlacementSet.add(rtAnchorPlacement)
    }
    return rtAnchorPlacementSet
}

/** Extension function that converts an ARCore [Plane.Type] to a Scene [PlaneOrientationValue] */
internal fun Plane.Type.toSceneCoreOrientation(): @PlaneOrientationValue Int =
    when (this) {
        Plane.Type.HORIZONTAL_UPWARD_FACING -> PlaneOrientation.HORIZONTAL
        Plane.Type.HORIZONTAL_DOWNWARD_FACING -> PlaneOrientation.HORIZONTAL
        Plane.Type.VERTICAL -> PlaneOrientation.VERTICAL
        else -> error("Unknown plane orientation: $this")
    }

/** Extension function that converts an ARCore [Plane.Label] to a Scene [PlaneSemanticTypeValue] */
internal fun Plane.Label.toSceneCoreSemanticType(): @PlaneSemanticTypeValue Int =
    when (this) {
        Plane.Label.FLOOR -> PlaneSemanticType.FLOOR
        Plane.Label.TABLE -> PlaneSemanticType.TABLE
        Plane.Label.WALL -> PlaneSemanticType.WALL
        Plane.Label.CEILING -> PlaneSemanticType.CEILING
        Plane.Label.UNKNOWN -> PlaneSemanticType.ANY
        else -> error("Unknown semantic type: $this")
    }

/** Extension function that converts a [Int] to [MoveEvent.MoveState]. */
@MoveEvent.MoveState
internal fun Int.toMoveState(): Int {
    return when (this) {
        RtMoveEvent.MOVE_STATE_START -> MoveEvent.MOVE_STATE_START
        RtMoveEvent.MOVE_STATE_ONGOING -> MoveEvent.MOVE_STATE_ONGOING
        RtMoveEvent.MOVE_STATE_END -> MoveEvent.MOVE_STATE_END
        else -> error("Unknown Move State: $this")
    }
}

/** Extension function that converts a [Int] to [ResizeEvent.ResizeState]. */
internal fun Int.toResizeState(): ResizeEvent.ResizeState {
    return when (this) {
        RtResizeEvent.RESIZE_STATE_UNKNOWN -> ResizeEvent.ResizeState.UNKNOWN
        RtResizeEvent.RESIZE_STATE_START -> ResizeEvent.ResizeState.START
        RtResizeEvent.RESIZE_STATE_ONGOING -> ResizeEvent.ResizeState.ONGOING
        RtResizeEvent.RESIZE_STATE_END -> ResizeEvent.ResizeState.END
        else -> error("Unknown Resize State: $this")
    }
}

/** Extension function that converts a [Int] to [InputEvent.Source]. */
internal fun Int.toInputEventSource(): InputEvent.Source {
    return when (this) {
        RtInputEvent.Source.UNKNOWN -> InputEvent.Source.UNKNOWN
        RtInputEvent.Source.HEAD -> InputEvent.Source.HEAD
        RtInputEvent.Source.CONTROLLER -> InputEvent.Source.CONTROLLER
        RtInputEvent.Source.HANDS -> InputEvent.Source.HANDS
        RtInputEvent.Source.MOUSE -> InputEvent.Source.MOUSE
        RtInputEvent.Source.GAZE_AND_GESTURE -> InputEvent.Source.GAZE_AND_GESTURE
        else -> error("Unknown Input Event Source: $this")
    }
}

/** Extension function that converts a [Int] to [InputEvent.Pointer]. */
internal fun Int.toInputEventPointer(): InputEvent.Pointer {
    return when (this) {
        RtInputEvent.Pointer.DEFAULT -> InputEvent.Pointer.DEFAULT
        RtInputEvent.Pointer.LEFT -> InputEvent.Pointer.LEFT
        RtInputEvent.Pointer.RIGHT -> InputEvent.Pointer.RIGHT
        else -> error("Unknown Input Event Pointer Type: $this")
    }
}

/** Extension function that converts a [Int] from RtSpatialVisibility to [SpatialVisibility]. */
internal fun Int.toSpatialVisibilityValue(): SpatialVisibility {
    return when (this) {
        RtSpatialVisibility.UNKNOWN -> SpatialVisibility.UNKNOWN
        RtSpatialVisibility.OUTSIDE_FOV -> SpatialVisibility.OUTSIDE_FIELD_OF_VIEW
        RtSpatialVisibility.PARTIALLY_WITHIN_FOV -> SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW
        RtSpatialVisibility.WITHIN_FOV -> SpatialVisibility.WITHIN_FIELD_OF_VIEW
        else -> error("Unknown Spatial Visibility Value: $this")
    }
}

/** Extension function that converts a [Int] to [InputEvent.Action]. */
internal fun Int.toInputEventAction(): InputEvent.Action {
    return when (this) {
        RtInputEvent.Action.DOWN -> InputEvent.Action.DOWN
        RtInputEvent.Action.UP -> InputEvent.Action.UP
        RtInputEvent.Action.MOVE -> InputEvent.Action.MOVE
        RtInputEvent.Action.CANCEL -> InputEvent.Action.CANCEL
        RtInputEvent.Action.HOVER_MOVE -> InputEvent.Action.HOVER_MOVE
        RtInputEvent.Action.HOVER_ENTER -> InputEvent.Action.HOVER_ENTER
        RtInputEvent.Action.HOVER_EXIT -> InputEvent.Action.HOVER_EXIT
        else -> error("Unknown Input Event Action: $this")
    }
}

@RtTextureSampler.WrapMode
private fun TextureSampler.WrapMode.toRtWrapMode(): Int =
    when (this) {
        TextureSampler.WrapMode.CLAMP_TO_EDGE -> RtTextureSampler.CLAMP_TO_EDGE
        TextureSampler.WrapMode.REPEAT -> RtTextureSampler.REPEAT
        TextureSampler.WrapMode.MIRRORED_REPEAT -> RtTextureSampler.MIRRORED_REPEAT
        else -> error("Unknown TextureSampler Wrap Mode: $this")
    }

@RtTextureSampler.MinFilter
private fun TextureSampler.MinificationFilter.toRtMinFilter(): Int =
    when (this) {
        TextureSampler.MinificationFilter.LINEAR -> RtTextureSampler.MinFilter.LINEAR
        TextureSampler.MinificationFilter.LINEAR_MIPMAP_LINEAR ->
            RtTextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR
        TextureSampler.MinificationFilter.LINEAR_MIPMAP_NEAREST ->
            RtTextureSampler.MinFilter.LINEAR_MIPMAP_NEAREST
        TextureSampler.MinificationFilter.NEAREST -> RtTextureSampler.MinFilter.NEAREST
        TextureSampler.MinificationFilter.LINEAR_MIPMAP_LINEAR ->
            RtTextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR
        TextureSampler.MinificationFilter.LINEAR_MIPMAP_NEAREST ->
            RtTextureSampler.MinFilter.LINEAR_MIPMAP_NEAREST
        else -> error("Unknown TextureSampler Minification Filter: $this")
    }

@RtTextureSampler.MagFilter
private fun TextureSampler.MagnificationFilter.toRtMagFilter(): Int =
    when (this) {
        TextureSampler.MagnificationFilter.LINEAR -> RtTextureSampler.MagFilter.LINEAR
        TextureSampler.MagnificationFilter.NEAREST -> RtTextureSampler.MagFilter.NEAREST
        else -> error("Unknown TextureSampler Magnification Filter: $this")
    }

@RtTextureSampler.CompareMode
private fun TextureSampler.CompareMode.toRtCompareMode(): Int =
    when (this) {
        TextureSampler.CompareMode.COMPARE_TO_TEXTURE ->
            RtTextureSampler.CompareMode.COMPARE_TO_TEXTURE
        TextureSampler.CompareMode.NONE -> RtTextureSampler.CompareMode.NONE
        else -> error("Unknown TextureSampler Compare Mode: $this")
    }

@RtTextureSampler.CompareFunc
private fun TextureSampler.CompareFunction.toRtCompareFunc(): Int =
    when (this) {
        TextureSampler.CompareFunction.ALWAYS -> RtTextureSampler.CompareFunc.A
        TextureSampler.CompareFunction.EQUAL -> RtTextureSampler.CompareFunc.E
        TextureSampler.CompareFunction.GREATER -> RtTextureSampler.CompareFunc.G
        TextureSampler.CompareFunction.GREATER_OR_EQUAL -> RtTextureSampler.CompareFunc.GE
        TextureSampler.CompareFunction.LESSER -> RtTextureSampler.CompareFunc.L
        TextureSampler.CompareFunction.LESSER_OR_EQUAL -> RtTextureSampler.CompareFunc.LE
        TextureSampler.CompareFunction.NEVER -> RtTextureSampler.CompareFunc.N
        TextureSampler.CompareFunction.NOT_EQUAL -> RtTextureSampler.CompareFunc.NE
        else -> error("Unknown TextureSampler Compare Function: $this")
    }

/**
 * Extension function that converts a [TextureSampler] to
 * [androidx.xr.scenecore.runtime.TextureSampler].
 */
internal fun TextureSampler.toRtTextureSampler(): RtTextureSampler {
    return RtTextureSampler(
        wrapModeHorizontal.toRtWrapMode(),
        wrapModeVertical.toRtWrapMode(),
        wrapModeDepth.toRtWrapMode(),
        minificationFilter.toRtMinFilter(),
        magnificationFilter.toRtMagFilter(),
        compareMode.toRtCompareMode(),
        compareFunction.toRtCompareFunc(),
        anisotropyLog2,
    )
}

/** Extension function that converts a [HitTestFilter] to a [RtHitTestFilter]. */
internal fun Int.toRtHitTestFilter(): Int {
    var filters: Int = 0
    if (this and HitTestFilter.OTHER_SCENES != 0) {
        filters = filters or RtHitTestFilter.OTHER_SCENES
    }
    if (this and HitTestFilter.SELF_SCENE != 0) {
        filters = filters or RtHitTestFilter.SELF_SCENE
    }
    return filters
}

/** Extension function that converts a [RtHitTestSurfaceType] to a [HitTestResult.SurfaceType]. */
internal fun Int.toHitTestSurfaceType(): Int {
    return when (this) {
        RtHitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN -> SurfaceType.UNKNOWN
        RtHitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE -> SurfaceType.PLANE
        RtHitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_OBJECT -> SurfaceType.OBJECT
        else -> SurfaceType.UNKNOWN
    }
}

/**
 * Extension function that converts a [androidx.xr.scenecore.runtime.HitTestResult] to a
 * [HitTestResult].
 */
internal fun RtHitTestResult.toHitTestResult(): HitTestResult? {
    if (hitPosition == null) {
        return null
    } else {
        return HitTestResult(
            hitPosition!!,
            surfaceNormal,
            surfaceType.toHitTestSurfaceType(),
            distance,
        )
    }
}

@RtSpatialPointerIconType
internal fun SpatialPointerIcon.toRtSpatialPointerIcon(): Int {
    return when (this) {
        SpatialPointerIcon.NONE -> RtSpatialPointerIcon.TYPE_NONE
        SpatialPointerIcon.CIRCLE -> RtSpatialPointerIcon.TYPE_CIRCLE
        SpatialPointerIcon.DEFAULT -> RtSpatialPointerIcon.TYPE_DEFAULT
        else -> error("Unknown spatial pointer icon type: $this")
    }
}

internal fun Int.toSpatialPointerIcon(): SpatialPointerIcon {
    return when (this) {
        RtSpatialPointerIcon.TYPE_NONE -> SpatialPointerIcon.NONE
        RtSpatialPointerIcon.TYPE_CIRCLE -> SpatialPointerIcon.CIRCLE
        RtSpatialPointerIcon.TYPE_DEFAULT -> SpatialPointerIcon.DEFAULT
        else -> error("Unknown spatial pointer icon type: $this")
    }
}

@RtKhronosPbrMaterialSpec.BlendMode
private fun alphaModeToRtBlendMode(alphaMode: AlphaMode): Int =
    when (alphaMode) {
        AlphaMode.BLEND -> RtKhronosPbrMaterialSpec.BlendMode.TRANSPARENT
        AlphaMode.MASK -> RtKhronosPbrMaterialSpec.BlendMode.MASKED
        AlphaMode.OPAQUE -> RtKhronosPbrMaterialSpec.BlendMode.OPAQUE
        else -> RtKhronosPbrMaterialSpec.BlendMode.OPAQUE
    }

/**
 * Extension function that converts a [AlphaMode] to
 * [androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec].
 */
internal fun AlphaMode.toRtKhronosUnlitMaterialSpec(): RtKhronosPbrMaterialSpec {
    return RtKhronosPbrMaterialSpec(
        lightingModel = RtKhronosPbrMaterialSpec.UNLIT,
        blendMode = alphaModeToRtBlendMode(this),
        doubleSidedMode = RtKhronosPbrMaterialSpec.SINGLE_SIDED,
    )
}

/**
 * Extension function that converts a [AlphaMode] to
 * [androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec].
 */
internal fun AlphaMode.toRtKhronosPbrMaterialSpec(): RtKhronosPbrMaterialSpec {
    return RtKhronosPbrMaterialSpec(
        lightingModel = RtKhronosPbrMaterialSpec.LIT,
        blendMode = alphaModeToRtBlendMode(this),
        doubleSidedMode = RtKhronosPbrMaterialSpec.SINGLE_SIDED,
    )
}

/**
 * Extension function that converts a [androidx.xr.scenecore.runtime.PerceivedResolutionResult] to
 * [PerceivedResolutionResult].
 */
internal fun RtPerceivedResolutionResult.toPerceivedResolutionResult(): PerceivedResolutionResult {
    return when (this) {
        is RtPerceivedResolutionResult.Success ->
            PerceivedResolutionResult.Success(this.perceivedResolution.toIntSize2d())
        is RtPerceivedResolutionResult.EntityTooClose -> PerceivedResolutionResult.EntityTooClose()
        is RtPerceivedResolutionResult.InvalidRenderViewpoint ->
            PerceivedResolutionResult.InvalidRenderViewpoint()
    }
}

internal suspend fun <T> ListenableFuture<T>.awaitSuspending(): T {
    val deferred = CompletableDeferred<T>(coroutineContext[Job])
    val futureBeingAwaited = this

    this.addListener(
        Runnable {
            try {
                deferred.complete(this.get())
            } catch (e: Throwable) {
                Log.e("AwaitSuspending", "ListenableFuture failed: $futureBeingAwaited", e)
                deferred.completeExceptionally(e)
            }
        },
        DirectExecutor,
    )

    return deferred.await()
}

internal object DirectExecutor : Executor {
    override fun execute(command: Runnable) {
        command.run()
    }
}

internal fun RtTriangleMesh.toTriangleMesh(): TriangleMesh {
    return TriangleMesh(positions = positions, texCoords = texCoords, indices = indices)
}

internal fun TriangleMesh.toRtTriangleMesh(): RtTriangleMesh {
    return RtTriangleMesh(positions = positions, texCoords = texCoords, indices = indices)
}
