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

import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ConcurrentModificationException;
import java.util.Locale;

/**
 * Unit tests for SimpleArrayMap
 */
public class SimpleArrayMapTest {
    SimpleArrayMap<String, String> map = new SimpleArrayMap<>();
    private boolean mDone;

    /**
     * Attempt to generate a ConcurrentModificationException in ArrayMap.
     */
    @Test
    public void testConcurrentModificationException() throws Exception {
        final int TEST_LEN_MS = 5000;
        System.out.println("Starting SimpleArrayMap concurrency test");
        mDone = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (!mDone) {
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
        mDone = true;
    }

    /**
     * Check to make sure the same operations behave as expected in a single thread.
     */
    @Test
    public void testNonConcurrentAccesses() throws Exception {
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
}
