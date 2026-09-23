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
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.widget.RemoteViews
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.glance.LocalSize
import androidx.glance.adaptive.appwidget.ui.AppWidgetTemplateRegistry
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.selection.HostConstraints
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.text.Text
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class BaseWidgetDelegateTest {

    private class TestTemplate : AdaptiveGlanceTemplate

    private lateinit var mockRenderer: (TestTemplate, GlanceSurface) -> Unit

    /** Every [HostConstraints] the registry handed the renderer, in composition order. */
    private val recordedConstraints = mutableListOf<HostConstraints<AppWidgetGlanceSurface>>()
    private val recordedLocalSizes = mutableListOf<DpSize>()

    /**
     * Declared category that yields exactly one picker entry, keeping size assertions free of the
     * keyguard preview that home screen widgets otherwise opt into by default.
     */
    private val HOME_SCREEN_ONLY =
        AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
            AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD

    @Before
    fun setUp() {
        mockRenderer = mock()
        recordedConstraints.clear()
        recordedLocalSizes.clear()
        GlanceRemoteViewsComposer.resetForTesting()
        AppWidgetTemplateRegistry.resetForTesting()
        AppWidgetTemplateRegistry.register(
            TestTemplate::class.java,
            TemplateRenderer { template, constraints ->
                recordedConstraints += constraints
                {
                    recordedLocalSizes += LocalSize.current
                    mockRenderer(template, constraints.surface)
                }
            },
        )
    }

    @Test
    fun pushUpdate_withoutReceivers_completesWithoutErrors() = runTest {
        val delegate = BaseWidgetDelegate(ApplicationProvider.getApplicationContext())
        delegate.pushUpdate(widgetName = "test_widget", currentData = TestTemplate())
        verify(mockRenderer, never()).invoke(any(), any())
    }

    @Test
    fun pushUpdate_withExplicitEmptyWidgetIds_completesWithoutErrors() = runTest {
        val delegate = BaseWidgetDelegate(ApplicationProvider.getApplicationContext())
        delegate.pushUpdate(
            widgetName = "test_widget",
            currentData = TestTemplate(),
            widgetIds = emptySet(),
        )
        verify(mockRenderer, never()).invoke(any(), any())
    }

    private class TestReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "test_widget"
    }

    private class OtherReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "other_widget"
    }

    @Test
    fun pushUpdate_withWidgetIds_filtersMatchingWidgetIds() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setupBoundWidget(context, 101, TestReceiver::class.java.name, widgetId = "widget_123")
        setupBoundWidget(context, 102, TestReceiver::class.java.name, widgetId = "widget_456")
        setupBoundWidget(context, 103, TestReceiver::class.java.name, widgetId = "widget_789")
        registerReceiverInManifest(context, TestReceiver::class.java.name)

        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()

        // Target widget_123 & widget_456: matches 101 and 102
        delegate.pushUpdate(
            widgetName = "test_widget",
            currentData = testData,
            widgetIds = setOf("widget_123", "widget_456"),
        )
        verify(mockRenderer).invoke(eq(testData), any())
        clearInvocations(mockRenderer)

        // Target widget_unknown: no matching widget
        delegate.pushUpdate(
            widgetName = "test_widget",
            currentData = testData,
            widgetIds = setOf("widget_unknown"),
        )
        verify(mockRenderer, never()).invoke(any(), any())
    }

    @Test
    fun pushUpdate_rendersTemplateAndUpdatesAppWidgetManager() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setupBoundWidget(context, 201, TestReceiver::class.java.name)
        registerReceiverInManifest(context, TestReceiver::class.java.name)

        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()
        delegate.pushUpdate(widgetName = "test_widget", currentData = testData)

        verify(mockRenderer).invoke(eq(testData), any())
    }

    @Test
    fun pushUpdate_withMismatchingReceiver_doesNotRender() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setupBoundWidget(context, 301, OtherReceiver::class.java.name)
        registerReceiverInManifest(context, OtherReceiver::class.java.name)

        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()
        delegate.pushUpdate(widgetName = "test_widget", currentData = testData)
        verify(mockRenderer, never()).invoke(any(), any())
    }

    @Test
    @Config(sdk = [35])
    fun setPreview_sdk35_withMatchingReceiver_rendersAndCallsSetWidgetPreview() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        registerReceiverInManifest(context, TestReceiver::class.java.name)
        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()

        delegate.setPreview(widgetName = "test_widget", previewData = testData)

        verify(mockRenderer).invoke(testData, AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)
        verify(mockRenderer).invoke(testData, AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN)
    }

    @Test
    @Config(sdk = [34])
    fun setPreview_preSdk35_gracefullyNoOpsWithoutCallingSetWidgetPreview() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        registerReceiverInManifest(context, TestReceiver::class.java.name)
        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()

        delegate.setPreview(widgetName = "test_widget", previewData = testData)

        verify(mockRenderer, never()).invoke(any(), any())
    }

    @Test
    @Config(sdk = [35])
    fun setPreview_withoutMatchingReceivers_doesNotRenderOrUpdate() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val delegate = BaseWidgetDelegate(context)

        delegate.setPreview(widgetName = "unregistered_widget", previewData = TestTemplate())

        verify(mockRenderer, never()).invoke(any(), any())
    }

    @Test
    @Config(sdk = [35])
    fun setPreview_providesDeclaredProviderSizeToRenderer() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        registerReceiverInManifest(
            context,
            TestReceiver::class.java.name,
            widgetCategory = HOME_SCREEN_ONLY,
            declaredWidthDp = 348,
            declaredHeightDp = 88,
        )

        BaseWidgetDelegate(context).setPreview("test_widget", TestTemplate())

        // Regression: previews used to compose at DpSize.Unspecified regardless of the size the
        // provider declares, so the picker advertised the most compact archetype even for a
        // widget that can never be placed that small.
        assertThat(recordedConstraints.map { it.dimensions }).containsExactly(Dimensions(348, 88))
        assertThat(recordedLocalSizes).containsExactly(DpSize(Dp(348f), Dp(88f)))
    }

    @Test
    @Config(sdk = [35])
    fun setPreview_composesSeparatelyPerDeclaredProviderSize() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        registerReceiverInManifest(
            context,
            TestReceiver::class.java.name,
            widgetCategory = HOME_SCREEN_ONLY,
            declaredWidthDp = 88,
            declaredHeightDp = 88,
        )
        registerReceiverInManifest(
            context,
            LockScreenReceiver::class.java.name,
            widgetCategory = HOME_SCREEN_ONLY,
            declaredWidthDp = 348,
            declaredHeightDp = 88,
        )

        BaseWidgetDelegate(context).setPreview("test_widget", TestTemplate())

        // Same surface, different declared sizes: grouping by surface alone would collapse these
        // into one composition and advertise one provider with the other's layout.
        assertThat(recordedConstraints.map { it.dimensions })
            .containsExactly(Dimensions(88, 88), Dimensions(348, 88))
        assertThat(recordedLocalSizes)
            .containsExactly(
                DpSize(Dp(88f), Dp(88f)),
                DpSize(Dp(348f), Dp(88f)),
            )
    }

    @Test
    @Config(sdk = [35])
    fun setPreview_resizableProvider_usesResizeMinimum() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        registerReceiverInManifest(
            context,
            TestReceiver::class.java.name,
            widgetCategory = HOME_SCREEN_ONLY,
            declaredWidthDp = 348,
            declaredHeightDp = 88,
            resizeMode = AppWidgetProviderInfo.RESIZE_HORIZONTAL,
            minResizeWidthDp = 176,
        )

        BaseWidgetDelegate(context).setPreview("test_widget", TestTemplate())

        // A horizontally resizable widget can be placed at its resize minimum, so that is the
        // size every archetype has to survive.
        assertThat(recordedConstraints.map { it.dimensions }).containsExactly(Dimensions(176, 88))
        assertThat(recordedLocalSizes).containsExactly(DpSize(Dp(176f), Dp(88f)))
    }

    @Test
    @Config(sdk = [35])
    fun setPreview_withoutDeclaredProviderSize_passesUnspecifiedDpSize() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        registerReceiverInManifest(
            context,
            TestReceiver::class.java.name,
            widgetCategory = HOME_SCREEN_ONLY,
        )

        BaseWidgetDelegate(context).setPreview("test_widget", TestTemplate())

        assertThat(recordedConstraints.map { it.dimensions }).containsExactly(Dimensions(0, 0))
        assertThat(recordedLocalSizes).containsExactly(DpSize.Unspecified)
    }

    @Test
    fun pushUpdate_routesSurfacePerWidgetInstance() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setupBoundWidget(
            context,
            appWidgetId = 401,
            receiverName = TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        setupBoundWidget(
            context,
            appWidgetId = 402,
            receiverName = TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )
        setupBoundWidget(
            context,
            appWidgetId = 403,
            receiverName = TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD,
        )
        registerReceiverInManifest(context, TestReceiver::class.java.name)

        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()
        delegate.pushUpdate(widgetName = "test_widget", currentData = testData)

        verify(mockRenderer).invoke(testData, AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)
        verify(mockRenderer).invoke(testData, AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN)
    }

    private class LockScreenReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "test_widget"
    }

    @Test
    @Config(sdk = [35])
    fun setPreview_sdk35_routesSurfacePerProviderCategory() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        registerReceiverInManifest(
            context,
            TestReceiver::class.java.name,
            widgetCategory =
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
                    AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD,
        )
        registerReceiverInManifest(
            context,
            LockScreenReceiver::class.java.name,
            widgetCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )
        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()

        delegate.setPreview(widgetName = "test_widget", previewData = testData)

        verify(mockRenderer).invoke(testData, AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)
        verify(mockRenderer).invoke(testData, AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN)
    }

    private class MultiCategoryReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "multi_widget"
    }

    @Test
    @Config(sdk = [35])
    fun setPreview_sdk35_withMultiCategoryReceiver_rendersBothSurfaces() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        registerReceiverInManifest(
            context,
            MultiCategoryReceiver::class.java.name,
            widgetCategory =
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
        )
        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()

        delegate.setPreview(widgetName = "multi_widget", previewData = testData)

        verify(mockRenderer).invoke(testData, AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)
        verify(mockRenderer).invoke(testData, AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN)
    }

    private class NotKeyguardReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetName: String = "not_keyguard_widget"
    }

    @Test
    @Config(sdk = [35])
    fun setPreview_sdk35_withNotKeyguardCategoryReceiver_routesToHomeScreen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        registerReceiverInManifest(
            context,
            NotKeyguardReceiver::class.java.name,
            widgetCategory =
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
                    AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD,
        )
        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()

        delegate.setPreview(widgetName = "not_keyguard_widget", previewData = testData)

        verify(mockRenderer).invoke(testData, AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)
        verify(mockRenderer, never()).invoke(any(), eq(AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN))
    }

    @Test
    fun pushUpdate_withoutHostCategory_routesToHomeScreen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setupBoundWidget(
            context,
            appWidgetId = 501,
            receiverName = TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        )
        // Widget 502 has no host category in its options bundle.
        setupBoundWidget(
            context,
            appWidgetId = 502,
            receiverName = TestReceiver::class.java.name,
            hostCategory = null,
        )
        registerReceiverInManifest(context, TestReceiver::class.java.name)

        val delegate = BaseWidgetDelegate(context)
        val testData = TestTemplate()
        delegate.pushUpdate(widgetName = "test_widget", currentData = testData)

        // Both route to HOME_SCREEN
        verify(mockRenderer).invoke(testData, AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)
    }

    @Test
    fun pushUpdate_providesHostReportedSizeToRenderer() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setupBoundWidget(
            context,
            appWidgetId = 601,
            receiverName = TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            minWidthDp = 348,
            minHeightDp = 88,
        )
        registerReceiverInManifest(context, TestReceiver::class.java.name)

        BaseWidgetDelegate(context).pushUpdate("test_widget", TestTemplate())

        // Regression: the delegate used to compose at DpSize.Unspecified without providing
        // LocalContainerDimensions, so every instance resolved to the Dimensions(0, 0) default and
        // rendered the most compact archetype regardless of how large the host had made it.
        assertThat(recordedConstraints.map { it.dimensions }).containsExactly(Dimensions(348, 88))
        assertThat(recordedLocalSizes).containsExactly(DpSize(Dp(348f), Dp(88f)))
    }

    @Test
    fun pushUpdate_composesSeparatelyPerContainerSize() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setupBoundWidget(
            context,
            appWidgetId = 611,
            receiverName = TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            minWidthDp = 88,
            minHeightDp = 88,
        )
        setupBoundWidget(
            context,
            appWidgetId = 612,
            receiverName = TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            minWidthDp = 348,
            minHeightDp = 88,
        )
        registerReceiverInManifest(context, TestReceiver::class.java.name)

        BaseWidgetDelegate(context).pushUpdate("test_widget", TestTemplate())

        // Same surface, different sizes: grouping by surface alone would collapse these into one
        // composition and give one of the two instances the other's layout.
        assertThat(recordedConstraints.map { it.dimensions })
            .containsExactly(Dimensions(88, 88), Dimensions(348, 88))
        assertThat(recordedLocalSizes)
            .containsExactly(
                DpSize(Dp(88f), Dp(88f)),
                DpSize(Dp(348f), Dp(88f)),
            )
    }

    @Test
    fun pushUpdate_coalescesIdenticalSurfaceAndContainerSize() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setupBoundWidget(
            context,
            appWidgetId = 621,
            receiverName = TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            minWidthDp = 348,
            minHeightDp = 88,
        )
        setupBoundWidget(
            context,
            appWidgetId = 622,
            receiverName = TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            minWidthDp = 348,
            minHeightDp = 88,
        )
        registerReceiverInManifest(context, TestReceiver::class.java.name)

        BaseWidgetDelegate(context).pushUpdate("test_widget", TestTemplate())

        assertThat(recordedConstraints.map { it.dimensions }).containsExactly(Dimensions(348, 88))
        assertThat(recordedLocalSizes).containsExactly(DpSize(Dp(348f), Dp(88f)))
    }

    @Test
    fun pushUpdate_missingDimensions_passesUnspecifiedDpSize() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        setupBoundWidget(
            context,
            appWidgetId = 631,
            receiverName = TestReceiver::class.java.name,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            minWidthDp = -10,
            minHeightDp = 0,
        )
        registerReceiverInManifest(context, TestReceiver::class.java.name)

        BaseWidgetDelegate(context).pushUpdate("test_widget", TestTemplate())

        assertThat(recordedConstraints.map { it.dimensions }).containsExactly(Dimensions(0, 0))
        assertThat(recordedLocalSizes).containsExactly(DpSize.Unspecified)
    }

    @Test
    fun pushUpdate_partiallyMissingDimensions_normalizesToZeroDimensionsAndUnspecifiedDpSize() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()

            setupBoundWidget(
                context,
                appWidgetId = 632,
                receiverName = TestReceiver::class.java.name,
                hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                minWidthDp = 348,
                minHeightDp = 0,
            )
            setupBoundWidget(
                context,
                appWidgetId = 633,
                receiverName = TestReceiver::class.java.name,
                hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                minWidthDp = -10,
                minHeightDp = 88,
            )
            registerReceiverInManifest(context, TestReceiver::class.java.name)

            BaseWidgetDelegate(context).pushUpdate("test_widget", TestTemplate())

            assertThat(recordedConstraints.map { it.dimensions }).containsExactly(Dimensions(0, 0))
            assertThat(recordedLocalSizes).containsExactly(DpSize.Unspecified)
        }

    @Test
    fun pushUpdate_acrossDelegateInstances_preservesAndRotatesLayoutIdOnStructureChange() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val appWidgetManager = spy(AppWidgetManager.getInstance(context))

            AppWidgetTemplateRegistry.register(
                TestTemplate::class.java,
                TemplateRenderer { _, constraints ->
                    {
                        if (constraints.dimensions.widthDp >= 200) {
                            Row { Text("wide") }
                        } else {
                            Box { Text("compact") }
                        }
                    }
                },
            )

            setupBoundWidget(
                context,
                appWidgetId = 641,
                receiverName = TestReceiver::class.java.name,
                hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                minWidthDp = 88,
                minHeightDp = 88,
            )
            registerReceiverInManifest(context, TestReceiver::class.java.name)

            val remoteViewsCaptor = argumentCaptor<RemoteViews>()

            BaseWidgetDelegate(
                    context = context,
                    appWidgetManager = appWidgetManager,
                )
                .pushUpdate("test_widget", TestTemplate())

            // Re-updating at the same size via a fresh delegate instance reuses the same layout ID.
            BaseWidgetDelegate(
                    context = context,
                    appWidgetManager = appWidgetManager,
                )
                .pushUpdate("test_widget", TestTemplate())

            // Resizing to a wider tier emits a Row instead of a Box; a fresh delegate instance
            // must assign a distinct root layout ID so AppWidgetHostView re-inflates instead of
            // calling reapply() on the previous Box ViewStub.
            appWidgetManager.updateAppWidgetOptions(
                641,
                Bundle().apply {
                    putInt(
                        AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                        AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                    )
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 348)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 88)
                },
            )
            BaseWidgetDelegate(
                    context = context,
                    appWidgetManager = appWidgetManager,
                )
                .pushUpdate("test_widget", TestTemplate())

            verify(appWidgetManager, times(3))
                .updateAppWidget(any<IntArray>(), remoteViewsCaptor.capture())
            val layoutIds = remoteViewsCaptor.allValues.map { it.layoutId }
            assertThat(layoutIds[1]).isEqualTo(layoutIds[0])
            assertThat(layoutIds[2]).isNotEqualTo(layoutIds[0])
        }

    private fun setupBoundWidget(
        context: Context,
        appWidgetId: Int,
        receiverName: String,
        widgetId: String? = null,
        hostCategory: Int? = null,
        minWidthDp: Int? = null,
        minHeightDp: Int? = null,
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadowManager = shadowOf(appWidgetManager)
        val componentName = ComponentName(context.packageName, receiverName)
        val info = AppWidgetProviderInfo().apply { provider = componentName }
        shadowManager.addBoundWidget(appWidgetId, info)

        val bundle = Bundle()
        if (widgetId != null) {
            bundle.putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, widgetId)
        }
        if (hostCategory != null) {
            bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, hostCategory)
        }
        if (minWidthDp != null) {
            bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidthDp)
        }
        if (minHeightDp != null) {
            bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeightDp)
        }
        if (!bundle.isEmpty) {
            appWidgetManager.updateAppWidgetOptions(appWidgetId, bundle)
        }
    }

    private fun registerReceiverInManifest(
        context: Context,
        receiverName: String,
        widgetCategory: Int = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
        declaredWidthDp: Int = 0,
        declaredHeightDp: Int = 0,
        resizeMode: Int = AppWidgetProviderInfo.RESIZE_NONE,
        minResizeWidthDp: Int = 0,
        minResizeHeightDp: Int = 0,
    ) {
        val componentName = ComponentName(context.packageName, receiverName)
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.addReceiverIfNotPresent(componentName)
        shadowPackageManager.addIntentFilterForReceiver(
            componentName,
            IntentFilter(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
        )
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadowManager = shadowOf(appWidgetManager)
        val info =
            AppWidgetProviderInfo().apply {
                provider = componentName
                this.widgetCategory = widgetCategory
                // AppWidgetProviderInfo reports its declared sizes in px, not dp.
                minWidth = context.dpToPx(declaredWidthDp)
                minHeight = context.dpToPx(declaredHeightDp)
                this.resizeMode = resizeMode
                minResizeWidth = context.dpToPx(minResizeWidthDp)
                minResizeHeight = context.dpToPx(minResizeHeightDp)
            }
        shadowManager.addInstalledProvider(info)
        shadowManager.addInstalledProvidersForProfile(null, info)
    }

    /** Converts [dp] into the pixel value an [AppWidgetProviderInfo] would report here. */
    private fun Context.dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).roundToInt()
}
