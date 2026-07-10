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
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure
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
        try {
            CoreDocument.setRelayoutBoundaryEnabled(false)
            assertThat(CoreDocument.isRelayoutBoundaryEnabled()).isFalse()

            val root = RootLayoutComponent(-2, 0f, 0f, 800f, 800f, null)
            val boundary = TestColumnLayout(root, 100)
            root.mList.add(boundary)

            val child = TestChildComponent(boundary, 101)
            boundary.addChild(child)

            // When relayout boundaries are disabled, child.invalidateMeasure() must mark root dirty
            child.invalidateMeasure()

            assertThat(root.mNeedsMeasure).isTrue()
        } finally {
            CoreDocument.setRelayoutBoundaryEnabled(true)
            assertThat(CoreDocument.isRelayoutBoundaryEnabled()).isTrue()
        }
    }

    @Test
    fun testMeasureCacheAndVersionControlViaCoreDocument() {
        try {
            CoreDocument.setMeasureCacheEnabled(false)
            assertThat(CoreDocument.isMeasureCacheEnabled()).isFalse()

            val measure = ComponentMeasure(1, 0f, 0f, 100f, 100f)
            measure.setCachedConstraints(0f, 800f, 0f, 800f)
            assertThat(measure.hasCachedConstraints(0f, 800f, 0f, 800f)).isFalse()

            val doc = CoreDocument()
            doc.setMeasureVersion(1)
            assertThat(doc.getMeasureVersion()).isEqualTo(1)
        } finally {
            CoreDocument.setMeasureCacheEnabled(true)
            assertThat(CoreDocument.isMeasureCacheEnabled()).isTrue()
        }
    }

    @Test
    fun testOptimizationLevelBitmaskViaCoreDocument() {
        try {
            CoreDocument.setOptimizationLevel(CoreDocument.OPTIMIZATION_NONE)
            assertThat(CoreDocument.getOptimizationLevel())
                .isEqualTo(CoreDocument.OPTIMIZATION_NONE)
            assertThat(CoreDocument.isMeasureCacheEnabled()).isFalse()
            assertThat(CoreDocument.isRelayoutBoundaryEnabled()).isFalse()

            CoreDocument.setOptimizationLevel(CoreDocument.OPTIMIZATION_MEASURE_CACHE)
            assertThat(CoreDocument.getOptimizationLevel())
                .isEqualTo(CoreDocument.OPTIMIZATION_MEASURE_CACHE)
            assertThat(CoreDocument.isMeasureCacheEnabled()).isTrue()
            assertThat(CoreDocument.isRelayoutBoundaryEnabled()).isFalse()

            CoreDocument.setOptimizationLevel(CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES)
            assertThat(CoreDocument.getOptimizationLevel())
                .isEqualTo(CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES)
            assertThat(CoreDocument.isMeasureCacheEnabled()).isFalse()
            assertThat(CoreDocument.isRelayoutBoundaryEnabled()).isTrue()

            CoreDocument.setOptimizationLevel(CoreDocument.OPTIMIZATION_ALL)
            assertThat(CoreDocument.getOptimizationLevel()).isEqualTo(CoreDocument.OPTIMIZATION_ALL)
            assertThat(CoreDocument.isMeasureCacheEnabled()).isTrue()
            assertThat(CoreDocument.isRelayoutBoundaryEnabled()).isTrue()
        } finally {
            CoreDocument.setOptimizationLevel(CoreDocument.OPTIMIZATION_ALL)
        }
    }

    @Test
    fun testHeaderDisablingAllOptimizations() {
        try {
            val buffer = RemoteComposeBuffer()
            val tags =
                shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.FEATURE_OPTIMIZATION_LEVEL)
            val values = arrayOf<Any>(800, 800, CoreDocument.OPTIMIZATION_NONE)
            buffer.addHeader(tags, values)

            val doc = CoreDocument()
            doc.initFromBuffer(buffer)

            assertThat(CoreDocument.getOptimizationLevel())
                .isEqualTo(CoreDocument.OPTIMIZATION_NONE)
            assertThat(CoreDocument.isMeasureCacheEnabled()).isFalse()
            assertThat(CoreDocument.isRelayoutBoundaryEnabled()).isFalse()
        } finally {
            CoreDocument.setOptimizationLevel(CoreDocument.OPTIMIZATION_ALL)
        }
    }

    @Test
    fun testHeaderEnablingOnlyMeasureCacheOptimization() {
        try {
            val buffer = RemoteComposeBuffer()
            val tags =
                shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.FEATURE_OPTIMIZATION_LEVEL)
            val values = arrayOf<Any>(800, 800, CoreDocument.OPTIMIZATION_MEASURE_CACHE)
            buffer.addHeader(tags, values)

            val doc = CoreDocument()
            doc.initFromBuffer(buffer)

            assertThat(CoreDocument.getOptimizationLevel())
                .isEqualTo(CoreDocument.OPTIMIZATION_MEASURE_CACHE)
            assertThat(CoreDocument.isMeasureCacheEnabled()).isTrue()
            assertThat(CoreDocument.isRelayoutBoundaryEnabled()).isFalse()
        } finally {
            CoreDocument.setOptimizationLevel(CoreDocument.OPTIMIZATION_ALL)
        }
    }

    @Test
    fun testHeaderEnablingOnlyLayoutBoundariesOptimization() {
        try {
            val buffer = RemoteComposeBuffer()
            val tags =
                shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.FEATURE_OPTIMIZATION_LEVEL)
            val values = arrayOf<Any>(800, 800, CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES)
            buffer.addHeader(tags, values)

            val doc = CoreDocument()
            doc.initFromBuffer(buffer)

            assertThat(CoreDocument.getOptimizationLevel())
                .isEqualTo(CoreDocument.OPTIMIZATION_LAYOUT_BOUNDARIES)
            assertThat(CoreDocument.isMeasureCacheEnabled()).isFalse()
            assertThat(CoreDocument.isRelayoutBoundaryEnabled()).isTrue()
        } finally {
            CoreDocument.setOptimizationLevel(CoreDocument.OPTIMIZATION_ALL)
        }
    }

    @Test
    fun testHeaderEnablingAllOptimizations() {
        try {
            // First set to NONE so we verify setOptimizationLevel(OPTIMIZATION_ALL) in header
            // changes it back
            CoreDocument.setOptimizationLevel(CoreDocument.OPTIMIZATION_NONE)

            val buffer = RemoteComposeBuffer()
            val tags =
                shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT, Header.FEATURE_OPTIMIZATION_LEVEL)
            val values = arrayOf<Any>(800, 800, CoreDocument.OPTIMIZATION_ALL)
            buffer.addHeader(tags, values)

            val doc = CoreDocument()
            doc.initFromBuffer(buffer)

            assertThat(CoreDocument.getOptimizationLevel()).isEqualTo(CoreDocument.OPTIMIZATION_ALL)
            assertThat(CoreDocument.isMeasureCacheEnabled()).isTrue()
            assertThat(CoreDocument.isRelayoutBoundaryEnabled()).isTrue()
        } finally {
            CoreDocument.setOptimizationLevel(CoreDocument.OPTIMIZATION_ALL)
        }
    }

    @Test
    fun testHeaderDefaultOptimizationLevelWhenAbsent() {
        try {
            // Set to NONE first to verify default header behavior restores OPTIMIZATION_ALL
            CoreDocument.setOptimizationLevel(CoreDocument.OPTIMIZATION_NONE)

            val buffer = RemoteComposeBuffer()
            val tags = shortArrayOf(Header.DOC_WIDTH, Header.DOC_HEIGHT)
            val values = arrayOf<Any>(800, 800)
            buffer.addHeader(tags, values)

            val doc = CoreDocument()
            doc.initFromBuffer(buffer)

            assertThat(CoreDocument.getOptimizationLevel()).isEqualTo(CoreDocument.OPTIMIZATION_ALL)
            assertThat(CoreDocument.isMeasureCacheEnabled()).isTrue()
            assertThat(CoreDocument.isRelayoutBoundaryEnabled()).isTrue()
        } finally {
            CoreDocument.setOptimizationLevel(CoreDocument.OPTIMIZATION_ALL)
        }
    }
}
