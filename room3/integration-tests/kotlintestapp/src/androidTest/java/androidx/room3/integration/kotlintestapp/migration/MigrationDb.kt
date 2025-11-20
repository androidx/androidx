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

package androidx.room3.integration.kotlintestapp.migration

import androidx.room3.AutoMigration
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Ignore
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomWarnings
import androidx.sqlite.SQLiteConnection

@Database(
    version = MigrationDb.LATEST_VERSION,
    entities =
        [
            MigrationDb.Entity1::class,
            MigrationDb.Entity2::class,
            MigrationDb.Entity4::class,
            MigrationDb.Entity5::class,
        ],
    autoMigrations = [AutoMigration(7, 8)],
)
abstract class MigrationDb : RoomDatabase() {

    internal abstract fun dao(): MigrationDao

    @Entity(indices = [Index(value = ["name"], unique = true)])
    data class Entity1(@PrimaryKey var id: Int = 0, var name: String?) {

        companion object {
            const val TABLE_NAME = "Entity1"
        }
    }

    @Entity
    open class Entity2(@PrimaryKey var id: Int = 0, var addedInV3: String?, var name: String?) {
        companion object {
            const val TABLE_NAME = "Entity2"
        }
    }

    @Entity
    data class Entity3(
        @PrimaryKey var id: Int = 0,
        @get:Ignore var removedInV5: String?,
        var name: String?,
        var addedInV8: Int = 1,
    ) { // added in version 4, removed at 6
        companion object {
            const val TABLE_NAME = "Entity3"
        }
    }

    @SuppressWarnings(RoomWarnings.MISSING_INDEX_ON_FOREIGN_KEY_CHILD)
    @Entity(
        foreignKeys =
            [
                ForeignKey(
                    entity = Entity1::class,
                    parentColumns = arrayOf("name"),
                    childColumns = arrayOf("name"),
                    deferred = true,
                )
            ]
    )
    data class Entity4(@PrimaryKey var id: Int = 0, var name: String?) {
        companion object {
            const val TABLE_NAME = "Entity4"
        }
    }

    @Entity // added in v8
    data class Entity5(
        @PrimaryKey var id: Int = 0,
        @ColumnInfo(defaultValue = "'Unknown") var addedInV9: String? = null,
        @ColumnInfo(defaultValue = "(0)") var addedInV10: Int,
    ) {
        companion object {
            const val TABLE_NAME = "Entity5"
        }
    }

    @Dao
    internal interface MigrationDao {
        @Query("SELECT * from Entity1 ORDER BY id ASC") fun loadAllEntity1s(): List<Entity1>

        @Query("SELECT * from Entity2 ORDER BY id ASC") fun loadAllEntity2s(): List<Entity2>

        @Query("SELECT * from Entity2 ORDER BY id ASC")
        fun loadAllEntity2sAsPojo(): List<Entity2Pojo>

        @Insert fun insert(vararg entity2: Entity2)
    }

    internal class Entity2Pojo(id: Int, addedInV3: String?, name: String?) :
        Entity2(id, addedInV3, name)

    /** not a real dao because database will change. */
    internal class DaoV1(val connection: SQLiteConnection) {

        fun insertIntoEntity1(id: Int, name: String) {
            connection.prepare("INSERT OR REPLACE INTO ${Entity1.TABLE_NAME} VALUES (?,?)").use {
                it.bindInt(1, id)
                it.bindText(2, name)
                it.step()
            }
        }
    }

    /** not a real dao because database will change. */
    internal class DaoV2(val connection: SQLiteConnection) {

        fun insertIntoEntity2(id: Int, name: String) {
            connection.prepare("INSERT OR REPLACE INTO ${Entity2.TABLE_NAME} VALUES (?,?)").use {
                it.bindInt(1, id)
                it.bindText(2, name)
                it.step()
            }
        }
    }

    companion object {
        const val LATEST_VERSION = 10
    }
}
