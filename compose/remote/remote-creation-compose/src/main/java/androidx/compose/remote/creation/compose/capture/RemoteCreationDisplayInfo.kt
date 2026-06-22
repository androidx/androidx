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

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

private const val baseDensity = 160f

/** Density behavior for the RemoteCompose document. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RemoteDensityBehavior(internal val value: Int) {
    /**
     * Legacy mode. Values are interpreted as pixels, but historically some layout properties might
     * have behaved differently.
     */
    Legacy(CoreDocument.DENSITY_BEHAVIOR_LEGACY),

    /** Values are interpreted as pixels. No density scaling is applied by the player. */
    Pixels(CoreDocument.DENSITY_BEHAVIOR_PIXELS),

    /** Values are interpreted as DP. Density scaling is applied by the player. */
    Dp(CoreDocument.DENSITY_BEHAVIOR_DP),
}

/**
 * Represents the virtual display metrics and configuration used as guide values for rendering a
 * RemoteCompose document.
 *
 * These values serve as a guide for the predicted characteristics of the remote display, but the
 * actual characteristics may differ when the document is played back on a remote client.
 *
 * @property size The physical dimensions of the virtual display in pixels.
 * @property density The Compose [Density] of the virtual display.
 * @property isInspectionMode Whether the capture is happening in an inspection or preview
 *   environment (e.g. inside an IDE preview). Defaults to false.
 */
public class RemoteCreationDisplayInfo
internal constructor(
    public val size: Size,
    public val density: Density,
    public val isInspectionMode: Boolean = false,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val densityBehavior: RemoteDensityBehavior = RemoteDensityBehavior.Legacy,
)

/**
 * Creates a [RemoteCreationDisplayInfo] instance from width, height, and density metrics.
 *
 * @param width The width of the display in pixels.
 * @param height The height of the display in pixels.
 * @param densityDpi The logical densityDpi of the display.
 * @param fontScale The user preference for the scaling factor for fonts, relative to the base
 *   density scaling.
 * @param isInspectionMode Whether the capture is happening in inspection mode (e.g. for a preview).
 *   Defaults to false.
 * @return A [RemoteCreationDisplayInfo] object containing the specified display metrics.
 */
public fun RemoteCreationDisplayInfo(
    width: Int,
    height: Int,
    densityDpi: Int,
    fontScale: Float = 1.0f,
    isInspectionMode: Boolean = false,
): RemoteCreationDisplayInfo {
    return RemoteCreationDisplayInfo(
        width,
        height,
        densityDpi,
        fontScale,
        isInspectionMode,
        RemoteDensityBehavior.Legacy,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCreationDisplayInfo(
    width: Int,
    height: Int,
    densityDpi: Int,
    fontScale: Float = 1.0f,
    isInspectionMode: Boolean = false,
    densityBehavior: RemoteDensityBehavior = RemoteDensityBehavior.Legacy,
): RemoteCreationDisplayInfo {
    return RemoteCreationDisplayInfo(
        Size(width.toFloat(), height.toFloat()),
        Density(densityDpi / baseDensity, fontScale),
        isInspectionMode,
        densityBehavior,
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
 * @param fontScale The user preference for the scaling factor for fonts, relative to the base
 *   density scaling.
 * @param isInspectionMode Whether the capture is happening in inspection mode (e.g. for a preview).
 * @return A [RemoteCreationDisplayInfo] object containing the specified display metrics.
 */
@Composable
public fun createCreationDisplayInfo(
    width: Int = LocalResources.current.displayMetrics.widthPixels,
    height: Int = LocalResources.current.displayMetrics.heightPixels,
    densityDpi: Int = LocalConfiguration.current.densityDpi,
    fontScale: Float = LocalConfiguration.current.fontScale,
    isInspectionMode: Boolean = LocalInspectionMode.current,
): RemoteCreationDisplayInfo {
    return createCreationDisplayInfo(
        width,
        height,
        densityDpi,
        fontScale,
        isInspectionMode,
        RemoteDensityBehavior.Legacy,
    )
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun createCreationDisplayInfo(
    width: Int = LocalResources.current.displayMetrics.widthPixels,
    height: Int = LocalResources.current.displayMetrics.heightPixels,
    densityDpi: Int = LocalConfiguration.current.densityDpi,
    fontScale: Float = LocalConfiguration.current.fontScale,
    isInspectionMode: Boolean = LocalInspectionMode.current,
    densityBehavior: RemoteDensityBehavior,
): RemoteCreationDisplayInfo {
    return RemoteCreationDisplayInfo(
        width,
        height,
        densityDpi,
        fontScale,
        isInspectionMode,
        densityBehavior,
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
): RemoteCreationDisplayInfo {
    return createCreationDisplayInfo(context, size, isInspectionMode, RemoteDensityBehavior.Legacy)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun createCreationDisplayInfo(
    context: Context,
    size: Size =
        Size(
            width = context.resources.displayMetrics.widthPixels.toFloat(),
            height = context.resources.displayMetrics.heightPixels.toFloat(),
        ),
    isInspectionMode: Boolean = false,
    densityBehavior: RemoteDensityBehavior,
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
        /* densityBehavior = */ this.densityBehavior.value,
    )
