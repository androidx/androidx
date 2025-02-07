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

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.annotation.Dimension.Companion.SP
import androidx.annotation.FloatRange
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.WrappedDimensionProp
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_END
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_LEFT
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_RIGHT
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_START
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_UNDEFINED
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.TextAlignment
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ModifiersBuilders.SEMANTICS_ROLE_BUTTON
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.percentageHeightToDp
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.percentageWidthToDp
import androidx.wear.protolayout.material3.Versions.hasExpandWithWeightSupport
import androidx.wear.protolayout.materialcore.fontscaling.FontScaleConverterFactory
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.modifiers.semanticsRole
import androidx.wear.protolayout.modifiers.tag
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.dp
import java.nio.charset.StandardCharsets

/**
 * The breakpoint value defining the screen width on and after which, some properties should be
 * changed, depending on the use case.
 */
internal const val SCREEN_SIZE_BREAKPOINT_DP = 225

/** Minimum tap target for any clickable element. */
internal val MINIMUM_TAP_TARGET_SIZE: DpProp = 48f.dp

/** Returns byte array representation of tag from String. */
internal fun String.toTagBytes(): ByteArray = toByteArray(StandardCharsets.UTF_8)

/** Returns String representation of tag from Metadata. */
internal fun ElementMetadata.toTagName(): String = String(tagData, StandardCharsets.UTF_8)

/**
 * Adds the given [newItem] between each element in this [Iterable], starting after the first and
 * ending before the last one.
 */
internal fun <T> Iterable<T>.addBetween(newItem: T): Sequence<T> = sequence {
    var isFirst = true
    for (element in this@addBetween) {
        if (!isFirst) {
            yield(newItem)
        } else {
            isFirst = false
        }
        yield(element)
    }
}

@Dimension(unit = SP)
internal fun Float.dpToSp(fontScale: Float): Float {
    val converter =
        if (SDK_INT >= UPSIDE_DOWN_CAKE) FontScaleConverterFactory.forScale(fontScale) else null
    return converter?.convertDpToSp(this) ?: dpToSpLinear(fontScale)
}

@Dimension(unit = SP) private fun Float.dpToSpLinear(fontScale: Float): Float = this / fontScale

internal fun Int.toDp() = this.toFloat().dp

/** Builds a horizontal Spacer, with width set to expand and height set to the given value. */
internal fun horizontalSpacer(@Dimension(unit = DP) heightDp: Int): Spacer =
    Spacer.Builder().setWidth(expand()).setHeight(heightDp.toDp()).build()

/** Builds a vertical Spacer, with height set to expand and width set to the given value. */
internal fun verticalSpacer(@Dimension(unit = DP) widthDp: Int): Spacer =
    Spacer.Builder().setWidth(widthDp.toDp()).setHeight(expand()).build()

/** Builds a vertical Spacer, with height set to expand and width set to the given value. */
internal fun verticalSpacer(width: DimensionBuilders.SpacerDimension): Spacer =
    Spacer.Builder().setWidth(width).setHeight(expand()).build()

/**
 * Returns [wrap] but with minimum dimension of [MINIMUM_TAP_TARGET_SIZE] for accessibility
 * requirements of tap targets.
 */
internal fun wrapWithMinTapTargetDimension(): WrappedDimensionProp =
    WrappedDimensionProp.Builder().setMinimumSize(MINIMUM_TAP_TARGET_SIZE).build()

/**
 * Changes the opacity/transparency of the given color.
 *
 * Note that this only looks at the static value of the [LayoutColor], any dynamic value will be
 * ignored.
 */
internal fun LayoutColor.withOpacity(@FloatRange(from = 0.0, to = 1.0) ratio: Float): LayoutColor {
    // From androidx.core.graphics.ColorUtils
    require(ratio in 0.0..1.0) { "withOpacity ratio must be between 0 and 1." }
    val fullyOpaque = 255
    val alphaMask = 0x00ffffff
    val alpha = (ratio * fullyOpaque).toInt()
    val alphaPosition = 24
    return ((this.staticArgb and alphaMask) or (alpha shl alphaPosition)).argb
}

/** Returns corresponding text alignment based on the given horizontal alignment. */
@TextAlignment
internal fun Int.horizontalAlignToTextAlign(): Int =
    when (this) {
        HORIZONTAL_ALIGN_CENTER -> LayoutElementBuilders.TEXT_ALIGN_CENTER
        HORIZONTAL_ALIGN_LEFT,
        HORIZONTAL_ALIGN_START -> LayoutElementBuilders.TEXT_ALIGN_START
        HORIZONTAL_ALIGN_END,
        HORIZONTAL_ALIGN_RIGHT -> LayoutElementBuilders.TEXT_ALIGN_END
        HORIZONTAL_ALIGN_UNDEFINED -> LayoutElementBuilders.TEXT_ALIGN_UNDEFINED
        else -> LayoutElementBuilders.TEXT_ALIGN_UNDEFINED
    }

/**
 * Returns whether the provided DP size is equal or above the [SCREEN_SIZE_BREAKPOINT_DP]
 * breakpoint.
 */
internal fun Int.isBreakpoint() = this >= SCREEN_SIZE_BREAKPOINT_DP

/**
 * Builds [Box] that represents a clickable container with the given [content] inside, and
 * [SEMANTICS_ROLE_BUTTON], that can be used to create container or more opinionated card or button
 * variants.
 */
internal fun MaterialScope.componentContainer(
    onClick: Clickable,
    modifier: LayoutModifier,
    width: ContainerDimension,
    height: ContainerDimension,
    backgroundContent: (MaterialScope.() -> LayoutElement)?,
    contentPadding: Padding,
    metadataTag: String?,
    content: (MaterialScope.() -> LayoutElement)?,
): LayoutElement {
    val mod =
        LayoutModifier.semanticsRole(SEMANTICS_ROLE_BUTTON) then
            if (metadataTag != null) {
                modifier.clickable(onClick).tag(metadataTag)
            } else {
                modifier.clickable(onClick)
            }

    val container =
        Box.Builder().setHeight(height).setWidth(width).apply {
            content?.let { addContent(content()) }
        }

    if (backgroundContent == null) {
        container.setModifiers(mod.padding(contentPadding).toProtoLayoutModifiers())
        return container.build()
    }

    val protoLayoutModifiers = mod.toProtoLayoutModifiers()
    return Box.Builder()
        .setModifiers(protoLayoutModifiers)
        .addContent(
            withStyle(
                    defaultBackgroundImageStyle =
                        BackgroundImageStyle(
                            width = expand(),
                            height = expand(),
                            overlayColor = colorScheme.primary.withOpacity(0.6f),
                            overlayWidth = width,
                            overlayHeight = height,
                            shape = protoLayoutModifiers.background?.corner ?: shapes.large,
                            contentScaleMode = LayoutElementBuilders.CONTENT_SCALE_MODE_FILL_BOUNDS,
                        )
                )
                .backgroundContent()
        )
        .setWidth(width)
        .setHeight(height)
        .addContent(
            container
                // Padding in this case is needed on the inner content, not the whole card.
                .setModifiers(LayoutModifier.padding(contentPadding).toProtoLayoutModifiers())
                .build()
        )
        .build()
}

/**
 * Returns [Padding] objects with values represented as percentages from the screen size.
 *
 * @param start The ratio percentage of the screen width that should be use as start padding
 * @param end The ratio percentage of the screen width that should be use as end padding
 * @param bottom The ratio percentage of the screen width that should be use as bottom padding
 */
internal fun MaterialScope.percentagePadding(
    @FloatRange(from = 0.0, to = 1.0) start: Float,
    @FloatRange(from = 0.0, to = 1.0) end: Float,
    @FloatRange(from = 0.0, to = 1.0) bottom: Float,
): Padding =
    padding(
        start = percentageWidthToDp(start),
        end = percentageWidthToDp(end),
        bottom = percentageHeightToDp(bottom),
    )

/**
 * Returns [Padding] objects with values represented as percentages from the screen size, using only
 * horizontal padding.
 *
 * @param start The ratio percentage of the screen width that should be use as start padding
 * @param end The ratio percentage of the screen width that should be use as end padding
 */
internal fun MaterialScope.percentagePadding(
    @FloatRange(from = 0.0, to = 1.0) start: Float,
    @FloatRange(from = 0.0, to = 1.0) end: Float,
): Padding = padding(start = percentageWidthToDp(start), end = percentageWidthToDp(end))

/**
 * Returns [DimensionBuilders.ExpandedDimensionProp] with [weight] set to the given [weightValue]
 * represented as percentage up to 100%, when the renderer's schema version supports it. Otherwise,
 * returns [DpProp] with a value as [weightValue] percentage of the screen width.
 */
internal fun DeviceParameters.weightForSpacer(
    @FloatRange(from = 0.0, to = 100.0) weightValue: Float
): DimensionBuilders.SpacerDimension =
    if (rendererSchemaVersion.hasExpandWithWeightSupport()) {
        weight(weightValue)
    } else {
        (screenWidthDp * weightValue / 100f).dp
    }

/**
 * Returns [DimensionBuilders.ExpandedDimensionProp] with [weight] set to the given [weightValue]
 * represented as percentage up to 100%, when the renderer's schema version supports it. Otherwise,
 * returns [DimensionBuilders.ExpandedDimensionProp] with no weight, that will occupy remaining
 * space.
 */
internal fun MaterialScope.weightAsExpand(
    @FloatRange(from = 0.0, to = 100.0) weightValue: Float
): DimensionBuilders.ExpandedDimensionProp =
    if (deviceConfiguration.rendererSchemaVersion.hasExpandWithWeightSupport()) {
        weight(weightValue)
    } else {
        expand()
    }

/**
 * Returns [DimensionBuilders.ExpandedDimensionProp] with [weight] set to the given [weightValue]
 * represented as percentage up to 100%, when the renderer's schema version supports it. Otherwise,
 * returns [DpProp] with a value as [weightValue] percentage of the screen width.
 */
internal fun DeviceParameters.weightForContainer(
    @FloatRange(from = 0.0, to = 100.0) weightValue: Float
): ContainerDimension =
    if (rendererSchemaVersion.hasExpandWithWeightSupport()) {
        weight(weightValue)
    } else {
        (screenWidthDp * weightValue / 100f).dp
    }
