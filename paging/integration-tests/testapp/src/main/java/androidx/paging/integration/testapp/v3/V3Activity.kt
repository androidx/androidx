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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map

@ExperimentalCoroutinesApi
@FlowPreview
class V3Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recycler_view)
        val viewModel by viewModels<V3ViewModel>()

        val adapter = V3Adapter()
        val orientation = getResources().getConfiguration().orientation
        val orientationText = when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "land"
            Configuration.ORIENTATION_PORTRAIT -> "port"
            else -> "unknown"
        }
        // NOTE: lifecycleScope means we don't respect paused state here
        adapter.connect(viewModel.flow.map { pagedData ->
            pagedData.map {
                it.copy(
                    text = "${it.text} - $orientationText"
                )
            }
        }, lifecycleScope)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerView.adapter = adapter
    }
}