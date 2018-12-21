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

import androidx.ui.async.Timer
import androidx.ui.engine.geometry.Size
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.proxybox.RenderProxyBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestCoroutineContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DynamicIntrinsicsTest {

    class RenderFixedSize : RenderBox() {
        var dimension = 100.0f

        fun grow() {
            dimension *= 2.0f
            markNeedsLayout()
        }

        override fun computeMinIntrinsicWidth(height: Float) = dimension
        override fun computeMaxIntrinsicWidth(height: Float) = dimension
        override fun computeMinIntrinsicHeight(width: Float) = dimension
        override fun computeMaxIntrinsicHeight(width: Float) = dimension

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
                    child!!.getMinIntrinsicWidth(Float.POSITIVE_INFINITY),
                    child!!.getMinIntrinsicHeight(Float.POSITIVE_INFINITY)
            )
        }
    }

    private lateinit var job: Job

    @Before
    fun setup() {
        job = Job()
        Timer.scope = CoroutineScope(TestCoroutineContext() + job)
    }

    @After
    fun teardown() {
        job.cancel()
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
                        minWidth = 0.0f,
                        minHeight = 0.0f,
                        maxWidth = 1000.0f,
                        maxHeight = 1000.0f
                )
        )
        assertEquals(root.size, inner.size)

        inner.grow()
        root.markNeedsLayout()
        pumpFrame()
        assertEquals(root.size, inner.size)
    }
}