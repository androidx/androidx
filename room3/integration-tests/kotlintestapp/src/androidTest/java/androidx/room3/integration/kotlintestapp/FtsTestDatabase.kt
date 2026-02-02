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
import androidx.room3.integration.kotlintestapp.dao.MailDao
import androidx.room3.integration.kotlintestapp.dao.SongDao
import androidx.room3.integration.kotlintestapp.vo.Mail
import androidx.room3.integration.kotlintestapp.vo.Song
import androidx.room3.integration.kotlintestapp.vo.SongDescription

@Database(
    entities = [Mail::class, SongDescription::class, Song::class],
    version = 1,
    exportSchema = false,
)
abstract class FtsTestDatabase : RoomDatabase() {
    abstract fun getMailDao(): MailDao

    abstract fun getSongDao(): SongDao
}
