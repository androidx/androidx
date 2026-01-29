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
import static com.android.extensions.xr.node.ReformEvent.REFORM_STATE_START;
import static com.android.extensions.xr.node.ReformEvent.REFORM_TYPE_MOVE;
import static com.android.extensions.xr.node.ReformEvent.REFORM_TYPE_RESIZE;
import static com.android.extensions.xr.node.ReformOptions.ALLOW_MOVE;
import static com.android.extensions.xr.node.ReformOptions.ALLOW_RESIZE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
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
import android.view.ViewGroup;

import androidx.xr.runtime.NodeHolder;
import androidx.xr.runtime.math.FloatSize2d;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.testing.FakeSpatialApiVersionProvider;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.MoveEventListener;
import androidx.xr.scenecore.runtime.PanelEntity;
import androidx.xr.scenecore.runtime.ResizeEvent;
import androidx.xr.scenecore.runtime.ResizeEventListener;
import androidx.xr.scenecore.runtime.SurfaceEntity;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeSurfaceFeature;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeRepository;
import com.android.extensions.xr.node.ReformEvent;
import com.android.extensions.xr.node.ReformOptions;
import com.android.extensions.xr.node.ShadowReformEvent;
import com.android.extensions.xr.node.Vec3;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public class ResizableComponentImplTest {

    private static final Dimensions MIN_DIMENSIONS = new Dimensions(0f, 0f, 0f);
    private static final Dimensions MAX_DIMENSIONS = new Dimensions(10f, 10f, 10f);
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final EntityManager mEntityManager = new EntityManager();
    private final PanelShadowRenderer mPanelShadowRenderer =
            Mockito.mock(PanelShadowRenderer.class);
    private final NodeRepository mNodeRepository = NodeRepository.getInstance();
    private ActivitySpaceImpl mActivitySpaceImpl;
    private SpatialSceneRuntime mFakeRuntime;

    @Before
    public void setUp() {
        assume().that(mXrExtensions).isNotNull();
        FakeSpatialApiVersionProvider.Companion.setTestSpatialApiVersion(1);
        Node activitySpaceNode = mXrExtensions.createNode();
        mActivitySpaceImpl =
                new ActivitySpaceImpl(
                        activitySpaceNode,
                        mActivity,
                        mXrExtensions,
                        mEntityManager,
                        () -> mXrExtensions.getSpatialState(mActivity),
                        /* unscaledGravityAlignedActivitySpace= */ false,
                        mFakeExecutor);
        mFakeRuntime =
                SpatialSceneRuntime.create(
                        mActivity,
                        mFakeExecutor,
                        mXrExtensions,
                        mEntityManager,
                        /* unscaledGravityAlignedActivitySpace= */ false);
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

    /**
     * Creates a generic panel entity instance for testing by creating a dummy view to insert into
     * the panel, and setting the activity space as parent.
     */
    private PanelEntity createTestPanelEntity(Pose pose, Dimensions dimensions) {
        Display display = mActivity.getSystemService(DisplayManager.class).getDisplays()[0];
        Context displayContext = mActivity.createDisplayContext(display);
        View view = new View(displayContext);
        view.setLayoutParams(new ViewGroup.LayoutParams(640, 480));
        return mFakeRuntime.createPanelEntity(
                displayContext,
                pose,
                view,
                dimensions,
                "testPanel",
                mFakeRuntime.getActivitySpace());
    }

    private SurfaceEntity createTestSurfaceEntity(SurfaceEntity.Shape shape) {
        NodeHolder<?> nodeHolder = new NodeHolder<>(mXrExtensions.createNode(), Node.class);
        SurfaceEntity surface =
                mFakeRuntime.createSurfaceEntity(
                        new FakeSurfaceFeature(nodeHolder),
                        Pose.Identity,
                        mFakeRuntime.getActivitySpace());
        surface.setShape(shape);
        return surface;
    }

    @Test
    public void addResizableComponentToTwoEntity_fails() {
        Entity entity1 = createTestEntity();
        Entity entity2 = createTestEntity();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();
        assertThat(entity1.addComponent(resizableComponent)).isTrue();
        assertThat(entity2.addComponent(resizableComponent)).isFalse();
    }

    @Test
    public void addResizableComponent_addsReformOptionsToNode() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        assertThat(options.getEnabledReform()).isEqualTo(ALLOW_RESIZE);
        assertThat(options.getMinimumSize().x).isEqualTo(MIN_DIMENSIONS.width);
        assertThat(options.getMinimumSize().y).isEqualTo(MIN_DIMENSIONS.height);
        assertThat(options.getMinimumSize().z).isEqualTo(MIN_DIMENSIONS.depth);
        assertThat(options.getMaximumSize().x).isEqualTo(MAX_DIMENSIONS.width);
        assertThat(options.getMaximumSize().y).isEqualTo(MAX_DIMENSIONS.height);
        assertThat(options.getMaximumSize().z).isEqualTo(MAX_DIMENSIONS.depth);
    }

    @Test
    public void addResizableComponentToPanel_getsPanelSize() {
        Dimensions panelSize = new Dimensions(1f, 20f, 0f);

        // case for attach without set size
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        AndroidXrEntity entity = (AndroidXrEntity) createTestPanelEntity(Pose.Identity, panelSize);
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        assertThat(options.getCurrentSize().x).isEqualTo(panelSize.width);
        assertThat(options.getCurrentSize().y).isEqualTo(panelSize.height);
        assertThat(options.getCurrentSize().z).isEqualTo(panelSize.depth);
        assertThat(resizableComponent.getSize().width).isEqualTo(panelSize.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(panelSize.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(panelSize.depth);

        entity.removeComponent(resizableComponent);

        // case for preset size
        Dimensions inputSize = new Dimensions(0f, 5f, 40f);

        resizableComponent.setSize(inputSize);
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        assertThat(options.getCurrentSize().x).isEqualTo(panelSize.width);
        assertThat(options.getCurrentSize().y).isEqualTo(panelSize.height);
        assertThat(options.getCurrentSize().z).isEqualTo(panelSize.depth);
        assertThat(resizableComponent.getSize().width).isEqualTo(panelSize.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(panelSize.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(panelSize.depth);
    }

    @Test
    public void addResizableComponentToQuadSurface_getsQuadSize() {
        Dimensions quadSize = new Dimensions(1f, 2f, 0f);

        // case for attach without set size
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        AndroidXrEntity entity =
                (AndroidXrEntity)
                        createTestSurfaceEntity(
                                new SurfaceEntity.Shape.Quad(
                                        new FloatSize2d(quadSize.width, quadSize.height)));
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        assertThat(options.getCurrentSize().x).isEqualTo(quadSize.width);
        assertThat(options.getCurrentSize().y).isEqualTo(quadSize.height);
        assertThat(options.getCurrentSize().z).isEqualTo(quadSize.depth);
        assertThat(resizableComponent.getSize().width).isEqualTo(quadSize.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(quadSize.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(quadSize.depth);

        entity.removeComponent(resizableComponent);

        // case for preset size
        Dimensions inputSize = new Dimensions(0f, 5f, 40f);

        resizableComponent.setSize(inputSize);
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        assertThat(options.getCurrentSize().x).isEqualTo(quadSize.width);
        assertThat(options.getCurrentSize().y).isEqualTo(quadSize.height);
        assertThat(options.getCurrentSize().z).isEqualTo(quadSize.depth);
        assertThat(resizableComponent.getSize().width).isEqualTo(quadSize.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(quadSize.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(quadSize.depth);
    }

    @Test
    public void addResizableComponent_setsMinMaxSizeOnNodeReformOptions() {
        Dimensions inputMin = new Dimensions(Float.NaN, 1f, 2f);
        Dimensions inputMax = new Dimensions(Float.NaN, -1f, 1f);
        Dimensions expectMin = new Dimensions(0f, 1f, 2f);
        Dimensions expectMax = new Dimensions(Float.POSITIVE_INFINITY, 1f, 2f);

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(mFakeExecutor, mXrExtensions, inputMin, inputMax);

        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());
        assertThat(options.getMinimumSize().x).isEqualTo(expectMin.width);
        assertThat(options.getMinimumSize().y).isEqualTo(expectMin.height);
        assertThat(options.getMinimumSize().z).isEqualTo(expectMin.depth);
        assertThat(options.getMaximumSize().x).isEqualTo(expectMax.width);
        assertThat(options.getMaximumSize().y).isEqualTo(expectMax.height);
        assertThat(options.getMaximumSize().z).isEqualTo(expectMax.depth);
    }

    @Test
    public void setSizeOnResizableComponent_setsSanitizedSizeOnCurrentAndReformOptions() {
        // case for valid size
        Dimensions inputSize = new Dimensions(0f, 5f, 40f);
        Dimensions expectSize = new Dimensions(0f, 5f, 40f);

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        resizableComponent.setSize(inputSize);

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        assertThat(options.getCurrentSize().x).isEqualTo(expectSize.width);
        assertThat(options.getCurrentSize().y).isEqualTo(expectSize.height);
        assertThat(options.getCurrentSize().z).isEqualTo(expectSize.depth);
        assertThat(resizableComponent.getSize().width).isEqualTo(expectSize.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(expectSize.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(expectSize.depth);

        // case for invalid(NaN) size
        inputSize = new Dimensions(Float.NaN, Float.NaN, 50f);
        expectSize = new Dimensions(0f, 5f, 50f);
        resizableComponent.setSize(inputSize);

        assertThat(options.getCurrentSize().x).isEqualTo(expectSize.width);
        assertThat(options.getCurrentSize().y).isEqualTo(expectSize.height);
        assertThat(options.getCurrentSize().z).isEqualTo(expectSize.depth);
        assertThat(resizableComponent.getSize().width).isEqualTo(expectSize.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(expectSize.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(expectSize.depth);
    }

    @Test
    public void getMinMaxSizeOnResizableComponent_returnsSanitizedInitialMinMaxSize() {
        Dimensions initMin = new Dimensions(Float.NaN, -1f, 2f);
        Dimensions initMax = new Dimensions(Float.NaN, 1f, 1f);
        Dimensions expectMin = new Dimensions(0f, 0f, 2f);
        Dimensions expectMax = new Dimensions(Float.POSITIVE_INFINITY, 1f, 2f);

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(mFakeExecutor, mXrExtensions, initMin, initMax);

        assertThat(resizableComponent.getMinimumSize().width).isEqualTo(expectMin.width);
        assertThat(resizableComponent.getMinimumSize().height).isEqualTo(expectMin.height);
        assertThat(resizableComponent.getMinimumSize().depth).isEqualTo(expectMin.depth);
        assertThat(resizableComponent.getMaximumSize().width).isEqualTo(expectMax.width);
        assertThat(resizableComponent.getMaximumSize().height).isEqualTo(expectMax.height);
        assertThat(resizableComponent.getMaximumSize().depth).isEqualTo(expectMax.depth);
    }

    @Test
    public void setMinSizeOnResizableComponent_setsSanitizedMinMaxSizeOnNodeReformOptions() {
        Dimensions initMin = new Dimensions(1f, 1f, 1f);
        Dimensions initMax = new Dimensions(10f, 10f, 10f);
        Dimensions inputMin = new Dimensions(20f, Float.NaN, -1f);
        Dimensions expectMin = new Dimensions(20f, 1f, 0f);
        Dimensions expectMax = new Dimensions(20f, 10f, 10f);

        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(mFakeExecutor, mXrExtensions, initMin, initMax);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        resizableComponent.setMinimumSize(inputMin);

        assertThat(options.getMinimumSize().x).isEqualTo(expectMin.width);
        assertThat(options.getMinimumSize().y).isEqualTo(expectMin.height);
        assertThat(options.getMinimumSize().z).isEqualTo(expectMin.depth);
        assertThat(resizableComponent.getMinimumSize().width).isEqualTo(expectMin.width);
        assertThat(resizableComponent.getMinimumSize().height).isEqualTo(expectMin.height);
        assertThat(resizableComponent.getMinimumSize().depth).isEqualTo(expectMin.depth);
        assertThat(options.getMaximumSize().x).isEqualTo(expectMax.width);
        assertThat(options.getMaximumSize().y).isEqualTo(expectMax.height);
        assertThat(options.getMaximumSize().z).isEqualTo(expectMax.depth);
        assertThat(resizableComponent.getMaximumSize().width).isEqualTo(expectMax.width);
        assertThat(resizableComponent.getMaximumSize().height).isEqualTo(expectMax.height);
        assertThat(resizableComponent.getMaximumSize().depth).isEqualTo(expectMax.depth);
    }

    @Test
    public void setMaxSizeOnResizableComponent_setsSanitizedMinMaxSizeOnNodeReformOptions() {
        Dimensions initMin = new Dimensions(2f, 2f, 2f);
        Dimensions initMax = new Dimensions(10f, 10f, 10f);
        Dimensions inputMax = new Dimensions(1f, Float.NaN, -1f);
        Dimensions expectMin = new Dimensions(1f, 2f, 0f);
        Dimensions expectMax = new Dimensions(1f, 10f, 0f);

        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(mFakeExecutor, mXrExtensions, initMin, initMax);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        resizableComponent.setMaximumSize(inputMax);

        assertThat(options.getMinimumSize().x).isEqualTo(expectMin.width);
        assertThat(options.getMinimumSize().y).isEqualTo(expectMin.height);
        assertThat(options.getMinimumSize().z).isEqualTo(expectMin.depth);
        assertThat(resizableComponent.getMinimumSize().width).isEqualTo(expectMin.width);
        assertThat(resizableComponent.getMinimumSize().height).isEqualTo(expectMin.height);
        assertThat(resizableComponent.getMinimumSize().depth).isEqualTo(expectMin.depth);
        assertThat(options.getMaximumSize().x).isEqualTo(expectMax.width);
        assertThat(options.getMaximumSize().y).isEqualTo(expectMax.height);
        assertThat(options.getMaximumSize().z).isEqualTo(expectMax.depth);
        assertThat(resizableComponent.getMaximumSize().width).isEqualTo(expectMax.width);
        assertThat(resizableComponent.getMaximumSize().height).isEqualTo(expectMax.height);
        assertThat(resizableComponent.getMaximumSize().depth).isEqualTo(expectMax.depth);
    }

    @Test
    public void setFixedAspectRatioOnResizableComponent_setsFixedAspectRatioOnNodeReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        // If no size was set the default aspect ratio will be 1.
        resizableComponent.setFixedAspectRatioEnabled(true);
        assertThat(options.getFixedAspectRatio()).isEqualTo(1.0f);

        // Updating the size will update the aspect ratio if enabled.
        resizableComponent.setSize(new Dimensions(1f, 2f, 1f));
        assertThat(options.getFixedAspectRatio()).isEqualTo(0.5f);

        // Disabling the aspect ratio will set the fixed aspect ratio to 0.
        resizableComponent.setFixedAspectRatioEnabled(false);
        assertThat(options.getFixedAspectRatio()).isEqualTo(0.0f);

        // Updating the size will not update the aspect ratio if disabled.
        resizableComponent.setSize(new Dimensions(3f, 1f, 1f));
        assertThat(options.getFixedAspectRatio()).isEqualTo(0.0f);

        // Enabling it will update the aspect ratio based on the current size.
        resizableComponent.setFixedAspectRatioEnabled(true);
        assertThat(options.getFixedAspectRatio()).isEqualTo(3.0f);
    }

    @Test
    public void setFixedAspectRatioOnResizableComponent_setsPanelAspectRatio() {
        Dimensions panelDimensions = new Dimensions(2.0f, 1.0f, 0.0f);
        PanelEntityImpl entity =
                (PanelEntityImpl) createTestPanelEntity(new Pose(), panelDimensions);

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();

        resizableComponent.setFixedAspectRatioEnabled(true);

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        assertThat(options.getFixedAspectRatio())
                .isEqualTo(panelDimensions.width / panelDimensions.height);
    }

    @Test
    public void
            setForceShowResizeOverlayOnResizableComponent_setsForceShowResizeOverlayOnNodeReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        resizableComponent.setForceShowResizeOverlay(true);

        assertThat(mNodeRepository.getReformOptions(entity.getNode()).getForceShowResizeOverlay())
                .isTrue();
    }

    @Test
    public void addResizableComponentLater_addsReformOptionsToNode() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();

        Dimensions testSize = new Dimensions(1f, 1f, 1f);
        Dimensions testMinSize = new Dimensions(0.25f, 0.25f, 0.25f);
        Dimensions testMaxSize = new Dimensions(5f, 5f, 5f);
        resizableComponent.setSize(testSize);
        resizableComponent.setMinimumSize(testMinSize);
        resizableComponent.setMaximumSize(testMaxSize);

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        assertThat(options.getEnabledReform()).isEqualTo(ALLOW_RESIZE);
        assertThat(options.getCurrentSize().x).isEqualTo(testSize.width);
        assertThat(options.getCurrentSize().y).isEqualTo(testSize.height);
        assertThat(options.getCurrentSize().z).isEqualTo(testSize.depth);
        assertThat(options.getMinimumSize().x).isEqualTo(testMinSize.width);
        assertThat(options.getMinimumSize().y).isEqualTo(testMinSize.height);
        assertThat(options.getMinimumSize().z).isEqualTo(testMinSize.depth);
        assertThat(options.getMaximumSize().x).isEqualTo(testMaxSize.width);
        assertThat(options.getMaximumSize().y).isEqualTo(testMaxSize.height);
        assertThat(options.getMaximumSize().z).isEqualTo(testMaxSize.depth);
    }

    @Test
    public void addResizeEventListener_onlyInvokedOnResizeEvent() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);

        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);

        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();
        assertThat(entity.getReformEventConsumerMap()).isNotEmpty();

        ReformEvent moveReformEvent =
                ShadowReformEvent.create(/* type= */ REFORM_TYPE_MOVE, /* state= */ 0, /* id= */ 0);

        sendResizeEvent(entity.getNode(), moveReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        verify(mockResizeEventListener, never()).onResizeEvent(any());

        ReformEvent resizeReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);
        sendResizeEvent(entity.getNode(), resizeReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        verify(mockResizeEventListener).onResizeEvent(any());
    }

    private void sendResizeEvent(Node node, ReformEvent reformEvent) {
        ReformOptions options = mNodeRepository.getReformOptions(node);
        options.getEventExecutor().execute(() -> options.getEventCallback().accept(reformEvent));
    }

    @Test
    public void addResizeEventListenerWithExecutor_invokesListenerOnGivenExecutor() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener);

        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent resizeReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);

        sendResizeEvent(entity.getNode(), resizeReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        assertThat(executorService.hasNext()).isTrue();

        executorService.runAll();

        verify(mockResizeEventListener).onResizeEvent(any());
    }

    @Test
    public void addResizeEventListenerMultiple_invokesAllListeners() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());
        ResizeEventListener mockResizeEventListener1 = mock(ResizeEventListener.class);
        ResizeEventListener mockResizeEventListener2 = mock(ResizeEventListener.class);
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener1);
        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener2);

        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent resizeReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);

        sendResizeEvent(entity.getNode(), resizeReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        assertThat(executorService.hasNext()).isTrue();

        executorService.runAll();

        verify(mockResizeEventListener1).onResizeEvent(any());
        verify(mockResizeEventListener2).onResizeEvent(any());
    }

    @Test
    public void removeResizeEventListenerMultiple_removesGivenListener() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());
        ResizeEventListener mockResizeEventListener1 = mock(ResizeEventListener.class);
        ResizeEventListener mockResizeEventListener2 = mock(ResizeEventListener.class);
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener1);
        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener2);
        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent resizeReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);

        sendResizeEvent(entity.getNode(), resizeReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        assertThat(executorService.hasNext()).isTrue();

        executorService.runAll();

        resizableComponent.removeResizeEventListener(mockResizeEventListener1);
        sendResizeEvent(entity.getNode(), resizeReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        assertThat(executorService.hasNext()).isTrue();

        executorService.runAll();

        verify(mockResizeEventListener1).onResizeEvent(any());
        verify(mockResizeEventListener2, times(2)).onResizeEvent(any());
    }

    @Test
    public void removeAllResizeEventListeners_removesReformEventConsumer() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());
        ResizeEventListener mockResizeEventListener1 = mock(ResizeEventListener.class);
        ResizeEventListener mockResizeEventListener2 = mock(ResizeEventListener.class);
        FakeScheduledExecutorService executorService = new FakeScheduledExecutorService();

        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener1);
        resizableComponent.addResizeEventListener(executorService, mockResizeEventListener2);

        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();

        ReformEvent resizeReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);

        sendResizeEvent(entity.getNode(), resizeReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        assertThat(executorService.hasNext()).isTrue();

        executorService.runAll();

        verify(mockResizeEventListener1).onResizeEvent(any());
        verify(mockResizeEventListener2).onResizeEvent(any());

        resizableComponent.removeResizeEventListener(mockResizeEventListener1);
        resizableComponent.removeResizeEventListener(mockResizeEventListener2);

        assertThat(entity.getReformEventConsumerMap()).isEmpty();
    }

    @Test
    public void removeResizableComponent_clearsResizeReformOptionsAndResizeEventListeners() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);

        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);

        assertThat(resizableComponent.mReformEventConsumer).isNotNull();

        entity.removeComponent(resizableComponent);

        assertThat(mNodeRepository.getReformOptions(entity.getNode())).isNull();
        assertThat(entity.getReformEventConsumerMap()).isEmpty();
    }

    @Test
    public void addMoveAndResizeComponents_setsCombinedReformsOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        true, true, false, mActivitySpaceImpl, mPanelShadowRenderer, mFakeExecutor);
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0f, 0f, 0f),
                        new Dimensions(5f, 5f, 5f));

        assertThat(entity.addComponent(movableComponent)).isTrue();
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        assertThat(mNodeRepository.getReformOptions(entity.getNode()).getEnabledReform())
                .isEqualTo(ALLOW_MOVE | ALLOW_RESIZE);
    }

    @Test
    public void addMoveAndResizeComponents_removingMoveKeepsResize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        true, true, false, mActivitySpaceImpl, mPanelShadowRenderer, mFakeExecutor);
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0f, 0f, 0f),
                        new Dimensions(5f, 5f, 5f));

        assertThat(entity.addComponent(movableComponent)).isTrue();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        MoveEventListener moveEventListener = mock(MoveEventListener.class);
        movableComponent.addMoveEventListener(directExecutor(), moveEventListener);
        ResizeEventListener resizeEventListener = mock(ResizeEventListener.class);
        resizableComponent.addResizeEventListener(directExecutor(), resizeEventListener);
        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        assertThat(options.getEnabledReform()).isEqualTo(ALLOW_MOVE | ALLOW_RESIZE);

        entity.removeComponent(movableComponent);

        assertThat(options.getEnabledReform()).isEqualTo(ALLOW_RESIZE);

        ReformEvent resizeReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);

        sendResizeEvent(entity.getNode(), resizeReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        verify(resizeEventListener).onResizeEvent(any());

        ReformEvent moveReformEvent =
                ShadowReformEvent.create(/* type= */ REFORM_TYPE_MOVE, /* state= */ 0, /* id= */ 0);

        sendResizeEvent(entity.getNode(), moveReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        verify(moveEventListener, never()).onMoveEvent(any());
    }

    @Test
    public void addMoveAndResizeComponents_removingResizeKeepsMove() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        true, true, false, mActivitySpaceImpl, mPanelShadowRenderer, mFakeExecutor);
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0f, 0f, 0f),
                        new Dimensions(5f, 5f, 5f));

        assertThat(entity.addComponent(movableComponent)).isTrue();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        MoveEventListener moveEventListener = mock(MoveEventListener.class);
        movableComponent.addMoveEventListener(directExecutor(), moveEventListener);
        ResizeEventListener resizeEventListener = mock(ResizeEventListener.class);
        resizableComponent.addResizeEventListener(directExecutor(), resizeEventListener);
        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        assertThat(options.getEnabledReform()).isEqualTo(ALLOW_MOVE | ALLOW_RESIZE);

        entity.removeComponent(resizableComponent);

        assertThat(options.getEnabledReform()).isEqualTo(ALLOW_MOVE);

        // Start the resize.
        ReformEvent startReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_MOVE, /* state= */ REFORM_STATE_START, /* id= */ 0);

        sendResizeEvent(entity.getNode(), startReformEvent);

        ReformEvent moveReformEvent =
                ShadowReformEvent.create(/* type= */ REFORM_TYPE_MOVE, /* state= */ 0, /* id= */ 0);

        sendResizeEvent(entity.getNode(), moveReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        verify(moveEventListener, times(2)).onMoveEvent(any());

        ReformEvent resizeReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE, /* state= */ 0, /* id= */ 0);

        sendResizeEvent(entity.getNode(), resizeReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        verify(resizeEventListener, never()).onResizeEvent(any());
    }

    @Test
    public void resizableComponent_canAttachAgainAfterDetach() {
        Entity entity = createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        entity.removeComponent(resizableComponent);

        assertThat(entity.addComponent(resizableComponent)).isTrue();
    }

    @Test
    public void resizableComponent_hidesEntityDuringResize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        entity.setAlpha(0.9f);

        assertThat(entity.getAlpha()).isEqualTo(0.9f);

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);

        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);

        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();
        assertThat(entity.getReformEventConsumerMap()).isNotEmpty();

        // Start the resize.
        ReformEvent startReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE,
                        /* state= */ REFORM_STATE_START,
                        /* id= */ 0);

        sendResizeEvent(entity.getNode(), startReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();
        ArgumentCaptor<ResizeEvent> resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);

        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture());

        ResizeEvent resizeEvent = resizeEventCaptor.getValue();

        assertThat(resizeEvent.getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_START);
        assertThat(mNodeRepository.getAlpha(entity.getNode())).isEqualTo(0.0f);

        // End the resize.
        ReformEvent endReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        sendResizeEvent(entity.getNode(), endReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();
        // Verify that alpha is not restored until the resize event is processed.
        assertThat(mNodeRepository.getAlpha(entity.getNode())).isEqualTo(0.0f);

        mFakeExecutor.runAll();

        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture());
        resizeEvent = resizeEventCaptor.getAllValues().get(2);

        assertThat(resizeEvent.getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        // Verify that alpha is restored after the resize event is processed.
        assertThat(mNodeRepository.getAlpha(entity.getNode())).isEqualTo(0.9f);
    }

    @Test
    public void resizableComponent_withAutoHideContentDisabled_doesNotHideEntityDuringResize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        entity.setAlpha(0.9f);

        assertThat(entity.getAlpha()).isEqualTo(0.9f);

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);

        resizableComponent.setAutoHideContent(false);
        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);

        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();
        assertThat(entity.getReformEventConsumerMap()).isNotEmpty();

        // Start the resize.
        ReformEvent startReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE,
                        /* state= */ REFORM_STATE_START,
                        /* id= */ 0);

        sendResizeEvent(entity.getNode(), startReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        ArgumentCaptor<ResizeEvent> resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);

        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture());
        ResizeEvent resizeEvent = resizeEventCaptor.getValue();

        assertThat(resizeEvent.getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_START);
        assertThat(mNodeRepository.getAlpha(entity.getNode())).isEqualTo(0.9f);

        // End the resize.
        ReformEvent endReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE, /* state= */ REFORM_STATE_END, /* id= */ 0);

        sendResizeEvent(entity.getNode(), endReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture());
        resizeEvent = resizeEventCaptor.getAllValues().get(2);

        assertThat(resizeEvent.getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        assertThat(mNodeRepository.getAlpha(entity.getNode())).isEqualTo(0.9f);
    }

    @Test
    public void resizableComponent_updatesComponentSizeAfterResize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());
        resizableComponent.setSize(new Dimensions(1.0f, 2.0f, 3.0f));

        assertThat(options.getCurrentSize().x).isEqualTo(1.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(2.0f);
        assertThat(options.getCurrentSize().z).isEqualTo(3.0f);

        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);

        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);

        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();
        assertThat(entity.getReformEventConsumerMap()).isNotEmpty();

        // Start the resize.
        ReformEvent startReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE,
                        /* state= */ REFORM_STATE_START,
                        /* id= */ 0);

        sendResizeEvent(entity.getNode(), startReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();
        ArgumentCaptor<ResizeEvent> resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);

        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture());

        ResizeEvent resizeEvent = resizeEventCaptor.getValue();

        assertThat(resizeEvent.getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_START);

        // End the resize.
        ReformEvent endReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE, /* state= */ REFORM_STATE_END, /* id= */ 0);
        ShadowReformEvent.extract(endReformEvent).setProposedSize(new Vec3(4.0f, 5.0f, 6.0f));

        sendResizeEvent(entity.getNode(), endReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture());

        resizeEvent = resizeEventCaptor.getAllValues().get(2);

        assertThat(resizeEvent.getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        assertThat(options.getCurrentSize().x).isEqualTo(4.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(5.0f);
        assertThat(options.getCurrentSize().z).isEqualTo(6.0f);
    }

    /**
     * Helper method to send a ReformEvent to the entity's node and immediately process it using the
     * fake executor.
     *
     * @param node The Node to which the ReformEvent is sent.
     * @param reformEvent The ReformEvent to send.
     */
    private void sendAndProcessReformEvent(Node node, ReformEvent reformEvent) {
        ReformOptions options = mNodeRepository.getReformOptions(node);
        options.getEventExecutor().execute(() -> options.getEventCallback().accept(reformEvent));
        mFakeExecutor.runAll();
    }

    @Test
    public void resizableComponent_onResizeEvent_proposeSanitizedAndClampedSize() {
        // Setup
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0.5f, 0.5f, 0.5f), // minSize
                        new Dimensions(10.0f, 10.0f, 10.0f)); // maxSize

        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());
        // Set an initial valid size for the component.
        resizableComponent.setSize(new Dimensions(2.0f, 3.0f, 4.0f));

        // Verify initial state.
        assertThat(options.getCurrentSize().x).isEqualTo(2.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(3.0f);
        assertThat(options.getCurrentSize().z).isEqualTo(4.0f);

        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);
        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);

        // 1. Send a START resize event.
        ReformEvent startReformEvent =
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_START, 0);
        ShadowReformEvent.extract(startReformEvent).setProposedSize(new Vec3(3.0f, 4.0f, 5.0f));
        sendAndProcessReformEvent(entity.getNode(), startReformEvent);

        // Capture all ResizeEvent arguments in a single verification.
        ArgumentCaptor<ResizeEvent> resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);

        verify(mockResizeEventListener, times(1)).onResizeEvent(resizeEventCaptor.capture());

        List<ResizeEvent> capturedEvents = resizeEventCaptor.getAllValues();

        // Event 1: RESIZE_STATE_START
        assertThat(capturedEvents.get(0).getResizeState())
                .isEqualTo(ResizeEvent.RESIZE_STATE_START);
        // Use the default ReformOptions size when resizing start.
        assertThat(options.getCurrentSize().x).isEqualTo(3.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(4.0f);
        assertThat(options.getCurrentSize().z).isEqualTo(5.0f);
        assertThat(resizableComponent.getSize().width).isEqualTo(3.0f);
        assertThat(resizableComponent.getSize().height).isEqualTo(4.0f);
        assertThat(resizableComponent.getSize().depth).isEqualTo(5.0f);

        // 2. Send an END resize event with a NaN width.
        ReformEvent endEventNanWidth =
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_END, 0);
        ShadowReformEvent.extract(endEventNanWidth)
                .setProposedSize(new Vec3(Float.NaN, 0.0f, 20.0f));
        sendAndProcessReformEvent(entity.getNode(), endEventNanWidth);

        resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);

        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture());

        capturedEvents = resizeEventCaptor.getAllValues();

        // Event 2: RESIZE_STATE_END (NaN width proposed)
        assertThat(capturedEvents.get(1).getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        // When received invalid size from event, for NaN, propose a fallback previous value,
        // for value out of min/max, propose clamped value.
        assertThat(options.getCurrentSize().x).isEqualTo(3.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(0.5f);
        assertThat(options.getCurrentSize().z).isEqualTo(10.0f);
        assertThat(resizableComponent.getSize().width).isEqualTo(3.0f);
        assertThat(resizableComponent.getSize().height).isEqualTo(0.5f);
        assertThat(resizableComponent.getSize().depth).isEqualTo(10.0f);

        // 3. Send an END resize event with a NaN height.
        ReformEvent endEventNanHeight =
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_END, 0);
        ShadowReformEvent.extract(endEventNanHeight)
                .setProposedSize(new Vec3(4.0f, Float.NaN, 6.0f));
        sendAndProcessReformEvent(entity.getNode(), endEventNanHeight);

        // Capture all ResizeEvent arguments in a single verification.
        resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);

        verify(mockResizeEventListener, times(3)).onResizeEvent(resizeEventCaptor.capture());

        capturedEvents = resizeEventCaptor.getAllValues();

        // Event 3: RESIZE_STATE_END (NaN height proposed)
        assertThat(capturedEvents.get(2).getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        // Propose sanitized and clamped size
        assertThat(options.getCurrentSize().x).isEqualTo(4.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(0.5f);
        assertThat(options.getCurrentSize().z).isEqualTo(6.0f);
        assertThat(resizableComponent.getSize().width).isEqualTo(4.0f);
        assertThat(resizableComponent.getSize().height).isEqualTo(0.5f);
        assertThat(resizableComponent.getSize().depth).isEqualTo(6.0f);

        // 4. Send an END resize event with a NaN depth.
        ReformEvent endEventNanDepth =
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_END, 0);
        ShadowReformEvent.extract(endEventNanDepth)
                .setProposedSize(new Vec3(4.0f, 5.0f, Float.NaN));
        sendAndProcessReformEvent(entity.getNode(), endEventNanDepth);

        // Capture all ResizeEvent arguments in a single verification.
        resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);

        verify(mockResizeEventListener, times(4)).onResizeEvent(resizeEventCaptor.capture());

        capturedEvents = resizeEventCaptor.getAllValues();

        // Event 4: RESIZE_STATE_END (NaN depth proposed)
        assertThat(capturedEvents.get(3).getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        // Propose sanitized and clamped size
        assertThat(options.getCurrentSize().x).isEqualTo(4.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(5.0f);
        assertThat(options.getCurrentSize().z).isEqualTo(6.0f);
        assertThat(resizableComponent.getSize().width).isEqualTo(4.0f);
        assertThat(resizableComponent.getSize().height).isEqualTo(5.0f);
        assertThat(resizableComponent.getSize().depth).isEqualTo(6.0f);
    }

    @Test
    public void
            resizableComponent_withAutoUpdateSizeDisabled_doesNotUpdateComponentSizeAfterResize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();

        assertThat(entity).isNotNull();

        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);

        assertThat(resizableComponent).isNotNull();
        assertThat(entity.addComponent(resizableComponent)).isTrue();

        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());
        resizableComponent.setAutoUpdateSize(false);
        resizableComponent.setSize(new Dimensions(1.0f, 2.0f, 3.0f));

        assertThat(options.getCurrentSize().x).isEqualTo(1.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(2.0f);
        assertThat(options.getCurrentSize().z).isEqualTo(3.0f);

        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);

        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);

        assertThat(options.getEventCallback()).isNotNull();
        assertThat(options.getEventExecutor()).isNotNull();
        assertThat(entity.getReformEventConsumerMap()).isNotEmpty();

        // Start the resize.
        ReformEvent startReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE,
                        /* state= */ REFORM_STATE_START,
                        /* id= */ 0);

        sendResizeEvent(entity.getNode(), startReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();
        ArgumentCaptor<ResizeEvent> resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);

        verify(mockResizeEventListener).onResizeEvent(resizeEventCaptor.capture());

        ResizeEvent resizeEvent = resizeEventCaptor.getValue();

        assertThat(resizeEvent.getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_START);

        // End the resize.
        ReformEvent endReformEvent =
                ShadowReformEvent.create(
                        /* type= */ REFORM_TYPE_RESIZE, /* state= */ REFORM_STATE_END, /* id= */ 0);
        ShadowReformEvent.extract(endReformEvent).setProposedSize(new Vec3(4.0f, 5.0f, 6.0f));

        sendResizeEvent(entity.getNode(), endReformEvent);

        assertThat(mFakeExecutor.hasNext()).isTrue();

        mFakeExecutor.runAll();

        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture());

        resizeEvent = resizeEventCaptor.getAllValues().get(2);

        assertThat(resizeEvent.getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        // Reform size should be unchanged.
        assertThat(options.getCurrentSize().x).isEqualTo(1.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(2.0f);
        assertThat(options.getCurrentSize().z).isEqualTo(3.0f);
    }

    @Test
    public void resizableComponent_restoresAlphaOnNonResizeEventWhenHidden() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        entity.setAlpha(0.9f);
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);
        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);

        // Start a resize event to hide the content.
        sendAndProcessReformEvent(
                entity.getNode(),
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_START, 0));

        // Verify content is hidden.
        assertThat(mNodeRepository.getAlpha(entity.getNode())).isEqualTo(0.0f);

        // Send a non-resize event (e.g., move).
        sendAndProcessReformEvent(
                entity.getNode(), ShadowReformEvent.create(REFORM_TYPE_MOVE, 0, 0));

        // Verify alpha is restored because a non-resize event was received.
        assertThat(mNodeRepository.getAlpha(entity.getNode())).isEqualTo(0.9f);
        // The resize event listener should not be called again for a move event.
        verify(mockResizeEventListener, times(1)).onResizeEvent(any());
    }

    @Test
    public void resizableComponent_restoresAlphaOnDetachWhenHidden() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);
        assertThat(entity.addComponent(resizableComponent)).isTrue();
        entity.setAlpha(0.9f);
        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);
        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);

        // Start a resize event to hide the content.
        sendAndProcessReformEvent(
                entity.getNode(),
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_START, 0));

        // Verify content is hidden.
        assertThat(mNodeRepository.getAlpha(entity.getNode())).isEqualTo(0.0f);

        // Detach the component.
        entity.removeComponent(resizableComponent);

        // Verify alpha is restored.
        assertThat(mNodeRepository.getAlpha(entity.getNode())).isEqualTo(0.9f);
    }
}
