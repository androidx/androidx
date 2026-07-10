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

package androidx.compose.runtime.retain.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Sampled
fun rememberAndRetainSample() {
    // To combine both retain and rememberSaveable so that an object can be both retained and
    // saved, we instead recommend splitting your class into three components: The retained state,
    // the saved state, and an object that acts on both.
    //
    // val saveData = rememberSaveable { ExtractedSaveData() }
    // val retainData = retain { ExtractedRetainData() }
    // val rememberedAndRetained = remember(saveData, retainData) {
    //     Combined(saveData, retainData)
    // }
    //
    // Correctly combining retain with rememberSaveable/rememberSerializable can significantly
    // increase the complexity of your state management. We recommend using this pattern when
    // building custom architecture patterns, and only when all of the following are true:
    //   - You are defining an object comprised of a mix of values that must be retained or saved
    //   - Your state is scoped to a Composable and isn’t suitable for the singleton scoping or
    //     lifespan of ViewModel
    @Composable
    fun rememberSearchUiModel(): SearchUiModel {
        val savedModel = rememberSerializable(serializer = serializer()) { SavedSearchUiModel() }
        val retainedModel = retain { RetainedSearchUiModel() }

        // Generally:
        // return remember(savedModel, retainedModel) { SearchUiModel(savedModel, retainedModel) }

        // The retained model needs to initialize if the saved state was restored but the retained
        // instance was recreated. Perform that initialization with an anonymous RememberObserver:
        return remember(savedModel, retainedModel) {
                object : RememberObserver {
                    val searchUiModel = SearchUiModel(savedModel, retainedModel)

                    override fun onRemembered() {
                        searchUiModel.initialize()
                    }

                    override fun onForgotten() {}

                    override fun onAbandoned() {}
                }
            }
            .searchUiModel
    }
}

@Serializable data class SavedSearchUiModel(var searchQuery: String = "")

class RetainedSearchUiModel() : RetainObserver {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var isInitialized = false

    var searchResults by mutableStateOf<List<SearchResult>>(emptyList())
        private set

    fun initialize(query: String) {
        if (!isInitialized) {
            isInitialized = true
            performSearch(query)
        }
    }

    fun performSearch(query: String) {
        coroutineScope.launch { searchResults = loadSearchResultsFromInternet(query) }
    }

    override fun onRetired() {
        coroutineScope.cancel()
    }

    override fun onUnused() {
        coroutineScope.cancel()
    }

    override fun onRetained() {}

    override fun onEnteredComposition() {}

    override fun onExitedComposition() {}

    private suspend fun loadSearchResultsFromInternet(query: String): List<SearchResult> {
        return emptyList()
    }
}

data class SearchResult(
    val title: String,
    val description: String,
    /* ... */
)

class SearchUiModel(
    private val savedModel: SavedSearchUiModel,
    private val retainedModel: RetainedSearchUiModel,
) {

    fun initialize() {
        // Saved state was kept, but retained state was discarded.
        // Perform the query to re-initialized our retained state.
        retainedModel.initialize(savedModel.searchQuery)
    }

    val searchTerm: String
        get() = savedModel.searchQuery

    fun search(query: String) {
        savedModel.searchQuery = query
        retainedModel.performSearch(query)
    }
}
