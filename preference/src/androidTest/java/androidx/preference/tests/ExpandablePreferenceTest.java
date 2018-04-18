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

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for expandable preferences in various contexts..
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ExpandablePreferenceTest {

    private Context mContext;
    private Preference mPreference1;
    private Preference mPreference2;
    private Preference mPreference3;
    private PreferenceScreen mScreen;

    @Before
    @UiThreadTest
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);

        mPreference1 = new Preference(mContext);
        mPreference1.setTitle("Preference 1");

        mPreference2 = new Preference(mContext);
        mPreference2.setTitle("Preference 2");

        mPreference3 = new Preference(mContext);
        mPreference3.setTitle("Preference 3");
    }

    @Test
    @UiThreadTest
    public void expandablePreference_inPreferenceScreen_collapsesCorrectly() {

        mScreen.setKey("screen");
        mScreen.setInitialExpandedChildrenCount(1);

        mScreen.addPreference(mPreference1);
        mScreen.addPreference(mPreference2);
        mScreen.addPreference(mPreference3);

        PreferenceGroupAdapter preferenceGroupAdapter = new PreferenceGroupAdapter(mScreen);

        assertEquals(2, preferenceGroupAdapter.getItemCount());

        assertEquals(mPreference1, preferenceGroupAdapter.getItem(0));
        assertEquals("Advanced", preferenceGroupAdapter.getItem(1).getTitle());
        assertEquals("Preference 2, Preference 3", preferenceGroupAdapter.getItem(1).getSummary());
    }

    @Test
    @UiThreadTest
    public void expandablePreference_inCategory_collapsesCorrectly() {
        PreferenceCategory category = new PreferenceCategory(mContext);

        mScreen.addPreference(category);

        category.setKey("category");
        category.setInitialExpandedChildrenCount(1);

        category.addPreference(mPreference1);
        category.addPreference(mPreference2);
        category.addPreference(mPreference3);

        PreferenceGroupAdapter preferenceGroupAdapter = new PreferenceGroupAdapter(mScreen);

        assertEquals(3, preferenceGroupAdapter.getItemCount());

        assertEquals(category, preferenceGroupAdapter.getItem(0));
        assertEquals(mPreference1, preferenceGroupAdapter.getItem(1));
        assertEquals("Advanced", preferenceGroupAdapter.getItem(2).getTitle());
        assertEquals("Preference 2, Preference 3", preferenceGroupAdapter.getItem(2).getSummary());
    }

    @Test
    @UiThreadTest
    public void expandablePreference_inNestedCategory_collapsesCorrectly() {
        PreferenceCategory category = new PreferenceCategory(mContext);
        PreferenceCategory nestedCategory = new PreferenceCategory(mContext);

        mScreen.addPreference(category);
        category.addPreference(nestedCategory);

        nestedCategory.setKey("nested_category");
        nestedCategory.setInitialExpandedChildrenCount(1);

        nestedCategory.addPreference(mPreference1);
        nestedCategory.addPreference(mPreference2);
        nestedCategory.addPreference(mPreference3);

        PreferenceGroupAdapter preferenceGroupAdapter = new PreferenceGroupAdapter(mScreen);

        assertEquals(4, preferenceGroupAdapter.getItemCount());

        assertEquals(category, preferenceGroupAdapter.getItem(0));
        assertEquals(nestedCategory, preferenceGroupAdapter.getItem(1));
        assertEquals(mPreference1, preferenceGroupAdapter.getItem(2));
        assertEquals("Advanced", preferenceGroupAdapter.getItem(3).getTitle());
        assertEquals("Preference 2, Preference 3", preferenceGroupAdapter.getItem(3).getSummary());
    }

    @Test
    @UiThreadTest
    public void expandablePreference_inCategoryContainingAnotherCategory_collapsesCorrectly() {
        PreferenceCategory category = new PreferenceCategory(mContext);
        PreferenceCategory nestedCategory = new PreferenceCategory(mContext);

        mScreen.addPreference(category);

        category.setKey("nested_category");
        category.setInitialExpandedChildrenCount(1);

        category.addPreference(mPreference1);
        category.addPreference(nestedCategory);
        nestedCategory.addPreference(mPreference2);
        nestedCategory.addPreference(mPreference3);

        PreferenceGroupAdapter preferenceGroupAdapter = new PreferenceGroupAdapter(mScreen);

        assertEquals(3, preferenceGroupAdapter.getItemCount());

        assertEquals(category, preferenceGroupAdapter.getItem(0));
        assertEquals(mPreference1, preferenceGroupAdapter.getItem(1));
        assertEquals("Advanced", preferenceGroupAdapter.getItem(2).getTitle());
        assertEquals("Preference 2, Preference 3", preferenceGroupAdapter.getItem(2).getSummary());

        // If the nested category has a title, display that in the summary instead of the children
        final String title = "Category";
        nestedCategory.setTitle(title);

        preferenceGroupAdapter = new PreferenceGroupAdapter(mScreen);

        assertEquals(3, preferenceGroupAdapter.getItemCount());

        assertEquals(category, preferenceGroupAdapter.getItem(0));
        assertEquals(mPreference1, preferenceGroupAdapter.getItem(1));
        assertEquals("Advanced", preferenceGroupAdapter.getItem(2).getTitle());
        assertEquals(title, preferenceGroupAdapter.getItem(2).getSummary());
    }

    @Test(expected = IllegalArgumentException.class)
    @UiThreadTest
    public void nestedExpandablePreferences_notAllowed_shouldThrowAnException() {
        PreferenceCategory category = new PreferenceCategory(mContext);
        PreferenceCategory nestedCategory = new PreferenceCategory(mContext);

        mScreen.addPreference(category);
        category.addPreference(nestedCategory);

        category.setKey("category");
        category.setInitialExpandedChildrenCount(1);

        nestedCategory.setKey("nested_category");
        nestedCategory.setInitialExpandedChildrenCount(1);

        // Trying to nest expandable preferences should throw an exception
        new PreferenceGroupAdapter(mScreen);
    }
}
