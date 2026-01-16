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

package androidx.xr.scenecore.spatial.core;

import static androidx.xr.scenecore.runtime.SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Activity;

import androidx.xr.scenecore.runtime.ExrImageResource;
import androidx.xr.scenecore.runtime.GltfModelResource;
import androidx.xr.scenecore.runtime.SpatialEnvironment.SpatialEnvironmentPreference;
import androidx.xr.scenecore.runtime.SpatialEnvironmentFeature;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.environment.EnvironmentVisibilityState;
import com.android.extensions.xr.environment.PassthroughVisibilityState;
import com.android.extensions.xr.environment.ShadowEnvironmentVisibilityState;
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeRepository;
import com.android.extensions.xr.space.ShadowSpatialState;
import com.android.extensions.xr.space.SpatialState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.Objects;
import java.util.function.Consumer;

// Technically this doesn't need to be a Robolectric test, since it doesn't directly depend on
// any Android subsystems. However, we're currently using an Android test runner for consistency
// with other Android XR impl tests in this directory.

/** Unit tests for the AndroidXR implementation of JXRCore's SpatialEnvironment module. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public final class SpatialEnvironmentImplTest {
    private final NodeRepository mNodeRepository = NodeRepository.getInstance();
    private final SpatialEnvironmentFeature mMockSpatialEnvironmentFeature =
            mock(SpatialEnvironmentFeature.class);
    private ActivityController<Activity> mActivityController;
    private Activity mActivity;
    private XrExtensions mXrExtensions = null;
    private SpatialEnvironmentImpl mEnvironment = null;

    @Before
    public void setUp() {
        mActivityController = Robolectric.buildActivity(Activity.class);
        mActivity = mActivityController.create().start().get();
        // Reset our state.
        mXrExtensions = XrExtensionsProvider.getXrExtensions();
        Node sceneRootNode = Objects.requireNonNull(mXrExtensions).createNode();

        mEnvironment =
                new SpatialEnvironmentImpl(
                        mActivity, mXrExtensions, sceneRootNode, this::getSpatialState);
    }

    private SpatialState getSpatialState() {
        return mXrExtensions.getSpatialState(mActivity);
    }

    private void onRenderingFeatureReady() {
        mEnvironment.onRenderingFeatureReady(mMockSpatialEnvironmentFeature);
    }

    @After
    public void tearDown() {
        mActivityController.destroy();
    }

    @Test
    public void setPreferredPassthroughOpacity() {
        mEnvironment.setPreferredPassthroughOpacity(NO_PASSTHROUGH_OPACITY_PREFERENCE);

        assertThat(mEnvironment.getPreferredPassthroughOpacity())
                .isEqualTo(NO_PASSTHROUGH_OPACITY_PREFERENCE);

        mEnvironment.setPreferredPassthroughOpacity(0.1f);

        assertThat(mNodeRepository.getPassthroughOpacity(mEnvironment.mPassthroughNode))
                .isEqualTo(0.1f);
        assertThat(mEnvironment.getPreferredPassthroughOpacity()).isEqualTo(0.1f);
    }

    @Test
    public void setPreferredPassthroughOpacityNearOrUnderZero_getsZeroOpacity() {
        // Opacity values below 1% should be treated as zero.
        mEnvironment.setPreferredPassthroughOpacity(0.009f);

        assertThat(mEnvironment.getPreferredPassthroughOpacity()).isEqualTo(0.0f);

        mEnvironment.setPreferredPassthroughOpacity(-0.1f);

        assertThat(mNodeRepository.getPassthroughOpacity(mEnvironment.mPassthroughNode))
                .isEqualTo(0.0f);
        assertThat(mEnvironment.getPreferredPassthroughOpacity()).isEqualTo(0.0f);
    }

    @Test
    public void setPreferredPassthroughOpacityNearOrOverOne_getsFullOpacity() {
        // Opacity values above 99% should be treated as full opacity.
        mEnvironment.setPreferredPassthroughOpacity(0.991f);

        assertThat(mEnvironment.getPreferredPassthroughOpacity()).isEqualTo(1.0f);

        mEnvironment.setPreferredPassthroughOpacity(1.1f);

        assertThat(mNodeRepository.getPassthroughOpacity(mEnvironment.mPassthroughNode))
                .isEqualTo(1.0f);
        assertThat(mEnvironment.getPreferredPassthroughOpacity()).isEqualTo(1.0f);
    }

    @Test
    public void getCurrentPassthroughOpacity_returnsZeroInitially() {
        assertThat(mEnvironment.getCurrentPassthroughOpacity()).isEqualTo(0.0f);
    }

    @Test
    public void onPassthroughOpacityChangedListener_firesOnPassthroughOpacityChange() {
        @SuppressWarnings(value = "unchecked")
        Consumer<Float> listener1 = (Consumer<Float>) mock(Consumer.class);
        @SuppressWarnings(value = "unchecked")
        Consumer<Float> listener2 = (Consumer<Float>) mock(Consumer.class);

        mEnvironment.addOnPassthroughOpacityChangedListener(directExecutor(), listener1);
        mEnvironment.addOnPassthroughOpacityChangedListener(directExecutor(), listener2);

        float opacity = mEnvironment.getCurrentPassthroughOpacity();
        mEnvironment.firePassthroughOpacityChangedEvent();

        verify(listener1).accept(opacity);
        verify(listener2).accept(opacity);

        mEnvironment.removeOnPassthroughOpacityChangedListener(listener1);
        mEnvironment.firePassthroughOpacityChangedEvent();

        verify(listener1).accept(opacity);
        verify(listener2, times(2)).accept(opacity);
    }

    @Test
    public void setPreferredSpatialEnv_throwsWhenRenderingFeatureNotReady() {

        assertThrows(
                UnsupportedOperationException.class,
                () ->
                        mEnvironment.setPreferredSpatialEnvironment(
                                new SpatialEnvironmentPreference(
                                        new ExrImageResource() {}, new GltfModelResource() {})));
    }

    @Test
    public void setPreferredSpatialEnv_featureReady_featureIsCalled() {
        onRenderingFeatureReady();

        mEnvironment.setPreferredSpatialEnvironment(
                new SpatialEnvironmentPreference(
                        new ExrImageResource() {}, new GltfModelResource() {}));

        verify(mMockSpatialEnvironmentFeature)
                .setPreferredSpatialEnvironment(any(SpatialEnvironmentPreference.class));
    }

    @Test
    public void isPreferredSpatialEnvironmentActive_defaultsToFalse() {
        assertThat(mEnvironment.isPreferredSpatialEnvironmentActive()).isFalse();
    }

    @Test
    public void onSpatialEnvironmentChangedListener_firesOnEnvironmentChange() {
        @SuppressWarnings(value = "unchecked")
        Consumer<Boolean> listener1 = (Consumer<Boolean>) mock(Consumer.class);
        @SuppressWarnings(value = "unchecked")
        Consumer<Boolean> listener2 = (Consumer<Boolean>) mock(Consumer.class);

        SpatialState spatialState = ShadowSpatialState.create();
        mEnvironment.setSpatialState(spatialState);

        mEnvironment.addOnSpatialEnvironmentChangedListener(directExecutor(), listener1);
        mEnvironment.addOnSpatialEnvironmentChangedListener(directExecutor(), listener2);

        boolean isPreferredSpatialEnvironmentActive =
                mEnvironment.isPreferredSpatialEnvironmentActive();

        mEnvironment.fireOnSpatialEnvironmentChangedEvent();

        verify(listener1).accept(isPreferredSpatialEnvironmentActive);
        verify(listener2).accept(isPreferredSpatialEnvironmentActive);

        mEnvironment.removeOnSpatialEnvironmentChangedListener(listener1);
        mEnvironment.fireOnSpatialEnvironmentChangedEvent();
        verify(listener1).accept(isPreferredSpatialEnvironmentActive);

        verify(listener2, times(2)).accept(isPreferredSpatialEnvironmentActive);
    }

    @Test
    public void dispose_clearsSpatialEnvironmentPreferenceListeners() {
        @SuppressWarnings(value = "unchecked")
        Consumer<Boolean> listener = (Consumer<Boolean>) mock(Consumer.class);

        SpatialState spatialState = ShadowSpatialState.create();
        mEnvironment.setSpatialState(spatialState);
        mEnvironment.addOnSpatialEnvironmentChangedListener(directExecutor(), listener);

        boolean isPreferredSpatialEnvironmentActive =
                mEnvironment.isPreferredSpatialEnvironmentActive();
        mEnvironment.fireOnSpatialEnvironmentChangedEvent();

        verify(listener).accept(isPreferredSpatialEnvironmentActive);

        mEnvironment.dispose();
        mEnvironment.fireOnSpatialEnvironmentChangedEvent();

        verify(listener).accept(isPreferredSpatialEnvironmentActive);
    }

    @Test
    public void dispose_clearsPreferredPassthroughOpacityListeners() {
        @SuppressWarnings(value = "unchecked")
        Consumer<Float> listener = (Consumer<Float>) mock(Consumer.class);
        mEnvironment.addOnPassthroughOpacityChangedListener(directExecutor(), listener);

        float opacity = mEnvironment.getCurrentPassthroughOpacity();
        mEnvironment.firePassthroughOpacityChangedEvent();

        verify(listener).accept(opacity);

        // Ensure the listener is called exactly once, even if the event is fired after dispose.
        mEnvironment.dispose();
        mEnvironment.firePassthroughOpacityChangedEvent();

        verify(listener).accept(opacity);
    }

    @Test
    public void dispose_clearsResources() {
        onRenderingFeatureReady();
        SpatialState spatialState = ShadowSpatialState.create();
        ShadowSpatialState.extract(spatialState)
                .setEnvironmentVisibilityState(
                        /* environmentVisibilityState= */ ShadowEnvironmentVisibilityState.create(
                                EnvironmentVisibilityState.APP_VISIBLE));
        ShadowSpatialState.extract(spatialState)
                .setPassthroughVisibilityState(
                        /* passthroughVisibilityState= */ ShadowPassthroughVisibilityState.create(
                                PassthroughVisibilityState.APP, 0.5f));

        mEnvironment.setSpatialState(spatialState);

        mEnvironment.setPreferredPassthroughOpacity(0.5f);

        assertThat(mEnvironment.isPreferredSpatialEnvironmentActive()).isTrue();
        assertThat(mEnvironment.getPreferredPassthroughOpacity())
                .isNotEqualTo(NO_PASSTHROUGH_OPACITY_PREFERENCE);
        assertThat(mEnvironment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);

        mEnvironment.dispose();

        verify(mMockSpatialEnvironmentFeature).dispose();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getEnvironmentNode(mActivity))
                .isNull();
        assertThat(mEnvironment.isPreferredSpatialEnvironmentActive()).isFalse();
        assertThat(mEnvironment.getPreferredPassthroughOpacity())
                .isEqualTo(NO_PASSTHROUGH_OPACITY_PREFERENCE);
        assertThat(mEnvironment.getCurrentPassthroughOpacity()).isEqualTo(0.0f);
    }
}
