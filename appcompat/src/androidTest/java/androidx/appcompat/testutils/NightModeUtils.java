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

package androidx.appcompat.testutils;

import static org.junit.Assert.assertEquals;

import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatDelegate.NightMode;
import androidx.lifecycle.Lifecycle;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.testutils.LifecycleOwnerUtils;

public class NightModeUtils {

    public enum NightSetMode {
        /**
         * Set the night mode using {@link AppCompatDelegate#setDefaultNightMode(int)}
         */
        DEFAULT,

        /**
         * Set the night mode using {@link AppCompatDelegate#setLocalNightMode(int)}
         */
        LOCAL
    }

    public static void assertConfigurationNightModeEquals(int expectedNightMode,
            @NonNull Context context) {
        assertConfigurationNightModeEquals(expectedNightMode,
                context.getResources().getConfiguration());
    }

    public static void assertConfigurationNightModeEquals(
            int expectedNightMode, Configuration configuration) {
        assertEquals(expectedNightMode, configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK);
    }

    public static <T extends AppCompatActivity> void setNightModeAndWait(
            final ActivityTestRule<T> activityRule,
            @NightMode final int nightMode,
            final NightSetMode setMode
    ) throws Throwable {
        setNightModeAndWait(activityRule.getActivity(), activityRule, nightMode, setMode);
    }

    public static <T extends AppCompatActivity> void setNightModeAndWait(
            final AppCompatActivity activity,
            final ActivityTestRule<T> activityRule,
            @NightMode final int nightMode,
            final NightSetMode setMode
    ) throws Throwable {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setNightMode(nightMode, activity, setMode);
            }
        });
        instrumentation.waitForIdleSync();
    }

    public static <T extends AppCompatActivity> void setNightModeAndWaitForDestroy(
            final ActivityTestRule<T> activityRule,
            @NightMode final int nightMode,
            final NightSetMode setMode
    ) throws Throwable {
        final T activity = activityRule.getActivity();
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setNightMode(nightMode, activity, setMode);
            }
        });
        LifecycleOwnerUtils.waitUntilState(activity, activityRule, Lifecycle.State.DESTROYED);
    }

    private static void setNightMode(
            @NightMode final int nightMode,
            final AppCompatActivity activity,
            final NightSetMode setMode) {
        if (setMode == NightSetMode.DEFAULT) {
            AppCompatDelegate.setDefaultNightMode(nightMode);
        } else {
            activity.getDelegate().setLocalNightMode(nightMode);
        }
    }
}
