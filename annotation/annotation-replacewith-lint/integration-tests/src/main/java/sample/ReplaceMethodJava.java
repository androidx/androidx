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

package sample;

import androidx.annotation.ReplaceWith;

public class ReplaceMethodJava {
    /**
     * Calls the method on the object.
     *
     * @param obj The object on which to call the method.
     * @deprecated Use {@link Object#toString()} directly.
     */
    @Deprecated
    @ReplaceWith(expression = "obj.toString()")
    public static void toString(Object obj) {
        // Stub.
    }

    private ReplaceMethodJava() {
        // This class is not instantiable.
    }
}
