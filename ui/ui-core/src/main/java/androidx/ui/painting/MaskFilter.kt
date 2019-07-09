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

// A mask filter to apply to shapes as they are painted. A mask filter is a
// function that takes a bitmap of color pixels, and returns another bitmap of
// color pixels.
//
// Instances of this class are used with [Paint.maskFilter] on [Paint] objects.
// TODO(Migration/njawad: add support for framework's EmbossMaskFilter
data class MaskFilter(val style: BlurStyle, val sigma: Float) {
    // Creates a mask filter that takes the shape being drawn and blurs it.
    //
    // This is commonly used to approximate shadows.
    //
    // The `style` argument controls the kind of effect to draw; see [BlurStyle].
    //
    // The `sigma` argument controls the size of the effect. It is the standard
    // deviation of the Gaussian blur to apply. The value must be greater than
    // zero. The sigma corresponds to very roughly half the radius of the effect
    // in pixels.
    //
    // A blur is an expensive operation and should therefore be used sparingly.
    //
    // The arguments must not be null.
    //
    // See also:
    //
    //  * [Canvas.drawShadow], which is a more efficient way to draw shadows.

    // The type of MaskFilter class to create for Skia.
    // These constants must be kept in sync with MaskFilterType in paint.cc.
    companion object {
        const val TYPE_NONE = 0 // null
        const val TYPE_BLUR = 1 // SkBlurMaskFilter
    }

    //  @override
    //  int get hashCode => hashValues(_style, _sigma);

    //  @override
    //  String toString() => 'MaskFilter.blur($_style, ${_sigma.toStringAsFixed(1)})';
}
