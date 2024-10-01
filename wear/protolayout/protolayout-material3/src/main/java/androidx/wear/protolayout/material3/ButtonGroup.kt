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
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.material3.ButtonGroupDefaults.DEFAULT_SPACER_SIZE_DP
import androidx.wear.protolayout.material3.ButtonGroupDefaults.METADATA_TAG

// TODO: b/356568440 - Put the above sample in proper samples file and link with @sample
/**
 * ProtoLayout Material3 component-layout that places its children in a horizontal sequence.
 *
 * These behave as a group which fill the available space to maximize on screen real estate. The
 * color and size of the child elements should be changed in order to create hierarchy and priority.
 *
 * The width and height of the Button/Card should be flexible (set to
 * [androidx.wear.protolayout.DimensionBuilders.expand] or
 * [androidx.wear.protolayout.DimensionBuilders.weight]) and their proportion should be either equal
 * or weight based. However the [buttonGroup] displays correctly too if the sizes of elements
 * provided are set to a fixed width/height, although it can lead to more empty space on large
 * screen sizes.
 *
 * A [buttonGroup] with more than one row can be created by using multiple [buttonGroup] and
 * [Spacer]s inside a [androidx.wear.protolayout.LayoutElementBuilders.Column]:
 * ```
 * Column.Builder()
 *   .setWidth(expand())
 *   .setHeight(expand())
 *   .addContent(buttonGroup {...})
 *   .addContent(DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS)
 *   .addContent(buttonGroup {...})
 *   .build()
 * }
 * ```
 *
 * Note that, having more than 2 rows in a Column could lead to too small height of elements that
 * aren't in line with minimum tap target.
 *
 * @param width The width of this button group
 * @param height The height of this button group
 * @param spacing The amount of spacing between buttons
 * @param content The content for each child. The UX guidance is to use no more than 3 elements
 *   within a this button group.
 */
// TODO: b/346958146 - Link visuals once they are available.
public fun MaterialScope.buttonGroup(
    width: ContainerDimension = expand(),
    height: ContainerDimension = expand(),
    @Dimension(unit = DP) spacing: Float = DEFAULT_SPACER_SIZE_DP,
    content: ButtonGroupScope.() -> Unit
): LayoutElement =
    Row.Builder()
        .setWidth(width)
        .setHeight(height)
        .setModifiers(
            Modifiers.Builder()
                .setMetadata(
                    ElementMetadata.Builder().setTagData(METADATA_TAG.toTagBytes()).build()
                )
                .build()
        )
        .also { row ->
            // List of children
            ButtonGroupScope(this)
                .apply { content() }
                .items
                .addBetween(Spacer.Builder().setWidth(dp(spacing)).setHeight(expand()).build())
                .forEach(row::addContent)
        }
        .build()

/** Scope for the children of a [buttonGroup] */
@MaterialScopeMarker
public class ButtonGroupScope internal constructor(private val scope: MaterialScope) {
    internal val items = mutableListOf<LayoutElement>()

    /**
     * Adds an item to a [buttonGroup]
     *
     * @param content the content to use for this item. Usually, this will be one of the Material 3
     *   button or card variants.
     */
    // TODO: b/360110062: Add link to Button and Card.
    public fun buttonGroupItem(content: MaterialScope.() -> LayoutElement) {
        items += scope.content()
    }
}

/** Contains the default values used by [buttonGroup] */
public object ButtonGroupDefaults {
    /**
     * Default size of the space between elements in one [buttonGroup] or between multiple
     * [buttonGroup]s.
     */
    @Dimension(unit = DP) internal const val DEFAULT_SPACER_SIZE_DP: Float = 4f

    /** Default width of the space between elements in this [buttonGroup]. */
    @Dimension(unit = DP) public const val DEFAULT_SPACER_WIDTH_DP: Float = DEFAULT_SPACER_SIZE_DP

    /**
     * This [Spacer] should be used in case when there are multiple [buttonGroup] stacked on top of
     * each other.
     */
    @JvmField
    public val DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS: Spacer =
        Spacer.Builder().setWidth(expand()).setHeight(dp(DEFAULT_SPACER_SIZE_DP)).build()

    /** Tool tag for Metadata in Modifiers, so we know that Row is actually a ButtonGroup. */
    internal const val METADATA_TAG: String = "BGRP"
}
