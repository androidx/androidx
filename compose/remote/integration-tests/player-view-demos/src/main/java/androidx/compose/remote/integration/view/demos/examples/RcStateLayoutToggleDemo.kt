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
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

/** Demo showing a StateLayout transition using the Compose-like RemoteCompose DSL. */
@Composable
@RemoteComposable
fun StateLayoutToggleDemo() {
    val isEndState = rememberMutableRemoteBoolean(false)

    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize().padding(16.rdp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
    ) {
        RemoteStateLayout(
            currentState = isEndState,
            modifier = RemoteModifier.fillMaxWidth().height(120.rdp),
        ) { isEnd ->
            if (!isEnd) {
                // State 0: Row on left
                RemoteRow(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(100.rdp)
                            .background(Color.LightGray.rc),
                    horizontalArrangement = RemoteArrangement.Start,
                    verticalAlignment = RemoteAlignment.CenterVertically,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.animationSpec(100, true)
                                .size(50.rdp)
                                .background(Color.Red.rc)
                    )
                }
            } else {
                // State 1: Row at end
                RemoteRow(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(100.rdp)
                            .background(Color.LightGray.rc),
                    horizontalArrangement = RemoteArrangement.End,
                    verticalAlignment = RemoteAlignment.CenterVertically,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.animationSpec(100, true)
                                .size(50.rdp)
                                .background(Color.Red.rc)
                    )
                }
            }
        }

        RemoteBox(modifier = RemoteModifier.size(24.rdp))

        // Interactive toggle button underneath
        RemoteBox(
            modifier =
                RemoteModifier.width(160.rdp)
                    .height(48.rdp)
                    .clip(RemoteRoundedCornerShape(12.rdp))
                    .background(Color.DarkGray.rc)
                    .clickable(valueChange(isEndState, !isEndState)),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText("Toggle State".rs, color = Color.White.rc, fontSize = 18.rsp)
        }
    }
}

@Preview
@Composable
private fun StateLayoutToggleDemoPreview() = RemoteContentPreview { StateLayoutToggleDemo() }
