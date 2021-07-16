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
import androidx.paging.SeparatorsTest.Companion.LETTER_SEPARATOR_GENERATOR
import androidx.paging.TerminalSeparatorType.FULLY_COMPLETE
import androidx.paging.TerminalSeparatorType.SOURCE_COMPLETE
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
    fun prependAfterPrependComplete() = runBlockingTest {
        val pageEventFlow = flowOf(
            generatePrepend(
                originalPageOffset = 0,
                pages = listOf(listOf("a1")),
                loadStates = remoteLoadStatesOf(
                    prependLocal = NotLoading.Complete,
                    prependRemote = NotLoading.Complete,
                )
            ),
            generatePrepend(
                originalPageOffset = -1,
                pages = listOf(listOf("a1")),
                loadStates = remoteLoadStatesOf(
                    prependLocal = NotLoading.Complete,
                    prependRemote = NotLoading.Complete,
                )
            )
        )
        assertFailsWith<IllegalArgumentException>(
            "Prepend after endOfPaginationReached already true is invalid"
        ) {
            PagingData(pageEventFlow, dummyReceiver)
                .insertSeparators(
                    terminalSeparatorType = FULLY_COMPLETE,
                    generator = LETTER_SEPARATOR_GENERATOR
                )
                .flow.toList()
        }
    }

    @Test
    fun appendAfterAppendComplete() = runBlockingTest {
        val pageEventFlow = flowOf(
            generateAppend(
                originalPageOffset = 0,
                pages = listOf(listOf("a1")),
                loadStates = remoteLoadStatesOf(
                    appendLocal = NotLoading.Complete,
                    appendRemote = NotLoading.Complete,
                ),
            ),
            generateAppend(
                originalPageOffset = 1,
                pages = listOf(listOf("a1")),
                loadStates = remoteLoadStatesOf(
                    appendLocal = NotLoading.Complete,
                    appendRemote = NotLoading.Complete,
                ),
            ),
        )
        assertFailsWith<IllegalArgumentException>(
            "Append after endOfPaginationReached already true is invalid"
        ) {
            PagingData(pageEventFlow, dummyReceiver)
                .insertSeparators(
                    terminalSeparatorType = FULLY_COMPLETE,
                    generator = LETTER_SEPARATOR_GENERATOR
                )
                .flow.toList()
        }
    }

    @Test
    fun insertValidation_emptyRemoteAfterHeaderAdded() = runBlockingTest {
        val pageEventFlow = flowOf(
            generatePrepend(
                originalPageOffset = 0,
                pages = listOf(listOf("a1")),
                loadStates = remoteLoadStatesOf(
                    prependLocal = NotLoading.Incomplete,
                    prependRemote = NotLoading.Complete,
                ),
            ),
            generatePrepend(
                originalPageOffset = 1,
                pages = listOf(listOf("a1")),
                loadStates = remoteLoadStatesOf(
                    prependLocal = NotLoading.Complete,
                    prependRemote = NotLoading.Complete,
                ),
            ),
        )

        // Verify asserts in separators do not throw IllegalArgumentException for a local prepend
        // or append that arrives after remote prepend or append marking endOfPagination.
        PagingData(pageEventFlow, dummyReceiver).insertSeparators { _, _ -> -1 }.flow.toList()
    }

    @Test
    fun insertValidation_emptyRemoteAfterFooterAdded() = runBlockingTest {
        val pageEventFlow = flowOf(
            generateAppend(
                originalPageOffset = 0,
                pages = listOf(listOf("a1")),
                loadStates = remoteLoadStatesOf(
                    appendLocal = NotLoading.Incomplete,
                    appendRemote = NotLoading.Complete,
                ),
            ),
            generateAppend(
                originalPageOffset = 1,
                pages = listOf(listOf("a1")),
                loadStates = remoteLoadStatesOf(
                    appendLocal = NotLoading.Complete,
                    appendRemote = NotLoading.Complete,
                ),
            ),
        )

        // Verify asserts in separators do not throw IllegalArgumentException for a local prepend
        // or append that arrives after remote prepend or append marking endOfPagination.
        PagingData(pageEventFlow, dummyReceiver).insertSeparators { _, _ -> -1 }.flow.toList()
    }

    @Test
    fun emptyPrependThenEmptyRemote_fullyComplete() = runBlockingTest {
        val pageEventFlow = flowOf(
            generateRefresh(listOf("a1"), remoteLoadStatesOf()),
            generatePrepend(
                originalPageOffset = 1,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(prependLocal = NotLoading.Complete)
            ),
            generatePrepend(
                originalPageOffset = 2,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(
                    prependLocal = NotLoading.Complete,
                    prependRemote = NotLoading.Complete
                )
            )
        )
        val expected = listOf(
            generateRefresh(listOf("a1"), remoteLoadStatesOf()),
            generatePrepend(
                originalPageOffset = 1,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(prependLocal = NotLoading.Complete)
            ),
            generatePrepend(
                // page offset becomes 0 here, as it's adjacent to page 0, the only page with data.
                originalPageOffset = 0,
                pages = listOf(listOf("A")),
                loadStates = remoteLoadStatesOf(
                    prependLocal = NotLoading.Complete,
                    prependRemote = NotLoading.Complete
                )
            )
        )

        val actual = PagingData(pageEventFlow, dummyReceiver)
            .insertSeparators(
                terminalSeparatorType = FULLY_COMPLETE,
                generator = LETTER_SEPARATOR_GENERATOR
            )
            .flow.toList()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun emptyPrependThenEmptyRemote_sourceComplete() = runBlockingTest {
        val pageEventFlow = flowOf(
            generateRefresh(listOf("a1"), remoteLoadStatesOf()),
            generatePrepend(
                originalPageOffset = 1,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(prependLocal = NotLoading.Complete)
            ),
            generatePrepend(
                originalPageOffset = 2,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(
                    prependLocal = NotLoading.Complete,
                    prependRemote = NotLoading.Complete
                )
            )
        )
        val expected = listOf(
            generateRefresh(listOf("a1"), remoteLoadStatesOf()),
            generatePrepend(
                // page offset becomes 0 here, as it's adjacent to page 0, the only page with data.
                originalPageOffset = 0,
                pages = listOf(listOf("A")),
                loadStates = remoteLoadStatesOf(prependLocal = NotLoading.Complete)
            ),
            generatePrepend(
                originalPageOffset = 2,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(
                    prependLocal = NotLoading.Complete,
                    prependRemote = NotLoading.Complete
                )
            )
        )

        val actual = PagingData(pageEventFlow, dummyReceiver)
            .insertSeparators(
                terminalSeparatorType = SOURCE_COMPLETE,
                generator = LETTER_SEPARATOR_GENERATOR
            )
            .flow.toList()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun emptyAppendThenEmptyRemote_fullyComplete() = runBlockingTest {
        val pageEventFlow = flowOf(
            generateRefresh(listOf("a1"), remoteLoadStatesOf()),
            generateAppend(
                originalPageOffset = 1,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(appendLocal = NotLoading.Complete)
            ),
            generateAppend(
                originalPageOffset = 2,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(
                    appendLocal = NotLoading.Complete,
                    appendRemote = NotLoading.Complete
                )
            )
        )
        val expected = listOf(
            generateRefresh(listOf("a1"), remoteLoadStatesOf()),
            generateAppend(
                originalPageOffset = 1,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(appendLocal = NotLoading.Complete)
            ),
            generateAppend(
                // page offset becomes 0 here, as it's adjacent to page 0, the only page with data.
                originalPageOffset = 0,
                pages = listOf(listOf("END")),
                loadStates = remoteLoadStatesOf(
                    appendLocal = NotLoading.Complete,
                    appendRemote = NotLoading.Complete
                )
            )
        )

        val actual = PagingData(pageEventFlow, dummyReceiver)
            .insertSeparators(
                terminalSeparatorType = FULLY_COMPLETE,
                generator = LETTER_SEPARATOR_GENERATOR
            )
            .flow.toList()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun emptyAppendThenEmptyRemote_sourceComplete() = runBlockingTest {
        val pageEventFlow = flowOf(
            generateRefresh(listOf("a1"), remoteLoadStatesOf()),
            generateAppend(
                originalPageOffset = 1,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(appendLocal = NotLoading.Complete)
            ),
            generateAppend(
                originalPageOffset = 2,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(
                    appendLocal = NotLoading.Complete,
                    appendRemote = NotLoading.Complete
                )
            )
        )
        val expected = listOf(
            generateRefresh(listOf("a1"), remoteLoadStatesOf()),
            generateAppend(
                // page offset becomes 0 here, as it's adjacent to page 0, the only page with data.
                originalPageOffset = 0,
                pages = listOf(listOf("END")),
                loadStates = remoteLoadStatesOf(appendLocal = NotLoading.Complete)
            ),
            generateAppend(
                originalPageOffset = 2,
                pages = listOf(),
                loadStates = remoteLoadStatesOf(
                    appendLocal = NotLoading.Complete,
                    appendRemote = NotLoading.Complete
                )
            )
        )

        val actual = PagingData(pageEventFlow, dummyReceiver)
            .insertSeparators(
                terminalSeparatorType = SOURCE_COMPLETE,
                generator = LETTER_SEPARATOR_GENERATOR
            )
            .flow.toList()

        assertThat(actual).isEqualTo(expected)
    }
}

private fun transformablePage(
    originalPageOffset: Int,
    data: List<String>
) = TransformablePage(
    originalPageOffsets = intArrayOf(originalPageOffset),
    data = data,
    hintOriginalPageOffset = originalPageOffset,
    hintOriginalIndices = data.fold(mutableListOf()) { acc, s ->
        acc.apply {
            add(
                when {
                    acc.isEmpty() -> 0
                    s.all { it.isUpperCase() } -> acc.last()
                    else -> acc.last() + 1
                }
            )
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