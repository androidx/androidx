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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(JUnit4.class)
public class SimpleArrayMapTest {
    @Test
    @SuppressWarnings({"SimplifiableJUnitAssertion", "EqualsWithItself",
            "EqualsBetweenInconvertibleTypes"})
    public void equalsEmpty() {
        SimpleArrayMap<String, String> empty = new SimpleArrayMap<>();

        assertTrue(empty.equals(empty));
        assertTrue(empty.equals(Collections.emptyMap()));
        assertTrue(empty.equals(new SimpleArrayMap<String, String>()));
        assertTrue(empty.equals(new HashMap<String, String>()));

        assertFalse(empty.equals(Collections.singletonMap("foo", "bar")));

        SimpleArrayMap<String, String> simpleArrayMapNotEmpty = new SimpleArrayMap<>();
        simpleArrayMapNotEmpty.put("foo", "bar");
        assertFalse(empty.equals(simpleArrayMapNotEmpty));

        HashMap<String, String> hashMapNotEquals = new HashMap<>();
        hashMapNotEquals.put("foo", "bar");
        assertFalse(empty.equals(hashMapNotEquals));
    }

    @Test
    @SuppressWarnings({"SimplifiableJUnitAssertion", "EqualsWithItself",
            "EqualsBetweenInconvertibleTypes"})
    public void equalsNonEmpty() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("foo", "bar");

        assertTrue(map.equals(map));
        assertTrue(map.equals(Collections.singletonMap("foo", "bar")));

        SimpleArrayMap<String, String> otherSimpleArrayMap = new SimpleArrayMap<>();
        otherSimpleArrayMap.put("foo", "bar");

        HashMap<String, String> otherHashMap = new HashMap<>();
        otherHashMap.put("foo", "bar");
        assertTrue(map.equals(otherHashMap));

        assertFalse(map.equals(Collections.emptyMap()));
        assertFalse(map.equals(new SimpleArrayMap<String, String>()));
        assertFalse(map.equals(new HashMap<String, String>()));
    }

    @Test
    public void getOrDefaultPrefersStoredValue() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", "1");
        assertEquals("1", map.getOrDefault("one", "2"));
    }

    @Test
    public void getOrDefaultUsesDefaultWhenAbsent() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        assertEquals("1", map.getOrDefault("one", "1"));
    }

    @Test
    public void getOrDefaultReturnsNullWhenNullStored() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", null);
        assertNull(map.getOrDefault("one", "1"));
    }

    @Test
    public void getOrDefaultDoesNotPersistDefault() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.getOrDefault("one", "1");
        assertFalse(map.containsKey("one"));
    }

    @Test
    public void putIfAbsentDoesNotOverwriteStoredValue() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", "1");
        map.putIfAbsent("one", "2");
        assertEquals("1", map.get("one"));
    }

    @Test
    public void putIfAbsentReturnsStoredValue() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", "1");
        assertEquals("1", map.putIfAbsent("one", "2"));
    }

    @Test
    public void putIfAbsentStoresValueWhenAbsent() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.putIfAbsent("one", "2");
        assertEquals("2", map.get("one"));
    }

    @Test
    public void putIfAbsentReturnsNullWhenAbsent() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        assertNull(map.putIfAbsent("one", "2"));
    }

    @Test
    public void replaceWhenAbsentDoesNotStore() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        assertNull(map.replace("one", "1"));
        assertFalse(map.containsKey("one"));
    }

    @Test
    public void replaceStoresAndReturnsOldValue() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", "1");
        assertEquals("1", map.replace("one", "2"));
        assertEquals("2", map.get("one"));
    }

    @Test
    public void replaceStoresAndReturnsNullWhenMappedToNull() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", null);
        assertNull(map.replace("one", "1"));
        assertEquals("1", map.get("one"));
    }

    @Test
    public void replaceValueKeyAbsent() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        assertFalse(map.replace("one", "1", "2"));
        assertFalse(map.containsKey("one"));
    }

    @Test
    public void replaceValueMismatchDoesNotReplace() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", "1");
        assertFalse(map.replace("one", "2", "3"));
        assertEquals("1", map.get("one"));
    }

    @Test
    public void replaceValueMismatchNullDoesNotReplace() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", "1");
        assertFalse(map.replace("one", null, "2"));
        assertEquals("1", map.get("one"));
    }

    @Test
    public void replaceValueMatchReplaces() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", "1");
        assertTrue(map.replace("one", "1", "2"));
        assertEquals("2",  map.get("one"));
    }

    @Test
    public void replaceNullValueMismatchDoesNotReplace() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", null);
        assertFalse(map.replace("one", "1", "2"));
        assertNull(map.get("one"));
    }

    @Test
    public void replaceNullValueMatchRemoves() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", null);
        assertTrue(map.replace("one", null, "1"));
        assertEquals("1", map.get("one"));
    }

    @Test
    public void removeValueKeyAbsent() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        assertFalse(map.remove("one", "1"));
    }

    @Test
    public void removeValueMismatchDoesNotRemove() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", "1");
        assertFalse(map.remove("one", "2"));
        assertTrue(map.containsKey("one"));
    }

    @Test
    public void removeValueMismatchNullDoesNotRemove() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", "1");
        assertFalse(map.remove("one", null));
        assertTrue(map.containsKey("one"));
    }

    @Test
    public void removeValueMatchRemoves() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", "1");
        assertTrue(map.remove("one", "1"));
        assertFalse(map.containsKey("one"));
    }

    @Test
    public void removeNullValueMismatchDoesNotRemove() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", null);
        assertFalse(map.remove("one", "2"));
        assertTrue(map.containsKey("one"));
    }

    @Test
    public void removeNullValueMatchRemoves() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put("one", null);
        assertTrue(map.remove("one", null));
        assertFalse(map.containsKey("one"));
    }

    /**
     * Attempt to generate a ConcurrentModificationException in ArrayMap.
     */
    @Test
    public void testConcurrentModificationException() {
        final SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        final AtomicBoolean done = new AtomicBoolean();

        final int TEST_LEN_MS = 5000;
        System.out.println("Starting SimpleArrayMap concurrency test");
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (!done.get()) {
                    try {
                        map.put(String.format(Locale.US, "key %d", i++), "B_DONT_DO_THAT");
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // SimpleArrayMap is not thread safe, so lots of concurrent modifications
                        // can still cause data corruption
                        System.err.println("concurrent modification uncaught, causing indexing failure");
                        e.printStackTrace();
                    } catch (ClassCastException e) {
                        // cache corruption should not occur as it is hard to trace and one thread
                        // may corrupt the pool for all threads in the same process.
                        System.err.println("concurrent modification uncaught, causing cache corruption");
                        e.printStackTrace();
                        fail();
                    } catch (ConcurrentModificationException e) {
                    }
                }
            }
        }).start();
        for (int i = 0; i < (TEST_LEN_MS / 100); i++) {
            try {
                Thread.sleep(100);
                map.clear();
            } catch (InterruptedException e) {
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("concurrent modification uncaught, causing indexing failure");
            } catch (ClassCastException e) {
                System.err.println("concurrent modification uncaught, causing cache corruption");
                fail();
            } catch (ConcurrentModificationException e) {
            }
        }
        done.set(true);
    }

    /**
     * Check to make sure the same operations behave as expected in a single thread.
     */
    @Test
    public void testNonConcurrentAccesses() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();

        for (int i = 0; i < 100000; i++) {
            try {
                map.put(String.format(Locale.US, "key %d", i++), "B_DONT_DO_THAT");
                if (i % 500 == 0) {
                    map.clear();
                }
            } catch (ConcurrentModificationException e) {
                System.err.println("Concurrent modification caught on single thread");
                e.printStackTrace();
                fail();
            }
        }
    }

    /**
     * Even though the Javadoc of {@link SimpleArrayMap#put(Object, Object)} says that the key
     * must not be null, the actual implementation allows it, and therefore we must ensure
     * that any future implementations of the class will still honor that contract.
     */
    @Test
    public void nullKeyCompatibility_canPutNullKeyAndNonNullValue() {
        SimpleArrayMap<String, Integer> map = new SimpleArrayMap<>();
        assertFalse(map.containsKey(null));
        map.put(null, 42);
        assertTrue(map.containsKey(null));
    }

    @Test
    public void nullKeyCompatibility_replacesValuesWithNullKey() {
        final Integer firstValue = 42;
        final Integer secondValue = 43;
        SimpleArrayMap<String, Integer> map = new SimpleArrayMap<>();
        assertFalse(map.containsKey(null));
        map.put(null, firstValue);
        assertTrue(map.containsKey(null));

        assertEquals(firstValue, map.get(null));
        assertEquals(firstValue, map.put(null, secondValue));

        assertEquals(secondValue, map.get(null));
        assertEquals(secondValue, map.remove(null));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void nullKeyCompatibility_putThenRemoveNullKeyAndValue() {
        SimpleArrayMap<String, Integer> map = new SimpleArrayMap<>();
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertNull(map.get(null));
        map.remove(null);
        assertFalse(map.containsKey(null));
    }

    @Test
    public void nullKeyCompatibility_removeNonNullValueWithNullKey() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put(null, null);
        assertNull(map.put(null, "42"));
        assertEquals("42", map.get(null));
        map.remove(null);
    }

    @Test
    public void nullKeyCompatibility_testReplaceMethodsWithNullKey() {
        SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
        map.put(null, null);
        assertNull(null, map.replace(null, "42"));
        assertFalse(map.replace(null, null, null));
        assertTrue(map.replace(null, "42", null));
        assertFalse(map.replace(null, "42", null));
        assertTrue(map.replace(null, null, null));
        assertTrue(map.containsKey(null));
        assertNull(map.get(null));
    }

    /**
     * Regression test against NPE in changes in the backing array growth implementation. Various
     * initial capacities are used, and for each capacity we always put in more elements than the
     * initial capacity can hold to exercise the code paths where the capacity is increased and the
     * backing arrays are expanded.
     */
    @Test
    public void backingArrayGrowth() {
        for (int initCapacity = 0; initCapacity <= 16; initCapacity++) {
            for (int entries = 1; entries < 32; entries++) {
                SimpleArrayMap<String, String> map = new SimpleArrayMap<>(initCapacity);
                for (int index = 0; index < entries; index++) {
                    map.put("key " + index, "value " + index);
                }
                for (int index = 0; index < entries; index++) {
                    assertEquals((Object) ("value " + index), map.get("key " + index));
                }
            }
        }
    }
}
