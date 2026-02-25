/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SpatialCapabilities
import androidx.xr.scenecore.runtime.SpatialVisibility as RtSpatialVisibility
import androidx.xr.scenecore.testing.FakeSceneRuntime
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config as RoboConfig

@RunWith(RobolectricTestRunner::class)
@RoboConfig(sdk = [RoboConfig.TARGET_SDK])
class SceneTest {
    private val activityController = Robolectric.buildActivity(ComponentActivity::class.java)
    private val activity = activityController.create().start().get()
    private lateinit var sceneRuntime: SceneRuntime
    lateinit var session: Session

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        sceneRuntime = session.sceneRuntime
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

        assertThat(activitySpace1).isEqualTo(activitySpace2)
    }

    @Test
    fun getPerceptionSpace_returnPerceptionSpace() {
        val perceptionSpace = session.scene.perceptionSpace

        assertThat(perceptionSpace).isNotNull()
    }

    @Test
    fun getMainPanelEntity_returnsPanelEntity() {
        @Suppress("UNUSED_VARIABLE") val mainPanelEntity1 = session.scene.mainPanelEntity
        @Suppress("UNUSED_VARIABLE") val mainPanelEntity2 = session.scene.mainPanelEntity

        assertThat(mainPanelEntity1.rtEntity).isSameInstanceAs(sceneRuntime.mainPanelEntity)
        assertThat(mainPanelEntity2.rtEntity).isSameInstanceAs(sceneRuntime.mainPanelEntity)
    }

    @Test
    fun getPanelEntityType_returnsAllPanelEntities() {
        val panelEntity =
            PanelEntity.create(session, TextView(activity), IntSize2d(720, 480), "test1")
        val activityPanelEntity = ActivityPanelEntity.create(session, IntSize2d(640, 480), "test2")

        assertThat(session.scene.getEntitiesOfType(PanelEntity::class.java))
            .containsAtLeast(panelEntity, activityPanelEntity)
    }

    @Test
    fun getEntitiesBaseType_returnsAllEntities() {
        val panelEntity =
            PanelEntity.create(session, TextView(activity), IntSize2d(720, 480), "test1")
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)

        assertThat(session.scene.getEntitiesOfType(Entity::class.java))
            .containsAtLeast(panelEntity, anchorEntity)
    }

    @Test
    fun setSpatialVisibilityChangedListener_receivesRuntimeSpatialVisibilityChangedEvent() {
        var listenerCalledWithValue = SpatialVisibility.UNKNOWN
        val listener =
            Consumer<SpatialVisibility> { visibility -> listenerCalledWithValue = visibility }

        // Test that it calls into the runtime and capture the runtime listener.
        val executor = directExecutor()
        session.scene.setSpatialVisibilityChangedListener(executor, listener)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        // Simulate the runtime listener being called with any value.
        fakeSceneRuntime.spatialVisibilityChangedMap.forEach { (listener, executor) ->
            executor.execute {
                listener.accept(RtSpatialVisibility(RtSpatialVisibility.WITHIN_FOV))
            }
        }
        assertThat(listenerCalledWithValue).isNotEqualTo(SpatialVisibility.UNKNOWN)
        assertThat(listenerCalledWithValue).isEqualTo(SpatialVisibility.WITHIN_FIELD_OF_VIEW)

        fakeSceneRuntime.spatialVisibilityChangedMap.forEach { (listener, executor) ->
            executor.execute {
                listener.accept(RtSpatialVisibility(RtSpatialVisibility.PARTIALLY_WITHIN_FOV))
            }
        }
        assertThat(listenerCalledWithValue)
            .isEqualTo(SpatialVisibility.PARTIALLY_WITHIN_FIELD_OF_VIEW)

        fakeSceneRuntime.spatialVisibilityChangedMap.forEach { (listener, executor) ->
            executor.execute {
                listener.accept(RtSpatialVisibility(RtSpatialVisibility.OUTSIDE_FOV))
            }
        }
        assertThat(listenerCalledWithValue).isEqualTo(SpatialVisibility.OUTSIDE_FIELD_OF_VIEW)

        fakeSceneRuntime.spatialVisibilityChangedMap.forEach { (listener, executor) ->
            executor.execute { listener.accept(RtSpatialVisibility(RtSpatialVisibility.UNKNOWN)) }
        }
        assertThat(listenerCalledWithValue).isEqualTo(SpatialVisibility.UNKNOWN)
    }

    @Test
    fun setSpatialVisibilityChangedListener_withNoExecutor_callsRuntimeSetSpatialVisibilityChangedListenerWithMainThreadExecutor() {
        val listener = Consumer<SpatialVisibility> { _ -> }
        session.scene.setSpatialVisibilityChangedListener(listener)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        assertThat(fakeSceneRuntime.spatialVisibilityChangedMap).hasSize(1)

        val storedExecutor = fakeSceneRuntime.spatialVisibilityChangedMap.values.first()
        assertThat(storedExecutor).isEqualTo(HandlerExecutor.mainThreadExecutor)
    }

    @Test
    fun clearSpatialVisibilityChangedListener_callsRuntimeClearSpatialVisibilityChangedListener() {
        val listener = Consumer<SpatialVisibility> { _ -> }
        session.scene.setSpatialVisibilityChangedListener(listener)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        assertThat(fakeSceneRuntime.spatialVisibilityChangedMap).hasSize(1)

        session.scene.clearSpatialVisibilityChangedListener()

        assertThat(fakeSceneRuntime.spatialVisibilityChangedMap).hasSize(0)
    }

    @Test
    fun sceneInit_setsDefaultSpatialModeChangedListener() {
        // Verify that default handler is always set.
        check(sceneRuntime.spatialModeChangeListener != null)
    }

    @Test
    fun setSpatialModeChangedListener_withExecutor_receivesEvent() {
        var receivedEvent: SpatialModeChangeEvent? = null
        val listener = Consumer<SpatialModeChangeEvent> { event -> receivedEvent = event }
        val executor = directExecutor()
        session.scene.setSpatialModeChangedListener(executor, listener)

        val pose = Pose.Identity
        val scale = Vector3(2f, 2f, 2f)
        sceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(pose, scale)

        assertThat(receivedEvent).isNotNull()
        assertThat(receivedEvent?.recommendedPose).isEqualTo(pose)
        assertThat(receivedEvent?.recommendedScale).isEqualTo(scale.x)
    }

    @Test
    fun setSpatialModeChangedListener_withNoExecutor_receivesEvent() {
        var receivedEvent: SpatialModeChangeEvent? = null
        val listener = Consumer<SpatialModeChangeEvent> { event -> receivedEvent = event }

        session.scene.setSpatialModeChangedListener(listener)

        val pose = Pose.Identity
        val scale = Vector3(2f, 2f, 2f)
        sceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(pose, scale)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(receivedEvent).isNotNull()
        assertThat(receivedEvent?.recommendedPose).isEqualTo(pose)
        assertThat(receivedEvent?.recommendedScale).isEqualTo(scale.x)
    }

    @Test
    fun clearSpatialModeChangedListener_removesListener() {
        var listenerCalled = false
        val listener = Consumer<SpatialModeChangeEvent> { _ -> listenerCalled = true }

        session.scene.setSpatialModeChangedListener(listener)
        session.scene.clearSpatialModeChangedListener()

        sceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(Pose.Identity, Vector3.One)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listenerCalled).isFalse()
    }

    @Test
    fun clearSpatialModeChangedListener_restoresDefaultKeyEntityBehavior() {
        val keyEntity = GroupEntity.create(session, "Test Entity")
        session.scene.keyEntity = keyEntity

        // Set a custom listener that does nothing
        session.scene.setSpatialModeChangedListener {}

        val initialPose = keyEntity.getPose()
        val initialScale = keyEntity.getScale()

        // Trigger change, keyEntity should not be updated
        val pose1 = Pose(Vector3(1f, 1f, 1f))
        val scale1 = Vector3(0.5f, 0.5f, 0.5f)
        sceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(pose1, scale1)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(keyEntity.getPose()).isEqualTo(initialPose)
        assertThat(keyEntity.getScale()).isEqualTo(initialScale)
        assertThat(keyEntity.getPose()).isNotEqualTo(pose1) // Ensure pose1 was different
        assertThat(keyEntity.getScale()).isNotEqualTo(scale1.x) // Ensure scale1 was different

        // Clear the listener
        session.scene.clearSpatialModeChangedListener()

        // Trigger change again, keyEntity should now be updated
        val pose2 = Pose(Vector3(2f, 2f, 2f))
        val scale2 = Vector3(3f, 3f, 3f)
        sceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(pose2, scale2)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(keyEntity.getPose(Space.ACTIVITY)).isEqualTo(pose2)
        assertThat(keyEntity.getScale(Space.ACTIVITY)).isEqualTo(scale2.x)
    }

    @Test
    fun setSpatialModeChangedListener_overridesDefaultBehavior() {
        val keyEntity = GroupEntity.create(session, "Test Entity")
        session.scene.keyEntity = keyEntity

        var listenerCalled = false
        val listener = Consumer<SpatialModeChangeEvent> { _ -> listenerCalled = true }
        session.scene.setSpatialModeChangedListener(listener)

        val pose1 = Pose(Vector3(1f, 1f, 1f))
        val scale1 = Vector3(0.5f, 0.5f, 0.5f)
        sceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(pose1, scale1)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listenerCalled).isTrue()
        assertThat(keyEntity.getPose()).isNotEqualTo(pose1)
        assertThat(keyEntity.getScale()).isNotEqualTo(scale1.x)
    }

    /**
     * A helper class for testing that acts as a [Consumer] for [SpatialCapabilities]. It counts how
     * many times it has been called and stores the last capabilities it received.
     */
    private class TestSpatialCapabilitiesListener : Consumer<SpatialCapabilities> {
        var callCount = 0
            private set // Make the setter private to prevent external modification

        var lastCapabilities: SpatialCapabilities? = null
            private set

        override fun accept(capabilities: SpatialCapabilities) {
            callCount++
            lastCapabilities = capabilities
        }
    }

    @Test
    fun requestFullSpaceMode_callsThrough() {
        val capabilitiesListener = TestSpatialCapabilitiesListener()
        sceneRuntime.addSpatialCapabilitiesChangedListener(directExecutor(), capabilitiesListener)
        session.scene.requestFullSpaceMode()

        assertThat(capabilitiesListener.callCount).isEqualTo(1)
        assertThat(capabilitiesListener.lastCapabilities?.capabilities)
            .isEqualTo(FakeSceneRuntime.ALL_SPATIAL_CAPABILITIES)
    }

    @Test
    fun requestHomeSpaceMode_callsThrough() {
        val capabilitiesListener = TestSpatialCapabilitiesListener()
        sceneRuntime.addSpatialCapabilitiesChangedListener(directExecutor(), capabilitiesListener)
        session.scene.requestHomeSpaceMode()

        assertThat(capabilitiesListener.callCount).isEqualTo(1)
        assertThat(capabilitiesListener.lastCapabilities?.capabilities).isEqualTo(0)
    }

    @Test
    fun panelClippingConfig_defaultValue_isTrue() {
        val defaultConfig = PanelClippingConfig(isDepthTestEnabled = true)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        assertThat(session.scene.panelClippingConfig).isEqualTo(defaultConfig)
        assertThat(fakeSceneRuntime.enabledPanelDepthTest).isFalse()
    }

    @Test
    fun panelClippingConfig_setFalse_callsPlatformAdapterWithFalse() {
        val disabledConfig = PanelClippingConfig(isDepthTestEnabled = false)
        session.scene.panelClippingConfig = disabledConfig
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        assertThat(fakeSceneRuntime.enabledPanelDepthTest).isFalse()
        assertThat(session.scene.panelClippingConfig).isEqualTo(disabledConfig)
    }

    @Test
    fun panelClippingConfig_setTrue_callsPlatformAdapterWithTrue() {
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime
        // First, set to disabled to ensure the next call is a change.
        session.scene.panelClippingConfig = PanelClippingConfig(isDepthTestEnabled = false)

        assertThat(fakeSceneRuntime.enabledPanelDepthTest).isFalse()

        val enabledConfig = PanelClippingConfig(isDepthTestEnabled = true)
        session.scene.panelClippingConfig = enabledConfig

        assertThat(fakeSceneRuntime.enabledPanelDepthTest).isTrue()
        assertThat(session.scene.panelClippingConfig).isEqualTo(enabledConfig)
    }

    @Test
    fun keyEntity_defaultValue_isNull() {
        assertThat(session.scene.keyEntity).isNull()
    }

    @Test
    fun keyEntity_setWithValidEntity_succeeds() {
        val keyEntity = GroupEntity.create(session, "Test Entity")
        session.scene.keyEntity = keyEntity
        assertThat(session.scene.keyEntity).isEqualTo(keyEntity)
    }

    @Test
    fun keyEntity_setWithAnchorEntity_throwsIllegalArgumentException() {
        val anchorEntity =
            AnchorEntity.create(session, FloatSize2d(), PlaneOrientation.ANY, PlaneSemanticType.ANY)

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
        val keyEntity = GroupEntity.create(session, "Test Entity")
        session.scene.keyEntity = keyEntity // Set it first
        assertThat(session.scene.keyEntity).isEqualTo(keyEntity)

        session.scene.keyEntity = null // Clear it
        assertThat(session.scene.keyEntity).isNull()
    }

    @Test
    fun keyEntity_setNewEntity_setsNewRtKeyEntity() {
        val entity = GroupEntity.create(session, "Entity")
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        session.scene.keyEntity = entity

        assertThat(fakeSceneRuntime.keyEntity).isNotNull()

        session.scene.keyEntity = null

        assertThat(fakeSceneRuntime.keyEntity).isNull()
    }

    @Test
    fun defaultSpatialModeChangedListener_withKeyEntity_updatesPoseAndScale() {
        val keyEntity = GroupEntity.create(session, "Test Entity")
        session.scene.keyEntity = keyEntity

        val recommendedPose = Pose(Vector3(1f, 2f, 3f))
        val recommendedScale = Vector3(2f, 2f, 2f)

        sceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(
            recommendedPose,
            recommendedScale,
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(keyEntity.getPose(Space.ACTIVITY)).isEqualTo(recommendedPose)
        assertThat(keyEntity.getScale(Space.ACTIVITY)).isEqualTo(recommendedScale.x)
    }

    @Test
    fun defaultSpatialModeChangedListener_withNullKeyEntity_isNoOp() {
        // Ensure keyEntity is null (it is by default)
        assertThat(session.scene.keyEntity).isNull()

        val recommendedPose = Pose.Identity
        val recommendedScale = Vector3.One

        // This should not throw any exception
        sceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(
            recommendedPose,
            recommendedScale,
        )
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun sceneClose_removesSpatialCapabilitiesListeners() {
        val capabilitiesListener = Consumer<Set<SpatialCapability>> {}
        session.scene.addSpatialCapabilitiesChangedListener(capabilitiesListener)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        assertThat(fakeSceneRuntime.spatialCapabilitiesChangedMap).hasSize(1)

        session.scene.close()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(fakeSceneRuntime.spatialCapabilitiesChangedMap).hasSize(0)
    }

    @Test
    fun sceneClose_clearsSpatialVisibilityListener() {
        val visibilityListener = Consumer<SpatialVisibility> {}
        session.scene.setSpatialVisibilityChangedListener(visibilityListener)
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        assertThat(fakeSceneRuntime.spatialVisibilityChangedMap).hasSize(1)

        session.scene.close()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(fakeSceneRuntime.spatialVisibilityChangedMap).hasSize(0)
    }

    @Test
    fun sceneClose_clearsSpatialModeChangeListener() {
        var modeChangeListenerCalled = false
        val modeChangeListener =
            Consumer<SpatialModeChangeEvent> { modeChangeListenerCalled = true }
        session.scene.setSpatialModeChangedListener(modeChangeListener)

        session.scene.close()
        shadowOf(Looper.getMainLooper()).idle()

        sceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(Pose.Identity, Vector3.One)
        assertThat(modeChangeListenerCalled).isFalse()
    }

    @Test
    fun sceneClose_clearsKeyEntity() {
        val modeChangeListener = Consumer<SpatialModeChangeEvent> {}
        session.scene.setSpatialModeChangedListener(modeChangeListener)

        session.scene.close()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(session.scene.keyEntity).isNull()
    }

    @Test
    fun keyEntity_setFirstTime_invokesSpatialModeChangeListenersWithLastRecommendedValues() {
        val recommendedPose = Pose(Vector3(1f, 2f, 3f))
        val recommendedScale = Vector3(5f, 5f, 5f)

        // Trigger a mode change to set lastRecommended values
        sceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(
            recommendedPose,
            recommendedScale,
        )

        val keyEntity = GroupEntity.create(session, "Test Entity")
        session.scene.keyEntity = keyEntity

        shadowOf(Looper.getMainLooper()).idle()

        assertThat(keyEntity.getPose()).isEqualTo(recommendedPose)
        assertThat(keyEntity.getScale()).isEqualTo(recommendedScale.x)
    }

    @Test
    fun keyEntity_setFirstTime_invokesCustomSpatialModeChangeListenersWithLastRecommendedValues() {
        val recommendedPose = Pose(Vector3(1f, 2f, 3f))
        val recommendedScale = Vector3(5f, 5f, 5f)
        var testSpatialModeChangeCount = 0

        // Trigger a mode change to set lastRecommended values
        sceneRuntime.spatialModeChangeListener?.onSpatialModeChanged(
            recommendedPose,
            recommendedScale,
        )

        val keyEntity = GroupEntity.create(session, "Test Entity")
        session.scene.keyEntity = keyEntity
        session.scene.setSpatialModeChangedListener { _ -> testSpatialModeChangeCount++ }

        shadowOf(Looper.getMainLooper()).idle()

        // Check that spatial mode change listener was invoked twice, once on spatial mode change
        // and later when keyEntity was set.
        assertThat(testSpatialModeChangeCount).isEqualTo(2)
    }

    @Test
    fun isBoundaryConsentGranted_callsThroughToRuntime() {
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        fakeSceneRuntime.onBoundaryConsentChanged(true)

        assertThat(session.scene.isBoundaryConsentGranted).isTrue()

        fakeSceneRuntime.onBoundaryConsentChanged(false)

        assertThat(session.scene.isBoundaryConsentGranted).isFalse()
    }

    @Test
    fun addOnBoundaryConsentChangedListener_withExecutor_callsThroughToRuntime() {
        val listener = Consumer<Boolean> {}
        val executor = directExecutor()
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        session.scene.addOnBoundaryConsentChangedListener(executor, listener)

        assertThat(fakeSceneRuntime.boundaryConsentChangedMap).containsEntry(listener, executor)
    }

    @Test
    fun addOnBoundaryConsentChangedListener_withNoExecutor_callsThroughToRuntimeWithMainExecutor() {
        val listener = Consumer<Boolean> {}
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        session.scene.addOnBoundaryConsentChangedListener(listener)

        assertThat(fakeSceneRuntime.boundaryConsentChangedMap)
            .containsEntry(listener, HandlerExecutor.mainThreadExecutor)
    }

    @Test
    fun removeOnBoundaryConsentChangedListener_callsThroughToRuntime() {
        val listener = Consumer<Boolean> {}
        val fakeSceneRuntime = sceneRuntime as FakeSceneRuntime

        session.scene.addOnBoundaryConsentChangedListener(listener)

        assertThat(fakeSceneRuntime.boundaryConsentChangedMap).hasSize(1)

        session.scene.removeOnBoundaryConsentChangedListener(listener)

        assertThat(fakeSceneRuntime.boundaryConsentChangedMap).hasSize(0)
    }
}
