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

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.wear.GlanceWearWidget
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
                widgetData.captureRawContent(context, params).rcDocument
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
