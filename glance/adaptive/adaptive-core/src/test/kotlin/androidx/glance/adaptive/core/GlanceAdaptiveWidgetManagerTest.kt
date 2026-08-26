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

package androidx.glance.adaptive.core

import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GlanceAdaptiveWidgetManagerTest {

    private class TestTemplate : AdaptiveGlanceTemplate

    private class FakeWidgetDelegate : GlanceAdaptiveWidgetDelegate {
        var lastWidgetName: String? = null
        var lastWidgetIds: Set<String>? = null
        var lastData: AdaptiveGlanceTemplate? = null

        override suspend fun pushUpdate(
            widgetName: String,
            currentData: AdaptiveGlanceTemplate,
            widgetIds: Set<String>?,
        ) {
            lastWidgetName = widgetName
            lastWidgetIds = widgetIds
            lastData = currentData
        }
    }

    @Test
    fun pushUpdate_broadcast_delegatesToDelegate() = runTest {
        val fakeDelegate = FakeWidgetDelegate()
        val manager = GlanceAdaptiveWidgetManager(fakeDelegate)
        val testTemplate = TestTemplate()

        manager.pushUpdate(widgetName = "test_widget", currentData = testTemplate)

        assertThat(fakeDelegate.lastWidgetName).isEqualTo("test_widget")
        assertThat(fakeDelegate.lastWidgetIds).isNull()
        assertThat(fakeDelegate.lastData).isSameInstanceAs(testTemplate)
    }

    @Test
    fun pushUpdate_withWidgetIds_delegatesToDelegate() = runTest {
        val fakeDelegate = FakeWidgetDelegate()
        val manager = GlanceAdaptiveWidgetManager(fakeDelegate)
        val testTemplate = TestTemplate()

        manager.pushUpdate(
            widgetName = "test_widget",
            currentData = testTemplate,
            widgetIds = setOf("widget_123", "widget_456"),
        )

        assertThat(fakeDelegate.lastWidgetName).isEqualTo("test_widget")
        assertThat(fakeDelegate.lastWidgetIds).containsExactly("widget_123", "widget_456")
        assertThat(fakeDelegate.lastData).isSameInstanceAs(testTemplate)
    }

    @Test
    fun pushUpdate_withSingleWidgetId_delegatesToDelegate() = runTest {
        val fakeDelegate = FakeWidgetDelegate()
        val manager = GlanceAdaptiveWidgetManager(fakeDelegate)
        val testTemplate = TestTemplate()

        manager.pushUpdate(
            widgetName = "test_widget",
            currentData = testTemplate,
            widgetId = "widget_123",
        )

        assertThat(fakeDelegate.lastWidgetName).isEqualTo("test_widget")
        assertThat(fakeDelegate.lastWidgetIds).containsExactly("widget_123")
        assertThat(fakeDelegate.lastData).isSameInstanceAs(testTemplate)
    }
}
