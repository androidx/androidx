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
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.ValueIntegerExpressionChange
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.animationSpec
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable

/**
 * Demo showing a StateLayout transition with 3 colored boxes. State 0 arranges the 3 boxes
 * horizontally in a Row (fillMaxWidth) and State 1 arranges them vertically in a Column
 * (fillMaxHeight). An interactive button underneath toggles between the states.
 */
fun RcStateLayoutRowToColumnDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        val stateVar = addNamedInt("state", 0)

        // Toggle action: (stateVar + 1) % 2
        val toggleExpr =
            integerExpression(
                stateVar,
                1L,
                Rc.IntegerExpression.L_ADD,
                2,
                Rc.IntegerExpression.L_MOD,
            )
        val toggleAction = ValueIntegerExpressionChange(stateVar, toggleExpr)

        root {
            column(
                Modifier.fillMaxSize().padding(16f),
                horizontal = ColumnLayout.CENTER,
                vertical = ColumnLayout.CENTER,
            ) {
                stateLayout(Modifier.fillMaxWidth().height(260f), stateVar.toInt()) {
                    // State 0: 3 boxes in a Row (fillMaxWidth)
                    row(
                        Modifier.fillMaxWidth().height(260f).background(Color.LTGRAY),
                        horizontal = RowLayout.SPACE_EVENLY,
                        vertical = RowLayout.CENTER,
                    ) {
                        box(Modifier.size(60).animationSpec(100).background(Color.RED))
                        box(
                            Modifier.size(60)
                                .horizontalWeight(1f)
                                .animationSpec(101)
                                .background(Color.GREEN)
                        )
                        box(Modifier.size(60).animationSpec(102).background(Color.BLUE))
                    }

                    // State 1: 3 boxes in a Column (fillMaxHeight)
                    column(
                        Modifier.fillMaxWidth().height(260f).background(Color.LTGRAY),
                        horizontal = ColumnLayout.CENTER,
                        vertical = ColumnLayout.SPACE_EVENLY,
                    ) {
                        box(Modifier.size(60).animationSpec(100).background(Color.RED))
                        box(Modifier.size(120).animationSpec(101).background(Color.GREEN))
                        box(Modifier.size(60).animationSpec(102).background(Color.BLUE))
                    }
                }

                box(Modifier.size(16f))

                // Interactive toggle button underneath
                box(
                    Modifier.width(160f)
                        .height(48f)
                        .clip(RoundedRectShape(12f, 12f, 12f, 12f))
                        .background(Color.DKGRAY)
                        .onClick(toggleAction),
                    BoxLayout.CENTER,
                    BoxLayout.CENTER,
                ) {
                    text("Toggle State", color = Color.WHITE, fontSize = 18f)
                }
            }
        }
    }
}

/** Demo showing a StateLayout transition using the Compose-like RemoteCompose DSL. */
@Composable
@RemoteComposable
fun StateLayoutRowToColumnDemo() {
    val isEndState = rememberMutableRemoteBoolean(false)

    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize().padding(16.rdp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
    ) {
        RemoteStateLayout(
            currentState = isEndState,
            modifier = RemoteModifier.fillMaxWidth().height(260.rdp),
        ) { isEnd ->
            if (!isEnd) {
                // State 0: 3 boxes in a Row
                RemoteRow(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(260.rdp)
                            .background(androidx.compose.ui.graphics.Color.LightGray),
                    horizontalArrangement = RemoteArrangement.SpaceEvenly,
                    verticalAlignment = RemoteAlignment.CenterVertically,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.animationSpec(100, true)
                                .size(60.rdp)
                                .background(androidx.compose.ui.graphics.Color.Red)
                    )
                    RemoteBox(
                        modifier =
                            RemoteModifier.animationSpec(101, true)
                                .size(60.rdp)
                                .weight(1f)
                                .background(androidx.compose.ui.graphics.Color.Green)
                    )
                    RemoteBox(
                        modifier =
                            RemoteModifier.animationSpec(102, true)
                                .size(60.rdp)
                                .background(androidx.compose.ui.graphics.Color.Blue)
                    )
                }
            } else {
                // State 1: 3 boxes in a Column
                RemoteColumn(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(260.rdp)
                            .background(androidx.compose.ui.graphics.Color.LightGray),
                    horizontalAlignment = RemoteAlignment.CenterHorizontally,
                    verticalArrangement = RemoteArrangement.SpaceEvenly,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.animationSpec(100, true)
                                .size(60.rdp)
                                .background(androidx.compose.ui.graphics.Color.Red)
                    )
                    RemoteBox(
                        modifier =
                            RemoteModifier.animationSpec(101, true)
                                .size(120.rdp)
                                .background(androidx.compose.ui.graphics.Color.Green)
                    )
                    RemoteBox(
                        modifier =
                            RemoteModifier.animationSpec(102, true)
                                .size(60.rdp)
                                .background(androidx.compose.ui.graphics.Color.Blue)
                    )
                }
            }
        }

        RemoteBox(modifier = RemoteModifier.size(16.rdp))

        // Interactive toggle button underneath
        RemoteBox(
            modifier =
                RemoteModifier.width(160.rdp)
                    .height(48.rdp)
                    .clip(RemoteRoundedCornerShape(12.rdp))
                    .background(androidx.compose.ui.graphics.Color.DarkGray)
                    .clickable(valueChange(isEndState, !isEndState)),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText(
                "Toggle State",
                color = androidx.compose.ui.graphics.Color.White.rc,
                fontSize = 18.rsp,
            )
        }
    }
}
