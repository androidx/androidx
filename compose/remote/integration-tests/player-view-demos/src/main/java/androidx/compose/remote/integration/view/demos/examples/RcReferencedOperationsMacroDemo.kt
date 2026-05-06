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

import android.annotation.SuppressLint
import android.graphics.Color
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
fun RcReferencedOperationsMacroDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        DefineCardMacro()

        root {
            column(Modifier.fillMaxSize(), horizontal = ColumnLayout.CENTER) {
                text(
                    "Macros with Content Inclusion",
                    fontSize = 50f,
                    modifier = Modifier.padding(20),
                )

                // Pass the component block directly to the macro helper
                CardMacro("Section Alpha") {
                    text("First item in A", fontSize = 25f)
                    text("Second item in A", fontSize = 25f)
                    box(Modifier.padding(5).background(Color.LTGRAY)) {
                        text("Nested box in A", fontSize = 20f)
                    }
                }

                box(Modifier.size(20))

                CardMacro("Section Beta") {
                    flow(Modifier.fillMaxWidth().background(0xFFE8F5E9.toInt()).padding(10)) {
                        text(
                            "Row Item 1",
                            Modifier.padding(8f).background(Color.YELLOW),
                            fontSize = 25f,
                        )
                        text(
                            "Row Item 2",
                            Modifier.padding(8f).background(Color.CYAN),
                            fontSize = 25f,
                        )
                    }
                    text("Footer in B", fontSize = 20f, color = Color.GRAY)
                }
            }
        }
    }
}

@SuppressLint("RestrictedApiAndroidX")
@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.CardMacro(
    title: String,
    content: RemoteComposeContextAndroid.() -> Unit,
) {
    // Internally capture the components into a referenced operations block
    val contentId = referencedOperations { (this as RemoteComposeContextAndroid).content() }

    val titleId = textId(title)
    val macroNameId = textId("CardMacro")
    inflatePattern(macroNameId, titleId, contentId) {}
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteComposeContextAndroid.DefineCardMacro() {
    val titleParam = definePatternParameter("title")
    val contentParam = definePatternParameter("contentId")

    definePattern("CardMacro", titleParam, contentParam) {
        column(
            Modifier.padding(16)
                .clip(RoundedRectShape(12f, 12f, 12f, 12f))
                .background(Color.WHITE)
                .border(2f, 12f, Color.GRAY, 2)
                .padding(16)
                .fillMaxWidth()
        ) {
            text(
                titleParam,
                fontSize = 35f,
                color = Color.BLUE,
                modifier = Modifier.padding(0f, 0f, 0f, 10f),
            )

            // Draw a separator
            canvas(Modifier.fillMaxWidth().height(2)) { drawLine(0f, 0f, windowWidth(), 0f) }

            box(Modifier.size(10))

            // HERE is where we include the passed content
            include(contentParam)
        }
    }
}

@Preview
@Composable
private fun RcReferencedOperationsMacroDemoPreview() {
    RemoteDocPreview(RcReferencedOperationsMacroDemo())
}
