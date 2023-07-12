/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.kruth.assertThat
import androidx.room.invalidationTrackerFlow
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class InvalidationTrackerFlowTest : TestDatabaseTest() {

    @After
    fun teardown() {
        // At the end of all tests, query executor should be idle (transaction thread released).
        countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
        assertThat(countingTaskExecutorRule.isIdle).isTrue()
    }

    @Test
    fun initiallyEmitAllTableNames(): Unit = runBlocking {
        val result = database.invalidationTrackerFlow("author", "publisher", "book")
            .first()
        assertThat(result).containsExactly("author", "publisher", "book")
    }

    @Test
    fun initiallyEmitNothingWhenLazy(): Unit = runBlocking {
        val channel = database.invalidationTrackerFlow(
            "author", "publisher", "book",
            emitInitialState = true
        ).produceIn(this)

        drain()
        yield()

        assertThat(channel.isEmpty)

        channel.cancel()
    }

    @Test
    fun invalidationEmitTableNames(): Unit = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val channel = database.invalidationTrackerFlow("author", "publisher", "book")
            .produceIn(this)

        assertThat(channel.receive()).isEqualTo(setOf("author", "publisher", "book"))

        booksDao.insertBookSuspend(TestUtil.BOOK_2)
        drain() // drain async invalidate
        yield()

        assertThat(channel.receive())
            .containsExactly("book")

        booksDao.addPublisherSuspend(TestUtil.PUBLISHER2)
        drain() // drain async invalidate
        yield()

        assertThat(channel.receive()).containsExactly("publisher")

        assertThat(channel.isEmpty).isTrue()

        channel.cancel()
    }

    @Ignore // b/268534919
    @Test
    fun emitOnceForMultipleTablesInTransaction(): Unit = runBlocking {
        val results = mutableListOf<Set<String>>()
        val latch = CountDownLatch(1)
        val job = async(Dispatchers.IO) {
            database.invalidationTrackerFlow("author", "publisher", "book").collect {
                results.add(it)
                latch.countDown()
            }
        }

        database.withTransaction {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1)
        }
        latch.await()
        job.cancelAndJoin()

        assertThat(results.size).isEqualTo(1)
        assertThat(results.first()).isEqualTo(setOf("author", "publisher", "book"))
    }

    @Test
    fun dropInvalidationUsingConflated() = runBlocking {
        val channel = database.invalidationTrackerFlow("author", "publisher", "book")
            .buffer(Channel.CONFLATED)
            .produceIn(this)

        booksDao.addAuthors(TestUtil.AUTHOR_1)
        drain() // drain async invalidate
        yield()

        booksDao.addPublishers(TestUtil.PUBLISHER)
        drain() // drain async invalidate
        yield()

        booksDao.addBooks(TestUtil.BOOK_1)
        drain() // drain async invalidate
        yield()

        assertThat(channel.receive()).containsExactly("book")
        assertThat(channel.isEmpty).isTrue()

        channel.cancel()
    }

    @Test
    fun collectInTransaction(): Unit = runBlocking {
        database.withTransaction {
            val result = database.invalidationTrackerFlow("author").first()
            assertThat(result).containsExactly("author")
        }
    }

    @Ignore("b/277764166")
    @Test
    fun mapBlockingQuery() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val channel = database.invalidationTrackerFlow("book")
            .map { booksDao.getAllBooks() }
            .produceIn(this)

        assertThat(channel.receive()).containsExactly(TestUtil.BOOK_1)

        booksDao.addBooks(TestUtil.BOOK_2)
        drain() // drain async invalidate
        yield()

        assertThat(channel.receive()).containsExactly(TestUtil.BOOK_1, TestUtil.BOOK_2)

        channel.cancel()
    }

    @Test
    fun mapSuspendingQuery() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val channel = database.invalidationTrackerFlow("book")
            .map { booksDao.getBooksSuspend() }
            .produceIn(this)

        assertThat(channel.receive()).containsExactly(TestUtil.BOOK_1)

        booksDao.addBooks(TestUtil.BOOK_2)
        drain() // drain async invalidate
        yield()

        assertThat(channel.receive()).containsExactly(TestUtil.BOOK_1, TestUtil.BOOK_2)

        channel.cancel()
    }

    @Test
    fun mapFlowQuery() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val channel = database.invalidationTrackerFlow("book")
            .map { booksDao.getBooksFlow().first() }
            .produceIn(this)

        assertThat(channel.receive()).containsExactly(TestUtil.BOOK_1)

        booksDao.addBooks(TestUtil.BOOK_2)
        drain() // drain async invalidate
        yield()

        assertThat(channel.receive()).containsExactly(TestUtil.BOOK_1, TestUtil.BOOK_2)

        channel.cancel()
    }

    @Test
    fun mapTransactionQuery() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val channel = database.invalidationTrackerFlow("book")
            .map {
                database.withTransaction {
                    booksDao.getBooksSuspend()
                }
            }
            .produceIn(this)

        assertThat(channel.receive()).containsExactly(TestUtil.BOOK_1)

        booksDao.addBooks(TestUtil.BOOK_2)
        drain() // drain async invalidate
        yield()

        assertThat(channel.receive()).containsExactly(TestUtil.BOOK_1, TestUtil.BOOK_2)

        channel.cancel()
    }

    @Test
    fun transactionUpdateAndTransactionQuery() = runBlocking {
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val results = mutableListOf<List<String>>()
        val firstResultLatch = CountDownLatch(1)
        val secondResultLatch = CountDownLatch(1)
        val job = async(Dispatchers.IO) {
            database.invalidationTrackerFlow("author", "publisher")
                .map {
                    val (books, publishers) = database.withTransaction {
                        booksDao.getBooksSuspend() to booksDao.getPublishersSuspend()
                    }
                    books.map { book ->
                        val publisherName =
                            publishers.first { it.publisherId == book.bookPublisherId }.name
                        "${book.title} from $publisherName"
                    }
                }.collect {
                    when (results.size) {
                        0 -> {
                            results.add(it)
                            firstResultLatch.countDown()
                        }
                        1 -> {
                            results.add(it)
                            secondResultLatch.countDown()
                        }
                        else -> fail("Should have only collected 2 results.")
                    }
                }
        }

        firstResultLatch.await()
        database.withTransaction {
            booksDao.addPublishers(TestUtil.PUBLISHER2)
            booksDao.addBooks(TestUtil.BOOK_2)
        }

        secondResultLatch.await()
        assertThat(results.size).isEqualTo(2)
        assertThat(results[0]).containsExactly(
            "book title 1 from publisher 1"
        )
        assertThat(results[1]).containsExactly(
            "book title 1 from publisher 1",
            "book title 2 from publisher 1"
        )

        job.cancelAndJoin()
    }

    @Test
    fun invalidTable() = runBlocking {
        val flow = database.invalidationTrackerFlow("foo")
        try {
            flow.first()
            fail("An exception should have thrown")
        } catch (ex: IllegalArgumentException) {
            assertThat(ex.message).isEqualTo("There is no table with name foo")
        }
    }

    @Test
    fun emptyTables() = runBlocking {
        booksDao.addAuthors(TestUtil.AUTHOR_1)

        val channel = database.invalidationTrackerFlow().produceIn(this)

        assertThat(channel.receive()).isEmpty()

        booksDao.addAuthorsSuspend(TestUtil.AUTHOR_2)
        drain() // drain async invalidate
        yield()

        assertThat(channel.isEmpty).isTrue()

        channel.cancel()
    }
}