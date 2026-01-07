/*
 * Copyright (C) 2023 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.Back
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.UserAction
import com.android.compose.animation.scene.UserActionResult

object SplitShade {
    fun userActions(
        isLockscreenDismissed: Boolean,
        lockscreenScene: SceneKey,
    ): Map<UserAction, UserActionResult> {
        val previousScreen =
            if (isLockscreenDismissed) {
                Scenes.Launcher
            } else {
                lockscreenScene
            }

        return mapOf(Back to previousScreen, Swipe.Up to previousScreen)
    }

    object Elements {
        val Background = ElementKey("SplitShadeBackground")
    }

    object Shapes {
        val Scrim = RoundedCornerShape(Shade.Dimensions.ScrimCornerSize)
    }
}

@Composable
fun ContentScope.SplitShade(
    notificationList: @Composable ContentScope.() -> Unit,
    mediaPlayer: @Composable (ContentScope.() -> Unit)?,
    quickSettingsTiles: List<QuickSettingsTileViewModel>,
    nQuickSettingsRows: Int,
    nQuickSettingsColumns: Int,
    onSettingsButtonClicked: () -> Unit,
    onPowerButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Box(modifier.element(SplitShade.Elements.Background).fillMaxSize().background(Color.Black))

        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(
                Modifier.fillMaxSize()
                    .padding(top = 16.dp, bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    ShadeTime(scale = 1f)
                    ShadeDate(Modifier.padding(start = 16.dp).element(Shade.Elements.Date))
                    Spacer(Modifier.weight(1f))
                    Operator()
                    BatteryPercentage(Modifier.padding(start = 16.dp))
                }

                Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(Modifier.weight(1f).padding(top = QuickSettings.Dimensions.Padding)) {
                        val horizontalPaddingModifier = QuickSettings.Modifiers.HorizontalPadding

                        BrightnessSlider(horizontalPaddingModifier)

                        val nPages =
                            nQuickSettingsPages(
                                nTiles = quickSettingsTiles.size,
                                nRows = nQuickSettingsRows,
                                nColumns = nQuickSettingsColumns,
                            )

                        QuickSettingsPager(
                            pagerState = rememberPagerState { nPages },
                            tiles = quickSettingsTiles,
                            nRows = nQuickSettingsRows,
                            nColumns = nQuickSettingsColumns,
                            Modifier.padding(top = QuickSettings.Dimensions.Padding),
                            revealEffect = false,
                        )

                        if (mediaPlayer != null) {
                            Box(horizontalPaddingModifier.padding()) { mediaPlayer() }
                        }

                        Spacer(Modifier.weight(1f))
                        FooterActions(
                            onSettingsButtonClicked,
                            onPowerButtonClicked,
                            horizontalPaddingModifier.element(QuickSettings.Elements.FooterActions),
                        )
                    }

                    Box(Modifier.weight(1f).padding(start = 16.dp).clip(SplitShade.Shapes.Scrim)) {
                        Box(
                            Modifier.element(Shade.Elements.ScrimBackground)
                                .fillMaxSize()
                                .background(Shade.Colors.Scrim)
                        )

                        notificationList()
                    }
                }
            }
        }
    }
}
