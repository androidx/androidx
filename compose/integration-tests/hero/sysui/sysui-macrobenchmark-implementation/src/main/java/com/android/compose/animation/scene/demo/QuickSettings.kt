/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.compose.animation.scene.demo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.Back
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.Edge
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.LowestZIndexContentPicker
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.UserAction
import com.android.compose.animation.scene.UserActionResult

object QuickSettings {
    /**
     * The progress from which we can assume that the QuickSettings => Shade transition is
     * *visually* committed. This is used so that if the QS pager is not on the first page when the
     * transition started, and we swipe up to the Shade scene (which shows the first n tiles) such
     * that the transition is at least 97%, then swiping down will reset the QS pager to the first
     * page and those first tiles will be shared.
     */
    val TransitionToShadeCommittedProgress = 0.97f

    fun userActions(
        shadeScene: SceneKey,
        lockscreenScene: SceneKey,
        isLockscreenDismissed: Boolean,
    ): Map<UserAction, UserActionResult> {
        val fastSwipeUpScene = if (isLockscreenDismissed) Scenes.Launcher else lockscreenScene
        return mapOf(
            Back to shadeScene,
            Swipe.Up to shadeScene,
            Swipe.Up(pointerCount = 2) to fastSwipeUpScene,
            Swipe.Up(fromSource = Edge.Bottom) to fastSwipeUpScene,
        )
    }

    object Elements {
        val Background =
            ElementKey("QuickSettingsBackground", contentPicker = LowestZIndexContentPicker)
        val Date = ElementKey("QuickSettingsDate")
        val Operator = ElementKey("QuickSettingsOperator")
        val BrightnessSlider = ElementKey("QuickSettingsBrightnessSlider")
        val ExpandedGrid = ElementKey("QuickSettingsExpandedGrid")
        val PagerIndicators = ElementKey("QuickSettingsPagerIndicator")
        val FooterActions = ElementKey("QuickSettingsFooterActions")
    }

    object Dimensions {
        val Padding = 16.dp
        val FooterActionsPadding = 16.dp
        val FooterActionsBottomPadding = 32.dp
        val FooterActionsButtonSize = 40.dp
    }

    object Shapes {
        val FooterActionsBackground = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    }

    object Modifiers {
        val HorizontalPadding = Modifier.padding(horizontal = Dimensions.Padding)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ContentScope.QuickSettings(
    qsPager: @Composable ContentScope.(revealEffect: Boolean) -> Unit,
    mediaPlayer: (@Composable ContentScope.() -> Unit)?,
    onSettingsButtonClicked: () -> Unit,
    onPowerButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        QuickSettingsBackground(Modifier.fillMaxSize())

        CompositionLocalProvider(LocalContentColor provides Color.White) {
            val scrollState = rememberScrollState()

            val offsetOverscrollEffect = rememberOverscrollEffect()
            Column(
                Modifier.noResizeDuringTransitions()
                    .overscroll(verticalOverscrollEffect)
                    .overscroll(offsetOverscrollEffect)
                    .disableSwipesWhenScrolling()
                    .verticalScroll(scrollState, overscrollEffect = offsetOverscrollEffect)
                    .padding(vertical = QuickSettings.Dimensions.Padding)
            ) {
                val horizontalPaddingModifier = QuickSettings.Modifiers.HorizontalPadding

                TimeRow(horizontalPaddingModifier)
                DateAndBatteryRow(horizontalPaddingModifier)
                BrightnessSlider(
                    horizontalPaddingModifier.padding(top = QuickSettings.Dimensions.Padding)
                )

                Box(
                    Modifier.padding(top = QuickSettings.Dimensions.Padding)
                        .element(QuickSettings.Elements.ExpandedGrid)
                ) {
                    qsPager(false)
                }

                if (mediaPlayer != null) {
                    Box(horizontalPaddingModifier) { mediaPlayer() }
                }

                // Add a spacer with the same height as the footer actions so that we can scroll the
                // media player fully above the footer actions.
                Spacer(
                    Modifier.height(
                        QuickSettings.Dimensions.FooterActionsButtonSize +
                            QuickSettings.Dimensions.FooterActionsPadding +
                            QuickSettings.Dimensions.FooterActionsBottomPadding
                    )
                )
            }

            FooterActions(
                onSettingsButtonClicked,
                onPowerButtonClicked,
                Modifier.align(Alignment.BottomCenter)
                    .element(QuickSettings.Elements.FooterActions)
                    // Intercepts touches, prevents the scrollable container behind from scrolling.
                    .clickable(interactionSource = null, indication = null) { /* do nothing */ }
                    .background(Color.Black, QuickSettings.Shapes.FooterActionsBackground)
                    .padding(
                        top = QuickSettings.Dimensions.FooterActionsPadding,
                        start = QuickSettings.Dimensions.FooterActionsPadding,
                        end = QuickSettings.Dimensions.FooterActionsPadding,
                        bottom = QuickSettings.Dimensions.FooterActionsBottomPadding,
                    ),
            )
        }
    }
}

@Composable
fun ContentScope.QuickSettingsBackground(modifier: Modifier = Modifier) {
    Box(
        modifier
            .element(QuickSettings.Elements.Background)
            .fillMaxSize()
            .background(colorResource(android.R.color.system_neutral1_1000))
    )
}

@Composable
private fun ContentScope.TimeRow(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ShadeTime(scale = 2.57f)
        Spacer(Modifier.weight(1f))
        Operator()
    }
}

@Composable
internal fun ContentScope.Operator(modifier: Modifier = Modifier) {
    Text("Emergency calls only", modifier.element(QuickSettings.Elements.Operator))
}

@Composable
private fun ContentScope.DateAndBatteryRow(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ShadeDate(Modifier.element(QuickSettings.Elements.Date))
        Spacer(Modifier.weight(1f))
        BatteryPercentage()
    }
}

@Composable
internal fun ContentScope.BrightnessSlider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .element(QuickSettings.Elements.BrightnessSlider)
            .fillMaxWidth()
            .height(48.dp)
            .background(QuickSettingsGrid.Colors.ActiveTileBackground, CircleShape),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            Icons.Default.Brightness5,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
internal fun FooterActions(
    onSettingsButtonClicked: () -> Unit,
    onPowerButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Spacer(Modifier.weight(1f))

        FilledIconButton(
            onClick = onSettingsButtonClicked,
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = QuickSettingsGrid.Colors.InactiveTileBackground,
                    contentColor = QuickSettingsGrid.Colors.InactiveTileText,
                ),
            modifier = Modifier.size(QuickSettings.Dimensions.FooterActionsButtonSize),
        ) {
            Icon(Icons.Default.Settings, null, Modifier.size(20.dp))
        }

        FilledIconButton(
            onClick = onPowerButtonClicked,
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = QuickSettingsGrid.Colors.ActiveTileBackground,
                    contentColor = QuickSettingsGrid.Colors.ActiveTileText,
                ),
            modifier = Modifier.size(QuickSettings.Dimensions.FooterActionsButtonSize),
        ) {
            Icon(Icons.Default.PowerSettingsNew, null, Modifier.size(20.dp))
        }
    }
}
