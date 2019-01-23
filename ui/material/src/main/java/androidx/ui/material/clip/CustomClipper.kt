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
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.foundation.change_notifier.Listenable

/**
 * An interface for providing custom clips.
 *
 * This class is used by a number of clip widgets (e.g., [ClipRect] and
 * [ClipPath]).
 *
 * The [getClip] method is called whenever the custom clip needs to be updated.
 *
 * The most efficient way to update the clip provided by this class is to
 * supply a reclip argument to the constructor of the [CustomClipper]. The
 * custom object will listen to this animation and update the clip whenever the
 * animation ticks, avoiding both the build and layout phases of the pipeline.
 *
 * See also:
 *
 *  * [ClipRect], which can be customized with a [CustomClipper].
 *  * [ClipRRect], which can be customized with a [CustomClipper].
 *  * [ClipOval], which can be customized with a [CustomClipper].
 *  * [ClipPath], which can be customized with a [CustomClipper].
 */
abstract class CustomClipper<T>(
    /** The clipper will update its clip whenever [reclip] notifies its listeners. */
    val reclip: Listenable? = null
) {

    /**
     * Returns a description of the clip given that the render object being
     * clipped is of the given size.
     */
    abstract fun getClip(size: Size, density: Density): T

    /**
     * Returns an approximation of the clip returned by [getClip], as
     * an axis-aligned Rect. This is used by the semantics layer to
     * determine whether widgets should be excluded.
     *
     * By default, this returns a rectangle that is the same size as
     * the RenderObject. If getClip returns a shape that is roughly the
     * same size as the RenderObject (e.g. it's a rounded rectangle
     * with very small arcs in the corners), then this may be adequate.
     */
    fun getApproximateClipRect(size: Size): Rect = Offset.zero and size
}
