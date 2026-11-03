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

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.Sampled
import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.rememberPendingIntentAction
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
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
fun ClickableValueChangeSample() {
    val isToggled = remember { MutableRemoteBoolean(false) }
    val backgroundColor = isToggled.select(Color(0xFF1E88E5).rc, Color.DarkGray.rc)
    val label = isToggled.select("Active (tap to toggle)".rs, "Inactive (tap to toggle)".rs)

    RemoteBox(
        modifier =
            RemoteModifier.clip(RemoteRoundedCornerShape(12.rdp))
                .background(backgroundColor)
                .clickable(valueChangeAction(isToggled, !isToggled))
                .padding(16.rdp),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteText(text = label, color = Color.White.rc)
    }
}

@Sampled
@Preview
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun HostActionSample() {
    RemoteBox(
        modifier =
            RemoteModifier.clip(RemoteRoundedCornerShape(12.rdp))
                .background(Color(0xFF1E88E5).rc)
                .clickable(hostAction(name = "open_details".rs))
                .padding(16.rdp),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteText(text = "Open Details".rs, color = Color.White.rc)
    }
}

@Sampled
@Preview
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun CombinedActionSample() {
    val isBookmarked = remember { MutableRemoteBoolean(false) }
    val nextState = !isBookmarked

    RemoteBox(
        modifier =
            RemoteModifier.clip(RemoteRoundedCornerShape(12.rdp))
                .background(isBookmarked.select(Color(0xFF2E7D32).rc, Color.DarkGray.rc))
                .clickable(
                    combinedAction(
                        valueChangeAction(isBookmarked, nextState),
                        hostAction(name = "bookmark_changed".rs, value = nextState),
                    )
                )
                .padding(16.rdp),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteText(
            text = isBookmarked.select("Bookmarked".rs, "Tap to Bookmark".rs),
            color = Color.White.rc,
        )
    }
}

@Sampled
@Preview
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun PendingIntentActionSample() {
    val launchAction = rememberPendingIntentAction { context ->
        val intent = Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
        PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    RemoteBox(
        modifier =
            RemoteModifier.clip(RemoteRoundedCornerShape(12.rdp))
                .background(Color(0xFF1E88E5).rc)
                .clickable(launchAction)
                .padding(16.rdp),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteText(text = "Open App".rs, color = Color.White.rc)
    }
}
