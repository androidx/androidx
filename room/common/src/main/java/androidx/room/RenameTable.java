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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Repeatable annotation to be used by the user in specifying renamed tables between the old and
 * new versions of one database.
 *
 * @hide
 */
@Repeatable(RenameTable.Entries.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public @interface RenameTable {
    /**
     * Name of the table in the previous version of the database.
     *
     * @return Original name of the table.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    String originalTableName();

    /**
     * Name of the table in the new version of the database.
     *
     * @return New name of the table.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    String newTableName();

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    @interface Entries {
        RenameTable[] value();
    }
}
