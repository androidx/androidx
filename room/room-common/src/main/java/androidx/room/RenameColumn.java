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

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Repeatable annotation declaring the renamed columns in the {@link AutoMigration#to} version of
 * an auto migration.
 *
 * @see AutoMigration
 */
@Repeatable(RenameColumn.Entries.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface RenameColumn {
    /**
     * Name of the table in the {@link AutoMigration#from} version of the database the renamed
     * column is found in. The name in {@link AutoMigration#from} version is used in case the table
     * was renamed in the {@link AutoMigration#to} version.
     *
     * @return Name of the table
     */
    String tableName();

    /**
     * Name of the column in the {@link AutoMigration#from} version of the database.
     *
     * @return Name of the column.
     */
    String fromColumnName();

    /**
     * Name of the column in the {@link AutoMigration#to} version of the database.
     *
     * @return Name of the column.
     */
    String toColumnName();

    /**
     * Container annotation for the repeatable annotation {@link RenameColumn}.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    @interface Entries {
        RenameColumn[] value();
    }
}

