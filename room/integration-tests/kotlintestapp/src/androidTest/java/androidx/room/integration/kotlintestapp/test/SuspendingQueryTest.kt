/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.room.integration.kotlintestapp.NewThreadDispatcher
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SuspendingQueryTest : TestDatabaseTest() {
    @Test
    fun bookByIdSuspend() {
        runBlocking {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1)

            assertThat(
                booksDao.getBookSuspend(TestUtil.BOOK_1.bookId),
                `is`<Book>(TestUtil.BOOK_1)
            )
        }
    }

    @Test
    fun allBookSuspend() {
        runBlocking {
            booksDao.addAuthors(TestUtil.AUTHOR_1)
            booksDao.addPublishers(TestUtil.PUBLISHER)
            booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

            val books = booksDao.getBooksSuspend()

            assertThat(books.size, `is`(2))
            assertThat(books[0], `is`<Book>(TestUtil.BOOK_1))
            assertThat(books[1], `is`<Book>(TestUtil.BOOK_2))
        }
    }

    @Test
    fun suspendingTransaction() {
        runBlocking(NewThreadDispatcher()) {
            try {
                database.beginTransaction()
                booksDao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId,
                    TestUtil.PUBLISHER.name)
                booksDao.insertBookSuspend(TestUtil.BOOK_1.copy(
                    salesCnt = 0
                ))
                booksDao.insertBookSuspend(TestUtil.BOOK_2)

                booksDao.deleteUnsoldBooks()
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
        runBlocking(NewThreadDispatcher()) {
            assertThat(booksDao.getBooksSuspend(), `is`(listOf(TestUtil.BOOK_2)))
        }
    }
}