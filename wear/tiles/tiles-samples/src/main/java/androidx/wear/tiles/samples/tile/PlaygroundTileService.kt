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

package androidx.wear.tiles.samples.tile

import android.content.Context
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId
import androidx.wear.protolayout.ResourceBuilders.ImageResource
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.VersionBuilders.VersionInfo
import androidx.wear.protolayout.expression.dynamicDataMapOf
import androidx.wear.protolayout.expression.intAppDataKey
import androidx.wear.protolayout.expression.mapTo
import androidx.wear.protolayout.material3.AvatarButtonStyle.Companion.largeAvatarButtonStyle
import androidx.wear.protolayout.material3.ButtonDefaults.filledVariantButtonColors
import androidx.wear.protolayout.material3.CardColors
import androidx.wear.protolayout.material3.CardDefaults.filledTonalCardColors
import androidx.wear.protolayout.material3.CardDefaults.filledVariantCardColors
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.recommendedAnimationSpec
import androidx.wear.protolayout.material3.DataCardStyle.Companion.extraLargeDataCardStyle
import androidx.wear.protolayout.material3.DataCardStyle.Companion.smallCompactDataCardStyle
import androidx.wear.protolayout.material3.GraphicDataCardDefaults.constructGraphic
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.MAX_PRIMARY_LAYOUT_MARGIN
import androidx.wear.protolayout.material3.TextButtonStyle.Companion.smallTextButtonStyle
import androidx.wear.protolayout.material3.appCard
import androidx.wear.protolayout.material3.avatarButton
import androidx.wear.protolayout.material3.avatarImage
import androidx.wear.protolayout.material3.button
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.compactButton
import androidx.wear.protolayout.material3.graphicDataCard
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.iconButton
import androidx.wear.protolayout.material3.iconDataCard
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.segmentedCircularProgressIndicator
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textButton
import androidx.wear.protolayout.material3.textDataCard
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.modifiers.loadAction
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.protolayout.types.asLayoutConstraint
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.samples.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlin.random.Random

private const val RESOURCES_VERSION = "0"

/** Base playground tile service for testing out features. */
class PlaygroundTileService : TileService() {
    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> = resources()

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> = tile(requestParams, this)
}

private const val AVATAR_ID = "id"
private const val ICON_ID = "icon"

private fun resources() =
    Futures.immediateFuture(
        ResourceBuilders.Resources.Builder()
            .addIdToImageMapping(
                AVATAR_ID,
                ImageResource.Builder()
                    .setAndroidResourceByResId(
                        AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.avatar)
                            .build()
                    )
                    .build()
            )
            .addIdToImageMapping(
                ICON_ID,
                ImageResource.Builder()
                    .setAndroidResourceByResId(
                        AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.baseline_blender_24)
                            .build()
                    )
                    .build()
            )
            .setVersion(RESOURCES_VERSION)
            .build()
    )

private fun tile(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): ListenableFuture<TileBuilders.Tile> =
    Futures.immediateFuture(
        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(tileLayout(requestParams, context))
            )
            .build()
    )

private fun getFooValue(): Int = Random.nextInt(1000)

private fun tileLayout(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): LayoutElementBuilders.LayoutElement =
    materialScope(context = context, deviceConfiguration = requestParams.deviceConfiguration) {
        val fooKey = intAppDataKey("foo")
        val dynamicFooValue = DynamicInt32.from(fooKey).format()
        primaryLayout(
            mainSlot = { graphicDataCardSample() },
            margins = MAX_PRIMARY_LAYOUT_MARGIN,
            bottomSlot = {
                textEdgeButton(
                    onClick =
                        clickable(
                            action = loadAction(dynamicDataMapOf(fooKey mapTo getFooValue()))
                        ),
                    modifier = LayoutModifier.contentDescription("EdgeButton"),
                ) {
                    text(
                        LayoutString(
                            staticValue = "Edge ---",
                            dynamicValue = dynamicFooValue,
                            "999".asLayoutConstraint()
                        )
                    )
                }
            }
        )
    }

private fun MaterialScope.avatarButtonSample() =
    avatarButton(
        onClick = clickable(),
        modifier = LayoutModifier.contentDescription("Avatar button"),
        avatarContent = { avatarImage(AVATAR_ID) },
        style = largeAvatarButtonStyle(),
        horizontalAlignment = LayoutElementBuilders.HORIZONTAL_ALIGN_END,
        labelContent = { text("Primary label overflowing".layoutString) },
        secondaryLabelContent = { text("Secondary label overflowing".layoutString) },
    )

private fun MaterialScope.pillShapeButton() =
    button(
        onClick = clickable(),
        modifier = LayoutModifier.contentDescription("Pill button"),
        width = expand(),
        iconContent = { icon(ICON_ID) },
        labelContent = { text("Primary label".layoutString) },
        secondaryLabelContent = { text("Secondary label".layoutString) },
    )

private fun MaterialScope.compactShapeButton() =
    compactButton(
        onClick = clickable(),
        modifier = LayoutModifier.contentDescription("Compact button"),
        width = expand(),
        iconContent = { icon(ICON_ID) },
        labelContent = { text("Overflowing compact button".layoutString) },
    )

private fun MaterialScope.oneSlotButtons() = buttonGroup {
    buttonGroupItem {
        iconButton(
            onClick = clickable(),
            modifier = LayoutModifier.contentDescription("Icon button"),
            width = expand(),
            height = expand(),
            iconContent = { icon(ICON_ID) }
        )
    }
    buttonGroupItem {
        iconButton(
            onClick = clickable(),
            modifier = LayoutModifier.contentDescription("Icon button"),
            width = expand(),
            height = expand(),
            shape = shapes.none,
            iconContent = { icon(ICON_ID) }
        )
    }
    buttonGroupItem {
        textButton(
            onClick = clickable(),
            modifier = LayoutModifier.contentDescription("Text button"),
            width = expand(),
            height = expand(),
            style = smallTextButtonStyle(),
            shape = shapes.extraSmall,
            colors = filledVariantButtonColors(),
            labelContent = { text("Dec".layoutString) }
        )
    }
}

private fun MaterialScope.appCardSample() =
    appCard(
        onClick = clickable(),
        modifier = LayoutModifier.contentDescription("Sample Card"),
        colors =
            CardColors(
                backgroundColor = colorScheme.tertiary,
                titleColor = colorScheme.onTertiary,
                contentColor = colorScheme.onTertiary,
                timeColor = colorScheme.onTertiary
            ),
        title = {
            text(
                "Title Card!".layoutString,
                maxLines = 1,
            )
        },
        content = {
            text(
                "Content of this Card!".layoutString,
                maxLines = 1,
            )
        },
        label = {
            text(
                "Hello and welcome Tiles in AndroidX!".layoutString,
            )
        },
        avatar = { avatarImage(AVATAR_ID) },
        time = {
            text(
                "NOW".layoutString,
            )
        }
    )

private fun MaterialScope.graphicDataCardSample() =
    graphicDataCard(
        onClick = clickable(),
        modifier = LayoutModifier.contentDescription("Graphic Data Card"),
        height = expand(),
        horizontalAlignment = LayoutElementBuilders.HORIZONTAL_ALIGN_END,
        title = {
            text(
                "1,234!".layoutString,
            )
        },
        content = {
            text(
                "steps".layoutString,
            )
        },
        graphic = {
            constructGraphic(
                mainContent = {
                    segmentedCircularProgressIndicator(
                        segmentCount = 6,
                        startAngleDegrees = 200F,
                        endAngleDegrees = 520F,
                        dynamicProgress =
                            DynamicFloat.animate(0.0F, 1.5F, recommendedAnimationSpec),
                    )
                },
                iconContent = { icon(ICON_ID) }
            )
        }
    )

private fun MaterialScope.graphicDataCardSampleWithFallbackProgressIndicator(context: Context) =
    graphicDataCard(
        onClick = clickable(),
        modifier = LayoutModifier.contentDescription("Graphic Data Card"),
        height = expand(),
        horizontalAlignment = LayoutElementBuilders.HORIZONTAL_ALIGN_END,
        title = {
            text(
                "1,234!".layoutString,
            )
        },
        content = {
            text(
                "steps".layoutString,
            )
        },
        graphic = {
            materialScope(
                context = context,
                deviceConfiguration =
                    DeviceParametersBuilders.DeviceParameters.Builder()
                        .setRendererSchemaVersion(
                            VersionInfo.Builder().setMajor(1).setMinor(402).build()
                        )
                        .build()
            ) {
                segmentedCircularProgressIndicator(
                    segmentCount = 6,
                    startAngleDegrees = 200F,
                    endAngleDegrees = 520F,
                    dynamicProgress = DynamicFloat.animate(0.0F, 1.5F, recommendedAnimationSpec),
                    size = dp(55F)
                )
            }
        }
    )

private fun MaterialScope.dataCards() = buttonGroup {
    buttonGroupItem {
        textDataCard(
            onClick = clickable(),
            modifier = LayoutModifier.contentDescription("Data Card with icon"),
            width = weight(1f),
            height = expand(),
            colors = filledTonalCardColors(),
            style = extraLargeDataCardStyle(),
            title = { this.text("1km".layoutString) },
            secondaryText = { this.text(AVATAR_ID.layoutString) },
            content = { this.text("Run".layoutString) },
        )
    }
    buttonGroupItem {
        iconDataCard(
            onClick = clickable(),
            modifier =
                LayoutModifier.contentDescription(
                    "Compact Data Card without icon or secondary label"
                ),
            width = weight(2f),
            height = expand(),
            colors = filledVariantCardColors(),
            style = smallCompactDataCardStyle(),
            title = { this.text("1".layoutString) },
            content = { this.text("PM".layoutString) },
        )
    }
}
