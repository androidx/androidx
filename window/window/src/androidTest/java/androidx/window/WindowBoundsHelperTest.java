/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link WindowBoundsHelper} class. */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class WindowBoundsHelperTest {
    @Rule
    public ActivityScenarioRule<TestActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(TestActivity.class);

    @Test
    public void testGetCurrentWindowBounds_matchParentWindowSize_avoidCutouts() {
        testGetCurrentWindowBoundsMatchesRealDisplaySize(activity -> {
            assumeFalse(isInMultiWindowMode(activity));

            WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            }
            activity.getWindow().setAttributes(lp);
        });
    }

    @Test
    public void testGetCurrentWindowBounds_fixedWindowSize_avoidCutouts() {
        testGetCurrentWindowBoundsMatchesRealDisplaySize(activity -> {
            assumeFalse(isInMultiWindowMode(activity));

            WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
            lp.width = 100;
            lp.height = 100;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            }
            activity.getWindow().setAttributes(lp);
        });
    }

    @Test
    public void testGetCurrentWindowBounds_matchParentWindowSize_layoutBehindCutouts() {
        testGetCurrentWindowBoundsMatchesRealDisplaySize(activity -> {
            assumeFalse(isInMultiWindowMode(activity));

            WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            activity.getWindow().setAttributes(lp);
        });
    }

    @Test
    public void testGetCurrentWindowBounds_fixedWindowSize_layoutBehindCutouts() {
        testGetCurrentWindowBoundsMatchesRealDisplaySize(activity -> {
            assumeFalse(isInMultiWindowMode(activity));

            WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
            lp.width = 100;
            lp.height = 100;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            activity.getWindow().setAttributes(lp);
        });
    }

    private void testGetCurrentWindowBoundsMatchesRealDisplaySize(
            ActivityScenario.ActivityAction<TestActivity> initialAction) {
        ActivityScenario<TestActivity> scenario = mActivityScenarioRule.getScenario();
        scenario.onActivity(initialAction);

        ActivityScenario.ActivityAction<TestActivity> assertWindowBoundsAction =
                new AssertWindowBoundsEqualsRealDisplaySizeAction();

        scenario.moveToState(Lifecycle.State.CREATED);
        scenario.onActivity(assertWindowBoundsAction);

        scenario.moveToState(Lifecycle.State.STARTED);
        scenario.onActivity(assertWindowBoundsAction);

        scenario.moveToState(Lifecycle.State.RESUMED);
        scenario.onActivity(assertWindowBoundsAction);
    }

    private static boolean isInMultiWindowMode(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return activity.isInMultiWindowMode();
        }
        return false;
    }

    private static final class AssertWindowBoundsEqualsRealDisplaySizeAction implements
            ActivityScenario.ActivityAction<TestActivity> {
        @Override
        public void perform(TestActivity activity) {
            Display display;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display = activity.getDisplay();
            } else {
                display = activity.getWindowManager().getDefaultDisplay();
            }

            Point realDisplaySize = WindowBoundsHelper.getRealSizeForDisplay(display);

            Rect bounds = WindowBoundsHelper.getInstance().computeCurrentWindowBounds(activity);
            assertEquals("Window bounds width does not match real display width",
                    realDisplaySize.x, bounds.width());
            assertEquals("Window bounds height does not match real display height",
                    realDisplaySize.y, bounds.height());
        }
    }
}
