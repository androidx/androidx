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

import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.baseui.shape.Shape
import androidx.ui.core.Dp
import androidx.ui.core.dp

/**
 * Draws the shadow. The [elevation] defines the visual dept of the physical object.
 * The physical object has a shape specified by [shape].
 *
 * TODO("Andrey: Replace with the real implementation based on RenderNode's elevation")
 *
 * @param elevation The z-coordinate at which to place this physical object.
 * @param shape Defines a shape of the physical object
 */
@Composable
fun DrawShadow(
    elevation: Dp,
    @Suppress("UNUSED_PARAMETER")
    shape: Shape
) {
    if (elevation != 0.dp) {
        TODO("Shadows are not yet supported")
    }
}
