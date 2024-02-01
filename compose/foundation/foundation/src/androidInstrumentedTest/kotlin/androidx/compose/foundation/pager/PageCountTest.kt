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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PageCountTest(val config: ParamConfig) : BasePagerTest(config) {

    @Test
    fun pageCountIsZero_checkNoPagesArePlaced() {
        // Arrange
        // Act
        createPager(pageCount = { 0 }, modifier = Modifier.fillMaxSize())

        // Assert
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun pageCountIsMax_shouldAllowScroll() {
        createPager(pageCount = { Int.MAX_VALUE }, modifier = Modifier.size(500.dp), pageSize = {
            PageSize.Fixed(200.dp)
        })
        val initialPage = pagerState.currentPage
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(2000f)
        }

        rule.runOnIdle {
            Truth.assertThat(initialPage).isNotEqualTo(pagerState.currentPage)
        }
    }

    @Test
    fun pageCountIsMax_shouldNotAllowScrollBeyondMax_fullPages() {
        createPager(pageCount = { Int.MAX_VALUE }, modifier = Modifier.fillMaxSize())
        rule.runOnIdle {
            runBlocking {
                pagerState.scrollToPage(Int.MAX_VALUE)
            }
        }

        rule.runOnIdle {
            Truth.assertThat(pagerState.currentPage).isEqualTo(Int.MAX_VALUE - 1)
        }

        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(20000f)
        }

        rule.runOnIdle {
            Truth.assertThat(pagerState.currentPage).isEqualTo(Int.MAX_VALUE - 1)
        }
    }

    @Test
    fun pageCountIsMax_shouldNotAllowScrollBeyondMax_fixesSizedPages() {
        createPager(pageCount = { Int.MAX_VALUE }, pageSize = {
            PageSize.Fixed(100.dp)
        }, modifier = Modifier.size(500.dp))
        rule.runOnIdle {
            runBlocking {
                pagerState.scrollToPage(Int.MAX_VALUE)
            }
        }

        val currentPage = pagerState.currentPage

        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(20000f)
        }

        rule.runOnIdle {
            Truth.assertThat(pagerState.currentPage).isEqualTo(currentPage)
        }
    }

    @Test
    fun pageCountIsMax_shouldSettleAfterSmallScroll() {
        createPager(pageCount = { Int.MAX_VALUE }, modifier = Modifier.fillMaxSize())
        rule.runOnIdle {
            runBlocking {
                pagerState.scrollToPage(Int.MAX_VALUE)
            }
        }

        rule.runOnIdle {
            Truth.assertThat(pagerState.currentPage).isEqualTo(Int.MAX_VALUE - 1)
        }

        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(100f, delta = pageSize * 0.2f * scrollForwardSign * -1)
        }

        rule.runOnIdle {
            Truth.assertThat(pagerState.currentPage).isEqualTo(Int.MAX_VALUE - 1)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = AllOrientationsParams
    }
}
