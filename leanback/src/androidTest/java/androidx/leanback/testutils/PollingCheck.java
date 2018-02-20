/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.leanback.testutils;

import static org.junit.Assert.fail;

import android.app.Activity;
import android.view.View;

public abstract class PollingCheck {

    private static final long TIME_SLICE = 250;
    private long mTimeout = 5000;

    public abstract static class PollingCheckCondition {
        public abstract boolean canProceed();

        public boolean canPreProceed() {
            return canProceed();
        }
    }

    public PollingCheck() {
    }

    public PollingCheck(long timeout) {
        mTimeout = timeout;
    }

    protected abstract boolean check();

    protected boolean preCheck() {
        return check();
    }

    public void run() {
        if (preCheck()) {
            return;
        }

        long timeout = mTimeout;
        while (timeout > 0) {
            try {
                Thread.sleep(TIME_SLICE);
            } catch (InterruptedException e) {
                fail("unexpected InterruptedException");
            }

            if (check()) {
                return;
            }

            timeout -= TIME_SLICE;
        }

        fail("unexpected timeout");
    }

    public static void waitFor(final PollingCheckCondition condition) {
        new PollingCheck() {
            @Override
            protected boolean check() {
                return condition.canProceed();
            }

            @Override
            protected boolean preCheck() {
                return condition.canPreProceed();
            }
        }.run();
    }

    public static void waitFor(long timeout, final PollingCheckCondition condition) {
        new PollingCheck(timeout) {
            @Override
            protected boolean check() {
                return condition.canProceed();
            }
        }.run();
    }

    public static class ViewScreenPositionDetector {

        int[] lastLocation = null;
        int[] newLocation = new int[2];

        public boolean isViewStableOnScreen(View view) {
            if (lastLocation == null) {
                // get initial location
                lastLocation = new int[2];
                view.getLocationInWindow(lastLocation);
            } else {
                // get new location and compare to old location
                view.getLocationInWindow(newLocation);
                if (newLocation[0] == lastLocation[0]
                        && newLocation[1] == lastLocation[1]) {
                    // location stable,  animation finished
                    return true;
                }
                lastLocation[0] = newLocation[0];
                lastLocation[1] = newLocation[1];
            }
            return false;
        }
    }

    public static class ViewStableOnScreen extends PollingCheckCondition {

        View mView;
        ViewScreenPositionDetector mDector = new ViewScreenPositionDetector();

        public ViewStableOnScreen(View view) {
            mView = view;
        }

        @Override
        public boolean canPreProceed() {
            return false;
        }

        @Override
        public boolean canProceed() {
            return mDector.isViewStableOnScreen(mView);
        }

    }

    public static class ActivityDestroy extends PollingCheckCondition {

        Activity mActivity;

        public ActivityDestroy(Activity activity) {
            mActivity = activity;
        }

        @Override
        public boolean canProceed() {
            return mActivity.isDestroyed();
        }

    }

}