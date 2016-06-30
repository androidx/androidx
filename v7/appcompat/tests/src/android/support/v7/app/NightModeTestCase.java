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

import static org.junit.Assert.assertFalse;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
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
        final NightModeActivity activity = getActivity();

        // Verify first that we're in day mode
        onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_DAY)));

        // Now force the local night mode to be yes (aka night mode)
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });
        instrumentation.waitForIdleSync();

        // Now check the text has changed, signifying that night resources are being used
        onView(withId(R.id.text_night_mode)).check(matches(withText(STRING_NIGHT)));
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
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // Force the local night mode to be auto
                delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
            }
        });
        instrumentation.waitForIdleSync();

        // Assert that the original Activity has not been destroyed yet
        assertFalse(activity.isDestroyed());

        // Now update the fake twilight manager to be in night and trigger a fake 'time' change
        instrumentation.runOnMainSync(new Runnable() {
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
