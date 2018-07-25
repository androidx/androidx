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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.test.R;
import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for {@link androidx.preference.Preference} visibility logic.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PreferenceVisibilityTest {

    private Context mContext;
    private PreferenceManager mPreferenceManager;

    @Before
    @UiThreadTest
    public void setup() {
        mContext = InstrumentationRegistry.getTargetContext();
        mPreferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = mPreferenceManager.inflateFromResource(mContext,
                R.xml.test_visibility,
                null);
        mPreferenceManager.setPreferences(screen);
    }

    @Test
    @UiThreadTest
    public void preferencesInflatedFromXml_visibilitySetCorrectly() {
        // Given visible ancestors that are correctly attached to the root preference screen:
        // Preference without visibility set should be visible
        assertTrue(mPreferenceManager.findPreference("default_visibility").isVisible());
        // Preference with visibility set to true should be visible
        assertTrue(mPreferenceManager.findPreference("visible").isVisible());
        // Preference with visibility set to false should not be visible
        assertFalse(mPreferenceManager.findPreference("invisible").isVisible());
    }

    @Test
    @UiThreadTest
    public void preferencesInflatedFromXml_isShownShouldMatchVisibility() {
        // Given visible ancestors that are correctly attached to the root preference screen:
        // Preference without visibility set should be shown
        assertTrue(mPreferenceManager.findPreference("default_visibility").isShown());
        // Preference with visibility set to true should be shown
        assertTrue(mPreferenceManager.findPreference("visible").isShown());
        // Preference with visibility set to false should not be shown
        assertFalse(mPreferenceManager.findPreference("invisible").isShown());
    }

    @Test
    @UiThreadTest
    public void hidingParentGroup_childVisibilityUnchanged() {
        // Hide the parent category
        mPreferenceManager.findPreference("category").setVisible(false);

        // Category should not be visible or shown
        assertFalse(mPreferenceManager.findPreference("category").isVisible());
        assertFalse(mPreferenceManager.findPreference("category").isShown());

        // Preference visibility should be unchanged
        assertTrue(mPreferenceManager.findPreference("default_visibility").isVisible());
        assertTrue(mPreferenceManager.findPreference("visible").isVisible());
        assertFalse(mPreferenceManager.findPreference("invisible").isVisible());
    }

    @Test
    @UiThreadTest
    public void hidingParentGroup_childrenNoLongerShown() {
        // Hide the parent category
        mPreferenceManager.findPreference("category").setVisible(false);

        // Category should not be visible or shown
        assertFalse(mPreferenceManager.findPreference("category").isVisible());
        assertFalse(mPreferenceManager.findPreference("category").isShown());

        // Preferences should not be shown since their parent is not visible
        assertFalse(mPreferenceManager.findPreference("default_visibility").isShown());
        assertFalse(mPreferenceManager.findPreference("visible").isShown());
        assertFalse(mPreferenceManager.findPreference("invisible").isShown());
    }

    @Test
    @UiThreadTest
    public void preferenceNotAttachedToHierarchy_visibleButNotShown() {
        // Create a new preference not attached to the root preference screen
        Preference preference = new Preference(mContext);

        // Preference is visible, but since it is not attached to the hierarchy, it is not shown
        assertTrue(preference.isVisible());
        assertFalse(preference.isShown());
    }
}
