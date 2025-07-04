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
package androidx.compose.ui.layout

/**
 * A collection of [Ruler]s used to define a Rectangle.
 *
 * @sample androidx.compose.ui.samples.WindowInsetsRulersSample
 */
interface RectRulers {
    /** The left position of the rectangle. */
    val left: VerticalRuler

    /** The top position of the rectangle. */
    val top: HorizontalRuler

    /** The right position of the rectangle */
    val right: VerticalRuler

    /** The bottom position of the rectangle */
    val bottom: HorizontalRuler

    companion object
}

/** Creates a [RectRulers]. */
fun RectRulers(): RectRulers = RectRulersImpl(null)

internal fun RectRulers(name: String): RectRulers = RectRulersImpl(name)

private class RectRulersImpl(private val name: String?) : RectRulers {
    override var left: VerticalRuler = VerticalRuler()
    override var top: HorizontalRuler = HorizontalRuler()
    override var right: VerticalRuler = VerticalRuler()
    override var bottom: HorizontalRuler = HorizontalRuler()

    override fun toString(): String {
        return if (name != null) "RectRulers($name)" else super.toString()
    }
}

/**
 * Merges multiple [RectRulers] into a single [RectRulers], using the inner-most value. That is, the
 * [RectRulers.left] will be the greatest [RectRulers.left], the [RectRulers.top] will be the
 * greatest [RectRulers.top], the [RectRulers.right] will be the least [RectRulers.right], and the
 * [RectRulers.bottom] will be the least of all [rulers].
 *
 * When [rulers] provide non-overlapping values, the result may have negative size. For example, if
 * one [RectRulers] provides (10, 20, 30, 40) as their ruler values, and another provides (1, 1, 5,
 * 5), the merged result will be (10, 20, 5, 5).
 *
 * If one of the [rulers] does not provide a value, it will not be considered in the calculation.
 */
fun RectRulers.Companion.innermostOf(vararg rulers: RectRulers): RectRulers =
    InnerRectRulers(rulers)

private class InnerRectRulers(private val rulers: Array<out RectRulers>) : RectRulers {
    override val left: VerticalRuler = VerticalRuler.maxOf(*Array(rulers.size) { rulers[it].left })
    override val top: HorizontalRuler =
        HorizontalRuler.maxOf(*Array(rulers.size) { rulers[it].top })
    override val right: VerticalRuler =
        VerticalRuler.minOf(*Array(rulers.size) { rulers[it].right })
    override val bottom: HorizontalRuler =
        HorizontalRuler.minOf(*Array(rulers.size) { rulers[it].bottom })

    override fun toString(): String {
        return rulers.joinToString(prefix = "innermostOf(", postfix = ")")
    }
}

/**
 * Merges multiple [RectRulers] into a single [RectRulers], using the outer-most value. That is, the
 * [RectRulers.left] will be the least [RectRulers.left], the [RectRulers.top] will be the least
 * [RectRulers.top], the [RectRulers.right] will be the greatest [RectRulers.right], and the
 * [RectRulers.bottom] will be the greatest of all [rulers].
 *
 * If one of the [rulers] does not provide a value, it will not be considered in the calculation.
 */
fun RectRulers.Companion.outermostOf(vararg rulers: RectRulers): RectRulers =
    OuterRectRulers(rulers)

private class OuterRectRulers(private val rulers: Array<out RectRulers>) : RectRulers {
    override val left: VerticalRuler = VerticalRuler.minOf(*Array(rulers.size) { rulers[it].left })
    override val top: HorizontalRuler =
        HorizontalRuler.minOf(*Array(rulers.size) { rulers[it].top })
    override val right: VerticalRuler =
        VerticalRuler.maxOf(*Array(rulers.size) { rulers[it].right })
    override val bottom: HorizontalRuler =
        HorizontalRuler.maxOf(*Array(rulers.size) { rulers[it].bottom })

    override fun toString(): String {
        return rulers.joinToString(prefix = "outermostOf(", postfix = ")")
    }
}
