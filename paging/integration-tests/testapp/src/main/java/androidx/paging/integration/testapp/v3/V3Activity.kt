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
import androidx.paging.LoadState
import androidx.paging.LoadType
import androidx.paging.PagingDataAdapter
import androidx.paging.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
@FlowPreview
class V3Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recycler_view)
        val viewModel by viewModels<V3ViewModel>()

        val adapter = V3Adapter()
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
                .collect { adapter.presentData(it) }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerView.adapter = adapter

        setupLoadStateButtons(adapter)

        findViewById<Button>(R.id.button_error).setOnClickListener {
            dataSourceError.set(true)
        }
    }

    private fun setupLoadStateButtons(adapter: PagingDataAdapter<Item, RecyclerView.ViewHolder>) {
        val buttonStart = findViewById<Button>(R.id.button_start)
        val buttonRefresh = findViewById<Button>(R.id.button_refresh)
        val buttonEnd = findViewById<Button>(R.id.button_end)

        buttonRefresh.setOnClickListener {
            adapter.refresh()
        }
        buttonStart.setOnClickListener {
            adapter.retry()
        }
        buttonEnd.setOnClickListener {
            adapter.retry()
        }

        adapter.addLoadStateListener { type: LoadType, state: LoadState ->
            val button = when (type) {
                LoadType.REFRESH -> buttonRefresh
                LoadType.START -> buttonStart
                LoadType.END -> buttonEnd
            }

            when (state) {
                is LoadState.Idle -> {
                    button.text = "Idle"
                    button.isEnabled = type == LoadType.REFRESH
                }
                is LoadState.Loading -> {
                    button.text = "Loading"
                    button.isEnabled = false
                }
                is LoadState.Done -> {
                    button.text = "Done"
                    button.isEnabled = false
                }
                is LoadState.Error -> {
                    button.text = "Error"
                    button.isEnabled = true
                }
            }
        }
    }
}