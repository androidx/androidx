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

package androidx.glance

import androidx.annotation.ColorRes
import androidx.annotation.RestrictTo
import androidx.compose.ui.graphics.Color
import androidx.glance.layout.ContentScale
import androidx.glance.unit.ColorProvider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface BackgroundModifier : GlanceModifier.Element {

    class Color(val colorProvider: ColorProvider) : BackgroundModifier {
        override fun toString() = "BackgroundModifier(colorProvider=$colorProvider)"
    }

    class Image(
        val imageProvider: ImageProvider?,
        val contentScale: ContentScale = ContentScale.FillBounds,
        val colorFilter: ColorFilter? = null
    ) : BackgroundModifier {
        override fun toString() =
            "BackgroundModifier(colorFilter=$colorFilter, imageProvider=$imageProvider, " +
                "contentScale=$contentScale)"
    }
}

/**
 * Apply a background color to the element this modifier is attached to. This will cause the element
 * to paint the specified [Color] as its background, which will fill the bounds of the element.
 *
 * @param color The color to set as the background.
 */
fun GlanceModifier.background(color: Color): GlanceModifier = background(ColorProvider(color))

/**
 * Apply a background color to the element this modifier is attached to. This will cause the element
 * to paint the specified color resource as its background, which will fill the bounds of the
 * element.
 *
 * @param color The color resource to set as the background.
 */
fun GlanceModifier.background(@ColorRes color: Int): GlanceModifier =
    background(ColorProvider(color))

/**
 * Apply a background color to the element this modifier is attached to. This will cause the element
 * to paint the specified [ColorProvider] as its background, which will fill the bounds of the
 * element.
 *
 * @param colorProvider The color to set as the background
 */
fun GlanceModifier.background(colorProvider: ColorProvider): GlanceModifier =
    this.then(BackgroundModifier.Color(colorProvider))

/**
 * Apply a background image to the element this modifier is attached to.
 *
 * @param imageProvider The content to set as the background
 * @param contentScale scaling to apply to the imageProvider.
 */
@Deprecated(
    "This method has been deprecated in favor of the one that accepts a colorFilter.",
    level = DeprecationLevel.HIDDEN
)
fun GlanceModifier.background(
    imageProvider: ImageProvider,
    contentScale: ContentScale = ContentScale.FillBounds
): GlanceModifier = this.then(BackgroundModifier.Image(imageProvider, contentScale))

/**
 * Apply a background image to the element this modifier is attached to.
 *
 * @param imageProvider The content to set as the background
 * @param colorFilter Optional color filter to apply to [imageProvider], such as tint.
 * @param contentScale scaling to apply to the imageProvider.
 */
fun GlanceModifier.background(
    imageProvider: ImageProvider,
    contentScale: ContentScale = ContentScale.FillBounds,
    colorFilter: ColorFilter? = null,
): GlanceModifier =
    this.then(
        BackgroundModifier.Image(
            imageProvider = imageProvider,
            contentScale = contentScale,
            colorFilter = colorFilter
        )
    )
