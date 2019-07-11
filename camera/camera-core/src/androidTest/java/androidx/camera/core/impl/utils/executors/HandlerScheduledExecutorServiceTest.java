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

import static org.mockito.Mockito.mock;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class HandlerScheduledExecutorServiceTest {

    private static final long DELAYED_TASK_DELAY_MILLIS = 250;
    private static final int MAGIC_VALUE = 42;

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

    @Test
    @MediumTest
    public void canExecuteTaskInFuture() throws InterruptedException {
        final ScheduledExecutorService executor = CameraXExecutors.mainThreadExecutor();

        final AtomicLong startTimeMillis = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong executionTimeMillis = new AtomicLong(0);
        final Semaphore semaphore = new Semaphore(0);
        Runnable postDelayedTaskRunnable = new Runnable() {
            @Override
            public void run() {
                startTimeMillis.set(SystemClock.uptimeMillis());
                executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        // Mark execution time
                        executionTimeMillis.set(SystemClock.uptimeMillis() - startTimeMillis.get());
                        semaphore.release();
                    }
                }, DELAYED_TASK_DELAY_MILLIS, TimeUnit.MILLISECONDS);
            }
        };

        // Start the runnable which will set the start time and post the delayed runnable
        executor.execute(postDelayedTaskRunnable);

        // Wait for the task to complete
        semaphore.acquire();

        assertThat(executionTimeMillis.get()).isAtLeast(DELAYED_TASK_DELAY_MILLIS);
    }

    @Test
    @MediumTest
    public void canCancelScheduledTask() throws InterruptedException {
        final ScheduledExecutorService executor = CameraXExecutors.mainThreadExecutor();

        final AtomicBoolean cancelledTaskRan = new AtomicBoolean(false);
        final Semaphore semaphore = new Semaphore(0);
        Runnable postMultipleDelayedRunnable = new Runnable() {
            @Override
            public void run() {
                ScheduledFuture<?> futureToCancel = executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        // This should not occur
                        cancelledTaskRan.set(true);
                    }
                }, DELAYED_TASK_DELAY_MILLIS, TimeUnit.MILLISECONDS);

                // Schedule after the time where the above runnable would have ran if not cancelled.
                executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        // Allow test to finish
                        semaphore.release();
                    }
                }, DELAYED_TASK_DELAY_MILLIS + 1, TimeUnit.MILLISECONDS);

                // Cancel the first runnable. It should be impossible for it to have run since we
                // are currently running on the execution thread.
                futureToCancel.cancel(true);
            }
        };

        // Post both to the thread that the runnables will be scheduled on to eliminate the
        // chance of a race in cancelling the task.
        executor.execute(postMultipleDelayedRunnable);

        semaphore.acquire();

        assertThat(cancelledTaskRan.get()).isFalse();
    }

    @Test
    @MediumTest
    public void canRetrieveValueFromFuture() throws ExecutionException, InterruptedException {
        final ScheduledExecutorService executor = CameraXExecutors.mainThreadExecutor();
        ScheduledFuture<Integer> future = executor.schedule(new Callable<Integer>() {
            @Override
            public Integer call() {
                return MAGIC_VALUE;
            }
        }, DELAYED_TASK_DELAY_MILLIS, TimeUnit.MILLISECONDS);

        assertThat(future.get()).isEqualTo(MAGIC_VALUE);
    }

    @Test(expected = ExecutionException.class)
    @MediumTest
    public void schedulingOnShutdownLooperReturnsException()
            throws InterruptedException, ExecutionException {
        final AtomicReference<ScheduledExecutorService> executor = new AtomicReference<>(null);
        Thread thread = new Thread("canExecuteOnCurrentThreadExecutor_thread") {
            @Override
            public void run() {
                Looper.prepare();

                final ScheduledExecutorService currentExecutor =
                        CameraXExecutors.myLooperExecutor();

                currentExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        executor.set(currentExecutor);
                        Looper.myLooper().quitSafely();
                    }
                });

                Looper.loop();
            }
        };

        thread.start();
        thread.join();

        ScheduledFuture<?> future = executor.get().schedule(mock(Runnable.class),
                DELAYED_TASK_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        future.get();
    }
}
