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

import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.assertThrows
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class AutoCloserTest {

    companion object {
        private const val DB_NAME = "test.db"
        private const val TIMEOUT_AMOUNT = 1L
    }

    private val testCoroutineScope = TestScope()

    private lateinit var autoCloser: AutoCloser
    private lateinit var testWatch: AutoCloserTestWatch
    private lateinit var callback: Callback

    private class Callback(var throwOnOpen: Boolean = false) : SupportSQLiteOpenHelper.Callback(1) {
        override fun onCreate(db: SupportSQLiteDatabase) {}

        override fun onOpen(db: SupportSQLiteDatabase) {
            if (throwOnOpen) {
                throw IOException()
            }
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    @Before
    fun setUp() {
        testWatch = AutoCloserTestWatch(TIMEOUT_AMOUNT, testCoroutineScope.testScheduler)
        callback = Callback()

        val delegateOpenHelper =
            FrameworkSQLiteOpenHelperFactory()
                .create(
                    SupportSQLiteOpenHelper.Configuration.builder(
                            ApplicationProvider.getApplicationContext()
                        )
                        .callback(callback)
                        .name(DB_NAME)
                        .build()
                )

        autoCloser =
            AutoCloser(TIMEOUT_AMOUNT, TimeUnit.MILLISECONDS, testWatch).apply {
                initOpenHelper(delegateOpenHelper)
                initCoroutineScope(testCoroutineScope)
                setAutoCloseCallback {}
            }
    }

    @After
    fun cleanUp() {
        // At the end of all tests we always expect to auto-close the database
        assertWithMessage("Database was not closed").that(autoCloser.delegateDatabase).isNull()
    }

    @Test
    fun refCountsCounted() = runTest {
        autoCloser.incrementCountAndEnsureDbIsOpen()
        assertThat(autoCloser.refCountForTest).isEqualTo(1)

        autoCloser.incrementCountAndEnsureDbIsOpen()
        assertThat(autoCloser.refCountForTest).isEqualTo(2)

        autoCloser.decrementCountAndScheduleClose()
        assertThat(autoCloser.refCountForTest).isEqualTo(1)

        autoCloser.executeRefCountingFunction {
            assertThat(autoCloser.refCountForTest).isEqualTo(2)
        }
        assertThat(autoCloser.refCountForTest).isEqualTo(1)

        autoCloser.decrementCountAndScheduleClose()
        assertThat(autoCloser.refCountForTest).isEqualTo(0)
    }

    @Test
    fun executeRefCountingFunctionPropagatesFailure() = runTest {
        assertThrows<IOException> { autoCloser.executeRefCountingFunction { throw IOException() } }

        assertThat(autoCloser.refCountForTest).isEqualTo(0)
    }

    @Test
    fun dbNotClosedWithRefCountIncremented() = runTest {
        autoCloser.incrementCountAndEnsureDbIsOpen()

        testWatch.step()

        assertThat(autoCloser.delegateDatabase!!.isOpen).isTrue()

        autoCloser.decrementCountAndScheduleClose()
    }

    @Test
    fun getDelegatedDatabaseReturnsUnwrappedDatabase() = runTest {
        assertThat(autoCloser.delegateDatabase).isNull()

        val db = autoCloser.incrementCountAndEnsureDbIsOpen()
        db.beginTransaction()
        // Beginning a transaction on the unwrapped db shouldn't increment our ref count.
        assertThat(autoCloser.refCountForTest).isEqualTo(1)
        db.endTransaction()

        autoCloser.delegateDatabase!!.beginTransaction()
        assertThat(autoCloser.refCountForTest).isEqualTo(1)
        autoCloser.delegateDatabase!!.endTransaction()
        autoCloser.decrementCountAndScheduleClose()

        autoCloser.executeRefCountingFunction {
            assertThat(autoCloser.refCountForTest).isEqualTo(1)
        }
    }

    @Test
    fun refCountStaysIncrementedWhenErrorIsEncountered() = runTest {
        callback.throwOnOpen = true
        assertThrows<IOException> { autoCloser.incrementCountAndEnsureDbIsOpen() }

        assertThat(autoCloser.refCountForTest).isEqualTo(1)

        autoCloser.decrementCountAndScheduleClose()
        callback.throwOnOpen = false
    }

    @Test
    fun testDbCanBeManuallyClosed() = runTest {
        val db = autoCloser.incrementCountAndEnsureDbIsOpen()

        assertThat(db.isOpen).isTrue()

        autoCloser.closeDatabaseIfOpen() // Should succeed...

        assertThat(db.isOpen).isFalse()

        assertThrows<IllegalStateException> { db.query("select * from users").close() }
            .hasMessageThat()
            .contains("closed")

        autoCloser.decrementCountAndScheduleClose() // Should succeed

        assertThrows<IllegalStateException> { autoCloser.incrementCountAndEnsureDbIsOpen() }
    }

    private fun runTest(testBody: suspend TestScope.() -> Unit) =
        testCoroutineScope.runTest {
            testBody.invoke(this)
            testWatch.step()
        }
}
