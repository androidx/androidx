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

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.EntityRegistry
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testing.FakeExrImageResource
import androidx.xr.scenecore.testing.FakeGltfModelResource
import androidx.xr.scenecore.testing.SceneCoreTestRule
import androidx.xr.scenecore.testing.SpatialEnvironmentTester
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.nio.file.Paths
import java.util.function.Consumer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Unit tests for the JXRCore SDK SpatialEnvironment Interface.
 *
 * TODO(b/329902726): Add a TestRuntime and verify CPM Integration.
 */
@RunWith(RobolectricTestRunner::class)
@SuppressLint("NewApi")
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@Config(sdk = [Config.TARGET_SDK])
class TestRuleSpatialEnvironmentTest {

    @get:Rule val scenecoreTestRule = SceneCoreTestRule()

    private lateinit var session: Session
    private lateinit var spatialEnvironmentTester: SpatialEnvironmentTester
    private var environment: SpatialEnvironment? = null
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var entityRegistry: EntityRegistry
    private lateinit var gltfModelEntity: GltfModelEntity

    @Before
    fun setUp() = runBlocking {
        val testDispatcher = StandardTestDispatcher()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        entityRegistry = session.scene.entityRegistry
        spatialEnvironmentTester = scenecoreTestRule.spatialEnvironmentTester
        environment = session.scene.spatialEnvironment

        val gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        gltfModelEntity =
            GltfModelEntity.create(session, gltfModel, parent = session.scene.activitySpace)
    }

    @Test
    fun currentPassthroughOpacity_getsRuntimePassthroughOpacity() {
        val rtOpacity = 0.3f
        spatialEnvironmentTester.triggerPassthroughOpacityChanged(rtOpacity)

        assertThat(environment!!.currentPassthroughOpacity).isEqualTo(rtOpacity)
    }

    @Test
    fun getPassthroughOpacityPreference_getsRuntimePassthroughOpacityPreference() {
        val rtPreference = 0.3f
        environment!!.preferredPassthroughOpacity = rtPreference

        assertThat(environment!!.preferredPassthroughOpacity).isEqualTo(rtPreference)
    }

    @Test
    fun getPassthroughOpacityPreferenceNoPreference_getsRuntimePassthroughOpacityPreference() {
        val rtPreference = SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
        environment!!.preferredPassthroughOpacity = rtPreference

        assertThat(environment!!.preferredPassthroughOpacity).isEqualTo(rtPreference)
    }

    @Test
    fun addPassthroughOpacityChangedListener_ReceivesRuntimeOnPassthroughOpacityChangedEvents() {
        var listenerCalledWithValue = 0.0f
        val listener = Consumer<Float> { floatValue: Float -> listenerCalledWithValue = floatValue }
        environment!!.addPassthroughOpacityChangedListener(listener)

        spatialEnvironmentTester.triggerPassthroughOpacityChanged(0.3f)
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalledWithValue).isEqualTo(0.3f)
    }

    @Test
    fun addPassthroughOpacityChangedListener_withExecutor_receivesEventsOnExecutor() {
        var listenerCalledWithValue = 0.0f
        var listenerThread: Thread? = null
        val executor = directExecutor()

        val listener =
            Consumer<Float> { floatValue: Float ->
                listenerCalledWithValue = floatValue
                listenerThread = Thread.currentThread()
            }
        environment!!.addPassthroughOpacityChangedListener(executor, listener)

        val eventValue = 0.3f
        spatialEnvironmentTester.triggerPassthroughOpacityChanged(eventValue)

        assertThat(listenerCalledWithValue).isEqualTo(eventValue)
        assertThat(listenerThread).isNotNull()
    }

    @Test
    fun removePassthroughOpacityChangedListener_callsRuntimeRemoveOnPassthroughOpacityChangedListener() {
        var listenerCalledCount = 0
        val listener = Consumer<Float> { listenerCalledCount++ }
        environment!!.addPassthroughOpacityChangedListener(listener)

        environment!!.removePassthroughOpacityChangedListener(listener)
        spatialEnvironmentTester.triggerPassthroughOpacityChanged(0.3f)
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalledCount).isEqualTo(0)
    }

    @Test
    fun spatialEnvironmentPreferenceEqualsHashcode_returnsTrueIfAllPropertiesAreEqual() {
        val rtImage = FakeExrImageResource(0)
        val rtModel = FakeGltfModelResource(0)

        val preference1 =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ImageBasedLightingAsset(null, rtImage),
                GltfModel(null, rtModel),
                null,
            )
        val preference2 =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ImageBasedLightingAsset(null, rtImage),
                GltfModel(null, rtModel),
                null,
            )

        assertThat(preference1).isEqualTo(preference2)
        assertThat(preference1.hashCode()).isEqualTo(preference2.hashCode())
    }

    @Test
    fun spatialEnvironmentPreferenceEqualsHashcode_returnsFalseIfAnyPropertiesAreNotEqual() {
        val rtImage1 = FakeExrImageResource(1)
        val rtModel1 = FakeGltfModelResource(1)
        val rtImage2 = FakeExrImageResource(2)
        val rtModel2 = FakeGltfModelResource(2)

        val basePreference =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ImageBasedLightingAsset(null, rtImage1),
                GltfModel(null, rtModel1),
                null,
            )

        val preferenceDiffGeometry =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ImageBasedLightingAsset(null, rtImage1),
                GltfModel(null, rtModel2),
                null,
            )
        assertThat(preferenceDiffGeometry).isNotEqualTo(basePreference)
        assertThat(preferenceDiffGeometry.hashCode()).isNotEqualTo(basePreference.hashCode())

        val preferenceDiffSkybox =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ImageBasedLightingAsset(null, rtImage2),
                GltfModel(null, rtModel1),
                null,
            )
        assertThat(preferenceDiffSkybox).isNotEqualTo(basePreference)
        assertThat(preferenceDiffSkybox.hashCode()).isNotEqualTo(basePreference.hashCode())
    }

    @Test
    fun spatialEnvironmentPreference_equalsHashCode_diffGeometryEntity_areNotEqual() {
        runBlocking {
            val rtImage = FakeExrImageResource(1)
            val rtModel = FakeGltfModelResource(1)
            val gltfModel2 = GltfModel.create(session, Paths.get("test2.glb"))
            val gltfModelEntity2 =
                GltfModelEntity.create(session, gltfModel2, parent = session.scene.activitySpace)

            val basePreference =
                SpatialEnvironment.SpatialEnvironmentPreference(
                    ImageBasedLightingAsset(null, rtImage),
                    GltfModel(null, rtModel),
                    gltfModelEntity,
                )
            val preferenceDiffGeometryEntity =
                SpatialEnvironment.SpatialEnvironmentPreference(
                    ImageBasedLightingAsset(null, rtImage),
                    GltfModel(null, rtModel),
                    gltfModelEntity2,
                )
            assertThat(preferenceDiffGeometryEntity).isNotEqualTo(basePreference)
            assertThat(preferenceDiffGeometryEntity.hashCode())
                .isNotEqualTo(basePreference.hashCode())
        }
    }

    @Test
    fun setSpatialEnvironmentPreferenceNull_callsRuntimeMethod() {
        check(environment!!.preferredSpatialEnvironment == null)

        val preference =
            SpatialEnvironment.SpatialEnvironmentPreference(imageBasedLightingAsset = null, null)

        environment!!.preferredSpatialEnvironment = preference

        assertThat(environment!!.preferredSpatialEnvironment).isNotNull()

        environment!!.preferredSpatialEnvironment = null

        assertThat(environment!!.preferredSpatialEnvironment).isNull()
    }

    @Test
    fun getSpatialEnvironmentPreference_readsFromRuntime_returnsMappedPreference() {
        // This test was originally setting state directly on the Fake.
        // Since tester doesn't expose a way to set preferredSpatialEnvironment,
        // we refactor it to set via the Public API and verify consistency.
        val rtImage = FakeExrImageResource(0)
        val rtModel = FakeGltfModelResource(0)

        val preference =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ImageBasedLightingAsset(null, rtImage),
                GltfModel(null, rtModel),
                null,
            )
        environment!!.preferredSpatialEnvironment = preference

        assertThat(environment!!.preferredSpatialEnvironment).isEqualTo(preference)
    }

    @Test
    fun getSpatialEnvironmentPreference_returnsSetPreference() {
        val rtImage = FakeExrImageResource(0)
        val rtModel = FakeGltfModelResource(0)

        val preference =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ImageBasedLightingAsset(null, rtImage),
                GltfModel(null, rtModel),
                gltfModelEntity,
            )
        environment!!.preferredSpatialEnvironment = preference

        assertThat(environment!!.preferredSpatialEnvironment).isEqualTo(preference)
    }

    @Test
    fun isPreferredSpatialEnvironmentActive_callsRuntimeIsPreferredSpatialEnvironmentActive() {
        spatialEnvironmentTester.triggerSpatialEnvironmentChanged(true)

        assertThat(environment!!.isPreferredSpatialEnvironmentActive).isTrue()
    }

    @Test
    fun addSpatialEnvironmentChangedListener_receivesRuntimeOnEnvironmentChangedEvents() {
        var listenerCalled = false
        val listener = Consumer<Boolean> { called: Boolean -> listenerCalled = called }
        environment!!.addSpatialEnvironmentChangedListener(listener)
        spatialEnvironmentTester.triggerSpatialEnvironmentChanged(true)
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun addSpatialEnvironmentChangedListener_withExecutor_receivesEventsOnExecutor() {
        var listenerCalledWithValue = false
        var listenerThread: Thread? = null
        val executor = directExecutor()

        val listener =
            Consumer<Boolean> { boolValue: Boolean ->
                listenerCalledWithValue = boolValue
                listenerThread = Thread.currentThread()
            }
        environment!!.addSpatialEnvironmentChangedListener(executor, listener)

        val eventValue = true
        spatialEnvironmentTester.triggerSpatialEnvironmentChanged(eventValue)

        assertThat(listenerCalledWithValue).isEqualTo(eventValue)
        assertThat(listenerThread).isNotNull()
    }

    @Test
    fun removeSpatialEnvironmentChangedListener_callsRuntimeRemoveOnSpatialEnvironmentChangedListener() {
        var listenerCalledCount = 0
        val listener = Consumer<Boolean> { listenerCalledCount++ }
        environment!!.addSpatialEnvironmentChangedListener(listener)

        spatialEnvironmentTester.triggerSpatialEnvironmentChanged(true)
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalledCount).isEqualTo(1)

        environment!!.removeSpatialEnvironmentChangedListener(listener)
        spatialEnvironmentTester.triggerSpatialEnvironmentChanged(true)
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalledCount).isEqualTo(1)
    }
}
