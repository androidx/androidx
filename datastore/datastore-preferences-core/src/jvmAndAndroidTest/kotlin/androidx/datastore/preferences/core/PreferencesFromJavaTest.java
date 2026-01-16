/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.datastore.preferences.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class PreferencesFromJavaTest {

    @Test
    public void testStoreAndGetInteger() {
        Preferences.Key<Integer> integerKey = PreferencesKeys.intKey("integer_key");

        MutablePreferences mutablePreferences = PreferencesFactory.createMutable();
        mutablePreferences.set(integerKey, 123);

        assertEquals(1, mutablePreferences.asMap().size());
        assertEquals(Integer.valueOf(123), mutablePreferences.get(integerKey));
    }

    @Test
    public void testAllKeyTypes() {
        Preferences.Key<Integer> integerKey = PreferencesKeys.intKey("integer_key");
        Preferences.Key<Long> longKey = PreferencesKeys.longKey("long_key");
        Preferences.Key<Float> floatKey = PreferencesKeys.floatKey("float_key");
        Preferences.Key<Double> doubleKey = PreferencesKeys.doubleKey("double_key");
        Preferences.Key<String> stringKey = PreferencesKeys.stringKey("string_key");
        Preferences.Key<Boolean> booleanKey = PreferencesKeys.booleanKey("boolean_key");
        Preferences.Key<Set<String>> stringSetKey = PreferencesKeys.stringSetKey(
                "string_set_key");

        MutablePreferences mutablePreferences = PreferencesFactory.createMutable();
        mutablePreferences.set(integerKey, 123);
        mutablePreferences.set(longKey, 1234567890123L);
        mutablePreferences.set(floatKey, 1.23f);
        mutablePreferences.set(doubleKey, 1.23d);
        mutablePreferences.set(stringKey, "123");
        mutablePreferences.set(booleanKey, true);
        mutablePreferences.set(stringSetKey,
                new HashSet<>(Arrays.asList("1", "2", "3")));

        assertEquals(Integer.valueOf(123),
                mutablePreferences.get(integerKey));
        assertEquals(Long.valueOf(1234567890123L),
                mutablePreferences.get(longKey));
        assertEquals(Float.valueOf(1.23f),
                mutablePreferences.get(floatKey));
        assertEquals(Double.valueOf(1.23d),
                mutablePreferences.get(doubleKey));
        assertEquals("123",
                mutablePreferences.get(stringKey));

        assertEquals(new HashSet<>(Arrays.asList("1", "2", "3")),
                mutablePreferences.get(stringSetKey));
    }

    @Test
    public void testNonExistentKeyIsNull() {
        MutablePreferences mutablePreferences = PreferencesFactory.createMutable();
        Preferences.Key<Integer> integerKey = PreferencesKeys.intKey("integer_key");

        assertNull(mutablePreferences.get(integerKey));
    }

    @Test
    public void testMutablePreferencesOfConstructor() {
        Preferences.Key<Integer> integerKey = PreferencesKeys.intKey("integer_key");

        MutablePreferences mutablePreferences =
                PreferencesFactory.createMutable(integerKey.to(123));
        assertEquals(Integer.valueOf(123), mutablePreferences.get(integerKey));
    }

    @Test
    public void testCreateEmpty() {
        assertEquals(PreferencesFactory.createMutable(), PreferencesFactory.createEmpty());
        assertEquals(PreferencesFactory.create(), PreferencesFactory.createEmpty());
        assertEquals(0, PreferencesFactory.createEmpty().asMap().size());
    }
}
