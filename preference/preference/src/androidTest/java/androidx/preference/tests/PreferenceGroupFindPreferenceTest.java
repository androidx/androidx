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

package androidx.preference.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
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
 * Test for {@link PreferenceGroup#findPreference(CharSequence)}
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PreferenceGroupFindPreferenceTest {

    private static final String KEY = "test_key";

    private Preference mPreference;
    private PreferenceScreen mScreen;

    @Before
    @UiThreadTest
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        PreferenceManager manager = new PreferenceManager(context);
        mPreference = new Preference(context);
        mPreference.setKey(KEY);
        mScreen = manager.createPreferenceScreen(context);
    }

    /**
     * Tests that the {@link Preference} with the given key is correctly returned.
     */
    @Test
    @UiThreadTest
    public void preferenceInGroup_findPreferenceReturnsPreference() {
        mScreen.addPreference(mPreference);
        assertEquals(mPreference, mScreen.findPreference(KEY));
    }

    /**
     * Tests that {@code null} is returned if no {@link Preference} exists with the given key.
     */
    @Test
    @UiThreadTest
    public void preferenceNotInGroup_findPreferenceReturnsNull() {
        // Preference not added to the group
        assertNull(mScreen.findPreference(KEY));
    }

    /**
     * Tests that an exception is thrown if the key is null.
     */
    @Test(expected = IllegalArgumentException.class)
    @UiThreadTest
    public void findPreferenceWithNullKey_exceptionThrown() {
        mScreen.findPreference(null);
    }
}
