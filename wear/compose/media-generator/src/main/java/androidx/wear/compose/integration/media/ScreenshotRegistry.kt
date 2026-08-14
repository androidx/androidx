/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.integration.media

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.samples.*

/**
 * Registry mapping sample names (strings) to their respective @Composable sample functions for
 * static screenshot capture in TransformingLazyColumn (TLC) container layout.
 */
val tlcScreenshotRegistry: Map<String, @Composable () -> Unit> =
    mapOf(
        "AppCardSample" to { AppCardSample() },
        "AppCardWithIconSample" to { AppCardWithIconSample() },
        "AppCardWithImageSample" to { AppCardWithImageSample() },
        "ButtonExtraLargeIconSample" to { ButtonExtraLargeIconSample() },
        "ButtonLargeIconSample" to { ButtonLargeIconSample() },
        "ButtonSample" to { ButtonSample() },
        "ButtonWithImageSample" to { ButtonWithImageSample() },
        "CardFillContentSample" to { CardFillContentSample() },
        "CardSample" to { CardSample() },
        "ChangedSliderSample" to { ChangedSliderSample() },
        "CheckboxButtonSample" to { CheckboxButtonSample() },
        "ChildButtonSample" to { ChildButtonSample() },
        "CompactButtonSample" to { CompactButtonSample() },
        "FilledIconButtonSample" to { FilledIconButtonSample() },
        "FilledTextButtonSample" to { FilledTextButtonSample() },
        "FilledTonalButtonSample" to { FilledTonalButtonSample() },
        "FilledTonalCompactButtonSample" to { FilledTonalCompactButtonSample() },
        "FilledTonalIconButtonSample" to { FilledTonalIconButtonSample() },
        "FilledTonalTextButtonSample" to { FilledTonalTextButtonSample() },
        "FilledVariantButtonSample" to { FilledVariantButtonSample() },
        "FilledVariantIconButtonSample" to { FilledVariantIconButtonSample() },
        "FilledVariantTextButtonSample" to { FilledVariantTextButtonSample() },
        "IconButtonSample" to { IconButtonSample() },
        "IconButtonWithImageSample" to { IconButtonWithImageSample() },
        "ImageCardSample" to { ImageCardSample() },
        "LargeFilledTonalTextButtonSample" to { LargeFilledTonalTextButtonSample() },
        "LevelIndicatorSample" to { LevelIndicatorSample() },
        "MediaButtonProgressIndicatorSample" to { MediaButtonProgressIndicatorSample() },
        "NonClickableAppCardSample" to { NonClickableAppCardSample() },
        "NonClickableCardSample" to { NonClickableCardSample() },
        "NonClickableImageCardSample" to { NonClickableImageCardSample() },
        "NonClickableOutlinedCardSample" to { NonClickableOutlinedCardSample() },
        "NonClickableTitleCardSample" to { NonClickableTitleCardSample() },
        "NonClickableTitleCardWithImageWithTimeAndTitleSample" to
            {
                NonClickableTitleCardWithImageWithTimeAndTitleSample()
            },
        "OutlinedAppCardSample" to { OutlinedAppCardSample() },
        "OutlinedButtonSample" to { OutlinedButtonSample() },
        "OutlinedCardSample" to { OutlinedCardSample() },
        "OutlinedCompactButtonSample" to { OutlinedCompactButtonSample() },
        "OutlinedIconButtonSample" to { OutlinedIconButtonSample() },
        "OutlinedTextButtonSample" to { OutlinedTextButtonSample() },
        "OutlinedTitleCardSample" to { OutlinedTitleCardSample() },
        "RadioButtonSample" to { RadioButtonSample() },
        "SimpleButtonSample" to { SimpleButtonSample() },
        "SimpleChildButtonSample" to { SimpleChildButtonSample() },
        "SimpleFilledTonalButtonSample" to { SimpleFilledTonalButtonSample() },
        "SimpleFilledVariantButtonSample" to { SimpleFilledVariantButtonSample() },
        "SimpleOutlinedButtonSample" to { SimpleOutlinedButtonSample() },
        "SliderSample" to { SliderSample() },
        "SliderSegmentedSample" to { SliderSegmentedSample() },
        "SliderWithIntegerSample" to { SliderWithIntegerSample() },
        "SplitCheckboxButtonSample" to { SplitCheckboxButtonSample() },
        "SplitRadioButtonSample" to { SplitRadioButtonSample() },
        "SplitSwitchButtonSample" to { SplitSwitchButtonSample() },
        "SwitchButtonSample" to { SwitchButtonSample() },
        "TextButtonSample" to { TextButtonSample() },
        "TitleCardSample" to { TitleCardSample() },
        "TitleCardWithImageWithTimeAndTitleSample" to
            {
                TitleCardWithImageWithTimeAndTitleSample()
            },
        "TitleCardWithMultipleImagesSample" to { TitleCardWithMultipleImagesSample() },
        "TitleCardWithSubtitleAndTimeSample" to { TitleCardWithSubtitleAndTimeSample() },
    )

/**
 * Registry mapping sample names (strings) to their respective @Composable sample functions for
 * static screenshot capture in full-screen Box container layout.
 */
val boxScreenshotRegistry: Map<String, @Composable () -> Unit> =
    mapOf(
        "AutoCenteringPickerGroup" to { AutoCenteringPickerGroup() },
        "DatePickerFutureOnlySample" to { DatePickerFutureOnlySample() },
        "DatePickerSample" to { DatePickerSample() },
        "DatePickerYearMonthDaySample" to { DatePickerYearMonthDaySample() },
        "PickerGroupSample" to { PickerGroupSample() },
        "SimplePicker" to { SimplePicker() },
        "StepperSample" to { StepperSample() },
        "StepperWithButtonSample" to { StepperWithButtonSample() },
        "StepperWithIntegerSample" to { StepperWithIntegerSample() },
        "TimePickerSample" to { TimePickerSample() },
        "TimePickerWith12HourClockSample" to { TimePickerWith12HourClockSample() },
        "TimePickerWithMinutesAndSecondsSample" to { TimePickerWithMinutesAndSecondsSample() },
        "TimePickerWithSecondsSample" to { TimePickerWithSecondsSample() },
        "ScaffoldSample" to { ScaffoldSample() },
        "FullScreenProgressIndicatorSample" to { FullScreenProgressIndicatorSample() },
        "LinearProgressIndicatorSample" to { LinearProgressIndicatorSample({ 0.5f }) },
        "ListHeaderSample" to { ListHeaderSample() },
        "OverflowProgressIndicatorSample" to { OverflowProgressIndicatorSample() },
        "SegmentedProgressIndicatorBinarySample" to { SegmentedProgressIndicatorBinarySample() },
        "SegmentedProgressIndicatorSample" to { SegmentedProgressIndicatorSample() },
        "SmallSegmentedProgressIndicatorBinarySample" to
            {
                SmallSegmentedProgressIndicatorBinarySample()
            },
        "SmallSegmentedProgressIndicatorSample" to { SmallSegmentedProgressIndicatorSample() },
        "SmallValuesProgressIndicatorSample" to { SmallValuesProgressIndicatorSample() },
        "SurfaceTransformationButtonSample" to { SurfaceTransformationButtonSample() },
        "SurfaceTransformationCardSample" to { SurfaceTransformationCardSample() },
        "TransformingLazyColumnMinimumVerticalContentPaddingSample" to
            {
                TransformingLazyColumnMinimumVerticalContentPaddingSample()
            },
        "ScrollIndicatorWithTLCSample" to { ScrollIndicatorWithTLCSample() },
        "TransformingLazyColumnButtonsSample" to { TransformingLazyColumnButtonsSample() },
        "TimeTextClockOnly" to { TimeTextClockOnly() },
        "TimeTextWithStatus" to { TimeTextWithStatus() },
        "TimeTextWithStatusEllipsized" to { TimeTextWithStatusEllipsized() },
        "CurvedTextBottom" to { CurvedTextBottom() },
        "CurvedTextTop" to { CurvedTextTop() },
    )
