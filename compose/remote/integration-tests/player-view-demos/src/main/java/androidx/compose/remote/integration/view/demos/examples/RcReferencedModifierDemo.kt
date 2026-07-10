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

import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
fun RcReferencedModifierDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        // Define a shared set of modifiers
        val sharedModifierId =
            referencedModifiers(
                Modifier.padding(16)
                    .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                    .background(0xFFE0E0E0.toInt())
                    .padding(24)
            )

        root {
            column(Modifier.fillMaxSize(), horizontal = ColumnLayout.CENTER) {
                text("Referenced Modifiers Demo", fontSize = 60f, modifier = Modifier.padding(20))

                text("The buttons below use a shared modifier container", fontSize = 30f)

                box(Modifier.include(sharedModifierId)) { text("Button 1", fontSize = 30f) }

                box(Modifier.include(sharedModifierId).padding(20f, 20f, 20f, 20f)) {
                    text("Button 2 (with extra padding)", fontSize = 30f)
                }

                box(
                    Modifier.padding(16)
                        .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                        .background(0xFFBBDEFB.toInt()) // Light blue
                        .padding(24)
                ) {
                    text("Normal Button", fontSize = 30f)
                }
            }
        }
    }
}

@Preview
@Composable
private fun RcReferencedModifierDemoPreview() {
    RemoteDocumentPreview(RcReferencedModifierDemo())
}
