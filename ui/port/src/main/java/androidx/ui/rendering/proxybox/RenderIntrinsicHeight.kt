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

import androidx.ui.rendering.box.RenderBox

/**
 * Sizes its child to the child's intrinsic height.
 *
 * This class is useful, for example, when unlimited height is available and
 * you would like a child that would otherwise attempt to expand infinitely to
 * instead size itself to a more reasonable height.
 *
 * This class is relatively expensive, because it adds a speculative layout
 * pass before the final layout phase. Avoid using it where possible. In the
 * worst case, this render object can result in a layout that is O(N²) in the
 * depth of the tree.
 */
class RenderIntrinsicHeight(
    child: RenderBox? = null
) : RenderProxyBox(child) {
/** Creates a render object that sizes itself to its child's intrinsic height. */

    override fun computeMinIntrinsicWidth(height: Double): Double {
        if (child == null)
            return 0.0
        var resultHeight = height
        if (!resultHeight.isFinite())
            resultHeight = child!!.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)
        assert(resultHeight.isFinite())
        return child!!.getMinIntrinsicWidth(resultHeight)
    }

    override fun computeMaxIntrinsicWidth(height: Double): Double {
        if (child == null)
            return 0.0
        var resultHeight = height
        if (!resultHeight.isFinite())
            resultHeight = child!!.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)
        assert(resultHeight.isFinite())
        return child!!.getMaxIntrinsicWidth(resultHeight)
    }

    override fun computeMinIntrinsicHeight(width: Double): Double {
        return computeMaxIntrinsicHeight(width)
    }

    override fun performLayout() {
        if (child != null) {
            var childConstraints = constraints!!
            if (!childConstraints.hasTightHeight) {
                val height = child!!.getMaxIntrinsicHeight(childConstraints.maxWidth)
                assert(height.isFinite())
                childConstraints = childConstraints.tighten(height = height)
            }
            child!!.layout(childConstraints, parentUsesSize = true)
            size = child!!.size
        } else {
            performResize()
        }
    }
}
