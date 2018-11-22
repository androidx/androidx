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
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.text.InputFilter;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.test.R;
import androidx.preference.tests.helpers.PreferenceTestHelperActivity;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for {@link androidx.preference.EditTextPreference.OnBindEditTextListener}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class EditTextPreferenceTest {

    @Rule
    public ActivityTestRule<PreferenceTestHelperActivity> mActivityRule =
            new ActivityTestRule<>(PreferenceTestHelperActivity.class);

    private static final String PREFERENCE = "preference";

    private EditTextPreference mEditTextPreference;

    @Before
    @UiThreadTest
    public void setUp() {
        PreferenceFragmentCompat fragment = mActivityRule.getActivity().setupPreferenceHierarchy(
                R.xml.test_edit_text_preference);
        PreferenceScreen screen = fragment.getPreferenceScreen();
        mEditTextPreference = (EditTextPreference) screen.findPreference(PREFERENCE);
    }

    @Test
    public void onBindEditTextListenerTest() throws Throwable {
        final InputFilter[] filters = { new InputFilter.LengthFilter(15) };
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertNull(mEditTextPreference.getOnBindEditTextListener());

                mEditTextPreference.setOnBindEditTextListener(
                        new EditTextPreference.OnBindEditTextListener() {
                            @Override
                            public void onBindEditText(@NonNull EditText editText) {
                                editText.setFilters(filters);
                            }
                        });
            }
        });
        onView(withText(PREFERENCE)).perform(click());
        onView(withId(android.R.id.edit)).check(matches(isDisplayed()));

        EditText editText =
                ((DialogFragment) mActivityRule.getActivity().getSupportFragmentManager()
                        .findFragmentByTag(
                                "androidx.preference.PreferenceFragment.DIALOG")).getDialog()
                        .findViewById(android.R.id.edit);
        assertEquals(filters, editText.getFilters());
    }
}
