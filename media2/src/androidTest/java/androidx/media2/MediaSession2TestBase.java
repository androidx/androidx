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

package androidx.media2;

import android.content.Context;
import android.os.Build;
import android.os.HandlerThread;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.MediaController2.ControllerCallback;
import androidx.media2.TestUtils.SyncHandler;
import androidx.test.InstrumentationRegistry;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for session test.
 * <p>
 * For all subclasses, all individual tests should begin with the {@link #prepareLooper()}. See
 * {@link #prepareLooper} for details.
 */
abstract class MediaSession2TestBase extends MediaTestBase {
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
                        mContext, token, new MockControllerCallback(controllerCallback)));
            }
        });
        return controller.get();
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
