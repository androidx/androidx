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

package androidx.room3.integration.kotlintestapp.test

import android.database.sqlite.SQLiteConstraintException
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.kruth.assertThat
import androidx.room3.integration.kotlintestapp.vo.Author
import androidx.room3.integration.kotlintestapp.vo.Book
import androidx.room3.integration.kotlintestapp.vo.BookWithPublisher
import androidx.room3.integration.kotlintestapp.vo.Either
import androidx.room3.integration.kotlintestapp.vo.Lang
import androidx.room3.integration.kotlintestapp.vo.Publisher
import androidx.room3.integration.kotlintestapp.vo.isLeft
import androidx.room3.integration.kotlintestapp.vo.isRight
import androidx.sqlite.SQLiteException
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.base.Optional
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subscribers.TestSubscriber
import java.util.Date
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@MediumTest
@RunWith(Parameterized::class)
class BooksDaoTest(useDriver: UseDriver) : TestDatabaseTest(useDriver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.BUNDLED, UseDriver.ANDROID)
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
        assertThat(result[1]).isEqualTo(2)
    }

    @Test
    fun bookById() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(booksDao.getBook(TestUtil.BOOK_1.bookId)).isEqualTo(TestUtil.BOOK_1)
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun bookByIdJavaOptional() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(booksDao.getBookJavaOptional(TestUtil.BOOK_1.bookId))
            .isEqualTo(java.util.Optional.of(TestUtil.BOOK_1))
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun bookByIdJavaOptionalEmpty() {
        assertThat(booksDao.getBookJavaOptional(TestUtil.BOOK_1.bookId))
            .isEqualTo(java.util.Optional.empty<Book>())
    }

    @Test
    fun bookByIdListenableFuture() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(booksDao.getBookListenableFuture(TestUtil.BOOK_1.bookId).get())
            .isEqualTo(TestUtil.BOOK_1)
    }

    @Test
    fun bookByIdOptional() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(booksDao.getBookOptional(TestUtil.BOOK_1.bookId)).hasValue(TestUtil.BOOK_1)
    }

    @Test
    fun bookByIdOptionalListenableFuture() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        assertThat(booksDao.getBookOptionalListenableFuture(TestUtil.BOOK_1.bookId).get())
            .hasValue(TestUtil.BOOK_1)
    }

    @Test
    fun bookByIdOptionalListenableFutureAbsent() {
        assertThat(booksDao.getBookOptionalListenableFuture(TestUtil.BOOK_1.bookId).get())
            .isAbsent()
    }

    @Test
    fun bookByIdOptionalAbsent() {
        assertThat(booksDao.getBookOptional(TestUtil.BOOK_1.bookId)).isAbsent()
    }

    @Test
    fun bookByIdOptionalFlowable() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val flowable: Flowable<Optional<Book>> =
            booksDao.getBookOptionalFlowable(TestUtil.BOOK_1.bookId)
        val subscriber =
            flowable
                .observeOn(Schedulers.from(ArchTaskExecutor.getMainThreadExecutor()))
                .subscribeWith(TestSubscriber())
        drain()
        assertThat(subscriber.values()).hasSize(1)
        assertThat(subscriber.values()[0]).hasValue(TestUtil.BOOK_1)
    }

    @Test
    fun bookByIdOptionalFlowableAbsent() {
        val flowable: Flowable<Optional<Book>> =
            booksDao.getBookOptionalFlowable(TestUtil.BOOK_1.bookId)
        val subscriber =
            flowable
                .observeOn(Schedulers.from(ArchTaskExecutor.getMainThreadExecutor()))
                .subscribeWith(TestSubscriber())
        drain()
        assertThat(subscriber.values()).hasSize(1)
        assertThat(subscriber.values()[0]).isAbsent()
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

        assertThat(database.booksDao().getBooksWithPublisher()).isEqualTo(expectedList)
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

        assertThat(database.booksDao().getBooksWithPublisherListenableFuture().get())
            .isEqualTo(expectedList)
    }

    @Test
    fun updateBookWithNullTitle() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val exception =
            assertFailsWith<SQLiteException> {
                booksDao.updateBookTitle(TestUtil.BOOK_1.bookId, null)
            }
        if (exception !is SQLiteConstraintException) {
            assertThat(exception).hasMessageThat().contains("NOT NULL constraint failed")
        }
    }

    @Test
    fun publisherWithBooks() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val actualPublisherWithBooks =
            booksDao.getPublisherWithBooks(TestUtil.PUBLISHER.publisherId)

        assertThat(actualPublisherWithBooks.publisher).isEqualTo(TestUtil.PUBLISHER)
        assertThat(actualPublisherWithBooks.books).hasSize(2)
        assertThat(actualPublisherWithBooks.books?.get(0)).isEqualTo(TestUtil.BOOK_1)
        assertThat(actualPublisherWithBooks.books?.get(1)).isEqualTo(TestUtil.BOOK_2)
    }

    @Test // b/68077506
    fun publisherWithBookSales() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)
        val actualPublisherWithBooks =
            booksDao.getPublisherWithBookSales(TestUtil.PUBLISHER.publisherId)

        assertThat(actualPublisherWithBooks.publisher).isEqualTo(TestUtil.PUBLISHER)
        assertThat(actualPublisherWithBooks.sales)
            .isEqualTo(listOf(TestUtil.BOOK_1.salesCnt, TestUtil.BOOK_2.salesCnt))
    }

    @Test
    fun insertAuthorWithAllFields() {
        val author = Author("id", "name", Date(), ArrayList())
        database.booksDao().addAuthors(author)

        val authorDb = database.booksDao().getAuthor(author.authorId)

        assertThat(authorDb).isEqualTo(author)
    }

    @Test
    fun insertInInheritedDao() {
        database.derivedDao().insert(TestUtil.AUTHOR_1)

        val author = database.derivedDao().getAuthor(TestUtil.AUTHOR_1.authorId)

        assertThat(author).isEqualTo(TestUtil.AUTHOR_1)
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
        assertThat(books).containsExactly(TestUtil.BOOK_2, TestUtil.BOOK_1).inOrder()
    }

    @Test
    fun findBooksInMultiLineQueryWithComment() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)
        booksDao.addBooks(TestUtil.BOOK_2)

        val books =
            database
                .booksDao()
                .getBooksMultiLineQueryWithComment(
                    arrayListOf(TestUtil.BOOK_1.bookId, TestUtil.BOOK_2.bookId)
                )
        assertThat(books).containsExactly(TestUtil.BOOK_2, TestUtil.BOOK_1).inOrder()
    }

    @Test
    fun findBooksByLanguage() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        val book1 = TestUtil.BOOK_1.copy(languages = setOf(Lang.TR))
        val book2 = TestUtil.BOOK_2.copy(languages = setOf(Lang.ES, Lang.TR))
        val book3 = TestUtil.BOOK_3.copy(languages = setOf(Lang.EN))
        booksDao.addBooks(book1, book2, book3)

        assertThat(booksDao.findByLanguages(setOf(Lang.EN, Lang.TR)))
            .containsExactly(book1, book2, book3)

        assertThat(booksDao.findByLanguages(setOf(Lang.TR))).containsExactly(book1, book2)

        assertThat(booksDao.findByLanguages(setOf(Lang.ES))).containsExactly(book2)

        assertThat(booksDao.findByLanguages(setOf(Lang.EN))).containsExactly(book3)
    }

    @Test
    fun insertVarargInInheritedDao() {
        database.derivedDao().insertAllArg(TestUtil.AUTHOR_1, TestUtil.AUTHOR_2)

        val author = database.derivedDao().getAuthor(TestUtil.AUTHOR_1.authorId)

        assertThat(author).isEqualTo(TestUtil.AUTHOR_1)
    }

    @Test
    fun insertListInInheritedDao() {
        database.derivedDao().insertAll(listOf(TestUtil.AUTHOR_1))

        val author = database.derivedDao().getAuthor(TestUtil.AUTHOR_1.authorId)

        assertThat(author).isEqualTo(TestUtil.AUTHOR_1)
    }

    @Test
    fun deleteAndAddPublisher() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.getPublishers().run {
            assertThat(this).hasSize(1)
            assertThat(this.first()).isEqualTo(TestUtil.PUBLISHER)
        }
        booksDao.deleteAndAddPublisher(TestUtil.PUBLISHER, TestUtil.PUBLISHER2)
        booksDao.getPublishers().run {
            assertThat(this).hasSize(1)
            assertThat(this.first()).isEqualTo(TestUtil.PUBLISHER2)
        }
    }

    @Test
    fun deleteAndAddPublisher_immutableList() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.getPublishersImmutable().run {
            assertThat(this).hasSize(1)
            assertThat(this.first()).isEqualTo(TestUtil.PUBLISHER)
        }
        booksDao.deleteAndAddPublisher(TestUtil.PUBLISHER, TestUtil.PUBLISHER2)
        booksDao.getPublishers().run {
            assertThat(this).hasSize(1)
            assertThat(this.first()).isEqualTo(TestUtil.PUBLISHER2)
        }
    }

    @Test
    fun deleteAndAddPublisher_failure() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.getPublishers().run {
            assertThat(this).hasSize(1)
            assertThat(this.first()).isEqualTo(TestUtil.PUBLISHER)
        }
        assertFailsWith<RuntimeException> {
            booksDao.deleteAndAddPublisher(TestUtil.PUBLISHER, TestUtil.PUBLISHER2, true)
        }
        booksDao.getPublishers().run {
            assertThat(this).hasSize(1)
            assertThat(this.first()).isEqualTo(TestUtil.PUBLISHER)
        }
    }

    @Test
    fun deleteBooksWithZeroSales() {
        val books = listOf(TestUtil.BOOK_1.copy(salesCnt = 0), TestUtil.BOOK_2.copy(salesCnt = 0))
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(*books.toTypedArray())

        runBlocking {
            assertThat(booksDao.deleteBooksWithZeroSales()).isEqualTo(books)
            assertThat(booksDao.getBooksSuspend()).isEmpty()
        }
    }

    @Test
    fun addAuthorPublisherBooks_failure() {
        runBlocking {
            val exception =
                assertFailsWith<SQLiteException> {
                    booksDao.addAuthorPublisherBooks(
                        author = TestUtil.AUTHOR_1,
                        publisher = TestUtil.PUBLISHER,
                        books = arrayOf(TestUtil.BOOK_1, TestUtil.BOOK_1),
                    )
                }
            if (exception !is SQLiteConstraintException) {
                assertThat(exception).hasMessageThat().contains("UNIQUE constraint failed")
            }

            assertThat(booksDao.getBooksSuspend()).isEmpty()
        }
    }

    @Test
    fun kotlinDefaultFunction() {
        booksDao.addAndRemovePublisher(TestUtil.PUBLISHER)
        assertThat(booksDao.getPublisherNullable(TestUtil.PUBLISHER.publisherId)).isNull()

        assertThat(booksDao.concreteFunction()).isEqualTo("")
        assertThat(booksDao.concreteFunctionWithParams(1, "hello")).isEqualTo("1 - hello")

        runBlocking {
            assertThat(booksDao.concreteSuspendFunction()).isEqualTo("")
            assertThat(booksDao.concreteSuspendFunctionWithParams(2, "hi")).isEqualTo("2 - hi")
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

    @Test
    fun resultCustomDaoReturnType() = runTest {
        assertThat(booksDao.getPublisherResult(TestUtil.PUBLISHER.publisherId).isFailure).isTrue()
        assertThat(booksDao.insertPublisherResult(TestUtil.PUBLISHER).isSuccess).isTrue()
        assertThat(booksDao.getPublisherResult(TestUtil.PUBLISHER.publisherId).getOrNull())
            .isEqualTo(TestUtil.PUBLISHER)
        assertThat(booksDao.insertPublisherResult(TestUtil.PUBLISHER).isFailure).isTrue()
    }

    @Test
    fun eitherCustomDaoReturnType() = runTest {
        booksDao.getPublisherEither(TestUtil.PUBLISHER.publisherId).let {
            if (it.isLeft()) {
                assertThat(it.value).isInstanceOf<IllegalStateException>()
            } else {
                assertThat(it).isInstanceOf<Either.Left<IllegalStateException>>()
            }
        }

        booksDao.insertPublisherEither(TestUtil.PUBLISHER).let {
            if (it.isRight()) {
                assertThat(it.value).isEqualTo(1)
            } else {
                assertThat(it).isInstanceOf<Either.Right<Long>>()
            }
        }

        booksDao.getPublisherEither(TestUtil.PUBLISHER.publisherId).let {
            if (it.isRight()) {
                assertThat(it.value).isEqualTo(TestUtil.PUBLISHER)
            } else {
                assertThat(it).isInstanceOf<Either.Left<Publisher>>()
            }
        }

        booksDao.insertPublisherEither(TestUtil.PUBLISHER).let {
            if (it.isLeft()) {
                assertThat(it.value).isInstanceOf<SQLiteException>()
            } else {
                assertThat(it).isInstanceOf<Either.Left<SQLiteException>>()
            }
        }
    }
}
