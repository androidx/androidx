/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.pager

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

open class BaseParameterizedGlimmerPagerTest {

    data class GlimmerPagerParamConfig(
        val reverseLayout: Boolean = false,
        val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        val scrollSign: Int
            get() = if (reverseLayout == (layoutDirection == LayoutDirection.Rtl)) 1 else -1
    }

    @Composable
    fun GlimmerParameterizedPager(
        config: GlimmerPagerParamConfig,
        state: GlimmerPagerState,
        modifier: Modifier = Modifier,
        contentPadding: PaddingValues = PaddingValues.Zero,
        pageSpacing: Dp = 0.dp,
        beyondViewportPageCount: Int = 0,
        verticalAlignment: Alignment.Vertical = Alignment.Bottom,
        userScrollEnabled: Boolean = true,
        pageContent: @Composable GlimmerPagerScope.(page: Int) -> Unit,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides config.layoutDirection) {
            GlimmerHorizontalPager(
                modifier = modifier.testTag("pager"),
                state = state,
                reverseLayout = config.reverseLayout,
                contentPadding = contentPadding,
                pageSpacing = pageSpacing,
                beyondViewportPageCount = beyondViewportPageCount,
                verticalAlignment = verticalAlignment,
                userScrollEnabled = userScrollEnabled,
                pageContent = pageContent,
            )
        }
    }

    companion object {
        val AllGlimmerPagerTestParams =
            mutableListOf<GlimmerPagerParamConfig>().apply {
                for (reverseLayout in listOf(false, true)) {
                    for (layoutDirection in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
                        add(
                            GlimmerPagerParamConfig(
                                reverseLayout = reverseLayout,
                                layoutDirection = layoutDirection,
                            )
                        )
                    }
                }
            }
    }
}
