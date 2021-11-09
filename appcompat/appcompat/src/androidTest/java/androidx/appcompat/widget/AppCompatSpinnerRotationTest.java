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
package androidx.appcompat.widget;

import static androidx.appcompat.testutils.TestUtilsActions.rotateScreenOrientation;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.app.Instrumentation;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.IdRes;
import androidx.appcompat.test.R;
import androidx.test.espresso.UiController;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.testutils.PollingCheck;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Rotation tests for {@link AppCompatSpinner}
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AppCompatSpinnerRotationTest {
    private Instrumentation mInstrumentation;

    @Rule
    public final ActivityTestRule<AppCompatSpinnerRotationActivity> mActivityTestRule;

    protected AppCompatSpinnerRotationActivity mActivity;
    protected UiController mUiController;

    public AppCompatSpinnerRotationTest() {
        mActivityTestRule = new ActivityTestRule<>(AppCompatSpinnerRotationActivity.class);
    }

    @Before
    public void setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mActivity = mActivityTestRule.getActivity();
    }

    @Test
    public void testChangeOrientationDialogPopupPersists() {
        verifyChangeOrientationPopupPersists(R.id.spinner_dialog_popup);
    }

    @Test
    public void testChangeOrientationDropdownPopupPersists() {
        verifyChangeOrientationPopupPersists(R.id.spinner_dropdown_popup);
    }

    private void verifyChangeOrientationPopupPersists(@IdRes int spinnerId) {
        // Does the device support both orientations?
        PackageManager pm = mActivity.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_SCREEN_PORTRAIT)
                || !pm.hasSystemFeature(PackageManager.FEATURE_SCREEN_LANDSCAPE)) {
            // Can't rotate - the screen might be locked to one orientation
            // or something like TV that doesn't support rotation at all.
            return;
        }

        onView(withId(spinnerId)).perform(click());
        // Wait until the popup is showing
        waitUntilPopupIsShown((AppCompatSpinner) mActivity.findViewById(spinnerId));

        // Use ActivityMonitor so that we can get the Activity instance after it has been
        // recreated when the rotation request completes
        Instrumentation.ActivityMonitor monitor =
                new Instrumentation.ActivityMonitor(mActivity.getClass().getName(), null, false);
        mInstrumentation.addMonitor(monitor);

        // Request screen rotation
        onView(isRoot()).perform(rotateScreenOrientation(mActivity));

        mActivity = (AppCompatSpinnerRotationActivity) mInstrumentation.waitForMonitorWithTimeout(
                monitor, 5000);
        if (mActivity == null) {
            // Device orientation is locked and screen can't be rotated
            Log.d("AppCompatSpinnerRotationTest", "Failed to recreate() activity after rotating "
                    + "the screen! Assuming screen orientation is locked and aborting test.");
            return;
        }
        mInstrumentation.waitForIdleSync();

        // Now we can get the new (post-rotation) instance of our spinner nd check that it's
        // showing the popup
        waitUntilPopupIsShown((AppCompatSpinner) mActivity.findViewById(spinnerId));
    }

    private void waitUntilPopupIsShown(final AppCompatSpinner spinner) {
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return spinner.getInternalPopup().isShowing();
            }
        });
    }
}
