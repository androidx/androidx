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

import androidx.activity.ComponentActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SpatialEnvironment as RtSpatialEnvironment
import androidx.xr.scenecore.testing.FakeExrImageResource
import androidx.xr.scenecore.testing.FakeGltfModelResource
import androidx.xr.scenecore.testing.FakeResource
import androidx.xr.scenecore.testing.FakeSpatialEnvironment
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
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
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SpatialEnvironmentTest {

    private lateinit var sceneRuntime: SceneRuntime
    private lateinit var session: Session
    private lateinit var fakeEnvironment: FakeSpatialEnvironment
    private var environment: SpatialEnvironment? = null
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        sceneRuntime = session.sceneRuntime
        fakeEnvironment = sceneRuntime.spatialEnvironment as FakeSpatialEnvironment
        environment = SpatialEnvironment(sceneRuntime)
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
        val rtMaterial = FakeResource()
        val rtNodeName = "nodeName"
        val rtAnimationName = "animationName"
        val rtPreference =
            RtSpatialEnvironment.SpatialEnvironmentPreference(
                rtImage,
                rtModel,
                rtMaterial,
                rtNodeName,
                rtAnimationName,
            )
        val preference =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(null, rtImage),
                GltfModel(null, rtModel),
                object : Material {
                    override val material = rtMaterial

                    override fun close() {
                        // The lifecycle of this material is managed by the SpatialEnvironment.
                    }
                },
                rtNodeName,
                rtAnimationName,
            )

        assertThat(preference).isEqualTo(rtPreference.toSpatialEnvironmentPreference())
        assertThat(preference.hashCode())
            .isEqualTo(rtPreference.toSpatialEnvironmentPreference().hashCode())
    }

    @Test
    fun spatialEnvironmentPreferenceEqualsHashcode_returnsFalseIfAnyPropertiesAreNotEqual() {
        val rtImage = FakeExrImageResource(1)
        val rtModel = FakeGltfModelResource(1)
        val rtMaterial = FakeResource(1)
        val rtNodeName = "nodeName"
        val rtAnimationName = "animationName"
        val rtImage2 = FakeExrImageResource(2)
        val rtModel2 = FakeGltfModelResource(2)
        val rtMaterial2 = FakeResource(2)
        val rtNodeName2 = "nodeName2"
        val rtAnimationName2 = "animationName2"
        val rtPreference =
            RtSpatialEnvironment.SpatialEnvironmentPreference(
                rtImage,
                rtModel,
                rtMaterial,
                rtNodeName,
                rtAnimationName,
            )

        val preferenceDiffGeometry =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(null, rtImage),
                GltfModel(null, rtModel2),
                object : Material {
                    override val material = rtMaterial2

                    override fun close() {
                        // The lifecycle of this material is managed by the SpatialEnvironment.
                    }
                },
                rtNodeName2,
                rtAnimationName2,
            )
        assertThat(preferenceDiffGeometry)
            .isNotEqualTo(rtPreference.toSpatialEnvironmentPreference())
        assertThat(preferenceDiffGeometry.hashCode())
            .isNotEqualTo(rtPreference.toSpatialEnvironmentPreference().hashCode())

        val preferenceDiffSkybox =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(null, rtImage2),
                GltfModel(null, rtModel),
                object : Material {
                    override val material = rtMaterial

                    override fun close() {
                        // The lifecycle of this material is managed by the SpatialEnvironment.
                    }
                },
                rtNodeName,
                rtAnimationName,
            )
        assertThat(preferenceDiffSkybox).isNotEqualTo(rtPreference.toSpatialEnvironmentPreference())
        assertThat(preferenceDiffSkybox.hashCode())
            .isNotEqualTo(rtPreference.toSpatialEnvironmentPreference().hashCode())
    }

    @Test
    fun setSpatialEnvironmentPreference_callsRuntimeMethod() {
        val rtImage = FakeExrImageResource(0)
        val rtModel = FakeGltfModelResource(0)

        val preference =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(null, rtImage),
                GltfModel(null, rtModel),
            )
        val rtPreference = preference.toRtSpatialEnvironmentPreference()

        environment!!.preferredSpatialEnvironment = preference

        assertThat(fakeEnvironment.preferredSpatialEnvironment).isEqualTo(rtPreference)
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
    fun getSpatialEnvironmentPreference_getsRuntimeEnvironmentSpatialEnvironmentPreference() {
        val rtImage = FakeExrImageResource(0)
        val rtModel = FakeGltfModelResource(0)
        val rtPreference = RtSpatialEnvironment.SpatialEnvironmentPreference(rtImage, rtModel)
        fakeEnvironment.preferredSpatialEnvironment = rtPreference

        assertThat(environment!!.preferredSpatialEnvironment)
            .isEqualTo(rtPreference.toSpatialEnvironmentPreference())
    }

    @Test
    fun getSpatialEnvironmentPreferenceNull_getsRuntimeEnvironmentSpatialEnvironmentPreference() {
        fakeEnvironment.preferredSpatialEnvironment = null

        assertThat(environment!!.preferredSpatialEnvironment).isNull()
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
