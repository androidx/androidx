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

package androidx.paging

import androidx.paging.PagingSource.LoadResult
import TestPager
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class TestPagerTest {

    @Test
    fun refresh_returnPage() {
        val source = TestPagingSource()
        val pager = TestPager(source, CONFIG)

        runTest {
            val result = pager.run {
                refresh()
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
    fun refresh_getlastPage() {
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

    private val CONFIG = PagingConfig(
        pageSize = 3,
        initialLoadSize = 5,
    )
}