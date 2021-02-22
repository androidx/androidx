/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.annotation.RestrictTo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Automatic migration strategy for Room databases.
 *
 * @hide
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public @interface AutoMigration {
    /**
     * Version of the original database schema to migrate from.
     *
     * @return Version number of the original database schema.
     */
    int from();

    /**
     * Version of the new database schema to migrate to.
     *
     * @return Version number of the new database schema.
     */
    int to();
}
