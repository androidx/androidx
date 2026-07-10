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

import android.content.Context
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.room3.Room
import androidx.room3.integration.kotlintestapp.TestDatabase
import androidx.room3.integration.kotlintestapp.dao.BooksDao
import androidx.room3.integration.kotlintestapp.dao.UsersDao
import androidx.room3.integration.kotlintestapp.testutil.TestObserver
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule

abstract class TestDatabaseTest(
    protected val useDriver: UseDriver = UseDriver.ANDROID,
    protected val useInMemoryDatabase: Boolean = true,
) {
    @get:Rule val countingTaskExecutorRule = CountingTaskExecutorRule()
    protected lateinit var database: TestDatabase
    protected lateinit var booksDao: BooksDao
    protected lateinit var usersDao: UsersDao

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        if (!useInMemoryDatabase) {
            context.deleteDatabase("test.db")
        }
        database =
            if (useInMemoryDatabase) {
                    Room.inMemoryDatabaseBuilder<TestDatabase>(context)
                } else {
                    Room.databaseBuilder(context, "test.db")
                }
                .setDriver(
                    when (useDriver) {
                        UseDriver.ANDROID -> AndroidSQLiteDriver()
                        UseDriver.BUNDLED -> BundledSQLiteDriver()
                    }
                )
                .setQueryCoroutineContext(
                    ArchTaskExecutor.getIOThreadExecutor().asCoroutineDispatcher()
                )
                .build()

        booksDao = database.booksDao()
        usersDao = database.usersDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    fun drain() {
        countingTaskExecutorRule.drainTasks(10, TimeUnit.SECONDS)
    }

    inner class LiveDataTestObserver<T> : TestObserver<T>() {
        override fun drain() {
            // Drain the background threads for things like invalidation
            countingTaskExecutorRule.drainTasks(1, TimeUnit.MINUTES)
            // Queue up and wait for a no-op main thread function to 'flush' the main thread,
            // important because LiveData things happen in the main thread.
            InstrumentationRegistry.getInstrumentation().runOnMainSync {}
        }
    }

    enum class UseDriver {
        ANDROID,
        BUNDLED,
    }
}
