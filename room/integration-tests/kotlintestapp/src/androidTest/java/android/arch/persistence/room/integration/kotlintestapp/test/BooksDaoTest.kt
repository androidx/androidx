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
import android.arch.persistence.room.integration.kotlintestapp.vo.Lang
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

        val expected = BookWithPublisher(TestUtil.BOOK_1.bookId, TestUtil.BOOK_1.title,
                TestUtil.PUBLISHER)
        val expectedList = ArrayList<BookWithPublisher>()
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

        val actualPublisherWithBooks = booksDao.getPublisherWithBooks(
                TestUtil.PUBLISHER.publisherId)

        assertThat(actualPublisherWithBooks.publisher, `is`<Publisher>(TestUtil.PUBLISHER))
        assertThat(actualPublisherWithBooks.books?.size, `is`(2))
        assertThat(actualPublisherWithBooks.books?.get(0), `is`<Book>(TestUtil.BOOK_1))
        assertThat(actualPublisherWithBooks.books?.get(1), `is`<Book>(TestUtil.BOOK_2))
    }

    @Test // b/68077506
    fun publisherWithBookSales() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)
        val actualPublisherWithBooks = booksDao.getPublisherWithBookSales(
                TestUtil.PUBLISHER.publisherId)

        assertThat(actualPublisherWithBooks.publisher, `is`<Publisher>(TestUtil.PUBLISHER))
        assertThat(actualPublisherWithBooks.sales, `is`(listOf(TestUtil.BOOK_1.salesCnt,
                TestUtil.BOOK_2.salesCnt)))
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

    @Test
    fun findBooksByLanguage() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        val book1 = TestUtil.BOOK_1.copy(languages = setOf(Lang.TR))
        val book2 = TestUtil.BOOK_2.copy(languages = setOf(Lang.ES, Lang.TR))
        val book3 = TestUtil.BOOK_3.copy(languages = setOf(Lang.EN))
        booksDao.addBooks(book1, book2, book3)

        assertThat(booksDao.findByLanguages(setOf(Lang.EN, Lang.TR)),
                `is`(listOf(book1, book2, book3)))

        assertThat(booksDao.findByLanguages(setOf(Lang.TR)),
                `is`(listOf(book1, book2)))

        assertThat(booksDao.findByLanguages(setOf(Lang.ES)),
                `is`(listOf(book2)))

        assertThat(booksDao.findByLanguages(setOf(Lang.EN)),
                `is`(listOf(book3)))
    }

    @Test
    fun insertVarargInInheritedDao() {
        database.derivedDao().insertAllArg(TestUtil.AUTHOR_1, TestUtil.AUTHOR_2)

        val author = database.derivedDao().getAuthor(TestUtil.AUTHOR_1.authorId)

        assertThat(author, CoreMatchers.`is`<Author>(TestUtil.AUTHOR_1))
    }

    @Test
    fun insertListInInheritedDao() {
        database.derivedDao().insertAll(listOf(TestUtil.AUTHOR_1))

        val author = database.derivedDao().getAuthor(TestUtil.AUTHOR_1.authorId)

        assertThat(author, CoreMatchers.`is`<Author>(TestUtil.AUTHOR_1))
    }
}
