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
import androidx.kruth.assertThrows
import androidx.room.RoomRawQuery
import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield

abstract class BaseQueryTest {

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
    fun preparedInsertAndDelete() = runTest {
        val dao = db.dao()
        assertThat(dao.insertItem(1)).isEqualTo(1)
        assertThat(dao.getSingleItem().pk).isEqualTo(1)
        assertThat(dao.getSingleItemSkipVerification().pk).isEqualTo(1)
        assertThat(dao.getSingleItemRaw(RoomRawQuery("SELECT * FROM SampleEntity")).pk).isEqualTo(1)
        assertThat(
                dao.getSingleItemRaw(
                        RoomRawQuery(
                            sql = "SELECT * FROM SampleEntity WHERE pk = ?",
                            onBindStatement = { it.bindLong(1, 1) }
                        )
                    )
                    .pk
            )
            .isEqualTo(1)
        assertThat(dao.deleteItem(1)).isEqualTo(1)
        assertThat(dao.deleteItem(1)).isEqualTo(0) // Nothing deleted
        assertThrows<IllegalStateException> { dao.getSingleItem() }
            .hasMessageThat()
            .contains("The query result was empty")
    }

    @Test
    fun emptyResult() = runTest {
        val db = db
        assertThrows<IllegalStateException> { db.dao().getSingleItem() }
            .hasMessageThat()
            .contains("The query result was empty")
    }

    @Test
    fun queryList() = runTest {
        val dao = db.dao()
        dao.insertItem(1)
        dao.insertItem(2)
        dao.insertItem(3)
        val result = dao.getItemList()
        assertThat(result.map { it.pk }).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun transactionDelegate() = runTest {
        val dao = db.dao()
        dao.insertItem(1)
        dao.insertItem(2)
        dao.insertItem(3)

        // Perform multiple delete transaction with error so delete is not committed
        assertThrows<IllegalArgumentException> {
            dao.deleteList(pks = listOf(1L, 2L, 3L), withError = true)
        }
        assertThat(dao.getItemList().map { it.pk }).containsExactly(1L, 2L, 3L)

        // Perform multiple delete in transaction successfully
        dao.deleteList(
            pks = listOf(1L, 3L),
        )
        assertThat(dao.getItemList().map { it.pk }).containsExactly(2L)
    }

    @Test
    fun queryFlow() = runTest {
        val dao = getRoomDatabase().dao()
        dao.insertItem(1)

        val channel = dao.getItemListFlow().produceIn(this)

        assertThat(channel.receive()).containsExactly(SampleEntity(1))

        dao.insertItem(2)
        assertThat(channel.receive())
            .containsExactly(
                SampleEntity(1),
                SampleEntity(2),
            )

        dao.insertItem(3)
        assertThat(channel.receive())
            .containsExactly(
                SampleEntity(1),
                SampleEntity(2),
                SampleEntity(3),
            )

        channel.cancel()
    }

    @Test
    fun insertAndDelete() = runTest {
        val sampleEntity = SampleEntity(1, 1)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity)
        assertThat(dao.getSingleItemWithColumn().pk).isEqualTo(1)

        dao.delete(sampleEntity)
        assertThrows<IllegalStateException> { dao.getSingleItemWithColumn() }
            .hasMessageThat()
            .contains("The query result was empty")
    }

    @Test
    fun insertAndUpdateAndDelete() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntity(1, 2)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity1)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(1)

        dao.update(sampleEntity2)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(2)

        dao.delete(sampleEntity2)
        assertThrows<IllegalStateException> { dao.getSingleItem() }
            .hasMessageThat()
            .contains("The query result was empty")
    }

    @Test
    fun insertAndUpsertAndDelete() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntity(1, 2)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity1)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(1)

        dao.upsert(sampleEntity2)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(2)

        dao.delete(sampleEntity2)
        assertThrows<IllegalStateException> { dao.getSingleItem() }
            .hasMessageThat()
            .contains("The query result was empty")
    }

    @Test
    fun insertMap() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntity2(1, 2)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity1)
        dao.insert(sampleEntity2)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(1)

        val map = dao.getSimpleMapReturnType()
        assertThat(map[sampleEntity1]).isEqualTo(sampleEntity2)
    }

    @Test
    fun insertListMap() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntity2(1, 2)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity1)
        dao.insert(sampleEntity2)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(1)

        val map = dao.getMapReturnTypeWithList()
        assertThat(map[sampleEntity1]).isEqualTo(listOf(sampleEntity2))
    }

    @Test
    fun insertSetMap() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntity2(1, 2)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity1)
        dao.insert(sampleEntity2)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(1)

        val map = dao.getMapReturnTypeWithSet()
        assertThat(map[sampleEntity1]).isEqualTo(setOf(sampleEntity2))
    }

    @Test
    fun mapWithDupeColumns() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntityCopy(1, 2)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity1)
        dao.insert(sampleEntity2)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(1)

        val map = dao.getMapWithDupeColumns()
        assertThat(map[sampleEntity1]).isEqualTo(sampleEntity2)

        val map2 = dao.getMapWithDupeColumnsSkipVerification()
        assertThat(map2[sampleEntity1]).isEqualTo(sampleEntity2)
    }

    @Test
    fun insertNestedMap() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntity2(1, 2)
        val sampleEntity3 = SampleEntity3(1, 2)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity1)
        dao.insert(sampleEntity2)
        dao.insert(sampleEntity3)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(1)

        val map = dao.getSimpleNestedMapReturnType()
        assertThat(map[sampleEntity1]).isEqualTo(mapOf(Pair(sampleEntity2, sampleEntity3)))
    }

    @Test
    fun insertNestedMapColumnMap() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntity2(1, 2)
        val sampleEntity3 = SampleEntity3(1, 2)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity1)
        dao.insert(sampleEntity2)
        dao.insert(sampleEntity3)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(1)

        val map = dao.getSimpleNestedMapColumnMap()
        assertThat(map[sampleEntity1]).isEqualTo(mapOf(Pair(sampleEntity2, sampleEntity3.data3)))
    }

    @Test
    fun combineInsertAndManualWrite() = runTest {
        val db = getRoomDatabase()
        db.useWriterConnection { connection ->
            db.dao().insertItem(1)
            connection.execSQL("INSERT INTO SampleEntity (pk) VALUES (2)")
        }
        db.useReaderConnection { connection ->
            val count =
                connection.usePrepared("SELECT count(*) FROM SampleEntity") {
                    it.step()
                    it.getLong(0)
                }
            assertThat(count).isEqualTo(2)
        }
    }

    @Test
    fun combineQueryAndManualRead() = runTest {
        val db = getRoomDatabase()
        val entity = SampleEntity(1, 10)
        db.dao().insert(entity)
        db.useReaderConnection { connection ->
            assertThat(db.dao().getItemList()).containsExactly(entity)
            assertThat(
                    connection.usePrepared("SELECT * FROM SampleEntity") {
                        buildList {
                            while (it.step()) {
                                add(SampleEntity(it.getLong(0), it.getLong(1)))
                            }
                        }
                    }
                )
                .containsExactly(entity)
        }
    }

    @Test
    fun queriesAreIsolated() = runTest {
        val db = getRoomDatabase()
        db.dao().insertItem(22)

        // Validates that Room's coroutine scope provides isolation, if one query fails
        // it doesn't affect others.
        val failureQueryScope = CoroutineScope(Job())
        val successQueryScope = CoroutineScope(Job())
        val failureDeferred =
            failureQueryScope.async {
                db.useReaderConnection { connection ->
                    connection.usePrepared("SELECT * FROM WrongTableName") {
                        assertThat(it.step()).isFalse()
                    }
                }
            }
        val successDeferred =
            successQueryScope.async {
                db.useReaderConnection { connection ->
                    connection.usePrepared("SELECT * FROM SampleEntity") {
                        assertThat(it.step()).isTrue()
                        it.getLong(0)
                    }
                }
            }
        assertThrows<SQLiteException> { failureDeferred.await() }
            .hasMessageThat()
            .contains("no such table: WrongTableName")
        assertThat(successDeferred.await()).isEqualTo(22)
    }

    @Test
    fun queriesAreIsolatedWhenCancelled() = runTest {
        val db = getRoomDatabase()

        // Validates that Room's coroutine scope provides isolation, if scope doing a query is
        // cancelled it doesn't affect others.
        val toBeCancelledScope = CoroutineScope(Job())
        val notCancelledScope = CoroutineScope(Job())
        val latch = Mutex(locked = true)
        val cancelledDeferred =
            toBeCancelledScope.async {
                db.useReaderConnection { latch.withLock {} }
                1
            }
        val notCancelledDeferred =
            notCancelledScope.async {
                db.useReaderConnection { latch.withLock {} }
                1
            }

        yield()
        toBeCancelledScope.cancel()
        latch.unlock()

        assertThrows<CancellationException> { cancelledDeferred.await() }
        assertThat(notCancelledDeferred.await()).isEqualTo(1)
    }

    @Test
    fun queryFlowFromManualWrite() = runTest {
        val db = getRoomDatabase()

        val channel = db.dao().getItemListFlow().produceIn(this)

        assertThat(channel.receive()).isEmpty()

        // Validates that a write using the connection directly will cause invalidation when
        // a refresh is requested.
        db.useWriterConnection { connection ->
            connection.execSQL("INSERT INTO SampleEntity (pk) VALUES (13)")
        }
        db.invalidationTracker.refreshAsync()
        assertThat(channel.receive())
            .containsExactly(
                SampleEntity(13),
            )

        channel.cancel()
    }

    @Test
    fun rollbackDaoQuery() = runTest {
        val db = getRoomDatabase()
        db.dao().insertItem(1)
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                db.dao().insertItem(2)
                rollback(Unit)
            }
            val count =
                transactor.usePrepared("SELECT count(*) FROM SampleEntity") {
                    it.step()
                    it.getLong(0)
                }
            assertThat(count).isEqualTo(1)
        }
        assertThat(db.dao().getItemList()).containsExactly(SampleEntity(1))
    }

    @Test
    fun insertAndDeleteArray() = runTest {
        val entityArray = arrayOf(SampleEntity(1, 1), SampleEntity(2, 2))
        val dao = getRoomDatabase().dao()

        dao.insertArray(entityArray)

        val result = dao.getItemArray()
        assertThat(result[0].pk).isEqualTo(1)
        assertThat(result[1].pk).isEqualTo(2)

        dao.deleteArray(entityArray)
        assertThrows<IllegalStateException> { dao.getSingleItemWithColumn() }
            .hasMessageThat()
            .contains("The query result was empty")
    }

    @Test
    fun insertAndReadArrays() = runTest {
        val expected = arrayOf(SampleEntity(1, 1), SampleEntity(2, 2))
        val dao = getRoomDatabase().dao()
        dao.insertArray(expected)

        val resultArray = dao.queryOfArray()
        val resultArrayWithLong = dao.queryOfArrayWithLong()
        val resultLongArray = dao.queryOfLongArray()

        assertContentEquals(expected, resultArray)
        assertContentEquals(arrayOf(1, 2), resultArrayWithLong)
        assertContentEquals(longArrayOf(1, 2), resultLongArray)
    }

    @Test
    fun relation1to1() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntity2(1, 2)
        db.dao().insert(sampleEntity1)
        db.dao().insert(sampleEntity2)
        assertThat(db.dao().getSample1To2())
            .isEqualTo(SampleDao.Sample1And2(sample1 = sampleEntity1, sample2 = sampleEntity2))
    }

    @Test
    fun relationByteKey() = runTest {
        val sampleEntity1 = SampleEntity1Byte(ByteArray(1))
        val sampleEntity2 = SampleEntity2Byte(ByteArray(1))
        db.dao().insert(sampleEntity1)
        db.dao().insert(sampleEntity2)
        assertThat(db.dao().getSample1To2Byte())
            .isEqualTo(SampleDao.Sample1And2Byte(sample1 = sampleEntity1, sample2 = sampleEntity2))
    }

    @Test
    fun relation1toMany() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntity2(1, 2)
        val sampleEntity2s = listOf(sampleEntity2, SampleEntity2(2, 3))

        db.dao().insert(sampleEntity1)
        db.dao().insertSampleEntity2List(sampleEntity2s)

        assertThat(db.dao().getSample1ToMany())
            .isEqualTo(
                SampleDao.Sample1AndMany(sample1 = sampleEntity1, sample2s = listOf(sampleEntity2))
            )
    }

    @Test
    fun relationManytoMany() = runTest {
        val sampleEntity1 = StringSampleEntity1("1", "1")
        val sampleEntity1s = listOf(sampleEntity1, StringSampleEntity1("2", "2"))

        val sampleEntity2 = StringSampleEntity2("1", "1")
        val sampleEntity2s = listOf(sampleEntity2, StringSampleEntity2("2", "2"))

        db.dao().insertSampleEntity1WithString(sampleEntity1s)
        db.dao().insertSampleEntity2WithString(sampleEntity2s)

        assertThat(db.dao().getSampleManyToMany())
            .isEqualTo(SampleDao.SampleManyAndMany(sample1 = sampleEntity1, sample2s = listOf()))
    }

    @Test
    fun invalidRawQueryOnBindStatement() = runTest {
        val query =
            RoomRawQuery(sql = "SELECT * FROM SampleEntity", onBindStatement = { it.step() })
        assertThrows<IllegalStateException> { db.dao().getSingleItemRaw(query) }
            .hasMessageThat()
            .contains("Only bind*() calls are allowed")
    }
}
