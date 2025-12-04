/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.creation.compose.action.ValueChange
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.LogTodo
import androidx.compose.remote.creation.compose.capture.NoRemoteCompose
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteContext
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.layout.StateLayout
import androidx.compose.remote.creation.compose.layout.createIds
import androidx.compose.remote.creation.compose.layout.rememberRemoteStringList
import androidx.compose.remote.creation.compose.layout.rememberStateMachine
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.modifier.wrapContentSize
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberRemoteInt
import androidx.compose.remote.creation.compose.state.rememberRemoteIntValue
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
fun SwitchWidgetOnState(modifier: RemoteModifier = RemoteModifier, id: Int = 0) {
    RemoteContext {
        Box(
            modifier =
                modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(63, 81, 181, 255))
                    .padding(2.dp),
            horizontalAlignment = RemoteAlignment.End,
            verticalArrangement = RemoteArrangement.Center,
        ) {
            Canvas(modifier = RemoteModifier.size(32.rdp)) {
                val color = Color(255, 255, 255)
                drawCircle(color = color, radius = 34f.rf)
            }
        }
    }
}

@Preview @Composable fun SwitchWidgetOnStatePreview() = RemotePreview { SwitchWidgetOnState() }

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
fun SwitchWidgetOffState(modifier: RemoteModifier = RemoteModifier, id: Int = 0) {
    RemoteContext {
        Box(
            modifier =
                modifier
                    // todo: use the animationId
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(100, 100, 100))
                    .padding(8.dp)
                    .then(modifier),
            horizontalAlignment = RemoteAlignment.Start,
            verticalArrangement = RemoteArrangement.Center,
        ) {
            Canvas(modifier = RemoteModifier.size(20.rdp)) {
                val color = Color(220, 220, 220)
                drawCircle(color = color, radius = 30f.rf)
            }
        }
    }
}

@Preview @Composable fun SwitchWidgetOffStatePreview() = RemotePreview { SwitchWidgetOffState() }

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
fun RemoteComponent(name: String, content: @Composable @RemoteComposable () -> Unit) {}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
fun SwitchComponent(value: MutableRemoteInt) {
    RemoteContext {
        RemoteComponent("switch") {
            // val localValue = parameter(value)
            SwitchWidget(value)
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
fun SwitchWidget(value: MutableRemoteInt) {
    RemoteContext {
        LogTodo("fix rememberStateMachine/createIds in Previews")
        val off = remember { 0 }
        val on = remember { 1 }
        val (id1) = remember { createIds }
        val fsm = rememberStateMachine(value, off, on)

        val captureMode = LocalRemoteComposeCreationState.current
        val modifier =
            if (captureMode is NoRemoteCompose) {
                LogTodo("support expressions in previews")
                RemoteModifier.clickable(
                    ValueChange(
                        fsm.currentState as MutableRemoteInt,
                        (fsm.currentState.constantValue!! + 1) % 2,
                    )
                )
            } else {
                val toggleExpression = rememberRemoteInt { (fsm.currentState + 1) % 2 }
                RemoteModifier.clickable(
                    ValueChange(fsm.currentState as MutableRemoteInt, toggleExpression)
                )
            }

        Box(
            modifier = RemoteModifier.padding(4.dp),
            verticalArrangement = RemoteArrangement.Center,
        ) {
            val modifierSize = RemoteModifier.size(60.rdp, 36.rdp)
            StateLayout(modifier = RemoteModifier.wrapContentSize(), stateMachine = fsm) { state ->
                Box {
                    when (state) {
                        off -> {
                            SwitchWidgetOffState(modifier = modifierSize, id = id1)
                        }
                        on -> {
                            SwitchWidgetOnState(modifier = modifierSize, id = id1)
                        }
                    }
                }
            }
            Box(modifier = modifierSize.clip(RoundedCornerShape(20.dp)).then(modifier))
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
fun RowSwitch(state: MutableRemoteInt, label: String, modifier: RemoteModifier = RemoteModifier) {
    RemoteContext {
        Row(modifier = modifier, verticalAlignment = RemoteAlignment.CenterVertically) {
            RemoteText(label)
            SwitchWidget(state)
            RemoteText("State value is ")
            val list = rememberRemoteStringList("OFF", "ON")
            RemoteText(list[state])
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
fun StateInfo(state: RemoteInt, label: String, modifier: RemoteModifier = RemoteModifier) {
    RemoteContext {
        Row(modifier = modifier, verticalAlignment = RemoteAlignment.CenterVertically) {
            RemoteText(label)
            val list = rememberRemoteStringList("OFF", "ON")
            RemoteText(list[state])
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
fun Divider(modifier: RemoteModifier = RemoteModifier) {
    RemoteContext {
        Box(
            modifier =
                modifier
                    .padding(left = 8.dp, right = 8.dp)
                    .size(2.rdp, 8.rdp)
                    .background(Color.LightGray)
        )
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
fun SwitchWidgetDemo() {
    RemoteContext {
        Column(modifier = Modifier.padding(8.dp).background(Color.LightGray)) {
            val checkedA = rememberRemoteIntValue { 0 }
            val checkedB = rememberRemoteIntValue { 0 }
            val checkedC = rememberRemoteIntValue { 1 }

            val visibilityModifierC = RemoteModifier.visibility(checkedC)
            RowSwitch(checkedA, "State A")
            RowSwitch(checkedB, "State B", modifier = visibilityModifierC)
            RowSwitch(checkedA, "State A", modifier = visibilityModifierC)
            RowSwitch(checkedC, "State C")
            Row(
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                horizontalArrangement = RemoteArrangement.CenterHorizontally,
                verticalAlignment = RemoteAlignment.CenterVertically,
            ) {
                val visibilityModifierB = RemoteModifier.visibility(checkedB)
                StateInfo(checkedA, "A is ")
                Divider(modifier = visibilityModifierB)
                StateInfo(checkedB, "B is ", modifier = visibilityModifierB)
                Divider()
                StateInfo(checkedC, "C is ")
            }
        }
    }
}

@Preview @Composable fun SwitchWidgetDemoPreview() = RemotePreview { SwitchWidgetDemo() }
