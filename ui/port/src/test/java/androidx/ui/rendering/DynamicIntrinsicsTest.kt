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

import androidx.ui.engine.geometry.Size
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.proxybox.RenderProxyBox
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DynamicIntrinsicsTest {

    class RenderFixedSize : RenderBox() {
        var dimension = 100.0

        fun grow() {
            dimension *= 2.0
            markNeedsLayout()
        }

        override fun computeMinIntrinsicWidth(height: Double) = dimension
        override fun computeMaxIntrinsicWidth(height: Double) = dimension
        override fun computeMinIntrinsicHeight(width: Double) = dimension
        override fun computeMaxIntrinsicHeight(width: Double) = dimension

        override fun performLayout() {
            size = Size.square(dimension)
        }
    }

    class RenderParentSize(child: RenderBox) : RenderProxyBox(child) {

        override val sizedByParent = true

        override fun performResize() {
            size = constraints!!.biggest
        }

        override fun performLayout() {
            child!!.layout(constraints!!)
        }
    }

    class RenderIntrinsicSize(child: RenderBox) : RenderProxyBox(child) {

        override fun performLayout() {
            child!!.layout(constraints!!)
            size = Size(
                    child!!.getMinIntrinsicWidth(Double.POSITIVE_INFINITY),
                    child!!.getMinIntrinsicHeight(Double.POSITIVE_INFINITY)
            )
        }
    }

    @Test
    fun `Whether using intrinsics means you get hooked into layout`() {
        val inner = RenderFixedSize()
        val root = RenderIntrinsicSize(
                child = RenderParentSize(
                        child = inner
                )
        )
        layout(root,
                constraints = BoxConstraints(
                        minWidth = 0.0,
                        minHeight = 0.0,
                        maxWidth = 1000.0,
                        maxHeight = 1000.0
                )
        )
        assertEquals(root.size, inner.size)

        inner.grow()
        root.markNeedsLayout()
        pumpFrame()
        assertEquals(root.size, inner.size)
    }
}