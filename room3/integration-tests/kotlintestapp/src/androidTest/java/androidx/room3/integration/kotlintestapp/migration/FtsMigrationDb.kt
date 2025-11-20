/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.FtsOptions
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase

@Database(
    entities =
        [
            FtsMigrationDb.Book::class,
            FtsMigrationDb.User::class,
            FtsMigrationDb.AddressFts::class,
            FtsMigrationDb.Mail::class,
        ],
    version = 6,
)
abstract class FtsMigrationDb : RoomDatabase() {
    abstract fun bookDao(): BookDao

    abstract fun userDao(): UserDao

    @Entity
    @Fts4(matchInfo = FtsOptions.MatchInfo.FTS3)
    data class Book(
        var title: String?,
        var author: String?,
        var numOfPages: Int,
        var text: String?,
    )

    @Entity
    data class User(
        @PrimaryKey var id: Long,
        var firstName: String?,
        var lastName: String?,
        @Embedded var address: Address?,
    )

    @Entity @Fts4(contentEntity = User::class) class AddressFts(@Embedded var address: Address?)

    class Address(var line1: String?, var line2: String?, var state: String?, var zipcode: Int)

    @Entity
    @Fts4(languageId = "lid")
    data class Mail(
        @PrimaryKey @ColumnInfo(name = "rowid") var mailId: Long,
        var content: String?,
        var lid: Int,
    )

    @Dao
    interface BookDao {
        @Insert fun insert(book: Book)

        @Query("SELECT * FROM BOOK WHERE title MATCH :title") fun getBook(title: String?): Book

        @Query("SELECT * FROM BOOK") fun getAllBooks(): List<Book>
    }

    @Dao
    interface UserDao {
        @Query("SELECT * FROM AddressFts WHERE AddressFts MATCH :searchQuery")
        fun searchAddress(searchQuery: String): List<Address>
    }
}
