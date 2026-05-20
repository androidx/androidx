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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testrule

import android.os.Looper
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelClippingConfig
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.SpaceChangeEvent
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.SpatialVisibility
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.sceneRuntime
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config as RoboConfig

@RunWith(RobolectricTestRunner::class)
@RoboConfig(sdk = [RoboConfig.TARGET_SDK])
class TestRuleSceneTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
    private val activity = activityController.create().start().get()
    lateinit var session: Session

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
    }

    @Test
    fun getSceneBeforeSessionDestroyed_returnsScene() {
        assertThat(session.scene).isInstanceOf(Scene::class.java)
    }

    @Test
    fun getSceneAfterSessionDestroyed_returnsScene() {
        // We currently allow getScene to be invoked on a Session whose lifecycle state is
        // DESTROYED. We may want to change this in the future, see b/450009236.
        activityController.destroy()

        assertThat(session.scene).isInstanceOf(Scene::class.java)
    }

    @Test
    fun getActivitySpace_returnsActivitySpace() {
        val activitySpace = session.scene.activitySpace

        assertThat(activitySpace).isNotNull()
    }

    @Test
    fun getActivitySpaceTwice_returnsSameSpace() {
        val activitySpace1 = session.scene.activitySpace
        val activitySpace2 = session.scene.activitySpace

        assertThat(activitySpace1).isSameInstanceAs(activitySpace2)
    }

    @Test
    fun getPerceptionSpace_returnsPerceptionSpace() {
        val perceptionSpace = session.scene.perceptionSpace

        assertThat(perceptionSpace).isNotNull()
    }

    @Test
    fun getPerceptionSpaceMultipleTimes_returnsSameInstance() {
        val perceptionSpace1 = session.scene.perceptionSpace
        val perceptionSpace2 = session.scene.perceptionSpace

        assertThat(perceptionSpace1).isSameInstanceAs(perceptionSpace2)
    }

    @Test
    fun getMainPanelEntity_returnsPanelEntity() {
        val mainPanelEntity1 = session.scene.mainPanelEntity
        val mainPanelEntity2 = session.scene.mainPanelEntity

        assertThat(mainPanelEntity1.rtEntity).isSameInstanceAs(session.sceneRuntime.mainPanelEntity)
        assertThat(mainPanelEntity2.rtEntity).isSameInstanceAs(session.sceneRuntime.mainPanelEntity)
    }

    @Test
    fun getPanelEntityType_returnsAllPanelEntities() {
        val panelEntity =
            PanelEntity.create(
                session,
                TextView(activity),
                IntSize2d(720, 480),
                "test1",
                parent = session.scene.activitySpace,
            )
        val activityPanelEntity =
            ActivityPanelEntity.create(
                session,
                IntSize2d(640, 480),
                "test2",
                parent = session.scene.activitySpace,
            )

        assertThat(session.scene.getEntitiesOfType(PanelEntity::class.java))
            .containsAtLeast(panelEntity, activityPanelEntity)
    }

    @Test
    fun getEntitiesBaseType_returnsAllEntities() {
        val panelEntity =
            PanelEntity.create(
                session,
                TextView(activity),
                IntSize2d(720, 480),
                "test1",
                parent = session.scene.activitySpace,
            )
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ALL, PlaneSemanticType.ALL)

        assertThat(session.scene.getEntitiesOfType(Entity::class.java))
            .containsAtLeast(panelEntity, anchorEntity)
    }

    // TODO - b/502272748: Once the deprecated set method is removed this can be removed
    @Test
    fun setSpatialVisibilityChangedListener_receivesRuntimeSpatialVisibilityChangedEvent() {
        var listenerCalledWithValue = SpatialVisibility.UNKNOWN
        val listener =
            Consumer<SpatialVisibility> { visibility -> listenerCalledWithValue = visibility }

        // Test that it calls into the runtime and capture the runtime listener.
        val executor = directExecutor()
        session.scene.setSpatialVisibilityChangedListener(executor, listener)
        val testScene = testRule.sceneTester

        var expectedResult = SpatialVisibility.WITHIN_FIELD_OF_VIEW
        // Simulate the runtime listener being called with any value.
        testScene.triggerSpatialVisibilityChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(listenerCalledWithValue).isNotEqualTo(SpatialVisibility.UNKNOWN)
        assertThat(listenerCalledWithValue).isEqualTo(expectedResult)

        expectedResult = SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW
        testScene.triggerSpatialVisibilityChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(listenerCalledWithValue).isEqualTo(expectedResult)

        expectedResult = SpatialVisibility.OUTSIDE_FIELD_OF_VIEW
        testScene.triggerSpatialVisibilityChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(listenerCalledWithValue).isEqualTo(expectedResult)

        expectedResult = SpatialVisibility.UNKNOWN
        testScene.triggerSpatialVisibilityChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(listenerCalledWithValue).isEqualTo(expectedResult)
    }

    @Test
    fun addSpatialVisibilityChangedListener_receivesRuntimeSpatialVisibilityChangedEvent() {
        var listenerCalledWithValue = SpatialVisibility.UNKNOWN
        val listener =
            Consumer<SpatialVisibility> { visibility -> listenerCalledWithValue = visibility }

        // Test that it calls into the runtime and capture the runtime listener.
        val executor = directExecutor()
        session.scene.addSpatialVisibilityChangedListener(executor, listener)
        val testScene = testRule.sceneTester

        var expectedResult = SpatialVisibility.WITHIN_FIELD_OF_VIEW
        // Simulate the runtime listener being called with any value.
        testScene.triggerSpatialVisibilityChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(listenerCalledWithValue).isNotEqualTo(SpatialVisibility.UNKNOWN)
        assertThat(listenerCalledWithValue).isEqualTo(expectedResult)

        expectedResult = SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW
        testScene.triggerSpatialVisibilityChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(listenerCalledWithValue).isEqualTo(expectedResult)

        expectedResult = SpatialVisibility.OUTSIDE_FIELD_OF_VIEW
        testScene.triggerSpatialVisibilityChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(listenerCalledWithValue).isEqualTo(expectedResult)

        expectedResult = SpatialVisibility.UNKNOWN
        testScene.triggerSpatialVisibilityChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(listenerCalledWithValue).isEqualTo(expectedResult)
    }

    @Test
    fun addSpatialVisibilityChangedListener_withNoExecutor_callsRuntimeSetSpatialVisibilityChangedListenerWithMainThreadExecutor() {
        var listenerCalledWithValue = SpatialVisibility.UNKNOWN
        val listener =
            Consumer<SpatialVisibility> { visibility -> listenerCalledWithValue = visibility }
        session.scene.addSpatialVisibilityChangedListener(listener)
        val testScene = testRule.sceneTester

        val expectedResult = SpatialVisibility.WITHIN_FIELD_OF_VIEW
        testScene.triggerSpatialVisibilityChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listenerCalledWithValue).isEqualTo(expectedResult)
    }

    @Test
    fun removeSpatialVisibilityChangedListener_callsRuntimeClearSpatialVisibilityChangedListener() {
        var listenerCalledWithValue = SpatialVisibility.UNKNOWN
        val listener =
            Consumer<SpatialVisibility> { visibility -> listenerCalledWithValue = visibility }
        val testScene = testRule.sceneTester

        session.scene.addSpatialVisibilityChangedListener(directExecutor(), listener)
        testScene.triggerSpatialVisibilityChanged(SpatialVisibility.WITHIN_FIELD_OF_VIEW)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listenerCalledWithValue).isEqualTo(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        session.scene.removeSpatialVisibilityChangedListener(listener)
        testScene.triggerSpatialVisibilityChanged(SpatialVisibility.UNKNOWN)
        shadowOf(Looper.getMainLooper()).idle()

        // No callback after removeSpatialVisibilityChangedListener
        assertThat(listenerCalledWithValue).isEqualTo(SpatialVisibility.WITHIN_FIELD_OF_VIEW)
    }

    @Test
    fun sceneInit_setsDefaultSpatialModeChangedListener() {
        // Verify that default handler is always set.
        assertThat(session.sceneRuntime.spatialModeChangeListener).isNotNull()
    }

    @Test
    fun setSpaceChangedListener_withExecutor_receivesEvent() {
        var receivedEvent: SpaceChangeEvent? = null
        val listener = Consumer<SpaceChangeEvent> { event -> receivedEvent = event }
        val executor = directExecutor()
        session.scene.setSpaceChangedListener(executor, listener)

        val expectedPose = Pose.Identity
        val expectedScale = 2f
        val expectedResult = SpaceChangeEvent(expectedPose, expectedScale)
        testRule.sceneTester.triggerSpaceChanged(expectedResult)

        assertThat(receivedEvent).isNotNull()
        assertThat(receivedEvent?.recommendedPose).isEqualTo(expectedPose)
        assertThat(receivedEvent?.recommendedScale).isEqualTo(expectedScale)
    }

    @Test
    fun setSpaceChangedListener_withNoExecutor_receivesEvent() {
        var receivedEvent: SpaceChangeEvent? = null
        val listener = Consumer<SpaceChangeEvent> { event -> receivedEvent = event }

        session.scene.setSpaceChangedListener(listener)

        val expectedPose = Pose.Identity
        val expectedScale = 2f
        val expectedResult = SpaceChangeEvent(expectedPose, expectedScale)
        testRule.sceneTester.triggerSpaceChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(receivedEvent).isNotNull()
        assertThat(receivedEvent?.recommendedPose).isEqualTo(expectedPose)
        assertThat(receivedEvent?.recommendedScale).isEqualTo(expectedScale)
    }

    @Test
    fun clearSpaceChangedListener_removesListener() {
        var listenerCalled = false
        val listener = Consumer<SpaceChangeEvent> { _ -> listenerCalled = true }

        session.scene.setSpaceChangedListener(listener)
        // Set keyEntity to null to avoid the IllegalStateException in FakeEntity
        session.scene.keyEntity = null
        session.scene.clearSpaceChangedListener()

        val expectedPose = Pose.Identity
        val expectedScale = 1f
        val expectedResult = SpaceChangeEvent(expectedPose, expectedScale)
        testRule.sceneTester.triggerSpaceChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listenerCalled).isFalse()
    }

    @Test
    fun clearSpaceChangedListener_restoresDefaultKeyEntityBehavior() {
        val keyEntity = Entity.create(session, "Test Entity")
        // Parent the entity to activitySpace so setPose(..., Space.ACTIVITY) doesn't throw.
        keyEntity.parent = session.scene.activitySpace
        session.scene.keyEntity = keyEntity

        // Set a custom listener that does nothing
        session.scene.setSpaceChangedListener {}

        val initialPose = keyEntity.getPose()
        val initialScale = keyEntity.getScale()

        // Trigger change, keyEntity should not be updated
        val pose1 = Pose(Vector3(1f, 1f, 1f))
        val scale1 = 0.5f
        val expectedResult1 = SpaceChangeEvent(pose1, scale1)
        testRule.sceneTester.triggerSpaceChanged(expectedResult1)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(keyEntity.getPose()).isEqualTo(initialPose)
        assertThat(keyEntity.getScale()).isEqualTo(initialScale)
        assertThat(keyEntity.getPose()).isNotEqualTo(pose1) // Ensure pose1 was different
        assertThat(keyEntity.getScale()).isNotEqualTo(scale1) // Ensure scale1 was different

        // Clear the listener
        session.scene.clearSpaceChangedListener()

        // Trigger change again, keyEntity should now be updated
        val pose2 = Pose(Vector3(2f, 2f, 2f))
        val scale2 = 3f
        val expectedResult2 = SpaceChangeEvent(pose2, scale2)
        testRule.sceneTester.triggerSpaceChanged(expectedResult2)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(keyEntity.getPose(Space.ACTIVITY)).isEqualTo(pose2)
        assertThat(keyEntity.getScale(Space.ACTIVITY)).isEqualTo(scale2)
    }

    @Test
    fun setSpaceChangedListener_overridesDefaultBehavior() {
        val keyEntity = Entity.create(session, "Test Entity")
        session.scene.keyEntity = keyEntity

        var listenerCalled = false
        val listener = Consumer<SpaceChangeEvent> { _ -> listenerCalled = true }
        session.scene.setSpaceChangedListener(listener)

        val pose1 = Pose(Vector3(1f, 1f, 1f))
        val scale1 = 0.5f
        val expectedResult = SpaceChangeEvent(pose1, scale1)
        testRule.sceneTester.triggerSpaceChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listenerCalled).isTrue()
        assertThat(keyEntity.getPose()).isNotEqualTo(pose1)
        assertThat(keyEntity.getScale()).isNotEqualTo(scale1)
    }

    @Test
    fun requestFullSpace_callsThrough() {
        val allCapabilities: Set<SpatialCapability> =
            setOf(
                SpatialCapability.SPATIAL_3D_CONTENT,
                SpatialCapability.APP_ENVIRONMENT,
                SpatialCapability.EMBED_ACTIVITY,
                SpatialCapability.PASSTHROUGH_CONTROL,
                SpatialCapability.SPATIAL_AUDIO,
                SpatialCapability.SPATIAL_UI,
            )
        var result: Set<SpatialCapability> = emptySet()
        val listener = Consumer<Set<SpatialCapability>> { capability -> result = capability }
        session.scene.addSpatialCapabilitiesChangedListener(listener)
        session.scene.requestFullSpace()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(result).isEqualTo(allCapabilities)
    }

    @Test
    fun requestHomeSpace_callsThrough() {
        val emptyCapabilities: Set<SpatialCapability> = emptySet()
        var result: Set<SpatialCapability> =
            setOf(SpatialCapability.SPATIAL_3D_CONTENT, SpatialCapability.APP_ENVIRONMENT)
        val listener = Consumer<Set<SpatialCapability>> { capability -> result = capability }
        session.scene.addSpatialCapabilitiesChangedListener(listener)
        session.scene.requestHomeSpace()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(result).isEqualTo(emptyCapabilities)
    }

    @Test
    fun requestHomeSpace_requestFullSpace_callsThrough() {
        val allCapabilities: Set<SpatialCapability> =
            setOf(
                SpatialCapability.SPATIAL_3D_CONTENT,
                SpatialCapability.APP_ENVIRONMENT,
                SpatialCapability.EMBED_ACTIVITY,
                SpatialCapability.PASSTHROUGH_CONTROL,
                SpatialCapability.SPATIAL_AUDIO,
                SpatialCapability.SPATIAL_UI,
            )
        val emptyCapabilities: Set<SpatialCapability> = emptySet()

        // Default is FSM
        assertThat(testRule.sceneTester.spatialCapabilities).isEqualTo(allCapabilities)

        // Request HSM
        session.scene.requestHomeSpace()
        assertThat(testRule.sceneTester.spatialCapabilities).isEqualTo(emptyCapabilities)

        // Request FSM
        session.scene.requestFullSpace()
        assertThat(testRule.sceneTester.spatialCapabilities).isEqualTo(allCapabilities)
    }

    @Test
    fun panelClippingConfig_defaultValue_isTrue() {
        val defaultConfig = PanelClippingConfig()

        assertThat(defaultConfig.isDepthTestEnabled).isTrue()
        assertThat(session.scene.panelClippingConfig).isEqualTo(defaultConfig)
    }

    @Test
    fun panelClippingConfig_setFalse_callsPlatformAdapterWithFalse() {
        val disabledConfig = PanelClippingConfig(isDepthTestEnabled = false)
        session.scene.panelClippingConfig = disabledConfig

        assertThat(testRule.sceneTester.panelClippingConfig).isEqualTo(disabledConfig)
    }

    @Test
    fun panelClippingConfig_setTrue_callsPlatformAdapterWithTrue() {
        // First, set to disabled to ensure the next call is a change.
        val disabledConfig = PanelClippingConfig(isDepthTestEnabled = false)
        session.scene.panelClippingConfig = disabledConfig

        assertThat(testRule.sceneTester.panelClippingConfig).isEqualTo(disabledConfig)

        val enabledConfig = PanelClippingConfig(isDepthTestEnabled = true)
        session.scene.panelClippingConfig = enabledConfig

        assertThat(testRule.sceneTester.panelClippingConfig).isEqualTo(enabledConfig)
    }

    @Test
    fun keyEntity_defaultValue_isMainPanelEntity() {
        assertThat(session.scene.keyEntity).isEqualTo(session.scene.mainPanelEntity)
    }

    @Test
    fun keyEntity_setWithValidEntity_succeeds() {
        val keyEntity = Entity.create(session, "Test Entity")
        session.scene.keyEntity = keyEntity
        assertThat(session.scene.keyEntity).isEqualTo(keyEntity)
    }

    @Test
    fun keyEntity_setWithAnchorEntity_throwsIllegalArgumentException() {
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ALL, PlaneSemanticType.ALL)

        val exception =
            assertFailsWith<IllegalArgumentException> { session.scene.keyEntity = anchorEntity }
        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("AnchorEntity cannot be set as the keyEntity.")
    }

    @Test
    fun keyEntity_setWithActivitySpace_throwsIllegalArgumentException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                session.scene.keyEntity = session.scene.activitySpace
            }
        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("ActivitySpace cannot be set as the keyEntity.")
    }

    @Test
    fun keyEntity_setWithNull_clearsKeyEntity() {
        val keyEntity = Entity.create(session, "Test Entity")
        session.scene.keyEntity = keyEntity // Set it first
        assertThat(session.scene.keyEntity).isEqualTo(keyEntity)

        session.scene.keyEntity = null // Clear it
        assertThat(session.scene.keyEntity).isNull()
    }

    @Test
    fun defaultSpaceChangedListener_withKeyEntity_updatesPoseAndScale() {
        val keyEntity = Entity.create(session, "Test Entity")
        // Parent the entity to activitySpace so setPose(..., Space.ACTIVITY) doesn't throw.
        keyEntity.parent = session.scene.activitySpace
        session.scene.keyEntity = keyEntity

        val recommendedPose = Pose(Vector3(1f, 2f, 3f))
        val recommendedScale = 2f
        val expectedResult = SpaceChangeEvent(recommendedPose, recommendedScale)
        testRule.sceneTester.triggerSpaceChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(keyEntity.getPose(Space.ACTIVITY)).isEqualTo(recommendedPose)
        assertThat(keyEntity.getScale(Space.ACTIVITY)).isEqualTo(recommendedScale)
    }

    @Test
    fun defaultSpaceChangedListener_withNullKeyEntity_isNoOp() {
        // Ensure keyEntity is null.
        session.scene.keyEntity = null
        assertThat(session.scene.keyEntity).isNull()

        val recommendedPose = Pose.Identity
        val recommendedScale = 1.0f
        val expectedResult = SpaceChangeEvent(recommendedPose, recommendedScale)

        // This should not throw any exception
        testRule.sceneTester.triggerSpaceChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun sceneClose_removesSpatialCapabilitiesListeners() {
        val emptyCapabilities: Set<SpatialCapability> = emptySet()
        var resultCount = 0
        val capabilitiesListener = Consumer<Set<SpatialCapability>> { _ -> resultCount++ }
        session.scene.addSpatialCapabilitiesChangedListener(capabilitiesListener)

        assertThat(resultCount).isEqualTo(0)

        testRule.sceneTester.spatialCapabilities = emptyCapabilities
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(resultCount).isEqualTo(1)

        session.scene.close()
        session.scene.requestFullSpace()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(resultCount).isEqualTo(1)
    }

    @Test
    fun sceneClose_clearsSpatialVisibilityListener() {
        var listenerCalledWithValue = SpatialVisibility.UNKNOWN
        val visibilityListener =
            Consumer<SpatialVisibility> { visibility -> listenerCalledWithValue = visibility }

        session.scene.addSpatialVisibilityChangedListener(visibilityListener)
        testRule.sceneTester.triggerSpatialVisibilityChanged(SpatialVisibility.WITHIN_FIELD_OF_VIEW)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listenerCalledWithValue).isEqualTo(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        session.scene.close()
        testRule.sceneTester.triggerSpatialVisibilityChanged(SpatialVisibility.UNKNOWN)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listenerCalledWithValue).isEqualTo(SpatialVisibility.WITHIN_FIELD_OF_VIEW)
    }

    @Test
    fun sceneClose_clearsSpaceChangeListener() {
        var spaceChangeListenerCalled = false
        val spaceChangeListener = Consumer<SpaceChangeEvent> { spaceChangeListenerCalled = true }
        session.scene.setSpaceChangedListener(spaceChangeListener)

        session.scene.close()

        val recommendedPose = Pose.Identity
        val recommendedScale = 1.0f
        val expectedResult = SpaceChangeEvent(recommendedPose, recommendedScale)
        testRule.sceneTester.triggerSpaceChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(spaceChangeListenerCalled).isFalse()
    }

    @Test
    fun sceneClose_clearsKeyEntity() {
        session.scene.keyEntity = Entity.create(session)
        session.scene.close()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(session.scene.keyEntity).isNull()
    }

    @Test
    fun keyEntity_setNonNullAfterNull_invokesSpaceChangeListenersWithLastRecommendedValues() {
        val recommendedPose = Pose(Vector3(1f, 2f, 3f))
        val recommendedScale = 5f
        val expectedResult = SpaceChangeEvent(recommendedPose, recommendedScale)

        // Trigger a space change to set lastRecommended values
        testRule.sceneTester.triggerSpaceChanged(expectedResult)

        // Reset keyEntity to null so that setting it next triggers immediate invocation
        session.scene.keyEntity = null

        val keyEntity = Entity.create(session, "Test Entity")
        // Parent the entity to activitySpace so setPose(..., Space.ACTIVITY) doesn't throw.
        keyEntity.parent = session.scene.activitySpace
        session.scene.keyEntity = keyEntity

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(keyEntity.getPose()).isEqualTo(recommendedPose)
        assertThat(keyEntity.getScale()).isEqualTo(recommendedScale)
    }

    @Test
    fun keyEntity_setNonNullAfterNull_invokesCustomSpaceChangeListenersWithLastRecommendedValues() {
        val recommendedPose = Pose(Vector3(1f, 2f, 3f))
        val recommendedScale = 5f
        var testSpaceChangeCount = 0
        session.scene.setSpaceChangedListener { _ -> testSpaceChangeCount++ }

        // Trigger a space change to set lastRecommended values
        testRule.sceneTester.triggerSpaceChanged(
            SpaceChangeEvent(recommendedPose, recommendedScale)
        )

        // Reset keyEntity to null so that setting it next triggers immediate invocation
        session.scene.keyEntity = null

        val keyEntity = Entity.create(session, "Test Entity")
        session.scene.keyEntity = keyEntity

        shadowOf(Looper.getMainLooper()).idle()

        // Check that space change listener was invoked twice, once on the space change
        // and later when keyEntity was set.
        assertThat(testSpaceChangeCount).isEqualTo(2)
    }
}
