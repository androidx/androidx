/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.support

import android.annotation.SuppressLint
import android.content.Context
import androidx.kruth.assertWithMessage
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutoClosingRoomOpenHelperFactoryTest {

    companion object {
        private const val DB_NAME = "test.db"
        private const val TIMEOUT_AMOUNT = 10L
    }

    private val testCoroutineScope = TestScope()

    private lateinit var autoCloser: AutoCloser
    private lateinit var testWatch: AutoCloserTestWatch
    private lateinit var autoClosingRoomOpenHelperFactory: AutoClosingRoomOpenHelperFactory

    @Before
    fun setUp() {
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(DB_NAME)

        testWatch = AutoCloserTestWatch(TIMEOUT_AMOUNT, testCoroutineScope.testScheduler)
        autoCloser =
            AutoCloser(TIMEOUT_AMOUNT, TimeUnit.MILLISECONDS, testWatch).apply {
                initCoroutineScope(testCoroutineScope)
                setAutoCloseCallback {}
            }
        autoClosingRoomOpenHelperFactory =
            AutoClosingRoomOpenHelperFactory(
                delegate = FrameworkSQLiteOpenHelperFactory(),
                autoCloser = autoCloser
            )
    }

    @After
    fun cleanUp() {
        // At the end of all tests we always expect to auto-close the database
        assertWithMessage("Database was not closed").that(autoCloser.delegateDatabase).isNull()
    }

    @Test
    fun testCallbacksCalled() = runTest {
        val callbackCount = AtomicInteger()

        val countingCallback =
            object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    callbackCount.incrementAndGet()
                }

                override fun onConfigure(db: SupportSQLiteDatabase) {
                    callbackCount.incrementAndGet()
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    callbackCount.incrementAndGet()
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {}
            }

        val autoClosingRoomOpenHelper =
            autoClosingRoomOpenHelperFactory.create(
                SupportSQLiteOpenHelper.Configuration.builder(
                        ApplicationProvider.getApplicationContext()
                    )
                    .callback(countingCallback)
                    .name(DB_NAME)
                    .build()
            )

        autoClosingRoomOpenHelper.writableDatabase

        // onConfigure + onCreate + onOpen
        assertEquals(3, callbackCount.get())

        testWatch.step()

        autoClosingRoomOpenHelper.writableDatabase

        // onCreate won't be called the second time.
        // onConfigure + onCreate + onOpen + onConfigure + onOpen
        assertEquals(5, callbackCount.get())
    }

    @Test
    fun testDatabaseIsOpenForSlowCallbacks() = runTest {
        val refCountCheckingCallback =
            object : SupportSQLiteOpenHelper.Callback(1) {
                @SuppressLint("BanThreadSleep")
                override fun onCreate(db: SupportSQLiteDatabase) {
                    testWatch.step()
                    db.execSQL("create table user (idk int)")
                }

                @SuppressLint("BanThreadSleep")
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    testWatch.step()
                    db.setMaximumSize(100000)
                }

                @SuppressLint("BanThreadSleep")
                override fun onOpen(db: SupportSQLiteDatabase) {
                    testWatch.step()
                    db.execSQL("select * from user")
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {}
            }

        val autoClosingRoomOpenHelper =
            autoClosingRoomOpenHelperFactory.create(
                SupportSQLiteOpenHelper.Configuration.builder(
                        ApplicationProvider.getApplicationContext()
                    )
                    .callback(refCountCheckingCallback)
                    .name(DB_NAME)
                    .build()
            )

        val db = autoClosingRoomOpenHelper.writableDatabase
        assertTrue(db.isOpen)
    }

    private fun runTest(testBody: suspend TestScope.() -> Unit) =
        testCoroutineScope.runTest {
            testBody.invoke(this)
            testWatch.step()
        }
}
