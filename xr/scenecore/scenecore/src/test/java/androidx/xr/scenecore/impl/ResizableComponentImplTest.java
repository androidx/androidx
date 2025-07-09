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

import static com.android.extensions.xr.node.ReformEvent.REFORM_STATE_END;
import static com.android.extensions.xr.node.ReformEvent.REFORM_STATE_START;
import static com.android.extensions.xr.node.ReformEvent.REFORM_TYPE_MOVE;
import static com.android.extensions.xr.node.ReformEvent.REFORM_TYPE_RESIZE;
import static com.android.extensions.xr.node.ReformOptions.ALLOW_MOVE;
import static com.android.extensions.xr.node.ReformOptions.ALLOW_RESIZE;

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
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.JxrPlatformAdapter;
import androidx.xr.runtime.internal.MoveEventListener;
import androidx.xr.runtime.internal.PanelEntity;
import androidx.xr.runtime.internal.ResizeEvent;
import androidx.xr.runtime.internal.ResizeEventListener;
import androidx.xr.runtime.math.Pose;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeRepository;
import com.android.extensions.xr.node.ReformEvent;
import com.android.extensions.xr.node.ReformOptions;
import com.android.extensions.xr.node.ShadowReformEvent;
import com.android.extensions.xr.node.Vec3;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
import com.google.common.collect.ImmutableSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ResizableComponentImplTest {

    private static final Dimensions MIN_DIMENSIONS = new Dimensions(0f, 0f, 0f);
    private static final Dimensions MAX_DIMENSIONS = new Dimensions(10f, 10f, 10f);
    private static final Dimensions DEFAULT_SIZE = new Dimensions(1.0f, 1.0f, 1.0f);
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary mPerceptionLibrary = mock(PerceptionLibrary.class);
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final FakeImpressApiImpl mFakeImpressApi = new FakeImpressApiImpl();
    private final EntityManager mEntityManager = new EntityManager();
    private final Node mActivitySpaceNode = mXrExtensions.createNode();
    private final ActivitySpaceImpl mActivitySpaceImpl =
            new ActivitySpaceImpl(
                    mActivitySpaceNode,
                    mActivity,
                    mXrExtensions,
                    mEntityManager,
                    () -> mXrExtensions.getSpatialState(mActivity),
                    /* unscaledGravityAlignedActivitySpace= */ false,
                    mFakeExecutor);
    private final AndroidXrEntity mActivitySpaceRoot = Mockito.mock(AndroidXrEntity.class);
    private final PerceptionSpaceActivityPoseImpl mPerceptionSpaceActivityPose =
            new PerceptionSpaceActivityPoseImpl(mActivitySpaceImpl, mActivitySpaceRoot);
    private final PanelShadowRenderer mPanelShadowRenderer =
            Mockito.mock(PanelShadowRenderer.class);

    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    private final ImpSplitEngineRenderer mSplitEngineRenderer =
            Mockito.mock(ImpSplitEngineRenderer.class);
    private JxrPlatformAdapter mFakeRuntime;
    private final NodeRepository mNodeRepository = NodeRepository.getInstance();

    @Before
    public void setUp() {
        when(mPerceptionLibrary.initSession(eq(mActivity), anyInt(), eq(mFakeExecutor)))
                .thenReturn(immediateFuture(mock(Session.class)));
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
    }

    @After
    public void tearDown() {
        // Dispose the runtime between test cases to clean up lingering references.
        mFakeRuntime.dispose();
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
                mFakeRuntime.getActivitySpaceRootImpl());
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
    public void addResizableComponentWithTooSmallMinSize_getsMinimumValidSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(-0.01f, -0.01f, -0.01f),
                        MAX_DIMENSIONS);
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
    public void addResizableComponentWithTooSmallMaxSize_getsDefaultSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, new Dimensions(0f, 0f, 0f));
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
    public void addResizableComponentToPanelEntity_resizableComponentCurrentSizeIsPanelSize() {
        // PanelEntity's depth will always be 9.
        Dimensions expectedDimensions = new Dimensions(1.0f, 1.0f, 0.0f);
        PanelEntity entity = createTestPanelEntity(new Pose(), expectedDimensions);
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0.5f, 0.5f, 0.0f), // minSize
                        new Dimensions(5.0f, 5.0f, 5.0f)); // maxSize
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        Dimensions size = resizableComponent.getSize();
        assertThat(size.width).isEqualTo(expectedDimensions.width);
        assertThat(size.height).isEqualTo(expectedDimensions.height);
        assertThat(size.depth).isEqualTo(expectedDimensions.depth);
    }

    @Test
    public void addResizableComponentToPanelEntityWithTooSmallSize_addsComponentFailed() {
        PanelEntity entity = createTestPanelEntity(new Pose(), MIN_DIMENSIONS);
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0.5f, 0.5f, 0.5f), // minSize
                        new Dimensions(5.0f, 5.0f, 5.0f)); // maxSize
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isFalse();
    }

    @Test
    public void addResizableComponentToPanelEntityWithTooBigSize_addsComponentFailed() {
        PanelEntity entity = createTestPanelEntity(new Pose(), MAX_DIMENSIONS);
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0.5f, 0.5f, 0.5f), // minSize
                        new Dimensions(5.0f, 5.0f, 5.0f)); // maxSize
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isFalse();
    }

    @Test
    public void getSizeOnResizableComponent_returnsDefaultSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        assertThat(resizableComponent.getSize().width).isEqualTo(DEFAULT_SIZE.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(DEFAULT_SIZE.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(DEFAULT_SIZE.depth);
    }

    @Test
    public void setSizeOnResizableComponent_returnsSetSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0.5f, 0.5f, 0.5f), // minSize
                        new Dimensions(5.0f, 5.0f, 5.0f)); // maxSize
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        resizableComponent.setSize(new Dimensions(2.0f, 3.0f, 4.0f));
        assertThat(resizableComponent.getSize().width).isEqualTo(2.0f);
        assertThat(resizableComponent.getSize().height).isEqualTo(3.0f);
        assertThat(resizableComponent.getSize().depth).isEqualTo(4.0f);
    }

    @Test
    public void setSizeFirstTimeOnResizableComponentWithTooSmallSize_returnsDefaultSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0.5f, 0.5f, 0.5f), // minSize
                        new Dimensions(5.0f, 5.0f, 5.0f)); // maxSize
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        // Smaller than minSize.
        resizableComponent.setSize(new Dimensions(0.1f, 1.0f, 1.0f));
        resizableComponent.setSize(new Dimensions(1.0f, 0.1f, 1.0f));
        resizableComponent.setSize(new Dimensions(1.0f, 1.0f, 0.1f));
        assertThat(resizableComponent.getSize().width).isEqualTo(DEFAULT_SIZE.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(DEFAULT_SIZE.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(DEFAULT_SIZE.depth);
    }

    @Test
    public void setSizeFirstTimeOnResizableComponentWithTooBigSize_returnsDefaultSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0.5f, 0.5f, 0.5f), // minSize
                        new Dimensions(5.0f, 5.0f, 5.0f)); // maxSize
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        // Bigger than maxSize.
        resizableComponent.setSize(new Dimensions(6.0f, 1.0f, 1.0f));
        resizableComponent.setSize(new Dimensions(1.0f, 6.0f, 1.0f));
        resizableComponent.setSize(new Dimensions(1.0f, 1.0f, 6.0f));
        assertThat(resizableComponent.getSize().width).isEqualTo(DEFAULT_SIZE.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(DEFAULT_SIZE.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(DEFAULT_SIZE.depth);
    }

    @Test
    public void setSizeOnResizableComponentMultipleTimes_returnsLastValidSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0.5f, 0.5f, 0.5f), // minSize
                        new Dimensions(5.0f, 5.0f, 5.0f)); // maxSize
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        resizableComponent.setSize(new Dimensions(2.0f, 3.0f, 4.0f));
        assertThat(resizableComponent.getSize().width).isEqualTo(2.0f);
        assertThat(resizableComponent.getSize().height).isEqualTo(3.0f);
        assertThat(resizableComponent.getSize().depth).isEqualTo(4.0f);

        // Set NaN size.
        resizableComponent.setSize(new Dimensions(Float.NaN, 1.0f, 1.0f));
        resizableComponent.setSize(new Dimensions(1.0f, Float.NaN, 1.0f));
        resizableComponent.setSize(new Dimensions(1.0f, 1.0f, Float.NaN));
        assertThat(resizableComponent.getSize().width).isEqualTo(2.0f);
        assertThat(resizableComponent.getSize().height).isEqualTo(3.0f);
        assertThat(resizableComponent.getSize().depth).isEqualTo(4.0f);

        // Smaller than minSize.
        resizableComponent.setSize(new Dimensions(0.1f, 1.0f, 1.0f));
        resizableComponent.setSize(new Dimensions(1.0f, 0.1f, 1.0f));
        resizableComponent.setSize(new Dimensions(1.0f, 1.0f, 0.1f));
        assertThat(resizableComponent.getSize().width).isEqualTo(2.0f);
        assertThat(resizableComponent.getSize().height).isEqualTo(3.0f);
        assertThat(resizableComponent.getSize().depth).isEqualTo(4.0f);

        // Bigger than maxSize
        resizableComponent.setSize(new Dimensions(6.0f, 1.0f, 1.0f));
        resizableComponent.setSize(new Dimensions(1.0f, 6.0f, 1.0f));
        resizableComponent.setSize(new Dimensions(1.0f, 1.0f, 6.0f));
        assertThat(resizableComponent.getSize().width).isEqualTo(2.0f);
        assertThat(resizableComponent.getSize().height).isEqualTo(3.0f);
        assertThat(resizableComponent.getSize().depth).isEqualTo(4.0f);
    }

    @Test
    public void setSizeOnResizableComponent_setsSizeOnNodeReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        resizableComponent.setSize(MAX_DIMENSIONS);
        assertThat(options.getCurrentSize().x).isEqualTo(MAX_DIMENSIONS.width);
        assertThat(options.getCurrentSize().y).isEqualTo(MAX_DIMENSIONS.height);
        assertThat(options.getCurrentSize().z).isEqualTo(MAX_DIMENSIONS.depth);
    }

    @Test
    public void getMinimumSizeOnResizableComponent_returnsOnCreateSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        assertThat(resizableComponent.getMinimumSize().width).isEqualTo(MIN_DIMENSIONS.width);
        assertThat(resizableComponent.getMinimumSize().height).isEqualTo(MIN_DIMENSIONS.height);
        assertThat(resizableComponent.getMinimumSize().depth).isEqualTo(MIN_DIMENSIONS.depth);
    }

    @Test
    public void getMinimumSizeOnResizableComponent_invalidMinSizeOnCreate_getsMinimumValidSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();

        // Create with NaN min size.
        ResizableComponentImpl nanMinSizeResizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(Float.NaN, Float.NaN, Float.NaN),
                        MIN_DIMENSIONS);
        assertThat(nanMinSizeResizableComponent).isNotNull();

        assertThat(entity.addComponent(nanMinSizeResizableComponent)).isTrue();

        assertThat(nanMinSizeResizableComponent.getMinimumSize().width)
                .isEqualTo(MIN_DIMENSIONS.width);
        assertThat(nanMinSizeResizableComponent.getMinimumSize().height)
                .isEqualTo(MIN_DIMENSIONS.height);
        assertThat(nanMinSizeResizableComponent.getMinimumSize().depth)
                .isEqualTo(MIN_DIMENSIONS.depth);

        // Create with minSize >= maxSize.
        ResizableComponentImpl tooBigResizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, DEFAULT_SIZE, DEFAULT_SIZE);
        assertThat(tooBigResizableComponent).isNotNull();

        assertThat(entity.addComponent(tooBigResizableComponent)).isTrue();

        assertThat(tooBigResizableComponent.getMinimumSize().width).isEqualTo(MIN_DIMENSIONS.width);
        assertThat(tooBigResizableComponent.getMinimumSize().height)
                .isEqualTo(MIN_DIMENSIONS.height);
        assertThat(tooBigResizableComponent.getMinimumSize().depth).isEqualTo(MIN_DIMENSIONS.depth);
    }

    @Test
    public void setMinimumSizeOnResizableComponentMultipleTimes_returnsLastValidSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0.5f, 0.5f, 0.5f), // minSize
                        new Dimensions(5.0f, 5.0f, 5.0f)); // maxSize
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        resizableComponent.setMinimumSize(new Dimensions(2.0f, 3.0f, 4.0f));
        assertThat(resizableComponent.getMinimumSize().width).isEqualTo(2.0f);
        assertThat(resizableComponent.getMinimumSize().height).isEqualTo(3.0f);
        assertThat(resizableComponent.getMinimumSize().depth).isEqualTo(4.0f);

        // Set NaN size.
        resizableComponent.setMinimumSize(new Dimensions(Float.NaN, 1.0f, 1.0f));
        resizableComponent.setMinimumSize(new Dimensions(1.0f, Float.NaN, 1.0f));
        resizableComponent.setMinimumSize(new Dimensions(1.0f, 1.0f, Float.NaN));
        assertThat(resizableComponent.getMinimumSize().width).isEqualTo(2.0f);
        assertThat(resizableComponent.getMinimumSize().height).isEqualTo(3.0f);
        assertThat(resizableComponent.getMinimumSize().depth).isEqualTo(4.0f);

        // If size <= zero size, don't set.
        resizableComponent.setMinimumSize(new Dimensions(-0.01f, 1f, 1f));
        resizableComponent.setMinimumSize(new Dimensions(1f, -0.01f, 1f));
        resizableComponent.setMinimumSize(new Dimensions(1f, 1f, -0.01f));
        assertThat(resizableComponent.getMinimumSize().width).isEqualTo(2.0f);
        assertThat(resizableComponent.getMinimumSize().height).isEqualTo(3.0f);
        assertThat(resizableComponent.getMinimumSize().depth).isEqualTo(4.0f);

        // If size >= maxSize, don't set.
        resizableComponent.setMinimumSize(new Dimensions(6f, 1f, 1f));
        resizableComponent.setMinimumSize(new Dimensions(1f, 6f, 1f));
        resizableComponent.setMinimumSize(new Dimensions(1f, 1f, 6f));
        assertThat(resizableComponent.getMinimumSize().width).isEqualTo(2.0f);
        assertThat(resizableComponent.getMinimumSize().height).isEqualTo(3.0f);
        assertThat(resizableComponent.getMinimumSize().depth).isEqualTo(4.0f);
    }

    @Test
    public void setMinimumSizeOnResizableComponent_setsMinimumSizeOnNodeReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        resizableComponent.setMinimumSize(DEFAULT_SIZE);
        assertThat(options.getMinimumSize().x).isEqualTo(DEFAULT_SIZE.width);
        assertThat(options.getMinimumSize().y).isEqualTo(DEFAULT_SIZE.height);
        assertThat(options.getMinimumSize().z).isEqualTo(DEFAULT_SIZE.depth);
    }

    @Test
    public void getMaximumSizeOnResizableComponent_returnsOnCreateSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);
        assertThat(resizableComponent).isNotNull();

        assertThat(resizableComponent.getMaximumSize().width).isEqualTo(MAX_DIMENSIONS.width);
        assertThat(resizableComponent.getMaximumSize().height).isEqualTo(MAX_DIMENSIONS.height);
        assertThat(resizableComponent.getMaximumSize().depth).isEqualTo(MAX_DIMENSIONS.depth);
    }

    @Test
    public void getMaximumSizeOnResizableComponent_invalidMaxSizeOnCreate_getsDefaultSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();

        // Create with NaN min size.
        ResizableComponentImpl nanMaxSizeResizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        MIN_DIMENSIONS,
                        new Dimensions(Float.NaN, Float.NaN, Float.NaN));
        assertThat(nanMaxSizeResizableComponent).isNotNull();

        assertThat(entity.addComponent(nanMaxSizeResizableComponent)).isTrue();

        assertThat(nanMaxSizeResizableComponent.getMaximumSize().width)
                .isEqualTo(MAX_DIMENSIONS.width);
        assertThat(nanMaxSizeResizableComponent.getMaximumSize().height)
                .isEqualTo(MAX_DIMENSIONS.height);
        assertThat(nanMaxSizeResizableComponent.getMaximumSize().depth)
                .isEqualTo(MAX_DIMENSIONS.depth);

        // Create with maxSize <= minSize.
        ResizableComponentImpl tooSmallResizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, DEFAULT_SIZE, DEFAULT_SIZE);
        assertThat(tooSmallResizableComponent).isNotNull();

        assertThat(entity.addComponent(tooSmallResizableComponent)).isTrue();

        assertThat(tooSmallResizableComponent.getMaximumSize().width)
                .isEqualTo(MAX_DIMENSIONS.width);
        assertThat(tooSmallResizableComponent.getMaximumSize().height)
                .isEqualTo(MAX_DIMENSIONS.height);
        assertThat(tooSmallResizableComponent.getMaximumSize().depth)
                .isEqualTo(MAX_DIMENSIONS.depth);
    }

    @Test
    public void setMaximumSizeOnResizableComponentMultipleTimes_returnsLastValidSize() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor,
                        mXrExtensions,
                        new Dimensions(0.5f, 0.5f, 0.5f), // minSize
                        new Dimensions(5.0f, 5.0f, 5.0f)); // maxSize
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();

        resizableComponent.setMaximumSize(new Dimensions(2.0f, 3.0f, 4.0f));
        assertThat(resizableComponent.getMaximumSize().width).isEqualTo(2.0f);
        assertThat(resizableComponent.getMaximumSize().height).isEqualTo(3.0f);
        assertThat(resizableComponent.getMaximumSize().depth).isEqualTo(4.0f);

        // Set NaN size.
        resizableComponent.setMaximumSize(new Dimensions(Float.NaN, 1.0f, 1.0f));
        resizableComponent.setMaximumSize(new Dimensions(1.0f, Float.NaN, 1.0f));
        resizableComponent.setMaximumSize(new Dimensions(1.0f, 1.0f, Float.NaN));
        assertThat(resizableComponent.getMaximumSize().width).isEqualTo(2.0f);
        assertThat(resizableComponent.getMaximumSize().height).isEqualTo(3.0f);
        assertThat(resizableComponent.getMaximumSize().depth).isEqualTo(4.0f);

        // If size <= minSize, don't set.
        resizableComponent.setMaximumSize(new Dimensions(0.1f, 1.0f, 1.0f));
        resizableComponent.setMaximumSize(new Dimensions(1.0f, 0.1f, 1.0f));
        resizableComponent.setMaximumSize(new Dimensions(1.0f, 1.0f, 0.1f));
        assertThat(resizableComponent.getMaximumSize().width).isEqualTo(2.0f);
        assertThat(resizableComponent.getMaximumSize().height).isEqualTo(3.0f);
        assertThat(resizableComponent.getMaximumSize().depth).isEqualTo(4.0f);
    }

    @Test
    public void setMaximumSizeOnResizableComponent_setsMaximumSizeOnNodeReformOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        ResizableComponentImpl resizableComponent =
                new ResizableComponentImpl(
                        mFakeExecutor, mXrExtensions, MIN_DIMENSIONS, MAX_DIMENSIONS);
        assertThat(resizableComponent).isNotNull();

        assertThat(entity.addComponent(resizableComponent)).isTrue();
        ReformOptions options = mNodeRepository.getReformOptions(entity.getNode());

        resizableComponent.setMaximumSize(DEFAULT_SIZE);
        assertThat(options.getMaximumSize().x).isEqualTo(DEFAULT_SIZE.width);
        assertThat(options.getMaximumSize().y).isEqualTo(DEFAULT_SIZE.height);
        assertThat(options.getMaximumSize().z).isEqualTo(DEFAULT_SIZE.depth);
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

        resizableComponent.setFixedAspectRatio(2.0f);
        assertThat(options.getFixedAspectRatio()).isEqualTo(2.0f);
        resizableComponent.setFixedAspectRatio(0.0f);
        assertThat(options.getFixedAspectRatio()).isEqualTo(0.0f);
        resizableComponent.setFixedAspectRatio(-1.0f);
        assertThat(options.getFixedAspectRatio()).isEqualTo(-1.0f);
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
        assertThat(entity.mReformEventConsumerMap).isNotEmpty();

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

        assertThat(entity.mReformEventConsumerMap).isEmpty();
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
        assertThat(entity.mReformEventConsumerMap).isEmpty();
    }

    @Test
    public void addMoveAndResizeComponents_setsCombinedReformsOptions() {
        AndroidXrEntity entity = (AndroidXrEntity) createTestEntity();
        assertThat(entity).isNotNull();
        MovableComponentImpl movableComponent =
                new MovableComponentImpl(
                        true,
                        true,
                        ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ true,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
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
                        true,
                        true,
                        ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ true,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
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
                        true,
                        true,
                        ImmutableSet.of(),
                        /* shouldDisposeParentAnchor= */ true,
                        mPerceptionLibrary,
                        mXrExtensions,
                        mActivitySpaceImpl,
                        mActivitySpaceRoot,
                        mPerceptionSpaceActivityPose,
                        mEntityManager,
                        mPanelShadowRenderer,
                        mFakeExecutor);
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
        assertThat(entity.mReformEventConsumerMap).isNotEmpty();

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
        assertThat(entity.mReformEventConsumerMap).isNotEmpty();

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
        assertThat(entity.mReformEventConsumerMap).isNotEmpty();

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
    public void resizableComponent_withNaNSize_useDefaultSizeAfterResize() {
        // Arrange
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
        resizableComponent.setSize(new Dimensions(1.0f, 2.0f, 3.0f));

        // Verify initial state.
        assertThat(options.getCurrentSize().x).isEqualTo(1.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(2.0f);
        assertThat(options.getCurrentSize().z).isEqualTo(3.0f);

        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);
        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);

        // 1. Send a START resize event.
        sendAndProcessReformEvent(
                entity.getNode(),
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_START, 0));

        // Capture all ResizeEvent arguments in a single verification.
        ArgumentCaptor<ResizeEvent> resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);
        verify(mockResizeEventListener, times(1)).onResizeEvent(resizeEventCaptor.capture());
        List<ResizeEvent> capturedEvents = resizeEventCaptor.getAllValues();

        // Event 1: RESIZE_STATE_START
        assertThat(capturedEvents.get(0).getResizeState())
                .isEqualTo(ResizeEvent.RESIZE_STATE_START);
        // Use the default ReformOptions size when resizing start.
        assertThat(options.getCurrentSize().x).isEqualTo(DEFAULT_SIZE.width);
        assertThat(options.getCurrentSize().y).isEqualTo(DEFAULT_SIZE.height);
        assertThat(options.getCurrentSize().z).isEqualTo(DEFAULT_SIZE.depth);
        assertThat(resizableComponent.getSize().width).isEqualTo(DEFAULT_SIZE.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(DEFAULT_SIZE.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(DEFAULT_SIZE.depth);

        // 2. Send an END resize event with a NaN width.
        ReformEvent endEventNanWidth =
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_END, 0);
        ShadowReformEvent.extract(endEventNanWidth)
                .setProposedSize(new Vec3(Float.NaN, 5.0f, 6.0f));
        sendAndProcessReformEvent(entity.getNode(), endEventNanWidth);

        resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);
        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture());
        capturedEvents = resizeEventCaptor.getAllValues();

        // Event 2: RESIZE_STATE_END (NaN width proposed)
        assertThat(capturedEvents.get(1).getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        // When a non-well-formed size is proposed, ResizableComponentImpl defaults
        // to DEFAULT_SIZE (1.0, 1.0, 1.0).
        assertThat(options.getCurrentSize().x).isEqualTo(DEFAULT_SIZE.width);
        assertThat(options.getCurrentSize().y).isEqualTo(DEFAULT_SIZE.height);
        assertThat(options.getCurrentSize().z).isEqualTo(DEFAULT_SIZE.depth);
        assertThat(resizableComponent.getSize().width).isEqualTo(DEFAULT_SIZE.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(DEFAULT_SIZE.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(DEFAULT_SIZE.depth);

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
        // The size should still be the default size.
        assertThat(options.getCurrentSize().x).isEqualTo(DEFAULT_SIZE.width);
        assertThat(options.getCurrentSize().y).isEqualTo(DEFAULT_SIZE.height);
        assertThat(options.getCurrentSize().z).isEqualTo(DEFAULT_SIZE.depth);
        assertThat(resizableComponent.getSize().width).isEqualTo(DEFAULT_SIZE.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(DEFAULT_SIZE.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(DEFAULT_SIZE.depth);

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
        // The size should still be the default size.
        assertThat(options.getCurrentSize().x).isEqualTo(DEFAULT_SIZE.width);
        assertThat(options.getCurrentSize().y).isEqualTo(DEFAULT_SIZE.height);
        assertThat(options.getCurrentSize().z).isEqualTo(DEFAULT_SIZE.depth);
        assertThat(resizableComponent.getSize().width).isEqualTo(DEFAULT_SIZE.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(DEFAULT_SIZE.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(DEFAULT_SIZE.depth);
    }

    @Test
    public void resizableComponent_withTooSmallSize_useLastValidSizeAfterResize() {
        // Arrange
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
        // Set an initial valid size for the component
        resizableComponent.setSize(new Dimensions(1.0f, 2.0f, 3.0f));

        // Verify initial state
        assertThat(options.getCurrentSize().x).isEqualTo(1.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(2.0f);
        assertThat(options.getCurrentSize().z).isEqualTo(3.0f);

        ResizeEventListener mockResizeEventListener = mock(ResizeEventListener.class);
        resizableComponent.addResizeEventListener(directExecutor(), mockResizeEventListener);

        // Act
        // 1. Send a START resize event
        sendAndProcessReformEvent(
                entity.getNode(),
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_START, 0));

        // Capture all ResizeEvent arguments in a single verification
        ArgumentCaptor<ResizeEvent> resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);
        verify(mockResizeEventListener, times(1)).onResizeEvent(resizeEventCaptor.capture());
        List<ResizeEvent> capturedEvents = resizeEventCaptor.getAllValues();

        // Event 1: RESIZE_STATE_START
        assertThat(capturedEvents.get(0).getResizeState())
                .isEqualTo(ResizeEvent.RESIZE_STATE_START);
        // Use the default ReformOptions size when resizing start.
        assertThat(options.getCurrentSize().x).isEqualTo(DEFAULT_SIZE.width);
        assertThat(options.getCurrentSize().y).isEqualTo(DEFAULT_SIZE.height);
        assertThat(options.getCurrentSize().z).isEqualTo(DEFAULT_SIZE.depth);
        assertThat(resizableComponent.getSize().width).isEqualTo(DEFAULT_SIZE.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(DEFAULT_SIZE.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(DEFAULT_SIZE.depth);

        // 2. Send an END resize event with an invalid (zero) proposed size
        ReformEvent endReformEventWithZeroSize =
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_END, 0);
        ShadowReformEvent.extract(endReformEventWithZeroSize)
                .setProposedSize(new Vec3(0.0f, 0.0f, 0.0f));
        sendAndProcessReformEvent(entity.getNode(), endReformEventWithZeroSize);

        // Capture all ResizeEvent arguments in a single verification
        resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);
        verify(mockResizeEventListener, times(2)).onResizeEvent(resizeEventCaptor.capture());
        capturedEvents = resizeEventCaptor.getAllValues();

        // Event 2: RESIZE_STATE_END (invalid zero size proposed)
        assertThat(capturedEvents.get(1).getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        // When an invalid size is proposed via ReformEvent, ResizableComponentImpl
        // currently defaults to DEFAULT_SIZE (1.0, 1.0, 1.0)
        assertThat(options.getCurrentSize().x).isEqualTo(DEFAULT_SIZE.width);
        assertThat(options.getCurrentSize().y).isEqualTo(DEFAULT_SIZE.height);
        assertThat(options.getCurrentSize().z).isEqualTo(DEFAULT_SIZE.depth);
        assertThat(resizableComponent.getSize().width).isEqualTo(DEFAULT_SIZE.width);
        assertThat(resizableComponent.getSize().height).isEqualTo(DEFAULT_SIZE.height);
        assertThat(resizableComponent.getSize().depth).isEqualTo(DEFAULT_SIZE.depth);

        // 3. Send an END resize event with a valid proposed size
        ReformEvent endReformEventValid =
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_END, 0);
        ShadowReformEvent.extract(endReformEventValid).setProposedSize(new Vec3(4.0f, 5.0f, 6.0f));
        sendAndProcessReformEvent(entity.getNode(), endReformEventValid);

        // Capture all ResizeEvent arguments in a single verification
        resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);
        verify(mockResizeEventListener, times(3)).onResizeEvent(resizeEventCaptor.capture());
        capturedEvents = resizeEventCaptor.getAllValues();

        // Event 3: RESIZE_STATE_END (valid size proposed)
        assertThat(capturedEvents.get(2).getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        // Component and Node size should update to the newly proposed valid size
        assertThat(options.getCurrentSize().x).isEqualTo(4.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(5.0f);
        assertThat(options.getCurrentSize().z).isEqualTo(6.0f);
        assertThat(resizableComponent.getSize().width).isEqualTo(4.0f);
        assertThat(resizableComponent.getSize().height).isEqualTo(5.0f);
        assertThat(resizableComponent.getSize().depth).isEqualTo(6.0f);

        // 4. Send an END resize event with an invalid (width too small) proposed size
        ReformEvent endReformEventWidthTooSmall =
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_END, 0);
        ShadowReformEvent.extract(endReformEventWidthTooSmall)
                .setProposedSize(new Vec3(0.0f, 3.0f, 4.0f));
        sendAndProcessReformEvent(entity.getNode(), endReformEventWidthTooSmall);

        // Capture all ResizeEvent arguments in a single verification
        resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);
        verify(mockResizeEventListener, times(4)).onResizeEvent(resizeEventCaptor.capture());
        capturedEvents = resizeEventCaptor.getAllValues();

        // Event 4: RESIZE_STATE_END (invalid width too small proposed)
        assertThat(capturedEvents.get(3).getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        // Component and Node size should retain the last valid size (4.0, 5.0, 6.0)
        assertThat(options.getCurrentSize().x).isEqualTo(4.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(5.0f);
        assertThat(options.getCurrentSize().z).isEqualTo(6.0f);
        assertThat(resizableComponent.getSize().width).isEqualTo(4.0f);
        assertThat(resizableComponent.getSize().height).isEqualTo(5.0f);
        assertThat(resizableComponent.getSize().depth).isEqualTo(6.0f);

        // 5. Send an END resize event with an invalid (height too small) proposed size
        ReformEvent endReformEventHeightTooSmall =
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_END, 0);
        ShadowReformEvent.extract(endReformEventHeightTooSmall)
                .setProposedSize(new Vec3(2.0f, 0.0f, 4.0f));
        sendAndProcessReformEvent(entity.getNode(), endReformEventHeightTooSmall);

        // Capture all ResizeEvent arguments in a single verification
        resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);
        verify(mockResizeEventListener, times(5)).onResizeEvent(resizeEventCaptor.capture());
        capturedEvents = resizeEventCaptor.getAllValues();

        // Event 5: RESIZE_STATE_END (invalid height too small proposed)
        assertThat(capturedEvents.get(4).getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        // Component and Node size should retain the last valid size (4.0, 5.0, 6.0)
        assertThat(options.getCurrentSize().x).isEqualTo(4.0f);
        assertThat(options.getCurrentSize().y).isEqualTo(5.0f);
        assertThat(options.getCurrentSize().z).isEqualTo(6.0f);
        assertThat(resizableComponent.getSize().width).isEqualTo(4.0f);
        assertThat(resizableComponent.getSize().height).isEqualTo(5.0f);
        assertThat(resizableComponent.getSize().depth).isEqualTo(6.0f);

        // 6. Send an END resize event with an invalid (depth too small) proposed size
        ReformEvent endReformEventDepthTooSmall =
                ShadowReformEvent.create(REFORM_TYPE_RESIZE, REFORM_STATE_END, 0);
        ShadowReformEvent.extract(endReformEventDepthTooSmall)
                .setProposedSize(new Vec3(2.0f, 3.0f, 0.0f));
        sendAndProcessReformEvent(entity.getNode(), endReformEventDepthTooSmall);

        // Capture all ResizeEvent arguments in a single verification
        resizeEventCaptor = ArgumentCaptor.forClass(ResizeEvent.class);
        verify(mockResizeEventListener, times(6)).onResizeEvent(resizeEventCaptor.capture());
        capturedEvents = resizeEventCaptor.getAllValues();

        // Event 6: RESIZE_STATE_END (invalid depth too small proposed)
        assertThat(capturedEvents.get(5).getResizeState()).isEqualTo(ResizeEvent.RESIZE_STATE_END);
        // Component and Node size should retain the last valid size (4.0, 5.0, 6.0)
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
        assertThat(entity.mReformEventConsumerMap).isNotEmpty();

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
}
