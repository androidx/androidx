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
 * Skips database verification for the annotated element.
 * <p>
 * If it is a class annotated with {@link Database}, none of the queries for the database will
 * be verified at compile time.
 * <p>
 * If it is a class annotated with {@link Dao}, none of the queries in the Dao class will
 * be verified at compile time.
 * <p>
 * If it is a method in a Dao class, just the method's sql verification will be skipped.
 * <p>
 * You should use this as the last resort if Room cannot properly understand your query and you are
 * 100% sure it works. Removing validation may limit the functionality of Room since it won't be
 * able to understand the query response.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface SkipQueryVerification {
}
