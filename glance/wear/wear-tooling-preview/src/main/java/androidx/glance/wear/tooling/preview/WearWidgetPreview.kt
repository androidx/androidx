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

package androidx.glance.wear.tooling.preview

import android.content.Context
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.WearWidgetBrush
import androidx.glance.wear.WearWidgetData
import androidx.glance.wear.WearWidgetDocument
import androidx.glance.wear.color
import androidx.glance.wear.core.WearWidgetParams
import kotlinx.coroutines.runBlocking

/**
 * Renders a preview of a [GlanceWearWidget] in Android Studio.
 *
 * This utility facilitates rapid development by allowing developers to visualize how their Glance
 * Wear widgets will appear on various device configurations without deploying to a physical device
 * or emulator. It uses the provided [WearWidgetParams] to represent the widget's layout, including
 * dimensions, padding, and container shape.
 *
 * @param widget The [GlanceWearWidget] instance to be previewed.
 * @param params The [WearWidgetParams] to configure parameters (size, shape, padding) for the
 *   widget container.
 * @param modifier The [Modifier] to be applied to the container box hosting the widget preview.
 *   Note that the preview's dimensions are enforced internally based on the provided [params].
 *   Applying layout-modifying modifiers here might conflict with these internal specifications.
 */
@Composable
public fun WearWidgetPreview(
    widget: GlanceWearWidget,
    params: WearWidgetParams,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val document =
        remember(widget, params, context) {
            runBlocking {
                val widgetData = widget.provideWidgetData(context, params)
                widgetData.captureRawContent(context, params, isInspectionMode = true).rcDocument
            }
        }

    RemoteDocumentPreview(
        document,
        modifier =
            modifier
                .width((params.widthDp + 2f * params.horizontalPaddingDp).dp)
                .height((params.heightDp + 2f * params.verticalPaddingDp).dp),
    )
}

/**
 * Renders a preview of a Wear Widget with the provided Remote Compose [content].
 *
 * This utility facilitates rapid development by allowing developers to visualize how their Glance
 * Wear widgets will appear on various device configurations without deploying to a physical device
 * or emulator. It uses the provided [WearWidgetParams] to represent the widget's layout, including
 * dimensions, padding, and container shape.
 *
 * This overload allows to directly preview a Wear Widget by supplying the Remote Compose [content]
 * and optional [background]. It is convenient for complex widgets that switch between multiple
 * layouts based on application state, avoiding the mocking the entire widget or its dependencies.
 *
 * @param params The [WearWidgetParams] to configure parameters (size, shape, padding) for the
 *   widget container.
 * @param modifier The [Modifier] to be applied to the container box hosting the widget preview.
 * @param background The [WearWidgetBrush] to be used as the background of the widget. Defaults to a
 *   transparent solid color.
 * @param content The [Composable] content of the widget to be previewed.
 */
@Composable
public fun WearWidgetPreview(
    params: WearWidgetParams,
    modifier: Modifier = Modifier,
    background: WearWidgetBrush = WearWidgetBrush.color(Color.Transparent.rc),
    content: @RemoteComposable @Composable () -> Unit,
) {
    val widget =
        remember(background, content) {
            object : GlanceWearWidget() {
                override suspend fun provideWidgetData(
                    context: Context,
                    params: WearWidgetParams,
                ): WearWidgetData = WearWidgetDocument(background, content)
            }
        }
    WearWidgetPreview(widget, params, modifier)
}
