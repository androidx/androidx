/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf

import androidx.annotation.RestrictTo

/**
 * Used for converting an object of type F to one of type T.
 *
 * @param <F> original type of the object
 * @param <T> type to be converted to
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Converter<F, T> {
    /** Converts object of type F to type T */
    public fun convert(from: F, vararg args: Any): T
}
