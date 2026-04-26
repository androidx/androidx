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

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Color
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * This demo demonstrates the use of Macro-Local IDs (Tier 2 IDs, 0x4000-0x4FFF). When an ID in this
 * range is used within a macro, the expansion engine automatically unique-ifies it for every call
 * site.
 *
 * This allows defining interactive components with internal state that stays independent across
 * multiple instances.
 */
@Suppress("RestrictedApiAndroidX")
fun RcMacroLocalDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        val counterMacroId = defineCounterButton()

        root {
            column(Modifier.fillMaxSize().background(Color.WHITE).padding(40f)) {
                text(
                    "Macro-Local State Demo",
                    fontSize = 48f,
                    modifier = Modifier.padding(0f, 0f, 0f, 20f),
                )
                text(
                    "Each button below has its own independent internal count.",
                    fontSize = 24f,
                    modifier = Modifier.padding(0f, 0f, 0f, 40f),
                )

                column(Modifier.fillMaxWidth()) {
                    // Call the macro 4 times. Each will have its own counter state
                    // because the counter variable uses an ID in the 0x4000-0x4FFF range.
                    val m = Modifier.padding(0f, 0f, 0f, 20f)
                    box(m) { inflatePattern(counterMacroId, textId("Button Alpha")) {} }
                    box(m) { inflatePattern(counterMacroId, textId("Button Beta")) {} }
                    box(m) { inflatePattern(counterMacroId, textId("Button Gamma")) {} }
                    box(m) { inflatePattern(counterMacroId, textId("Button Delta")) {} }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.defineCounterButton(): Int {
    val labelParam = definePatternParameter("label")

    // 0x4000 is in the Tier 2 Macro-Local range.
    // CoreDocument expansion will unique-ify this ID for each MacroCall.
    val localCounterIntId = 0x4000

    return definePattern("CounterButton", labelParam) {
        // Initialize the local counter to 0 (using a float)
        androidx.compose.remote.core.operations.FloatConstant.apply(
            mRemoteWriter.buffer.buffer,
            localCounterIntId,
            0f,
        )

        // Convert the float counter to a string.
        // We use floatId to indicate that the 'value' is actually a variable ID (NaN-encoded).
        val localTextId = createTextFromFloat(floatId(localCounterIntId), 3, 0, 0)

        // Define a float expression that increments the local counter: (counter + 1)
        val nextValueExprId =
            floatExpression(floatId(localCounterIntId), 1f, AnimatedFloatExpression.ADD)

        val corner = 24f
        column(
            Modifier.fillMaxWidth()
                .padding(10f)
                .clip(RoundedRectShape(corner, corner, corner, corner))
                .background(Color.LTGRAY)
                .padding(20f)
                // When clicked, update the UNIQUE-IFIED local counter with the UNIQUE-IFIED
                // expression
                .onClick(
                    ValueFloatExpressionChange(localCounterIntId, Utils.idFromNan(nextValueExprId))
                )
        ) {
            text(labelParam, fontSize = 28f, color = Color.BLACK)
            row {
                text("Count: ", fontSize = 24f, color = Color.DKGRAY)
                // Display the current value of the local counter (via the converted text ID)
                text(localTextId, fontSize = 24f, color = Color.BLUE)
            }
        }
    }
}

@Composable
@Preview
fun RcMacroLocalDemoPreview() {
    RemoteDocPreview(RcMacroLocalDemo())
}
