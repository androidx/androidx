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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.InputEventListener;
import androidx.xr.runtime.internal.InteractableComponent;
import androidx.xr.runtime.math.Pose;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.InputEvent;
import com.android.extensions.xr.node.ShadowInputEvent;
import com.android.extensions.xr.node.ShadowNode;
import com.android.extensions.xr.node.Vec3;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.apibindings.FakeImpressApiImpl;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
public class InteractableComponentImplTest {
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary mPerceptionLibrary = mock(PerceptionLibrary.class);
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final FakeImpressApiImpl mFakeImpressApi = new FakeImpressApiImpl();
    private JxrPlatformAdapterAxr mFakeRuntime;
    SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    ImpSplitEngineRenderer mSplitEngineRenderer = Mockito.mock(ImpSplitEngineRenderer.class);

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
                        new EntityManager(),
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

    private void sendInputEvent(ShadowNode node, InputEvent inputEvent) {
        node.getInputExecutor().execute(() -> node.getInputListener().accept(inputEvent));
    }

    @Test
    public void addInteractableComponent_addsListenerToNode() {
        Entity entity = createTestEntity();
        Executor executor = directExecutor();
        InputEventListener inputEventListener = mock(InputEventListener.class);
        InteractableComponent interactableComponent =
                new InteractableComponentImpl(executor, inputEventListener);
        assertThat(entity.addComponent(interactableComponent)).isTrue();
        ShadowNode node = ShadowNode.extract(((AndroidXrEntity) entity).getNode());

        assertThat(node.getInputListener()).isNotNull();
        assertThat(node.getInputExecutor()).isEqualTo(mFakeExecutor);

        InputEvent inputEvent =
                ShadowInputEvent.create(
                        /* origin= */ new Vec3(0, 0, 0), /* direction= */ new Vec3(1, 1, 1));
        sendInputEvent(node, inputEvent);
        mFakeExecutor.runAll();

        assertThat(((AndroidXrEntity) entity).mInputEventListenerMap).isNotEmpty();
        verify(inputEventListener).onInputEvent(any());
    }

    @Test
    public void removeInteractableComponent_removesListenerFromNode() {
        Entity entity = createTestEntity();
        Executor executor = directExecutor();
        InputEventListener inputEventListener = mock(InputEventListener.class);
        InteractableComponent interactableComponent =
                new InteractableComponentImpl(executor, inputEventListener);
        assertThat(entity.addComponent(interactableComponent)).isTrue();
        ShadowNode node = ShadowNode.extract(((AndroidXrEntity) entity).getNode());

        assertThat(node.getInputListener()).isNotNull();
        assertThat(node.getInputExecutor()).isEqualTo(mFakeExecutor);

        InputEvent inputEvent =
                ShadowInputEvent.create(
                        /* origin= */ new Vec3(0, 0, 0), /* direction= */ new Vec3(1, 1, 1));
        sendInputEvent(node, inputEvent);
        mFakeExecutor.runAll();

        assertThat(((AndroidXrEntity) entity).mInputEventListenerMap).isNotEmpty();
        verify(inputEventListener).onInputEvent(any());

        entity.removeComponent(interactableComponent);
        assertThat(node.getInputListener()).isNull();
        assertThat(node.getInputExecutor()).isNull();
    }

    @Test
    public void interactableComponent_canAttachOnlyOnce() {
        Entity entity = createTestEntity();
        Entity entity2 = createTestEntity();
        Executor executor = directExecutor();
        InputEventListener inputEventListener = mock(InputEventListener.class);
        InteractableComponent interactableComponent =
                new InteractableComponentImpl(executor, inputEventListener);
        assertThat(entity.addComponent(interactableComponent)).isTrue();
        assertThat(entity2.addComponent(interactableComponent)).isFalse();
    }

    @Test
    public void interactableComponent_canAttachAgainAfterDetach() {
        Entity entity = createTestEntity();
        Executor executor = directExecutor();
        InputEventListener inputEventListener = mock(InputEventListener.class);
        InteractableComponent interactableComponent =
                new InteractableComponentImpl(executor, inputEventListener);
        assertThat(entity.addComponent(interactableComponent)).isTrue();
        entity.removeComponent(interactableComponent);
        assertThat(entity.addComponent(interactableComponent)).isTrue();
    }

    @Test
    public void interactableComponent_enablesColliderForGltfEntity() {
        GltfEntityImpl gltfEntity = mock(GltfEntityImpl.class);
        Executor executor = directExecutor();
        InputEventListener inputEventListener = mock(InputEventListener.class);
        InteractableComponent interactableComponent =
                new InteractableComponentImpl(executor, inputEventListener);
        assertThat(interactableComponent.onAttach(gltfEntity)).isTrue();
        verify(gltfEntity).setColliderEnabled(true);
    }
}
