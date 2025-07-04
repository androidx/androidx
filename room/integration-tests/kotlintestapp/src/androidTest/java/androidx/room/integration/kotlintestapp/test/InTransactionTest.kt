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

package androidx.room.integration.kotlintestapp.test

import android.database.sqlite.SQLiteTransactionListener
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.integration.kotlintestapp.TestDatabase
import androidx.room.integration.kotlintestapp.vo.Publisher
import androidx.sqlite.SQLiteException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import kotlin.test.fail
import org.junit.Test

@SmallTest
class InTransactionTest {

    @Test
    fun inTransaction_alreadyClosed() {
        var onOpenCalled = 0
        val database =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            onOpenCalled++
                        }
                    }
                )
                .build()
        assertThat(onOpenCalled).isEqualTo(0)
        database.close()
        assertThat(database.inTransaction()).isFalse()
        assertThat(onOpenCalled).isEqualTo(0)
    }

    @Test
    fun beginTransactionWithListener_commit() {
        val publisher = Publisher("p1", "pub1")
        val roomDb =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .build()
        val supportDb = roomDb.openHelper.writableDatabase
        supportDb.beginTransactionWithListenerNonExclusive(
            object : SQLiteTransactionListener {
                override fun onBegin() {
                    // TODO(b/408279360): Assert this case once fixed in framework.
                    // We do not assert that `inTransaction()` is true here because at the time this
                    // callback is invoked the transaction stack has not been set and the API
                    // will return false. Meanwhile starting a transaction here will fail due to
                    // the recursive invocation of beginTransaction(), i.e. the database is
                    // neither in a transaction nor once can be started at this time.
                }

                override fun onCommit() {
                    assertThat(supportDb.inTransaction()).isTrue()
                    roomDb.booksDao().insertPublisher(publisher.publisherId, publisher.name)
                }

                override fun onRollback() {
                    fail("onRollback should not be called")
                }
            }
        )
        supportDb.setTransactionSuccessful()
        supportDb.endTransaction()

        assertThat(roomDb.booksDao().getPublishers()).containsExactly(publisher)
        roomDb.close()
    }

    @Test
    fun beginTransactionWithListener_rollback() {
        val roomDb =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .build()
        val supportDb = roomDb.openHelper.writableDatabase
        supportDb.beginTransactionWithListenerNonExclusive(
            object : SQLiteTransactionListener {
                override fun onBegin() {}

                override fun onCommit() {
                    fail("onCommit should not be called")
                }

                override fun onRollback() {
                    assertThat(supportDb.inTransaction()).isTrue()
                    roomDb.booksDao().insertPublisher("p1", "pub1")
                }
            }
        )
        supportDb.endTransaction()

        assertThat(roomDb.booksDao().getPublishers()).isEmpty()
        roomDb.close()
    }

    @Test
    fun daoTransactionBlock() {
        val roomDb =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .build()
        val supportDb = roomDb.openHelper.writableDatabase
        roomDb.booksDao().executeTransaction {
            assertThat(roomDb.inTransaction()).isTrue()
            assertThat(supportDb.inTransaction()).isTrue()
        }
    }

    @Test
    fun daoTransactionBlock_withDriver() {
        val publisher = Publisher("p1", "pub1")

        val roomDb =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .setDriver(BundledSQLiteDriver())
                .build()
        roomDb.booksDao().executeTransaction {
            roomDb.booksDao().insertPublisher(publisher.publisherId, publisher.name)
        }
        assertThat(roomDb.booksDao().getPublishers()).containsExactly(publisher)

        assertThrows<SQLiteException> {
            roomDb.booksDao().executeTransaction {
                // will fails due to non unique id
                roomDb.booksDao().insertPublisher(publisher.publisherId, publisher.name)
            }
        }
        assertThat(roomDb.booksDao().getPublishers()).containsExactly(publisher)

        roomDb.close()
    }

    @Test
    fun runInTransaction_withDriver() {
        val publisher = Publisher("p1", "pub1")

        val roomDb =
            Room.inMemoryDatabaseBuilder<TestDatabase>(ApplicationProvider.getApplicationContext())
                .setDriver(BundledSQLiteDriver())
                .build()
        roomDb.runInTransaction {
            roomDb.booksDao().insertPublisher(publisher.publisherId, publisher.name)
        }
        assertThat(roomDb.booksDao().getPublishers()).containsExactly(publisher)

        assertThrows<SQLiteException> {
            roomDb.runInTransaction {
                // will fails due to non unique id
                roomDb.booksDao().insertPublisher(publisher.publisherId, publisher.name)
            }
        }
        assertThat(roomDb.booksDao().getPublishers()).containsExactly(publisher)

        roomDb.close()
    }
}
