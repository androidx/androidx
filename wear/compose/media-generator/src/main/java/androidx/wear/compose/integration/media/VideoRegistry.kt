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

// Wildcard import to import many Wear Compose M3 samples without adding bulk.
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.ConfirmationDialog
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.FailureConfirmationDialog
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SuccessConfirmationDialog
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.material3.samples.*
import androidx.wear.compose.material3.samples.icons.FavoriteIcon

/**
 * Registry mapping sample names (strings) to their respective @Composable sample functions. Used by
 * VideoActivity to dynamically render the target sample during automated video recording.
 */
val videoRegistry: Map<String, @Composable () -> Unit> =
    mapOf(
        // --- Alert Dialogs ---
        "AlertDialogWithConfirmAndDismissSample" to { AlertDialogWithConfirmAndDismissSample() },
        "AlertDialogWithConfirmAndDismissTransformingContentSample" to
            {
                AlertDialogWithConfirmAndDismissTransformingContentSample()
            },
        "AlertDialogWithEdgeButtonSample" to { AlertDialogWithEdgeButtonSample() },
        "AlertDialogWithContentGroupsSample" to { AlertDialogWithContentGroupsSample() },
        "AlertDialogWithEdgeButtonTransformingContentSample" to
            {
                AlertDialogWithEdgeButtonTransformingContentSample()
            },
        "AlertDialogWithContentGroupsTransformingContentSample" to
            {
                AlertDialogWithContentGroupsTransformingContentSample()
            },

        // --- One Handed Gestures ---
        "OneHandedGestureButtonSample" to { OneHandedGestureButtonSample() },
        "OneHandedGestureDisableButtonSample" to { OneHandedGestureDisableButtonSample() },
        "OneHandedGestureTransformingLazyColumnSample" to
            {
                OneHandedGestureTransformingLazyColumnSample()
            },
        "OneHandedGestureScalingLazyColumnSample" to { OneHandedGestureScalingLazyColumnSample() },
        "OneHandedGestureTransformingLazyColumnScrollToNextItemSample" to
            {
                OneHandedGestureTransformingLazyColumnScrollToNextItemSample()
            },
        "OneHandedGestureScalingLazyColumnScrollToNextItemSample" to
            {
                OneHandedGestureScalingLazyColumnScrollToNextItemSample()
            },
        "OneHandedGestureHorizontalPagerSample" to { OneHandedGestureHorizontalPagerSample() },
        "OneHandedGestureVerticalPagerSample" to { OneHandedGestureVerticalPagerSample() },
        "ButtonContentWithOneHandedGestureSample" to { ButtonContentWithOneHandedGestureSample() },
        "CompactButtonContentWithOneHandedGestureSample" to
            {
                CompactButtonContentWithOneHandedGestureSample()
            },
        "AppCardContentWithOneHandedGestureSample" to
            {
                AppCardContentWithOneHandedGestureSample()
            },
        "TitleCardContentWithOneHandedGestureSample" to
            {
                TitleCardContentWithOneHandedGestureSample()
            },

        // --- Progress Indicators ---
        "IndeterminateProgressArcSample" to { IndeterminateProgressArcSample() },
        "IndeterminateProgressIndicatorSample" to { IndeterminateProgressIndicatorSample() },
        "CircularProgressIndicatorCustomAnimationSample" to
            {
                CircularProgressIndicatorCustomAnimationSample()
            },

        // --- Buttons & Button Groups ---
        "ButtonWithIconAndLabelAndPlaceholders" to { ButtonWithIconAndLabelAndPlaceholders() },
        "ButtonWithIconAndLabelCachedData" to { ButtonWithIconAndLabelCachedData() },
        "ButtonGroupSample" to { ButtonGroupSample() },
        "ButtonGroupThreeButtonsSample" to { ButtonGroupThreeButtonsSample() },
        "IconButtonWithCornerAnimationSample" to { IconButtonWithCornerAnimationSample() },
        "TextButtonWithCornerAnimationSample" to { TextButtonWithCornerAnimationSample() },
        "FadingExpandingLabelButtonSample" to { FadingExpandingLabelButtonSample() },

        // --- Toggle Buttons ---
        "IconToggleButtonSample" to { IconToggleButtonSample() },
        "IconToggleButtonVariantSample" to { IconToggleButtonVariantSample() },
        "LargeTextToggleButtonSample" to { LargeTextToggleButtonSample() },
        "TextToggleButtonSample" to { TextToggleButtonSample() },
        "TextToggleButtonVariantSample" to { TextToggleButtonVariantSample() },

        // --- Animated Text & Placeholders ---
        "AnimatedTextSample" to { AnimatedTextSample() },
        "AnimatedTextSampleSharedFontRegistry" to { AnimatedTextSampleSharedFontRegistry() },
        "AnimatedTextSampleButtonResponse" to { AnimatedTextSampleButtonResponse() },
        "TextPlaceholder" to { TextPlaceholder() },

        // --- Swipe to Reveal ---
        /*
         * SwipeToReveal (STR) is designed to be a list item inside a curved container
         * like TransformingLazyColumn. Placing STR inside a bare Box causes layout problems,
         * such as the swipeable component inflating to fill the entire screen.
         */
        "SwipeToRevealSample" to { CenteredTlcSample { SwipeToRevealSample() } },
        "SwipeToRevealSingleActionCardSample" to
            {
                CenteredTlcSample { SwipeToRevealSingleActionCardSample() }
            },
        "SwipeToRevealWithTransformingLazyColumnSample" to
            {
                SwipeToRevealWithTransformingLazyColumnSample()
            },
        "SwipeToRevealNoPartialRevealWithScalingLazyColumnSample" to
            {
                SwipeToRevealNoPartialRevealWithScalingLazyColumnSample()
            },

        // --- Pagers & Scaffolds ---
        "HorizontalPageIndicatorWithPagerSample" to { HorizontalPageIndicatorWithPagerSample({}) },
        "HorizontalPagerScaffoldSample" to { HorizontalPagerScaffoldSample({}) },
        "HorizontalPagerScaffoldWithLowSensitivitySample" to
            {
                HorizontalPagerScaffoldWithLowSensitivitySample({})
            },
        "VerticalPageIndicatorWithPagerSample" to { VerticalPageIndicatorWithPagerSample() },
        "VerticalPagerScaffoldSample" to { VerticalPagerScaffoldSample() },
        "VerticalPagerScaffoldWithLowSensitivitySample" to
            {
                VerticalPagerScaffoldWithLowSensitivitySample()
            },
        "ScaffoldWithTLCEdgeButtonSample" to { ScaffoldWithTLCEdgeButtonSample() },
        "ScrollAwaySample" to { ScrollAwaySample() },

        // --- Confirmation Dialogs ---
        "ConfirmationDialogSample" to
            {
                ConfirmationDialog(true, {}, curved("Confirmed")) {
                    FavoriteIcon(ConfirmationDialogDefaults.IconSize)
                }
            },
        "LongTextConfirmationDialogSample" to
            {
                ConfirmationDialog(true, {}, text = { Text("Your message has been sent") }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        null,
                        Modifier.size(ConfirmationDialogDefaults.SmallIconSize),
                    )
                }
            },
        "SuccessConfirmationDialogSample" to
            {
                SuccessConfirmationDialog(true, {}, curved("Success"))
            },
        "FailureConfirmationDialogSample" to
            {
                FailureConfirmationDialog(true, {}, curved("Failure"))
            },
        "FailureConfirmationDialogWithGenericFailureIconSample" to
            {
                FailureConfirmationDialog(true, {}, curved("Failure")) {
                    ConfirmationDialogDefaults.GenericFailureIcon()
                }
            },
        "OpenOnPhoneDialogSample" to { OpenOnPhoneDialogSample() },
    )

@Composable
fun FallbackSample(sampleName: String) {
    Text(
        text = "Sample not found:\n$sampleName",
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CenteredTlcSample(content: @Composable () -> Unit) {
    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        item { content() }
    }
}

@Composable
private fun curved(text: String): CurvedScope.() -> Unit {
    val style = ConfirmationDialogDefaults.curvedTextStyle
    return { confirmationDialogCurvedText(text, style) }
}
