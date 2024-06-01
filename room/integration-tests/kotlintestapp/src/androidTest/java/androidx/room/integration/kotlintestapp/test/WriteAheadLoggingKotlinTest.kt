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

import android.content.Context
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.integration.kotlintestapp.TestDatabase
import androidx.room.integration.kotlintestapp.dao.BooksDao
import androidx.room.integration.kotlintestapp.test.TestUtil.Companion.BOOK_1
import androidx.room.integration.kotlintestapp.vo.Book
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import junit.framework.TestCase.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
class WriteAheadLoggingKotlinTest {
    @get:Rule val countingTaskExecutorRule = CountingTaskExecutorRule()

    private suspend fun withDb(fn: suspend (TestDatabase) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "observe.db"
        context.deleteDatabase(dbName)
        val db =
            Room.databaseBuilder(context, TestDatabase::class.java, dbName)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        try {
            fn(db)
        } finally {
            countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
            assertTrue(countingTaskExecutorRule.isIdle)

            db.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun runDbTest(fn: suspend (TestDatabase) -> Unit) = runTest { withDb(fn) }

    @Test
    fun observeLiveData() = runDbTest { db ->
        val dao: BooksDao = db.booksDao()
        dao.insertPublisherSuspend(TestUtil.PUBLISHER.publisherId, TestUtil.PUBLISHER.name)

        val booksSeen = MutableStateFlow<Book?>(null)
        val liveData = dao.getBookLiveData(BOOK_1.bookId)

        val observer = Observer<Book> { booksSeen.value = it }

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            liveData.observeForever(observer)
        }

        dao.insertBookSuspend(BOOK_1)

        val firstBookSeen =
            withContext(Dispatchers.Default) {
                withTimeout(3000) { booksSeen.filterNotNull().first() }
            }

        assertEquals(BOOK_1.bookId, firstBookSeen.bookId)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            liveData.removeObserver(observer)
        }
    }
}
