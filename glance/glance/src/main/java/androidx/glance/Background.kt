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

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BackgroundModifier private constructor(
    val colorProvider: ColorProvider?,
    val imageProvider: ImageProvider?,
    val contentScale: ContentScale = ContentScale.FillBounds,
) : GlanceModifier.Element {
    init {
        require((colorProvider != null) xor (imageProvider != null)) {
            "Exactly one of colorProvider and imageProvider must be non-null"
        }
    }

    constructor(colorProvider: ColorProvider) :
        this(colorProvider = colorProvider, imageProvider = null)

    constructor(imageProvider: ImageProvider, contentScale: ContentScale) :
        this(colorProvider = null, imageProvider = imageProvider, contentScale = contentScale)

    override fun toString() =
        "BackgroundModifier(colorProvider=$colorProvider, imageProvider=$imageProvider, " +
            "contentScale=$contentScale)"
}

/**
 * Apply a background color to the element this modifier is attached to. This will cause the
 * element to paint the specified [Color] as its background, which will fill the bounds of the
 * element.
 */
fun GlanceModifier.background(color: Color): GlanceModifier =
    background(ColorProvider(color))

/**
 * Apply a background color to the element this modifier is attached to. This will cause the
 * element to paint the specified color resource as its background, which will fill the bounds of
 * the element.
 */
fun GlanceModifier.background(@ColorRes color: Int): GlanceModifier =
    background(ColorProvider(color))

/**
 * Apply a background color to the element this modifier is attached to. This will cause the
 * element to paint the specified [ColorProvider] as its background, which will fill the bounds of
 * the element.
 */
fun GlanceModifier.background(colorProvider: ColorProvider): GlanceModifier =
    this.then(BackgroundModifier(colorProvider))

/**
 * Apply a background image to the element this modifier is attached to.
 */
fun GlanceModifier.background(
    imageProvider: ImageProvider,
    contentScale: ContentScale = ContentScale.FillBounds
): GlanceModifier =
    this.then(BackgroundModifier(imageProvider, contentScale))
