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
import androidx.glance.findModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Dimension
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.EmittableColumn
import androidx.glance.layout.EmittableRow
import androidx.glance.layout.EmittableText
import androidx.glance.layout.FontStyle
import androidx.glance.layout.FontWeight
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.PaddingModifier
import androidx.glance.layout.TextDecoration
import androidx.glance.layout.TextStyle
import androidx.glance.layout.WidthModifier
import androidx.glance.wear.layout.AnchorType
import androidx.glance.wear.layout.BackgroundModifier
import androidx.glance.wear.layout.CurvedTextStyle
import androidx.glance.wear.layout.EmittableCurvedRow
import androidx.glance.wear.layout.EmittableCurvedText
import androidx.glance.wear.layout.RadialAlignment
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DimensionBuilders
import androidx.wear.tiles.DimensionBuilders.degrees
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.DimensionBuilders.expand
import androidx.wear.tiles.DimensionBuilders.sp
import androidx.wear.tiles.DimensionBuilders.wrap
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.LayoutElementBuilders.ARC_ANCHOR_CENTER
import androidx.wear.tiles.LayoutElementBuilders.ARC_ANCHOR_END
import androidx.wear.tiles.LayoutElementBuilders.ARC_ANCHOR_START
import androidx.wear.tiles.LayoutElementBuilders.ArcAnchorType
import androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_BOLD
import androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_MEDIUM
import androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_NORMAL
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_END
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_START
import androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_TOP
import androidx.wear.tiles.LayoutElementBuilders.VerticalAlignment
import androidx.wear.tiles.ModifiersBuilders

@VerticalAlignment
private fun Alignment.Vertical.toProto(): Int =
    when (this) {
        Alignment.Vertical.Top -> VERTICAL_ALIGN_TOP
        Alignment.Vertical.CenterVertically -> VERTICAL_ALIGN_CENTER
        Alignment.Vertical.Bottom -> VERTICAL_ALIGN_BOTTOM
        else -> throw IllegalArgumentException("Unknown vertical alignment type $this")
    }

@HorizontalAlignment
private fun Alignment.Horizontal.toProto(): Int =
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

private fun BackgroundModifier.toProto(): ModifiersBuilders.Background =
    ModifiersBuilders.Background.Builder()
        .setColor(argb(this.color.value.toInt()))
        .build()

private fun Dimension.toContainerDimension(): DimensionBuilders.ContainerDimension =
    when (this) {
        is Dimension.Wrap -> wrap()
        is Dimension.Expand -> expand()
        is Dimension.Dp -> dp(this.dp.value)
    }

@ArcAnchorType
private fun AnchorType.toProto(): Int =
    when (this) {
        AnchorType.Start -> ARC_ANCHOR_START
        AnchorType.Center -> ARC_ANCHOR_CENTER
        AnchorType.End -> ARC_ANCHOR_END
        else -> throw IllegalArgumentException("Unknown arc anchor type $this")
    }

@VerticalAlignment
private fun RadialAlignment.toProto(): Int =
    when (this) {
        RadialAlignment.Outer -> VERTICAL_ALIGN_TOP
        RadialAlignment.Center -> VERTICAL_ALIGN_CENTER
        RadialAlignment.Inner -> VERTICAL_ALIGN_BOTTOM
        else -> throw IllegalArgumentException("Unknown radial alignment $this")
    }

private fun Modifier.getWidth(
    default: Dimension = Dimension.Wrap
): Dimension = findModifier<WidthModifier>()?.width ?: default

private fun Modifier.getHeight(
    default: Dimension = Dimension.Wrap
): Dimension = findModifier<HeightModifier>()?.height ?: default

private fun translateEmittableBox(element: EmittableBox) = LayoutElementBuilders.Box.Builder()
    .setVerticalAlignment(element.contentAlignment.vertical.toProto())
    .setHorizontalAlignment(element.contentAlignment.horizontal.toProto())
    .setModifiers(translateModifiers(element.modifier))
    .setWidth(element.modifier.getWidth().toContainerDimension())
    .setHeight(element.modifier.getHeight().toContainerDimension())
    .also { box -> element.children.forEach { box.addContent(translateComposition(it)) } }
    .build()

private fun translateEmittableRow(element: EmittableRow): LayoutElementBuilders.LayoutElement {
    val width = element.modifier.getWidth()
    val height = element.modifier.getHeight()

    val baseRowBuilder = LayoutElementBuilders.Row.Builder()
        .setHeight(height.toContainerDimension())
        .setVerticalAlignment(element.verticalAlignment.toProto())
        .also { row -> element.children.forEach { row.addContent(translateComposition(it)) } }

    // Do we need to wrap it in a column to set the horizontal alignment?
    return if (element.horizontalAlignment != Alignment.Horizontal.Start &&
        width !is Dimension.Wrap
    ) {
        LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(element.horizontalAlignment.toProto())
            .setModifiers(translateModifiers(element.modifier))
            .setWidth(width.toContainerDimension())
            .setHeight(height.toContainerDimension())
            .addContent(baseRowBuilder.setWidth(wrap()).build())
            .build()
    } else {
        baseRowBuilder
            .setModifiers(translateModifiers(element.modifier))
            .setWidth(width.toContainerDimension())
            .build()
    }
}

private fun translateEmittableColumn(
    element: EmittableColumn
): LayoutElementBuilders.LayoutElement {
    val width = element.modifier.getWidth()
    val height = element.modifier.getHeight()

    val baseColumnBuilder = LayoutElementBuilders.Column.Builder()
        .setWidth(width.toContainerDimension())
        .setHorizontalAlignment(element.horizontalAlignment.toProto())
        .also { column -> element.children.forEach { column.addContent(translateComposition(it)) } }

    // Do we need to wrap it in a row to set the vertical alignment?
    return if (element.verticalAlignment != Alignment.Vertical.Top &&
        height !is Dimension.Wrap
    ) {
        LayoutElementBuilders.Row.Builder()
            .setVerticalAlignment(element.verticalAlignment.toProto())
            .setModifiers(translateModifiers(element.modifier))
            .setWidth(width.toContainerDimension())
            .setHeight(height.toContainerDimension())
            .addContent(baseColumnBuilder.setHeight(wrap()).build())
            .build()
    } else {
        baseColumnBuilder
            .setModifiers(translateModifiers(element.modifier))
            .setHeight(height.toContainerDimension())
            .build()
    }
}

private fun translateTextStyle(style: TextStyle): LayoutElementBuilders.FontStyle {
    val fontStyleBuilder = LayoutElementBuilders.FontStyle.Builder()

    style.fontSize?.let { fontStyleBuilder.setSize(sp(it.value)) }
    style.fontStyle?.let { fontStyleBuilder.setItalic(it == FontStyle.Italic) }
    style.fontWeight?.let {
        fontStyleBuilder.setWeight(
            when (it) {
                FontWeight.Normal -> FONT_WEIGHT_NORMAL
                FontWeight.Medium -> FONT_WEIGHT_MEDIUM
                FontWeight.Bold -> FONT_WEIGHT_BOLD
                else -> throw IllegalArgumentException("Unknown font weight $it")
            }
        )
    }
    style.textDecoration?.let {
        fontStyleBuilder.setUnderline(TextDecoration.Underline in it)
    }

    return fontStyleBuilder.build()
}

private fun translateTextStyle(style: CurvedTextStyle): LayoutElementBuilders.FontStyle {
    val fontStyleBuilder = LayoutElementBuilders.FontStyle.Builder()

    style.fontSize?.let { fontStyleBuilder.setSize(sp(it.value)) }
    style.fontStyle?.let { fontStyleBuilder.setItalic(it == FontStyle.Italic) }
    style.fontWeight?.let {
        fontStyleBuilder.setWeight(
            when (it) {
                FontWeight.Normal -> FONT_WEIGHT_NORMAL
                FontWeight.Medium -> FONT_WEIGHT_MEDIUM
                FontWeight.Bold -> FONT_WEIGHT_BOLD
                else -> throw IllegalArgumentException("Unknown font weight $it")
            }
        )
    }

    return fontStyleBuilder.build()
}

private fun translateEmittableText(element: EmittableText): LayoutElementBuilders.LayoutElement {
    // Does it have a width or height set? If so, we need to wrap it in a Box.
    val width = element.modifier.getWidth()
    val height = element.modifier.getHeight()

    val textBuilder = LayoutElementBuilders.Text.Builder()
        .setText(element.text)

    element.style?.let { textBuilder.setFontStyle(translateTextStyle(it)) }

    return if (width !is Dimension.Wrap || height !is Dimension.Wrap) {
        LayoutElementBuilders.Box.Builder()
            .setWidth(width.toContainerDimension())
            .setHeight(height.toContainerDimension())
            .setModifiers(translateModifiers(element.modifier))
            .addContent(textBuilder.build())
            .build()
    } else {
        textBuilder.setModifiers(translateModifiers(element.modifier)).build()
    }
}

private fun translateEmittableCurvedRow(
    element: EmittableCurvedRow
): LayoutElementBuilders.LayoutElement {
    // Does it have a width or height set? If so, we need to wrap it in a Box.
    val width = element.modifier.getWidth()
    val height = element.modifier.getHeight()

    // Note: Wear Tiles uses 0 degrees = 12 o clock, but Glance / Wear Compose use 0 degrees = 3
    // o clock. Tiles supports wraparound etc though, so just add on the 90 degrees here.
    val arcBuilder = LayoutElementBuilders.Arc.Builder()
        .setAnchorAngle(degrees(element.anchorDegrees + 90f))
        .setAnchorType(element.anchorType.toProto())
        .setVerticalAlign(element.radialAlignment.toProto())

    // Add all the children first...
    element.children.forEach { arcBuilder.addContent(translateCompositionInArc(it)) }

    return if (width is Dimension.Dp || height is Dimension.Dp) {
        LayoutElementBuilders.Box.Builder()
            .setWidth(width.toContainerDimension())
            .setHeight(height.toContainerDimension())
            .setModifiers(translateModifiers(element.modifier))
            .addContent(arcBuilder.build())
            .build()
    } else {
        arcBuilder
            .setModifiers(translateModifiers(element.modifier))
            .build()
    }
}

private fun translateEmittableCurvedText(
    element: EmittableCurvedText
): LayoutElementBuilders.ArcLayoutElement {
    // Modifiers are currently ignored for this element; we'll have to add CurvedScope modifiers in
    // future which can be used with ArcModifiers, but we don't have any of those added right now.
    val arcTextBuilder = LayoutElementBuilders.ArcText.Builder()
        .setText(element.text)

    element.textStyle?.let { arcTextBuilder.setFontStyle(translateTextStyle(it)) }

    return arcTextBuilder.build()
}

private fun translateEmittableElementInArc(
    element: Emittable
): LayoutElementBuilders.ArcLayoutElement = LayoutElementBuilders.ArcAdapter.Builder()
    .setContent(translateComposition(element))
    .build()

private fun translateModifiers(modifier: Modifier): ModifiersBuilders.Modifiers = modifier
    .foldOut(ModifiersBuilders.Modifiers.Builder()) { element, builder ->
        when (element) {
            is PaddingModifier -> builder.setPadding(element.toProto())
            is BackgroundModifier -> builder.setBackground(element.toProto())
            is WidthModifier -> builder /* Skip for now, handled elsewhere. */
            is HeightModifier -> builder /* Skip for now, handled elsewhere. */
            else -> throw IllegalArgumentException("Unknown modifier type")
        }
    }.build()

private fun translateCompositionInArc(element: Emittable): LayoutElementBuilders.ArcLayoutElement {
    return when (element) {
        is EmittableCurvedText -> translateEmittableCurvedText(element)
        else -> translateEmittableElementInArc(element)
    }
}

/**
 * Translates a Glance Composition to a Wear Tile.
 *
 * @throws IllegalArgumentException If the provided Emittable is not recognised (e.g. it is an
 *   element which this translator doesn't understand).
 */
internal fun translateComposition(element: Emittable): LayoutElementBuilders.LayoutElement {
    return when (element) {
        is EmittableBox -> translateEmittableBox(element)
        is EmittableRow -> translateEmittableRow(element)
        is EmittableColumn -> translateEmittableColumn(element)
        is EmittableText -> translateEmittableText(element)
        is EmittableCurvedRow -> translateEmittableCurvedRow(element)
        else -> throw IllegalArgumentException("Unknown element $element")
    }
}
