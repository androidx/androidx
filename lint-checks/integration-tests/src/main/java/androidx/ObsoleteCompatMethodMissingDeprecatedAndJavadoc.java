/*
 * Copyright 2024 The Android Open Source Project
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

package androidx;

import android.annotation.SuppressLint;

import androidx.annotation.ReplaceWith;

@SuppressLint({"UnknownNullness", "DeprecationMismatch"})
public class ObsoleteCompatMethodMissingDeprecatedAndJavadoc {
    private ObsoleteCompatMethodMissingDeprecatedAndJavadoc() {
        // This class is non-instantiable.
    }

    /**
     * Return the object's hash code.
     *
     * @param obj the object
     * @return the hash code
     */
    @ReplaceWith(expression = "Object.hashCode()")
    public static long hashCode(Object obj) {
        return obj.hashCode();
    }

    // This method does not have Javadoc yet.
    @ReplaceWith(expression = "Object.hashCode()")
    public static long hashCodeNoDoc(Object obj) {
        return obj.hashCode();
    }
}
