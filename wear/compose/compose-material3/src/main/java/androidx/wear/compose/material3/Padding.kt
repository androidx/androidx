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

import androidx.compose.ui.unit.dp

internal object PaddingDefaults {

    /**
     * Vertical padding between top and bottom edges of the screen and the content for full screen
     * components, as a percentage.
     */
    val verticalContentPaddingPercentage = 10f

    /**
     * Horizontal padding between start and end edges of the screen and the content for full screen
     * components, as a percentage.
     */
    val horizontalContentPaddingPercentage = 5.2f

    /** Default minimum padding between the edge of the screen and the content. */
    val edgePadding = 2.dp
}
