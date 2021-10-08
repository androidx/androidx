/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.paging.integration.testapp.custom

import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.paging.LoadState
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType
import androidx.paging.integration.testapp.R
import androidx.paging.integration.testapp.v3.StateItemAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Sample PagedList activity with artificial data source.
 */
class PagedListSampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_view)
        val viewModel by viewModels<PagedListItemViewModel>()

        val pagingAdapter = PagedListItemAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerView.adapter = pagingAdapter.withLoadStateHeaderAndFooter(
            header = StateItemAdapter { pagingAdapter.currentList?.retry() },
            footer = StateItemAdapter { pagingAdapter.currentList?.retry() }
        )

        @Suppress("DEPRECATION")
        viewModel.livePagedList.observe(this) { pagedList ->
            pagingAdapter.submitList(pagedList)
        }

        setupLoadStateButtons(viewModel, pagingAdapter)

        findViewById<Button>(R.id.button_error).setOnClickListener {
            dataSourceError.set(true)
        }
    }

    private fun setupLoadStateButtons(
        viewModel: PagedListItemViewModel,
        @Suppress("DEPRECATION")
        adapter: androidx.paging.PagedListAdapter<Item, RecyclerView.ViewHolder>
    ) {
        val button = findViewById<Button>(R.id.button_refresh)

        button.setOnClickListener {
            viewModel.invalidateList()
        }

        adapter.addLoadStateListener { type: LoadType, state: LoadState ->
            if (type != LoadType.REFRESH) return@addLoadStateListener

            when (state) {
                is NotLoading -> {
                    button.text = if (state.endOfPaginationReached) "Refresh" else "Done"
                    button.isEnabled = state.endOfPaginationReached
                }
                Loading -> {
                    button.text = "Loading"
                    button.isEnabled = false
                }
                is Error -> {
                    button.text = "Error"
                    button.isEnabled = true
                }
            }
        }
    }
}
