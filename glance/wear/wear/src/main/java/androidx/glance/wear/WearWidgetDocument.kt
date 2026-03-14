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
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.wear.composable.WearWidgetContainer
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WearWidgetRawContent
import androidx.glance.wear.parcel.WearWidgetCapture

/**
 * Defines the content of a widget using Remote Compose.
 *
 * The provided composable content will be captured into a Remote Compose document for display
 * within a widget.
 *
 * @param background The [WearWidgetBrush] for the widget's background. The system draws this behind
 *   the [content], applying host-defined clipping and padding.
 * @param content The RemoteComposable content of the widget. This content is rendered in a padded
 *   area on top of the background. See [WearWidgetParams.horizontalPaddingDp] and
 *   [WearWidgetParams.verticalPaddingDp].
 */
public class WearWidgetDocument(
    private val background: WearWidgetBrush,
    private val content: @RemoteComposable @Composable () -> Unit,
) : WearWidgetData {

    // TODO: b/470080675 - Remove this after G3 drop.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        backgroundColor: Color,
        content: @RemoteComposable @Composable () -> Unit,
    ) : this(WearWidgetBrush.color(backgroundColor.rc), content)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override suspend fun captureRawContent(
        context: Context,
        params: WearWidgetParams,
    ): WearWidgetRawContent {
        return WearWidgetCapture.capture(
            context,
            CreationDisplayInfo(
                params.widthDp.dpToPx(context),
                params.heightDp.dpToPx(context),
                context.resources.displayMetrics.densityDpi,
            ),
        ) {
            WearWidgetContainer(
                horizontalPadding = params.horizontalPaddingDp.rdp,
                verticalPadding = params.verticalPaddingDp.rdp,
                cornerRadius = params.cornerRadiusDp.dp,
                background = background,
                content = content,
            )
        }
    }
}

private fun Float.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()
