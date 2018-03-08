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
package androidx.collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class ArrayMapCompatTest {

    @Test
    public void testCanNotIteratePastEnd_entrySetIterator() {
        Map<String, String> map = new ArrayMap<>();
        map.put("key 1", "value 1");
        map.put("key 2", "value 2");
        Set<Map.Entry<String, String>> expectedEntriesToIterate = new HashSet<>(Arrays.asList(
                entryOf("key 1", "value 1"),
                entryOf("key 2", "value 2")
        ));
        Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();

        // Assert iteration over the expected two entries in any order
        assertTrue(iterator.hasNext());
        Map.Entry<String, String> firstEntry = copyOf(iterator.next());
        assertTrue(expectedEntriesToIterate.remove(firstEntry));

        assertTrue(iterator.hasNext());
        Map.Entry<String, String> secondEntry = copyOf(iterator.next());
        assertTrue(expectedEntriesToIterate.remove(secondEntry));

        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail();
        } catch (NoSuchElementException expected) {
        }
    }

    private static <K, V> Map.Entry<K, V> entryOf(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    private static <K, V> Map.Entry<K, V> copyOf(Map.Entry<K, V> entry) {
        return entryOf(entry.getKey(), entry.getValue());
    }

    @Test
    public void testCanNotIteratePastEnd_keySetIterator() {
        Map<String, String> map = new ArrayMap<>();
        map.put("key 1", "value 1");
        map.put("key 2", "value 2");
        Set<String> expectedKeysToIterate = new HashSet<>(Arrays.asList("key 1", "key 2"));
        Iterator<String> iterator = map.keySet().iterator();

        // Assert iteration over the expected two keys in any order
        assertTrue(iterator.hasNext());
        String firstKey = iterator.next();
        assertTrue(expectedKeysToIterate.remove(firstKey));

        assertTrue(iterator.hasNext());
        String secondKey = iterator.next();
        assertTrue(expectedKeysToIterate.remove(secondKey));

        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail();
        } catch (NoSuchElementException expected) {
        }
    }

    @Test
    public void testCanNotIteratePastEnd_valuesIterator() {
        Map<String, String> map = new ArrayMap<>();
        map.put("key 1", "value 1");
        map.put("key 2", "value 2");
        Set<String> expectedValuesToIterate = new HashSet<>(Arrays.asList("value 1", "value 2"));
        Iterator<String> iterator = map.values().iterator();

        // Assert iteration over the expected two values in any order
        assertTrue(iterator.hasNext());
        String firstValue = iterator.next();
        assertTrue(expectedValuesToIterate.remove(firstValue));

        assertTrue(iterator.hasNext());
        String secondValue = iterator.next();
        assertTrue(expectedValuesToIterate.remove(secondValue));

        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail();
        } catch (NoSuchElementException expected) {
        }
    }
}
