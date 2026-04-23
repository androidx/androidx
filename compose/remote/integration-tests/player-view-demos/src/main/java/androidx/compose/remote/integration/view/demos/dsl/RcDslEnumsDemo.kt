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

import androidx.compose.remote.creation.dsl.*
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcFontWeight
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.RcStrokeJoin
import androidx.compose.remote.creation.dsl.RcTextAlign
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rdp
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.size
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/** A demo showcasing the new idiomatic enums and types in the Remote Compose DSL. */
@Suppress("RestrictedApiAndroidX")
public fun enumsDemo(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Column(
            modifier = Modifier.fillMaxSize().background(0xFFF0F0F0.toInt()).padding(86.rdp),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Top,
            // spacedBy = 12.rdp
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Idiomatic Enums Demo",
                fontSize = 64.rsp,
                fontWeight = RcFontWeight.Bold,
                textAlign = RcTextAlign.Center,
            )

            // Text Alignment Demo
            Column() { // spacedBy = 4.rdp
                Text("Text Alignments:", fontSize = 64.rsp, fontWeight = RcFontWeight.SemiBold)
                Text(
                    "Start Aligned",
                    modifier = Modifier.size(600.rdp, 70.rdp).background(0xFFDDDDDD.toInt()),
                    fontSize = 64.rsp,
                    textAlign = RcTextAlign.Start,
                )
                Text(
                    "Center Aligned",
                    modifier = Modifier.size(600.rdp, 70.rdp).background(0xFFDDDDDD.toInt()),
                    fontSize = 64.rsp,
                    textAlign = RcTextAlign.Center,
                )
                Text(
                    "End Aligned",
                    modifier = Modifier.size(600.rdp, 70.rdp).background(0xFFDDDDDD.toInt()),
                    fontSize = 64.rsp,
                    textAlign = RcTextAlign.End,
                )
            }

            // Canvas / Paint Enums Demo
            Text("Paint Styles & Caps:", fontSize = 64.rsp, fontWeight = RcFontWeight.SemiBold)
            Canvas(modifier = Modifier.size(600.rdp, 200.rdp).background(0xFFFFFFFF.toInt())) {
                // Stroke Cap: Round
                applyPaint {
                    setColor(0xFFFF0000.toInt())
                    setStrokeWidth(40f)
                    setStrokeCap(RcStrokeCap.Round)
                    setStyle(RcPaintStyle.Stroke)
                }
                drawLine(20f, 30f, 220f, 30f)

                // Stroke Cap: Square
                applyPaint {
                    setColor(0xFF00FF00.toInt())
                    setStrokeCap(RcStrokeCap.Square)
                }
                drawLine(20f, 170f, 220f, 170f)

                // Paint Style: Fill and Stroke
                applyPaint {
                    setColor(0xFF0000FF.toInt())
                    setStyle(RcPaintStyle.FillAndStroke)
                    setStrokeWidth(4f)
                    setStrokeJoin(RcStrokeJoin.Round)
                }
                drawRect(280f, 20f, 360f, 80f)
            }
        }
    }
}
