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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
        val screenHeight = LocalConfiguration.current.screenHeightDp
        return screenHeight.dp * verticalContentPaddingPercentage / 100
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
    fun horizontalContentPadding(): Dp {
        val screenWidth = LocalConfiguration.current.screenWidthDp
        return screenWidth.dp * horizontalContentPaddingPercentage / 100
    }

    /** Default minimum padding between the edge of the screen and the content. */
    val edgePadding = 2.dp
}
