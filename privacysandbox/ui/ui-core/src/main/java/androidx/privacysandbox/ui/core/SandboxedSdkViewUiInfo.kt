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
import androidx.core.os.BundleCompat

/** A class representing the UI state of a SandboxedSdkView. */
class SandboxedSdkViewUiInfo(
    /** Returns the width of the UI container in pixels. */
    val uiContainerWidth: Int,
    /** Returns the height of the UI container in pixels. */
    val uiContainerHeight: Int,
    /**
     * Returns the portion of the UI container which is not clipped by parent views and is visible
     * on screen. The coordinates of this [Rect] are relative to the UI container and measured in
     * pixels.
     *
     * If none of the UI container is visible on screen, each coordinate in this [Rect] will be -1.
     */
    val onScreenGeometry: Rect
) {
    companion object {
        private const val UI_CONTAINER_WIDTH_KEY = "uiContainerWidth"
        private const val UI_CONTAINER_HEIGHT_KEY = "uiContainerHeight"
        private const val ONSCREEN_GEOMETRY_KEY = "onScreenGeometry"

        @JvmStatic
        fun fromBundle(bundle: Bundle): SandboxedSdkViewUiInfo {
            val uiContainerWidth = bundle.getInt(UI_CONTAINER_WIDTH_KEY)
            val uiContainerHeight = bundle.getInt(UI_CONTAINER_HEIGHT_KEY)
            val onScreenGeometry =
                checkNotNull(
                    BundleCompat.getParcelable(bundle, ONSCREEN_GEOMETRY_KEY, Rect::class.java)
                )
            return SandboxedSdkViewUiInfo(uiContainerWidth, uiContainerHeight, onScreenGeometry)
        }

        @JvmStatic
        fun toBundle(sandboxedSdkViewUiInfo: SandboxedSdkViewUiInfo): Bundle {
            val bundle = Bundle()
            bundle.putInt(UI_CONTAINER_WIDTH_KEY, sandboxedSdkViewUiInfo.uiContainerWidth)
            bundle.putInt(UI_CONTAINER_HEIGHT_KEY, sandboxedSdkViewUiInfo.uiContainerHeight)
            bundle.putParcelable(ONSCREEN_GEOMETRY_KEY, sandboxedSdkViewUiInfo.onScreenGeometry)
            return bundle
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SandboxedSdkViewUiInfo) return false

        return onScreenGeometry == other.onScreenGeometry &&
            uiContainerWidth == other.uiContainerWidth &&
            uiContainerHeight == other.uiContainerHeight
    }

    override fun hashCode(): Int {
        var result = uiContainerWidth
        result = 31 * result + uiContainerHeight
        result = 31 * result + onScreenGeometry.hashCode()
        return result
    }

    override fun toString(): String {
        return "SandboxedSdkViewUiInfo(" +
            "uiContainerWidth=$uiContainerWidth, " +
            "uiContainerHeight=$uiContainerHeight, " +
            "onScreenGeometry=$onScreenGeometry"
    }
}
