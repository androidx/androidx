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

package androidx.glance.wear.samples

import android.content.Context
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.GlanceWearWidgetService
import androidx.glance.wear.WearWidgetBrush
import androidx.glance.wear.WearWidgetData
import androidx.glance.wear.WearWidgetDocument
import androidx.glance.wear.color
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.tooling.preview.SquircleLargeWidgetPreviewParams
import androidx.glance.wear.tooling.preview.SquircleSmallWidgetPreviewParams
import androidx.glance.wear.tooling.preview.WearWidgetPreview
import androidx.wear.compose.remote.material3.RemoteText

class HelloWidgetService : GlanceWearWidgetService() {
    override val widget: GlanceWearWidget = HelloWidget()
}

private class HelloWidget : GlanceWearWidget() {
    override suspend fun provideWidgetData(
        context: Context,
        params: WearWidgetParams,
    ): WearWidgetData {
        val backgroundColor = getBackgroundColor(params.containerType)
        return WearWidgetDocument(background = WearWidgetBrush.color(backgroundColor.rc)) {
            HelloWidgetContent(params.containerType)
        }
    }
}

@RemoteComposable
@Composable
private fun HelloWidgetContent(@ContainerInfo.ContainerType containerType: Int) {
    RemoteBox(modifier = RemoteModifier.fillMaxSize(), contentAlignment = RemoteAlignment.Center) {
        RemoteText(
            when (containerType) {
                ContainerInfo.CONTAINER_TYPE_SMALL -> stringResource(R.string.hello_small)
                ContainerInfo.CONTAINER_TYPE_LARGE -> stringResource(R.string.hello_large)
                else -> stringResource(R.string.hello)
            }.rs
        )
    }
}

private fun getBackgroundColor(@ContainerInfo.ContainerType containerType: Int): Color =
    when (containerType) {
        ContainerInfo.CONTAINER_TYPE_SMALL -> Color.Red
        ContainerInfo.CONTAINER_TYPE_LARGE -> Color.Blue
        else -> Color.Gray
    }

/**
 * Demonstrates previewing the full widget by passing an instance of [HelloWidget].
 *
 * This approach is useful when you want to verify the entire [GlanceWearWidget] logic, including
 * how it selects backgrounds or handles state in [GlanceWearWidget.provideWidgetData].
 */
@Preview
@Composable
fun HelloLargeWidgetPreview(
    @PreviewParameter(SquircleLargeWidgetPreviewParams::class) params: WearWidgetParams
) = WearWidgetPreview(HelloWidget(), params = params)

/**
 * Demonstrates the simplified preview overload using a trailing lambda for [HelloWidgetContent].
 *
 * This is ideal for rapid UI prototyping or for previewing internal sub-composables (like
 * [HelloWidgetContent]) in isolation. It avoids the need to instantiate the full widget class or
 * its dependencies, which is particularly convenient for complex widgets with multiple layouts.
 */
@Preview
@Composable
fun HelloSmallWidgetPreview(
    @PreviewParameter(SquircleSmallWidgetPreviewParams::class) params: WearWidgetParams
) =
    WearWidgetPreview(
        params = params,
        background = WearWidgetBrush.color(getBackgroundColor(params.containerType).rc),
    ) {
        HelloWidgetContent(params.containerType)
    }
