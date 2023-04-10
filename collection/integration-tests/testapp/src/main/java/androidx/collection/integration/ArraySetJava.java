/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.collection.integration;

import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Integration (actually build) test for source compatibility for usages of ArraySet.
 */
public class ArraySetJava {
    private ArraySetJava() {
    }

    @SuppressWarnings("unused")
    public static boolean sourceCompatibility() {
        //noinspection UnusedAssignment
        ArraySet<Integer> arraySet = new ArraySet<>();
        arraySet = new ArraySet<>(5);
        //noinspection UnusedAssignment
        arraySet = new ArraySet<>(arraySet);
        //noinspection UnusedAssignment
        arraySet = new ArraySet<>(new HashSet<>());
        arraySet = new ArraySet<>(new Integer[5]);

        arraySet.clear();
        arraySet.ensureCapacity(10);
        arraySet.addAll(new ArraySet<>());
        arraySet.removeAll(new ArraySet<>());

        for (Integer item : arraySet) {
            System.out.println(item);
        }

        //noinspection MismatchedQueryAndUpdateOfCollection
        ArraySet<Integer> other = new ArraySet<>();
        //noinspection MismatchedQueryAndUpdateOfCollection
        ArrayList<Integer> otherList = new ArrayList<>();

        //noinspection Since15,ConstantConditions,ExcessiveRangeCheck,NewObjectEquality
        return arraySet.remove(0) && arraySet.isEmpty() && arraySet.removeAll(other)
                && arraySet.removeAt(0) == 0 && arraySet.contains(0) && arraySet.size() == 0
                && arraySet.isEmpty() && arraySet.toArray() == arraySet.toArray(new Object[5])
                && arraySet == new Object()
                && arraySet.toArray() == arraySet.toArray(new Integer[5])
                && arraySet.toArray(value -> new Object[5]) == null
                && arraySet.containsAll(otherList);
    }
}
