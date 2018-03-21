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
 * Marks a method as a type converter. A class can have as many @TypeConverter methods as it needs.
 * <p>
 * Each converter method should receive 1 parameter and have non-void return type.
 *
 * <pre>
 * // example converter for java.util.Date
 * public static class Converters {
 *    {@literal @}TypeConverter
 *    public Date fromTimestamp(Long value) {
 *        return value == null ? null : new Date(value);
 *    }
 *
 *    {@literal @}TypeConverter
 *    public Long dateToTimestamp(Date date) {
 *        if (date == null) {
 *            return null;
 *        } else {
 *            return date.getTime();
 *        }
 *    }
 *}
 * </pre>
 * @see TypeConverters
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface TypeConverter {
}
