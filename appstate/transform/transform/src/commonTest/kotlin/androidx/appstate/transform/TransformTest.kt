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

package androidx.appstate.transform

import androidx.appstate.transform.testing.transformTest
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.kruth.assertThat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

class TransformTest {

    @BeforeTest
    fun setUp() {
        TransformScope.isGlobalSnapshotManagerEnabled = false
    }

    @AfterTest
    fun tearDown() {
        TransformScope.isGlobalSnapshotManagerEnabled = true
    }

    @Test
    fun testBasicTransform() = transformTest {
        val state = transform(initialValue = 0, backgroundScope, dispatcher = testDispatcher) { 1 }
        assertThat(state.value).isEqualTo(1)
    }

    @Test
    fun testStateUpdate() = transformTest {
        var sourceState by mutableStateOf(0)
        val state =
            transform(initialValue = 0, scope = backgroundScope, dispatcher = testDispatcher) {
                sourceState
            }

        assertThat(state.value).isEqualTo(0)

        sourceState = 1
        advance()

        assertThat(state.value).isEqualTo(1)
    }

    @Test
    fun testCancellation() = transformTest {
        val childScope =
            CoroutineScope(
                backgroundScope.coroutineContext +
                    Job(parent = backgroundScope.coroutineContext[Job])
            )
        var sourceState by mutableStateOf(0)
        val state =
            transform(initialValue = 0, childScope, dispatcher = testDispatcher) { sourceState }

        assertThat(state.value).isEqualTo(0)

        sourceState = 1
        advance()
        assertThat(state.value).isEqualTo(1)

        childScope.cancel()

        sourceState = 2
        advance()
        // Should still be 1 because the scope was cancelled.
        assertThat(state.value).isEqualTo(1)
    }

    @Test
    fun testSingleStateFlowToState() = transformTest {
        val flow = MutableStateFlow(0)
        val state =
            transform(initialValue = -1, scope = backgroundScope, dispatcher = testDispatcher) {
                flow.collectAsState().value
            }

        assertThat(state.value).isEqualTo(0)

        flow.value = 1
        advance()
        assertThat(state.value).isEqualTo(1)
    }

    @Test
    fun testMultipleStateFlowsToState() = transformTest {
        val flow1 = MutableStateFlow(1)
        val flow2 = MutableStateFlow(2)
        val flow3 = MutableStateFlow(3)

        val state =
            transform(initialValue = 0, scope = backgroundScope, dispatcher = testDispatcher) {
                val v1 = flow1.collectAsState().value
                val v2 = flow2.collectAsState().value
                val v3 = flow3.collectAsState().value
                v1 + v2 + v3
            }

        assertThat(state.value).isEqualTo(6)

        flow1.value = 10
        advance()
        assertThat(state.value).isEqualTo(15)

        flow2.value = 20
        flow3.value = 30
        advance()
        assertThat(state.value).isEqualTo(60)
    }

    @Test
    fun testViewModelTransform() = transformTest {
        val viewModel = HomeViewModel(backgroundScope, testDispatcher)

        assertThat(viewModel.uiState.value)
            .isEqualTo(HomeUiState(tracks = emptyList(), isOnline = false, currentTrackId = "none"))

        viewModel.isOnline.value = true
        advance()
        assertThat(viewModel.uiState.value)
            .isEqualTo(HomeUiState(tracks = emptyList(), isOnline = true, currentTrackId = "none"))

        viewModel.tracksFlow.value = listOf("Track 1", "Track 2")
        viewModel.currentTrackId.value = "track-1"
        advance()
        assertThat(viewModel.uiState.value)
            .isEqualTo(
                HomeUiState(
                    tracks = listOf("Track 1", "Track 2"),
                    isOnline = true,
                    currentTrackId = "track-1",
                )
            )
    }

    private data class HomeUiState(
        val tracks: List<String> = emptyList(),
        val isOnline: Boolean = false,
        val currentTrackId: String = "none",
    )

    private class HomeViewModel(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
    ) : ViewModel(scope) {
        val tracksFlow = MutableStateFlow(emptyList<String>())
        val isOnline = mutableStateOf(false)
        val currentTrackId = mutableStateOf("none")

        val uiState =
            transform(
                initialValue = HomeUiState(),
                scope = viewModelScope,
                dispatcher = dispatcher,
            ) {
                val tracks by tracksFlow.collectAsState()
                val online by isOnline
                val trackId by currentTrackId
                HomeUiState(
                    tracks = tracks,
                    isOnline = online,
                    currentTrackId = trackId,
                )
            }
    }
}
