/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.room.androidx.room.integration.kotlintestapp.test

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.room.integration.kotlintestapp.test.TestDatabaseTest
import androidx.room.integration.kotlintestapp.test.TestUtil
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.room.integration.kotlintestapp.vo.Lang
import androidx.room.integration.kotlintestapp.vo.MiniBook
import androidx.room.integration.kotlintestapp.vo.Publisher
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import io.reactivex.observers.TestObserver
import io.reactivex.subscribers.TestSubscriber
import java.util.concurrent.CountDownLatch
import kotlin.test.assertFails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Test

@MediumTest
class UpsertTest : TestDatabaseTest() {
    @Test
    fun upsertBookById() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.upsertBooks(TestUtil.BOOK_1)
        assertThat(booksDao.getBook(TestUtil.BOOK_1.bookId)).isEqualTo(TestUtil.BOOK_1)

        booksDao.upsertBooks(TestUtil.BOOK_1.copy(title = "changed title"))
        assertThat(booksDao.getBook(TestUtil.BOOK_1.bookId).title).isEqualTo("changed title")
    }

    @Test
    fun multiUpsertBookPublisher() {
        booksDao.upsertBookPublisher(TestUtil.PUBLISHER, TestUtil.BOOK_1)
        assertThat(booksDao.getPublisher(TestUtil.PUBLISHER.publisherId))
            .isEqualTo(TestUtil.PUBLISHER)
        assertThat(booksDao.getBook(TestUtil.BOOK_1.bookId))
            .isEqualTo(TestUtil.BOOK_1)

        booksDao.upsertBookPublisher(
            TestUtil.PUBLISHER.copy(name = "changed name"),
            TestUtil.BOOK_1.copy(title = "changed title")
        )
        assertThat(booksDao.getPublisher(TestUtil.PUBLISHER.publisherId).name)
            .isEqualTo("changed name")
        assertThat(booksDao.getBook(TestUtil.BOOK_1.bookId).title)
            .isEqualTo("changed title")
    }

    val PUBLISHERLIST: List<Publisher> = buildList {
        add(Publisher("ph4", "publisher 4"))
        add(Publisher("ph2", "change publisher 2"))
        add(TestUtil.PUBLISHER3)
    }

    val BOOK_1_EDIT: Book = Book(
        "b1", "book title 4", "ph1",
        setOf(Lang.EN), 6
    )
    @Test
    fun upsertMultiParams() {
        booksDao.upsertTwoPublishers(TestUtil.PUBLISHER, TestUtil.PUBLISHER2)

        assertThat(booksDao.getPublisher(TestUtil.PUBLISHER.publisherId)).isEqualTo(
            TestUtil.PUBLISHER
        )
        assertThat(booksDao.getPublisher(TestUtil.PUBLISHER2.publisherId)).isEqualTo(
            TestUtil.PUBLISHER2
        )

        val modifyPublisher3 = Publisher("ph3", "changed publisher 3")
        booksDao.upsertPublishers(modifyPublisher3)
        booksDao.upsertMultiple(TestUtil.PUBLISHER3, PUBLISHERLIST)

        assertThat(booksDao.getPublisher(TestUtil.PUBLISHER3.publisherId)).isEqualTo(
            TestUtil.PUBLISHER3
        )
        assertThat(booksDao.getPublisher(PUBLISHERLIST[1].publisherId).name).isEqualTo(
            "change publisher 2"
        )
        assertThat(booksDao.getPublisher(PUBLISHERLIST[0].publisherId).name).isEqualTo(
            "publisher 4"
        )
    }

    @Test
    fun upsertBookByFlow() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.upsertBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val firstResultLatch = CountDownLatch(1)
        val secondResultLatch = CountDownLatch(1)
        val thirdResultLatch = CountDownLatch(1)
        val results = mutableListOf<List<Book>>()
        val job = async(Dispatchers.IO) {
            booksDao.getBooksFlow().collect {
                when (results.size) {
                    0 -> {
                        results.add(it)
                        firstResultLatch.countDown()
                    }
                    1 -> {
                        results.add(it)
                        secondResultLatch.countDown()
                    }
                    2 -> {
                        results.add(it)
                        thirdResultLatch.countDown()
                    }
                    else -> fail("Should have only collected 3 results.")
                }
            }
        }

        firstResultLatch.await()

        booksDao.upsertBooksSuspend(TestUtil.BOOK_3)
        secondResultLatch.await()

        booksDao.upsertBooksSuspend(TestUtil.BOOK_3.copy(title = "new title"))
        thirdResultLatch.await()

        assertThat(results.size).isEqualTo(3)
        assertThat(results[0])
            .isEqualTo(listOf(TestUtil.BOOK_1,
                TestUtil.BOOK_2))
        assertThat(results[1])
            .isEqualTo(listOf(TestUtil.BOOK_1,
                TestUtil.BOOK_2,
                TestUtil.BOOK_3))
        assertThat(results[2])
            .isEqualTo(listOf(TestUtil.BOOK_1,
                TestUtil.BOOK_2,
                TestUtil.BOOK_3.copy(title = "new title")))

        job.cancelAndJoin()
    }

    @Test
    fun upsertBookByLiveData() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)

        val bookLiveData: LiveData<Book> = booksDao.getBookLiveData(TestUtil.BOOK_1.bookId)
        val testOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val observer = LiveDataTestObserver<Book>()

        TestUtil.observeOnMainThread(bookLiveData, testOwner, observer)

        assertThat(observer.hasValue()).isFalse()
        observer.reset()

        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(observer.get()).isNull()
        observer.reset()

        booksDao.upsertBooks(TestUtil.BOOK_2)
        assertThat(observer.get()).isNull()
        observer.reset()

        booksDao.upsertBooks(TestUtil.BOOK_1)
        assertThat(observer.get()).isNotNull()
        observer.reset()

        booksDao.upsertBooks(TestUtil.BOOK_1.copy(title = "changed title"))
        assertThat(observer.get()).isNotNull()
        assertThat(observer.get()?.title).isEqualTo("changed title")

        testOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        observer.reset()
    }

    @Test
    fun upsertSingle() {
        val testObserver = TestObserver<Long>()
        booksDao.upsertPublisherSingle(TestUtil.PUBLISHER).subscribeWith(testObserver)
        testObserver.assertComplete()
        val result = testObserver.values().single()
        assertThat(booksDao.getPublisher(TestUtil.PUBLISHER.publisherId)).isEqualTo(
            TestUtil.PUBLISHER
        )
        assertThat(result).isEqualTo(1)
    }

    @Test
    fun upsertSingleError() {
        val testObserver = TestObserver<Long>()
        booksDao.upsertBookSingle(TestUtil.BOOK_1).subscribeWith(testObserver)
        testObserver.assertError(SQLiteConstraintException::class.java)
        assertThat(testObserver.errors().get(0).message).ignoringCase().contains("foreign key")
    }

    @Test
    fun upsertSingleWithFlowableQuery() {
        val testObserver = TestObserver<Long>()
        val testObserver2 = TestObserver<Long>()
        val subscriber = TestSubscriber<Book>()
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.upsertBookSingle(TestUtil.BOOK_1).subscribeWith(testObserver)
        booksDao.getBookFlowable(TestUtil.BOOK_1.bookId).subscribeWith(subscriber)
        testObserver.assertComplete()
        drain()
        assertThat(subscriber.values().size).isEqualTo(1)
        assertThat(subscriber.values()[0]).isEqualTo(TestUtil.BOOK_1)
        booksDao.upsertBookSingle(TestUtil.BOOK_1.copy(title = "changed title"))
            .subscribeWith(testObserver2)
        drain()
        assertThat(subscriber.values().size).isEqualTo(2)
        assertThat(subscriber.values()[1].title).isEqualTo("changed title")
    }

    @Test
    fun upsertMaybe() {
        val testObserver = TestObserver<Long>()
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(BOOK_1_EDIT)
        booksDao.upsertBookMaybe(TestUtil.BOOK_1).subscribeWith(testObserver)
        testObserver.assertComplete()
        assertThat(booksDao.getBook(TestUtil.BOOK_1.bookId)).isEqualTo(
            TestUtil.BOOK_1
        )
    }

    @Test
    fun upsertMaybeError() {
        val testObserver = TestObserver<Long>()
        booksDao.upsertBookMaybe(TestUtil.BOOK_1).subscribeWith(testObserver)
        testObserver.assertError(SQLiteConstraintException::class.java)
        assertThat(testObserver.errors().get(0).message).ignoringCase().contains("foreign key")
    }

    @Test
    fun upsertMaybeWithFlowableQuery() {
        val testObserver = TestObserver<Long>()
        val testObserver2 = TestObserver<Long>()
        val subscriber = TestSubscriber<Book>()
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.upsertBookMaybe(TestUtil.BOOK_1).subscribeWith(testObserver)
        booksDao.getBookFlowable(TestUtil.BOOK_1.bookId).subscribeWith(subscriber)
        testObserver.assertComplete()
        drain()
        assertThat(subscriber.values().size).isEqualTo(1)
        assertThat(subscriber.values()[0]).isEqualTo(TestUtil.BOOK_1)
        booksDao.upsertBookMaybe(TestUtil.BOOK_1.copy(title = "changed title"))
            .subscribeWith(testObserver2)
        drain()
        assertThat(subscriber.values().size).isEqualTo(2)
        assertThat(subscriber.values()[1].title).isEqualTo("changed title")
    }

    @Test
    fun upsertCompletable() {
        val testObserver = TestObserver<Long>()
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(BOOK_1_EDIT)
        booksDao.upsertBookCompletable(TestUtil.BOOK_1).subscribeWith(testObserver)
        testObserver.assertComplete()
        assertThat(booksDao.getBook(TestUtil.BOOK_1.bookId)).isEqualTo(
            TestUtil.BOOK_1
        )
    }

    @Test
    fun upsertCompletableError() {
        val testObserver = TestObserver<Long>()
        booksDao.upsertBookCompletable(TestUtil.BOOK_1).subscribeWith(testObserver)
        testObserver.assertError(SQLiteConstraintException::class.java)
        assertThat(testObserver.errors().get(0).message).ignoringCase().contains("foreign key")
    }

    @Test
    fun upsertFlowable() {
        booksDao.upsertPublishers(TestUtil.PUBLISHER)
        booksDao.upsertBooks(TestUtil.BOOK_1)
        val subscriber = TestSubscriber<List<Book>>()
        booksDao.getBooksFlowable().subscribeWith(subscriber)
        drain()
        assertThat(subscriber.values().size).isEqualTo(1)
        assertThat(subscriber.values()[0]).isEqualTo(listOf(TestUtil.BOOK_1))
        booksDao.upsertBooks(TestUtil.BOOK_1.copy(title = "changed title"))
        drain()
        assertThat(subscriber.values()[1][0].title).isEqualTo("changed title")
        booksDao.upsertListOfBooksReturnLongArray(buildList {
            add(TestUtil.BOOK_2)
            add(TestUtil.BOOK_3)
        })
        drain()
        assertThat(subscriber.values().size).isEqualTo(3)
        assertThat(subscriber.values()[2]).isEqualTo(
            listOf(TestUtil.BOOK_1.copy(title = "changed title"), TestUtil.BOOK_2, TestUtil.BOOK_3)
        )
    }

    @Test
    fun upsertObservable() {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.upsertBooks(TestUtil.BOOK_1)
        val observer = TestObserver<List<Book>>()
        booksDao.getBooksObservable().subscribeWith(observer)
        drain()
        assertThat(observer.values().size).isEqualTo(1)
        assertThat(observer.values()[0]).isEqualTo(listOf(TestUtil.BOOK_1))
        booksDao.upsertBooks(TestUtil.BOOK_1.copy(title = "changed title"))
        drain()
        assertThat(observer.values()[1][0].title).isEqualTo("changed title")
        booksDao.upsertListOfBooksReturnLongArray(buildList {
            add(TestUtil.BOOK_2)
            add(TestUtil.BOOK_3)
        })
        drain()
        assertThat(observer.values().size).isEqualTo(3)
        assertThat(observer.values()[2]).isEqualTo(
            listOf(TestUtil.BOOK_1.copy(title = "changed title"), TestUtil.BOOK_2, TestUtil.BOOK_3)
        )
    }

    @Test
    fun upsertPartialEntity() {
        val MINI_BOOK_1 = MiniBook(
            "b1", "book title 1", "ph1")

        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.upsertMiniBook(MINI_BOOK_1)

        booksDao.upsertMiniBook(MINI_BOOK_1.copy(title = "changed title"))
        assertThat(booksDao.getBook(MINI_BOOK_1.bookId).title).isEqualTo("changed title")
    }

    @Test
    fun upsertForeignKeyConstraint_failure() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        // TODO: (b/242928508) Fix problem if foreign key continues to be updated when aborted
        val exception = assertFails {
            booksDao.upsertBooks(TestUtil.BOOK_1.copy(bookPublisherId = "phnew"))
        }

        assertThat(exception).hasMessageThat().ignoringCase()
            .contains("foreign key constraint failed")
    }

    private fun <T> upsertReturnTypeBasic(
        upsertMethod: (book: Book) -> T,
        insertedAlready: Boolean = false
    ): T {
        if (insertedAlready) {
            return upsertMethod(TestUtil.BOOK_1.copy(title = "changed title"))
        }
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        return upsertMethod(TestUtil.BOOK_1)
    }

    @Test
    fun upsertBookReturnUnit() {
        assertThat(upsertReturnTypeBasic(booksDao::upsertBooks)).isEqualTo(Unit)
        assertThat(upsertReturnTypeBasic(booksDao::upsertBooks, true)).isEqualTo(Unit)
    }

    @Test
    fun upsertBookReturnLong() {
        assertThat(upsertReturnTypeBasic(booksDao::upsertBookReturnLong))
            .isEqualTo(1)
        assertThat(upsertReturnTypeBasic(booksDao::upsertBookReturnLong, true))
            .isEqualTo(-1)
    }

    @Test
    fun upsertBookReturnList() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)

        assertThat(booksDao.upsertBooksReturnLongList(TestUtil.BOOK_1)).containsExactly(1L)
        assertThat(booksDao.upsertBooksReturnLongList(
            TestUtil.BOOK_1.copy(title = "changed title"))
        )
            .containsExactly(-1L)
    }

    @Test
    fun upsertBookReturnArrayPrimitive() {
        val result = longArrayOf(1)
        assertThat(upsertReturnTypeBasic(booksDao::upsertBooksReturnLongArrayPrimitive))
            .isEqualTo(result)

        val updatedResult = longArrayOf(-1)
        assertThat(upsertReturnTypeBasic(booksDao::upsertBooksReturnLongArrayPrimitive, true))
            .isEqualTo(updatedResult)
    }

    @Test
    fun upsertBookReturnArray() {
        val result = arrayOf<Long>(1)
        assertThat(upsertReturnTypeBasic(booksDao::upsertBooksReturnLongArray)).isEqualTo(result)

        val updatedResult = arrayOf<Long>(-1)
        assertThat(upsertReturnTypeBasic(booksDao::upsertBooksReturnLongArray, true))
            .isEqualTo(updatedResult)
    }

    @Test
    fun upsertBookReturnListenableFuture() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)

        val result = listOf<Long>(1)
        assertThat(booksDao.upsertBooksReturnListenableFuture(TestUtil.BOOK_1).get())
            .isEqualTo(result)

        val updatedResult = listOf<Long>(-1)
        assertThat(booksDao.upsertBooksReturnListenableFuture(
            TestUtil.BOOK_1.copy(title = "changed title")).get())
            .isEqualTo(updatedResult)
    }
}
