/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core.operations.layout.managers

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasurePool
import androidx.compose.remote.core.operations.layout.measure.MeasurePass
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class CollapsibleColumnPartialPassTest {

    private open class TestLayoutComponent(parent: Component?, id: Int, w: Float, h: Float) :
        LayoutComponent(parent, id, -1, 0f, 0f, w, h) {
        init {
            mWidthModifier = WidthModifierOperation(DimensionModifierOperation.Type.FILL, 1f)
            mHeightModifier = HeightModifierOperation(DimensionModifierOperation.Type.FILL, 1f)
        }
    }

    private class TestChildComponent(parent: Component?, id: Int) :
        TestLayoutComponent(parent, id, 100f, 50f) {
        var measureCount = 0

        override fun measure(
            context: PaintContext,
            minWidth: Float,
            maxWidth: Float,
            minHeight: Float,
            maxHeight: Float,
            measurePass: MeasurePass,
        ) {
            measureCount++
            super.measure(context, minWidth, maxWidth, minHeight, maxHeight, measurePass)
        }
    }

    private class TestCollapsibleColumnLayout(parent: Component?, id: Int) :
        CollapsibleColumnLayout(parent, id, -1, 0, 0, 0f) {
        init {
            mWidthModifier = WidthModifierOperation(DimensionModifierOperation.Type.FILL, 1f)
            mHeightModifier = HeightModifierOperation(DimensionModifierOperation.Type.FILL, 1f)
        }

        fun addChild(c: Component) {
            mChildrenComponents.add(c)
            mList.add(c)
        }

        override fun isRelayoutBoundary(): Boolean = true
    }

    private class TestColumnLayout(parent: Component?, id: Int) :
        ColumnLayout(parent, id, -1, 0, 0, 0f) {
        init {
            mWidthModifier = WidthModifierOperation(DimensionModifierOperation.Type.FILL, 1f)
            mHeightModifier = HeightModifierOperation(DimensionModifierOperation.Type.FILL, 1f)
        }

        fun addChild(c: Component) {
            mChildrenComponents.add(c)
            mList.add(c)
        }

        override fun isRelayoutBoundary(): Boolean = true
    }

    @Test
    fun testCollapsibleColumnPartialPassStaleChildMeasureRepro() {
        val pool = ComponentMeasurePool()
        val mockPaintContext: PaintContext = mock { on { density } doReturn 1.0f }
        val mockDocument: CoreDocument = mock {
            on { originX } doReturn 0.0f
            on { originY } doReturn 0.0f
        }
        val mockRemoteContext: RemoteContext = mock {
            on { paintContext } doReturn mockPaintContext
            on { componentMeasurePool } doReturn pool
            on { document } doReturn mockDocument
        }

        val root = RootLayoutComponent(-2, 0f, 0f, 800f, 800f, null)
        val collapsible = TestCollapsibleColumnLayout(root, 100)
        root.mList.add(collapsible)

        val child1 = TestChildComponent(collapsible, 101)
        val child2 = TestChildComponent(collapsible, 102)
        collapsible.addChild(child1)
        collapsible.addChild(child2)

        assertThat(collapsible.isRelayoutBoundary).isTrue()

        // 1. Initial full layout pass
        root.mNeedsMeasure = true
        root.layout(mockRemoteContext)

        val initialChild2Count = child2.measureCount
        assertThat(initialChild2Count).isGreaterThan(0)

        // 2. Child 2 invalidates measure
        child2.invalidateMeasure()

        // 3. Perform partial layout pass
        root.layout(mockRemoteContext)

        // Verify that child2 is re-measured in the partial pass after fix
        assertThat(child2.measureCount).isGreaterThan(initialChild2Count)
    }

    @Test
    fun testDirtyBoundaryNotClearedInFullPassRepro() {
        val pool = ComponentMeasurePool()
        val mockPaintContext: PaintContext = mock { on { density } doReturn 1.0f }
        val mockDocument: CoreDocument = mock {
            on { originX } doReturn 0.0f
            on { originY } doReturn 0.0f
        }
        val mockRemoteContext: RemoteContext = mock {
            on { paintContext } doReturn mockPaintContext
            on { componentMeasurePool } doReturn pool
            on { document } doReturn mockDocument
        }

        val root = RootLayoutComponent(-2, 0f, 0f, 800f, 800f, null)
        val boundary = TestColumnLayout(root, 100)
        root.mList.add(boundary)

        val child = TestChildComponent(boundary, 101)
        boundary.addChild(child)

        // 1. Child invalidates measure before initial full pass
        child.invalidateMeasure()
        assertThat(boundary.mNeedsMeasure).isTrue()

        // 2. Initial full pass runs
        root.mNeedsMeasure = true
        root.layout(mockRemoteContext)

        // After full pass, boundary.mNeedsMeasure is set to false.
        assertThat(boundary.mNeedsMeasure).isFalse()

        // Verify dirty boundaries are cleared after full pass
        assertThat(root.needsMeasure()).isFalse()
    }
}
