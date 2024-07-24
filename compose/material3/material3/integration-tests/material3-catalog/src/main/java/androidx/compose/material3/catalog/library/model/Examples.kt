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
import androidx.compose.material3.adaptive.navigationsuite.samples.NavigationSuiteScaffoldCustomNavigationRail
import androidx.compose.material3.adaptive.navigationsuite.samples.NavigationSuiteScaffoldSample
import androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSample
import androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPane
import androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldWithNavigationSample
import androidx.compose.material3.catalog.library.util.AdaptiveNavigationSuiteSampleSourceUrl
import androidx.compose.material3.catalog.library.util.AdaptiveSampleSourceUrl
import androidx.compose.material3.catalog.library.util.SampleSourceUrl
import androidx.compose.material3.samples.AlertDialogSample
import androidx.compose.material3.samples.AlertDialogWithIconSample
import androidx.compose.material3.samples.AnimatedExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.AssistChipSample
import androidx.compose.material3.samples.BasicAlertDialogSample
import androidx.compose.material3.samples.BottomAppBarWithFAB
import androidx.compose.material3.samples.BottomSheetScaffoldNestedScrollSample
import androidx.compose.material3.samples.ButtonSample
import androidx.compose.material3.samples.ButtonWithIconSample
import androidx.compose.material3.samples.CardSample
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
import androidx.compose.material3.samples.DateInputSample
import androidx.compose.material3.samples.DatePickerDialogSample
import androidx.compose.material3.samples.DatePickerSample
import androidx.compose.material3.samples.DatePickerWithDateSelectableDatesSample
import androidx.compose.material3.samples.DateRangePickerSample
import androidx.compose.material3.samples.DenseTextFieldContentPadding
import androidx.compose.material3.samples.DeterminateContainedLoadingIndicatorSample
import androidx.compose.material3.samples.DeterminateLoadingIndicatorSample
import androidx.compose.material3.samples.DismissibleNavigationDrawerSample
import androidx.compose.material3.samples.DockedSearchBarSample
import androidx.compose.material3.samples.EditableExposedDropdownMenuSample
import androidx.compose.material3.samples.ElevatedAssistChipSample
import androidx.compose.material3.samples.ElevatedButtonSample
import androidx.compose.material3.samples.ElevatedCardSample
import androidx.compose.material3.samples.ElevatedFilterChipSample
import androidx.compose.material3.samples.ElevatedSplitButtonSample
import androidx.compose.material3.samples.ElevatedSuggestionChipSample
import androidx.compose.material3.samples.EnterAlwaysTopAppBar
import androidx.compose.material3.samples.ExitAlwaysBottomAppBar
import androidx.compose.material3.samples.ExitAlwaysBottomAppBarFixed
import androidx.compose.material3.samples.ExitAlwaysBottomAppBarSpacedAround
import androidx.compose.material3.samples.ExitAlwaysBottomAppBarSpacedBetween
import androidx.compose.material3.samples.ExitAlwaysBottomAppBarSpacedEvenly
import androidx.compose.material3.samples.ExitUntilCollapsedCenterAlignedLargeTopAppBarWithSubtitle
import androidx.compose.material3.samples.ExitUntilCollapsedCenterAlignedMediumTopAppBarWithSubtitle
import androidx.compose.material3.samples.ExitUntilCollapsedLargeTopAppBar
import androidx.compose.material3.samples.ExitUntilCollapsedMediumTopAppBar
import androidx.compose.material3.samples.ExposedDropdownMenuSample
import androidx.compose.material3.samples.ExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.ExtendedFloatingActionButtonTextSample
import androidx.compose.material3.samples.FadingHorizontalMultiBrowseCarouselSample
import androidx.compose.material3.samples.FancyIndicatorContainerTabs
import androidx.compose.material3.samples.FancyIndicatorTabs
import androidx.compose.material3.samples.FancyTabs
import androidx.compose.material3.samples.FilledIconButtonSample
import androidx.compose.material3.samples.FilledIconToggleButtonSample
import androidx.compose.material3.samples.FilledSplitButtonSample
import androidx.compose.material3.samples.FilledTonalButtonSample
import androidx.compose.material3.samples.FilledTonalIconButtonSample
import androidx.compose.material3.samples.FilledTonalIconToggleButtonSample
import androidx.compose.material3.samples.FilterChipSample
import androidx.compose.material3.samples.FilterChipWithLeadingIconSample
import androidx.compose.material3.samples.FloatingActionButtonMenuSample
import androidx.compose.material3.samples.FloatingActionButtonSample
import androidx.compose.material3.samples.HorizontalFloatingAppBar
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
import androidx.compose.material3.samples.LargeExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.LargeExtendedFloatingActionButtonTextSample
import androidx.compose.material3.samples.LargeFloatingActionButtonSample
import androidx.compose.material3.samples.LeadingIconTabs
import androidx.compose.material3.samples.LinearProgressIndicatorSample
import androidx.compose.material3.samples.LinearWavyProgressIndicatorSample
import androidx.compose.material3.samples.LoadingIndicatorPullToRefreshSample
import androidx.compose.material3.samples.LoadingIndicatorSample
import androidx.compose.material3.samples.MediumAnimatedExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.MediumExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.MediumExtendedFloatingActionButtonTextSample
import androidx.compose.material3.samples.MediumFloatingActionButtonSample
import androidx.compose.material3.samples.MenuSample
import androidx.compose.material3.samples.MenuWithScrollStateSample
import androidx.compose.material3.samples.ModalBottomSheetSample
import androidx.compose.material3.samples.ModalNavigationDrawerSample
import androidx.compose.material3.samples.MultiAutocompleteExposedDropdownMenuSample
import androidx.compose.material3.samples.NavigationBarItemWithBadge
import androidx.compose.material3.samples.NavigationBarSample
import androidx.compose.material3.samples.NavigationBarWithOnlySelectedLabelsSample
import androidx.compose.material3.samples.NavigationRailBottomAlignSample
import androidx.compose.material3.samples.NavigationRailSample
import androidx.compose.material3.samples.NavigationRailWithOnlySelectedLabelsSample
import androidx.compose.material3.samples.OneLineListItem
import androidx.compose.material3.samples.OutlinedButtonSample
import androidx.compose.material3.samples.OutlinedCardSample
import androidx.compose.material3.samples.OutlinedIconButtonSample
import androidx.compose.material3.samples.OutlinedIconToggleButtonSample
import androidx.compose.material3.samples.OutlinedSplitButtonSample
import androidx.compose.material3.samples.OutlinedTextFieldWithInitialValueAndSelection
import androidx.compose.material3.samples.PasswordTextField
import androidx.compose.material3.samples.PermanentNavigationDrawerSample
import androidx.compose.material3.samples.PinnedTopAppBar
import androidx.compose.material3.samples.PlainTooltipSample
import androidx.compose.material3.samples.PlainTooltipWithCaret
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
import androidx.compose.material3.samples.ScrollingFancyIndicatorContainerTabs
import androidx.compose.material3.samples.ScrollingPrimaryTextTabs
import androidx.compose.material3.samples.ScrollingSecondaryTextTabs
import androidx.compose.material3.samples.SearchBarSample
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
import androidx.compose.material3.samples.SimpleTextFieldSample
import androidx.compose.material3.samples.SimpleTopAppBar
import androidx.compose.material3.samples.SimpleTopAppBarWithSubtitle
import androidx.compose.material3.samples.SliderSample
import androidx.compose.material3.samples.SliderWithCustomThumbSample
import androidx.compose.material3.samples.SliderWithCustomTrackAndThumb
import androidx.compose.material3.samples.SmallAnimatedExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.SmallButtonSample
import androidx.compose.material3.samples.SmallExtendedFloatingActionButtonSample
import androidx.compose.material3.samples.SmallExtendedFloatingActionButtonTextSample
import androidx.compose.material3.samples.SmallFloatingActionButtonSample
import androidx.compose.material3.samples.SplitButtonSample
import androidx.compose.material3.samples.SplitButtonWithIconSample
import androidx.compose.material3.samples.SplitButtonWithTextSample
import androidx.compose.material3.samples.SquareButtonSample
import androidx.compose.material3.samples.StepRangeSliderSample
import androidx.compose.material3.samples.StepsSliderSample
import androidx.compose.material3.samples.SuggestionChipSample
import androidx.compose.material3.samples.SwitchSample
import androidx.compose.material3.samples.SwitchWithThumbIconSample
import androidx.compose.material3.samples.TextAndIconTabs
import androidx.compose.material3.samples.TextArea
import androidx.compose.material3.samples.TextButtonSample
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
import androidx.compose.material3.samples.TonalSplitButtonSample
import androidx.compose.material3.samples.TriStateCheckboxSample
import androidx.compose.material3.samples.TwoLineListItem
import androidx.compose.material3.samples.VerticalFloatingAppBar
import androidx.compose.material3.samples.WideNavigationRailArrangementsSample
import androidx.compose.material3.samples.WideNavigationRailCollapsedSample
import androidx.compose.material3.samples.WideNavigationRailExpandedSample
import androidx.compose.material3.samples.WideNavigationRailResponsiveSample
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Example(
    val name: String,
    val description: String,
    val sourceUrl: String,
    val content: @Composable () -> Unit
)

private const val AdaptiveExampleDescription = "Adaptive examples"
private const val AdaptiveExampleSourceUrl = "$AdaptiveSampleSourceUrl/ThreePaneScaffoldSamples.kt"
val AdaptiveExamples =
    listOf(
        Example(
            name = "ListDetailPaneScaffoldSample",
            description = AdaptiveExampleDescription,
            sourceUrl = AdaptiveExampleSourceUrl
        ) {
            ListDetailPaneScaffoldSample()
        },
        Example(
            name = "ListDetailPaneScaffoldSampleWithExtraPane",
            description = AdaptiveExampleDescription,
            sourceUrl = AdaptiveExampleSourceUrl
        ) {
            ListDetailPaneScaffoldSampleWithExtraPane()
        },
        Example(
            name = "ListDetailPaneScaffoldWithNavigationSample",
            description = AdaptiveExampleDescription,
            sourceUrl = AdaptiveExampleSourceUrl
        ) {
            ListDetailPaneScaffoldWithNavigationSample()
        }
    )

private const val BadgeExampleDescription = "Badge examples"
private const val BadgeExampleSourceUrl = "$SampleSourceUrl/BadgeSamples.kt"
val BadgeExamples =
    listOf(
        Example(
            name = "NavigationBarItemWithBadge",
            description = BadgeExampleDescription,
            sourceUrl = BadgeExampleSourceUrl
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
            sourceUrl = BottomSheetExampleSourceUrl
        ) {
            ModalBottomSheetSample()
        },
        Example(
            name = "SimpleBottomSheetScaffoldSample",
            description = BottomSheetExampleDescription,
            sourceUrl = BottomSheetExampleSourceUrl
        ) {
            SimpleBottomSheetScaffoldSample()
        },
        Example(
            name = "BottomSheetScaffoldNestedScrollSample",
            description = BottomSheetExampleDescription,
            sourceUrl = BottomSheetExampleSourceUrl
        ) {
            BottomSheetScaffoldNestedScrollSample()
        }
    )

private const val ButtonsExampleDescription = "Button examples"
private const val ButtonsExampleSourceUrl = "$SampleSourceUrl/ButtonSamples.kt"
val ButtonsExamples =
    listOf(
        Example(
            name = "ButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) {
            ButtonSample()
        },
        Example(
            name = "SquareButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) {
            SquareButtonSample()
        },
        Example(
            name = "SmallButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) {
            SmallButtonSample()
        },
        Example(
            name = "ElevatedButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) {
            ElevatedButtonSample()
        },
        Example(
            name = "FilledTonalButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) {
            FilledTonalButtonSample()
        },
        Example(
            name = "OutlinedButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) {
            OutlinedButtonSample()
        },
        Example(
            name = "TextButtonSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) {
            TextButtonSample()
        },
        Example(
            name = "ButtonWithIconSample",
            description = ButtonsExampleDescription,
            sourceUrl = ButtonsExampleSourceUrl,
        ) {
            ButtonWithIconSample()
        }
    )

private const val CardsExampleDescription = "Cards examples"
private const val CardsExampleSourceUrl = "$SampleSourceUrl/CardSamples.kt"
val CardExamples =
    listOf(
        Example(
            name = "CardSample",
            description = CardsExampleDescription,
            sourceUrl = CardsExampleSourceUrl
        ) {
            CardSample()
        },
        Example(
            name = "ClickableCardSample",
            description = CardsExampleDescription,
            sourceUrl = CardsExampleSourceUrl
        ) {
            ClickableCardSample()
        },
        Example(
            name = "ElevatedCardSample",
            description = CardsExampleDescription,
            sourceUrl = CardsExampleSourceUrl
        ) {
            ElevatedCardSample()
        },
        Example(
            name = "ClickableElevatedCardSample",
            description = CardsExampleDescription,
            sourceUrl = CardsExampleSourceUrl
        ) {
            ClickableElevatedCardSample()
        },
        Example(
            name = "OutlinedCardSample",
            description = CardsExampleDescription,
            sourceUrl = CardsExampleSourceUrl
        ) {
            OutlinedCardSample()
        },
        Example(
            name = "ClickableOutlinedCardSample",
            description = CardsExampleDescription,
            sourceUrl = CardsExampleSourceUrl
        ) {
            ClickableOutlinedCardSample()
        }
    )

private const val CarouselExampleDescription = "Carousel examples"
private const val CarouselExampleSourceUrl = "$SampleSourceUrl/CarouselSamples.kt"
val CarouselExamples =
    listOf(
        Example(
            name = "HorizontalMultiBrowseCarouselSample",
            description = CarouselExampleDescription,
            sourceUrl = CarouselExampleSourceUrl
        ) {
            HorizontalMultiBrowseCarouselSample()
        },
        Example(
            name = "HorizontalUncontainedCarouselSample",
            description = CarouselExampleDescription,
            sourceUrl = CarouselExampleSourceUrl
        ) {
            HorizontalUncontainedCarouselSample()
        },
        Example(
            name = "FadingHorizontalMultiBrowseCarouselSample",
            description = CarouselExampleDescription,
            sourceUrl = CarouselExampleSourceUrl
        ) {
            FadingHorizontalMultiBrowseCarouselSample()
        }
    )

private const val CheckboxesExampleDescription = "Checkboxes examples"
private const val CheckboxesExampleSourceUrl = "$SampleSourceUrl/CheckboxSamples.kt"
val CheckboxesExamples =
    listOf(
        Example(
            name = "CheckboxSample",
            description = CheckboxesExampleDescription,
            sourceUrl = CheckboxesExampleSourceUrl
        ) {
            CheckboxSample()
        },
        Example(
            name = "CheckboxWithTextSample",
            description = CheckboxesExampleDescription,
            sourceUrl = CheckboxesExampleSourceUrl
        ) {
            CheckboxWithTextSample()
        },
        Example(
            name = "TriStateCheckboxSample",
            description = CheckboxesExampleDescription,
            sourceUrl = CheckboxesExampleSourceUrl
        ) {
            TriStateCheckboxSample()
        }
    )

private const val ChipsExampleDescription = "Chips examples"
private const val ChipsExampleSourceUrl = "$SampleSourceUrl/ChipSamples.kt"
val ChipsExamples =
    listOf(
        Example(
            name = "AssistChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl
        ) {
            AssistChipSample()
        },
        Example(
            name = "ElevatedAssistChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl
        ) {
            ElevatedAssistChipSample()
        },
        Example(
            name = "FilterChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl
        ) {
            FilterChipSample()
        },
        Example(
            name = "ElevatedFilterChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl
        ) {
            ElevatedFilterChipSample()
        },
        Example(
            name = "FilterChipWithLeadingIconSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl
        ) {
            FilterChipWithLeadingIconSample()
        },
        Example(
            name = "InputChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl
        ) {
            InputChipSample()
        },
        Example(
            name = "InputChipWithAvatarSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl
        ) {
            InputChipWithAvatarSample()
        },
        Example(
            name = "SuggestionChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl
        ) {
            SuggestionChipSample()
        },
        Example(
            name = "ElevatedSuggestionChipSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl
        ) {
            ElevatedSuggestionChipSample()
        },
        Example(
            name = "ChipGroupSingleLineSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl
        ) {
            ChipGroupSingleLineSample()
        },
        Example(
            name = "ChipGroupReflowSample",
            description = ChipsExampleDescription,
            sourceUrl = ChipsExampleSourceUrl
        ) {
            ChipGroupReflowSample()
        }
    )

private const val DatePickerExampleDescription = "Date picker examples"
private const val DatePickerExampleSourceUrl = "$SampleSourceUrl/DatePickerSamples.kt"
val DatePickerExamples =
    listOf(
        Example(
            name = "DatePickerSample",
            description = DatePickerExampleDescription,
            sourceUrl = DatePickerExampleSourceUrl
        ) {
            DatePickerSample()
        },
        Example(
            name = "DatePickerDialogSample",
            description = DatePickerExampleDescription,
            sourceUrl = DatePickerExampleSourceUrl
        ) {
            DatePickerDialogSample()
        },
        Example(
            name = "DatePickerWithDateSelectableDatesSample",
            description = DatePickerExampleDescription,
            sourceUrl = DatePickerExampleSourceUrl
        ) {
            DatePickerWithDateSelectableDatesSample()
        },
        Example(
            name = "DateInputSample",
            description = DatePickerExampleDescription,
            sourceUrl = DatePickerExampleSourceUrl
        ) {
            DateInputSample()
        },
        Example(
            name = "DateRangePickerSample",
            description = DatePickerExampleDescription,
            sourceUrl = DatePickerExampleSourceUrl
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
        ) {
            AlertDialogSample()
        },
        Example(
            name = "AlertDialogWithIconSample",
            description = DialogExampleDescription,
            sourceUrl = DialogExampleSourceUrl,
        ) {
            AlertDialogWithIconSample()
        },
        Example(
            name = "BasicAlertDialogSample",
            description = DialogExampleDescription,
            sourceUrl = DialogExampleSourceUrl,
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
        ) {
            SimpleBottomAppBar()
        },
        Example(
            name = "BottomAppBarWithFAB",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
        ) {
            BottomAppBarWithFAB()
        },
        Example(
            name = "ExitAlwaysBottomAppBar",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
        ) {
            ExitAlwaysBottomAppBar()
        },
        Example(
            name = "ExitAlwaysBottomAppBarSpacedAround",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
        ) {
            ExitAlwaysBottomAppBarSpacedAround()
        },
        Example(
            name = "ExitAlwaysBottomAppBarSpacedBetween",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
        ) {
            ExitAlwaysBottomAppBarSpacedBetween()
        },
        Example(
            name = "ExitAlwaysBottomAppBarSpacedEvenly",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
        ) {
            ExitAlwaysBottomAppBarSpacedEvenly()
        },
        Example(
            name = "ExitAlwaysBottomAppBarFixed",
            description = BottomAppBarsExampleDescription,
            sourceUrl = BottomAppBarsExampleSourceUrl,
        ) {
            ExitAlwaysBottomAppBarFixed()
        }
    )

private const val TopAppBarExampleDescription = "Top app bar examples"
private const val TopAppBarExampleSourceUrl = "$SampleSourceUrl/AppBarSamples.kt"
val TopAppBarExamples =
    listOf(
        Example(
            name = "SimpleTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) {
            SimpleTopAppBar()
        },
        Example(
            name = "SimpleTopAppBarWithSubtitle",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) {
            SimpleTopAppBarWithSubtitle()
        },
        Example(
            name = "SimpleCenterAlignedTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) {
            SimpleCenterAlignedTopAppBar()
        },
        Example(
            name = "SimpleCenterAlignedTopAppBarWithSubtitle",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) {
            SimpleCenterAlignedTopAppBarWithSubtitle()
        },
        Example(
            name = "PinnedTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) {
            PinnedTopAppBar()
        },
        Example(
            name = "EnterAlwaysTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) {
            EnterAlwaysTopAppBar()
        },
        Example(
            name = "ExitUntilCollapsedMediumTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) {
            ExitUntilCollapsedMediumTopAppBar()
        },
        Example(
            name = "ExitUntilCollapsedCenterAlignedMediumTopAppBarWithSubtitle",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) {
            ExitUntilCollapsedCenterAlignedMediumTopAppBarWithSubtitle()
        },
        Example(
            name = "ExitUntilCollapsedLargeTopAppBar",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) {
            ExitUntilCollapsedLargeTopAppBar()
        },
        Example(
            name = "ExitUntilCollapsedCenterAlignedLargeTopAppBarWithSubtitle",
            description = TopAppBarExampleDescription,
            sourceUrl = TopAppBarExampleSourceUrl,
        ) {
            ExitUntilCollapsedCenterAlignedLargeTopAppBarWithSubtitle()
        },
    )

private const val FloatingAppBarsExampleDescription = "Floating app bar examples"
private const val FloatingAppBarsExampleSourceUrl = "$SampleSourceUrl/FloatingAppBarSamples.kt"

val FloatingAppBarsExamples =
    listOf(
        Example(
            name = "HorizontalFloatingAppBar",
            description = FloatingAppBarsExampleDescription,
            sourceUrl = FloatingAppBarsExampleSourceUrl,
        ) {
            HorizontalFloatingAppBar()
        },
        Example(
            name = "VerticalFloatingAppBar",
            description = FloatingAppBarsExampleDescription,
            sourceUrl = FloatingAppBarsExampleSourceUrl,
        ) {
            VerticalFloatingAppBar()
        }
    )

private const val ExtendedFABExampleDescription = "Extended FAB examples"
private const val ExtendedFABExampleSourceUrl = "$SampleSourceUrl/FloatingActionButtonSamples.kt"
val ExtendedFABExamples =
    listOf(
        Example(
            name = "ExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) {
            ExtendedFloatingActionButtonSample()
        },
        Example(
            name = "SmallExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) {
            SmallExtendedFloatingActionButtonSample()
        },
        Example(
            name = "MediumExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) {
            MediumExtendedFloatingActionButtonSample()
        },
        Example(
            name = "LargeExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) {
            LargeExtendedFloatingActionButtonSample()
        },
        Example(
            name = "ExtendedFloatingActionButtonTextSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) {
            ExtendedFloatingActionButtonTextSample()
        },
        Example(
            name = "SmallExtendedFloatingActionButtonTextSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) {
            SmallExtendedFloatingActionButtonTextSample()
        },
        Example(
            name = "MediumExtendedFloatingActionButtonTextSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) {
            MediumExtendedFloatingActionButtonTextSample()
        },
        Example(
            name = "LargeExtendedFloatingActionButtonTextSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) {
            LargeExtendedFloatingActionButtonTextSample()
        },
        Example(
            name = "AnimatedExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) {
            AnimatedExtendedFloatingActionButtonSample()
        },
        Example(
            name = "SmallAnimatedExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) {
            SmallAnimatedExtendedFloatingActionButtonSample()
        },
        Example(
            name = "MediumAnimatedExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
        ) {
            MediumAnimatedExtendedFloatingActionButtonSample()
        },
        Example(
            name = "LargeAnimatedExtendedFloatingActionButtonSample",
            description = ExtendedFABExampleDescription,
            sourceUrl = ExtendedFABExampleSourceUrl,
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
        ) {
            FloatingActionButtonSample()
        },
        Example(
            name = "LargeFloatingActionButtonSample",
            description = FloatingActionButtonsExampleDescription,
            sourceUrl = FloatingActionButtonsExampleSourceUrl,
        ) {
            LargeFloatingActionButtonSample()
        },
        Example(
            name = "MediumFloatingActionButtonSample",
            description = FloatingActionButtonsExampleDescription,
            sourceUrl = FloatingActionButtonsExampleSourceUrl,
        ) {
            MediumFloatingActionButtonSample()
        },
        Example(
            name = "SmallFloatingActionButtonSample",
            description = FloatingActionButtonsExampleDescription,
            sourceUrl = FloatingActionButtonsExampleSourceUrl,
        ) {
            SmallFloatingActionButtonSample()
        }
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
        ) {
            FloatingActionButtonMenuSample()
        },
    )

private const val ListsExampleDescription = "List examples"
private const val ListsExampleSourceUrl = "$SampleSourceUrl/ListSamples.kt"
val ListsExamples =
    listOf(
        Example(
            name = "OneLineListItem",
            description = ListsExampleDescription,
            sourceUrl = ListsExampleSourceUrl
        ) {
            OneLineListItem()
        },
        Example(
            name = "TwoLineListItem",
            description = ListsExampleDescription,
            sourceUrl = ListsExampleSourceUrl
        ) {
            TwoLineListItem()
        },
        Example(
            name = "ThreeLineListItemWithOverlineAndSupporting",
            description = ListsExampleDescription,
            sourceUrl = ListsExampleSourceUrl
        ) {
            ThreeLineListItemWithOverlineAndSupporting()
        },
        Example(
            name = "ThreeLineListItemWithExtendedSupporting",
            description = ListsExampleDescription,
            sourceUrl = ListsExampleSourceUrl
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
        ) {
            IconButtonSample()
        },
        Example(
            name = "TintedIconButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) {
            TintedIconButtonSample()
        },
        Example(
            name = "IconToggleButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) {
            IconToggleButtonSample()
        },
        Example(
            name = "FilledIconButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) {
            FilledIconButtonSample()
        },
        Example(
            name = "FilledIconToggleButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) {
            FilledIconToggleButtonSample()
        },
        Example(
            name = "FilledTonalIconButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) {
            FilledTonalIconButtonSample()
        },
        Example(
            name = "FilledTonalIconToggleButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) {
            FilledTonalIconToggleButtonSample()
        },
        Example(
            name = "OutlinedIconButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) {
            OutlinedIconButtonSample()
        },
        Example(
            name = "OutlinedIconToggleButtonSample",
            description = IconButtonExampleDescription,
            sourceUrl = IconButtonExampleSourceUrl,
        ) {
            OutlinedIconToggleButtonSample()
        }
    )

private const val LoadingIndicatorsExampleDescription = "Loading indicators examples"
private const val LoadingIndicatorsExampleSourceUrl =
    "$SampleSourceUrl/" + "LoadingIndicatorSamples.kt"
val LoadingIndicatorsExamples =
    listOf(
        Example(
            name = "LoadingIndicatorSample",
            description = LoadingIndicatorsExampleDescription,
            sourceUrl = LoadingIndicatorsExampleSourceUrl
        ) {
            LoadingIndicatorSample()
        },
        Example(
            name = "ContainedLoadingIndicatorSample",
            description = LoadingIndicatorsExampleDescription,
            sourceUrl = LoadingIndicatorsExampleSourceUrl
        ) {
            ContainedLoadingIndicatorSample()
        },
        Example(
            name = "DeterminateLoadingIndicatorSample",
            description = LoadingIndicatorsExampleDescription,
            sourceUrl = LoadingIndicatorsExampleSourceUrl
        ) {
            DeterminateLoadingIndicatorSample()
        },
        Example(
            name = "DeterminateContainedLoadingIndicatorSample",
            description = LoadingIndicatorsExampleDescription,
            sourceUrl = LoadingIndicatorsExampleSourceUrl
        ) {
            DeterminateContainedLoadingIndicatorSample()
        },
        Example(
            name = "LoadingIndicatorPullToRefreshSample",
            description = LoadingIndicatorsExampleDescription,
            sourceUrl = LoadingIndicatorsExampleSourceUrl
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
            sourceUrl = MenusExampleSourceUrl
        ) {
            MenuSample()
        },
        Example(
            name = "MenuWithScrollStateSample",
            description = MenusExampleDescription,
            sourceUrl = MenusExampleSourceUrl
        ) {
            MenuWithScrollStateSample()
        },
        Example(
            name = "ExposedDropdownMenuSample",
            description = MenusExampleDescription,
            sourceUrl = MenusExampleSourceUrl
        ) {
            ExposedDropdownMenuSample()
        },
        Example(
            name = "EditableExposedDropdownMenuSample",
            description = MenusExampleDescription,
            sourceUrl = MenusExampleSourceUrl
        ) {
            EditableExposedDropdownMenuSample()
        },
        Example(
            name = "MultiAutocompleteExposedDropdownMenuSample",
            description = MenusExampleDescription,
            sourceUrl = MenusExampleSourceUrl
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
        ) {
            ShortNavigationBarSample()
        },
        Example(
            name = "ShortNavigationBarWithHorizontalItemsSample",
            description = NavigationBarExampleDescription,
            sourceUrl = NavigationBarExampleSourceUrl,
        ) {
            ShortNavigationBarWithHorizontalItemsSample()
        },
        Example(
            name = "NavigationBarSample",
            description = NavigationBarExampleDescription,
            sourceUrl = NavigationBarExampleSourceUrl,
        ) {
            NavigationBarSample()
        },
        Example(
            name = "NavigationBarWithOnlySelectedLabelsSample",
            description = NavigationBarExampleDescription,
            sourceUrl = NavigationBarExampleSourceUrl,
        ) {
            NavigationBarWithOnlySelectedLabelsSample()
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
        ) {
            WideNavigationRailResponsiveSample()
        },
        Example(
            name = "WideNavigationRailCollapsedSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
        ) {
            WideNavigationRailCollapsedSample()
        },
        Example(
            name = "WideNavigationRailExpandedSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
        ) {
            WideNavigationRailExpandedSample()
        },
        Example(
            name = "WideNavigationRailArrangementsSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
        ) {
            WideNavigationRailArrangementsSample()
        },
        Example(
            name = "NavigationRailSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
        ) {
            NavigationRailSample()
        },
        Example(
            name = "NavigationRailWithOnlySelectedLabelsSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
        ) {
            NavigationRailWithOnlySelectedLabelsSample()
        },
        Example(
            name = "NavigationRailBottomAlignSample",
            description = NavigationRailExampleDescription,
            sourceUrl = NavigationRailExampleSourceUrl,
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
            sourceUrl = NavigationDrawerExampleSourceUrl
        ) {
            ModalNavigationDrawerSample()
        },
        Example(
            name = "PermanentNavigationDrawerSample",
            description = NavigationDrawerExampleDescription,
            sourceUrl = NavigationDrawerExampleSourceUrl
        ) {
            PermanentNavigationDrawerSample()
        },
        Example(
            name = "DismissibleNavigationDrawerSample",
            description = NavigationDrawerExampleDescription,
            sourceUrl = NavigationDrawerExampleSourceUrl
        ) {
            DismissibleNavigationDrawerSample()
        }
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
        ) {
            NavigationSuiteScaffoldSample()
        },
        Example(
            name = "NavigationSuiteScaffoldCustomConfigSample",
            description = NavigationSuiteScaffoldExampleDescription,
            sourceUrl = NavigationSuiteScaffoldExampleSourceUrl,
        ) {
            NavigationSuiteScaffoldCustomConfigSample()
        },
        Example(
            name = "NavigationSuiteScaffoldCustomNavigationRail",
            description = NavigationSuiteScaffoldExampleDescription,
            sourceUrl = NavigationSuiteScaffoldExampleSourceUrl,
        ) {
            NavigationSuiteScaffoldCustomNavigationRail()
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
            sourceUrl = ProgressIndicatorsExampleSourceUrl
        ) {
            LinearProgressIndicatorSample()
        },
        Example(
            name = "LinearWavyProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl
        ) {
            LinearWavyProgressIndicatorSample()
        },
        Example(
            name = "IndeterminateLinearProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl
        ) {
            IndeterminateLinearProgressIndicatorSample()
        },
        Example(
            name = "IndeterminateLinearWavyProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl
        ) {
            IndeterminateLinearWavyProgressIndicatorSample()
        },
        Example(
            name = "CircularProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl
        ) {
            CircularProgressIndicatorSample()
        },
        Example(
            name = "CircularWavyProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl
        ) {
            CircularWavyProgressIndicatorSample()
        },
        Example(
            name = "IndeterminateCircularProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl
        ) {
            IndeterminateCircularProgressIndicatorSample()
        },
        Example(
            name = "IndeterminateCircularWavyProgressIndicatorSample",
            description = ProgressIndicatorsExampleDescription,
            sourceUrl = ProgressIndicatorsExampleSourceUrl
        ) {
            IndeterminateCircularWavyProgressIndicatorSample()
        }
    )

private const val PullToRefreshExampleDescription = "Pull-to-refresh examples"
private const val PullToRefreshExampleSourceUrl = "$SampleSourceUrl/PullToRefreshSamples.kt"
val PullToRefreshExamples =
    listOf(
        Example(
            name = "PullToRefreshSample",
            description = PullToRefreshExampleDescription,
            sourceUrl = PullToRefreshExampleSourceUrl
        ) {
            PullToRefreshSample()
        },
        Example(
            name = "PullToRefreshWithLoadingIndicatorSample",
            description = PullToRefreshExampleDescription,
            sourceUrl = PullToRefreshExampleSourceUrl
        ) {
            PullToRefreshWithLoadingIndicatorSample()
        },
        Example(
            name = "PullToRefreshScalingSample",
            description = PullToRefreshExampleDescription,
            sourceUrl = PullToRefreshExampleSourceUrl
        ) {
            PullToRefreshScalingSample()
        },
        Example(
            name = "PullToRefreshSampleCustomState",
            description = PullToRefreshExampleDescription,
            sourceUrl = PullToRefreshExampleSourceUrl
        ) {
            PullToRefreshSampleCustomState()
        },
        Example(
            name = "PullToRefreshViewModelSample",
            description = PullToRefreshExampleDescription,
            sourceUrl = PullToRefreshExampleSourceUrl
        ) {
            PullToRefreshViewModelSample()
        },
        Example(
            name = "PullToRefreshViewModelSample",
            description = PullToRefreshExampleDescription,
            sourceUrl = PullToRefreshExampleSourceUrl
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
            sourceUrl = RadioButtonsExampleSourceUrl
        ) {
            RadioButtonSample()
        },
        Example(
            name = "RadioGroupSample",
            description = RadioButtonsExampleDescription,
            sourceUrl = RadioButtonsExampleSourceUrl
        ) {
            RadioGroupSample()
        },
    )

private const val SearchBarExampleDescription = "Search bar examples"
private const val SearchBarExampleSourceUrl = "$SampleSourceUrl/SearchBarSamples.kt"
val SearchBarExamples =
    listOf(
        Example(
            name = "SearchBarSample",
            description = SearchBarExampleDescription,
            sourceUrl = SearchBarExampleSourceUrl
        ) {
            SearchBarSample()
        },
        Example(
            name = "DockedSearchBarSample",
            description = SearchBarExampleDescription,
            sourceUrl = SearchBarExampleSourceUrl
        ) {
            DockedSearchBarSample()
        }
    )

private const val SegmentedButtonExampleDescription = "Segmented Button examples"
private const val SegmentedButtonSourceUrl = "$SampleSourceUrl/SegmentedButtonSamples.kt"
val SegmentedButtonExamples =
    listOf(
        Example(
            name = "SegmentedButtonSingleSelectSample",
            description = SegmentedButtonExampleDescription,
            sourceUrl = SegmentedButtonSourceUrl
        ) {
            SegmentedButtonSingleSelectSample()
        },
        Example(
            name = "SegmentedButtonMultiSelectSample",
            description = SegmentedButtonExampleDescription,
            sourceUrl = SegmentedButtonSourceUrl
        ) {
            SegmentedButtonMultiSelectSample()
        },
    )

private const val SlidersExampleDescription = "Sliders examples"
private const val SlidersExampleSourceUrl = "$SampleSourceUrl/SliderSamples.kt"
val SlidersExamples =
    listOf(
        Example(
            name = "SliderSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl
        ) {
            SliderSample()
        },
        Example(
            name = "StepsSliderSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl
        ) {
            StepsSliderSample()
        },
        Example(
            name = "SliderWithCustomThumbSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl
        ) {
            SliderWithCustomThumbSample()
        },
        Example(
            name = "SliderWithCustomTrackAndThumb",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl
        ) {
            SliderWithCustomTrackAndThumb()
        },
        Example(
            name = "RangeSliderSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl
        ) {
            RangeSliderSample()
        },
        Example(
            name = "StepRangeSliderSample",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl
        ) {
            StepRangeSliderSample()
        },
        Example(
            name = "RangeSliderWithCustomComponents",
            description = SlidersExampleDescription,
            sourceUrl = SlidersExampleSourceUrl
        ) {
            RangeSliderWithCustomComponents()
        }
    )

private const val SnackbarsExampleDescription = "Snackbars examples"
private const val SnackbarsExampleSourceUrl = "$SampleSourceUrl/ScaffoldSamples.kt"
val SnackbarsExamples =
    listOf(
        Example(
            name = "ScaffoldWithSimpleSnackbar",
            description = SnackbarsExampleDescription,
            sourceUrl = SnackbarsExampleSourceUrl
        ) {
            ScaffoldWithSimpleSnackbar()
        },
        Example(
            name = "ScaffoldWithIndefiniteSnackbar",
            description = SnackbarsExampleDescription,
            sourceUrl = SnackbarsExampleSourceUrl
        ) {
            ScaffoldWithIndefiniteSnackbar()
        },
        Example(
            name = "ScaffoldWithCustomSnackbar",
            description = SnackbarsExampleDescription,
            sourceUrl = SnackbarsExampleSourceUrl
        ) {
            ScaffoldWithCustomSnackbar()
        },
        Example(
            name = "ScaffoldWithCoroutinesSnackbar",
            description = SnackbarsExampleDescription,
            sourceUrl = SnackbarsExampleSourceUrl
        ) {
            ScaffoldWithCoroutinesSnackbar()
        },
        Example(
            name = "ScaffoldWithMultilineSnackbar",
            description = SnackbarsExampleDescription,
            sourceUrl = SnackbarsExampleSourceUrl
        ) {
            ScaffoldWithMultilineSnackbar()
        }
    )

private const val SplitButtonExampleDescription = "Split Button examples"
private const val SplitButtonSourceUrl = "$SampleSourceUrl/SplitButtonSamples.kt"
val SplitButtonExamples =
    listOf(
        Example(
            name = "SplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl
        ) {
            SplitButtonSample()
        },
        Example(
            name = "FilledSplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl
        ) {
            FilledSplitButtonSample()
        },
        Example(
            name = "TonalSplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl
        ) {
            TonalSplitButtonSample()
        },
        Example(
            name = "ElevatedSplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl
        ) {
            ElevatedSplitButtonSample()
        },
        Example(
            name = "OutlinedSplitButtonSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl
        ) {
            OutlinedSplitButtonSample()
        },
        Example(
            name = "SplitButtonWithTextSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl
        ) {
            SplitButtonWithTextSample()
        },
        Example(
            name = "SplitButtonWithIconSample",
            description = SplitButtonExampleDescription,
            sourceUrl = SplitButtonSourceUrl
        ) {
            SplitButtonWithIconSample()
        },
    )

private const val SwitchExampleDescription = "Switch examples"
private const val SwitchExampleSourceUrl = "$SampleSourceUrl/SwitchSamples.kt"
val SwitchExamples =
    listOf(
        Example(
            name = "SwitchSample",
            description = SwitchExampleDescription,
            sourceUrl = SwitchExampleSourceUrl
        ) {
            SwitchSample()
        },
        Example(
            name = "SwitchWithThumbIconSample",
            description = SwitchExampleDescription,
            sourceUrl = SwitchExampleSourceUrl
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
            sourceUrl = TabsExampleSourceUrl
        ) {
            PrimaryTextTabs()
        },
        Example(
            name = "PrimaryIconTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl
        ) {
            PrimaryIconTabs()
        },
        Example(
            name = "SecondaryTextTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl
        ) {
            SecondaryTextTabs()
        },
        Example(
            name = "SecondaryIconTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl
        ) {
            SecondaryIconTabs()
        },
        Example(
            name = "TextAndIconTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl
        ) {
            TextAndIconTabs()
        },
        Example(
            name = "LeadingIconTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl
        ) {
            LeadingIconTabs()
        },
        Example(
            name = "ScrollingPrimaryTextTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl
        ) {
            ScrollingPrimaryTextTabs()
        },
        Example(
            name = "ScrollingSecondaryTextTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl
        ) {
            ScrollingSecondaryTextTabs()
        },
        Example(
            name = "FancyTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl
        ) {
            FancyTabs()
        },
        Example(
            name = "FancyIndicatorTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl
        ) {
            FancyIndicatorTabs()
        },
        Example(
            name = "FancyIndicatorContainerTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl
        ) {
            FancyIndicatorContainerTabs()
        },
        Example(
            name = "ScrollingFancyIndicatorContainerTabs",
            description = TabsExampleDescription,
            sourceUrl = TabsExampleSourceUrl
        ) {
            ScrollingFancyIndicatorContainerTabs()
        }
    )

private const val TimePickerDescription = "Time Picker examples"
private const val TimePickerSourceUrl = "$SampleSourceUrl/TimePicker.kt"
val TimePickerExamples =
    listOf(
        Example(
            name = "TimePickerSample",
            description = TimePickerDescription,
            sourceUrl = TimePickerSourceUrl
        ) {
            TimePickerSample()
        },
        Example(
            name = "TimeInputSample",
            description = TimePickerDescription,
            sourceUrl = TimePickerSourceUrl
        ) {
            TimeInputSample()
        },
        Example(
            name = "TimePickerSwitchableSample",
            description = TimePickerDescription,
            sourceUrl = TimePickerSourceUrl
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
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                SimpleTextFieldSample()
            },
            Example(
                name = "TextFieldWithInitialValueAndSelection",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                TextFieldWithInitialValueAndSelection()
            },
            Example(
                name = "SimpleOutlinedTextFieldSample",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                SimpleOutlinedTextFieldSample()
            },
            Example(
                name = "OutlinedTextFieldWithInitialValueAndSelection",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                OutlinedTextFieldWithInitialValueAndSelection()
            },
            Example(
                name = "TextFieldWithTransformations",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                TextFieldWithTransformations()
            },
            Example(
                name = "TextFieldWithIcons",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                TextFieldWithIcons()
            },
            Example(
                name = "TextFieldWithPlaceholder",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                TextFieldWithPlaceholder()
            },
            Example(
                name = "TextFieldWithPrefixAndSuffix",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                TextFieldWithPrefixAndSuffix()
            },
            Example(
                name = "TextFieldWithErrorState",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                TextFieldWithErrorState()
            },
            Example(
                name = "TextFieldWithSupportingText",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                TextFieldWithSupportingText()
            },
            Example(
                name = "DenseTextFieldContentPadding",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                DenseTextFieldContentPadding()
            },
            Example(
                name = "PasswordTextField",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                PasswordTextField()
            },
            Example(
                name = "TextFieldWithHideKeyboardOnImeAction",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                TextFieldWithHideKeyboardOnImeAction()
            },
            Example(
                name = "TextArea",
                description = TextFieldsExampleDescription,
                sourceUrl = TextFieldsExampleSourceUrl
            ) {
                TextArea()
            }
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
            sourceUrl = TooltipsExampleSourceUrl
        ) {
            PlainTooltipSample()
        },
        Example(
            name = "PlainTooltipWithManualInvocationSample",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl
        ) {
            PlainTooltipWithManualInvocationSample()
        },
        Example(
            name = "PlainTooltipWithCaret",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl
        ) {
            PlainTooltipWithCaret()
        },
        Example(
            name = "PlainTooltipWithCustomCaret",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl
        ) {
            PlainTooltipWithCustomCaret()
        },
        Example(
            name = "RichTooltipSample",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl
        ) {
            RichTooltipSample()
        },
        Example(
            name = "RichTooltipWithManualInvocationSample",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl
        ) {
            RichTooltipWithManualInvocationSample()
        },
        Example(
            name = "RichTooltipWithCaretSample",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl
        ) {
            RichTooltipWithCaretSample()
        },
        Example(
            name = "RichTooltipWithCustomCaretSample",
            description = TooltipsExampleDescription,
            sourceUrl = TooltipsExampleSourceUrl
        ) {
            RichTooltipWithCustomCaretSample()
        }
    )
