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
 * Repeatable annotation declaring the deleted tables in the [AutoMigration.to] version of
 * an auto migration.
 *
 * @see AutoMigration
 */
@JvmRepeatable(DeleteTable.Entries::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class DeleteTable(
    /**
     * Name of the table in the [AutoMigration.from] version of the database to be deleted.
     *
     * @return Name of the table.
     */
    val tableName: String
) {
    /**
     * Container annotation for the repeatable annotation [DeleteTable].
     */
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.BINARY)
    public annotation class Entries(
        vararg val value: DeleteTable
    )
}
