/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.work.impl.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.work.WorkInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

/**
 * A Data Access Object for accessing [androidx.work.WorkInfo]s that uses raw SQL queries.
 */
@Dao
interface RawWorkInfoDao {
    /**
     * @param query The raw query obtained using [androidx.work.WorkQuery]
     * @return A [List] of [WorkSpec.WorkInfoPojo]s using the raw query.
     */
    @RawQuery(observedEntities = [WorkSpec::class])
    fun getWorkInfoPojos(query: SupportSQLiteQuery): List<WorkSpec.WorkInfoPojo>

    /**
     * @param query The raw query obtained using [androidx.work.WorkQuery]
     * @return A [LiveData] of a [List] of [WorkSpec.WorkInfoPojo]s using the
     * raw query.
     */
    @RawQuery(observedEntities = [WorkSpec::class])
    fun getWorkInfoPojosLiveData(
        query: SupportSQLiteQuery
    ): LiveData<List<WorkSpec.WorkInfoPojo>>

    /**
     * @param query The raw query obtained using [androidx.work.WorkQuery]
     * @return A [Flow] of a [List] of [WorkSpec.WorkInfoPojo]s using the
     * raw query.
     */
    @RawQuery(observedEntities = [WorkSpec::class])
    fun getWorkInfoPojosFlow(query: SupportSQLiteQuery): Flow<List<WorkSpec.WorkInfoPojo>>
}

fun RawWorkInfoDao.getWorkInfoPojosFlow(
    dispatcher: CoroutineDispatcher,
    query: SupportSQLiteQuery
): Flow<List<WorkInfo>> = getWorkInfoPojosFlow(query).dedup(dispatcher)
