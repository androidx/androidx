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

package androidx.compose.remote.integration.demos.integration

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.compose.action.ValueChange
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSpacer
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.combinedClickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Suppress("RestrictedApiAndroidX")
@Composable
fun GesturePropagationDemo() {
    val experimentalProfile =
        Profile(
            RcPlatformProfiles.ANDROIDX.apiLevel,
            RcPlatformProfiles.ANDROIDX.operationsProfiles or RcProfiles.PROFILE_EXPERIMENTAL,
            RcPlatformProfiles.ANDROIDX.platform,
            RcPlatformProfiles.ANDROIDX.profileFactory,
        )
    var composeClickCounter by remember { mutableIntStateOf(0) }
    var composeDoubleClickCounter by remember { mutableIntStateOf(0) }
    var composeLongClickCounter by remember { mutableIntStateOf(0) }
    var remoteComposeClickCounter by remember { mutableIntStateOf(0) }
    var remoteComposeDoubleClickCounter by remember { mutableIntStateOf(0) }
    var remoteComposeLongClickCounter by remember { mutableIntStateOf(0) }
    val remoteComposeClick = "remote_compose_click"
    val remoteComposeDoubleClick = "remote_compose_double_click"
    val remoteComposeLongClick = "remote_compose_long_click"

    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(16.dp)
                .combinedClickable(
                    onClick = { composeClickCounter++ },
                    onDoubleClick = { composeDoubleClickCounter++ },
                    onLongClick = { composeLongClickCounter++ },
                )
    ) {
        Text(
            text = "Gestures on the red boxes should increase the Remote Compose gesture counter. ",
            color = Color.Black,
        )
        Text(
            text = "Gestures on the green boxes should increase the ValueChange counter. ",
            color = Color.Black,
        )
        Text(
            text =
                "Gestures outside the colored boxes should increase the Compose gesture counter. ",
            color = Color.Black,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column {
            val rowModifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            val cellModifier = Modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Black)
            Row(modifier = rowModifier) {
                Cell { Text(text = " ") }
                Cell { Text(text = "Click") }
                Cell { Text(text = "Double") }
                Cell { Text(text = "Long") }
            }
            Row(modifier = rowModifier) {
                Box(modifier = cellModifier, contentAlignment = Alignment.CenterStart) {
                    Text(text = "Compose", modifier = Modifier.padding(horizontal = 4.dp))
                }
                Cell { Text(text = "$composeClickCounter") }
                Cell { Text(text = "$composeDoubleClickCounter") }
                Cell { Text(text = "$composeLongClickCounter") }
            }
            Row(modifier = rowModifier) {
                Box(modifier = cellModifier, contentAlignment = Alignment.CenterStart) {
                    Text(text = "Remote Compose", modifier = Modifier.padding(horizontal = 4.dp))
                }
                Cell { Text(text = "$remoteComposeClickCounter") }
                Cell { Text(text = "$remoteComposeDoubleClickCounter") }
                Cell { Text(text = "$remoteComposeLongClickCounter") }
            }
        }
        RemoteDemo(
            modifier = Modifier.fillMaxSize().padding(10.dp).border(1.dp, Color.Blue),
            profile = experimentalProfile,
            onNamedAction = { name, _, _ ->
                when (name) {
                    remoteComposeClick -> remoteComposeClickCounter++
                    remoteComposeDoubleClick -> remoteComposeDoubleClickCounter++
                    remoteComposeLongClick -> remoteComposeLongClickCounter++
                }
            },
        ) {
            val clickCount = rememberMutableRemoteInt(0)
            val doubleClickCount = rememberMutableRemoteInt(0)
            val longClickCount = rememberMutableRemoteInt(0)
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize().padding(5.rdp),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
            ) {
                RemoteText("Clickable:".rs, color = Color.Black.rc)
                RemoteRow {
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(80.rdp)
                                .background(RemoteColor(Color.Red))
                                .clickable(hostAction(remoteComposeClick.rs)),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText("HostAction.".rs)
                    }
                    RemoteSpacer(modifier = RemoteModifier.width(5.rdp))
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(80.rdp)
                                .background(RemoteColor(Color.Green))
                                .clickable(ValueChange(clickCount, clickCount + 1)),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText("ValueChange.".rs, color = Color.Black.rc)
                    }
                }
                RemoteSpacer(modifier = RemoteModifier.height(10.rdp))
                RemoteText("CombinedClickable:".rs, color = Color.Black.rc)
                RemoteRow {
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(80.rdp)
                                .background(RemoteColor(Color.Red))
                                .combinedClickable(
                                    onClick = hostAction(remoteComposeClick.rs),
                                    onDoubleClick = hostAction(remoteComposeDoubleClick.rs),
                                    onLongClick = hostAction(remoteComposeLongClick.rs),
                                ),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText("HostAction.".rs)
                    }
                    RemoteSpacer(modifier = RemoteModifier.width(5.rdp))
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(80.rdp)
                                .background(RemoteColor(Color.Green))
                                .combinedClickable(
                                    onClick = ValueChange(clickCount, clickCount + 1),
                                    onDoubleClick =
                                        ValueChange(doubleClickCount, doubleClickCount + 1),
                                    onLongClick = ValueChange(longClickCount, longClickCount + 1),
                                ),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText("ValueChange.".rs, color = Color.Black.rc)
                    }
                }
                RemoteSpacer(modifier = RemoteModifier.height(10.rdp))
                RemoteText(
                    "ValueChange click counter: ".rs + clickCount.toRemoteString(),
                    color = Color.Black.rc,
                )
                RemoteText(
                    "ValueChange double click counter: ".rs + doubleClickCount.toRemoteString(),
                    color = Color.Black.rc,
                )
                RemoteText(
                    "ValueChange long click counter: ".rs + longClickCount.toRemoteString(),
                    color = Color.Black.rc,
                )
            }
        }
    }
}

@Composable
private fun RowScope.Cell(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.weight(1f).fillMaxHeight().border(1.dp, Color.Black),
        contentAlignment = contentAlignment,
        content = content,
    )
}
