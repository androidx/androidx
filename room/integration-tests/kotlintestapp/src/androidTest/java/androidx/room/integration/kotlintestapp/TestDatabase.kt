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

package androidx.room.integration.kotlintestapp

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.integration.kotlintestapp.dao.AbstractDao
import androidx.room.integration.kotlintestapp.dao.BooksDao
import androidx.room.integration.kotlintestapp.dao.CounterDao
import androidx.room.integration.kotlintestapp.dao.DependencyDao
import androidx.room.integration.kotlintestapp.dao.DerivedDao
import androidx.room.integration.kotlintestapp.dao.MusicDao
import androidx.room.integration.kotlintestapp.dao.PetDao
import androidx.room.integration.kotlintestapp.dao.ToyDao
import androidx.room.integration.kotlintestapp.dao.UsersDao
import androidx.room.integration.kotlintestapp.vo.Album
import androidx.room.integration.kotlintestapp.vo.Artist
import androidx.room.integration.kotlintestapp.vo.Author
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.integration.kotlintestapp.vo.BookAuthor
import androidx.room.integration.kotlintestapp.vo.Counter
import androidx.room.integration.kotlintestapp.vo.DataClassFromDependency
import androidx.room.integration.kotlintestapp.vo.EntityWithJavaPojoList
import androidx.room.integration.kotlintestapp.vo.Image
import androidx.room.integration.kotlintestapp.vo.JavaEntity
import androidx.room.integration.kotlintestapp.vo.NoArgClass
import androidx.room.integration.kotlintestapp.vo.Pet
import androidx.room.integration.kotlintestapp.vo.PetUser
import androidx.room.integration.kotlintestapp.vo.PetWithUser
import androidx.room.integration.kotlintestapp.vo.Playlist
import androidx.room.integration.kotlintestapp.vo.PlaylistSongXRef
import androidx.room.integration.kotlintestapp.vo.Publisher
import androidx.room.integration.kotlintestapp.vo.Song
import androidx.room.integration.kotlintestapp.vo.Toy
import androidx.room.integration.kotlintestapp.vo.User
import java.nio.ByteBuffer
import java.util.Date
import java.util.UUID

@Database(
    entities =
        [
            Book::class,
            Author::class,
            Publisher::class,
            BookAuthor::class,
            NoArgClass::class,
            DataClassFromDependency::class,
            JavaEntity::class,
            EntityWithJavaPojoList::class,
            User::class,
            Counter::class,
            Toy::class,
            Pet::class,
            PetUser::class,
            Song::class,
            Playlist::class,
            PlaylistSongXRef::class,
            Artist::class,
            Album::class,
            Image::class
        ],
    views = [PetWithUser::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(TestDatabase.Converters::class)
abstract class TestDatabase : RoomDatabase() {

    abstract fun usersDao(): UsersDao

    abstract fun booksDao(): BooksDao

    abstract fun derivedDao(): DerivedDao

    abstract fun dependencyDao(): DependencyDao

    abstract fun abstractDao(): AbstractDao

    abstract fun counterDao(): CounterDao

    abstract fun toyDao(): ToyDao

    abstract fun petDao(): PetDao

    abstract fun musicDao(): MusicDao

    class Converters {
        @TypeConverter
        fun fromTimestamp(value: Long): Date {
            return Date(value)
        }

        @TypeConverter
        fun dateToTimestamp(date: Date): Long {
            return date.time
        }

        @TypeConverter
        fun decomposeDays(flags: Int): Set<androidx.room.integration.kotlintestapp.vo.Day> {
            val result: MutableSet<androidx.room.integration.kotlintestapp.vo.Day> = HashSet()
            for (day in androidx.room.integration.kotlintestapp.vo.Day.values()) {
                if (flags and (1 shl day.ordinal) != 0) {
                    result.add(day)
                }
            }
            return result
        }

        @TypeConverter
        fun composeDays(days: Set<androidx.room.integration.kotlintestapp.vo.Day>): Int {
            var result = 0
            for (day in days) {
                result = result or (1 shl day.ordinal)
            }
            return result
        }

        @TypeConverter
        fun asUuid(bytes: ByteArray): UUID {
            val bb = ByteBuffer.wrap(bytes)
            val firstLong = bb.long
            val secondLong = bb.long
            return UUID(firstLong, secondLong)
        }

        @TypeConverter
        fun asBytes(uuid: UUID): ByteArray {
            val bb = ByteBuffer.wrap(ByteArray(16))
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            return bb.array()
        }
    }
}
