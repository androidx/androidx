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
import androidx.ui.matchers.MoreOrLessEquals
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.proxybox.RenderIntrinsicHeight
import androidx.ui.rendering.proxybox.RenderIntrinsicWidth
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IntrinsicWidthTest {

    // before using this, consider using RenderSizedBox from rendering_tester.dart
    class RenderTestBox(private val _intrinsicDimensions: BoxConstraints) : RenderBox() {

        override fun computeMinIntrinsicWidth(height: Double) = _intrinsicDimensions.minWidth

        override fun computeMaxIntrinsicWidth(height: Double) = _intrinsicDimensions.maxWidth

        override fun computeMinIntrinsicHeight(width: Double) = _intrinsicDimensions.minHeight

        override fun computeMaxIntrinsicHeight(width: Double) = _intrinsicDimensions.maxHeight

        override val sizedByParent = true

        override fun performResize() {
            size = constraints!!.constrain(
                    Size(_intrinsicDimensions.minWidth +
                            (_intrinsicDimensions.maxWidth - _intrinsicDimensions.minWidth) / 2.0,
                            _intrinsicDimensions.minHeight + (_intrinsicDimensions.maxHeight -
                                    _intrinsicDimensions.minHeight) / 2.0))
        }
    }

    @Test
    fun `Shrink-wrapping width`() {
        val child = RenderTestBox(
                BoxConstraints(minWidth = 10.0, maxWidth = 100.0, minHeight = 20.0,
                        maxHeight = 200.0))
        val parent = RenderIntrinsicWidth(child = child)
        layout(parent,
                constraints = BoxConstraints(
                        minWidth = 5.0,
                        minHeight = 8.0,
                        maxWidth = 500.0,
                        maxHeight = 800.0
                )
        )
        assertThat(100.0, MoreOrLessEquals(parent.size.width))
        assertThat(110.0, MoreOrLessEquals(parent.size.height))

        assertThat(100.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(0.0)))
        assertThat(100.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(0.0)))
        assertThat(20.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(0.0)))
        assertThat(200.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(0.0)))

        assertThat(100.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(10.0)))
        assertThat(100.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(10.0)))
        assertThat(20.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(10.0)))
        assertThat(200.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(10.0)))

        assertThat(100.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(80.0)))
        assertThat(100.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(80.0)))
        assertThat(20.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(80.0)))
        assertThat(200.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(80.0)))

        assertThat(100.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(100.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(20.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(Double.POSITIVE_INFINITY)))
        assertThat(200.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)))
    }

    @Test
    fun `IntrinsicWidth without a child`() {
        val parent = RenderIntrinsicWidth()
        layout(parent,
                constraints = BoxConstraints(
                        minWidth = 5.0,
                        minHeight = 8.0,
                        maxWidth = 500.0,
                        maxHeight = 800.0
                )
        )
        assertThat(5.0, MoreOrLessEquals(parent.size.width))
        assertThat(8.0, MoreOrLessEquals(parent.size.height))

        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(0.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(0.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(0.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(0.0)))

        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(10.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(10.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(10.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(10.0)))

        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(80.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(80.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(80.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(80.0)))

        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(Double.POSITIVE_INFINITY)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)))
    }

    @Test
    fun `Shrink-wrapping width (stepped width)`() {
        val child = RenderTestBox(
                BoxConstraints(minWidth = 10.0, maxWidth = 100.0, minHeight = 20.0,
                        maxHeight = 200.0))
        val parent = RenderIntrinsicWidth(child = child, _stepWidth = 47.0)
        layout(parent,
                constraints = BoxConstraints(
                        minWidth = 5.0,
                        minHeight = 8.0,
                        maxWidth = 500.0,
                        maxHeight = 800.0
                )
        )
        assertThat(3.0 * 47.0, MoreOrLessEquals(parent.size.width))
        assertThat(110.0, MoreOrLessEquals(parent.size.height))

        assertThat(3.0 * 47.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(0.0)))
        assertThat(3.0 * 47.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(0.0)))
        assertThat(20.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(0.0)))
        assertThat(200.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(0.0)))

        assertThat(3.0 * 47.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(10.0)))
        assertThat(3.0 * 47.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(10.0)))
        assertThat(20.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(10.0)))
        assertThat(200.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(10.0)))

        assertThat(3.0 * 47.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(80.0)))
        assertThat(3.0 * 47.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(80.0)))
        assertThat(20.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(80.0)))
        assertThat(200.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(80.0)))

        assertThat(3.0 * 47.0,
                MoreOrLessEquals(parent.getMinIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(3.0 * 47.0,
                MoreOrLessEquals(parent.getMaxIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(20.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(Double.POSITIVE_INFINITY)))
        assertThat(200.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)))
    }

    @Test
    fun `Shrink-wrapping width (stepped height)`() {
        val child = RenderTestBox(
                BoxConstraints(minWidth = 10.0, maxWidth = 100.0, minHeight = 20.0,
                        maxHeight = 200.0))
        val parent = RenderIntrinsicWidth(child = child, _stepHeight = 47.0)
        layout(parent,
                constraints = BoxConstraints(
                        minWidth = 5.0,
                        minHeight = 8.0,
                        maxWidth = 500.0,
                        maxHeight = 800.0
                )
        )
        assertThat(100.0, MoreOrLessEquals(parent.size.width))
        assertThat(235.0, MoreOrLessEquals(parent.size.height))

        assertThat(100.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(0.0)))
        assertThat(100.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(0.0)))
        assertThat(1.0 * 47.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(0.0)))
        assertThat(5.0 * 47.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(0.0)))

        assertThat(100.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(10.0)))
        assertThat(100.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(10.0)))
        assertThat(1.0 * 47.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(10.0)))
        assertThat(5.0 * 47.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(10.0)))

        assertThat(100.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(80.0)))
        assertThat(100.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(80.0)))
        assertThat(1.0 * 47.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(80.0)))
        assertThat(5.0 * 47.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(80.0)))

        assertThat(100.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(100.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(1.0 * 47.0,
                MoreOrLessEquals(parent.getMinIntrinsicHeight(Double.POSITIVE_INFINITY)))
        assertThat(5.0 * 47.0,
                MoreOrLessEquals(parent.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)))
    }

    @Test
    fun `Shrink-wrapping width (stepped everything)`() {
        val child = RenderTestBox(
                BoxConstraints(minWidth = 10.0, maxWidth = 100.0, minHeight = 20.0,
                        maxHeight = 200.0))
        val parent = RenderIntrinsicWidth(child = child, _stepHeight = 47.0, _stepWidth = 37.0)
        layout(parent,
                constraints = BoxConstraints(
                        minWidth = 5.0,
                        minHeight = 8.0,
                        maxWidth = 500.0,
                        maxHeight = 800.0
                )
        )
        assertThat(3.0 * 37.0, MoreOrLessEquals(parent.size.width))
        assertThat(235.0, MoreOrLessEquals(parent.size.height))

        assertThat(3.0 * 37.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(0.0)))
        assertThat(3.0 * 37.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(0.0)))
        assertThat(1.0 * 47.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(0.0)))
        assertThat(5.0 * 47.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(0.0)))

        assertThat(3.0 * 37.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(10.0)))
        assertThat(3.0 * 37.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(10.0)))
        assertThat(1.0 * 47.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(10.0)))
        assertThat(5.0 * 47.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(10.0)))

        assertThat(3.0 * 37.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(80.0)))
        assertThat(3.0 * 37.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(80.0)))
        assertThat(1.0 * 47.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(80.0)))
        assertThat(5.0 * 47.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(80.0)))

        assertThat(3.0 * 37.0,
                MoreOrLessEquals(parent.getMinIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(3.0 * 37.0,
                MoreOrLessEquals(parent.getMaxIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(1.0 * 47.0,
                MoreOrLessEquals(parent.getMinIntrinsicHeight(Double.POSITIVE_INFINITY)))
        assertThat(5.0 * 47.0,
                MoreOrLessEquals(parent.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)))
    }

    @Test
    fun `Shrink-wrapping height`() {
        val child = RenderTestBox(
                BoxConstraints(minWidth = 10.0, maxWidth = 100.0, minHeight = 20.0,
                        maxHeight = 200.0))
        val parent = RenderIntrinsicHeight(child = child)
        layout(parent,
                constraints = BoxConstraints(
                        minWidth = 5.0,
                        minHeight = 8.0,
                        maxWidth = 500.0,
                        maxHeight = 800.0
                )
        )
        assertThat(55.0, MoreOrLessEquals(parent.size.width))
        assertThat(200.0, MoreOrLessEquals(parent.size.height))

        assertThat(10.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(0.0)))
        assertThat(100.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(0.0)))
        assertThat(200.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(0.0)))
        assertThat(200.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(0.0)))

        assertThat(10.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(10.0)))
        assertThat(100.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(10.0)))
        assertThat(200.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(10.0)))
        assertThat(200.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(10.0)))

        assertThat(10.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(80.0)))
        assertThat(100.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(80.0)))
        assertThat(200.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(80.0)))
        assertThat(200.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(80.0)))

        assertThat(10.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(100.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(200.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(Double.POSITIVE_INFINITY)))
        assertThat(200.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)))
    }

    @Test
    fun `IntrinsicHeight without a child`() {
        val parent = RenderIntrinsicHeight()
        layout(parent,
                constraints = BoxConstraints(
                        minWidth = 5.0,
                        minHeight = 8.0,
                        maxWidth = 500.0,
                        maxHeight = 800.0
                )
        )
        assertThat(5.0, MoreOrLessEquals(parent.size.width))
        assertThat(8.0, MoreOrLessEquals(parent.size.height))

        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(0.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(0.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(0.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(0.0)))

        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(10.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(10.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(10.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(10.0)))

        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(80.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(80.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(80.0)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(80.0)))

        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicWidth(Double.POSITIVE_INFINITY)))
        assertThat(0.0, MoreOrLessEquals(parent.getMinIntrinsicHeight(Double.POSITIVE_INFINITY)))
        assertThat(0.0, MoreOrLessEquals(parent.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)))
    }

    // TODO("Migration/Andrey: Next tests needs RenderPadding class")
//    @Test
//    fun `Padding and boring intrinsics`() {
//        val box = RenderPadding(
//                padding = EdgeInsets.all(15.0),
//                child = RenderSizedBox(Size(20.0, 20.0))
//        )
//
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicWidth(0.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(0.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicHeight(0.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(0.0)))
//
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicWidth(10.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(10.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicHeight(10.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(10.0)))
//
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicWidth(80.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(80.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicHeight(80.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(80.0)))
//
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicWidth(Double.POSITIVE_INFINITY)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(Double.POSITIVE_INFINITY)))
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicHeight(Double.POSITIVE_INFINITY)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)))
//
//        // also a smoke test:
//        layout(
//                box,
//                constraints = BoxConstraints(
//                        minWidth = 10.0,
//                        minHeight = 10.0,
//                        maxWidth = 10.0,
//                        maxHeight = 10.0
//                )
//        )
//    }
//
//    @Test
//    fun `Padding and interesting intrinsics`() {
//        val box = RenderPadding(
//                padding = EdgeInsets.all(15.0),
//                child = RenderAspectRatio(aspectRatio= 1.0)
//        )
//
//        assertThat(30.0, MoreOrLessEquals(box.getMinIntrinsicWidth(0.0)))
//        assertThat(30.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(0.0)))
//        assertThat(30.0, MoreOrLessEquals(box.getMinIntrinsicHeight(0.0)))
//        assertThat(30.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(0.0)))
//
//        assertThat(30.0, MoreOrLessEquals(box.getMinIntrinsicWidth(10.0)))
//        assertThat(30.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(10.0)))
//        assertThat(30.0, MoreOrLessEquals(box.getMinIntrinsicHeight(10.0)))
//        assertThat(30.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(10.0)))
//
//        assertThat(80.0, MoreOrLessEquals(box.getMinIntrinsicWidth(80.0)))
//        assertThat(80.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(80.0)))
//        assertThat(80.0, MoreOrLessEquals(box.getMinIntrinsicHeight(80.0)))
//        assertThat(80.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(80.0)))
//
//        assertThat(30.0, MoreOrLessEquals(box.getMinIntrinsicWidth(Double.POSITIVE_INFINITY)))
//        assertThat(30.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(Double.POSITIVE_INFINITY)))
//        assertThat(30.0, MoreOrLessEquals(box.getMinIntrinsicHeight(Double.POSITIVE_INFINITY)))
//        assertThat(30.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)))
//
//        // also a smoke test:
//        layout(
//                box,
//                constraints = BoxConstraints(
//                        minWidth = 10.0,
//                        minHeight = 10.0,
//                        maxWidth = 10.0,
//                        maxHeight = 10.0
//                )
//        );
//    }
//
//    @Test
//    fun `Padding and boring intrinsics 2`() {
//        val box = RenderPadding(
//                padding = EdgeInsets.all(15.0),
//                child = RenderSizedBox(Size(20.0, 20.0))
//        )
//
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicWidth(0.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(0.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicHeight(0.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(0.0)))
//
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicWidth(10.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(10.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicHeight(10.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(10.0)))
//
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicWidth(80.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(80.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicHeight(80.0)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(80.0)))
//
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicWidth(Double.POSITIVE_INFINITY)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(Double.POSITIVE_INFINITY)))
//        assertThat(50.0, MoreOrLessEquals(box.getMinIntrinsicHeight(Double.POSITIVE_INFINITY)))
//        assertThat(50.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)))
//
//        // also a smoke test:
//        layout(
//                box,
//                constraints = BoxConstraints(
//                        minWidth = 10.0,
//                        minHeight = 10.0,
//                        maxWidth = 10.0,
//                        maxHeight = 10.0
//                )
//        )
//    }
//
//    @Test
//    fun `Padding and interesting intrinsics 2`() {
//        val box = RenderPadding(
//                padding = EdgeInsets.all(15.0),
//                child = RenderAspectRatio(aspectRatio = 1.0)
//        )
//
//        assertThat(30.0, MoreOrLessEquals(box.getMinIntrinsicWidth(0.0)))
//        assertThat(30.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(0.0)))
//        assertThat(30.0, MoreOrLessEquals(box.getMinIntrinsicHeight(0.0)))
//        assertThat(30.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(0.0)))
//
//        assertThat(30.0, MoreOrLessEquals(box.getMinIntrinsicWidth(10.0)))
//        assertThat(30.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(10.0)))
//        assertThat(30.0, MoreOrLessEquals(box.getMinIntrinsicHeight(10.0)))
//        assertThat(30.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(10.0)))
//
//        assertThat(80.0, MoreOrLessEquals(box.getMinIntrinsicWidth(80.0)))
//        assertThat(80.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(80.0)))
//        assertThat(80.0, MoreOrLessEquals(box.getMinIntrinsicHeight(80.0)))
//        assertThat(80.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(80.0)))
//
//        assertThat(30.0, MoreOrLessEquals(box.getMinIntrinsicWidth(Double.POSITIVE_INFINITY)))
//        assertThat(30.0, MoreOrLessEquals(box.getMaxIntrinsicWidth(Double.POSITIVE_INFINITY)))
//        assertThat(30.0, MoreOrLessEquals(box.getMinIntrinsicHeight(Double.POSITIVE_INFINITY)))
//        assertThat(30.0, MoreOrLessEquals(box.getMaxIntrinsicHeight(Double.POSITIVE_INFINITY)))
//
//        // also a smoke test:
//        layout(
//                box,
//                constraints = BoxConstraints(
//                        minWidth = 10.0,
//                        minHeight = 10.0,
//                        maxWidth = 10.0,
//                        maxHeight = 10.0
//                )
//        )
//    }
}