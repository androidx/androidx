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
}
