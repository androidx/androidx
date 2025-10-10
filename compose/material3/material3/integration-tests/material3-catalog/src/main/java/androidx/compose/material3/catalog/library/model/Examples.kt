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

package androidx.compose.material3.catalog.library.model

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.adaptive.navigationsuite.samples.NavigationSuiteScaffoldCustomConfigSample
import androidx.compose.material3.adaptive.navigationsuite.samples.NavigationSuiteScaffoldSample
import androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSample
import androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPane
import androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPaneLevitatedAsDialog
import androidx.compose.material3.adaptive.samples.NavigableListDetailPaneScaffoldSample
import androidx.compose.material3.adaptive.samples.SupportingPaneScaffoldSample
import androidx.compose.material3.catalog.library.util.AdaptiveNavigationSuiteSampleSourceUrl
import androidx.compose.material3.catalog.library.util.AdaptiveSampleSourceUrl
import androidx.compose.material3.catalog.library.util.SampleSourceUrl
import androidx.compose.material3.samples.AlertDialogSample
import androidx.compose.material3.samples.AlertDialogWithIconSample
import androidx.compose.material3.samples.AnimatedExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.AnimatedFloatingActionButtonSample
import androidx.compose.material3.samples.AssistChipSample
import androidx.compose.material3.samples.BasicAlertDialogSample
import androidx.compose.material3.samples.BottomAppBarWithFAB
import androidx.compose.material3.samples.BottomAppBarWithOverflow
import androidx.compose.material3.samples.BottomSheetScaffoldNestedScrollSample
import androidx.compose.material3.samples.ButtonGroupSample
import androidx.compose.material3.samples.ButtonSample
import androidx.compose.material3.samples.ButtonWithAnimatedShapeSample
import androidx.compose.material3.samples.ButtonWithIconSample
import androidx.compose.material3.samples.CardSample
import androidx.compose.material3.samples.CarouselWithShowAllButtonSample
import androidx.compose.material3.samples.CenteredHorizontalFloatingToolbarWithFabSample
import androidx.compose.material3.samples.CenteredSliderSample
import androidx.compose.material3.samples.CenteredVerticalFloatingToolbarWithFabSample
import androidx.compose.material3.samples.CheckboxRoundedStrokesSample
import androidx.compose.material3.samples.CheckboxSample
import androidx.compose.material3.samples.CheckboxWithTextSample
import androidx.compose.material3.samples.ChipGroupReflowSample
import androidx.compose.material3.samples.ChipGroupSingleLineSample
import androidx.compose.material3.samples.CircularProgressIndicatorSample
import androidx.compose.material3.samples.CircularWavyProgressIndicatorSample
import androidx.compose.material3.samples.ClickableCardSample
import androidx.compose.material3.samples.ClickableElevatedCardSample
import androidx.compose.material3.samples.ClickableOutlinedCardSample
import androidx.compose.material3.samples.ContainedLoadingIndicatorSample
import androidx.compose.material3.samples.CustomTwoRowsTopAppBar
import androidx.compose.material3.samples.DateInputSample
import androidx.compose.material3.samples.DatePickerDialogSample
import androidx.compose.material3.samples.DatePickerSample
import androidx.compose.material3.samples.DatePickerWithDateSelectableDatesSample
import androidx.compose.material3.samples.DateRangePickerSample
import androidx.compose.material3.samples.DenseTextFieldContentPadding
import androidx.compose.material3.samples.DeterminateContainedLoadingIndicatorSample
import androidx.compose.material3.samples.DeterminateLoadingIndicatorSample
import androidx.compose.material3.samples.DismissibleModalWideNavigationRailSample
import androidx.compose.material3.samples.DismissibleNavigationDrawerSample
import androidx.compose.material3.samples.DockedSearchBarScaffoldSample
import androidx.compose.material3.samples.EditableExposedDropdownMenuSample
import androidx.compose.material3.samples.ElevatedAssistChipSample
import androidx.compose.material3.samples.ElevatedButtonSample
import androidx.compose.material3.samples.ElevatedButtonWithAnimatedShapeSample
import androidx.compose.material3.samples.ElevatedCardSample
import androidx.compose.material3.samples.ElevatedFilterChipSample
import androidx.compose.material3.samples.ElevatedSplitButtonSample
import androidx.compose.material3.samples.ElevatedSuggestionChipSample
import androidx.compose.material3.samples.ElevatedToggleButtonSample
import androidx.compose.material3.samples.EnterAlwaysTopAppBar
import androidx.compose.material3.samples.ExitAlwaysBottomAppBar
import androidx.compose.material3.samples.ExitAlwaysBottomAppBarFixed
import androidx.compose.material3.samples.ExitAlwaysBottomAppBarFixedVibrant
import androidx.compose.material3.samples.ExitAlwaysBottomAppBarSpacedAround
import androidx.compose.material3.samples.ExitAlwaysBottomAppBarSpacedBetween
import androidx.compose.material3.samples.ExitAlwaysBottomAppBarSpacedEvenly
import androidx.compose.material3.samples.ExitUntilCollapsedCenterAlignedLargeFlexibleTopAppBar
import androidx.compose.material3.samples.ExitUntilCollapsedCenterAlignedMediumFlexibleTopAppBar
import androidx.compose.material3.samples.ExitUntilCollapsedLargeTopAppBar
import androidx.compose.material3.samples.ExitUntilCollapsedMediumTopAppBar
import androidx.compose.material3.samples.ExpandableHorizontalFloatingToolbarSample
import androidx.compose.material3.samples.ExpandableVerticalFloatingToolbarSample
import androidx.compose.material3.samples.ExposedDropdownMenuSample
import androidx.compose.material3.samples.ExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.ExtendedFloatingActionButtonTextSample
import androidx.compose.material3.samples.ExtraLargeFilledSplitButtonSample
import androidx.compose.material3.samples.ExtraSmallNarrowSquareIconButtonsSample
import androidx.compose.material3.samples.FadingHorizontalMultiBrowseCarouselSample
import androidx.compose.material3.samples.FancyIndicatorContainerTabs
import androidx.compose.material3.samples.FancyIndicatorTabs
import androidx.compose.material3.samples.FancyTabs
import androidx.compose.material3.samples.FilledIconButtonSample
import androidx.compose.material3.samples.FilledIconToggleButtonSample
import androidx.compose.material3.samples.FilledSplitButtonSample
import androidx.compose.material3.samples.FilledTonalButtonSample
import androidx.compose.material3.samples.FilledTonalButtonWithAnimatedShapeSample
import androidx.compose.material3.samples.FilledTonalIconButtonSample
import androidx.compose.material3.samples.FilledTonalIconToggleButtonSample
import androidx.compose.material3.samples.FilterChipSample
import androidx.compose.material3.samples.FilterChipWithLeadingIconSample
import androidx.compose.material3.samples.FilterChipWithTrailingIconSample
import androidx.compose.material3.samples.FloatingActionButtonMenuSample
import androidx.compose.material3.samples.FloatingActionButtonSample
import androidx.compose.material3.samples.FullScreenSearchBarScaffoldSample
import androidx.compose.material3.samples.HorizontalCenteredHeroCarouselSample
import androidx.compose.material3.samples.HorizontalFloatingToolbarAsScaffoldFabSample
import androidx.compose.material3.samples.HorizontalFloatingToolbarWithFabSample
import androidx.compose.material3.samples.HorizontalMultiBrowseCarouselSample
import androidx.compose.material3.samples.HorizontalUncontainedCarouselSample
import androidx.compose.material3.samples.IconButtonSample
import androidx.compose.material3.samples.IconToggleButtonSample
import androidx.compose.material3.samples.IndeterminateCircularProgressIndicatorSample
import androidx.compose.material3.samples.IndeterminateCircularWavyProgressIndicatorSample
import androidx.compose.material3.samples.IndeterminateLinearProgressIndicatorSample
import androidx.compose.material3.samples.IndeterminateLinearWavyProgressIndicatorSample
import androidx.compose.material3.samples.InputChipSample
import androidx.compose.material3.samples.InputChipWithAvatarSample
import androidx.compose.material3.samples.LargeAnimatedExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.LargeButtonWithIconSample
import androidx.compose.material3.samples.LargeExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.LargeExtendedFloatingActionButtonTextSample
import androidx.compose.material3.samples.LargeFilledSplitButtonSample
import androidx.compose.material3.samples.LargeFloatingActionButtonSample
import androidx.compose.material3.samples.LargeRoundUniformOutlinedIconButtonSample
import androidx.compose.material3.samples.LargeToggleButtonWithIconSample
import androidx.compose.material3.samples.LeadingIconTabs
import androidx.compose.material3.samples.LinearProgressIndicatorSample
import androidx.compose.material3.samples.LinearWavyProgressIndicatorSample
import androidx.compose.material3.samples.LoadingIndicatorPullToRefreshSample
import androidx.compose.material3.samples.LoadingIndicatorSample
import androidx.compose.material3.samples.MediumAnimatedExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.MediumButtonWithIconSample
import androidx.compose.material3.samples.MediumExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.MediumExtendedFloatingActionButtonTextSample
import androidx.compose.material3.samples.MediumFilledSplitButtonSample
import androidx.compose.material3.samples.MediumFloatingActionButtonSample
import androidx.compose.material3.samples.MediumRoundWideIconButtonSample
import androidx.compose.material3.samples.MediumToggleButtonWithIconSample
import androidx.compose.material3.samples.MenuSample
import androidx.compose.material3.samples.MenuWithScrollStateSample
import androidx.compose.material3.samples.ModalBottomSheetSample
import androidx.compose.material3.samples.ModalNavigationDrawerSample
import androidx.compose.material3.samples.ModalWideNavigationRailSample
import androidx.compose.material3.samples.MultiAutocompleteExposedDropdownMenuSample
import androidx.compose.material3.samples.MultiSelectConnectedButtonGroupWithFlowLayoutSample
import androidx.compose.material3.samples.NavigationBarItemWithBadge
import androidx.compose.material3.samples.NavigationBarSample
import androidx.compose.material3.samples.NavigationRailBottomAlignSample
import androidx.compose.material3.samples.NavigationRailSample
import androidx.compose.material3.samples.OneLineListItem
import androidx.compose.material3.samples.OutlinedButtonSample
import androidx.compose.material3.samples.OutlinedButtonWithAnimatedShapeSample
import androidx.compose.material3.samples.OutlinedCardSample
import androidx.compose.material3.samples.OutlinedIconButtonSample
import androidx.compose.material3.samples.OutlinedIconToggleButtonSample
import androidx.compose.material3.samples.OutlinedSplitButtonSample
import androidx.compose.material3.samples.OutlinedTextFieldWithInitialValueAndSelection
import androidx.compose.material3.samples.OutlinedToggleButtonSample
import androidx.compose.material3.samples.OverflowingHorizontalFloatingToolbarSample
import androidx.compose.material3.samples.OverflowingVerticalFloatingToolbarSample
import androidx.compose.material3.samples.PasswordTextField
import androidx.compose.material3.samples.PermanentNavigationDrawerSample
import androidx.compose.material3.samples.PinnedTopAppBar
import androidx.compose.material3.samples.PlainTooltipSample
import androidx.compose.material3.samples.PlainTooltipWithCaret
import androidx.compose.material3.samples.PlainTooltipWithCaretBelowAnchor
import androidx.compose.material3.samples.PlainTooltipWithCaretEndOfAnchor
import androidx.compose.material3.samples.PlainTooltipWithCaretLeftOfAnchor
import androidx.compose.material3.samples.PlainTooltipWithCaretRightOfAnchor
import androidx.compose.material3.samples.PlainTooltipWithCaretStartOfAnchor
import androidx.compose.material3.samples.PlainTooltipWithCustomCaret
import androidx.compose.material3.samples.PlainTooltipWithManualInvocationSample
import androidx.compose.material3.samples.PrimaryIconTabs
import androidx.compose.material3.samples.PrimaryTextTabs
import androidx.compose.material3.samples.PullToRefreshCustomIndicatorWithDefaultTransform
import androidx.compose.material3.samples.PullToRefreshSample
import androidx.compose.material3.samples.PullToRefreshSampleCustomState
import androidx.compose.material3.samples.PullToRefreshScalingSample
import androidx.compose.material3.samples.PullToRefreshViewModelSample
import androidx.compose.material3.samples.PullToRefreshWithLoadingIndicatorSample
import androidx.compose.material3.samples.RadioButtonSample
import androidx.compose.material3.samples.RadioGroupSample
import androidx.compose.material3.samples.RangeSliderSample
import androidx.compose.material3.samples.RangeSliderWithCustomComponents
import androidx.compose.material3.samples.RichTooltipSample
import androidx.compose.material3.samples.RichTooltipWithCaretSample
import androidx.compose.material3.samples.RichTooltipWithCustomCaretSample
import androidx.compose.material3.samples.RichTooltipWithManualInvocationSample
import androidx.compose.material3.samples.ScaffoldWithCoroutinesSnackbar
import androidx.compose.material3.samples.ScaffoldWithCustomSnackbar
import androidx.compose.material3.samples.ScaffoldWithIndefiniteSnackbar
import androidx.compose.material3.samples.ScaffoldWithMultilineSnackbar
import androidx.compose.material3.samples.ScaffoldWithSimpleSnackbar
import androidx.compose.material3.samples.ScrollableHorizontalFloatingToolbarSample
import androidx.compose.material3.samples.ScrollableVerticalFloatingToolbarSample
import androidx.compose.material3.samples.ScrollingFancyIndicatorContainerTabs
import androidx.compose.material3.samples.ScrollingPrimaryTextTabs
import androidx.compose.material3.samples.ScrollingSecondaryTextTabs
import androidx.compose.material3.samples.SecondaryIconTabs
import androidx.compose.material3.samples.SecondaryTextTabs
import androidx.compose.material3.samples.SegmentedButtonMultiSelectSample
import androidx.compose.material3.samples.SegmentedButtonSingleSelectSample
import androidx.compose.material3.samples.ShortNavigationBarSample
import androidx.compose.material3.samples.ShortNavigationBarWithHorizontalItemsSample
import androidx.compose.material3.samples.SimpleBottomAppBar
import androidx.compose.material3.samples.SimpleBottomSheetScaffoldSample
import androidx.compose.material3.samples.SimpleCenterAlignedTopAppBar
import androidx.compose.material3.samples.SimpleCenterAlignedTopAppBarWithSubtitle
import androidx.compose.material3.samples.SimpleOutlinedTextFieldSample
import androidx.compose.material3.samples.SimpleSearchBarSample
import androidx.compose.material3.samples.SimpleTextFieldSample
import androidx.compose.material3.samples.SimpleTopAppBar
import androidx.compose.material3.samples.SimpleTopAppBarWithAdaptiveActions
import androidx.compose.material3.samples.SimpleTopAppBarWithSubtitle
import androidx.compose.material3.samples.SingleSelectConnectedButtonGroupWithFlowLayoutSample
import androidx.compose.material3.samples.SliderSample
import androidx.compose.material3.samples.SliderWithCustomThumbSample
import androidx.compose.material3.samples.SliderWithCustomTrackAndThumbSample
import androidx.compose.material3.samples.SliderWithTrackIconsSample
import androidx.compose.material3.samples.SmallAnimatedExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.SmallButtonSample
import androidx.compose.material3.samples.SmallExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.SmallExtendedFloatingActionButtonTextSample
import androidx.compose.material3.samples.SmallFloatingActionButtonSample
import androidx.compose.material3.samples.SplitButtonWithDropdownMenuSample
import androidx.compose.material3.samples.SplitButtonWithIconSample
import androidx.compose.material3.samples.SplitButtonWithTextSample
import androidx.compose.material3.samples.SplitButtonWithUnCheckableTrailingButtonSample
import androidx.compose.material3.samples.SquareButtonSample
import androidx.compose.material3.samples.SquareToggleButtonSample
import androidx.compose.material3.samples.StepRangeSliderSample
import androidx.compose.material3.samples.StepsSliderSample
import androidx.compose.material3.samples.SuggestionChipSample
import androidx.compose.material3.samples.SwitchSample
import androidx.compose.material3.samples.SwitchWithThumbIconSample
import androidx.compose.material3.samples.TextAndIconTabs
import androidx.compose.material3.samples.TextArea
import androidx.compose.material3.samples.TextButtonSample
import androidx.compose.material3.samples.TextButtonWithAnimatedShapeSample
import androidx.compose.material3.samples.TextFieldWithErrorState
import androidx.compose.material3.samples.TextFieldWithHideKeyboardOnImeAction
import androidx.compose.material3.samples.TextFieldWithIcons
import androidx.compose.material3.samples.TextFieldWithInitialValueAndSelection
import androidx.compose.material3.samples.TextFieldWithPlaceholder
import androidx.compose.material3.samples.TextFieldWithPrefixAndSuffix
import androidx.compose.material3.samples.TextFieldWithSupportingText
import androidx.compose.material3.samples.TextFieldWithTransformations
import androidx.compose.material3.samples.ThreeLineListItemWithExtendedSupporting
import androidx.compose.material3.samples.ThreeLineListItemWithOverlineAndSupporting
import androidx.compose.material3.samples.TimeInputSample
import androidx.compose.material3.samples.TimePickerSample
import androidx.compose.material3.samples.TimePickerSwitchableSample
import androidx.compose.material3.samples.TintedIconButtonSample
import androidx.compose.material3.samples.ToggleButtonSample
import androidx.compose.material3.samples.ToggleButtonWithIconSample
import androidx.compose.material3.samples.TonalSplitButtonSample
import androidx.compose.material3.samples.TonalToggleButtonSample
import androidx.compose.material3.samples.TriStateCheckboxRoundedStrokesSample
import androidx.compose.material3.samples.TriStateCheckboxSample
import androidx.compose.material3.samples.TwoLineListItem
import androidx.compose.material3.samples.VerticalCenteredSliderSample
import androidx.compose.material3.samples.VerticalFloatingToolbarWithFabSample
import androidx.compose.material3.samples.VerticalSliderSample
import androidx.compose.material3.samples.WideNavigationRailArrangementsSample
import androidx.compose.material3.samples.WideNavigationRailCollapsedSample
import androidx.compose.material3.samples.WideNavigationRailExpandedSample
import androidx.compose.material3.samples.WideNavigationRailResponsiveSample
import androidx.compose.material3.samples.XLargeButtonWithIconSample
import androidx.compose.material3.samples.XLargeToggleButtonWithIconSample
import androidx.compose.material3.samples.XSmallButtonWithIconSample
import androidx.compose.material3.samples.XSmallFilledSplitButtonSample
import androidx.compose.material3.samples.XSmallToggleButtonWithIconSample
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Example(
    val name: String,
    val description: String,
    val sourceUrl: String,
    val isExpressive: Boolean,
    val content: @Composable () -> Unit,
)

private const val AdaptiveExampleDescription = "Adaptive examples"
private const val AdaptiveExampleSourceUrl = "$AdaptiveSampleSourceUrl/ThreePaneScaffoldSamples.kt"
val AdaptiveExamples =
    listOf(
        Example(
            name = "ListDetailPaneScaffoldSample",
            description = AdaptiveExampleDescription,
            sourceUrl = AdaptiveExampleSourceUrl,
            isExpressive = false,
        ) {
            ListDetailPaneScaffoldSample()
        },
        Example(
            name = "ListDetailPaneScaffoldSampleWithExtraPane",
            description = AdaptiveExampleDescription,
            sourceUrl = AdaptiveExampleSourceUrl,
            isExpressive = false,
        ) {
            ListDetailPaneScaffoldSampleWithExtraPane()
        },
        Example(
            name = "ListDetailPaneScaffoldSampleWithExtraPaneLevitatedAsDialog",
            description = AdaptiveExampleDescription,
            sourceUrl = AdaptiveExampleSourceUrl,
            isExpressive = false,
        ) {
            ListDetailPaneScaffoldSampleWithExtraPaneLevitatedAsDialog()
        },
        Example(
            name = "NavigableListDetailPaneScaffoldSample",
            description = AdaptiveExampleDescription,
            sourceUrl = AdaptiveExampleSourceUrl,
            isExpressive = false,
        ) {
            NavigableListDetailPaneScaffoldSample()
        },
        Example(
            name = "SupportingPaneScaffoldSample",
            description = AdaptiveExampleDescription,
            sourceUrl = AdaptiveExampleSourceUrl,
            isExpressive = false,
        ) {
            SupportingPaneScaffoldSample()
        },
    )

private const val BadgeExampleDescription = "Badge examples"
private const val BadgeExampleSourceUrl = "$SampleSourceUrl/BadgeSamples.kt"
val BadgeExamples =
    listOf(
        Example(
            name = "NavigationBarItemWithBadge",
            description = BadgeExampleDescription,
            sourceUrl = BadgeExampleSourceUrl,
            isExpressive = false,
        ) {
            NavigationBarItemWithBadge()
        }
    )

private const val BottomSheetExampleDescription = "Bottom Sheet examples"
private const val BottomSheetExampleSourceUrl = "$SampleSourceUrl/BottomSheetSamples.kt"
val BottomSheetExamples =
    listOf(
        Example(
            name = "ModalBottomSheetSample",
            description = BottomSheetExampleDescription,
            sourceUrl = BottomSheetExampleSourceUrl,
            isExpressive = false,
        ) {
            ModalBottomSheetSample()
        },
        Example(
            name = "SimpleBottomSheetScaffoldSample",
            description = BottomSheetExampleDescription,
            sourceUrl = BottomSheetExampleSourceUrl,
            isExpressive = false,
        ) {
            SimpleBottomSheetScaffoldSample()
        },
        Example(
            name = "BottomSheetScaffoldNestedScrollSample",
            description = BottomSheetExampleDescription,
            sourceUrl = BottomSheetExampleSourceUrl,
            isExpressive = false,
        ) {
            BottomSheetScaffoldNestedScrollSample()
        },
    )

private const val ButtonsExampleDescription = "Button examples"
private const val ButtonsExampleSourceUrl = "$SampleSourceUrl/ButtonSamples.kt"
val ButtonsExamples =
    listOf(
        Example(
            name = "ButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = false,
        ) {
            ButtonSample()
        },
        Example(
            name = "ButtonWithAnimatedShapeSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            ButtonWithAnimatedShapeSample()
        },
        Example(
            name = "SquareButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            SquareButtonSample()
        },
        Example(
            name = "SmallButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            SmallButtonSample()
        },
        Example(
            name = "ElevatedButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = false,
        ) {
            ElevatedButtonSample()
        },
        Example(
            name = "ElevatedButtonWithAnimatedShapeSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            ElevatedButtonWithAnimatedShapeSample()
        },
        Example(
            name = "FilledTonalButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = false,
        ) {
            FilledTonalButtonSample()
        },
        Example(
            name = "FilledTonalButtonWithAnimatedShapeSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            FilledTonalButtonWithAnimatedShapeSample()
        },
        Example(
            name = "OutlinedButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = false,
        ) {
            OutlinedButtonSample()
        },
        Example(
            name = "OutlinedButtonWithAnimatedShapeSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            OutlinedButtonWithAnimatedShapeSample()
        },
        Example(
            name = "TextButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = false,
        ) {
            TextButtonSample()
        },
        Example(
            name = "TextButtonWithAnimatedShapeSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            TextButtonWithAnimatedShapeSample()
        },
        Example(
            name = "ButtonWithIconSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = false,
        ) {
            ButtonWithIconSample()
        },
        Example(
            name = "XSmallButtonWithIconSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            XSmallButtonWithIconSample()
        },
        Example(
            name = "MediumButtonWithIconSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            MediumButtonWithIconSample()
        },
        Example(
            name = "LargeButtonWithIconSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            LargeButtonWithIconSample()
        },
        Example(
            name = "XLargeButtonWithIconSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            XLargeButtonWithIconSample()
        },
    )

private const val ButtonGroupsExampleDescription = "ButtonGroup examples"
private const val ButtonGroupsExampleSourceUrl = "$SampleSourceUrl/ButtonGroupSamples.kt"
val ButtonGroupsExamples =
    listOf(
        Example(
            name = "ButtonGroupSample",
            description = ButtonGroupsExampleDescription,
            sourceUrl = ButtonGroupsExampleSourceUrl,
            isExpressive = true,
        ) {
            ButtonGroupSample()
        },
        Example(
            name = "SingleSelectConnectedButtonGroupWithFlowLayoutSample",
            description = ButtonGroupsExampleDescription,
            sourceUrl = ButtonGroupsExampleSourceUrl,
            isExpressive = true,
        ) {
            SingleSelectConnectedButtonGroupWithFlowLayoutSample()
        },
        Example(
            name = "MultiSelectConnectedButtonGroupWithFlowLayoutSample",
            description = ButtonGroupsExampleDescription,
            sourceUrl = ButtonGroupsExampleSourceUrl,
            isExpressive = true,
        ) {
            MultiSelectConnectedButtonGroupWithFlowLayoutSample()
        },
    )

private const val CardsExampleDescription = "Cards examples"
private const val CardsExampleSourceUrl = "$SampleSourceUrl/CardSamples.kt"
val CardExamples =
    listOf(
        Example(
            name = "CardSample",
            description = CardsExampleDescription,
            sourceUrl = CardsExampleSourceUrl,
            isExpressive = false,
        ) {
            CardSample()
        },
        Example(
            name = "ClickableCardSample",
            description = CardsExampleDescription,
            sourceUrl = CardsExampleSourceUrl,
            isExpressive = false,
        ) {
            ClickableCardSample()
        },
        Example(
            name = "ElevatedCardSample",
            description = CardsExampleDescription,
            sourceUrl = CardsExampleSourceUrl,
            isExpressive = false,
        ) {
            ElevatedCardSample()
        },
        Example(
            name = "ClickableElevatedCardSample",
            description = CardsExampleDescription,
            sourceUrl = CardsExampleSourceUrl,
            isExpressive = false,
        ) {
            ClickableElevatedCardSample()
        },
        Example(
            name = "OutlinedCardSample",
            description = CardsExampleDescription,
            sourceUrl = CardsExampleSourceUrl,
            isExpressive = false,
        ) {
            OutlinedCardSample()
        },
        Example(
            name = "ClickableOutlinedCardSample",
            description = CardsExampleDescription,
            sourceUrl = CardsExampleSourceUrl,
            isExpressive = false,
        ) {
            ClickableOutlinedCardSample()
        },
    )

private const val CarouselExampleDescription = "Carousel examples"
private const val CarouselExampleSourceUrl = "$SampleSourceUrl/CarouselSamples.kt"
val CarouselExamples =
    listOf(
        Example(
            name = "HorizontalMultiBrowseCarouselSample",
            description = CarouselExampleDescription,
            sourceUrl = CarouselExampleSourceUrl,
            isExpressive = false,
        ) {
            HorizontalMultiBrowseCarouselSample()
        },
        Example(
            name = "HorizontalUncontainedCarouselSample",
            description = CarouselExampleDescription,
            sourceUrl = CarouselExampleSourceUrl,
            isExpressive = false,
        ) {
            HorizontalUncontainedCarouselSample()
        },
        Example(
            name = "HorizontalCenteredHeroCarouselSample",
            description = CarouselExampleDescription,
            sourceUrl = CarouselExampleSourceUrl,
            isExpressive = false,
        ) {
            HorizontalCenteredHeroCarouselSample()
        },
        Example(
            name = "FadingHorizontalMultiBrowseCarouselSample",
            description = CarouselExampleDescription,
            sourceUrl = CarouselExampleSourceUrl,
            isExpressive = false,
        ) {
            FadingHorizontalMultiBrowseCarouselSample()
        },
        Example(
            name = "CarouselWithShowAllButtonSample",
            description = CarouselExampleDescription,
            sourceUrl = CarouselExampleSourceUrl,
            isExpressive = false,
        ) {
            CarouselWithShowAllButtonSample()
        },
    )

private const val CheckboxesExampleDescription = "Checkboxes examples"
private const val CheckboxesExampleSourceUrl = "$SampleSourceUrl/CheckboxSamples.kt"
val CheckboxesExamples =
    listOf(
        Example(
            name = "CheckboxSample",
            description = CheckboxesExampleDescription,
            sourceUrl = CheckboxesExampleSourceUrl,
            isExpressive = false,
        ) {
            CheckboxSample()
        },
        Example(
            name = "CheckboxWithTextSample",
            description = CheckboxesExampleDescription,
            sourceUrl = CheckboxesExampleSourceUrl,
            isExpressive = false,
        ) {
            CheckboxWithTextSample()
        },
        Example(
            name = "CheckboxRoundedStrokesSample",
            description = CheckboxesExampleDescription,
            sourceUrl = CheckboxesExampleSourceUrl,
            isExpressive = false,
        ) {
            CheckboxRoundedStrokesSample()
        },
        Example(
            name = "TriStateCheckboxSample",
            description = CheckboxesExampleDescription,
            sourceUrl = CheckboxesExampleSourceUrl,
            isExpressive = false,
        ) {
            TriStateCheckboxSample()
        },
        Example(
            name = "TriStateCheckboxRoundedStrokesSample",
            description = CheckboxesExampleDescription,
            sourceUrl = CheckboxesExampleSourceUrl,
            isExpressive = false,
        ) {
            TriStateCheckboxRoundedStrokesSample()
        },
    )

private const val ChipsExampleDescription = "Chips examples"
private const val ChipsExampleSourceUrl = "$SampleSourceUrl/ChipSamples.kt"
val ChipsExamples =
    listOf(
        Example(
            name = "AssistChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl,
            isExpressive = false,
        ) {
            AssistChipSample()
        },
        Example(
            name = "ElevatedAssistChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl,
            isExpressive = false,
        ) {
            ElevatedAssistChipSample()
        },
        Example(
            name = "FilterChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl,
            isExpressive = false,
        ) {
            FilterChipSample()
        },
        Example(
            name = "ElevatedFilterChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl,
            isExpressive = false,
        ) {
            ElevatedFilterChipSample()
        },
        Example(
            name = "FilterChipWithLeadingIconSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl,
            isExpressive = false,
        ) {
            FilterChipWithLeadingIconSample()
        },
        Example(
            name = "FilterChipWithTrailingIconSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl,
            isExpressive = false,
        ) {
            FilterChipWithTrailingIconSample()
        },
        Example(
            name = "InputChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl,
            isExpressive = false,
        ) {
            InputChipSample()
        },
        Example(
            name = "InputChipWithAvatarSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl,
            isExpressive = false,
        ) {
            InputChipWithAvatarSample()
        },
        Example(
            name = "SuggestionChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl,
            isExpressive = false,
        ) {
            SuggestionChipSample()
        },
        Example(
            name = "ElevatedSuggestionChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl,
            isExpressive = false,
        ) {
            ElevatedSuggestionChipSample()
        },
        Example(
            name = "ChipGroupSingleLineSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl,
            isExpressive = false,
        ) {
            ChipGroupSingleLineSample()
        },
        Example(
            name = "ChipGroupReflowSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl,
            isExpressive = false,
        ) {
            ChipGroupReflowSample()
        },
    )

private const val DatePickerExampleDescription = "Date picker examples"
private const val DatePickerExampleSourceUrl = "$SampleSourceUrl/DatePickerSamples.kt"
val DatePickerExamples =
    listOf(
        Example(
            name = "DatePickerSample",
            description = DatePickerExampleDescription,
            sourceUrl = DatePickerExampleSourceUrl,
            isExpressive = false,
        ) {
            DatePickerSample()
        },
        Example(
            name = "DatePickerDialogSample",
            description = DatePickerExampleDescription,
            sourceUrl = DatePickerExampleSourceUrl,
            isExpressive = false,
        ) {
            DatePickerDialogSample()
        },
        Example(
            name = "DatePickerWithDateSelectableDatesSample",
            description = DatePickerExampleDescription,
            sourceUrl = DatePickerExampleSourceUrl,
            isExpressive = false,
        ) {
            DatePickerWithDateSelectableDatesSample()
        },
        Example(
            name = "DateInputSample",
            description = DatePickerExampleDescription,
            sourceUrl = DatePickerExampleSourceUrl,
            isExpressive = false,
        ) {
            DateInputSample()
        },
        Example(
            name = "DateRangePickerSample",
            description = DatePickerExampleDescription,
            sourceUrl = DatePickerExampleSourceUrl,
            isExpressive = false,
        ) {
            DateRangePickerSample()
        },
    )

private const val DialogExampleDescription = "Dialog examples"
private const val DialogExampleSourceUrl = "$SampleSourceUrl/AlertDialogSamples.kt"
val DialogExamples =
    listOf(
        Example(
            name = "AlertDialogSample",
            description = DialogExampleDescription,
            sourceUrl = DialogExampleSourceUrl,
            isExpressive = false,
        ) {
            AlertDialogSample()
        },
        Example(
            name = "AlertDialogWithIconSample",
            description = DialogExampleDescription,
            sourceUrl = DialogExampleSourceUrl,
            isExpressive = false,
        ) {
            AlertDialogWithIconSample()
        },
        Example(
            name = "BasicAlertDialogSample",
            description = DialogExampleDescription,
            sourceUrl = DialogExampleSourceUrl,
            isExpressive = false,
        ) {
            BasicAlertDialogSample()
        },
    )

private const val BottomAppBarsExampleDescription = "Bottom app bar examples"
private const val BottomAppBarsExampleSourceUrl = "$SampleSourceUrl/AppBarSamples.kt"
val BottomAppBarsExamples =
    listOf(
        Example(
            name = "SimpleBottomAppBar",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
            isExpressive = false,
        ) {
            SimpleBottomAppBar()
        },
        Example(
            name = "BottomAppBarWithFAB",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
            isExpressive = false,
        ) {
            BottomAppBarWithFAB()
        },
        Example(
            name = "BottomAppBarWithOverflow",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
            isExpressive = true,
        ) {
            BottomAppBarWithOverflow()
        },
        Example(
            name = "ExitAlwaysBottomAppBar",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
            isExpressive = false,
        ) {
            ExitAlwaysBottomAppBar()
        },
        Example(
            name = "ExitAlwaysBottomAppBarSpacedAround",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
            isExpressive = true,
        ) {
            ExitAlwaysBottomAppBarSpacedAround()
        },
        Example(
            name = "ExitAlwaysBottomAppBarSpacedBetween",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
            isExpressive = true,
        ) {
            ExitAlwaysBottomAppBarSpacedBetween()
        },
        Example(
            name = "ExitAlwaysBottomAppBarSpacedEvenly",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
            isExpressive = true,
        ) {
            ExitAlwaysBottomAppBarSpacedEvenly()
        },
        Example(
            name = "ExitAlwaysBottomAppBarFixed",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
            isExpressive = true,
        ) {
            ExitAlwaysBottomAppBarFixed()
        },
        Example(
            name = "ExitAlwaysBottomAppBarFixedVibrant",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
            isExpressive = true,
        ) {
            ExitAlwaysBottomAppBarFixedVibrant()
        },
    )

private const val TopAppBarExampleDescription = "Top app bar examples"
private const val TopAppBarExampleSourceUrl = "$SampleSourceUrl/AppBarSamples.kt"
val TopAppBarExamples =
    listOf(
        Example(
            name = "SimpleTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
            isExpressive = false,
        ) {
            SimpleTopAppBar()
        },
        Example(
            name = "SimpleTopAppBarWithAdaptiveActions",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleDescription,
            isExpressive = false,
        ) {
            SimpleTopAppBarWithAdaptiveActions()
        },
        Example(
            name = "SimpleTopAppBarWithSubtitle",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
            isExpressive = true,
        ) {
            SimpleTopAppBarWithSubtitle()
        },
        Example(
            name = "SimpleCenterAlignedTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
            isExpressive = false,
        ) {
            SimpleCenterAlignedTopAppBar()
        },
        Example(
            name = "SimpleCenterAlignedTopAppBarWithSubtitle",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
            isExpressive = true,
        ) {
            SimpleCenterAlignedTopAppBarWithSubtitle()
        },
        Example(
            name = "PinnedTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
            isExpressive = false,
        ) {
            PinnedTopAppBar()
        },
        Example(
            name = "EnterAlwaysTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
            isExpressive = true,
        ) {
            EnterAlwaysTopAppBar()
        },
        Example(
            name = "ExitUntilCollapsedMediumTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
            isExpressive = false,
        ) {
            ExitUntilCollapsedMediumTopAppBar()
        },
        Example(
            name = "ExitUntilCollapsedCenterAlignedMediumFlexibleTopAppBar with subtitle",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
            isExpressive = false,
        ) {
            ExitUntilCollapsedCenterAlignedMediumFlexibleTopAppBar()
        },
        Example(
            name = "ExitUntilCollapsedLargeTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
            isExpressive = false,
        ) {
            ExitUntilCollapsedLargeTopAppBar()
        },
        Example(
            name = "ExitUntilCollapsedCenterAlignedLargeFlexibleTopAppBar with subtitle",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
            isExpressive = true,
        ) {
            ExitUntilCollapsedCenterAlignedLargeFlexibleTopAppBar()
        },
        Example(
            name = "CustomTwoRowsTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
            isExpressive = true,
        ) {
            CustomTwoRowsTopAppBar()
        },
    )

private const val FloatingToolbarsExampleDescription = "Floating toolbar examples"
private const val FloatingToolbarsExampleSourceUrl = "$SampleSourceUrl/FloatingToolbarSamples.kt"

val FloatingToolbarsExamples =
    listOf(
        Example(
            name = "ExpandableHorizontalFloatingToolbarSample",
            description = FloatingToolbarsExampleDescription,
            sourceUrl = FloatingToolbarsExampleSourceUrl,
            isExpressive = true,
        ) {
            ExpandableHorizontalFloatingToolbarSample()
        },
        Example(
            name = "OverflowingHorizontalFloatingToolbarSample",
            description = FloatingToolbarsExampleDescription,
            sourceUrl = FloatingToolbarsExampleSourceUrl,
            isExpressive = true,
        ) {
            OverflowingHorizontalFloatingToolbarSample()
        },
        Example(
            name = "ScrollableHorizontalFloatingToolbarSample",
            description = FloatingToolbarsExampleDescription,
            sourceUrl = FloatingToolbarsExampleSourceUrl,
            isExpressive = true,
        ) {
            ScrollableHorizontalFloatingToolbarSample()
        },
        Example(
            name = "ExpandableVerticalFloatingToolbarSample",
            description = FloatingToolbarsExampleDescription,
            sourceUrl = FloatingToolbarsExampleSourceUrl,
            isExpressive = true,
        ) {
            ExpandableVerticalFloatingToolbarSample()
        },
        Example(
            name = "OverflowingVerticalFloatingToolbarSample",
            description = FloatingToolbarsExampleDescription,
            sourceUrl = FloatingToolbarsExampleSourceUrl,
            isExpressive = true,
        ) {
            OverflowingVerticalFloatingToolbarSample()
        },
        Example(
            name = "ScrollableVerticalFloatingToolbarSample",
            description = FloatingToolbarsExampleDescription,
            sourceUrl = FloatingToolbarsExampleSourceUrl,
            isExpressive = true,
        ) {
            ScrollableVerticalFloatingToolbarSample()
        },
        Example(
            name = "HorizontalFloatingToolbarWithFabSample",
            description = FloatingToolbarsExampleDescription,
            sourceUrl = FloatingToolbarsExampleSourceUrl,
            isExpressive = true,
        ) {
            HorizontalFloatingToolbarWithFabSample()
        },
        Example(
            name = "CenteredHorizontalFloatingToolbarWithFabSample",
            description = FloatingToolbarsExampleDescription,
            sourceUrl = FloatingToolbarsExampleSourceUrl,
            isExpressive = true,
        ) {
            CenteredHorizontalFloatingToolbarWithFabSample()
        },
        Example(
            name = "HorizontalFloatingToolbarAsScaffoldFabSample",
            description = FloatingToolbarsExampleDescription,
            sourceUrl = FloatingToolbarsExampleSourceUrl,
            isExpressive = true,
        ) {
            HorizontalFloatingToolbarAsScaffoldFabSample()
        },
        Example(
            name = "VerticalFloatingToolbarWithFabSample",
            description = FloatingToolbarsExampleDescription,
            sourceUrl = FloatingToolbarsExampleSourceUrl,
            isExpressive = true,
        ) {
            VerticalFloatingToolbarWithFabSample()
        },
        Example(
            name = "CenteredVerticalFloatingToolbarWithFabSample",
            description = FloatingToolbarsExampleDescription,
            sourceUrl = FloatingToolbarsExampleSourceUrl,
            isExpressive = true,
        ) {
            CenteredVerticalFloatingToolbarWithFabSample()
        },
    )

private const val ExtendedFABExampleDescription = "Extended FAB examples"
private const val ExtendedFABExampleSourceUrl = "$SampleSourceUrl/FloatingActionButtonSamples.kt"
val ExtendedFABExamples =
    listOf(
        Example(
            name = "ExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
            isExpressive = false,
        ) {
            ExtendedFloatingActionButtonSample()
        },
        Example(
            name = "SmallExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
            isExpressive = true,
        ) {
            SmallExtendedFloatingActionButtonSample()
        },
        Example(
            name = "MediumExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
            isExpressive = true,
        ) {
            MediumExtendedFloatingActionButtonSample()
        },
        Example(
            name = "LargeExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
            isExpressive = true,
        ) {
            LargeExtendedFloatingActionButtonSample()
        },
        Example(
            name = "ExtendedFloatingActionButtonTextSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
            isExpressive = false,
        ) {
            ExtendedFloatingActionButtonTextSample()
        },
        Example(
            name = "SmallExtendedFloatingActionButtonTextSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
            isExpressive = true,
        ) {
            SmallExtendedFloatingActionButtonTextSample()
        },
        Example(
            name = "MediumExtendedFloatingActionButtonTextSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
            isExpressive = true,
        ) {
            MediumExtendedFloatingActionButtonTextSample()
        },
        Example(
            name = "LargeExtendedFloatingActionButtonTextSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
            isExpressive = true,
        ) {
            LargeExtendedFloatingActionButtonTextSample()
        },
        Example(
            name = "AnimatedExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
            isExpressive = false,
        ) {
            AnimatedExtendedFloatingActionButtonSample()
        },
        Example(
            name = "SmallAnimatedExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
            isExpressive = true,
        ) {
            SmallAnimatedExtendedFloatingActionButtonSample()
        },
        Example(
            name = "MediumAnimatedExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
            isExpressive = true,
        ) {
            MediumAnimatedExtendedFloatingActionButtonSample()
        },
        Example(
            name = "LargeAnimatedExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
            isExpressive = true,
        ) {
            LargeAnimatedExtendedFloatingActionButtonSample()
        },
    )

private const val FloatingActionButtonsExampleDescription = "Floating action button examples"
private const val FloatingActionButtonsExampleSourceUrl =
    "$SampleSourceUrl/FloatingActionButtonSamples.kt"
val FloatingActionButtonsExamples =
    listOf(
        Example(
            name = "FloatingActionButtonSample",
            description = FloatingActionButtonsExampleDescription,
            sourceUrl = FloatingActionButtonsExampleSourceUrl,
            isExpressive = false,
        ) {
            FloatingActionButtonSample()
        },
        Example(
            name = "LargeFloatingActionButtonSample",
            description = FloatingActionButtonsExampleDescription,
            sourceUrl = FloatingActionButtonsExampleSourceUrl,
            isExpressive = false,
        ) {
            LargeFloatingActionButtonSample()
        },
        Example(
            name = "AnimatedFloatingActionButtonSample",
            description = FloatingActionButtonsExampleDescription,
            sourceUrl = FloatingActionButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            AnimatedFloatingActionButtonSample()
        },
        Example(
            name = "MediumFloatingActionButtonSample",
            description = FloatingActionButtonsExampleDescription,
            sourceUrl = FloatingActionButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            MediumFloatingActionButtonSample()
        },
        Example(
            name = "SmallFloatingActionButtonSample",
            description = FloatingActionButtonsExampleDescription,
            sourceUrl = FloatingActionButtonsExampleSourceUrl,
            isExpressive = false,
        ) {
            SmallFloatingActionButtonSample()
        },
    )

private const val FloatingActionButtonMenuExampleDescription = "FAB Menu examples"
private const val FloatingActionButtonMenuExampleSourceUrl =
    "$SampleSourceUrl/FloatingActionButtonMenuSamples.kt"
val FloatingActionButtonMenuExamples =
    listOf(
        Example(
            name = "FloatingActionButtonMenuSample",
            description = FloatingActionButtonMenuExampleDescription,
            sourceUrl = FloatingActionButtonMenuExampleSourceUrl,
            isExpressive = true,
        ) {
            FloatingActionButtonMenuSample()
        }
    )

private const val ListsExampleDescription = "List examples"
private const val ListsExampleSourceUrl = "$SampleSourceUrl/ListSamples.kt"
val ListsExamples =
    listOf(
        Example(
            name = "OneLineListItem",
            description = ListsExampleDescription,
            sourceUrl = ListsExampleSourceUrl,
            isExpressive = false,
        ) {
            OneLineListItem()
        },
        Example(
            name = "TwoLineListItem",
            description = ListsExampleDescription,
            sourceUrl = ListsExampleSourceUrl,
            isExpressive = false,
        ) {
            TwoLineListItem()
        },
        Example(
            name = "ThreeLineListItemWithOverlineAndSupporting",
            description = ListsExampleDescription,
            sourceUrl = ListsExampleSourceUrl,
            isExpressive = false,
        ) {
            ThreeLineListItemWithOverlineAndSupporting()
        },
        Example(
            name = "ThreeLineListItemWithExtendedSupporting",
            description = ListsExampleDescription,
            sourceUrl = ListsExampleSourceUrl,
            isExpressive = false,
        ) {
            ThreeLineListItemWithExtendedSupporting()
        },
    )

private const val IconButtonExampleDescription = "Icon button examples"
private const val IconButtonExampleSourceUrl = "$SampleSourceUrl/IconButtonSamples.kt"
val IconButtonExamples =
    listOf(
        Example(
            name = "IconButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
            isExpressive = false,
        ) {
            IconButtonSample()
        },
        Example(
            name = "TintedIconButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
            isExpressive = false,
        ) {
            TintedIconButtonSample()
        },
        Example(
            name = "IconToggleButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
            isExpressive = false,
        ) {
            IconToggleButtonSample()
        },
        Example(
            name = "FilledIconButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
            isExpressive = false,
        ) {
            FilledIconButtonSample()
        },
        Example(
            name = "FilledIconToggleButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
            isExpressive = false,
        ) {
            FilledIconToggleButtonSample()
        },
        Example(
            name = "FilledTonalIconButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
            isExpressive = false,
        ) {
            FilledTonalIconButtonSample()
        },
        Example(
            name = "FilledTonalIconToggleButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
            isExpressive = false,
        ) {
            FilledTonalIconToggleButtonSample()
        },
        Example(
            name = "OutlinedIconButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
            isExpressive = false,
        ) {
            OutlinedIconButtonSample()
        },
        Example(
            name = "OutlinedIconToggleButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
            isExpressive = false,
        ) {
            OutlinedIconToggleButtonSample()
        },
        Example(
            name = "XSmallNarrowSquareIconButtonsSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
            isExpressive = true,
        ) {
            ExtraSmallNarrowSquareIconButtonsSample()
        },
        Example(
            name = "MediumRoundWideIconButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
            isExpressive = true,
        ) {
            MediumRoundWideIconButtonSample()
        },
        Example(
            name = "LargeRoundUniformOutlinedIconButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
            isExpressive = true,
        ) {
            LargeRoundUniformOutlinedIconButtonSample()
        },
    )

private const val LoadingIndicatorsExampleDescription = "Loading indicators examples"
private const val LoadingIndicatorsExampleSourceUrl =
    "$SampleSourceUrl/" + "LoadingIndicatorSamples.kt"
val LoadingIndicatorsExamples =
    listOf(
        Example(
            name = "LoadingIndicatorSample",
            description = LoadingIndicatorsExampleDescription,
            sourceUrl = LoadingIndicatorsExampleSourceUrl,
            isExpressive = true,
        ) {
            LoadingIndicatorSample()
        },
        Example(
            name = "ContainedLoadingIndicatorSample",
            description = LoadingIndicatorsExampleDescription,
            sourceUrl = LoadingIndicatorsExampleSourceUrl,
            isExpressive = true,
        ) {
            ContainedLoadingIndicatorSample()
        },
        Example(
            name = "DeterminateLoadingIndicatorSample",
            description = LoadingIndicatorsExampleDescription,
            sourceUrl = LoadingIndicatorsExampleSourceUrl,
            isExpressive = true,
        ) {
            DeterminateLoadingIndicatorSample()
        },
        Example(
            name = "DeterminateContainedLoadingIndicatorSample",
            description = LoadingIndicatorsExampleDescription,
            sourceUrl = LoadingIndicatorsExampleSourceUrl,
            isExpressive = true,
        ) {
            DeterminateContainedLoadingIndicatorSample()
        },
        Example(
            name = "LoadingIndicatorPullToRefreshSample",
            description = LoadingIndicatorsExampleDescription,
            sourceUrl = LoadingIndicatorsExampleSourceUrl,
            isExpressive = true,
        ) {
            LoadingIndicatorPullToRefreshSample()
        },
    )

private const val MenusExampleDescription = "Menus examples"
private const val MenusExampleSourceUrl = "$SampleSourceUrl/MenuSamples.kt"
val MenusExamples =
    listOf(
        Example(
            name = "MenuSample",
            description = MenusExampleDescription,
            sourceUrl = MenusExampleSourceUrl,
            isExpressive = false,
        ) {
            MenuSample()
        },
        Example(
            name = "MenuWithScrollStateSample",
            description = MenusExampleDescription,
            sourceUrl = MenusExampleSourceUrl,
            isExpressive = false,
        ) {
            MenuWithScrollStateSample()
        },
        Example(
            name = "ExposedDropdownMenuSample",
            description = MenusExampleDescription,
            sourceUrl = MenusExampleSourceUrl,
            isExpressive = false,
        ) {
            ExposedDropdownMenuSample()
        },
        Example(
            name = "EditableExposedDropdownMenuSample",
            description = MenusExampleDescription,
            sourceUrl = MenusExampleSourceUrl,
            isExpressive = false,
        ) {
            EditableExposedDropdownMenuSample()
        },
        Example(
            name = "MultiAutocompleteExposedDropdownMenuSample",
            description = MenusExampleDescription,
            sourceUrl = MenusExampleSourceUrl,
            isExpressive = false,
        ) {
            MultiAutocompleteExposedDropdownMenuSample()
        },
    )

private const val NavigationBarExampleDescription = "Navigation bar examples"
private const val NavigationBarExampleSourceUrl = "$SampleSourceUrl/NavigationBarSamples.kt"
val NavigationBarExamples =
    listOf(
        Example(
            name = "ShortNavigationBarSample",
            description = NavigationBarExampleDescription,
            sourceUrl = NavigationBarExampleSourceUrl,
            isExpressive = true,
        ) {
            ShortNavigationBarSample()
        },
        Example(
            name = "ShortNavigationBarWithHorizontalItemsSample",
            description = NavigationBarExampleDescription,
            sourceUrl = NavigationBarExampleSourceUrl,
            isExpressive = true,
        ) {
            ShortNavigationBarWithHorizontalItemsSample()
        },
        Example(
            name = "NavigationBarSample",
            description = NavigationBarExampleDescription,
            sourceUrl = NavigationBarExampleSourceUrl,
            isExpressive = false,
        ) {
            NavigationBarSample()
        },
    )

private const val NavigationRailExampleDescription = "Navigation rail examples"
private const val NavigationRailExampleSourceUrl = "$SampleSourceUrl/NavigationRailSamples.kt"
val NavigationRailExamples =
    listOf(
        Example(
            name = "WideNavigationRailResponsiveSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
            isExpressive = true,
        ) {
            WideNavigationRailResponsiveSample()
        },
        Example(
            name = "ModalWideNavigationRailSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
            isExpressive = true,
        ) {
            ModalWideNavigationRailSample()
        },
        Example(
            name = "DismissibleModalWideNavigationRailSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
            isExpressive = true,
        ) {
            DismissibleModalWideNavigationRailSample()
        },
        Example(
            name = "WideNavigationRailCollapsedSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
            isExpressive = true,
        ) {
            WideNavigationRailCollapsedSample()
        },
        Example(
            name = "WideNavigationRailExpandedSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
            isExpressive = true,
        ) {
            WideNavigationRailExpandedSample()
        },
        Example(
            name = "WideNavigationRailArrangementsSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
            isExpressive = true,
        ) {
            WideNavigationRailArrangementsSample()
        },
        Example(
            name = "NavigationRailSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
            isExpressive = false,
        ) {
            NavigationRailSample()
        },
        Example(
            name = "NavigationRailBottomAlignSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
            isExpressive = false,
        ) {
            NavigationRailBottomAlignSample()
        },
    )

private const val NavigationDrawerExampleDescription = "Navigation drawer examples"
private const val NavigationDrawerExampleSourceUrl = "$SampleSourceUrl/DrawerSamples.kt"
val NavigationDrawerExamples =
    listOf(
        Example(
            name = "ModalNavigationDrawerSample",
            description = NavigationDrawerExampleDescription,
            sourceUrl = NavigationDrawerExampleSourceUrl,
            isExpressive = false,
        ) {
            ModalNavigationDrawerSample()
        },
        Example(
            name = "PermanentNavigationDrawerSample",
            description = NavigationDrawerExampleDescription,
            sourceUrl = NavigationDrawerExampleSourceUrl,
            isExpressive = false,
        ) {
            PermanentNavigationDrawerSample()
        },
        Example(
            name = "DismissibleNavigationDrawerSample",
            description = NavigationDrawerExampleDescription,
            sourceUrl = NavigationDrawerExampleSourceUrl,
            isExpressive = false,
        ) {
            DismissibleNavigationDrawerSample()
        },
    )

private const val NavigationSuiteScaffoldExampleDescription = "Navigation suite scaffold examples"
private const val NavigationSuiteScaffoldExampleSourceUrl =
    "$AdaptiveNavigationSuiteSampleSourceUrl/NavigationSuiteScaffoldSamples.kt"
val NavigationSuiteScaffoldExamples =
    listOf(
        Example(
            name = "NavigationSuiteScaffoldSample",
            description = NavigationSuiteScaffoldExampleDescription,
            sourceUrl = NavigationSuiteScaffoldExampleSourceUrl,
            isExpressive = true,
        ) {
            NavigationSuiteScaffoldSample()
        },
        Example(
            name = "NavigationSuiteScaffoldCustomConfigSample",
            description = NavigationSuiteScaffoldExampleDescription,
            sourceUrl = NavigationSuiteScaffoldExampleSourceUrl,
            isExpressive = true,
        ) {
            NavigationSuiteScaffoldCustomConfigSample()
        },
    )

private const val ProgressIndicatorsExampleDescription = "Progress indicators examples"
private const val ProgressIndicatorsExampleSourceUrl =
    "$SampleSourceUrl/" + "ProgressIndicatorSamples.kt"
val ProgressIndicatorsExamples =
    listOf(
        Example(
            name = "LinearProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl,
            isExpressive = false,
        ) {
            LinearProgressIndicatorSample()
        },
        Example(
            name = "LinearWavyProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl,
            isExpressive = true,
        ) {
            LinearWavyProgressIndicatorSample()
        },
        Example(
            name = "IndeterminateLinearProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl,
            isExpressive = false,
        ) {
            IndeterminateLinearProgressIndicatorSample()
        },
        Example(
            name = "IndeterminateLinearWavyProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl,
            isExpressive = true,
        ) {
            IndeterminateLinearWavyProgressIndicatorSample()
        },
        Example(
            name = "CircularProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl,
            isExpressive = false,
        ) {
            CircularProgressIndicatorSample()
        },
        Example(
            name = "CircularWavyProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl,
            isExpressive = true,
        ) {
            CircularWavyProgressIndicatorSample()
        },
        Example(
            name = "IndeterminateCircularProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl,
            isExpressive = false,
        ) {
            IndeterminateCircularProgressIndicatorSample()
        },
        Example(
            name = "IndeterminateCircularWavyProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl,
            isExpressive = true,
        ) {
            IndeterminateCircularWavyProgressIndicatorSample()
        },
    )

private const val PullToRefreshExampleDescription = "Pull-to-refresh examples"
private const val PullToRefreshExampleSourceUrl = "$SampleSourceUrl/PullToRefreshSamples.kt"
val PullToRefreshExamples =
    listOf(
        Example(
            name = "PullToRefreshSample",
            description = PullToRefreshExampleDescription,
            sourceUrl = PullToRefreshExampleSourceUrl,
            isExpressive = false,
        ) {
            PullToRefreshSample()
        },
        Example(
            name = "PullToRefreshWithLoadingIndicatorSample",
            description = PullToRefreshExampleDescription,
            sourceUrl = PullToRefreshExampleSourceUrl,
            isExpressive = true,
        ) {
            PullToRefreshWithLoadingIndicatorSample()
        },
        Example(
            name = "PullToRefreshScalingSample",
            description = PullToRefreshExampleDescription,
            sourceUrl = PullToRefreshExampleSourceUrl,
            isExpressive = false,
        ) {
            PullToRefreshScalingSample()
        },
        Example(
            name = "PullToRefreshSampleCustomState",
            description = PullToRefreshExampleDescription,
            sourceUrl = PullToRefreshExampleSourceUrl,
            isExpressive = false,
        ) {
            PullToRefreshSampleCustomState()
        },
        Example(
            name = "PullToRefreshViewModelSample",
            description = PullToRefreshExampleDescription,
            sourceUrl = PullToRefreshExampleSourceUrl,
            isExpressive = false,
        ) {
            PullToRefreshViewModelSample()
        },
        Example(
            name = "PullToRefreshCustomIndicatorWithDefaultTransform",
            description = PullToRefreshExampleDescription,
            sourceUrl = PullToRefreshExampleSourceUrl,
            isExpressive = false,
        ) {
            PullToRefreshCustomIndicatorWithDefaultTransform()
        },
    )

private const val RadioButtonsExampleDescription = "Radio buttons examples"
private const val RadioButtonsExampleSourceUrl = "$SampleSourceUrl/RadioButtonSamples.kt"
val RadioButtonsExamples =
    listOf(
        Example(
            name = "RadioButtonSample",
            description = RadioButtonsExampleDescription,
            sourceUrl = RadioButtonsExampleSourceUrl,
            isExpressive = false,
        ) {
            RadioButtonSample()
        },
        Example(
            name = "RadioGroupSample",
            description = RadioButtonsExampleDescription,
            sourceUrl = RadioButtonsExampleSourceUrl,
            isExpressive = false,
        ) {
            RadioGroupSample()
        },
    )

private const val SearchBarExampleDescription = "Search bar examples"
private const val SearchBarExampleSourceUrl = "$SampleSourceUrl/SearchBarSamples.kt"
val SearchBarExamples =
    listOf(
        Example(
            name = "SimpleSearchBarSample",
            description = SearchBarExampleDescription,
            sourceUrl = SearchBarExampleSourceUrl,
            isExpressive = false,
        ) {
            SimpleSearchBarSample()
        },
        Example(
            name = "FullScreenSearchBarScaffoldSample",
            description = SearchBarExampleDescription,
            sourceUrl = SearchBarExampleSourceUrl,
            isExpressive = true,
        ) {
            FullScreenSearchBarScaffoldSample()
        },
        Example(
            name = "DockedSearchBarScaffoldSample",
            description = SearchBarExampleDescription,
            sourceUrl = SearchBarExampleSourceUrl,
            isExpressive = false,
        ) {
            DockedSearchBarScaffoldSample()
        },
    )

private const val SegmentedButtonExampleDescription = "Segmented Button examples"
private const val SegmentedButtonSourceUrl = "$SampleSourceUrl/SegmentedButtonSamples.kt"
val SegmentedButtonExamples =
    listOf(
        Example(
            name = "SegmentedButtonSingleSelectSample",
            description = SegmentedButtonExampleDescription,
            sourceUrl = SegmentedButtonSourceUrl,
            isExpressive = false,
        ) {
            SegmentedButtonSingleSelectSample()
        },
        Example(
            name = "SegmentedButtonMultiSelectSample",
            description = SegmentedButtonExampleDescription,
            sourceUrl = SegmentedButtonSourceUrl,
            isExpressive = false,
        ) {
            SegmentedButtonMultiSelectSample()
        },
    )

private const val ToggleButtonsExampleDescription = "ToggleButton examples"
private const val ToggleButtonsExampleSourceUrl = "$SampleSourceUrl/ToggleButtonSamples.kt"
val ToggleButtonsExamples =
    listOf(
        Example(
            name = "ToggleButtonSample",
            description = ToggleButtonsExampleDescription,
            sourceUrl = ToggleButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            ToggleButtonSample()
        },
        Example(
            name = "RoundToggleButtonSample",
            description = ToggleButtonsExampleDescription,
            sourceUrl = ToggleButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            SquareToggleButtonSample()
        },
        Example(
            name = "ElevatedToggleButtonSample",
            description = ToggleButtonsExampleDescription,
            sourceUrl = ToggleButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            ElevatedToggleButtonSample()
        },
        Example(
            name = "TonalToggleButtonSample",
            description = ToggleButtonsExampleDescription,
            sourceUrl = ToggleButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            TonalToggleButtonSample()
        },
        Example(
            name = "OutlinedToggleButtonSample",
            description = ToggleButtonsExampleDescription,
            sourceUrl = ToggleButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            OutlinedToggleButtonSample()
        },
        Example(
            name = "ToggleButtonWithIconSample",
            description = ToggleButtonsExampleDescription,
            sourceUrl = ToggleButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            ToggleButtonWithIconSample()
        },
        Example(
            name = "XSmallToggleButtonWithIconSample",
            description = ToggleButtonsExampleDescription,
            sourceUrl = ToggleButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            XSmallToggleButtonWithIconSample()
        },
        Example(
            name = "MediumToggleButtonWithIconSample",
            description = ToggleButtonsExampleDescription,
            sourceUrl = ToggleButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            MediumToggleButtonWithIconSample()
        },
        Example(
            name = "LargeToggleButtonWithIconSample",
            description = ToggleButtonsExampleDescription,
            sourceUrl = ToggleButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            LargeToggleButtonWithIconSample()
        },
        Example(
            name = "XLargeToggleButtonWithIconSample",
            description = ToggleButtonsExampleDescription,
            sourceUrl = ToggleButtonsExampleSourceUrl,
            isExpressive = true,
        ) {
            XLargeToggleButtonWithIconSample()
        },
    )

private const val SlidersExampleDescription = "Sliders examples"
private const val SlidersExampleSourceUrl = "$SampleSourceUrl/SliderSamples.kt"
val SlidersExamples =
    listOf(
        Example(
            name = "SliderSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl,
            isExpressive = false,
        ) {
            SliderSample()
        },
        Example(
            name = "StepsSliderSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl,
            isExpressive = false,
        ) {
            StepsSliderSample()
        },
        Example(
            name = "SliderWithCustomThumbSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl,
            isExpressive = false,
        ) {
            SliderWithCustomThumbSample()
        },
        Example(
            name = "SliderWithCustomTrackAndThumbSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl,
            isExpressive = false,
        ) {
            SliderWithCustomTrackAndThumbSample()
        },
        Example(
            name = "SliderWithTrackIconsSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl,
            isExpressive = true,
        ) {
            SliderWithTrackIconsSample()
        },
        Example(
            name = "CenteredSliderSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl,
            isExpressive = true,
        ) {
            CenteredSliderSample()
        },
        Example(
            name = "VerticalSliderSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl,
            isExpressive = true,
        ) {
            VerticalSliderSample()
        },
        Example(
            name = "VerticalCenteredSliderSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl,
            isExpressive = true,
        ) {
            VerticalCenteredSliderSample()
        },
        Example(
            name = "RangeSliderSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl,
            isExpressive = false,
        ) {
            RangeSliderSample()
        },
        Example(
            name = "StepRangeSliderSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl,
            isExpressive = false,
        ) {
            StepRangeSliderSample()
        },
        Example(
            name = "RangeSliderWithCustomComponents",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl,
            isExpressive = false,
        ) {
            RangeSliderWithCustomComponents()
        },
    )

private const val SnackbarsExampleDescription = "Snackbars examples"
private const val SnackbarsExampleSourceUrl = "$SampleSourceUrl/ScaffoldSamples.kt"
val SnackbarsExamples =
    listOf(
        Example(
            name = "ScaffoldWithSimpleSnackbar",
            description = SnackbarsExampleDescription,
            sourceUrl = SnackbarsExampleSourceUrl,
            isExpressive = false,
        ) {
            ScaffoldWithSimpleSnackbar()
        },
        Example(
            name = "ScaffoldWithIndefiniteSnackbar",
            description = SnackbarsExampleDescription,
            sourceUrl = SnackbarsExampleSourceUrl,
            isExpressive = false,
        ) {
            ScaffoldWithIndefiniteSnackbar()
        },
        Example(
            name = "ScaffoldWithCustomSnackbar",
            description = SnackbarsExampleDescription,
            sourceUrl = SnackbarsExampleSourceUrl,
            isExpressive = false,
        ) {
            ScaffoldWithCustomSnackbar()
        },
        Example(
            name = "ScaffoldWithCoroutinesSnackbar",
            description = SnackbarsExampleDescription,
            sourceUrl = SnackbarsExampleSourceUrl,
            isExpressive = false,
        ) {
            ScaffoldWithCoroutinesSnackbar()
        },
        Example(
            name = "ScaffoldWithMultilineSnackbar",
            description = SnackbarsExampleDescription,
            sourceUrl = SnackbarsExampleSourceUrl,
            isExpressive = false,
        ) {
            ScaffoldWithMultilineSnackbar()
        },
    )

private const val SplitButtonExampleDescription = "Split Button examples"
private const val SplitButtonSourceUrl = "$SampleSourceUrl/SplitButtonSamples.kt"
val SplitButtonExamples =
    listOf(
        Example(
            name = "FilledSplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl,
            isExpressive = true,
        ) {
            FilledSplitButtonSample()
        },
        Example(
            name = "SplitButtonWithUnCheckableTrailingButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl,
            isExpressive = true,
        ) {
            SplitButtonWithUnCheckableTrailingButtonSample()
        },
        Example(
            name = "SplitButtonWithDropdownMenuSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl,
            isExpressive = true,
        ) {
            SplitButtonWithDropdownMenuSample()
        },
        Example(
            name = "TonalSplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl,
            isExpressive = true,
        ) {
            TonalSplitButtonSample()
        },
        Example(
            name = "ElevatedSplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl,
            isExpressive = true,
        ) {
            ElevatedSplitButtonSample()
        },
        Example(
            name = "OutlinedSplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl,
            isExpressive = true,
        ) {
            OutlinedSplitButtonSample()
        },
        Example(
            name = "SplitButtonWithTextSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl,
            isExpressive = true,
        ) {
            SplitButtonWithTextSample()
        },
        Example(
            name = "SplitButtonWithIconSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl,
            isExpressive = true,
        ) {
            SplitButtonWithIconSample()
        },
        Example(
            name = "XSmallFilledSplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl,
            isExpressive = true,
        ) {
            XSmallFilledSplitButtonSample()
        },
        Example(
            name = "MediumFilledSplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl,
            isExpressive = true,
        ) {
            MediumFilledSplitButtonSample()
        },
        Example(
            name = "LargeFilledSplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl,
            isExpressive = true,
        ) {
            LargeFilledSplitButtonSample()
        },
        Example(
            name = "ExtraLargeFilledSplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl,
            isExpressive = true,
        ) {
            ExtraLargeFilledSplitButtonSample()
        },
    )

private const val SwitchExampleDescription = "Switch examples"
private const val SwitchExampleSourceUrl = "$SampleSourceUrl/SwitchSamples.kt"
val SwitchExamples =
    listOf(
        Example(
            name = "SwitchSample",
            description = SwitchExampleDescription,
            sourceUrl = SwitchExampleSourceUrl,
            isExpressive = false,
        ) {
            SwitchSample()
        },
        Example(
            name = "SwitchWithThumbIconSample",
            description = SwitchExampleDescription,
            sourceUrl = SwitchExampleSourceUrl,
            isExpressive = false,
        ) {
            SwitchWithThumbIconSample()
        },
    )

private const val TabsExampleDescription = "Tabs examples"
private const val TabsExampleSourceUrl = "$SampleSourceUrl/TabSamples.kt"
val TabsExamples =
    listOf(
        Example(
            name = "PrimaryTextTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl,
            isExpressive = false,
        ) {
            PrimaryTextTabs()
        },
        Example(
            name = "PrimaryIconTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl,
            isExpressive = false,
        ) {
            PrimaryIconTabs()
        },
        Example(
            name = "SecondaryTextTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl,
            isExpressive = false,
        ) {
            SecondaryTextTabs()
        },
        Example(
            name = "SecondaryIconTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl,
            isExpressive = false,
        ) {
            SecondaryIconTabs()
        },
        Example(
            name = "TextAndIconTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl,
            isExpressive = false,
        ) {
            TextAndIconTabs()
        },
        Example(
            name = "LeadingIconTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl,
            isExpressive = false,
        ) {
            LeadingIconTabs()
        },
        Example(
            name = "ScrollingPrimaryTextTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl,
            isExpressive = false,
        ) {
            ScrollingPrimaryTextTabs()
        },
        Example(
            name = "ScrollingSecondaryTextTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl,
            isExpressive = false,
        ) {
            ScrollingSecondaryTextTabs()
        },
        Example(
            name = "FancyTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl,
            isExpressive = false,
        ) {
            FancyTabs()
        },
        Example(
            name = "FancyIndicatorTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl,
            isExpressive = false,
        ) {
            FancyIndicatorTabs()
        },
        Example(
            name = "FancyIndicatorContainerTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl,
            isExpressive = false,
        ) {
            FancyIndicatorContainerTabs()
        },
        Example(
            name = "ScrollingFancyIndicatorContainerTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl,
            isExpressive = false,
        ) {
            ScrollingFancyIndicatorContainerTabs()
        },
    )

private const val TimePickerDescription = "Time Picker examples"
private const val TimePickerSourceUrl = "$SampleSourceUrl/TimePicker.kt"
val TimePickerExamples =
    listOf(
        Example(
            name = "TimePickerSample",
            description = TimePickerDescription,
            sourceUrl = TimePickerSourceUrl,
            isExpressive = false,
        ) {
            TimePickerSample()
        },
        Example(
            name = "TimeInputSample",
            description = TimePickerDescription,
            sourceUrl = TimePickerSourceUrl,
            isExpressive = false,
        ) {
            TimeInputSample()
        },
        Example(
            name = "TimePickerSwitchableSample",
            description = TimePickerDescription,
            sourceUrl = TimePickerSourceUrl,
            isExpressive = false,
        ) {
            TimePickerSwitchableSample()
        },
    )

private const val TextFieldsExampleDescription = "Text fields examples"
private const val TextFieldsExampleSourceUrl = "$SampleSourceUrl/TextFieldSamples.kt"
val TextFieldsExamples =
    listOf(
            Example(
                name = "SimpleTextFieldSample",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                SimpleTextFieldSample()
            },
            Example(
                name = "TextFieldWithInitialValueAndSelection",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                TextFieldWithInitialValueAndSelection()
            },
            Example(
                name = "SimpleOutlinedTextFieldSample",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                SimpleOutlinedTextFieldSample()
            },
            Example(
                name = "OutlinedTextFieldWithInitialValueAndSelection",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                OutlinedTextFieldWithInitialValueAndSelection()
            },
            Example(
                name = "TextFieldWithTransformations",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                TextFieldWithTransformations()
            },
            Example(
                name = "TextFieldWithIcons",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                TextFieldWithIcons()
            },
            Example(
                name = "TextFieldWithPlaceholder",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                TextFieldWithPlaceholder()
            },
            Example(
                name = "TextFieldWithPrefixAndSuffix",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                TextFieldWithPrefixAndSuffix()
            },
            Example(
                name = "TextFieldWithErrorState",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                TextFieldWithErrorState()
            },
            Example(
                name = "TextFieldWithSupportingText",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                TextFieldWithSupportingText()
            },
            Example(
                name = "DenseTextFieldContentPadding",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                DenseTextFieldContentPadding()
            },
            Example(
                name = "PasswordTextField",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                PasswordTextField()
            },
            Example(
                name = "TextFieldWithHideKeyboardOnImeAction",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                TextFieldWithHideKeyboardOnImeAction()
            },
            Example(
                name = "TextArea",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl,
                isExpressive = false,
            ) {
                TextArea()
            },
        )
        .map {
            // By default text field samples are minimal and don't have a `width` modifier to
            // restrict the width. As a result, they grow horizontally if enough text is typed. To
            // prevent this behavior in Catalog app, the code below restricts the width of every
            // text field sample
            it.copy(content = { Box(Modifier.wrapContentWidth().width(280.dp)) { it.content() } })
        }

private const val TooltipsExampleDescription = "Tooltips examples"
private const val TooltipsExampleSourceUrl = "$SampleSourceUrl/TooltipSamples.kt"
val TooltipsExamples =
    listOf(
        Example(
            name = "PlainTooltipSample",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            PlainTooltipSample()
        },
        Example(
            name = "PlainTooltipWithManualInvocationSample",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            PlainTooltipWithManualInvocationSample()
        },
        Example(
            name = "PlainTooltipWithCaret",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            PlainTooltipWithCaret()
        },
        Example(
            name = "PlainTooltipWithCaretBelowAnchor",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            PlainTooltipWithCaretBelowAnchor()
        },
        Example(
            name = "PlainTooltipWithCaretLeftOfAnchor",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            PlainTooltipWithCaretLeftOfAnchor()
        },
        Example(
            name = "PlainTooltipWithCaretRightOfAnchor",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            PlainTooltipWithCaretRightOfAnchor()
        },
        Example(
            name = "PlainTooltipWithCaretStartOfAnchor",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            PlainTooltipWithCaretStartOfAnchor()
        },
        Example(
            name = "PlainTooltipWithCaretEndOfAnchor",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            PlainTooltipWithCaretEndOfAnchor()
        },
        Example(
            name = "PlainTooltipWithCustomCaret",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            PlainTooltipWithCustomCaret()
        },
        Example(
            name = "RichTooltipSample",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            RichTooltipSample()
        },
        Example(
            name = "RichTooltipWithManualInvocationSample",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            RichTooltipWithManualInvocationSample()
        },
        Example(
            name = "RichTooltipWithCaretSample",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            RichTooltipWithCaretSample()
        },
        Example(
            name = "RichTooltipWithCustomCaretSample",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl,
            isExpressive = false,
        ) {
            RichTooltipWithCustomCaretSample()
        },
    )
