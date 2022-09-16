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
import androidx.paging.TestPagingSource
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            assertFailsWith<IllegalStateException> {
                pager.run {
                    refresh()
                }
            }
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
                    LoadResult.Page(
                        data = listOf(0, 1, 2, 3, 4),
                        prevKey = null,
                        nextKey = 5,
                        itemsBefore = 0,
                        itemsAfter = 95
                    )
                )
            ).inOrder()
        }
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
                    LoadResult.Page(
                        data = listOf(0, 1, 2, 3, 4),
                        prevKey = null,
                        nextKey = 5,
                        itemsBefore = 0,
                        itemsAfter = 95
                    ),
                    LoadResult.Page(
                        data = listOf(5, 6, 7),
                        prevKey = 4,
                        nextKey = 8,
                        itemsBefore = 5,
                        itemsAfter = 92
                    )
                )
            ).inOrder()
        }
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
                LoadResult.Page(
                    data = listOf(0, 1, 2, 3, 4),
                    prevKey = null,
                    nextKey = 5,
                    itemsBefore = 0,
                    itemsAfter = 95
                ),
                LoadResult.Page(
                    data = listOf(5, 6, 7),
                    prevKey = 4,
                    nextKey = 8,
                    itemsBefore = 5,
                    itemsAfter = 92
                )
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
                LoadResult.Page(
                    data = listOf(27, 28, 29),
                    prevKey = 26,
                    nextKey = 30,
                    itemsBefore = 27,
                    itemsAfter = 70
                ),
                // refresh
                LoadResult.Page(
                    data = listOf(30, 31, 32, 33, 34),
                    prevKey = 29,
                    nextKey = 35,
                    itemsBefore = 30,
                    itemsAfter = 65
                ),
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

        assertFailsWith<IllegalStateException> {
            pager.run {
                refresh()
                source.invalidate()
                append()
            }
        }
    }

    @Test
    fun prepend_invalidPagingSource() = runTest {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        assertFailsWith<IllegalStateException> {
            pager.run {
                refresh()
                source.invalidate()
                prepend()
            }
        }
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
                LoadResult.Page(
                    data = listOf(20, 21, 22, 23, 24),
                    prevKey = 19,
                    nextKey = 25,
                    itemsBefore = 20,
                    itemsAfter = 75
                ),
                LoadResult.Page(
                    data = listOf(25, 26, 27),
                    prevKey = 24,
                    nextKey = 28,
                    itemsBefore = 25,
                    itemsAfter = 72
                ),
                LoadResult.Page(
                    data = listOf(28, 29, 30),
                    prevKey = 27,
                    nextKey = 31,
                    itemsBefore = 28,
                    itemsAfter = 69
                )
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
                LoadResult.Page(
                    data = listOf(14, 15, 16),
                    prevKey = 13,
                    nextKey = 17,
                    itemsBefore = 14,
                    itemsAfter = 83
                ),
                // 1st prepend
                LoadResult.Page(
                    data = listOf(17, 18, 19),
                    prevKey = 16,
                    nextKey = 20,
                    itemsBefore = 17,
                    itemsAfter = 80
                ),
                // refresh
                LoadResult.Page(
                    data = listOf(20, 21, 22, 23, 24),
                    prevKey = 19,
                    nextKey = 25,
                    itemsBefore = 20,
                    itemsAfter = 75
                ),
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
                LoadResult.Page(
                    data = listOf(17, 18, 19),
                    prevKey = 16,
                    nextKey = 20,
                    itemsBefore = 17,
                    itemsAfter = 80
                ),
                // refresh
                LoadResult.Page(
                    data = listOf(20, 21, 22, 23, 24),
                    prevKey = 19,
                    nextKey = 25,
                    itemsBefore = 20,
                    itemsAfter = 75
                ),
                // append
                LoadResult.Page(
                    data = listOf(25, 26, 27),
                    prevKey = 24,
                    nextKey = 28,
                    itemsBefore = 25,
                    itemsAfter = 72
                ),
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
                LoadResult.Page(
                    data = listOf(17, 18, 19),
                    prevKey = 16,
                    nextKey = 20,
                    itemsBefore = 17,
                    itemsAfter = 80
                ),
                // refresh
                LoadResult.Page(
                    data = listOf(20, 21, 22, 23, 24),
                    prevKey = 19,
                    nextKey = 25,
                    itemsBefore = 20,
                    itemsAfter = 75
                ),
                // append
                LoadResult.Page(
                    data = listOf(25, 26, 27),
                    prevKey = 24,
                    nextKey = 28,
                    itemsBefore = 25,
                    itemsAfter = 72
                ),
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

        job.invokeOnCompletion {
            launch {
                assertThat(loadOrder).containsExactlyElementsIn(listOf(1, 2, 3, 4, 5)).inOrder()
                assertThat(pager.getPages()).containsExactlyElementsIn(
                    listOf(
                        LoadResult.Page(
                            data = listOf(14, 15, 16),
                            prevKey = 13,
                            nextKey = 17,
                            itemsBefore = 14,
                            itemsAfter = 83
                        ),
                        LoadResult.Page(
                            data = listOf(17, 18, 19),
                            prevKey = 16,
                            nextKey = 20,
                            itemsBefore = 17,
                            itemsAfter = 80
                        ),
                        LoadResult.Page(
                            data = listOf(20, 21, 22, 23, 24),
                            prevKey = 19,
                            nextKey = 25,
                            itemsBefore = 20,
                            itemsAfter = 75
                        ),
                        LoadResult.Page(
                            data = listOf(25, 26, 27),
                            prevKey = 24,
                            nextKey = 28,
                            itemsBefore = 25,
                            itemsAfter = 72
                        ),
                        LoadResult.Page(
                            data = listOf(28, 29, 30),
                            prevKey = 27,
                            nextKey = 31,
                            itemsBefore = 28,
                            itemsAfter = 69
                        ),
                    )
                ).inOrder()
            }
        }
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

        job.invokeOnCompletion {
            launch {
                assertThat(loadOrder).containsExactlyElementsIn(
                    listOf(1, 2, 3, 4, 5, 6, 7)
                ).inOrder()
                assertThat(lastLoadedPage).isEqualTo(
                    LoadResult.Page(
                        data = listOf(25, 26, 27),
                        prevKey = 24,
                        nextKey = 28,
                        itemsBefore = 25,
                        itemsAfter = 72
                    ),
                )
                // should not contain the second prepend, with a total of 4 pages
                assertThat(pages).containsExactlyElementsIn(
                    listOf(
                        LoadResult.Page( // first prepend
                            data = listOf(17, 18, 19),
                            prevKey = 16,
                            nextKey = 20,
                            itemsBefore = 17,
                            itemsAfter = 80
                        ),
                        LoadResult.Page( // refresh
                            data = listOf(20, 21, 22, 23, 24),
                            prevKey = 19,
                            nextKey = 25,
                            itemsBefore = 20,
                            itemsAfter = 75
                        ),
                        LoadResult.Page( // first append
                            data = listOf(25, 26, 27),
                            prevKey = 24,
                            nextKey = 28,
                            itemsBefore = 25,
                            itemsAfter = 72
                        ),
                        LoadResult.Page( // second append
                            data = listOf(28, 29, 30),
                            prevKey = 27,
                            nextKey = 31,
                            itemsBefore = 28,
                            itemsAfter = 69
                        ),
                    )
                ).inOrder()
            }
        }
    }

    private val CONFIG = PagingConfig(
        pageSize = 3,
        initialLoadSize = 5,
    )
}