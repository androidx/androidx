/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.wear

import androidx.compose.remote.creation.compose.state.RemoteColor

/**
 * Defines a brush for a Wear Widget surface.
 *
 * This class acts similar to [androidx.compose.remote.creation.compose.shaders.RemoteBrush] but it
 * restricts the available options to ensure compatibility with surfaces that support a limited
 * feature set, such as [WearWidgetDocument.background].
 */
public sealed class WearWidgetBrush {

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each element from outside in.
     *
     * Elements wrap one another in a chain from left to right; an [Element] that appears to the
     * left of another in a `+` expression or in [operation]'s parameter order affects all the
     * elements that appear after it. [foldIn] may be used to accumulate a value starting from the
     * parent or head of the brush chain to the final wrapped child.
     */
    internal abstract fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    /**
     * Concatenates this brush with another.
     *
     * Returns a [WearWidgetBrush] representing this brush followed by [other] in sequence.
     */
    internal open infix fun then(other: WearWidgetBrush): WearWidgetBrush =
        if (other === WearWidgetBrush) this else Combined(this, other)

    /** A single element contained within a [WearWidgetBrush] chain. */
    internal data class Element(
        // TODO: b/470080675 - Change it to
        // [androidx.compose.remote.creation.compose.shaders.RemoteBrush] when public.
        internal val color: RemoteColor
    ) : WearWidgetBrush() {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)
    }

    /**
     * A node in a [WearWidgetBrush] chain. A Combined always contains at least two elements; a
     * Brush [outer] that wraps around the Brush [inner].
     */
    internal data class Combined(
        internal val outer: WearWidgetBrush,
        internal val inner: WearWidgetBrush,
    ) : WearWidgetBrush() {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            inner.foldIn(outer.foldIn(initial, operation), operation)
    }

    /**
     * The [WearWidgetBrush] companion object is the empty, default, or starter [WearWidgetBrush]
     * that contains no elements.
     *
     * Use it to create a new [WearWidgetBrush] using modifier extension factory functions.
     *
     * Example: `WearWidgetBrush.color(Color.Black.rc)`
     */
    public companion object : WearWidgetBrush() {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial

        override fun then(other: WearWidgetBrush): WearWidgetBrush = other
    }
}

/**
 * Creates a [WearWidgetBrush] with a solid color.
 *
 * @param color The [RemoteColor] to use for the brush.
 */
public fun WearWidgetBrush.color(color: RemoteColor): WearWidgetBrush =
    then(WearWidgetBrush.Element(color))
