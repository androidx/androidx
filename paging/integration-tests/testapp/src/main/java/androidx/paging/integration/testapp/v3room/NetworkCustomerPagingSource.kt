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

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.integration.testapp.room.Customer
import java.util.UUID

/**
 * Sample position-based PagingSource with artificial data.
 */
internal class NetworkCustomerPagingSource : PagingSource<Int, Customer>() {
    private fun createCustomer(i: Int): Customer {
        val customer = Customer()
        customer.name = "customer_$i"
        customer.lastName = "${"%04d".format(i)}_${UUID.randomUUID()}"
        return customer
    }

    override fun getRefreshKey(
        state: PagingState<Int, Customer>
    ): Int? = state.anchorPosition?.let {
        maxOf(0, it - 5)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Customer> {
        val key = params.key ?: 0
        val data = if (params is LoadParams.Prepend) {
            List(params.loadSize) { createCustomer(it + key - params.loadSize) }
        } else {
            List(params.loadSize) { createCustomer(it + key) }
        }
        return LoadResult.Page(
            data = data,
            prevKey = if (key > 0) key else null,
            nextKey = key + data.size
        )
    }

    companion object {
        val FACTORY = { NetworkCustomerPagingSource() }
    }
}
