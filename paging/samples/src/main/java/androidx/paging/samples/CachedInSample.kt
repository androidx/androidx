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

@file:Suppress("unused")

package androidx.paging.samples

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.Sampled
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private class UiModel(@Suppress("UNUSED_PARAMETER") string: String)
private class MyPagingAdapter : BasePagingAdapter<UiModel>()
private class MyViewModel : BaseViewModel<UiModel>()

private lateinit var pagingSourceFactory: () -> PagingSource<String, String>

@Sampled
fun cachedInSample() {
    class MyViewModel : ViewModel() {
        val flow = Pager(
            config = PagingConfig(pageSize = 40),
            pagingSourceFactory = pagingSourceFactory
        ).flow
            // Loads and transformations before the cachedIn operation will be cached, so that
            // multiple observers get the same data. This is true either for simultaneous
            // observers, or e.g. an Activity re-subscribing after being recreated
            .cachedIn(viewModelScope)
    }

    class MyActivity : AppCompatActivity() {
        val pagingAdapter = MyPagingAdapter()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val viewModel by viewModels<MyViewModel>()

            lifecycleScope.launch {
                viewModel.flow
                    // Any transformations after the ViewModel's cachedIn step will not be cached,
                    // and will instead by re-run immediately on Activity re-creation.
                    .map { pagingData ->
                        // example un-cached transformation
                        pagingData.map { UiModel(it) }
                    }
                    .collectLatest {
                        pagingAdapter.submitData(it)
                    }
            }
        }
    }
}
