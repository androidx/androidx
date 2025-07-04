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

package androidx.compose.material3

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.rememberAnimatedShape
import androidx.compose.material3.tokens.ButtonSmallTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.SplitButtonLargeTokens
import androidx.compose.material3.tokens.SplitButtonMediumTokens
import androidx.compose.material3.tokens.SplitButtonSmallTokens
import androidx.compose.material3.tokens.SplitButtonXLargeTokens
import androidx.compose.material3.tokens.SplitButtonXSmallTokens
import androidx.compose.material3.tokens.StateTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.util.fastSumBy

/**
 * A [SplitButtonLayout] let user define a button group consisting of 2 buttons. The leading button
 * performs a primary action, and the trailing button performs a secondary action that is
 * contextually related to the primary action.
 *
 * @sample androidx.compose.material3.samples.FilledSplitButtonSample
 * @sample androidx.compose.material3.samples.SplitButtonWithDropdownMenuSample
 * @sample androidx.compose.material3.samples.TonalSplitButtonSample
 * @sample androidx.compose.material3.samples.ElevatedSplitButtonSample
 * @sample androidx.compose.material3.samples.OutlinedSplitButtonSample
 * @sample androidx.compose.material3.samples.SplitButtonWithUnCheckableTrailingButtonSample
 * @sample androidx.compose.material3.samples.SplitButtonWithTextSample
 * @sample androidx.compose.material3.samples.SplitButtonWithIconSample
 * @sample androidx.compose.material3.samples.XSmallFilledSplitButtonSample
 * @sample androidx.compose.material3.samples.MediumFilledSplitButtonSample
 * @sample androidx.compose.material3.samples.LargeFilledSplitButtonSample
 * @sample androidx.compose.material3.samples.ExtraLargeFilledSplitButtonSample
 *
 * Choose the best split button for an action based on the amount of emphasis it needs. The more
 * important an action is, the higher emphasis its button should be.
 *
 * Use [SplitButtonDefaults.LeadingButton] and [SplitButtonDefaults.TrailingButton] to construct a
 * `FilledSplitButton`. Filled split button is the high-emphasis version of split button. It should
 * be used for emphasizing important or final actions.
 *
 * Use [SplitButtonDefaults.TonalLeadingButton] and [SplitButtonDefaults.TonalTrailingButton] to
 * construct a `tonal SplitButton`. Tonal split button is the medium-emphasis version of split
 * buttons. It's a middle ground between `filled SplitButton` and `outlined SplitButton`
 *
 * Use [SplitButtonDefaults.ElevatedLeadingButton] and [SplitButtonDefaults.ElevatedTrailingButton]
 * to construct a `elevated SplitButton`. Elevated split buttons are essentially `tonal
 * SplitButton`s with a shadow. To prevent shadow creep, only use them when absolutely necessary,
 * such as when the button requires visual separation from patterned container.
 *
 * Use [SplitButtonDefaults.OutlinedLeadingButton] and [SplitButtonDefaults.OutlinedTrailingButton]
 * to construct a `outlined SplitButton`. Outlined split buttons are medium-emphasis buttons. They
 * contain actions that are important, but are not the primary action in an app. Outlined buttons
 * pair well with `filled SplitButton`s to indicate an alternative, secondary action.
 *
 * @param leadingButton the leading button. You can specify your own composable or construct a
 *   [SplitButtonDefaults.LeadingButton]
 * @param trailingButton the trailing button.You can specify your own composable or construct a
 *   [SplitButtonDefaults.TrailingButton]
 * @param modifier the [Modifier] to be applied to this split button.
 * @param spacing The spacing between the [leadingButton] and [trailingButton]
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SplitButtonLayout(
    leadingButton: @Composable () -> Unit,
    trailingButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = SplitButtonDefaults.Spacing,
) {
    Layout(
        {
            // Override min component size enforcement to avoid create extra padding internally
            // Enforce it on the parent instead
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Box(
                    modifier = Modifier.layoutId(LeadingButtonLayoutId),
                    contentAlignment = Alignment.Center,
                    content = { leadingButton() },
                )
                Box(
                    modifier = Modifier.layoutId(TrailingButtonLayoutId),
                    contentAlignment = Alignment.Center,
                    content = { trailingButton() },
                )
            }
        },
        modifier.minimumInteractiveComponentSize(),
        measurePolicy = { measurables, constraints ->
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

            val leadingButtonPlaceable =
                measurables
                    .fastFirst { it.layoutId == LeadingButtonLayoutId }
                    .measure(looseConstraints)

            val trailingButtonPlaceable =
                measurables
                    .fastFirst { it.layoutId == TrailingButtonLayoutId }
                    .measure(
                        looseConstraints
                            .offset(
                                horizontal = -(leadingButtonPlaceable.width + spacing.roundToPx())
                            )
                            .copy(
                                minHeight = leadingButtonPlaceable.height,
                                maxHeight = leadingButtonPlaceable.height,
                            )
                    )

            val placeables = listOf(leadingButtonPlaceable, trailingButtonPlaceable)

            val contentWidth = placeables.fastSumBy { it.width } + spacing.roundToPx()
            val contentHeight = placeables.fastMaxOfOrNull { it.height } ?: 0

            val width = constraints.constrainWidth(contentWidth)
            val height = constraints.constrainHeight(contentHeight)

            layout(width, height) {
                leadingButtonPlaceable.placeRelative(0, 0)
                trailingButtonPlaceable.placeRelative(
                    x = leadingButtonPlaceable.width + spacing.roundToPx(),
                    y = 0,
                )
            }
        },
    )
}

/** Contains default values used by [SplitButtonLayout] and its style variants. */
@ExperimentalMaterial3ExpressiveApi
object SplitButtonDefaults {
    /** Default icon size for the leading button */
    val LeadingIconSize = ButtonSmallTokens.IconSize

    /** Default icon size for the trailing button */
    val TrailingIconSize = SplitButtonSmallTokens.TrailingIconSize

    /** Default spacing between the `leading` and `trailing` button */
    val Spacing = SplitButtonSmallTokens.BetweenSpace

    /** Default size for the leading button end corners and trailing button start corners */
    @Deprecated(
        message = "Use `SmallInnerCornerSize` instead",
        replaceWith = ReplaceWith("SmallInnerCornerSize"),
        level = DeprecationLevel.HIDDEN,
    )
    val InnerCornerSize = SplitButtonSmallTokens.InnerCornerCornerSize

    /**
     * Default extra small size for the leading button end corners and trailing button start corners
     */
    val ExtraSmallInnerCornerSize = SplitButtonXSmallTokens.InnerCornerCornerSize

    /** Default small size for the leading button end corners and trailing button start corners */
    val SmallInnerCornerSize = SplitButtonSmallTokens.InnerCornerCornerSize

    /** Default medium size for the leading button end corners and trailing button start corners */
    val MediumInnerCornerSize = SplitButtonMediumTokens.InnerCornerCornerSize

    /** Default large size for the leading button end corners and trailing button start corners */
    val LargeInnerCornerSize = SplitButtonLargeTokens.InnerCornerCornerSize

    /**
     * Default extra large size for the leading button end corners and trailing button start corners
     */
    val ExtraLargeInnerCornerSize = SplitButtonXLargeTokens.InnerCornerCornerSize

    /**
     * Default extra small size for the leading button end corners and trailing button start corners
     * when pressed
     */
    val ExtraSmallInnerCornerSizePressed = SplitButtonXSmallTokens.InnerPressedCornerCornerSize

    /**
     * Default small size for the leading button end corners and trailing button start corners when
     * pressed
     */
    val SmallInnerCornerSizePressed = SplitButtonSmallTokens.InnerPressedCornerCornerSize

    /**
     * Default medium size for the leading button end corners and trailing button start corners when
     * pressed
     */
    val MediumInnerCornerSizePressed = SplitButtonMediumTokens.InnerPressedCornerCornerSize

    /**
     * Default large size for the leading button end corners and trailing button start corners when
     * pressed
     */
    val LargeInnerCornerSizePressed = SplitButtonLargeTokens.InnerPressedCornerCornerSize

    /**
     * Default extra large size for the leading button end corners and trailing button start corners
     * when pressed
     */
    val ExtraLargeInnerCornerSizePressed = SplitButtonXLargeTokens.InnerPressedCornerCornerSize

    /**
     * Default percentage size for the leading button start corners and trailing button end corners
     */
    val OuterCornerSize = ShapeDefaults.CornerFull

    /** Default content padding of the leading button */
    @Deprecated(
        message = "Use `leadingButtonContentPaddingFor(buttonHeight)` instead",
        replaceWith = ReplaceWith("leadingButtonContentPaddingFor(buttonHeight)"),
        level = DeprecationLevel.HIDDEN,
    )
    val LeadingButtonContentPadding =
        PaddingValues(
            start = SplitButtonSmallTokens.LeadingButtonLeadingSpace,
            end = SplitButtonSmallTokens.LeadingButtonTrailingSpace,
        )

    /** Default content padding of the extra small leading button */
    @ExperimentalMaterial3ExpressiveApi
    val ExtraSmallLeadingButtonContentPadding =
        PaddingValues(
            start = SplitButtonXSmallTokens.LeadingButtonLeadingSpace,
            end = SplitButtonXSmallTokens.LeadingButtonTrailingSpace,
        )

    /** Default content padding of the small leading button */
    @ExperimentalMaterial3ExpressiveApi
    val SmallLeadingButtonContentPadding =
        PaddingValues(
            start = SplitButtonSmallTokens.LeadingButtonLeadingSpace,
            end = SplitButtonSmallTokens.LeadingButtonTrailingSpace,
        )

    /** Default content padding of the medium leading button */
    @ExperimentalMaterial3ExpressiveApi
    val MediumLeadingButtonContentPadding =
        PaddingValues(
            start = SplitButtonMediumTokens.LeadingButtonLeadingSpace,
            end = SplitButtonMediumTokens.LeadingButtonTrailingSpace,
        )

    /** Default content padding of the large leading button */
    @ExperimentalMaterial3ExpressiveApi
    val LargeLeadingButtonContentPadding =
        PaddingValues(
            start = SplitButtonLargeTokens.LeadingButtonLeadingSpace,
            end = SplitButtonLargeTokens.LeadingButtonTrailingSpace,
        )

    /** Default content padding of the extra large leading button */
    @ExperimentalMaterial3ExpressiveApi
    val ExtraLargeLeadingButtonContentPadding =
        PaddingValues(
            start = SplitButtonXLargeTokens.LeadingButtonLeadingSpace,
            end = SplitButtonXLargeTokens.LeadingButtonTrailingSpace,
        )

    /** Default content padding of the extra small trailing button */
    @ExperimentalMaterial3ExpressiveApi
    val ExtraSmallTrailingButtonContentPadding =
        PaddingValues(
            start = SplitButtonXSmallTokens.TrailingButtonLeadingSpace,
            end = SplitButtonXSmallTokens.TrailingButtonTrailingSpace,
        )

    /** Default content padding of the small trailing button */
    @ExperimentalMaterial3ExpressiveApi
    val SmallTrailingButtonContentPadding =
        PaddingValues(
            start = SplitButtonSmallTokens.TrailingButtonLeadingSpace,
            end = SplitButtonSmallTokens.TrailingButtonTrailingSpace,
        )

    /** Default content padding of the medium trailing button */
    @ExperimentalMaterial3ExpressiveApi
    val MediumTrailingButtonContentPadding =
        PaddingValues(
            start = SplitButtonMediumTokens.TrailingButtonLeadingSpace,
            end = SplitButtonMediumTokens.TrailingButtonTrailingSpace,
        )

    /** Default content padding of the large trailing button */
    @ExperimentalMaterial3ExpressiveApi
    val LargeTrailingButtonContentPadding =
        PaddingValues(
            start = SplitButtonLargeTokens.TrailingButtonLeadingSpace,
            end = SplitButtonLargeTokens.TrailingButtonTrailingSpace,
        )

    /** Default content padding of the extra large trailing button */
    @ExperimentalMaterial3ExpressiveApi
    val ExtraLargeTrailingButtonContentPadding =
        PaddingValues(
            start = SplitButtonXLargeTokens.TrailingButtonLeadingSpace,
            end = SplitButtonXLargeTokens.TrailingButtonTrailingSpace,
        )

    /**
     * The default size of the icon used inside of an extra small trailing button of a split button.
     */
    val ExtraSmallTrailingButtonIconSize = SplitButtonXSmallTokens.TrailingIconSize

    /** The default size of the icon used inside of an small trailing button of a split button. */
    val SmallTrailingButtonIconSize = SplitButtonSmallTokens.TrailingIconSize

    /** The default size of the icon used inside of an medium trailing button of a split button. */
    val MediumTrailingButtonIconSize = SplitButtonMediumTokens.TrailingIconSize

    /** The default size of the icon used inside of an large trailing button of a split button. */
    val LargeTrailingButtonIconSize = SplitButtonLargeTokens.TrailingIconSize

    /**
     * The default size of the icon used inside of an extra large trailing button of a split button.
     */
    val ExtraLargeTrailingButtonIconSize = SplitButtonXLargeTokens.TrailingIconSize

    /** Default content padding of the small trailing button */
    @Deprecated(
        message = "Use `SmallTrailingButtonContentPadding` instead",
        replaceWith = ReplaceWith("SmallTrailingButtonContentPadding"),
        level = DeprecationLevel.HIDDEN,
    )
    val TrailingButtonContentPadding =
        PaddingValues(
            start = SplitButtonSmallTokens.TrailingButtonLeadingSpace,
            end = SplitButtonSmallTokens.TrailingButtonTrailingSpace,
        )

    /**
     * Default minimum width of the [LeadingButton], applies to all 4 variants of the split button
     */
    private val LeadingButtonMinWidth = 48.dp

    /**
     * Default minimum height of the split button. This applies to both [LeadingButton] and
     * [TrailingButton]. Applies to all 4 variants of the split button
     */
    val SmallContainerHeight = SplitButtonSmallTokens.ContainerHeight

    /** The default height for an extra small button container. */
    @ExperimentalMaterial3ExpressiveApi
    val ExtraSmallContainerHeight = SplitButtonXSmallTokens.ContainerHeight

    /** The default height for a medium button container. */
    @ExperimentalMaterial3ExpressiveApi
    val MediumContainerHeight = SplitButtonMediumTokens.ContainerHeight

    /** The default height for a large button container. */
    @ExperimentalMaterial3ExpressiveApi
    val LargeContainerHeight = SplitButtonLargeTokens.ContainerHeight

    /** The default height for an extra large button container. */
    @ExperimentalMaterial3ExpressiveApi
    val ExtraLargeContainerHeight = SplitButtonXLargeTokens.ContainerHeight

    /** Default minimum width of the [TrailingButton]. */
    private val TrailingButtonMinWidth = LeadingButtonMinWidth

    /** Trailing button state layer alpha when in checked state */
    private const val TrailingButtonStateLayerAlpha = StateTokens.PressedStateLayerOpacity

    /** Default shape of the leading button. */
    private fun leadingButtonShape(endCornerSize: CornerSize = SmallInnerCornerSize) =
        RoundedCornerShape(OuterCornerSize, endCornerSize, endCornerSize, OuterCornerSize)

    private val TrailingCheckedShape = CircleShape

    /**
     * Default shapes for the leading button. This defines the shapes the leading button should
     * morph to when enabled, pressed etc.
     *
     * @param endCornerSize the size for top end corner and bottom end corner
     */
    @Deprecated(
        message = "This function will be private in the future",
        replaceWith = ReplaceWith("leadingButtonShapesFor(buttonHeight)"),
        level = DeprecationLevel.HIDDEN,
    )
    fun leadingButtonShapes(endCornerSize: CornerSize = SmallInnerCornerSize) =
        SplitButtonShapes(
            shape = leadingButtonShape(endCornerSize),
            pressedShape =
                RoundedCornerShape(
                    topStart = OuterCornerSize,
                    bottomStart = OuterCornerSize,
                    topEnd = SmallInnerCornerSizePressed,
                    bottomEnd = SmallInnerCornerSizePressed,
                ),
            checkedShape = null,
        )

    private fun leadingButtonShapesInternal(
        endCornerSize: CornerSize = SmallInnerCornerSize,
        endCornerSizePressed: CornerSize = SmallInnerCornerSizePressed,
    ) =
        SplitButtonShapes(
            shape = leadingButtonShape(endCornerSize),
            pressedShape = leadingButtonShape(endCornerSizePressed),
            checkedShape = null,
        )

    /** Default shape of the trailing button */
    private fun trailingButtonShape(startCornerSize: CornerSize = SmallInnerCornerSize) =
        RoundedCornerShape(startCornerSize, OuterCornerSize, OuterCornerSize, startCornerSize)

    /**
     * Default shapes for the trailing button
     *
     * @param startCornerSize the size for top start corner and bottom start corner
     */
    @Deprecated(
        message = "This function will be private in the future",
        replaceWith = ReplaceWith("trailingButtonShapesFor(buttonHeight)"),
        level = DeprecationLevel.HIDDEN,
    )
    fun trailingButtonShapes(startCornerSize: CornerSize = SmallInnerCornerSize) =
        SplitButtonShapes(
            shape = trailingButtonShape(startCornerSize),
            pressedShape =
                RoundedCornerShape(
                    topStart = SmallInnerCornerSizePressed,
                    bottomStart = SmallInnerCornerSizePressed,
                    topEnd = OuterCornerSize,
                    bottomEnd = OuterCornerSize,
                ),
            checkedShape = TrailingCheckedShape,
        )

    private fun trailingButtonShapesInternal(
        startCornerSize: CornerSize = SmallInnerCornerSize,
        startCornerSizePressed: CornerSize = SmallInnerCornerSizePressed,
    ) =
        SplitButtonShapes(
            shape = trailingButtonShape(startCornerSize),
            pressedShape = trailingButtonShape(startCornerSizePressed),
            checkedShape = TrailingCheckedShape,
        )

    /**
     * Recommended leading button [SplitButtonShapes] for a provided leading button height.
     *
     * @param buttonHeight The height of the button
     */
    @Composable
    @ExperimentalMaterial3ExpressiveApi
    fun leadingButtonShapesFor(buttonHeight: Dp): SplitButtonShapes {
        val xSmallHeight = ButtonDefaults.ExtraSmallContainerHeight
        val smallHeight = ButtonDefaults.MinHeight
        val mediumHeight = ButtonDefaults.MediumContainerHeight
        val largeHeight = ButtonDefaults.LargeContainerHeight
        val xLargeHeight = ButtonDefaults.ExtraLargeContainerHeight
        return when {
            buttonHeight <= (xSmallHeight + smallHeight) / 2 ->
                leadingButtonShapesInternal(
                    endCornerSize = ExtraSmallInnerCornerSize,
                    endCornerSizePressed = ExtraSmallInnerCornerSizePressed,
                )
            buttonHeight <= (smallHeight + mediumHeight) / 2 ->
                leadingButtonShapesInternal(
                    endCornerSize = SmallInnerCornerSize,
                    endCornerSizePressed = SmallInnerCornerSizePressed,
                )
            buttonHeight <= (mediumHeight + largeHeight) / 2 ->
                leadingButtonShapesInternal(
                    endCornerSize = MediumInnerCornerSize,
                    endCornerSizePressed = MediumInnerCornerSizePressed,
                )
            buttonHeight <= (largeHeight + xLargeHeight) / 2 ->
                leadingButtonShapesInternal(
                    endCornerSize = LargeInnerCornerSize,
                    endCornerSizePressed = LargeInnerCornerSizePressed,
                )
            else ->
                leadingButtonShapesInternal(
                    endCornerSize = ExtraLargeInnerCornerSize,
                    endCornerSizePressed = ExtraLargeInnerCornerSizePressed,
                )
        }
    }

    /**
     * Recommended trailing button [SplitButtonShapes] for a provided trailing button height.
     *
     * @param buttonHeight The height of the button
     */
    @Composable
    @ExperimentalMaterial3ExpressiveApi
    fun trailingButtonShapesFor(buttonHeight: Dp): SplitButtonShapes {
        val xSmallHeight = ButtonDefaults.ExtraSmallContainerHeight
        val smallHeight = ButtonDefaults.MinHeight
        val mediumHeight = ButtonDefaults.MediumContainerHeight
        val largeHeight = ButtonDefaults.LargeContainerHeight
        val xLargeHeight = ButtonDefaults.ExtraLargeContainerHeight
        return when {
            buttonHeight <= (xSmallHeight + smallHeight) / 2 ->
                trailingButtonShapesInternal(
                    startCornerSize = ExtraSmallInnerCornerSize,
                    startCornerSizePressed = ExtraSmallInnerCornerSizePressed,
                )
            buttonHeight <= (smallHeight + mediumHeight) / 2 ->
                trailingButtonShapesInternal(
                    startCornerSize = SmallInnerCornerSize,
                    startCornerSizePressed = SmallInnerCornerSizePressed,
                )
            buttonHeight <= (mediumHeight + largeHeight) / 2 ->
                trailingButtonShapesInternal(
                    startCornerSize = MediumInnerCornerSize,
                    startCornerSizePressed = MediumInnerCornerSizePressed,
                )
            buttonHeight <= (largeHeight + xLargeHeight) / 2 ->
                trailingButtonShapesInternal(
                    startCornerSize = LargeInnerCornerSize,
                    startCornerSizePressed = LargeInnerCornerSizePressed,
                )
            else ->
                trailingButtonShapesInternal(
                    startCornerSize = ExtraLargeInnerCornerSize,
                    startCornerSizePressed = ExtraLargeInnerCornerSizePressed,
                )
        }
    }

    /**
     * Recommended [PaddingValues] for a provided leading button height.
     *
     * @param buttonHeight The height of the button
     */
    fun leadingButtonContentPaddingFor(buttonHeight: Dp): PaddingValues {
        val smallHeight = SmallContainerHeight
        val mediumHeight = MediumContainerHeight
        val largeHeight = LargeContainerHeight
        val xLargeHeight = ExtraLargeContainerHeight
        return when {
            buttonHeight < smallHeight -> ExtraSmallLeadingButtonContentPadding
            buttonHeight < mediumHeight -> SmallLeadingButtonContentPadding
            buttonHeight < largeHeight -> MediumLeadingButtonContentPadding
            buttonHeight < xLargeHeight -> LargeLeadingButtonContentPadding
            else -> ExtraLargeLeadingButtonContentPadding
        }
    }

    /**
     * Recommended [PaddingValues] for a provided trailing button height.
     *
     * @param buttonHeight The height of the button
     */
    fun trailingButtonContentPaddingFor(buttonHeight: Dp): PaddingValues {
        val smallHeight = SmallContainerHeight
        val mediumHeight = MediumContainerHeight
        val largeHeight = LargeContainerHeight
        val xLargeHeight = ExtraLargeContainerHeight
        return when {
            buttonHeight < smallHeight -> ExtraSmallTrailingButtonContentPadding
            buttonHeight < mediumHeight -> SmallTrailingButtonContentPadding
            buttonHeight < largeHeight -> MediumTrailingButtonContentPadding
            buttonHeight < xLargeHeight -> LargeTrailingButtonContentPadding
            else -> ExtraLargeTrailingButtonContentPadding
        }
    }

    /**
     * Recommended leading button Icon size for a provided button height.
     *
     * @param buttonHeight The height of the button
     */
    fun leadingButtonIconSizeFor(buttonHeight: Dp): Dp {
        return ButtonDefaults.iconSizeFor(buttonHeight)
    }

    /**
     * Recommended trailing button Icon size for a provided button height.
     *
     * @param buttonHeight The height of the button
     */
    fun trailingButtonIconSizeFor(buttonHeight: Dp): Dp {
        val smallHeight = ButtonDefaults.MinHeight
        val mediumHeight = ButtonDefaults.MediumContainerHeight
        val largeHeight = ButtonDefaults.LargeContainerHeight
        val xLargeHeight = ButtonDefaults.ExtraLargeContainerHeight
        return when {
            buttonHeight < smallHeight -> ExtraSmallTrailingButtonIconSize
            buttonHeight < mediumHeight -> SmallTrailingButtonIconSize
            buttonHeight < largeHeight -> MediumTrailingButtonIconSize
            buttonHeight < xLargeHeight -> LargeTrailingButtonIconSize
            else -> ExtraLargeTrailingButtonIconSize
        }
    }

    /**
     * Create a default `leading` button that has the same visual as a Filled[Button]. To create a
     * `tonal`, `outlined`, or `elevated` version, the default value of [Button] params can be
     * passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param onClick called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button contentPadding the
     *   spacing values to apply internally between the container and the content
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content for the button.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun LeadingButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes = leadingButtonShapesFor(SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues = leadingButtonContentPaddingFor(SmallContainerHeight),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ) {
        @Suppress("NAME_SHADOWING")
        val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

        // TODO Load the motionScheme tokens from the component tokens file
        val defaultAnimationSpec = MotionSchemeKeyTokens.DefaultEffects.value<Float>()
        val pressed by interactionSource.collectIsPressedAsState()
        val contentColor = colors.contentColor(enabled)
        val containerColor = colors.containerColor(enabled)

        Surface(
            onClick = onClick,
            modifier = modifier.semantics { role = Role.Button },
            enabled = enabled,
            shape = shapeByInteraction(shapes, pressed, checked = false, defaultAnimationSpec),
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp,
            border = border,
            interactionSource = interactionSource,
        ) {
            ProvideContentColorTextStyle(
                contentColor = contentColor,
                textStyle = MaterialTheme.typography.labelLarge,
            ) {
                Row(
                    Modifier.defaultMinSize(
                            minWidth = LeadingButtonMinWidth,
                            minHeight = SmallContainerHeight,
                        )
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }

    /**
     * Creates a `trailing` button that has the same visual as a [Button].
     *
     * To create a `tonal`, `outlined`, or `elevated` version, the default value of [Button] params
     * can be passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param onClick called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button contentPadding the
     *   spacing values to apply internally between the container and the content
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content to be placed in the button
     */
    @Composable
    @ExperimentalMaterial3ExpressiveApi
    fun TrailingButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes = trailingButtonShapesFor(SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues = trailingButtonContentPaddingFor(SmallContainerHeight),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ) {
        @Suppress("NAME_SHADOWING")
        val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

        // TODO Load the motionScheme tokens from the component tokens file
        val defaultAnimationSpec = MotionSchemeKeyTokens.DefaultEffects.value<Float>()
        val pressed by interactionSource.collectIsPressedAsState()
        val layoutDirection = LocalLayoutDirection.current
        val shape = shapeByInteraction(shapes, pressed, false, defaultAnimationSpec)
        val contentColor = colors.contentColor(enabled)
        val containerColor = colors.containerColor(enabled)

        Surface(
            onClick = onClick,
            modifier = modifier.semantics { role = Role.Button },
            enabled = enabled,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp,
            border = border,
            interactionSource = interactionSource,
        ) {
            ProvideContentColorTextStyle(
                contentColor = contentColor,
                textStyle = MaterialTheme.typography.labelLarge,
            ) {
                Row(
                    Modifier.defaultMinSize(
                            minWidth = TrailingButtonMinWidth,
                            minHeight = SmallContainerHeight,
                        )
                        .then(
                            when (shape) {
                                is ShapeWithHorizontalCenterOptically -> {
                                    Modifier.horizontalCenterOptically(
                                        shape = shape,
                                        maxStartOffset =
                                            contentPadding.calculateStartPadding(layoutDirection),
                                        maxEndOffset =
                                            contentPadding.calculateEndPadding(layoutDirection),
                                    )
                                }
                                is CornerBasedShape -> {
                                    Modifier.horizontalCenterOptically(
                                        shape = shape,
                                        maxStartOffset =
                                            contentPadding.calculateStartPadding(layoutDirection),
                                        maxEndOffset =
                                            contentPadding.calculateEndPadding(layoutDirection),
                                    )
                                }
                                else -> {
                                    Modifier
                                }
                            }
                        )
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }

    /**
     * Creates a `trailing` button that has the same visual as a [Button]. When [checked] is updated
     * from `false` to `true`, the buttons corners will morph to `full` by default. Pressed shape
     * and checked shape can be customized via [shapes] param.
     *
     * To create a `tonal`, `outlined`, or `elevated` version, the default value of [Button] params
     * can be passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param checked indicates whether the button is checked. This will trigger the corner morphing
     *   animation to reflect the updated state.
     * @param onCheckedChange called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button contentPadding the
     *   spacing values to apply internally between the container and the content
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content to be placed in the button
     */
    @Composable
    @ExperimentalMaterial3ExpressiveApi
    fun TrailingButton(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes = trailingButtonShapesFor(SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues = trailingButtonContentPaddingFor(SmallContainerHeight),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ) {
        @Suppress("NAME_SHADOWING")
        val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

        // TODO Load the motionScheme tokens from the component tokens file
        val defaultAnimationSpec = MotionSchemeKeyTokens.DefaultEffects.value<Float>()
        val pressed by interactionSource.collectIsPressedAsState()
        val layoutDirection = LocalLayoutDirection.current
        val density = LocalDensity.current
        val shape = shapeByInteraction(shapes, pressed, checked, defaultAnimationSpec)
        val contentColor = colors.contentColor(enabled)
        val containerColor = colors.containerColor(enabled)

        Surface(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier =
                modifier
                    .drawWithContent {
                        drawContent()
                        if (checked) {
                            drawOutline(
                                outline = shape.createOutline(size, layoutDirection, density),
                                color = contentColor,
                                alpha = TrailingButtonStateLayerAlpha,
                            )
                        }
                    }
                    .semantics { role = Role.Button },
            enabled = enabled,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp,
            border = border,
            interactionSource = interactionSource,
        ) {
            ProvideContentColorTextStyle(
                contentColor = contentColor,
                textStyle = MaterialTheme.typography.labelLarge,
            ) {
                Row(
                    Modifier.defaultMinSize(
                            minWidth = TrailingButtonMinWidth,
                            minHeight = SmallContainerHeight,
                        )
                        .then(
                            when (shape) {
                                is ShapeWithHorizontalCenterOptically -> {
                                    Modifier.horizontalCenterOptically(
                                        shape = shape,
                                        maxStartOffset =
                                            contentPadding.calculateStartPadding(layoutDirection),
                                        maxEndOffset =
                                            contentPadding.calculateEndPadding(layoutDirection),
                                    )
                                }
                                is CornerBasedShape -> {
                                    Modifier.horizontalCenterOptically(
                                        shape = shape,
                                        maxStartOffset =
                                            contentPadding.calculateStartPadding(layoutDirection),
                                        maxEndOffset =
                                            contentPadding.calculateEndPadding(layoutDirection),
                                    )
                                }
                                else -> {
                                    Modifier
                                }
                            }
                        )
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }

    /**
     * Create a tonal `leading` button that has the same visual as a Tonal[Button]. To create a
     * `filled`, `outlined`, or `elevated` version, the default value of [Button] params can be
     * passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param onClick called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button contentPadding the
     *   spacing values to apply internally between the container and the content
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content for the button.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun TonalLeadingButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes = leadingButtonShapesFor(SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
        elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues = leadingButtonContentPaddingFor(SmallContainerHeight),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ) {
        LeadingButton(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }

    /**
     * Creates a tonal `trailing` button that has the same visual as a [FilledTonalButton]. When
     * [checked] is updated from `false` to `true`, the buttons corners will morph to `full` by
     * default. Pressed shape and checked shape can be customized via [shapes] param.
     *
     * To create a `tonal`, `outlined`, or `elevated` version, the default value of [Button] params
     * can be passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param checked indicates whether the button is checked. This will trigger the corner morphing
     *   animation to reflect the updated state.
     * @param onCheckedChange called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button contentPadding the
     *   spacing values to apply internally between the container and the content
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content to be placed in the button
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun TonalTrailingButton(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes = trailingButtonShapesFor(SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
        elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues = trailingButtonContentPaddingFor(SmallContainerHeight),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ) {
        TrailingButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }

    /**
     * Create a elevated `leading` button that has the same visual as a [ElevatedButton]. To create
     * a `filled`, `outlined`, or `elevated` version, the default value of [Button] params can be
     * passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param onClick called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button contentPadding the
     *   spacing values to apply internally between the container and the content
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content for the button.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun OutlinedLeadingButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes = leadingButtonShapesFor(SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
        elevation: ButtonElevation? = null,
        border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
        contentPadding: PaddingValues = leadingButtonContentPaddingFor(SmallContainerHeight),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ) {
        LeadingButton(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }

    /**
     * Creates a outlined `trailing` button that has the same visual as a [OutlinedButton]. When
     * [checked] is updated from `false` to `true`, the buttons corners will morph to `full` by
     * default. Pressed shape and checked shape can be customized via [shapes] param.
     *
     * To create a `tonal`, `outlined`, or `elevated` version, the default value of [Button] params
     * can be passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param checked indicates whether the button is checked. This will trigger the corner morphing
     *   animation to reflect the updated state.
     * @param onCheckedChange called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button contentPadding the
     *   spacing values to apply internally between the container and the content
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content to be placed in the button
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun OutlinedTrailingButton(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes = trailingButtonShapesFor(SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
        elevation: ButtonElevation? = null,
        border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
        contentPadding: PaddingValues = trailingButtonContentPaddingFor(SmallContainerHeight),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ) {
        TrailingButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }

    /**
     * Create a elevated `leading` button that has the same visual as a [ElevatedButton]. To create
     * a `filled`, `outlined`, or `elevated` version, the default value of [Button] params can be
     * passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param onClick called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button contentPadding the
     *   spacing values to apply internally between the container and the content
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content for the button.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun ElevatedLeadingButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes = leadingButtonShapesFor(SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
        elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues = leadingButtonContentPaddingFor(SmallContainerHeight),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ) {
        LeadingButton(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }

    /**
     * Creates a elevated `trailing` button that has the same visual as a [ElevatedButton]. When
     * [checked] is updated from `false` to `true`, the buttons corners will morph to `full` by
     * default. Pressed shape and checked shape can be customized via [shapes] param.
     *
     * To create a `tonal`, `outlined`, or `elevated` version, the default value of [Button] params
     * can be passed in. For example, [ElevatedButton].
     *
     * The default text style for internal [Text] components will be set to [Typography.labelLarge].
     *
     * @param checked indicates whether the button is checked. This will trigger the corner morphing
     *   animation to reflect the updated state.
     * @param onCheckedChange called when the button is clicked
     * @param modifier the [Modifier] to be applied to this button.
     * @param enabled controls the enabled state of the split button. When `false`, this component
     *   will not respond to user input, and it will appear visually disabled and disabled to
     *   accessibility services.
     * @param shapes the [SplitButtonShapes] that the trailing button will morph between depending
     *   on the user's interaction with the button.
     * @param colors [ButtonColors] that will be used to resolve the colors for this button in
     *   different states. See [ButtonDefaults.buttonColors].
     * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
     *   states. This controls the size of the shadow below the button. See
     *   [ButtonElevation.shadowElevation].
     * @param border the border to draw around the container of this button contentPadding the
     *   spacing values to apply internally between the container and the content
     * @param contentPadding the spacing values to apply internally between the container and the
     *   content
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this button. You can use this to change the button's appearance
     *   or preview the button in different states. Note that if `null` is provided, interactions
     *   will still happen internally.
     * @param content the content to be placed in the button
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun ElevatedTrailingButton(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shapes: SplitButtonShapes = trailingButtonShapesFor(SmallContainerHeight),
        colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
        elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
        border: BorderStroke? = null,
        contentPadding: PaddingValues = trailingButtonContentPaddingFor(SmallContainerHeight),
        interactionSource: MutableInteractionSource? = null,
        content: @Composable RowScope.() -> Unit,
    ) {
        TrailingButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            shapes = shapes,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun shapeByInteraction(
    shapes: SplitButtonShapes,
    pressed: Boolean,
    checked: Boolean,
    animationSpec: FiniteAnimationSpec<Float>,
): Shape {
    val shape =
        if (pressed) {
            shapes.pressedShape ?: shapes.shape
        } else if (checked) {
            shapes.checkedShape ?: shapes.shape
        } else shapes.shape

    if (shapes.hasRoundedCornerShapes) {
        return rememberAnimatedShape(shape as RoundedCornerShape, animationSpec)
    }
    return shape
}

/**
 * The shapes that will be used in [SplitButtonLayout]. Split button will morph between these shapes
 * depending on the interaction of the buttons, assuming all of the shapes are [CornerBasedShape]s.
 *
 * @property shape is the default shape.
 * @property pressedShape is the pressed shape.
 * @property checkedShape is the checked shape.
 */
@ExperimentalMaterial3ExpressiveApi
class SplitButtonShapes(val shape: Shape, val pressedShape: Shape?, val checkedShape: Shape?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is SplitButtonShapes) return false

        if (shape != other.shape) return false
        if (pressedShape != other.pressedShape) return false
        if (checkedShape != other.checkedShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        if (pressedShape != null) {
            result = 31 * result + pressedShape.hashCode()
        }
        if (checkedShape != null) {
            result = 31 * result + checkedShape.hashCode()
        }

        return result
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val SplitButtonShapes.hasRoundedCornerShapes: Boolean
    get() {
        // Ignore null shapes and only check default shape for RoundedCorner
        if (pressedShape != null && pressedShape !is RoundedCornerShape) return false
        if (checkedShape != null && checkedShape !is RoundedCornerShape) return false
        return shape is RoundedCornerShape
    }

private const val LeadingButtonLayoutId = "LeadingButton"
private const val TrailingButtonLayoutId = "TrailingButton"
