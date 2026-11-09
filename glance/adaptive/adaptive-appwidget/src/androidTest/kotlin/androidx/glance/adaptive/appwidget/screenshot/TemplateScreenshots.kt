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

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.FloatRange
import androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput
import androidx.glance.adaptive.appwidget.renderer.RemoteViewsRenderOutput
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import java.util.Locale
import kotlin.math.roundToInt

/*
 * Screenshot harness for adaptive templates drawn with RemoteViews.
 *
 * A test renders a template for one container of the design's density matrix, inflates the
 * RemoteViews into a view laid out at exactly that size, and compares the drawing with its golden
 * in platform/frameworks/support-golden. The harness knows nothing about any one template: a test
 * passes the template and the TemplateRenderer that draws it.
 *
 * Drawings are deterministic: on API 31 and above, colors come from the fixed M3 baseline palette
 * rather than the wallpaper's dynamic colors (see androidTest/res/values-v31), the locale is US
 * English, and the font scale is the default unless a test sets it.
 */

/** The screenshot rule of this module, whose goldens live under its project path. */
internal fun templateScreenshotRule(): AndroidXScreenshotTestRule =
    AndroidXScreenshotTestRule("glance/adaptive/adaptive-appwidget")

/**
 * The containers of the design's density matrix, by tier: the card sizes the Figma matrix draws (W1
 * 88, W2 177, W3 264 and W4 344 dp wide; H1 88, H2 132, H3 176 and H4 264 dp tall), and the Mini
 * layouts of the lock screen.
 *
 * @property widthDp Width of the container.
 * @property heightDp Height of the container.
 * @property surface Surface the container is on.
 */
internal enum class TierSize(
    val widthDp: Int,
    val heightDp: Int,
    val surface: AppWidgetSurface = AppWidgetSurface.MOBILE_HOME_SCREEN,
) {
    W1H1(W1_DP, H1_DP),
    W1H2(W1_DP, H2_DP),
    W1H3(W1_DP, H3_DP),
    W1H4(W1_DP, H4_DP),
    W2H1(W2_DP, H1_DP),
    W2H2(W2_DP, H2_DP),
    W2H3(W2_DP, H3_DP),
    W2H4(W2_DP, H4_DP),
    W3H1(W3_DP, H1_DP),
    W3H2(W3_DP, H2_DP),
    W3H3(W3_DP, H3_DP),
    W3H4(W3_DP, H4_DP),
    W4H1(W4_DP, H1_DP),
    W4H2(W4_DP, H2_DP),
    W4H3(W4_DP, H3_DP),
    W4H4(W4_DP, H4_DP),

    /** The Mini bulb: a round button, on the lock screen. */
    MINI_BULB(56, 56, AppWidgetSurface.MOBILE_LOCK_SCREEN),

    /** The Mini strip: a glyph and a title on one line, on the lock screen. */
    MINI_STRIP(176, 24, AppWidgetSurface.MOBILE_LOCK_SCREEN);

    /** [surface] carrying this container's size. */
    val sizedSurface: AppWidgetSurface
        get() = surface.withDimensions(Dimensions(widthDp, heightDp))
}

private const val W1_DP = 88
private const val W2_DP = 177
private const val W3_DP = 264
private const val W4_DP = 344
private const val H1_DP = 88
private const val H2_DP = 132
private const val H3_DP = 176
private const val H4_DP = 264

/**
 * Draws [template] as [renderer] lays it out for [size], into a bitmap exactly as large as the
 * container.
 *
 * @param template The payload to draw.
 * @param renderer Draws [template]; it must produce RemoteViews.
 * @param size The container, which also gives the surface unless [surface] overrides it.
 * @param surface The surface to draw for, sized as [size].
 * @param nightMode Whether to draw with the dark theme's colors.
 * @param fontScale The font scale to draw text at.
 * @return The drawing.
 */
internal fun <T : GlanceAdaptiveTemplate> renderTemplate(
    template: T,
    renderer: TemplateRenderer<T, AppWidgetSurface, AppWidgetRenderOutput>,
    size: TierSize,
    surface: AppWidgetSurface = size.surface,
    nightMode: Boolean = false,
    fontScale: Float = 1f,
): Bitmap {
    val context = screenshotContext(nightMode, fontScale)
    val sized = surface.withDimensions(Dimensions(size.widthDp, size.heightDp))
    val output = renderer.render(template, sized)
    val remoteViews =
        (output as? RemoteViewsRenderOutput)?.provider?.invoke(context)
            ?: error("The renderer drew $output, which is not RemoteViews.")
    val view = remoteViews.apply(context, FrameLayout(context))
    val density = context.resources.displayMetrics.density
    return drawAt(
        view,
        (size.widthDp * density).roundToInt(),
        (size.heightDp * density).roundToInt(),
    )
}

/**
 * Draws [template] as [renderer] lays it out for [size] and compares the drawing with the golden
 * named [golden].
 *
 * @param golden The golden's name, unique in this module, such as `dashboard_smarthome_w4h4`.
 * @param threshold The least MSSIM similarity that matches the golden.
 * @see renderTemplate for the other parameters.
 */
internal fun <T : GlanceAdaptiveTemplate> AndroidXScreenshotTestRule.assertTemplateAgainstGolden(
    template: T,
    renderer: TemplateRenderer<T, AppWidgetSurface, AppWidgetRenderOutput>,
    size: TierSize,
    golden: String,
    surface: AppWidgetSurface = size.surface,
    nightMode: Boolean = false,
    fontScale: Float = 1f,
    @FloatRange(from = 0.0, to = 1.0) threshold: Double = 0.98,
) {
    val bitmap = renderTemplate(template, renderer, size, surface, nightMode, fontScale)
    assertBitmapAgainstGolden(bitmap, golden, MSSIMMatcher(threshold))
}

/**
 * The test app's context, with the US English locale, the given [fontScale], and the dark or light
 * theme by [nightMode].
 */
internal fun screenshotContext(nightMode: Boolean, fontScale: Float): Context {
    val base = InstrumentationRegistry.getInstrumentation().targetContext
    val config =
        Configuration(base.resources.configuration).apply {
            uiMode =
                (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (nightMode) {
                        Configuration.UI_MODE_NIGHT_YES
                    } else {
                        Configuration.UI_MODE_NIGHT_NO
                    }
            this.fontScale = fontScale
            setLocale(Locale.US)
        }
    return base.createConfigurationContext(config)
}

/** Measures and lays [view] out at exactly [widthPx] by [heightPx], and draws it into a bitmap. */
private fun drawAt(view: View, widthPx: Int, heightPx: Int): Bitmap {
    view.measure(
        View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
    )
    view.layout(0, 0, widthPx, heightPx)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    return bitmap
}
