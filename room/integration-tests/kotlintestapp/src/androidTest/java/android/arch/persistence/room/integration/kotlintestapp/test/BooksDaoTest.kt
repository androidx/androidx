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

import android.arch.persistence.room.integration.kotlintestapp.vo.Author
import android.arch.persistence.room.integration.kotlintestapp.vo.Book
import android.arch.persistence.room.integration.kotlintestapp.vo.BookWithPublisher
import android.arch.persistence.room.integration.kotlintestapp.vo.Publisher
import android.database.sqlite.SQLiteConstraintException
import android.support.test.filters.SmallTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Date
import kotlin.collections.ArrayList

@SmallTest
class BooksDaoTest : TestDatabaseTest() {

    @Test
    fun bookById() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(booksDao.getBook(TestUtil.BOOK_1.bookId), `is`<Book>(TestUtil.BOOK_1))
    }

    @Test
    fun bookWithPublisher() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        var expected = BookWithPublisher(TestUtil.BOOK_1.bookId, TestUtil.BOOK_1.title,
                TestUtil.PUBLISHER)
        var expectedList = ArrayList<BookWithPublisher>()
        expectedList.add(expected)

        assertThat(database.booksDao().getBooksWithPublisher(),
                `is`<List<BookWithPublisher>>(expectedList))
    }

    @Test
    fun updateBookWithNullTitle() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        var throwable: Throwable? = null
        try {
            booksDao.updateBookTitle(TestUtil.BOOK_1.bookId, null)
        } catch (t: Throwable) {
            throwable = t
        }
        assertNotNull(throwable)
        assertThat<Throwable>(throwable, instanceOf<Throwable>(SQLiteConstraintException::class
                .java))
    }

    @Test
    fun publisherWithBooks() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        var actualPublisherWithBooks = booksDao.getPublisherWithBooks(
                TestUtil.PUBLISHER.publisherId)

        assertThat(actualPublisherWithBooks.publisher, `is`<Publisher>(TestUtil.PUBLISHER))
        assertThat(actualPublisherWithBooks.books?.size, `is`(2))
        assertThat(actualPublisherWithBooks.books?.get(0), `is`<Book>(TestUtil.BOOK_1))
        assertThat(actualPublisherWithBooks.books?.get(1), `is`<Book>(TestUtil.BOOK_2))
    }

    @Test
    fun insertAuthorWithAllFields() {
        val author = Author("id", "name", Date(), ArrayList())
        database.booksDao().addAuthors(author)

        val authorDb = database.booksDao().getAuthor(author.authorId)

        assertThat(authorDb, CoreMatchers.`is`<Author>(author))
    }

    @Test
    fun insertInInheritedDao() {
        database.derivedDao().insert(TestUtil.AUTHOR_1)

        val author = database.derivedDao().getAuthor(TestUtil.AUTHOR_1.authorId)

        assertThat(author, CoreMatchers.`is`<Author>(TestUtil.AUTHOR_1))
    }

    @Test
    fun findBooksInMultiLineQuery() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)
        booksDao.addBooks(TestUtil.BOOK_2)

        val books = database.booksDao().getBooksMultiLineQuery(arrayListOf(
                TestUtil.BOOK_1.bookId,
                TestUtil.BOOK_2.bookId))
        assertThat(books, `is`(listOf(TestUtil.BOOK_2, TestUtil.BOOK_1)))
    }
}
