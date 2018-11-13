/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media.test.client.tests;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.test.lib.TestUtils.SyncHandler;
import androidx.media2.MediaController;
import androidx.media2.MediaController.ControllerCallback;
import androidx.media2.MediaItem;
import androidx.media2.MediaMetadata;
import androidx.media2.MediaSession.CommandButton;
import androidx.media2.SessionCommand;
import androidx.media2.SessionCommandGroup;
import androidx.media2.SessionToken;
import androidx.test.InstrumentationRegistry;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for session test.
 * <p>
 * For all subclasses, all individual tests should begin with the {@link #prepareLooper()}. See
 * {@link #prepareLooper} for details.
 */
abstract class MediaSessionTestBase {
    static final int TIMEOUT_MS = 1000;

    static SyncHandler sHandler;
    static Executor sHandlerExecutor;

    Context mContext;
    private List<MediaController> mControllers = new ArrayList<>();

    interface TestControllerInterface {
        ControllerCallback getCallback();
    }

    interface TestControllerCallbackInterface {
        void waitForConnect(boolean expect) throws InterruptedException;
        void waitForDisconnect(boolean expect) throws InterruptedException;
        void setRunnableForOnCustomCommand(Runnable runnable);
    }

    /**
     * All tests methods should start with this.
     * <p>
     * MediaControllerCompat, which is wrapped by the MediaSession, can be only created by the
     * thread whose Looper is prepared. However, when the presubmit tests runs on the server,
     * test runs with the {@link org.junit.internal.runners.statements.FailOnTimeout} which creates
     * dedicated thread for running test methods while methods annotated with @After or @Before
     * runs on the different thread. This ensures that the current Looper is prepared.
     * <p>
     * To address the issue .
     */
    public static void prepareLooper() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    @BeforeClass
    public static void setUpThread() {
        synchronized (MediaSessionTestBase.class) {
            if (sHandler != null) {
                return;
            }
            prepareLooper();
            HandlerThread handlerThread = new HandlerThread("MediaSessionTestBase");
            handlerThread.start();
            sHandler = new SyncHandler(handlerThread.getLooper());
            sHandlerExecutor = new Executor() {
                @Override
                public void execute(Runnable runnable) {
                    SyncHandler handler;
                    synchronized (MediaSessionTestBase.class) {
                        handler = sHandler;
                    }
                    if (handler != null) {
                        handler.post(runnable);
                    }
                }
            };
        }
    }

    @AfterClass
    public static void cleanUpThread() {
        synchronized (MediaSessionTestBase.class) {
            if (sHandler == null) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 18) {
                sHandler.getLooper().quitSafely();
            } else {
                sHandler.getLooper().quit();
            }
            sHandler = null;
            sHandlerExecutor = null;
        }
    }

    @CallSuper
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @CallSuper
    public void cleanUp() throws Exception {
        for (int i = 0; i < mControllers.size(); i++) {
            mControllers.get(i).close();
        }
    }

    final MediaController createController(@NonNull MediaSessionCompat.Token token)
            throws InterruptedException {
        return createController(token, true, null);
    }

    final MediaController createController(@NonNull MediaSessionCompat.Token token,
            boolean waitForConnect, @Nullable ControllerCallback callback)
            throws InterruptedException {
        TestControllerInterface instance = onCreateController(token, callback);
        if (!(instance instanceof MediaController)) {
            throw new RuntimeException("Test has a bug. Expected MediaController but returned "
                    + instance);
        }
        MediaController controller = (MediaController) instance;
        mControllers.add(controller);
        if (waitForConnect) {
            waitForConnect(controller, true);
        }
        return controller;
    }

    final MediaController createController(@NonNull SessionToken token)
            throws InterruptedException {
        return createController(token, true, null);
    }

    final MediaController createController(@NonNull SessionToken token,
            boolean waitForConnect, @Nullable ControllerCallback callback)
            throws InterruptedException {
        TestControllerInterface instance = onCreateController(token, callback);
        if (!(instance instanceof MediaController)) {
            throw new RuntimeException("Test has a bug. Expected MediaController but returned "
                    + instance);
        }
        MediaController controller = (MediaController) instance;
        mControllers.add(controller);
        if (waitForConnect) {
            waitForConnect(controller, true);
        }
        return controller;
    }

    private static TestControllerCallbackInterface getTestControllerCallbackInterface(
            MediaController controller) {
        if (!(controller instanceof TestControllerInterface)) {
            throw new RuntimeException("Test has a bug. Expected controller implemented"
                    + " TestControllerInterface but got " + controller);
        }
        ControllerCallback callback = ((TestControllerInterface) controller).getCallback();
        if (!(callback instanceof TestControllerCallbackInterface)) {
            throw new RuntimeException("Test has a bug. Expected controller with callback "
                    + " implemented TestControllerCallbackInterface but got " + controller);
        }
        return (TestControllerCallbackInterface) callback;
    }

    public static void waitForConnect(MediaController controller, boolean expected)
            throws InterruptedException {
        getTestControllerCallbackInterface(controller).waitForConnect(expected);
    }

    public static void waitForDisconnect(MediaController controller, boolean expected)
            throws InterruptedException {
        getTestControllerCallbackInterface(controller).waitForDisconnect(expected);
    }

    public static void setRunnableForOnCustomCommand(MediaController controller,
            Runnable runnable) {
        getTestControllerCallbackInterface(controller).setRunnableForOnCustomCommand(runnable);
    }

    TestControllerInterface onCreateController(final @NonNull MediaSessionCompat.Token token,
            @Nullable ControllerCallback callback) throws InterruptedException {
        final ControllerCallback controllerCallback =
                callback != null ? callback : new ControllerCallback() {};
        final AtomicReference<TestControllerInterface> controller = new AtomicReference<>();

        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                // Create controller on the test handler, for changing MediaBrowserCompat's Handler
                // Looper. Otherwise, MediaBrowserCompat will post all the commands to the handler
                // and commands wouldn't be run if tests codes waits on the test handler.
                controller.set(new TestMediaController(
                        mContext, token, new TestControllerCallback(controllerCallback)));
            }
        });
        return controller.get();
    }

    TestControllerInterface onCreateController(final @NonNull SessionToken token,
            @Nullable ControllerCallback callback) throws InterruptedException {
        final ControllerCallback controllerCallback =
                callback != null ? callback : new ControllerCallback() {};
        final AtomicReference<TestControllerInterface> controller = new AtomicReference<>();
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                // Create controller on the test handler, for changing MediaBrowserCompat's Handler
                // Looper. Otherwise, MediaBrowserCompat will post all the commands to the handler
                // and commands wouldn't be run if tests codes waits on the test handler.
                controller.set(new TestMediaController(
                        mContext, token, new TestControllerCallback(controllerCallback)));
            }
        });
        return controller.get();
    }

    // TODO(jaewan): (Can be Post-P): Deprecate this
    public static class TestControllerCallback extends MediaController.ControllerCallback
            implements TestControllerCallbackInterface {
        public final ControllerCallback mCallbackProxy;
        public final CountDownLatch connectLatch = new CountDownLatch(1);
        public final CountDownLatch disconnectLatch = new CountDownLatch(1);
        @GuardedBy("this")
        private Runnable mOnCustomCommandRunnable;

        TestControllerCallback(@NonNull ControllerCallback callbackProxy) {
            if (callbackProxy == null) {
                throw new IllegalArgumentException("Callback proxy shouldn't be null. Test bug");
            }
            mCallbackProxy = callbackProxy;
        }

        @CallSuper
        @Override
        public void onConnected(MediaController controller, SessionCommandGroup commands) {
            connectLatch.countDown();
        }

        @CallSuper
        @Override
        public void onDisconnected(MediaController controller) {
            disconnectLatch.countDown();
        }

        @Override
        public void waitForConnect(boolean expect) throws InterruptedException {
            if (expect) {
                assertTrue(connectLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            } else {
                assertFalse(connectLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            }
        }

        @Override
        public void waitForDisconnect(boolean expect) throws InterruptedException {
            if (expect) {
                assertTrue(disconnectLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            } else {
                assertFalse(disconnectLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            }
        }

        @Override
        public MediaController.ControllerResult onCustomCommand(MediaController controller,
                SessionCommand command, Bundle args) {
            synchronized (this) {
                if (mOnCustomCommandRunnable != null) {
                    mOnCustomCommandRunnable.run();
                }
            }
            return mCallbackProxy.onCustomCommand(controller, command, args);
        }

        @Override
        public void onPlaybackInfoChanged(MediaController controller,
                MediaController.PlaybackInfo info) {
            mCallbackProxy.onPlaybackInfoChanged(controller, info);
        }

        @Override
        public int onSetCustomLayout(MediaController controller, List<CommandButton> layout) {
            return mCallbackProxy.onSetCustomLayout(controller, layout);
        }

        @Override
        public void onAllowedCommandsChanged(MediaController controller,
                SessionCommandGroup commands) {
            mCallbackProxy.onAllowedCommandsChanged(controller, commands);
        }

        @Override
        public void onPlayerStateChanged(MediaController controller, int state) {
            mCallbackProxy.onPlayerStateChanged(controller, state);
        }

        @Override
        public void onSeekCompleted(MediaController controller, long position) {
            mCallbackProxy.onSeekCompleted(controller, position);
        }

        @Override
        public void onPlaybackSpeedChanged(MediaController controller, float speed) {
            mCallbackProxy.onPlaybackSpeedChanged(controller, speed);
        }

        @Override
        public void onBufferingStateChanged(MediaController controller, MediaItem item,
                int state) {
            mCallbackProxy.onBufferingStateChanged(controller, item, state);
        }

        @Override
        public void onCurrentMediaItemChanged(MediaController controller, MediaItem item) {
            mCallbackProxy.onCurrentMediaItemChanged(controller, item);
        }

        @Override
        public void onPlaylistChanged(MediaController controller,
                List<MediaItem> list, MediaMetadata metadata) {
            mCallbackProxy.onPlaylistChanged(controller, list, metadata);
        }

        @Override
        public void onPlaylistMetadataChanged(MediaController controller,
                MediaMetadata metadata) {
            mCallbackProxy.onPlaylistMetadataChanged(controller, metadata);
        }

        @Override
        public void onShuffleModeChanged(MediaController controller, int shuffleMode) {
            mCallbackProxy.onShuffleModeChanged(controller, shuffleMode);
        }

        @Override
        public void onRepeatModeChanged(MediaController controller, int repeatMode) {
            mCallbackProxy.onRepeatModeChanged(controller, repeatMode);
        }

        @Override
        public void onPlaybackCompleted(MediaController controller) {
            mCallbackProxy.onPlaybackCompleted(controller);
        }

        @Override
        public void setRunnableForOnCustomCommand(Runnable runnable) {
            synchronized (this) {
                mOnCustomCommandRunnable = runnable;
            }
        }
    }

    public class TestMediaController extends MediaController implements TestControllerInterface {
        private final ControllerCallback mCallback;

        TestMediaController(@NonNull Context context, @NonNull MediaSessionCompat.Token token,
                @NonNull ControllerCallback callback) {
            super(context, token, sHandlerExecutor, callback);
            mCallback = callback;
        }

        TestMediaController(@NonNull Context context, @NonNull SessionToken token,
                @NonNull ControllerCallback callback) {
            super(context, token, sHandlerExecutor, callback);
            mCallback = callback;
        }

        @Override
        public ControllerCallback getCallback() {
            return mCallback;
        }
    }
}
