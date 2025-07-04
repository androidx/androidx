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

import androidx.xr.runtime.internal.ExrImageResource as RtExrImageResource
import androidx.xr.runtime.internal.GltfModelResource as RtGltfModelResource
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.MaterialResource as RtMaterialResource
import androidx.xr.runtime.internal.SpatialEnvironment as RtSpatialEnvironment
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for the JXRCore SDK SpatialEnvironment Interface.
 *
 * TODO(b/329902726): Add a TestRuntime and verify CPM Integration.
 */
@RunWith(JUnit4::class)
class SpatialEnvironmentTest {

    private var mockRuntime: JxrPlatformAdapter = mock<JxrPlatformAdapter>()
    private var mockRtEnvironment: RtSpatialEnvironment? = null
    private var environment: SpatialEnvironment? = null

    @Before
    fun setUp() {
        mockRtEnvironment = mock<RtSpatialEnvironment>()
        whenever(mockRuntime.spatialEnvironment).thenReturn(mockRtEnvironment)

        environment = SpatialEnvironment(mockRuntime)
    }

    @Test
    fun currentPassthroughOpacity_getsRuntimePassthroughOpacity() {
        val rtOpacity = 0.3f
        whenever(mockRtEnvironment!!.currentPassthroughOpacity).thenReturn(rtOpacity)
        assertThat(environment!!.currentPassthroughOpacity).isEqualTo(rtOpacity)
        verify(mockRtEnvironment!!).currentPassthroughOpacity
    }

    @Test
    fun getPassthroughOpacityPreference_getsRuntimePassthroughOpacityPreference() {
        val rtPreference = 0.3f
        whenever(mockRtEnvironment!!.preferredPassthroughOpacity).thenReturn(rtPreference)

        assertThat(environment!!.preferredPassthroughOpacity).isEqualTo(rtPreference)
        verify(mockRtEnvironment!!).preferredPassthroughOpacity
    }

    @Test
    fun getPassthroughOpacityPreferenceNoPreference_getsRuntimePassthroughOpacityPreference() {
        val rtPreference = SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
        whenever(mockRtEnvironment!!.preferredPassthroughOpacity).thenReturn(rtPreference)

        assertThat(environment!!.preferredPassthroughOpacity).isEqualTo(rtPreference)
        verify(mockRtEnvironment!!).preferredPassthroughOpacity
    }

    @Test
    fun setPassthroughOpacityPreference_callsRuntimeSetPassthroughOpacityPreference() {
        val preference = 0.3f
        environment!!.preferredPassthroughOpacity = preference
        verify(mockRtEnvironment)!!.preferredPassthroughOpacity = preference
    }

    @Test
    fun setPassthroughOpacityPreferenceNoPreference_callsRuntimeSetPassthroughOpacityPreference() {
        val preference = SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
        environment!!.preferredPassthroughOpacity = preference
        verify(mockRtEnvironment)!!.preferredPassthroughOpacity = preference
    }

    @Test
    fun addOnPassthroughOpacityChangedListener_ReceivesRuntimeOnPassthroughOpacityChangedEvents() {
        var listenerCalledWithValue = 0.0f
        val captor = argumentCaptor<Consumer<Float>>()
        val listener = Consumer<Float> { floatValue: Float -> listenerCalledWithValue = floatValue }
        environment!!.addOnPassthroughOpacityChangedListener(listener)
        verify(mockRtEnvironment!!).addOnPassthroughOpacityChangedListener(any(), captor.capture())
        captor.firstValue.accept(0.3f)
        assertThat(listenerCalledWithValue).isEqualTo(0.3f)
    }

    @Test
    fun addOnPassthroughOpacityChangedListener_withExecutor_receivesEventsOnExecutor() {
        var listenerCalledWithValue = 0.0f
        var listenerThread: Thread? = null
        val rtListenerCaptor = argumentCaptor<Consumer<Float>>()
        val executor = directExecutor()

        val listener =
            Consumer<Float> { floatValue: Float ->
                listenerCalledWithValue = floatValue
                listenerThread = Thread.currentThread()
            }
        environment!!.addOnPassthroughOpacityChangedListener(executor, listener)
        verify(mockRtEnvironment!!)
            .addOnPassthroughOpacityChangedListener(eq(executor), rtListenerCaptor.capture())

        val eventValue = 0.3f
        executor.execute { rtListenerCaptor.firstValue.accept(eventValue) }

        assertThat(listenerCalledWithValue).isEqualTo(eventValue)
        assertThat(listenerThread).isNotNull()
    }

    @Test
    fun addOnPassthroughOpacityChangedListener_withoutExecutor_usesMainThreadExecutor() {
        val listener = Consumer<Float> {}
        environment!!.addOnPassthroughOpacityChangedListener(listener)
        verify(mockRtEnvironment!!)
            .addOnPassthroughOpacityChangedListener(eq(HandlerExecutor.mainThreadExecutor), any())
    }

    @Test
    fun removeOnPassthroughOpacityChangedListener_callsRuntimeRemoveOnPassthroughOpacityChangedListener() {
        val captor = argumentCaptor<Consumer<Float>>()
        val listener = Consumer<Float> {}
        environment!!.removeOnPassthroughOpacityChangedListener(listener)
        verify(mockRtEnvironment!!).removeOnPassthroughOpacityChangedListener(captor.capture())
        assertThat(captor.firstValue).isEqualTo(listener)
    }

    @Test
    fun spatialEnvironmentPreferenceEqualsHashcode_returnsTrueIfAllPropertiesAreEqual() {
        val rtImageMock = mock<RtExrImageResource>()
        val rtModelMock = mock<RtGltfModelResource>()
        val rtMaterialMock = mock<RtMaterialResource>()
        val rtMeshName = "meshName"
        val rtAnimationName = "animationName"
        val rtPreference =
            RtSpatialEnvironment.SpatialEnvironmentPreference(
                rtImageMock,
                rtModelMock,
                rtMaterialMock,
                rtMeshName,
                rtAnimationName,
            )
        val preference =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(rtImageMock),
                GltfModel(rtModelMock),
                Material(rtMaterialMock),
                rtMeshName,
                rtAnimationName,
            )

        assertThat(preference).isEqualTo(rtPreference.toSpatialEnvironmentPreference())
        assertThat(preference.hashCode())
            .isEqualTo(rtPreference.toSpatialEnvironmentPreference().hashCode())
    }

    @Test
    fun spatialEnvironmentPreferenceEqualsHashcode_returnsFalseIfAnyPropertiesAreNotEqual() {
        val rtImageMock = mock<RtExrImageResource>()
        val rtModelMock = mock<RtGltfModelResource>()
        val rtMaterialMock = mock<RtMaterialResource>()
        val rtMeshName = "meshName"
        val rtAnimationName = "animationName"
        val rtImageMock2 = mock<RtExrImageResource>()
        val rtModelMock2 = mock<RtGltfModelResource>()
        val rtMaterialMock2 = mock<RtMaterialResource>()
        val rtMeshName2 = "meshName2"
        val rtAnimationName2 = "animationName2"
        val rtPreference =
            RtSpatialEnvironment.SpatialEnvironmentPreference(
                rtImageMock,
                rtModelMock,
                rtMaterialMock,
                rtMeshName,
                rtAnimationName,
            )

        val preferenceDiffGeometry =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(rtImageMock),
                GltfModel(rtModelMock2),
                Material(rtMaterialMock2),
                rtMeshName2,
                rtAnimationName2,
            )
        assertThat(preferenceDiffGeometry)
            .isNotEqualTo(rtPreference.toSpatialEnvironmentPreference())
        assertThat(preferenceDiffGeometry.hashCode())
            .isNotEqualTo(rtPreference.toSpatialEnvironmentPreference().hashCode())

        val preferenceDiffSkybox =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(rtImageMock2),
                GltfModel(rtModelMock),
                Material(rtMaterialMock),
                rtMeshName,
                rtAnimationName,
            )
        assertThat(preferenceDiffSkybox).isNotEqualTo(rtPreference.toSpatialEnvironmentPreference())
        assertThat(preferenceDiffSkybox.hashCode())
            .isNotEqualTo(rtPreference.toSpatialEnvironmentPreference().hashCode())
    }

    @Test
    fun setSpatialEnvironmentPreference_callsRuntimeMethod() {
        val rtImageMock = mock<RtExrImageResource>()
        val rtModelMock = mock<RtGltfModelResource>()

        val preference =
            SpatialEnvironment.SpatialEnvironmentPreference(
                ExrImage(rtImageMock),
                GltfModel(rtModelMock),
            )

        environment!!.preferredSpatialEnvironment = preference

        verify(mockRtEnvironment!!).preferredSpatialEnvironment = any()
    }

    @Test
    fun setSpatialEnvironmentPreferenceNull_callsRuntimeMethod() {
        val preference = null as SpatialEnvironment.SpatialEnvironmentPreference?
        environment!!.preferredSpatialEnvironment = preference
        verify(mockRtEnvironment!!).preferredSpatialEnvironment = anyOrNull()
    }

    @Test
    fun getSpatialEnvironmentPreference_getsRuntimeEnvironmentSpatialEnvironmentPreference() {
        val rtImageMock = mock<RtExrImageResource>()
        val rtModelMock = mock<RtGltfModelResource>()
        val rtPreference =
            RtSpatialEnvironment.SpatialEnvironmentPreference(rtImageMock, rtModelMock)
        whenever(mockRtEnvironment!!.preferredSpatialEnvironment).thenReturn(rtPreference)

        assertThat(environment!!.preferredSpatialEnvironment)
            .isEqualTo(rtPreference.toSpatialEnvironmentPreference())
        verify(mockRtEnvironment!!).preferredSpatialEnvironment
    }

    @Test
    fun getSpatialEnvironmentPreferenceNull_getsRuntimeEnvironmentSpatialEnvironmentPreference() {
        val rtPreference = null as RtSpatialEnvironment.SpatialEnvironmentPreference?
        whenever(mockRtEnvironment!!.preferredSpatialEnvironment).thenReturn(rtPreference)

        assertThat(environment!!.preferredSpatialEnvironment).isEqualTo(null)
        verify(mockRtEnvironment!!).preferredSpatialEnvironment
    }

    @Test
    fun isPreferredSpatialEnvironmentActive_callsRuntimeisPreferredSpatialEnvironmentActive() {
        whenever(mockRtEnvironment!!.isPreferredSpatialEnvironmentActive).thenReturn(true)
        assertThat(environment!!.isPreferredSpatialEnvironmentActive).isTrue()
        verify(mockRtEnvironment!!).isPreferredSpatialEnvironmentActive
    }

    @Test
    fun addOnSpatialEnvironmentChangedListener_ReceivesRuntimeEnvironmentOnEnvironmentChangedEvents() {
        var listenerCalled = false
        val captor = argumentCaptor<Consumer<Boolean>>()
        val listener = Consumer<Boolean> { called: Boolean -> listenerCalled = called }
        environment!!.addOnSpatialEnvironmentChangedListener(listener)
        verify(mockRtEnvironment!!).addOnSpatialEnvironmentChangedListener(any(), captor.capture())
        captor.firstValue.accept(true)
        assertThat(listenerCalled).isTrue()
    }

    @Test
    fun addOnSpatialEnvironmentChangedListener_withExecutor_receivesEventsOnExecutor() {
        var listenerCalledWithValue = false
        var listenerThread: Thread? = null
        val rtListenerCaptor = argumentCaptor<Consumer<Boolean>>()
        val executor = directExecutor()

        val listener =
            Consumer<Boolean> { boolValue: Boolean ->
                listenerCalledWithValue = boolValue
                listenerThread = Thread.currentThread()
            }
        environment!!.addOnSpatialEnvironmentChangedListener(executor, listener)
        verify(mockRtEnvironment!!)
            .addOnSpatialEnvironmentChangedListener(eq(executor), rtListenerCaptor.capture())

        val eventValue = true
        executor.execute { rtListenerCaptor.firstValue.accept(eventValue) }

        assertThat(listenerCalledWithValue).isEqualTo(eventValue)
        assertThat(listenerThread).isNotNull()
    }

    @Test
    fun addOnSpatialEnvironmentChangedListener_withoutExecutor_usesMainThreadExecutor() {
        val listener = Consumer<Boolean> {}
        environment!!.addOnSpatialEnvironmentChangedListener(listener)
        // Verify that the rtEnvironment's method was called with the main thread executor
        verify(mockRtEnvironment!!)
            .addOnSpatialEnvironmentChangedListener(eq(HandlerExecutor.mainThreadExecutor), any())
    }

    @Test
    fun removeOnSpatialEnvironmentChangedListener_callsRuntimeRemoveOnSpatialEnvironmentChangedListener() {
        val captor = argumentCaptor<Consumer<Boolean>>()
        val listener = Consumer<Boolean> {}
        environment!!.removeOnSpatialEnvironmentChangedListener(listener)
        verify(mockRtEnvironment!!).removeOnSpatialEnvironmentChangedListener(captor.capture())
        assertThat(listener).isEqualTo(captor.firstValue)
    }
}
