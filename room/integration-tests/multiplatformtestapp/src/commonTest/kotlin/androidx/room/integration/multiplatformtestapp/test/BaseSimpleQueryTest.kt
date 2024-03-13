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
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

abstract class BaseSimpleQueryTest {

    abstract fun getRoomDatabase(): SampleDatabase

    @Test
    fun preparedInsertAndDelete() = runTest {
        val dao = getRoomDatabase().dao()
        assertThat(dao.insertItem(1)).isEqualTo(1)
        assertThat(dao.getSingleItem().pk).isEqualTo(1)
        assertThat(dao.deleteItem(1)).isEqualTo(1)
        assertThat(dao.deleteItem(1)).isEqualTo(0) // Nothing deleted
        assertThrows<IllegalStateException> {
            dao.getSingleItem()
        }.hasMessageThat().contains("The query result was empty")
    }

    @Test
    fun emptyResult() = runTest {
        val db = getRoomDatabase()
        assertThrows<IllegalStateException> {
            db.dao().getSingleItem()
        }.hasMessageThat().contains("The query result was empty")
    }

    @Test
    fun queryList() = runTest {
        val dao = getRoomDatabase().dao()
        dao.insertItem(1)
        dao.insertItem(2)
        dao.insertItem(3)
        val result = dao.getItemList()
        assertThat(result.map { it.pk }).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun transactionDelegate() = runTest {
        val dao = getRoomDatabase().dao()
        dao.insertItem(1)
        dao.insertItem(2)
        dao.insertItem(3)

        // Perform multiple delete transaction with error so delete is not committed
        assertThrows<IllegalArgumentException> {
            dao.deleteList(
                pks = listOf(1L, 2L, 3L),
                withError = true
            )
        }
        assertThat(dao.getItemList().map { it.pk }).containsExactly(1L, 2L, 3L)

        // Perform multiple delete in transaction successfully
        dao.deleteList(
            pks = listOf(1L, 3L),
        )
        assertThat(dao.getItemList().map { it.pk }).containsExactly(2L)
    }

    @Test
    fun simpleInsertAndDelete() = runTest {
        val sampleEntity = SampleEntity(1, 1)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity)
        assertThat(dao.getSingleItemWithColumn().pk).isEqualTo(1)

        dao.delete(sampleEntity)
        assertThrows<IllegalStateException> {
            dao.getSingleItemWithColumn()
        }.hasMessageThat().contains("The query result was empty")
    }

    @Test
    fun simpleInsertAndUpdateAndDelete() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntity(1, 2)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity1)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(1)

        dao.update(sampleEntity2)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(2)

        dao.delete(sampleEntity2)
        assertThrows<IllegalStateException> {
            dao.getSingleItem()
        }.hasMessageThat().contains("The query result was empty")
    }

    @Test
    fun simpleInsertAndUpsertAndDelete() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntity(1, 2)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity1)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(1)

        dao.upsert(sampleEntity2)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(2)

        dao.delete(sampleEntity2)
        assertThrows<IllegalStateException> {
            dao.getSingleItem()
        }.hasMessageThat().contains("The query result was empty")
    }

    @Test
    fun simpleInsertMap() = runTest {
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
    fun simpleMapWithDupeColumns() = runTest {
        val sampleEntity1 = SampleEntity(1, 1)
        val sampleEntity2 = SampleEntityCopy(1, 2)
        val dao = getRoomDatabase().dao()

        dao.insert(sampleEntity1)
        dao.insert(sampleEntity2)
        assertThat(dao.getSingleItemWithColumn().data).isEqualTo(1)

        val map = dao.getMapWithDupeColumns()
        assertThat(map[sampleEntity1]).isEqualTo(sampleEntity2)
    }

    @Test
    fun simpleInsertNestedMap() = runTest {
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
    fun simpleInsertNestedMapColumnMap() = runTest {
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
}
