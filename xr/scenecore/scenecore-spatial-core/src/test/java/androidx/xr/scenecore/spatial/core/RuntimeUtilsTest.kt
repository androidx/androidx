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

import android.app.Activity
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.runtime.InputEvent as RuntimeInputEvent
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.ResizeEvent
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.SpatialCapabilities as RuntimeSpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialPointerIcon
import androidx.xr.scenecore.runtime.SpatialVisibility
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.environment.EnvironmentVisibilityState
import com.android.extensions.xr.environment.PassthroughVisibilityState
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState
import com.android.extensions.xr.node.InputEvent
import com.android.extensions.xr.node.Mat4f
import com.android.extensions.xr.node.NodeTransaction
import com.android.extensions.xr.node.ReformEvent
import com.android.extensions.xr.node.Vec3
import com.android.extensions.xr.space.HitTestResult
import com.android.extensions.xr.space.PerceivedResolution
import com.android.extensions.xr.space.ShadowSpatialCapabilities
import com.android.extensions.xr.space.SpatialCapabilities
import com.android.extensions.xr.space.VisibilityState
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RuntimeUtilsTest {

    fun createSceneRuntime(entityManager: EntityManager): SpatialSceneRuntime {
        val activityController: ActivityController<Activity> =
            Robolectric.buildActivity(Activity::class.java)
        val activity: Activity = activityController.create().start().get()

        val fakeExecutor = FakeScheduledExecutorService()
        val xrExtensions = getXrExtensions()
        checkNotNull(xrExtensions) { "XrExtensions is null. Stop testing" }
        return SpatialSceneRuntime.create(activity, fakeExecutor, xrExtensions, entityManager)
    }

    @Test
    fun getMatrix_returnsMatrix() {
        val expected =
            floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f)
        val matrix = Mat4f(expected)

        Truth.assertThat(RuntimeUtils.getMatrix(matrix).data)
            .usingExactEquality()
            .containsExactly(Matrix4(expected).data)
            .inOrder()
    }

    @Test
    fun convertSpatialCapabilities_noCapabilities() {
        val extensionCapabilities: SpatialCapabilities =
            ShadowSpatialCapabilities.create(*byteArrayOf())
        val caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities)

        Truth.assertThat(caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_UI))
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(
                    RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL
                )
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
            )
            .isFalse()
    }

    @Test
    fun convertSpatialCapabilities_allCapabilities() {
        val extensionCapabilities = ShadowSpatialCapabilities.createAll()
        val caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities)

        Truth.assertThat(caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_UI))
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)
            )
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(
                    RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL
                )
            )
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT)
            )
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO)
            )
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
            )
            .isTrue()

        Truth.assertThat(
                caps.hasCapability(
                    (RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_UI or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
                )
            )
            .isTrue()
    }

    @Test
    fun convertSpatialCapabilities_singleCapability() {
        // check conversions of a few different instances of the extensions SpatialCapabilities that
        // each have exactly one capability.
        var extensionCapabilities =
            ShadowSpatialCapabilities.create(SpatialCapabilities.SPATIAL_UI_CAPABLE)
        var caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities)

        Truth.assertThat(caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_UI))
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(
                    RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL
                )
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
            )
            .isFalse()

        extensionCapabilities =
            ShadowSpatialCapabilities.create(SpatialCapabilities.SPATIAL_3D_CONTENTS_CAPABLE)
        caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities)

        Truth.assertThat(caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_UI))
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)
            )
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(
                    RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL
                )
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
            )
            .isFalse()

        extensionCapabilities =
            ShadowSpatialCapabilities.create(SpatialCapabilities.SPATIAL_AUDIO_CAPABLE)
        caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities)

        Truth.assertThat(caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_UI))
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(
                    RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL
                )
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO)
            )
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
            )
            .isFalse()
    }

    @Test
    fun convertSpatialCapabilities_mixedCapabilities() {
        // Check conversions for a couple of different combinations of capabilities.
        var extensionCapabilities =
            ShadowSpatialCapabilities.create(
                SpatialCapabilities.SPATIAL_AUDIO_CAPABLE,
                SpatialCapabilities.SPATIAL_3D_CONTENTS_CAPABLE,
            )
        var caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities)

        Truth.assertThat(caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_UI))
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)
            )
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(
                    RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL
                )
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO)
            )
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
            )
            .isFalse()

        extensionCapabilities =
            ShadowSpatialCapabilities.create(
                SpatialCapabilities.SPATIAL_UI_CAPABLE,
                SpatialCapabilities.PASSTHROUGH_CONTROL_CAPABLE,
                SpatialCapabilities.APP_ENVIRONMENTS_CAPABLE,
                SpatialCapabilities.SPATIAL_ACTIVITY_EMBEDDING_CAPABLE,
            )
        caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities)

        Truth.assertThat(caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_UI))
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(
                    RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL
                )
            )
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT)
            )
            .isTrue()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO)
            )
            .isFalse()
        Truth.assertThat(
                caps.hasCapability(RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
            )
            .isTrue()

        // Assert checking as a combination works too
        Truth.assertThat(
                caps.hasCapability(
                    (RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_UI or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
                )
            )
            .isTrue()

        Truth.assertThat(
                caps.hasCapability(
                    RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT or
                        RuntimeSpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO
                )
            )
            .isFalse()
    }

    @Test
    fun getIsPreferredSpatialEnvironmentActive_convertsFromExtensionState() {
        Truth.assertThat(
                RuntimeUtils.getIsPreferredSpatialEnvironmentActive(
                    EnvironmentVisibilityState.INVISIBLE
                )
            )
            .isFalse()

        Truth.assertThat(
                RuntimeUtils.getIsPreferredSpatialEnvironmentActive(
                    EnvironmentVisibilityState.HOME_VISIBLE
                )
            )
            .isFalse()

        Truth.assertThat(
                RuntimeUtils.getIsPreferredSpatialEnvironmentActive(
                    EnvironmentVisibilityState.APP_VISIBLE
                )
            )
            .isTrue()
    }

    @Test
    fun getPassthroughOpacity_returnsZeroFromDisabledExtensionState() {
        var passthroughVisibilityState =
            ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.DISABLED, 0.0f)

        Truth.assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState))
            .isEqualTo(0.0f)

        passthroughVisibilityState =
            ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.DISABLED, 1.0f)

        Truth.assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState))
            .isEqualTo(0.0f)
    }

    @Test
    fun getPassthroughOpacity_convertsValidValuesFromExtensionState() {
        var passthroughVisibilityState =
            ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.HOME, 0.5f)

        Truth.assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState))
            .isEqualTo(0.5f)

        passthroughVisibilityState =
            ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.APP, 0.75f)

        Truth.assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState))
            .isEqualTo(0.75f)

        passthroughVisibilityState =
            ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.SYSTEM, 1.0f)

        Truth.assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState))
            .isEqualTo(1.0f)
    }

    @Test
    fun getPassthroughOpacity_convertsInvalidValuesFromExtensionStateToOneAndLogsError() {
        var passthroughVisibilityState =
            ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.HOME, 0.0f)

        Truth.assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState))
            .isEqualTo(1.0f)

        passthroughVisibilityState =
            ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.APP, -0.0000001f)

        Truth.assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState))
            .isEqualTo(1.0f)

        passthroughVisibilityState =
            ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.SYSTEM, -1.0f)

        Truth.assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState))
            .isEqualTo(1.0f)

        // Log is removed from RuntimeUtils
        // expectedLogMessagesRule.expectLogMessagePattern(
        //        Log.ERROR,
        //        "RuntimeUtils",
        //        Pattern.compile(".* Opacity should be greater than zero.*"));
    }

    @Test
    fun getHitInfo_convertsFromHitInfo() {
        val entityManager = EntityManager()
        val sceneRuntime = createSceneRuntime(entityManager)
        val testEntity =
            sceneRuntime.createGroupEntity(Pose(), "testGroup", sceneRuntime.activitySpace)
        val testNode = (testEntity as AndroidXrEntity).getNode()

        val expectedTransform =
            floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f)
        val transform = Mat4f(expectedTransform)
        val expectedHitPosition = Vector3(1f, 2f, 3f)
        val hitPosition = Vec3(1f, 2f, 3f)

        val extensionHitInfo = InputEvent.HitInfo(1, testNode, transform, hitPosition)
        val hitInfo = RuntimeUtils.getHitInfo(extensionHitInfo, entityManager)

        Truth.assertThat(hitInfo).isNotNull()
        Truth.assertThat(hitInfo!!.inputEntity).isEqualTo(testEntity)
        Truth.assertThat(hitInfo.hitPosition).isNotNull()
        assertVector3(hitInfo.hitPosition!!, expectedHitPosition)
        Truth.assertThat(hitInfo.transform.data)
            .usingExactEquality()
            .containsExactly(Matrix4(expectedTransform).data)
            .inOrder()
    }

    @Test
    fun getHitInfo_nullHitInfo_returnsNull() {
        val entityManager = EntityManager()

        Truth.assertThat(RuntimeUtils.getHitInfo(null, entityManager)).isNull()
    }

    @Test
    fun getHitInfo_unKnownNode_returnsNull() {
        val entityManager = EntityManager()
        val sceneRuntime = createSceneRuntime(entityManager)
        sceneRuntime.createGroupEntity(Pose(), "testGroup", sceneRuntime.activitySpace)
        val testNode = getXrExtensions()!!.createNode()

        val transformData =
            floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f)
        val transform = Mat4f(transformData)
        val hitPosition = Vec3(1f, 2f, 3f)

        val extensionHitInfo = InputEvent.HitInfo(1, testNode, transform, hitPosition)
        val hitInfo = RuntimeUtils.getHitInfo(extensionHitInfo, entityManager)

        Truth.assertThat(hitInfo).isNull()
    }

    @Test
    fun getHitInfo_nullHitPosition_convertsFromHitInfo() {
        val entityManager = EntityManager()
        val sceneRuntime = createSceneRuntime(entityManager)
        val testEntity =
            sceneRuntime.createGroupEntity(Pose(), "testGroup", sceneRuntime.activitySpace)
        val testNode = (testEntity as AndroidXrEntity).getNode()

        val expectedTransform =
            floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f)
        val transform = Mat4f(expectedTransform)
        val hitPosition: Vec3? = null

        val extensionHitInfo = InputEvent.HitInfo(1, testNode, transform, hitPosition)
        val hitInfo = RuntimeUtils.getHitInfo(extensionHitInfo, entityManager)

        Truth.assertThat(hitInfo).isNotNull()
        Truth.assertThat(hitInfo!!.inputEntity).isNotNull()
        Truth.assertThat(hitInfo.inputEntity).isEqualTo(testEntity)
        Truth.assertThat(hitInfo.hitPosition).isNull()
        Truth.assertThat(hitInfo.transform.data)
            .usingExactEquality()
            .containsExactly(Matrix4(expectedTransform).data)
            .inOrder()
    }

    @Test
    fun getInputEventSource_convertsFromExtensionSource() {
        Truth.assertThat(RuntimeUtils.getInputEventSource(InputEvent.SOURCE_UNKNOWN))
            .isEqualTo(RuntimeInputEvent.Source.UNKNOWN)
        Truth.assertThat(RuntimeUtils.getInputEventSource(InputEvent.SOURCE_HEAD))
            .isEqualTo(RuntimeInputEvent.Source.HEAD)
        Truth.assertThat(RuntimeUtils.getInputEventSource(InputEvent.SOURCE_CONTROLLER))
            .isEqualTo(RuntimeInputEvent.Source.CONTROLLER)
        Truth.assertThat(RuntimeUtils.getInputEventSource(InputEvent.SOURCE_HANDS))
            .isEqualTo(RuntimeInputEvent.Source.HANDS)
        Truth.assertThat(RuntimeUtils.getInputEventSource(InputEvent.SOURCE_MOUSE))
            .isEqualTo(RuntimeInputEvent.Source.MOUSE)
        Truth.assertThat(RuntimeUtils.getInputEventSource(InputEvent.SOURCE_GAZE_AND_GESTURE))
            .isEqualTo(RuntimeInputEvent.Source.GAZE_AND_GESTURE)
    }

    @Test
    fun getInputEventSource_throwsExceptionForInvalidValue() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            RuntimeUtils.getInputEventSource(100)
        }
    }

    @Test
    fun getInputEventPointerType_convertsFromExtensionPointerType() {
        Truth.assertThat(RuntimeUtils.getInputEventPointerType(InputEvent.POINTER_TYPE_DEFAULT))
            .isEqualTo(RuntimeInputEvent.Pointer.DEFAULT)
        Truth.assertThat(RuntimeUtils.getInputEventPointerType(InputEvent.POINTER_TYPE_LEFT))
            .isEqualTo(RuntimeInputEvent.Pointer.LEFT)
        Truth.assertThat(RuntimeUtils.getInputEventPointerType(InputEvent.POINTER_TYPE_RIGHT))
            .isEqualTo(RuntimeInputEvent.Pointer.RIGHT)
    }

    @Test
    fun getInputEventPointerType_throwsExceptionForInvalidValue() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            RuntimeUtils.getInputEventPointerType(100)
        }
    }

    @Test
    fun getInputEventAction_convertsFromExtensionAction() {
        Truth.assertThat(RuntimeUtils.getInputEventAction(InputEvent.ACTION_DOWN))
            .isEqualTo(RuntimeInputEvent.Action.DOWN)
        Truth.assertThat(RuntimeUtils.getInputEventAction(InputEvent.ACTION_UP))
            .isEqualTo(RuntimeInputEvent.Action.UP)
        Truth.assertThat(RuntimeUtils.getInputEventAction(InputEvent.ACTION_MOVE))
            .isEqualTo(RuntimeInputEvent.Action.MOVE)
        Truth.assertThat(RuntimeUtils.getInputEventAction(InputEvent.ACTION_CANCEL))
            .isEqualTo(RuntimeInputEvent.Action.CANCEL)
        Truth.assertThat(RuntimeUtils.getInputEventAction(InputEvent.ACTION_HOVER_MOVE))
            .isEqualTo(RuntimeInputEvent.Action.HOVER_MOVE)
        Truth.assertThat(RuntimeUtils.getInputEventAction(InputEvent.ACTION_HOVER_ENTER))
            .isEqualTo(RuntimeInputEvent.Action.HOVER_ENTER)
        Truth.assertThat(RuntimeUtils.getInputEventAction(InputEvent.ACTION_HOVER_EXIT))
            .isEqualTo(RuntimeInputEvent.Action.HOVER_EXIT)
    }

    @Test
    fun getInputEventAction_throwsExceptionForInvalidValue() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            RuntimeUtils.getInputEventAction(100)
        }
    }

    @Test
    fun getResizeEventState_convertsFromExtensionResizeState() {
        Truth.assertThat(RuntimeUtils.getResizeEventState(ReformEvent.REFORM_STATE_UNKNOWN))
            .isEqualTo(ResizeEvent.RESIZE_STATE_UNKNOWN)
        Truth.assertThat(RuntimeUtils.getResizeEventState(ReformEvent.REFORM_STATE_START))
            .isEqualTo(ResizeEvent.RESIZE_STATE_START)
        Truth.assertThat(RuntimeUtils.getResizeEventState(ReformEvent.REFORM_STATE_ONGOING))
            .isEqualTo(ResizeEvent.RESIZE_STATE_ONGOING)
        Truth.assertThat(RuntimeUtils.getResizeEventState(ReformEvent.REFORM_STATE_END))
            .isEqualTo(ResizeEvent.RESIZE_STATE_END)
    }

    @Test
    fun getResizeEventState_throwsExceptionForInvalidValue() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            RuntimeUtils.getResizeEventState(100)
        }
    }

    @Test
    fun getHitTestResult_convertsFromExtensionHitTestResult() {
        val distance = 2.0f
        val hitPosition = Vec3(1.0f, 2.0f, 3.0f)
        val surfaceNormal = Vec3(4.0f, 5.0f, 6.0f)
        val surfaceType = HitTestResult.SURFACE_PANEL

        val hitTestResultBuilder = HitTestResult.Builder(distance, hitPosition, true, surfaceType)
        val extensionsHitTestResult = hitTestResultBuilder.setSurfaceNormal(surfaceNormal).build()

        val hitTestResult = RuntimeUtils.getHitTestResult(extensionsHitTestResult)

        Truth.assertThat(hitTestResult.distance).isEqualTo(distance)
        Truth.assertThat(hitTestResult.hitPosition).isNotNull()
        assertVector3(hitTestResult.hitPosition!!, Vector3(1f, 2f, 3f))
        Truth.assertThat(hitTestResult.surfaceNormal).isNotNull()
        assertVector3(hitTestResult.surfaceNormal!!, Vector3(4f, 5f, 6f))
        Truth.assertThat(hitTestResult.surfaceType)
            .isEqualTo(
                androidx.xr.scenecore.runtime.HitTestResult.HitTestSurfaceType
                    .HIT_TEST_RESULT_SURFACE_TYPE_PLANE
            )
    }

    @Test
    fun getHitTestResult_convertsFromExtensionHitTestResult_withNoHit() {
        val distance = Float.POSITIVE_INFINITY
        val expectedNoHitPosition = Vector3(0.0f, 0.0f, 0.0f)
        val hitPosition = Vec3(0.0f, 0.0f, 0.0f)
        val surfaceType = HitTestResult.SURFACE_UNKNOWN

        val hitTestResultBuilder = HitTestResult.Builder(distance, hitPosition, true, surfaceType)
        val extensionsHitTestResult = hitTestResultBuilder.build()

        val hitTestResult = RuntimeUtils.getHitTestResult(extensionsHitTestResult)

        Truth.assertThat(hitTestResult.distance).isEqualTo(distance)
        Truth.assertThat(hitTestResult.hitPosition).isEqualTo(expectedNoHitPosition)
        Truth.assertThat(hitTestResult.surfaceNormal).isNull()
        Truth.assertThat(hitTestResult.surfaceType)
            .isEqualTo(
                androidx.xr.scenecore.runtime.HitTestResult.HitTestSurfaceType
                    .HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN
            )
    }

    @Test
    fun getHitTestFilter_convertsToExtensionHitTestFilter_noFilter() {
        @ScenePose.HitTestFilterValue val hitTestFilter = 0
        val expectedHitTestFilter = 0

        val extensionsHitTestFilter = RuntimeUtils.getHitTestFilter(hitTestFilter)

        Truth.assertThat(extensionsHitTestFilter).isEqualTo(expectedHitTestFilter)
    }

    @Test
    fun getHitTestFilter_convertsToExtensionHitTestFilter_oneFilter() {
        @ScenePose.HitTestFilterValue val hitTestFilter = ScenePose.HitTestFilter.OTHER_SCENES
        val expectedHitTestFilter = XrExtensions.HIT_TEST_FILTER_INCLUDE_OUTSIDE_ACTIVITY

        val extensionsHitTestFilter = RuntimeUtils.getHitTestFilter(hitTestFilter)

        Truth.assertThat(extensionsHitTestFilter).isEqualTo(expectedHitTestFilter)
    }

    @Test
    fun getHitTestFilter_convertsToExtensionHitTestFilter_multipleFilters() {
        @ScenePose.HitTestFilterValue
        val hitTestFilter =
            ScenePose.HitTestFilter.SELF_SCENE or ScenePose.HitTestFilter.OTHER_SCENES
        val expectedHitTestFilter =
            (XrExtensions.HIT_TEST_FILTER_INCLUDE_INSIDE_ACTIVITY or
                XrExtensions.HIT_TEST_FILTER_INCLUDE_OUTSIDE_ACTIVITY)

        val extensionsHitTestFilter = RuntimeUtils.getHitTestFilter(hitTestFilter)

        Truth.assertThat(extensionsHitTestFilter).isEqualTo(expectedHitTestFilter)
    }

    @Test
    fun convertSpatialVisibility_convertsFromExtensionVisibility() {
        Truth.assertThat(RuntimeUtils.convertSpatialVisibility(VisibilityState.FULLY_VISIBLE))
            .isEqualTo(SpatialVisibility(SpatialVisibility.WITHIN_FOV))

        Truth.assertThat(RuntimeUtils.convertSpatialVisibility(VisibilityState.PARTIALLY_VISIBLE))
            .isEqualTo(SpatialVisibility(SpatialVisibility.PARTIALLY_WITHIN_FOV))

        Truth.assertThat(RuntimeUtils.convertSpatialVisibility(VisibilityState.NOT_VISIBLE))
            .isEqualTo(SpatialVisibility(SpatialVisibility.OUTSIDE_FOV))

        Truth.assertThat(RuntimeUtils.convertSpatialVisibility(VisibilityState.UNKNOWN))
            .isEqualTo(SpatialVisibility(SpatialVisibility.UNKNOWN))
    }

    @Test
    fun convertSpatialVisibility_throwsExceptionForInvalidValue() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            RuntimeUtils.convertSpatialVisibility(100)
        }
    }

    @Test
    fun convertPerceivedResolution_convertsFromExtension() {
        Truth.assertThat(RuntimeUtils.convertPerceivedResolution(PerceivedResolution(100, 200)))
            .isEqualTo(PixelDimensions(100, 200))
    }

    @Test
    fun convertSpatialPointerIconType_convertsFromRuntimeIconType() {
        Truth.assertThat(RuntimeUtils.convertSpatialPointerIconType(SpatialPointerIcon.TYPE_NONE))
            .isEqualTo(NodeTransaction.POINTER_ICON_TYPE_NONE)
        Truth.assertThat(
                RuntimeUtils.convertSpatialPointerIconType(SpatialPointerIcon.TYPE_DEFAULT)
            )
            .isEqualTo(NodeTransaction.POINTER_ICON_TYPE_DEFAULT)
        Truth.assertThat(RuntimeUtils.convertSpatialPointerIconType(SpatialPointerIcon.TYPE_CIRCLE))
            .isEqualTo(NodeTransaction.POINTER_ICON_TYPE_CIRCLE)
    }

    @Test
    fun getPositionFromTransform_returnsCorrectPosition() {
        val transformData =
            floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, -10.5f, 20.1f, -30.0f, 1f)
        val transform = Matrix4(transformData)

        val position = RuntimeUtils.getPositionFromTransform(transform)

        Truth.assertThat(position.x).isEqualTo(-10.5f)
        Truth.assertThat(position.y).isEqualTo(20.1f)
        Truth.assertThat(position.z).isEqualTo(-30.0f)
    }

    @Test
    fun getRotationFromTransform_identity_returnsIdentity() {
        val transformData =
            floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
        val transform = Matrix4(transformData)

        val result = RuntimeUtils.getRotationFromTransform(transform)

        Truth.assertThat(result.x).isEqualTo(0f)
        Truth.assertThat(result.y).isEqualTo(0f)
        Truth.assertThat(result.z).isEqualTo(0f)
        Truth.assertThat(result.w).isEqualTo(1f)
    }

    @Test
    fun getRotationFromTransform_rotationZ90_returnsCorrectRotation() {
        // Rotate 90 degrees around Z axis.
        val transformData =
            floatArrayOf(0f, 1f, 0f, 0f, -1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
        val transform = Matrix4(transformData)

        val result = RuntimeUtils.getRotationFromTransform(transform)

        Truth.assertThat(result.x).isWithin(1.0e-5f).of(0f)
        Truth.assertThat(result.y).isWithin(1.0e-5f).of(0f)
        Truth.assertThat(result.z).isWithin(1.0e-5f).of(0.70710677f)
        Truth.assertThat(result.w).isWithin(1.0e-5f).of(0.70710677f)
    }
}
