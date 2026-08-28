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

package androidx.wear.compose.remote.material3.samples

import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.wear.compose.remote.material3.RemoteEdgeButton
import androidx.wear.compose.remote.material3.RemoteEdgeButtonDefaults
import androidx.wear.compose.remote.material3.RemoteEdgeButtonSize
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.previews.utils.RemoteComponentPreviewWrapper
import androidx.wear.compose.remote.material3.previews.utils.TestImageVectors
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteEdgeButtonSample(modifier: RemoteModifier = RemoteModifier) {
    val tapCount = rememberMutableRemoteInt(0)
    val countSuffix = " (".rs + tapCount.toRemoteString() + " taps)"

    RemoteEdgeButton(
        onClick = valueChange(tapCount, tapCount + 1),
        modifier = modifier,
        buttonSize = RemoteEdgeButtonSize.Small,
    ) {
        RemoteText("Tap me!".rs + countSuffix)
    }
}

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteEdgeButtonIconSample(modifier: RemoteModifier = RemoteModifier) {
    val tapCount = rememberMutableRemoteInt(0)

    RemoteEdgeButton(
        onClick = valueChange(tapCount, tapCount + 1),
        modifier = modifier,
        buttonSize = RemoteEdgeButtonSize.ExtraSmall,
    ) {
        RemoteIcon(
            imageVector = TestImageVectors.VolumeUp,
            contentDescription = "Volume Up".rs,
            modifier =
                RemoteModifier.size(
                    RemoteEdgeButtonDefaults.iconSizeFor(RemoteEdgeButtonSize.ExtraSmall)
                ),
        )
    }
}

@Sampled
@Composable
@WearPreviewDevices
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
fun RemoteEdgeButtonFilledTonalSample(modifier: RemoteModifier = RemoteModifier) {
    val tapCount = rememberMutableRemoteInt(0)

    RemoteEdgeButton(
        onClick = valueChange(tapCount, tapCount + 1),
        modifier = modifier,
        buttonSize = RemoteEdgeButtonSize.Medium,
        colors = RemoteEdgeButtonDefaults.filledTonalButtonColors(),
    ) {
        RemoteText("Filled Tonal".rs)
    }
}
