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

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package androidx.paging

import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PageEvent.Insert.Companion.Append
import androidx.paging.PageEvent.Insert.Companion.Prepend
import androidx.paging.PageEvent.Insert.Companion.Refresh
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals

@RunWith(JUnit4::class)
class HeaderFooterTest {

    private fun <T : Any> PageEvent<T>.toPagingData() = PagingData(
        flowOf(this),
        PagingData.NOOP_RECEIVER
    )

    private suspend fun <T : Any> PageEvent<T>.insertHeaderItem(item: T) = toPagingData()
        .insertHeaderItem(item)
        .flow
        .single()

    private suspend fun <T : Any> PageEvent<T>.insertFooterItem(item: T) = toPagingData()
        .insertFooterItem(item)
        .flow
        .single()

    @Test
    fun insertHeader_prepend() = runBlockingTest {
        val actual = Prepend(
            pages = listOf(
                TransformablePage(
                    data = listOf(0),
                    originalPageOffset = -1,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersBefore = 0,
            loadStates = loadStates
        ).insertHeaderItem(-1)

        val expected = Prepend(
            pages = listOf(
                TransformablePage(
                    data = listOf(-1),
                    originalPageOffset = -1,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                ),
                TransformablePage(
                    data = listOf(0),
                    originalPageOffset = -1,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersBefore = 0,
            loadStates = loadStates
        )

        assertEquals(expected, actual)
    }

    @Test
    fun insertHeader_refresh() = runBlockingTest {
        val actual = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("a"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            loadStates = loadStates
        ).insertHeaderItem("HEADER")

        val expected = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("HEADER"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                ),
                TransformablePage(
                    data = listOf("a"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                ),
                TransformablePage(
                    data = listOf(),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = null
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            loadStates = loadStates
        )

        assertEquals(expected, actual)
    }

    @Test
    fun insertHeader_empty() = runBlockingTest {
        val actual = Refresh(
            pages = listOf(
                TransformablePage(
                    data = emptyList<String>(),
                    originalPageOffset = 0,
                    originalPageSize = 0,
                    originalIndices = emptyList()
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            loadStates = loadStates
        ).insertHeaderItem("HEADER")

        val expected = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("HEADER"),
                    originalPageOffset = 0,
                    originalPageSize = 0,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            loadStates = loadStates
        )

        assertEquals(expected, actual)
    }

    @Test
    fun insertFooter_append() = runBlockingTest {
        val actual = Append(
            pages = listOf(
                TransformablePage(
                    data = listOf("b"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersAfter = 0,
            loadStates = loadStates
        ).insertFooterItem("FOOTER")

        val expected = Append(
            pages = listOf(
                TransformablePage(
                    data = listOf("b"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                ),
                TransformablePage(
                    data = listOf("FOOTER"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersAfter = 0,
            loadStates = loadStates
        )

        assertEquals(expected, actual)
    }

    @Test
    fun insertFooter_refresh() = runBlockingTest {
        val actual = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("a"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            loadStates = loadStates
        ).insertFooterItem("FOOTER")

        val expected = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf(),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = null
                ),
                TransformablePage(
                    data = listOf("a"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                ),
                TransformablePage(
                    data = listOf("FOOTER"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            loadStates = loadStates
        )

        assertEquals(expected, actual)
    }

    @Test
    fun insertFooter_empty() = runBlockingTest {
        val actual = Refresh(
            pages = listOf(
                TransformablePage(
                    data = emptyList<String>(),
                    originalPageOffset = 0,
                    originalPageSize = 0,
                    originalIndices = emptyList()
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            loadStates = loadStates
        ).insertFooterItem("FOOTER")

        val expected = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("FOOTER"),
                    originalPageOffset = 0,
                    originalPageSize = 0,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersBefore = 0,
            placeholdersAfter = 0,
            loadStates = loadStates
        )

        assertEquals(expected, actual)
    }
}

private val loadStates = mapOf(
    REFRESH to NotLoading.Idle,
    PREPEND to NotLoading.Done,
    APPEND to NotLoading.Done
)