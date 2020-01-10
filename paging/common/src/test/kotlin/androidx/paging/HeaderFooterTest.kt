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

import androidx.paging.LoadState.Done
import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Insert.Companion.End
import androidx.paging.PageEvent.Insert.Companion.Refresh
import androidx.paging.PageEvent.Insert.Companion.Start
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals

@UseExperimental(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class HeaderFooterTest {
    private val testScope = TestCoroutineScope()

    @Test
    fun insertHeader_prepend() = testScope.runBlockingTest {
        val actual = Start(
            pages = listOf(
                TransformablePage(
                    data = listOf(0),
                    originalPageOffset = -1,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersStart = 0,
            loadStates = loadStatesDone
        ).addHeader(-1)

        val expected = Start(
            pages = listOf(
                TransformablePage(
                    data = listOf(-1, 0),
                    originalPageOffset = -1,
                    originalPageSize = 1,
                    originalIndices = listOf(0, 0)
                )
            ),
            placeholdersStart = 0,
            loadStates = loadStatesDone
        )

        assertEquals(expected, actual)
    }

    @Test
    fun insertHeader_refresh() = testScope.runBlockingTest {
        val actual = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("a"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersStart = 0,
            placeholdersEnd = 0,
            loadStates = loadStatesDone
        ).addHeader("HEADER")

        val expected = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("HEADER", "a"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0, 0)
                )
            ),
            placeholdersStart = 0,
            placeholdersEnd = 0,
            loadStates = loadStatesDone
        )

        assertEquals(expected, actual)
    }

    @Test
    fun insertHeader_empty() = testScope.runBlockingTest {
        val actual = Refresh(
            pages = listOf(
                TransformablePage(
                    data = emptyList<String>(),
                    originalPageOffset = 0,
                    originalPageSize = 0,
                    originalIndices = emptyList()
                )
            ),
            placeholdersStart = 0,
            placeholdersEnd = 0,
            loadStates = loadStatesDone
        ).addHeader("HEADER")

        val expected = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("HEADER"),
                    originalPageOffset = 0,
                    originalPageSize = 0,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersStart = 0,
            placeholdersEnd = 0,
            loadStates = loadStatesDone
        )

        assertEquals(expected, actual)
    }

    @Test
    fun insertFooter_append() = testScope.runBlockingTest {
        val actual = End(
            pages = listOf(
                TransformablePage(
                    data = listOf("b"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersEnd = 0,
            loadStates = loadStatesDone
        ).addFooter("FOOTER")

        val expected = End(
            pages = listOf(
                TransformablePage(
                    data = listOf("b", "FOOTER"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0, 0)
                )
            ),
            placeholdersEnd = 0,
            loadStates = loadStatesDone
        )

        assertEquals(expected, actual)
    }

    @Test
    fun insertFooter_refresh() = testScope.runBlockingTest {
        val actual = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("a"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersStart = 0,
            placeholdersEnd = 0,
            loadStates = loadStatesDone
        ).addFooter("FOOTER")

        val expected = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("a", "FOOTER"),
                    originalPageOffset = 0,
                    originalPageSize = 1,
                    originalIndices = listOf(0, 0)
                )
            ),
            placeholdersStart = 0,
            placeholdersEnd = 0,
            loadStates = loadStatesDone
        )

        assertEquals(expected, actual)
    }

    @Test
    fun insertFooter_empty() = testScope.runBlockingTest {
        val actual = Refresh(
            pages = listOf(
                TransformablePage(
                    data = emptyList<String>(),
                    originalPageOffset = 0,
                    originalPageSize = 0,
                    originalIndices = emptyList()
                )
            ),
            placeholdersStart = 0,
            placeholdersEnd = 0,
            loadStates = loadStatesDone
        ).addFooter("FOOTER")

        val expected = Refresh(
            pages = listOf(
                TransformablePage(
                    data = listOf("FOOTER"),
                    originalPageOffset = 0,
                    originalPageSize = 0,
                    originalIndices = listOf(0)
                )
            ),
            placeholdersStart = 0,
            placeholdersEnd = 0,
            loadStates = loadStatesDone
        )

        assertEquals(expected, actual)
    }
}

private val loadStatesDone = mapOf(REFRESH to Done, START to Done, END to Done)