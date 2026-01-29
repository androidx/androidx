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

import static com.android.extensions.xr.node.ReformEvent.REFORM_STATE_END;
import static com.android.extensions.xr.node.ReformEvent.REFORM_STATE_ONGOING;
import static com.android.extensions.xr.node.ReformEvent.REFORM_STATE_START;
import static com.android.extensions.xr.node.ReformEvent.REFORM_TYPE_MOVE;
import static com.android.extensions.xr.node.ReformEvent.REFORM_TYPE_RESIZE;
import static com.android.extensions.xr.node.ReformOptions.ALLOW_MOVE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.test.rule.GrantPermissionRule;
import androidx.xr.runtime.NodeHolder;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.runtime.testing.FakeSpatialApiVersionProvider;
import androidx.xr.scenecore.runtime.AnchorPlacement;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.GltfEntity;
import androidx.xr.scenecore.runtime.GltfFeature;
import androidx.xr.scenecore.runtime.MovableComponent;
import androidx.xr.scenecore.runtime.MoveEvent;
import androidx.xr.scenecore.runtime.MoveEventListener;
import androidx.xr.scenecore.runtime.PanelEntity;
import androidx.xr.scenecore.runtime.PixelDimensions;
import androidx.xr.scenecore.runtime.PlaneSemantic;
import androidx.xr.scenecore.runtime.PlaneType;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeGltfFeature;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.InputEvent;
import com.android.extensions.xr.node.Mat4f;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeRepository;
import com.android.extensions.xr.node.NodeTransform;
import com.android.extensions.xr.node.Quatf;
import com.android.extensions.xr.node.ReformEvent;
import com.android.extensions.xr.node.ReformOptions;
import com.android.extensions.xr.node.ShadowInputEvent;
import com.android.extensions.xr.node.ShadowNode;
import com.android.extensions.xr.node.ShadowNodeTransform;
import com.android.extensions.xr.node.ShadowReformEvent;
import com.android.extensions.xr.node.Vec3;

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
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public class MovableComponentImplTest {

    @Rule public final Expect expect = Expect.create();
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final EntityManager mEntityManager = new EntityManager();
    private final PanelShadowRenderer mPanelShadowRenderer =
            Mockito.mock(PanelShadowRenderer.class);
    private final NodeRepository mNodeRepository = NodeRepository.getInstance();

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant("android.permission.SCENE_UNDERSTANDING");

    private SpatialSceneRuntime mFakeRuntime;
    private ActivitySpaceImpl mActivitySpaceImpl;
    private Node mActivitySpaceNode;
    private final GltfFeature mMockGltfFeature = Mockito.mock(GltfFeature.class);

    @Before
    public void setUp() {
        FakeSpatialApiVersionProvider.Companion.setTestSpatialApiVersion(1);
        mFakeRuntime =
                SpatialSceneRuntime.create(
                        mActivity,
                        mFakeExecutor,
                        mXrExtensions,
                        mEntityManager,
                        /* unscaledGravityAlignedActivitySpace= */ false);
        mActivitySpaceImpl = (ActivitySpaceImpl) mFakeRuntime.getActivitySpace();
        mActivitySpaceNode = mActivitySpaceImpl.mNode;
    }

    @After
    public void tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        mFakeRuntime.destroy();
        FakeSpatialApiVersionProvider.Companion.setTestSpatialApiVersion(null);
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
        return ((AndroidXrEntity) entity).mNode;
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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
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

    private GltfEntityImpl createGltfEntity(Activity activity) {
        NodeHolder<?> nodeHolder = new NodeHolder<>(mXrExtensions.createNode(), Node.class);
        GltfFeature fakeGltfFeature =
                FakeGltfFeature.Companion.createWithMockFeature(mMockGltfFeature, nodeHolder);

        return new GltfEntityImpl(
                activity,
                fakeGltfFeature,
                mActivitySpaceImpl,
                mXrExtensions,
                mEntityManager,
                mFakeExecutor);
    }

    @Test
    public void addSystemMovableComponentToGltfEntity_returnsTrue() {
        GltfEntity entity = createGltfEntity(mActivity);
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ false,
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();

        assertThat(entity.addComponent(movableComponent)).isTrue();
    }

    @Test
    public void addCustomMovableComponentToGltfEntity_returnsTrue() {
        GltfEntity entity = createGltfEntity(mActivity);
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ false,
                        /* scaleInZ= */ false,
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();

        assertThat(entity.addComponent(movableComponent)).isTrue();
    }

    @Test
    public void addAnchorableMovableComponentToGltfEntity_returnsTrue() {
        GltfEntity entity = createGltfEntity(mActivity);
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ false,
                        /* userAnchorable= */ true,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();

        assertThat(entity.addComponent(movableComponent)).isTrue();
    }

    @Test
    public void addMovableComponentToGltfEntity_ReparentsGltfEntity() {
        GltfEntity gltfEntity = createGltfEntity(mActivity);
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        true,
                        false,
                        false,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();

        AndroidXrEntity entity = (AndroidXrEntity) gltfEntity;
        // Cache parent before adding the component
        Node parentBefore = mNodeRepository.getParent(entity.mNode);
        // Assert the component has be added.
        assertThat(gltfEntity.addComponent(movableComponent)).isTrue();
        // Get parent after adding the component
        Node parentAfter = mNodeRepository.getParent(entity.mNode);
        // Assert it has been reparented by Impress.
        assertThat(parentBefore != parentAfter).isTrue();
    }

    @Test
    public void movableComponentAttachedToGltf_propagatesInputEvents() {
        GltfEntity gltfEntity = createGltfEntity(mActivity);
        AtomicReference<MoveEvent> moveEvent = new AtomicReference<>(null);
        AtomicInteger moveEventCounter = new AtomicInteger(0);
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        false,
                        false,
                        false,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        MoveEventListener eventListener =
                event -> {
                    moveEvent.set(event);
                    moveEventCounter.incrementAndGet();
                };
        movableComponent.addMoveEventListener(eventListener);

        InputEvent inputEvent =
                ShadowInputEvent.create(
                        InputEvent.SOURCE_UNKNOWN,
                        InputEvent.POINTER_TYPE_DEFAULT,
                        /* timestamp */ 0,
                        new Vec3(0, 0, 0),
                        new Vec3(1, 1, 1),
                        InputEvent.DISPATCH_FLAG_NONE,
                        InputEvent.ACTION_DOWN);
        AndroidXrEntity entity = (AndroidXrEntity) gltfEntity;
        ShadowNode shadowNode = ShadowNode.extract(entity.mNode);
        assertThat(gltfEntity.addComponent(movableComponent)).isTrue();

        assertThat(shadowNode.getInputListener()).isNotNull();
        assertThat(shadowNode.getInputExecutor()).isEqualTo(mFakeExecutor);
        shadowNode
                .getInputExecutor()
                .execute(() -> shadowNode.getInputListener().accept(inputEvent));

        mFakeExecutor.runAll();
        assertThat(moveEventCounter.get() == 1).isTrue();
        assertThat(moveEvent.get()).isNotNull();
    }

    @Test
    public void addMovableComponent_addsSystemMovableFlagToNode() {
        Entity entity = createTestEntity();
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ false,
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
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

        sendReformEvent(entity.mNode, reformEvent);

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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
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

        sendReformEvent(entity.mNode, reformEvent);

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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        movableComponent.setSize(new Dimensions(2f, 2f, 2f));
        MoveEventListener mockMoveEventListener = mock(MoveEventListener.class);
        movableComponent.addMoveEventListener(directExecutor(), mockMoveEventListener);
        assertThat(movableComponent.mReformEventConsumer).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(entity.mNode);

        assertThat(options.getCurrentSize().x).isEqualTo(2f);
        assertThat(options.getCurrentSize().y).isEqualTo(2f);
        assertThat(options.getCurrentSize().z).isEqualTo(2f);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();
        assertThat(entity.getReformEventConsumerMap()).isNotEmpty();
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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(entity.mNode);
        MoveEventListener mockMoveEventListener = mock(MoveEventListener.class);

        movableComponent.addMoveEventListener(directExecutor(), mockMoveEventListener);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();
        assertThat(entity.getReformEventConsumerMap()).isNotEmpty();

        ReformEvent resizeReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE,
                        /* state= */ REFORM_STATE_START,
                        /* id= */ 0);

        sendReformEvent(entity.mNode, resizeReformEvent);
        verify(mockMoveEventListener, never()).onMoveEvent(any());

        ReformEvent moveReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(entity.mNode, moveReformEvent);
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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
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
    public void addMoveEventListenerOnDefaultExecutor_invokesListenerOnDefaultExecutor() {
        Entity entity = createTestEntity();
        MovableComponent movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(getEntityNode(entity));
        MoveEventListener mockMoveEventListener = mock(MoveEventListener.class);

        movableComponent.addMoveEventListener(mockMoveEventListener);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent reformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), reformEvent);
        verify(mockMoveEventListener).onMoveEvent(any());
    }

    @Test
    public void removeMoveEventListenerMultiple_removesGivenListener() {
        Entity entity = createTestEntity();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
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
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        MoveEventListener mockMoveEventListener = mock(MoveEventListener.class);

        movableComponent.addMoveEventListener(directExecutor(), mockMoveEventListener);
        assertThat(movableComponent.mReformEventConsumer).isNotNull();
        assertThat(((AndroidXrEntity) entity).getReformEventConsumerMap()).isNotEmpty();

        entity.removeComponent(movableComponent);
        assertThat(mNodeRepository.getReformOptions(getEntityNode(entity))).isNull();
        assertThat(((AndroidXrEntity) entity).getReformEventConsumerMap()).isEmpty();
    }

    @Test
    public void movableComponent_canAttachAgainAfterDetach() {
        Entity entity = createTestEntity();
        assertThat(entity).isNotNull();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ true,
                        /* userAnchorable= */ false,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();
        entity.removeComponent(movableComponent);
        assertThat(entity.addComponent(movableComponent)).isTrue();
    }

    @Test
    public void anchorableComponentMoving_sendMoveEvent_rendersShadow() {
        // Set the activity space pose to be 1 unit down and to the left of the origin.
        Pose activitySpacePose =
                new Pose(new Vector3(-1f, -1f, 0f), new Quaternion(0f, 0f, 0f, 1f));
        setActivitySpacePose(activitySpacePose, 1f);
        PanelEntity entity = createTestPanelEntity();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ false,
                        /* userAnchorable= */ true,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();

        MoveEventListener moveEventListener = new TestMoveEventListener(movableComponent);
        movableComponent.addMoveEventListener(directExecutor(), moveEventListener);

        ReformEvent moveStartReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);
        ReformEvent moveOngoingReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE,
                        /* state= */ REFORM_STATE_ONGOING,
                        /* id= */ 0);

        Pose proposedPoseInActivitySpace =
                new Pose(new Vector3(1.0f, 1.0f, 1.0f), new Quaternion(0.0f, 0.0f, 0.0f, 1.0f));
        Vec3 proposedPosition =
                new Vec3(
                        proposedPoseInActivitySpace.getTranslation().getX(),
                        proposedPoseInActivitySpace.getTranslation().getY(),
                        proposedPoseInActivitySpace.getTranslation().getZ());
        Pose proposedPoseInOxr =
                proposedPoseInActivitySpace.translate(activitySpacePose.getTranslation());
        Pose expectedPlanePoseInOxr = proposedPoseInOxr.translate(new Vector3(0.0f, 1.0f, 0.0f));

        // Put the proposed position at 1 above the origin. so it would need to move up 1 unit to
        // be on the plane.
        ShadowReformEvent.extract(moveStartReformEvent).setProposedPosition(proposedPosition);
        ShadowReformEvent.extract(moveOngoingReformEvent).setProposedPosition(proposedPosition);

        sendReformEvent(getEntityNode(entity), moveStartReformEvent);
        sendReformEvent(getEntityNode(entity), moveOngoingReformEvent);

        // Since it is by the plane a call should be made to the panel shadow renderer.
        verify(mPanelShadowRenderer)
                .updatePanelPose(
                        proposedPoseInOxr, expectedPlanePoseInOxr, (PanelEntityImpl) entity);
    }

    @Test
    public void anchorableComponentNotMoving_sendMoveEvent_hidesShadow() {
        PanelEntity entity = createTestPanelEntity();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ false,
                        /* userAnchorable= */ true,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();

        MoveEventListener moveEventListener = new TestMoveEventListener(movableComponent);
        movableComponent.addMoveEventListener(directExecutor(), moveEventListener);

        ReformEvent moveStartReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);
        ReformEvent moveOngoingReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE,
                        /* state= */ REFORM_STATE_ONGOING,
                        /* id= */ 0);
        ReformEvent moveEndReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), moveStartReformEvent);
        sendReformEvent(getEntityNode(entity), moveEndReformEvent);
        sendReformEvent(getEntityNode(entity), moveOngoingReformEvent);

        // Panel pose is not updated when the component is not in the moving state
        verify(mPanelShadowRenderer, never()).updatePanelPose(any(), any(), any());
    }

    @Test
    public void movableComponent_endMovement_hidesShadow() {
        PanelEntity entity = createTestPanelEntity();
        // Set anchorPlacement to any plane.
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        /* systemMovable= */ true,
                        /* scaleInZ= */ false,
                        /* userAnchorable= */ true,
                        mActivitySpaceImpl,
                        mPanelShadowRenderer,
                        mFakeExecutor);
        assertThat(movableComponent).isNotNull();
        assertThat(entity.addComponent(movableComponent)).isTrue();

        MoveEventListener moveEventListener = new TestMoveEventListener(movableComponent);
        movableComponent.addMoveEventListener(directExecutor(), moveEventListener);

        ReformEvent moveStartReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);
        ReformEvent moveEndReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        sendReformEvent(getEntityNode(entity), moveStartReformEvent);
        sendReformEvent(getEntityNode(entity), moveEndReformEvent);

        verify(mPanelShadowRenderer).destroy();
    }

    class TestMoveEventListener implements MoveEventListener {
        int mCallCount = 0;
        MoveEvent mLastMoveEvent = null;
        MovableComponentImpl mMovableComponent;

        TestMoveEventListener(MovableComponentImpl movableComponent) {
            mMovableComponent = movableComponent;
        }

        @Override
        public void onMoveEvent(@NonNull MoveEvent event) {
            mLastMoveEvent = event;
            mCallCount++;
            Pose currentPoseInParentSpace = event.getCurrentPose();
            Pose currentPoseInOxr =
                    event.getInitialParent()
                            .transformPoseTo(
                                    currentPoseInParentSpace,
                                    mFakeRuntime.getPerceptionSpaceActivityPose());
            Pose planePoseInOxr = currentPoseInOxr.translate(new Vector3(0.0f, 1.0f, 0.0f));
            switch (event.getMoveState()) {
                case MoveEvent.MOVE_STATE_START:
                    break;
                case MoveEvent.MOVE_STATE_ONGOING:
                    // Notify movable component that there is a plane 1 unit up from proposed pose.
                    mMovableComponent.setPlanePoseForMoveUpdatePose(
                            planePoseInOxr, currentPoseInOxr);
                    break;
                case MoveEvent.MOVE_STATE_END:
                    mMovableComponent.setPlanePoseForMoveUpdatePose(null, currentPoseInOxr);
                    break;
                default:
                    break;
            }
        }
    }
}
