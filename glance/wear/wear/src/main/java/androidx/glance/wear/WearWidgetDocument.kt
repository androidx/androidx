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
import androidx.compose.remote.creation.compose.capture.createRemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemotePaint
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
 * The provided composable [background] and [content] will be captured into a Remote Compose
 * document for display within a widget.
 *
 * The system draws [background] directly inside the widget container behind [content], clipped to
 * the widget's shape ([WearWidgetParams.cornerRadiusDp]) without applying content padding; callers
 * should typically apply [RemoteModifier.fillMaxSize] inside [background] so it fills the
 * container. It is strongly recommended to explicitly define a non-transparent background; if
 * [background] is `null`, a default surface color will be applied. The [content] is rendered in a
 * padded area on top of the background (see [WearWidgetParams.horizontalPaddingDp] and
 * [WearWidgetParams.verticalPaddingDp]).
 *
 * @param background The optional `@RemoteComposable` background of the widget, or `null` to apply
 *   the default surface color.
 * @param content The `@RemoteComposable` content of the widget.
 */
public class WearWidgetDocument(
    private val background: (@RemoteComposable @Composable () -> Unit)? = null,
    private val content: @RemoteComposable @Composable () -> Unit,
) : WearWidgetData {

    /**
     * Defines the content of a widget using Remote Compose with a [WearWidgetBrush] background.
     *
     * See [WearWidgetDocument] for details on how [background] and [content] are clipped, padded,
     * and drawn, or how the default surface color is applied when [background] is empty.
     *
     * @param background The [WearWidgetBrush] for the widget's background.
     * @param content The `@RemoteComposable` content of the widget.
     */
    // TODO(b/587330676): Remove this constructor once WearWidgetBrush is removed.
    public constructor(
        background: WearWidgetBrush,
        content: @RemoteComposable @Composable () -> Unit,
    ) : this(
        background =
            if (background.isEmpty()) {
                null
            } else {
                {
                    RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                        val paint = RemotePaint()
                        background.foldIn(Unit) { _, element ->
                            with(element.brush) { applyTo(paint, size) }
                            drawRect(paint = paint)
                        }
                    }
                }
            },
        content = content,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override suspend fun captureRawContent(
        context: Context,
        params: WearWidgetParams,
        isInspectionMode: Boolean,
    ): WearWidgetRawContent {
        return WearWidgetCapture.capture(
            context,
            createRemoteCreationDisplayInfo(
                context = context,
                size =
                    Size(
                        width = params.widthDp.dpToPx(context).toFloat(),
                        height = params.heightDp.dpToPx(context).toFloat(),
                    ),
                isInspectionMode = isInspectionMode,
            ),
            params.rendererVersion.supportedOperations,
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
