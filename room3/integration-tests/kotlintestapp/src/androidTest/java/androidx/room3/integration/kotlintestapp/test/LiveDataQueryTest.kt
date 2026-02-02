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

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.kruth.assertThat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.room3.integration.kotlintestapp.vo.BookWithPublisher
import androidx.test.filters.MediumTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Test

@MediumTest
class LiveDataQueryTest : TestDatabaseTest(UseDriver.ANDROID) {

    @Test
    fun observeBooksById() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val book = awaitValue(booksDao.getBookLiveData(TestUtil.BOOK_1.bookId))

        assertThat(book).isEqualTo(TestUtil.BOOK_1)
    }

    @Test
    fun observeBooksWithPublisher() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)

        val expected =
            BookWithPublisher(TestUtil.BOOK_1.bookId, TestUtil.BOOK_1.title, TestUtil.PUBLISHER)
        val expectedList = ArrayList<BookWithPublisher>()
        expectedList.add(expected)
        val actual = awaitValue(booksDao.getBooksWithPublisherLiveData())
        assertThat(actual).isEqualTo(expectedList)
    }

    @Test
    fun publisherWithBooks() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1, TestUtil.BOOK_2)

        val actualPublisherWithBooks =
            awaitValue(booksDao.getPublisherWithBooksLiveData(TestUtil.PUBLISHER.publisherId))

        assertThat(actualPublisherWithBooks.publisher).isEqualTo(TestUtil.PUBLISHER)
        assertThat(actualPublisherWithBooks.books?.size).isEqualTo(2)
        assertThat(actualPublisherWithBooks.books?.get(0)).isEqualTo(TestUtil.BOOK_1)
        assertThat(actualPublisherWithBooks.books?.get(1)).isEqualTo(TestUtil.BOOK_2)
    }

    private fun <T> awaitValue(liveData: LiveData<T>): T {
        val latch = CountDownLatch(1)
        var data: T? = null
        val observer =
            object : Observer<T> {
                override fun onChanged(value: T) {
                    data = value
                    liveData.removeObserver(this)
                    latch.countDown()
                }
            }
        ArchTaskExecutor.getMainThreadExecutor().execute { liveData.observeForever(observer) }
        latch.await(10, TimeUnit.SECONDS)
        return data!!
    }
}
