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
package androidx.room.integration.kotlintestapp.migration

import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.DatabaseView
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts3
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    version = AutoMigrationDb.LATEST_VERSION,
    entities = [
        AutoMigrationDb.Entity1::class,
        AutoMigrationDb.Entity2::class,
        AutoMigrationDb.Entity3::class,
        AutoMigrationDb.Entity4::class,
        AutoMigrationDb.Entity5::class,
        AutoMigrationDb.Entity6::class,
        AutoMigrationDb.Entity7::class,
        AutoMigrationDb.Entity8::class,
        AutoMigrationDb.Entity9::class,
        AutoMigrationDb.Entity10::class,
        AutoMigrationDb.Entity11::class,
        AutoMigrationDb.Entity12::class,
        AutoMigrationDb.Entity13_V2::class,
        AutoMigrationDb.Entity14::class,
        AutoMigrationDb.Entity15::class,
        AutoMigrationDb.Entity16::class,
        AutoMigrationDb.Entity17::class,
        AutoMigrationDb.Entity19_V2::class,
        AutoMigrationDb.Entity20_V2::class,
        AutoMigrationDb.Entity21::class,
        AutoMigrationDb.Entity22::class,
        AutoMigrationDb.Entity23::class,
        AutoMigrationDb.Entity24::class,
        AutoMigrationDb.Entity25::class,
        AutoMigrationDb.Entity26::class,
        AutoMigrationDb.Entity27::class],
    autoMigrations = [AutoMigration(
        from = 1,
        to = 2,
        spec = AutoMigrationDb.SimpleAutoMigration1::class
    ), AutoMigration(from = 2, to = 3)],
    views = [AutoMigrationDb.Entity25Detail::class],
    exportSchema = true
)
abstract class AutoMigrationDb : RoomDatabase() {
    internal abstract fun dao(): AutoMigrationDao

    /**
     * No change between versions.
     */
    @Entity
    data class Entity1(
        @PrimaryKey
        var id: Int,
        var name: String,
        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int,
    ) {

        companion object {
            const val TABLE_NAME = "Entity1"
        }
    }

    /**
     * A new simple column added to Entity 2 with a default value.
     */
    @Entity
    data class Entity2(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int,

        @ColumnInfo(defaultValue = "2")
        var addedInV2: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity2"
        }
    }

    /**
     * Added Entity 3 to the schema. No foreign keys, views, indices added.
     */
    @Entity
    data class Entity3(
        @PrimaryKey
        var id: Int,
        var name: String
    ) {
        companion object {
            const val TABLE_NAME = "Entity3"
        }
    }

    /**
     * Changing the default value of ‘addedInV1’ in Entity 4.
     */
    @Entity
    data class Entity4(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "2")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity4"
        }
    }

    /**
     * Changing the affinity of ‘addedInV1’ in Entity 5.
     */
    @Entity
    internal class Entity5(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: String
    ) {
        companion object {
            const val TABLE_NAME = "Entity5"
        }
    }

    /**
     * Changing the nullability of ‘addedInV1’ in Entity 6.
     */
    @Entity
    internal class Entity6(
        @PrimaryKey
        var id: Int,
        var name: String,

        // @ColumnInfo(defaultValue = "1") - now nullable
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity6"
        }
    }

    /**
     * No change between versions.
     */
    @Entity
    data class Entity7(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity7"
        }
    }

    /**
     * Change the primary key of Entity 8.
     */
    @Entity
    data class Entity8(
        var id: Int,

        @PrimaryKey
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity8"
        }
    }

    /**
     * Add a foreign key to Entity 9.
     */
    @Entity(
        foreignKeys = [ForeignKey(
            entity = Entity27::class,
            parentColumns = ["id27"],
            childColumns = ["id"]
        )]
    )
    data class Entity9(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity9"
        }
    }

    /**
     * Change the foreign key added in Entity 10 to ‘addedInV1’. Add index for addedInV1 on
     * Entity 10. The reference table of the foreign key has been renamed from Entity13 to
     * Entity13_V2.
     */
    @Entity(
        foreignKeys = [ForeignKey(
            entity = Entity13_V2::class,
            parentColumns = ["addedInV1"],
            childColumns = ["addedInV1"],
            deferred = true
        )], indices = [Index(value = ["addedInV1"], unique = true)]
    )
    data class Entity10(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity10"
        }
    }

    /**
     * Remove the foreign key in Entity 11.
     */
    @Entity
    data class Entity11(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity11"
        }
    }

    /**
     * Add an index ‘name’ to Entity 12.
     */
    @Entity(indices = [Index(value = ["name"], unique = true)])
    data class Entity12(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity12"
        }
    }

    /**
     * Rename to Entity13_V2, it is a table referenced by the foreign key in Entity10. Change the
     * index added in Entity 13 to ‘addedInV1’.
     */
    @Entity(indices = [Index(value = ["addedInV1"], unique = true)])
    data class Entity13_V2(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity13"
        }
    }

    /**
     * Remove the index ‘name’ added in Entity 14.
     */
    @Entity
    data class Entity14(
        @PrimaryKey
        var id: Int,
        var name: String
    ) {
        companion object {
            const val TABLE_NAME = "Entity14"
        }
    }

    /**
     * Deleting the column ‘addedInV1’ from Entity 15.
     */
    @Entity
    data class Entity15(
        @PrimaryKey
        var id: Int,
        var name: String
    ) {
        companion object {
            const val TABLE_NAME = "Entity15"
        }
    }

    /**
     * Renaming the column ‘addedInV1’ from Entity 16 to ‘renamedInV2’.
     */
    @Entity
    data class Entity16(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var renamedInV2: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity16"
        }
    }

    /**
     * Renaming the column ‘addedInV1’ from Entity 17 to ‘renamedInV2’. Changing the affinity of
     * this column.
     */
    @Entity
    data class Entity17(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var renamedInV2: String
    ) {
        companion object {
            const val TABLE_NAME = "Entity17"
        }
    }

    /**
     * Deleted Entity 18.
     *
     * Rename Entity19 to ‘Entity19_V2’.
     */
    @Entity
    data class Entity19_V2(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity19_V2"
        }
    }

    /**
     * Rename Entity20 to ‘Entity20_V2’. Rename the column ‘addedInV1’ to ‘renamedInV2’. Change
     * the primary key of this table to ‘name’. Change the affinity of the column ‘renamedInV2’.
     * Add new column ‘addedInV2’.
     */
    @Entity
    data class Entity20_V2(
        var id: Int,

        @PrimaryKey
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var renamedInV2: String,

        @ColumnInfo(defaultValue = "2")
        var addedInV2: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity20_V2"
        }
    }

    /**
     * The content table of this FTS table has been renamed from Entity13 to Entity13_V2.
     */
    @Entity
    @Fts4(contentEntity = Entity13_V2::class)
    data class Entity21(
        @PrimaryKey
        var rowid: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity21"
        }
    }

    /**
     * Change the options of the table from FTS3 to FTS4.
     */
    @Entity
    @Fts4(matchInfo = FtsOptions.MatchInfo.FTS4)
    data class Entity22(
        @PrimaryKey
        var rowid: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity22"
        }
    }

    @Entity
    @Fts3
    data class Entity23(
        @PrimaryKey
        var rowid: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int,

        @ColumnInfo(defaultValue = "2")
        var addedInV2: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity23"
        }
    }

    @Entity
    @Fts3
    internal class Entity24(
        @PrimaryKey
        var rowid: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity24"
        }
    }

    @DatabaseView(
        "SELECT Entity25.id, Entity25.name, Entity25.entity1Id, Entity1.name AS userNameAndId " +
            "FROM Entity25 INNER JOIN Entity1 ON Entity25.entity1Id = Entity1.id "
    )
    internal class Entity25Detail {
        var id = 0
        var name: String? = null
        var entity1Id: String? = null
    }

    /**
     * Change the view between versions to use Entity1 instead of Entity7.
     */
    @Entity
    data class Entity25(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var entity1Id: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity25"
        }
    }

    /**
     * Added a new table that has an index.
     */
    @Entity(indices = [Index(value = ["addedInV2"], unique = true)])
    data class Entity26(
        @PrimaryKey
        var id: Int,
        var name: String,

        @ColumnInfo(defaultValue = "1")
        var addedInV2: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity26"
        }
    }

    /**
     * No change between versions.
     */
    @Entity
    data class Entity27(
        @PrimaryKey
        var id27: Int,

        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int
    ) {
        companion object {
            const val TABLE_NAME = "Entity27"
        }
    }

    @Dao
    internal interface AutoMigrationDao {
        @Query("SELECT * from Entity1 ORDER BY id ASC")
        fun getAllEntity1s(): List<Entity1>
    }

    @DeleteTable(tableName = "Entity18")
    @RenameTable(fromTableName = "Entity19", toTableName = "Entity19_V2")
    @RenameTable(fromTableName = "Entity20", toTableName = "Entity20_V2")
    @RenameTable(fromTableName = "Entity13", toTableName = "Entity13_V2")
    @RenameColumn(tableName = "Entity16", fromColumnName = "index", toColumnName = "renamedInV2")
    @RenameColumn(
        tableName = "Entity17",
        fromColumnName = "addedInV1",
        toColumnName = "renamedInV2"
    )
    @RenameColumn(
        tableName = "Entity20",
        fromColumnName = "addedInV1",
        toColumnName = "renamedInV2"
    )
    @DeleteColumn(tableName = "Entity15", columnName = "addedInV1")
    @RenameColumn(tableName = "Entity25", fromColumnName = "entity7Id", toColumnName = "entity1Id")
    internal class SimpleAutoMigration1 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            // Do something
        }
    }

    companion object {
        const val LATEST_VERSION = 3
    }
}
