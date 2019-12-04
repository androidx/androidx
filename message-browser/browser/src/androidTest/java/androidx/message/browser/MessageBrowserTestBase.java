/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.message.browser;

import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Base class for androidx.message tests.
 */
public class MessageBrowserTestBase {
    private static final String TAG = "MsgBrowserTestBase";
    private static final int TIMEOUT_MS = 1000;

    static SyncHandler sHandler;
    static Executor sHandlerExecutor;

    @BeforeClass
    public static void setUpThread() {
        synchronized (MessageBrowserTest.class) {
            if (sHandler != null) {
                return;
            }
            if (Looper.myLooper() == null) {
                Looper.prepare();
            }
            HandlerThread handlerThread = new HandlerThread(TAG);
            handlerThread.start();
            sHandler = new SyncHandler(handlerThread.getLooper());
            sHandlerExecutor = new Executor() {
                @Override
                public void execute(Runnable runnable) {
                    SyncHandler handler;
                    synchronized (MessageBrowserTestBase.class) {
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
        synchronized (MessageBrowserTestBase.class) {
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

    /**
     * Handler that always waits until the Runnable finishes.
     */
    static class SyncHandler extends Handler {
        SyncHandler(Looper looper) {
            super(looper);
        }

        public void postAndSync(final Runnable runnable) throws InterruptedException {
            if (getLooper() == Looper.myLooper()) {
                runnable.run();
            } else {
                final CountDownLatch latch = new CountDownLatch(1);
                post(new Runnable() {
                    @Override
                    public void run() {
                        runnable.run();
                        latch.countDown();
                    }
                });
                assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            }
        }
    }
}
