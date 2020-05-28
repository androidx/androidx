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
import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert.Companion.Append
import androidx.paging.PageEvent.Insert.Companion.Prepend
import androidx.paging.PageEvent.Insert.Companion.Refresh
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

@OptIn(ExperimentalCoroutinesApi::class)
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
                    placeholdersBefore = 0,
                    placeholdersAfter = 1,
                    combinedLoadStates = localLoadStatesOf()
                )
            ),
            flowOf(
                Refresh(
                    pages = listOf(
                        listOf("a2", "b1"),
                        listOf("c1", "c2")
                    ).toTransformablePages(),
                    placeholdersBefore = 0,
                    placeholdersAfter = 1,
                    combinedLoadStates = localLoadStatesOf()
                )
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshStartFull() = runBlockingTest {
        val refresh = Refresh(
            pages = listOf(
                listOf("c1")
            ).toTransformablePages(),
            placeholdersBefore = 0,
            placeholdersAfter = 1,
            combinedLoadStates = localLoadStatesOf()
        )
        assertEvents(
            listOf(
                refresh,
                Prepend(
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
                    placeholdersBefore = 1,
                    combinedLoadStates = localLoadStatesOf()
                )
            ),
            flowOf(
                refresh,
                Prepend(
                    pages = listOf(
                        listOf("a1", "b1"),
                        listOf("b2", "b3")
                    ).toTransformablePages(2),
                    placeholdersBefore = 1,
                    combinedLoadStates = localLoadStatesOf()
                )
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEndFull() = runBlockingTest {
        val refresh = Refresh(
            pages = listOf(
                listOf("a1", "a2")
            ).toTransformablePages(),
            placeholdersBefore = 0,
            placeholdersAfter = 1,
            combinedLoadStates = localLoadStatesOf()
        )
        assertEvents(
            listOf(
                refresh,
                Append(
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
                    placeholdersAfter = 1,
                    combinedLoadStates = localLoadStatesOf()
                )
            ),
            flowOf(
                refresh,
                Append(
                    pages = listOf(
                        listOf("c1", "d1"),
                        listOf("d2", "d3")
                    ).toTransformablePages(-1),
                    placeholdersAfter = 1,
                    combinedLoadStates = localLoadStatesOf()
                )
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshDropFull() = runBlockingTest {
        assertEvents(
            expected = listOf(
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
                    placeholdersBefore = 0,
                    placeholdersAfter = 1,
                    combinedLoadStates = localLoadStatesOf()
                ),
                Drop<String>(APPEND, 2, 4)
            ),
            actual = flowOf(
                Refresh(
                    pages = listOf(
                        listOf("a1"),
                        listOf("a2")
                    ).toTransformablePages(),
                    placeholdersBefore = 0,
                    placeholdersAfter = 1,
                    combinedLoadStates = localLoadStatesOf()
                ),
                Drop<String>(APPEND, 1, 4)
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    private fun refresh(
        pages: List<String?>,
        prepend: LoadState = NotLoading.Idle,
        append: LoadState = NotLoading.Idle
    ) = Refresh(
        pages = pages.map {
            if (it != null) listOf(it) else emptyList()
        }.toTransformablePages(),
        placeholdersBefore = 0,
        placeholdersAfter = 1,
        combinedLoadStates = localLoadStatesOf(prependLocal = prepend, appendLocal = append)
    )

    private fun prepend(
        pages: List<String?>,
        prepend: LoadState = NotLoading.Idle
    ) = Prepend(
        pages = pages.map {
            if (it != null) listOf(it) else emptyList()
        }.toTransformablePages(),
        placeholdersBefore = 0,
        combinedLoadStates = localLoadStatesOf(prependLocal = prepend)
    )

    private fun append(
        pages: List<String?>,
        append: LoadState = NotLoading.Idle
    ) = Append(
        pages = pages.map {
            if (it != null) listOf(it) else emptyList()
        }.toTransformablePages(),
        placeholdersAfter = 0,
        combinedLoadStates = localLoadStatesOf(appendLocal = append)
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
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshStartDone() = runBlockingTest {
        assertInsertData(
            listOf(
                refresh(pages = listOf("A", "a1"))
            ),
            flowOf(
                refresh(pages = listOf("a1"), prepend = NotLoading.Done)
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEndDone() = runBlockingTest {
        assertInsertData(
            listOf(
                refresh(pages = listOf("a1", "END"))
            ),
            flowOf(
                refresh(pages = listOf("a1"), append = NotLoading.Done)
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshBothDone() = runBlockingTest {
        assertInsertData(
            listOf(
                refresh(pages = listOf("A", "a1", "END"))
            ),
            flowOf(
                refresh(
                    pages = listOf("a1"),
                    prepend = NotLoading.Done,
                    append = NotLoading.Done
                )
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEmptyNoop() = runBlockingTest {
        assertInsertData(
            listOf(
                // should only get one item when initial page is endOfPaginationReached is false on
                // both sides.
                refresh(pages = listOf<String>())
            ),
            flowOf(
                refresh(pages = listOf())
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEmptyStartDone() = runBlockingTest {
        assertInsertData(
            listOf(
                // not enough data to create separators yet
                refresh(pages = listOf()),
                // now we can, since we know what's on the other side
                append(pages = listOf("A", "a1")),
                // empty should be noop, since it's not append = NotLoading(true)
                append(pages = listOf()),
                // append = NotLoading(true), so resolve final separator
                append(pages = listOf("END"))
            ),
            flowOf(
                refresh(pages = listOf(), prepend = NotLoading.Done),
                append(pages = listOf("a1")),
                append(pages = listOf()),
                append(pages = listOf(), append = NotLoading.Done)
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEmptyEndDone() = runBlockingTest {
        assertInsertData(
            listOf(
                // not enough data to create separators yet
                refresh(pages = listOf()),
                // now we can, since we know what's on the other side
                prepend(pages = listOf("a1", "END")),
                // empty should be noop, since it's not prepend = NotLoading(true)
                prepend(pages = listOf()),
                // prepend = NotLoading(true), so resolve final separator
                prepend(pages = listOf("A"))
            ),
            flowOf(
                refresh(pages = listOf(), append = NotLoading.Done),
                prepend(pages = listOf("a1")),
                prepend(pages = listOf()),
                prepend(pages = listOf(), prepend = NotLoading.Done)
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun emptyDropResetsDeferredFrontSeparator() = runBlockingTest {
        assertInsertData(
            listOf(
                // not enough data to create separators yet
                refresh(pages = listOf()),
                // don't insert a separator, since a drop occurred (even though it's 0 size)
                append(pages = listOf("a1")),
                // but now add the separator, since start is done again
                prepend(pages = listOf("A"))
            ),
            flowOf(
                refresh(pages = listOf(), prepend = NotLoading.Done),
                drop(loadType = PREPEND, count = 0),
                append(pages = listOf("a1")),
                prepend(pages = listOf(), prepend = NotLoading.Done)
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun emptyDropResetsDeferredEndSeparator() = runBlockingTest {
        assertInsertData(
            listOf(
                // not enough data to create separators yet
                refresh(pages = listOf()),
                // don't insert a separator, since a drop occurred (even though it's 0 size)
                prepend(pages = listOf("a1")),
                // but now add the separator, since end is done again
                append(pages = listOf("END"))
            ),
            flowOf(
                refresh(pages = listOf(), append = NotLoading.Done),
                drop(loadType = APPEND, count = 0),
                prepend(pages = listOf("a1")),
                append(pages = listOf(), append = NotLoading.Done)
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEmptyStart() = runBlockingTest {
        assertInsertData(
            listOf(
                refresh(pages = listOf()),
                // not enough data to create separators yet
                prepend(pages = listOf("a1"))
            ),
            flowOf(
                refresh(pages = listOf()),
                prepend(pages = listOf("a1"))
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun refreshEmptyEnd() = runBlockingTest {
        assertInsertData(
            listOf(
                refresh(pages = listOf()),
                // not enough data to create separators yet
                append(pages = listOf("a1"))
            ),
            flowOf(
                refresh(pages = listOf()),
                append(pages = listOf("a1"))
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
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
                    placeholdersBefore = 0,
                    placeholdersAfter = 1,
                    combinedLoadStates = localLoadStatesOf(prependLocal = NotLoading.Done)
                ),
                drop(loadType = PREPEND, count = 3)
            ),
            flowOf(
                refresh(
                    pages = listOf("a1", "b1"),
                    prepend = NotLoading.Done
                ),
                drop(loadType = PREPEND, count = 1)
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
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
                    placeholdersBefore = 0,
                    placeholdersAfter = 1,
                    combinedLoadStates = localLoadStatesOf(appendLocal = NotLoading.Done)
                ),
                drop(loadType = APPEND, count = 3)
            ),
            flowOf(
                refresh(
                    pages = listOf("a1", "b1"),
                    append = NotLoading.Done
                ),
                drop(loadType = APPEND, count = 1)
            ).insertEventSeparators(LETTER_SEPARATOR_GENERATOR).toList()
        )
    }

    @Test
    fun types() = runBlockingTest {
        open class Base
        data class PrimaryType(val string: String) : Base()
        data class SeparatorType(val string: String) : Base()

        assertInsertData(
            listOf(
                Refresh(
                    pages = listOf(
                        listOf(
                            PrimaryType("a1"),
                            SeparatorType("B"),
                            PrimaryType("b1")
                        )
                    ).toTransformablePages(),
                    placeholdersBefore = 0,
                    placeholdersAfter = 1,
                    combinedLoadStates = localLoadStatesOf()
                )
            ),
            flowOf(
                Refresh(
                    pages = listOf(listOf(PrimaryType("a1"), PrimaryType("b1")))
                        .toTransformablePages(),
                    placeholdersBefore = 0,
                    placeholdersAfter = 1,
                    combinedLoadStates = localLoadStatesOf()
                )
            ).insertEventSeparators<PrimaryType, Base> { before, after ->
                return@insertEventSeparators (if (before != null && after != null) {
                    SeparatorType("B")
                } else null)
            }.toList()
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
