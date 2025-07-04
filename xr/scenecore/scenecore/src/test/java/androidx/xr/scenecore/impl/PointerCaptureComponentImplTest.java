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

import static com.android.extensions.xr.node.InputEvent.ACTION_MOVE;
import static com.android.extensions.xr.node.InputEvent.DISPATCH_FLAG_NONE;
import static com.android.extensions.xr.node.InputEvent.POINTER_TYPE_DEFAULT;
import static com.android.extensions.xr.node.InputEvent.SOURCE_UNKNOWN;
import static com.android.extensions.xr.node.Node.POINTER_CAPTURE_STATE_ACTIVE;
import static com.android.extensions.xr.node.Node.POINTER_CAPTURE_STATE_PAUSED;
import static com.android.extensions.xr.node.Node.POINTER_CAPTURE_STATE_STOPPED;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.InputEvent;
import androidx.xr.runtime.internal.InputEventListener;
import androidx.xr.runtime.internal.PointerCaptureComponent;
import androidx.xr.runtime.internal.PointerCaptureComponent.StateListener;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.ShadowInputEvent;
import com.android.extensions.xr.node.ShadowNode;
import com.android.extensions.xr.node.Vec3;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PointerCaptureComponentImplTest {

    // Static private implementation of fakes so that the last received state can be grabbed.
    private static class FakeStateListener implements StateListener {
        public int lastState = -1;

        @Override
        public void onStateChanged(int newState) {
            lastState = newState;
        }
    }

    private static class FakeInputEventListener implements InputEventListener {
        public InputEvent lastEvent = null;

        @Override
        public void onInputEvent(@NonNull InputEvent event) {
            lastEvent = event;
        }
    }

    private final FakeStateListener mStateListener = new FakeStateListener();

    private final FakeInputEventListener mInputListener = new FakeInputEventListener();

    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final FakeScheduledExecutorService mFakeScheduler = new FakeScheduledExecutorService();
    private final Node mNode = mXrExtensions.createNode();
    private final ShadowNode mShadowNode = ShadowNode.extract(mNode);

    private final Entity mEntity =
            new AndroidXrEntity(null, mNode, mXrExtensions, new EntityManager(), mFakeScheduler) {};

    private void sendInputEvent(com.android.extensions.xr.node.InputEvent inputEvent) {
        mShadowNode
                .getInputExecutor()
                .execute(() -> mShadowNode.getInputListener().accept(inputEvent));
    }

    @Test
    public void onAttach_enablesPointerCapture() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);

        assertThat(component.onAttach(mEntity)).isTrue();

        assertThat(mShadowNode.getPointerCaptureStateCallback()).isNotNull();
    }

    @Test
    public void onAttach_setsUpInputEventPropagation() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        com.android.extensions.xr.node.InputEvent fakeInput =
                ShadowInputEvent.create(
                        SOURCE_UNKNOWN,
                        POINTER_TYPE_DEFAULT,
                        /* timestamp= */ 0,
                        /* origin= */ new Vec3(0, 0, 0),
                        /* direction= */ new Vec3(1, 1, 1),
                        com.android.extensions.xr.node.InputEvent.DISPATCH_FLAG_CAPTURED_POINTER,
                        ACTION_MOVE);
        sendInputEvent(fakeInput);
        mFakeScheduler.runAll();

        assertThat(mInputListener.lastEvent).isNotNull();
    }

    // This should really be a test on AndroidXrEntity, but that does not have tests so it is here
    // for
    // the meantime.
    @Test
    public void onAttach_onlyPropagatesCapturedEvents() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        com.android.extensions.xr.node.InputEvent fakeCapturedInput =
                ShadowInputEvent.create(
                        SOURCE_UNKNOWN,
                        POINTER_TYPE_DEFAULT,
                        /* timestamp= */ 100,
                        /* origin= */ new Vec3(0, 0, 0),
                        /* direction= */ new Vec3(1, 1, 1),
                        com.android.extensions.xr.node.InputEvent.DISPATCH_FLAG_CAPTURED_POINTER,
                        ACTION_MOVE);

        com.android.extensions.xr.node.InputEvent fakeInput =
                ShadowInputEvent.create(
                        SOURCE_UNKNOWN,
                        POINTER_TYPE_DEFAULT,
                        /* timestamp= */ 200,
                        /* origin= */ new Vec3(0, 0, 0),
                        /* direction= */ new Vec3(1, 1, 1),
                        DISPATCH_FLAG_NONE,
                        ACTION_MOVE);

        sendInputEvent(fakeCapturedInput);
        sendInputEvent(fakeInput);

        mFakeScheduler.runAll();

        assertThat(mInputListener.lastEvent).isNotNull();
        assertThat(mInputListener.lastEvent.getTimestamp())
                .isEqualTo(fakeCapturedInput.getTimestamp());
    }

    @Test
    public void onAttach_propagatesInputOnCorrectThread() {
        FakeScheduledExecutorService propagationExecutor = new FakeScheduledExecutorService();
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(
                        propagationExecutor, mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        com.android.extensions.xr.node.InputEvent fakeCapturedInput =
                ShadowInputEvent.create(
                        SOURCE_UNKNOWN,
                        POINTER_TYPE_DEFAULT,
                        /* timestamp= */ 100,
                        /* origin= */ new Vec3(0, 0, 0),
                        /* direction= */ new Vec3(1, 1, 1),
                        com.android.extensions.xr.node.InputEvent.DISPATCH_FLAG_CAPTURED_POINTER,
                        ACTION_MOVE);

        sendInputEvent(fakeCapturedInput);

        assertThat(propagationExecutor.hasNext()).isFalse();
        // Run the scheduler associated with the Entity so that the component's executor has the
        // task
        // scheduled on it.
        mFakeScheduler.runAll();

        assertThat(propagationExecutor.hasNext()).isTrue();
    }

    @Test
    public void onAttach_setsUpCorrectStatePropagation() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        mShadowNode.getPointerCaptureStateCallback().accept(POINTER_CAPTURE_STATE_PAUSED);
        assertThat(mStateListener.lastState)
                .isEqualTo(
                        PointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_PAUSED);

        mShadowNode.getPointerCaptureStateCallback().accept(POINTER_CAPTURE_STATE_ACTIVE);
        assertThat(mStateListener.lastState)
                .isEqualTo(
                        PointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE);

        mShadowNode.getPointerCaptureStateCallback().accept(POINTER_CAPTURE_STATE_STOPPED);
        assertThat(mStateListener.lastState)
                .isEqualTo(
                        PointerCaptureComponent.PointerCaptureState.POINTER_CAPTURE_STATE_STOPPED);
    }

    @Test
    public void onAttach_failsIfAlreadyAttached() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();
        assertThat(component.onAttach(mEntity)).isFalse();
    }

    @Test
    public void onAttach_failesIfEntityAlreadyHasAnAttachedPointerCaptureComponent() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        PointerCaptureComponentImpl component2 =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component2.onAttach(mEntity)).isFalse();
    }

    @Test
    public void onDetach_stopsPointerCapture() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        component.onDetach(mEntity);

        assertThat(mShadowNode.getPointerCaptureStateCallback()).isNull();
    }

    @Test
    public void onDetach_removesInputListener() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        component.onDetach(mEntity);

        assertThat(mShadowNode.getInputListener()).isNull();
    }
}
