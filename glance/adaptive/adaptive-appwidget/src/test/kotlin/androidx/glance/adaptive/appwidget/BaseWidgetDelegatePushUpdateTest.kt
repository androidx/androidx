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
import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.OtherReceiver
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.TestReceiver
import androidx.glance.adaptive.appwidget.BaseWidgetDelegateTestRule.TestTemplate
import androidx.glance.adaptive.appwidget.renderer.RemoteViewsTemplateRenderer
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.GlanceAdaptiveUpdateException
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.selection.ArchetypeSelector
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

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
    fun pushUpdate_whenUpdateFailsForOneTarget_updatesRemainingTargetsThenThrows() = runTest {
        rule.setupBoundWidget(
            401,
            TestReceiver::class.java.name,
            widgetId = "home_widget",
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        rule.setupBoundWidget(
            402,
            TestReceiver::class.java.name,
            widgetId = "lock_widget",
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        // Fail whichever target is delivered first, then behave normally.
        val failure = IllegalStateException("Simulated host failure")
        doThrow(failure)
            .doCallRealMethod()
            .whenever(rule.appWidgetManager)
            .updateAppWidget(any<IntArray>(), any<RemoteViews>())

        val error = pushUpdateFailure()

        // Both targets were attempted.
        val attempts = rule.capturedUpdatedIds()
        assertThat(attempts).containsExactly(listOf(401), listOf(402))
        val failedWidgetId = if (attempts.first() == listOf(401)) "home_widget" else "lock_widget"
        assertThat(error.failedWidgetIds).containsExactly(failedWidgetId)
        assertThat(error.allFailedWidgetIdsKnown).isTrue()
        assertThat(error.cause).isSameInstanceAs(failure)
    }

    @Test
    fun pushUpdate_whenEveryTargetFails_throwsOneExceptionListingEveryWidgetId() = runTest {
        rule.setupBoundWidget(
            411,
            TestReceiver::class.java.name,
            widgetId = "home_widget",
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        rule.setupBoundWidget(
            412,
            TestReceiver::class.java.name,
            widgetId = "lock_widget",
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        doThrow(IllegalStateException("Simulated host failure"))
            .whenever(rule.appWidgetManager)
            .updateAppWidget(any<IntArray>(), any<RemoteViews>())

        val error = pushUpdateFailure()

        assertThat(error.failedWidgetIds).containsExactly("home_widget", "lock_widget")
        assertThat(error.allFailedWidgetIdsKnown).isTrue()
        assertThat(error.suppressed).hasLength(1)
        assertThat(error).hasMessageThat().contains("2 placement(s) of widgetName test_widget")
    }

    @Test
    fun pushUpdate_whenComposeFailsForOneTarget_updatesRemainingTargetsThenThrows() = runTest {
        rule.setupBoundWidget(
            421,
            TestReceiver::class.java.name,
            widgetId = "home_widget",
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        rule.setupBoundWidget(
            422,
            TestReceiver::class.java.name,
            widgetId = "lock_widget",
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        whenever(rule.mockRenderer.invoke(any(), eq(AppWidgetSurface.MOBILE_LOCK_SCREEN)))
            .thenThrow(IllegalStateException("Simulated renderer failure"))

        val error = pushUpdateFailure()

        assertThat(rule.capturedUpdatedIds()).containsExactly(listOf(421))
        assertThat(error.failedWidgetIds).containsExactly("lock_widget")
        assertThat(error).hasMessageThat().contains("${AppWidgetSurface.MOBILE_LOCK_SCREEN}")
        // The size of the failed target is named too.
        assertThat(error).hasMessageThat().containsMatch("\\d+x\\d+dp")
    }

    @Test
    fun pushUpdate_whenTargetedPlacementOptionsCannotBeRead_reportsItAsUnidentified() = runTest {
        WidgetOptionsCache.clear()
        rule.setupBoundWidget(431, TestReceiver::class.java.name, widgetId = "widget_a")
        rule.setupBoundWidget(432, TestReceiver::class.java.name, widgetId = "widget_b")
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        doThrow(RuntimeException("Simulated IPC failure"))
            .whenever(rule.appWidgetManager)
            .getAppWidgetOptions(432)

        val error = pushUpdateFailure(widgetIds = setOf("widget_a", "widget_b"))

        assertThat(rule.capturedUpdatedIds()).containsExactly(listOf(431))
        // Without its options, nothing tells whether the placement was targeted.
        assertThat(error.failedWidgetIds).isEmpty()
        assertThat(error.allFailedWidgetIdsKnown).isFalse()
        assertThat(error).hasMessageThat().contains("1 whose options could not be read")
    }

    @Test
    fun pushUpdate_whenCachedPlacementCannotBeSized_reportsItByCachedWidgetId() = runTest {
        WidgetOptionsCache.clear()
        rule.setupBoundWidget(441, TestReceiver::class.java.name, widgetId = "widget_a")
        rule.setupBoundWidget(442, TestReceiver::class.java.name, widgetId = "widget_b")
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        WidgetOptionsCache.cacheWidgetOptionsIfMissing(
            442,
            Bundle().apply { putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "widget_b") },
        )
        doThrow(RuntimeException("Simulated IPC failure"))
            .whenever(rule.appWidgetManager)
            .getAppWidgetOptions(442)

        val error = pushUpdateFailure()

        assertThat(rule.capturedUpdatedIds()).containsExactly(listOf(441))
        assertThat(error.failedWidgetIds).containsExactly("widget_b")
        assertThat(error.allFailedWidgetIdsKnown).isTrue()
    }

    @Test
    fun pushUpdate_whenListingPlacementsFails_propagatesUnchanged() = runTest {
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        val failure = RuntimeException("Simulated Binder failure")
        doThrow(failure).whenever(rule.appWidgetManager).getAppWidgetIds(any<ComponentName>())

        val error = runCatching {
            spiedDelegate().pushUpdate(widgetName = "test_widget", currentData = TestTemplate())
        }
            .exceptionOrNull()

        // Coroutine stack trace recovery may copy the exception, so compare type and message.
        assertThat(error).isInstanceOf(failure.javaClass)
        assertThat(error).hasMessageThat().isEqualTo(failure.message)
    }

    @Test
    fun pushUpdate_withBlankWidgetId_throwsBeforeAnyIpc() = runTest {
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        val error = runCatching {
            spiedDelegate()
                .pushUpdate(
                    widgetName = "test_widget",
                    currentData = TestTemplate(),
                    widgetIds = setOf("widget_a", " "),
                )
        }
            .exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        verify(rule.appWidgetManager, never()).getAppWidgetIds(any<ComponentName>())
    }

    @Test
    fun pushUpdate_whenCancelled_rethrowsCancellationWithoutWrapping() = runTest {
        rule.setupBoundWidget(451, TestReceiver::class.java.name, widgetId = "widget_a")
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        doThrow(CancellationException("Simulated cancellation"))
            .whenever(rule.appWidgetManager)
            .updateAppWidget(any<IntArray>(), any<RemoteViews>())

        val error = runCatching {
            rule.delegate().pushUpdate(widgetName = "test_widget", currentData = TestTemplate())
        }
            .exceptionOrNull()

        assertThat(error).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun pushUpdate_whenOnePlacementOfSharedWidgetIdFails_reportsThatWidgetId() = runTest {
        rule.setupBoundWidget(
            461,
            TestReceiver::class.java.name,
            widgetId = "shared_widget",
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        rule.setupBoundWidget(
            462,
            TestReceiver::class.java.name,
            widgetId = "shared_widget",
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        // Fail whichever target is delivered first, then behave normally.
        doThrow(IllegalStateException("Simulated host failure"))
            .doCallRealMethod()
            .whenever(rule.appWidgetManager)
            .updateAppWidget(any<IntArray>(), any<RemoteViews>())

        val error = pushUpdateFailure()

        // Both targets were attempted, and one placement of the widgetId missed the update.
        assertThat(rule.capturedUpdatedIds()).hasSize(2)
        assertThat(error.failedWidgetIds).containsExactly("shared_widget")
        assertThat(error.allFailedWidgetIdsKnown).isTrue()
    }

    @Test
    fun pushUpdate_whenCancelledAfterAFailure_throwsCancellationAndSkipsRemainingTargets() =
        runTest {
            rule.setupBoundWidget(
                471,
                TestReceiver::class.java.name,
                widgetId = "home_widget",
                hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            )
            rule.setupBoundWidget(
                472,
                TestReceiver::class.java.name,
                widgetId = "lock_widget",
                hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
            )
            rule.registerReceiverInManifest(TestReceiver::class.java.name)
            lateinit var call: Deferred<Unit>
            // The first target fails, and the call is cancelled while it is handled.
            doAnswer<Unit> {
                    call.cancel()
                    throw IllegalStateException("Simulated host failure")
                }
                .whenever(rule.appWidgetManager)
                .updateAppWidget(any<IntArray>(), any<RemoteViews>())
            call =
                CoroutineScope(Job()).async(start = CoroutineStart.LAZY) {
                    rule
                        .delegate()
                        .pushUpdate(widgetName = "test_widget", currentData = TestTemplate())
                }

            val error = runCatching { call.await() }.exceptionOrNull()

            assertThat(error).isInstanceOf(CancellationException::class.java)
            assertThat(error).isNotInstanceOf(GlanceAdaptiveUpdateException::class.java)
            assertThat(rule.capturedUpdatedIds()).hasSize(1)
        }

    /**
     * A delegate whose repository also uses the spied [rule.appWidgetManager], so that the calls
     * made to resolve placements can be stubbed too.
     */
    private fun spiedDelegate() =
        BaseWidgetDelegate(
            context = rule.context,
            repository = WidgetInstanceRepository(rule.context, rule.appWidgetManager),
            appWidgetManager = rule.appWidgetManager,
        )

    /** Runs [BaseWidgetDelegate.pushUpdate], expecting it to report failed placements. */
    private suspend fun pushUpdateFailure(
        widgetIds: Set<String>? = null
    ): GlanceAdaptiveUpdateException {
        val error = runCatching {
            spiedDelegate()
                .pushUpdate(
                    widgetName = "test_widget",
                    currentData = TestTemplate(),
                    widgetIds = widgetIds,
                )
        }
            .exceptionOrNull()
        assertThat(error).isInstanceOf(GlanceAdaptiveUpdateException::class.java)
        return error as GlanceAdaptiveUpdateException
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
                RemoteViewsTemplateRenderer { ctx, _, archetype, _ ->
                    onRender(archetype)
                    RemoteViews(ctx.packageName, android.R.layout.simple_list_item_1)
                },
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
    fun pushUpdate_whenTelemetryWriteFails_logsWarningAndStillUpdatesRemoteViews() = runTest {
        rule.setupBoundWidget(612, TestReceiver::class.java.name)
        rule.registerReceiverInManifest(TestReceiver::class.java.name)

        doThrow(RuntimeException("Simulated IPC write failure"))
            .whenever(rule.appWidgetManager)
            .updateAppWidgetOptions(eq(612), any())

        val testData = TestTemplate()
        rule.delegate().pushUpdate(widgetName = "test_widget", currentData = testData)

        verify(rule.mockRenderer).invoke(testData, AppWidgetSurface.MOBILE_HOME_SCREEN)
        verify(rule.appWidgetManager).updateAppWidget(eq(intArrayOf(612)), any())
        assertThat(ShadowLog.getLogsForTag("WidgetTelemetryHandler").filter { it.type == Log.WARN })
            .hasSize(1)
    }

    @Test
    fun pushUpdate_afterTelemetryWriteFailed_retriesItAndCachesNoUnstoredWidgetId() = runTest {
        rule.setupBoundWidget(613, TestReceiver::class.java.name)
        rule.registerReceiverInManifest(TestReceiver::class.java.name)
        doThrow(RuntimeException("Simulated IPC write failure"))
            .doCallRealMethod()
            .whenever(rule.appWidgetManager)
            .updateAppWidgetOptions(eq(613), any())

        rule.delegate().pushUpdate(widgetName = "test_widget", currentData = TestTemplate())
        // The widgetId generated for the failed write was never stored, so it is not cached.
        assertThat(WidgetOptionsCache.getCachedWidgetId(613)).isNull()

        rule.delegate().pushUpdate(widgetName = "test_widget", currentData = TestTemplate())

        val storedOptions = rule.appWidgetManager.getAppWidgetOptions(613)
        assertThat(storedOptions.getBoolean(WidgetTelemetryHandler.OPTION_ADAPTIVE_WIDGET_ENABLED))
            .isTrue()
        assertThat(WidgetOptionsCache.getCachedWidgetId(613))
            .isEqualTo(storedOptions.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID))
    }
}
