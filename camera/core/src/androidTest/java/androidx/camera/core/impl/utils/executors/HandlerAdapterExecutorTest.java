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

package androidx.camera.core.impl.utils.executors;

import static com.google.common.truth.Truth.assertThat;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class HandlerAdapterExecutorTest {

    @Test
    public void canExecuteOnCurrentThreadExecutor() throws InterruptedException {
        final AtomicBoolean executed = new AtomicBoolean(false);
        Thread thread = new Thread("canExecuteOnCurrentThreadExecutor_thread") {
            @Override
            public void run() {
                Looper.prepare();

                Executor currentExecutor = CameraXExecutors.myLooperExecutor();

                currentExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        executed.set(true);
                        Looper.myLooper().quitSafely();
                    }
                });

                Looper.loop();
            }
        };

        thread.start();
        thread.join();

        assertThat(executed.get()).isTrue();
    }

    @Test
    public void retrieveCurrentThreadExecutor_throwsOnNonLooperThread()
            throws InterruptedException {
        final AtomicReference<Throwable> thrownException = new AtomicReference<>(null);

        Thread thread = new Thread("retrieveCurrentThreadExecutor_throwsOnNonLooperThread_thread") {
            @Override
            public void run() {
                try {
                    CameraXExecutors.myLooperExecutor();
                } catch (Throwable throwable) {
                    thrownException.set(throwable);
                }

            }
        };

        thread.start();
        thread.join();

        assertThat(thrownException.get()).isNotNull();
        assertThat(thrownException.get()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @UiThreadTest
    public void canRetrieveMainThreadExecutor_withCurrentThreadExecutor() {
        // Current thread is main thread since this test is annotated @UiThreadTest
        Executor currentThreadExecutor = CameraXExecutors.myLooperExecutor();
        Executor mainThreadExecutor = CameraXExecutors.mainThreadExecutor();

        assertThat(currentThreadExecutor).isSameInstanceAs(mainThreadExecutor);
    }

    @Test
    public void canWrapHandlerAndExecute() throws InterruptedException {
        final HandlerThread handlerThread = new HandlerThread("canWrapHandlerAndExecute_thread");
        handlerThread.start();

        Handler handler = new Handler(handlerThread.getLooper());

        Executor executor = CameraXExecutors.newHandlerExecutor(handler);
        final Semaphore semaphore = new Semaphore(0);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                // Clean up the handlerThread while we're here.
                handlerThread.quitSafely();
                semaphore.release();
            }
        });

        // Wait for the thread to execute
        semaphore.acquire();

        // No need to assert. If we don't time out, the test passed.
    }
}
