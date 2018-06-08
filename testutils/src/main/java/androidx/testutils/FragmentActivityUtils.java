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
import android.os.Looper;
import android.support.test.rule.ActivityTestRule;

import androidx.fragment.app.FragmentActivity;

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

    private static void waitForExecution(final ActivityTestRule<? extends FragmentActivity> rule) {
        // Wait for two cycles. When starting a postponed transition, it will post to
        // the UI thread and then the execution will be added onto the queue after that.
        // The two-cycle wait makes sure fragments have the opportunity to complete both
        // before returning.
        try {
            rule.runOnUiThread(DO_NOTHING);
            rule.runOnUiThread(DO_NOTHING);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
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
        RecreatedActivity.sResumed = new CountDownLatch(1);
        RecreatedActivity.sDestroyed = new CountDownLatch(1);

        runOnUiThreadRethrow(rule, new Runnable() {
            @Override
            public void run() {
                activity.recreate();
            }
        });
        assertTrue(RecreatedActivity.sResumed.await(1, TimeUnit.SECONDS));
        assertTrue(RecreatedActivity.sDestroyed.await(1, TimeUnit.SECONDS));
        T newActivity = (T) RecreatedActivity.sActivity;

        waitForExecution(rule);

        RecreatedActivity.clearState();
        return newActivity;
    }

    private FragmentActivityUtils() {
    }
}
