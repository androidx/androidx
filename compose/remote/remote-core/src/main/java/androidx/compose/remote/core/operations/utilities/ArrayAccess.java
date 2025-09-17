/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.core.operations.utilities;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.Nullable;

/**
 * Support a standardized interface to commands that contain arrays All commands that implement
 * array access will be collected in a map in the state TODO: refactor to DataAccess,
 * FloatArrayAccess, ListAccess, MapAccess
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ArrayAccess {
    /**
     * Get a value as a float for an index
     *
     * @param index position in the collection
     * @return float value
     */
    float getFloatValue(int index);

    /**
     * If the objects have id's return the id
     *
     * @param index index of the object
     * @return id or -1 if no id is available
     */
    default int getId(int index) {
        return -1;
    }

    /**
     * Get the backing array of float if available for float arrays
     *
     * @return array of floats
     */
    float @Nullable [] getFloats();

    /**
     * Get the length of the collection
     *
     * @return length of the collection
     */
    int getLength();

    /**
     * Get the value as an integer if available
     *
     * @param index the position in the collection
     * @return it value as and integer
     */
    default int getIntValue(int index) {
        return (int) getFloatValue(index);
    }
}
