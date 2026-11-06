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

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Immutable

private const val baseDensity = 160f

/** Density behavior for the RemoteCompose document. */
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
 * @property width The width of the virtual display in pixels.
 * @property height The height of the virtual display in pixels.
 * @property density The logical density of the virtual display.
 * @property fontScale The user preference for the scaling factor for fonts, relative to the base
 *   density scaling.
 * @property isInspectionMode Whether the capture is happening in an inspection or preview
 *   environment (e.g. inside an IDE preview). Defaults to false.
 * @property densityBehavior The [RemoteDensityBehavior] to use for interpreting dimension values.
 */
@Immutable
public class RemoteCreationDisplayInfo
internal constructor(
    public val width: Int,
    public val height: Int,
    public val density: Float,
    public val fontScale: Float = 1.0f,
    public val isInspectionMode: Boolean = false,
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
 * @param densityBehavior The [RemoteDensityBehavior] to use. Defaults to
 *   [RemoteDensityBehavior.Legacy].
 * @return A [RemoteCreationDisplayInfo] object containing the specified display metrics.
 */
public fun RemoteCreationDisplayInfo(
    @IntRange(from = 1) width: Int,
    @IntRange(from = 1) height: Int,
    @IntRange(from = 1) densityDpi: Int,
    @FloatRange(from = 0.0, fromInclusive = false) fontScale: Float = 1.0f,
    isInspectionMode: Boolean = false,
    densityBehavior: RemoteDensityBehavior = RemoteDensityBehavior.Legacy,
): RemoteCreationDisplayInfo {
    return RemoteCreationDisplayInfo(
        width = width,
        height = height,
        density = densityDpi / baseDensity,
        fontScale = fontScale,
        isInspectionMode = isInspectionMode,
        densityBehavior = densityBehavior,
    )
}

internal fun RemoteCreationDisplayInfo.toCreationDisplayInfo() =
    CreationDisplayInfo(
        /* width = */ this.width,
        /* height = */ this.height,
        /* mDensityDpi = */ (this.density * baseDensity).toInt(),
        /* densityBehavior = */ this.densityBehavior.value,
    )

internal fun RemoteDensity.Companion.from(
    creationDisplayInfo: RemoteCreationDisplayInfo
): RemoteDensity = RemoteDensity(creationDisplayInfo.density.rf, creationDisplayInfo.fontScale.rf)
