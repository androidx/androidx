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
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.test.R;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for {@link androidx.preference.Preference} visibility set with XML.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PreferenceVisibilityTest {

    @Test
    @UiThreadTest
    public void testPreferencesAreCreatedWithTheVisibilitySetInXml() {
        final Context context = InstrumentationRegistry.getTargetContext();
        final PreferenceManager manager = new PreferenceManager(context);
        final PreferenceScreen screen = manager.inflateFromResource(context,
                R.layout.test_visibility,
                null);

        // Preference without visibility set should be visible
        assertTrue(screen.getPreference(0).isVisible());
        // Preference with visibility set to true should be visible
        assertTrue(screen.getPreference(1).isVisible());
        // Preference with visibility set to false should not be invisible
        assertFalse(screen.getPreference(2).isVisible());
    }
}
