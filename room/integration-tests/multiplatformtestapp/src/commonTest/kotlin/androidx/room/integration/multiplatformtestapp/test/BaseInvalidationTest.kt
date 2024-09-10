/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.room.InvalidationTracker
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

abstract class BaseInvalidationTest {

    private lateinit var db: SampleDatabase

    abstract fun getRoomDatabase(): SampleDatabase

    @BeforeTest
    fun before() {
        db = getRoomDatabase()
    }

    @AfterTest
    fun after() {
        db.close()
    }

    @Test
    fun observeOneTable(): Unit = runBlocking {
        val dao = db.dao()

        val tableName = SampleEntity::class.simpleName!!
        val observer = LatchObserver(tableName)

        db.invalidationTracker.subscribe(observer)

        dao.insertItem(1)

        assertThat(observer.await()).isTrue()
        assertThat(observer.invalidatedTables).containsExactly(tableName)

        observer.reset()
        db.invalidationTracker.unsubscribe(observer)

        dao.insertItem(2)

        assertThat(observer.await()).isFalse()
        assertThat(observer.invalidatedTables).isNull()
    }

    private class LatchObserver(table: String) : InvalidationTracker.Observer(table) {

        var invalidatedTables: Set<String>? = null
            private set

        private var latch = Mutex(locked = true)

        override fun onInvalidated(tables: Set<String>) {
            invalidatedTables = tables
            latch.unlock()
        }

        suspend fun await(): Boolean {
            try {
                withTimeout(200) { latch.withLock {} }
                return true
            } catch (ex: TimeoutCancellationException) {
                return false
            }
        }

        fun reset() {
            invalidatedTables = null
            latch = Mutex(locked = true)
        }
    }
}
