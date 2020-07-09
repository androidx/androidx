/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.compose.Immutable
import androidx.compose.Stable
import androidx.ui.graphics.Brush
import androidx.ui.graphics.Color
import androidx.ui.graphics.SolidColor
import androidx.ui.unit.Dp

/**
 * Class to specify border appearance.
 *
 * @param size size of the border in [Dp]. Use [Dp.Hairline] for one-pixel border.
 * @param brush brush to paint the border with
 */
@Immutable
data class Border(val size: Dp, val brush: Brush)

/**
 * Create [Border] class with size and [Color]
 *
 * @param size size of the border in [Dp]. Use [Dp.Hairline] for one-pixel border.
 * @param color color to paint the border with
 */
@Stable
fun Border(size: Dp, color: Color) = Border(size, SolidColor(color))