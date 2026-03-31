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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

private const val baseDensity = 160f

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteCreationDisplayInfo(public val size: Size, public val density: Density)

/**
 * Creates a [RemoteCreationDisplayInfo] instance from width, height, and density metrics.
 *
 * @param width The width of the display in pixels.
 * @param height The height of the display in pixels.
 * @param densityDpi The logical densityDpi of the display.
 * @return A [RemoteCreationDisplayInfo] object containing the specified display metrics.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCreationDisplayInfo(
    width: Int,
    height: Int,
    densityDpi: Int,
): RemoteCreationDisplayInfo {
    return RemoteCreationDisplayInfo(
        Size(width.toFloat(), height.toFloat()),
        Density(densityDpi / baseDensity),
    )
}

/**
 * Creates a [RemoteCreationDisplayInfo] instance from display metrics.
 *
 * This function is used to capture the essential display properties required for remote rendering.
 * By default, it uses the system's current display metrics.
 *
 * @param width The width of the display in pixels. Defaults to the system display width.
 * @param height The height of the display in pixels. Defaults to the system display height.
 * @param densityDpi The logical densityDpi of the display. Defaults to the system display density.
 * @return A [RemoteCreationDisplayInfo] object containing the specified display metrics.
 */
@Composable
public fun createCreationDisplayInfo(
    width: Int = LocalResources.current.displayMetrics.widthPixels,
    height: Int = LocalResources.current.displayMetrics.heightPixels,
    densityDpi: Int = LocalConfiguration.current.densityDpi,
): RemoteCreationDisplayInfo {
    return RemoteCreationDisplayInfo(width, height, densityDpi)
}

/**
 * Creates a [RemoteCreationDisplayInfo] instance from the provided [Context].
 *
 * This function extracts the display metrics (width, height, and density) from the [Context]'s
 * resources.
 *
 * @param context The [Context] used to access display metrics.
 * @return A [RemoteCreationDisplayInfo] object containing the display metrics from the context.
 */
public fun createCreationDisplayInfo(context: Context): RemoteCreationDisplayInfo {
    val resources = context.resources
    return RemoteCreationDisplayInfo(
        width = resources.displayMetrics.widthPixels,
        height = resources.displayMetrics.heightPixels,
        densityDpi = context.resources.displayMetrics.densityDpi,
    )
}

/** The width of the display in [Dp] units. */
public val RemoteCreationDisplayInfo.widthDp: Dp
    get() = with(density) { size.width.toDp() }

/** The height of the display in [Dp] units. */
public val RemoteCreationDisplayInfo.heightDp: Dp
    get() = with(density) { size.height.toDp() }

internal fun RemoteCreationDisplayInfo.toCreationDisplayInfo() =
    CreationDisplayInfo(
        /* width = */ this.size.width.toInt(),
        /* height = */ this.size.height.toInt(),
        /* mDensityDpi = */ (this.density.density * baseDensity).toInt(),
    )
