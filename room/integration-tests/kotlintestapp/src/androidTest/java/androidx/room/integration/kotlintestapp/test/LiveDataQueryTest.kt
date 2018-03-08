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

import android.support.test.filters.SmallTest
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.integration.kotlintestapp.vo.BookWithPublisher
import androidx.room.integration.kotlintestapp.vo.Publisher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@SmallTest
class LiveDataQueryTest : TestDatabaseTest() {

    @Test
    fun observeBooksById() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val book = LiveDataTestUtil.getValue(booksDao.getBookLiveData(TestUtil.BOOK_1.bookId))

        assertThat(book, `is`<Book>(TestUtil.BOOK_1))
    }

    @Test
    fun observeBooksWithPublisher() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        var expected = BookWithPublisher(TestUtil.BOOK_1.bookId, TestUtil.BOOK_1.title,
                TestUtil.PUBLISHER)
        var expectedList = ArrayList<BookWithPublisher>()
        expectedList.add(expected)

        val actual = LiveDataTestUtil.getValue(booksDao.getBooksWithPublisherLiveData())
        assertThat(actual, `is`<List<BookWithPublisher>>(expectedList))
    }

    @Test
    fun publisherWithBooks() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        var actualPublisherWithBooks = LiveDataTestUtil.getValue(
                booksDao.getPublisherWithBooksLiveData(TestUtil.PUBLISHER.publisherId))

        assertThat(actualPublisherWithBooks.publisher, `is`<Publisher>(TestUtil.PUBLISHER))
        assertThat(actualPublisherWithBooks.books?.size, `is`(2))
        assertThat(actualPublisherWithBooks.books?.get(0), `is`<Book>(TestUtil.BOOK_1))
        assertThat(actualPublisherWithBooks.books?.get(1), `is`<Book>(TestUtil.BOOK_2))
    }
}
