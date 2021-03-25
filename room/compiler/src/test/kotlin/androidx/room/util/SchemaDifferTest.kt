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
import androidx.room.migration.bundle.ForeignKeyBundle
import androidx.room.migration.bundle.IndexBundle
import androidx.room.migration.bundle.PrimaryKeyBundle
import androidx.room.migration.bundle.SchemaBundle
import androidx.room.processor.ProcessorErrors
import com.google.common.truth.Truth.assertThat
import com.ibm.icu.impl.Assert.fail
import org.junit.Test

class SchemaDifferTest {

    @Test
    fun testPrimaryKeyChanged() {
        try {
            SchemaDiffer(
                fromSchemaBundle = from.database,
                toSchemaBundle = toChangeInPrimaryKey.database
            ).diffSchemas()
            fail("DiffException should have been thrown.")
        } catch (ex: DiffException) {
            assertThat(ex.errorMessage).isEqualTo(
                ProcessorErrors.tableWithComplexChangedSchemaFound("Song")
            )
        }
    }

    @Test
    fun testForeignKeyFieldChanged() {
        try {
            SchemaDiffer(
                fromSchemaBundle = from.database,
                toSchemaBundle = toForeignKeyAdded.database
            ).diffSchemas()
            fail("DiffException should have been thrown.")
        } catch (ex: DiffException) {
            assertThat(ex.errorMessage).isEqualTo(
                ProcessorErrors.tableWithComplexChangedSchemaFound("Song")
            )
        }
    }

    @Test
    fun testComplexChangeInvolvingIndex() {
        try {
            SchemaDiffer(
                fromSchemaBundle = from.database,
                toSchemaBundle = toIndexAdded.database
            ).diffSchemas()
            fail("DiffException should have been thrown.")
        } catch (ex: DiffException) {
            assertThat(ex.errorMessage).isEqualTo(
                ProcessorErrors.tableWithComplexChangedSchemaFound("Song")
            )
        }
    }

    @Test
    fun testColumnAddedWithColumnInfoDefaultValue() {
        val schemaDiffResult = SchemaDiffer(
            fromSchemaBundle = from.database,
            toSchemaBundle = toColumnAddedWithColumnInfoDefaultValue.database
        ).diffSchemas()
        assertThat(schemaDiffResult.addedColumns[0].fieldBundle.columnName).isEqualTo("artistId")
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
    fun testTableAddedWithColumnInfoDefaultValue() {
        val schemaDiffResult = SchemaDiffer(
            fromSchemaBundle = from.database,
            toSchemaBundle = toTableAddedWithColumnInfoDefaultValue.database
        ).diffSchemas()
        assertThat(schemaDiffResult.addedTables[0].entityBundle.tableName).isEqualTo("Artist")
        assertThat(schemaDiffResult.addedTables[1].entityBundle.tableName).isEqualTo("Album")
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
    fun testColumnFieldBundleChanged() {
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
    private val toColumnRemoved = SchemaBundle(
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
    private val toColumnAddedWithNoDefaultValue = SchemaBundle(
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
    private val toColumnRenamed = SchemaBundle(
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

    private val toTableAddedWithColumnInfoDefaultValue = SchemaBundle(
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
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`artistId` INTEGER NOT NULL, `name` " +
                        "TEXT NOT NULL, PRIMARY KEY(`artistId`))",
                    listOf(
                        FieldBundle(
                            "artistId",
                            "artistId",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(true, listOf("artistId")),
                    listOf(),
                    listOf()
                ),
                EntityBundle(
                    "Album",
                    "CREATE TABLE IF NOT EXISTS `Album` (`albumId` INTEGER NOT NULL, `name` TEXT " +
                        "NOT NULL, PRIMARY KEY(`albumId`))",
                    listOf(
                        FieldBundle(
                            "albumId",
                            "albumId",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(true, listOf("albumId")),
                    listOf(),
                    listOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    private val toForeignKeyAdded = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            listOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, `artistId` " +
                        "INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`), FOREIGN KEY(`title`) " +
                        "REFERENCES `Song`(`artistId`) ON UPDATE NO ACTION ON DELETE NO " +
                        "ACTION DEFERRABLE INITIALLY DEFERRED))",
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
                    listOf(
                        ForeignKeyBundle(
                            "Song",
                            "onDelete",
                            "onUpdate",
                            listOf("title"),
                            listOf("artistId")
                        )
                    )
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    val toIndexAdded = SchemaBundle(
        2,
        DatabaseBundle(
            2,
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
                    listOf(
                        IndexBundle(
                            "index1",
                            true,
                            listOf("title"),
                            "CREATE UNIQUE INDEX IF NOT EXISTS `index1` ON `Song`" +
                                "(`title`)"
                        )
                    ),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    val toChangeInPrimaryKey = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`title`))",
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
                        mutableListOf("title")
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