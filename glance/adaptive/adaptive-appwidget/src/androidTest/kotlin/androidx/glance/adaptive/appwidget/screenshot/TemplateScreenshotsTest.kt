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

package androidx.glance.adaptive.appwidget.screenshot

import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.glance.adaptive.appwidget.R
import androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput
import androidx.glance.adaptive.appwidget.renderer.RemoteViewsRenderOutput
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TemplateScreenshotsTest {

    private val template = object : GlanceAdaptiveTemplate() {}

    @Test
    fun renderTemplate_drawsExactlyTheContainer() {
        var drawnFor: AppWidgetSurface? = null
        val renderer =
            TemplateRenderer<GlanceAdaptiveTemplate, AppWidgetSurface, AppWidgetRenderOutput> {
                _,
                surface ->
                drawnFor = surface
                RemoteViewsRenderOutput { context ->
                    RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
                }
            }

        val bitmap = renderTemplate(template, renderer, TierSize.W4H3)

        val density = screenshotContext(nightMode = false, fontScale = 1f).resources.displayMetrics
        assertThat(bitmap.width).isEqualTo((344 * density.density).roundToInt())
        assertThat(bitmap.height).isEqualTo((176 * density.density).roundToInt())
        assertThat(drawnFor).isEqualTo(AppWidgetSurface.MOBILE_HOME_SCREEN)
        assertThat(drawnFor!!.dimensions.widthDp).isEqualTo(344)
        assertThat(drawnFor!!.dimensions.heightDp).isEqualTo(176)
    }

    @Test
    fun tierSizes_matchTheDesignsMatrix() {
        assertThat(TierSize.W1H1.widthDp).isEqualTo(88)
        assertThat(TierSize.W2H2.widthDp).isEqualTo(177)
        assertThat(TierSize.W3H3.heightDp).isEqualTo(176)
        assertThat(TierSize.W4H4.widthDp).isEqualTo(344)
        assertThat(TierSize.W4H4.heightDp).isEqualTo(264)
        assertThat(TierSize.MINI_BULB.surface).isEqualTo(AppWidgetSurface.MOBILE_LOCK_SCREEN)
        assertThat(TierSize.MINI_STRIP.heightDp).isEqualTo(24)
    }

    @Test
    fun screenshotContext_fixesTheLocaleThemeAndFontScale() {
        val dark = screenshotContext(nightMode = true, fontScale = 2f).resources.configuration

        assertThat(dark.uiMode and Configuration.UI_MODE_NIGHT_MASK)
            .isEqualTo(Configuration.UI_MODE_NIGHT_YES)
        assertThat(dark.fontScale).isEqualTo(2f)
        assertThat(dark.locales[0]).isEqualTo(Locale.US)
    }

    @Test
    fun screenshotContext_drawsWithTheBaselinePaletteRatherThanTheWallpapers() {
        val light = screenshotContext(nightMode = false, fontScale = 1f)
        val dark = screenshotContext(nightMode = true, fontScale = 1f)

        // The M3 baseline primary, whatever the device's wallpaper.
        assertThat(light.getColor(R.color.glance_adaptive_appwidget_remoteviews_primary))
            .isEqualTo(0xff6750a4.toInt())
        assertThat(dark.getColor(R.color.glance_adaptive_appwidget_remoteviews_primary))
            .isEqualTo(0xffd0bcff.toInt())
    }
}
