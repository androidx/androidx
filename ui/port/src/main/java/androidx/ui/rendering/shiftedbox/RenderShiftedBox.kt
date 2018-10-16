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

package androidx.ui.rendering.shiftedbox

import androidx.ui.engine.geometry.Offset
import androidx.ui.rendering.box.BoxParentData
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.PaintingContext

// / Abstract class for one-child-layout render boxes that provide control over
// / the child's position.
abstract class RenderShiftedBox(
    child: RenderBox?
) : RenderBox() /*with RenderObjectWithChildMixin<RenderBox>*/ {

    init {
        this.child = child
        markAsLayoutOnlyNode()
    }

    override fun computeMinIntrinsicWidth(height: Double): Double {
        if (child != null)
            return child!!.getMinIntrinsicWidth(height)
        return 0.0
    }

    override fun computeMaxIntrinsicWidth(height: Double): Double {
        if (child != null)
            return child!!.getMaxIntrinsicWidth(height)
        return 0.0
    }

    override fun computeMinIntrinsicHeight(width: Double): Double {
        if (child != null)
            return child!!.getMinIntrinsicHeight(width)
        return 0.0
    }

    override fun computeMaxIntrinsicHeight(width: Double): Double {
        if (child != null)
            return child!!.getMaxIntrinsicHeight(width)
        return 0.0
    }

//    @override
//    double computeDistanceToActualBaseline(TextBaseline baseline) {
//        double result;
//        if (child != null) {
//            assert(!debugNeedsLayout);
//            result = child.getDistanceToActualBaseline(baseline);
//            final BoxParentData childParentData = child.parentData;
//            if (result != null)
//                result += childParentData.offset.dy;
//        } else {
//            result = super.computeDistanceToActualBaseline(baseline);
//        }
//        return result;
//    }

    override fun paint(context: PaintingContext, offset: Offset) {
        if (child != null) {
            val childParentData = child!!.parentData as BoxParentData
            context.paintChild(child!!, childParentData.offset + offset)
        }
    }

//    override fun hitTestChildren(result: HitTestResult, position: Offset): Boolean {
//        if (child != null) {
//            val childParentData = child!!.parentData as BoxParentData;
//            return child.hitTest(result, position = position - childParentData.offset);
//        }
//        return false;
//    }
}