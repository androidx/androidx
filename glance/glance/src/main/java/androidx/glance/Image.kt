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

import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.glance.layout.ContentScale
import androidx.glance.semantics.SemanticsModifier
import androidx.glance.semantics.SemanticsProperties
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider

/**
 * Interface representing an Image source which can be used with a Glance [Image] element.
 */
interface ImageProvider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AndroidResourceImageProvider(@DrawableRes val resId: Int) : ImageProvider {
    override fun toString() = "AndroidResourceImageProvider(resId=$resId)"
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BitmapImageProvider(val bitmap: Bitmap) : ImageProvider {
    override fun toString() =
        "BitmapImageProvider(bitmap=Bitmap(${bitmap.width}px x ${bitmap.height}px))"
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IconImageProvider(val icon: Icon) : ImageProvider {
    override fun toString() = "IconImageProvider(icon=$icon)"
}

/**
 * Image resource from an Android Drawable resource.
 *
 * @param resId The resource ID of the Drawable resource to be used.
 */
fun ImageProvider(@DrawableRes resId: Int): ImageProvider =
    AndroidResourceImageProvider(resId)

/**
 * Image resource from a bitmap.
 *
 * @param bitmap The bitmap to be displayed.
 */
fun ImageProvider(bitmap: Bitmap): ImageProvider = BitmapImageProvider(bitmap)

/**
 * Image resource from an icon.
 *
 * @param icon The icon to be displayed.
 */
@RequiresApi(Build.VERSION_CODES.M)
fun ImageProvider(icon: Icon): ImageProvider = IconImageProvider(icon)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ColorFilterParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TintColorFilterParams(val colorProvider: ColorProvider) : ColorFilterParams {
    override fun toString() =
        "TintColorFilterParams(colorProvider=$colorProvider))"
}

/**
 * Effects used to modify the color of an image.
 */
class ColorFilter internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val colorFilterParams: ColorFilterParams
) {
    companion object {
        /**
         * Set a tinting option for the image using the platform-specific default blending mode.
         *
         * @param colorProvider Provider used to get the color for blending the source content.
         */
        fun tint(colorProvider: ColorProvider): ColorFilter =
            ColorFilter(TintColorFilterParams(colorProvider))
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmittableImage : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var provider: ImageProvider? = null
    var colorFilterParams: ColorFilterParams? = null
    var contentScale: ContentScale = ContentScale.Fit

    override fun copy(): Emittable = EmittableImage().also {
        it.modifier = modifier
        it.provider = provider
        it.colorFilterParams = colorFilterParams
        it.contentScale = contentScale
    }

    override fun toString(): String = "EmittableImage(" +
        "modifier=$modifier, " +
        "provider=$provider, " +
        "colorFilterParams=$colorFilterParams, " +
        "contentScale=$contentScale" +
        ")"
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun EmittableImage.isDecorative(): Boolean {
    val semanticsConfiguration = modifier.findModifier<SemanticsModifier>()?.configuration
    return semanticsConfiguration?.getOrNull(SemanticsProperties.ContentDescription)?.get(0)
        .isNullOrEmpty()
}

/**
 * A composable which lays out and draws the image specified in [provider]. This will attempt to lay
 * out the image using the intrinsic width and height of the provided image, but this can be
 * overridden by using a modifier to set the width or height of this element.
 *
 * @param provider The image provider to use to draw the image
 * @param contentDescription text used by accessibility services to describe what this image
 *   represents. This should always be provided unless this image is used for decorative purposes,
 *   and does not represent a meaningful action that a user can take. This text should be
 *   localized.
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content.
 * @param contentScale How to lay the image out with respect to its bounds, if the bounds are
 *   smaller than the image.
 * @param colorFilter The effects to use to modify the color of an image.
 */
@Composable
fun Image(
    provider: ImageProvider,
    contentDescription: String?,
    modifier: GlanceModifier = GlanceModifier,
    contentScale: ContentScale = ContentScale.Fit,
    colorFilter: ColorFilter? = null
) {
    val finalModifier = if (contentDescription != null) {
        modifier.semantics {
            this.contentDescription = contentDescription
        }
    } else {
        modifier
    }

    GlanceNode(
        factory = ::EmittableImage,
        update = {
            this.set(provider) { this.provider = it }
            this.set(finalModifier) { this.modifier = it }
            this.set(contentScale) { this.contentScale = it }
            this.set(colorFilter) { this.colorFilterParams = it?.colorFilterParams }
        }
    )
}
