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

package androidx.wear.protolayout.material3

import androidx.wear.protolayout.DimensionBuilders.ExpandedDimensionProp
import androidx.wear.protolayout.DimensionBuilders.ImageDimension
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.ContentScaleMode
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.background
import androidx.wear.protolayout.modifiers.clip
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers
import androidx.wear.protolayout.types.LayoutColor

/**
 * Returns the image background with the defined style.
 *
 * Material components provide proper defaults for the background image. In order to take advantage
 * of those defaults, this should be used with the resource ID only: `backgroundImage("id")`.
 *
 * This image can have optional overlay on top of it, that is usually a dark color with opacity.
 * This is highly recommended to be added when there's additional content like text on top of image,
 * to improve readability.
 *
 * If this is used in [imageButton] as image button with no other content, [overlayColor] can be
 * omitted, and the overlay color on top of the image would be ignored.
 *
 * @param protoLayoutResourceId The protolayout resource id of the image. Node that, this is not an
 *   Android resource id.
 * @param modifier Modifiers to set to this element.
 * @param width The width of an image. Usually, this matches the width of the parent component this
 *   is used in.
 * @param height The height of an image. Usually, this matches the height of the parent component
 *   this is used in.
 * @param overlayColor The color used to provide the overlay over the image for better readability.
 *   It's recommended to use [ColorScheme.background] color with 60% opacity. If `null`, overlay
 *   would be ignored.
 * @param contentScaleMode The content scale mode for the image to define how image will adapt to
 *   the given size
 */
public fun MaterialScope.backgroundImage(
    protoLayoutResourceId: String,
    modifier: LayoutModifier = LayoutModifier,
    width: ImageDimension = defaultBackgroundImageStyle.width,
    height: ImageDimension = defaultBackgroundImageStyle.height,
    overlayColor: LayoutColor? = defaultBackgroundImageStyle.overlayColor,
    @ContentScaleMode contentScaleMode: Int = defaultBackgroundImageStyle.contentScaleMode,
): LayoutElement =
    Box.Builder()
        .setWidth(if (width is ExpandedDimensionProp) expand() else wrap())
        .setHeight(if (height is ExpandedDimensionProp) expand() else wrap())
        // Image content
        .addContent(
            Image.Builder()
                .setWidth(width)
                .setHeight(height)
                .setModifiers(
                    (LayoutModifier.clip(defaultBackgroundImageStyle.shape) then modifier)
                        .toProtoLayoutModifiers()
                )
                .setResourceId(protoLayoutResourceId)
                .setContentScaleMode(contentScaleMode)
                .build()
        )
        .apply {
            // Overlay above it for contrast, if specified.
            if (overlayColor != null) {
                this.addContent(
                    Box.Builder()
                        .setWidth(expand())
                        .setHeight(expand())
                        .setModifiers(
                            LayoutModifier.background(overlayColor).toProtoLayoutModifiers()
                        )
                        .build()
                )
            }
        }
        .build()

/**
 * Returns the avatar image with the defined style.
 *
 * Material components such as [appCard] provide proper defaults for the small avatar image. In
 * order to take advantage of those defaults, this should be used with the resource ID only:
 * `avatarImage("id")`.
 *
 * @param protoLayoutResourceId The protolayout resource id of the image. Node that, this is not an
 *   Android resource id.
 * @param modifier Modifiers to set to this element.
 * @param width The width of an image. Usually, a small image that fit into the component's slot.
 * @param height The height of an image. Usually, a small image that fit into the component's slot.
 * @param contentScaleMode The content scale mode for the image to define how image will adapt to
 *   the given size
 */
public fun MaterialScope.avatarImage(
    protoLayoutResourceId: String,
    width: ImageDimension = defaultAvatarImageStyle.width,
    height: ImageDimension = defaultAvatarImageStyle.height,
    modifier: LayoutModifier = LayoutModifier,
    @ContentScaleMode contentScaleMode: Int = defaultAvatarImageStyle.contentScaleMode,
): LayoutElement =
    Image.Builder()
        .setWidth(width)
        .setHeight(height)
        .setModifiers(
            (LayoutModifier.clip(defaultAvatarImageStyle.shape) then modifier)
                .toProtoLayoutModifiers()
        )
        .setResourceId(protoLayoutResourceId)
        .setContentScaleMode(contentScaleMode)
        .build()
