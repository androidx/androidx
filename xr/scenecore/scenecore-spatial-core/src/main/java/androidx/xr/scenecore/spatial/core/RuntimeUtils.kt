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
package androidx.xr.scenecore.spatial.core

import androidx.annotation.VisibleForTesting
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.HitTestResult as RuntimeHitTestResult
import androidx.xr.scenecore.runtime.InputEvent
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.ResizeEvent
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.SpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialPointerIcon
import androidx.xr.scenecore.runtime.SpatialPointerIconType
import androidx.xr.scenecore.runtime.SpatialVisibility
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.environment.EnvironmentVisibilityState
import com.android.extensions.xr.environment.PassthroughVisibilityState
import com.android.extensions.xr.node.InputEvent as ExtensionsInputEvent
import com.android.extensions.xr.node.Mat4f
import com.android.extensions.xr.node.NodeTransaction
import com.android.extensions.xr.node.Quatf
import com.android.extensions.xr.node.ReformEvent
import com.android.extensions.xr.node.Vec3
import com.android.extensions.xr.space.HitTestResult
import com.android.extensions.xr.space.PerceivedResolution
import com.android.extensions.xr.space.SpatialCapabilities as ExtensionsSpatialCapabilities
import com.android.extensions.xr.space.VisibilityState

internal object RuntimeUtils {
    @VisibleForTesting
    fun getHitInfo(
        xrHitInfo: ExtensionsInputEvent.HitInfo?,
        entityManager: EntityManager,
    ): InputEvent.HitInfo? {
        if (xrHitInfo == null || xrHitInfo.inputNode == null || xrHitInfo.transform == null) {
            return null
        }
        // TODO: b/377541143 - Replace instance equality check in EntityManager.
        val hitEntity = entityManager.getEntityForNode(xrHitInfo.inputNode) ?: return null
        return InputEvent.HitInfo(
            hitEntity,
            if (xrHitInfo.hitPosition == null) null else getVector3(xrHitInfo.hitPosition),
            getMatrix(xrHitInfo.transform),
        )
    }

    /**
     * Converts an XR InputEvent to a SceneCore InputEvent.
     *
     * @param xrInputEvent an [ExtensionsInputEvent] instance to be converted.
     * @param entityManager an [EntityManager] instance to look up entities.
     * @return a [InputEvent] instance representing the input event.
     */
    fun getInputEvent(
        xrInputEvent: ExtensionsInputEvent,
        entityManager: EntityManager,
    ): InputEvent {
        val origin = getVector3(xrInputEvent.origin)
        val direction = getVector3(xrInputEvent.direction)
        // TODO: b/431250469 - Handle unregistered hitInfo nodes.
        val hitInfo = getHitInfo(xrInputEvent.hitInfo, entityManager)
        val secondaryHitInfo = getHitInfo(xrInputEvent.secondaryHitInfo, entityManager)
        val hitInfos = mutableListOf<InputEvent.HitInfo>()
        if (hitInfo != null) {
            hitInfos.add(hitInfo)
        }
        if (secondaryHitInfo != null) {
            hitInfos.add(secondaryHitInfo)
        }

        return InputEvent(
            getInputEventSource(xrInputEvent.source),
            getInputEventPointerType(xrInputEvent.pointerType),
            xrInputEvent.timestamp,
            origin,
            direction,
            getInputEventAction(xrInputEvent.action),
            hitInfos,
        )
    }

    @InputEvent.SourceValue
    fun getInputEventSource(xrInputEventSource: Int): Int {
        return when (xrInputEventSource) {
            ExtensionsInputEvent.SOURCE_UNKNOWN -> InputEvent.Source.UNKNOWN

            ExtensionsInputEvent.SOURCE_HEAD -> InputEvent.Source.HEAD
            ExtensionsInputEvent.SOURCE_CONTROLLER -> InputEvent.Source.CONTROLLER

            ExtensionsInputEvent.SOURCE_HANDS -> InputEvent.Source.HANDS
            ExtensionsInputEvent.SOURCE_MOUSE -> InputEvent.Source.MOUSE
            ExtensionsInputEvent.SOURCE_GAZE_AND_GESTURE -> InputEvent.Source.GAZE_AND_GESTURE

            else ->
                throw IllegalArgumentException("Unknown Input Event Source: $xrInputEventSource")
        }
    }

    @InputEvent.PointerType
    fun getInputEventPointerType(xrInputEventPointerType: Int): Int {
        return when (xrInputEventPointerType) {
            ExtensionsInputEvent.POINTER_TYPE_DEFAULT -> InputEvent.Pointer.DEFAULT

            ExtensionsInputEvent.POINTER_TYPE_LEFT -> InputEvent.Pointer.LEFT

            ExtensionsInputEvent.POINTER_TYPE_RIGHT -> InputEvent.Pointer.RIGHT

            else ->
                throw IllegalArgumentException(
                    "Unknown Input Event Pointer Type: $xrInputEventPointerType"
                )
        }
    }

    @InputEvent.ActionValue
    fun getInputEventAction(xrInputEventAction: Int): Int {
        return when (xrInputEventAction) {
            ExtensionsInputEvent.ACTION_DOWN -> InputEvent.Action.DOWN
            ExtensionsInputEvent.ACTION_UP -> InputEvent.Action.UP
            ExtensionsInputEvent.ACTION_MOVE -> InputEvent.Action.MOVE
            ExtensionsInputEvent.ACTION_CANCEL -> InputEvent.Action.CANCEL

            ExtensionsInputEvent.ACTION_HOVER_MOVE -> InputEvent.Action.HOVER_MOVE

            ExtensionsInputEvent.ACTION_HOVER_ENTER -> InputEvent.Action.HOVER_ENTER

            ExtensionsInputEvent.ACTION_HOVER_EXIT -> InputEvent.Action.HOVER_EXIT

            else ->
                throw IllegalArgumentException("Unknown Input Event Action: $xrInputEventAction")
        }
    }

    @ResizeEvent.ResizeState
    fun getResizeEventState(resizeState: Int): Int {
        return when (resizeState) {
            ReformEvent.REFORM_STATE_UNKNOWN -> ResizeEvent.RESIZE_STATE_UNKNOWN
            ReformEvent.REFORM_STATE_START -> ResizeEvent.RESIZE_STATE_START
            ReformEvent.REFORM_STATE_ONGOING -> ResizeEvent.RESIZE_STATE_ONGOING
            ReformEvent.REFORM_STATE_END -> ResizeEvent.RESIZE_STATE_END
            else -> throw IllegalArgumentException("Unknown Resize State: $resizeState")
        }
    }

    @JvmStatic
    fun getMatrix(xrMatrix: Mat4f): Matrix4 {
        val matrixData = xrMatrix.flattenedMatrix
        return Matrix4(matrixData)
    }

    @JvmStatic
    fun getPose(position: Vec3, quatf: Quatf): Pose {
        return Pose(
            Vector3(position.x, position.y, position.z),
            Quaternion(quatf.x, quatf.y, quatf.z, quatf.w),
        )
    }

    @JvmStatic
    fun getVector3(vec3: Vec3): Vector3 {
        return Vector3(vec3.x, vec3.y, vec3.z)
    }

    fun getQuaternion(quatf: Quatf): Quaternion {
        return Quaternion(quatf.x, quatf.y, quatf.z, quatf.w)
    }

    /**
     * Converts from the Extensions spatial capabilities to the runtime spatial capabilities.
     *
     * @param extCapabilities a [ExtensionsSpatialCapabilities] instance to be converted.
     */
    @JvmStatic
    fun convertSpatialCapabilities(
        extCapabilities: ExtensionsSpatialCapabilities
    ): SpatialCapabilities {
        var capabilities = 0
        if (extCapabilities.get(ExtensionsSpatialCapabilities.SPATIAL_UI_CAPABLE)) {
            capabilities = capabilities or SpatialCapabilities.SPATIAL_CAPABILITY_UI
        }
        if (extCapabilities.get(ExtensionsSpatialCapabilities.SPATIAL_3D_CONTENTS_CAPABLE)) {
            capabilities = capabilities or SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT
        }
        if (extCapabilities.get(ExtensionsSpatialCapabilities.PASSTHROUGH_CONTROL_CAPABLE)) {
            capabilities =
                capabilities or SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL
        }
        if (extCapabilities.get(ExtensionsSpatialCapabilities.APP_ENVIRONMENTS_CAPABLE)) {
            capabilities = capabilities or SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT
        }
        if (extCapabilities.get(ExtensionsSpatialCapabilities.SPATIAL_AUDIO_CAPABLE)) {
            capabilities = capabilities or SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO
        }
        if (extCapabilities.get(ExtensionsSpatialCapabilities.SPATIAL_ACTIVITY_EMBEDDING_CAPABLE)) {
            capabilities = capabilities or SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY
        }

        return SpatialCapabilities(capabilities)
    }

    /**
     * Converts from the Extensions perceived resolution to the runtime perceived resolution.
     *
     * @param extResolution a [PerceivedResolution] instance to be converted.
     */
    @JvmStatic
    fun convertPerceivedResolution(extResolution: PerceivedResolution): PixelDimensions {
        return PixelDimensions(extResolution.width, extResolution.height)
    }

    /**
     * Converts from the Extensions spatial visibility to the runtime spatial visibility.
     *
     * @param extVisibility a [VisibilityState] instance to be converted.
     */
    @JvmStatic
    fun convertSpatialVisibility(extVisibility: Int): SpatialVisibility {
        val visibility: Int =
            when (extVisibility) {
                VisibilityState.UNKNOWN -> SpatialVisibility.UNKNOWN
                VisibilityState.NOT_VISIBLE -> SpatialVisibility.OUTSIDE_FOV
                VisibilityState.PARTIALLY_VISIBLE -> SpatialVisibility.PARTIALLY_WITHIN_FOV
                VisibilityState.FULLY_VISIBLE -> SpatialVisibility.WITHIN_FOV
                else -> throw IllegalArgumentException("Unknown Spatial Visibility: $extVisibility")
            }
        return SpatialVisibility(visibility)
    }

    /**
     * Converts from the Extensions environment visibility state to the runtime environment
     * visibility state.
     *
     * @param environmentState a [ ] instance to be converted.
     */
    @JvmStatic
    fun getIsPreferredSpatialEnvironmentActive(environmentState: Int): Boolean {
        return environmentState == EnvironmentVisibilityState.APP_VISIBLE
    }

    @JvmStatic
    fun getPassthroughOpacity(passthroughVisibilityState: PassthroughVisibilityState): Float {
        val passthroughState = passthroughVisibilityState.currentState
        return if (passthroughState == PassthroughVisibilityState.DISABLED) {
            0.0f
        } else {
            val opacity = passthroughVisibilityState.opacity
            if (opacity > 0.0f) {
                opacity
            } else {
                // When passthrough is enabled, the opacity should be greater than zero.
                1.0f
            }
        }
    }

    private fun getHitTestSurfaceType(extSurfaceType: Int): Int {
        return when (extSurfaceType) {
            HitTestResult.SURFACE_PANEL ->
                RuntimeHitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE

            HitTestResult.SURFACE_3D_OBJECT ->
                RuntimeHitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_OBJECT

            else -> RuntimeHitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN
        }
    }

    /**
     * Converts from the Extensions hit test result to the runtime hit test result.
     *
     * @param hitTestResultExt a [HitTestResult] instance to be converted.
     */
    fun getHitTestResult(hitTestResultExt: HitTestResult): RuntimeHitTestResult {
        val hitPosition =
            if (hitTestResultExt.hitPosition == null) null
            else
                Vector3(
                    hitTestResultExt.hitPosition.x,
                    hitTestResultExt.hitPosition.y,
                    hitTestResultExt.hitPosition.z,
                )
        val surfaceNormal =
            if (hitTestResultExt.surfaceNormal == null) null
            else
                Vector3(
                    hitTestResultExt.surfaceNormal.x,
                    hitTestResultExt.surfaceNormal.y,
                    hitTestResultExt.surfaceNormal.z,
                )
        val surfaceType = getHitTestSurfaceType(hitTestResultExt.surfaceType)
        return RuntimeHitTestResult(
            hitPosition,
            surfaceNormal,
            surfaceType,
            hitTestResultExt.distance,
        )
    }

    fun getHitTestFilter(@ScenePose.HitTestFilterValue hitTestFilter: Int): Int {
        var hitTestFilterResult = 0
        if ((hitTestFilter and ScenePose.HitTestFilter.SELF_SCENE) != 0) {
            hitTestFilterResult =
                hitTestFilterResult or XrExtensions.HIT_TEST_FILTER_INCLUDE_INSIDE_ACTIVITY
        }
        if ((hitTestFilter and ScenePose.HitTestFilter.OTHER_SCENES) != 0) {
            hitTestFilterResult =
                hitTestFilterResult or XrExtensions.HIT_TEST_FILTER_INCLUDE_OUTSIDE_ACTIVITY
        }
        return hitTestFilterResult
    }

    @JvmStatic
    fun convertSpatialPointerIconType(@SpatialPointerIconType rtIconType: Int): Int {
        return when (rtIconType) {
            SpatialPointerIcon.TYPE_NONE -> NodeTransaction.POINTER_ICON_TYPE_NONE
            SpatialPointerIcon.TYPE_DEFAULT -> NodeTransaction.POINTER_ICON_TYPE_DEFAULT
            SpatialPointerIcon.TYPE_CIRCLE -> NodeTransaction.POINTER_ICON_TYPE_CIRCLE
            else -> NodeTransaction.POINTER_ICON_TYPE_DEFAULT
        }
    }

    @JvmStatic
    fun getPositionFromTransform(transform: Matrix4): android.extensions.xr.node.Vec3 {
        return android.extensions.xr.node.Vec3(
            transform.translation.x,
            transform.translation.y,
            transform.translation.z,
        )
    }

    @JvmStatic
    fun getRotationFromTransform(transform: Matrix4): android.extensions.xr.node.Quatf {
        return android.extensions.xr.node.Quatf(
            transform.rotation.x,
            transform.rotation.y,
            transform.rotation.z,
            transform.rotation.w,
        )
    }
}
