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

package androidx.media2.test.common;

import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestUtils {

    public static final int TIMEOUT_MS = 1000;
    public static final int PROVIDER_SERVICE_CONNECTION_TIMEOUT_MS = 3000;

    /**
     * Compares contents of two bundles.
     *
     * @param a a bundle
     * @param b another bundle
     * @return {@code true} if two bundles are the same. {@code false} otherwise. This may be
     *     incorrect if any bundle contains a bundle.
     */
    public static boolean equals(Bundle a, Bundle b) {
        return contains(a, b) && contains(b, a);
    }

    /**
     * Checks whether a Bundle contains another bundle.
     *
     * @param a a bundle
     * @param b another bundle
     * @return {@code true} if a contains b. {@code false} otherwise. This may be incorrect if any
     *      bundle contains a bundle.
     */
    @SuppressWarnings("deprecation")
    public static boolean contains(Bundle a, Bundle b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return b == null;
        }
        if (!a.keySet().containsAll(b.keySet())) {
            return false;
        }
        for (String key : b.keySet()) {
            if (!equals(a.get(key), b.get(key))) {
                return false;
            }
        }
        return true;
    }

    // Copied code from ObjectsCompat.java due to build dependency problem of
    // previous version of support lib.
    public static boolean equals(Object a, Object b) {
        if (Build.VERSION.SDK_INT >= 19) {
            return Objects.equals(a, b);
        } else {
            return (a == b) || (a != null && a.equals(b));
        }
    }

    /**
     * Create a bundle for testing purpose.
     *
     * @return the newly created bundle.
     */
    public static Bundle createTestBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("test_key", "test_value");
        return bundle;
    }

    /**
     * When testing with any fake lists, get the expected media ID of index {@param index}.
     */
    public static String getMediaIdInFakeList(int index) {
        // Set the media as the index with leading zeros.
        return String.format("%08d", index);
    }

    /**
     * Handler that always waits until the Runnable finishes.
     */
    public static class SyncHandler extends Handler {
        public SyncHandler(Looper looper) {
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

    private TestUtils() {
    }
}
