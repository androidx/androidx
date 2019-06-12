/*
 * Copyright 2019 The Android Open Source Project
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

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
import static androidx.appcompat.testutils.NightModeUtils.assertConfigurationNightModeEquals;
import static androidx.appcompat.testutils.NightModeUtils.setNightModeAndWait;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.res.Configuration;

import androidx.appcompat.testutils.NightModeUtils.NightSetMode;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@LargeTest
@RunWith(Parameterized.class)
public class NightModeUiModeConfigChangesTestCase {
    @Parameterized.Parameters
    public static Collection<NightSetMode> data() {
        return Arrays.asList(NightSetMode.DEFAULT, NightSetMode.LOCAL);
    }

    private final NightSetMode mSetMode;

    @Rule
    public final ActivityTestRule<NightModeUiModeConfigChangesActivity> mActivityTestRule;

    public NightModeUiModeConfigChangesTestCase(NightSetMode setMode) {
        mSetMode = setMode;
        mActivityTestRule = new ActivityTestRule<>(
                NightModeUiModeConfigChangesActivity.class, false, false);
    }

    @Before
    public void setup() {
        // By default we'll set the night mode to NO, which allows us to make better
        // assumptions in the tests below
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO);
        // Now launch the test activity
        mActivityTestRule.launchActivity(null);
    }

    @Test
    public void testOnConfigurationChangeCalled() throws Throwable {
        // Set local night mode to YES
        setNightModeAndWait(mActivityTestRule, MODE_NIGHT_YES, mSetMode);

        // Assert that the onConfigurationChange was called with a new correct config
        Configuration lastConfig = mActivityTestRule.getActivity()
                .getLastConfigurationChangeAndClear();
        assertNotNull(lastConfig);
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES, lastConfig);

        // Set local night mode back to NO
        setNightModeAndWait(mActivityTestRule, MODE_NIGHT_NO, mSetMode);

        // Assert that the onConfigurationChange was again called with a new correct config
        lastConfig = mActivityTestRule.getActivity().getLastConfigurationChangeAndClear();
        assertNotNull(lastConfig);
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_NO, lastConfig);
    }

    @Test
    public void testResourcesUpdated() throws Throwable {
        // Set local night mode to YES
        setNightModeAndWait(mActivityTestRule, MODE_NIGHT_YES, mSetMode);

        // Assert that the Activity resources configuration was updated
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_YES,
                mActivityTestRule.getActivity().getResources().getConfiguration());

        // Set local night mode back to NO
        setNightModeAndWait(mActivityTestRule, MODE_NIGHT_NO, mSetMode);

        // Assert that the Activity resources configuration was updated
        assertConfigurationNightModeEquals(Configuration.UI_MODE_NIGHT_NO,
                mActivityTestRule.getActivity().getResources().getConfiguration());
    }

    @Test
    public void testOnNightModeChangedCalled() throws Throwable {
        // Set local night mode to YES
        setNightModeAndWait(mActivityTestRule, MODE_NIGHT_YES, mSetMode);
        // Assert that the Activity received a new value
        assertEquals(MODE_NIGHT_YES, mActivityTestRule.getActivity().getLastNightModeAndReset());

        // Set local night mode to NO
        setNightModeAndWait(mActivityTestRule, MODE_NIGHT_NO, mSetMode);
        // Assert that the Activity received a new value
        assertEquals(MODE_NIGHT_NO, mActivityTestRule.getActivity().getLastNightModeAndReset());
    }

    @After
    public void cleanup() throws Throwable {
        // Reset the default night mode
        setNightModeAndWait(mActivityTestRule, MODE_NIGHT_NO, NightSetMode.DEFAULT);
    }
}
