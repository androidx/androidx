/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static androidx.core.view.WindowInsetsCompat.Type.ime;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isFocused;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.fail;

import android.app.UiAutomation;
import android.os.Build;
import android.provider.Settings;

import androidx.core.view.ViewCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.tests.helpers.PreferenceTestHelperActivity;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
/**
 * Test for IME visibility when launching
 * {@link androidx.preference.EditTextPreferenceDialogFragmentCompat}.
 */
public class EditTextPreferenceDialogFragmentCompatTest {

    private EditTextPreferenceDialogFragmentCompat mFragment;
    private EditTextPreference mEditTextPreference;
    private PreferenceFragmentCompat mTargetPreference;
    private static final String PREFERENCE = "preference";

    private int mOriginalShowImeWithHardKeyboard;

    @Rule
    public ActivityScenarioRule<PreferenceTestHelperActivity> mActivityRule =
            new ActivityScenarioRule<>(PreferenceTestHelperActivity.class);

    @Before
    @UiThreadTest
    public void setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mOriginalShowImeWithHardKeyboard = Settings.Secure.getInt(
                    InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(),
                    "show_ime_with_hard_keyboard",
                    0);
            final UiAutomation uiAutomation =
                    InstrumentationRegistry.getInstrumentation().getUiAutomation();
            uiAutomation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS");
            try {
                uiAutomation.executeShellCommand(
                        "settings put secure show_ime_with_hard_keyboard 1"
                );
            } finally {
                uiAutomation.dropShellPermissionIdentity();
            }
        }

        mActivityRule.getScenario().onActivity(
                new ActivityScenario.ActivityAction<PreferenceTestHelperActivity>() {
                    @SuppressWarnings("deprecation")
                    @Override
                    public void perform(PreferenceTestHelperActivity activity) {
                        mTargetPreference = activity.setupPreferenceHierarchy(
                                androidx.preference.test.R.xml.test_edit_text_preference);
                        PreferenceScreen screen = mTargetPreference.getPreferenceScreen();
                        mEditTextPreference = screen.findPreference(PREFERENCE);
                        mEditTextPreference.setDialogLayoutResource(
                                androidx.preference.test.R.layout.preference_dialog_edittext);
                        mFragment = EditTextPreferenceDialogFragmentCompat.newInstance(
                                mEditTextPreference.getKey());
                        mFragment.setTargetFragment(mTargetPreference, 0);
                    }
                });
    }

    @After
    @UiThreadTest
    public void tearDown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final UiAutomation uiAutomation =
                    InstrumentationRegistry.getInstrumentation().getUiAutomation();
            uiAutomation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS");
            try {
                uiAutomation.executeShellCommand("settings put secure show_ime_with_hard_keyboard "
                        + mOriginalShowImeWithHardKeyboard
                );
            } finally {
                uiAutomation.dropShellPermissionIdentity();
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    public void testImeVisibilityAfterPressedPreference() throws Throwable {
        // Make sure EditTextPreferenceDialogFragment is showing a dialog, and clicking
        // EditText to make sure the editor is displayed and focused.
        onView(withText(PREFERENCE)).perform(click());
        onView(withId(android.R.id.edit))
                .check(matches(isDisplayed()))
                .check(matches(isFocused()));

        // Wait until the IME insets be visible.
        waitUntil(
                "IME insets should be visible",
                () -> ViewCompat.getRootWindowInsets(mTargetPreference.getView()).isVisible(ime()),
                5000 /* timeoutMs */
        );
    }

    private static void waitUntil(String message, BooleanSupplier condition, int timeoutMs) {
        final long timeout = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < timeout) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        fail("Timed out for: " + message);
    }

    /**
     * Clone of java.util.BooleanSupplier which isn't available on older devices
     */
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }
}
