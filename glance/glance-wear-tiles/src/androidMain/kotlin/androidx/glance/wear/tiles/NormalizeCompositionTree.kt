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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.BitmapImageProvider
import androidx.glance.Emittable
import androidx.glance.EmittableImage
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceModifier
import androidx.glance.IconImageProvider
import androidx.glance.ImageProvider
import androidx.glance.Visibility
import androidx.glance.VisibilityModifier
import androidx.glance.findModifier
import androidx.glance.layout.EmittableSpacer
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.PaddingModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.layout.size
import androidx.glance.text.EmittableText
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.Dimension
import androidx.glance.visibility
import androidx.glance.wear.tiles.curved.CurvedTextStyle
import androidx.glance.wear.tiles.curved.EmittableCurvedText

internal fun normalizeCompositionTree(context: Context, root: EmittableWithChildren) {
    root.transformTree { view ->
        view.normalizeVisibility(context)
    }
}

/** Transform each node in the tree. */
private fun EmittableWithChildren.transformTree(block: (Emittable) -> Emittable?) {
    val toDelete = mutableListOf<Int>()
    children.mapIndexed { index, child ->
        val newChild = block(child)
        if (newChild == null) {
            toDelete += index
        } else {
            children[index] = newChild
        }
        if (newChild is EmittableWithChildren) newChild.transformTree(block)
    }
    toDelete.reverse()
    toDelete.forEach {
        children.removeAt(it)
    }
}

private fun Emittable.normalizeVisibility(context: Context): Emittable? =
    when (modifier.findModifier<VisibilityModifier>()?.visibility) {
        Visibility.Gone -> null
        Visibility.Invisible -> makeInvisible(context)
        else -> this
    }

private fun Emittable.makeInvisible(context: Context): Emittable {
    val width = modifier.findModifier<WidthModifier>()?.width ?: Dimension.Wrap
    val height = modifier.findModifier<HeightModifier>()?.height ?: Dimension.Wrap
    if (width != Dimension.Wrap && height != Dimension.Wrap) {
        return EmittableSpacer().apply {
            modifier = GlanceModifier.then(WidthModifier(width)).then(HeightModifier(height))
        }
    }
    return when (this) {
        is EmittableWithChildren -> {
            modifier = GlanceModifier.then(WidthModifier(width)).then(HeightModifier(height))
            children.forEach { child ->
                val visibility =
                    child.modifier.findModifier<VisibilityModifier>()?.visibility
                        ?: Visibility.Visible
                if (visibility == Visibility.Visible) {
                    child.modifier = child.modifier.visibility(Visibility.Invisible)
                }
            }
            this
        }
        is EmittableImage -> {
            val provider = provider
            require(provider != null) { "No image provider available on the EmittableImage" }
            val imageSize = provider.getImageSize(context)
            EmittableSpacer().apply {
                modifier = GlanceModifier.size(imageSize.width, imageSize.height)
            }
        }
        is EmittableText -> {
            val oldStyle = style
            style = oldStyle?.updateColor(Color.Transparent)
                ?: TextStyle(color = ColorProvider(Color.Transparent))
            this
        }
        is EmittableCurvedText -> {
            val oldStyle = style
            style = oldStyle?.updateColor(Color.Transparent)
                ?: CurvedTextStyle(color = ColorProvider(Color.Transparent))
            this
        }
        else -> this
    }.keepOnlySizeModifiers()
}

/** Keeps only size-related modifiers, that is: width, height and padding. */
private fun Emittable.keepOnlySizeModifiers(): Emittable {
    modifier = modifier.foldIn<GlanceModifier>(GlanceModifier) { acc, cur ->
        if (cur is WidthModifier || cur is HeightModifier || cur is PaddingModifier) {
            acc.then(cur)
        } else {
            acc
        }
    }
    return this
}

private fun TextStyle.updateColor(color: Color) = TextStyle(
    color = ColorProvider(color),
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
    textAlign = textAlign,
    textDecoration = textDecoration,
)

private fun CurvedTextStyle.updateColor(color: Color) = CurvedTextStyle(
    color = ColorProvider(color),
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
)

private fun ImageProvider.getImageSize(context: Context): DpSize {
    val density = context.resources.displayMetrics.density
    return when (this) {
        is AndroidResourceImageProvider -> {
            val image = context.resources.getDrawable(resId, null)
            DpSize(
                (image.intrinsicWidth.toFloat() / density).dp,
                (image.intrinsicHeight.toFloat() / density).dp
            )
        }
        is BitmapImageProvider -> {
            DpSize(
                (bitmap.width.toFloat() / bitmap.density).dp,
                (bitmap.height.toFloat() / bitmap.density).dp
            )
        }
        is IconImageProvider -> {
            val drawable = icon.loadDrawable(context)!!
            DpSize(
                (drawable.intrinsicWidth.toFloat() / density).dp,
                (drawable.intrinsicHeight.toFloat() / density).dp,
            )
        }
        else -> error("Unknown image provider type: ${this.javaClass.canonicalName}")
    }
}