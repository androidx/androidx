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

package androidx.appcompat.app;

import static androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals;
import static androidx.appcompat.testutils.NightModeUtils.setLocalNightModeAndWait;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.res.Configuration;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class NightModeConfigChangesTestCase {
    @Rule
    public final ActivityTestRule<NightModeConfigChangesActivity> mActivityTestRule;

    public NightModeConfigChangesTestCase() {
        mActivityTestRule = new ActivityTestRule<>(NightModeConfigChangesActivity.class);
    }

    @Before
    public void setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the test below
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    @Test
    public void testOnConfigurationChangedCalled() throws Throwable {
        final NightModeConfigChangesActivity activity = mActivityTestRule.getActivity();

        // Assert that the Activity does not have a config currently
        assertNull(activity.lastChangeConfiguration);

        // Set local night mode to YES
        setLocalNightModeAndWait(mActivityTestRule, AppCompatDelegate.MODE_NIGHT_YES);

        // Assert that the Activity received a new config
        assertNotNull(activity.lastChangeConfiguration);
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES,
                activity.lastChangeConfiguration);

        // Set local night mode back to NO
        setLocalNightModeAndWait(mActivityTestRule, AppCompatDelegate.MODE_NIGHT_NO);

        // Assert that the Activity received a new config
        assertNotNull(activity.lastChangeConfiguration);
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_NO,
                activity.lastChangeConfiguration);
    }
}
