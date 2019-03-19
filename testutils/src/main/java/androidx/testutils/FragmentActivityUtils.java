/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.testutils;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.os.Build;
import android.os.Looper;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.test.rule.ActivityTestRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for testing fragment activities.
 */
public class FragmentActivityUtils {
    private static final Runnable DO_NOTHING = new Runnable() {
        @Override
        public void run() {
        }
    };

    /**
     * Wait until the specified number of cycles have passed.
     *
     * @param cycles The number of cycles to wait
     * @param rule The test's ActivityTestRule
     */
    public static void waitForCycles(final int cycles,
            final ActivityTestRule<? extends FragmentActivity> rule) {
        try {
            for (int i = 0; i < cycles; i++) {
                rule.runOnUiThread(DO_NOTHING);
            }
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    /**
     * Waits for the execution of the provided activity test rule.
     *
     * @param rule Activity test rule to wait for
     */
    public static void waitForExecution(final ActivityTestRule<? extends FragmentActivity> rule) {
        // Wait for two cycles. When starting a postponed transition, it will post to
        // the UI thread and then the execution will be added onto the queue after that.
        // The two-cycle wait makes sure fragments have the opportunity to complete both
        // before returning.
        waitForCycles(2, rule);
    }

    private static void runOnUiThreadRethrow(ActivityTestRule<? extends Activity> rule,
            Runnable r) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            r.run();
        } else {
            try {
                rule.runOnUiThread(r);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    /**
     * Restarts the RecreatedActivity and waits for the new activity to be resumed.
     *
     * @return The newly-restarted Activity
     */
    public static <T extends RecreatedActivity> T recreateActivity(
            ActivityTestRule<? extends RecreatedActivity> rule, final T activity)
            throws InterruptedException {
        // Now switch the orientation
        RecreatedActivity.setResumedLatch(new CountDownLatch(1));
        RecreatedActivity.setDestroyedLatch(new CountDownLatch(1));

        runOnUiThreadRethrow(rule, new Runnable() {
            @Override
            public void run() {
                activity.recreate();
            }
        });
        assertTrue(RecreatedActivity.getResumedLatch().await(1, TimeUnit.SECONDS));
        assertTrue(RecreatedActivity.getDestroyedLatch().await(1, TimeUnit.SECONDS));
        T newActivity = (T) RecreatedActivity.getActivity();

        waitForExecution(rule);

        RecreatedActivity.clearState();
        return newActivity;
    }

    /**
     * Waits until the activity is (re)drawn.
     *
     * @param activity An Activity
     */
    public static <T extends FragmentActivity> void waitForActivityDrawn(final T activity) {
        final CountDownLatch latch = new CountDownLatch(1);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View view = activity.getWindow().getDecorView();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    view.getViewTreeObserver().addOnDrawListener(
                            new CountOnDraw(latch, view));
                } else {
                    view.getViewTreeObserver().addOnPreDrawListener(
                            new CountOnPreDraw(latch, view));
                }
                view.invalidate();
            }
        });

        try {
            assertTrue("Draw pass did not occur within 5 seconds",
                    latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private FragmentActivityUtils() {
    }
}
