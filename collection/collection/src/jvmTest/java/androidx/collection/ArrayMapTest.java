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

package androidx.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

@RunWith(JUnit4.class)
public class ArrayMapTest {
    ArrayMap<String, String> mMap = new ArrayMap<>();

    /**
     * Attempt to generate a ConcurrentModificationException in ArrayMap.
     * <p>
     * ArrayMap is explicitly documented to be non-thread-safe, yet it's easy to accidentally screw
     * this up; ArrayMap should (in the spirit of the core Java collection types) make an effort to
     * catch this and throw ConcurrentModificationException instead of crashing somewhere in its
     * internals.
     */
    @Test
    public void testConcurrentModificationException() throws Exception {
        final int testLenMs = 5000;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (mMap != null) {
                    try {
                        mMap.put(String.format("key %d", i++), "B_DONT_DO_THAT");
                    } catch (ArrayIndexOutOfBoundsException e) {
                        fail(e.getMessage());
                    } catch (ClassCastException e) {
                        fail(e.getMessage());
                    } catch (ConcurrentModificationException e) {
                        System.out.println("[successfully caught CME at put #" + i
                                + " size=" + (mMap == null ? "??" : String.valueOf(mMap.size()))
                                + "]");
                    }
                }
            }
        }).start();
        for (int i = 0; i < (testLenMs / 100); i++) {
            try {
                Thread.sleep(100);
                mMap.clear();
            } catch (InterruptedException e) {
            } catch (ArrayIndexOutOfBoundsException e) {
                fail(e.getMessage());
            } catch (ClassCastException e) {
                fail(e.getMessage());
            } catch (ConcurrentModificationException e) {
                System.out.println(
                        "[successfully caught CME at clear #"
                                + i + " size=" + mMap.size() + "]");
            }
        }
        mMap = null; // will stop other thread
        System.out.println();
    }

    /**
     * Check to make sure the same operations behave as expected in a single thread.
     */
    @Test
    public void testNonConcurrentAccesses() throws Exception {
        for (int i = 0; i < 100000; i++) {
            try {
                mMap.put(String.format("key %d", i++), "B_DONT_DO_THAT");
                if (i % 200 == 0) {
                    System.out.print(".");
                }
                if (i % 500 == 0) {
                    mMap.clear();
                    System.out.print("X");
                }
            } catch (ConcurrentModificationException e) {
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testIsSubclassOfSimpleArrayMap() {
        Object map = new ArrayMap<String, Integer>();
        assertTrue(map instanceof SimpleArrayMap);
    }

    /**
     * Regression test for ensure capacity: b/224971154
     */
    @Test
    public void putAll() {
        ArrayMap<String, String> map = new ArrayMap<>();
        Map<String, String> otherMap = new HashMap<>();
        otherMap.put("abc", "def");
        map.putAll(otherMap);
        assertEquals(map.size(), 1);
        assertEquals(map.keyAt(0), "abc");
        assertEquals(map.valueAt(0), "def");
    }
}
