/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.paging.compose.demos.room3

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.launch

internal class CatDatabaseViewModel : ViewModel() {
    private val database =
        Room.inMemoryDatabaseBuilder<CatDatabase>().setDriver(AndroidSQLiteDriver()).build()

    val pager =
        Pager(
                config =
                    PagingConfig(pageSize = 10, initialLoadSize = 25, enablePlaceholders = false),
                pagingSourceFactory = {
                    // Need to pass key column name after init because right now there is no good
                    // way to pass query arguments to DaoReturnTypeConverter. b/492164283
                    database.getDao().getCatsPagingSource().apply { keyColumnName = "name" }
                },
            )
            .flow

    init {
        viewModelScope.launch {
            // If database is empty, populate with data since we need something to paginate on...
            val dao = database.getDao()
            if (dao.getCatsCount() == 0) {
                dao.insertNewRandomCat(100)
            }
        }
    }

    fun insertNewCat() {
        viewModelScope.launch { database.getDao().insertNewRandomCat() }
    }

    override fun onCleared() {
        database.close()
    }
}
