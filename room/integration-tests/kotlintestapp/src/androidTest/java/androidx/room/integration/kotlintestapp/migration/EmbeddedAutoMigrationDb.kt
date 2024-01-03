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
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomWarnings
import java.io.Serializable

@Database(
    version = EmbeddedAutoMigrationDb.LATEST_VERSION,
    entities = [
        EmbeddedAutoMigrationDb.Entity1::class,
        EmbeddedAutoMigrationDb.EmbeddedEntity1::class,
        EmbeddedAutoMigrationDb.EmbeddedEntity2::class],
    autoMigrations = [AutoMigration(from = 1, to = 2)],
    exportSchema = true
)
abstract class EmbeddedAutoMigrationDb : RoomDatabase() {
    @Dao
    internal interface EmbeddedAutoMigrationDao {
        @Query("SELECT * from Entity1 ORDER BY id ASC")
        fun allEntity1s(): List<Entity1>
    }

    internal abstract fun dao(): EmbeddedAutoMigrationDao

    /**
     * No change between versions.
     */
    @Entity
    @SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
    data class Entity1(
        @PrimaryKey
        val id: Int,
        var name: String,
        @ColumnInfo(defaultValue = "1")
        var addedInV1: Int,
        @Embedded
        var embeddedEntity1: EmbeddedEntity1?
    ) : Serializable {
        companion object {
            const val TABLE_NAME = "Entity1"
        }
    }

    @Entity
    @SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
    data class EmbeddedEntity1(
        @PrimaryKey
        val embeddedId1: Int,

        @ColumnInfo(defaultValue = "1")
        var addedInV2: Int,
        @Embedded
        var embeddedEntity2: EmbeddedEntity2?
    ) : Serializable {
        companion object {
            const val TABLE_NAME = "EmbeddedEntity1"
        }
    }

    @Entity
    @SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
    data class EmbeddedEntity2(@PrimaryKey val embeddedId2: Int) : Serializable {
        companion object {
            const val TABLE_NAME = "EmbeddedEntity2"
        }
    }
    companion object {
        const val LATEST_VERSION = 2
    }
}
