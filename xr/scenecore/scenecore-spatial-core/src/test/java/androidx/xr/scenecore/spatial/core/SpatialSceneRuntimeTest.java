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

import static androidx.xr.runtime.testing.math.MathAssertions.assertPose;
import static androidx.xr.runtime.testing.math.MathAssertions.assertRotation;
import static androidx.xr.runtime.testing.math.MathAssertions.assertVector3;

import static com.android.extensions.xr.node.ReformOptions.ALLOW_MOVE;
import static com.android.extensions.xr.node.ReformOptions.ALLOW_RESIZE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import androidx.xr.runtime.NodeHolder;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.ActivitySpace;
import androidx.xr.scenecore.runtime.AnchorEntity;
import androidx.xr.scenecore.runtime.AnchorEntity.State;
import androidx.xr.scenecore.runtime.AnchorPlacement;
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper;
import androidx.xr.scenecore.runtime.Component;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.GltfEntity;
import androidx.xr.scenecore.runtime.GltfFeature;
import androidx.xr.scenecore.runtime.InputEvent;
import androidx.xr.scenecore.runtime.InputEventListener;
import androidx.xr.scenecore.runtime.InteractableComponent;
import androidx.xr.scenecore.runtime.LoggingEntity;
import androidx.xr.scenecore.runtime.MediaPlayerExtensionsWrapper;
import androidx.xr.scenecore.runtime.MovableComponent;
import androidx.xr.scenecore.runtime.PanelEntity;
import androidx.xr.scenecore.runtime.PixelDimensions;
import androidx.xr.scenecore.runtime.PlaneSemantic;
import androidx.xr.scenecore.runtime.PlaneType;
import androidx.xr.scenecore.runtime.PointerCaptureComponent;
import androidx.xr.scenecore.runtime.ResizableComponent;
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper;
import androidx.xr.scenecore.runtime.Space;
import androidx.xr.scenecore.runtime.SpatialCapabilities;
import androidx.xr.scenecore.runtime.SpatialEnvironment;
import androidx.xr.scenecore.runtime.SpatialModeChangeListener;
import androidx.xr.scenecore.runtime.SpatialPointerComponent;
import androidx.xr.scenecore.runtime.SpatialVisibility;
import androidx.xr.scenecore.runtime.SubspaceNodeEntity;
import androidx.xr.scenecore.runtime.SurfaceEntity;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeComponent;
import androidx.xr.scenecore.testing.FakeGltfFeature;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeSurfaceFeature;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.environment.EnvironmentVisibilityState;
import com.android.extensions.xr.environment.PassthroughVisibilityState;
import com.android.extensions.xr.environment.ShadowEnvironmentVisibilityState;
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState;
import com.android.extensions.xr.node.FakeCloseable;
import com.android.extensions.xr.node.InputEvent.HitInfo;
import com.android.extensions.xr.node.Mat4f;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeRepository;
import com.android.extensions.xr.node.ReformOptions;
import com.android.extensions.xr.node.ShadowInputEvent;
import com.android.extensions.xr.node.ShadowNode;
import com.android.extensions.xr.node.ShadowNodeTransform;
import com.android.extensions.xr.node.Vec3;
import com.android.extensions.xr.space.PerceivedResolution;
import com.android.extensions.xr.space.ShadowSpatialCapabilities;
import com.android.extensions.xr.space.ShadowSpatialState;
import com.android.extensions.xr.space.SpatialState;
import com.android.extensions.xr.space.VisibilityState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** Tests for {@link SpatialSceneRuntimeFactory}. */
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public class SpatialSceneRuntimeTest {
    private static final int OPEN_XR_REFERENCE_SPACE_TYPE = 1;
    private static final String GUARDIAN_CONSENT_GRANTED = "guardian_consent_granted";
    private static final String TOGGLE_GUARDIAN = "toggle_guardian";
    private static final Uri IS_EXPLICITLY_BOUNDARY_CONSENT_GRANTED_URI =
            Settings.Secure.getUriFor(GUARDIAN_CONSENT_GRANTED);
    private static final Uri IS_BOUNDARY_ENABLED_IN_DEVELOPER_OPTIONS_URI =
            Settings.System.getUriFor(TOGGLE_GUARDIAN);
    private final EntityManager mEntityManager = new EntityManager();
    private final NodeRepository mNodeRepository = NodeRepository.getInstance();
    private final @NonNull XrExtensions mXrExtensions =
            Objects.requireNonNull(XrExtensionsProvider.getXrExtensions());
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final GltfFeature mMockGltfFeature = Mockito.mock(GltfFeature.class);
    Activity mActivity;
    private SpatialSceneRuntime mRuntime;
    private ContentResolver mContentResolver;
    private ShadowContentResolver mShadowContentResolver;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(Activity.class).create().start().get();
        mShadowContentResolver = shadowOf(mActivity.getContentResolver());
        mContentResolver = mActivity.getContentResolver();
        ShadowXrExtensions.extract(mXrExtensions).setApiVersion(1);
        ShadowXrExtensions.extract(mXrExtensions)
                .setOpenXrWorldSpaceType(OPEN_XR_REFERENCE_SPACE_TYPE);
        mRuntime =
                SpatialSceneRuntime.create(
                        mActivity, mFakeExecutor, mXrExtensions, mEntityManager);
    }

    @After
    public void tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        mRuntime.destroy();
        mRuntime = null;
    }

    private GltfEntityImpl createGltfEntity() {
        NodeHolder<?> nodeHolder = new NodeHolder<>(mXrExtensions.createNode(), Node.class);
        GltfFeature fakeGltfFeature =
                FakeGltfFeature.Companion.createWithMockFeature(mMockGltfFeature, nodeHolder);

        return new GltfEntityImpl(
                mActivity,
                fakeGltfFeature,
                mRuntime.getActivitySpace(),
                mXrExtensions,
                mEntityManager,
                mFakeExecutor);
    }

    GltfEntity createGltfEntity(Pose pose) throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        gltfEntity.setPose(pose);
        return gltfEntity;
    }

    private SpatialSceneRuntime createRuntime() {
        return SpatialSceneRuntime.create(
                mActivity, mFakeExecutor, mXrExtensions, mEntityManager);
    }

    @Test
    public void sceneRuntime_setUpSceneRootAndTaskLeashNodes() {
        Node rootNode = mRuntime.getSceneRootNode();
        Node taskWindowLeashNode = mRuntime.getTaskWindowLeashNode();

        assertThat(mNodeRepository.getName(rootNode))
                .isEqualTo("SpatialSceneAndActivitySpaceRootNode");
        assertThat(mNodeRepository.getName(taskWindowLeashNode))
                .isEqualTo("MainPanelAndTaskWindowLeashNode");
        assertThat(mNodeRepository.getParent(taskWindowLeashNode)).isEqualTo(rootNode);
    }

    @Test
    public void getEnvironment_returnsEnvironment() {
        SpatialEnvironment environment = mRuntime.getSpatialEnvironment();
        assertThat(environment).isNotNull();
    }

    @Test
    public void getActivitySpace_returnsEntity() {
        ActivitySpace activitySpace = mRuntime.getActivitySpace();

        assertThat(activitySpace).isNotNull();
        // Verify that there is an underlying extension node.
        ActivitySpaceImpl activitySpaceImpl = (ActivitySpaceImpl) activitySpace;
        assertThat(activitySpaceImpl.getNode()).isNotNull();
    }

    @Test
    public void onSpatialStateChanged_setsSpatialCapabilities() {
        SpatialState spatialState = ShadowSpatialState.create();
        ShadowSpatialState.extract(spatialState)
                .setSpatialCapabilities(
                        ShadowSpatialCapabilities.create(
                                com.android.extensions.xr.space.SpatialCapabilities
                                        .SPATIAL_UI_CAPABLE));
        mRuntime.onSpatialStateChanged(spatialState);

        SpatialCapabilities caps = mRuntime.getSpatialCapabilities();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isFalse();
    }

    @Test
    public void onSpatialStateChanged_setsEnvironmentVisibility() {
        SpatialEnvironment environment = mRuntime.getSpatialEnvironment();
        assertThat(environment.isPreferredSpatialEnvironmentActive()).isFalse();

        SpatialState state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setEnvironmentVisibilityState(
                        ShadowEnvironmentVisibilityState.create(
                                EnvironmentVisibilityState.APP_VISIBLE));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        assertThat(environment.isPreferredSpatialEnvironmentActive()).isTrue();

        state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setEnvironmentVisibilityState(
                        ShadowEnvironmentVisibilityState.create(
                                EnvironmentVisibilityState.INVISIBLE));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        assertThat(environment.isPreferredSpatialEnvironmentActive()).isFalse();

        state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setEnvironmentVisibilityState(
                        ShadowEnvironmentVisibilityState.create(
                                EnvironmentVisibilityState.HOME_VISIBLE));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        assertThat(environment.isPreferredSpatialEnvironmentActive()).isFalse();
    }

    @Test
    public void onSpatialStateChanged_callsEnvironmentListenerOnlyForChanges() {
        SpatialEnvironment environment = mRuntime.getSpatialEnvironment();
        @SuppressWarnings(value = "unchecked")
        Consumer<Boolean> listener = (Consumer<Boolean>) mock(Consumer.class);

        environment.addOnSpatialEnvironmentChangedListener(directExecutor(), listener);

        assertThat(environment.isPreferredSpatialEnvironmentActive()).isFalse();

        // The first spatial state should always fire the listener
        SpatialState state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setEnvironmentVisibilityState(
                        ShadowEnvironmentVisibilityState.create(
                                EnvironmentVisibilityState.APP_VISIBLE));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        verify(listener).accept(true);

        // The second spatial state should also fire the listener since it's a different state
        state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setEnvironmentVisibilityState(
                        ShadowEnvironmentVisibilityState.create(
                                EnvironmentVisibilityState.INVISIBLE));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        assertThat(environment.isPreferredSpatialEnvironmentActive()).isFalse();
        verify(listener).accept(false);

        // The third spatial state should not fire the listener since it is the same as the last
        // state.
        state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setEnvironmentVisibilityState(
                        ShadowEnvironmentVisibilityState.create(
                                EnvironmentVisibilityState.INVISIBLE));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        assertThat(environment.isPreferredSpatialEnvironmentActive()).isFalse();
        verify(listener, times(2))
                .accept(any()); // Verify the listener was not called a third time.
    }

    @Test
    public void onSpatialStateChanged_setsPassthroughOpacity() {
        SpatialEnvironment environment = mRuntime.getSpatialEnvironment();
        assertThat(environment.getCurrentPassthroughOpacity()).isZero();

        SpatialState state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setPassthroughVisibilityState(
                        ShadowPassthroughVisibilityState.create(
                                PassthroughVisibilityState.APP, 0.4f));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.4f);

        state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setPassthroughVisibilityState(
                        ShadowPassthroughVisibilityState.create(
                                PassthroughVisibilityState.HOME, 0.5f));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);

        state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setPassthroughVisibilityState(
                        ShadowPassthroughVisibilityState.create(
                                PassthroughVisibilityState.SYSTEM, 0.9f));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.9f);

        state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setPassthroughVisibilityState(
                        ShadowPassthroughVisibilityState.create(
                                PassthroughVisibilityState.DISABLED, 0.0f));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        assertThat(environment.getCurrentPassthroughOpacity()).isZero();
    }

    @Test
    public void onSpatialStateChanged_callsPassthroughListenerOnlyForChanges() {
        SpatialEnvironment environment = mRuntime.getSpatialEnvironment();
        @SuppressWarnings(value = "unchecked")
        Consumer<Float> listener = (Consumer<Float>) mock(Consumer.class);

        environment.addOnPassthroughOpacityChangedListener(directExecutor(), listener);

        assertThat(environment.getCurrentPassthroughOpacity()).isZero();

        // The first spatial state should always fire the listener
        SpatialState state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setPassthroughVisibilityState(
                        ShadowPassthroughVisibilityState.create(
                                PassthroughVisibilityState.APP, 1.0f));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        verify(listener).accept(1.0f);

        // The second spatial state should also fire the listener even if only the opacity changes
        state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setPassthroughVisibilityState(
                        ShadowPassthroughVisibilityState.create(
                                PassthroughVisibilityState.APP, 0.5f));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);

        // The third spatial state should also fire the listener even if only the visibility state
        // changes, but getCurrentPassthroughOpacity() returns the same value as the last state.
        state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setPassthroughVisibilityState(
                        ShadowPassthroughVisibilityState.create(
                                PassthroughVisibilityState.HOME, 0.5f));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);
        verify(listener, times(2))
                .accept(0.5f); // Verify it was called a second time with this value.

        // The fourth spatial state should not fire the listener since it is the same as the last
        // state.
        state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setPassthroughVisibilityState(
                        ShadowPassthroughVisibilityState.create(
                                PassthroughVisibilityState.HOME, 0.5f));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        assertThat(environment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);
        verify(listener, times(3))
                .accept(any()); // Verify the listener was not called a fourth time.
    }

    @Test
    public void currentPassthroughOpacity_isSetDuringRuntimeCreation() {
        ShadowSpatialState.extract(mXrExtensions.getSpatialState(mActivity))
                .setPassthroughVisibilityState(
                        ShadowPassthroughVisibilityState.create(
                                PassthroughVisibilityState.APP, 0.5f));

        SpatialEnvironment newEnvironment = mRuntime.getSpatialEnvironment();
        assertThat(newEnvironment.getCurrentPassthroughOpacity()).isEqualTo(0.5f);
    }

    @Test
    public void onSpatialStateChanged_firesSpatialCapabilitiesChangedListener() {
        @SuppressWarnings(value = "unchecked")
        Consumer<SpatialCapabilities> listener1 =
                (Consumer<SpatialCapabilities>) mock(Consumer.class);
        @SuppressWarnings(value = "unchecked")
        Consumer<SpatialCapabilities> listener2 =
                (Consumer<SpatialCapabilities>) mock(Consumer.class);

        mRuntime.addSpatialCapabilitiesChangedListener(directExecutor(), listener1);
        mRuntime.addSpatialCapabilitiesChangedListener(directExecutor(), listener2);

        SpatialState state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setSpatialCapabilities(ShadowSpatialCapabilities.createAll());
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        verify(listener1).accept(any());
        verify(listener2).accept(any());

        state = ShadowSpatialState.create();
        ShadowSpatialState.extract(state)
                .setSpatialCapabilities(ShadowSpatialCapabilities.create());
        mRuntime.removeSpatialCapabilitiesChangedListener(listener1);
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, state);
        verify(listener1).accept(any()); // Verify the removed listener was called exactly once
        verify(listener2, times(2)).accept(any()); // Verify the active listener was called twice
    }

    private Node getNode(Entity entity) {
        return ((AndroidXrEntity) entity).getNode();
    }

    @Test
    public void createSubspaceNodeEntity_returnSubspaceNodeEntity() {
        Dimensions size = new Dimensions(1.0f, 2.0f, 3.0f);

        SubspaceNodeEntity entity =
                mRuntime.createSubspaceNodeEntity(mXrExtensions.createNode(), size);
        entity.setSize(size);

        assertThat(entity).isNotNull();
        assertThat(entity.getSize()).isEqualTo(size);
    }

    private PanelEntity createPanelEntity() {
        return createPanelEntity(new Pose());
    }

    /**
     * Creates a generic panel entity instance for testing by creating a dummy view to insert into
     * the panel, and setting the activity space as parent.
     */
    private PanelEntity createPanelEntity(Pose pose) {
        Display display = mActivity.getSystemService(DisplayManager.class).getDisplays()[0];
        Context displayContext = mActivity.createDisplayContext(display);
        View view = new View(displayContext);
        view.setLayoutParams(new ViewGroup.LayoutParams(640, 480));
        return mRuntime.createPanelEntity(
                displayContext,
                pose,
                view,
                new PixelDimensions(640, 480),
                "testPanel",
                mRuntime.getActivitySpace());
    }

    private Entity createGroupEntity() {
        return createGroupEntity(new Pose());
    }

    private Entity createGroupEntity(Pose pose) {
        return mRuntime.createGroupEntity(pose, "test", mRuntime.getActivitySpace());
    }

    @Test
    public void createGroupEntity_returnsEntity() throws Exception {
        assertThat(createGroupEntity()).isNotNull();
    }

    @Test
    public void groupEntity_hasActivitySpaceRootImplAsParentByDefault() throws Exception {
        Entity entity = createGroupEntity();
        assertThat(entity.getParent()).isEqualTo(mRuntime.getActivitySpace());
    }

    @Test
    public void groupEntityAddChildren_addsChildren() throws Exception {
        Entity childEntity1 = createGroupEntity();
        Entity childEntity2 = createGroupEntity();
        Entity parentEntity = createGroupEntity();

        parentEntity.addChild(childEntity1);

        assertThat(parentEntity.getChildren()).containsExactly(childEntity1);

        parentEntity.addChildren(ImmutableList.of(childEntity2));

        assertThat(childEntity1.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity2.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getChildren()).containsExactly(childEntity1, childEntity2);

        Node childNode1 = getNode(childEntity1);
        assertThat(mNodeRepository.getParent(childNode1)).isEqualTo(getNode(parentEntity));
        Node childNode2 = getNode(childEntity2);
        assertThat(mNodeRepository.getParent(childNode2)).isEqualTo(getNode(parentEntity));
    }

    @Test
    public void createLoggingEntity_returnsEntity() {
        Pose pose = new Pose();
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(pose);
        Pose updatedPose =
                new Pose(
                        new Vector3(1f, pose.getTranslation().getY(), pose.getTranslation().getZ()),
                        pose.getRotation());
        loggingEntity.setPose(updatedPose);
    }

    @Test
    public void loggingEntitySetParent() {
        Pose pose = new Pose();
        LoggingEntity childEntity = mRuntime.createLoggingEntity(pose);
        LoggingEntity parentEntity = mRuntime.createLoggingEntity(pose);

        childEntity.setParent(parentEntity);
        parentEntity.addChild(childEntity);

        assertThat(childEntity.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getParent()).isEqualTo(null);
        assertThat(childEntity.getChildren()).isEmpty();
        assertThat(parentEntity.getChildren()).containsExactly(childEntity);
    }

    @Test
    public void loggingEntityUpdateParent() {
        Pose pose = new Pose();
        LoggingEntity childEntity = mRuntime.createLoggingEntity(pose);
        LoggingEntity parentEntity1 = mRuntime.createLoggingEntity(pose);
        LoggingEntity parentEntity2 = mRuntime.createLoggingEntity(pose);

        childEntity.setParent(parentEntity1);

        assertThat(childEntity.getParent()).isEqualTo(parentEntity1);
        assertThat(parentEntity1.getChildren()).containsExactly(childEntity);
        assertThat(parentEntity2.getChildren()).isEmpty();

        childEntity.setParent(parentEntity2);

        assertThat(childEntity.getParent()).isEqualTo(parentEntity2);
        assertThat(parentEntity2.getChildren()).containsExactly(childEntity);
        assertThat(parentEntity1.getChildren()).isEmpty();
    }

    @Test
    public void loggingEntity_getActivitySpacePose_returnsIdentityPose() {
        Pose identityPose = new Pose();
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(identityPose);
        assertPose(loggingEntity.getActivitySpacePose(), identityPose);
    }

    @Test
    public void loggingEntity_transformPoseTo_returnsIdentityPose() {
        Pose identityPose = new Pose();
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(identityPose);
        assertPose(loggingEntity.transformPoseTo(identityPose, loggingEntity), identityPose);
    }

    @Test
    public void loggingEntityAddChildren() {
        Pose pose = new Pose();
        LoggingEntity childEntity1 = mRuntime.createLoggingEntity(pose);
        LoggingEntity childEntity2 = mRuntime.createLoggingEntity(pose);
        LoggingEntity parentEntity = mRuntime.createLoggingEntity(pose);

        parentEntity.addChild(childEntity1);

        assertThat(parentEntity.getChildren()).containsExactly(childEntity1);

        parentEntity.addChildren(ImmutableList.of(childEntity2));

        assertThat(childEntity1.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity2.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getChildren()).containsExactly(childEntity1, childEntity2);
    }

    @Test
    public void createAnchorEntity_returnsUnanchoredAnchorEntity() {
        AnchorEntity anchorEntity = mRuntime.createAnchorEntity();

        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
    }

    @Test
    public void spatialStateChangeHandler_invokedWhenSpatialStateChangesToFSM() {
        SpatialState spatialState = ShadowSpatialState.create();
        SpatialModeChangeListener mockSpatialModeChangeListener =
                mock(SpatialModeChangeListener.class);
        mRuntime.setSpatialModeChangeListener(mockSpatialModeChangeListener);
        ShadowSpatialState.extract(spatialState)
                .setSpatialCapabilities(ShadowSpatialCapabilities.createAll());
        ShadowSpatialState.extract(spatialState).setSceneParentTransform(new Mat4f(new float[16]));
        mRuntime.onSpatialStateChanged(spatialState);

        verify(mockSpatialModeChangeListener).onSpatialModeChanged(any(), any());
    }

    private void sendVisibilityState(ShadowXrExtensions shadowXrExtensions, int visibilityState) {
        sendVisibilityState(shadowXrExtensions, visibilityState, 1, 1);
    }

    private void sendVisibilityState(ShadowXrExtensions shadowXrExtensions, int width, int height) {
        sendVisibilityState(shadowXrExtensions, VisibilityState.FULLY_VISIBLE, width, height);
    }

    private void sendVisibilityState(
            ShadowXrExtensions shadowXrExtensions, int visibilityState, int width, int height) {
        shadowXrExtensions.sendVisibilityState(
                mActivity,
                new VisibilityState(visibilityState, new PerceivedResolution(width, height)));
    }

    @Test
    public void setSpatialVisibilityChangedListener_callsExtensions() {
        @SuppressWarnings(value = "unchecked")
        Consumer<SpatialVisibility> mockListener =
                (Consumer<SpatialVisibility>) mock(Consumer.class);
        mRuntime.setSpatialVisibilityChangedListener(directExecutor(), mockListener);
        ShadowXrExtensions shadowXrExtensions = ShadowXrExtensions.extract(mXrExtensions);

        // VISIBLE
        sendVisibilityState(shadowXrExtensions, VisibilityState.FULLY_VISIBLE);
        verify(mockListener).accept(new SpatialVisibility(SpatialVisibility.WITHIN_FOV));

        // PARTIALLY_VISIBLE
        sendVisibilityState(shadowXrExtensions, VisibilityState.PARTIALLY_VISIBLE);
        verify(mockListener).accept(new SpatialVisibility(SpatialVisibility.PARTIALLY_WITHIN_FOV));

        // OUTSIDE_OF_FOV
        sendVisibilityState(shadowXrExtensions, VisibilityState.NOT_VISIBLE);
        verify(mockListener).accept(new SpatialVisibility(SpatialVisibility.OUTSIDE_FOV));

        // UNKNOWN
        sendVisibilityState(shadowXrExtensions, VisibilityState.UNKNOWN);
        verify(mockListener).accept(new SpatialVisibility(SpatialVisibility.UNKNOWN));
    }

    @Test
    public void setSpatialVisibilityChangedListener_replacesExistingListenerOnSecondCall() {
        @SuppressWarnings(value = "unchecked")
        Consumer<SpatialVisibility> mockListener1 =
                (Consumer<SpatialVisibility>) mock(Consumer.class);
        @SuppressWarnings(value = "unchecked")
        Consumer<SpatialVisibility> mockListener2 =
                (Consumer<SpatialVisibility>) mock(Consumer.class);
        ShadowXrExtensions shadowXrExtensions = ShadowXrExtensions.extract(mXrExtensions);

        // Listener 1 is set and called once.
        mRuntime.setSpatialVisibilityChangedListener(directExecutor(), mockListener1);
        sendVisibilityState(shadowXrExtensions, VisibilityState.FULLY_VISIBLE);
        verify(mockListener1).accept(new SpatialVisibility(SpatialVisibility.WITHIN_FOV));
        verify(mockListener2, never()).accept(new SpatialVisibility(SpatialVisibility.WITHIN_FOV));

        // Listener 2 is set and called once. Listener 1 is not called again.
        mRuntime.setSpatialVisibilityChangedListener(directExecutor(), mockListener2);
        sendVisibilityState(shadowXrExtensions, VisibilityState.NOT_VISIBLE);
        verify(mockListener2).accept(new SpatialVisibility(SpatialVisibility.OUTSIDE_FOV));
        verify(mockListener1, never()).accept(new SpatialVisibility(SpatialVisibility.OUTSIDE_FOV));
    }

    @Test
    public void setSpatialVisibilityChangedListener_handlesException() {
        // the subscription method throws an exception if the executor or listener are null.
        // No assert needed, the test will fail if the exception is not handled.
        mRuntime.setSpatialVisibilityChangedListener(null, null);
    }

    @Test
    public void clearSpatialVisibilityChangedListener_stopsSpatialVisibilityCallbacks() {
        @SuppressWarnings(value = "unchecked")
        Consumer<SpatialVisibility> mockListener =
                (Consumer<SpatialVisibility>) mock(Consumer.class);
        mRuntime.setSpatialVisibilityChangedListener(directExecutor(), mockListener);

        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isTrue();

        // Verify that the callback is called once when the visibility changes.
        ShadowXrExtensions shadowXrExtensions = ShadowXrExtensions.extract(mXrExtensions);
        sendVisibilityState(shadowXrExtensions, VisibilityState.FULLY_VISIBLE);
        verify(mockListener).accept(any());

        // Clear the listener and verify that the callback is not called a second time.
        mRuntime.clearSpatialVisibilityChangedListener();
        sendVisibilityState(shadowXrExtensions, VisibilityState.NOT_VISIBLE);
        sendVisibilityState(shadowXrExtensions, VisibilityState.PARTIALLY_VISIBLE);
        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isFalse();
        verify(mockListener).accept(any());
    }

    @Test
    public void
            clearSpatialVisibilityChangedListener_handlesThrowWhenCalledWithoutSettingListener() {
        // No assert needed, the test will fail if an unhandled exception is thrown.
        mRuntime.clearSpatialVisibilityChangedListener();
    }

    @Test
    public void destroy_closesSpatialVisibilityAndPerceivedResolutionSubscription() {
        @SuppressWarnings(value = "unchecked")
        Consumer<SpatialVisibility> mockSpatialVisListener =
                (Consumer<SpatialVisibility>) mock(Consumer.class);
        @SuppressWarnings(value = "unchecked")
        Consumer<PixelDimensions> mockPerceivedResListener =
                (Consumer<PixelDimensions>) mock(Consumer.class);

        mRuntime.setSpatialVisibilityChangedListener(directExecutor(), mockSpatialVisListener);
        mRuntime.addPerceivedResolutionChangedListener(directExecutor(), mockPerceivedResListener);

        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isTrue();

        // Verify that the callback is called once when the visibility changes.
        ShadowXrExtensions shadowXrExtensions = ShadowXrExtensions.extract(mXrExtensions);
        sendVisibilityState(shadowXrExtensions, VisibilityState.FULLY_VISIBLE);

        verify(mockSpatialVisListener).accept(any());
        verify(mockPerceivedResListener).accept(any());

        // Ensure destroy() clears the listener that the callbacks are not called a second time.
        mRuntime.destroy();

        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isFalse();

        sendVisibilityState(shadowXrExtensions, VisibilityState.NOT_VISIBLE);
        sendVisibilityState(shadowXrExtensions, VisibilityState.PARTIALLY_VISIBLE);

        verify(mockSpatialVisListener).accept(any());
        verify(mockPerceivedResListener).accept(any());
    }

    @Test
    public void clearSpatialVisibilityChangedListener_doesNotStopPerceivedResolutionListener() {
        @SuppressWarnings("unchecked")
        Consumer<SpatialVisibility> mockSpatialListener =
                (Consumer<SpatialVisibility>) mock(Consumer.class);
        @SuppressWarnings("unchecked")
        Consumer<PixelDimensions> mockPerceivedResListener =
                (Consumer<PixelDimensions>) mock(Consumer.class);

        mRuntime.setSpatialVisibilityChangedListener(directExecutor(), mockSpatialListener);
        mRuntime.addPerceivedResolutionChangedListener(directExecutor(), mockPerceivedResListener);

        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isTrue();

        mRuntime.clearSpatialVisibilityChangedListener();

        // Perceived resolution listener is still active, so callback should remain registered.
        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isTrue();

        ShadowXrExtensions shadowXrExtensions = ShadowXrExtensions.extract(mXrExtensions);
        sendVisibilityState(shadowXrExtensions, SpatialVisibility.WITHIN_FOV, 10, 20);

        verify(mockSpatialListener, never()).accept(any());
        verify(mockPerceivedResListener).accept(any());
    }

    @Test
    public void addPerceivedResolutionChangedListener_registersCombinedCallbackFirstTime() {
        @SuppressWarnings("unchecked")
        Consumer<PixelDimensions> mockListener = (Consumer<PixelDimensions>) mock(Consumer.class);
        ShadowXrExtensions shadowXrExtensions = ShadowXrExtensions.extract(mXrExtensions);

        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isFalse();

        mRuntime.addPerceivedResolutionChangedListener(directExecutor(), mockListener);

        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isTrue();

        verify(mockListener, never()).accept(any());

        sendVisibilityState(shadowXrExtensions, 10, 20);

        verify(mockListener).accept(new PixelDimensions(10, 20));
    }

    @Test
    public void removePerceivedResolutionChangedListener_clearsCombinedCallbackIfLastListener() {
        @SuppressWarnings("unchecked")
        Consumer<PixelDimensions> mockListener = (Consumer<PixelDimensions>) mock(Consumer.class);
        ShadowXrExtensions shadowXrExtensions = ShadowXrExtensions.extract(mXrExtensions);

        mRuntime.addPerceivedResolutionChangedListener(directExecutor(), mockListener);

        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isTrue();

        sendVisibilityState(shadowXrExtensions, 10, 20);

        verify(mockListener).accept(new PixelDimensions(10, 20));

        mRuntime.removePerceivedResolutionChangedListener(mockListener);

        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isFalse();

        // It shouldn't be called a second time
        sendVisibilityState(shadowXrExtensions, 10, 20);

        verify(mockListener, times(1)).accept(new PixelDimensions(10, 20));
    }

    @Test
    public void removePerceivedResolutionChangedListener_doesNotStopSpatialListener() {
        ShadowXrExtensions shadowXrExtensions = ShadowXrExtensions.extract(mXrExtensions);
        @SuppressWarnings("unchecked")
        Consumer<SpatialVisibility> mockSpatialListener =
                (Consumer<SpatialVisibility>) mock(Consumer.class);
        @SuppressWarnings("unchecked")
        Consumer<PixelDimensions> mockPerceivedResListener =
                (Consumer<PixelDimensions>) mock(Consumer.class);

        mRuntime.setSpatialVisibilityChangedListener(directExecutor(), mockSpatialListener);
        mRuntime.addPerceivedResolutionChangedListener(directExecutor(), mockPerceivedResListener);

        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isTrue();

        mRuntime.removePerceivedResolutionChangedListener(mockPerceivedResListener);

        // Spatial listener still active, so callback should remain registered.
        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isTrue();

        sendVisibilityState(shadowXrExtensions, SpatialVisibility.WITHIN_FOV, 10, 20);

        verify(mockSpatialListener).accept(any());
        verify(mockPerceivedResListener, never()).accept(any());
    }

    @Test
    public void removePerceivedResolutionChangedListener_doesNotStopAnotherPerceivedResListener() {
        ShadowXrExtensions shadowXrExtensions = ShadowXrExtensions.extract(mXrExtensions);
        @SuppressWarnings("unchecked")
        Consumer<PixelDimensions> mockListener1 = (Consumer<PixelDimensions>) mock(Consumer.class);
        @SuppressWarnings("unchecked")
        Consumer<PixelDimensions> mockListener2 = (Consumer<PixelDimensions>) mock(Consumer.class);

        mRuntime.addPerceivedResolutionChangedListener(directExecutor(), mockListener1);
        mRuntime.addPerceivedResolutionChangedListener(directExecutor(), mockListener2);

        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isTrue();

        mRuntime.removePerceivedResolutionChangedListener(mockListener1);

        // mockListener2 still active, so callback should remain registered.
        assertThat(mRuntime.mIsExtensionVisibilityStateCallbackRegistered).isTrue();

        sendVisibilityState(shadowXrExtensions, 10, 20);

        verify(mockListener2).accept(any());
        verify(mockListener1, never()).accept(any());
    }

    @Test
    public void combinedCallback_dispatchesToBothListenersCorrectly() {
        @SuppressWarnings("unchecked")
        Consumer<SpatialVisibility> mockSpatialListener =
                (Consumer<SpatialVisibility>) mock(Consumer.class);
        @SuppressWarnings("unchecked")
        Consumer<PixelDimensions> mockPerceivedResListener =
                (Consumer<PixelDimensions>) mock(Consumer.class);

        mRuntime.setSpatialVisibilityChangedListener(directExecutor(), mockSpatialListener);
        mRuntime.addPerceivedResolutionChangedListener(directExecutor(), mockPerceivedResListener);

        ShadowXrExtensions shadowXrExtensions = ShadowXrExtensions.extract(mXrExtensions);
        sendVisibilityState(shadowXrExtensions, SpatialVisibility.OUTSIDE_FOV, 30, 40);

        verify(mockSpatialListener)
                .accept(eq(new SpatialVisibility(SpatialVisibility.OUTSIDE_FOV)));
        verify(mockPerceivedResListener).accept(eq(new PixelDimensions(30, 40)));
    }

    @Test
    public void requestHomeSpaceMode_callsExtensions() {
        mRuntime.requestHomeSpaceMode();

        assertThat(ShadowXrExtensions.extract(mXrExtensions).getSpaceMode(mActivity))
                .isEqualTo(ShadowXrExtensions.SpaceMode.HOME_SPACE);
    }

    @Test
    public void requestFullSpaceMode_callsExtensions() {
        mRuntime.requestFullSpaceMode();

        assertThat(ShadowXrExtensions.extract(mXrExtensions).getSpaceMode(mActivity))
                .isEqualTo(ShadowXrExtensions.SpaceMode.FULL_SPACE);
    }

    @Test
    public void setFullSpaceMode_callsExtensions() {
        Bundle bundle = Bundle.EMPTY;
        bundle = mRuntime.setFullSpaceMode(bundle);

        // TODO: b/440191514 - Change to assertThat(bundle).isNotEqualTo(Bundle.EMPTY);
        assertThat(bundle).isNotNull();
    }

    @Test
    public void setFullSpaceModeWithEnvironmentInherited_callsExtensions() {
        Bundle bundle = Bundle.EMPTY;
        bundle = mRuntime.setFullSpaceModeWithEnvironmentInherited(bundle);

        // TODO: b/440191514 - Change to assertThat(bundle).isNotEqualTo(Bundle.EMPTY);
        assertThat(bundle).isNotNull();
    }

    @Test
    public void enablePanelDepthTest_callsExtensions() {
        Node rootNode = mRuntime.getSceneRootNode();

        mRuntime.enablePanelDepthTest(true);

        assertThat(mNodeRepository.isEnablePanelDepthTest(rootNode)).isTrue();

        mRuntime.enablePanelDepthTest(false);

        assertThat(mNodeRepository.isEnablePanelDepthTest(rootNode)).isFalse();
    }

    @Test
    public void setPreferredAspectRatio_callsExtensions() {
        mRuntime.setPreferredAspectRatio(mActivity, 1.23f);
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getPreferredAspectRatio(mActivity))
                .isEqualTo(1.23f);
    }

    @Test
    public void sceneRuntime_getSoundPoolExtensionsWrapper() {
        SoundPoolExtensionsWrapper extensions = mRuntime.getSoundPoolExtensionsWrapper();

        assertThat(extensions).isNotNull();
    }

    @Test
    public void sceneRuntime_getAudioTrackExtensionsWrapper() {
        AudioTrackExtensionsWrapper extensions = mRuntime.getAudioTrackExtensionsWrapper();

        assertThat(extensions).isNotNull();
    }

    @Test
    public void sceneRuntime_getMediaPlayerExtensionsWrapper() {
        MediaPlayerExtensionsWrapper extensions = mRuntime.getMediaPlayerExtensionsWrapper();

        assertThat(extensions).isNotNull();
    }

    @Test
    public void createAnchorPlacement_returnsAnchorPlacement() {
        AnchorPlacement anchorPlacement =
                mRuntime.createAnchorPlacementForPlanes(
                        ImmutableSet.of(PlaneType.ANY), ImmutableSet.of(PlaneSemantic.ANY));

        assertThat(anchorPlacement).isNotNull();
    }

    @Test
    public void createMovableComponent_returnsComponent() {
        MovableComponent movableComponent = mRuntime.createMovableComponent(true, true, false);

        assertThat(movableComponent).isNotNull();
    }

    @Test
    public void createResizableComponent_returnsComponent() {
        ResizableComponent resizableComponent =
                mRuntime.createResizableComponent(
                        new Dimensions(0f, 0f, 0f), new Dimensions(5f, 5f, 5f));

        assertThat(resizableComponent).isNotNull();
    }

    @Test
    public void createPointerCaptureComponent_returnsComponent() {
        PointerCaptureComponent pointerCaptureComponent =
                mRuntime.createPointerCaptureComponent(
                        mFakeExecutor, (inputEvent) -> {}, (state) -> {});

        assertThat(pointerCaptureComponent).isNotNull();
    }

    @Test
    public void createSpatialPointerComponent_returnsComponent() {
        SpatialPointerComponent pointerComponent = mRuntime.createSpatialPointerComponent();

        assertThat(pointerComponent).isNotNull();
    }

    @Test
    public void createSurfaceEntity_returnsSurfaceEntity() {
        NodeHolder<?> nodeHolder = new NodeHolder<>(mXrExtensions.createNode(), Node.class);
        SurfaceEntity surfaceEntity =
                mRuntime.createSurfaceEntity(
                        new FakeSurfaceFeature(nodeHolder),
                        new Pose(),
                        mRuntime.getActivitySpace());

        assertThat(surfaceEntity).isNotNull();
        assertThat(surfaceEntity).isInstanceOf(SurfaceEntityImpl.class);
    }

    @Test
    public void createGltfEntity_returnsEntity() {
        assertThat(createGltfEntity()).isNotNull();
    }

    @Test
    public void animateGltfEntity_gltfEntityIsAnimating() {
        GltfEntity gltfEntity = createGltfEntity();
        gltfEntity.startAnimation(false, "animation_name");

        verify(mMockGltfFeature).startAnimation(false, "animation_name", mFakeExecutor);
    }

    @Test
    public void animateLoopGltfEntity_gltfEntityIsAnimatingInLoop() {
        GltfEntity gltfEntity = createGltfEntity();
        gltfEntity.startAnimation(true, "animation_name");

        verify(mMockGltfFeature).startAnimation(true, "animation_name", mFakeExecutor);
    }

    @Test
    public void stopAnimateGltfEntity_gltfEntityStopsAnimating() {
        GltfEntity gltfEntity = createGltfEntity();
        gltfEntity.startAnimation(true, "animation_name");
        gltfEntity.stopAnimation();

        verify(mMockGltfFeature).stopAnimation();
    }

    @Test
    public void gltfEntitySetParent() {
        GltfEntityImpl childEntity = createGltfEntity();
        GltfEntityImpl parentEntity = createGltfEntity();

        childEntity.setParent(parentEntity);

        assertThat(childEntity.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getParent()).isEqualTo(mRuntime.getActivitySpace());
        assertThat(childEntity.getChildren()).isEmpty();
        assertThat(parentEntity.getChildren()).containsExactly(childEntity);

        // Verify that there is an underlying extension node relationship.
        Node childNode = childEntity.getNode();

        assertThat(mNodeRepository.getParent(childNode)).isEqualTo(parentEntity.getNode());
    }

    @Test
    public void gltfEntityUpdateParent() {
        GltfEntityImpl childEntity = createGltfEntity();
        GltfEntityImpl parentEntity1 = createGltfEntity();
        GltfEntityImpl parentEntity2 = createGltfEntity();

        childEntity.setParent(parentEntity1);

        assertThat(childEntity.getParent()).isEqualTo(parentEntity1);
        assertThat(parentEntity1.getChildren()).containsExactly(childEntity);
        assertThat(parentEntity2.getChildren()).isEmpty();

        Node childNode = childEntity.getNode();

        assertThat(mNodeRepository.getParent(childNode)).isEqualTo(parentEntity1.getNode());

        childEntity.setParent(parentEntity2);

        assertThat(childEntity.getParent()).isEqualTo(parentEntity2);
        assertThat(parentEntity2.getChildren()).containsExactly(childEntity);
        assertThat(parentEntity1.getChildren()).isEmpty();
        assertThat(mNodeRepository.getParent(childNode)).isEqualTo(parentEntity2.getNode());
    }

    @Test
    public void gltfEntityAddChildren() {
        GltfEntityImpl childEntity1 = createGltfEntity();
        GltfEntityImpl childEntity2 = createGltfEntity();
        GltfEntityImpl parentEntity = createGltfEntity();

        parentEntity.addChild(childEntity1);

        assertThat(parentEntity.getChildren()).containsExactly(childEntity1);

        parentEntity.addChildren(ImmutableList.of(childEntity2));

        assertThat(childEntity1.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity2.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getChildren()).containsExactly(childEntity1, childEntity2);

        Node childNode1 = childEntity1.getNode();

        assertThat(mNodeRepository.getParent(childNode1)).isEqualTo(parentEntity.getNode());

        Node childNode2 = childEntity2.getNode();

        assertThat(mNodeRepository.getParent(childNode2)).isEqualTo(parentEntity.getNode());
    }

    @Test
    public void createPanelEntity_returnsEntity() {
        assertThat(createPanelEntity()).isNotNull();
    }

    @Test
    public void allPanelEntities_haveActivitySpaceRootImplAsParentByDefault() {
        PanelEntity panelEntity = createPanelEntity();

        assertThat(panelEntity.getParent()).isEqualTo(mRuntime.getActivitySpace());
    }

    @Test
    public void panelEntitySetParent_setsParent() {
        PanelEntity childEntity = createPanelEntity();
        PanelEntity parentEntity = createPanelEntity();

        childEntity.setParent(parentEntity);

        assertThat(childEntity.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity.getChildren()).isEmpty();
        assertThat(parentEntity.getChildren()).containsExactly(childEntity);

        // Verify that there is an underlying extension node relationship.
        Node childNode = getNode(childEntity);

        assertThat(mNodeRepository.getParent(childNode)).isEqualTo(getNode(parentEntity));
    }

    @Test
    public void panelEntityUpdateParent_updatesParent() {
        PanelEntity childEntity = createPanelEntity();
        PanelEntity parentEntity1 = createPanelEntity();
        PanelEntity parentEntity2 = createPanelEntity();

        childEntity.setParent(parentEntity1);

        assertThat(childEntity.getParent()).isEqualTo(parentEntity1);
        assertThat(parentEntity1.getChildren()).containsExactly(childEntity);
        assertThat(parentEntity2.getChildren()).isEmpty();

        Node childNode = getNode(childEntity);

        assertThat(mNodeRepository.getParent(childNode)).isEqualTo(getNode(parentEntity1));

        childEntity.setParent(parentEntity2);

        assertThat(childEntity.getParent()).isEqualTo(parentEntity2);
        assertThat(parentEntity2.getChildren()).containsExactly(childEntity);
        assertThat(parentEntity1.getChildren()).isEmpty();
        assertThat(mNodeRepository.getParent(childNode)).isEqualTo(getNode(parentEntity2));
    }

    @Test
    public void panelEntityAddChildren_addsChildren() {
        PanelEntity childEntity1 = createPanelEntity();
        PanelEntity childEntity2 = createPanelEntity();
        PanelEntity parentEntity = createPanelEntity();

        parentEntity.addChild(childEntity1);

        assertThat(parentEntity.getChildren()).containsExactly(childEntity1);

        parentEntity.addChildren(ImmutableList.of(childEntity2));

        assertThat(childEntity1.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity2.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getChildren()).containsExactly(childEntity1, childEntity2);

        Node childNode1 = getNode(childEntity1);

        assertThat(mNodeRepository.getParent(childNode1)).isEqualTo(getNode(parentEntity));

        Node childNode2 = getNode(childEntity2);

        assertThat(mNodeRepository.getParent(childNode2)).isEqualTo(getNode(parentEntity));
    }

    @Test
    public void getMainPanelEntity_returnsPanelEntity() {
        assertThat(mRuntime.getMainPanelEntity()).isNotNull();
    }

    @Test
    public void getMainPanelEntity_usesWindowLeashNode() {
        PanelEntity mainPanel = mRuntime.getMainPanelEntity();

        assertThat(((MainPanelEntityImpl) mainPanel).getNode())
                .isEqualTo(ShadowXrExtensions.extract(mXrExtensions).getMainWindowNode(mActivity));
    }

    @Test
    public void getPose_returnsSetPose() {
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), new Quaternion(1f, 2f, 3f, 4f));
        Pose identityPose = new Pose();
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(identityPose);
        Entity groupEntity = createGroupEntity();

        assertPose(panelEntity.getPose(), identityPose);
        assertPose(gltfEntity.getPose(), identityPose);
        assertPose(loggingEntity.getPose(), identityPose);
        assertPose(groupEntity.getPose(), identityPose);

        panelEntity.setPose(pose);
        gltfEntity.setPose(pose);
        loggingEntity.setPose(pose);
        groupEntity.setPose(pose);

        assertPose(panelEntity.getPose(), pose);
        assertPose(gltfEntity.getPose(), pose);
        assertPose(loggingEntity.getPose(), pose);
        assertPose(groupEntity.getPose(), pose);
    }

    @Test
    public void getPose_returnsFactoryMethodPose() throws Exception {
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), new Quaternion(1f, 2f, 3f, 4f));
        PanelEntity panelEntity = createPanelEntity(pose);
        GltfEntity gltfEntity = createGltfEntity(pose);
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(pose);
        Entity groupEntity = createGroupEntity(pose);

        assertPose(panelEntity.getPose(), pose);
        assertPose(gltfEntity.getPose(), pose);
        assertPose(loggingEntity.getPose(), pose);
        assertPose(groupEntity.getPose(), pose);
    }

    @Test
    public void getPoseInActivitySpace_withParentChainTranslation_returnsOffsetPositionFromRoot() {
        // Create a simple pose with only a small translation on all axes
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);

        // Set the activity space as the root of this entity hierarchy..
        AndroidXrEntity parentEntity =
                (AndroidXrEntity)
                        mRuntime.createGroupEntity(pose, "parent", mRuntime.getActivitySpace());
        AndroidXrEntity childEntity1 =
                (AndroidXrEntity) mRuntime.createGroupEntity(pose, "child1", parentEntity);
        AndroidXrEntity childEntity2 =
                (AndroidXrEntity) mRuntime.createGroupEntity(pose, "child2", childEntity1);

        assertVector3(
                parentEntity.getPoseInActivitySpace().getTranslation(), new Vector3(1f, 2f, 3f));
        assertVector3(
                childEntity1.getPoseInActivitySpace().getTranslation(), new Vector3(2f, 4f, 6f));
        assertVector3(
                childEntity2.getPoseInActivitySpace().getTranslation(), new Vector3(3f, 6f, 9f));
    }

    @Test
    public void getPoseInActivitySpace_withParentChainRotation_returnsOffsetRotationFromRoot() {
        // Create a pose with a translation and one with 90 degree rotation around the y axis.
        Vector3 parentTranslation = new Vector3(1f, 2f, 3f);
        Pose translatedPose = new Pose(parentTranslation, Quaternion.Identity);
        Quaternion quaternion = Quaternion.fromAxisAngle(new Vector3(0f, 1f, 0f), 90f);
        Pose rotatedPose = new Pose(new Vector3(0f, 0f, 0f), quaternion);

        // The parent has a translation and no rotation.
        AndroidXrEntity parentEntity =
                (AndroidXrEntity)
                        mRuntime.createGroupEntity(
                                translatedPose, "parent", mRuntime.getActivitySpace());

        // Each child adds a rotation, but no translation.
        AndroidXrEntity childEntity1 =
                (AndroidXrEntity) mRuntime.createGroupEntity(rotatedPose, "child1", parentEntity);
        AndroidXrEntity childEntity2 =
                (AndroidXrEntity) mRuntime.createGroupEntity(rotatedPose, "child2", childEntity1);

        // There should be no translation offset from the root, only changes in rotation.
        assertPose(parentEntity.getPoseInActivitySpace(), translatedPose);
        assertPose(childEntity1.getPoseInActivitySpace(), new Pose(parentTranslation, quaternion));
        assertPose(
                childEntity2.getPoseInActivitySpace(),
                new Pose(
                        parentTranslation,
                        Quaternion.fromAxisAngle(new Vector3(0f, 1f, 0f), 180f)));
    }

    @Test
    public void getPoseInActivitySpace_withParentChainPoseOffsets_returnsOffsetPoseFromRoot() {
        // Create a pose with a 1D translation and a 90 degree rotation around the z axis.
        Vector3 parentTranslation = new Vector3(1f, 0f, 0f);
        Quaternion quaternion = Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 90f);
        Pose pose = new Pose(parentTranslation, quaternion);

        // Each entity adds a translation and a rotation.
        AndroidXrEntity parentEntity =
                (AndroidXrEntity)
                        mRuntime.createGroupEntity(pose, "parent", mRuntime.getActivitySpace());
        AndroidXrEntity childEntity1 =
                (AndroidXrEntity) mRuntime.createGroupEntity(pose, "child1", parentEntity);
        AndroidXrEntity childEntity2 =
                (AndroidXrEntity) mRuntime.createGroupEntity(pose, "child2", childEntity1);

        // Local pose of ActivitySpace's direct child must be the same as child's ActivitySpace
        // pose.
        assertPose(parentEntity.getPoseInActivitySpace(), parentEntity.getPose());

        // Each child should be positioned one unit away at 90 degrees from its parent's position.
        // Since our coordinate system is right-handed, a +ve rotation around the z axis is a
        // counter-clockwise rotation of the XY plane.
        // First child should be 1 unit in the ActivitySpace's positive y direction from its parent
        assertPose(
                childEntity1.getPoseInActivitySpace(),
                new Pose(
                        new Vector3(1f, 1f, 0f),
                        Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 180f)));
        // Second child should be 1 unit in the ActivitySpace's negative x direction from its parent
        assertPose(
                childEntity2.getPoseInActivitySpace(),
                new Pose(
                        new Vector3(0f, 1f, 0f),
                        Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 270f)));
    }

    @Test
    public void getPoseInActivitySpace_withScaledActivitySpaceParent_returnsPose()
            throws Exception {
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), new Quaternion(1f, 2f, 3f, 4f));

        // Set the parent as the activity space so these entities' activitySpacePose should match
        // their
        // local pose relative to their parent regardless of the activity space
        // scale/position/rotation.
        PanelEntityImpl panelEntity = (PanelEntityImpl) createPanelEntity(pose);
        GltfEntityImpl gltfEntity = (GltfEntityImpl) createGltfEntity(pose);
        AndroidXrEntity groupEntity = (AndroidXrEntity) createGroupEntity(pose);
        ActivitySpace activitySpace = mRuntime.getActivitySpace();
        ((ActivitySpaceImpl) activitySpace)
                .setOpenXrReferenceSpaceTransform(
                        Matrix4.fromTrs(
                                new Vector3(5f, 6f, 7f),
                                Quaternion.fromEulerAngles(22f, 33f, 44f),
                                new Vector3(2f, 2f, 2f)));
        panelEntity.setParent(activitySpace);
        gltfEntity.setParent(activitySpace);
        groupEntity.setParent(activitySpace);

        assertPose(panelEntity.getPoseInActivitySpace(), pose);
        assertPose(gltfEntity.getPoseInActivitySpace(), pose);
        assertPose(groupEntity.getPoseInActivitySpace(), pose);
    }

    @Test
    public void getPoseInActivitySpace_withScale_returnsPose() throws Exception {
        Pose localPose = new Pose(new Vector3(1f, 2f, 1f), Quaternion.Identity);

        // Create a hierarchy of entities each translated from their parent by (1,2,1) in parent
        // space.
        GltfEntityImpl child1 = (GltfEntityImpl) createGltfEntity(localPose);
        GltfEntityImpl child2 = (GltfEntityImpl) createGltfEntity(localPose);
        GltfEntityImpl child3 = (GltfEntityImpl) createGltfEntity(localPose);
        ActivitySpace activitySpace = mRuntime.getActivitySpace();
        ((ActivitySpaceImpl) activitySpace)
                .setOpenXrReferenceSpaceTransform(
                        Matrix4.fromTrs(
                                new Vector3(5f, 6f, 7f),
                                Quaternion.fromEulerAngles(22f, 33, 44),
                                new Vector3(2f, 2f, 2f)));

        // Set a non-unit local scale to each child.
        child1.setParent(activitySpace);
        child1.setScale(new Vector3(2f, 2f, 2f));

        child2.setParent(child1);
        child2.setScale(new Vector3(3f, 2f, 3f));

        child3.setParent(child2);
        child3.setScale(new Vector3(1f, 1f, 2f));

        // The position (in ActivitySpace) should be:
        // child's local position * parent's scale in AS + parent's position since there's no
        // rotation.

        // Assuming c1 = child1, c2 = child2, c3 = child3, AS = activitySpace.
        // c1.posInAS = c1.localPos * AS.scaleInAS + AS.posInAS = (1,2,1) * (1,1,1) + (0,0,0) =
        // (1,2,1)
        assertPose(
                child1.getPoseInActivitySpace(),
                new Pose(new Vector3(1f, 2f, 1f), Quaternion.Identity));

        // c2.posInAS = c2.localPos * c1.scaleInAS + c1.posInAS = (1,2,1) * (2,2,2) + (1,2,1) =
        // (3,6,3)
        assertPose(
                child2.getPoseInActivitySpace(),
                new Pose(new Vector3(3f, 6f, 3f), Quaternion.Identity));

        // c2.scaleInA = c2.localScale * c1.scaleInAS * AS.scale = (3,2,3) * (2,2,2) * (1,1,1) =
        // (6,4,6)
        // c3.posInAS = c3.localPos * c2.scaleInAS + c2.posInAS = (1,2,1) * (6,4,6) + (3,6,3) =
        // (9,14,9)
        assertPose(
                child3.getPoseInActivitySpace(),
                new Pose(new Vector3(9f, 14f, 9f), Quaternion.Identity));
    }

    @Test
    public void getActivitySpacePose_withParentChainTranslation_returnsOffsetPositionFromRoot() {
        // Create a simple pose with only a small translation on all axes
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);

        // Set the ActivitySpace as the root of this entity hierarchy.
        Entity parentEntity =
                mRuntime.createGroupEntity(pose, "parent", mRuntime.getActivitySpace());
        Entity childEntity1 = mRuntime.createGroupEntity(pose, "child1", parentEntity);
        Entity childEntity2 = mRuntime.createGroupEntity(pose, "child2", childEntity1);

        // The translations should accumulate with each child, but there should be no rotation.
        assertVector3(
                parentEntity.getActivitySpacePose().getTranslation(), new Vector3(1f, 2f, 3f));
        assertVector3(
                childEntity1.getActivitySpacePose().getTranslation(), new Vector3(2f, 4f, 6f));
        assertVector3(
                childEntity2.getActivitySpacePose().getTranslation(), new Vector3(3f, 6f, 9f));
        assertRotation(childEntity2.getActivitySpacePose().getRotation(), Quaternion.Identity);
    }

    @Test
    public void getActivitySpacePose_withParentChainRotation_returnsOffsetRotationFromRoot() {
        // Create a pose with a translation and one with 90 degree rotation around the y axis.
        Vector3 parentTranslation = new Vector3(1f, 0f, 0f);
        Pose translatedPose = new Pose(parentTranslation, Quaternion.Identity);
        Quaternion quaternion = Quaternion.fromAxisAngle(new Vector3(0f, 1f, 0f), 90f);
        Pose rotatedPose = new Pose(new Vector3(0f, 0f, 0f), quaternion);

        // The parent has a translation and no rotation and each child adds a rotation.
        Entity parentEntity =
                mRuntime.createGroupEntity(translatedPose, "parent", mRuntime.getActivitySpace());
        Entity childEntity1 = mRuntime.createGroupEntity(rotatedPose, "child1", parentEntity);
        Entity childEntity2 = mRuntime.createGroupEntity(rotatedPose, "child2", childEntity1);

        // There should be no translation offset from the parent, but rotations should accumulate.
        assertPose(parentEntity.getActivitySpacePose(), translatedPose);
        assertPose(childEntity1.getActivitySpacePose(), new Pose(parentTranslation, quaternion));
        assertPose(
                childEntity2.getActivitySpacePose(),
                new Pose(
                        parentTranslation,
                        Quaternion.fromAxisAngle(new Vector3(0f, 1f, 0f), 180f)));
    }

    @Test
    public void getActivitySpacePose_withParentChainPoseOffsets_returnsOffsetPoseFromRoot() {
        // Create a pose with a 1D translation and a 90 degree rotation around the z axis.
        Vector3 parentTranslation = new Vector3(1f, 0f, 0f);
        Quaternion quaternion = Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 90f);
        Pose pose = new Pose(parentTranslation, quaternion);

        // Each entity adds a translation and a rotation.
        Entity parentEntity =
                mRuntime.createGroupEntity(pose, "parent", mRuntime.getActivitySpace());
        Entity childEntity1 = mRuntime.createGroupEntity(pose, "child1", parentEntity);
        Entity childEntity2 = mRuntime.createGroupEntity(pose, "child2", childEntity1);

        // Local pose of ActivitySpace's direct child must be the same as child's ActivitySpace
        // pose.
        assertPose(parentEntity.getActivitySpacePose(), parentEntity.getPose());

        // Each child should be positioned one unit away at 90 degrees from its parent's position.
        // Since our coordinate system is right-handed, a +ve rotation around the z axis is a
        // counter-clockwise rotation of the XY plane.
        // First child should be 1 unit in the ActivitySpace's positive y direction from its parent
        assertPose(
                childEntity1.getActivitySpacePose(),
                new Pose(
                        new Vector3(1f, 1f, 0f),
                        Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 180f)));
        // Second child should be 1 unit in the ActivitySpace's negative x direction from its parent
        assertPose(
                childEntity2.getActivitySpacePose(),
                new Pose(
                        new Vector3(0f, 1f, 0f),
                        Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 270f)));
    }

    @Test
    public void getActivitySpacePose_withDefaultParent_returnsPose() throws Exception {
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), new Quaternion(1f, 2f, 3f, 4f));

        // All these entities should have the ActivitySpaceRootImpl as their parent by default.
        PanelEntity panelEntity = createPanelEntity(pose);
        GltfEntity gltfEntity = createGltfEntity(pose);
        Entity groupEntity = createGroupEntity(pose);

        assertPose(panelEntity.getActivitySpacePose(), pose);
        assertPose(gltfEntity.getActivitySpacePose(), pose);
        assertPose(groupEntity.getActivitySpacePose(), pose);
    }

    @Test
    public void getPoseInActivitySpace_withScale_returnsScaledPose() throws Exception {
        Pose localPose = new Pose(new Vector3(1f, 2f, 1f), Quaternion.Identity);

        // Create a hierarchy of entities each translated from their parent by (1,1,1) in parent
        // space.
        GltfEntityImpl child1 = (GltfEntityImpl) createGltfEntity(localPose);
        GltfEntityImpl child2 = (GltfEntityImpl) createGltfEntity(localPose);
        GltfEntityImpl child3 = (GltfEntityImpl) createGltfEntity(localPose);

        assertVector3(
                mRuntime.getActivitySpace().getScale(Space.ACTIVITY), new Vector3(1f, 1f, 1f));

        // Set a non-unit local scale to each child.
        child1.setParent(mRuntime.getActivitySpace());
        child1.setScale(new Vector3(2f, 2f, 2f));

        child2.setParent(child1);
        child2.setScale(new Vector3(3f, 2f, 3f));

        child3.setParent(child2);
        child3.setScale(new Vector3(1f, 1f, 2f));

        // See getPoseInActivitySpace_withScale_returnsScaledPose for more detailed comments.
        // The position should be (parent's scale * child's position) + parent's position
        // since there's no rotation.
        assertPose(
                child1.getActivitySpacePose(),
                new Pose(new Vector3(1f, 2f, 1f), Quaternion.Identity));
        assertPose(
                child2.getActivitySpacePose(),
                new Pose(new Vector3(3f, 6f, 3f), Quaternion.Identity));
        assertPose(
                child3.getActivitySpacePose(),
                new Pose(new Vector3(9f, 14f, 9f), Quaternion.Identity));
    }

    @Test
    public void transformPoseTo_sameDestAndSourceEntity_returnsUnchangedPose() throws Exception {
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), new Quaternion(1f, 2f, 3f, 4f));
        Pose identity = new Pose();

        PanelEntity panelEntity = createPanelEntity(pose);
        GltfEntity gltfEntity = createGltfEntity(pose);
        Entity groupEntity = createGroupEntity(pose);

        assertPose(panelEntity.transformPoseTo(pose, panelEntity), pose);
        assertPose(gltfEntity.transformPoseTo(pose, gltfEntity), pose);
        assertPose(groupEntity.transformPoseTo(pose, groupEntity), pose);
        assertPose(panelEntity.transformPoseTo(identity, panelEntity), identity);
        assertPose(gltfEntity.transformPoseTo(identity, gltfEntity), identity);
        assertPose(groupEntity.transformPoseTo(identity, groupEntity), identity);
    }

    @Test
    public void transformPoseTo_withOnlyTranslationOffset_returnsTranslationDifference() {
        PanelEntityImpl sourceEntity = (PanelEntityImpl) createPanelEntity();
        GltfEntityImpl destinationEntity = createGltfEntity();
        sourceEntity.setPose(
                new Pose(new Vector3(1f, 2f, 3f), sourceEntity.getPose().getRotation()));
        destinationEntity.setPose(
                new Pose(new Vector3(4f, 5f, 6f), destinationEntity.getPose().getRotation()));
        Pose offsetFromSource = new Pose(new Vector3(7f, 8f, 9f), Quaternion.Identity);

        // The expected translation is destOffset = (sourceOrigin + sourceOffset) - destOrigin
        // since there's no rotation and the entities are in the same coordinate space.
        // So ((1,2,3) + (7,8,9)) - (4,5,6) = (4, 5, 6)
        Pose offsetInDestinationSpace =
                sourceEntity.transformPoseTo(offsetFromSource, destinationEntity);
        Pose expectedPose = new Pose(new Vector3(4f, 5f, 6f), Quaternion.Identity);

        assertPose(offsetInDestinationSpace, expectedPose);
    }

    @Test
    public void transformPoseTo_withOnlyRotationOffset_returnsRotationDifference() {
        PanelEntityImpl sourceEntity = (PanelEntityImpl) createPanelEntity();
        GltfEntityImpl destinationEntity = createGltfEntity();

        sourceEntity.setPose(
                new Pose(
                        sourceEntity.getPose().getTranslation(),
                        Quaternion.fromEulerAngles(new Vector3(1f, 2f, 3f))));
        destinationEntity.setPose(
                new Pose(
                        destinationEntity.getPose().getTranslation(),
                        Quaternion.fromEulerAngles(new Vector3(4f, 5f, 6f))));

        Pose offsetFromSource =
                new Pose(new Vector3(), Quaternion.fromEulerAngles(new Vector3(7f, 8f, 9f)));

        // The expected rotation is (source + sourceOffset) - destination since the source and
        // destination are in the same coordinate space: ((1,2,3) + (7,8,9)) - (4,5,6) = (4, 5, 6)
        Pose offsetInDestinationSpace =
                sourceEntity.transformPoseTo(offsetFromSource, destinationEntity);
        Pose expectedPose =
                new Pose(new Vector3(), Quaternion.fromEulerAngles(new Vector3(4f, 5f, 6f)));

        assertPose(offsetInDestinationSpace, expectedPose);
    }

    @Test
    public void transformPoseTo_withDifferentTranslationAndRotation_returnsTransformedPose() {
        // Assume the source and destination entities are in the same coordinate space.
        Vector3 sourceVector = new Vector3(1f, 2f, 3f);
        Quaternion sourceQuaternion = Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), -90f);
        Vector3 destinationVector = new Vector3(10f, 20f, 30f);
        Quaternion destinationQuaternion = Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 90f);
        Pose identity = new Pose();

        AndroidXrEntity sourceEntity =
                (AndroidXrEntity) createGroupEntity(new Pose(sourceVector, sourceQuaternion));
        AndroidXrEntity destinationEntity =
                (AndroidXrEntity)
                        createGroupEntity(new Pose(destinationVector, destinationQuaternion));

        //// Transform an identity pose from the source to the destination space. ////
        Pose sourceToDestinationPose = sourceEntity.transformPoseTo(identity, destinationEntity);

        // The expected rotation is the difference between the quaternions -90 - 90 = -180.
        Quaternion expectedQuaternion = Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), -180f);
        assertRotation(sourceToDestinationPose.getRotation(), expectedQuaternion);

        // The expected translation is the difference between the source and destination vectors
        // rotated
        // by the inverse of the destination quaternion.
        Vector3 expectedVector =
                destinationQuaternion.getInverse().times(sourceVector.minus(destinationVector));

        // So difference is (1,2,3) - (10,20,30) = (-9,-18,-27) then rotate CCW by -90 degrees
        // around
        // the z axis (ie. swap x and y, set x positive and y negative since we're rotating from 3rd
        // quadrant to the 2nd quadrant of XY plane) => (-18, 9, -27)
        assertVector3(expectedVector, new Vector3(-18f, 9f, -27f));
        assertVector3(sourceToDestinationPose.getTranslation(), expectedVector);

        //// Transform an offset pose from the source to the destination. ////
        Pose offsetPose =
                new Pose(
                        new Vector3(1f, 0f, 0f),
                        Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), 20f));
        Pose newSourceToDestinationPose =
                sourceEntity.transformPoseTo(offsetPose, destinationEntity);

        // The expected rotation is the difference between the quaternions (20-90) - 90 = -160.
        Quaternion newExpectedQuaternion = Quaternion.fromAxisAngle(new Vector3(0f, 0f, 1f), -160f);
        assertRotation(newSourceToDestinationPose.getRotation(), newExpectedQuaternion);

        // The expected translation is expected to be the same as the previous one but with the
        // offset
        // vector added to it in the destination space.
        Vector3 offsetInActivitySpace = sourceQuaternion.times(offsetPose.getTranslation());
        Vector3 offsetInDestinationSpace =
                destinationQuaternion.getInverse().times(offsetInActivitySpace);
        Vector3 newExpectedVector = expectedVector.plus(offsetInDestinationSpace);

        // So (1, 0, 0) rotated by -90 degrees around the z axis is (0, 1, 0) in activity space then
        // add to the difference from source to destination vector (-9,-18,-27) to get (-9, -19,
        // -27)
        // and finally rotate by the inverse of the destination quaternion to get (-19, 9, -27).
        assertVector3(newExpectedVector, new Vector3(-19f, 9f, -27f));
        assertVector3(newSourceToDestinationPose.getTranslation(), newExpectedVector);
    }

    @Test
    public void getAlpha_returnsSetAlpha() {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        Entity groupEntity = createGroupEntity();

        assertThat(panelEntity.getAlpha()).isEqualTo(1.0f);
        assertThat(gltfEntity.getAlpha()).isEqualTo(1.0f);
        assertThat(groupEntity.getAlpha()).isEqualTo(1.0f);

        panelEntity.setAlpha(0.5f);
        gltfEntity.setAlpha(0.5f);
        groupEntity.setAlpha(0.5f);

        assertThat(panelEntity.getAlpha()).isEqualTo(0.5f);
        assertThat(gltfEntity.getAlpha()).isEqualTo(0.5f);
        assertThat(groupEntity.getAlpha()).isEqualTo(0.5f);
        assertThat(mNodeRepository.map(NodeRepository.NodeMetadata::getAlpha))
                .containsAtLeast(0.5f, 0.5f, 0.5f);
    }

    @Test
    public void getActivitySpaceAlpha_returnsTotalAncestorAlpha() {
        PanelEntity grandparent = createPanelEntity();
        GltfEntity parent = createGltfEntity();
        Entity entity = createGroupEntity();

        assertThat(grandparent.getAlpha(Space.ACTIVITY)).isEqualTo(1.0f);
        assertThat(parent.getAlpha(Space.ACTIVITY)).isEqualTo(1.0f);
        assertThat(entity.getAlpha(Space.ACTIVITY)).isEqualTo(1.0f);

        grandparent.setAlpha(0.5f);
        parent.setParent(grandparent);
        parent.setAlpha(0.5f);
        entity.setParent(parent);
        entity.setAlpha(0.5f);

        assertThat(grandparent.getAlpha(Space.ACTIVITY)).isEqualTo(0.5f);
        assertThat(parent.getAlpha(Space.ACTIVITY)).isEqualTo(0.25f);
        assertThat(entity.getAlpha(Space.ACTIVITY)).isEqualTo(0.125f);
        assertThat(mNodeRepository.map(NodeRepository.NodeMetadata::getAlpha))
                .containsAtLeast(0.5f, 0.5f, 0.5f);
    }

    @Test
    public void transformPoseTo_withScaleAndNoOffset_returnsPose() {
        PanelEntityImpl sourceEntity = (PanelEntityImpl) createPanelEntity();
        GltfEntityImpl destinationEntity = createGltfEntity();
        sourceEntity.setPose(new Pose(new Vector3(0f, 0f, 1f), Quaternion.Identity));
        sourceEntity.setScale(new Vector3(2f, 2f, 2f));
        destinationEntity.setPose(new Pose(new Vector3(1f, 0f, 0f), Quaternion.Identity));
        destinationEntity.setScale(new Vector3(3f, 3f, 3f));

        assertPose(
                sourceEntity.transformPoseTo(Pose.Identity, destinationEntity),
                new Pose(new Vector3(-1 / 3f, 0f, 1 / 3f), Quaternion.Identity));
    }

    @Test
    public void transformPoseTo_withScale_returnsPose() {
        PanelEntityImpl sourceEntity = (PanelEntityImpl) createPanelEntity();
        GltfEntityImpl destinationEntity = createGltfEntity();
        sourceEntity.setPose(new Pose(new Vector3(0f, 0f, 1f), Quaternion.Identity));
        sourceEntity.setScale(new Vector3(2f, 2f, 2f));
        destinationEntity.setPose(new Pose(new Vector3(1f, 0f, 0f), Quaternion.Identity));
        destinationEntity.setScale(new Vector3(3f, 3f, 3f));

        Pose offsetFromSource = new Pose(new Vector3(0f, 0f, 1f), Quaternion.Identity);

        assertPose(
                sourceEntity.transformPoseTo(offsetFromSource, destinationEntity),
                new Pose(new Vector3(-1 / 3f, 0f, 1f), Quaternion.Identity));
    }

    @Test
    public void transformPoseTo_withNonUniformScalesAndTranslations_returnsPose() {
        PanelEntityImpl sourceEntity = (PanelEntityImpl) createPanelEntity();
        GltfEntityImpl destinationEntity = createGltfEntity();
        sourceEntity.setPose(new Pose(new Vector3(0f, 0f, 1f), Quaternion.Identity));
        sourceEntity.setScale(new Vector3(0.5f, 2f, -3f));
        destinationEntity.setPose(new Pose(new Vector3(1f, 1f, 0f), Quaternion.Identity));
        destinationEntity.setScale(new Vector3(4f, 5f, 6f));

        Pose offsetFromSource = new Pose(new Vector3(1f, 3f, 1f), Quaternion.Identity);

        // translation is:
        //  ((localOffsetFromSource * scale of source) + sourceTranslation - destinationTranslation)
        //    * (1/scale of  destination)
        //
        //  ((1, 3, 1) * (1/2, 2, -3) + (0, 0, 1) - (1, 1, 0)) * (1/4, 1/5, 1/6) =
        //              ((1/2, 6, -3) + (0, 0, 1) - (1, 1, 0)) * (1/4, 1/5, 1/6) =
        //                                       (-1/2, 5, -2) * (1/4, 1/5, 1/6) =
        //                                                     (-1/8, 1, -2/6) = (-1/8, 1, -1/3)
        assertPose(
                sourceEntity.transformPoseTo(offsetFromSource, destinationEntity),
                new Pose(new Vector3(-1 / 8f, 1f, -1 / 3f), Quaternion.Identity));
    }

    @Test
    public void addComponent_callsOnAttach() {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(new Pose());
        Component component = mock(Component.class);
        when(component.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component)).isTrue();
        verify(component).onAttach(panelEntity);

        assertThat(gltfEntity.addComponent(component)).isTrue();
        verify(component).onAttach(gltfEntity);

        assertThat(loggingEntity.addComponent(component)).isTrue();
        verify(component).onAttach(loggingEntity);
    }

    @Test
    public void addComponent_failsIfOnAttachFails() {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(new Pose());
        Component component = mock(Component.class);
        when(component.onAttach(any())).thenReturn(false);

        assertThat(panelEntity.addComponent(component)).isFalse();
        verify(component).onAttach(panelEntity);

        assertThat(gltfEntity.addComponent(component)).isFalse();
        verify(component).onAttach(gltfEntity);

        assertThat(loggingEntity.addComponent(component)).isFalse();
        verify(component).onAttach(loggingEntity);
    }

    @Test
    public void removeComponent_callsOnDetach() {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(new Pose());
        Component component = mock(Component.class);
        when(component.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component)).isTrue();
        verify(component).onAttach(panelEntity);

        panelEntity.removeComponent(component);

        verify(component).onDetach(panelEntity);

        assertThat(gltfEntity.addComponent(component)).isTrue();
        verify(component).onAttach(gltfEntity);

        gltfEntity.removeComponent(component);

        verify(component).onDetach(gltfEntity);

        assertThat(loggingEntity.addComponent(component)).isTrue();
        verify(component).onAttach(loggingEntity);

        loggingEntity.removeComponent(component);

        verify(component).onDetach(loggingEntity);
    }

    @Test
    public void addingSameComponentTypeAgain_addsComponent() {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(new Pose());
        Component component1 = mock(Component.class);
        Component component2 = mock(Component.class);
        when(component1.onAttach(any())).thenReturn(true);
        when(component2.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component1)).isTrue();
        assertThat(panelEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(panelEntity);
        verify(component2).onAttach(panelEntity);

        assertThat(gltfEntity.addComponent(component1)).isTrue();
        assertThat(gltfEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(gltfEntity);
        verify(component2).onAttach(panelEntity);

        assertThat(loggingEntity.addComponent(component1)).isTrue();
        assertThat(loggingEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(loggingEntity);
        verify(component2).onAttach(panelEntity);
    }

    @Test
    public void addingDifferentComponentType_addComponentSucceeds() {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(new Pose());
        Component component1 = mock(Component.class);
        Component component2 = mock(FakeComponent.class);
        when(component1.onAttach(any())).thenReturn(true);
        when(component2.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component1)).isTrue();
        assertThat(panelEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(panelEntity);
        verify(component2).onAttach(panelEntity);

        assertThat(gltfEntity.addComponent(component1)).isTrue();
        assertThat(gltfEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(gltfEntity);
        verify(component2).onAttach(gltfEntity);

        assertThat(loggingEntity.addComponent(component1)).isTrue();
        assertThat(loggingEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(loggingEntity);
        verify(component2).onAttach(loggingEntity);
    }

    @Test
    public void removeAll_callsOnDetachOnAll() {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(new Pose());
        Component component1 = mock(Component.class);
        Component component2 = mock(FakeComponent.class);
        when(component1.onAttach(any())).thenReturn(true);
        when(component2.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component1)).isTrue();
        assertThat(panelEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(panelEntity);
        verify(component2).onAttach(panelEntity);

        panelEntity.removeAllComponents();

        verify(component1).onDetach(panelEntity);
        verify(component2).onDetach(panelEntity);

        assertThat(gltfEntity.addComponent(component1)).isTrue();
        assertThat(gltfEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(gltfEntity);
        verify(component2).onAttach(gltfEntity);

        gltfEntity.removeAllComponents();

        verify(component1).onDetach(gltfEntity);
        verify(component2).onDetach(gltfEntity);

        assertThat(loggingEntity.addComponent(component1)).isTrue();
        assertThat(loggingEntity.addComponent(component2)).isTrue();
        verify(component1).onAttach(loggingEntity);
        verify(component2).onAttach(loggingEntity);

        loggingEntity.removeAllComponents();

        verify(component1).onDetach(loggingEntity);
        verify(component2).onDetach(loggingEntity);
    }

    @Test
    public void addSameComponentTwice_callsOnAttachTwice() {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(new Pose());
        Component component = mock(Component.class);
        when(component.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component)).isTrue();
        assertThat(panelEntity.addComponent(component)).isTrue();

        verify(component, times(2)).onAttach(panelEntity);
        assertThat(gltfEntity.addComponent(component)).isTrue();
        assertThat(gltfEntity.addComponent(component)).isTrue();
        verify(component, times(2)).onAttach(gltfEntity);
        assertThat(loggingEntity.addComponent(component)).isTrue();
        assertThat(loggingEntity.addComponent(component)).isTrue();
        verify(component, times(2)).onAttach(loggingEntity);
    }

    @Test
    public void removeSameComponentTwice_callsOnDetachOnce() {
        PanelEntity panelEntity = createPanelEntity();
        GltfEntity gltfEntity = createGltfEntity();
        LoggingEntity loggingEntity = mRuntime.createLoggingEntity(new Pose());
        Component component = mock(Component.class);
        when(component.onAttach(any())).thenReturn(true);

        assertThat(panelEntity.addComponent(component)).isTrue();
        verify(component).onAttach(panelEntity);

        panelEntity.removeComponent(component);
        panelEntity.removeComponent(component);

        verify(component).onDetach(panelEntity);
        assertThat(gltfEntity.addComponent(component)).isTrue();
        verify(component).onAttach(gltfEntity);

        gltfEntity.removeComponent(component);
        gltfEntity.removeComponent(component);

        verify(component).onDetach(gltfEntity);
        assertThat(loggingEntity.addComponent(component)).isTrue();
        verify(component).onAttach(loggingEntity);

        loggingEntity.removeComponent(component);
        loggingEntity.removeComponent(component);

        verify(component).onDetach(loggingEntity);
    }

    @Test
    public void createInteractableComponent_returnsComponent() {
        InputEventListener mockConsumer = mock(InputEventListener.class);
        InteractableComponent interactableComponent =
                mRuntime.createInteractableComponent(directExecutor(), mockConsumer);

        assertThat(interactableComponent).isNotNull();
    }

    private void sendInputEvent(Node node, com.android.extensions.xr.node.InputEvent inputEvent) {
        ShadowNode shadowNode = ShadowNode.extract(node);
        shadowNode
                .getInputExecutor()
                .execute(() -> shadowNode.getInputListener().accept(inputEvent));
    }

    @Test
    public void addInputEventConsumerToEntity_setsUpNodeListener() {
        InputEventListener mockConsumer = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        Executor executor = directExecutor();
        panelEntity.addInputEventListener(executor, mockConsumer);
        ShadowNode shadowNode = ShadowNode.extract(getNode(panelEntity));

        assertThat(shadowNode.getInputListener()).isNotNull();
        assertThat(shadowNode.getInputExecutor()).isEqualTo(mFakeExecutor);

        com.android.extensions.xr.node.InputEvent inputEvent =
                ShadowInputEvent.create(
                        /* origin= */ new Vec3(0, 0, 0), /* direction= */ new Vec3(1, 1, 1));
        sendInputEvent(getNode(panelEntity), inputEvent);
        mFakeExecutor.runAll();

        verify(mockConsumer).onInputEvent(any());
    }

    @Test
    public void inputEvent_hasHitInfo() {
        InputEventListener mockConsumer = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        Executor executor = directExecutor();
        panelEntity.addInputEventListener(executor, mockConsumer);
        Node node = getNode(panelEntity);
        com.android.extensions.xr.node.InputEvent inputEvent =
                ShadowInputEvent.create(
                        /* origin= */ new Vec3(0, 0, 0),
                        /* direction= */ new Vec3(1, 1, 1),
                        /* histInfo= */ new HitInfo(
                                /* subspaceImpressNodeId= */ 0,
                                /* inputNode= */ node,
                                /* transform= */ new Mat4f(new float[16]),
                                /* hitPosition= */ new Vec3(1, 2, 3)),
                        /* secondaryHitInfo= */ null);
        sendInputEvent(node, inputEvent);
        mFakeExecutor.runAll();

        ArgumentCaptor<InputEvent> inputEventCaptor = ArgumentCaptor.forClass(InputEvent.class);

        verify(mockConsumer).onInputEvent(inputEventCaptor.capture());

        InputEvent capturedEvent = inputEventCaptor.getValue();

        assertThat(capturedEvent.getHitInfoList()).isNotEmpty();

        InputEvent.HitInfo hitInfo = capturedEvent.getHitInfoList().get(0);

        assertThat(hitInfo.getInputEntity()).isEqualTo(panelEntity);
        assertThat(hitInfo.getHitPosition()).isEqualTo(new Vector3(1, 2, 3));
    }

    @Test
    public void passingNoExecutorWhenAddingConsumer_usesInternalExecutor() {
        InputEventListener mockConsumer = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        panelEntity.addInputEventListener(null, mockConsumer);
        ShadowNode shadowNode = ShadowNode.extract(getNode(panelEntity));

        assertThat(shadowNode.getInputListener()).isNotNull();
        assertThat(shadowNode.getInputExecutor()).isNotNull();
    }

    @Test
    public void addMultipleInputEventConsumerToEntity_setsUpInputCallbacksForAll() {
        InputEventListener mockConsumer1 = mock(InputEventListener.class);
        InputEventListener mockConsumer2 = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        Executor executor = directExecutor();
        panelEntity.addInputEventListener(executor, mockConsumer1);
        panelEntity.addInputEventListener(executor, mockConsumer2);
        com.android.extensions.xr.node.InputEvent inputEvent =
                ShadowInputEvent.create(
                        /* origin= */ new Vec3(0, 0, 0), /* direction= */ new Vec3(1, 1, 1));
        sendInputEvent(getNode(panelEntity), inputEvent);
        mFakeExecutor.runAll();

        verify(mockConsumer1).onInputEvent(any());
        verify(mockConsumer2).onInputEvent(any());
    }

    @Test
    public void addMultipleInputEventConsumersToEntity_setsUpInputCallbacksOnGivenExecutors() {
        InputEventListener mockConsumer1 = mock(InputEventListener.class);
        InputEventListener mockConsumer2 = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        FakeScheduledExecutorService executor1 = new FakeScheduledExecutorService();
        FakeScheduledExecutorService executor2 = new FakeScheduledExecutorService();
        panelEntity.addInputEventListener(executor1, mockConsumer1);
        panelEntity.addInputEventListener(executor2, mockConsumer2);
        com.android.extensions.xr.node.InputEvent inputEvent =
                ShadowInputEvent.create(
                        /* origin= */ new Vec3(0, 0, 0), /* direction= */ new Vec3(1, 1, 1));

        sendInputEvent(getNode(panelEntity), inputEvent);
        mFakeExecutor.runAll();

        assertThat(executor1.hasNext()).isTrue();
        assertThat(executor2.hasNext()).isTrue();

        executor1.runAll();
        executor2.runAll();

        verify(mockConsumer1).onInputEvent(any());
        verify(mockConsumer2).onInputEvent(any());
    }

    @Test
    public void removeInputEventConsumerToEntity_removesFromCallbacks() {
        InputEventListener mockConsumer1 = mock(InputEventListener.class);
        InputEventListener mockConsumer2 = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        Executor executor = directExecutor();
        panelEntity.addInputEventListener(executor, mockConsumer1);
        panelEntity.addInputEventListener(executor, mockConsumer2);
        com.android.extensions.xr.node.InputEvent inputEvent =
                ShadowInputEvent.create(
                        /* origin= */ new Vec3(0, 0, 0), /* direction= */ new Vec3(1, 1, 1));

        sendInputEvent(getNode(panelEntity), inputEvent);
        mFakeExecutor.runAll();

        panelEntity.removeInputEventListener(mockConsumer1);

        sendInputEvent(getNode(panelEntity), inputEvent);
        mFakeExecutor.runAll();

        verify(mockConsumer2, times(2)).onInputEvent(any());
        verify(mockConsumer1).onInputEvent(any());
    }

    @Test
    public void removeAllInputEventConsumers_stopsInputListening() {
        InputEventListener mockConsumer1 = mock(InputEventListener.class);
        InputEventListener mockConsumer2 = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        Executor executor = directExecutor();
        panelEntity.addInputEventListener(executor, mockConsumer1);
        panelEntity.addInputEventListener(executor, mockConsumer2);
        ShadowNode shadowNode = ShadowNode.extract(getNode(panelEntity));
        com.android.extensions.xr.node.InputEvent inputEvent =
                ShadowInputEvent.create(
                        /* origin= */ new Vec3(0, 0, 0), /* direction= */ new Vec3(1, 1, 1));

        sendInputEvent(getNode(panelEntity), inputEvent);
        mFakeExecutor.runAll();

        verify(mockConsumer1).onInputEvent(any());
        verify(mockConsumer2).onInputEvent(any());

        panelEntity.removeInputEventListener(mockConsumer1);
        panelEntity.removeInputEventListener(mockConsumer2);

        assertThat(((PanelEntityImpl) panelEntity).getReformEventConsumerMap()).isEmpty();
        assertThat(shadowNode.getInputListener()).isNull();
        assertThat(shadowNode.getInputExecutor()).isNull();
    }

    @Test
    public void dispose_stopsInputListening() {
        InputEventListener mockConsumer1 = mock(InputEventListener.class);
        InputEventListener mockConsumer2 = mock(InputEventListener.class);
        PanelEntity panelEntity = createPanelEntity();
        Executor executor = directExecutor();
        panelEntity.addInputEventListener(executor, mockConsumer1);
        panelEntity.addInputEventListener(executor, mockConsumer2);
        ShadowNode shadowNode = ShadowNode.extract(getNode(panelEntity));
        com.android.extensions.xr.node.InputEvent inputEvent =
                ShadowInputEvent.create(
                        /* origin= */ new Vec3(0, 0, 0), /* direction= */ new Vec3(1, 1, 1));

        sendInputEvent(getNode(panelEntity), inputEvent);
        mFakeExecutor.runAll();

        verify(mockConsumer1).onInputEvent(any());
        verify(mockConsumer2).onInputEvent(any());

        panelEntity.dispose();

        assertThat(((PanelEntityImpl) panelEntity).getReformEventConsumerMap()).isEmpty();
        assertThat(shadowNode.getInputListener()).isNull();
        assertThat(shadowNode.getInputExecutor()).isNull();
    }

    @Test
    public void isHidden_returnsSetHidden() {
        PanelEntity parentEntity = createPanelEntity();

        assertThat(parentEntity.isHidden(true)).isFalse();
        assertThat(parentEntity.isHidden(false)).isFalse();

        PanelEntity childEntity1 = createPanelEntity();
        PanelEntity childEntity2 = createPanelEntity();
        childEntity1.setParent(parentEntity);
        childEntity2.setParent(childEntity1);

        assertThat(childEntity1.isHidden(true)).isFalse();
        assertThat(childEntity1.isHidden(false)).isFalse();

        parentEntity.setHidden(true);

        assertThat(parentEntity.isHidden(true)).isTrue();
        assertThat(parentEntity.isHidden(false)).isTrue();
        assertThat(childEntity1.isHidden(true)).isTrue();
        assertThat(childEntity1.isHidden(false)).isFalse();
        assertThat(childEntity2.isHidden(true)).isTrue();
        assertThat(childEntity2.isHidden(false)).isFalse();

        parentEntity.setHidden(false);

        assertThat(parentEntity.isHidden(true)).isFalse();
        assertThat(parentEntity.isHidden(false)).isFalse();
        assertThat(childEntity1.isHidden(true)).isFalse();
        assertThat(childEntity1.isHidden(false)).isFalse();
        assertThat(childEntity2.isHidden(true)).isFalse();
        assertThat(childEntity2.isHidden(false)).isFalse();

        childEntity1.setHidden(true);

        assertThat(parentEntity.isHidden(true)).isFalse();
        assertThat(parentEntity.isHidden(false)).isFalse();
        assertThat(childEntity1.isHidden(true)).isTrue();
        assertThat(childEntity1.isHidden(false)).isTrue();
        assertThat(childEntity2.isHidden(true)).isTrue();
        assertThat(childEntity2.isHidden(false)).isFalse();
    }

    @Test
    public void setHidden_modifiesReforms() {
        PanelEntity testEntity = createPanelEntity();

        assertThat(
                        testEntity.addComponent(
                                mRuntime.createMovableComponent(
                                        /* systemMovable= */ true,
                                        /* scaleInZ= */ true,
                                        /* userAnchorable */ false)))
                .isTrue();

        testEntity.setHidden(true);

        assertThat(mNodeRepository.getReformOptions(getNode(testEntity))).isNull();

        testEntity.setHidden(false);

        assertThat(mNodeRepository.getReformOptions(getNode(testEntity)).getEnabledReform())
                .isEqualTo(ALLOW_MOVE);
    }

    @Test
    public void constructor_initializesBoundaryConsentCacheCorrectly() {
        mRuntime.destroy();
        Settings.System.putInt(mContentResolver, TOGGLE_GUARDIAN, 1);
        Settings.Secure.putInt(mContentResolver, GUARDIAN_CONSENT_GRANTED, 1);
        mRuntime = createRuntime();

        boolean result = mRuntime.isBoundaryConsentGranted();

        assertThat(result).isTrue();

        mRuntime.destroy();
        Settings.System.putInt(mContentResolver, TOGGLE_GUARDIAN, 0);
        Settings.Secure.putInt(mContentResolver, GUARDIAN_CONSENT_GRANTED, 1);
        mRuntime = createRuntime();

        result = mRuntime.isBoundaryConsentGranted();

        assertThat(result).isTrue();

        mRuntime.destroy();
        Settings.System.putInt(mContentResolver, TOGGLE_GUARDIAN, 0);
        Settings.Secure.putInt(mContentResolver, GUARDIAN_CONSENT_GRANTED, 0);
        mRuntime = createRuntime();

        result = mRuntime.isBoundaryConsentGranted();

        assertThat(result).isTrue();

        mRuntime.destroy();
        Settings.System.putInt(mContentResolver, TOGGLE_GUARDIAN, 1);
        Settings.Secure.putInt(mContentResolver, GUARDIAN_CONSENT_GRANTED, 0);
        mRuntime = createRuntime();

        result = mRuntime.isBoundaryConsentGranted();

        assertThat(result).isFalse();
    }

    @Test
    public void constructor_registerBoundaryConsentStateListener() {
        assertThat(
                        mShadowContentResolver.getContentObservers(
                                IS_BOUNDARY_ENABLED_IN_DEVELOPER_OPTIONS_URI))
                .hasSize(1);
        assertThat(
                        mShadowContentResolver.getContentObservers(
                                IS_EXPLICITLY_BOUNDARY_CONSENT_GRANTED_URI))
                .hasSize(1);
    }

    @Test
    public void isBoundaryConsentGranted_returnsCachedValue() {
        // Set initial state to GRANTED = false
        Settings.System.putInt(mContentResolver, TOGGLE_GUARDIAN, 1);
        Settings.Secure.putInt(mContentResolver, GUARDIAN_CONSENT_GRANTED, 0);
        // Recreate runtime to pick up the new initial state
        mRuntime.destroy();
        mRuntime = createRuntime();
        assertThat(mRuntime.isBoundaryConsentGranted()).isFalse();

        // Directly change the underlying setting without notifying the observer
        // This simulates a situation where the cache should NOT be updated.
        Settings.Secure.putInt(mContentResolver, GUARDIAN_CONSENT_GRANTED, 1);

        // The method should still return the old, cached value.
        assertThat(mRuntime.isBoundaryConsentGranted()).isFalse();
    }

    @Test
    @SuppressWarnings(value = "unchecked")
    public void addOnBoundaryConsentChangedListener_contentResolverChange_notifiesListeners() {
        Consumer<Boolean> listener1 = (Consumer<Boolean>) mock(Consumer.class);
        Consumer<Boolean> listener2 = (Consumer<Boolean>) mock(Consumer.class);
        mRuntime.addOnBoundaryConsentChangedListener(directExecutor(), listener1);
        mRuntime.addOnBoundaryConsentChangedListener(directExecutor(), listener2);

        // Simulate initial state of system settings
        Settings.System.putInt(mContentResolver, TOGGLE_GUARDIAN, 1);
        Settings.Secure.putInt(mContentResolver, GUARDIAN_CONSENT_GRANTED, 0);
        shadowOf(Looper.getMainLooper()).idle();
        mFakeExecutor.runAll();

        // Change setting
        Settings.Secure.putInt(mActivity.getContentResolver(), GUARDIAN_CONSENT_GRANTED, 1);
        shadowOf(Looper.getMainLooper()).idle();
        mFakeExecutor.runAll();

        verify(listener1, times(1)).accept(true);
        verify(listener2, times(1)).accept(true);

        Mockito.clearInvocations(listener1, listener2);
        // Change setting again
        Settings.Secure.putInt(mActivity.getContentResolver(), GUARDIAN_CONSENT_GRANTED, 0);
        shadowOf(Looper.getMainLooper()).idle();
        mFakeExecutor.runAll();
        verify(listener1, times(1)).accept(false);
        verify(listener2, times(1)).accept(false);
    }

    @Test
    @SuppressWarnings(value = "unchecked")
    public void removeOnBoundaryConsentChangedListener_stopsReceivingEvents() {
        // Recreate the runtime to ensure a clean initial state.
        mRuntime.destroy();
        // Set an explicit initial state (GRANTED = false).
        Settings.System.putInt(mContentResolver, TOGGLE_GUARDIAN, 1);
        Settings.Secure.putInt(mContentResolver, GUARDIAN_CONSENT_GRANTED, 0);
        mRuntime = createRuntime(); // Recreate the runtime to ensure a clean state for the test.

        Consumer<Boolean> listener1 = (Consumer<Boolean>) mock(Consumer.class);
        Consumer<Boolean> listener2 = (Consumer<Boolean>) mock(Consumer.class);
        mRuntime.addOnBoundaryConsentChangedListener(directExecutor(), listener1);
        mRuntime.addOnBoundaryConsentChangedListener(directExecutor(), listener2);

        mRuntime.removeOnBoundaryConsentChangedListener(listener1);

        // Trigger a state change (from false to true).
        Settings.Secure.putInt(mContentResolver, GUARDIAN_CONSENT_GRANTED, 1);
        shadowOf(Looper.getMainLooper()).idle();
        mFakeExecutor.runAll();

        verify(listener2).accept(true);
        verify(listener1, never()).accept(any());
    }

    @Test
    public void destroy_unregisterBoundaryConsentStateListener() {
        // A runtime is created in setUp(), which registers the observer.
        assertThat(
                        mShadowContentResolver.getContentObservers(
                                IS_BOUNDARY_ENABLED_IN_DEVELOPER_OPTIONS_URI))
                .isNotEmpty();
        assertThat(
                        mShadowContentResolver.getContentObservers(
                                IS_EXPLICITLY_BOUNDARY_CONSENT_GRANTED_URI))
                .isNotEmpty();

        mRuntime.destroy();

        assertThat(
                        mShadowContentResolver.getContentObservers(
                                IS_BOUNDARY_ENABLED_IN_DEVELOPER_OPTIONS_URI))
                .isEmpty();
        assertThat(
                        mShadowContentResolver.getContentObservers(
                                IS_EXPLICITLY_BOUNDARY_CONSENT_GRANTED_URI))
                .isEmpty();
    }

    @Test
    public void dispose_clearsReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createGroupEntity();
        ReformOptions reformOptions = entity.getReformOptions();

        assertThat(reformOptions).isNotNull();

        reformOptions.setEnabledReform(ALLOW_MOVE | ALLOW_RESIZE);
        entity.dispose();

        assertThat(mNodeRepository.getReformOptions(entity.getNode())).isNull();
    }

    @Test
    public void dispose_clearsParents() {
        AndroidXrEntity entity = (AndroidXrEntity) createGroupEntity();
        entity.setParent(mRuntime.getActivitySpace());

        assertThat(entity.getParent()).isNotNull();

        entity.dispose();

        assertThat(entity.getParent()).isNull();
    }

    @Test
    public void destroy_clearsResources() {
        AndroidXrEntity entity = (AndroidXrEntity) createGroupEntity();
        assertThat(entity.getNode()).isNotNull();
        assertThat(mNodeRepository.getParent(entity.getNode())).isNotNull();

        mRuntime.destroy();

        assertThat(mNodeRepository.getParent(entity.getNode())).isNull();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getSpatialStateCallback(mActivity))
                .isNull();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getMainWindowNode(mActivity)).isNull();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getTaskNode(mActivity)).isNull();
    }

    @Test
    public void destroy_disposeInvoked() {
        AndroidXrEntity entity = (AndroidXrEntity) createGroupEntity();
        assertThat(entity.getNode()).isNotNull();
        assertThat(mNodeRepository.getParent(entity.getNode())).isNotNull();

        mRuntime.destroy();

        assertThat(mNodeRepository.getParent(entity.getNode())).isNull();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getSpatialStateCallback(mActivity))
                .isNull();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getMainWindowNode(mActivity)).isNull();
        assertThat(ShadowXrExtensions.extract(mXrExtensions).getTaskNode(mActivity)).isNull();
    }

    @Test
    public void setKeyEntity_setsKeyEntity() {
        Entity entity = createGroupEntity();
        mRuntime.setKeyEntity(entity);
        assertThat(mRuntime.getKeyEntity()).isEqualTo(entity);
    }

    @Test
    public void setKeyEntity_apiV1_doesNotSubscribe() {
        mRuntime.destroy();
        ShadowXrExtensions.extract(mXrExtensions).setApiVersion(1);
        mRuntime = createRuntime();

        Entity entity = createGroupEntity();
        ShadowNode node = ShadowNode.extract(((AndroidXrEntity) entity).getNode());

        mRuntime.setKeyEntity(entity);

        assertThat(node.getTransformListener()).isNull();
        assertThat(mRuntime.mKeyEntityTransformCloseable).isNull();
    }

    @Test
    public void setKeyEntity_apiV2_subscribesToTransformAndUpdatesHint() {
        mRuntime.destroy();
        ShadowXrExtensions.extract(mXrExtensions).setApiVersion(2);
        mRuntime = createRuntime();

        Entity entity = createGroupEntity();
        ShadowNode node = ShadowNode.extract(((AndroidXrEntity) entity).getNode());

        mRuntime.setKeyEntity(entity);

        assertThat(node.getTransformListener()).isNotNull();
        assertThat(node.getTransformExecutor()).isEqualTo(mFakeExecutor);
        assertThat(mRuntime.mKeyEntityTransformCloseable).isNotNull();

        // Trigger callback
        node.getTransformExecutor()
                .execute(
                        () ->
                                node.getTransformListener()
                                        .accept(
                                                ShadowNodeTransform.create(
                                                        new Mat4f(new float[16]))));

        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
        // Since we cannot verify the extension call directly, we just ensure it doesn't crash.
    }

    @Test
    public void setKeyEntity_apiV2_null_unsubscribes() throws Exception {
        mRuntime.destroy();
        ShadowXrExtensions.extract(mXrExtensions).setApiVersion(2);
        mRuntime = createRuntime();

        Entity entity = createGroupEntity();
        ShadowNode node = ShadowNode.extract(((AndroidXrEntity) entity).getNode());

        mRuntime.setKeyEntity(entity);

        assertThat(mRuntime.mKeyEntityTransformCloseable).isNotNull();

        FakeCloseable keyEntityTransformCloseable =
                (FakeCloseable) mRuntime.mKeyEntityTransformCloseable;

        assertThat(node.getTransformListener()).isNotNull();
        assertThat(node.getTransformExecutor()).isEqualTo(mFakeExecutor);
        assertThat(keyEntityTransformCloseable.isClosed()).isFalse();

        mRuntime.setKeyEntity(null);

        assertThat(mRuntime.getKeyEntity()).isNull();
        assertThat(keyEntityTransformCloseable.isClosed()).isTrue();
        assertThat(mRuntime.mKeyEntityTransformCloseable).isNull();
    }

    @Test
    public void setKeyEntity_apiV2_sameEntity_doesNotResubscribe() {
        mRuntime.destroy();
        ShadowXrExtensions.extract(mXrExtensions).setApiVersion(2);
        mRuntime = createRuntime();

        Entity entity = createGroupEntity();
        mRuntime.setKeyEntity(entity);
        Closeable closeable1 = mRuntime.mKeyEntityTransformCloseable;

        mRuntime.setKeyEntity(entity);
        Closeable closeable2 = mRuntime.mKeyEntityTransformCloseable;

        assertThat(closeable1).isSameInstanceAs(closeable2);
    }

    @Test
    public void setKeyEntity_apiV2_differentEntity_resubscribes() throws Exception {
        mRuntime.destroy();
        ShadowXrExtensions.extract(mXrExtensions).setApiVersion(2);
        mRuntime = createRuntime();

        Entity entity1 = createGroupEntity();
        Entity entity2 = createGroupEntity();
        ShadowNode node1 = ShadowNode.extract(((AndroidXrEntity) entity1).getNode());
        ShadowNode node2 = ShadowNode.extract(((AndroidXrEntity) entity2).getNode());

        mRuntime.setKeyEntity(entity1);
        FakeCloseable closeable1 = (FakeCloseable) mRuntime.mKeyEntityTransformCloseable;
        assertThat(closeable1.isClosed()).isFalse();
        assertThat(node1.getTransformListener()).isNotNull();

        mRuntime.setKeyEntity(entity2);
        FakeCloseable closeable2 = (FakeCloseable) mRuntime.mKeyEntityTransformCloseable;

        assertThat(closeable1.isClosed()).isTrue();
        assertThat(closeable2).isNotSameInstanceAs(closeable1);
        assertThat(closeable2.isClosed()).isFalse();
        assertThat(node2.getTransformListener()).isNotNull();
        assertThat(mRuntime.getKeyEntity()).isEqualTo(entity2);
    }

    @Test
    public void setKeyEntity_nullWhenAlreadyNull_doesNothing() {
        mRuntime.setKeyEntity(null);
        assertThat(mRuntime.getKeyEntity()).isNull();
        assertThat(mRuntime.mKeyEntityTransformCloseable).isNull();
    }

    @Test
    public void destroy_apiV2_unsubscribesKeyEntity() throws Exception {
        mRuntime.destroy();
        ShadowXrExtensions.extract(mXrExtensions).setApiVersion(2);
        mRuntime = createRuntime();

        Entity entity = createGroupEntity();
        mRuntime.setKeyEntity(entity);
        FakeCloseable closeable = (FakeCloseable) mRuntime.mKeyEntityTransformCloseable;
        assertThat(closeable.isClosed()).isFalse();

        mRuntime.destroy();

        assertThat(closeable.isClosed()).isTrue();
        assertThat(mRuntime.mKeyEntityTransformCloseable).isNull();
    }
}
