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
import androidx.annotation.VisibleForTesting
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.DpProp
import androidx.wear.protolayout.DimensionBuilders.dp
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
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.BOTTOM_EDGE_BUTTON_TOP_MARGIN_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.BOTTOM_SLOT_EMPTY_MARGIN_BOTTOM_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.BOTTOM_SLOT_OTHER_MARGIN_SIDE_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.BOTTOM_SLOT_OTHER_NO_LABEL_MARGIN_BOTTOM_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.BOTTOM_SLOT_OTHER_NO_LABEL_MARGIN_TOP_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.BOTTOM_SLOT_OTHER_WITH_LABEL_MARGIN_BOTTOM_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.BOTTOM_SLOT_OTHER_WITH_LABEL_MARGIN_TOP_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.FOOTER_LABEL_SLOT_MARGIN_SIDE_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.FOOTER_LABEL_TO_BOTTOM_SLOT_SPACER_HEIGHT_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.HEADER_ICON_SIZE_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.HEADER_ICON_TITLE_SPACER_HEIGHT_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.HEADER_MARGIN_BOTTOM_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.HEADER_MARGIN_SIDE_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.HEADER_MARGIN_TOP_DP
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.MAIN_SLOT_WITHOUT_BOTTOM_SLOT_WITHOUT_TITLE_MARGIN_SIDE_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.MAIN_SLOT_WITHOUT_BOTTOM_SLOT_WITH_TITLE_MARGIN_SIDE_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.MAIN_SLOT_WITH_BOTTOM_SLOT_WITHOUT_TITLE_MARGIN_SIDE_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.MAIN_SLOT_WITH_BOTTOM_SLOT_WITH_TITLE_MARGIN_SIDE_PERCENTAGE
import androidx.wear.protolayout.material3.PrimaryLayoutDefaults.METADATA_TAG

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
 *   [ColorTokens.ON_BACKGROUND] color.
 * @param bottomSlot The content for bottom slot in this layout, that will be anchored to the bottom
 *   edge of the screen. This should be either a small non tappable content such as Text with
 *   optional label for it or tappable main action with [textEdgeButton] or [iconEdgeButton] which
 *   is designed to have its bottom following the screen's curvature. This bottom slot is optional,
 *   if unset the main content will expand more towards the edge of the screen.
 * @param labelForBottomSlot The label displayed just above the [bottomSlot]. Default will be one
 *   line of [text] with [Typography.TITLE_SMALL] typography, [ColorTokens.ON_SURFACE] color that
 *   should contain additional description of this layout. When the [bottomSlot] is not provided or
 *   it an edge button, the given label will be ignored.
 * @param onClick The clickable action for whole layout. If any area (outside of other added
 *   tappable components) is clicked, it will fire the associated action.
 * @sample androidx.wear.protolayout.material3.samples.topLeveLayout
 */
// TODO: b/356568440 - Add sample above and put it in a proper samples file and link with @sample
// TODO: b/346958146 - Link visuals once they are available.
// TODO: b/353247528 - Handle the icon.
// TODO: b/369162409 -Allow side and bottom margins in PrimaryLayout to be customizable.
// TODO: b/370976767 - Specify that this should be used with MaterialTileService.
public fun MaterialScope.primaryLayout(
    mainSlot: (MaterialScope.() -> LayoutElement),
    titleSlot: (MaterialScope.() -> LayoutElement)? = null,
    bottomSlot: (MaterialScope.() -> LayoutElement)? = null,
    labelForBottomSlot: (MaterialScope.() -> LayoutElement)? = null,
    onClick: Clickable? = null
): LayoutElement =
    primaryLayoutWithOverrideIcon(
        overrideIcon = false,
        titleSlot = titleSlot,
        mainSlot = mainSlot,
        bottomSlot = bottomSlot,
        labelForBottomSlot = labelForBottomSlot,
        onClick = onClick
    )

/**
 * Overrides the icon slot by showing colors circle. For the rest, see [primaryLayout]. This should
 * only be used for testing or building internal samples to validate the UI.
 */
// TODO: b/353247528 - Set as @VisibleForTesting only.
internal fun MaterialScope.primaryLayoutWithOverrideIcon(
    overrideIcon: Boolean,
    titleSlot: (MaterialScope.() -> LayoutElement)? = null,
    mainSlot: (MaterialScope.() -> LayoutElement)? = null,
    bottomSlot: (MaterialScope.() -> LayoutElement)? = null,
    labelForBottomSlot: (MaterialScope.() -> LayoutElement)? = null,
    onClick: Clickable? = null,
): LayoutElement {
    val screenWidth = deviceConfiguration.screenWidthDp
    val screenHeight = deviceConfiguration.screenHeightDp
    val labelSlot: LayoutElement? =
        labelForBottomSlot?.let {
            withStyle(
                    defaultTextElementStyle =
                        TextElementStyle(
                            typography = Typography.TITLE_SMALL,
                            color = getColorProp(ColorTokens.ON_SURFACE)
                        )
                )
                .labelForBottomSlot()
        }

    val modifiers =
        Modifiers.Builder()
            .setMetadata(ElementMetadata.Builder().setTagData(METADATA_TAG.toTagBytes()).build())

    onClick?.apply { modifiers.setClickable(this) }

    val mainSlotSideMargin: DpProp =
        dp(
            screenWidth *
                if (bottomSlot != null)
                    (if (titleSlot != null)
                        MAIN_SLOT_WITH_BOTTOM_SLOT_WITH_TITLE_MARGIN_SIDE_PERCENTAGE
                    else MAIN_SLOT_WITH_BOTTOM_SLOT_WITHOUT_TITLE_MARGIN_SIDE_PERCENTAGE)
                else
                    (if (titleSlot != null)
                        MAIN_SLOT_WITHOUT_BOTTOM_SLOT_WITH_TITLE_MARGIN_SIDE_PERCENTAGE
                    else MAIN_SLOT_WITHOUT_BOTTOM_SLOT_WITHOUT_TITLE_MARGIN_SIDE_PERCENTAGE)
        )

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
                                        color = getColorProp(ColorTokens.ON_BACKGROUND)
                                    )
                            )
                            .titleSlot()
                    },
                    overrideIcon
                )
            )
    // Contains main content. This Box is needed to set to expand, even if empty so it
    // fills the empty space until bottom content.

    mainSlot?.let { mainLayout.addContent(mainSlot().getMainContentBox(mainSlotSideMargin)) }

    bottomSlot?.let {
        // Contains bottom slot, optional label or needed padding if empty.
        mainLayout.addContent(getFooterContent(bottomSlot(), labelSlot))
    }

    return mainLayout.build()
}

private fun MaterialScope.getIconPlaceholder(overrideIcon: Boolean): LayoutElement {
    val iconSlot =
        Box.Builder().setWidth(HEADER_ICON_SIZE_DP.toDp()).setHeight(HEADER_ICON_SIZE_DP.toDp())
    if (overrideIcon) {
        iconSlot.setModifiers(
            Modifiers.Builder()
                .setBackground(
                    ModifiersBuilders.Background.Builder()
                        .setCorner(getCorner(Shape.CORNER_FULL))
                        .setColor(getColorProp(ColorTokens.ON_BACKGROUND))
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
            .addContent(horizontalSpacer(HEADER_ICON_TITLE_SPACER_HEIGHT_DP))
            .addContent(titleSlot)
    }

    return headerBuilder.build()
}

/** Returns central slot with the optional main content. It expands to fill the available space. */
private fun LayoutElement.getMainContentBox(sideMargin: DpProp): Box =
    Box.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setModifiers(
            Modifiers.Builder()
                .setPadding(
                    Padding.Builder() // Top and bottom space has been added to other elements.
                        .setStart(sideMargin)
                        .setEnd(sideMargin)
                        .build()
                )
                .build()
        )
        .addContent(this)
        .build()

/**
 * Returns the footer content, containing bottom slot and optional label with the corresponding
 * spacing and margins depending on what is that content, or Box with padding if there's no bottom
 * slot.
 */
private fun MaterialScope.getFooterContent(
    bottomSlot: LayoutElement?,
    labelSlot: LayoutElement?
): LayoutElement {
    val footer = Box.Builder().setWidth(wrap()).setHeight(wrap())

    if (bottomSlot == null) {
        footer.setWidth(expand())
        footer.setHeight(
            dp(BOTTOM_SLOT_EMPTY_MARGIN_BOTTOM_PERCENTAGE * deviceConfiguration.screenHeightDp)
        )
    } else if (bottomSlot.isSlotEdgeButton()) {
        // Label shouldn't be used with EdgeButton.
        footer.setModifiers(
            Modifiers.Builder()
                .setPadding(
                    Padding.Builder().setTop(BOTTOM_EDGE_BUTTON_TOP_MARGIN_DP.toDp()).build()
                )
                .build()
        )

        footer.addContent(bottomSlot)
    } else {
        val otherBottomSlot = Column.Builder().setWidth(wrap()).setHeight(wrap())

        footer.setModifiers(
            Modifiers.Builder()
                .setPadding(
                    Padding.Builder()
                        .setTop(
                            dp(
                                (if (labelSlot == null)
                                    BOTTOM_SLOT_OTHER_NO_LABEL_MARGIN_TOP_PERCENTAGE
                                else BOTTOM_SLOT_OTHER_WITH_LABEL_MARGIN_TOP_PERCENTAGE) *
                                    deviceConfiguration.screenHeightDp
                            )
                        )
                        .setBottom(
                            dp(
                                (if (labelSlot == null)
                                    BOTTOM_SLOT_OTHER_NO_LABEL_MARGIN_BOTTOM_PERCENTAGE
                                else BOTTOM_SLOT_OTHER_WITH_LABEL_MARGIN_BOTTOM_PERCENTAGE) *
                                    deviceConfiguration.screenHeightDp
                            )
                        )
                        .build()
                )
                .build()
        )

        labelSlot?.apply {
            otherBottomSlot
                .addContent(
                    generateLabelContent(
                        dp(
                            FOOTER_LABEL_SLOT_MARGIN_SIDE_PERCENTAGE *
                                deviceConfiguration.screenWidthDp
                        )
                    )
                )
                .addContent(horizontalSpacer(FOOTER_LABEL_TO_BOTTOM_SLOT_SPACER_HEIGHT_DP))
        }

        footer.addContent(
            otherBottomSlot
                .addContent(
                    bottomSlot.generateBottomSlotContent(
                        dp(
                            BOTTOM_SLOT_OTHER_MARGIN_SIDE_PERCENTAGE *
                                deviceConfiguration.screenWidthDp
                        )
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

private fun MaterialScope.getMarginForHeader(): Padding {
    return Padding.Builder()
        .setTop(HEADER_MARGIN_TOP_DP.toDp())
        .setBottom(HEADER_MARGIN_BOTTOM_DP.toDp())
        .setStart(dp(HEADER_MARGIN_SIDE_PERCENTAGE * deviceConfiguration.screenWidthDp))
        .setEnd(dp(HEADER_MARGIN_SIDE_PERCENTAGE * deviceConfiguration.screenWidthDp))
        .build()
}

/** Contains the default values used by Material layout. */
internal object PrimaryLayoutDefaults {
    /** Tool tag for Metadata in Modifiers, so we know that Row is actually a PrimaryLayout. */
    @VisibleForTesting const val METADATA_TAG: String = "PL"

    @Dimension(unit = DP) const val HEADER_MARGIN_TOP_DP: Int = 3

    @Dimension(unit = DP) const val HEADER_MARGIN_BOTTOM_DP: Int = 6

    const val HEADER_MARGIN_SIDE_PERCENTAGE: Float = 14.5f / 100

    @Dimension(unit = DP) const val HEADER_ICON_SIZE_DP: Int = 24

    @Dimension(unit = DP) const val HEADER_ICON_TITLE_SPACER_HEIGHT_DP: Int = 2

    // The remaining margins around EdgeButton are within the component itself.
    @Dimension(unit = DP) const val BOTTOM_EDGE_BUTTON_TOP_MARGIN_DP: Int = 4

    const val BOTTOM_SLOT_OTHER_NO_LABEL_MARGIN_TOP_PERCENTAGE: Float = 4f / 100
    const val BOTTOM_SLOT_OTHER_NO_LABEL_MARGIN_BOTTOM_PERCENTAGE: Float = 8.3f / 100

    const val BOTTOM_SLOT_OTHER_WITH_LABEL_MARGIN_TOP_PERCENTAGE: Float = 3f / 100
    const val BOTTOM_SLOT_OTHER_WITH_LABEL_MARGIN_BOTTOM_PERCENTAGE: Float = 5f / 100
    const val BOTTOM_SLOT_OTHER_MARGIN_SIDE_PERCENTAGE: Float = 26f / 100

    @Dimension(unit = DP) const val FOOTER_LABEL_TO_BOTTOM_SLOT_SPACER_HEIGHT_DP: Int = 2

    const val FOOTER_LABEL_SLOT_MARGIN_SIDE_PERCENTAGE: Float = 16.64f / 100

    const val BOTTOM_SLOT_EMPTY_MARGIN_BOTTOM_PERCENTAGE: Float = 14f / 100
    const val MAIN_SLOT_WITH_BOTTOM_SLOT_WITH_TITLE_MARGIN_SIDE_PERCENTAGE: Float = 3f / 100
    const val MAIN_SLOT_WITH_BOTTOM_SLOT_WITHOUT_TITLE_MARGIN_SIDE_PERCENTAGE: Float = 6f / 100
    const val MAIN_SLOT_WITHOUT_BOTTOM_SLOT_WITH_TITLE_MARGIN_SIDE_PERCENTAGE: Float = 7.3f / 100
    const val MAIN_SLOT_WITHOUT_BOTTOM_SLOT_WITHOUT_TITLE_MARGIN_SIDE_PERCENTAGE: Float = 8.3f / 100
}
