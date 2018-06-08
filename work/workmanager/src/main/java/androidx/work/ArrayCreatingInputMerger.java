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

package androidx.work;

import android.support.annotation.NonNull;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link InputMerger} that attempts to merge the various inputs.  For each input, we look at
 * each key:
 * <p><ul>
 * <li>If this is the first time we encountered the key</li>
 *   <ul>
 *   <li>If it's an array, put it in the output</li>
 *   <li>If it's a primitive, turn it into a size 1 array and put it in the output</li>
 *   </ul>
 * <li>Else</li>
 *   <ul>
 *   <li>If the value type matches the old value type</li>
 *     <ul>
 *     <li>If they are arrays, concatenate them</li>
 *     <li>If they are primitives, turn them into a size 2 array</li>
 *     </ul>
 *   <li>Else if one is an array and the other is a primitive</li>
 *     <ul>
 *     <li>Make a longer array and concatenate them</li>
 *     </ul>
 *   <li>Else throw an {@link IllegalArgumentException}</li>
 *   </ul>
 * </ul>
 */

public final class ArrayCreatingInputMerger extends InputMerger {

    @Override
    public @NonNull Data merge(@NonNull List<Data> inputs) {
        Data.Builder output = new Data.Builder();
        Map<String, Object> mergedValues = new HashMap<>();

        for (Data input : inputs) {
            for (Map.Entry<String, Object> entry : input.getKeyValueMap().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                Class valueClass = value.getClass();
                Object mergedValue = null;

                if (!mergedValues.containsKey(key)) {
                    // First time encountering this key.
                    if (valueClass.isArray()) {
                        // Arrays carry over as-is.
                        mergedValue = value;
                    } else {
                        // Primitives get turned into size 1 arrays.
                        mergedValue = createArrayFor(value);
                    }
                } else {
                    // We've encountered this key before.
                    Object existingValue = mergedValues.get(key);
                    Class existingValueClass = existingValue.getClass();

                    if (existingValueClass.equals(valueClass)) {
                        // The classes match; we can merge.
                        if (existingValueClass.isArray()) {
                            mergedValue = concatenateArrays(existingValue, value);
                        } else {
                            mergedValue = concatenateNonArrays(existingValue, value);
                        }
                    } else if (existingValueClass.isArray()
                            && existingValueClass.getComponentType().equals(valueClass)) {
                        // We have an existing array of the same type.
                        mergedValue = concatenateArrayAndNonArray(existingValue, value);
                    } else if (valueClass.isArray()
                            && valueClass.getComponentType().equals(existingValueClass)) {
                        // We have an existing array of the same type.
                        mergedValue = concatenateArrayAndNonArray(value, existingValue);
                    } else {
                        throw new IllegalArgumentException();
                    }
                }

                mergedValues.put(key, mergedValue);
            }
        }

        output.putAll(mergedValues);
        return output.build();
    }

    private Object concatenateArrays(Object array1, Object array2) {
        int length1 = Array.getLength(array1);
        int length2 = Array.getLength(array2);
        Object newArray = Array.newInstance(array1.getClass().getComponentType(),
                length1 + length2);
        System.arraycopy(array1, 0, newArray, 0, length1);
        System.arraycopy(array2, 0, newArray, length1, length2);
        return newArray;
    }

    private Object concatenateNonArrays(Object obj1, Object obj2) {
        Object newArray = Array.newInstance(obj1.getClass(), 2);
        Array.set(newArray, 0, obj1);
        Array.set(newArray, 1, obj2);
        return newArray;
    }

    private Object concatenateArrayAndNonArray(Object array, Object obj) {
        int arrayLength = Array.getLength(array);
        Object newArray = Array.newInstance(obj.getClass(), arrayLength + 1);
        System.arraycopy(array, 0, newArray, 0, arrayLength);
        Array.set(newArray, arrayLength, obj);
        return newArray;
    }

    private Object createArrayFor(Object obj) {
        Object newArray = Array.newInstance(obj.getClass(), 1);
        Array.set(newArray, 0, obj);
        return newArray;
    }
}
