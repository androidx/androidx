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
import androidx.compose.remote.creation.actions.ValueStringChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
fun RcReferencedMacroDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        // Base style factories to ensure fresh Modifier instances
        fun greyStyle() =
            Modifier.padding(16)
                .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                .background(0xFFE0E0E0.toInt())
                .padding(24)

        fun bluePillStyle() =
            Modifier.padding(16)
                .clip(RoundedRectShape(32f, 32f, 32f, 32f))
                .background(0xFFBBDEFB.toInt())
                .padding(24)

        DefineReferencedMacroButton()

        root {
            column(Modifier.fillMaxSize(), horizontal = ColumnLayout.CENTER) {
                text(
                    "Macro with Behavior-in-Modifiers",
                    fontSize = 60f,
                    modifier = Modifier.padding(20),
                )

                val statusId = addText("Last clicked: None")
                text(statusId, fontSize = 30f, modifier = Modifier.padding(10))

                // Pass the Modifier objects including onClick behavior
                ReferencedMacroButton(
                    "Grey Button",
                    greyStyle().onClick(ValueStringChange(statusId, "Grey clicked")),
                )

                ReferencedMacroButton(
                    "Blue Pill Button",
                    bluePillStyle().onClick(ValueStringChange(statusId, "Blue clicked")),
                )

                // Inline styling and behavior
                ReferencedMacroButton(
                    "Danger Button",
                    Modifier.padding(16)
                        .background(Color.RED)
                        .padding(24)
                        .onClick(ValueStringChange(statusId, "DANGER!")),
                )
            }
        }
    }
}

private fun RemoteComposeContextAndroid.ReferencedMacroButton(
    label: String,
    modifier: RecordingModifier,
) {
    // Internally convert the modifier (including behavior) to a referenced set
    val modifierId = referencedModifiers(modifier)

    val labelId = textId(label)
    val macroNameId = textId("ReferencedMacroButton")

    // Macro only needs the label and the style/behavior ID
    inflatePattern(macroNameId, labelId, modifierId) {}
}

private fun RemoteComposeContextAndroid.DefineReferencedMacroButton() {
    val labelParam = definePatternParameter("label")
    val modifierParam = definePatternParameter("modifierId")

    definePattern("ReferencedMacroButton", labelParam, modifierParam) {
        box(Modifier.include(modifierParam)) {
            text(labelParam, fontSize = 30f, color = 0xFF333333.toInt())
        }
    }
}

@Preview
@Composable
private fun RcReferencedMacroDemoPreview() {
    RemoteDocumentPreview(RcReferencedMacroDemo())
}
