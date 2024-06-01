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

package androidx.room.coroutines

import androidx.kruth.assertThat
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TransactionElement
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TransactionCoroutineTest {

    private val database =
        Room.inMemoryDatabaseBuilder<TestDatabase>(
                InstrumentationRegistry.getInstrumentation().context
            )
            .build()

    @Test
    fun transactionDaoFunctionHasElement() = runTest {
        database.getDao().suspendingTransaction {
            assertThat(database.inTransaction()).isTrue()
            // Validate that in compatibility mode (no driver) a coroutine database transaction
            // from a DAO will utilize the Android's transaction coroutine element for confinement.
            assertTransactionElement()
        }
    }

    private suspend fun assertTransactionElement() {
        assertThat(coroutineContext[TransactionElement]).isNotNull()
    }

    @Database(entities = [TestEntity::class], version = 1, exportSchema = false)
    abstract class TestDatabase : RoomDatabase() {
        abstract fun getDao(): TestDao
    }

    @Entity data class TestEntity(@PrimaryKey val id: Long)

    @Dao
    interface TestDao {
        @Transaction
        suspend fun suspendingTransaction(block: suspend () -> Unit) {
            block.invoke()
        }
    }
}
