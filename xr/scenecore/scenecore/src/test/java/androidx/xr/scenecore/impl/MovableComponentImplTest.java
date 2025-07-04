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

package androidx.xr.scenecore.impl;

import static androidx.xr.runtime.testing.math.MathAssertions.assertPose;
import static androidx.xr.runtime.testing.math.MathAssertions.assertVector3;

import static com.android.extensions.xr.node.ReformEvent.REFORM_STATE_END;
import static com.android.extensions.xr.node.ReformEvent.REFORM_STATE_ONGOING;
import static com.android.extensions.xr.node.ReformEvent.REFORM_STATE_START;
import static com.android.extensions.xr.node.ReformEvent.REFORM_TYPE_MOVE;
import static com.android.extensions.xr.node.ReformEvent.REFORM_TYPE_RESIZE;
import static com.android.extensions.xr.node.ReformOptions.ALLOW_MOVE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.test.rule.GrantPermissionRule;
import androidx.xr.runtime.internal.AnchorEntity;
import androidx.xr.runtime.internal.AnchorPlacement;
import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.JxrPlatformAdapter;
import androidx.xr.runtime.internal.MovableComponent;
import androidx.xr.runtime.internal.MoveEvent;
import androidx.xr.runtime.internal.MoveEventListener;
import androidx.xr.runtime.internal.PanelEntity;
import androidx.xr.runtime.internal.PixelDimensions;
import androidx.xr.runtime.internal.PlaneSemantic;
import androidx.xr.runtime.internal.PlaneType;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.perception.Anchor;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Plane;
import androidx.xr.scenecore.impl.perception.Plane.PlaneData;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Mat4f;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeRepository;
import com.android.extensions.xr.node.NodeTransform;
import com.android.extensions.xr.node.Quatf;
import com.android.extensions.xr.node.ReformEvent;
import com.android.extensions.xr.node.ReformOptions;
import com.android.extensions.xr.node.ShadowNode;
import com.android.extensions.xr.node.ShadowNodeTransform;
import com.android.extensions.xr.node.ShadowReformEvent;
import com.android.extensions.xr.node.Vec3;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.apibindings.FakeImpressApiImpl;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Expect;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class MovableComponentImplTest {

    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary mPerceptionLibrary = mock(PerceptionLibrary.class);
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final FakeImpressApiImpl mFakeImpressApi = new FakeImpressApiImpl();
    private final EntityManager mEntityManager = new EntityManager();

    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    private final ImpSplitEngineRenderer mSplitEngineRenderer =
            Mockito.mock(ImpSplitEngineRenderer.class);
    private JxrPlatformAdapter mFakeRuntime;
    private ActivitySpaceImpl mActivitySpaceImpl;
    private Node mActivitySpaceNode;
    private final AndroidXrEntity mActivitySpaceRoot = Mockito.mock(AndroidXrEntity.class);
    private PerceptionSpaceActivityPoseImpl mPerceptionSpaceActivityPose;
    private final PanelShadowRenderer mPanelShadowRenderer =
            Mockito.mock(PanelShadowRenderer.class);
    private final NodeRepository mNodeRepository = NodeRepository.getInstance();

    @Rule public final Expect expect = Expect.create();

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant("android.permission.SCENE_UNDERSTANDING");

    @Before
    public void setUp() {
        when(mPerceptionLibrary.initSession(eq(mActivity), anyInt(), eq(mFakeExecutor)))
                .thenReturn(immediateFuture(mock(Session.class)));
        when(mPerceptionLibrary.getActivity()).thenReturn(mActivity);
        mFakeRuntime =
                JxrPlatformAdapterAxr.create(
                        mActivity,
                        mFakeExecutor,
                        mXrExtensions,
                        mFakeImpressApi,
                        mEntityManager,
                        mPerceptionLibrary,
                        mSplitEngineSubspaceManager,
                        mSplitEngineRenderer,
                        /* useSplitEngine= */ false,
                        /* unscaledGravityAlignedActivitySpace= */ false);
        mActivitySpaceImpl = (ActivitySpaceImpl) mFakeRuntime.getActivitySpace();
        mActivitySpaceNode = mActivitySpaceImpl.getNode();
        mPerceptionSpaceActivityPose =
                (PerceptionSpaceActivityPoseImpl) mFakeRuntime.getPerceptionSpaceActivityPose();
    }

    @After
    public void tearDown() {
        // Dispose the runtime between test cases to clean up lingering references.
        mFakeRuntime.dispose();
    }

    private Entity createTestEntity() {
        return mFakeRuntime.createGroupEntity(new Pose(), "test", mFakeRuntime.getActivitySpace());
    }

    private PanelEntity createTestPanelEntity() {
        Display display = mActivity.getSystemService(DisplayManager.class).getDisplays()[0];
        Context displayContext = mActivity.createDisplayContext(display);
        View view = new View(displayContext);
        view.setLayoutParams(new LayoutParams(640, 480));
        Node node = mXrExtensions.createNode();

        PanelEntityImpl panelEntity =
                new PanelEntityImpl(
                        displayContext,
                        node,
                        view,
                        mXrExtensions,
                        mEntityManager,
                        new PixelDimensions(10, 10),
                        "panelShadow",
                        mFakeExecutor);
        panelEntity.setParent(mActivitySpaceImpl);
        return panelEntity;
    }

    private void setActivitySpacePose(Pose pose, float scale) {
        Matrix4 poseMatrix = Matrix4.fromPose(pose);
        Matrix4 scaleMatrix = Matrix4.fromScale(scale);
        Matrix4 scaledPoseMatrix = poseMatrix.times(scaleMatrix);
        Mat4f mat4f = new Mat4f(scaledPoseMatrix.getData());
        NodeTransform nodeTransformEvent = ShadowNodeTransform.create(mat4f);

        ShadowNode shadowNode = ShadowNode.extract(mActivitySpaceNode);
        shadowNode
                .getTransformExecutor()
                .execute(() -> shadowNode.getTransformListener().accept(nodeTransformEvent));
        mFakeExecutor.runAll();
    }

    private ImmutableSet<AnchorPlacement> createAnyAnchorPlacement() {
        AnchorPlacement anchorPlacement =
                mFakeRuntime.createAnchorPlacementForPlanes(
                        ImmutableSet.of(PlaneType.ANY), ImmutableSet.of(PlaneSemantic.ANY));
        return ImmutableSet.of(anchorPlacement);
    }

    private Node getEntityNode(Entity entity) {
        return ((AndroidXrEntity) entity).getNode();
    }

    private void sendReformEvent(Node node, ReformEvent reformEvent) {
        ReformOptions options = mNodeRepository.getReformOptions(node);
        options.getEventExecutor().execute(() -> options.getEventCallback().accept(reformEvent));
        assertThat(mFakeExecutor.hasNext()).isTrue();
        mFakeExecutor.runAll();
    }

    @Test
    public void addMovableComponent_addsReformOptionsToNode() {
        Entity entity = createTestEntity();
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ false,
                        /* scaleInZ= */ false,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));

        assertThat(options.getEnabledReform()).isEqualTo(ALLOW_MOVE);
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();
    }

    @Test
    public void addMovableComponent_addsSystemMovableFlagToNode() {
        Entity entity = createTestEntity();
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ false,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));

        assertThat(options.getEnabledReform()).isEqualTo(ALLOW_MOVE);
        assertThat(options.getFlags())
                .isEqualTo(
                        ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT
                                | ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT);
    }

    @Test
    public void addMovableComponent_addsScaleInZFlagToNode() {
        Entity entity = createTestEntity();
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ false,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));

        assertThat(options.getEnabledReform()).isEqualTo(ALLOW_MOVE);
        assertThat(options.getFlags())
                .isEqualTo(
                        ReformOptions.FLAG_SCALE_WITH_DISTANCE
                                | ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT);
    }

    @Test
    public void addMovableComponent_addsAllFlagsToNode() {
        Entity entity = createTestEntity();
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));

        assertThat(options.getEnabledReform()).isEqualTo(ALLOW_MOVE);
        assertThat(options.getFlags())
                .isEqualTo(
                        ReformOptions.FLAG_SCALE_WITH_DISTANCE
                                | ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT
                                | ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT);
    }

    @Test
    public void setSystemMovableFlag_alsoUpdatesEntityPoseAndScale() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(entity.addComponent(movableComponent)).isTrue();
        Pose expectedPose =
                new Pose(new Vector3(2f, 2f, 2f), new Quaternion(0.5f, 0.5f, 0.5f, 0.5f));
        Vector3 expectedScale = new Vector3(1.2f, 1.2f, 1.2f);

        entity.setPose(new Pose(new Vector3(1f, 1f, 1f), new Quaternion(0f, 0f, 0f, 1f)));
        entity.setScale(new Vector3(1f, 1f, 1f));

        ReformEvent reformEvent =
                ShadowReformEvent.create(/* type= */ REFORM_TYPE_MOVE, /* state= */ 0, /* id= */ 0);
        ShadowReformEvent shadowReformEvent = ShadowReformEvent.extract(reformEvent);
        shadowReformEvent.setProposedPosition(new Vec3(2f, 2f, 2f));
        shadowReformEvent.setProposedOrientation(new Quatf(0.5f, 0.5f, 0.5f, 0.5f));
        shadowReformEvent.setProposedScale(new Vec3(1.2f, 1.2f, 1.2f));

        sendReformEvent(entity.getNode(), reformEvent);

        expect.that(entity.getPose()).isEqualTo(expectedPose);
        expect.that(entity.getScale()).isEqualTo(expectedScale);
    }

    @Test
    public void systemMovableFlagNotSet_doesNotUpdateEntityPoseAndScale() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ false,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(entity.addComponent(movableComponent)).isTrue();
        Pose expectedPose = new Pose(new Vector3(1f, 1f, 1f), new Quaternion(0f, 0f, 0f, 1f));
        Vector3 expectedScale = new Vector3(1f, 1f, 1f);
        entity.setPose(new Pose(new Vector3(1f, 1f, 1f), new Quaternion(0f, 0f, 0f, 1f)));
        entity.setScale(new Vector3(1f, 1f, 1f));

        ReformEvent reformEvent =
                ShadowReformEvent.create(/* type= */ REFORM_TYPE_MOVE, /* state= */ 0, /* id= */ 0);
        ShadowReformEvent shadowReformEvent = ShadowReformEvent.extract(reformEvent);
        shadowReformEvent.setProposedPosition(new Vec3(2f, 2f, 2f));
        shadowReformEvent.setProposedOrientation(new Quatf(0.5f, 0.5f, 0.5f, 0.5f));
        shadowReformEvent.setProposedScale(new Vec3(1.2f, 1.2f, 1.2f));

        sendReformEvent(entity.getNode(), reformEvent);

        expect.that(entity.getPose()).isEqualTo(expectedPose);
        expect.that(entity.getScale()).isEqualTo(expectedScale);
    }

    @Test
    public void setSizeOnMovableComponent_setsSizeOnNodeReformOptions() {
        Entity entity = createTestEntity();
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));

        movableComponent.setSize(new Dimensions(2f, 2f, 2f));
        assertThat(options.getCurrentSize().x).isEqualTo(2f);
        assertThat(options.getCurrentSize().y).isEqualTo(2f);
        assertThat(options.getCurrentSize().z).isEqualTo(2f);
    }

    @Test
    public void scaleWithDistanceOnMovableComponent_defaultsToDefaultMode() {
        Entity entity = createTestEntity();
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);

        // Default value for scaleWithDistanceMode is DEFAULT.
        assertThat(movableComponent.getScaleWithDistanceMode())
                .isEqualTo(MovableComponent.ScaleWithDistanceMode.DEFAULT);
        assertThat(entity.addComponent(movableComponent)).isTrue();
        assertThat(
                        mNodeRepository
                                .getReformOptions(getEntityNode(entity))
                                .getScaleWithDistanceMode())
                .isEqualTo(ReformOptions.SCALE_WITH_DISTANCE_MODE_DEFAULT);
    }

    @Test
    public void setScaleWithDistanceOnMovableComponent_setsScaleWithDistanceOnNodeReformOptions() {
        Entity entity = createTestEntity();
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(entity.addComponent(movableComponent)).isTrue();

        movableComponent.setScaleWithDistanceMode(MovableComponent.ScaleWithDistanceMode.DMM);

        assertThat(movableComponent.getScaleWithDistanceMode())
                .isEqualTo(MovableComponent.ScaleWithDistanceMode.DMM);
        assertThat(
                        mNodeRepository
                                .getReformOptions(getEntityNode(entity))
                                .getScaleWithDistanceMode())
                .isEqualTo(ReformOptions.SCALE_WITH_DISTANCE_MODE_DMM);
    }

    @Test
    public void setPropertiesOnMovableComponentAttachLater_setsPropertiesOnNodeReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        movableComponent.setSize(new Dimensions(2f, 2f, 2f));
        MoveEventListener mockMoveEventListener = mock(MoveEventListener.class);
        movableComponent.addMoveEventListener(directExecutor(), mockMoveEventListener);
        assertThat(movableComponent.mReformEventConsumer).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        assertThat(options.getCurrentSize().x).isEqualTo(2f);
        assertThat(options.getCurrentSize().y).isEqualTo(2f);
        assertThat(options.getCurrentSize().z).isEqualTo(2f);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();
        assertThat(entity.mReformEventConsumerMap).isNotEmpty();
    }

    @Test
    public void addMoveEventListener_onlyInvokedOnMoveEvent() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        Vector3 initialTranslation = new Vector3(1f, 2f, 3f);
        Vector3 initialScale = new Vector3(1.1f, 1.1f, 1.1f);
        entity.setPose(new Pose(initialTranslation, new Quaternion()));
        entity.setScale(initialScale);
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());
        MoveEventListener mockMoveEventListener = mock(MoveEventListener.class);

        movableComponent.addMoveEventListener(directExecutor(), mockMoveEventListener);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();
        assertThat(entity.mReformEventConsumerMap).isNotEmpty();

        ReformEvent resizeReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE,
                        /* state= */ REFORM_STATE_START,
                        /* id= */ 0);

        sendReformEvent(entity.getNode(), resizeReformEvent);
        verify(mockMoveEventListener, never()).onMoveEvent(any());

        ReformEvent moveReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(entity.getNode(), moveReformEvent);
        ArgumentCaptor<MoveEvent> moveEventCaptor = ArgumentCaptor.forClass(MoveEvent.class);
        verify(mockMoveEventListener).onMoveEvent(moveEventCaptor.capture());
        List<MoveEvent> capturedEvents = moveEventCaptor.getAllValues();
        MoveEvent moveEvent = capturedEvents.get(0);
        assertThat(moveEvent.getPreviousPose().getTranslation()).isEqualTo(initialTranslation);
        assertThat(moveEvent.getPreviousScale()).isEqualTo(initialScale);
    }

    @Test
    public void addMoveEventListenerWithExecutor_invokesListenerOnGivenExecutor() {
        Entity entity = createTestEntity();
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        MoveEventListener mockMoveEventListener = mock(MoveEventListener.class);
        MoveEventListener mockMoveEventListener2 = mock(MoveEventListener.class);
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        movableComponent.addMoveEventListener(executorService, mockMoveEventListener);
        movableComponent.addMoveEventListener(directExecutor(), mockMoveEventListener2);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();
        verify(mockMoveEventListener).onMoveEvent(any());
        verify(mockMoveEventListener2).onMoveEvent(any());
    }

    @Test
    public void addMoveEventListenerMultiple_invokesAllListeners() {
        Entity entity = createTestEntity();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(entity.addComponent(movableComponent)).isTrue();
        MoveEventListener mockMoveEventListener1 = mock(MoveEventListener.class);
        MoveEventListener mockMoveEventListener2 = mock(MoveEventListener.class);
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        movableComponent.addMoveEventListener(executorService, mockMoveEventListener1);
        movableComponent.addMoveEventListener(executorService, mockMoveEventListener2);
        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        verify(mockMoveEventListener1).onMoveEvent(any());
        verify(mockMoveEventListener2).onMoveEvent(any());
    }

    @Test
    public void removeMoveEventListenerMultiple_removesGivenListener() {
        Entity entity = createTestEntity();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(entity.addComponent(movableComponent)).isTrue();
        MoveEventListener mockMoveEventListener1 = mock(MoveEventListener.class);
        MoveEventListener mockMoveEventListener2 = mock(MoveEventListener.class);
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        movableComponent.addMoveEventListener(executorService, mockMoveEventListener1);
        movableComponent.addMoveEventListener(executorService, mockMoveEventListener2);
        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), reformEvent);
        executorService.runAll();

        // Verify both listeners are invoked.
        verify(mockMoveEventListener1).onMoveEvent(any());
        verify(mockMoveEventListener2).onMoveEvent(any());

        movableComponent.removeMoveEventListener(mockMoveEventListener1);
        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // The first listener, which we removed, should not be invoked again.
        verify(mockMoveEventListener1).onMoveEvent(any());
        verify(mockMoveEventListener2, times(2)).onMoveEvent(any());
    }

    @Test
    public void removeMovableComponent_clearsMoveReformOptionsAndMoveEventListeners() {
        Entity entity = createTestEntity();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        MoveEventListener mockMoveEventListener = mock(MoveEventListener.class);

        movableComponent.addMoveEventListener(directExecutor(), mockMoveEventListener);
        assertThat(movableComponent.mReformEventConsumer).isNotNull();
        assertThat(((AndroidXrEntity) entity).mReformEventConsumerMap).isNotEmpty();

        entity.removeComponent(movableComponent);
        assertThat(mNodeRepository.getReformOptions(getEntityNode(entity))).isNull();
        assertThat(movableComponent.mReformEventConsumer).isNull();
        assertThat(((AndroidXrEntity) entity).mReformEventConsumerMap).isEmpty();
    }

    @Test
    public void movableComponent_canAttachAgainAfterDetach() {
        Entity entity = createTestEntity();
        assertThat(entity).isNotNull();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* anchorPlacement= */ ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        entity.removeComponent(movableComponent);
        assertThat(entity.addComponent(movableComponent)).isTrue();
    }

    @Test
    public void anchorable_updatesThePoseBasedOnPlanes() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.HORIZONTAL_UPWARD_FACING.intValue,
                        Plane.Label.FLOOR.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        Entity entity = createTestEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ false,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(0);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        // Put the proposed position at 1  above the origin. so it would need to move up 1 unit to
        // be on the plane.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1f, 1f, 1f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // The expected position should be 3 unit above the activity in order to rest on the plane.
        // It
        // is 3 units because the activity space is 1 unit below of the origin and the plane is 2
        // units
        // above it.
        Pose expectedPosition = new Pose(new Vector3(1f, 3f, 1f), new Quaternion(0f, 0f, 0f, 1f));
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        assertThat(moveEventListener.mCallCount).isEqualTo(1);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent()).isNull();

        // The panel shadow renderer should have no interaction.
        verify(mPanelShadowRenderer, never()).updatePanelPose(any(), any(), any());
        verify(mPanelShadowRenderer, never()).destroy();
        verify(mPanelShadowRenderer, never()).hidePlane();

        // The pose should have moved since the systemMovable is true.
        assertPose(entity.getPose(), expectedPosition);
    }

    @Test
    public void anchorable_nullParent_updatesThePoseBasedOnPlanes() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.HORIZONTAL_UPWARD_FACING.intValue,
                        Plane.Label.FLOOR.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        Entity entity = createTestEntity();
        entity.setParent(null);
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ false,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(0);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        // Put the proposed position at 1  above the origin. so it would need to move up 1 unit to
        // be on the plane.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1f, 1f, 1f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // The expected position should be 3 unit above the activity in order to rest on the plane.
        // It
        // is 3 units because the activity space is 1 unit below of the origin and the plane is 2
        // units
        // above it.
        Pose expectedPosition = new Pose(new Vector3(1f, 3f, 1f), new Quaternion(0f, 0f, 0f, 1f));
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        assertThat(moveEventListener.mCallCount).isEqualTo(1);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent()).isNull();

        // The pose should have moved since the systemMovable is true.
        assertPose(entity.getPose(), expectedPosition);
    }

    @Test
    public void anchorable_updatesPoseButDoesNotMove_ifNotSystemMovable() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.HORIZONTAL_UPWARD_FACING.intValue,
                        Plane.Label.FLOOR.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        Entity entity = createTestEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ false,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        // Put the proposed position at 1  above the origin. so it would need to move up 1 unit to
        // be on the plane.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1f, 1f, 1f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // The expected position should be 3 unit above the activity in order to rest on the plane.
        // It
        // is 3 units because the activity space is 1 unit below of the origin and the plane is 2
        // units
        // above it.
        Pose expectedPosition = new Pose(new Vector3(1f, 3f, 1f), new Quaternion(0f, 0f, 0f, 1f));

        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent()).isNull();

        // The panel shadow renderer should have no interaction if it is not system movable.
        verify(mPanelShadowRenderer, never()).updatePanelPose(any(), any(), any());
        verify(mPanelShadowRenderer, never()).destroy();
        verify(mPanelShadowRenderer, never()).hidePlane();

        // The pose should not have moved since the systemMovable is false.
        assertPose(entity.getPose(), Pose.Identity);
    }

    @Test
    public void anchorable_withNonActivityParent_updatesPoseBasedOnPlanesAndParent() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.HORIZONTAL_UPWARD_FACING.intValue,
                        Plane.Label.FLOOR.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        // Create a parent entity whose pose is below the activity space pose.
        Entity parentEntity = createTestEntity();
        parentEntity.setPose(new Pose(new Vector3(0f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)));
        PanelEntity entity = createTestPanelEntity();
        entity.setParent(parentEntity);
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        // Put the proposed position at 1  above the origin. so it would need to move up 1 unit to
        // be on the plane.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1f, 1f, 1f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // The expected position should be 3 unit above the activity in order to rest on the plane.
        // It
        // is 3 units because the activity space is 1 unit below of the origin and the plane is 2
        // units
        // above it. Since the parent is 1 unit below the activity space, the expected position
        // should
        // be 4 units above the parent.
        Pose expectedPosition = new Pose(new Vector3(1f, 4f, 1f), new Quaternion(0f, 0f, 0f, 1f));
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        assertThat(moveEventListener.mCallCount).isEqualTo(1);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent()).isNull();

        // The pose should have moved since the systemMovable is true.
        assertPose(entity.getPose(), expectedPosition);
    }

    @Test
    public void anchorableAndScaledParent_updatesThePoseBasedOnPlanes() {
        // Set the activity space pose to be 1 unit to the left of the origin. with a scale of 2.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 2f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.HORIZONTAL_UPWARD_FACING.intValue,
                        Plane.Label.FLOOR.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        // Put the proposed position at 1  above the origin. so it would need to move up 1 unit to
        // be on the plane. This needs to be divided by the scale of the activity space.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(.5f, .5f, .5f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // The expected position should be 3 unit above the activity in order to rest on the plane.
        // It
        // is 1.5 units because the activity space is 1 unit below of the origin and the plane is 2
        // units above it and the activity space is scaled by 2.
        Pose expectedPosition =
                new Pose(new Vector3(.5f, 1.5f, .5f), new Quaternion(0f, 0f, 0f, 1f));
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        assertThat(moveEventListener.mCallCount).isEqualTo(1);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent()).isNull();
    }

    @Test
    public void anchorable_withinAnchorDistance_setsAnchorEntity() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        Anchor anchor = mock(Anchor.class);
        IBinder sharedAnchorToken = Mockito.mock(IBinder.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.createAnchor(any(), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.VERTICAL.intValue,
                        Plane.Label.WALL.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        // Add the move event listener and the anchored event listener.
        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), reformEvent);

        reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 1  above the origin. so it would need to move up 1 unit to
        // be on the plane.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1f, 1f, 1f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor which
        // is
        // (1, 3, 0) relative to the activity space. which results in an updated pose of (0, 0, 1)
        // relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion represents a 90 degree
        // rotation around the x-axis Which is expected when the panel is rotated into the plane's
        // reference space.
        Pose expectedPosition =
                new Pose(new Vector3(0f, 0f, 1f), new Quaternion(-0.707f, 0f, 0f, 0.707f));

        assertThat(moveEventListener.mCallCount).isEqualTo(2);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        // Check that the anchor entity was set.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isInstanceOf(AnchorEntity.class);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());
    }

    @Test
    public void anchorable_withinAnchorDistanceAboveAnchor_resetsPose() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        Anchor anchor = mock(Anchor.class);
        IBinder sharedAnchorToken = Mockito.mock(IBinder.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.createAnchor(any(), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.VERTICAL.intValue,
                        Plane.Label.WALL.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        // Add the move event listener and the anchored event listener.
        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), reformEvent);

        reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 2 + half the MIN_PLANE_ANCHOR_DISTANCE above the origin. So
        // it
        // would be right above the plane.
        ShadowReformEvent.extract(reformEvent)
                .setProposedPosition(
                        new Vec3(1f, 3f + MovableComponentImpl.MIN_PLANE_ANCHOR_DISTANCE / 2f, 1f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor which
        // is
        // (1, 3, 0) relative to the activity space. which results in an updated pose of (0, 0, 1)
        // relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion represents a 90 degree
        // rotation around the x-axis Which is expected when the panel is rotated into the plane's
        // reference space.
        Pose expectedPosition =
                new Pose(new Vector3(0f, 0f, 1f), new Quaternion(-0.707f, 0f, 0f, 0.707f));

        assertThat(moveEventListener.mCallCount).isEqualTo(2);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        // Check that the anchor entity was set.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isInstanceOf(AnchorEntity.class);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());
    }

    @Test
    public void anchorable_withIncorrectPlaneType_doesNotCreateAnchor() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        Anchor anchor = mock(Anchor.class);
        IBinder sharedAnchorToken = Mockito.mock(IBinder.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.createAnchor(any(), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.VERTICAL.intValue,
                        Plane.Label.WALL.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        // Set the anchor placement to be a table.
        AnchorPlacement anchorPlacement =
                mFakeRuntime.createAnchorPlacementForPlanes(
                        ImmutableSet.of(PlaneType.ANY), ImmutableSet.of(PlaneSemantic.TABLE));
        ImmutableSet<AnchorPlacement> anchorPlacementSet = ImmutableSet.of(anchorPlacement);

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        anchorPlacementSet,
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        // Add the move event listener and the anchored event listener.
        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        // Put the proposed position at 1  above the origin. so it would need to move up 1 unit to
        // be on the plane.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1f, 1f, 1f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // The expected position should be 3 unit above the activity in order to rest on the plane.
        // It
        // is 3 units because the activity space is 1 unit below of the origin and the plane is 2
        // units
        // above it. However, since the plane is not a table plane, the anchor should not be
        // created.
        Pose expectedPosition = new Pose(new Vector3(1f, 3f, 1f), new Quaternion(0f, 0f, 0f, 1f));
        assertThat(moveEventListener.mCallCount).isEqualTo(1);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        // Check that the anchor entity was not set.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent()).isNull();
    }

    @Test
    public void anchorable_withinAnchorDistanceAndScale_setsAnchorEntityAndScales() {
        // Set the activity space pose to be 1 unit to the left of the OpenXR origin and add a scale
        // of
        // 2.
        float activityScale = 2f;
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), activityScale);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        Anchor anchor = mock(Anchor.class);
        IBinder sharedAnchorToken = Mockito.mock(IBinder.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.createAnchor(any(), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.VERTICAL.intValue,
                        Plane.Label.WALL.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        Vector3 entityScale = new Vector3(1f, 3f, 5f);
        PanelEntity entity = createTestPanelEntity();
        entity.setScale(entityScale);
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        // Add the move event listener and the anchored event listener.
        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), reformEvent);

        reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 1  above the origin. so it would need to move up 1 unit to
        // be on the plane. This needs to be divided by the scale of the activity space.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(.5f, .5f, .5f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor which
        // is
        // (1, 3, 0) relative to the activity space. which results in an updated pose of (0, 0, 1)
        // relative to the anchor. The (-0.707f, 0f, 0f, 0.707f) Quaternion represents a 90 degree
        // rotation around the x-axis. Which is expected when the panel is rotated into the plane's
        // reference space.
        Pose expectedPosition =
                new Pose(new Vector3(0f, 0f, 1f), new Quaternion(-0.707f, 0f, 0f, 0.707f));

        assertThat(moveEventListener.mCallCount).isEqualTo(2);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        // Check that the anchor entity was set.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isInstanceOf(AnchorEntity.class);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());
        assertVector3(entity.getWorldSpaceScale(), entityScale.times(activityScale));
    }

    @Test
    public void anchorable_noPlanes_keepsProposedPose() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of());

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1f, 1f, 1f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // The expected position should be unchanged from the proposed event
        Pose expectedPosition = new Pose(new Vector3(1f, 1f, 1f), new Quaternion(0f, 0f, 0f, 1f));

        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        assertThat(moveEventListener.mCallCount).isEqualTo(1);
    }

    @Test
    public void anchorable_noPlaneData_keepsProposedPose() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        Plane plane = mock(Plane.class);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.getData(any())).thenReturn(null);

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1f, 1f, 1f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // The expected position should be unchanged from the proposed event
        Pose expectedPosition = new Pose(new Vector3(1f, 1f, 1f), new Quaternion(0f, 0f, 0f, 1f));

        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        assertThat(moveEventListener.mCallCount).isEqualTo(1);
        assertThat(moveEventListener.mCallCount).isEqualTo(1);
    }

    @Test
    public void anchorable_outsideExtents_keepsProposedPose() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        Plane plane = mock(Plane.class);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(5f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.HORIZONTAL_UPWARD_FACING.intValue,
                        Plane.Label.FLOOR.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1f, 1f, 1f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // The expected position should be unchanged from the proposed event
        Pose expectedPosition = new Pose(new Vector3(1f, 1f, 1f), new Quaternion(0f, 0f, 0f, 1f));

        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        assertThat(moveEventListener.mCallCount).isEqualTo(1);
        assertThat(moveEventListener.mCallCount).isEqualTo(1);
    }

    @Test
    public void anchorable_resetsToActivityPoseAfterAnchoring() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        Anchor anchor = mock(Anchor.class);
        IBinder sharedAnchorToken = Mockito.mock(IBinder.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.createAnchor(any(), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.VERTICAL.intValue,
                        Plane.Label.WALL.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        // Add the move event listener and the anchored event listener.
        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), reformEvent);

        reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 1  above the origin. so it would need to move up 1 unit to
        // be on the plane.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1f, 1f, 1f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor which
        // is
        // (1, 3, 0) relative to the activity space. which results in an updated pose of (0, 0, 1)
        // relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion represents a 90 degree
        // rotation around the x-axis Which is expected when the panel is rotated into the plane's
        // reference space.
        Pose expectedPosition =
                new Pose(new Vector3(0f, 0f, 1f), new Quaternion(-0.707f, 0f, 0f, 0.707f));

        assertThat(moveEventListener.mCallCount).isEqualTo(2);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        // Check that the anchor entity was set.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isInstanceOf(AnchorEntity.class);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());

        ReformEvent secondReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 4 above the origin so it would be off the plane. It should
        // reset to the activity space pose and rotation.
        ShadowReformEvent.extract(secondReformEvent).setProposedPosition(new Vec3(1f, 4f, 1f));

        sendReformEvent(getEntityNode(entity), secondReformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away from
        // the
        // anchor and it should be reparented to the activity space.
        expectedPosition = new Pose(new Vector3(1f, 4f, 1f), new Quaternion(0f, 0f, 0f, 1f));

        assertThat(moveEventListener.mCallCount).isEqualTo(3);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        assertPose(entity.getPose(), expectedPosition);
        // Check that parent was updated to the activity space.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(mActivitySpaceImpl);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());
    }

    @Test
    public void anchorable_resetsAndScaleToActivityPoseAfterAnchoring() {
        // Set the activity space pose to be 1 unit to the left of the OpenXR origin and add a scale
        // of
        // 2.
        float activityScale = 2f;
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), activityScale);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        Anchor anchor = mock(Anchor.class);
        IBinder sharedAnchorToken = Mockito.mock(IBinder.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.createAnchor(any(), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.VERTICAL.intValue,
                        Plane.Label.WALL.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        Vector3 entityScale = new Vector3(1f, 3f, 5f);
        PanelEntity entity = createTestPanelEntity();
        entity.setScale(entityScale);
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        // Add the move event listener and the anchored event listener.
        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), reformEvent);

        reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 1  above the origin. so it would need to move up 1 unit to
        // be on the plane. This needs to be divided by the scale of the activity space.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(.5f, .5f, .5f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor which
        // is
        // (1, 3, 0) relative to the activity space. Which results in an updated pose of (0, 0, 1)
        // relative to the anchor. The (-0.707f, 0f, 0f, 0.707f) Quaternion represents a 90 degree
        // rotation around the x-axis. Which is expected when the panel is rotated into the plane's
        // reference space.
        Pose expectedPosition =
                new Pose(new Vector3(0f, 0f, 1f), new Quaternion(-0.707f, 0f, 0f, 0.707f));

        assertThat(moveEventListener.mCallCount).isEqualTo(2);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        assertPose(entity.getPose(), expectedPosition);
        assertVector3(entity.getScale(), entityScale.times(activityScale));
        // Check that the anchor entity was set.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isInstanceOf(AnchorEntity.class);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());
        assertVector3(entity.getWorldSpaceScale(), entityScale.times(activityScale));

        ReformEvent secondReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 4 above the activity space so it would be off the plane. It
        // should reset to the activity space pose and rotation.
        ShadowReformEvent.extract(secondReformEvent).setProposedPosition(new Vec3(1f, 4f, 1f));

        sendReformEvent(getEntityNode(entity), secondReformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away from
        // the
        // anchor and it should be reparented to the activity space.
        expectedPosition = new Pose(new Vector3(1f, 4f, 1f), new Quaternion(0f, 0f, 0f, 1f));

        assertThat(moveEventListener.mCallCount).isEqualTo(3);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        assertPose(entity.getPose(), expectedPosition);
        // Check that parent was updated to the activity space.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(mActivitySpaceImpl);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());
        // Check that the scale was updated to the original scale.
        assertVector3(entity.getScale(), entityScale);
    }

    @Test
    public void anchorableChildOfEntity_resetsToActivityPoseAfterAnchoring() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        Anchor anchor = mock(Anchor.class);
        IBinder sharedAnchorToken = Mockito.mock(IBinder.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.createAnchor(any(), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.VERTICAL.intValue,
                        Plane.Label.WALL.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        // Create a parent entity whose pose is below the activity space pose.
        Entity parentEntity = createTestEntity();
        parentEntity.setPose(new Pose(new Vector3(0f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)));
        PanelEntity entity = createTestPanelEntity();
        entity.setParent(parentEntity);

        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        // Add the move event listener and the anchored event listener.
        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), reformEvent);

        reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 1 above the origin. It would need to move up 1 unit to be on
        // the
        // plane.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1.0f, 1.0f, 1.0f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor which
        // is
        // (1, 3, 0) relative to the activity space. which results in an updated pose of (0, 0, 1)
        // relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion represents a 90 degree
        // rotation around the x-axis Which is expected when the panel is rotated into the plane's
        // reference space.
        Pose expectedPosition =
                new Pose(new Vector3(0f, 0f, 1f), new Quaternion(-0.707f, 0f, 0f, 0.707f));

        assertThat(moveEventListener.mCallCount).isEqualTo(2);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        // Check that the anchor entity was set.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isInstanceOf(AnchorEntity.class);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());

        final ReformEvent secondReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 4 above the origin so it would be off the plane. It should
        // reset to the activity space pose and rotation.
        ShadowReformEvent.extract(secondReformEvent).setProposedPosition(new Vec3(1f, 4f, 1f));

        sendReformEvent(getEntityNode(entity), secondReformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away from
        // the
        // anchor and it should be reparented to the activity space not the original parent..
        expectedPosition = new Pose(new Vector3(1f, 4f, 1f), new Quaternion(0f, 0f, 0f, 1f));

        assertThat(moveEventListener.mCallCount).isEqualTo(3);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        assertPose(entity.getPose(), expectedPosition);
        // Check that parent was updated to the activity space.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(mActivitySpaceImpl);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());
    }

    @Test
    public void anchorable_shouldDispose_disposesAnchorAfterUnparenting() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        Anchor anchor = mock(Anchor.class);
        IBinder sharedAnchorToken = Mockito.mock(IBinder.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.createAnchor(any(), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.VERTICAL.intValue,
                        Plane.Label.WALL.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ true,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        // Add the move event listener and the anchored event listener.
        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), reformEvent);

        reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 1  above the origin. so it would need to move up 1 unit to
        // be on the plane.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1.0f, 1.0f, 1.0f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor which
        // is
        // (1, 3, 0) relative to the activity space. which results in an updated pose of (0, 0, 1)
        // relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion represents a 90 degree
        // rotation around the x-axis Which is expected when the panel is rotated into the plane's
        // reference space.
        Pose expectedPosition =
                new Pose(new Vector3(0f, 0f, 1f), new Quaternion(-0.707f, 0f, 0f, 0.707f));

        assertThat(moveEventListener.mCallCount).isEqualTo(2);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        // Check that the anchor entity was set.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isInstanceOf(AnchorEntity.class);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());
        // Cache the anchor entity.
        Entity anchorEntity = moveEventListener.mLastMoveEvent.getUpdatedParent();

        final ReformEvent secondReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 4 above the origin so it would be off the plane. It should
        // reset to the activity space pose and rotation.
        ShadowReformEvent.extract(secondReformEvent).setProposedPosition(new Vec3(1f, 4f, 1f));

        sendReformEvent(getEntityNode(entity), secondReformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away from
        // the
        // anchor and it should be reparented to the activity space.
        expectedPosition = new Pose(new Vector3(1f, 4f, 1f), new Quaternion(0f, 0f, 0f, 1f));

        assertThat(moveEventListener.mCallCount).isEqualTo(3);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        // Check that the anchor entity was set.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(mActivitySpaceImpl);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());

        // Verify that the anchor entity was disposed by checking that it is no longer in the entity
        // manager.
        assertThat(mEntityManager.getEntityForNode(((AndroidXrEntity) anchorEntity).getNode()))
                .isNull();
    }

    @Test
    public void anchorable_shouldDispose_doeNotDisposeIfAnchorHasChildren() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        Anchor anchor = mock(Anchor.class);
        IBinder sharedAnchorToken = Mockito.mock(IBinder.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));
        when(plane.createAnchor(any(), any())).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(sharedAnchorToken);

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.VERTICAL.intValue,
                        Plane.Label.WALL.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ true,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        TestMoveEventListener moveEventListener = new TestMoveEventListener();
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        // Add the move event listener and the anchored event listener.
        movableComponent.addMoveEventListener(executorService, moveEventListener);
        // The reform options for parenting and moving should not be set when it is anchorable.
        assertThat(options.getFlags()).isEqualTo(ReformOptions.FLAG_SCALE_WITH_DISTANCE);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), reformEvent);

        reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 1  above the origin. so it would need to move up 1 unit to
        // be on the plane.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1.0f, 1.0f, 1.0f));

        sendReformEvent(getEntityNode(entity), reformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving from (1, 3, 1) relative to the activity space to be relative to the anchor which
        // is
        // (1, 3, 0) relative to the activity space. which results in an updated pose of (0, 0, 1)
        // relative to the anchor.The (-0.707f, 0f, 0f, 0.707f) Quaternion represents a 90 degree
        // rotation around the x-axis Which is expected when the panel is rotated into the plane's
        // reference space.
        Pose expectedPosition =
                new Pose(new Vector3(0f, 0f, 1f), new Quaternion(-0.707f, 0f, 0f, 0.707f));

        assertThat(moveEventListener.mCallCount).isEqualTo(2);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        // Check that the anchor entity was set.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isInstanceOf(AnchorEntity.class);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());
        // Cache the anchor entity.
        Entity anchorEntity = moveEventListener.mLastMoveEvent.getUpdatedParent();

        Entity child = createTestEntity();
        anchorEntity.addChild(child);

        final ReformEvent secondReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Put the proposed position at 4 above the origin so it would be off the plane. It should
        // reset to the activity space pose and rotation.
        ShadowReformEvent.extract(secondReformEvent)
                .setProposedPosition(new Vec3(1.0f, 4.0f, 1.0f));
        sendReformEvent(getEntityNode(entity), secondReformEvent);
        assertThat(executorService.hasNext()).isTrue();
        executorService.runAll();

        // Moving to (1, 4, 1) relative to the activity space. This should pull the entity away from
        // the
        // anchor and it should be reparented to the activity space.
        expectedPosition = new Pose(new Vector3(1f, 4f, 1f), new Quaternion(0f, 0f, 0f, 1f));

        assertThat(moveEventListener.mCallCount).isEqualTo(3);
        assertPose(moveEventListener.mLastMoveEvent.getCurrentPose(), expectedPosition);
        // Check that the anchor entity was set.
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(mActivitySpaceImpl);
        assertThat(moveEventListener.mLastMoveEvent.getUpdatedParent())
                .isEqualTo(entity.getParent());

        // Verify that the anchor entity wasn't disposed by checking that it is in the entity
        // manager.
        assertThat(mEntityManager.getEntityForNode(((AndroidXrEntity) anchorEntity).getNode()))
                .isEqualTo(anchorEntity);
    }

    @Test
    public void anchorablePanelEntity_nearPlane_rendersShadow() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.HORIZONTAL_UPWARD_FACING.intValue,
                        Plane.Label.FLOOR.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ false,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE,
                        /* state= */ REFORM_STATE_ONGOING,
                        /* id= */ 0);

        // Put the proposed position at 1 above the origin. so it would need to move up 1 unit to
        // be on the plane.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1.0f, 1.0f, 1.0f));

        sendReformEvent(getEntityNode(entity), reformEvent);

        // Since it is by the plane a call should be made to the panel shadow renderer.
        verify(mPanelShadowRenderer)
                .updatePanelPose(
                        new Pose(new Vector3(0f, 2f, 1f), new Quaternion(0f, 0f, 0f, 1f)),
                        RuntimeUtils.fromPerceptionPose(perceptionPose),
                        (PanelEntityImpl) entity);
    }

    @Test
    public void anchorablePanelEntity_awayFromPlane_hidesShadow() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.HORIZONTAL_UPWARD_FACING.intValue,
                        Plane.Label.FLOOR.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ false,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE,
                        /* state= */ REFORM_STATE_ONGOING,
                        /* id= */ 0);

        // Put the proposed position at 5 above the origin. so it is far away from the plane.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1.0f, 5.0f, 1.0f));

        sendReformEvent(getEntityNode(entity), reformEvent);

        // Since it is by the plane a call should be made to the panel shadow renderer.
        verify(mPanelShadowRenderer).hidePlane();
    }

    @Test
    public void anchorablePanelEntity_endMovement_callsDestroy() {
        // Set the activity space pose to be 1 unit to the left of the origin.
        setActivitySpacePose(
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f)), 1f);
        Session session = Mockito.mock(Session.class);
        Plane plane = mock(Plane.class);
        when(mPerceptionLibrary.getSession()).thenReturn(session);
        when(session.getAllPlanes()).thenReturn(ImmutableList.of(plane));

        // Create a perception plane that is 2 units above the origin.
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(0f, 2f, 0f, 0f, 0f, 0f, 1f);
        PlaneData planeData =
                new PlaneData(
                        perceptionPose,
                        1f,
                        1f,
                        Plane.Type.HORIZONTAL_UPWARD_FACING.intValue,
                        Plane.Label.FLOOR.intValue);
        when(plane.getData(any())).thenReturn(planeData);

        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ false,
                        createAnyAnchorPlacement(),
                        /* shouldDisposeParentAnchor= */ false,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        // Set the reform state to end so that the plane shadow gets destroyed.
        ShadowReformEvent.extract(reformEvent).setProposedPosition(new Vec3(1.0f, 1.0f, 1.0f));

        sendReformEvent(getEntityNode(entity), reformEvent);

        // Since it is by the plane a call should be made to the panel shadow renderer.
        verify(mPanelShadowRenderer).destroy();
    }

    static class TestMoveEventListener implements MoveEventListener {
        int mCallCount = 0;
        MoveEvent mLastMoveEvent = null;

        @Override
        public void onMoveEvent(@NonNull MoveEvent event) {
            mLastMoveEvent = event;
            mCallCount++;
        }
    }
}
