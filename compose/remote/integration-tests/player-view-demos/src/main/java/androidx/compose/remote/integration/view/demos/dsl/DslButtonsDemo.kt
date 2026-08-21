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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter.HTag
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcFontWeight
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcRowHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcTextAlign
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.fillMaxWidth
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/**
 * A simple DSL demo featuring a dynamic header text ("buttons") and a 3x3 grid of evenly spaced
 * buttons ("button 1" .. "button 9"). Each button is based on a Box with an onClick callback that
 * modifies the header text to say the clicked button's label.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslButtonsDemo(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        HTag(Header.DOC_DENSITY_BEHAVIOR, CoreDocument.DENSITY_BEHAVIOR_DP),
        experimental = true,
    ) {
        val headerText = remoteNamedText("header", "buttons")

        Column(
            modifier = Modifier.fillMaxSize().background(0xFF181A20.toInt()).padding(24f),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Center,
        ) {
            // Dynamic text at the top
            Text(
                text = headerText,
                fontSize = 64.rsp,
                fontWeight = RcFontWeight.Bold,
                color = 0xFFFFFFFF.toInt(),
                textAlign = RcTextAlign.Center,
                modifier = Modifier.padding(bottom = 32f),
            )

            // 3 x 3 grid of evenly spaced buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontal = RcHorizontalPositioning.Center,
            ) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8f, bottom = 8f),
                        horizontal = RcRowHorizontalPositioning.SpaceEvenly,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        for (col in 0 until 3) {
                            val buttonNumber = row * 3 + col + 1
                            val buttonLabel = "button $buttonNumber"
                            Box(
                                modifier =
                                    Modifier.background(0xFF2C3E50.toInt())
                                        .padding(4f, 4f, 4f, 4f)
                                        .onClick {
                                            setValue(headerText, buttonLabel)
                                            hostAction(
                                                23,
                                                this@Row.remoteText("foo " + buttonLabel),
                                            )
                                        },
                                horizontal = RcHorizontalPositioning.Center,
                                vertical = RcVerticalPositioning.Center,
                            ) {
                                Text(
                                    text = buttonLabel,
                                    color = 0xFFECF0F1.toInt(),
                                    fontSize = 64.rsp,
                                    fontWeight = RcFontWeight.Medium,
                                    textAlign = RcTextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
