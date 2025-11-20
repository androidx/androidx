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
package androidx.work

import java.lang.reflect.Array

/**
 * An [InputMerger] that attempts to merge the inputs, creating arrays when necessary. For each
 * input, we look at each key:
 * * If this is the first time we encountered the key:
 *     * If it's an array, put it in the output
 *     * If it's a primitive, turn it into a size 1 array and put it in the output
 * * Else (we have encountered the key before):
 *     * If the value type matches the old value type:
 *         * If they are arrays, concatenate them
 *         * If they are primitives, turn them into a size 2 array
 *     * Else if one is an array and the other is a primitive of that type:
 *             * Make a longer array and concatenate them
 *     * Else throw an [IllegalArgumentException] because the types don't match.
 *
 * If a value by a key is `null`, it is considered to have type `String`, because it is the only
 * nullable typed allowed in [Data].
 */
public class ArrayCreatingInputMerger : InputMerger() {
    @Suppress("DocumentExceptions")
    override fun merge(inputs: List<Data>): Data {
        val output = Data.Builder()
        // values are always arrays
        val mergedValues: MutableMap<String, Any> = HashMap()
        for (input in inputs) {
            for ((key, value) in input.keyValueMap) {
                val valueClass: Class<*> = value?.javaClass ?: String::class.java
                val existingValue = mergedValues[key]
                mergedValues[key] =
                    if (existingValue == null) {
                        // First time encountering this key.
                        if (valueClass.isArray) {
                            // Arrays carry over as-is.
                            // if valueClass.isArray is true then value isn't null
                            value as Any
                        } else {
                            // Primitives get turned into size 1 arrays.
                            createArrayFor(value, valueClass)
                        }
                    } else {
                        // We've encountered this key before.
                        val existingValueClass: Class<*> = existingValue.javaClass
                        when {
                            existingValueClass == valueClass -> {
                                // The classes match; we can merge.
                                // both classes are arrays in this case => value isn't null
                                concatenateArrays(existingValue, value as Any)
                            }
                            existingValueClass.componentType == valueClass -> {
                                // We have an existing array of the same type.
                                concatenateArrayAndNonArray(existingValue, value, valueClass)
                            }
                            else -> throw IllegalArgumentException()
                        }
                    }
            }
        }
        output.putAll(mergedValues)
        return output.build()
    }

    private fun concatenateArrays(array1: Any, array2: Any): Any {
        val length1 = Array.getLength(array1)
        val length2 = Array.getLength(array2)
        val newArray = Array.newInstance(array1.javaClass.componentType!!, length1 + length2)
        System.arraycopy(array1, 0, newArray, 0, length1)
        System.arraycopy(array2, 0, newArray, length1, length2)
        return newArray
    }

    private fun concatenateArrayAndNonArray(array: Any, obj: Any?, valueClass: Class<*>): Any {
        val arrayLength = Array.getLength(array)
        val newArray = Array.newInstance(valueClass, arrayLength + 1)
        System.arraycopy(array, 0, newArray, 0, arrayLength)
        Array.set(newArray, arrayLength, obj)
        return newArray
    }

    private fun createArrayFor(obj: Any?, valueClass: Class<*>): Any {
        val newArray = Array.newInstance(valueClass, 1)
        Array.set(newArray, 0, obj)
        return newArray
    }
}
