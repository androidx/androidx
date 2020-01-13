/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.foundation.shape.border

import androidx.ui.graphics.Brush
import androidx.ui.graphics.Color
import androidx.ui.graphics.SolidColor
import androidx.ui.unit.Dp

/**
 * A border of a shape which will be drawn on top of the shape as an inner stroke.
 * This can also be used for the border of a table layout.
 *
 * @param brush the brush to paint the border with.
 * @param width the width of the border. Use [Dp.Hairline] for a hairline border.
 */
data class Border(val brush: Brush, val width: Dp)

/**
 * A border of a shape which will be drawn on top of the shape as an inner stroke.
 * This can also be used for the border of a table layout.
 *
 * @param color the color to fill the border with.
 * @param width the width of the border. Use [Dp.Hairline] for a hairline border.
 */
/*inline*/ fun Border(color: Color, width: Dp): Border =
    Border(brush = SolidColor(color), width = width)
