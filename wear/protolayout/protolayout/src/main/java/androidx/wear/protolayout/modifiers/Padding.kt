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

package androidx.wear.protolayout.modifiers

import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.types.dp

/**
 * Applies [all] dp of additional space along each edge of the content, left, top, right and bottom.
 */
fun LayoutModifier.padding(@Dimension(DP) all: Float): LayoutModifier =
    this then BasePaddingElement(start = all, top = all, end = all, bottom = all, rtlAware = false)

/**
 * Creates a [Padding] that applies [all] dp of additional space along each edge of the content,
 * left, top, right and bottom.
 */
fun padding(@Dimension(DP) all: Float): Padding = Padding.Builder().setAll(all.dp).build()

/**
 * Applies [horizontal] dp of additional space along the left and right edges of the content and
 * [vertical] dp of additional space along the top and bottom edges of the content.
 */
fun LayoutModifier.padding(
    @Dimension(DP) horizontal: Float,
    @Dimension(DP) vertical: Float
): LayoutModifier = padding(horizontal, vertical, horizontal, vertical, rtlAware = false)

/**
 * Creates a [Padding] that applies [horizontal] dp of additional space along the left and right
 * edges of the content and [vertical] dp of additional space along the top and bottom edges of the
 * content.
 */
fun padding(@Dimension(DP) horizontal: Float, @Dimension(DP) vertical: Float): Padding =
    padding(horizontal, vertical, horizontal, vertical)

/**
 * Applies additional space along each edge of the content in [DP]: [start], [top], [end] and
 * [bottom]
 *
 * @param start The padding on the start of the content, depending on the layout direction, in [DP]
 *   and the value of [rtlAware].
 * @param top The padding at the top, in [DP].
 * @param end The padding on the end of the content, depending on the layout direction, in [DP] and
 *   the value of [rtlAware].
 * @param bottom The padding at the bottom, in [DP].
 * @param rtlAware specifies whether the [start]/[end] padding is aware of RTL support. If `true`,
 *   the values for [start]/[end] will follow the layout direction (i.e. [start] will refer to the
 *   right hand side of the container if the device is using an RTL locale). If `false`,
 *   [start]/[end] will always map to left/right, accordingly.
 */
fun LayoutModifier.padding(
    @Dimension(DP) start: Float = Float.NaN,
    @Dimension(DP) top: Float = Float.NaN,
    @Dimension(DP) end: Float = Float.NaN,
    @Dimension(DP) bottom: Float = Float.NaN,
    rtlAware: Boolean = true
): LayoutModifier =
    this then
        BasePaddingElement(
            start = start,
            top = top,
            end = end,
            bottom = bottom,
            rtlAware = rtlAware
        )

/** Applies additional space along each edge of the content. */
fun LayoutModifier.padding(padding: Padding): LayoutModifier =
    padding(
        start = padding.start?.value ?: Float.NaN,
        top = padding.top?.value ?: Float.NaN,
        end = padding.end?.value ?: Float.NaN,
        bottom = padding.bottom?.value ?: Float.NaN
    )

/**
 * Creates a [Padding] that applies additional space along each edge of the content in [DP]:
 * [start], [top], [end] and [bottom]
 *
 * @param start The padding on the start of the content, depending on the layout direction, in [DP]
 *   and the value of [rtlAware].
 * @param top The padding at the top, in [DP].
 * @param end The padding on the end of the content, depending on the layout direction, in [DP] and
 *   the value of [rtlAware].
 * @param bottom The padding at the bottom, in [DP].
 * @param rtlAware specifies whether the [start]/[end] padding is aware of RTL support. If `true`,
 *   the values for [start]/[end] will follow the layout direction (i.e. [start] will refer to the
 *   right hand side of the container if the device is using an RTL locale). If `false`,
 *   [start]/[end] will always map to left/right, accordingly.
 */
@Suppress("MissingJvmstatic") // Conflicts with the other overloads
fun padding(
    @Dimension(DP) start: Float = Float.NaN,
    @Dimension(DP) top: Float = Float.NaN,
    @Dimension(DP) end: Float = Float.NaN,
    @Dimension(DP) bottom: Float = Float.NaN,
    rtlAware: Boolean = true
): Padding =
    Padding.Builder()
        .apply {
            if (!start.isNaN()) {
                setStart(start.dp)
            }
            if (!top.isNaN()) {
                setTop(top.dp)
            }
            if (!end.isNaN()) {
                setEnd(end.dp)
            }
            if (!bottom.isNaN()) {
                setBottom(bottom.dp)
            }
        }
        .setRtlAware(rtlAware)
        .build()

internal class BasePaddingElement(
    val start: Float = Float.NaN,
    val top: Float = Float.NaN,
    val end: Float = Float.NaN,
    val bottom: Float = Float.NaN,
    val rtlAware: Boolean = true
) : LayoutModifier.Element {

    fun mergeTo(initial: Padding.Builder?): Padding.Builder =
        (initial ?: Padding.Builder()).apply {
            if (!start.isNaN()) {
                setStart(start.dp)
            }
            if (!top.isNaN()) {
                setTop(top.dp)
            }
            if (!end.isNaN()) {
                setEnd(end.dp)
            }
            if (!bottom.isNaN()) {
                setBottom(bottom.dp)
            }
            setRtlAware(rtlAware)
        }
}
