/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Callable
import kotlin.coroutines.ContinuationInterceptor

@RunWith(JUnit4::class)
class CoroutinesRoomTest {

    private val database = TestDatabase()
    private val invalidationTracker = database.invalidationTracker as TestInvalidationTracker

    @Test
    fun testCreateFlow() = testRun {
        var callableExecuted = false
        val expectedResult = Any()
        val flow = CoroutinesRoom.createFlow(
            db = database,
            inTransaction = false,
            tableNames = arrayOf("Pet"),
            callable = Callable {
                callableExecuted = true
                expectedResult
            }
        )

        assertThat(invalidationTracker.observers.isEmpty()).isTrue()
        assertThat(callableExecuted).isFalse()

        val job = async {
            flow.first()
        }
        yield(); yield() // yield for async and flow

        assertThat(invalidationTracker.observers.size).isEqualTo(1)
        assertThat(callableExecuted).isTrue()

        assertThat(job.await()).isEqualTo(expectedResult)
        assertThat(invalidationTracker.observers.isEmpty()).isTrue()
    }

    // Use runBlocking dispatcher as query dispatchers, keeps the tests consistent.
    private fun testRun(block: suspend CoroutineScope.() -> Unit) = runBlocking {
        database.backingFieldMap["QueryDispatcher"] = coroutineContext[ContinuationInterceptor]
        block.invoke(this)
    }

    private class TestDatabase : RoomDatabase() {
        override fun createOpenHelper(config: DatabaseConfiguration?): SupportSQLiteOpenHelper {
            throw UnsupportedOperationException("Shouldn't be called!")
        }

        override fun createInvalidationTracker(): InvalidationTracker {
            return TestInvalidationTracker(this)
        }

        override fun clearAllTables() {
            throw UnsupportedOperationException("Shouldn't be called!")
        }
    }

    private class TestInvalidationTracker(db: RoomDatabase) : InvalidationTracker(db) {
        val observers = mutableListOf<Observer>()

        override fun addObserver(observer: Observer) {
            observers.add(observer)
        }

        override fun removeObserver(observer: Observer) {
            observers.remove(observer)
        }
    }
}
