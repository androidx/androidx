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
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteEnum
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteStateLayoutBooleanSample() {
    val state = rememberMutableRemoteBoolean(true)
    RemoteStateLayout(currentState = state) { isTrue ->
        if (isTrue) {
            RemoteText("True State".rs, color = Color.Green.rc)
        } else {
            RemoteText("False State".rs, color = Color.Red.rc)
        }
    }
}

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteStateLayoutIntSample() {
    val state = rememberMutableRemoteInt(0)
    RemoteStateLayout(currentState = state, 0, 1, 2) { index ->
        when (index) {
            0 -> RemoteText("State 0".rs, color = Color.Red.rc)
            1 -> RemoteText("State 1".rs, color = Color.Green.rc)
            2 -> RemoteText("State 2".rs, color = Color.Blue.rc)
        }
    }
}

enum class SampleState {
    Loading,
    Success,
    Error,
}

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun RemoteStateLayoutEnumSample() {
    val state = rememberMutableRemoteEnum(SampleState.Loading)
    RemoteStateLayout(currentState = state) { screen ->
        when (screen) {
            SampleState.Loading -> RemoteText("Loading...".rs, color = Color.Gray.rc)
            SampleState.Success -> RemoteText("Success!".rs, color = Color.Green.rc)
            SampleState.Error -> RemoteText("Error occurred".rs, color = Color.Red.rc)
        }
    }
}
