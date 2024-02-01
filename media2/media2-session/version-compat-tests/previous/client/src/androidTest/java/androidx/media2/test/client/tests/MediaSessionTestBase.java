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

package androidx.media2.test.client.tests;

import android.content.Context;
import android.os.Bundle;
import android.os.HandlerThread;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaController.ControllerCallback;
import androidx.media2.session.SessionToken;
import androidx.media2.test.common.TestUtils.SyncHandler;
import androidx.test.core.app.ApplicationProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for session test.
 */
abstract class MediaSessionTestBase {
    static final int TIMEOUT_MS = 1000;
    static final int BROWSER_COMPAT_CONNECT_TIMEOUT_MS = 3000;

    static SyncHandler sHandler;
    static Executor sHandlerExecutor;

    Context mContext;
    private Map<MediaController, TestBrowserCallback> mControllers = new HashMap<>();

    interface TestControllerCallbackInterface {
        void waitForConnect(boolean expect) throws InterruptedException;
        void waitForDisconnect(boolean expect) throws InterruptedException;
        void setRunnableForOnCustomCommand(Runnable runnable);
    }

    @BeforeClass
    public static void setUpThread() {
        synchronized (MediaSessionTestBase.class) {
            if (sHandler != null) {
                return;
            }
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
            sHandler.getLooper().quitSafely();
            sHandler = null;
            sHandlerExecutor = null;
        }
    }

    @CallSuper
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @CallSuper
    public void cleanUp() throws Exception {
        for (MediaController controller : mControllers.keySet()) {
            controller.close();
        }
        mControllers.clear();
    }

    final MediaController createController(@NonNull MediaSessionCompat.Token token)
            throws InterruptedException {
        return createController(token, true, null);
    }

    final MediaController createController(@NonNull MediaSessionCompat.Token token,
            boolean waitForConnect, @Nullable ControllerCallback callback)
            throws InterruptedException {
        TestBrowserCallback testCallback = new TestBrowserCallback(callback);
        MediaController controller = onCreateController(token, testCallback);
        mControllers.put(controller, testCallback);
        if (waitForConnect) {
            waitForConnect(controller, true);
        }
        return controller;
    }

    final MediaController createController(@NonNull SessionToken token)
            throws InterruptedException {
        return createController(token, true, null, null);
    }

    final MediaController createController(@NonNull SessionToken token,
            boolean waitForConnect, @Nullable Bundle connectionHints,
            @Nullable ControllerCallback callback)
            throws InterruptedException {
        TestBrowserCallback testCallback = new TestBrowserCallback(callback);
        MediaController controller = onCreateController(token, connectionHints, testCallback);
        mControllers.put(controller, testCallback);
        if (waitForConnect) {
            waitForConnect(controller, true);
        }
        return controller;
    }

    final TestControllerCallbackInterface getTestControllerCallbackInterface(
            MediaController controller) {
        return mControllers.get(controller);
    }

    final void waitForConnect(MediaController controller, boolean expected)
            throws InterruptedException {
        getTestControllerCallbackInterface(controller).waitForConnect(expected);
    }

    final void waitForDisconnect(MediaController controller, boolean expected)
            throws InterruptedException {
        getTestControllerCallbackInterface(controller).waitForDisconnect(expected);
    }

    final void setRunnableForOnCustomCommand(MediaController controller,
            Runnable runnable) {
        getTestControllerCallbackInterface(controller).setRunnableForOnCustomCommand(runnable);
    }

    MediaController onCreateController(@NonNull final MediaSessionCompat.Token token,
            @NonNull final TestBrowserCallback callback) throws InterruptedException {
        final AtomicReference<MediaController> controller = new AtomicReference<>();

        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                // Create controller on the test handler, for changing MediaBrowserCompat's Handler
                // Looper. Otherwise, MediaBrowserCompat will post all the commands to the handler
                // and commands wouldn't be run if tests codes waits on the test handler.
                controller.set(new MediaController.Builder(mContext)
                        .setSessionCompatToken(token)
                        .setControllerCallback(sHandlerExecutor, callback)
                        .build());
            }
        });
        return controller.get();
    }

    MediaController onCreateController(@NonNull final SessionToken token,
            @Nullable final Bundle connectionHints, @NonNull final TestBrowserCallback callback)
            throws InterruptedException {
        final AtomicReference<MediaController> controller = new AtomicReference<>();
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                // Create controller on the test handler, for changing MediaBrowserCompat's Handler
                // Looper. Otherwise, MediaBrowserCompat will post all the commands to the handler
                // and commands wouldn't be run if tests codes waits on the test handler.
                MediaController.Builder builder = new MediaController.Builder(mContext)
                        .setSessionToken(token)
                        .setControllerCallback(sHandlerExecutor, callback);
                if (connectionHints != null) {
                    builder.setConnectionHints(connectionHints);
                }
                controller.set(builder.build());
            }
        });
        return controller.get();
    }
}
