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

import androidx.room.EmptyResultSetException
import androidx.room.integration.kotlintestapp.vo.BookWithPublisher
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import io.reactivex.schedulers.Schedulers
import org.junit.Test

@SmallTest
class RxJava2QueryTest : TestDatabaseTest() {

    @Test
    fun observeBooksByIdFlowable() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)
        booksDao
            .getBookFlowable(TestUtil.BOOK_1.bookId)
            .test()
            .also { drain() }
            .assertValue { book -> book == TestUtil.BOOK_1 }
    }

    @Test
    fun observeBooksByIdFlowable_noBook() {
        booksDao
            .getBookFlowable(TestUtil.BOOK_1.bookId)
            .test()
            .also { drain() }
            .assertNoErrors()
            .assertNoValues()
    }

    @Test
    fun observeBooksByIdObservable() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)
        booksDao
            .getBookObservable(TestUtil.BOOK_1.bookId)
            .test()
            .also { drain() }
            .assertValue { book -> book == TestUtil.BOOK_1 }
    }

    @Test
    fun observeBooksById_noBook() {
        booksDao
            .getBookObservable(TestUtil.BOOK_1.bookId)
            .test()
            .also { drain() }
            .assertNoErrors()
            .assertNoValues()
    }

    @Test
    fun observeBooksByIdSingle() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        booksDao.getBookSingle(TestUtil.BOOK_1.bookId).test().assertComplete().assertValue { book ->
            book == TestUtil.BOOK_1
        }
    }

    @Test
    fun observeBooksByIdSingle_noBook() {
        booksDao.getBookSingle("x").test().assertError(EmptyResultSetException::class.java)
    }

    @Test
    fun observeBooksByIdMaybe() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        booksDao.getBookMaybe(TestUtil.BOOK_1.bookId).test().assertComplete().assertValue { book ->
            book == TestUtil.BOOK_1
        }
    }

    @Test
    fun observeBooksByIdMaybe_noBook() {
        booksDao.getBookMaybe("x").test().assertComplete().assertNoErrors().assertNoValues()
    }

    @Test
    fun observeBooksWithPublisher() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val expected =
            BookWithPublisher(TestUtil.BOOK_1.bookId, TestUtil.BOOK_1.title, TestUtil.PUBLISHER)
        val expectedList = listOf(expected)
        booksDao.getBooksWithPublisherFlowable().test().also { drain() }.assertValue(expectedList)
    }

    @Test
    fun publisherWithBooks() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)
        booksDao
            .getPublisherWithBooksFlowable(TestUtil.PUBLISHER.publisherId)
            .test()
            .also { drain() }
            .assertValue {
                it.publisher == TestUtil.PUBLISHER &&
                    it.books?.size == 2 &&
                    it.books?.get(0) == TestUtil.BOOK_1 &&
                    it.books?.get(1) == TestUtil.BOOK_2
            }
    }

    @Test
    fun mainThreadSubscribe_sharedPreparedQuery() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            booksDao
                .insertPublisherCompletable("a1", "author1")
                .subscribeOn(Schedulers.io())
                .blockingAwait()
        }
    }

    @Test
    fun mainThreadSubscribe_preparedQuery() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            booksDao.deleteBookWithIdsSingle("b1", "b2").subscribeOn(Schedulers.io()).blockingGet()
        }
    }
}
