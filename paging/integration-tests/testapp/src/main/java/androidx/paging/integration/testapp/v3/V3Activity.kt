/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging.integration.testapp.v3

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.PagingDataAdapter
import androidx.paging.integration.testapp.R
import androidx.paging.map
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class V3Activity : AppCompatActivity() {
    val pagingAdapter = V3Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recycler_view)
        val viewModel by viewModels<V3ViewModel>()

        val orientationText = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "land"
            Configuration.ORIENTATION_PORTRAIT -> "port"
            else -> "unknown"
        }
        // NOTE: lifecycleScope means we don't respect paused state here
        lifecycleScope.launch {
            viewModel.flow
                .map { pagingData ->
                    pagingData.map { it.copy(text = "${it.text} - $orientationText") }
                }
                .collectLatest { pagingAdapter.submitData(it) }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerView.adapter = pagingAdapter.withLoadStateHeaderAndFooter(
            header = StateItemAdapter { pagingAdapter.retry() },
            footer = StateItemAdapter { pagingAdapter.retry() }
        )

        setupLoadStateButtons(pagingAdapter)

        findViewById<Button>(R.id.button_error).setOnClickListener {
            dataSourceError.set(true)
        }
    }

    private fun setupLoadStateButtons(adapter: PagingDataAdapter<Item, RecyclerView.ViewHolder>) {
        val button = findViewById<Button>(R.id.button_refresh)
        adapter.addLoadStateListener { loadStates: CombinedLoadStates ->
            button.text = when (loadStates.refresh) {
                is NotLoading -> "Refresh"
                is Loading -> "Loading"
                is Error -> "Error"
            }

            if (loadStates.refresh is NotLoading) {
                button.setOnClickListener { adapter.refresh() }
            } else if (loadStates.refresh is Error) {
                button.setOnClickListener { adapter.retry() }
            }

            button.isEnabled = loadStates.refresh !is Loading
        }
    }
}