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
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.ValueIntegerExpressionChange
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.toInt
import androidx.compose.ui.tooling.preview.Preview

/** Simple demo with a single switch to verify basic functionality. */
@Suppress("RestrictedApiAndroidX")
fun RcSimpleSwitchDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        val checked = addNamedInt("checked", 0)

        root {
            column(Modifier.fillMaxSize().padding(20f), horizontal = ColumnLayout.CENTER) {
                text("Simple Switch Demo", fontSize = 30f)

                box(Modifier.size(20f))

                // Toggle action: 1 - checked
                val toggleExpr =
                    integerExpression(
                        checked,
                        1L,
                        Rc.IntegerExpression.L_ADD,
                        2,
                        Rc.IntegerExpression.L_MOD,
                    )
                val toggleAction = ValueIntegerExpressionChange(checked, toggleExpr)

                box(Modifier.padding(4f).onClick(toggleAction)) {
                    stateLayout(Modifier, checked.toInt()) {
                        // OFF state
                        box(
                            Modifier.width(60f)
                                .height(36f)
                                .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                                .background(Color.GRAY)
                                .padding(8f),
                            BoxLayout.START,
                            BoxLayout.CENTER,
                        ) {
                            box(
                                Modifier.size(20f)
                                    .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                                    .background(Color.WHITE)
                            )
                        }

                        // ON state
                        box(
                            Modifier.width(60f)
                                .height(36f)
                                .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                                .background(Color.BLUE)
                                .padding(2f),
                            BoxLayout.END,
                            BoxLayout.CENTER,
                        ) {
                            box(
                                Modifier.size(32f)
                                    .clip(RoundedRectShape(16f, 16f, 16f, 16f))
                                    .background(Color.WHITE)
                            )
                        }
                    }
                }

                box(Modifier.size(20f))

                val listId = addStringList("OFF", "ON")
                text(textLookup(listId, (checked - 0x100000000L).toInt()), fontSize = 24f)
            }
        }
    }
}

@Preview
@Composable
private fun RcSimpleSwitchDemoPreview() {
    RemoteDocumentPreview(RcSimpleSwitchDemo())
}
