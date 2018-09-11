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

import androidx.ui.assert
import androidx.ui.engine.geometry.Size
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box.RenderBox
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DoubleProperty

// /// Attempts to size the child to a specific aspect ratio.
// ///
// /// The render object first tries the largest width permitted by the layout
// /// constraints. The height of the render object is determined by applying the
// /// given aspect ratio to the width, expressed as a ratio of width to height.
// ///
// /// For example, a 16:9 width:height aspect ratio would have a value of
// /// 16.0/9.0. If the maximum width is infinite, the initial width is determined
// /// by applying the aspect ratio to the maximum height.
// ///
// /// Now consider a second example, this time with an aspect ratio of 2.0 and
// /// layout constraints that require the width to be between 0.0 and 100.0 and
// /// the height to be between 0.0 and 100.0. We'll select a width of 100.0 (the
// /// biggest allowed) and a height of 50.0 (to match the aspect ratio).
// ///
// /// In that same situation, if the aspect ratio is 0.5, we'll also select a
// /// width of 100.0 (still the biggest allowed) and we'll attempt to use a height
// /// of 200.0. Unfortunately, that violates the constraints because the child can
// /// be at most 100.0 pixels tall. The render object will then take that value
// /// and apply the aspect ratio again to obtain a width of 50.0. That width is
// /// permitted by the constraints and the child receives a width of 50.0 and a
// /// height of 100.0. If the width were not permitted, the render object would
// /// continue iterating through the constraints. If the render object does not
// /// find a feasible size after consulting each constraint, the render object
// /// will eventually select a size for the child that meets the layout
// /// constraints but fails to meet the aspect ratio constraints.
class RenderAspectRatio(
    child: RenderBox? = null,
    // /// The aspect ratio to attempt to use.
    // ///
    // /// The aspect ratio is expressed as a ratio of width to height. For example,
    // /// a 16:9 width:height aspect ratio would have a value of 16.0/9.0. It must
    // /// be a finite, positive value.
    aspectRatio: Double
) : RenderProxyBox(child) {
    init {
        assert(aspectRatio > 0.0)
        assert(aspectRatio.isFinite())
    }

    var aspectRatio: Double = aspectRatio
        set(value) {
            assert(value > 0.0)
            assert(value.isFinite())
            if (field == value) return
            field = value
            markNeedsLayout()
        }

    override fun computeMinIntrinsicWidth(height: Double): Double {
        if (height.isFinite()) return height * aspectRatio
        return child?.getMinIntrinsicWidth(height) ?: 0.0
    }

    override fun computeMaxIntrinsicWidth(height: Double): Double {
        if (height.isFinite()) return height * aspectRatio
        return child?.getMaxIntrinsicWidth(height) ?: 0.0
    }

    override fun computeMinIntrinsicHeight(width: Double): Double {
        if (width.isFinite()) return width / aspectRatio
        return child?.getMinIntrinsicHeight(width) ?: 0.0
    }

    override fun computeMaxIntrinsicHeight(width: Double): Double {
        if (width.isFinite()) return width / aspectRatio
        return child?.getMaxIntrinsicHeight(width) ?: 0.0
    }

    private fun applyAspectRatio(constraints: BoxConstraints): Size {
        assert(constraints.debugAssertIsValid())
        assert {
            if (!constraints.hasBoundedWidth && !constraints.hasBoundedHeight) {
                val name = this.javaClass.canonicalName
                throw FlutterError(
                    "$name has unbounded constraints.\n" +
                            "This $name was given an aspect ratio of $aspectRatio but was " +
                            "given both unbounded width and unbounded height constraints. " +
                            "Because both constraints were unbounded, this render object " +
                            "doesn\'t know how much size to consume."
                )
            }
            true
        }

        if (constraints.isTight) return constraints.smallest

        var width: Double = constraints.maxWidth
        var height: Double

        // We default to picking the height based on the width, but if the width
        // would be infinite, that's not sensible so we try to infer the height
        // from the width.
        if (width.isFinite()) {
            height = width / aspectRatio
        } else {
            height = constraints.maxHeight
            width = height * aspectRatio
        }

        // Similar to RenderImage, we iteratively attempt to fit within the given
        // constraints while maintaining the given aspect ratio. The order of
        // applying the constraints is also biased towards inferring the height
        // from the width.

        if (width > constraints.maxWidth) {
            width = constraints.maxWidth
            height = width / aspectRatio
        }

        if (height > constraints.maxHeight) {
            height = constraints.maxHeight
            width = height * aspectRatio
        }

        if (width < constraints.minWidth) {
            width = constraints.minWidth
            height = width / aspectRatio
        }

        if (height < constraints.minHeight) {
            height = constraints.minHeight
            width = height * aspectRatio
        }

        return constraints.constrain(Size(width, height))
    }

    override fun performLayout() {
        size = applyAspectRatio(constraints!!)
        child?.layout(BoxConstraints.tight(size))
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DoubleProperty.create("aspectRatio", aspectRatio))
    }
}