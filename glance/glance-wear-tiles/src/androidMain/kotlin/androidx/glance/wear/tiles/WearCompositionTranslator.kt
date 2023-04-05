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

package androidx.glance.wear.tiles

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.BackgroundModifier
import androidx.glance.BitmapImageProvider
import androidx.glance.Emittable
import androidx.glance.EmittableButton
import androidx.glance.EmittableImage
import androidx.glance.GlanceModifier
import androidx.glance.TintColorFilterParams
import androidx.glance.VisibilityModifier
import androidx.glance.action.Action
import androidx.glance.action.ActionModifier
import androidx.glance.action.LambdaAction
import androidx.glance.action.StartActivityAction
import androidx.glance.action.StartActivityClassAction
import androidx.glance.action.StartActivityComponentAction
import androidx.glance.findModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.ContentScale
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.EmittableColumn
import androidx.glance.layout.EmittableRow
import androidx.glance.layout.EmittableSpacer
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.PaddingInDp
import androidx.glance.layout.PaddingModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.layout.collectPaddingInDp
import androidx.glance.semantics.SemanticsModifier
import androidx.glance.semantics.SemanticsProperties
import androidx.glance.text.EmittableText
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.toEmittableText
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.Dimension
import androidx.glance.wear.tiles.action.RunCallbackAction
import androidx.glance.wear.tiles.curved.ActionCurvedModifier
import androidx.glance.wear.tiles.curved.AnchorType
import androidx.glance.wear.tiles.curved.CurvedTextStyle
import androidx.glance.wear.tiles.curved.EmittableCurvedChild
import androidx.glance.wear.tiles.curved.EmittableCurvedLine
import androidx.glance.wear.tiles.curved.EmittableCurvedRow
import androidx.glance.wear.tiles.curved.EmittableCurvedSpacer
import androidx.glance.wear.tiles.curved.EmittableCurvedText
import androidx.glance.wear.tiles.curved.GlanceCurvedModifier
import androidx.glance.wear.tiles.curved.RadialAlignment
import androidx.glance.wear.tiles.curved.SemanticsCurvedModifier
import androidx.glance.wear.tiles.curved.SweepAngleModifier
import androidx.glance.wear.tiles.curved.ThicknessModifier
import androidx.glance.wear.tiles.curved.findModifier
import androidx.wear.tiles.ActionBuilders
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
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_LEFT
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_RIGHT
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_START
import androidx.wear.tiles.LayoutElementBuilders.HorizontalAlignment
import androidx.wear.tiles.LayoutElementBuilders.TEXT_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.TEXT_ALIGN_END
import androidx.wear.tiles.LayoutElementBuilders.TEXT_ALIGN_START
import androidx.wear.tiles.LayoutElementBuilders.TextAlignment
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.VERTICAL_ALIGN_TOP
import androidx.wear.tiles.LayoutElementBuilders.VerticalAlignment
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.ResourceBuilders
import java.io.ByteArrayOutputStream
import java.util.Arrays

internal const val GlanceWearTileTag = "GlanceWearTile"

@VerticalAlignment
private fun Alignment.Vertical.toProto(): Int =
    when (this) {
        Alignment.Vertical.Top -> VERTICAL_ALIGN_TOP
        Alignment.Vertical.CenterVertically -> VERTICAL_ALIGN_CENTER
        Alignment.Vertical.Bottom -> VERTICAL_ALIGN_BOTTOM
        else -> {
            Log.w(
                GlanceWearTileTag,
                "Unknown vertical alignment type $this, align to Top instead"
            )
            VERTICAL_ALIGN_TOP
        }
    }

@HorizontalAlignment
private fun Alignment.Horizontal.toProto(): Int =
    when (this) {
        Alignment.Horizontal.Start -> HORIZONTAL_ALIGN_START
        Alignment.Horizontal.CenterHorizontally -> HORIZONTAL_ALIGN_CENTER
        Alignment.Horizontal.End -> HORIZONTAL_ALIGN_END
        else -> {
            Log.w(
                GlanceWearTileTag,
                "Unknown horizontal alignment type $this, align to Start instead"
            )
            HORIZONTAL_ALIGN_START
        }
    }

private fun PaddingInDp.toProto(): ModifiersBuilders.Padding =
    ModifiersBuilders.Padding.Builder()
        .setStart(dp(start.value))
        .setTop(dp(top.value))
        .setEnd(dp(end.value))
        .setBottom((dp(bottom.value)))
        .setRtlAware(true)
        .build()

private fun BackgroundModifier.toProto(context: Context): ModifiersBuilders.Background? =
    this.colorProvider?.let { provider ->
        ModifiersBuilders.Background.Builder()
            .setColor(argb(provider.getColorAsArgb(context)))
            .build()
    }

private fun BorderModifier.toProto(context: Context): ModifiersBuilders.Border =
    ModifiersBuilders.Border.Builder()
        .setWidth(dp(this.width.toDp(context.resources).value))
        .setColor(argb(this.color.getColorAsArgb(context)))
        .build()

private fun SemanticsModifier.toProto(): ModifiersBuilders.Semantics? =
    this.configuration.getOrNull(SemanticsProperties.ContentDescription)?.let {
        ModifiersBuilders.Semantics.Builder()
            .setContentDescription(it.joinToString())
            .build()
    }

private fun SemanticsCurvedModifier.toProto(): ModifiersBuilders.Semantics? =
    this.configuration.getOrNull(SemanticsProperties.ContentDescription)?.let {
        ModifiersBuilders.Semantics.Builder()
            .setContentDescription(it.joinToString())
            .build()
    }

private fun ColorProvider.getColorAsArgb(context: Context) = getColor(context).toArgb()

// TODO: handle parameters
private fun StartActivityAction.toProto(context: Context): ActionBuilders.LaunchAction =
    ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName(
                    when (this) {
                        is StartActivityComponentAction -> componentName.packageName
                        is StartActivityClassAction -> context.packageName
                        else -> error("Action type not defined in wear package: $this")
                    }
                )
                .setClassName(
                    when (this) {
                        is StartActivityComponentAction -> componentName.className
                        is StartActivityClassAction -> activityClass.name
                        else -> error("Action type not defined in wear package: $this")
                    }
                )
                .build()
        )
        .build()

private fun Action.toClickable(context: Context): ModifiersBuilders.Clickable {
    val builder = ModifiersBuilders.Clickable.Builder()

    when (this) {
        is StartActivityAction -> {
            builder.setOnClick(toProto(context))
        }
        is RunCallbackAction -> {
            builder.setOnClick(ActionBuilders.LoadAction.Builder().build())
                .setId(callbackClass.canonicalName!!)
        }
        is LambdaAction -> {
            Log.e(
                GlanceWearTileTag,
                "Lambda actions are not currently supported on Wear Tiles. Use " +
                    "actionRunCallback actions instead."
            )
        }
        else -> {
            Log.e(GlanceWearTileTag, "Unknown Action $this, skipped")
        }
    }

    return builder.build()
}

private fun ActionModifier.toProto(context: Context): ModifiersBuilders.Clickable =
    this.action.toClickable(context)

private fun ActionCurvedModifier.toProto(context: Context): ModifiersBuilders.Clickable =
    this.action.toClickable(context)

private fun Dimension.toContainerDimension(): DimensionBuilders.ContainerDimension =
    when (this) {
        is Dimension.Wrap -> wrap()
        is Dimension.Expand -> expand()
        is Dimension.Fill -> expand()
        is Dimension.Dp -> dp(this.dp.value)
        else -> throw IllegalArgumentException("The dimension should be fully resolved, not $this.")
    }

@ArcAnchorType
private fun AnchorType.toProto(): Int =
    when (this) {
        AnchorType.Start -> ARC_ANCHOR_START
        AnchorType.Center -> ARC_ANCHOR_CENTER
        AnchorType.End -> ARC_ANCHOR_END
        else -> {
            Log.w(GlanceWearTileTag, "Unknown arc anchor type $this, anchor to center instead")
            ARC_ANCHOR_CENTER
        }
    }

@VerticalAlignment
private fun RadialAlignment.toProto(): Int =
    when (this) {
        RadialAlignment.Outer -> VERTICAL_ALIGN_TOP
        RadialAlignment.Center -> VERTICAL_ALIGN_CENTER
        RadialAlignment.Inner -> VERTICAL_ALIGN_BOTTOM
        else -> {
            Log.w(
                GlanceWearTileTag,
                "Unknown radial alignment $this, align to center instead"
            )
            VERTICAL_ALIGN_CENTER
        }
    }

@TextAlignment
private fun TextAlign.toTextAlignment(isRtl: Boolean): Int =
    when (this) {
        TextAlign.Center -> TEXT_ALIGN_CENTER
        TextAlign.End -> TEXT_ALIGN_END
        TextAlign.Left -> if (isRtl) TEXT_ALIGN_END else TEXT_ALIGN_START
        TextAlign.Right -> if (isRtl) TEXT_ALIGN_START else TEXT_ALIGN_END
        TextAlign.Start -> TEXT_ALIGN_START
        else -> {
            Log.w(GlanceWearTileTag, "Unknown text alignment $this, align to Start instead")
            TEXT_ALIGN_START
        }
    }

@HorizontalAlignment
private fun TextAlign.toHorizontalAlignment(): Int =
    when (this) {
        TextAlign.Center -> HORIZONTAL_ALIGN_CENTER
        TextAlign.End -> HORIZONTAL_ALIGN_END
        TextAlign.Left -> HORIZONTAL_ALIGN_LEFT
        TextAlign.Right -> HORIZONTAL_ALIGN_RIGHT
        TextAlign.Start -> HORIZONTAL_ALIGN_START
        else -> {
            Log.w(GlanceWearTileTag, "Unknown text alignment $this, align to Start instead")
            HORIZONTAL_ALIGN_START
        }
    }

private fun Dimension.resolve(context: Context): Dimension {
    if (this !is Dimension.Resource) return this
    val sizePx = context.resources.getDimension(res)
    return when (sizePx.toInt()) {
        ViewGroup.LayoutParams.MATCH_PARENT -> Dimension.Fill
        ViewGroup.LayoutParams.WRAP_CONTENT -> Dimension.Wrap
        else -> Dimension.Dp((sizePx / context.resources.displayMetrics.density).dp)
    }
}

private fun GlanceModifier.getWidth(
    context: Context,
    default: Dimension = Dimension.Wrap
): Dimension = findModifier<WidthModifier>()?.width?.resolve(context) ?: default

private fun GlanceModifier.getHeight(
    context: Context,
    default: Dimension = Dimension.Wrap
): Dimension = findModifier<HeightModifier>()?.height?.resolve(context) ?: default

private fun translateEmittableBox(
    context: Context,
    resourceBuilder: ResourceBuilders.Resources.Builder,
    element: EmittableBox
) = LayoutElementBuilders.Box.Builder()
    .setVerticalAlignment(element.contentAlignment.vertical.toProto())
    .setHorizontalAlignment(element.contentAlignment.horizontal.toProto())
    .setModifiers(translateModifiers(context, element.modifier))
    .setWidth(element.modifier.getWidth(context).toContainerDimension())
    .setHeight(element.modifier.getHeight(context).toContainerDimension())
    .also { box ->
        element.children.forEach {
            box.addContent(translateComposition(context, resourceBuilder, it))
        }
    }
    .build()

private fun translateEmittableRow(
    context: Context,
    resourceBuilder: ResourceBuilders.Resources.Builder,
    element: EmittableRow
): LayoutElementBuilders.LayoutElement {
    val width = element.modifier.getWidth(context)
    val height = element.modifier.getHeight(context)

    val baseRowBuilder = LayoutElementBuilders.Row.Builder()
        .setHeight(height.toContainerDimension())
        .setVerticalAlignment(element.verticalAlignment.toProto())
        .also { row ->
            element.children.forEach {
                row.addContent(translateComposition(context, resourceBuilder, it))
            }
        }

    // Do we need to wrap it in a column to set the horizontal alignment?
    return if (element.horizontalAlignment != Alignment.Horizontal.Start &&
        width !is Dimension.Wrap
    ) {
        LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(element.horizontalAlignment.toProto())
            .setModifiers(translateModifiers(context, element.modifier))
            .setWidth(width.toContainerDimension())
            .setHeight(height.toContainerDimension())
            .addContent(baseRowBuilder.setWidth(wrap()).build())
            .build()
    } else {
        baseRowBuilder
            .setModifiers(translateModifiers(context, element.modifier))
            .setWidth(width.toContainerDimension())
            .build()
    }
}

private fun translateEmittableColumn(
    context: Context,
    resourceBuilder: ResourceBuilders.Resources.Builder,
    element: EmittableColumn
): LayoutElementBuilders.LayoutElement {
    val width = element.modifier.getWidth(context)
    val height = element.modifier.getHeight(context)

    val baseColumnBuilder = LayoutElementBuilders.Column.Builder()
        .setWidth(width.toContainerDimension())
        .setHorizontalAlignment(element.horizontalAlignment.toProto())
        .also { column ->
            element.children.forEach {
                column.addContent(translateComposition(context, resourceBuilder, it))
            }
        }

    // Do we need to wrap it in a row to set the vertical alignment?
    return if (element.verticalAlignment != Alignment.Vertical.Top &&
        height !is Dimension.Wrap
    ) {
        LayoutElementBuilders.Row.Builder()
            .setVerticalAlignment(element.verticalAlignment.toProto())
            .setModifiers(translateModifiers(context, element.modifier))
            .setWidth(width.toContainerDimension())
            .setHeight(height.toContainerDimension())
            .addContent(baseColumnBuilder.setHeight(wrap()).build())
            .build()
    } else {
        baseColumnBuilder
            .setModifiers(translateModifiers(context, element.modifier))
            .setHeight(height.toContainerDimension())
            .build()
    }
}

private fun translateTextStyle(
    context: Context,
    style: TextStyle,
): LayoutElementBuilders.FontStyle {
    val fontStyleBuilder = LayoutElementBuilders.FontStyle.Builder()

    style.color.let { fontStyleBuilder.setColor(argb(it.getColorAsArgb(context))) }
    // TODO(b/203656358): Can we support Em here too?
    style.fontSize?.let {
        if (!it.isSp) {
            throw IllegalArgumentException("Only Sp is supported for font size")
        }
        fontStyleBuilder.setSize(sp(it.value))
    }
    style.fontStyle?.let { fontStyleBuilder.setItalic(it == FontStyle.Italic) }
    style.fontWeight?.let {
        fontStyleBuilder.setWeight(
            when (it) {
                FontWeight.Normal -> FONT_WEIGHT_NORMAL
                FontWeight.Medium -> FONT_WEIGHT_MEDIUM
                FontWeight.Bold -> FONT_WEIGHT_BOLD
                else -> {
                    Log.w(
                        GlanceWearTileTag,
                        "Unknown font weight $it, use Normal weight instead"
                    )
                    FONT_WEIGHT_NORMAL
                }
            }
        )
    }
    style.textDecoration?.let {
        fontStyleBuilder.setUnderline(TextDecoration.Underline in it)
    }

    return fontStyleBuilder.build()
}

private fun translateTextStyle(
    context: Context,
    style: CurvedTextStyle,
): LayoutElementBuilders.FontStyle {
    val fontStyleBuilder = LayoutElementBuilders.FontStyle.Builder()

    style.color?.let { fontStyleBuilder.setColor(argb(it.getColorAsArgb(context))) }
    style.fontSize?.let { fontStyleBuilder.setSize(sp(it.value)) }
    style.fontStyle?.let { fontStyleBuilder.setItalic(it == FontStyle.Italic) }
    style.fontWeight?.let {
        fontStyleBuilder.setWeight(
            when (it) {
                FontWeight.Normal -> FONT_WEIGHT_NORMAL
                FontWeight.Medium -> FONT_WEIGHT_MEDIUM
                FontWeight.Bold -> FONT_WEIGHT_BOLD
                else -> {
                    Log.w(
                        GlanceWearTileTag,
                        "Unknown font weight $it, use Normal weight instead"
                    )
                    FONT_WEIGHT_NORMAL
                }
            }
        )
    }

    return fontStyleBuilder.build()
}

private fun translateEmittableText(
    context: Context,
    element: EmittableText
): LayoutElementBuilders.LayoutElement {
    // Does it have a width or height set? If so, we need to wrap it in a Box.
    val width = element.modifier.getWidth(context)
    val height = element.modifier.getHeight(context)

    val textBuilder = LayoutElementBuilders.Text.Builder()
        .setText(element.text)
        .setMaxLines(element.maxLines)

    element.style?.let { textBuilder.setFontStyle(translateTextStyle(context, it)) }

    val textAlign: TextAlign? = element.style?.textAlign
    if (textAlign != null) {
        val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        textBuilder.setMultilineAlignment(textAlign.toTextAlignment(isRtl))
    }

    return if (width !is Dimension.Wrap || height !is Dimension.Wrap) {
        val boxBuilder = LayoutElementBuilders.Box.Builder()
        if (textAlign != null) {
            boxBuilder.setHorizontalAlignment(textAlign.toHorizontalAlignment())
        }
        boxBuilder.setWidth(width.toContainerDimension())
            .setHeight(height.toContainerDimension())
            .setModifiers(translateModifiers(context, element.modifier))
            .addContent(textBuilder.build())
            .build()
    } else {
        textBuilder.setModifiers(translateModifiers(context, element.modifier)).build()
    }
}

private fun Dimension.toImageDimension(): DimensionBuilders.ImageDimension =
    when (this) {
        is Dimension.Expand -> expand()
        is Dimension.Fill -> expand()
        is Dimension.Dp -> dp(this.dp.value)
        else -> throw IllegalArgumentException("The dimension should be fully resolved, not $this.")
    }

private fun translateEmittableImage(
    context: Context,
    resourceBuilder: ResourceBuilders.Resources.Builder,
    element: EmittableImage
): LayoutElementBuilders.LayoutElement {
    var mappedResId: String
    when (element.provider) {
        is AndroidResourceImageProvider -> {
            val resId = (element.provider as AndroidResourceImageProvider).resId
            mappedResId = "android_$resId"
            resourceBuilder.addIdToImageMapping(
                mappedResId,
                ResourceBuilders.ImageResource.Builder().setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(resId)
                        .build()
                ).build()
            )
        }
        is BitmapImageProvider -> {
            val bitmap = (element.provider as BitmapImageProvider).bitmap
            val buffer = ByteArrayOutputStream().apply {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
            }.toByteArray()
            mappedResId = "android_${Arrays.hashCode(buffer)}"
            resourceBuilder.addIdToImageMapping(
                mappedResId,
                ResourceBuilders.ImageResource.Builder().setInlineResource(
                    ResourceBuilders.InlineImageResource.Builder()
                        .setWidthPx(bitmap.width)
                        .setHeightPx(bitmap.height)
                        .setData(buffer)
                        .build()
                ).build()
            )
        }
        else ->
            throw IllegalArgumentException("An unsupported ImageProvider type was used")
    }

    val imageBuilder = LayoutElementBuilders.Image.Builder()
        .setWidth(element.modifier.getWidth(context).toImageDimension())
        .setHeight(element.modifier.getHeight(context).toImageDimension())
        .setModifiers(translateModifiers(context, element.modifier))
        .setResourceId(mappedResId)
        .setContentScaleMode(
            when (element.contentScale) {
                ContentScale.Crop -> LayoutElementBuilders.CONTENT_SCALE_MODE_CROP
                ContentScale.Fit -> LayoutElementBuilders.CONTENT_SCALE_MODE_FIT
                ContentScale.FillBounds -> LayoutElementBuilders.CONTENT_SCALE_MODE_FILL_BOUNDS
                // Defaults to CONTENT_SCALE_MODE_FIT
                else -> LayoutElementBuilders.CONTENT_SCALE_MODE_FIT
            }
        )

    element.colorFilterParams?.let { colorFilterParams ->
        when (colorFilterParams) {
            is TintColorFilterParams -> {
                imageBuilder.setColorFilter(
                    LayoutElementBuilders.ColorFilter.Builder()
                        .setTint(argb(colorFilterParams.colorProvider.getColorAsArgb(context)))
                        .build()
                )
            }

            else -> throw IllegalArgumentException("An unsupported ColorFilter was used.")
        }
    }
    return imageBuilder.build()
}

private fun translateEmittableCurvedRow(
    context: Context,
    resourceBuilder: ResourceBuilders.Resources.Builder,
    element: EmittableCurvedRow
): LayoutElementBuilders.LayoutElement {
    // Does it have a width or height set? If so, we need to wrap it in a Box.
    val width = element.modifier.getWidth(context)
    val height = element.modifier.getHeight(context)

    // Note: Wear Tiles uses 0 degrees = 12 o clock, but Glance / Wear Compose use 0 degrees = 3
    // o clock. Tiles supports wraparound etc though, so just add on the 90 degrees here.
    val arcBuilder = LayoutElementBuilders.Arc.Builder()
        .setAnchorAngle(degrees(element.anchorDegrees + 90f))
        .setAnchorType(element.anchorType.toProto())
        .setVerticalAlign(element.radialAlignment.toProto())

    // Add all the children first...
    element.children.forEach { curvedChild ->
        if (curvedChild is EmittableCurvedChild) {
            curvedChild.children.forEach {
                arcBuilder.addContent(
                    translateEmittableElementInArc(
                        context,
                        resourceBuilder,
                        it,
                        curvedChild.rotateContent)
                )
            }
        } else {
            arcBuilder.addContent(translateCurvedCompositionInArc(context, curvedChild))
        }
    }

    return if (width is Dimension.Dp || height is Dimension.Dp) {
        LayoutElementBuilders.Box.Builder()
            .setWidth(width.toContainerDimension())
            .setHeight(height.toContainerDimension())
            .setModifiers(translateModifiers(context, element.modifier))
            .addContent(arcBuilder.build())
            .build()
    } else {
        arcBuilder
            .setModifiers(translateModifiers(context, element.modifier))
            .build()
    }
}

private fun translateEmittableCurvedText(
    context: Context,
    element: EmittableCurvedText
): LayoutElementBuilders.ArcLayoutElement {
    // Modifiers are currently ignored for this element; we'll have to add CurvedScope modifiers in
    // future which can be used with ArcModifiers, but we don't have any of those added right now.
    val arcTextBuilder = LayoutElementBuilders.ArcText.Builder()
        .setText(element.text)

    element.style?.let {
        arcTextBuilder.setFontStyle(translateTextStyle(context, it))
    }

    arcTextBuilder.setModifiers(translateCurvedModifiers(context, element.curvedModifier))

    return arcTextBuilder.build()
}

private fun translateEmittableCurvedLine(
    context: Context,
    element: EmittableCurvedLine
): LayoutElementBuilders.ArcLayoutElement {
    var sweepAngleDegrees =
        element.curvedModifier.findModifier<SweepAngleModifier>() ?. degrees ?: 0f
    var thickness = element.curvedModifier.findModifier<ThicknessModifier>() ?. thickness ?: 0.dp

    return LayoutElementBuilders.ArcLine.Builder()
        .setLength(degrees(sweepAngleDegrees))
        .setThickness(dp(thickness.value))
        .setColor(argb(element.color.getColorAsArgb(context)))
        .setModifiers(translateCurvedModifiers(context, element.curvedModifier))
        .build()
}

private fun translateEmittableCurvedSpacer(
    context: Context,
    element: EmittableCurvedSpacer
): LayoutElementBuilders.ArcLayoutElement {
    var sweepAngleDegrees =
        element.curvedModifier.findModifier<SweepAngleModifier>() ?. degrees ?: 0f
    var thickness = element.curvedModifier.findModifier<ThicknessModifier>() ?. thickness ?: 0.dp

    return LayoutElementBuilders.ArcSpacer.Builder()
        .setLength(degrees(sweepAngleDegrees))
        .setThickness(dp(thickness.value))
        .setModifiers(translateCurvedModifiers(context, element.curvedModifier))
        .build()
}

private fun translateEmittableElementInArc(
    context: Context,
    resourceBuilder: ResourceBuilders.Resources.Builder,
    element: Emittable,
    rotateContent: Boolean
): LayoutElementBuilders.ArcLayoutElement = LayoutElementBuilders.ArcAdapter.Builder()
    .setContent(translateComposition(context, resourceBuilder, element))
    .setRotateContents(rotateContent)
    .build()

private fun translateCurvedModifiers(
    context: Context,
    curvedModifier: GlanceCurvedModifier
): ModifiersBuilders.ArcModifiers =
    curvedModifier.foldIn(ModifiersBuilders.ArcModifiers.Builder()) { builder, element ->
       when (element) {
           is ActionCurvedModifier -> builder.setClickable(element.toProto(context))
           is ThicknessModifier -> builder /* Skip for now, handled elsewhere. */
           is SweepAngleModifier -> builder /* Skip for now, handled elsewhere. */
           is SemanticsCurvedModifier -> {
               element.toProto()?.let { builder.setSemantics(it) } ?: builder
           }
           else -> throw IllegalArgumentException("Unknown curved modifier type")
       }
    }.build()

private fun translateModifiers(
    context: Context,
    modifier: GlanceModifier,
): ModifiersBuilders.Modifiers =
    modifier.foldIn(ModifiersBuilders.Modifiers.Builder()) { builder, element ->
        when (element) {
            is BackgroundModifier -> {
                element.toProto(context)?.let { builder.setBackground(it) } ?: builder
            }
            is WidthModifier -> builder /* Skip for now, handled elsewhere. */
            is HeightModifier -> builder /* Skip for now, handled elsewhere. */
            is ActionModifier -> builder.setClickable(element.toProto(context))
            is PaddingModifier -> builder // Processing that after
            is VisibilityModifier -> builder // Already processed
            is BorderModifier -> builder.setBorder(element.toProto(context))
            is SemanticsModifier -> {
                element.toProto()?.let { builder.setSemantics(it) } ?: builder
            }
            else -> throw IllegalArgumentException("Unknown modifier type")
        }
    }
        .also { builder ->
            val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
            modifier.collectPaddingInDp(context.resources)
                ?.toRelative(isRtl)
                ?.let {
                    builder.setPadding(it.toProto())
                }
        }
        .build()

private fun translateCurvedCompositionInArc(
    context: Context,
    element: Emittable
): LayoutElementBuilders.ArcLayoutElement {
    return when (element) {
        is EmittableCurvedText -> translateEmittableCurvedText(context, element)
        is EmittableCurvedLine -> translateEmittableCurvedLine(context, element)
        is EmittableCurvedSpacer -> translateEmittableCurvedSpacer(context, element)
        else -> throw IllegalArgumentException(
            "Unknown curved Element: $element"
        )
    }
}

private fun Dimension.toSpacerDimension(): DimensionBuilders.SpacerDimension =
    when (this) {
        is Dimension.Dp -> dp(this.dp.value)
        else -> throw IllegalArgumentException(
            "The spacer dimension should be with dp value, not $this."
        )
    }

private fun translateEmittableSpacer(
    context: Context,
    element: EmittableSpacer
) = LayoutElementBuilders.Spacer.Builder()
    .setWidth(element.modifier.getWidth(context, Dimension.Dp(0.dp)).toSpacerDimension())
    .setHeight(element.modifier.getHeight(context, Dimension.Dp(0.dp)).toSpacerDimension())
    .build()

private fun translateEmittableAndroidLayoutElement(element: EmittableAndroidLayoutElement) =
    element.layoutElement

/**
 * Translates a Glance Composition to a Wear Tile.
 *
 * @throws IllegalArgumentException If the provided Emittable is not recognised (e.g. it is an
 *   element which this translator doesn't understand).
 */
internal fun translateComposition(
    context: Context,
    resourceBuilder: ResourceBuilders.Resources.Builder,
    element: Emittable
): LayoutElementBuilders.LayoutElement {
    return when (element) {
        is EmittableBox -> translateEmittableBox(context, resourceBuilder, element)
        is EmittableRow -> translateEmittableRow(context, resourceBuilder, element)
        is EmittableColumn ->
            translateEmittableColumn(context, resourceBuilder, element)
        is EmittableText -> translateEmittableText(context, element)
        is EmittableCurvedRow ->
            translateEmittableCurvedRow(context, resourceBuilder, element)
        is EmittableAndroidLayoutElement -> translateEmittableAndroidLayoutElement(element)
        is EmittableButton ->
            translateEmittableText(context, element.toEmittableText())
        is EmittableSpacer -> translateEmittableSpacer(context, element)
        is EmittableImage ->
            translateEmittableImage(context, resourceBuilder, element)
        else -> throw IllegalArgumentException("Unknown element $element")
    }
}

internal class CompositionResult(
    val layout: LayoutElementBuilders.LayoutElement,
    val resources: ResourceBuilders.Resources.Builder
)

internal fun translateTopLevelComposition(
    context: Context,
    element: Emittable
): CompositionResult {
    val resourceBuilder = ResourceBuilders.Resources.Builder()
    val layout = translateComposition(context, resourceBuilder, element)
    return CompositionResult(layout, resourceBuilder)
}
