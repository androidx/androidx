/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.tests.helpers.PreferenceWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link androidx.preference.Preference} persist / retrieve logic.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PreferencePersistTest {

    private static final String KEY = "TestPrefKey";

    private static final float FLOAT_PRECISION = 0.01f;

    private static final String[] A_B = {"a", "b"};
    private static final String[] C_D = {"c", "d"};
    private static final Set<String> TEST_STR_SET = new HashSet<>(Arrays.asList(A_B));
    private static final Set<String> TEST_STR_SET2 = new HashSet<>(Arrays.asList(C_D));
    private static final Set<String> TEST_DEFAULT_STR_SET = Collections.singleton("e");

    private PreferenceWrapper mPreference;
    private SharedPreferences mSharedPref;

    @Before
    @UiThreadTest
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        PreferenceManager manager = new PreferenceManager(context);
        mSharedPref = manager.getSharedPreferences();

        mPreference = new PreferenceWrapper(context);
        mPreference.setKey(KEY);

        PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.addPreference(mPreference);

        // Make sure that the key is not present in SharedPreferences to ensure tests
        // correctness.
        mSharedPref.edit().remove(KEY).apply();
        assertNull(mSharedPref.getString(KEY, null));
    }

    @Test
    @UiThreadTest
    public void string_retrieveWhenEmpty_returnsDefault() {
        final String expected = "Default";

        String result = mPreference.getString(expected);

        assertEquals(expected, result);
    }

    @Test
    @UiThreadTest
    public void string_persist_getsStoredToSharedPrefs() {
        final String expected = "Test";

        boolean wasPersisted = mPreference.putString(expected);

        assertTrue(wasPersisted);
        assertEquals(expected, mSharedPref.getString(KEY, null));
    }

    @Test
    @UiThreadTest
    public void string_persistWhileDisabled_notPersisted() {
        mPreference.setPersistent(false);

        boolean wasPersisted = mPreference.putString("Test");

        assertFalse(wasPersisted);
        assertNull(mSharedPref.getString(KEY, null));
    }

    @Test
    @UiThreadTest
    public void string_persistAndRetrieve_returnsPersistedValue() {
        final String expected = "Test";

        mPreference.putString(expected);
        String result = mPreference.getString("Default");

        assertEquals(expected, result);
    }

    @Test
    @UiThreadTest
    public void string_persistTwiceAndRetrieve_returnsSecondValue() {
        final String expected = "Second";

        mPreference.putString("First");
        mPreference.putString(expected);
        String result = mPreference.getString("Default");

        assertEquals(expected, result);
    }


    @Test
    @UiThreadTest
    public void stringSet_retrieveWhenEmpty_returnsDefault() {
        final Set<String> expected = TEST_DEFAULT_STR_SET;

        Set<String> result = mPreference.getStringSet(expected);

        assertThat(result, containsInAnyOrder(expected.toArray()));
    }

    @Test
    @UiThreadTest
    public void stringSet_persist_getsStoredToSharedPrefs() {
        boolean wasPersisted = mPreference.putStringSet(TEST_DEFAULT_STR_SET);

        assertTrue(wasPersisted);
        assertThat(mSharedPref.getStringSet(KEY, null),
                containsInAnyOrder(TEST_DEFAULT_STR_SET.toArray()));
    }

    @Test
    @UiThreadTest
    public void stringSet_persistWhileDisabled_notPersisted() {
        mPreference.setPersistent(false);

        boolean wasPersisted = mPreference.putStringSet(TEST_STR_SET);

        assertFalse(wasPersisted);
        assertNull(mSharedPref.getString(KEY, null));
    }

    @Test
    @UiThreadTest
    public void stringSet_persistAndRetrieve_returnsPersistedValue() {
        final Set<String> expected = TEST_STR_SET;

        mPreference.putStringSet(expected);
        Set<String> result = mPreference.getStringSet(TEST_DEFAULT_STR_SET);

        assertThat(result, containsInAnyOrder(expected.toArray()));
    }

    @Test
    @UiThreadTest
    public void stringSet_persistTwiceAndRetrieve_returnsSecondValue() {
        final Set<String> expected = TEST_STR_SET2;

        mPreference.putStringSet(TEST_STR_SET);
        mPreference.putStringSet(expected);
        Set<String> result = mPreference.getStringSet(TEST_DEFAULT_STR_SET);

        assertThat(result, containsInAnyOrder(expected.toArray()));
    }


    @Test
    @UiThreadTest
    public void int_retrieveWhenEmpty_returnsDefault() {
        final int expected = 1;
        int result = mPreference.getInt(expected);

        assertEquals(expected, result);
    }

    @Test
    @UiThreadTest
    public void int_persist_getsStoredToSharedPrefs() {
        final int expected = 1;

        boolean wasPersisted = mPreference.putInt(expected);

        assertTrue(wasPersisted);
        assertEquals(expected, mSharedPref.getInt(KEY, -1));
    }

    @Test
    @UiThreadTest
    public void int_persistWhileDisabled_notPersisted() {
        mPreference.setPersistent(false);

        boolean wasPersisted = mPreference.putInt(1);

        assertFalse(wasPersisted);
        assertEquals(-1, mSharedPref.getLong(KEY, -1));
    }

    @Test
    @UiThreadTest
    public void int_persistAndRetrieve_returnsPersistedValue() {
        final int expected = 1;

        mPreference.putInt(expected);
        int result = mPreference.getInt(-1);

        assertEquals(expected, result);
    }

    @Test
    @UiThreadTest
    public void int_persistTwiceAndRetrieve_returnsSecondValue() {
        final int expected = 2;

        mPreference.putInt(1);
        mPreference.putInt(expected);
        int result = mPreference.getInt(-1);

        assertEquals(expected, result);
    }


    @Test
    @UiThreadTest
    public void long_retrieveWhenEmpty_returnsDefault() {
        assertEquals(1, mPreference.getLong(1));
    }

    @Test
    @UiThreadTest
    public void long_persist_getsStoredToSharedPrefs() {
        final long expected = 1;

        boolean wasPersisted = mPreference.putLong(expected);

        assertTrue(wasPersisted);
        assertEquals(expected, mSharedPref.getLong(KEY, -1));
    }

    @Test
    @UiThreadTest
    public void long_persistWhileDisabled_notPersisted() {
        mPreference.setPersistent(false);

        boolean wasPersisted = mPreference.putLong(1);

        assertFalse(wasPersisted);
        assertEquals(-1, mSharedPref.getLong(KEY, -1));
    }

    @Test
    @UiThreadTest
    public void long_persistAndRetrieve_returnsPersistedValue() {
        final long expected = 1;

        mPreference.putLong(expected);
        long result = mPreference.getLong(-1);

        assertEquals(expected, result);
    }

    @Test
    @UiThreadTest
    public void long_persistTwiceAndRetrieve_returnsSecondValue() {
        final long expected = 2;

        mPreference.putLong(1);
        mPreference.putLong(expected);
        long result = mPreference.getLong(-1);

        assertEquals(expected, result);
    }


    @Test
    @UiThreadTest
    public void float_retrieveWhenEmpty_returnsDefault() {
        assertEquals(1, mPreference.getFloat(1), FLOAT_PRECISION);
    }

    @Test
    @UiThreadTest
    public void float_persist_getsStoredToSharedPrefs() {
        final float expected = 1;

        boolean wasPersisted = mPreference.putFloat(expected);

        assertTrue(wasPersisted);
        assertEquals(expected, mSharedPref.getFloat(KEY, -1), FLOAT_PRECISION);
    }

    @Test
    @UiThreadTest
    public void float_persistWhileDisabled_notPersisted() {
        mPreference.setPersistent(false);

        boolean wasPersisted = mPreference.putFloat(1);

        assertFalse(wasPersisted);
        assertEquals(-1, mSharedPref.getFloat(KEY, -1), FLOAT_PRECISION);
    }

    @Test
    @UiThreadTest
    public void float_persistAndRetrieve_returnsPersistedValue() {
        final float expected = 1;

        mPreference.putFloat(expected);
        float result = mPreference.getFloat(-1);

        assertEquals(expected, result, FLOAT_PRECISION);
    }

    @Test
    @UiThreadTest
    public void float_persistTwiceAndRetrieve_returnsSecondValue() {
        final float expected = 2;

        mPreference.putFloat(1);
        mPreference.putFloat(expected);
        float result = mPreference.getFloat(-1);

        assertEquals(expected, result, FLOAT_PRECISION);
    }


    @Test
    @UiThreadTest
    public void boolean_retrieveWhenEmpty_returnsDefault() {
        final boolean expected = true;

        boolean result = mPreference.getBoolean(expected);

        assertEquals(expected, result);
    }

    @Test
    @UiThreadTest
    public void boolean_persist_getsStoredToSharedPrefs() {
        final boolean expected = true;

        boolean wasPersisted = mPreference.putBoolean(expected);

        assertTrue(wasPersisted);
        assertEquals(expected, mSharedPref.getBoolean(KEY, !expected));
    }

    @Test
    @UiThreadTest
    public void boolean_persistWhileDisabled_notPersisted() {
        mPreference.setPersistent(false);

        boolean wasPersisted = mPreference.putBoolean(true);

        assertFalse(wasPersisted);
        assertEquals(false, mSharedPref.getBoolean(KEY, false));
    }

    @Test
    @UiThreadTest
    public void boolean_persistAndRetrieve_returnsPersistedValue() {
        final boolean expected = true;

        mPreference.putBoolean(expected);
        boolean result = mPreference.getBoolean(!expected);

        assertEquals(expected, result);
    }

    @Test
    @UiThreadTest
    public void boolean_persistTwiceAndRetrieve_returnsSecondValue() {
        final boolean expected = false;

        mPreference.putBoolean(!expected);
        mPreference.putBoolean(expected);
        boolean result = mPreference.getBoolean(!expected);

        assertEquals(expected, result);
    }

}
