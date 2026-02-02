/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.autoclose

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.kruth.assertWithMessage
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
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
        private const val TIMEOUT_AMOUNT = 1L
    }

    private val testCoroutineScope = TestScope()

    private lateinit var autoCloser: AutoCloser
    private lateinit var autoCloseDriver: AutoClosingSQLiteDriver
    private lateinit var testWatch: AutoCloserTestWatch

    private val autoCloseConnections = mutableListOf<TestSQLiteConnection>()
    private var autoCloseCallbackInvoked = 0
    private var autoOpenCallbackInvoked = 0

    @Before
    fun setUp() {
        autoCloseCallbackInvoked = 0
        autoOpenCallbackInvoked = 0

        testWatch = AutoCloserTestWatch(TIMEOUT_AMOUNT, testCoroutineScope.testScheduler)

        autoCloser =
            AutoCloser(
                    config = AutoCloserConfig(TIMEOUT_AMOUNT, TimeUnit.MILLISECONDS),
                    watch = testWatch,
                )
                .apply {
                    initCoroutineScope(testCoroutineScope)
                    setAutoCloseCallback { autoCloseCallbackInvoked++ }
                    setAutoOpenCallback { autoOpenCallbackInvoked++ }
                }

        val realDriver = AndroidSQLiteDriver()
        val testDriver =
            object : SQLiteDriver by realDriver {
                override fun open(fileName: String): SQLiteConnection {
                    return TestSQLiteConnection(realDriver.open(fileName)).also {
                        autoCloseConnections.add(it)
                    }
                }
            }
        autoCloseDriver = AutoClosingSQLiteDriver(autoCloser, testDriver)
    }

    @After
    fun cleanUp() {
        // At the end of all tests we always expect to auto-close the database
        assertWithMessage("Reference count is not zero").that(autoCloser.isOpen()).isFalse()
        assertWithMessage("Database is not closed").that(areAllConnectionsClosed()).isTrue()
    }

    @Test
    fun dbClosedWithRefCountDecremented() = runTest {
        autoCloser.incrementCount()
        val poolConnection = autoCloseDriver.open(":memory:")
        autoCloser.decrementCount()

        testWatch.step()

        assertThat(autoCloser.isOpen()).isFalse()
        assertThat(areAllConnectionsClosed()).isTrue()
        assertThat(autoCloseCallbackInvoked).isEqualTo(1)
        assertThat(autoOpenCallbackInvoked).isEqualTo(0)

        poolConnection.close()
    }

    @Test
    fun dbClosedWithRefCountDecrementedToZero() = runTest {
        autoCloser.incrementCount()
        autoCloser.incrementCount()
        val poolConnection = autoCloseDriver.open(":memory:")
        autoCloser.decrementCount()

        testWatch.step()

        assertThat(autoCloser.isOpen()).isTrue()
        assertThat(areAllConnectionsClosed()).isFalse()

        autoCloser.decrementCount()

        testWatch.step()

        assertThat(autoCloser.isOpen()).isFalse()
        assertThat(areAllConnectionsClosed()).isTrue()

        poolConnection.close()
    }

    @Test
    fun dbNotClosedWithRefCountIncremented() = runTest {
        autoCloser.incrementCount()
        val poolConnection = autoCloseDriver.open(":memory:")

        testWatch.step()

        assertThat(autoCloser.isOpen()).isTrue()
        assertThat(areAllConnectionsClosed()).isFalse()

        autoCloser.decrementCount()

        poolConnection.close()
    }

    @Test
    fun dbNotClosedWithLiveStatement() = runTest {
        autoCloser.incrementCount()
        val poolConnection = autoCloseDriver.open(":memory:")
        val liveStatement = poolConnection.prepare("PRAGMA user_version")
        autoCloser.decrementCount()

        testWatch.step()

        assertThat(autoCloser.isOpen()).isTrue()
        assertThat(areAllConnectionsClosed()).isFalse()

        liveStatement.close()

        testWatch.step()

        assertThat(autoCloser.isOpen()).isFalse()
        assertThat(areAllConnectionsClosed()).isTrue()

        poolConnection.close()
    }

    @Test
    fun dbReOpenedWithRefCountIncremented() = runTest {
        autoCloser.incrementCount()
        val poolConnection = autoCloseDriver.open(":memory:")
        autoCloser.decrementCount()

        testWatch.step()

        autoCloser.incrementCount()
        poolConnection.execSQL("PRAGMA user_version = 1")
        autoCloser.decrementCount()

        assertThat(autoCloseCallbackInvoked).isEqualTo(1)
        assertThat(autoOpenCallbackInvoked).isEqualTo(1)

        testWatch.step()

        assertThat(autoCloseCallbackInvoked).isEqualTo(2)
        assertThat(autoOpenCallbackInvoked).isEqualTo(1)

        poolConnection.close()
    }

    @Test
    fun throwIfInvalidRefCount() = runTest {
        assertThrows<IllegalStateException> { autoCloser.decrementCount() }
            .hasMessageThat()
            .isEqualTo("Unbalanced reference count.")
    }

    @Test
    fun dbCanBeManuallyClosed() = runTest {
        autoCloser.incrementCount()
        val poolConnection = autoCloseDriver.open(":memory:")
        autoCloser.decrementCount()

        poolConnection.close()

        assertThat(autoCloser.isOpen()).isFalse()
        assertThat(areAllConnectionsClosed()).isTrue()
    }

    private fun runTest(testBody: suspend TestScope.() -> Unit) =
        testCoroutineScope.runTest {
            testBody.invoke(this)
            testWatch.step()
        }

    private fun areAllConnectionsClosed() = autoCloseConnections.all { it.isClosed }

    private class TestSQLiteConnection(private val delegate: SQLiteConnection) :
        SQLiteConnection by delegate {
        var isClosed = false

        override fun close() {
            delegate.close()
            isClosed = true
        }
    }
}
