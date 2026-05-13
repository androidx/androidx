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
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.ValueIntegerExpressionChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/** Reproduction of SwitchWidgetDemo using the procedural Kotlin DSL. */
@Suppress("RestrictedApiAndroidX")
fun RcSwitchWidgetDemo(): RemoteComposeContext {
    return RemoteComposeContextAndroid(
        AndroidxRcPlatformServices(),
        7,
        RemoteComposeWriter.HTag(
            Header.DOC_PROFILES,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
        ),
    ) {
        val checkedA = addNamedInt("checkedA", 0)
        val checkedB = addNamedInt("checkedB", 0)
        val checkedC = addNamedInt("checkedC", 1)

        val animId = Utils.idFromNan(reserveFloatVariable())
        val switchMacroId = DefineSwitchMacro(animId)

        root {
            column(Modifier.fillMaxSize().padding(8f).background(Color.parseColor("#EEEEEE"))) {
                RowSwitch(checkedA, "State A", switchMacroId)

                // Visibility of B and A(repeated) depends on C
                val visibilityC = Modifier.visibility(checkedC.toInt()).animationSpec(animId)
                RowSwitch(checkedB, "State B", switchMacroId, modifier = visibilityC)
                RowSwitch(checkedA, "State A", switchMacroId, modifier = visibilityC)

                RowSwitch(checkedC, "State C", switchMacroId)

                row(
                    Modifier.padding(0f, 8f, 0f, 0f).fillMaxWidth(),
                    horizontal = RowLayout.CENTER,
                    vertical = RowLayout.CENTER,
                ) {
                    StateInfo(checkedA, "A is ", animId = animId)
                    Divider(Modifier.visibility(checkedB.toInt()).animationSpec(animId))
                    StateInfo(
                        checkedB,
                        "B is ",
                        modifier = Modifier.visibility(checkedB.toInt()).animationSpec(animId),
                        animId = animId,
                    )
                    Divider()
                    StateInfo(checkedC, "C is ", animId = animId)
                }
            }
        }
    }
}

private fun RemoteComposeContext.RowSwitch(
    stateId: Long,
    label: String,
    switchMacroId: Int,
    modifier: RecordingModifier = Modifier,
) {
    row(modifier.height(40f), vertical = RowLayout.CENTER) {
        text(label, modifier = Modifier.padding(0f, 0f, 8f, 0f))

        // Call the Switch Macro
        inflatePattern(switchMacroId, stateId.toInt()) {}

        text(" State value is ", modifier = Modifier.padding(8f, 0f, 0f, 0f))
        val listId = addStringList("OFF", "ON")
        text(textLookup(listId, Utils.idFromLong(stateId).toInt()))
    }
}

private fun RemoteComposeContext.StateInfo(
    stateId: Long,
    label: String,
    modifier: RecordingModifier = Modifier,
    animId: Int = -1,
) {
    row(modifier.animationSpec(animId), vertical = RowLayout.CENTER) {
        text(label)
        val listId = addStringList("OFF", "ON")
        text(textLookup(listId, Utils.idFromLong(stateId).toInt()))
    }
}

private fun RemoteComposeContext.Divider(modifier: RecordingModifier = Modifier) {
    box(modifier.padding(8f, 0f, 8f, 0f).width(2f).height(8f).background(Color.LTGRAY))
}

private fun RemoteComposeContextAndroid.DefineSwitchMacro(animId: Int): Int {
    val stateParam = definePatternParameter("state")

    return definePattern("Switch", stateParam) {
        // Toggle action: 1 - state
        // Correct RPN order: 1, state, SUB
        // We use 'or 0x100000000L' to ensure the parameter index is treated as an ID in the
        // expression
        val stateId = stateParam.toLong() + 0x100000000L
        //        val toggleExpr = integerExpression(
        //            1L,
        //            stateId,
        //            Rc.IntegerExpression.L_SUB
        //        )
        val toggleExpr =
            integerExpression(
                stateId,
                1L,
                Rc.IntegerExpression.L_ADD,
                2,
                Rc.IntegerExpression.L_MOD,
            )
        val toggleAction = ValueIntegerExpressionChange(stateId, toggleExpr)

        box(
            Modifier.width(80f).height(37f).padding(4f).onClick(toggleAction),
            BoxLayout.START,
            BoxLayout.TOP,
        ) {
            stateLayout(Modifier.fillMaxSize(), stateParam) {
                // State 0: OFF
                box(
                    Modifier.width(60f)
                        .height(36f)
                        .animationSpec(animId)
                        .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                        .background(Color.parseColor("#646464"))
                        .padding(8f),
                    BoxLayout.START,
                    BoxLayout.CENTER,
                ) {
                    box(
                        Modifier.size(20f)
                            .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                            .background(Color.parseColor("#DCDCDC"))
                    )
                }

                // State 1: ON
                box(
                    Modifier.width(60f)
                        .height(36f)
                        .animationSpec(animId)
                        .clip(RoundedRectShape(20f, 20f, 20f, 20f))
                        .background(Color.parseColor("#3F51B5"))
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
    }
}

@Preview
@Composable
private fun RcSwitchWidgetDemoPreview() {
    RemoteDocumentPreview(RcSwitchWidgetDemo())
}
