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

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
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
 *   the [content], applying host-defined clipping and padding. It is strongly recommended to
 *   explicitly define a non-transparent background. If the given [background] is empty, a default
 *   surface color will be applied.
 * @param content The RemoteComposable content of the widget. This content is rendered in a padded
 *   area on top of the background. See [WearWidgetParams.horizontalPaddingDp] and
 *   [WearWidgetParams.verticalPaddingDp].
 */
public class WearWidgetDocument(
    private val background: WearWidgetBrush,
    private val content: @RemoteComposable @Composable () -> Unit,
) : WearWidgetData {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @SuppressLint("RestrictedApiAndroidX")
    override suspend fun captureRawContent(
        context: Context,
        params: WearWidgetParams,
        isInspectionMode: Boolean,
    ): WearWidgetRawContent {
        return WearWidgetCapture.capture(
            context,
            createCreationDisplayInfo(
                context = context,
                size =
                    Size(
                        width = params.widthDp.dpToPx(context).toFloat(),
                        height = params.heightDp.dpToPx(context).toFloat(),
                    ),
                isInspectionMode = isInspectionMode,
            ),
        ) {
            WearWidgetContainer(
                horizontalPadding = params.horizontalPaddingDp.rdp,
                verticalPadding = params.verticalPaddingDp.rdp,
                cornerRadius = params.cornerRadiusDp.rdp,
                background = background,
                content = content,
            )
        }
    }
}

private fun Float.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()
