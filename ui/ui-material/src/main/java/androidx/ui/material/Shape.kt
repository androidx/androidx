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

package androidx.ui.material

import androidx.compose.ambientOf
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Shape
import androidx.ui.unit.dp

/**
 * Data class holding current shapes for common surfaces like Button or Card.
 */
// TODO(Andrey): should have small, medium, large components categories. b/129278276
// See https://material.io/design/shape/applying-shape-to-ui.html#baseline-shape-values
data class Shapes(
    /**
     * Shape used for [Button]
     */
    val button: Shape = RoundedCornerShape(4.dp),
    /**
     * Shape used for [androidx.ui.material.surface.Card]
     */
    val card: Shape = RectangleShape
    // TODO(Andrey): Add shapes for other surfaces? will see what we need.
)

/**
 * Ambient used to specify the default shapes for the surfaces.
 */
internal val ShapeAmbient = ambientOf { Shapes() }
