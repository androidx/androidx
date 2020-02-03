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

package androidx.ui.core

import androidx.ui.unit.IntPx

/**
 * Defines a layout line that can be used by layout models to align and position layout children.
 * When a [Layout] or [ComplexLayout] provides a value for a particular [AlignmentLine], this can
 * be read by the parent of the layout after measuring, using the corresponding [Placeable]
 * instance. Also, the value will be automatically inherited by the parent which will offset
 * the value by the position of the child within itself.
 *
 * [AlignmentLine]s cannot be created directly, please create [VerticalAlignmentLine] or
 * [HorizontalAlignmentLine] instances instead.
 *
 * @see VerticalAlignmentLine
 * @see HorizontalAlignmentLine
 */
sealed class AlignmentLine(
    internal val merger: (IntPx, IntPx) -> IntPx
)

/**
 * Merges two values of the current [alignment line][AlignmentLine].
 */
fun AlignmentLine.merge(position1: IntPx, position2: IntPx) = merger(position1, position2)

/**
 * A vertical [AlignmentLine].
 *
 * @param merger How to merge two alignment line values defined by different children
 */
class VerticalAlignmentLine(merger: (IntPx, IntPx) -> IntPx) : AlignmentLine(merger)

/**
 * A horizontal [AlignmentLine].
 *
 * @param merger How to merge two alignment line values defined by different children
 */
class HorizontalAlignmentLine(merger: (IntPx, IntPx) -> IntPx) : AlignmentLine(merger)
