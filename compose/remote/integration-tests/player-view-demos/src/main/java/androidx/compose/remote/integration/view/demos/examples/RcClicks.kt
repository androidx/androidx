/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *2
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Color
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview @Composable fun RcClicksDemoPreview() = RemoteDocPreview(RcClicksDemo())

@Suppress("RestrictedApiAndroidX")
fun RcClicksDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        val clickCount = addNamedFloat("clickCount", 0f)
        val longPressCount = addNamedFloat("longPressCount", 0f)
        val doubleTapCount = addNamedFloat("doubleTapCount", 0f)

        root {
            column(Modifier.fillMaxSize().background(Color.WHITE).padding(40f)) {
                text("Click Interactions Demo", Modifier.padding(0f, 0f, 0f, 20f))

                // Single Click
                column(
                    Modifier.fillMaxWidth()
                        .background(Color.LTGRAY)
                        .padding(20f)
                        .onClick(
                            ValueFloatExpressionChange(
                                Utils.idFromNan(clickCount),
                                Utils.idFromNan((this.rf(clickCount) + 1f).toFloat()),
                            )
                        )
                ) {
                    text(string = "Single Click Me")
                    row {
                        text(string = "Clicks: ")
                        this.text(rf(clickCount).genTextId(after = 0))
                    }
                }

                box(Modifier.height(20f))

                // Long Press
                column(
                    Modifier.fillMaxWidth()
                        .background(Color.LTGRAY)
                        .padding(20f)
                        .onLongClick(
                            ValueFloatExpressionChange(
                                Utils.idFromNan(longPressCount),
                                Utils.idFromNan((this.rf(longPressCount) + 1f).toFloat()),
                            )
                        )
                ) {
                    text(string = "Long Press Me")
                    row {
                        text(string = "Long Presses: ")
                        this.text(rf(longPressCount).genTextId(after = 0))
                    }
                }

                box(Modifier.height(20f))

                // Double Tap
                column(
                    Modifier.fillMaxWidth()
                        .background(Color.LTGRAY)
                        .padding(20f)
                        .onDoubleClick(
                            ValueFloatExpressionChange(
                                Utils.idFromNan(doubleTapCount),
                                Utils.idFromNan((this.rf(doubleTapCount) + 1f).toFloat()),
                            )
                        )
                ) {
                    text(string = "Double Tap Me")
                    row {
                        text(string = "Double Taps: ")
                        this.text(rf(doubleTapCount).genTextId(after = 0))
                    }
                }

                box(Modifier.height(40f))

                // Reset button
                column(
                    Modifier.fillMaxWidth()
                        .background(Color.RED)
                        .padding(10f)
                        .onClick(
                            ValueFloatExpressionChange(
                                Utils.idFromNan(clickCount),
                                Utils.idFromNan((rf(0f).toFloat())),
                            ),
                            ValueFloatExpressionChange(
                                Utils.idFromNan(longPressCount),
                                Utils.idFromNan((rf(0f).toFloat())),
                            ),
                            ValueFloatExpressionChange(
                                Utils.idFromNan(doubleTapCount),
                                Utils.idFromNan((rf(0f).toFloat())),
                            ),
                        )
                ) {
                    text("Reset All", color = Color.WHITE)
                }
            }
        }
    }
}
