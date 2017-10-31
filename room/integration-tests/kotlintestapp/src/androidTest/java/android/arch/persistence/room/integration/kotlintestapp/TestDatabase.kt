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

package android.arch.persistence.room.integration.kotlintestapp

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.integration.kotlintestapp.dao.BooksDao
import android.arch.persistence.room.integration.kotlintestapp.dao.DerivedDao
import android.arch.persistence.room.integration.kotlintestapp.vo.Author
import android.arch.persistence.room.integration.kotlintestapp.vo.Book
import android.arch.persistence.room.integration.kotlintestapp.vo.BookAuthor
import android.arch.persistence.room.integration.kotlintestapp.vo.NoArgClass
import android.arch.persistence.room.integration.kotlintestapp.vo.Publisher

@Database(entities = arrayOf(Book::class, Author::class, Publisher::class, BookAuthor::class,
        NoArgClass::class),
        version = 1)
abstract class TestDatabase : RoomDatabase() {

    abstract fun booksDao(): BooksDao

    abstract fun derivedDao(): DerivedDao
}
