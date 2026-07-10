/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.room3.Fts5
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase

/** FTS5 test database for [Fts5MigrationTest] */
@Database(
    entities =
        [Fts5MigrationDb.Song::class, Fts5MigrationDb.SongFts::class, Fts5MigrationDb.Mail::class],
    autoMigrations =
        [
            AutoMigration(from = 1, to = 2),
            AutoMigration(from = 2, to = 3),
            AutoMigration(from = 3, to = 4),
        ],
    version = 4,
)
abstract class Fts5MigrationDb : RoomDatabase() {

    abstract fun getMailDao(): MailDao

    @Entity data class Song(@PrimaryKey val songId: String, val title: String)

    @Entity
    @Fts5(contentEntity = Song::class, contentRowId = "songId")
    data class SongFts(val title: String)

    @Fts5
    @Entity
    data class Mail(
        @PrimaryKey @ColumnInfo(name = "rowid") val mailId: Long,
        val title: String,
        val content: String,
    )

    @Dao
    interface MailDao {
        @Query("SELECT COUNT(*) FROM Mail") suspend fun getMailCount(): Long
    }
}
