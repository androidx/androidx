/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room;

import androidx.annotation.NonNull;

/**
 * Implementations of {@code TypeConverterFactory} interface are responsible to instantiate
 * TypeConverters and must be final.
 */
public interface TypeConverterFactory {
    /**
     * Creates a new instance of the given {@code Class}.
     * <p>
     *
     * @param converterClass a {@code Class} whose instance is requested
     * @param <T> The type parameter for the TypeConverter.
     * @return a newly created TypeConverter
     */
    @NonNull
    <T> T create(@NonNull Class<T> converterClass);
}
