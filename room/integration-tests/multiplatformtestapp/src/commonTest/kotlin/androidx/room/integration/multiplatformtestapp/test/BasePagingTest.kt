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
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

abstract class BasePagingTest {

    abstract fun getRoomDatabase(): SampleDatabase

    @Test
    fun pagingQuery() = runTest {
        val db = getRoomDatabase()
        val entity1 = SampleEntity(1, 1)
        val entity2 = SampleEntity(2, 2)
        val sampleEntities = listOf(entity1, entity2)
        val dao = db.dao()

        dao.insertSampleEntityList(sampleEntities)
        val pagingSource = dao.getAllIds()

        val onlyLoadFirst =
            pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 1,
                    placeholdersEnabled = true
                )
            ) as LoadResult.Page
        assertThat(onlyLoadFirst.data).containsExactly(entity1)

        val loadAll =
            pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 2,
                    placeholdersEnabled = true
                )
            ) as LoadResult.Page
        assertThat(loadAll.data).containsExactlyElementsIn(sampleEntities)
    }

    @Test
    fun pagingQueryWithParams() = runTest {
        val db = getRoomDatabase()
        val entity1 = SampleEntity(1, 1)
        val entity2 = SampleEntity(2, 2)
        val entity3 = SampleEntity(3, 3)
        val sampleEntities = listOf(entity1, entity2, entity3)
        val dao = db.dao()

        dao.insertSampleEntityList(sampleEntities)
        val pagingSource = dao.getAllIdsWithArgs(1)

        val onlyLoadFirst =
            pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 1,
                    placeholdersEnabled = true
                )
            ) as LoadResult.Page
        assertThat(onlyLoadFirst.data).containsExactly(entity2)

        val loadAll =
            pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 2,
                    placeholdersEnabled = true
                )
            ) as LoadResult.Page
        assertThat(loadAll.data).containsExactlyElementsIn(listOf(entity2, entity3))
    }
}
