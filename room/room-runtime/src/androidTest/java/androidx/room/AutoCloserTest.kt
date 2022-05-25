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

package androidx.room

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@MediumTest
public class AutoCloserTest {

    @get:Rule
    public val countingTaskExecutorRule: CountingTaskExecutorRule = CountingTaskExecutorRule()

    private lateinit var autoCloser: AutoCloser

    private lateinit var callback: Callback

    private class Callback(var throwOnOpen: Boolean = false) :
        SupportSQLiteOpenHelper.Callback(1) {
        override fun onCreate(db: SupportSQLiteDatabase) {}

        override fun onOpen(db: SupportSQLiteDatabase) {
            if (throwOnOpen) {
                throw IOException()
            }
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    @Before
    public fun setUp() {
        callback = Callback()

        val delegateOpenHelper = FrameworkSQLiteOpenHelperFactory()
            .create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(ApplicationProvider.getApplicationContext())
                    .callback(callback)
                    .name("name")
                    .build()
            )

        val autoCloseExecutor = ArchTaskExecutor.getIOThreadExecutor()

        autoCloser = AutoCloser(
            1,
            TimeUnit.MILLISECONDS,
            autoCloseExecutor
        ).also {
            it.init(delegateOpenHelper)
            it.setAutoCloseCallback { }
        }
    }

    @After
    public fun cleanUp() {
        assertThat(countingTaskExecutorRule.isIdle).isTrue()
    }

    @Test
    public fun refCountsCounted() {
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

        // TODO(rohitsat): remove these sleeps and add a hook in AutoCloser to confirm that the
        // scheduled tasks are done.
        Thread.sleep(5)
        countingTaskExecutorRule.drainTasks(10, TimeUnit.MILLISECONDS)
    }

    @Test
    public fun executeRefCountingFunctionPropagatesFailure() {
        assertThrows<IOException> {
            autoCloser.executeRefCountingFunction<Nothing> {
                throw IOException()
            }
        }

        assertThat(autoCloser.refCountForTest).isEqualTo(0)

        // TODO(rohitsat): remove these sleeps and add a hook in AutoCloser to confirm that the
        // scheduled tasks are done.
        Thread.sleep(5)
        countingTaskExecutorRule.drainTasks(10, TimeUnit.MILLISECONDS)
    }

    @Test
    @FlakyTest(bugId = 182343970)
    public fun dbNotClosedWithRefCountIncremented() {
        autoCloser.incrementCountAndEnsureDbIsOpen()

        Thread.sleep(10)

        assertThat(autoCloser.delegateDatabase!!.isOpen).isTrue()

        autoCloser.decrementCountAndScheduleClose()

        // TODO(rohitsat): remove these sleeps and add a hook in AutoCloser to confirm that the
        // scheduled tasks are done.
        Thread.sleep(10)
        assertThat(autoCloser.delegateDatabase).isNull()
    }

    @FlakyTest(bugId = 189775887)
    @Test
    public fun getDelegatedDatabaseReturnsUnwrappedDatabase() {
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

        // TODO(rohitsat): remove these sleeps and add a hook in AutoCloser to confirm that the
        // scheduled tasks are done.
        Thread.sleep(5)
        countingTaskExecutorRule.drainTasks(10, TimeUnit.MILLISECONDS)
    }

    @Test
    public fun refCountStaysIncrementedWhenErrorIsEncountered() {
        callback.throwOnOpen = true
        assertThrows<IOException> {
            autoCloser.incrementCountAndEnsureDbIsOpen()
        }

        assertThat(autoCloser.refCountForTest).isEqualTo(1)

        autoCloser.decrementCountAndScheduleClose()
        callback.throwOnOpen = false

        // TODO(rohitsat): remove these sleeps and add a hook in AutoCloser to confirm that the
        // scheduled tasks are done.
        Thread.sleep(5)
        countingTaskExecutorRule.drainTasks(10, TimeUnit.MILLISECONDS)
    }

    @Test
    public fun testDbCanBeManuallyClosed() {
        val db = autoCloser.incrementCountAndEnsureDbIsOpen()

        assertThat(db.isOpen).isTrue()

        autoCloser.closeDatabaseIfOpen() // Should succeed...

        assertThat(db.isOpen).isFalse()

        assertThrows<IllegalStateException> { db.query("select * from users").close() }
            .hasMessageThat().contains("closed")

        autoCloser.decrementCountAndScheduleClose() // Should succeed

        assertThrows<IllegalStateException> {
            autoCloser.incrementCountAndEnsureDbIsOpen()
        }
    }
}
