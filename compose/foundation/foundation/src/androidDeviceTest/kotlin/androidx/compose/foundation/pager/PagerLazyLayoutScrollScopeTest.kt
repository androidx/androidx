/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PagerLazyLayoutScrollScopeTest : BasePagerTest(ParamConfig(Orientation.Horizontal)) {

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun calculateDistance_shouldNotReturnMoreThanMaxScrollOffset() {

        val pageSize =
            object : PageSize {
                override fun Density.calculateMainAxisPageSize(
                    availableSpace: Int,
                    pageSpacing: Int,
                ): Int {
                    return (availableSpace * 0.75).roundToInt()
                }
            }
        createPager(pageSize = { pageSize })

        val distance =
            rule.runOnIdle {
                runBlocking {
                    var calculatedDistance = 0
                    pagerState.scroll {
                        calculatedDistance =
                            LazyLayoutScrollScope(pagerState, this)
                                .calculateDistanceTo(DefaultPageCount - 1)
                    }
                    calculatedDistance
                }
            }

        assertThat(distance.toLong()).isAtLeast(pagerState.minScrollOffset)
        assertThat(distance.toLong()).isAtMost(pagerState.maxScrollOffset)
    }
}
