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

package android.support.design.widget;

import static android.support.design.testutils.AppBarLayoutMatchers.isCollapsed;
import static android.support.design.testutils.SwipeUtils.swipeUp;
import static android.support.design.testutils.TestUtilsMatchers.hasZ;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.design.test.R;
import android.support.test.filters.LargeTest;
import android.support.testutils.PollingCheck;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@LargeTest
public class AppBarWithCollapsingToolbarStateRestoreTest
        extends BaseInstrumentationTestCase<AppBarLayoutCollapsePinTestActivity> {

    private Activity mActivity;
    private int mOldOrientation;

    public AppBarWithCollapsingToolbarStateRestoreTest() {
        super(AppBarLayoutCollapsePinTestActivity.class);
    }

    @Before
    public void setup() {
        mActivity = mActivityTestRule.getActivity();
        mOldOrientation = mActivity.getResources().getConfiguration().orientation;
    }

    @After
    public void tearDown() {
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mActivity.getResources().getConfiguration().orientation == mOldOrientation;
            }
        });
    }

    @Test
    public void testRotateAndRestore() {
        final AppBarLayout appBar = (AppBarLayout) mActivity.findViewById(R.id.app_bar);

        // Swipe up and collapse the AppBarLayout
        onView(withId(R.id.coordinator_layout))
                .perform(swipeUp(
                        appBar.getLeft() + (appBar.getWidth() / 2),
                        appBar.getBottom() + 20,
                        appBar.getHeight()));
        onView(withId(R.id.app_bar))
                .check(matches(hasZ()))
                .check(matches(isCollapsed()));

        // Now rotate the Activity
        final int newOrientation = mOldOrientation == Configuration.ORIENTATION_PORTRAIT
                ? Configuration.ORIENTATION_LANDSCAPE
                : Configuration.ORIENTATION_PORTRAIT;
        mActivity.setRequestedOrientation(toActivityInfoConfiguration(newOrientation));
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mActivity.getResources().getConfiguration().orientation == newOrientation;
            }
        });

        // And check that the app bar still is restored correctly
        onView(withId(R.id.app_bar))
                .check(matches(hasZ()))
                .check(matches(isCollapsed()));
    }

    private static int toActivityInfoConfiguration(int configuration) {
        return configuration == Configuration.ORIENTATION_PORTRAIT
                ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }
}
