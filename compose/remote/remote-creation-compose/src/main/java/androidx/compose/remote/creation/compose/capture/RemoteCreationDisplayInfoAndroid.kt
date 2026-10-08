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

@file:JvmName("RemoteCreationDisplayInfoKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources

/**
 * Creates a [RemoteCreationDisplayInfo] instance from display metrics.
 *
 * This function is used to capture the essential display properties required for remote rendering.
 * By default, it uses the system's current display metrics.
 *
 * @param width The width of the display in pixels. Defaults to the system display width.
 * @param height The height of the display in pixels. Defaults to the system display height.
 * @param densityDpi The logical densityDpi of the display. Defaults to the system display density.
 * @param fontScale The user preference for the scaling factor for fonts, relative to the base
 *   density scaling.
 * @param isInspectionMode Whether the capture is happening in inspection mode (e.g. for a preview).
 * @param densityBehavior The [RemoteDensityBehavior] to use. Defaults to
 *   [RemoteDensityBehavior.Legacy].
 * @return A [RemoteCreationDisplayInfo] object containing the specified display metrics.
 */
@Composable
public fun createCreationDisplayInfo(
    width: Int = LocalResources.current.displayMetrics.widthPixels,
    height: Int = LocalResources.current.displayMetrics.heightPixels,
    densityDpi: Int = LocalConfiguration.current.densityDpi,
    fontScale: Float = LocalConfiguration.current.fontScale,
    isInspectionMode: Boolean = LocalInspectionMode.current,
    densityBehavior: RemoteDensityBehavior = RemoteDensityBehavior.Legacy,
): RemoteCreationDisplayInfo {
    return RemoteCreationDisplayInfo(
        width = width,
        height = height,
        densityDpi = densityDpi,
        densityBehavior = densityBehavior,
        fontScale = fontScale,
        isInspectionMode = isInspectionMode,
    )
}

/**
 * Creates a [RemoteCreationDisplayInfo] instance from the provided [Context].
 *
 * This function extracts the display metrics (width, height, and density) from the [Context]'s
 * resources.
 *
 * @param context The [Context] used to access display metrics.
 * @param size The size of the display.
 * @param isInspectionMode Whether the capture is happening in inspection mode (e.g. for a preview).
 *   Defaults to false.
 * @param densityBehavior The [RemoteDensityBehavior] to use. Defaults to
 *   [RemoteDensityBehavior.Legacy].
 * @return A [RemoteCreationDisplayInfo] object containing the display metrics from the context.
 */
public fun createCreationDisplayInfo(
    context: Context,
    size: Size =
        Size(
            width = context.resources.displayMetrics.widthPixels.toFloat(),
            height = context.resources.displayMetrics.heightPixels.toFloat(),
        ),
    isInspectionMode: Boolean = false,
    densityBehavior: RemoteDensityBehavior = RemoteDensityBehavior.Legacy,
): RemoteCreationDisplayInfo {
    val resources = context.resources
    return RemoteCreationDisplayInfo(
        width = size.width.toInt(),
        height = size.height.toInt(),
        densityDpi = resources.displayMetrics.densityDpi,
        fontScale = resources.configuration.fontScale,
        isInspectionMode = isInspectionMode,
        densityBehavior = densityBehavior,
    )
}
