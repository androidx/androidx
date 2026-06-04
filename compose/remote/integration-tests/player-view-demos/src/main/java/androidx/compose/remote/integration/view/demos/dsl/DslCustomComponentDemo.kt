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

package androidx.compose.remote.integration.view.demos.dsl

import android.graphics.Color
import androidx.compose.remote.creation.dsl.CustomProperty
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcTextAlign
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.RcWeight
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.clip
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.fillMaxWidth
import androidx.compose.remote.creation.dsl.height
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rcColor
import androidx.compose.remote.creation.dsl.rdp
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.width
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.view.demos.customviews.SupportProgressBar
import androidx.compose.remote.integration.view.demos.customviews.SupportTextView
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * An integration demo showcasing the newly introduced layout manager component `Custom` designed to
 * host and configure native Android platform components dynamically.
 *
 * Showcases:
 * 1. Hosting a native Android `TextView` inside the remote layout tree.
 * 2. Dynamic property mappings for native components using decoupled key-value pairs:
 *     - Type 1: String text contents configuration.
 *     - Type 2: Integer text color configuration.
 *     - Type 3: Integer text size configuration.
 *     - Type 4: Integer background color configuration.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslCustomComponentDemo(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Column(
            modifier = Modifier.fillMaxSize().background(0xFF0F17FF.toInt()).padding(32.rdp),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Top,
        ) {
            // Title Header
            Text(
                text = "Native Interoperability",
                weight = RcWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 36.rsp,
                textAlign = RcTextAlign.Center,
                color = 0xFFFFFFFF.toInt(),
            )

            Spacer(Modifier.height(16.rdp))

            Text(
                text = "Uses Fully native Android TextView",
                weight = RcWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 48.rsp,
                textAlign = RcTextAlign.Center,
                color = 0xFF94A3B8.toInt(),
            )
            val timeCount = createTextFromFloat(seconds(), 2, 0, 0)

            Row(Modifier.padding(8f), vertical = RcVerticalPositioning.Center) {
                Text(
                    text = "RC Canvas :",
                    fontSize = 48.rsp,
                    textAlign = RcTextAlign.Center,
                    color = 0xFF94A3B8.toInt(),
                    modifier = Modifier.width(300.rdp),
                )
                Canvas(
                    Modifier.width(200.rdp)
                        .height(100.rdp)
                        .clip(RoundedRectShape(32f, 32f, 32f, 32f))
                        .background(Color.GREEN)
                ) {
                    paint { textSize(48f) }
                    drawTextAnchored(timeCount, width / 2f, height / 2f, 0.rf, 0.rf, 0)
                }
            }

            Row(Modifier.padding(8f), vertical = RcVerticalPositioning.Center) {
                Text(
                    text = "TextView :",
                    fontSize = 48.rsp,
                    textAlign = RcTextAlign.Center,
                    color = 0xFF94A3B8.toInt(),
                    modifier = Modifier.width(300.rdp),
                )
                Custom(
                    config = "TextView",
                    properties =
                        listOf(
                            CustomProperty.text(SupportTextView.PROP_TEXT, timeCount),
                            CustomProperty.color(
                                SupportTextView.PROP_TEXT_COLOR,
                                Color.rgb(56, 189, 248).rcColor(),
                            ),
                            CustomProperty(
                                SupportTextView.PROP_TEXT_SIZE,
                                CustomProperty.FLOAT_PROP,
                                120f / 3,
                            ),
                            CustomProperty.color(
                                SupportTextView.PROP_BACKGROUND_COLOR,
                                Color.rgb(30, 41, 59).rcColor(),
                            ),
                        ),
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(160.rdp)
                            .clip(RoundedRectShape(32f, 32f, 32f, 32f)),
                )
            }
            Row(Modifier.padding(8f), vertical = RcVerticalPositioning.Center) {
                Text(
                    text = "TextView :",
                    fontSize = 48.rsp,
                    textAlign = RcTextAlign.Center,
                    color = 0xFF94A3B8.toInt(),
                    modifier = Modifier.width(300.rdp),
                )
                Custom(
                    config = "TextView",
                    properties =
                        listOf(
                            CustomProperty.text(SupportTextView.PROP_TEXT, timeCount),
                            CustomProperty.color(
                                SupportTextView.PROP_TEXT_COLOR,
                                Color.rgb(56, 248, 189).rcColor(),
                            ),
                            CustomProperty(
                                SupportTextView.PROP_TEXT_SIZE,
                                CustomProperty.FLOAT_PROP,
                                120f / 3,
                            ),
                            CustomProperty.color(
                                SupportTextView.PROP_BACKGROUND_COLOR,
                                Color.rgb(30, 41, 59).rcColor(),
                            ),
                        ),
                    modifier = Modifier,
                )
            }

            val prop = CustomProperty.returnFloat(SupportProgressBar.RET_PROGRESS, this)
            val slider = prop.getFloatValue()
            // Native hosted Custom Component ProgressBar!
            val progressProps =
                listOf(
                    // PROP_PROGRESS = 5
                    CustomProperty(
                        SupportProgressBar.PROP_PROGRESS,
                        CustomProperty.FLOAT_PROP,
                        slider,
                    ),
                    // PROP_MAX_PROGRESS = 6
                    CustomProperty(
                        SupportProgressBar.PROP_MAX_PROGRESS,
                        CustomProperty.INT_PROP,
                        100,
                    ),
                    // PROP_INDETERMINATE = 7 (false)
                    CustomProperty(
                        SupportProgressBar.PROP_INDETERMINATE,
                        CustomProperty.INT_PROP,
                        0,
                    ),
                    // PROP_PROGRESS_COLOR = 8 (Sky Blue)
                    CustomProperty.color(
                        SupportProgressBar.PROP_PROGRESS_COLOR,
                        Color.rgb(56, 189, 248).rcColor(),
                    ),
                    // Return float
                    prop,
                )
            Row(Modifier.padding(8f), vertical = RcVerticalPositioning.Center) {
                Text(
                    text = "SeekBar :",
                    fontSize = 48.rsp,
                    textAlign = RcTextAlign.Center,
                    color = 0xFF94A3B8.toInt(),
                    modifier = Modifier.width(300.rdp),
                )
                Custom(
                    config = "ProgressBar",
                    properties = progressProps,
                    modifier = Modifier.width(450.rdp),
                )
            }

            Spacer(Modifier.height(12.rdp))
            val sliderValText = createTextFromFloat(slider, 3, 0, 0)
            Text(
                text = "Text Slider Value: " join slider.genTextId(3),
                weight = RcWeight.Normal,
                fontSize = 64.rsp,
                color = 0xFFE2E8F0.toInt(),
            )

            Row(Modifier.padding(8f), vertical = RcVerticalPositioning.Center) {
                Text(
                    text = "TextView :",
                    fontSize = 48.rsp,
                    textAlign = RcTextAlign.Center,
                    color = 0xFF94A3B8.toInt(),
                    modifier = Modifier.width(300.rdp),
                )
                val sum = slider + seconds() % 100f
                Custom(
                    config = "TextView",
                    properties =
                        listOf(
                            CustomProperty.text(SupportTextView.PROP_TEXT, sum.genTextId(3)),
                            CustomProperty.color(
                                SupportTextView.PROP_TEXT_COLOR,
                                Color.rgb(248, 189, 56).rcColor(),
                            ),
                            CustomProperty(
                                SupportTextView.PROP_TEXT_SIZE,
                                CustomProperty.FLOAT_PROP,
                                120f / 3,
                            ),
                            CustomProperty.color(
                                SupportTextView.PROP_BACKGROUND_COLOR,
                                Color.rgb(30, 41, 59).rcColor(),
                            ),
                        ),
                    modifier = Modifier.width(450.rdp).height(130.rdp),
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
private fun DslCustomComponentDemoPreview() {
    RemoteDocumentPreview(RemoteDocument(dslCustomComponentDemo()))
}
