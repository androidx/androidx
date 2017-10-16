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

package android.arch.persistence.room.integration.kotlintestapp.migration

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Database
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.Index
import android.arch.persistence.room.Insert
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Query
import android.arch.persistence.room.RoomDatabase
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

@Database(version = MigrationDbKotlin.LATEST_VERSION,
        entities = arrayOf(MigrationDbKotlin.Entity1::class, MigrationDbKotlin.Entity2::class,
                MigrationDbKotlin.Entity4::class))
abstract class MigrationDbKotlin : RoomDatabase() {

    internal abstract fun dao(): MigrationDao

    @Entity(indices = arrayOf(Index(value = "name", unique = true)))
    data class Entity1(@PrimaryKey var id: Int = 0, var name: String?) {

        companion object {
            val TABLE_NAME = "Entity1"
        }
    }

    @Entity
    open class Entity2(@PrimaryKey var id: Int = 0, var addedInV3: String?, var name: String?) {
        companion object {
            val TABLE_NAME = "Entity2"
        }
    }

    @Entity
    data class Entity3(@PrimaryKey var id: Int = 0, @Ignore var removedInV5: String?,
                       var name: String?) { // added in version 4, removed at 6
        companion object {
            val TABLE_NAME = "Entity3"
        }
    }

    @Entity(foreignKeys = arrayOf(ForeignKey(entity = Entity1::class,
            parentColumns = arrayOf("name"),
            childColumns = arrayOf("name"),
            deferred = true)))
    data class Entity4(@PrimaryKey var id: Int = 0, var name: String?) {
        companion object {
            val TABLE_NAME = "Entity4"
        }
    }

    @Dao
    internal interface MigrationDao {
        @Query("SELECT * from Entity1 ORDER BY id ASC")
        fun loadAllEntity1s(): List<Entity1>

        @Query("SELECT * from Entity2 ORDER BY id ASC")
        fun loadAllEntity2s(): List<Entity2>

        @Query("SELECT * from Entity2 ORDER BY id ASC")
        fun loadAllEntity2sAsPojo(): List<Entity2Pojo>

        @Insert
        fun insert(vararg entity2: Entity2)
    }

    internal class Entity2Pojo(id: Int, addedInV3: String?, name: String?)
        : Entity2(id, addedInV3, name)

    /**
     * not a real dao because database will change.
     */
    internal class Dao_V1(val mDb: SupportSQLiteDatabase) {

        fun insertIntoEntity1(id: Int, name: String) {
            val values = ContentValues()
            values.put("id", id)
            values.put("name", name)
            val insertionId = mDb.insert(Entity1.TABLE_NAME,
                    SQLiteDatabase.CONFLICT_REPLACE, values)
            if (insertionId == -1L) {
                throw RuntimeException("test sanity failure")
            }
        }
    }

    /**
     * not a real dao because database will change.
     */
    internal class Dao_V2(val mDb: SupportSQLiteDatabase) {

        fun insertIntoEntity2(id: Int, name: String) {
            val values = ContentValues()
            values.put("id", id)
            values.put("name", name)
            val insertionId = mDb.insert(Entity2.TABLE_NAME,
                    SQLiteDatabase.CONFLICT_REPLACE, values)
            if (insertionId == -1L) {
                throw RuntimeException("test sanity failure")
            }
        }
    }

    companion object {
        const val LATEST_VERSION = 7
    }
}
