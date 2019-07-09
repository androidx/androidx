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

package androidx.ui.painting

import androidx.ui.graphics.Color

/**
 * A description of a color filter to apply when drawing a shape or compositing
 * a layer with a particular [Paint]. A color filter is a function that takes
 * two colors, and outputs one color. When applied during compositing, it is
 * independently applied to each pixel of the layer being drawn before the
 * entire layer is merged with the destination.
 *
 * Instances of this class are used with [Paint.colorFilter] on [Paint]
 * objects.
 *
 */
// Ctor comment:
/**
 * Creates a color filter that applies the blend mode given as the second
 * argument. The source color is the one given as the first argument, and the
 * destination color is the one from the layer being composited.
 *
 * The output of this filter is then composited into the background according
 * to the [Paint.blendMode], using the output of this filter as the source
 * and the background as the destination.
 */
data class ColorFilter(
    val color: Color,
    val blendMode: BlendMode
) {

// TODO(Migration/Filip): Not needed for data class
//    @override
//    bool operator ==(dynamic other) {
//        if (other is! ColorFilter)
//        return false;
//        final ColorFilter typedOther = other;
//        return _color == typedOther._color &&
//                _blendMode == typedOther._blendMode;
//    }
//
//    @override
//    int get hashCode => hashValues(_color, _blendMode);

    override fun toString() = "ColorFilter($color, $blendMode)"
}
