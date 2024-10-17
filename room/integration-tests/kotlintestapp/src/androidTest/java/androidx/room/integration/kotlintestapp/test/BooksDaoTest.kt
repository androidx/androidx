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

import android.database.sqlite.SQLiteConstraintException
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.kruth.assertThat
import androidx.room.integration.kotlintestapp.vo.Author
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.integration.kotlintestapp.vo.BookWithPublisher
import androidx.room.integration.kotlintestapp.vo.Lang
import androidx.room.integration.kotlintestapp.vo.Publisher
import androidx.sqlite.SQLiteException
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.base.Optional
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.TestSubscriber
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@MediumTest
@RunWith(Parameterized::class)
class BooksDaoTest(useBundledSQLite: Boolean) : TestDatabaseTest(useBundledSQLite) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useBundledSQLite={0}")
        fun parameters() = arrayOf(false, true)
    }

    @Test
    fun addPublisherIdError() {
        // the following would cause Unique constraint fail and would not return -1
        // booksDao.addPublishers(TestUtil.PUBLISHER2)
        val publisherList =
            buildList<Publisher> {
                add(TestUtil.PUBLISHER)
                add(TestUtil.PUBLISHER2)
            }
        val result = booksDao.addPublisherReturnArray(publisherList)
        assertEquals(result[1], 2)
    }

    @Test
    fun bookById() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(booksDao.getBook(TestUtil.BOOK_1.bookId), `is`<Book>(TestUtil.BOOK_1))
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun bookByIdJavaOptional() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(
            booksDao.getBookJavaOptional(TestUtil.BOOK_1.bookId),
            `is`<java.util.Optional<Book>>(java.util.Optional.of(TestUtil.BOOK_1))
        )
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun bookByIdJavaOptionalEmpty() {
        assertThat(
            booksDao.getBookJavaOptional(TestUtil.BOOK_1.bookId),
            `is`<java.util.Optional<Book>>(java.util.Optional.empty())
        )
    }

    @Test
    fun bookByIdListenableFuture() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(
            booksDao.getBookListenableFuture(TestUtil.BOOK_1.bookId).get(),
            `is`<Book>(TestUtil.BOOK_1)
        )
    }

    @Test
    fun bookByIdOptional() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(
            booksDao.getBookOptional(TestUtil.BOOK_1.bookId),
            `is`<Optional<Book>>(Optional.of(TestUtil.BOOK_1))
        )
    }

    @Test
    fun bookByIdOptionalListenableFuture() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(
            booksDao.getBookOptionalListenableFuture(TestUtil.BOOK_1.bookId).get(),
            `is`<Optional<Book>>(Optional.of(TestUtil.BOOK_1))
        )
    }

    @Test
    fun bookByIdOptionalListenableFutureAbsent() {
        assertThat(
            booksDao.getBookOptionalListenableFuture(TestUtil.BOOK_1.bookId).get(),
            `is`<Optional<Book>>(Optional.absent())
        )
    }

    @Test
    fun bookByIdOptionalAbsent() {
        assertThat(
            booksDao.getBookOptional(TestUtil.BOOK_1.bookId),
            `is`<Optional<Book>>(Optional.absent())
        )
    }

    @Test
    fun bookByIdOptionalFlowable() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val subscriber = TestSubscriber<Optional<Book>>()
        val flowable: Flowable<Optional<Book>> =
            booksDao.getBookOptionalFlowable(TestUtil.BOOK_1.bookId)
        flowable
            .observeOn(Schedulers.from(ArchTaskExecutor.getMainThreadExecutor()))
            .subscribeWith(subscriber)
        drain()
        assertThat(subscriber.values().size, `is`(1))
        assertThat(subscriber.values()[0], `is`(Optional.of(TestUtil.BOOK_1)))
    }

    @Test
    fun bookByIdOptionalFlowableAbsent() {
        val subscriber = TestSubscriber<Optional<Book>>()
        val flowable: Flowable<Optional<Book>> =
            booksDao.getBookOptionalFlowable(TestUtil.BOOK_1.bookId)
        flowable
            .observeOn(Schedulers.from(ArchTaskExecutor.getMainThreadExecutor()))
            .subscribeWith(subscriber)
        drain()
        assertThat(subscriber.values().size, `is`(1))
        assertThat(subscriber.values()[0], `is`(Optional.absent()))
    }

    @Test
    fun bookWithPublisher() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val expected =
            BookWithPublisher(TestUtil.BOOK_1.bookId, TestUtil.BOOK_1.title, TestUtil.PUBLISHER)
        val expectedList = ArrayList<BookWithPublisher>()
        expectedList.add(expected)

        assertThat(
            database.booksDao().getBooksWithPublisher(),
            `is`<List<BookWithPublisher>>(expectedList)
        )
    }

    @Test
    fun bookWithPublisherListenableFuture() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val expected =
            BookWithPublisher(TestUtil.BOOK_1.bookId, TestUtil.BOOK_1.title, TestUtil.PUBLISHER)
        val expectedList = ArrayList<BookWithPublisher>()
        expectedList.add(expected)

        assertThat(
            database.booksDao().getBooksWithPublisherListenableFuture().get(),
            `is`<List<BookWithPublisher>>(expectedList)
        )
    }

    @Test
    fun updateBookWithNullTitle() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        try {
            booksDao.updateBookTitle(TestUtil.BOOK_1.bookId, null)
            fail("updateBookTitle should have failed")
        } catch (ex: SQLiteConstraintException) {
            // ignored on purpose
        } catch (ex: SQLiteException) {
            assertThat(ex).hasMessageThat().contains("NOT NULL constraint failed")
        }
    }

    @Test
    fun publisherWithBooks() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val actualPublisherWithBooks =
            booksDao.getPublisherWithBooks(TestUtil.PUBLISHER.publisherId)

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
        val actualPublisherWithBooks =
            booksDao.getPublisherWithBookSales(TestUtil.PUBLISHER.publisherId)

        assertThat(actualPublisherWithBooks.publisher, `is`<Publisher>(TestUtil.PUBLISHER))
        assertThat(
            actualPublisherWithBooks.sales,
            `is`(listOf(TestUtil.BOOK_1.salesCnt, TestUtil.BOOK_2.salesCnt))
        )
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

        val books =
            database
                .booksDao()
                .getBooksMultiLineQuery(arrayListOf(TestUtil.BOOK_1.bookId, TestUtil.BOOK_2.bookId))
        assertThat(books, `is`(listOf(TestUtil.BOOK_2, TestUtil.BOOK_1)))
    }

    @Test
    fun findBooksByLanguage() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        val book1 = TestUtil.BOOK_1.copy(languages = setOf(Lang.TR))
        val book2 = TestUtil.BOOK_2.copy(languages = setOf(Lang.ES, Lang.TR))
        val book3 = TestUtil.BOOK_3.copy(languages = setOf(Lang.EN))
        booksDao.addBooks(book1, book2, book3)

        assertThat(
            booksDao.findByLanguages(setOf(Lang.EN, Lang.TR)),
            `is`(listOf(book1, book2, book3))
        )

        assertThat(booksDao.findByLanguages(setOf(Lang.TR)), `is`(listOf(book1, book2)))

        assertThat(booksDao.findByLanguages(setOf(Lang.ES)), `is`(listOf(book2)))

        assertThat(booksDao.findByLanguages(setOf(Lang.EN)), `is`(listOf(book3)))
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

    @Test
    fun deleteAndAddPublisher() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.getPublishers().run {
            assertThat(this.size, `is`(1))
            assertThat(this.first(), `is`(equalTo(TestUtil.PUBLISHER)))
        }
        booksDao.deleteAndAddPublisher(TestUtil.PUBLISHER, TestUtil.PUBLISHER2)
        booksDao.getPublishers().run {
            assertThat(this.size, `is`(1))
            assertThat(this.first(), `is`(equalTo(TestUtil.PUBLISHER2)))
        }
    }

    @Test
    fun deleteAndAddPublisher_immutableList() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.getPublishersImmutable().run {
            assertThat(this.size, `is`(1))
            assertThat(this.first(), `is`(equalTo(TestUtil.PUBLISHER)))
        }
        booksDao.deleteAndAddPublisher(TestUtil.PUBLISHER, TestUtil.PUBLISHER2)
        booksDao.getPublishers().run {
            assertThat(this.size, `is`(1))
            assertThat(this.first(), `is`(equalTo(TestUtil.PUBLISHER2)))
        }
    }

    @Test
    fun deleteAndAddPublisher_failure() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.getPublishers().run {
            assertThat(this.size, `is`(1))
            assertThat(this.first(), `is`(equalTo(TestUtil.PUBLISHER)))
        }
        var throwable: Throwable? = null
        try {
            booksDao.deleteAndAddPublisher(TestUtil.PUBLISHER, TestUtil.PUBLISHER2, true)
        } catch (e: RuntimeException) {
            throwable = e
        }
        assertThat(throwable, `is`(notNullValue()))
        booksDao.getPublishers().run {
            assertThat(this.size, `is`(1))
            assertThat(this.first(), `is`(equalTo(TestUtil.PUBLISHER)))
        }
    }

    @Test
    fun deleteBooksWithZeroSales() {
        val books = listOf(TestUtil.BOOK_1.copy(salesCnt = 0), TestUtil.BOOK_2.copy(salesCnt = 0))
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(*books.toTypedArray())

        runBlocking {
            assertThat(booksDao.deleteBooksWithZeroSales(), `is`(equalTo(books)))
            assertThat(booksDao.getBooksSuspend(), `is`(equalTo(emptyList())))
        }
    }

    @Test
    fun addAuthorPublisherBooks_failure() {
        runBlocking {
            try {
                booksDao.addAuthorPublisherBooks(
                    author = TestUtil.AUTHOR_1,
                    publisher = TestUtil.PUBLISHER,
                    books = arrayOf(TestUtil.BOOK_1, TestUtil.BOOK_1)
                )
                fail("addAuthorPublisherBooks should have failed")
            } catch (ex: SQLiteConstraintException) {
                // ignored on purpose
            } catch (ex: SQLiteException) {
                assertThat(ex).hasMessageThat().contains("UNIQUE constraint failed")
            }

            assertThat(booksDao.getBooksSuspend().isEmpty(), `is`(true))
        }
    }

    @Test
    fun kotlinDefaultFunction() {
        booksDao.addAndRemovePublisher(TestUtil.PUBLISHER)
        assertNull(booksDao.getPublisherNullable(TestUtil.PUBLISHER.publisherId))

        assertEquals("", booksDao.concreteFunction())
        assertEquals("1 - hello", booksDao.concreteFunctionWithParams(1, "hello"))

        runBlocking {
            assertEquals("", booksDao.concreteSuspendFunction())
            assertEquals("2 - hi", booksDao.concreteSuspendFunctionWithParams(2, "hi"))
        }
    }

    @Test
    fun multimapDataClassKey() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        booksDao.getBooksByPublisher().let { result ->
            assertThat(result[TestUtil.PUBLISHER]).containsExactly(TestUtil.BOOK_1)
        }
    }
}
