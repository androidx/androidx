/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core

import android.os.Build
import androidx.ui.geometry.RRect
import androidx.ui.geometry.Rect
import androidx.ui.geometry.isSimple
import androidx.ui.graphics.Outline
import androidx.ui.graphics.Path
import androidx.ui.graphics.Shape
import androidx.ui.unit.Density
import androidx.ui.unit.PxSize
import androidx.ui.unit.px
import kotlin.math.roundToInt

/**
 * Resolves the Android [android.graphics.Outline] from the [Shape] of an [OwnedLayer].
 */
internal class OutlineResolver(private val density: Density) {
    /**
     * The Android Outline that is used in the layer.
     */
    private val cachedOutline = android.graphics.Outline().apply { alpha = 1f }

    /**
     * The size of the layer. This is used in generating the [Outline] from the [Shape].
     */
    private var size: PxSize = PxSize.Zero

    /**
     * The [Shape] of the Outline of the Layer. `null` indicates that there is no outline.
     */
    private var shape: Shape? = null

    /**
     * Asymmetric rounded rectangles need to use a Path. This caches that Path so that
     * a new one doesn't have to be generated each time.
     */
    // TODO(andreykulikov): Make Outline API reuse the Path when generating.
    private var cachedRrectPath: Path? = null // for temporary allocation in rounded rects

    /**
     * The outline Path when a non-conforming (rect or symmetric rounded rect) Outline
     * is used. This Path is necessary when [usePathForClip] is true to indicate the
     * Path to clip in [clipPath].
     */
    private var outlinePath: Path? = null

    /**
     * The opacity of the outline, which is the same as the opacity of the layer.
     */
    private var alpha = 1f

    /**
     * True when there's been an update that caused a change in the path and the Outline
     * has to be reevaluated.
     */
    private var cacheIsDirty = false

    /**
     * True when Outline cannot clip the content and the path should be used instead.
     * This is when an asymmetric rounded rect or general Path is used in the outline.
     * This is false when a Rect or a symmetric RRect is used in the outline.
     */
    private var usePathForClip = false

    /**
     * Returns the Android Outline to be used in the layer.
     */
    val outline: android.graphics.Outline?
        get() {
            updateCache()
            return if (shape == null) null else cachedOutline
        }

    /**
     * When a the layer doesn't support clipping of the outline, this returns the Path
     * that should be used to manually clip. When the layer does support manual clipping
     * or there is no outline, this returns null.
     */
    val clipPath: Path?
        get() {
            updateCache()
            return if (usePathForClip) outlinePath else null
        }

    /**
     * `true` when an Outline can be used. This can be `true` even if the outline
     * doesn't support clipping because it may be used for shadows. An Outline
     * is not supported when the shape is `null` or a concave path is used on
     * pre-Q devices.
     */
    val supportsNativeOutline: Boolean
        get() {
            if (shape == null) {
                return false
            }
            updateCache()
            return !cachedOutline.isEmpty
        }

    /**
     * Updates the values of the outline.
     */
    fun update(shape: Shape?, alpha: Float) {
        if (this.shape != shape) {
            this.shape = shape
            cacheIsDirty = true
        }
        if (this.alpha != alpha) {
            this.alpha = alpha
            cacheIsDirty = true
        }
    }

    /**
     * Updates the size.
     */
    fun update(size: PxSize) {
        if (this.size != size) {
            this.size = size
            cacheIsDirty = true
        }
    }

    private fun updateCache() {
        if (cacheIsDirty) {
            cacheIsDirty = false
            usePathForClip = false
            val shape = this.shape
            if (shape == null || size.width == 0.px || size.height == 0.px) {
                cachedOutline.setEmpty()
                return
            }
            cachedOutline.alpha = alpha
            val outline = shape.createOutline(size, density)
            when (outline) {
                is Outline.Rectangle -> updateCacheWithRect(outline.rect)
                is Outline.Rounded -> updateCacheWithRRect(outline.rrect)
                is Outline.Generic -> updateCacheWithPath(outline.path)
            }
        }
    }

    private fun updateCacheWithRect(rect: Rect) {
        cachedOutline.setRect(
            rect.left.roundToInt(),
            rect.top.roundToInt(),
            rect.right.roundToInt(),
            rect.bottom.roundToInt()
        )
    }

    private fun updateCacheWithRRect(rrect: RRect) {
        val radius = rrect.topLeftRadiusX
        if (rrect.isSimple) {
            cachedOutline.setRoundRect(
                rrect.left.roundToInt(),
                rrect.top.roundToInt(),
                rrect.right.roundToInt(),
                rrect.bottom.roundToInt(),
                radius
            )
        } else {
            val path = cachedRrectPath ?: Path().also { cachedRrectPath = it }
            path.reset()
            path.addRRect(rrect)
            updateCacheWithPath(path)
        }
    }

    private fun updateCacheWithPath(composePath: Path) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P || composePath.isConvex) {
            cachedOutline.setConvexPath(composePath.toFrameworkPath())
            usePathForClip = !cachedOutline.canClip()
        } else {
            cachedOutline.setEmpty()
            usePathForClip = true
        }
        outlinePath = composePath
    }
}
