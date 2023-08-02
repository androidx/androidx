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

package androidx.room

/**
 * Repeatable annotation declaring the renamed tables in the new version of an auto migration.
 *
 * @see [AutoMigration]
 *
 */
@JvmRepeatable(RenameTable.Entries::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class RenameTable(
    /**
     * Name of the table in the [AutoMigration.from] version of the database.
     *
     * @return Name of the table to rename from.
     */
    val fromTableName: String,

    /**
     * Name of the table in the [AutoMigration.to] version of the database.
     *
     * @return Name of the table to rename to.
     */
    val toTableName: String,
) {
    /**
     * Container annotation for the repeatable annotation [RenameTable].
     */
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.BINARY)
    public annotation class Entries(
        vararg val value: RenameTable
    )
}
