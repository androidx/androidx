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

package androidx.room.util

import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.FieldBundle
import androidx.room.migration.bundle.PrimaryKeyBundle
import androidx.room.migration.bundle.SchemaBundle
import androidx.room.processor.ProcessorErrors
import com.google.common.truth.Truth.assertThat
import com.ibm.icu.impl.Assert.fail
import org.junit.Test

class SchemaDifferTest {

    @Test
    fun testColumnAddedWithColumnInfoDefaultValue() {
        val schemaDiffResult = SchemaDiffer(
            fromSchemaBundle = from.database,
            toSchemaBundle = toColumnAddedWithColumnInfoDefaultValue.database
        ).diffSchemas()
        assertThat(schemaDiffResult.added[0].fieldBundle.columnName).isEqualTo("artistId")
    }

    @Test
    fun testColumnAddedWithNoDefaultValue() {
        try {
            SchemaDiffer(
                fromSchemaBundle = from.database,
                toSchemaBundle = toColumnAddedWithNoDefaultValue.database
            ).diffSchemas()
            fail("DiffException should have been thrown.")
        } catch (ex: DiffException) {
            assertThat(ex.errorMessage).isEqualTo(
                ProcessorErrors.newNotNullColumnMustHaveDefaultValue("artistId")
            )
        }
    }

    @Test
    fun testColumnRenamed() {
        try {
            SchemaDiffer(
                fromSchemaBundle = from.database,
                toSchemaBundle = toColumnRenamed.database
            ).diffSchemas()
            fail("DiffException should have been thrown.")
        } catch (ex: DiffException) {
            assertThat(ex.errorMessage).isEqualTo(
                ProcessorErrors.removedOrRenamedColumnFound("length")
            )
        }
    }

    @Test
    fun testColumnAffinityChanged() {
        try {
            SchemaDiffer(
                fromSchemaBundle = from.database,
                toSchemaBundle = toColumnAffinityChanged.database
            ).diffSchemas()
            fail("DiffException should have been thrown.")
        } catch (ex: DiffException) {
            assertThat(ex.errorMessage).isEqualTo(
                ProcessorErrors.columnWithChangedSchemaFound("length")
            )
        }
    }

    @Test
    fun testColumnRemoved() {
        try {
            SchemaDiffer(
                fromSchemaBundle = from.database,
                toSchemaBundle = toColumnRemoved.database
            ).diffSchemas()
            fail("DiffException should have been thrown.")
        } catch (ex: DiffException) {
            assertThat(ex.errorMessage).isEqualTo(
                ProcessorErrors.removedOrRenamedColumnFound("length")
            )
        }
    }

    val from = SchemaBundle(
        1,
        DatabaseBundle(
            1,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    /** Valid "to" Schemas */
    val toColumnAddedWithColumnInfoDefaultValue = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            listOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, `artistId` " +
                        "INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "artistId",
                            "artistId",
                            "INTEGER",
                            true,
                            "0"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    emptyList(),
                    emptyList()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    /** Invalid "to" Schemas (These are expected to throw an error.) */

    /**
     * The length column is removed from the first version. No other changes made.
     *
     */
    val toColumnRemoved = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            listOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    emptyList(),
                    emptyList()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    /**
     * If the user declared the default value in the SQL statement and not used a @ColumnInfo,
     * Room will put null for that default value in the exported schema. In this case we
     * can't migrate.
     */
    val toColumnAddedWithNoDefaultValue = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            listOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, `artistId` " +
                        "INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "artistId",
                            "artistId",
                            "INTEGER",
                            true,
                            null
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    emptyList(),
                    emptyList()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    /**
     * Renaming the length column to duration.
     */
    // TODO: We currently do not support column renames as we can't detect rename or deletion
    //  yet.
    val toColumnRenamed = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT " +
                        "NULL, `title` TEXT NOT NULL, `duration` INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "duration",
                            "duration",
                            "INTEGER",
                            true,
                            "0"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    /**
     * The affinity of a length column is changed from Integer to Text. No columns are
     * added/removed.
     */
    // TODO: We currently do not support column affinity changes.
    val toColumnAffinityChanged = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` TEXT NOT NULL DEFAULT length, " +
                        "PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "TEXT",
                            true,
                            "length"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )
}