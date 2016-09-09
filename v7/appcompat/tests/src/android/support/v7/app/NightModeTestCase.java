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

package android.support.v7.app;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.v7.app.NightModeActivity.TOP_ACTIVITY;
import static android.support.v7.testutils.TestUtils.setLocalNightModeAndWaitForRecreate;
import static android.support.v7.testutils.TestUtilsMatchers.isBackground;

import static org.junit.Assert.assertFalse;

import android.support.test.filters.SdkSuppress;
import android.support.v7.appcompat.test.R;
import android.test.suitebuilder.annotation.MediumTest;

import org.junit.Before;
import org.junit.Test;

@MediumTest
@SdkSuppress(minSdkVersion = 14)
public class NightModeTestCase extends BaseInstrumentationTestCase<NightModeActivity> {

    private static final String STRING_DAY = "DAY";
    private static final String STRING_NIGHT = "NIGHT";

    public NightModeTestCase() {
        super(NightModeActivity.class);
    }

    @Before
    public void setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the test below
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    @Test
    public void testLocalDayNightModeRecreatesActivity() {
        // Verify first that we're in day mode
        onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_DAY)));

        // Now force the local night mode to be yes (aka night mode)
        setLocalNightModeAndWaitForRecreate(getActivity(), AppCompatDelegate.MODE_NIGHT_YES);

        // Now check the text has changed, signifying that night resources are being used
        onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_NIGHT)));
    }

    @Test
    public void testColorConvertedDrawableChangesWithNightMode() {
        final NightModeActivity activity = getActivity();
        final int dayColor = activity.getResources().getColor(R.color.color_sky_day);
        final int nightColor = activity.getResources().getColor(R.color.color_sky_night);

        // Loop through and switching from day to night and vice-versa multiple times. It needs
        // to be looped since the issue is with drawable caching, therefore we need to prime the
        // cache for the issue to happen
        for (int i = 0; i < 5; i++) {
            // First force it to not be night mode
            setLocalNightModeAndWaitForRecreate(TOP_ACTIVITY, AppCompatDelegate.MODE_NIGHT_NO);
            // ... and verify first that we're in day mode
            onView(withId(R.id.view_background)).check(matches(isBackground(dayColor)));

            // Now force the local night mode to be yes (aka night mode)
            setLocalNightModeAndWaitForRecreate(TOP_ACTIVITY, AppCompatDelegate.MODE_NIGHT_YES);
            // ... and verify first that we're in night mode
            onView(withId(R.id.view_background)).check(matches(isBackground(nightColor)));
        }
    }

    @Test
    public void testNightModeAutoRecreatesOnTimeChange() {
        // Create a fake TwilightManager and set it as the app instance
        final FakeTwilightManager twilightManager = new FakeTwilightManager();
        TwilightManager.setInstance(twilightManager);

        final NightModeActivity activity = getActivity();
        final AppCompatDelegateImplV14 delegate = (AppCompatDelegateImplV14) activity.getDelegate();

        // Verify that we're currently in day mode
        onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_DAY)));

        // Now set MODE_NIGHT_AUTO so that we will change to night mode automatically
        setLocalNightModeAndWaitForRecreate(activity, AppCompatDelegate.MODE_NIGHT_AUTO);

        // Assert that the original Activity has not been destroyed yet
        assertFalse(activity.isDestroyed());

        // Now update the fake twilight manager to be in night and trigger a fake 'time' change
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                twilightManager.setIsNight(true);
                delegate.getAutoNightModeManager().dispatchTimeChanged();
            }
        });

        // Now wait for the recreate
        getInstrumentation().waitForIdleSync();

        // Now check that the text has changed, signifying that night resources are being used
        onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_NIGHT)));
    }

    private static class FakeTwilightManager extends TwilightManager {
        private boolean mIsNight;

        FakeTwilightManager() {
            super(null, null);
        }

        @Override
        boolean isNight() {
            return mIsNight;
        }

        void setIsNight(boolean night) {
            mIsNight = night;
        }
    }
}
