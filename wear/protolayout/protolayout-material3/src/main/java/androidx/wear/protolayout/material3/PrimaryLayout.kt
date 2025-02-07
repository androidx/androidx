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

import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material3.PredefinedPrimaryLayoutMargins.defaultPrimaryLayoutMargins
import androidx.wear.protolayout.material3.PredefinedPrimaryLayoutMargins.maxBottomMargin
import androidx.wear.protolayout.material3.PredefinedPrimaryLayoutMargins.maxPrimaryLayoutMargins
import androidx.wear.protolayout.material3.PredefinedPrimaryLayoutMargins.maxSideMargin
import androidx.wear.protolayout.material3.PredefinedPrimaryLayoutMargins.midPrimaryLayoutMargins
import androidx.wear.protolayout.material3.PredefinedPrimaryLayoutMargins.minPrimaryLayoutMargins
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.BOTTOM_EDGE_BUTTON_TOP_MARGIN_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.BOTTOM_SLOT_OTHER_MARGIN_BOTTOM_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.BOTTOM_SLOT_OTHER_MARGIN_SIDE_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.BOTTOM_SLOT_OTHER_MARGIN_TOP_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.FOOTER_LABEL_SLOT_MARGIN_SIDE_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.FOOTER_LABEL_TO_BOTTOM_SLOT_SPACER_HEIGHT_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.HEADER_ICON_SIZE_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.HEADER_ICON_TITLE_SPACER_HEIGHT_LARGE_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.HEADER_ICON_TITLE_SPACER_HEIGHT_SMALL_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.HEADER_MARGIN_BOTTOM_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.HEADER_MARGIN_SIDE_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.HEADER_MARGIN_TOP_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.METADATA_TAG
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.percentageHeightToDp
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.DEFAULT_PRIMARY_LAYOUT_MARGIN
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.MAX_PRIMARY_LAYOUT_MARGIN
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.MID_PRIMARY_LAYOUT_MARGIN
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.MIN_PRIMARY_LAYOUT_MARGIN
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.customizedPrimaryLayoutMargin
import androidx.wear.protolayout.material3.PrimaryLayoutMarginsImpl.Companion.DEFAULT
import androidx.wear.protolayout.material3.PrimaryLayoutMarginsImpl.Companion.MAX
import androidx.wear.protolayout.material3.PrimaryLayoutMarginsImpl.Companion.MID
import androidx.wear.protolayout.material3.PrimaryLayoutMarginsImpl.Companion.MIN
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers
import androidx.wear.protolayout.types.dp

/**
 * ProtoLayout Material3 full screen layout that represents a suggested Material3 layout style that
 * is responsive and takes care of the elements placement, together with the recommended margin and
 * padding applied.
 *
 * This layout is meant to occupy the whole screen, so nothing else should be added on top of it.
 *
 * On the top, there is an icon that will be automatically placed by the system, followed by the
 * optional title slot. The icon slot needs to be reserved for the whole ProtoLayout Layout and no
 * other content should be added at the top of the screen as it will be overlapped with the system
 * placed icon.
 *
 * At the bottom, there is an optional fixed slot for either {@link EdgeButton} as a main action or
 * small non tappable content.
 *
 * The middle of the layout is main content, that will fill the available space. For the best
 * results across different screen sizes, it's recommended that this content's dimension are also
 * [DimensionBuilders.expand] or [DimensionBuilders.weight]. Additional content in the main one can
 * be added after a `225dp` breakpoint.
 *
 * @param mainSlot The main, central content for this layout. It's recommended for this content to
 *   fill the available width and height for the best result across different screen size. This
 *   layout places proper padding to prevent content from being cropped by the screen. Note that
 *   depending on the corner shapes and different elements on the screen, there might be a need to
 *   change padding on some of the elements in this slot. The content passed here can also have an
 *   additional content value added to it, after `225dp` breakpoint. Some of the examples of content
 *   that can be passed in here are:
 *     * [buttonGroup] with buttons or cards
 *     * two [buttonGroup
 *     * Expanded card
 *
 * @param titleSlot The app title in the top slot, just below the icon. This should be one line of
 *   [text] with [Typography.TITLE_SMALL] typography, describing the main purpose of this layout.
 *   Title is an optional slot which can be omitted to make space for other elements. Defaults to
 *   [ColorScheme.onBackground] color.
 * @param bottomSlot The content for bottom slot in this layout, that will be anchored to the bottom
 *   edge of the screen. This should be either a small non tappable content such as Text with
 *   optional label for it or tappable main action with [textEdgeButton] or [iconEdgeButton] which
 *   is designed to have its bottom following the screen's curvature. This bottom slot is optional,
 *   if unset the main content will expand more towards the edge of the screen.
 * @param labelForBottomSlot The label displayed just above the [bottomSlot]. Default will be one
 *   line of [text] with [Typography.TITLE_SMALL] typography, [ColorScheme.onSurface] color that
 *   should contain additional description of this layout. When the [bottomSlot] is not provided or
 *   it an edge button, the given label will be ignored.
 * @param onClick The clickable action for whole layout. If any area (outside of other added
 *   tappable components) is clicked, it will fire the associated action.
 * @param margins The customized outer margin that will be applied as following:
 *     * `start` and `end` would be applied as a side margins on [mainSlot]
 *     * `bottom` would be applied as a bottom margin when [bottomSlot] is not present.
 *
 *   It is highly recommended to use provided constants for these
 *   margins - [DEFAULT_PRIMARY_LAYOUT_MARGIN], [MIN_PRIMARY_LAYOUT_MARGIN],
 *   [MID_PRIMARY_LAYOUT_MARGIN] or [MAX_PRIMARY_LAYOUT_MARGIN], depending on inner content and its
 *   corners shape. If providing custom numbers by [customizedPrimaryLayoutMargin], it is a
 *   requirement for those to be percentages of the screen width and height.
 *
 * @sample androidx.wear.protolayout.material3.samples.topLevelLayout
 * @sample androidx.wear.protolayout.material3.samples.cardSample
 * @sample androidx.wear.protolayout.material3.samples.oneSlotButtonsSample
 * @sample androidx.wear.protolayout.material3.samples.graphicDataCardSample
 */
// TODO: b/346958146 - Link visuals once they are available.
// TODO: b/353247528 - Handle the icon.
// TODO: b/370976767 - Specify that this should be used with MaterialTileService.
public fun MaterialScope.primaryLayout(
    mainSlot: (MaterialScope.() -> LayoutElement),
    titleSlot: (MaterialScope.() -> LayoutElement)? = null,
    bottomSlot: (MaterialScope.() -> LayoutElement)? = null,
    labelForBottomSlot: (MaterialScope.() -> LayoutElement)? = null,
    onClick: Clickable? = null,
    margins: PrimaryLayoutMargins = DEFAULT_PRIMARY_LAYOUT_MARGIN
): LayoutElement =
    primaryLayoutWithOverrideIcon(
        overrideIcon = false,
        titleSlot = titleSlot,
        mainSlot = mainSlot,
        bottomSlot = bottomSlot,
        labelForBottomSlot = labelForBottomSlot,
        onClick = onClick,
        margins = margins
    )

/**
 * Overrides the icon slot by showing colors circle. For the rest, see [primaryLayout]. This should
 * only be used for testing or building internal samples to validate the UI.
 */
// TODO: b/353247528 - Set as @VisibleForTesting only.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun MaterialScope.primaryLayoutWithOverrideIcon(
    overrideIcon: Boolean,
    titleSlot: (MaterialScope.() -> LayoutElement)? = null,
    mainSlot: (MaterialScope.() -> LayoutElement)? = null,
    bottomSlot: (MaterialScope.() -> LayoutElement)? = null,
    labelForBottomSlot: (MaterialScope.() -> LayoutElement)? = null,
    onClick: Clickable? = null,
    margins: PrimaryLayoutMargins = DEFAULT_PRIMARY_LAYOUT_MARGIN
): LayoutElement {
    val screenWidth = deviceConfiguration.screenWidthDp
    val screenHeight = deviceConfiguration.screenHeightDp
    val labelSlot: LayoutElement? =
        labelForBottomSlot?.let {
            withStyle(
                    defaultTextElementStyle =
                        TextElementStyle(
                            typography = Typography.TITLE_SMALL,
                            color = theme.colorScheme.onSurface
                        )
                )
                .labelForBottomSlot()
        }

    val modifiers =
        Modifiers.Builder()
            .setMetadata(ElementMetadata.Builder().setTagData(METADATA_TAG.toTagBytes()).build())

    onClick?.apply { modifiers.setClickable(this) }

    val mainLayout =
        Column.Builder()
            .setModifiers(modifiers.build())
            .setWidth(screenWidth.toDp())
            .setHeight(screenHeight.toDp())
            // Contains icon and optional title.
            .addContent(
                getHeaderContent(
                    titleSlot?.let {
                        withStyle(
                                defaultTextElementStyle =
                                    TextElementStyle(
                                        typography = Typography.TITLE_SMALL,
                                        color = theme.colorScheme.onBackground
                                    )
                            )
                            .titleSlot()
                    },
                    overrideIcon
                )
            )

    val bottomSlotValue = bottomSlot?.let { bottomSlot() }

    val marginsValues: Padding =
        withStyle(
                layoutSlotsPresence =
                    LayoutSlotsPresence(
                        isTitleSlotPresent = titleSlot != null,
                        isBottomSlotPresent = bottomSlot != null,
                        isBottomSlotEdgeButton = bottomSlotValue?.isSlotEdgeButton() == true
                    )
            )
            .let { scope ->
                if (margins is PrimaryLayoutMarginsImpl) {
                    when (margins.size) {
                        MIN -> scope.minPrimaryLayoutMargins()
                        MID -> scope.midPrimaryLayoutMargins()
                        MAX -> scope.maxPrimaryLayoutMargins()
                        DEFAULT -> scope.defaultPrimaryLayoutMargins()
                        else -> scope.defaultPrimaryLayoutMargins()
                    }
                } else if (margins is CustomPrimaryLayoutMargins) {
                    margins.toPadding(scope)
                } else {
                    // Fallback to default
                    scope.defaultPrimaryLayoutMargins()
                }
            }

    // Contains main content. This Box is needed to set to expand, even if empty so it
    // fills the empty space until bottom content.
    mainSlot?.let {
        mainLayout.addContent(
            mainSlot()
                .getMainContentBox(
                    sideMargins = marginsValues,
                    maxSideMarginFallbackDp = maxSideMargin()
                )
        )
    }

    // Contains bottom slot, optional label or needed padding if empty.
    mainLayout.addContent(
        getFooterContent(
            bottomSlot = bottomSlotValue,
            labelSlot = labelSlot,
            bottomMarginForNoContentDp = marginsValues.bottom?.value ?: maxBottomMargin()
        )
    )

    return mainLayout.build()
}

private fun MaterialScope.getIconPlaceholder(overrideIcon: Boolean): LayoutElement {
    val iconSlot = Box.Builder().setWidth(HEADER_ICON_SIZE_DP.dp).setHeight(HEADER_ICON_SIZE_DP.dp)
    if (overrideIcon) {
        iconSlot.setModifiers(
            Modifiers.Builder()
                .setBackground(
                    ModifiersBuilders.Background.Builder()
                        .setCorner(shapes.full)
                        .setColor(theme.colorScheme.onBackground.prop)
                        .build()
                )
                .build()
        )
    }
    return iconSlot.build()
}

/** Returns header content with the mandatory icon and optional title. */
private fun MaterialScope.getHeaderContent(
    titleSlot: LayoutElement?,
    overrideIcon: Boolean
): Column {
    val headerBuilder =
        Column.Builder()
            .setWidth(wrap())
            .setHeight(wrap())
            .setModifiers(Modifiers.Builder().setPadding(getMarginForHeader()).build())
            .addContent(getIconPlaceholder(overrideIcon))

    titleSlot?.apply {
        headerBuilder
            .addContent(
                horizontalSpacer(
                    if (deviceConfiguration.screenHeightDp.isBreakpoint()) {
                        HEADER_ICON_TITLE_SPACER_HEIGHT_LARGE_DP
                    } else {
                        HEADER_ICON_TITLE_SPACER_HEIGHT_SMALL_DP
                    }
                )
            )
            .addContent(titleSlot)
    }

    return headerBuilder.build()
}

/** Returns central slot with the optional main content. It expands to fill the available space. */
private fun LayoutElement.getMainContentBox(
    sideMargins: Padding,
    maxSideMarginFallbackDp: Float,
): Box {
    // Start and end Padding shouldn't be null if these are predefined margins, but if developers
    // sets some other object, we will fallback to the max margin.
    val sideMarginStart = sideMargins.start?.value ?: maxSideMarginFallbackDp
    val sideMarginEnd = sideMargins.end?.value ?: maxSideMarginFallbackDp
    return Box.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setModifiers(
            // Top and bottom space has been added to other elements.
            LayoutModifier.padding(start = sideMarginStart, end = sideMarginEnd)
                .toProtoLayoutModifiers()
        )
        .addContent(this)
        .build()
}

/**
 * Returns the footer content, containing bottom slot and optional label with the corresponding
 * spacing and margins depending on what is that content, or Box with padding if there's no bottom
 * slot.
 */
private fun MaterialScope.getFooterContent(
    bottomSlot: LayoutElement?,
    labelSlot: LayoutElement?,
    bottomMarginForNoContentDp: Float
): LayoutElement {
    val footer = Box.Builder().setWidth(wrap()).setHeight(wrap())

    if (bottomSlot == null) {
        footer.setWidth(expand())
        footer.setHeight(bottomMarginForNoContentDp.dp)
    } else if (bottomSlot.isSlotEdgeButton()) {
        // Label shouldn't be used with EdgeButton.
        footer.setModifiers(
            Modifiers.Builder()
                .setPadding(Padding.Builder().setTop(BOTTOM_EDGE_BUTTON_TOP_MARGIN_DP.dp).build())
                .build()
        )

        footer.addContent(bottomSlot)
    } else {
        val otherBottomSlot = Column.Builder().setWidth(wrap()).setHeight(wrap())

        footer.setModifiers(
            Modifiers.Builder()
                .setPadding(
                    Padding.Builder()
                        .setTop(BOTTOM_SLOT_OTHER_MARGIN_TOP_DP.dp)
                        .setBottom(
                            percentageHeightToDp(BOTTOM_SLOT_OTHER_MARGIN_BOTTOM_PERCENTAGE / 100)
                                .dp
                        )
                        .build()
                )
                .build()
        )

        labelSlot?.apply {
            otherBottomSlot
                .addContent(
                    generateLabelContent(
                        (FOOTER_LABEL_SLOT_MARGIN_SIDE_PERCENTAGE *
                                deviceConfiguration.screenWidthDp)
                            .dp
                    )
                )
                .addContent(horizontalSpacer(FOOTER_LABEL_TO_BOTTOM_SLOT_SPACER_HEIGHT_DP))
        }

        footer.addContent(
            otherBottomSlot
                .addContent(
                    bottomSlot.generateBottomSlotContent(
                        (BOTTOM_SLOT_OTHER_MARGIN_SIDE_PERCENTAGE *
                                deviceConfiguration.screenWidthDp)
                            .dp
                    )
                )
                .build()
        )
    }

    return footer.build()
}

private fun LayoutElement.generateBottomSlotContent(sidePadding: DpProp): LayoutElement =
    Box.Builder()
        .setModifiers(
            Modifiers.Builder()
                .setPadding(Padding.Builder().setStart(sidePadding).setEnd(sidePadding).build())
                .build()
        )
        .addContent(this)
        .build()

private fun LayoutElement.generateLabelContent(sidePadding: DpProp): LayoutElement =
    Box.Builder()
        .setModifiers(
            Modifiers.Builder()
                .setPadding(Padding.Builder().setStart(sidePadding).setEnd(sidePadding).build())
                .build()
        )
        .addContent(this)
        .build()

private fun MaterialScope.getMarginForHeader() =
    padding(
        top = HEADER_MARGIN_TOP_DP,
        bottom = HEADER_MARGIN_BOTTOM_DP,
        start = HEADER_MARGIN_SIDE_PERCENTAGE * deviceConfiguration.screenWidthDp,
        end = HEADER_MARGIN_SIDE_PERCENTAGE * deviceConfiguration.screenWidthDp
    )

/** Contains the default values used by Material layout. */
internal object PrimaryLayoutDefaults {
    internal fun MaterialScope.percentageWidthToDp(
        @FloatRange(from = 0.0, to = 1.0) percentage: Float
    ): Float = percentage * deviceConfiguration.screenWidthDp

    internal fun MaterialScope.percentageHeightToDp(
        @FloatRange(from = 0.0, to = 1.0) percentage: Float
    ): Float = percentage * deviceConfiguration.screenHeightDp

    /** Tool tag for Metadata in Modifiers, so we know that Row is actually a PrimaryLayout. */
    @VisibleForTesting internal const val METADATA_TAG: String = "M3_PL"

    @Dimension(DP) internal const val HEADER_MARGIN_TOP_DP = 3f

    @Dimension(DP) internal const val HEADER_MARGIN_BOTTOM_DP = 6f

    internal const val HEADER_MARGIN_SIDE_PERCENTAGE = 14.5f / 100

    @Dimension(DP) internal const val HEADER_ICON_SIZE_DP = 24f

    @Dimension(DP) internal const val HEADER_ICON_TITLE_SPACER_HEIGHT_SMALL_DP = 2
    @Dimension(DP) internal const val HEADER_ICON_TITLE_SPACER_HEIGHT_LARGE_DP = 4

    // The remaining margins around EdgeButton are within the component itself.
    @Dimension(DP) internal const val BOTTOM_EDGE_BUTTON_TOP_MARGIN_DP = 4f

    @Dimension(DP) internal const val BOTTOM_SLOT_OTHER_MARGIN_TOP_DP = 6f
    internal const val BOTTOM_SLOT_OTHER_MARGIN_BOTTOM_PERCENTAGE = 5.2f

    internal const val BOTTOM_SLOT_OTHER_MARGIN_SIDE_PERCENTAGE = 26f / 100

    @Dimension(DP) internal const val FOOTER_LABEL_TO_BOTTOM_SLOT_SPACER_HEIGHT_DP = 2

    internal const val FOOTER_LABEL_SLOT_MARGIN_SIDE_PERCENTAGE = 16.64f / 100
}
