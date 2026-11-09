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
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.heightDp
import androidx.compose.remote.creation.compose.capture.widthDp
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.ui.geometry.Size
import androidx.glance.adaptive.appwidget.renderer.GlanceAppWidgetProfiles
import androidx.glance.adaptive.appwidget.renderer.RemoteViewsRenderOutput
import androidx.glance.adaptive.appwidget.renderer.remoteComposeRenderer
import androidx.glance.adaptive.appwidget.renderer.remoteViewsRenderer
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class GlanceRemoteViewsComposerTest {

    private class TestTemplate : GlanceAdaptiveTemplate(templateId = 1)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val component = ComponentName(context.packageName, "TestReceiver")
    private lateinit var composer: GlanceRemoteViewsComposer

    /** Every placed surface the registry handed the renderer, in composition order. */
    private val recordedSurfaces = mutableListOf<AppWidgetSurface>()

    @Before
    fun setUp() {
        recordedSurfaces.clear()
        AppWidgetTemplateRegistry.resetForTesting()
        AppWidgetTemplateRegistry.registerDefault(
            TestTemplate::class.java,
            remoteViewsRenderer { ctx, _, surface ->
                recordedSurfaces += surface
                RemoteViews(ctx.packageName, android.R.layout.simple_list_item_1)
            },
        )
        composer = GlanceRemoteViewsComposer(context, appWidgetManager)
    }

    // region groupInstancesByRenderTarget

    @Test
    fun groupInstances_routesSurfacePerHostCategory() {
        bindWidget(401, hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
        bindWidget(402, hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
        bindWidget(403, hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD)

        val groups = groupInstances(401, 402, 403)

        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, UNSPECIFIED),
                listOf(401, 403),
                RenderTarget(AppWidgetSurface.MOBILE_LOCK_SCREEN, UNSPECIFIED),
                listOf(402),
            )
    }

    @Test
    fun groupInstances_withUnreadableOptions_reportsPlacementAndGroupsTheOthers() {
        bindWidget(701)
        bindWidget(702)
        val failingManager = spy(appWidgetManager)
        val failure = RuntimeException("Simulated IPC failure")
        doThrow(failure).whenever(failingManager).getAppWidgetOptions(702)
        val unreadable = mutableListOf<Pair<Int, Exception>>()

        val grouped =
            GlanceRemoteViewsComposer(context, failingManager).groupInstancesByRenderTarget(
                mapOf(component to intArrayOf(701, 702))
            ) { appWidgetId, error ->
                unreadable += appWidgetId to error
            }

        assertThat(grouped.targetToInstances.values.flatten()).containsExactly(701)
        assertThat(unreadable).containsExactly(702 to failure)
    }

    @Test
    fun groupInstances_returnsStoredWidgetIds() {
        bindWidget(711)
        appWidgetManager.updateAppWidgetOptions(
            711,
            Bundle().apply { putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, "stored") },
        )
        bindWidget(712)

        val grouped =
            composer.groupInstancesByRenderTarget(mapOf(component to intArrayOf(711, 712))) { _, e
                ->
                throw e
            }

        assertThat(grouped.intIdsToStringIds[711]).isEqualTo("stored")
        assertThat(grouped.intIdsToStringIds.containsKey(712)).isFalse()
    }

    @Test
    fun groupInstances_emptyOptions_defaultsToHomeScreen() {
        // The platform reports an empty bundle, never null, for a widget whose host has not set
        // any options yet.
        bindWidget(501)

        val groups = groupInstances(501)

        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, UNSPECIFIED),
                listOf(501),
            )
    }

    @Test
    fun groupInstances_usesHostReportedSize() {
        bindWidget(
            601,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            minWidthDp = 348,
            minHeightDp = 88,
        )

        val groups = groupInstances(601)

        // Regression: instances used to be composed without the host-reported size, so every
        // instance resolved to Dimensions(0, 0) and rendered the most compact archetype
        // regardless of how large the host had made it.
        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
                listOf(601),
            )
    }

    @Test
    fun groupInstances_separatesSameSurfaceWithDifferentSizes() {
        bindWidget(
            611,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            minWidthDp = 88,
            minHeightDp = 88,
        )
        bindWidget(
            612,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            minWidthDp = 348,
            minHeightDp = 88,
        )

        val groups = groupInstances(611, 612)

        // Same surface, different sizes: grouping by surface alone would collapse these into one
        // composition and give one of the two instances the other's layout.
        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(88, 88)),
                listOf(611),
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
                listOf(612),
            )
    }

    @Test
    fun groupInstances_coalescesIdenticalSurfaceAndSize() {
        bindWidget(
            621,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            minWidthDp = 348,
            minHeightDp = 88,
        )
        bindWidget(
            622,
            hostCategory = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            minWidthDp = 348,
            minHeightDp = 88,
        )

        val groups = groupInstances(621, 622)

        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
                listOf(621, 622),
            )
    }

    @Test
    fun groupInstances_nonPositiveSize_normalizesToUnspecified() {
        bindWidget(631, minWidthDp = -10, minHeightDp = 0)
        bindWidget(632, minWidthDp = 348, minHeightDp = 0)
        bindWidget(633, minWidthDp = -10, minHeightDp = 88)

        val groups = groupInstances(631, 632, 633)

        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, UNSPECIFIED),
                listOf(631, 632, 633),
            )
    }

    @Test
    fun groupInstances_hostReportsSizes_keepsMinimumDimensionsAndDistinctUsableSizes() {
        // Sizes a Pixel launcher reports for a 4x2 widget: portrait first, then landscape.
        bindWidget(
            641,
            minWidthDp = 373,
            minHeightDp = 108,
            sizes =
                listOf(
                    SizeF(373.7f, 198.9f),
                    SizeF(787.8f, 108.9f),
                    SizeF(787.8f, 108.9f),
                    SizeF(0f, 108.9f),
                ),
        )

        val groups = groupInstances(641)

        assertThat(groups)
            .containsExactly(
                RenderTarget(
                    AppWidgetSurface.MOBILE_HOME_SCREEN,
                    Dimensions(373, 108),
                    sizes = listOf(Dimensions(374, 199), Dimensions(788, 109)),
                ),
                listOf(641),
            )
    }

    @Test
    fun groupInstances_hostReportsMoreSizesThanRemoteViewsAccepts_keepsFirst16() {
        bindWidget(
            651,
            minWidthDp = 101,
            minHeightDp = 100,
            sizes = List(20) { SizeF(101f + it, 100f) },
        )

        val groups = groupInstances(651)

        assertThat(groups.keys.single().sizes)
            .containsExactlyElementsIn(List(16) { Dimensions(101 + it, 100) })
            .inOrder()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun groupInstances_beforeApi31_ignoresHostSizes() {
        bindWidget(
            661,
            minWidthDp = 373,
            minHeightDp = 108,
            sizes = listOf(SizeF(373.7f, 198.9f), SizeF(787.8f, 108.9f)),
        )

        val groups = groupInstances(661)

        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(373, 108)),
                listOf(661),
            )
    }

    // endregion

    // region groupPreviewsByRenderTarget

    @Test
    fun groupPreviews_usesDeclaredProviderSize() {
        val provider = providerInfo("Provider", declaredWidthDp = 348, declaredHeightDp = 88)

        val groups = composer.groupPreviewsByRenderTarget(listOf(provider))

        // Regression: previews used to compose at DpSize.Unspecified regardless of the size the
        // provider declares, so the picker advertised the most compact archetype even for a
        // widget that can never be placed that small.
        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
                listOf(homePreview(provider)),
            )
    }

    @Test
    fun groupPreviews_separatesSameSurfaceWithDifferentDeclaredSizes() {
        val small = providerInfo("Small", declaredWidthDp = 88, declaredHeightDp = 88)
        val wide = providerInfo("Wide", declaredWidthDp = 348, declaredHeightDp = 88)

        val groups = composer.groupPreviewsByRenderTarget(listOf(small, wide))

        // Same surface, different declared sizes: grouping by surface alone would collapse these
        // into one composition and advertise one provider with the other's layout.
        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(88, 88)),
                listOf(homePreview(small)),
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
                listOf(homePreview(wide)),
            )
    }

    @Test
    fun groupPreviews_coalescesProvidersWithIdenticalSurfaceAndSize() {
        val first = providerInfo("First", declaredWidthDp = 348, declaredHeightDp = 88)
        val second = providerInfo("Second", declaredWidthDp = 348, declaredHeightDp = 88)

        val groups = composer.groupPreviewsByRenderTarget(listOf(first, second))

        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
                listOf(homePreview(first), homePreview(second)),
            )
    }

    @Test
    fun groupPreviews_resizableProvider_ignoresResizeMinimum() {
        val provider =
            providerInfo(
                "Provider",
                declaredWidthDp = 348,
                declaredHeightDp = 88,
                resizeMode =
                    AppWidgetProviderInfo.RESIZE_HORIZONTAL or
                        AppWidgetProviderInfo.RESIZE_VERTICAL,
                minResizeWidthDp = 40,
                minResizeHeightDp = 40,
            )

        val groups = composer.groupPreviewsByRenderTarget(listOf(provider))

        // Picker previews render in a slot sized to the widget's initial placement, not the
        // post-placement resize floor.
        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
                listOf(homePreview(provider)),
            )
    }

    @Test
    fun groupPreviews_withoutDeclaredSize_usesUnspecifiedDimensions() {
        val provider = providerInfo("Provider")

        val groups = composer.groupPreviewsByRenderTarget(listOf(provider))

        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, UNSPECIFIED),
                listOf(homePreview(provider)),
            )
    }

    @Test
    fun groupPreviews_multiCategoryProvider_producesOneTargetPerSurface() {
        // Category-to-surface resolution itself is covered by AppWidgetSurfaceTest; this
        // only checks that each resolved picker entry lands in its own RenderTarget.
        val provider =
            providerInfo(
                "Provider",
                widgetCategory =
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
                        AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
            )

        val groups = composer.groupPreviewsByRenderTarget(listOf(provider))

        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, UNSPECIFIED),
                listOf(
                    PreviewTarget(
                        provider.provider,
                        AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                    )
                ),
                RenderTarget(AppWidgetSurface.MOBILE_LOCK_SCREEN, UNSPECIFIED),
                listOf(
                    PreviewTarget(provider.provider, AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
                ),
            )
    }

    // endregion

    // region pinPreviewRenderTarget

    @Test
    fun pinPreviewRenderTarget_targetsHomeScreenAtDeclaredSize() {
        val provider =
            providerInfo(
                "Provider",
                // A pinned widget is placed on the home screen even if it may also go elsewhere.
                widgetCategory =
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
                        AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD,
                declaredWidthDp = 348,
                declaredHeightDp = 88,
                resizeMode =
                    AppWidgetProviderInfo.RESIZE_HORIZONTAL or
                        AppWidgetProviderInfo.RESIZE_VERTICAL,
                minResizeWidthDp = 40,
                minResizeHeightDp = 40,
            )

        val target = composer.pinPreviewRenderTarget(provider)

        // Sized like a picker preview, at the widget's initial placement size rather than its
        // post-placement resize floor.
        assertThat(target)
            .isEqualTo(RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)))
    }

    // endregion

    // region compose

    @Test
    fun compose_providesTargetToRenderer() = runTest {
        val target = RenderTarget(AppWidgetSurface.MOBILE_LOCK_SCREEN, Dimensions(348, 88))

        composer.compose(TestTemplate(), target)

        assertThat(recordedSurfaces).containsExactly(AppWidgetSurface.MOBILE_LOCK_SCREEN)
        assertThat(recordedSurfaces.map { it.dimensions }).containsExactly(Dimensions(348, 88))
    }

    @Test
    fun compose_unspecifiedDimensions_providesUnspecifiedDimensionsToRenderer() = runTest {
        val target = RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, UNSPECIFIED)

        composer.compose(TestTemplate(), target)

        assertThat(recordedSurfaces.map { it.dimensions }).containsExactly(UNSPECIFIED)
    }

    @Test
    fun compose_homeScreenWithSizes_rendersOncePerSize() = runTest {
        val target =
            RenderTarget(
                AppWidgetSurface.MOBILE_HOME_SCREEN,
                Dimensions(373, 108),
                sizes = listOf(Dimensions(374, 199), Dimensions(788, 109)),
            )

        val result = composer.compose(TestTemplate(), target)

        assertThat(result).isNotNull()
        assertThat(recordedSurfaces.map { it.dimensions })
            .containsExactly(Dimensions(374, 199), Dimensions(788, 109))
            .inOrder()
    }

    @Test
    fun compose_homeScreenWithSizes_returnsSizeMappedRemoteViews() = runTest {
        val renderedRemoteViews = mutableListOf<RemoteViews>()
        AppWidgetTemplateRegistry.registerDefault(
            TestTemplate::class.java,
            remoteViewsRenderer { ctx, _, surface ->
                val layout =
                    if (surface.dimensions.heightDp > 150) {
                        android.R.layout.simple_list_item_2
                    } else {
                        android.R.layout.simple_list_item_1
                    }
                RemoteViews(ctx.packageName, layout).also { renderedRemoteViews += it }
            },
        )
        val target =
            RenderTarget(
                AppWidgetSurface.MOBILE_HOME_SCREEN,
                Dimensions(373, 108),
                sizes = listOf(Dimensions(374, 199), Dimensions(788, 109)),
            )

        val result = composer.compose(TestTemplate(), target)

        // A size-mapped RemoteViews wraps one RemoteViews per size and takes its default layout
        // from the one with the smallest area, here 374x199.
        assertThat(renderedRemoteViews).hasSize(2)
        assertThat(renderedRemoteViews).doesNotContain(result)
        assertThat(result.layoutId).isEqualTo(android.R.layout.simple_list_item_2)
    }

    @Test
    fun compose_lockScreenWithSizes_rendersMinimumDimensionsOnly() = runTest {
        val target =
            RenderTarget(
                AppWidgetSurface.MOBILE_LOCK_SCREEN,
                Dimensions(168, 28),
                sizes = listOf(Dimensions(168, 28), Dimensions(200, 40)),
            )

        composer.compose(TestTemplate(), target)

        assertThat(recordedSurfaces.map { it.dimensions }).containsExactly(Dimensions(168, 28))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun compose_beforeApi31_rendersMinimumDimensionsOnly() = runTest {
        val target =
            RenderTarget(
                AppWidgetSurface.MOBILE_HOME_SCREEN,
                Dimensions(373, 108),
                sizes = listOf(Dimensions(374, 199), Dimensions(788, 109)),
            )

        composer.compose(TestTemplate(), target)

        assertThat(recordedSurfaces.map { it.dimensions }).containsExactly(Dimensions(373, 108))
    }

    @Test
    fun compose_remoteViewsRenderer_invokesRendererDirectly() = runTest {
        val expectedRemoteViews =
            RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        var observedContext: Context? = null
        var observedSurface: AppWidgetSurface? = null

        AppWidgetTemplateRegistry.registerDefault(
            TestTemplate::class.java,
            remoteViewsRenderer { ctx, _, surface ->
                observedContext = ctx
                observedSurface = surface
                expectedRemoteViews
            },
        )
        val target = RenderTarget(AppWidgetSurface.MOBILE_LOCK_SCREEN, Dimensions(348, 88))

        val result = composer.compose(TestTemplate(), target)

        assertThat(result).isSameInstanceAs(expectedRemoteViews)
        assertThat(observedContext).isEqualTo(context.applicationContext)
        assertThat(observedSurface).isEqualTo(target.surface)
        assertThat(observedSurface?.dimensions).isEqualTo(Dimensions(348, 88))
    }

    @Test
    fun compose_remoteViewsConstructor_returnsProvidedRemoteViews() = runTest {
        val expectedRemoteViews =
            RemoteViews(context.packageName, android.R.layout.simple_list_item_2)

        AppWidgetTemplateRegistry.registerDefault(
            TestTemplate::class.java,
            TemplateRenderer { _, _ -> RemoteViewsRenderOutput(expectedRemoteViews) },
        )
        val target = RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(200, 100))

        val result = composer.compose(TestTemplate(), target)

        assertThat(result).isSameInstanceAs(expectedRemoteViews)
    }

    @Ignore("b/580960449")
    @Test
    @Config(sdk = [37], qualifiers = "xhdpi")
    fun compose_remoteComposeRenderer_capturesDocumentIntoRemoteViewsWithPxSize() = runTest {
        var remoteComposableInvoked = false
        var observedSurface: AppWidgetSurface? = null
        var capturedDisplaySize: Size? = null
        var capturedProfile: Profile? = null

        AppWidgetTemplateRegistry.registerDefault(
            TestTemplate::class.java,
            remoteComposeRenderer { _, surface ->
                observedSurface = surface
                {
                    remoteComposableInvoked = true
                    val state = LocalRemoteComposeCreationState.current
                    val density = context.resources.displayMetrics.density
                    capturedDisplaySize =
                        Size(
                            state.creationDisplayInfo.widthDp.value * density,
                            state.creationDisplayInfo.heightDp.value * density,
                        )
                    capturedProfile = state.profile
                    RemoteBox {}
                }
            },
        )
        val target = RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88))

        val result = composer.compose(TestTemplate(), target)

        val density = context.resources.displayMetrics.density
        assertThat(density).isEqualTo(2f)
        assertThat(result).isNotNull()
        assertThat(remoteComposableInvoked).isTrue()
        assertThat(observedSurface).isEqualTo(target.surface)
        assertThat(observedSurface?.dimensions).isEqualTo(Dimensions(348, 88))
        assertThat(capturedDisplaySize).isEqualTo(Size(348 * density, 88 * density))
        assertThat(capturedProfile).isSameInstanceAs(GlanceAppWidgetProfiles.widgetsV7)
        assertThat(capturedProfile?.apiLevel)
            .isEqualTo(GlanceAppWidgetProfiles.WIDGETS_V7_DOC_API_LEVEL)
        assertThat(capturedProfile?.operationsProfiles)
            .isEqualTo(GlanceAppWidgetProfiles.PROFILE_WIDGETS)
    }

    @Ignore("b/580960449")
    @Test
    @Config(sdk = [37], qualifiers = "xhdpi")
    fun compose_remoteComposeRenderer_unspecifiedDimensions_fallsBackToDisplaySize() = runTest {
        var capturedDisplaySize: Size? = null

        AppWidgetTemplateRegistry.registerDefault(
            TestTemplate::class.java,
            remoteComposeRenderer { _, _ ->
                {
                    val displayInfo = LocalRemoteComposeCreationState.current.creationDisplayInfo
                    val density = context.resources.displayMetrics.density
                    capturedDisplaySize =
                        Size(
                            displayInfo.widthDp.value * density,
                            displayInfo.heightDp.value * density,
                        )
                    RemoteBox {}
                }
            },
        )
        val target = RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, UNSPECIFIED)

        val result = composer.compose(TestTemplate(), target)

        val displayMetrics = context.resources.displayMetrics
        assertThat(result).isNotNull()
        assertThat(capturedDisplaySize)
            .isEqualTo(
                Size(displayMetrics.widthPixels.toFloat(), displayMetrics.heightPixels.toFloat())
            )
    }

    @Test(expected = IllegalStateException::class)
    @Config(sdk = [36])
    fun compose_remoteComposePreSdk37_throwsIllegalStateException() = runTest {
        AppWidgetTemplateRegistry.registerDefault(
            TestTemplate::class.java,
            remoteComposeRenderer { _, _ -> {} },
        )
        val target = RenderTarget(AppWidgetSurface.MOBILE_HOME_SCREEN, Dimensions(200, 100))

        composer.compose(TestTemplate(), target)
    }

    // endregion

    private fun groupInstances(vararg appWidgetIds: Int) =
        composer
            .groupInstancesByRenderTarget(mapOf(component to appWidgetIds)) { _, e -> throw e }
            .targetToInstances

    private fun bindWidget(
        appWidgetId: Int,
        hostCategory: Int? = null,
        minWidthDp: Int? = null,
        minHeightDp: Int? = null,
        sizes: List<SizeF>? = null,
    ) {
        shadowOf(appWidgetManager)
            .addBoundWidget(appWidgetId, AppWidgetProviderInfo().apply { provider = component })

        val bundle = Bundle()
        if (hostCategory != null) {
            bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, hostCategory)
        }
        if (minWidthDp != null) {
            bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidthDp)
        }
        if (minHeightDp != null) {
            bundle.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeightDp)
        }
        if (sizes != null) {
            bundle.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, ArrayList(sizes))
        }
        if (!bundle.isEmpty) {
            appWidgetManager.updateAppWidgetOptions(appWidgetId, bundle)
        }
    }

    private fun providerInfo(
        className: String,
        widgetCategory: Int = HOME_SCREEN_ONLY,
        declaredWidthDp: Int = 0,
        declaredHeightDp: Int = 0,
        resizeMode: Int = AppWidgetProviderInfo.RESIZE_NONE,
        minResizeWidthDp: Int = 0,
        minResizeHeightDp: Int = 0,
    ): AppWidgetProviderInfo =
        AppWidgetProviderInfo().apply {
            provider = ComponentName(context.packageName, className)
            this.widgetCategory = widgetCategory
            // AppWidgetProviderInfo reports its declared sizes in px, not dp.
            minWidth = context.dpToPx(declaredWidthDp)
            minHeight = context.dpToPx(declaredHeightDp)
            this.resizeMode = resizeMode
            minResizeWidth = context.dpToPx(minResizeWidthDp)
            minResizeHeight = context.dpToPx(minResizeHeightDp)
        }

    private fun homePreview(info: AppWidgetProviderInfo) =
        PreviewTarget(info.provider, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)

    /** Converts [dp] into the pixel value an [AppWidgetProviderInfo] would report here. */
    private fun Context.dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).roundToInt()

    private companion object {
        val UNSPECIFIED = Dimensions(0, 0)

        /**
         * Declared category that yields exactly one picker entry, keeping size assertions free of
         * the keyguard preview that home screen widgets otherwise opt into by default.
         */
        const val HOME_SCREEN_ONLY =
            AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
                AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD
    }
}
