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
@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos.examples

import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.creation.compose.action.ValueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow as Row
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier as Modifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.modifier.wrapContentSize
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.MutableRemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteEnum
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.integration.view.demos.examples.SwitchState.*
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Composable
@RemoteComposable
fun SwitchWidgetOnState(modifier: RemoteModifier = RemoteModifier, id: Int = 0) {
    RemoteBox(
        modifier =
            modifier
                .clip(RemoteRoundedCornerShape(20.rdp))
                .background(Color(63, 81, 181, 255))
                .padding(2.rdp),
        contentAlignment = RemoteAlignment.CenterEnd,
    ) {
        RemoteCanvas(modifier = RemoteModifier.size(32.rdp)) {
            val paint = RemotePaint().apply { color = Color(255, 255, 255).rc }
            drawCircle(paint = paint, radius = 34f.rf)
        }
    }
}

@Preview
@Composable
private fun SwitchWidgetOnStatePreview() = RemoteContentPreview { SwitchWidgetOnState() }

@Composable
@RemoteComposable
fun SwitchWidgetOffState(modifier: RemoteModifier = RemoteModifier) {
    RemoteBox(
        modifier =
            modifier
                // todo: use the animationId
                .clip(RemoteRoundedCornerShape(20.rdp))
                .background(Color(100, 100, 100))
                .padding(8.rdp)
                .then(modifier),
        contentAlignment = RemoteAlignment.CenterStart,
    ) {
        RemoteCanvas(modifier = RemoteModifier.size(20.rdp)) {
            val paint = RemotePaint().apply { color = Color(220, 220, 220).rc }
            drawCircle(paint = paint, radius = 34f.rf)
        }
    }
}

@Preview
@Composable
private fun SwitchWidgetOffStatePreview() = RemoteContentPreview { SwitchWidgetOffState() }

@Composable
@RemoteComposable
fun RemoteComponent(name: String, content: @Composable @RemoteComposable () -> Unit) {
    content()
}

@Composable
@RemoteComposable
fun SwitchComponent(value: MutableRemoteEnum<SwitchState>) {
    RemoteComponent("switch") { SwitchWidget(value) }
}

enum class SwitchState(val visibility: RemoteInt) {
    Off(Component.Visibility.GONE.ri),
    On(Component.Visibility.VISIBLE.ri),
}

@Composable
@RemoteComposable
fun SwitchWidget(value: MutableRemoteEnum<SwitchState>) {
    val modifier =
        RemoteModifier.clickable(
            ValueChange(remoteState = value.remoteInt, updatedValue = (value.remoteInt + 1) % 2)
        )

    RemoteBox(
        modifier = RemoteModifier.padding(4.rdp),
        contentAlignment = RemoteAlignment.CenterStart,
    ) {
        val modifierSize = RemoteModifier.size(60.rdp, 36.rdp)
        RemoteStateLayout(modifier = RemoteModifier.wrapContentSize(), state = value) { state ->
            RemoteBox {
                when (state) {
                    Off -> SwitchWidgetOffState(modifier = modifierSize)

                    On -> SwitchWidgetOnState(modifier = modifierSize)
                }
            }
        }
        RemoteBox(modifier = modifierSize.clip(RemoteRoundedCornerShape(20.rdp)).then(modifier))
    }
}

@Composable
@RemoteComposable
fun RowSwitch(
    state: MutableRemoteEnum<SwitchState>,
    label: String,
    modifier: RemoteModifier = RemoteModifier,
) {
    Row(modifier = modifier, verticalAlignment = RemoteAlignment.CenterVertically) {
        RemoteText(label)
        SwitchWidget(state)
        RemoteText("State value is ")
        RemoteText(state.toRemoteString { it.name.rs })
    }
}

@Composable
@RemoteComposable
fun StateInfo(
    state: RemoteEnum<SwitchState>,
    label: String,
    modifier: RemoteModifier = RemoteModifier,
) {
    Row(modifier = modifier, verticalAlignment = RemoteAlignment.CenterVertically) {
        RemoteText(label)
        RemoteText(state.toRemoteString { it.name.rs })
    }
}

@Composable
@RemoteComposable
fun Divider(modifier: RemoteModifier = RemoteModifier) {
    RemoteBox(
        modifier =
            modifier
                .padding(start = 8.rdp, end = 8.rdp)
                .size(2.rdp, 8.rdp)
                .background(Color.LightGray)
    )
}

@Composable
@RemoteComposable
fun SwitchWidgetDemo() {
    RemoteColumn(modifier = Modifier.padding(8.rdp).background(Color.LightGray)) {
        val checkedA = rememberMutableRemoteEnum(Off)
        val checkedB = rememberMutableRemoteEnum(Off)
        val checkedC = rememberMutableRemoteEnum(On)

        val visibilityModifierC = RemoteModifier.visibility(checkedC.visibility)
        RowSwitch(checkedA, "State A")
        RowSwitch(checkedB, "State B", modifier = visibilityModifierC)
        RowSwitch(checkedA, "State A", modifier = visibilityModifierC)
        RowSwitch(checkedC, "State C")
        Row(
            modifier = Modifier.padding(top = 8.rdp).fillMaxWidth(),
            horizontalArrangement = RemoteArrangement.Center,
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            val visibilityModifierB = RemoteModifier.visibility(checkedB.visibility)
            StateInfo(checkedA, "A is ")
            Divider(modifier = visibilityModifierB)
            StateInfo(checkedB, "B is ", modifier = visibilityModifierB)
            Divider()
            StateInfo(checkedC, "C is ")
        }
    }
}

val RemoteEnum<SwitchState>.visibility: RemoteInt
    get() = toRemoteInt { it.visibility }

@Preview
@Composable
private fun SwitchWidgetDemoPreview() = RemoteContentPreview { SwitchWidgetDemo() }
