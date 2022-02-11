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
public class SparseArrayCompatTest {
    @Test
    public void getOrDefaultPrefersStoredValue() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, "1");
        assertEquals("1", map.get(1, "2"));
    }

    @Test
    public void getOrDefaultUsesDefaultWhenAbsent() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        assertEquals("1", map.get(1, "1"));
    }

    @Test
    public void getOrDefaultReturnsNullWhenNullStored() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, null);
        assertNull(map.get(1, "1"));
    }

    @Test
    public void getOrDefaultDoesNotPersistDefault() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.get(1, "1");
        assertFalse(map.containsKey(1));
    }

    @Test
    public void putIfAbsentDoesNotOverwriteStoredValue() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, "1");
        map.putIfAbsent(1, "2");
        assertEquals("1", map.get(1));
    }

    @Test
    public void putIfAbsentReturnsStoredValue() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, "1");
        assertEquals("1", map.putIfAbsent(1, "2"));
    }

    @Test
    public void putIfAbsentStoresValueWhenAbsent() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.putIfAbsent(1, "2");
        assertEquals("2", map.get(1));
    }

    @Test
    public void putIfAbsentReturnsNullWhenAbsent() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        assertNull(map.putIfAbsent(1, "2"));
    }

    @Test
    public void replaceWhenAbsentDoesNotStore() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        assertNull(map.replace(1, "1"));
        assertFalse(map.containsKey(1));
    }

    @Test
    public void replaceStoresAndReturnsOldValue() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, "1");
        assertEquals("1", map.replace(1, "2"));
        assertEquals("2", map.get(1));
    }

    @Test
    public void replaceStoresAndReturnsNullWhenMappedToNull() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, null);
        assertNull(map.replace(1, "1"));
        assertEquals("1", map.get(1));
    }

    @Test
    public void replaceValueKeyAbsent() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        assertFalse(map.replace(1, "1", "2"));
        assertFalse(map.containsKey(1));
    }

    @Test
    public void replaceValueMismatchDoesNotReplace() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, "1");
        assertFalse(map.replace(1, "2", "3"));
        assertEquals("1", map.get(1));
    }

    @Test
    public void replaceValueMismatchNullDoesNotReplace() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, "1");
        assertFalse(map.replace(1, null, "2"));
        assertEquals("1", map.get(1));
    }

    @Test
    public void replaceValueMatchReplaces() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, "1");
        assertTrue(map.replace(1, "1", "2"));
        assertEquals("2",  map.get(1));
    }

    @Test
    public void replaceNullValueMismatchDoesNotReplace() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, null);
        assertFalse(map.replace(1, "1", "2"));
        assertNull(map.get(1));
    }

    @Test
    public void replaceNullValueMatchRemoves() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, null);
        assertTrue(map.replace(1, null, "1"));
        assertEquals("1", map.get(1));
    }

    @Test
    public void removeValueKeyAbsent() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        assertFalse(map.remove(1, "1"));
    }

    @Test
    public void removeValueMismatchDoesNotRemove() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, "1");
        assertFalse(map.remove(1, "2"));
        assertTrue(map.containsKey(1));
    }

    @Test
    public void removeValueMismatchNullDoesNotRemove() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, "1");
        assertFalse(map.remove(1, null));
        assertTrue(map.containsKey(1));
    }

    @Test
    public void removeValueMatchRemoves() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, "1");
        assertTrue(map.remove(1, "1"));
        assertFalse(map.containsKey(1));
    }

    @Test
    public void removeNullValueMismatchDoesNotRemove() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, null);
        assertFalse(map.remove(1, "2"));
        assertTrue(map.containsKey(1));
    }

    @Test
    public void removeNullValueMatchRemoves() {
        SparseArrayCompat<String> map = new SparseArrayCompat<>();
        map.put(1, null);
        assertTrue(map.remove(1, null));
        assertFalse(map.containsKey(1));
    }

    @Test
    public void isEmpty() throws Exception {
        SparseArrayCompat<String> sparseArrayCompat = new SparseArrayCompat<>();
        assertTrue(sparseArrayCompat.isEmpty()); // Newly created SparseArrayCompat should be empty

        // Adding elements should change state from empty to not empty.
        for (int i = 0; i < 5; i++) {
            sparseArrayCompat.put(i, Integer.toString(i));
            assertFalse(sparseArrayCompat.isEmpty());
        }
        sparseArrayCompat.clear();
        assertTrue(sparseArrayCompat.isEmpty()); // A cleared SparseArrayCompat should be empty.


        int key1 = 1, key2 = 2;
        String value1 = "some value", value2 = "some other value";
        sparseArrayCompat.append(key1, value1);
        assertFalse(sparseArrayCompat.isEmpty()); // has 1 element.
        sparseArrayCompat.append(key2, value2);
        assertFalse(sparseArrayCompat.isEmpty());  // has 2 elements.
        assertFalse(sparseArrayCompat.isEmpty());  // consecutive calls should be OK.

        sparseArrayCompat.remove(key1);
        assertFalse(sparseArrayCompat.isEmpty()); // has 1 element.
        sparseArrayCompat.remove(key2);
        assertTrue(sparseArrayCompat.isEmpty());
    }

    @Test
    public void containsKey() {
        SparseArrayCompat<String> array = new SparseArrayCompat<>();
        array.put(1, "one");

        assertTrue(array.containsKey(1));
        assertFalse(array.containsKey(2));
    }

    @Test
    public void containsValue() {
        SparseArrayCompat<String> array = new SparseArrayCompat<>();
        array.put(1, "one");

        assertTrue(array.containsValue("one"));
        assertFalse(array.containsValue("two"));
    }

    @Test
    public void putAll() {
        SparseArrayCompat<String> dest = new SparseArrayCompat<>();
        dest.put(1, "one");
        dest.put(3, "three");

        SparseArrayCompat<String> source = new SparseArrayCompat<>();
        source.put(1, "uno");
        source.put(2, "dos");

        dest.putAll(source);
        assertEquals(3, dest.size());
        assertEquals("uno", dest.get(1));
        assertEquals("dos", dest.get(2));
        assertEquals("three", dest.get(3));
    }

    @Test
    public void putAllVariance() {
        SparseArrayCompat<Object> dest = new SparseArrayCompat<>();
        dest.put(1, 1L);

        SparseArrayCompat<String> source = new SparseArrayCompat<>();
        dest.put(2, "two");

        dest.putAll(source);
        assertEquals(2, dest.size());
        assertEquals(1L, dest.get(1));
        assertEquals("two", dest.get(2));
    }

    @Test
    public void cloning() {
        SparseArrayCompat<String> source = new SparseArrayCompat<>();
        source.put(10, "hello");
        source.put(20, "world");

        SparseArrayCompat<String> dest = source.clone();
        assertNotSame(source, dest);

        for (int i = 0; i < source.size(); i++) {
            assertEquals(source.keyAt(i), dest.keyAt(i));
            assertEquals(source.valueAt(i), dest.valueAt(i));
        }
    }
}
