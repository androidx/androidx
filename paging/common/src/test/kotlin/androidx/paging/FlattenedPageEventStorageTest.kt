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

package androidx.paging

import androidx.paging.LoadState.Idle
import androidx.paging.LoadState.Loading
import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert.Companion.Refresh
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FlattenedPageEventStorageTest {
    private val list = FlattenedPageEventStorage<String>()

    @Test
    fun empty() {
        assertThat(list.snapshot()).isEqualTo(Snapshot<String>())
        assertThat(list.getAsEvents()).isEmpty()
    }

    @Test
    fun refresh() {
        list.add(
            Refresh(
                pages = listOf(
                    TransformablePage(data = listOf("a", "b", "c"))
                ),
                placeholdersStart = 3,
                placeholdersEnd = 5,
                loadStates = emptyMap()
            )
        )
        assertThat(list.snapshot()).isEqualTo(
            Snapshot(
                items = listOf("a", "b", "c"),
                placeholdersStart = 3,
                placeholdersEnd = 5
            )
        )
    }

    @Test
    fun refresh_thenPrepend() {
        list.add(
            Refresh(
                pages = listOf(
                    TransformablePage(data = listOf("a", "b", "c"))
                ),
                placeholdersStart = 3,
                placeholdersEnd = 5,
                loadStates = emptyMap()
            )
        )
        list.add(
            PageEvent.Insert.Start(
                pages = listOf(
                    TransformablePage(data = listOf("x1")),
                    TransformablePage(data = listOf("x2"))
                ),
                placeholdersStart = 1,
                loadStates = emptyMap()
            )
        )
        assertThat(list.snapshot()).isEqualTo(
            Snapshot(
                items = listOf("x1", "x2", "a", "b", "c"),
                placeholdersStart = 1,
                placeholdersEnd = 5
            )
        )
    }

    @Test
    fun refresh_thenAppend() {
        list.add(
            Refresh(
                pages = listOf(
                    TransformablePage(data = listOf("a", "b", "c"))
                ),
                placeholdersStart = 3,
                placeholdersEnd = 5,
                loadStates = emptyMap()
            )
        )
        list.add(
            PageEvent.Insert.End(
                pages = listOf(
                    TransformablePage(data = listOf("x1")),
                    TransformablePage(data = listOf("x2")),
                    TransformablePage(data = listOf("x3"))
                ),
                placeholdersEnd = 2,
                loadStates = emptyMap()
            )
        )
        assertThat(list.snapshot()).isEqualTo(
            Snapshot(
                items = listOf("a", "b", "c", "x1", "x2", "x3"),
                placeholdersStart = 3,
                placeholdersEnd = 2
            )
        )
    }

    @Test
    fun refresh_refreshAgain() {
        list.add(
            Refresh(
                pages = listOf(
                    TransformablePage(data = listOf("a", "b", "c"))
                ),
                placeholdersStart = 3,
                placeholdersEnd = 5,
                loadStates = emptyMap()
            )
        )
        list.add(
            Refresh(
                pages = listOf(
                    TransformablePage(data = listOf("x", "y"))
                ),
                placeholdersStart = 2,
                placeholdersEnd = 4,
                loadStates = emptyMap()
            )
        )
        assertThat(list.snapshot()).isEqualTo(
            Snapshot(
                items = listOf("x", "y"),
                placeholdersStart = 2,
                placeholdersEnd = 4
            )
        )
    }

    @Test
    fun drop_fromStart() {
        list.add(
            Refresh(
                pages = listOf(
                    TransformablePage(data = listOf("a", "b", "c")),
                    TransformablePage(data = listOf("d", "e"))
                ),
                placeholdersStart = 3,
                placeholdersEnd = 5,
                loadStates = emptyMap()
            )
        )
        assertThat(list.snapshot()).isEqualTo(
            Snapshot(
                items = listOf("a", "b", "c", "d", "e"),
                placeholdersStart = 3,
                placeholdersEnd = 5
            )
        )
        list.add(
            Drop(
                loadType = START,
                count = 1,
                placeholdersRemaining = 6
            )
        )
        assertThat(list.snapshot()).isEqualTo(
            Snapshot(
                items = listOf("d", "e"),
                placeholdersStart = 6,
                placeholdersEnd = 5
            )
        )
    }

    @Test
    fun drop_fromEnd() {
        list.add(
            Refresh(
                pages = listOf(
                    TransformablePage(data = listOf("a", "b", "c")),
                    TransformablePage(data = listOf("d", "e"))
                ),
                placeholdersStart = 3,
                placeholdersEnd = 5,
                loadStates = emptyMap()
            )
        )
        assertThat(list.snapshot()).isEqualTo(
            Snapshot(
                items = listOf("a", "b", "c", "d", "e"),
                placeholdersStart = 3,
                placeholdersEnd = 5
            )
        )
        list.add(
            Drop(
                loadType = END,
                count = 1,
                placeholdersRemaining = 7
            )
        )
        assertThat(list.snapshot()).isEqualTo(
            Snapshot(
                items = listOf("a", "b", "c"),
                placeholdersStart = 3,
                placeholdersEnd = 7
            )
        )
    }

    @Test
    fun stateInInsert() {
        val error = LoadState.Error(RuntimeException("?"))
        list.add(
            Refresh(
                pages = listOf(
                    TransformablePage(data = listOf("a", "b", "c")),
                    TransformablePage(data = listOf("d", "e"))
                ),
                placeholdersStart = 3,
                placeholdersEnd = 5,
                loadStates = mapOf(
                    REFRESH to Idle,
                    START to Loading,
                    END to error
                )
            )
        )
        assertThat(list.snapshot()).isEqualTo(
            Snapshot(
                items = listOf("a", "b", "c", "d", "e"),
                placeholdersStart = 3,
                placeholdersEnd = 5,
                refreshState = Idle,
                startState = Loading,
                endState = error
            )
        )
    }

    private fun <T : Any> FlattenedPageEventStorage<T>.snapshot(): Snapshot<T> {
        return this.getAsEvents().fold(Snapshot()) { snapshot, event ->
            when (event) {
                is PageEvent.Insert -> {
                    check(event.loadType == REFRESH) {
                        "should only send refresh event"
                    }
                    snapshot.copy(
                        items = snapshot.items + event.pages.flatMap { it.data },
                        placeholdersStart = event.placeholdersStart,
                        placeholdersEnd = event.placeholdersEnd,
                        refreshState = event.loadStates[REFRESH] ?: Idle,
                        startState = event.loadStates[START] ?: Idle,
                        endState = event.loadStates[END] ?: Idle
                    )
                }
                is Drop -> {
                    throw IllegalStateException("shouldn't have any drops")
                }
                is PageEvent.StateUpdate -> {
                    when (event.loadType) {
                        REFRESH -> snapshot.copy(
                            refreshState = event.loadState
                        )
                        START -> snapshot.copy(
                            startState = event.loadState
                        )
                        END -> snapshot.copy(
                            endState = event.loadState
                        )
                    }
                }
            }
        }
    }

    data class Snapshot<T>(
        val items: List<T> = emptyList(),
        val refreshState: LoadState = Idle,
        val startState: LoadState = Idle,
        val endState: LoadState = Idle,
        val placeholdersStart: Int = 0,
        val placeholdersEnd: Int = 0
    )
}