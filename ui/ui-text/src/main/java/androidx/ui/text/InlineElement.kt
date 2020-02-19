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

package androidx.ui.text

import androidx.ui.unit.TextUnit

/**
 * The configuration that specifies the size and the [TextVerticalAlign] type of a inline element.
 * @param width the width of the inline element, it must be specified in sp or em.
 * [TextUnit.Inherit] is not allowed.
 * @param height the height of the inline element, it must be specified in sp or em.
 * [TextUnit.Inherit] is not allowed.
 * @param textVerticalAlign the vertical alignment of the inline element within the text line.
 * Check [TextVerticalAlign] for more information.
 * @throws IllegalArgumentException if [TextUnit.Inherit] is passed to [width] or [height].
 */
data class InlineElementMetrics(
    val width: TextUnit,
    val height: TextUnit,
    val textVerticalAlign: TextVerticalAlign
) {
    init {
        require(!width.isInherit) { "width cannot be TextUnit.Inherit" }
        require(!height.isInherit) { "height cannot be TextUnit.Inherit" }
    }
}

/**
 * The interface that all inline elements should implement. It specifies that inline elements
 * should define their own sizes and [TextVerticalAlign]s.
 */
interface InlineElement {
    val inlineElementMetrics: InlineElementMetrics
}