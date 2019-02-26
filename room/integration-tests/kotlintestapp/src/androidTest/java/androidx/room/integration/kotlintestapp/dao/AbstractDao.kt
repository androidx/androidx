/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.integration.kotlintestapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.integration.kotlintestapp.vo.Author
import androidx.room.integration.kotlintestapp.vo.Book

@Dao
abstract class AbstractDao {
    // For verifying b/123466702
    @Query("DELETE FROM book")
    internal abstract suspend fun deleteAllBooksSuspend()

    @Insert
    abstract fun addBooks(vararg books: Book)

    @Insert
    abstract fun addAuthors(vararg authors: Author)

    @Transaction
    open suspend fun insertBookAndAuthorSuspend(book: Book, author: Author) {
        addBooks(book)
        addAuthors(author)
    }
}