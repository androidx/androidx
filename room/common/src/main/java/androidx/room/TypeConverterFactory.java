/*
 * Copyright (C) 2017 The Android Open Source Project
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies type converter factory that Room can use to instantiate a class containing
 * {@link TypeConverter} annotated methods.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface TypeConverterFactory {
    /**
     * The type converter factory class.
     *
     * @return The class of a {@code TypeConverterFactory } that should be used to instantiate a
     * class that contains the converter methods.
     */
    Class<?> value();
}
