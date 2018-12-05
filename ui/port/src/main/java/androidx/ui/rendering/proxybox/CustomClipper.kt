/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering.proxybox

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
 * The [shouldReclip] method is called when a new instance of the class
 * is provided, to check if the new instance actually represents different
 * information.
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
    abstract fun getClip(size: Size): T

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

    /**
     * Called whenever a new instance of the custom clipper delegate class is
     * provided to the clip object, or any time that a new clip object is created
     * with a new instance of the custom painter delegate class (which amounts to
     * the same thing, because the latter is implemented in terms of the former).
     *
     * If the new instance represents different information than the old
     * instance, then the method should return true, otherwise it should return
     * false.
     *
     * If the method returns false, then the [getClip] call might be optimized
     * away.
     *
     * It's possible that the [getClip] method will get called even if
     * [shouldReclip] returns false or if the [shouldReclip] method is never
     * called at all (e.g. if the box changes size).
     */
    abstract fun shouldReclip(oldClipper: CustomClipper<T>): Boolean
}