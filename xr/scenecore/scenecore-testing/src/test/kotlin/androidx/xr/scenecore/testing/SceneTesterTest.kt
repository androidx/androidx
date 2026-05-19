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

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelClippingConfig
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.SpatialModeChangeEvent
import androidx.xr.scenecore.SpatialVisibility
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testing.internal.FakeRenderingRuntime
import androidx.xr.scenecore.testing.internal.FakeSceneRuntime
import androidx.xr.scenecore.toSpatialCapabilities
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SceneTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session
    private lateinit var sceneRuntime: FakeSceneRuntime
    private lateinit var renderingRuntime: FakeRenderingRuntime
    private lateinit var underTest: SceneTester

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

        assertThat(FakeSceneRuntime.instance).isNotNull()

        sceneRuntime = FakeSceneRuntime.instance!!

        assertThat(FakeRenderingRuntime.instance).isNotNull()

        renderingRuntime = FakeRenderingRuntime.instance!!
        underTest = testRule.sceneTester
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun instancesAreEqual() {
        val tester1 = SceneTester(sceneRuntime)
        val tester2 = SceneTester(sceneRuntime)

        // Assert equality and hashcode even if they are different instances
        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())

        // Optionally, ensure they are still equal if the scene state changes
        tester1.spatialCapabilities = setOf(SpatialCapability.SPATIAL_AUDIO)
        assertThat(tester1).isEqualTo(tester2)
    }

    @Test
    fun isDepthTestEnabled_reflectsScenePanelClippingConfig() {
        val configDepthTestDisabled = PanelClippingConfig(isDepthTestEnabled = false)
        session.scene.panelClippingConfig = configDepthTestDisabled
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(underTest.panelClippingConfig).isEqualTo(configDepthTestDisabled)

        val configDepthTestEnabled = PanelClippingConfig(isDepthTestEnabled = true)
        session.scene.panelClippingConfig = configDepthTestEnabled
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(underTest.panelClippingConfig).isEqualTo(configDepthTestEnabled)
    }

    @Test
    fun getSpatialCapabilities_getsRuntimeValue() {
        assertThat(underTest.spatialCapabilities)
            .isEqualTo(sceneRuntime.spatialCapabilities.toSpatialCapabilities())
    }

    @Test
    fun setSpatialCapabilities_setsRuntimeValue() {
        // initial value
        val allCapabilities: Set<SpatialCapability> =
            setOf(
                SpatialCapability.SPATIAL_3D_CONTENT,
                SpatialCapability.APP_ENVIRONMENT,
                SpatialCapability.EMBED_ACTIVITY,
                SpatialCapability.PASSTHROUGH_CONTROL,
                SpatialCapability.SPATIAL_AUDIO,
                SpatialCapability.SPATIAL_UI,
            )
        assertThat(underTest.spatialCapabilities).isEqualTo(allCapabilities)

        val expectedResult: Set<SpatialCapability> =
            setOf(
                SpatialCapability.SPATIAL_3D_CONTENT,
                SpatialCapability.APP_ENVIRONMENT,
                SpatialCapability.SPATIAL_AUDIO,
                SpatialCapability.SPATIAL_UI,
            )
        underTest.spatialCapabilities = expectedResult

        assertThat(underTest.spatialCapabilities).isEqualTo(expectedResult)
    }

    @Test
    fun setSpatialCapabilities_triggersSceneListeners() {
        var listenerCalledWith: Set<SpatialCapability> = emptySet()
        val listener = Consumer<Set<SpatialCapability>> { caps -> listenerCalledWith = caps }
        session.scene.addSpatialCapabilitiesChangedListener(directExecutor(), listener)

        val expectedResult: Set<SpatialCapability> = setOf(SpatialCapability.SPATIAL_AUDIO)
        underTest.spatialCapabilities = expectedResult
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listenerCalledWith).isNotNull()
        assertThat(listenerCalledWith).isEqualTo(expectedResult)
    }

    @Test
    fun triggerSpatialModeChanged_changesRuntimeValue() {
        val entity = Entity.create(session = session, parent = session.scene.activitySpace)
        session.scene.keyEntity = entity

        assertThat(session.scene.keyEntity?.getPose()).isEqualTo(Pose.Identity)
        assertThat(session.scene.keyEntity?.getScale()).isEqualTo(1.0f)

        val expectedPose = Pose(Vector3(1f, 1f, 1f))
        val expectedScale = 0.5f
        underTest.triggerSpatialModeChanged(SpatialModeChangeEvent(expectedPose, expectedScale))
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(session.scene.keyEntity?.getPose()).isEqualTo(expectedPose)
        assertThat(session.scene.keyEntity?.getScale()).isEqualTo(expectedScale)
    }

    @Test
    fun triggerSpatialVisibilityChanged_callbacksSuccessfully() {
        var listenerCalledWithValue = SpatialVisibility.UNKNOWN
        val listener =
            Consumer<SpatialVisibility> { visibility -> listenerCalledWithValue = visibility }
        session.scene.addSpatialVisibilityChangedListener(listener)

        val expectedResult = SpatialVisibility.WITHIN_FIELD_OF_VIEW
        underTest.triggerSpatialVisibilityChanged(expectedResult)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(listenerCalledWithValue).isEqualTo(expectedResult)
    }
}
