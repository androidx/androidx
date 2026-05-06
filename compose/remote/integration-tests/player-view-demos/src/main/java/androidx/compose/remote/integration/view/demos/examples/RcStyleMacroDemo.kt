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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Color
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices

/**
 * Demo of Modifier Macros (Option #1 from the proposal). This shows how a set of modifiers can be
 * encapsulated into a macro and reused as a "Style".
 */
@Suppress("RestrictedApiAndroidX")
fun RcStyleMacroDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        val primaryColor = addColor(Color.parseColor("#6200EE"))
        val secondaryColor = addColor(Color.parseColor("#03DAC6"))

        val buttonStyleId = DefineButtonStyleMacro()
        val cardStyleId = DefineCardStyleMacro()

        root {
            column(Modifier.fillMaxSize().padding(20f), horizontal = ColumnLayout.CENTER) {
                text(
                    "Modifier Macros (Style Factories)",
                    fontSize = 40f,
                    modifier = Modifier.padding(0f, 0f, 0f, 20f),
                )

                // Use the ButtonStyle macro with primary color
                box(Modifier.includeMacro(buttonStyleId, intArrayOf(primaryColor))) {
                    text("Primary Button", color = Color.WHITE, fontSize = 24f)
                }

                box(Modifier.size(20f))

                // Use the ButtonStyle macro with secondary color
                box(Modifier.includeMacro(buttonStyleId, intArrayOf(secondaryColor))) {
                    text("Secondary Button", color = Color.BLACK, fontSize = 24f)
                }

                box(Modifier.size(40f))

                // A card with a shared style
                column(Modifier.includeMacro(cardStyleId)) {
                    text("Card Title", fontSize = 30f, fontWeight = 700f)
                    text(
                        "This card uses a macro to define its padding, background, and border.",
                        fontSize = 20f,
                    )
                }
            }
        }
    }
}

private fun RemoteComposeContextAndroid.DefineButtonStyleMacro(): Int {
    val colorParam = definePatternParameter("backgroundColor")

    return definePattern("ButtonStyle", colorParam) {
        // Emit modifiers directly into the macro
        modifier(
            Modifier.clip(RoundedRectShape(8f, 8f, 8f, 8f))
                .backgroundId(colorParam)
                .padding(24f, 12f, 24f, 12f)
        )
    }
}

private fun RemoteComposeContextAndroid.DefineCardStyleMacro(): Int {
    return definePattern("CardStyle") {
        modifier(
            Modifier.padding(16f)
                .border(1f, 12f, Color.LTGRAY, 2)
                .clip(RoundedRectShape(8f, 8f, 8f, 8f))
                .background(Color.WHITE)
                .padding(16f)
                .fillMaxWidth()
        )
    }
}
