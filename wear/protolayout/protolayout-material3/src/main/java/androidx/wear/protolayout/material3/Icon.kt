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

import androidx.wear.protolayout.DimensionBuilders.ImageDimension
import androidx.wear.protolayout.LayoutElementBuilders.ColorFilter
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ResourceBuilders.ImageResource
import androidx.wear.protolayout.layout.basicImage
import androidx.wear.protolayout.types.LayoutColor

/**
 * Returns the icon components with the defined style which will be automatically registered as a
 * resource.
 *
 * Material components provide proper defaults for this icon. In order to take advantage of those,
 * this should be used with the resource ID only: `icon(imageResource)`.
 *
 * Note that, by using this method, there's no need to register resource for Tiles in
 * `androidx.wear.tiles.TileService.onTileResourcesRequest`.
 *
 * @param resource An Image resource, used in the layout in the place of this element.
 * @param protoLayoutResourceId The optional protolayout resource id of the icon.
 * @param width The width of the icon.
 * @param height The height of the icon.
 * @param tintColor The color used to tint the icon.
 *     @throws IllegalStateException if this method is called without
 *       [androidx.wear.protolayout.ProtoLayoutScope] set to the [MaterialScope], i.e. when
 *       [materialScopeWithResources] wasn't used
 */
public fun MaterialScope.icon(
    resource: ImageResource,
    protoLayoutResourceId: String? = null,
    width: ImageDimension = defaultIconStyle.width,
    height: ImageDimension = defaultIconStyle.height,
    tintColor: LayoutColor = defaultIconStyle.tintColor,
): LayoutElement {
    require(hasProtoLayoutScope) {
        "APIs for automatic resource registration must have ProtoLayoutScope in MaterialScope."
    }
    return protoLayoutScope.basicImage(
        resource = resource,
        protoLayoutResourceId = protoLayoutResourceId,
        width = width,
        height = height,
        tintColor = tintColor,
    )
}

/**
 * Returns the icon components with the defined style.
 *
 * Material components provide proper defaults for this icon. In order to take advantage of those,
 * this should be used with the resource ID only: `icon("id")`.
 *
 * @param protoLayoutResourceId The protolayout resource id of the icon. Note that, this is not an
 *   Android resource id.
 * @param width The width of the icon.
 * @param height The height of the icon.
 * @param tintColor The color used to tint the icon.
 */
@Deprecated(
    "Use icon(ImageResource, String, ImageDimension, ImageDimension, LayoutColor) instead, in combination with [androidx.wear.tiles.Material3TileService] that creates scopes for you."
)
@Suppress("deprecation")
public fun MaterialScope.icon(
    protoLayoutResourceId: String,
    width: ImageDimension = defaultIconStyle.width,
    height: ImageDimension = defaultIconStyle.height,
    tintColor: LayoutColor = defaultIconStyle.tintColor,
): LayoutElement =
    Image.Builder()
        .setResourceId(protoLayoutResourceId)
        .setWidth(width)
        .setHeight(height)
        .setColorFilter(ColorFilter.Builder().setTint(tintColor.prop).build())
        .build()
