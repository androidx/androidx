/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.core

import android.graphics.Rect
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.BundleCompat

/** A class representing the UI state of a SandboxedSdkView. */
public class SandboxedSdkViewUiInfo(
    /** Returns the width of the UI container in pixels. */
    public val uiContainerWidth: Int,
    /** Returns the height of the UI container in pixels. */
    public val uiContainerHeight: Int,
    /**
     * Returns the portion of the UI container which is not clipped by parent views and is visible
     * on screen. The coordinates of this [Rect] are relative to the UI container and measured in
     * pixels.
     *
     * If none of the UI container is visible on screen, each coordinate in this [Rect] will be -1.
     */
    public val onScreenGeometry: Rect,
    /**
     * Returns the opacity of the UI container, where available.
     *
     * When available, this is a value from 0 to 1, where 0 means the container is completely
     * transparent and 1 means the container is completely opaque. This value doesn't necessarily
     * reflect the user-visible opacity of the UI container, as shaders and other overlays can
     * affect that.
     *
     * When the opacity is not available, the value will be -1.
     */
    public val uiContainerOpacityHint: Float,
    /**
     * Returns the list of coordinate rectangles, relative to the UI container, that are obstructed.
     *
     * The container may be considered to be obstructed by another UI element if that UI element
     * overlaps with the container, is displayed on top of it, and is not transparent. Ancestor
     * views and children views will not be considered as obstructing the container. If the
     * container's window is placed above the client window, no obstructions will be reported.
     *
     * The coordinates of each obstruction in this list are relative to the UI container, and
     * measured in pixels. The rectangles may overlap each other.
     *
     * This value will only be non-empty if [SandboxedUiAdapterSignalOptions.OBSTRUCTIONS] is set on
     * the [SessionObserverFactory] associated with the UI container's [SandboxedUiAdapter].
     * Otherwise, this will return an empty list irrespective of any obstructions on the container.
     */
    public val obstructedGeometry: List<Rect>,
) {
    public companion object {
        private const val UI_CONTAINER_WIDTH_KEY = "uiContainerWidth"
        private const val UI_CONTAINER_HEIGHT_KEY = "uiContainerHeight"
        private const val ONSCREEN_GEOMETRY_KEY = "onScreenGeometry"
        private const val UI_CONTAINER_OPACITY_KEY = "uiContainerOpacity"
        private const val OBSTRUCTED_GEOMETRY_KEY = "obstructedGeometry"

        @JvmStatic
        public fun fromBundle(bundle: Bundle): SandboxedSdkViewUiInfo {
            val uiContainerWidth = bundle.getInt(UI_CONTAINER_WIDTH_KEY)
            val uiContainerHeight = bundle.getInt(UI_CONTAINER_HEIGHT_KEY)
            val onScreenGeometry =
                checkNotNull(
                    BundleCompat.getParcelable(bundle, ONSCREEN_GEOMETRY_KEY, Rect::class.java)
                )
            val uiContainerOpacityHint = bundle.getFloat(UI_CONTAINER_OPACITY_KEY)
            val obstructedGeometry =
                BundleCompat.getParcelableArrayList<Rect>(
                        bundle,
                        OBSTRUCTED_GEOMETRY_KEY,
                        Rect::class.java,
                    )
                    ?.toList() ?: listOf()
            return SandboxedSdkViewUiInfo(
                uiContainerWidth,
                uiContainerHeight,
                onScreenGeometry,
                uiContainerOpacityHint,
                obstructedGeometry,
            )
        }

        @JvmStatic
        public fun toBundle(sandboxedSdkViewUiInfo: SandboxedSdkViewUiInfo): Bundle {
            val bundle = Bundle()
            bundle.putInt(UI_CONTAINER_WIDTH_KEY, sandboxedSdkViewUiInfo.uiContainerWidth)
            bundle.putInt(UI_CONTAINER_HEIGHT_KEY, sandboxedSdkViewUiInfo.uiContainerHeight)
            bundle.putParcelable(ONSCREEN_GEOMETRY_KEY, sandboxedSdkViewUiInfo.onScreenGeometry)
            bundle.putFloat(UI_CONTAINER_OPACITY_KEY, sandboxedSdkViewUiInfo.uiContainerOpacityHint)
            bundle.putParcelableArrayList(
                OBSTRUCTED_GEOMETRY_KEY,
                ArrayList<Rect>(sandboxedSdkViewUiInfo.obstructedGeometry),
            )
            return bundle
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun pruneBundle(bundle: Bundle, signalOptions: Set<String>) {
            if (!signalOptions.contains(SandboxedUiAdapterSignalOptions.GEOMETRY)) {
                bundle.remove(UI_CONTAINER_HEIGHT_KEY)
                bundle.remove(UI_CONTAINER_WIDTH_KEY)
                bundle.remove(UI_CONTAINER_OPACITY_KEY)
                bundle.remove(ONSCREEN_GEOMETRY_KEY)
            }
            if (!signalOptions.contains(SandboxedUiAdapterSignalOptions.OBSTRUCTIONS)) {
                bundle.remove(OBSTRUCTED_GEOMETRY_KEY)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SandboxedSdkViewUiInfo) return false

        return onScreenGeometry == other.onScreenGeometry &&
            uiContainerWidth == other.uiContainerWidth &&
            uiContainerHeight == other.uiContainerHeight &&
            uiContainerOpacityHint == other.uiContainerOpacityHint
    }

    override fun hashCode(): Int {
        var result = uiContainerWidth
        result = 31 * result + uiContainerHeight
        result = 31 * result + onScreenGeometry.hashCode()
        result = 31 * result + obstructedGeometry.hashCode()
        return result
    }

    override fun toString(): String {
        return "SandboxedSdkViewUiInfo(" +
            "uiContainerWidth=$uiContainerWidth, " +
            "uiContainerHeight=$uiContainerHeight, " +
            "onScreenGeometry=$onScreenGeometry," +
            "uiContainerOpacityHint=$uiContainerOpacityHint," +
            "obstructedGeometry=$obstructedGeometry"
    }
}
