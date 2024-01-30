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

package androidx.wear.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.widget.Button;
import org.junit.Ignore;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.wear.test.R;
import androidx.wear.widget.util.WakeLockRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ConfirmationActivityTest {

    // Number of milliseconds before the end of the duration period that we should check that the
    // ConfirmationActivity still has focus
    private static final int MILLIS_BEFORE_END_OF_DURATION = 450;

    // Number of milliseconds after the end of the duration period that we should wait before the
    // ConfirmationActivity still lost focus
    private static final int MILLIS_AFTER_END_OF_DURATION = 500;

    // Number of milliseconds to wait for the confirmation activity to display initially
    private static final int MILLIS_TO_WAIT_FOR_ACTIVITY_TO_BE_DRAWN = 500;

    @Rule
    public final WakeLockRule wakeLock = new WakeLockRule();

    @Rule
    public final ActivityTestRule<ConfirmationActivityTestActivity> mActivityRule =
            new ActivityTestRule<>(ConfirmationActivityTestActivity.class, true, true);

    @Test
    @Ignore("b/272346886")
    public void testConfirmationDialogShownForDefaultDuration() throws Throwable {
        int testDuration = ConfirmationActivity.DEFAULT_ANIMATION_DURATION_MILLIS;
        // Check that the structure of the test is still valid
        assertTrue(testDuration
                > (MILLIS_BEFORE_END_OF_DURATION + MILLIS_TO_WAIT_FOR_ACTIVITY_TO_BE_DRAWN));
        testConfirmationDialogShownForConfiguredDuration(
                ConfirmationActivity.DEFAULT_ANIMATION_DURATION_MILLIS, "A message");
    }

    @Test
    @Ignore("b/272346886")
    public void testConfirmationDialogShownForLongerDuration() throws Throwable {
        testConfirmationDialogShownForConfiguredDuration(
                ConfirmationActivity.DEFAULT_ANIMATION_DURATION_MILLIS * 2, "A message");
    }

    @Test
    @Ignore("b/272346886")
    public void testConfirmationDialogWithMissingMessage() throws Throwable {
        testConfirmationDialogShownForConfiguredDuration(
                ConfirmationActivity.DEFAULT_ANIMATION_DURATION_MILLIS * 2, /* message= */null);
    }

    private void testConfirmationDialogShownForConfiguredDuration(int duration,
            @Nullable String message) throws Throwable {
        // Wait for the test activity to be visible
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(MILLIS_TO_WAIT_FOR_ACTIVITY_TO_BE_DRAWN);

        // Check that the test activity currently hasWindowFocus()
        assertTrue(mActivityRule.getActivity().hasWindowFocus());
        Button button =
                mActivityRule.getActivity().findViewById(R.id.show_confirmation_activity_button);

        // GIVEN a display duration in milliseconds, and message
        mActivityRule.getActivity().setDuration(duration);
        mActivityRule.getActivity().setMessage(message);
        // WHEN we click on the button
        mActivityRule.runOnUiThread(button::performClick);
        // THEN wait for the activity to be drawn
        Thread.sleep(MILLIS_TO_WAIT_FOR_ACTIVITY_TO_BE_DRAWN);
        // AND lose window focus to the confirmation activity on top
        assertFalse(mActivityRule.getActivity().hasWindowFocus());
        // AND wait until a short while before the confirmation activity is due to expire
        Thread.sleep(duration - MILLIS_BEFORE_END_OF_DURATION
                - MILLIS_TO_WAIT_FOR_ACTIVITY_TO_BE_DRAWN);
        // AND confirm that the confirmation activity still has focus
        assertFalse(mActivityRule.getActivity().hasWindowFocus());
        // AND wait for until the confirmation activity should be gone
        Thread.sleep(MILLIS_AFTER_END_OF_DURATION + MILLIS_BEFORE_END_OF_DURATION
                + MILLIS_TO_WAIT_FOR_ACTIVITY_TO_BE_DRAWN);
        // AND confirm that the test activity has focus again
        assertTrue(mActivityRule.getActivity().hasWindowFocus());
    }

}
