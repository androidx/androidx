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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LongSparseArrayTest {
    @Test
    public void getOrDefaultPrefersStoredValue() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, "1");
        assertEquals("1", map.get(1L, "2"));
    }

    @Test
    public void getOrDefaultUsesDefaultWhenAbsent() {
        LongSparseArray<String> map = new LongSparseArray<>();
        assertEquals("1", map.get(1L, "1"));
    }

    @Test
    public void getOrDefaultReturnsNullWhenNullStored() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, null);
        assertNull(map.get(1L, "1"));
    }

    @Test
    public void getOrDefaultDoesNotPersistDefault() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.get(1L, "1");
        assertFalse(map.containsKey(1L));
    }

    @Test
    public void putIfAbsentDoesNotOverwriteStoredValue() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, "1");
        map.putIfAbsent(1L, "2");
        assertEquals("1", map.get(1L));
    }

    @Test
    public void putIfAbsentReturnsStoredValue() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, "1");
        assertEquals("1", map.putIfAbsent(1L, "2"));
    }

    @Test
    public void putIfAbsentStoresValueWhenAbsent() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.putIfAbsent(1L, "2");
        assertEquals("2", map.get(1L));
    }

    @Test
    public void putIfAbsentReturnsNullWhenAbsent() {
        LongSparseArray<String> map = new LongSparseArray<>();
        assertNull(map.putIfAbsent(1L, "2"));
    }

    @Test
    public void replaceWhenAbsentDoesNotStore() {
        LongSparseArray<String> map = new LongSparseArray<>();
        assertNull(map.replace(1L, "1"));
        assertFalse(map.containsKey(1L));
    }

    @Test
    public void replaceStoresAndReturnsOldValue() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, "1");
        assertEquals("1", map.replace(1L, "2"));
        assertEquals("2", map.get(1L));
    }

    @Test
    public void replaceStoresAndReturnsNullWhenMappedToNull() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, null);
        assertNull(map.replace(1L, "1"));
        assertEquals("1", map.get(1L));
    }

    @Test
    public void replaceValueKeyAbsent() {
        LongSparseArray<String> map = new LongSparseArray<>();
        assertFalse(map.replace(1L, "1", "2"));
        assertFalse(map.containsKey(1L));
    }

    @Test
    public void replaceValueMismatchDoesNotReplace() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, "1");
        assertFalse(map.replace(1L, "2", "3"));
        assertEquals("1", map.get(1L));
    }

    @Test
    public void replaceValueMismatchNullDoesNotReplace() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, "1");
        assertFalse(map.replace(1L, null, "2"));
        assertEquals("1", map.get(1L));
    }

    @Test
    public void replaceValueMatchReplaces() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, "1");
        assertTrue(map.replace(1L, "1", "2"));
        assertEquals("2",  map.get(1L));
    }

    @Test
    public void replaceNullValueMismatchDoesNotReplace() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, null);
        assertFalse(map.replace(1L, "1", "2"));
        assertNull(map.get(1L));
    }

    @Test
    public void replaceNullValueMatchRemoves() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, null);
        assertTrue(map.replace(1L, null, "1"));
        assertEquals("1", map.get(1L));
    }

    @Test
    public void removeValueKeyAbsent() {
        LongSparseArray<String> map = new LongSparseArray<>();
        assertFalse(map.remove(1L, "1"));
    }

    @Test
    public void removeValueMismatchDoesNotRemove() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, "1");
        assertFalse(map.remove(1L, "2"));
        assertTrue(map.containsKey(1L));
    }

    @Test
    public void removeValueMismatchNullDoesNotRemove() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, "1");
        assertFalse(map.remove(1L, null));
        assertTrue(map.containsKey(1L));
    }

    @Test
    public void removeValueMatchRemoves() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, "1");
        assertTrue(map.remove(1L, "1"));
        assertFalse(map.containsKey(1L));
    }

    @Test
    public void removeNullValueMismatchDoesNotRemove() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, null);
        assertFalse(map.remove(1L, "2"));
        assertTrue(map.containsKey(1L));
    }

    @Test
    public void removeNullValueMatchRemoves() {
        LongSparseArray<String> map = new LongSparseArray<>();
        map.put(1L, null);
        assertTrue(map.remove(1L, null));
        assertFalse(map.containsKey(1L));
    }

    @Test
    public void isEmpty() {
        LongSparseArray<String> LongSparseArray = new LongSparseArray<>();
        assertTrue(LongSparseArray.isEmpty()); // Newly created LongSparseArray should be empty

        // Adding elements should change state from empty to not empty.
        for (long i = 0L; i < 5L; i++) {
            LongSparseArray.put(i, Long.toString(i));
            assertFalse(LongSparseArray.isEmpty());
        }
        LongSparseArray.clear();
        assertTrue(LongSparseArray.isEmpty()); // A cleared LongSparseArray should be empty.


        long key1 = 1L, key2 = 2L;
        String value1 = "some value", value2 = "some other value";
        LongSparseArray.append(key1, value1);
        assertFalse(LongSparseArray.isEmpty()); // has 1 element.
        LongSparseArray.append(key2, value2);
        assertFalse(LongSparseArray.isEmpty());  // has 2 elements.
        assertFalse(LongSparseArray.isEmpty());  // consecutive calls should be OK.

        LongSparseArray.remove(key1);
        assertFalse(LongSparseArray.isEmpty()); // has 1 element.
        LongSparseArray.remove(key2);
        assertTrue(LongSparseArray.isEmpty());
    }

    @Test
    public void containsKey() {
        LongSparseArray<String> array = new LongSparseArray<>();
        array.put(1L, "one");

        assertTrue(array.containsKey(1L));
        assertFalse(array.containsKey(2L));
    }

    @Test
    public void containsValue() {
        LongSparseArray<String> array = new LongSparseArray<>();
        array.put(1L, "one");

        assertTrue(array.containsValue("one"));
        assertFalse(array.containsValue("two"));
    }

    @Test
    public void putAll() {
        LongSparseArray<String> dest = new LongSparseArray<>();
        dest.put(1L, "one");
        dest.put(3L, "three");

        LongSparseArray<String> source = new LongSparseArray<>();
        source.put(1L, "uno");
        source.put(2L, "dos");

        dest.putAll(source);
        assertEquals(3, dest.size());
        assertEquals("uno", dest.get(1L));
        assertEquals("dos", dest.get(2L));
        assertEquals("three", dest.get(3L));
    }

    @Test
    public void putAllVariance() {
        LongSparseArray<Object> dest = new LongSparseArray<>();
        dest.put(1L, 1L);

        LongSparseArray<String> source = new LongSparseArray<>();
        dest.put(2L, "two");

        dest.putAll(source);
        assertEquals(2, dest.size());
        assertEquals(1L, dest.get(1L));
        assertEquals("two", dest.get(2L));
    }

    @Test
    public void cloning() {
        LongSparseArray<String> source = new LongSparseArray<>();
        source.put(10L, "hello");
        source.put(20L, "world");

        LongSparseArray<String> dest = source.clone();
        assertNotSame(source, dest);

        for (int i = 0; i < source.size(); i++) {
            assertEquals(source.keyAt(i), dest.keyAt(i));
            assertEquals(source.valueAt(i), dest.valueAt(i));
        }
    }
}
