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
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.alpha
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.animateRemoteDpAsState
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteSpring
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
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
fun AnimateRemoteFloatSample() {
    val isDimmed = remember { MutableRemoteBoolean(false) }
    val animatedAlpha =
        animateRemoteFloatAsState(
            targetValue = isDimmed.select(0.35f.rf, 1f.rf),
            animationSpec = remoteTween(durationMillis = 400, easing = RemoteEasing.Standard),
        )

    RemoteBox(
        modifier =
            RemoteModifier.size(140.rdp, 64.rdp)
                .alpha(animatedAlpha)
                .clip(RemoteRoundedCornerShape(16.rdp))
                .background(Color(0xFF1E88E5).rc)
                .clickable(valueChangeAction(isDimmed, !isDimmed)),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteText(text = "Tap to Fade".rs, color = Color.White.rc)
    }
}

@Sampled
@Preview
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun AnimateRemoteDpSample() {
    val isExpanded = remember { MutableRemoteBoolean(false) }
    val animatedSize =
        animateRemoteDpAsState(
            targetValue = isExpanded.select(160.rdp, 96.rdp),
            animationSpec = remoteSpring(dampingRatio = 0.7f, stiffness = 80f),
        )

    RemoteBox(
        modifier =
            RemoteModifier.size(animatedSize)
                .clip(RemoteRoundedCornerShape(20.rdp))
                .background(Color(0xFF2E7D32).rc)
                .clickable(valueChangeAction(isExpanded, !isExpanded)),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteText(text = "Tap to Resize".rs, color = Color.White.rc)
    }
}
