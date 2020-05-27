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

package androidx.paging

import androidx.paging.LoadState.NotLoading
import androidx.paging.PageEvent.Insert.Companion.Append
import androidx.paging.PageEvent.Insert.Companion.Prepend
import androidx.paging.PageEvent.Insert.Companion.Refresh
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class SeparatorsWithRemoteMediatorTest {
    @Test
    fun addRemotePrependAfterRefresh() = runBlockingTest {
        val pageEventFlow = flowOf(
            generateRefresh(listOf("a1"), localLoadStatesOf(prependLocal = NotLoading.Done)),
            generatePrepend(
                originalPageOffset = -1,
                pages = listOf(),
                // Now switching to states with mediator != null - this is invalid!
                loadStates = remoteLoadStatesOf(prependRemote = NotLoading.Done)
            )
        )
        assertFailsWith<IllegalArgumentException>(
            "Additional prepend event after prepend state is done"
        ) {
            PagingData(pageEventFlow, dummyReceiver)
                .insertSeparators(SeparatorsTest.LETTER_SEPARATOR_GENERATOR)
                .flow.toList()
        }
    }

    @Test
    fun addRemoteAppendAfterRefresh() = runBlockingTest {
        val pageEventFlow = flowOf(
            generateRefresh(listOf("a1"), localLoadStatesOf(appendLocal = NotLoading.Done)),
            generateAppend(
                originalPageOffset = 1,
                pages = listOf(),
                // Now switching to states with mediator != null - this is invalid!
                loadStates = remoteLoadStatesOf(appendRemote = NotLoading.Done)
            )
        )
        assertFailsWith<IllegalArgumentException>(
            "Additional append event after append state is done"
        ) {
            PagingData(pageEventFlow, dummyReceiver)
                .insertSeparators(SeparatorsTest.LETTER_SEPARATOR_GENERATOR)
                .flow.toList()
        }
    }

    @Test
    fun emptyPrependThenEmptyRemote() = runBlockingTest {
        val pageEventFlow = flowOf(
            generateRefresh(listOf("a1"), remoteLoadStatesOf()),
            generatePrepend(
                originalPageOffset = 1,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(prependLocal = NotLoading.Done)
            ),
            generatePrepend(
                originalPageOffset = 2,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(
                    prependLocal = NotLoading.Done,
                    prependRemote = NotLoading.Done
                )
            )
        )
        val expected = listOf(
            generateRefresh(listOf("a1"), remoteLoadStatesOf()),
            generatePrepend(
                originalPageOffset = 1,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(prependLocal = NotLoading.Done)
            ),
            generatePrepend(
                // page offset becomes 0 here, as it's adjacent to page 0, the only page with data.
                // Ideally it would be 2, since that's the index of the page last added,
                // but empty pages are filtered out, so originalPageOffset = 1 and 2 don't make
                // it to separators logic.
                originalPageOffset = 0,
                pages = listOf(listOf("A")),
                loadStates = remoteLoadStatesOf(
                    prependLocal = NotLoading.Done,
                    prependRemote = NotLoading.Done
                )
            )
        )

        val actual = PagingData(pageEventFlow, dummyReceiver)
            .insertSeparators(SeparatorsTest.LETTER_SEPARATOR_GENERATOR)
            .flow.toList()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun emptyAppendThenEmptyRemote() = runBlockingTest {
        val pageEventFlow = flowOf(
            generateRefresh(listOf("a1"), remoteLoadStatesOf()),
            generateAppend(
                originalPageOffset = 1,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(appendLocal = NotLoading.Done)
            ),
            generateAppend(
                originalPageOffset = 2,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(
                    appendLocal = NotLoading.Done,
                    appendRemote = NotLoading.Done
                )
            )
        )
        val expected = listOf(
            generateRefresh(listOf("a1"), remoteLoadStatesOf()),
            generateAppend(
                originalPageOffset = 1,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(appendLocal = NotLoading.Done)
            ),
            generateAppend(
                // page offset becomes 0 here, as it's adjacent to page 0, the only page with data.
                // Ideally it would be 2, since that's the index of the page last added,
                // but empty pages are filtered out, so originalPageOffset = 1 and 2 don't make
                // it to separators logic.
                originalPageOffset = 0,
                pages = listOf(listOf("END")),
                loadStates = remoteLoadStatesOf(
                    appendLocal = NotLoading.Done,
                    appendRemote = NotLoading.Done
                )
            )
        )

        val actual = PagingData(pageEventFlow, dummyReceiver)
            .insertSeparators(SeparatorsTest.LETTER_SEPARATOR_GENERATOR)
            .flow.toList()

        assertThat(actual).isEqualTo(expected)
    }
}

private fun transformablePage(
    originalPageOffset: Int,
    data: List<String>
) = TransformablePage(
    originalPageOffset = originalPageOffset,
    data = data,
    originalPageSize = data.size,
    originalIndices = data.fold(mutableListOf()) { acc, s ->
        acc.apply {
            add(when {
                acc.isEmpty() -> 0
                s.all { it.isUpperCase() } -> acc.last()
                else -> acc.last() + 1
            })
        }
    }
)

private fun generateRefresh(
    data: List<String>,
    loadStates: CombinedLoadStates
) = Refresh(
    pages = listOf(transformablePage(0, data)),
    placeholdersBefore = 0,
    placeholdersAfter = 0,
    combinedLoadStates = loadStates
)

private fun generatePrepend(
    originalPageOffset: Int,
    pages: List<List<String>>,
    loadStates: CombinedLoadStates
) = Prepend(
    pages = pages.map { data -> transformablePage(originalPageOffset, data) },
    placeholdersBefore = 0,
    combinedLoadStates = loadStates
)

private fun generateAppend(
    originalPageOffset: Int,
    pages: List<List<String>>,
    loadStates: CombinedLoadStates
) = Append(
    pages = pages.map { data -> transformablePage(originalPageOffset, data) },
    placeholdersAfter = 0,
    combinedLoadStates = loadStates
)