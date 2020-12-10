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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.text.Html;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for String formatting behaviour in {@link ListPreference#setSummary(CharSequence)}.
 *
 * <p> ListPreference supports setting a summary with String formatting markers - in this case we
 * ignore any custom styled spans set on the summary. If a formatting marker is not present, we
 * should respect any styled spans.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ListPreferenceSummaryTest {

    private Context mContext;
    private PreferenceScreen mScreen;

    @Before
    @UiThreadTest
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
    }

    @Test
    public void plainSummary() {
        ListPreference preference = new ListPreference(mContext);
        CharSequence summary = "Test summary";
        preference.setSummary(summary);
        mScreen.addPreference(preference);

        assertEquals(summary, preference.getSummary());
        // Summary should be a String with no styled spans
        assertTrue(preference.getSummary() instanceof String);
    }

    @SuppressWarnings("deprecation")
    @Test
    @UiThreadTest
    public void styledSummary() {
        ListPreference preference = new ListPreference(mContext);
        CharSequence summary = Html.fromHtml("<b>Test summary</b>");
        preference.setSummary(summary);
        mScreen.addPreference(preference);

        assertEquals(summary, preference.getSummary());
        // Summary should not be a String as it contains styled spans
        assertFalse(preference.getSummary() instanceof String);
    }

    @Test
    @UiThreadTest
    public void formattedSummary() {
        ListPreference preference = new ListPreference(mContext);
        CharSequence summary = "Test summary: %s";
        preference.setSummary(summary);
        mScreen.addPreference(preference);

        // No value has been set for this preference, so it should substitute '%s' with ''
        assertEquals("Test summary: ", preference.getSummary());
        // Summary should be a String with no styled spans
        assertTrue(preference.getSummary() instanceof String);
    }
}
