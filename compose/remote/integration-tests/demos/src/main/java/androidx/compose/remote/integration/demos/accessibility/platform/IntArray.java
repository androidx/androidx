/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.demos.accessibility.platform;

import android.annotation.SuppressLint;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Copy of Platform IntArray to allow testing Platform A11y classes in local tests.
 */
@SuppressLint({"PrimitiveInCollection", "deprecation", "UnknownNullness"})
public class IntArray {
    /**
     * The list of integers stored in this array.
     */
    public List<Integer> values = new ArrayList<>();

    /**
     * Adds the specified integer to the end of this array.
     *
     * @param child the integer value to be added
     */
    public void add(int child) {
        values.add(child);
    }

    /**
     * Removes all elements from this array.
     */
    public void clear() {
        values.clear();
    }

    /**
     * Returns the number of elements in this array.
     *
     * @return the number of elements in this array
     */
    public int size() {
        return values.size();
    }

    /**
     * Returns the element at the specified position in this array.
     *
     * @param i index of the element to return
     * @return the element at the specified position in this array
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public int get(int i) {
        return values.get(i);
    }

    @NonNull
    @Override
    public String toString() {
        return values.toString();
    }
}
