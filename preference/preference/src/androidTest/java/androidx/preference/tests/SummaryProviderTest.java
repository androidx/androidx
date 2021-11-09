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
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
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
 * Test for {@link androidx.preference.Preference.SummaryProvider} implementations in
 * {@link ListPreference}, {@link DropDownPreference}, and {@link EditTextPreference}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class SummaryProviderTest {

    private static final String KEY = "TestPrefKey";

    private Context mContext;
    private PreferenceScreen mScreen;
    private SharedPreferences mSharedPreferences;

    @Before
    @UiThreadTest
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mSharedPreferences = preferenceManager.getSharedPreferences();

        // Make sure that the key does not exist in SharedPreferences to ensure a clean state.
        mSharedPreferences.edit().remove(KEY).apply();
        assertNull(mSharedPreferences.getString(KEY, null));
    }

    @Test
    @UiThreadTest
    public void listPreference_noValueSet_summaryDisplaysNotSet() {
        ListPreference listPreference = createListPreference();
        listPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        mScreen.addPreference(listPreference);

        assertEquals("Not set", listPreference.getSummary());
    }

    @Test
    @UiThreadTest
    public void listPreference_valueSet_summaryCorrectlySet() {
        ListPreference listPreference = createListPreference();
        listPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        // The user visible entry corresponding to this value is 'New value' - the summary should
        // display this entry, not the internal value.
        listPreference.setValue("key");

        mScreen.addPreference(listPreference);

        assertEquals("New value", listPreference.getSummary());
    }


    @Test
    @UiThreadTest
    public void listPreference_valueChanged_summaryIsUpdated() {
        ListPreference listPreference = createListPreference();
        listPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        mScreen.addPreference(listPreference);

        assertEquals("Not set", listPreference.getSummary());

        // The user visible entry corresponding to this value is 'New value' - the summary should
        // display this entry, not the internal value.
        listPreference.setValue("key");

        assertEquals("New value", listPreference.getSummary());
    }

    @Test
    @UiThreadTest
    public void listPreference_valueRetrievedFromSharedPreferences_summaryIsUpdated() {
        mSharedPreferences.edit().putString(KEY, "key").apply();
        ListPreference listPreference = createListPreference();
        listPreference.setPersistent(true);
        listPreference.setKey(KEY);

        listPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        mScreen.addPreference(listPreference);

        assertEquals("New value", listPreference.getEntry());
        assertEquals("New value", listPreference.getSummary());
    }

    @Test
    @UiThreadTest
    public void dropDownPreference_noValueSet_summaryDisplaysNotSet() {
        DropDownPreference dropDownPreference = createDropDownPreference();
        dropDownPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        mScreen.addPreference(dropDownPreference);

        assertEquals("Not set", dropDownPreference.getSummary());
    }

    @Test
    @UiThreadTest
    public void dropDownPreference_valueSet_summaryCorrectlySet() {
        DropDownPreference dropDownPreference = createDropDownPreference();
        dropDownPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        // The user visible entry corresponding to this value is 'New value' - the summary should
        // display this entry, not the internal value.
        dropDownPreference.setValue("key");

        mScreen.addPreference(dropDownPreference);

        assertEquals("New value", dropDownPreference.getSummary());
    }


    @Test
    @UiThreadTest
    public void dropDownPreference_valueChanged_summaryIsUpdated() {
        DropDownPreference dropDownPreference = createDropDownPreference();
        dropDownPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        mScreen.addPreference(dropDownPreference);

        assertEquals("Not set", dropDownPreference.getSummary());

        // The user visible entry corresponding to this value is 'New value' - the summary should
        // display this entry, not the internal value.
        dropDownPreference.setValue("key");

        assertEquals("New value", dropDownPreference.getSummary());
    }

    @Test
    @UiThreadTest
    public void dropDownPreference_valueRetrievedFromSharedPreferences_summaryIsUpdated() {
        mSharedPreferences.edit().putString(KEY, "key").apply();
        DropDownPreference dropDownPreference = createDropDownPreference();
        dropDownPreference.setPersistent(true);
        dropDownPreference.setKey(KEY);

        dropDownPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        mScreen.addPreference(dropDownPreference);

        assertEquals("New value", dropDownPreference.getEntry());
        assertEquals("New value", dropDownPreference.getSummary());
    }

    @Test
    @UiThreadTest
    public void editTextPreference_noValueSet_summaryDisplaysNotSet() {
        EditTextPreference editTextPreference = createEditTextPreference();
        editTextPreference.setSummaryProvider(
                EditTextPreference.SimpleSummaryProvider.getInstance());
        mScreen.addPreference(editTextPreference);

        assertEquals("Not set", editTextPreference.getSummary());
    }

    @Test
    @UiThreadTest
    public void editTextPreference_valueSet_summaryCorrectlySet() {
        EditTextPreference editTextPreference = createEditTextPreference();
        editTextPreference.setSummaryProvider(
                EditTextPreference.SimpleSummaryProvider.getInstance());
        editTextPreference.setText("New value");

        mScreen.addPreference(editTextPreference);

        assertEquals("New value", editTextPreference.getSummary());
    }


    @Test
    @UiThreadTest
    public void editTextPreference_valueChanged_summaryIsUpdated() {
        EditTextPreference editTextPreference = createEditTextPreference();
        editTextPreference.setSummaryProvider(
                EditTextPreference.SimpleSummaryProvider.getInstance());
        mScreen.addPreference(editTextPreference);

        assertEquals("Not set", editTextPreference.getSummary());

        editTextPreference.setText("New value");

        assertEquals("New value", editTextPreference.getSummary());
    }

    @Test
    @UiThreadTest
    public void editTextPreference_valueRetrievedFromSharedPreferences_summaryIsUpdated() {
        mSharedPreferences.edit().putString(KEY, "New value").apply();
        EditTextPreference editTextPreference = createEditTextPreference();
        editTextPreference.setPersistent(true);
        editTextPreference.setKey(KEY);

        editTextPreference.setSummaryProvider(
                EditTextPreference.SimpleSummaryProvider.getInstance());
        mScreen.addPreference(editTextPreference);

        assertEquals("New value", editTextPreference.getText());
        assertEquals("New value", editTextPreference.getSummary());
    }

    private ListPreference createListPreference() {
        ListPreference listPreference = new ListPreference(mContext);
        listPreference.setPersistent(false);
        // User visible options in the list
        listPreference.setEntries(new String[]{"New value"});
        // Value persisted internally that corresponds to the user visible option
        listPreference.setEntryValues(new String[]{"key"});
        return listPreference;
    }

    private DropDownPreference createDropDownPreference() {
        DropDownPreference dropDownPreference = new DropDownPreference(mContext);
        dropDownPreference.setPersistent(false);
        // User visible options in the dropdown
        dropDownPreference.setEntries(new String[]{"New value"});
        // Value persisted internally that corresponds to the user visible option
        dropDownPreference.setEntryValues(new String[]{"key"});
        return dropDownPreference;
    }

    private EditTextPreference createEditTextPreference() {
        EditTextPreference editTextPreference = new EditTextPreference(mContext);
        editTextPreference.setPersistent(false);
        return editTextPreference;
    }
}
