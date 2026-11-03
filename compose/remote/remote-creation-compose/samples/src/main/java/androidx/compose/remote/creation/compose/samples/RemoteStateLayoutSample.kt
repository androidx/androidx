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

package androidx.compose.remote.creation.compose.samples

import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.MutableRemoteEnum
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteEnum
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@Preview
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteStateLayoutBooleanSample() {
    val state = remember { MutableRemoteBoolean(true) }
    RemoteStateLayout(
        currentState = state,
        modifier = RemoteModifier.clickable(valueChangeAction(state, !state)).padding(8.rdp),
    ) { isTrue ->
        if (isTrue) {
            RemoteText("True State (tap to toggle)".rs, color = Color.Green.rc)
        } else {
            RemoteText("False State (tap to toggle)".rs, color = Color.Red.rc)
        }
    }
}

@Sampled
@Preview
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteStateLayoutIntSample() {
    val state = remember { MutableRemoteInt(0) }
    RemoteStateLayout(currentState = state, 0, 1, 2) { index ->
        when (index) {
            0 ->
                RemoteText(
                    "State 0 (tap to advance)".rs,
                    color = Color.Red.rc,
                    modifier =
                        RemoteModifier.clickable(valueChangeAction(state, 1.ri)).padding(8.rdp),
                )
            1 ->
                RemoteText(
                    "State 1 (tap to advance)".rs,
                    color = Color.Green.rc,
                    modifier =
                        RemoteModifier.clickable(valueChangeAction(state, 2.ri)).padding(8.rdp),
                )
            2 ->
                RemoteText(
                    "State 2 (tap to reset)".rs,
                    color = Color.Blue.rc,
                    modifier =
                        RemoteModifier.clickable(valueChangeAction(state, 0.ri)).padding(8.rdp),
                )
        }
    }
}

enum class SampleState {
    Loading,
    Success,
    Error,
}

@Sampled
@Preview
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteStateLayoutEnumSample() {
    val state = remember { MutableRemoteEnum(SampleState.Loading) }
    RemoteStateLayout(currentState = state) { screen ->
        when (screen) {
            SampleState.Loading ->
                RemoteText(
                    "Loading... (tap for Success)".rs,
                    color = Color.Gray.rc,
                    modifier =
                        RemoteModifier.clickable(
                                valueChangeAction(state, RemoteEnum(SampleState.Success))
                            )
                            .padding(8.rdp),
                )
            SampleState.Success ->
                RemoteText(
                    "Success! (tap for Error)".rs,
                    color = Color.Green.rc,
                    modifier =
                        RemoteModifier.clickable(
                                valueChangeAction(state, RemoteEnum(SampleState.Error))
                            )
                            .padding(8.rdp),
                )
            SampleState.Error ->
                RemoteText(
                    "Error occurred (tap to retry)".rs,
                    color = Color.Red.rc,
                    modifier =
                        RemoteModifier.clickable(
                                valueChangeAction(state, RemoteEnum(SampleState.Loading))
                            )
                            .padding(8.rdp),
                )
        }
    }
}
