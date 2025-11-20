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

package androidx.room3.integration.multiplatformtestapp.test

import androidx.kruth.assertThat
import androidx.paging.PagingSource
import androidx.room3.ColumnInfo
import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.Transaction
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

abstract class BasePagingTest {

    abstract fun getRoomDatabase(): PagingDatabase

    @Test
    fun pagingQuery() = runTest {
        val db = getRoomDatabase()
        val entity1 = SampleEntity(1, 1)
        val entity2 = SampleEntity(2, 2)
        val sampleEntities = listOf(entity1, entity2)

        db.pagingDao().insertSampleEntityList(sampleEntities)
        val pagingSource = db.pagingDao().getAllIds()

        val onlyLoadFirst =
            pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 1,
                    placeholdersEnabled = true,
                )
            ) as PagingSource.LoadResult.Page
        assertThat(onlyLoadFirst.data).containsExactly(entity1)

        val loadAll =
            pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 2,
                    placeholdersEnabled = true,
                )
            ) as PagingSource.LoadResult.Page
        assertThat(loadAll.data).containsExactlyElementsIn(sampleEntities)
        db.close()
    }

    @Test
    fun pagingQueryWithParams() = runTest {
        val db = getRoomDatabase()
        val entity1 = SampleEntity(1, 1)
        val entity2 = SampleEntity(2, 2)
        val entity3 = SampleEntity(3, 3)
        val sampleEntities = listOf(entity1, entity2, entity3)

        db.pagingDao().insertSampleEntityList(sampleEntities)
        val pagingSource = db.pagingDao().getAllIdsWithArgs(1)

        val onlyLoadFirst =
            pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 1,
                    placeholdersEnabled = true,
                )
            ) as PagingSource.LoadResult.Page
        assertThat(onlyLoadFirst.data).containsExactly(entity2)

        val loadAll =
            pagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 2,
                    placeholdersEnabled = true,
                )
            ) as PagingSource.LoadResult.Page
        assertThat(loadAll.data).containsExactlyElementsIn(listOf(entity2, entity3))
        db.close()
    }

    @Database(
        entities = [SampleEntity::class, SampleEntity2::class, SampleEntity3::class],
        version = 1,
        exportSchema = false,
    )
    @ConstructedBy(PagingDatabaseConstructor::class)
    abstract class PagingDatabase : RoomDatabase() {
        abstract fun pagingDao(): PagingDao
    }

    @Dao
    interface PagingDao {

        @Insert suspend fun insertSampleEntityList(entities: List<SampleEntity>)

        @Transaction
        @Query("SELECT * FROM SampleEntity3")
        fun getPagingSourceRelation(): PagingSource<Int, SampleRelation>

        data class SampleRelation(
            val pk3: Long,
            @ColumnInfo(defaultValue = "0") val data3: Long,
            @Relation(parentColumn = "pk3", entityColumn = "pk3") val relationEntity: SampleEntity3,
        )

        @Query("SELECT * FROM SampleEntity") fun getAllIds(): PagingSource<Int, SampleEntity>

        @Query("SELECT * FROM SampleEntity WHERE pk > :gt ORDER BY pk ASC")
        fun getAllIdsWithArgs(gt: Long): PagingSource<Int, SampleEntity>
    }
}

expect object PagingDatabaseConstructor : RoomDatabaseConstructor<BasePagingTest.PagingDatabase>
