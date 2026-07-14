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

package androidx.compose.remote.core.operations.layout

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OptimizationLevelTest {

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

    private class TestChildComponent(parent: Component?, id: Int) :
        LayoutComponent(parent, id, -1, 0f, 0f, 100f, 50f) {
        init {
            mWidthModifier = WidthModifierOperation(DimensionModifierOperation.Type.FILL, 1f)
            mHeightModifier = HeightModifierOperation(DimensionModifierOperation.Type.FILL, 1f)
        }
    }

    @Test
    fun testDisabledRelayoutBoundariesViaCoreDocument() {
        val doc = CoreDocument()
        doc.setOptimizationLevel(
            CoreDocument.OPTIMIZATION_ALL and CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES.inv()
        )
        assertThat(doc.isRelayoutBoundaryEnabled()).isFalse()

        val root = RootLayoutComponent(-2, 0f, 0f, 800f, 800f, null)
        val boundary = TestColumnLayout(root, 100)
        root.mList.add(boundary)

        val child = TestChildComponent(boundary, 101)
        boundary.addChild(child)

        child.invalidateMeasure()

        assertThat(root.mNeedsMeasure).isTrue()
    }

    @Test
    fun testMeasureCacheAndVersionControlViaCoreDocument() {
        val doc = CoreDocument()
        doc.setOptimizationLevel(
            CoreDocument.OPTIMIZATION_ALL and CoreDocument.OPTIMIZATION_MEASURE_CACHE.inv()
        )
        assertThat(doc.isMeasureCacheEnabled()).isFalse()

        doc.setMeasureVersion(1)
        assertThat(doc.getMeasureVersion()).isEqualTo(1)
    }

    @Test
    fun testOptimizationLevelBitmaskViaCoreDocument() {
        val doc = CoreDocument()
        doc.setOptimizationLevel(CoreDocument.OPTIMIZATION_NONE)
        assertThat(doc.getOptimizationLevel()).isEqualTo(CoreDocument.OPTIMIZATION_NONE)
        assertThat(doc.isMeasureCacheEnabled()).isFalse()
        assertThat(doc.isRelayoutBoundaryEnabled()).isFalse()
        assertThat(doc.isFlatMeasurePassEnabled()).isFalse()

        doc.setOptimizationLevel(CoreDocument.OPTIMIZATION_MEASURE_CACHE)
        assertThat(doc.getOptimizationLevel()).isEqualTo(CoreDocument.OPTIMIZATION_MEASURE_CACHE)
        assertThat(doc.isMeasureCacheEnabled()).isTrue()
        assertThat(doc.isRelayoutBoundaryEnabled()).isFalse()
        assertThat(doc.isFlatMeasurePassEnabled()).isFalse()

        doc.setOptimizationLevel(CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES)
        assertThat(doc.getOptimizationLevel())
            .isEqualTo(CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES)
        assertThat(doc.isMeasureCacheEnabled()).isFalse()
        assertThat(doc.isRelayoutBoundaryEnabled()).isTrue()
        assertThat(doc.isFlatMeasurePassEnabled()).isFalse()

        doc.setOptimizationLevel(CoreDocument.OPTIMIZATION_FLAT_MEASURE_PASS)
        assertThat(doc.getOptimizationLevel())
            .isEqualTo(CoreDocument.OPTIMIZATION_FLAT_MEASURE_PASS)
        assertThat(doc.isMeasureCacheEnabled()).isFalse()
        assertThat(doc.isRelayoutBoundaryEnabled()).isFalse()
        assertThat(doc.isFlatMeasurePassEnabled()).isTrue()

        doc.setOptimizationLevel(CoreDocument.OPTIMIZATION_ALL)
        assertThat(doc.getOptimizationLevel()).isEqualTo(CoreDocument.OPTIMIZATION_ALL)
        assertThat(doc.isMeasureCacheEnabled()).isTrue()
        assertThat(doc.isRelayoutBoundaryEnabled()).isTrue()
        assertThat(doc.isFlatMeasurePassEnabled()).isTrue()
    }

    @Test
    fun testHeaderDisablingAllOptimizations() {
        val buffer = RemoteComposeBuffer()
        val tags =
            shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.FEATURE_OPTIMIZATION_LEVEL)
        val values = arrayOf<Any>(800, 800, CoreDocument.OPTIMIZATION_NONE)
        buffer.addHeader(tags, values)

        val doc = CoreDocument()
        doc.initFromBuffer(buffer)

        assertThat(doc.getOptimizationLevel()).isEqualTo(CoreDocument.OPTIMIZATION_NONE)
        assertThat(doc.isMeasureCacheEnabled).isFalse()
        assertThat(doc.isRelayoutBoundaryEnabled).isFalse()
    }

    @Test
    fun testHeaderEnablingOnlyMeasureCacheOptimization() {
        val buffer = RemoteComposeBuffer()
        val tags =
            shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.FEATURE_OPTIMIZATION_LEVEL)
        val values = arrayOf<Any>(800, 800, CoreDocument.OPTIMIZATION_MEASURE_CACHE)
        buffer.addHeader(tags, values)

        val doc = CoreDocument()
        doc.initFromBuffer(buffer)

        assertThat(doc.getOptimizationLevel()).isEqualTo(CoreDocument.OPTIMIZATION_MEASURE_CACHE)
        assertThat(doc.isMeasureCacheEnabled).isTrue()
        assertThat(doc.isRelayoutBoundaryEnabled).isFalse()
    }

    @Test
    fun testHeaderEnablingOnlyLayoutBoundariesOptimization() {
        val buffer = RemoteComposeBuffer()
        val tags =
            shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.FEATURE_OPTIMIZATION_LEVEL)
        val values = arrayOf<Any>(800, 800, CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES)
        buffer.addHeader(tags, values)

        val doc = CoreDocument()
        doc.initFromBuffer(buffer)

        assertThat(doc.getOptimizationLevel())
            .isEqualTo(CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES)
        assertThat(doc.isMeasureCacheEnabled).isFalse()
        assertThat(doc.isRelayoutBoundaryEnabled).isTrue()
    }

    @Test
    fun testHeaderEnablingAllOptimizations() {
        val buffer = RemoteComposeBuffer()
        val tags =
            shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.FEATURE_OPTIMIZATION_LEVEL)
        val values = arrayOf<Any>(800, 800, CoreDocument.OPTIMIZATION_ALL)
        buffer.addHeader(tags, values)

        val doc = CoreDocument()
        doc.initFromBuffer(buffer)

        assertThat(doc.getOptimizationLevel()).isEqualTo(CoreDocument.OPTIMIZATION_ALL)
        assertThat(doc.isMeasureCacheEnabled).isTrue()
        assertThat(doc.isRelayoutBoundaryEnabled).isTrue()
    }

    @Test
    fun testHeaderDefaultOptimizationLevelWhenAbsent() {
        val buffer = RemoteComposeBuffer()
        val tags = shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT)
        val values = arrayOf<Any>(800, 800)
        buffer.addHeader(tags, values)

        val doc = CoreDocument()
        doc.initFromBuffer(buffer)

        assertThat(doc.getOptimizationLevel()).isEqualTo(CoreDocument.OPTIMIZATION_ALL)
        assertThat(doc.isMeasureCacheEnabled).isTrue()
        assertThat(doc.isRelayoutBoundaryEnabled).isTrue()
    }
}
