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

package androidx.glance.adaptive.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import android.widget.RemoteViews
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.OtherReceiver
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.TestReceiver
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.TestTemplate
import androidx.glance.adaptive.appwidget.renderer.GlanceTemplateRenderer
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.selection.ArchetypeSelector
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests [BaseWidgetDelegate.pushUpdate]. */
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class BaseWidgetDelegatePushUpdateTest {

    @get:Rule val rule = BaseWidgetDelegateTestRule()

    @Test
    fun pushUpdate_withoutReceivers_throwsAndDoesNotComposeOrUpdate() = runTest {
        val error = runCatching {
            rule.delegate().pushUpdate(widgetName = "test_widget", currentData = TestTemplate())
        }
            .exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        verify(rule.mockRenderer, never()).invoke(any(), any())
        verify(rule.appWidgetManager, never()).updateAppWidget(any<IntArray>(), any<RemoteViews>())
    }

    @Test
    fun pushUpdate_withExplicitEmptyWidgetIds_doesNotComposeOrUpdate() = runTest {
        rule.setupBoundWidget(201, TestReceiver::class.java.name)
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        rule
            .delegate()
            .pushUpdate(
                widgetName = "test_widget",
                currentData = TestTemplate(),
                widgetIds = emptySet(),
            )

        verify(rule.mockRenderer, never()).invoke(any(), any())
        verify(rule.appWidgetManager, never()).updateAppWidget(any<IntArray>(), any<RemoteViews>())
    }

    @Test
    fun pushUpdate_withMismatchingReceiver_throwsAndDoesNotComposeOrUpdate() = runTest {
        rule.setupBoundWidget(301, OtherReceiver::class.java.name)
        rule.registerReceiverInManifest(OtherReceiver::class.java.name)

        val error = runCatching {
            rule.delegate().pushUpdate(widgetName = "test_widget", currentData = TestTemplate())
        }
            .exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        verify(rule.mockRenderer, never()).invoke(any(), any())
        verify(rule.appWidgetManager, never()).updateAppWidget(any<IntArray>(), any<RemoteViews>())
    }

    @Test
    fun pushUpdate_rendersTemplateAndUpdatesAppWidgetManager() = runTest {
        rule.setupBoundWidget(201, TestReceiver::class.java.name)
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        val testData = TestTemplate()

        rule.delegate().pushUpdate(widgetName = "test_widget", currentData = testData)

        verify(rule.mockRenderer).invoke(eq(testData), any())
        assertThat(rule.capturedUpdatedIds()).containsExactly(listOf(201))
    }

    @Test
    fun pushUpdate_withWidgetIds_updatesOnlyMatchingWidgets() = runTest {
        rule.setupBoundWidget(101, TestReceiver::class.java.name, widgetId = "widget_123")
        rule.setupBoundWidget(102, TestReceiver::class.java.name, widgetId = "widget_456")
        rule.setupBoundWidget(103, TestReceiver::class.java.name, widgetId = "widget_789")
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        rule
            .delegate()
            .pushUpdate(
                widgetName = "test_widget",
                currentData = TestTemplate(),
                widgetIds = setOf("widget_123", "widget_456"),
            )

        assertThat(rule.capturedUpdatedIds().flatten()).containsExactly(101, 102)
    }

    @Test
    fun pushUpdate_withUnknownWidgetIds_doesNotComposeOrUpdate() = runTest {
        rule.setupBoundWidget(101, TestReceiver::class.java.name, widgetId = "widget_123")
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        rule
            .delegate()
            .pushUpdate(
                widgetName = "test_widget",
                currentData = TestTemplate(),
                widgetIds = setOf("widget_unknown"),
            )

        verify(rule.mockRenderer, never()).invoke(any(), any())
        verify(rule.appWidgetManager, never()).updateAppWidget(any<IntArray>(), any<RemoteViews>())
    }

    @Test
    fun pushUpdate_updatesEachRenderTargetWithItsOwnWidgets() = runTest {
        rule.setupBoundWidget(
            401,
            TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        rule.setupBoundWidget(
            402,
            TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        rule.delegate().pushUpdate(widgetName = "test_widget", currentData = TestTemplate())

        // Different surfaces compose separately, and each result goes only to its own instances.
        assertThat(rule.capturedUpdatedIds()).containsExactly(listOf(401), listOf(402))
    }

    @Test
    fun pushUpdate_whenUpdateFailsForOneTarget_stillUpdatesRemainingTargets() = runTest {
        rule.setupBoundWidget(
            401,
            TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        rule.setupBoundWidget(
            402,
            TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        // Fail whichever target is delivered first, then behave normally.
        doThrow(IllegalStateException("Simulated host failure"))
            .doCallRealMethod()
            .whenever(rule.appWidgetManager)
            .updateAppWidget(any<IntArray>(), any<RemoteViews>())

        rule.delegate().pushUpdate(widgetName = "test_widget", currentData = TestTemplate())

        assertThat(rule.capturedUpdatedIds()).containsExactly(listOf(401), listOf(402))
    }

    private enum class TestArchetype {
        COMPACT_HERO,
        SPLIT_ROW,
    }

    private class DimensionAwareTemplate : GlanceAdaptiveTemplate(templateId = 2)

    /**
     * Registers [DimensionAwareTemplate] with a width-based selector, reporting each rendered
     * archetype to [onRender].
     */
    private fun registerDimensionAwareTemplate(onRender: (TestArchetype) -> Unit) {
        val selector =
            ArchetypeSelector<DimensionAwareTemplate, AppWidgetSurface, TestArchetype> { _, surface
                ->
                if (surface.dimensions.widthDp >= 250) TestArchetype.SPLIT_ROW
                else TestArchetype.COMPACT_HERO
            }
        AppWidgetTemplateRegistry.registerDefault(
            DimensionAwareTemplate::class.java,
            TemplateRenderer(
                selector,
                GlanceTemplateRenderer { _, archetype, _ -> { onRender(archetype) } },
            ),
        )
    }

    @Test
    fun pushUpdate_writesTelemetryOptionsBeforeUpdatingRemoteViews() = runTest {
        rule.setupBoundWidget(601, TestReceiver::class.java.name)
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        rule
            .delegate()
            .pushUpdate(widgetName = "test_widget", currentData = TestTemplate(templateId = 2))

        // Written first so that a host reading the extras on update sees current values.
        val inOrder = inOrder(rule.appWidgetManager)
        inOrder.verify(rule.appWidgetManager).updateAppWidgetOptions(eq(601), any())
        inOrder
            .verify(rule.appWidgetManager)
            .updateAppWidget(eq(intArrayOf(601)), any<RemoteViews>())
        assertThat(
                rule.appWidgetManager
                    .getAppWidgetOptions(601)
                    .getInt(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_TEMPLATE_TYPE)
            )
            .isEqualTo(2)
    }

    @Test
    fun pushUpdate_rendersMatchingArchetypeFromDimensions_andWritesTemplateType() = runTest {
        var renderedArchetype: TestArchetype? = null
        registerDimensionAwareTemplate { renderedArchetype = it }
        rule.setupBoundWidget(
            appWidgetId = 606,
            receiverName = TestReceiver::class.java.name,
            extraOptions =
                Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120)
                },
        )
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        rule
            .delegate()
            .pushUpdate(widgetName = "test_widget", currentData = DimensionAwareTemplate())

        assertThat(
                rule.appWidgetManager
                    .getAppWidgetOptions(606)
                    .getInt(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_TEMPLATE_TYPE)
            )
            .isEqualTo(2)
        assertThat(renderedArchetype).isEqualTo(TestArchetype.SPLIT_ROW)
    }

    @Test
    fun pushUpdate_widthOnly_rendersUnspecifiedArchetype_andWritesTemplateType() = runTest {
        var renderedArchetype: TestArchetype? = null
        registerDimensionAwareTemplate { renderedArchetype = it }
        // The composer renders a width without a height at unspecified (0, 0) dimensions.
        rule.setupBoundWidget(
            appWidgetId = 608,
            receiverName = TestReceiver::class.java.name,
            extraOptions =
                Bundle().apply { putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280) },
        )
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        rule
            .delegate()
            .pushUpdate(widgetName = "test_widget", currentData = DimensionAwareTemplate())

        assertThat(renderedArchetype).isEqualTo(TestArchetype.COMPACT_HERO)
        assertThat(
                rule.appWidgetManager
                    .getAppWidgetOptions(608)
                    .getInt(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_TEMPLATE_TYPE)
            )
            .isEqualTo(2)
    }

    @Test
    fun pushUpdate_whenUpdateAppWidgetOptionsThrows_stillUpdatesRemoteViews() = runTest {
        rule.setupBoundWidget(612, TestReceiver::class.java.name)
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        doThrow(RuntimeException("Simulated IPC write failure"))
            .whenever(rule.appWidgetManager)
            .updateAppWidgetOptions(eq(612), any())

        val testData = TestTemplate()
        rule.delegate().pushUpdate(widgetName = "test_widget", currentData = testData)

        verify(rule.mockRenderer).invoke(testData, AppWidgetSurface.MOBILE_HOME_SCREEN)
        verify(rule.appWidgetManager).updateAppWidget(eq(intArrayOf(612)), any())
    }
}
