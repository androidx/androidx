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

package android.arch.persistence.room.integration.kotlintestapp.test

import android.arch.persistence.room.integration.kotlintestapp.vo.BookWithPublisher
import org.junit.Test

class RxJava2QueryTest : TestDatabaseTest() {

    @Test
    fun observeBooksById() {
        database.booksDao().addAuthors(TestUtil.AUTHOR_1)
        database.booksDao().addPublishers(TestUtil.PUBLISHER)
        database.booksDao().addBooks(TestUtil.BOOK_1)

        database.booksDao().getBookFlowable(TestUtil.BOOK_1.bookId)
                .test()
                .assertValue { book -> book == TestUtil.BOOK_1 }
    }

    @Test
    fun observeBooksWithPublisher() {
        database.booksDao().addAuthors(TestUtil.AUTHOR_1)
        database.booksDao().addPublishers(TestUtil.PUBLISHER)
        database.booksDao().addBooks(TestUtil.BOOK_1)

        var expected = BookWithPublisher(TestUtil.BOOK_1.bookId, TestUtil.BOOK_1.title,
                TestUtil.PUBLISHER)
        var expectedList = ArrayList<BookWithPublisher>()
        expectedList.add(expected)

        database.booksDao().getBooksWithPublisherFlowable()
                .test()
                .assertValue(expectedList)
    }

    @Test
    fun publisherWithBooks() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        booksDao.getPublisherWithBooksFlowable(TestUtil.PUBLISHER.publisherId)
                .test()
                .assertValue {
                    it.publisher == TestUtil.PUBLISHER
                            && it.books?.size == 2
                            && it.books?.get(0) == TestUtil.BOOK_1
                            && it.books?.get(1) == TestUtil.BOOK_2
                }
    }
}
