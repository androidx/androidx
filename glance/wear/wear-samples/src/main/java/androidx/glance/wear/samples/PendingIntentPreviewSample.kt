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

package androidx.glance.wear.samples

import android.app.PendingIntent
import android.content.Intent
import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.glance.wear.WearWidgetBrush
import androidx.glance.wear.color
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.tooling.preview.SquircleAllWidgetPreviewParams
import androidx.glance.wear.tooling.preview.WearWidgetPreview
import androidx.wear.compose.remote.material3.RemoteText

/** A sample preview for a Wear Widget with a clickable pending intent action. */
@Preview
@Composable
fun PendingIntentPreview(
    @PreviewParameter(SquircleAllWidgetPreviewParams::class) params: WearWidgetParams
) =
    WearWidgetPreview(params = params, background = WearWidgetBrush.color(Color.White.rc)) {
        RemoteBox(
            modifier =
                RemoteModifier.fillMaxSize()
                    .clickable(
                        pendingIntentAction { context ->
                            PendingIntent.getActivity(
                                context,
                                0,
                                Intent(),
                                PendingIntent.FLAG_IMMUTABLE,
                            )
                        }
                    ),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteText("Click Me!".rs, color = Color.Black.rc)
        }
    }
