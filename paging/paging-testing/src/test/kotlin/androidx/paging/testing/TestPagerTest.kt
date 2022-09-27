/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.paging.testing

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import androidx.paging.TestPagingSource
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class TestPagerTest {

    @Test
    fun refresh_nullKey() {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        runTest {
            val result = pager.run {
                refresh(null)
            } as LoadResult.Page

            assertThat(result.data).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4)).inOrder()
        }
    }

    @Test
    fun refresh_withInitialKey() {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        runTest {
            val result = pager.run {
                refresh(50)
            } as LoadResult.Page

            assertThat(result.data).containsExactlyElementsIn(listOf(50, 51, 52, 53, 54)).inOrder()
        }
    }

    @Test
    fun refresh_returnError() {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        runTest {
            source.errorNextLoad = true
            val result = pager.run {
                refresh()
            }
            assertTrue(result is LoadResult.Error)

            val page = pager.run {
                getLastLoadedPage()
            }
            assertThat(page).isNull()
        }
    }

    @Test
    fun refresh_returnInvalid() {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        runTest {
            source.nextLoadResult = LoadResult.Invalid()
            val result = pager.run {
                refresh()
            }
            assertTrue(result is LoadResult.Invalid)

            val page = pager.run {
                getLastLoadedPage()
            }
            assertThat(page).isNull()
        }
    }

    @Test
    fun refresh_invalidPagingSource() {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        runTest {
            source.invalidate()
            assertTrue(source.invalid)
            // simulate a PagingSource that returns LoadResult.Invalid when it's invalidated
            source.nextLoadResult = LoadResult.Invalid()

            assertThat(pager.refresh()).isInstanceOf(LoadResult.Invalid::class.java)
        }
    }

    @Test
    fun refresh_getLastLoadedPage() {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        runTest {
            val page: LoadResult.Page<Int, Int>? = pager.run {
                refresh()
                getLastLoadedPage()
            }
            assertThat(page).isNotNull()
            assertThat(page?.data).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4)).inOrder()
        }
    }

    @Test
    fun getLastLoadedPage_afterInvalidPagingSource() {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        runTest {
            val page = pager.run {
                refresh()
                append() // page should be this appended page
                source.invalidate()
                assertTrue(source.invalid)
                getLastLoadedPage()
            }
            assertThat(page).isNotNull()
            assertThat(page?.data).containsExactlyElementsIn(listOf(5, 6, 7)).inOrder()
        }
    }

    @Test
    fun refresh_getPages() {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        runTest {
            val pages = pager.run {
                refresh()
                getPages()
            }
            assertThat(pages).hasSize(1)
            assertThat(pages).containsExactlyElementsIn(
                listOf(
                    listOf(0, 1, 2, 3, 4).asPage()
                )
            ).inOrder()
        }
    }

    @Test
    fun getPages_multiplePages() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        pager.run {
            refresh(20)
            prepend()
        }
        assertThat(pager.getPages()).containsExactlyElementsIn(
            listOf(
                // prepend
                listOf(17, 18, 19).asPage(),
                // refresh
                listOf(20, 21, 22, 23, 24).asPage(),
            )
        ).inOrder()
    }

    @Test
    fun getPages_fromEmptyList() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)
        val pages = pager.run {
            getPages()
        }
        assertThat(pages).isEmpty()
    }

    @Test
    fun getPages_afterInvalidPagingSource() {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        runTest {
            val pages = pager.run {
                refresh()
                append()
                source.invalidate()
                assertTrue(source.invalid)
                getPages()
            }
            assertThat(pages).containsExactlyElementsIn(
                listOf(
                    listOf(0, 1, 2, 3, 4).asPage(),
                    listOf(5, 6, 7).asPage()
                )
            ).inOrder()
        }
    }

    @Test
    fun getPages_multiThread() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        var pages: List<LoadResult.Page<Int, Int>>? = null
        val job = launch {
            pager.run {
                refresh(20) // first
                pages = getPages() // third
                prepend() // fifth
            }
        }
        job.start()
        assertTrue(job.isActive)
        val pages2 = pager.run {
            delay(200) // let launch start first
            append() // second
            prepend() // fourth
            getPages() // sixth
        }

        advanceUntilIdle()
        assertThat(pages).containsExactlyElementsIn(
            listOf(
                // should contain first and second load
                listOf(20, 21, 22, 23, 24).asPage(), // refresh
                listOf(25, 26, 27).asPage(), // append
            )
        ).inOrder()
        assertThat(pages2).containsExactlyElementsIn(
            // should contain all loads
            listOf(
                listOf(14, 15, 16).asPage(),
                listOf(17, 18, 19).asPage(),
                listOf(20, 21, 22, 23, 24).asPage(),
                listOf(25, 26, 27).asPage(),
            )
        ).inOrder()
    }

    @Test
    fun multipleRefresh_onSinglePager_throws() {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        runTest {
            pager.run {
                // second refresh should throw since testPager is not mult-generational
                assertFailsWith<IllegalStateException> {
                    refresh()
                    refresh()
                }
            }
            assertTrue(source.invalid)
            // the first refresh should still have succeeded
            assertThat(pager.run {
                getPages()
            }).hasSize(1)
        }
    }

    @Test
    fun multipleRefresh_onMultiplePagers() = runTest {
        val source1 = TestPagingSource()
        val pager1 = TestPager(source1, CONFIG)

        // first gen
        val result1 = pager1.run {
            refresh()
        } as LoadResult.Page

        assertThat(result1.data).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4)).inOrder()

        // second gen
        val source2 = TestPagingSource()
        val pager2 = TestPager(source2, CONFIG)

        val result2 = pager2.run {
            refresh()
        } as LoadResult.Page

        assertThat(result2.data).containsExactlyElementsIn(listOf(0, 1, 2, 3, 4)).inOrder()
    }

    @Test
    fun simpleAppend() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        val result = pager.run {
            refresh(null)
            append()
        } as LoadResult.Page

        assertThat(result.data).containsExactlyElementsIn(listOf(5, 6, 7)).inOrder()
        assertThat(pager.getPages()).containsExactlyElementsIn(
            listOf(
                listOf(0, 1, 2, 3, 4).asPage(),
                listOf(5, 6, 7).asPage()
            )
        ).inOrder()
    }

    @Test
    fun simplePrepend() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        val result = pager.run {
            refresh(30)
            prepend()
        } as LoadResult.Page

        assertThat(result.data).containsExactlyElementsIn(listOf(27, 28, 29)).inOrder()
        // prepended pages should be inserted before refresh
        assertThat(pager.getPages()).containsExactlyElementsIn(
            listOf(
                // prepend
                listOf(27, 28, 29).asPage(),
                // refresh
                listOf(30, 31, 32, 33, 34).asPage()
            )
        ).inOrder()
    }

    @Test
    fun append_beforeRefresh_throws() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)
        assertFailsWith<IllegalStateException> {
            pager.run {
                append()
            }
        }
    }

    @Test
    fun prepend_beforeRefresh_throws() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)
        assertFailsWith<IllegalStateException> {
            pager.run {
                prepend()
            }
        }
    }

    @Test
    fun append_invalidPagingSource() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        val result = pager.run {
            refresh()
            source.invalidate()
            assertThat(source.invalid).isTrue()
            // simulate a PagingSource which returns LoadResult.Invalid when it's invalidated
            source.nextLoadResult = LoadResult.Invalid()
            append()
        }
        assertThat(result).isInstanceOf(LoadResult.Invalid::class.java)
    }

    @Test
    fun prepend_invalidPagingSource() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        val result = pager.run {
            refresh(initialKey = 20)
            source.invalidate()
            assertThat(source.invalid).isTrue()
            // simulate a PagingSource which returns LoadResult.Invalid when it's invalidated
            source.nextLoadResult = LoadResult.Invalid()
            prepend()
        }
        assertThat(result).isInstanceOf(LoadResult.Invalid::class.java)
    }

    @Test
    fun consecutive_append() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        pager.run {
            refresh(20)
            append()
            append()
        } as LoadResult.Page

        assertThat(pager.getPages()).containsExactlyElementsIn(
            listOf(
                listOf(20, 21, 22, 23, 24).asPage(),
                listOf(25, 26, 27).asPage(),
                listOf(28, 29, 30).asPage()
            )
        ).inOrder()
    }

    @Test
    fun consecutive_prepend() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        pager.run {
            refresh(20)
            prepend()
            prepend()
        } as LoadResult.Page

        // prepended pages should be ordered before the refresh
        assertThat(pager.getPages()).containsExactlyElementsIn(
            listOf(
                // 2nd prepend
                listOf(14, 15, 16).asPage(),
                // 1st prepend
                listOf(17, 18, 19).asPage(),
                // refresh
                listOf(20, 21, 22, 23, 24).asPage(),
            )
        ).inOrder()
    }

    @Test
    fun append_then_prepend() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        pager.run {
            refresh(20)
            append()
            prepend()
        } as LoadResult.Page

        assertThat(pager.getPages()).containsExactlyElementsIn(
            listOf(
                // prepend
                listOf(17, 18, 19).asPage(),
                // refresh
                listOf(20, 21, 22, 23, 24).asPage(),
                // append
                listOf(25, 26, 27).asPage(),
            )
        ).inOrder()
    }

    @Test
    fun prepend_then_append() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        pager.run {
            refresh(20)
            prepend()
            append()
        } as LoadResult.Page

        assertThat(pager.getPages()).containsExactlyElementsIn(
            listOf(
                // prepend
                listOf(17, 18, 19).asPage(),
                // refresh
                listOf(20, 21, 22, 23, 24).asPage(),
                // append
                listOf(25, 26, 27).asPage(),
            )
        ).inOrder()
    }

    @Test
    fun multiThread_loads() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)
        // load operations upon completion add an int to the list.
        // after all loads complete, we assert the order that the ints were added.
        val loadOrder = mutableListOf<Int>()

        val job = launch {
            pager.run {
                refresh(20).also { loadOrder.add(1) } // first load
                prepend().also { loadOrder.add(3) } // third load
                append().also { loadOrder.add(5) } // fifth load
            }
        }
        job.start()
        assertTrue(job.isActive)

        pager.run {
            // give some time for job to start
            delay(200)
            append().also { loadOrder.add(2) } // second load
            prepend().also { loadOrder.add(4) } // fourth load
        }

        advanceUntilIdle()
        assertThat(loadOrder).containsExactlyElementsIn(listOf(1, 2, 3, 4, 5)).inOrder()
        assertThat(pager.getPages()).containsExactlyElementsIn(
            listOf(
                listOf(14, 15, 16).asPage(),
                listOf(17, 18, 19).asPage(),
                listOf(20, 21, 22, 23, 24).asPage(),
                listOf(25, 26, 27).asPage(),
                listOf(28, 29, 30).asPage(),
            )
        ).inOrder()
    }

    @Test
    fun multiThread_operations() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)
        // operations upon completion add an int to the list.
        // after all operations complete, we assert the order that the ints were added.
        val loadOrder = mutableListOf<Int>()

        var lastLoadedPage: LoadResult.Page<Int, Int>? = null
        val job = launch {
            pager.run {
                refresh(20).also { loadOrder.add(1) } // first operation
                // third operation, should return first appended page
                lastLoadedPage = getLastLoadedPage().also { loadOrder.add(3) }
                append().also { loadOrder.add(5) } // fifth operation
                prepend().also { loadOrder.add(7) } // last operation
            }
        }
        job.start()
        assertTrue(job.isActive)

        val pages = pager.run {
            // give some time for job to start first
            delay(200)
            append().also { loadOrder.add(2) } // second operation
            prepend().also { loadOrder.add(4) } // fourth operation
            // sixth operation, should return 4 pages
            getPages().also { loadOrder.add(6) }
        }

        advanceUntilIdle()
        assertThat(loadOrder).containsExactlyElementsIn(
            listOf(1, 2, 3, 4, 5, 6, 7)
        ).inOrder()
        assertThat(lastLoadedPage).isEqualTo(
            listOf(25, 26, 27).asPage(),
        )
        // should not contain the second prepend, with a total of 4 pages
        assertThat(pages).containsExactlyElementsIn(
            listOf(
                listOf(17, 18, 19).asPage(), // first prepend
                listOf(20, 21, 22, 23, 24).asPage(), // refresh
                listOf(25, 26, 27).asPage(), // first append
                listOf(28, 29, 30).asPage(), // second append
            )
        ).inOrder()
    }

    @Test
    fun getPagingStateWithAnchorPosition_placeHoldersEnabled() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        val state = pager.run {
            refresh(20)
            prepend()
            append()
            getPagingState(7)
        }
        assertThat(state).isEqualTo(
            PagingState(
                pages = listOf(
                    listOf(17, 18, 19).asPage(),
                    // refresh
                    listOf(20, 21, 22, 23, 24).asPage(),
                    // append
                    listOf(25, 26, 27).asPage(),
                ),
                anchorPosition = 7,
                config = CONFIG,
                leadingPlaceholderCount = 17
            )
        )
    }

    @Test
    fun getPagingStateWithAnchorPosition_placeHoldersDisabled() = runTest {
        val source = TestPagingSource(placeholdersEnabled = false)
        val config = PagingConfig(
            pageSize = 3,
            initialLoadSize = 5,
            enablePlaceholders = false
        )
        val pager = TestPager(source, config)

        val state = pager.run {
            refresh(20)
            prepend()
            append()
            getPagingState(7)
        }
        assertThat(state).isEqualTo(
            PagingState(
                pages = listOf(
                    listOf(17, 18, 19).asPage(placeholdersEnabled = false),
                    // refresh
                    listOf(20, 21, 22, 23, 24).asPage(placeholdersEnabled = false),
                    // append
                    listOf(25, 26, 27).asPage(placeholdersEnabled = false),
                ),
                anchorPosition = 7,
                config = config,
                leadingPlaceholderCount = 0
            )
        )
    }

    @Test
    fun getPagingStateWithAnchorPosition_indexOutOfBoundsWithPlaceholders() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        val msg = assertFailsWith<IllegalStateException> {
            pager.run {
                refresh()
                append()
                getPagingState(-1)
            }
        }.localizedMessage
        assertThat(msg).isEqualTo(
            "anchorPosition -1 is out of bounds between [0..${ITEM_COUNT - 1}]. Please " +
                "provide a valid anchorPosition."
        )

        val msg2 = assertFailsWith<IllegalStateException> {
            pager.run {
                getPagingState(ITEM_COUNT)
            }
        }.localizedMessage
        assertThat(msg2).isEqualTo(
            "anchorPosition $ITEM_COUNT is out of bounds between [0..${ITEM_COUNT - 1}]. " +
                "Please provide a valid anchorPosition."
        )
    }

    @Test
    fun getPagingStateWithAnchorPosition_indexOutOfBoundsWithoutPlaceholders() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(
            source,
            PagingConfig(
                pageSize = 3,
                initialLoadSize = 5,
                enablePlaceholders = false
            )
        )

        val msg = assertFailsWith<IllegalStateException> {
            pager.run {
                refresh()
                append()
                getPagingState(-1)
            }
        }.localizedMessage
        assertThat(msg).isEqualTo(
            "anchorPosition -1 is out of bounds between [0..7]. Please " +
                "provide a valid anchorPosition."
        )

        // total loaded items = 8, anchorPos with index 8 should be out of bounds
        val msg2 = assertFailsWith<IllegalStateException> {
            pager.run {
                getPagingState(8)
            }
        }.localizedMessage
        assertThat(msg2).isEqualTo(
            "anchorPosition 8 is out of bounds between [0..7]. Please " +
        "provide a valid anchorPosition."
        )
    }

    private val CONFIG = PagingConfig(
        pageSize = 3,
        initialLoadSize = 5,
    )

    private fun List<Int>.asPage(placeholdersEnabled: Boolean = true): LoadResult.Page<Int, Int> {
        val itemsBefore = if (placeholdersEnabled) {
            if (first() == 0) 0 else first()
        } else {
            Int.MIN_VALUE
        }
        val itemsAfter = if (placeholdersEnabled) {
            if (last() == ITEM_COUNT - 1) 0 else ITEM_COUNT - 1 - last()
        } else {
            Int.MIN_VALUE
        }
        return LoadResult.Page(
            data = this,
            prevKey = if (first() == 0) null else first() - 1,
            nextKey = if (last() == ITEM_COUNT - 1) null else last() + 1,
            itemsBefore = itemsBefore,
            itemsAfter = itemsAfter
        )
    }

    private val ITEM_COUNT = 100
}