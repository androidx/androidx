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
import androidx.paging.LoadState.Idle
import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert.Companion.End
import androidx.paging.PageEvent.Insert.Companion.Refresh
import androidx.paging.PageEvent.Insert.Companion.Start
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals

private fun <T : Any> assertEvents(expected: List<PageEvent<T>>, actual: List<PageEvent<T>>) {
    try {
        assertEquals(expected, actual)
    } catch (e: Throwable) {
        throw AssertionError(
            e.localizedMessage
                .replace("),", "),\n")
                .replace("<[", "<[\n ")
                .replace("actual", "\nactual\n")
                .replace("Expected", "\nExpected\n")
                .replace("pages=", "pages=\n")
        )
    }
}
/*
private fun <T : Any> List<PageEvent.Insert<T>>.getItems() = map { event ->
    event.pages.map { it.data }
}
*/

private fun <T : Any> List<PageEvent<T>>.getItems() = mapNotNull { event ->
    when (event) {
        is PageEvent.Insert<T> -> event.pages.map { it.data }
        else -> null
    }
}

/**
 * Simpler assert which only concerns itself with page data, all other event types are ignored.
 */
private fun <T : Any> assertInsertData(
    expected: List<PageEvent<T>>,
    actual: List<PageEvent<T>>
) {
    @Suppress("UNCHECKED_CAST")
    assertEquals(
        expected.getItems(),
        actual.getItems()
    )
}

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class SeparatorsTest {
    @Test
    fun refreshFull() = runBlockingTest {
        assertEvents(
            listOf(
                Refresh(
                    pages = listOf(
                        TransformablePage(
                            originalPageOffset = 0,
                            data = listOf("a2", "B", "b1"),
                            originalPageSize = 2,
                            originalIndices = listOf(0, 1, 1)
                        ),
                        TransformablePage(
                            originalPageOffset = 1,
                            data = listOf("C"),
                            originalPageSize = 2,
                            originalIndices = listOf(0)
                        ),
                        TransformablePage(
                            originalPageOffset = 1,
                            data = listOf("c1", "c2"),
                            originalPageSize = 2,
                            originalIndices = null
                        )
                    ),
                    placeholdersStart = 0,
                    placeholdersEnd = 1,
                    loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
                )
            ),
            flowOf(
                Refresh(
                    pages = listOf(
                        listOf("a2", "b1"),
                        listOf("c1", "c2")
                    ).toTransformablePages(),
                    placeholdersStart = 0,
                    placeholdersEnd = 1,
                    loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
                )
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshStartFull() = runBlockingTest {
        val refresh = Refresh(
            pages = listOf(
                listOf("c1")
            ).toTransformablePages(),
            placeholdersStart = 0,
            placeholdersEnd = 1,
            loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
        )
        assertEvents(
            listOf(
                refresh,
                Start(
                    pages = listOf(
                        TransformablePage(
                            originalPageOffset = -2,
                            data = listOf("a1", "B", "b1"),
                            originalPageSize = 2,
                            originalIndices = listOf(0, 1, 1)
                        ),
                        TransformablePage(
                            originalPageOffset = -1,
                            data = listOf(),
                            originalPageSize = 2,
                            originalIndices = null
                        ),
                        TransformablePage(
                            originalPageOffset = -1,
                            data = listOf("b2", "b3"),
                            originalPageSize = 2,
                            originalIndices = null
                        ),
                        TransformablePage(
                            originalPageOffset = -1,
                            data = listOf("C"),
                            originalPageSize = 2,
                            originalIndices = listOf(1) // note: using last index of 2nd page in
                        )
                    ),
                    placeholdersStart = 1,
                    loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
                )
            ),
            flowOf(
                refresh,
                Start(
                    pages = listOf(
                        listOf("a1", "b1"),
                        listOf("b2", "b3")
                    ).toTransformablePages(2),
                    placeholdersStart = 1,
                    loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
                )
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEndFull() = runBlockingTest {
        val refresh = Refresh(
            pages = listOf(
                listOf("a1", "a2")
            ).toTransformablePages(),
            placeholdersStart = 0,
            placeholdersEnd = 1,
            loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
        )
        assertEvents(
            listOf(
                refresh,
                End(
                    pages = listOf(
                        TransformablePage(
                            originalPageOffset = 1,
                            data = listOf("C"),
                            originalPageSize = 2,
                            originalIndices = listOf(0)
                        ),
                        TransformablePage(
                            originalPageOffset = 1,
                            data = listOf("c1", "D", "d1"),
                            originalPageSize = 2,
                            originalIndices = listOf(0, 1, 1)
                        ),
                        TransformablePage(
                            originalPageOffset = 2,
                            data = listOf(),
                            originalPageSize = 2,
                            originalIndices = null
                        ),
                        TransformablePage(
                            originalPageOffset = 2,
                            data = listOf("d2", "d3"),
                            originalPageSize = 2,
                            originalIndices = null
                        )
                    ),
                    placeholdersEnd = 1,
                    loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
                )
            ),
            flowOf(
                refresh,
                End(
                    pages = listOf(
                        listOf("c1", "d1"),
                        listOf("d2", "d3")
                    ).toTransformablePages(-1),
                    placeholdersEnd = 1,
                    loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
                )
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshDropFull() = runBlockingTest {
        assertEvents(
            listOf(
                Refresh(
                    pages = listOf(
                        TransformablePage(
                            originalPageOffset = 0,
                            data = listOf("a1"),
                            originalPageSize = 1,
                            originalIndices = null
                        ),
                        TransformablePage(
                            originalPageOffset = 1,
                            data = listOf(),
                            originalPageSize = 1,
                            originalIndices = null
                        ),
                        TransformablePage(
                            originalPageOffset = 1,
                            data = listOf("a2"),
                            originalPageSize = 1,
                            originalIndices = null
                        )
                    ),
                    placeholdersStart = 0,
                    placeholdersEnd = 1,
                    loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
                ),
                Drop(
                    END,
                    2,
                    4
                )
            ),
            flowOf(
                Refresh(
                    pages = listOf(
                        listOf("a1"),
                        listOf("a2")
                    ).toTransformablePages(),
                    placeholdersStart = 0,
                    placeholdersEnd = 1,
                    loadStates = mapOf(REFRESH to Idle, START to Idle, END to Idle)
                ),
                Drop<String>(
                    END,
                    1,
                    4
                )
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    private fun refresh(
        pages: List<String?>,
        start: LoadState = Idle,
        end: LoadState = Idle
    ) = Refresh(
        pages = pages.map {
            if (it != null) listOf(it) else emptyList()
        }.toTransformablePages(),
        placeholdersStart = 0,
        placeholdersEnd = 1,
        loadStates = mapOf(REFRESH to Idle, START to start, END to end)
    )

    private fun start(
        pages: List<String?>,
        start: LoadState = Idle
    ) = Start(
        pages = pages.map {
            if (it != null) listOf(it) else emptyList()
        }.toTransformablePages(),
        placeholdersStart = 0,
        loadStates = mapOf(REFRESH to Idle, START to start, END to Idle)
    )

    private fun end(
        pages: List<String?>,
        end: LoadState = Idle
    ) = End(
        pages = pages.map {
            if (it != null) listOf(it) else emptyList()
        }.toTransformablePages(),
        placeholdersEnd = 0,
        loadStates = mapOf(REFRESH to Idle, START to Idle, END to end)
    )

    private fun drop(
        loadType: LoadType,
        count: Int
    ) = Drop<String>(loadType = loadType, count = count, placeholdersRemaining = 0)

    @Test
    fun refreshNoop() = runBlockingTest {
        assertInsertData(
            listOf(
                refresh(pages = listOf("a1"))
            ),
            flowOf(
                refresh(pages = listOf("a1"))
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }
    @Test
    fun refreshStartDone() = runBlockingTest {
        assertInsertData(
            listOf(
                refresh(pages = listOf("A", "a1"))
            ),
            flowOf(
                refresh(pages = listOf("a1"), start = Done)
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEndDone() = runBlockingTest {
        assertInsertData(
            listOf(
                refresh(pages = listOf("a1", "END"))
            ),
            flowOf(
                refresh(pages = listOf("a1"), end = Done)
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshBothDone() = runBlockingTest {
        assertInsertData(
            listOf(
                refresh(pages = listOf("A", "a1", "END"))
            ),
            flowOf(
                refresh(pages = listOf("a1"), start = Done, end = Done)
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEmptyNoop() = runBlockingTest {
        assertInsertData(
            listOf(
                // should only get one item when initial page is done on both sides
                refresh(pages = listOf<String>())
            ),
            flowOf(
                refresh(pages = listOf())
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEmptyStartDone() = runBlockingTest {
        assertInsertData(
            listOf(
                // not enough data to create separators yet
                refresh(pages = listOf()),
                // now we can, since we know what's on the other side
                end(pages = listOf("A", "a1")),
                // empty should be noop, since it's not end = Done
                end(pages = listOf()),
                // end = Done, so resolve final separator
                end(pages = listOf("END"))
            ),
            flowOf(
                refresh(pages = listOf(), start = Done),
                end(pages = listOf("a1")),
                end(pages = listOf()),
                end(pages = listOf(), end = Done)
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEmptyEndDone() = runBlockingTest {
        assertInsertData(
            listOf(
                // not enough data to create separators yet
                refresh(pages = listOf()),
                // now we can, since we know what's on the other side
                start(pages = listOf("a1", "END")),
                // empty should be noop, since it's not start = Done
                start(pages = listOf()),
                // start = Done, so resolve final separator
                start(pages = listOf("A"))
            ),
            flowOf(
                refresh(pages = listOf(), end = Done),
                start(pages = listOf("a1")),
                start(pages = listOf()),
                start(pages = listOf(), start = Done)
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun emptyDropResetsDeferredFrontSeparator() = runBlockingTest {
        assertInsertData(
            listOf(
                // not enough data to create separators yet
                refresh(pages = listOf()),
                // don't insert a separator, since a drop occurred (even though it's 0 size)
                end(pages = listOf("a1")),
                // but now add the separator, since start is done again
                start(pages = listOf("A"))
            ),
            flowOf(
                refresh(pages = listOf(), start = Done),
                drop(loadType = START, count = 0),
                end(pages = listOf("a1")),
                start(pages = listOf(), start = Done)
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun emptyDropResetsDeferredEndSeparator() = runBlockingTest {
        assertInsertData(
            listOf(
                // not enough data to create separators yet
                refresh(pages = listOf()),
                // don't insert a separator, since a drop occurred (even though it's 0 size)
                start(pages = listOf("a1")),
                // but now add the separator, since end is done again
                end(pages = listOf("END"))
            ),
            flowOf(
                refresh(pages = listOf(), end = Done),
                drop(loadType = END, count = 0),
                start(pages = listOf("a1")),
                end(pages = listOf(), end = Done)
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEmptyStart() = runBlockingTest {
        assertInsertData(
            listOf(
                refresh(pages = listOf()),
                // not enough data to create separators yet
                start(pages = listOf("a1"))
            ),
            flowOf(
                refresh(pages = listOf()),
                start(pages = listOf("a1"))
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEmptyEnd() = runBlockingTest {
        assertInsertData(
            listOf(
                refresh(pages = listOf()),
                // not enough data to create separators yet
                end(pages = listOf("a1"))
            ),
            flowOf(
                refresh(pages = listOf()),
                end(pages = listOf("a1"))
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEmptyStartDropFull() = runBlockingTest {
        // when start terminal separator is inserted, we need to drop count*2 + 1
        assertEvents(
            listOf(
                Refresh(
                    pages = listOf(
                        TransformablePage(
                            originalPageOffset = 0,
                            data = listOf("A"),
                            originalPageSize = 1,
                            originalIndices = listOf(0)
                        ),
                        TransformablePage(
                            originalPageOffset = 0,
                            data = listOf("a1"),
                            originalPageSize = 1,
                            originalIndices = null
                        ),
                        TransformablePage(
                            originalPageOffset = 1,
                            data = listOf("B"),
                            originalPageSize = 1,
                            originalIndices = listOf(0)
                        ),
                        TransformablePage(
                            originalPageOffset = 1,
                            data = listOf("b1"),
                            originalPageSize = 1,
                            originalIndices = null
                        )
                    ),
                    placeholdersStart = 0,
                    placeholdersEnd = 1,
                    loadStates = mapOf(REFRESH to Idle, START to Done, END to Idle)
                ),
                drop(loadType = START, count = 3)
            ),
            flowOf(
                refresh(pages = listOf("a1", "b1"), start = Done),
                drop(loadType = START, count = 1)
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEmptyEndDropFull() = runBlockingTest {
        // when end terminal separator is inserted, we need to drop count*2 + 1
        assertEvents(
            listOf(
                Refresh(
                    pages = listOf(
                        TransformablePage(
                            originalPageOffset = 0,
                            data = listOf("a1"),
                            originalPageSize = 1,
                            originalIndices = null
                        ),
                        TransformablePage(
                            originalPageOffset = 1,
                            data = listOf("B"),
                            originalPageSize = 1,
                            originalIndices = listOf(0)
                        ),
                        TransformablePage(
                            originalPageOffset = 1,
                            data = listOf("b1"),
                            originalPageSize = 1,
                            originalIndices = null
                        ),
                        TransformablePage(
                            originalPageOffset = 1,
                            data = listOf("END"),
                            originalPageSize = 1,
                            originalIndices = listOf(0)
                        )
                    ),
                    placeholdersStart = 0,
                    placeholdersEnd = 1,
                    loadStates = mapOf(REFRESH to Idle, START to Idle, END to Done)
                ),
                drop(loadType = END, count = 3)
            ),
            flowOf(
                refresh(pages = listOf("a1", "b1"), end = Done),
                drop(loadType = END, count = 1)
            ).insertSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    companion object {
        /**
         * Creates an upper-case letter at the beginning of each section of strings that start
         * with the same letter, and the string "END" at the very end.
         */
        val LETTER_SEPARATOR_GENERATOR: (String?, String?) -> String? = { before, after ->
            if (after == null) {
                "END"
            } else if (before == null || before.first() != after.first()) {
                after.first().toUpperCase().toString()
            } else null
        }
    }
}