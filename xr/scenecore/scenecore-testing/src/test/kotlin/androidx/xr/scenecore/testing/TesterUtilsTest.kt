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

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorEntity.State
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.HitTestResult
import androidx.xr.scenecore.HitTestResult.SurfaceType
import androidx.xr.scenecore.HitTestResult.SurfaceTypeValue
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InputEvent.Action
import androidx.xr.scenecore.InputEvent.HitInfo
import androidx.xr.scenecore.InputEvent.Pointer
import androidx.xr.scenecore.InputEvent.Source
import androidx.xr.scenecore.PerceivedResolutionResult
import androidx.xr.scenecore.PointerCaptureComponent.PointerCaptureState
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.ResizeEvent.ResizeState
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.SpatialVisibility
import androidx.xr.scenecore.SpatializerConstants
import androidx.xr.scenecore.SurfaceEntity.EdgeFeatheringParams
import androidx.xr.scenecore.SurfaceEntity.Shape
import androidx.xr.scenecore.runtime.AnchorEntity.State as RtState
import androidx.xr.scenecore.runtime.Dimensions as RtDimensions
import androidx.xr.scenecore.runtime.HitTestResult.HitTestSurfaceType as RtHitTestSurfaceType
import androidx.xr.scenecore.runtime.InputEvent as RtInputEvent
import androidx.xr.scenecore.runtime.PerceivedResolutionResult as RtPerceivedResolutionResult
import androidx.xr.scenecore.runtime.PointerCaptureComponent.PointerCaptureState as RtPointerCaptureState
import androidx.xr.scenecore.runtime.ResizeEvent as RtResizeEvent
import androidx.xr.scenecore.runtime.SoundFieldAttributes as RtSoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatialCapabilities as RtSpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialVisibility as RtSpatialVisibility
import androidx.xr.scenecore.runtime.SpatializerConstants as RtSpatializerConstants
import androidx.xr.scenecore.runtime.SurfaceEntity.EdgeFeather as RtEdgeFeather
import androidx.xr.scenecore.runtime.SurfaceEntity.Shape as RtShape
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class TesterUtilsTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(
                context = activity,
                coroutineContext = testDispatcher,
                lifecycleOwner = activity as LifecycleOwner,
            )

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    // region Dimensions, Resolution & Resizing

    @Test
    fun perceivedResolutionResultToRtPerceivedResolutionResult_convertsCorrectly() {
        val resultSuccess = PerceivedResolutionResult.Success(IntSize2d(100, 200))
        val resultEntityTooClose = PerceivedResolutionResult.EntityTooClose()
        val resultInvalidRenderViewpoint = PerceivedResolutionResult.InvalidRenderViewpoint()

        assertThat(resultSuccess.toRtPerceivedResolutionResult())
            .isInstanceOf(RtPerceivedResolutionResult.Success::class.java)
        assertThat(resultEntityTooClose.toRtPerceivedResolutionResult())
            .isInstanceOf(RtPerceivedResolutionResult.EntityTooClose::class.java)
        assertThat(resultInvalidRenderViewpoint.toRtPerceivedResolutionResult())
            .isInstanceOf(RtPerceivedResolutionResult.InvalidRenderViewpoint::class.java)
    }

    @Test
    fun resizeStateToRtResizeState_convertsCorrectly() {
        assertThat(ResizeState.UNKNOWN.toRtResizeState())
            .isEqualTo(RtResizeEvent.RESIZE_STATE_UNKNOWN)
        assertThat(ResizeState.START.toRtResizeState()).isEqualTo(RtResizeEvent.RESIZE_STATE_START)
        assertThat(ResizeState.ONGOING.toRtResizeState())
            .isEqualTo(RtResizeEvent.RESIZE_STATE_ONGOING)
        assertThat(ResizeState.END.toRtResizeState()).isEqualTo(RtResizeEvent.RESIZE_STATE_END)
    }

    @Test
    fun resizeEventToRtResizeEvent_convertsCorrectly() {
        val entity = Entity.create(session)
        val resizeState = ResizeState.UNKNOWN
        val size = FloatSize3d(1.0f, 1.0f, 1.0f)
        val rtDimensions = RtDimensions(1.0f, 1.0f, 1.0f)

        val resizeEvent = ResizeEvent(entity, resizeState, size)
        val rtResizeEvent = resizeEvent.toRtResizeEvent()

        assertThat(rtResizeEvent.resizeState).isEqualTo(resizeState.toRtResizeState())
        assertThat(rtResizeEvent.newSize).isEqualTo(rtDimensions)
    }

    // endregion

    // region Entity Anchors & State

    @Test
    fun anchorStateToRtAnchorState_convertsCorrectly() {
        assertThat(State.ANCHORED.toRtState()).isEqualTo(RtState.ANCHORED)
        assertThat(State.UNANCHORED.toRtState()).isEqualTo(RtState.UNANCHORED)
        assertThat(State.TIMED_OUT.toRtState()).isEqualTo(RtState.TIMED_OUT)
        assertThat(State.ERROR.toRtState()).isEqualTo(RtState.ERROR)
    }

    @Test
    fun rtAnchorStateToAnchorState_convertsCorrectly() {
        assertThat(RtState.ANCHORED.toState()).isEqualTo(State.ANCHORED)
        assertThat(RtState.UNANCHORED.toState()).isEqualTo(State.UNANCHORED)
        assertThat(RtState.TIMED_OUT.toState()).isEqualTo(State.TIMED_OUT)
        assertThat(RtState.ERROR.toState()).isEqualTo(State.ERROR)
    }

    // endregion

    // region Geometry (Edges)

    @Test
    fun shapeToRtShape_convertsCorrectly() {
        // 1. Test Quad
        val extents = FloatSize2d(2.0f, 3.0f)
        val cornerRadius = 0.5f
        val quad = Shape.Quad(extents, cornerRadius)
        val rtQuad = quad.toRtShape()

        assertThat(rtQuad).isInstanceOf(RtShape.Quad::class.java)
        rtQuad as RtShape.Quad
        assertThat(rtQuad.extents.width).isEqualTo(extents.width)
        assertThat(rtQuad.extents.height).isEqualTo(extents.height)
        assertThat(rtQuad.cornerRadius).isEqualTo(cornerRadius)

        // 2. Test Sphere
        val sphereRadius = 4.0f
        val sphere = Shape.Sphere(sphereRadius)
        val rtSphere = sphere.toRtShape()

        assertThat(rtSphere).isInstanceOf(RtShape.Sphere::class.java)
        rtSphere as RtShape.Sphere
        assertThat(rtSphere.radius).isEqualTo(sphereRadius)

        // 3. Test Hemisphere
        val hemisphereRadius = 5.0f
        val hemisphere = Shape.Hemisphere(hemisphereRadius)
        val rtHemisphere = hemisphere.toRtShape()

        assertThat(rtHemisphere).isInstanceOf(RtShape.Hemisphere::class.java)
        rtHemisphere as RtShape.Hemisphere
        assertThat(rtHemisphere.radius).isEqualTo(hemisphereRadius)
    }

    @Test
    fun rtShapeToShape_convertsCorrectly() {
        // 1. Test Quad
        val extents = FloatSize2d(2.0f, 3.0f)
        val cornerRadius = 0.5f
        val rtQuad = RtShape.Quad(extents, cornerRadius)
        val quad = rtQuad.toShape()

        assertThat(quad).isInstanceOf(Shape.Quad::class.java)
        quad as Shape.Quad
        assertThat(quad.extents.width).isEqualTo(extents.width)
        assertThat(quad.extents.height).isEqualTo(extents.height)
        assertThat(quad.cornerRadius).isEqualTo(cornerRadius)

        // 2. Test Sphere
        val sphereRadius = 4.0f
        val rtSphere = RtShape.Sphere(sphereRadius)
        val sphere = rtSphere.toShape()

        assertThat(sphere).isInstanceOf(Shape.Sphere::class.java)
        sphere as Shape.Sphere
        assertThat(sphere.radius).isEqualTo(sphereRadius)

        // 3. Test Hemisphere
        val hemisphereRadius = 5.0f
        val rtHemisphere = RtShape.Hemisphere(hemisphereRadius)
        val hemisphere = rtHemisphere.toShape()

        assertThat(hemisphere).isInstanceOf(Shape.Hemisphere::class.java)
        hemisphere as Shape.Hemisphere
        assertThat(hemisphere.radius).isEqualTo(hemisphereRadius)
    }

    @Test
    fun edgeFeatheringParamsToRtEdgeFeather_noFeathering_convertsCorrectly() {
        val noFeathering = EdgeFeatheringParams.NoFeathering()
        val rtEdgeFeather = noFeathering.toRtEdgeFeather()

        assertThat(rtEdgeFeather).isInstanceOf(RtEdgeFeather.NoFeathering::class.java)
    }

    @Test
    fun edgeFeatheringParamsToRtEdgeFeather_rectangleFeather_convertsCorrectly() {
        val rectangleFeather =
            EdgeFeatheringParams.RectangleFeather(leftRight = 1.0f, topBottom = 2.0f)
        val rtEdgeFeather = rectangleFeather.toRtEdgeFeather() as RtEdgeFeather.RectangleFeather

        assertThat(rtEdgeFeather.leftRight).isEqualTo(1.0f)
        assertThat(rtEdgeFeather.topBottom).isEqualTo(2.0f)
    }

    @Test
    fun rtEdgeFeatherToEdgeFeatheringParams_noFeathering_convertsCorrectly() {
        val rtNoFeathering = RtEdgeFeather.NoFeathering()
        val edgeFeatheringParams = rtNoFeathering.toEdgeFeatheringParams()

        assertThat(edgeFeatheringParams).isInstanceOf(EdgeFeatheringParams.NoFeathering::class.java)
    }

    @Test
    fun rtEdgeFeatherToEdgeFeatheringParams_rectangleFeather_convertsCorrectly() {
        val rtRectangleFeather = RtEdgeFeather.RectangleFeather(leftRight = 3.0f, topBottom = 4.0f)
        val edgeFeatheringParams =
            rtRectangleFeather.toEdgeFeatheringParams() as EdgeFeatheringParams.RectangleFeather

        assertThat(edgeFeatheringParams.leftRight).isEqualTo(3.0f)
        assertThat(edgeFeatheringParams.topBottom).isEqualTo(4.0f)
    }

    // endregion

    // region Hit Testing

    @Test
    fun surfaceTypeToRtHitTestSurfaceType_convertsCorrectly() {
        assertThat(SurfaceType.UNKNOWN.toRtHitTestSurfaceType())
            .isEqualTo(RtHitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN)
        assertThat(SurfaceType.PLANE.toRtHitTestSurfaceType())
            .isEqualTo(RtHitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE)
        assertThat(SurfaceType.OBJECT.toRtHitTestSurfaceType())
            .isEqualTo(RtHitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_OBJECT)
    }

    @Test
    fun hitTestResultToRtHitTestResult_convertsCorrectly() {
        val hitPosition = Vector3.One
        val surfaceNormal = Vector3.Right
        @SurfaceTypeValue val surfaceType: Int = SurfaceType.PLANE
        val distance = 1.2f
        val hitTestResult = HitTestResult(hitPosition, surfaceNormal, surfaceType, distance)
        val rtHitTestResult = hitTestResult.toRtHitTestResult()

        assertThat(rtHitTestResult.hitPosition).isEqualTo(hitPosition)
        assertThat(rtHitTestResult.surfaceNormal).isEqualTo(surfaceNormal)
        assertThat(rtHitTestResult.surfaceType).isEqualTo(surfaceType.toRtHitTestSurfaceType())
        assertThat(rtHitTestResult.distance).isEqualTo(distance)
    }

    // endregion

    // region Input Events & Interactions

    @Test
    fun hitInfoToRtHitInfo_convertsCorrectly() {
        val inputEntity: Entity = Entity.create(session)
        val hitPosition: Vector3 = Vector3.One
        val transform: Matrix4 = Matrix4.Identity
        val hitInfo = HitInfo(inputEntity, hitPosition, transform)
        val rtHitInfo = hitInfo.toRtHitInfo()

        assertThat(rtHitInfo.inputEntity).isEqualTo(inputEntity.rtEntity)
        assertThat(rtHitInfo.hitPosition).isEqualTo(hitPosition)
        assertThat(rtHitInfo.transform).isEqualTo(transform)
    }

    @Test
    fun inputEventToRtInputEvent_convertsCorrectly() {
        val source = Source.HANDS
        val pointerType = Pointer.RIGHT
        val timestamp = 1L
        val origin: Vector3 = Vector3.Zero
        val direction: Vector3 = Vector3.Up
        val action: Action = Action.UP

        val inputEntity: Entity = Entity.create(session)
        val hitPosition: Vector3 = Vector3.One
        val transform: Matrix4 = Matrix4.Identity
        val hitInfo = HitInfo(inputEntity, hitPosition, transform)
        val hitInfoList: List<HitInfo> = listOf(hitInfo)

        val inputEvent =
            InputEvent(source, pointerType, timestamp, origin, direction, action, hitInfoList)
        val rtInputEvent = inputEvent.toRtInputEvent()

        assertThat(rtInputEvent.source).isEqualTo(source.toRtInputEventSource())
        assertThat(rtInputEvent.pointerType).isEqualTo(pointerType.toRtInputEventPointer())
        assertThat(rtInputEvent.timestamp).isEqualTo(timestamp)
        assertThat(rtInputEvent.origin).isEqualTo(origin)
        assertThat(rtInputEvent.direction).isEqualTo(direction)
        assertThat(rtInputEvent.action).isEqualTo(action.toRtInputEventAction())
        assertThat(rtInputEvent.hitInfoList[0].inputEntity).isEqualTo(inputEntity.rtEntity)
        assertThat(rtInputEvent.hitInfoList[0].hitPosition).isEqualTo(hitPosition)
        assertThat(rtInputEvent.hitInfoList[0].transform).isEqualTo(transform)
    }

    @Test
    fun inputEventSourceToRtInputEventSource_convertsCorrectly() {
        assertThat(Source.UNKNOWN.toRtInputEventSource()).isEqualTo(RtInputEvent.Source.UNKNOWN)
        assertThat(Source.HEAD.toRtInputEventSource()).isEqualTo(RtInputEvent.Source.HEAD)
        assertThat(Source.CONTROLLER.toRtInputEventSource())
            .isEqualTo(RtInputEvent.Source.CONTROLLER)
        assertThat(Source.HANDS.toRtInputEventSource()).isEqualTo(RtInputEvent.Source.HANDS)
        assertThat(Source.MOUSE.toRtInputEventSource()).isEqualTo(RtInputEvent.Source.MOUSE)
        assertThat(Source.GAZE_AND_GESTURE.toRtInputEventSource())
            .isEqualTo(RtInputEvent.Source.GAZE_AND_GESTURE)
    }

    @Test
    fun inputEventPointerToRtInputEventPointer_convertsCorrectly() {
        assertThat(Pointer.DEFAULT.toRtInputEventPointer()).isEqualTo(RtInputEvent.Pointer.DEFAULT)
        assertThat(Pointer.LEFT.toRtInputEventPointer()).isEqualTo(RtInputEvent.Pointer.LEFT)
        assertThat(Pointer.RIGHT.toRtInputEventPointer()).isEqualTo(RtInputEvent.Pointer.RIGHT)
    }

    @Test
    fun inputEventActionToRtInputEventAction_convertsCorrectly() {
        assertThat(Action.DOWN.toRtInputEventAction()).isEqualTo(RtInputEvent.Action.DOWN)
        assertThat(Action.UP.toRtInputEventAction()).isEqualTo(RtInputEvent.Action.UP)
        assertThat(Action.MOVE.toRtInputEventAction()).isEqualTo(RtInputEvent.Action.MOVE)
        assertThat(Action.CANCEL.toRtInputEventAction()).isEqualTo(RtInputEvent.Action.CANCEL)
        assertThat(Action.HOVER_MOVE.toRtInputEventAction())
            .isEqualTo(RtInputEvent.Action.HOVER_MOVE)
        assertThat(Action.HOVER_ENTER.toRtInputEventAction())
            .isEqualTo(RtInputEvent.Action.HOVER_ENTER)
        assertThat(Action.HOVER_EXIT.toRtInputEventAction())
            .isEqualTo(RtInputEvent.Action.HOVER_EXIT)
    }

    @Test
    fun pointerCaptureStateToRtPointerCaptureState_convertsCorrectly() {
        assertThat(PointerCaptureState.PAUSED.toRtPointerCaptureState())
            .isEqualTo(RtPointerCaptureState.POINTER_CAPTURE_STATE_PAUSED)
        assertThat(PointerCaptureState.ACTIVE.toRtPointerCaptureState())
            .isEqualTo(RtPointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE)
        assertThat(PointerCaptureState.STOPPED.toRtPointerCaptureState())
            .isEqualTo(RtPointerCaptureState.POINTER_CAPTURE_STATE_STOPPED)
    }

    @Test
    fun rtPointerCaptureStateToPointerCaptureState_convertsCorrectly() {
        assertThat(RtPointerCaptureState.POINTER_CAPTURE_STATE_PAUSED.toPointerCaptureState())
            .isEqualTo(PointerCaptureState.PAUSED)
        assertThat(RtPointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE.toPointerCaptureState())
            .isEqualTo(PointerCaptureState.ACTIVE)
        assertThat(RtPointerCaptureState.POINTER_CAPTURE_STATE_STOPPED.toPointerCaptureState())
            .isEqualTo(PointerCaptureState.STOPPED)
    }

    // endregion

    // region Spatial Audio

    @Test
    fun sourceTypeToRtSourceType_convertsCorrectly() {
        assertThat(SpatializerConstants.SourceType.POINT_SOURCE.toRtSourceType())
            .isEqualTo(RtSpatializerConstants.SourceType.SOURCE_TYPE_POINT_SOURCE)
        assertThat(SpatializerConstants.SourceType.SOUND_FIELD.toRtSourceType())
            .isEqualTo(RtSpatializerConstants.SourceType.SOURCE_TYPE_SOUND_FIELD)
        assertThat(SpatializerConstants.SourceType.DEFAULT.toRtSourceType())
            .isEqualTo(RtSpatializerConstants.SourceType.SOURCE_TYPE_BYPASS)
    }

    @Test
    fun rtSourceTypeToSourceType_convertsCorrectly() {
        assertThat(RtSpatializerConstants.SourceType.SOURCE_TYPE_POINT_SOURCE.toSourceType())
            .isEqualTo(SpatializerConstants.SourceType.POINT_SOURCE)
        assertThat(RtSpatializerConstants.SourceType.SOURCE_TYPE_SOUND_FIELD.toSourceType())
            .isEqualTo(SpatializerConstants.SourceType.SOUND_FIELD)
        assertThat(RtSpatializerConstants.SourceType.SOURCE_TYPE_BYPASS.toSourceType())
            .isEqualTo(SpatializerConstants.SourceType.DEFAULT)
    }

    @Test
    fun ambisonicsOrderToRtAmbisonicsOrder_convertsCorrectly() {
        assertThat(SpatializerConstants.AmbisonicsOrder.FIRST_ORDER.toRtAmbisonicsOrder())
            .isEqualTo(RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_FIRST_ORDER)
        assertThat(SpatializerConstants.AmbisonicsOrder.SECOND_ORDER.toRtAmbisonicsOrder())
            .isEqualTo(RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_SECOND_ORDER)
        assertThat(SpatializerConstants.AmbisonicsOrder.THIRD_ORDER.toRtAmbisonicsOrder())
            .isEqualTo(RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_THIRD_ORDER)
    }

    @Test
    fun rtAmbisonicsOrderToAmbisonicsOrder_convertsCorrectly() {
        assertThat(
                RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_FIRST_ORDER
                    .toAmbisonicsOrder()
            )
            .isEqualTo(SpatializerConstants.AmbisonicsOrder.FIRST_ORDER)
        assertThat(
                RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_SECOND_ORDER
                    .toAmbisonicsOrder()
            )
            .isEqualTo(SpatializerConstants.AmbisonicsOrder.SECOND_ORDER)
        assertThat(
                RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_THIRD_ORDER
                    .toAmbisonicsOrder()
            )
            .isEqualTo(SpatializerConstants.AmbisonicsOrder.THIRD_ORDER)
    }

    @Test
    fun rtSoundFieldAttributesToSoundFieldAttributes_convertsCorrectly() {
        val rtAmbisonicsOrder = RtSpatializerConstants.AmbisonicsOrder.AMBISONICS_ORDER_FIRST_ORDER
        val rtSoundFieldAttributes = RtSoundFieldAttributes(rtAmbisonicsOrder)
        val soundFieldAttributes = rtSoundFieldAttributes.toSoundFieldAttributes()

        assertThat(soundFieldAttributes.order).isEqualTo(rtAmbisonicsOrder.toAmbisonicsOrder())
    }

    // endregion

    // region Capabilities and Visibility

    @Test
    fun spatialCapabilitySetToRtSpatialCapabilities_convertsCorrectly() {
        val capabilities =
            setOf(SpatialCapability.SPATIAL_3D_CONTENT, SpatialCapability.SPATIAL_AUDIO)

        val rtCapabilities = capabilities.toRtSpatialCapabilities()
        val expectedMask =
            RtSpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT or
                RtSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO

        assertThat(rtCapabilities.capabilities).isEqualTo(expectedMask)
    }

    @Test
    fun spatialVisibilityToRtSpatialVisibility_convertsCorrectly() {
        assertThat(SpatialVisibility.UNKNOWN.toRtSpatialVisibility().visibility)
            .isEqualTo(RtSpatialVisibility.UNKNOWN)
        assertThat(SpatialVisibility.OUTSIDE_FIELD_OF_VIEW.toRtSpatialVisibility().visibility)
            .isEqualTo(RtSpatialVisibility.OUTSIDE_FOV)
        assertThat(
                SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW.toRtSpatialVisibility().visibility
            )
            .isEqualTo(RtSpatialVisibility.PARTIALLY_WITHIN_FOV)
        assertThat(SpatialVisibility.WITHIN_FIELD_OF_VIEW.toRtSpatialVisibility().visibility)
            .isEqualTo(RtSpatialVisibility.WITHIN_FOV)
    }

    // endregion
}
