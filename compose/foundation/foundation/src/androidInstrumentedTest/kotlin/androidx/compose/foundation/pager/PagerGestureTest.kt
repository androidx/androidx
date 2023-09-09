/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.foundation.gestures.snapping.MinFlingVelocityDp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.filters.MediumTest
import kotlin.math.max
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(Parameterized::class)
class PagerGestureTest(private val paramConfig: ParamConfig) : BasePagerTest(config = paramConfig) {

    @Test
    fun swipeWithLowVelocity_bouncesBack_shouldNotRunThePrefetch() {
        val initialPage = 5
        createPager(initialPage = initialPage, modifier = Modifier.fillMaxSize())
        val swipeValue = 0.4f
        val delta = pagerSize * swipeValue * scrollForwardSign

        runAndWaitForPageSettling {
            onPager().performTouchInput {
                swipeWithVelocityAcrossMainAxis(
                    with(rule.density) { 0.5f * MinFlingVelocityDp.toPx() },
                    delta
                )
            }
        }

        rule.onNodeWithTag(
            max(
                0,
                initialPage - paramConfig.beyondBoundsPageCount - 1
            ).toString()
        ).assertDoesNotExist()
    }
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = AllOrientationsParams
    }
}
