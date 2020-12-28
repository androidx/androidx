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
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals

/**
 * Prepend and append are both Done, so that headers will appear
 */
private val fullLoadStates = localLoadStatesOf(
    prependLocal = NotLoading.Complete,
    appendLocal = NotLoading.Complete
)

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class HeaderFooterTest {

    private fun <T : Any> PageEvent<T>.toPagingData() = PagingData(
        flowOf(this),
        PagingData.NOOP_RECEIVER
    )

    private suspend fun <T : Any> PageEvent<T>.insertHeaderItem(item: T) = toPagingData()
        .insertHeaderItem(item = item)
        .flow
        .single()

    private suspend fun <T : Any> PageEvent<T>.insertFooterItem(item: T) = toPagingData()
        .insertFooterItem(item = item)
        .flow
        .single()

    @Test
    fun insertHeader_prepend() = runBlockingTest {
        val actual = Prepend(
            pages = listOf(
                TransformablePage(
                    data = listOf(0),
                    originalPageOffset = -1
                )
            ),
            placeholdersBefore = 0,
            combinedLoadStates = fullLoadStates
        ).insertHeaderItem(-1)

        val expected = Prepend(
            pages = listOf(
                TransformablePage(
                    data = listOf(-1),
                    originalPageOffsets = intArrayOf(-1),
                    hintOriginalPageOffset = -1,
                    hintOriginalIndices = listOf(0)
                ),
                TransformablePage(
                    data = listOf(0),
                    originalPageOffset = -1
                )
            ),
            placeholdersBefore = 0,
            combinedLoadStates = fullLoadStates
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun insertHeader_refresh() = runBlockingTest {
        val actual = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("a"),
                    originalPageOffset = 0
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = fullLoadStates
        ).insertHeaderItem("HEADER")

        val expected = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("HEADER"),
                    originalPageOffsets = intArrayOf(0),
                    hintOriginalPageOffset = 0,
                    hintOriginalIndices = listOf(0)
                ),
                TransformablePage(
                    data = listOf("a"),
                    originalPageOffset = 0
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = fullLoadStates
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun insertHeader_empty() = runBlockingTest {
        val actual = Refresh(
            pages = listOf(
                TransformablePage(
                    data = emptyList<String>(),
                    originalPageOffset = 0
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = fullLoadStates
        ).insertHeaderItem("HEADER")

        val expected = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("HEADER"),
                    originalPageOffsets = intArrayOf(0),
                    hintOriginalPageOffset = 0,
                    hintOriginalIndices = listOf(0)
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = fullLoadStates
        )

        assertEquals(expected, actual)
    }

    @Test
    fun insertFooter_append() = runBlockingTest {
        val actual = Append(
            pages = listOf(
                TransformablePage(
                    data = listOf("b"),
                    originalPageOffset = 0
                )
            ),
            placeholdersAfter = 0,
            combinedLoadStates = fullLoadStates
        ).insertFooterItem("FOOTER")

        val expected = Append(
            pages = listOf(
                TransformablePage(
                    data = listOf("b"),
                    originalPageOffset = 0
                ),
                TransformablePage(
                    data = listOf("FOOTER"),
                    originalPageOffsets = intArrayOf(0),
                    hintOriginalPageOffset = 0,
                    hintOriginalIndices = listOf(0)
                )
            ),
            placeholdersAfter = 0,
            combinedLoadStates = fullLoadStates
        )

        assertEquals(expected, actual)
    }

    @Test
    fun insertFooter_refresh() = runBlockingTest {
        val actual = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("a"),
                    originalPageOffset = 0
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = fullLoadStates
        ).insertFooterItem("FOOTER")

        val expected = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("a"),
                    originalPageOffset = 0
                ),
                TransformablePage(
                    data = listOf("FOOTER"),
                    originalPageOffsets = intArrayOf(0),
                    hintOriginalPageOffset = 0,
                    hintOriginalIndices = listOf(0)
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = fullLoadStates
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun insertFooter_empty() = runBlockingTest {
        val actual = Refresh(
            pages = listOf(
                TransformablePage(
                    data = emptyList<String>(),
                    originalPageOffset = 0
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = fullLoadStates
        ).insertFooterItem("FOOTER")

        val expected = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("FOOTER"),
                    originalPageOffsets = intArrayOf(0),
                    hintOriginalPageOffset = 0,
                    hintOriginalIndices = listOf(0)
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            combinedLoadStates = fullLoadStates
        )

        assertThat(actual).isEqualTo(expected)
    }
}