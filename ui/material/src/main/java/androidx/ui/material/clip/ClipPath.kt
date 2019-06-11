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

import androidx.ui.core.Draw
import androidx.ui.painting.Path
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer

/**
 * A Composable that clips its child using a path.
 *
 * Clipping to a path is expensive. Certain shapes have more
 * optimized composables:
 *
 * TODO("Andrey: provide this extra clip composables")
 *  * To clip to a rectangle, consider [ClipRect].
 *  * To clip to an oval or circle, consider [ClipOval].
 *  * To clip to a rounded rectangle, consider [ClipRRect].
 */
@Composable
fun ClipPath(
    clipper: CustomClipper<Path>,
    @Children children: @Composable() () -> Unit
) {
// TODO("Andrey: We will need a mechanism to only allow taps within the clip path")
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
    Draw { canvas, parentSize ->
        // TODO (njawad) replace with save lambda when multi children DrawNodes are supported
        canvas.nativeCanvas.save()
        canvas.clipPath(clipper.getClip(parentSize, density))
    }
    children()
    Draw { canvas, _ ->
        // TODO (njawad) replace with save lambda when multi children DrawNodes are supported
        canvas.nativeCanvas.restore()
    }
}