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

import androidx.ui.core.adapter.Draw
import androidx.ui.engine.geometry.Offset
import androidx.ui.painting.Path
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.CompositionContext

/**
 * A widget that clips its child using a path.
 *
 * Calls a callback on a delegate whenever the widget is to be
 * painted. The callback returns a path and the widget prevents the
 * child from painting outside the path.
 *
 * Clipping to a path is expensive. Certain shapes have more
 * optimized widgets:
 *
 *  * To clip to a rectangle, consider [ClipRect].
 *  * To clip to an oval or circle, consider [ClipOval].
 *  * To clip to a rounded rectangle, consider [ClipRRect].
 */
class ClipPath(
    /**
     * If [clipper] is null, the clip will be a rectangle that matches the layout
     * size and location of the child. However, rather than use this default,
     * consider using a [ClipRect], which can achieve the same effect more
     * efficiently.
     */
    var clipper: CustomClipper<Path>? = null,
    @Children var children: () -> Unit
) : Component() {

    private val clipHolder = ClipHolder(this::recompose) { size ->
        val path = Path()
        path.addRect(Offset.zero and size)
        path
    }

//    // TODO("Migration|Andrey: Needs semantics in R4a")
//    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
//        super.debugFillProperties(properties)
//        properties.add(DiagnosticsProperty.create("clipper", clipper, defaultValue = null))
//    }

    // TODO("Andrey: We need a mechanism to only allow taps within the clip path")
//    override fun hitTest(result: HitTestResult, position: Offset): Boolean {
//        if (clipper != null) {
//            updateClip()
//            assert(clip != null)
//            if (!clip!!.contains(position)) {
//                return false
//            }
//        }
//        return super.hitTest(result, position = position)
//    }

    override fun compose() {
        val context = CompositionContext.current.context
        <Draw> canvas, parentSize ->
            canvas.save()
            canvas.clipPath(clipHolder.getClip(clipper, parentSize, context))
        </Draw>
        <children/>
        <Draw> canvas, _ ->
            canvas.restore()
        </Draw>
        // TODO("Andrey: Call clipHolder.dispose() in onDispose effect")
    }

}