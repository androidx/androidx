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

package androidx.concurrent.futures;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;

import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.concurrent.futures.CallbackToFutureAdapter.FutureGarbageCollectedException;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(JUnit4.class)
public class CallbackToFutureAdapterTest {

    private static final long TIMEOUT_NANOS = SECONDS.toNanos(10);
    private static final long GC_AWAIT_TIME_MS = 200L;

    @Test
    public void testSuccess() {
        final AtomicReference<Completer<String>> completerRef = new AtomicReference<>();
        ListenableFuture<String> future =
                CallbackToFutureAdapter.getFuture(
                        new CallbackToFutureAdapter.Resolver<String>() {
                            @Override
                            public Object attachCompleter(Completer<String> completer) {
                                completerRef.set(completer);
                                return "my special callback";
                            }
                        });
        assertThat(future.isDone()).isFalse();
        assertThat(future.toString()).contains("my special callback");
        completerRef.get().set("my special result");
        assertFutureCompletedWith(future, "my special result");
    }

    private static <T> void assertFutureCompletedWith(Future<T> future, T expectedValue) {
        if (!future.isDone()) {
            throw new AssertionError("Future isn't done yet, "
                    + "but was expected to complete successfully with " + expectedValue);
        }
        if (future.isCancelled()) {
            throw new AssertionError("Future was cancelled,"
                    + "but was expected to complete successfully with " + expectedValue);
        }
        T t;
        try {
            t = future.get();
        } catch (Exception e) {
            throw new AssertionError("Future failed, "
                    + "but was expected to complete successfully with " + expectedValue, e);
        }
        assertThat(t).isEqualTo(t);
    }

    @Test
    public void testGcedCallback() throws Exception {
        AtomicBoolean wasCalled = new AtomicBoolean();
        ListenableFuture<String> future = exampleLeakyCallbackAdapter(wasCalled);
        final long deadline = System.nanoTime() + TIMEOUT_NANOS;
        while (!future.isDone() && (deadline - System.nanoTime() > 0)) {
            CountDownLatch latch = new CountDownLatch(1);
            createUnreachableLatchFinalizer(latch);
            System.gc();
            System.runFinalization();
            latch.await(GC_AWAIT_TIME_MS, TimeUnit.MILLISECONDS);
        }
        assertThat(wasCalled.get()).isTrue();
        try {
            future.get();
            Assert.fail("Future was expected to fail");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(FutureGarbageCollectedException.class);
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception instead "
                    + "of FutureGarbageCollectedException", e);
        }
    }
    /**
     * Creates a garbage object that counts down the latch in its finalizer. Sequestered into a
     * separate method to make it somewhat more likely to be unreachable.
     */
    private static void createUnreachableLatchFinalizer(final CountDownLatch latch) {
        new Object() {
            @Override
            protected void finalize() {
                latch.countDown();
            }
        };
    }

    private ListenableFuture<String> exampleLeakyCallbackAdapter(final AtomicBoolean wasCalled) {
        return CallbackToFutureAdapter.getFuture(
                new CallbackToFutureAdapter.Resolver<String>() {
                    @Override
                    public Object attachCompleter(final Completer<String> completer) {
                        // Callback (used as tag) has a hard reference on completer
                        Runnable myCallback = new Runnable() {
                            @Override
                            public void run() {
                                completer.set("My callback ran");
                            }
                        };
                        completer.addCancellationListener(new Runnable() {
                            @Override
                            public void run() {
                                wasCalled.set(true);
                            }
                        }, directExecutor());
                        // Whoops! Forgot to actually call the callback!
                        return myCallback;
                    }
                });
    }

    @Test
    public void testCancellationListenersCalledOnFutureCancelled() {
        final AtomicReference<Completer<String>> completerRef = new AtomicReference<>();
        final AtomicBoolean wasCalled = new AtomicBoolean();
        ListenableFuture<String> future =
                CallbackToFutureAdapter.getFuture(
                        new CallbackToFutureAdapter.Resolver<String>() {
                            @Override
                            public Object attachCompleter(Completer<String> completer) {
                                completer.addCancellationListener(new Runnable() {
                                    @Override
                                    public void run() {
                                        wasCalled.set(true);
                                    }
                                }, DirectExecutor.INSTANCE);
                                completerRef.set(completer);
                                return null;
                            }
                        });
        assertThat(future.cancel(true)).isTrue();
        assertThat(wasCalled.get()).isTrue();
        assertThat(completerRef.get().setCancelled()).isFalse();
    }

    @Test
    public void testCancellationListenersNotCalledOnCompleterCancelled() {
        final AtomicBoolean wasCalled = new AtomicBoolean();
        ListenableFuture<String> future =
                CallbackToFutureAdapter.getFuture(
                        new CallbackToFutureAdapter.Resolver<String>() {
                            @Override
                            public Object attachCompleter(Completer<String> completer) {
                                completer.addCancellationListener(new Runnable() {
                                    @Override
                                    public void run() {
                                        wasCalled.set(true);
                                    }
                                }, DirectExecutor.INSTANCE);
                                completer.setCancelled();
                                return null;
                            }
                        });
        assertThat(future.isCancelled()).isTrue();
        assertThat(wasCalled.get()).isFalse();
    }

    /** Verifies that there is no cycle in toString between the completer and the tag */
    @Test
    public void testNoRecursiveToString() {
        final AtomicReference<Runnable> callbackRef = new AtomicReference<>();
        ListenableFuture<String> future =
                CallbackToFutureAdapter.getFuture(
                        new CallbackToFutureAdapter.Resolver<String>() {
                            @Override
                            public Object attachCompleter(final Completer<String> completer) {
                                Runnable callback =
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                completer.set("my special result");
                                            }

                                            @Override
                                            public String toString() {
                                                return "custom callback, completer=[" + completer
                                                        + "]";
                                            }
                                        };
                                callbackRef.set(callback);
                                return callback;
                            }
                        });
        assertThat(future.isDone()).isFalse();
        assertThat(future.toString()).contains("custom callback");
        callbackRef.get().run();
        assertFutureCompletedWith(future, "my special result");
    }
}
