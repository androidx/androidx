/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room3.integration.kotlintestapp

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.integration.kotlintestapp.dao.Fts4MailDao
import androidx.room3.integration.kotlintestapp.dao.Fts4SongDao
import androidx.room3.integration.kotlintestapp.vo.Fts4Mail
import androidx.room3.integration.kotlintestapp.vo.Fts4SongDescription
import androidx.room3.integration.kotlintestapp.vo.Song

@Database(
    entities = [Fts4Mail::class, Fts4SongDescription::class, Song::class],
    version = 1,
    exportSchema = false,
)
abstract class Fts4TestDatabase : RoomDatabase() {
    abstract fun getMailDao(): Fts4MailDao

    abstract fun getSongDao(): Fts4SongDao
}
