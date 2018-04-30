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

package androidx.media;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.test.InstrumentationRegistry;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaController2.ControllerCallback;
import androidx.media.MediaSession2.CommandButton;
import androidx.media.TestUtils.SyncHandler;

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
abstract class MediaSession2TestBase {
    // Expected success
    static final int WAIT_TIME_MS = 1000;

    // Expected timeout
    static final int TIMEOUT_MS = 500;

    static SyncHandler sHandler;
    static Executor sHandlerExecutor;

    Context mContext;
    private List<MediaController2> mControllers = new ArrayList<>();

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
     * MediaControllerCompat, which is wrapped by the MediaSession2, can be only created by the
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
        synchronized (MediaSession2TestBase.class) {
            if (sHandler != null) {
                return;
            }
            prepareLooper();
            HandlerThread handlerThread = new HandlerThread("MediaSession2TestBase");
            handlerThread.start();
            sHandler = new SyncHandler(handlerThread.getLooper());
            sHandlerExecutor = new Executor() {
                @Override
                public void execute(Runnable runnable) {
                    SyncHandler handler;
                    synchronized (MediaSession2TestBase.class) {
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
        synchronized (MediaSession2TestBase.class) {
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

    final MediaController2 createController(SessionToken2 token) throws InterruptedException {
        return createController(token, true, null);
    }

    final MediaController2 createController(@NonNull SessionToken2 token,
            boolean waitForConnect, @Nullable ControllerCallback callback)
            throws InterruptedException {
        TestControllerInterface instance = onCreateController(token, callback);
        if (!(instance instanceof MediaController2)) {
            throw new RuntimeException("Test has a bug. Expected MediaController2 but returned "
                    + instance);
        }
        MediaController2 controller = (MediaController2) instance;
        mControllers.add(controller);
        if (waitForConnect) {
            waitForConnect(controller, true);
        }
        return controller;
    }

    private static TestControllerCallbackInterface getTestControllerCallbackInterface(
            MediaController2 controller) {
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

    public static void waitForConnect(MediaController2 controller, boolean expected)
            throws InterruptedException {
        getTestControllerCallbackInterface(controller).waitForConnect(expected);
    }

    public static void waitForDisconnect(MediaController2 controller, boolean expected)
            throws InterruptedException {
        getTestControllerCallbackInterface(controller).waitForDisconnect(expected);
    }

    public static void setRunnableForOnCustomCommand(MediaController2 controller,
            Runnable runnable) {
        getTestControllerCallbackInterface(controller).setRunnableForOnCustomCommand(runnable);
    }

    TestControllerInterface onCreateController(final @NonNull SessionToken2 token,
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
    public static class TestControllerCallback extends MediaController2.ControllerCallback
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
        public void onConnected(MediaController2 controller, SessionCommandGroup2 commands) {
            connectLatch.countDown();
        }

        @CallSuper
        @Override
        public void onDisconnected(MediaController2 controller) {
            disconnectLatch.countDown();
        }

        @Override
        public void waitForConnect(boolean expect) throws InterruptedException {
            if (expect) {
                assertTrue(connectLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
            } else {
                assertFalse(connectLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            }
        }

        @Override
        public void waitForDisconnect(boolean expect) throws InterruptedException {
            if (expect) {
                assertTrue(disconnectLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
            } else {
                assertFalse(disconnectLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            }
        }

        @Override
        public void onCustomCommand(MediaController2 controller, SessionCommand2 command,
                Bundle args, ResultReceiver receiver) {
            mCallbackProxy.onCustomCommand(controller, command, args, receiver);
            synchronized (this) {
                if (mOnCustomCommandRunnable != null) {
                    mOnCustomCommandRunnable.run();
                }
            }
        }

        @Override
        public void onPlaybackInfoChanged(MediaController2 controller,
                MediaController2.PlaybackInfo info) {
            mCallbackProxy.onPlaybackInfoChanged(controller, info);
        }

        @Override
        public void onCustomLayoutChanged(MediaController2 controller, List<CommandButton> layout) {
            mCallbackProxy.onCustomLayoutChanged(controller, layout);
        }

        @Override
        public void onAllowedCommandsChanged(MediaController2 controller,
                SessionCommandGroup2 commands) {
            mCallbackProxy.onAllowedCommandsChanged(controller, commands);
        }

        @Override
        public void onPlayerStateChanged(MediaController2 controller, int state) {
            mCallbackProxy.onPlayerStateChanged(controller, state);
        }

        @Override
        public void onSeekCompleted(MediaController2 controller, long position) {
            mCallbackProxy.onSeekCompleted(controller, position);
        }

        @Override
        public void onPlaybackSpeedChanged(MediaController2 controller, float speed) {
            mCallbackProxy.onPlaybackSpeedChanged(controller, speed);
        }

        @Override
        public void onBufferingStateChanged(MediaController2 controller, MediaItem2 item,
                int state) {
            mCallbackProxy.onBufferingStateChanged(controller, item, state);
        }

        @Override
        public void onError(MediaController2 controller, int errorCode, Bundle extras) {
            mCallbackProxy.onError(controller, errorCode, extras);
        }

        @Override
        public void onCurrentMediaItemChanged(MediaController2 controller, MediaItem2 item) {
            mCallbackProxy.onCurrentMediaItemChanged(controller, item);
        }

        @Override
        public void onPlaylistChanged(MediaController2 controller,
                List<MediaItem2> list, MediaMetadata2 metadata) {
            mCallbackProxy.onPlaylistChanged(controller, list, metadata);
        }

        @Override
        public void onPlaylistMetadataChanged(MediaController2 controller,
                MediaMetadata2 metadata) {
            mCallbackProxy.onPlaylistMetadataChanged(controller, metadata);
        }

        @Override
        public void onShuffleModeChanged(MediaController2 controller, int shuffleMode) {
            mCallbackProxy.onShuffleModeChanged(controller, shuffleMode);
        }

        @Override
        public void onRepeatModeChanged(MediaController2 controller, int repeatMode) {
            mCallbackProxy.onRepeatModeChanged(controller, repeatMode);
        }

        @Override
        public void setRunnableForOnCustomCommand(Runnable runnable) {
            synchronized (this) {
                mOnCustomCommandRunnable = runnable;
            }
        }

        @Override
        public void onRoutesInfoChanged(@NonNull MediaController2 controller,
                @Nullable List<Bundle> routes) {
            mCallbackProxy.onRoutesInfoChanged(controller, routes);
        }
    }

    public class TestMediaController extends MediaController2 implements TestControllerInterface {
        private final ControllerCallback mCallback;

        TestMediaController(@NonNull Context context, @NonNull SessionToken2 token,
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
