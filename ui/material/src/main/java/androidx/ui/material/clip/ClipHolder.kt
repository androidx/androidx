/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.clip

import androidx.ui.core.Density
import androidx.ui.core.PixelSize
import androidx.ui.engine.geometry.Size

/**
 * This class helps to maintain a clip object cache.
 *
 */
class ClipHolder<T>(
    /**
     * A callback to invoke when Clipper's parameters have been changed, so a clip
     * should be invalidated.
     */
    private val markNeedsClip: () -> Unit,
    /**
     * Returns a default clip. Will be used when holder would be used with a null clipper.
     */
    private val defaultClip: (Size) -> T
) {

    private var lastClipper: CustomClipper<T>? = null
    private var lastSize: Size? = null
    private var lastClip: T? = null

    /**
     * Returns a clip.
     *
     * It will recreate a clip only when clipper or parent size have been changed,
     * otherwise will return the cached object.
     */
    fun getClip(clipper: CustomClipper<T>?, pixelSize: PixelSize, density: Density): T {
        val size = Size(pixelSize.width, pixelSize.height)
        if (lastClipper != clipper) {
            lastClip = null
            lastClipper?.reclip?.removeListener(markNeedsClip)
            lastClipper = clipper
            clipper?.reclip?.addListener(markNeedsClip)
        }
        if (size != lastSize) {
            lastClip = null
            lastSize = size
        }
        val clip = lastClip ?: clipper?.getClip(size, density) ?: defaultClip(size)
        lastClip = clip
        return clip
    }

    /**
     * Cleans up the listener. Call this from an onDispose effect.
     */
    fun dispose() {
        lastClipper?.reclip?.removeListener(markNeedsClip)
        lastClipper = null
        lastClip = null
        lastSize = null
    }
}