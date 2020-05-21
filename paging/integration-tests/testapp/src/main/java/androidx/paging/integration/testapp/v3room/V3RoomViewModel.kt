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

package androidx.paging.integration.testapp.v3room

import android.annotation.SuppressLint
import android.app.Application
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.integration.testapp.room.Customer
import androidx.paging.integration.testapp.room.SampleDatabase
import androidx.room.Room
import kotlinx.coroutines.flow.map
import java.util.UUID

class V3RoomViewModel(application: Application) : AndroidViewModel(application) {
    val database = Room.databaseBuilder(
        getApplication(),
        SampleDatabase::class.java,
        "customerDatabaseV3"
    ).build()

    private fun createCustomer(): Customer {
        val customer = Customer()
        customer.name = UUID.randomUUID().toString()
        customer.lastName = UUID.randomUUID().toString()
        return customer
    }

    @SuppressLint("RestrictedApi")
    internal fun insertCustomer() {
        ArchTaskExecutor.getInstance()
            .executeOnDiskIO { database.customerDao.insert(createCustomer()) }
    }

    @SuppressLint("RestrictedApi")
    internal fun clearAllCustomers() {
        ArchTaskExecutor.getInstance()
            .executeOnDiskIO { database.customerDao.removeAll() }
    }

    val flow = Pager(
        PagingConfig(10),
        remoteMediator = V3RemoteMediator(
            database,
            NetworkCustomerPagingSource.FACTORY()
        )
    ) {
        database.customerDao.loadPagedAgeOrderPagingSource()
    }.flow
        .map { pagingData ->
            pagingData
                .insertSeparators { before: Customer?, after: Customer? ->
                    if (after == null || (after.id / 3) == (before?.id ?: 0) / 3) {
                        // no separator, because at bottom or not needed yet
                        null
                    } else {
                        Customer().apply {
                            id = -1
                            name = "RIGHT ABOVE DIVIDER"
                            lastName = "LAST NAME"
                        }
                    }
                }
                .insertSeparators { before: Customer?, _: Customer? ->
                    if (before != null && before.id == -1) {
                        Customer().apply {
                            id = -2
                            name = "RIGHT BELOW DIVIDER"
                            lastName = "LAST NAME"
                        }
                    } else null
                }
                .insertHeaderItem(
                    Customer().apply {
                        id = Int.MIN_VALUE
                        lastName = "HEADER"
                    }
                )
                .insertFooterItem(
                    Customer().apply {
                        id = Int.MAX_VALUE
                        lastName = "FOOTER"
                    }
                )
        }
        .cachedIn(viewModelScope)
}
