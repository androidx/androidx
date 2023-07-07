/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.room.testing.kotlintestapp.migration

import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    version = SimpleAutoMigrationDb.LATEST_VERSION,
    entities = [
        SimpleAutoMigrationDb.Entity1::class,
        SimpleAutoMigrationDb.Entity2::class],
    autoMigrations = [AutoMigration(
        from = 1,
        to = 2,
        spec = SimpleAutoMigrationDb.SimpleAutoMigration1::class
    )],
    exportSchema = true
)
abstract class SimpleAutoMigrationDb : RoomDatabase() {
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

    @Dao
    internal interface AutoMigrationDao {
        @Query("SELECT * from Entity1 ORDER BY id ASC")
        fun getAllEntity1s(): List<Entity1>
    }

    internal class SimpleAutoMigration1 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            // Do something
        }
    }

    companion object {
        const val LATEST_VERSION = 1
    }
}
