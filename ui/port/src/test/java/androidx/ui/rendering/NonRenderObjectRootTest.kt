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
import androidx.ui.foundation.AbstractNode
import androidx.ui.painting.alignment.Alignment
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.obj.PipelineOwner
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.shiftedbox.RenderPositionedBox
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NonRenderObjectRootTest {

    class RealRoot(val child: RenderObject?) : AbstractNode() {

        init {
            if (child != null) {
                adoptChild(child)
            }
        }

        override fun redepthChildren() {
            if (child != null)
                redepthChild(child)
        }

        override fun attach(owner: Any) {
            super.attach(owner)
            child?.attach(owner)
        }

        override fun detach() {
            super.detach()
            child?.detach()
        }

        fun layout() {
            child?.layout(BoxConstraints.tight(Size(500.0, 500.0)))
        }
    }

    @Test
    fun `non-RenderObject roots`() {
        val child = RenderPositionedBox(
        alignment = Alignment.center,
        child = RenderSizedBox(Size(100.0, 100.0))
        )
        val root = RealRoot(child = child)
        root.attach(PipelineOwner())

        child.scheduleInitialLayout()
        root.layout()

        child.markNeedsLayout()
        root.layout()
    }
}