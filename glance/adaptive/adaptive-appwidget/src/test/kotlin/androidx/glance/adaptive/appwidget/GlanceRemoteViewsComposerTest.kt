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
import android.os.Bundle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.glance.LocalSize
import androidx.glance.adaptive.appwidget.ui.AppWidgetTemplateRegistry
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.selection.Dimensions
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class GlanceRemoteViewsComposerTest {

    private class TestTemplate : AdaptiveGlanceTemplate

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val component = ComponentName(context.packageName, "TestReceiver")
    private lateinit var composer: GlanceRemoteViewsComposer

    /** Every [HostConstraints] the registry handed the renderer, in composition order. */
    private val recordedConstraints = mutableListOf<HostConstraints<AppWidgetGlanceSurface>>()
    private val recordedLocalSizes = mutableListOf<DpSize>()

    @Before
    fun setUp() {
        recordedConstraints.clear()
        recordedLocalSizes.clear()
        GlanceRemoteViewsComposer.resetForTesting()
        AppWidgetTemplateRegistry.resetForTesting()
        AppWidgetTemplateRegistry.register(
            TestTemplate::class.java,
            TemplateRenderer { _, constraints ->
                recordedConstraints += constraints
                { recordedLocalSizes += LocalSize.current }
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
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, UNSPECIFIED),
                listOf(401, 403),
                RenderTarget(AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN, UNSPECIFIED),
                listOf(402),
            )
    }

    @Test
    fun groupInstances_emptyOptions_defaultsToHomeScreen() {
        // The platform reports an empty bundle, never null, for a widget whose host has not set
        // any options yet.
        bindWidget(501)

        val groups = groupInstances(501)

        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, UNSPECIFIED),
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
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
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
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, Dimensions(88, 88)),
                listOf(611),
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
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
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
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
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, UNSPECIFIED),
                listOf(631, 632, 633),
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
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
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
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, Dimensions(88, 88)),
                listOf(homePreview(small)),
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
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
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88)),
                listOf(homePreview(first), homePreview(second)),
            )
    }

    @Test
    fun groupPreviews_resizableProvider_usesResizeMinimum() {
        val provider =
            providerInfo(
                "Provider",
                declaredWidthDp = 348,
                declaredHeightDp = 88,
                resizeMode = AppWidgetProviderInfo.RESIZE_HORIZONTAL,
                minResizeWidthDp = 176,
            )

        val groups = composer.groupPreviewsByRenderTarget(listOf(provider))

        // A horizontally resizable widget can be placed at its resize minimum, so that is the
        // size every archetype has to survive.
        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, Dimensions(176, 88)),
                listOf(homePreview(provider)),
            )
    }

    @Test
    fun groupPreviews_withoutDeclaredSize_usesUnspecifiedDimensions() {
        val provider = providerInfo("Provider")

        val groups = composer.groupPreviewsByRenderTarget(listOf(provider))

        assertThat(groups)
            .containsExactly(
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, UNSPECIFIED),
                listOf(homePreview(provider)),
            )
    }

    @Test
    fun groupPreviews_multiCategoryProvider_producesOneTargetPerSurface() {
        // Category-to-surface resolution itself is covered by AppWidgetGlanceSurfaceTest; this
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
                RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, UNSPECIFIED),
                listOf(
                    PreviewTarget(
                        provider.provider,
                        AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                    )
                ),
                RenderTarget(AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN, UNSPECIFIED),
                listOf(
                    PreviewTarget(provider.provider, AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
                ),
            )
    }

    // endregion

    // region compose

    @Test
    fun compose_providesTargetToRendererAndLocalSize() = runTest {
        val target = RenderTarget(AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN, Dimensions(348, 88))

        composer.compose(TestTemplate(), target)

        assertThat(recordedConstraints.map { it.surface })
            .containsExactly(AppWidgetGlanceSurface.MOBILE_LOCK_SCREEN)
        assertThat(recordedConstraints.map { it.dimensions }).containsExactly(Dimensions(348, 88))
        assertThat(recordedLocalSizes).containsExactly(DpSize(Dp(348f), Dp(88f)))
    }

    @Test
    fun compose_unspecifiedDimensions_composesAtUnspecifiedDpSize() = runTest {
        val target = RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, UNSPECIFIED)

        composer.compose(TestTemplate(), target)

        assertThat(recordedConstraints.map { it.dimensions }).containsExactly(UNSPECIFIED)
        assertThat(recordedLocalSizes).containsExactly(DpSize.Unspecified)
    }

    @Test
    fun compose_acrossComposerInstances_preservesAndRotatesLayoutIdOnStructureChange() = runTest {
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
        val compact = RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, Dimensions(88, 88))
        val wide = RenderTarget(AppWidgetGlanceSurface.MOBILE_HOME_SCREEN, Dimensions(348, 88))

        val first = GlanceRemoteViewsComposer(context).compose(TestTemplate(), compact)
        // Re-composing the same structure via a fresh composer instance reuses the same layout ID.
        val second = GlanceRemoteViewsComposer(context).compose(TestTemplate(), compact)
        // A wider tier emits a Row instead of a Box; a fresh composer instance must assign a
        // distinct root layout ID so AppWidgetHostView re-inflates instead of calling reapply()
        // on the previous Box ViewStub.
        val third = GlanceRemoteViewsComposer(context).compose(TestTemplate(), wide)

        assertThat(second.layoutId).isEqualTo(first.layoutId)
        assertThat(third.layoutId).isNotEqualTo(first.layoutId)
    }

    // endregion

    private fun groupInstances(vararg appWidgetIds: Int) =
        composer.groupInstancesByRenderTarget(mapOf(component to appWidgetIds))

    private fun bindWidget(
        appWidgetId: Int,
        hostCategory: Int? = null,
        minWidthDp: Int? = null,
        minHeightDp: Int? = null,
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
