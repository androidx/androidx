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

package androidx.wear.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

internal object PaddingDefaults {

    /**
     * Vertical padding between top and bottom edges of the screen and the content for full screen
     * components, as a percentage.
     */
    val verticalContentPaddingPercentage = 10f

    /**
     * Vertical padding between top and bottom edges of the screen and the content for full screen
     * components, as a dp.
     */
    @Composable
    fun verticalContentPadding(): Dp {
        // Use screenWidthDp rather than screenHeightDp because circular Wear screens have equal
        // physical width and height, whereas screenHeightDp can be reduced by window insets such
        // as the global status bar.
        val screenWidth = LocalConfiguration.current.screenWidthDp
        return ceilDp(screenWidth.dp * verticalContentPaddingPercentage / 100)
    }

    /**
     * Horizontal padding between start and end edges of the screen and the content for full screen
     * components, as a percentage.
     */
    val horizontalContentPaddingPercentage = 5.2f

    /**
     * Horizontal padding between start and end edges of the screen and the content for full screen
     * components, as a dp.
     */
    @Composable
    fun horizontalContentPadding(percentage: Float = horizontalContentPaddingPercentage): Dp {
        val screenWidth = LocalConfiguration.current.screenWidthDp
        return ceilDp(screenWidth.dp * percentage / 100)
    }

    /** Default minimum padding between the edge of the screen and the content. */
    val edgePadding = 2.dp
}

/**
 * Returns a fraction of the screen width in Dp, rounded up to the next integer Dp.
 *
 * Typically used for determining screen-relative sizes (both horizontal and vertical) on circular
 * Wear screens, because `Configuration.screenHeightDp` can be affected by window insets such as the
 * global status bar.
 */
@Composable
internal fun screenWidthFraction(fraction: Float): Dp =
    ceilDp(LocalConfiguration.current.screenWidthDp.dp * fraction)

internal const val LARGE_VERTICAL_CONTENT_PADDING_FRACTION = 0.23f
internal const val SMALL_VERTICAL_CONTENT_PADDING_FRACTION = 0.13f

@Composable private fun ceilDp(dp: Dp): Dp = with(LocalDensity.current) { Dp(ceil(dp.value)) }
