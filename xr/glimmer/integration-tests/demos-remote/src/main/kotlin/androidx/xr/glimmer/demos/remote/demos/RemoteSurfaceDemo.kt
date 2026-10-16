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

package androidx.xr.glimmer.demos.remote.demos

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.onTouchCancel
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.modifier.onTouchUp
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteBoolean
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.demos.remote.ComposableDemo
import androidx.xr.glimmer.demos.remote.DemoCategory
import androidx.xr.glimmer.remote.surface

internal val RemoteSurfaceDemos =
    DemoCategory(
        "Remote Surface",
        listOf(
            ComposableDemo("Static surfaces") { StaticSurfacesDemo() },
            ComposableDemo("Player state") { PlayerStateSurfacesDemo() },
            ComposableDemo("Host state") { HostStateSurfaceDemo() },
            ComposableDemo("Host action") { HostActionSurfaceDemo() },
        ),
    )

@Composable
private fun StaticSurfacesDemo() {
    val documentBytes by rememberRemoteDocumentState {
        RemoteColumn(
            verticalArrangement = RemoteArrangement.spacedBy(24.rdp),
            modifier = RemoteModifier.padding(2.rdp),
        ) {
            ListItem(
                text = "Enabled",
                modifier =
                    RemoteModifier.surface(
                        enabled = true.rb,
                        focused = false.rb,
                        pressed = false.rb,
                    ),
            )
            ListItem(
                text = "Focused",
                modifier =
                    RemoteModifier.surface(
                        enabled = true.rb,
                        focused = true.rb,
                        pressed = false.rb,
                    ),
            )
            ListItem(
                text = "Pressed",
                modifier =
                    RemoteModifier.surface(enabled = true.rb, focused = true.rb, pressed = true.rb),
            )
            ListItem(
                text = "Disabled",
                modifier =
                    RemoteModifier.surface(
                        enabled = false.rb,
                        focused = false.rb,
                        pressed = false.rb,
                    ),
            )
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedRemoteDocumentPlayer(documentBytes = documentBytes, modifier = Modifier.weight(1f))
        SaveRemoteDocumentButton(fileName = "static_surfaces.rc", documentBytes = documentBytes)
    }
}

@Composable
private fun PlayerStateSurfacesDemo() {
    val documentBytes by rememberRemoteDocumentState {
        val focusedIndex = remember { MutableRemoteInt(0) }
        RemoteColumn(
            verticalArrangement = RemoteArrangement.spacedBy(24.rdp),
            modifier = RemoteModifier.padding(2.rdp),
        ) {
            repeat(2) { index ->
                val pressed = remember { MutableRemoteBoolean(false) }
                ListItem(
                    text = "Button ${index + 1}",
                    modifier =
                        RemoteModifier.onTouchDown(
                                combinedAction(
                                    valueChangeAction(focusedIndex, index.ri),
                                    valueChangeAction(pressed, true.rb),
                                )
                            )
                            .onTouchUp(valueChangeAction(pressed, false.rb))
                            .onTouchCancel(valueChangeAction(pressed, false.rb))
                            .surface(pressed = pressed, focused = focusedIndex.isEqualTo(index.ri)),
                )
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedRemoteDocumentPlayer(documentBytes = documentBytes, modifier = Modifier.weight(1f))
        SaveRemoteDocumentButton(
            fileName = "player_state_surfaces.rc",
            documentBytes = documentBytes,
        )
    }
}

@Composable
private fun HostStateSurfaceDemo() {
    val remoteBooleanName = "Surface focused state"
    val documentBytes: ByteArray? by rememberRemoteDocumentState {
        // TODO: b/574504841 - The animation doesn't work when flips from `true` to `false`.
        val remoteFocused =
            rememberNamedRemoteBoolean(name = remoteBooleanName, initialValue = false)
        RemoteColumn(
            verticalArrangement = RemoteArrangement.spacedBy(24.rdp),
            modifier = RemoteModifier.padding(2.rdp),
        ) {
            ListItem(
                text = "Focused by the host",
                modifier = RemoteModifier.surface(focused = remoteFocused),
            )
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        var focused by remember { mutableStateOf(false) }
        OutlinedRemoteDocumentPlayer(
            documentBytes = documentBytes,
            modifier = Modifier.weight(1f),
            update = { player ->
                @SuppressLint("RestrictedApiAndroidX")
                player.setUserLocalInt(remoteBooleanName, if (focused) 1 else 0)
            },
        )
        Button(onClick = { focused = !focused }, modifier = Modifier.padding(24.dp)) {
            Text(if (focused) "Unfocus surface" else "Focus surface")
        }
    }
}

@Composable
private fun HostActionSurfaceDemo() {
    val documentBytes: ByteArray? by rememberRemoteDocumentState {
        val pressed = remember { MutableRemoteBoolean(false) }
        RemoteColumn(
            verticalArrangement = RemoteArrangement.spacedBy(24.rdp),
            modifier = RemoteModifier.padding(2.rdp),
        ) {
            ListItem(
                text = "Click me",
                modifier =
                    RemoteModifier.onTouchDown(valueChangeAction(pressed, true.rb))
                        .onTouchUp(
                            combinedAction(
                                valueChangeAction(pressed, false.rb),
                                hostAction("onSurfaceClick".rs),
                            )
                        )
                        .onTouchCancel(valueChangeAction(pressed, false.rb))
                        .surface(pressed = pressed, focused = true.rb),
            )
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        var clickCount by remember { mutableIntStateOf(0) }
        Text(text = "Host received $clickCount clicks", modifier = Modifier.padding(24.dp))
        OutlinedRemoteDocumentPlayer(
            documentBytes = documentBytes,
            modifier = Modifier.weight(1f),
            onNamedAction = { name, _, _ -> if (name == "onSurfaceClick") ++clickCount },
        )
    }
}

@SuppressLint("RestrictedApiAndroidX")
@Composable
@RemoteComposable
private fun ListItem(text: String, modifier: RemoteModifier) {
    RemoteBox(modifier) {
        // TODO: b/560193421 - Replace once the real text is ready.
        RemoteText(
            text = text.rs,
            color = Color.White.rc,
            fontSize = 9.rsp,
            modifier = RemoteModifier.fillMaxWidth().padding(20.rdp),
        )
    }
}
