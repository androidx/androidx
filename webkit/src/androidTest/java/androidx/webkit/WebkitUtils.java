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

package androidx.webkit;

import android.os.Handler;
import android.os.Looper;

import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Assume;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Helper methods for common webkit test tasks.
 *
 * <p>
 * This should remain functionally equivalent to android.webkit.cts.WebkitUtils.
 * Modifications to this class should be reflected in that class as necessary. See
 * http://go/modifying-webview-cts.
 */
public final class WebkitUtils {

    /**
     * Arbitrary timeout for tests. This is intended to be used with {@link TimeUnit#MILLISECONDS}
     * so that this can represent 20 seconds.
     *
     * <p class=note><b>Note:</b> only use this timeout value for the unexpected case, not for the
     * correct case, as this exceeds the time recommendation for {@link
     * androidx.test.filters.MediumTest}.
     */
    public static final long TEST_TIMEOUT_MS = 20000L; // 20s.

    // A handler for the main thread.
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    /**
     * Executes a callable asynchronously on the main thread, returning a future for the result.
     *
     * @param callable the {@link Callable} to execute.
     * @return a {@link ListenableFuture} representing the result of {@code callable}.
     */
    public static <T> ListenableFuture<T> onMainThread(final Callable<T> callable)  {
        final ResolvableFuture<T> future = ResolvableFuture.create();
        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(callable.call());
                } catch (Throwable t) {
                    future.setException(t);
                }
            }
        });
        return future;
    }

    /**
     * Executes a runnable asynchronously on the main thread.
     *
     * @param runnable the {@link Runnable} to execute.
     */
    public static void onMainThread(final Runnable runnable)  {
        sMainHandler.post(runnable);
    }

    /**
     * Executes a callable synchronously on the main thread, returning its result. This rethrows any
     * exceptions on the thread this is called from. This means callers may use {@link
     * org.junit.Assert} methods within the {@link Callable} if they invoke this method from the
     * instrumentation thread.
     *
     * <p class="note"><b>Note:</b> this should not be called from the UI thread.
     *
     * @param callable the {@link Callable} to execute.
     * @return the result of the {@link Callable}.
     */
    public static <T> T onMainThreadSync(final Callable<T> callable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("This cannot be called from the UI thread.");
        }
        return waitForFuture(onMainThread(callable));
    }

    /**
     * Executes a {@link Runnable} synchronously on the main thread. This is similar to {@link
     * android.app.Instrumentation#runOnMainSync(Runnable)}, with the main difference that this
     * re-throws exceptions on the calling thread. This is useful if {@code runnable} contains any
     * {@link org.junit.Assert} methods, or otherwise throws an Exception.
     *
     * <p class="note"><b>Note:</b> this should not be called from the UI thread.
     *
     * @param Runnable the {@link Runnable} to execute.
     */
    public static void onMainThreadSync(final Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("This cannot be called from the UI thread.");
        }
        final ResolvableFuture<Void> exceptionPropagatingFuture = ResolvableFuture.create();
        onMainThread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                    exceptionPropagatingFuture.set(null);
                } catch (Throwable t) {
                    exceptionPropagatingFuture.setException(t);
                }
            }
        });
        waitForFuture(exceptionPropagatingFuture);
    }

    /**
     * Throws {@link org.junit.AssumptionViolatedException} if the device does not support the
     * particular feature, otherwise returns.
     *
     * <p>
     * This provides a more descriptive error message than a bare {@code assumeTrue} call.
     *
     * <p>
     * Note that this method is AndroidX-specific, and is not reflected in the CTS class.
     *
     * @param featureName the feature to be checked
     */
    public static void checkFeature(String featureName) {
        final String msg = "This device does not have the feature '" +  featureName + "'";
        final boolean hasFeature = WebViewFeature.isFeatureSupported(featureName);
        Assume.assumeTrue(msg, hasFeature);
    }

    /**
     * Waits for {@code future} and returns its value (or times out).
     */
    public static <T> T waitForFuture(Future<T> future) {
        try {
            return future.get(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            // Try to throw the cause itself, to avoid unnecessarily wrapping an unchecked
            // throwable.
            Throwable cause = e.getCause();
            if (cause instanceof Error) throw (Error) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new RuntimeException(cause);
        } catch (InterruptedException | TimeoutException e) {
            // Don't handle InterruptedException specially, since it indicates that a different
            // Thread was interrupted, not this one.
            throw new RuntimeException(e.getCause());
        }
    }

    /**
     * Takes an element out of the {@link BlockingQueue} (or times out).
     */
    public static <T> T waitForNextQueueElement(BlockingQueue<T> queue) {
        try {
            T value = queue.poll(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (value == null) {
                // {@code null} is the special value which means {@link BlockingQueue#poll} has
                // timed out (also: there's no risk for collision with real values, because
                // BlockingQueue does not allow null entries). Instead of returning this special
                // value, let's throw a proper TimeoutException to stay consistent with {@link
                // #waitForFuture}.
                throw new TimeoutException(
                        "Timeout while trying to take next entry from BlockingQueue");
            }
            return value;
        } catch (TimeoutException | InterruptedException e) {
            // Don't handle InterruptedException specially, since it indicates that a different
            // Thread was interrupted, not this one.
            throw new RuntimeException(e.getCause());
        }
    }

    // Do not instantiate this class.
    private WebkitUtils() {}
}
