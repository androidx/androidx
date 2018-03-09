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

import org.junit.Test;

public class LongSparseArrayTest {
    @Test
    public void isEmpty() throws Exception {
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

}
