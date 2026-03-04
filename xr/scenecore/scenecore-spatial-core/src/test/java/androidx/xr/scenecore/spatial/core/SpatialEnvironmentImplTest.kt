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
import androidx.xr.scenecore.runtime.ExrImageResource
import androidx.xr.scenecore.runtime.GltfModelResource
import androidx.xr.scenecore.runtime.SpatialEnvironment
import androidx.xr.scenecore.runtime.SpatialEnvironmentFeature
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.environment.EnvironmentVisibilityState
import com.android.extensions.xr.environment.PassthroughVisibilityState
import com.android.extensions.xr.environment.ShadowEnvironmentVisibilityState
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState
import com.android.extensions.xr.node.NodeRepository
import com.android.extensions.xr.space.ShadowSpatialState
import com.android.extensions.xr.space.SpatialState
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import java.util.function.Consumer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Technically this doesn't need to be a Robolectric test, since it doesn't directly depend on
// any Android subsystems. However, we're currently using an Android test runner for consistency
// with other Android XR impl tests in this directory.
/** Unit tests for the AndroidXR implementation of JXRCore's SpatialEnvironment module. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SpatialEnvironmentImplTest {
    private val nodeRepository: NodeRepository = NodeRepository.getInstance()
    private val mockSpatialEnvironmentFeature: SpatialEnvironmentFeature =
        mock<SpatialEnvironmentFeature>()
    private val activityController = Robolectric.buildActivity(Activity::class.java)
    private val activity = activityController.create().start().get()
    private val xrExtensions: XrExtensions = getXrExtensions()!!
    private lateinit var spatialEnvironmentImpl: SpatialEnvironmentImpl

    @Before
    fun setUp() {
        // Reset our state.
        val sceneRootNode = xrExtensions.createNode()
        spatialEnvironmentImpl =
            SpatialEnvironmentImpl(activity, xrExtensions, sceneRootNode) { this.spatialState }
    }

    private val spatialState: SpatialState
        get() = xrExtensions.getSpatialState(activity)

    private fun onRenderingFeatureReady() {
        spatialEnvironmentImpl.onRenderingFeatureReady(mockSpatialEnvironmentFeature)
    }

    @After
    fun tearDown() {
        activityController.destroy()
    }

    @Test
    fun setPreferredPassthroughOpacity() {
        spatialEnvironmentImpl.preferredPassthroughOpacity =
            SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE

        assertThat(spatialEnvironmentImpl.preferredPassthroughOpacity)
            .isEqualTo(SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE)

        spatialEnvironmentImpl.preferredPassthroughOpacity = (0.1f)

        assertThat(nodeRepository.getPassthroughOpacity(spatialEnvironmentImpl.passthroughNode))
            .isEqualTo(0.1f)
        assertThat(spatialEnvironmentImpl.preferredPassthroughOpacity).isEqualTo(0.1f)
    }

    @Test
    fun setPreferredPassthroughOpacityNearOrUnderZero_getsZeroOpacity() {
        // Opacity values below 1% should be treated as zero.
        spatialEnvironmentImpl.preferredPassthroughOpacity = (0.009f)

        assertThat(spatialEnvironmentImpl.preferredPassthroughOpacity).isEqualTo(0.0f)

        spatialEnvironmentImpl.preferredPassthroughOpacity = (-0.1f)

        assertThat(nodeRepository.getPassthroughOpacity(spatialEnvironmentImpl.passthroughNode))
            .isEqualTo(0.0f)
        assertThat(spatialEnvironmentImpl.preferredPassthroughOpacity).isEqualTo(0.0f)
    }

    @Test
    fun setPreferredPassthroughOpacityNearOrOverOne_getsFullOpacity() {
        // Opacity values above 99% should be treated as full opacity.
        spatialEnvironmentImpl.preferredPassthroughOpacity = (0.991f)

        assertThat(spatialEnvironmentImpl.preferredPassthroughOpacity).isEqualTo(1.0f)

        spatialEnvironmentImpl.preferredPassthroughOpacity = (1.1f)

        assertThat(nodeRepository.getPassthroughOpacity(spatialEnvironmentImpl.passthroughNode))
            .isEqualTo(1.0f)
        assertThat(spatialEnvironmentImpl.preferredPassthroughOpacity).isEqualTo(1.0f)
    }

    @Test
    fun getCurrentPassthroughOpacity_returnsZeroInitially() {
        assertThat(spatialEnvironmentImpl.currentPassthroughOpacity).isEqualTo(0.0f)
    }

    @Test
    fun onPassthroughOpacityChangedListener_firesOnPassthroughOpacityChange() {
        val listener1 = mock<Consumer<Float>>()
        val listener2 = mock<Consumer<Float>>()

        spatialEnvironmentImpl.addOnPassthroughOpacityChangedListener(
            MoreExecutors.directExecutor(),
            listener1,
        )
        spatialEnvironmentImpl.addOnPassthroughOpacityChangedListener(
            MoreExecutors.directExecutor(),
            listener2,
        )

        val opacity = spatialEnvironmentImpl.currentPassthroughOpacity
        spatialEnvironmentImpl.firePassthroughOpacityChangedEvent()

        verify(listener1).accept(opacity)
        verify(listener2).accept(opacity)

        spatialEnvironmentImpl.removeOnPassthroughOpacityChangedListener(listener1)
        spatialEnvironmentImpl.firePassthroughOpacityChangedEvent()

        verify(listener1).accept(opacity)
        verify(listener2, times(2)).accept(opacity)
    }

    @Test
    fun setPreferredSpatialEnv_featureNotReady_throwsError_whenPreferenceIsNonNullAndHasContent() {
        Assert.assertThrows(UnsupportedOperationException::class.java) {
            spatialEnvironmentImpl.preferredSpatialEnvironment =
                (SpatialEnvironment.SpatialEnvironmentPreference(
                    object : ExrImageResource {},
                    object : GltfModelResource {},
                ))
        }
    }

    @Test
    fun setPreferredSpatialEnv_featureNotReady_setsAndGetsEmptyPrefCorrectly() {
        val emptyPref = SpatialEnvironment.SpatialEnvironmentPreference(null, null)

        spatialEnvironmentImpl.preferredSpatialEnvironment = emptyPref

        assertThat(spatialEnvironmentImpl.preferredSpatialEnvironment).isEqualTo(emptyPref)
    }

    @Test
    fun setPreferredSpatialEnv_featureReady_featureIsCalled() {
        onRenderingFeatureReady()

        spatialEnvironmentImpl.preferredSpatialEnvironment =
            (SpatialEnvironment.SpatialEnvironmentPreference(
                object : ExrImageResource {},
                object : GltfModelResource {},
            ))

        verify(mockSpatialEnvironmentFeature).preferredSpatialEnvironment =
            any<SpatialEnvironment.SpatialEnvironmentPreference>()
    }

    @Test
    fun getPreferredSpatialEnv_featureReady_featureIsCalled() {
        onRenderingFeatureReady()

        val mockPref = SpatialEnvironment.SpatialEnvironmentPreference(null, null)
        whenever(mockSpatialEnvironmentFeature.preferredSpatialEnvironment).thenReturn(mockPref)

        val result = spatialEnvironmentImpl.preferredSpatialEnvironment

        assertThat(result).isEqualTo(mockPref)

        verify(mockSpatialEnvironmentFeature).preferredSpatialEnvironment
    }

    @Test
    fun isPreferredSpatialEnvironmentActive_defaultsToFalse() {
        assertThat(spatialEnvironmentImpl.isPreferredSpatialEnvironmentActive).isFalse()
    }

    @Test
    fun onSpatialEnvironmentChangedListener_firesOnEnvironmentChange() {
        val listener1 = mock<Consumer<Boolean>>()
        val listener2 = mock<Consumer<Boolean>>()

        val spatialState = ShadowSpatialState.create()
        spatialEnvironmentImpl.setSpatialState(spatialState)

        spatialEnvironmentImpl.addOnSpatialEnvironmentChangedListener(
            MoreExecutors.directExecutor(),
            listener1,
        )
        spatialEnvironmentImpl.addOnSpatialEnvironmentChangedListener(
            MoreExecutors.directExecutor(),
            listener2,
        )

        val isPreferredSpatialEnvironmentActive =
            spatialEnvironmentImpl.isPreferredSpatialEnvironmentActive

        spatialEnvironmentImpl.fireOnSpatialEnvironmentChangedEvent()

        verify(listener1).accept(isPreferredSpatialEnvironmentActive)
        verify(listener2).accept(isPreferredSpatialEnvironmentActive)

        spatialEnvironmentImpl.removeOnSpatialEnvironmentChangedListener(listener1)
        spatialEnvironmentImpl.fireOnSpatialEnvironmentChangedEvent()
        verify(listener1).accept(isPreferredSpatialEnvironmentActive)

        verify(listener2, times(2)).accept(isPreferredSpatialEnvironmentActive)
    }

    @Test
    fun dispose_clearsSpatialEnvironmentPreferenceListeners() {
        val listener = mock<Consumer<Boolean>>()

        val spatialState = ShadowSpatialState.create()
        spatialEnvironmentImpl.setSpatialState(spatialState)
        spatialEnvironmentImpl.addOnSpatialEnvironmentChangedListener(
            MoreExecutors.directExecutor(),
            listener,
        )

        val isPreferredSpatialEnvironmentActive =
            spatialEnvironmentImpl.isPreferredSpatialEnvironmentActive
        spatialEnvironmentImpl.fireOnSpatialEnvironmentChangedEvent()

        verify(listener).accept(isPreferredSpatialEnvironmentActive)

        spatialEnvironmentImpl.dispose()
        spatialEnvironmentImpl.fireOnSpatialEnvironmentChangedEvent()

        verify(listener).accept(isPreferredSpatialEnvironmentActive)
    }

    @Test
    fun dispose_clearsPreferredPassthroughOpacityListeners() {
        val listener = mock<Consumer<Float>>()
        spatialEnvironmentImpl.addOnPassthroughOpacityChangedListener(
            MoreExecutors.directExecutor(),
            listener,
        )

        val opacity = spatialEnvironmentImpl.currentPassthroughOpacity
        spatialEnvironmentImpl.firePassthroughOpacityChangedEvent()

        verify(listener).accept(opacity)

        // Ensure the listener is called exactly once, even if the event is fired after dispose.
        spatialEnvironmentImpl.dispose()
        spatialEnvironmentImpl.firePassthroughOpacityChangedEvent()

        verify(listener).accept(opacity)
    }

    @Test
    fun dispose_clearsResources() {
        onRenderingFeatureReady()
        val spatialState = ShadowSpatialState.create()
        ShadowSpatialState.extract(spatialState)
            .setEnvironmentVisibilityState(
                /* environmentVisibilityState= */ ShadowEnvironmentVisibilityState.create(
                    EnvironmentVisibilityState.APP_VISIBLE
                )
            )
        ShadowSpatialState.extract(spatialState)
            .setPassthroughVisibilityState(
                /* passthroughVisibilityState= */ ShadowPassthroughVisibilityState.create(
                    PassthroughVisibilityState.APP,
                    0.5f,
                )
            )

        spatialEnvironmentImpl.setSpatialState(spatialState)

        spatialEnvironmentImpl.preferredPassthroughOpacity = 0.5f

        assertThat(spatialEnvironmentImpl.isPreferredSpatialEnvironmentActive).isTrue()
        assertThat(spatialEnvironmentImpl.preferredPassthroughOpacity)
            .isNotEqualTo(SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE)
        assertThat(spatialEnvironmentImpl.currentPassthroughOpacity).isEqualTo(0.5f)

        spatialEnvironmentImpl.dispose()

        verify(mockSpatialEnvironmentFeature).dispose()
        assertThat(ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity)).isNull()
        assertThat(spatialEnvironmentImpl.isPreferredSpatialEnvironmentActive).isFalse()
        assertThat(spatialEnvironmentImpl.preferredPassthroughOpacity)
            .isEqualTo(SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE)
        assertThat(spatialEnvironmentImpl.currentPassthroughOpacity).isEqualTo(0.0f)
    }
}
