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

package androidx.wear.compose.remote.material3.samples

import androidx.annotation.Sampled
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.creation.compose.action.ValueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.tooling.preview.RemotePreview
import androidx.compose.runtime.Composable
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.remote.material3.RemoteTextButton
import androidx.wear.compose.remote.material3.RemoteTextButtonDefaults
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Sampled
@Composable
fun RemoteTextButtonSimpleSample(modifier: RemoteModifier = RemoteModifier) {
    val tapCount = rememberMutableRemoteInt(0)
    val text = "+".rs + tapCount.toRemoteString(3, TextFromFloat.PAD_PRE_NONE)

    RemoteTextButton(
        ValueChange(tapCount, tapCount + 1),
        modifier = modifier,
        colors = filledTonalColor(),
    ) {
        RemoteText(text = text)
    }
}

@WearPreviewDevices
@Composable
fun RemoteTextButtonSimpleSamplePreview() = RemotePreview {
    Container { RemoteTextButtonSimpleSample() }
}

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(
        modifier,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
        content = content,
    )
}

@Composable
private fun filledTonalColor() =
    RemoteTextButtonDefaults.textButtonColors()
        .copy(
            containerColor = RemoteMaterialTheme.colorScheme.primary,
            contentColor = RemoteMaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = RemoteMaterialTheme.colorScheme.primary.copy(alpha = 0.12f.rf),
            disabledContentColor = RemoteMaterialTheme.colorScheme.primary.copy(0.38f.rf),
        )
