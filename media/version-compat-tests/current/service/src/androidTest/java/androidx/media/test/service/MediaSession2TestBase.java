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

package androidx.media.test.service;

import android.content.Context;
import android.os.Build;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.media.SessionToken2;
import androidx.media.test.lib.TestUtils.SyncHandler;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

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
    private List<RemoteMediaController2> mControllers = new ArrayList<>();

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
            mControllers.get(i).cleanUp();
        }
    }

    final RemoteMediaController2 createRemoteController2(SessionToken2 token)
            throws InterruptedException {
        return createRemoteController2(token, true);
    }

    final RemoteMediaController2 createRemoteController2(@NonNull SessionToken2 token,
            boolean waitForConnection) throws InterruptedException {
        RemoteMediaController2 controller2 =
                new RemoteMediaController2(mContext, token, waitForConnection);
        mControllers.add(controller2);
        return controller2;
    }
}
