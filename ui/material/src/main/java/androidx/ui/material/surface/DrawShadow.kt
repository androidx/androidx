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

package androidx.ui.material.surface

import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.dp
import androidx.ui.material.clip.CustomClipper
import androidx.ui.painting.Path
import androidx.compose.Composable
import androidx.compose.composer

/**
 * Draws the shadow. The [elevation] defines the visual dept of the physical object.
 * The physical object has a shape specified by [clipper].
 *
 * TODO("Andrey: Find the proper module and package for it")
 *
 * @param elevation The z-coordinate at which to place this physical object.
 * @param clipper Defines a shape of the physical object
 */
@Composable
fun DrawShadow(
    elevation: Dp,
    @Suppress("UNUSED_PARAMETER")
    clipper: CustomClipper<Path>
) {
    if (elevation != 0.dp) {
        Draw { _, _ ->
            TODO("Migration|Andrey: Needs canvas.drawShadow. b/123215187")
//            canvas.drawShadow(
//                clipper.getClip(parentSize, density),
//                shadowColor,
//                elevation,
//                color.alpha != 0xFF
//            )
        }
    }
}
