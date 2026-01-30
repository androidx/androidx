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

package androidx.room3.integration.kotlintestapp.test

import androidx.test.filters.SmallTest
import io.reactivex.rxjava3.observers.TestObserver
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@SmallTest
@RunWith(Parameterized::class)
class DeferredBooksDaoTest(useDriver: UseDriver) : TestDatabaseTest(useDriver) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.BUNDLED, UseDriver.ANDROID)
    }

    @Before
    fun setup() {
        booksDao.addAuthors(TestUtil.AUTHOR_1)
        booksDao.addPublishers(TestUtil.PUBLISHER)
        booksDao.addBooks(TestUtil.BOOK_1)
    }

    @Test
    fun increaseBookSales() {
        booksDao.increaseBookSales(TestUtil.BOOK_1.bookId)
        assertThat(
            booksDao.getBook(TestUtil.BOOK_1.bookId).salesCnt,
            `is`(TestUtil.BOOK_1.salesCnt + 1),
        )
    }

    @Test
    fun increaseBookSalesSuspend() = runTest {
        booksDao.increaseBookSalesSuspend(TestUtil.BOOK_1.bookId)
        assertThat(
            booksDao.getBookSuspend(TestUtil.BOOK_1.bookId).salesCnt,
            `is`(TestUtil.BOOK_1.salesCnt + 1),
        )
    }

    @Test
    fun increaseBookSalesSingle() {
        val testObserver =
            booksDao.increaseBookSalesSingle(TestUtil.BOOK_1.bookId).subscribeWith(TestObserver())
        drain()
        testObserver.assertComplete()
        testObserver.assertValue(1)
    }

    @Test
    fun increaseBookSalesMaybe() {
        val testObserver =
            booksDao.increaseBookSalesMaybe(TestUtil.BOOK_1.bookId).subscribeWith(TestObserver())
        drain()
        testObserver.assertComplete()
        testObserver.assertValue(1)
    }

    @Test
    fun increaseBookSalesCompletable() {
        val testObserver =
            booksDao
                .increaseBookSalesCompletable(TestUtil.BOOK_1.bookId)
                .subscribeWith(TestObserver<Int>())
        drain()
        testObserver.assertComplete()
    }

    @Test
    fun increaseBookSalesFuture() {
        assertThat(booksDao.increaseBookSalesFuture(TestUtil.BOOK_1.bookId).get(), `is`(1))
    }

    @Test
    fun increaseBookSalesVoidFuture() {
        booksDao.increaseBookSalesVoidFuture(TestUtil.BOOK_1.bookId).get()
        assertThat(
            booksDao.getBook(TestUtil.BOOK_1.bookId).salesCnt,
            `is`(TestUtil.BOOK_1.salesCnt + 1),
        )
    }

    @Test
    fun deleteUnsoldBooks() {
        assertThat(booksDao.deleteUnsoldBooks(), `is`(0))
    }

    @Test
    fun deleteUnsoldBooksSuspend() {
        runBlocking { assertThat(booksDao.deleteUnsoldBooksSuspend(), `is`(0)) }
    }

    @Test
    fun deleteUnsoldBooksSingle() {
        val testObserver = booksDao.deleteUnsoldBooksSingle().subscribeWith(TestObserver())
        drain()
        testObserver.assertComplete()
        testObserver.assertValue(0)
    }

    @Test
    fun deleteUnsoldBooksMaybe() {
        val testObserver = booksDao.deleteUnsoldBooksMaybe().subscribeWith(TestObserver())
        drain()
        testObserver.assertComplete()
        testObserver.assertValue(0)
    }

    @Test
    fun deleteUnsoldBooksCompletable() {
        val testObserver =
            booksDao.deleteUnsoldBooksCompletable().subscribeWith(TestObserver<Int>())
        drain()
        testObserver.assertComplete()
    }

    @Test
    fun deleteUnsoldBooksFuture() {
        assertThat(booksDao.deleteUnsoldBooksFuture().get(), `is`(0))
    }

    @Test
    fun deleteUnsoldBooksVoidFuture() {
        booksDao.deleteUnsoldBooksVoidFuture().get()
        assertThat(booksDao.getBook(TestUtil.BOOK_1.bookId), notNullValue())
    }

    @Test
    fun deleteBookWithIds() {
        booksDao.deleteBookWithIds(TestUtil.BOOK_1.bookId)
        assertThat(booksDao.getBookNullable(TestUtil.BOOK_1.bookId), nullValue())
    }

    @Test
    fun deleteBookWithIdsSuspend() = runTest {
        booksDao.deleteBookWithIdsSuspend(TestUtil.BOOK_1.bookId)
        assertThat(booksDao.getBookNullableSuspend(TestUtil.BOOK_1.bookId), nullValue())
    }

    @Test
    fun deleteBookWithIdsSingle() {
        val testObserver =
            booksDao.deleteBookWithIdsSingle(TestUtil.BOOK_1.bookId).subscribeWith(TestObserver())
        drain()
        testObserver.assertComplete()
        testObserver.assertValue(1)
    }

    @Test
    fun deleteBookWithIdsMaybe() {
        val testObserver =
            booksDao.deleteBookWithIdsMaybe(TestUtil.BOOK_1.bookId).subscribeWith(TestObserver())
        drain()
        testObserver.assertComplete()
        testObserver.assertValue(1)
    }

    @Test
    fun deleteBookWithIdsCompletable() {
        val testObserver =
            booksDao
                .deleteBookWithIdsCompletable(TestUtil.BOOK_1.bookId)
                .subscribeWith(TestObserver<Int>())
        drain()
        testObserver.assertComplete()
    }

    @Test
    fun deleteBookWithIdsFuture() {
        assertThat(booksDao.deleteBookWithIdsFuture(TestUtil.BOOK_1.bookId).get(), `is`(1))
    }

    @Test
    fun insertPublisher() {
        val rowid = booksDao.insertPublisher("ph3", "publisher 3")
        assertThat(booksDao.getPublisher(rowid), notNullValue())
    }

    @Test
    fun insertPublisherVoid() {
        booksDao.insertPublisher("ph3", "publisher 3")
        assertThat(booksDao.getPublisher("ph3"), notNullValue())
    }

    @Test
    fun insertPublisherSuspend() {
        runBlocking {
            val rowid = booksDao.insertPublisherSuspend("ph3", "publisher 3")
            assertThat(booksDao.getPublisher(rowid), notNullValue())
        }
    }

    @Test
    fun insertPublisherSingle() {
        val testObserver =
            booksDao.insertPublisherSingle("ph3", "publisher 3").subscribeWith(TestObserver())
        drain()
        testObserver.assertComplete()
        assertThat(booksDao.getPublisher(testObserver.values()[0]), notNullValue())
    }

    @Test
    fun insertPublisherMaybe() {
        val testObserver =
            booksDao.insertPublisherMaybe("ph3", "publisher 3").subscribeWith(TestObserver())
        drain()
        testObserver.assertComplete()
        assertThat(booksDao.getPublisher(testObserver.values()[0]), notNullValue())
    }

    @Test
    fun insertPublisherCompletable() {
        val testObserver =
            booksDao
                .insertPublisherCompletable("ph3", "publisher 3")
                .subscribeWith(TestObserver<Long>())
        drain()
        testObserver.assertComplete()
    }

    @Test
    fun insertPublisherFuture() {
        val rowid = booksDao.insertPublisherFuture("ph3", "publisher 3").get()
        assertThat(booksDao.getPublisher(rowid), notNullValue())
    }
}
