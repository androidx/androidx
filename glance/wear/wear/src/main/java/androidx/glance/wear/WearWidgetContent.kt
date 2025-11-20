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

package androidx.glance.wear

import android.content.Context
import androidx.compose.remote.creation.compose.capture.CreationDisplayInfo
import androidx.compose.runtime.Composable
import androidx.glance.wear.parcel.WearWidgetCapture

/**
 * Describes the contents of a Widget.
 *
 * @property content The RemoteComposable corresponding to contents of the widget.
 */
// TODO: Add @RemoteComposable annotation once it's public.
public class WearWidgetContent(public val content: @Composable () -> Unit) {

    /** Captures the RemoteCompose content. */
    // TODO: specify size bounds for capture.
    @Suppress("RestrictedApiAndroidX")
    internal suspend fun captureRawContent(
        context: Context,
        widthDp: Float,
        heightDp: Float,
    ): WearWidgetRawContent {
        return WearWidgetCapture.capture(
            context,
            CreationDisplayInfo(widthDp.dpToPx(context), heightDp.dpToPx(context)),
            content,
        )
    }
}

private fun Float.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()
