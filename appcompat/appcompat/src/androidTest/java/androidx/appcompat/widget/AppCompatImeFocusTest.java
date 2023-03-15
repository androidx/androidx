/*
 * Copyright 2023 The Android Open Source Project
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


import static androidx.appcompat.widget.AppCompatImeFocusActivity.TEST_COMPAT_EDIT_TEXT;
import static androidx.appcompat.widget.AppCompatImeFocusActivity.TEST_COMPAT_TEXT_VIEW;

import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.testutils.PollingCheck;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AppCompatImeFocusTest {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    private static final int STRESS_TEST_COUNTS = 500;

    @Rule
    public final ActivityScenarioRule<AppCompatImeFocusActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(AppCompatImeFocusActivity.class);

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public void detachServedWithDifferentNextServed_appCompatEditText_stressTest() {
        for (int i = 0; i < STRESS_TEST_COUNTS; i++) {
            detachServed_withDifferentNextServed(TEST_COMPAT_EDIT_TEXT);
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public void detachServedWithDifferentNextServed_appCompatTextView_stressTest() {
        for (int i = 0; i < STRESS_TEST_COUNTS; i++) {
            detachServed_withDifferentNextServed(TEST_COMPAT_TEXT_VIEW);
        }
    }

    @SdkSuppress(minSdkVersion = 30)
    private void detachServed_withDifferentNextServed(int targetWidget) {
        final ActivityScenario<AppCompatImeFocusActivity> activityScenario =
                mActivityScenarioRule.getScenario();

        // Initialize activity components and add first editor into layout
        activityScenario.onActivity(activity -> {
            activity.initActivity(targetWidget);
            activity.addFirstEditorAndRequestFocus();
        });

        // The first editor should be active for input method.
        activityScenario.onActivity(
                activity -> PollingCheck.waitFor(TIMEOUT, activity::isFirstEditorActive));

        // Add second editor into layout and ensure it is laid out.
        activityScenario.onActivity(AppCompatImeFocusActivity::addSecondEditor);
        activityScenario.onActivity(
                activity -> PollingCheck.waitFor(TIMEOUT, activity::isSecondEditorLayout));

        // Second editor request focus and detach the served view(first editor).
        // Verify second editor should be active.
        activityScenario.onActivity(activity -> {
            activity.switchToSecondEditor();
            assertTrue(activity.isSecondEditorActive());
            activity.removeSecondEditor();
        });
    }
}
