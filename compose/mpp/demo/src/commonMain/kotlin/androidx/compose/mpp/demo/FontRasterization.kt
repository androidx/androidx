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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.FontHinting
import androidx.compose.ui.text.FontRasterizationSettings
import androidx.compose.ui.text.FontSmoothing
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalTextApi::class)
@Composable
fun FontRasterization() {
    MaterialTheme {
        val state = rememberScrollState()
        Column(Modifier.fillMaxSize().padding(10.dp).verticalScroll(state)) {
            val hintingOptions = listOf(FontHinting.None, FontHinting.Slight, FontHinting.Normal, FontHinting.Full)
            val isAutoHintingForcedOptions = listOf(false, true)
            val subpixelOptions = listOf(false, true)
            val smoothingOptions = listOf(FontSmoothing.None, FontSmoothing.AntiAlias, FontSmoothing.SubpixelAntiAlias)
            val text = "Lorem ipsum"

            for (subpixel in subpixelOptions) {
                for (smoothing in smoothingOptions) {
                    for (hinting in hintingOptions) {
                        for (autoHintingForced in isAutoHintingForcedOptions) {
                            val fontRasterizationSettings = FontRasterizationSettings(
                                smoothing = smoothing,
                                hinting = hinting,
                                subpixelPositioning = subpixel,
                                autoHintingForced = autoHintingForced
                            )
                            BasicText(
                                text = "$text [$fontRasterizationSettings]",
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(
                                        null, PlatformParagraphStyle(fontRasterizationSettings)
                                    )
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}