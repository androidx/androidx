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

package androidx.glance.layout

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.glance.Applier
import androidx.glance.Emittable
import androidx.glance.GlanceModifier

/**
 * Interface representing an Image source which can be used with a Glance [Image] element.
 */
public interface ImageProvider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
/** @suppress */
public class AndroidResourceImageProvider(@DrawableRes public val resId: Int) : ImageProvider

/**
 * Image resource from an Android Drawable resource.
 *
 * @param resId The resource ID of the Drawable resource to be used.
 */
public fun ImageProvider(@DrawableRes resId: Int): ImageProvider =
    AndroidResourceImageProvider(resId)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
/** @suppress */
public class EmittableImage : Emittable {
    override var modifier: GlanceModifier = GlanceModifier

    public var provider: ImageProvider? = null
    public var contentDescription: String? = null
    public var contentScale: ContentScale = ContentScale.Fit
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
 */
@Composable
public fun Image(
    provider: ImageProvider,
    contentDescription: String?,
    modifier: GlanceModifier = GlanceModifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    ComposeNode<EmittableImage, Applier>(
        factory = ::EmittableImage,
        update = {
            this.set(provider) { this.provider = it }
            this.set(contentDescription) { this.contentDescription = it }
            this.set(modifier) { this.modifier = it }
            this.set(contentScale) { this.contentScale = it }
        }
    )
}