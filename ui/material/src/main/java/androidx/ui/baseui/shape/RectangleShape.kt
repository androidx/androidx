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

package androidx.ui.baseui.shape

import androidx.ui.core.DensityReceiver
import androidx.ui.core.PxSize
import androidx.ui.core.toRect
import androidx.ui.engine.geometry.Outline

/**
 * A shape describing the rectangle.
 *
 * @param border optional border to draw on top of the shape
 */
data class RectangleShape(
    override val border: Border? = null
) : Shape {
    override fun DensityReceiver.createOutline(size: PxSize) =
        Outline.Rectangle(size.toRect())
}
