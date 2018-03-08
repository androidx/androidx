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

package androidx.room.integration.kotlintestapp.test

import androidx.room.integration.kotlintestapp.vo.Author
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.integration.kotlintestapp.vo.BookAuthor
import androidx.room.integration.kotlintestapp.vo.Lang
import androidx.room.integration.kotlintestapp.vo.Publisher

class TestUtil {

    companion object {

        val PUBLISHER = Publisher("ph1", "publisher 1")
        val PUBLISHER2 = Publisher("ph2", "publisher 2")

        val AUTHOR_1 = Author("a1", "author 1")
        val AUTHOR_2 = Author("a2", "author 2")

        val BOOK_1 = Book("b1", "book title 1", "ph1",
                setOf(Lang.EN), 3)
        val BOOK_2 = Book("b2", "book title 2", "ph1",
                setOf(Lang.TR), 5)
        val BOOK_3 = Book("b3", "book title 2", "ph1",
                setOf(Lang.ES), 7)

        val BOOK_AUTHOR_1_1 = BookAuthor(BOOK_1.bookId, AUTHOR_1.authorId)
        val BOOK_AUTHOR_1_2 = BookAuthor(BOOK_1.bookId, AUTHOR_2.authorId)
        val BOOK_AUTHOR_2_2 = BookAuthor(BOOK_2.bookId, AUTHOR_2.authorId)
    }
}
