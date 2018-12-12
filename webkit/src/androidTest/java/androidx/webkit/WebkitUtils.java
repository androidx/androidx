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

import org.junit.Assume;

import java.util.concurrent.BlockingQueue;
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
    public static <T> T waitForFuture(Future<T> future) throws InterruptedException,
             ExecutionException,
             TimeoutException {
        // TODO(ntfschr): consider catching ExecutionException and throwing e.getCause().
        return future.get(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Takes an element out of the {@link BlockingQueue} (or times out).
     */
    public static <T> T waitForNextQueueElement(BlockingQueue<T> queue) throws InterruptedException,
             TimeoutException {
        T value = queue.poll(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (value == null) {
            // {@code null} is the special value which means {@link BlockingQueue#poll} has timed
            // out (also: there's no risk for collision with real values, because BlockingQueue does
            // not allow null entries). Instead of returning this special value, let's throw a
            // proper TimeoutException to stay consistent with {@link #waitForFuture}.
            throw new TimeoutException(
                    "Timeout while trying to take next entry from BlockingQueue");
        }
        return value;
    }

    // Do not instantiate this class.
    private WebkitUtils() {}
}
