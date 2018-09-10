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

package androidx.ui.rendering

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Size
import androidx.ui.rendering.box.RenderBox

class RenderSizedBox(private val forcedSize: Size) : RenderBox() {

    override fun computeMinIntrinsicWidth(height: Double): Double {
        return forcedSize.width
    }

    override fun computeMaxIntrinsicWidth(height: Double): Double {
        return forcedSize.width
    }

    override fun computeMinIntrinsicHeight(width: Double): Double {
        return forcedSize.height
    }

    override fun computeMaxIntrinsicHeight(width: Double): Double {
        return forcedSize.height
    }

    override val sizedByParent = true

    override fun performResize() {
        size = constraints!!.constrain(forcedSize)
    }

    override fun performLayout() { }

    override fun hitTestSelf(position: Offset) = true
}