@file:OptIn(GlanceInternalApi::class)
/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.glance.Emittable
import androidx.glance.GlanceInternalApi
import androidx.glance.Modifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.PaddingModifier
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_END
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_START
import androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_TOP
import androidx.wear.tiles.LayoutElementBuilders.VerticalAlignment
import androidx.wear.tiles.ModifiersBuilders
import java.lang.IllegalArgumentException

@VerticalAlignment private fun Alignment.Vertical.toProto(): Int =
    when (this) {
        Alignment.Vertical.Top -> VERTICAL_ALIGN_TOP
        Alignment.Vertical.CenterVertically -> VERTICAL_ALIGN_CENTER
        Alignment.Vertical.Bottom -> VERTICAL_ALIGN_BOTTOM
        else -> throw IllegalArgumentException("Unknown vertical alignment type $this")
    }

@HorizontalAlignment private fun Alignment.Horizontal.toProto(): Int =
    when (this) {
        Alignment.Horizontal.Start -> HORIZONTAL_ALIGN_START
        Alignment.Horizontal.CenterHorizontally -> HORIZONTAL_ALIGN_CENTER
        Alignment.Horizontal.End -> HORIZONTAL_ALIGN_END
        else -> throw IllegalArgumentException("Unknown horizontal alignment type $this")
    }

private fun PaddingModifier.toProto(): ModifiersBuilders.Padding =
    ModifiersBuilders.Padding.Builder()
        .setStart(dp(this.start.value))
        .setTop(dp(this.top.value))
        .setEnd(dp(this.end.value))
        .setBottom(dp(this.bottom.value))
        .setRtlAware(this.rtlAware)
        .build()

private fun translateEmittableBox(element: EmittableBox) = LayoutElementBuilders.Box.Builder()
    .setVerticalAlignment(element.contentAlignment.vertical.toProto())
    .setHorizontalAlignment(element.contentAlignment.horizontal.toProto())
    .setModifiers(translateModifiers(element.modifier))
    .also { box -> element.children.forEach { box.addContent(translateComposition(it)) } }
    .build()

private fun translateModifiers(modifier: Modifier): ModifiersBuilders.Modifiers = modifier
    .foldOut(ModifiersBuilders.Modifiers.Builder()) { element, builder ->
        when (element) {
            is PaddingModifier -> builder.setPadding(element.toProto())
            else -> throw IllegalArgumentException("Unknown modifier type")
        }
    }.build()

/**
 * Translates a Glance Composition to a Wear Tile.
 *
 * @throws IllegalArgumentException If the provided Emittable is not recognised (e.g. it is an
 *   element which this translator doesn't understand).
 */
@GlanceInternalApi
public fun translateComposition(element: Emittable): LayoutElementBuilders.LayoutElement {
    return when (element) {
        is EmittableBox -> translateEmittableBox(element)
        else -> throw IllegalArgumentException("Unknown element $element")
    }
}