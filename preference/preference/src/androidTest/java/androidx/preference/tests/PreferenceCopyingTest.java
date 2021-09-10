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

package androidx.preference.tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.test.R;
import androidx.preference.tests.helpers.PreferenceTestHelperActivity;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for {@link androidx.preference.Preference} copying logic.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PreferenceCopyingTest {

    @SuppressWarnings("deprecation")
    @Rule
    public androidx.test.rule.ActivityTestRule<PreferenceTestHelperActivity> mActivityRule =
            new androidx.test.rule.ActivityTestRule<>(PreferenceTestHelperActivity.class);

    private static final String COPY_BUTTON = "Copy";
    private static final String PREFERENCE_1 = "Preference 1";
    private static final String PREFERENCE_2 = "Preference 2";
    private static final String PREFERENCE_3 = "Preference 3";
    private static final String PREFERENCE_1_SUMMARY = "12345";
    private static final String PREFERENCE_2_SUMMARY = "6789";

    private ClipboardManager mClipboard;
    private PreferenceScreen mScreen;

    @Before
    @UiThreadTest
    public void setUp() {
        PreferenceFragmentCompat fragment = mActivityRule.getActivity().setupPreferenceHierarchy(
                R.xml.test_copying);
        mScreen = fragment.getPreferenceScreen();
        mClipboard = (ClipboardManager) mActivityRule.getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", "");
        mClipboard.setPrimaryClip(clip);
    }

    @Test
    public void enablingCopying_withXml_copiesToClipboard() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertTrue(mScreen.getPreference(0).isCopyingEnabled());
                assertFalse(mScreen.getPreference(1).isCopyingEnabled());
                assertTrue(mScreen.getPreference(2).isCopyingEnabled());
            }
        });

        getInstrumentation().waitForIdleSync();

        onView(withText(PREFERENCE_1)).perform(longClick());
        onView(withText(COPY_BUTTON)).check(matches(isDisplayed())).perform(click());


        assertEquals(PREFERENCE_1_SUMMARY, mClipboard.getPrimaryClip().getItemAt(0).getText());
    }

    @Test
    public void disablingCopying_dynamically_doesNotShowContextMenu() throws Throwable {
        onView(withText(PREFERENCE_1)).perform(longClick());
        onView(withText(COPY_BUTTON)).check(matches(isDisplayed()));
        pressBack();

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mScreen.getPreference(0).setCopyingEnabled(false);
            }
        });

        getInstrumentation().waitForIdleSync();

        onView(withText(PREFERENCE_1)).perform(longClick());
        onView(withText(COPY_BUTTON)).check(doesNotExist());
    }

    @Test
    public void enablingCopying_dynamically_copiesToClipboard() throws Throwable {
        onView(withText(PREFERENCE_2)).perform(longClick());
        onView(withText(COPY_BUTTON)).check(doesNotExist());

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mScreen.getPreference(1).setCopyingEnabled(true);
            }
        });

        getInstrumentation().waitForIdleSync();

        onView(withText(PREFERENCE_2)).perform(longClick());
        onView(withText(COPY_BUTTON)).check(matches(isDisplayed())).perform(click());

        assertEquals(PREFERENCE_2_SUMMARY, mClipboard.getPrimaryClip().getItemAt(0).getText());

    }

    @Test
    public void copyingWithoutSummary_doesNotShowContextMenu() {
        onView(withText(PREFERENCE_3)).perform(longClick());
        onView(withText(COPY_BUTTON)).check(doesNotExist());
    }
}
