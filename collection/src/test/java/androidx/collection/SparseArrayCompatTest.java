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

public class SparseArrayCompatTest {
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
}
