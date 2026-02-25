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

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SpatialEnvironment as RtSpatialEnvironment
import androidx.xr.scenecore.testing.FakeExrImageResource
import androidx.xr.scenecore.testing.FakeGltfModelResource
import androidx.xr.scenecore.testing.FakeSpatialEnvironment
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.nio.file.Paths
import java.util.function.Consumer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/**
 * Unit tests for the JXRCore SDK SpatialEnvironment Interface.
 *
 * TODO(b/329902726): Add a TestRuntime and verify CPM Integration.
 */
@RunWith(RobolectricTestRunner::class)
@SuppressLint("NewApi")
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SpatialEnvironmentTest {

    private lateinit var sceneRuntime: SceneRuntime
    private lateinit var session: Session
    private lateinit var fakeEnvironment: FakeSpatialEnvironment
    private var environment: SpatialEnvironment? = null
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var renderingRuntime: RenderingRuntime
    private lateinit var entityManager: EntityManager
    private lateinit var gltfModelEntity: GltfModelEntity

    @Before
    fun setUp() = runBlocking {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        sceneRuntime = session.sceneRuntime
        renderingRuntime = session.renderingRuntime
        entityManager = session.scene.entityManager
        fakeEnvironment = sceneRuntime.spatialEnvironment as FakeSpatialEnvironment
        environment = SpatialEnvironment(sceneRuntime, entityManager)

        val gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        gltfModelEntity =
            GltfModelEntity.create(sceneRuntime, renderingRuntime, entityManager, gltfModel)
    }

    @Test
    fun currentPassthroughOpacity_getsRuntimePassthroughOpacity() {
        val rtOpacity = 0.3f
        fakeEnvironment.passthroughOpacityChangedListenerMap.forEach { (consumer, executor) ->
            executor.execute { consumer.accept(rtOpacity) }
        }

        assertThat(environment!!.currentPassthroughOpacity).isEqualTo(rtOpacity)
    }

    @Test
    fun getPassthroughOpacityPreference_getsRuntimePassthroughOpacityPreference() {
        val rtPreference = 0.3f
        fakeEnvironment.preferredPassthroughOpacity = rtPreference

        assertThat(environment!!.preferredPassthroughOpacity).isEqualTo(rtPreference)
    }

    @Test
    fun getPassthroughOpacityPreferenceNoPreference_getsRuntimePassthroughOpacityPreference() {
        val rtPreference = SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
        fakeEnvironment.preferredPassthroughOpacity = rtPreference

        assertThat(environment!!.preferredPassthroughOpacity).isEqualTo(rtPreference)
    }

    @Test
    fun setPassthroughOpacityPreference_callsRuntimeSetPassthroughOpacityPreference() {
        val preference = 0.3f
        environment!!.preferredPassthroughOpacity = preference

        assertThat(fakeEnvironment.preferredPassthroughOpacity).isEqualTo(preference)
    }

    @Test
    fun setPassthroughOpacityPreferenceNoPreference_callsRuntimeSetPassthroughOpacityPreference() {
        val preference = SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
        environment!!.preferredPassthroughOpacity = preference

        assertThat(fakeEnvironment.preferredPassthroughOpacity).isEqualTo(preference)
    }

    @Test
    fun addOnPassthroughOpacityChangedListener_ReceivesRuntimeOnPassthroughOpacityChangedEvents() {
        check(fakeEnvironment.passthroughOpacityChangedListenerMap.size == 1)

        var listenerCalledWithValue = 0.0f
        val listener = Consumer<Float> { floatValue: Float -> listenerCalledWithValue = floatValue }
        environment!!.addOnPassthroughOpacityChangedListener(listener)

        assertThat(fakeEnvironment.passthroughOpacityChangedListenerMap).hasSize(2)

        fakeEnvironment.passthroughOpacityChangedListenerMap.forEach { (consumer, executor) ->
            executor.execute { consumer.accept(0.3f) }
        }
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalledWithValue).isEqualTo(0.3f)
    }

    @Test
    fun addOnPassthroughOpacityChangedListener_withExecutor_receivesEventsOnExecutor() {
        var listenerCalledWithValue = 0.0f
        var listenerThread: Thread? = null
        val executor = directExecutor()

        val listener =
            Consumer<Float> { floatValue: Float ->
                listenerCalledWithValue = floatValue
                listenerThread = Thread.currentThread()
            }
        environment!!.addOnPassthroughOpacityChangedListener(executor, listener)

        val eventValue = 0.3f
        fakeEnvironment.passthroughOpacityChangedListenerMap.forEach { (consumer, executor) ->
            executor.execute { consumer.accept(0.3f) }
        }

        assertThat(listenerCalledWithValue).isEqualTo(eventValue)
        assertThat(listenerThread).isNotNull()
    }

    @Test
    fun addOnPassthroughOpacityChangedListener_withoutExecutor_usesMainThreadExecutor() {
        val listener = Consumer<Float> {}
        environment!!.addOnPassthroughOpacityChangedListener(listener)

        assertThat(fakeEnvironment.passthroughOpacityChangedListenerMap[listener])
            .isEqualTo(HandlerExecutor.mainThreadExecutor)
    }

    @Test
    fun removeOnPassthroughOpacityChangedListener_callsRuntimeRemoveOnPassthroughOpacityChangedListener() {
        val listener = Consumer<Float> {}
        environment!!.addOnPassthroughOpacityChangedListener(listener)

        assertThat(fakeEnvironment.passthroughOpacityChangedListenerMap).hasSize(2)

        environment!!.removeOnPassthroughOpacityChangedListener(listener)

        assertThat(fakeEnvironment.passthroughOpacityChangedListenerMap).hasSize(1)
    }

    @Test
    fun spatialEnvironmentPreferenceEqualsHashcode_returnsTrueIfAllPropertiesAreEqual() {
        val rtImage = FakeExrImageResource(0)
        val rtModel = FakeGltfModelResource(0)

        val preference1 =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(null, rtImage),
                GltfModel(null, rtModel),
                null,
            )
        val preference2 =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(null, rtImage),
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
                ExrImage(null, rtImage1),
                GltfModel(null, rtModel1),
                null,
            )

        val preferenceDiffGeometry =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(null, rtImage1),
                GltfModel(null, rtModel2),
                null,
            )
        assertThat(preferenceDiffGeometry).isNotEqualTo(basePreference)
        assertThat(preferenceDiffGeometry.hashCode()).isNotEqualTo(basePreference.hashCode())

        val preferenceDiffSkybox =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(null, rtImage2),
                GltfModel(null, rtModel1),
                null,
            )
        assertThat(preferenceDiffSkybox).isNotEqualTo(basePreference)
        assertThat(preferenceDiffSkybox.hashCode()).isNotEqualTo(basePreference.hashCode())
    }

    @Test
    fun setSpatialEnvironmentPreference_callsRuntimeMethod() {
        val rtImage = FakeExrImageResource(0)
        val rtModel = FakeGltfModelResource(0)

        val preference =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(null, rtImage),
                GltfModel(null, rtModel),
                gltfModelEntity,
            )
        val rtPreference = preference.toRtSpatialEnvironmentPreference()

        environment!!.preferredSpatialEnvironment = preference

        assertThat(fakeEnvironment.preferredSpatialEnvironment).isEqualTo(rtPreference)

        assertThat(fakeEnvironment.preferredSpatialEnvironment?.geometryEntity)
            .isEqualTo(gltfModelEntity.rtEntity)
    }

    @Test
    fun setSpatialEnvironmentPreferenceNull_callsRuntimeMethod() {
        check(environment!!.preferredSpatialEnvironment == null)

        val preference = SpatialEnvironment.SpatialEnvironmentPreference(null, null)

        environment!!.preferredSpatialEnvironment = preference

        assertThat(fakeEnvironment.preferredSpatialEnvironment).isNotNull()

        environment!!.preferredSpatialEnvironment = null

        assertThat(fakeEnvironment.preferredSpatialEnvironment).isNull()
    }

    @Test
    fun getSpatialEnvironmentPreference_readsFromRuntime_returnsMappedPreference() {
        val rtImage = FakeExrImageResource(0)
        val rtModel = FakeGltfModelResource(0)
        val rtPreference = RtSpatialEnvironment.SpatialEnvironmentPreference(rtImage, rtModel, null)
        fakeEnvironment.preferredSpatialEnvironment = rtPreference

        val expectedPreference =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(null, rtImage),
                GltfModel(null, rtModel),
                null,
            )

        assertThat(environment!!.preferredSpatialEnvironment).isEqualTo(expectedPreference)
    }

    @Test
    fun getSpatialEnvironmentPreference_returnsSetPreference() {
        val rtImage = FakeExrImageResource(0)
        val rtModel = FakeGltfModelResource(0)

        val preference =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(null, rtImage),
                GltfModel(null, rtModel),
                gltfModelEntity,
            )
        environment!!.preferredSpatialEnvironment = preference

        assertThat(environment!!.preferredSpatialEnvironment).isEqualTo(preference)
    }

    @Test
    fun isPreferredSpatialEnvironmentActive_callsRuntimeisPreferredSpatialEnvironmentActive() {
        fakeEnvironment.spatialEnvironmentChangedListenerMap.forEach { (consumer, executor) ->
            executor.execute { consumer.accept(true) }
        }

        assertThat(environment!!.isPreferredSpatialEnvironmentActive).isTrue()
    }

    @Test
    fun addOnSpatialEnvironmentChangedListener_ReceivesRuntimeEnvironmentOnEnvironmentChangedEvents() {
        var listenerCalled = false
        val listener = Consumer<Boolean> { called: Boolean -> listenerCalled = called }
        environment!!.addOnSpatialEnvironmentChangedListener(listener)
        fakeEnvironment.spatialEnvironmentChangedListenerMap.forEach { (consumer, executor) ->
            executor.execute { consumer.accept(true) }
        }
        ShadowLooper.idleMainLooper()

        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun addOnSpatialEnvironmentChangedListener_withExecutor_receivesEventsOnExecutor() {
        var listenerCalledWithValue = false
        var listenerThread: Thread? = null
        val executor = directExecutor()

        val listener =
            Consumer<Boolean> { boolValue: Boolean ->
                listenerCalledWithValue = boolValue
                listenerThread = Thread.currentThread()
            }
        environment!!.addOnSpatialEnvironmentChangedListener(executor, listener)

        val eventValue = true
        fakeEnvironment.spatialEnvironmentChangedListenerMap.forEach { (consumer, executor) ->
            executor.execute { consumer.accept(eventValue) }
        }

        assertThat(listenerCalledWithValue).isEqualTo(eventValue)
        assertThat(listenerThread).isNotNull()
    }

    @Test
    fun addOnSpatialEnvironmentChangedListener_withoutExecutor_usesMainThreadExecutor() {
        val listener = Consumer<Boolean> {}
        environment!!.addOnSpatialEnvironmentChangedListener(listener)

        assertThat(fakeEnvironment.spatialEnvironmentChangedListenerMap[listener])
            .isEqualTo(HandlerExecutor.mainThreadExecutor)
    }

    @Test
    fun removeOnSpatialEnvironmentChangedListener_callsRuntimeRemoveOnSpatialEnvironmentChangedListener() {
        val listener = Consumer<Boolean> {}
        environment!!.addOnSpatialEnvironmentChangedListener(listener)
        assertThat(fakeEnvironment.spatialEnvironmentChangedListenerMap).hasSize(2)

        environment!!.removeOnSpatialEnvironmentChangedListener(listener)
        assertThat(fakeEnvironment.spatialEnvironmentChangedListenerMap).hasSize(1)
    }
}
