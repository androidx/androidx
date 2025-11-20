/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.remoteviews.test.R
import androidx.core.util.SizeFCompat
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [23])
class AppWidgetManagerCompatTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val packageName = context.packageName
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val appWidgetId = 1
    private val landscapeContext = configurationContext { orientation = ORIENTATION_LANDSCAPE }
    private val portraitContext = configurationContext { orientation = ORIENTATION_PORTRAIT }

    @Test
    fun exact_oneSize_shouldUpdateRemoteViewsWithoutLandPort() {
        setWidgetSize(100 x 200)

        val rv =
            createExactSizeAppWidget(appWidgetManager, appWidgetId) { (width, height) ->
                remoteViews { setTextViewText(R.id.text, "$width x $height") }
            }

        assertThat(rv.landscape).isNull()
        assertThat(rv.portrait).isNull()
    }

    @Test
    fun exact_multipleSizes_shouldUpdateRemoteViewsWithLandPort() {
        setWidgetSize(landscape = 200 x 100, portrait = 110 x 190)

        val rv =
            createExactSizeAppWidget(appWidgetManager, appWidgetId) { (width, height) ->
                remoteViews { setTextViewText(R.id.text, "$width x $height") }
            }

        assertThat(rv.landscape).isNotNull()
        assertThat(rv.portrait).isNotNull()
    }

    @Test
    fun exact_multipleSizes_shouldUseLandscapeSizeForLandscapeView() {
        setWidgetSize(landscape = 200 x 100, portrait = 110 x 190)

        val rv =
            createExactSizeAppWidget(appWidgetManager, appWidgetId) { (width, height) ->
                remoteViews { setTextViewText(R.id.text, "$width x $height") }
            }

        val view = rv.apply(landscapeContext, FrameLayout(landscapeContext))
        assertThat(view.findViewById<TextView>(R.id.text).text.toString())
            .isEqualTo("200.0 x 100.0")
    }

    @Test
    fun exact_multipleSizes_shouldUsePortraitSizeForPortraitView() {
        setWidgetSize(landscape = 200 x 100, portrait = 110 x 190)

        val rv =
            createExactSizeAppWidget(appWidgetManager, appWidgetId) { (width, height) ->
                remoteViews { setTextViewText(R.id.text, "$width x $height") }
            }

        val view = rv.apply(portraitContext, FrameLayout(portraitContext))
        assertThat(view.findViewById<TextView>(R.id.text).text.toString())
            .isEqualTo("110.0 x 190.0")
    }

    @Test
    fun exact_invalidWidget_shouldThrow() {
        assertFailsWith<IllegalArgumentException> {
            createExactSizeAppWidget(appWidgetManager, appWidgetId) { (width, height) ->
                remoteViews { setTextViewText(R.id.text, "$width x $height") }
            }
        }
    }

    @Test
    fun responsive_singleSizeFromLauncher_shouldChooseBestFitAsSingleRemoteViews() {
        setWidgetSize(100 x 200)

        val rv =
            createResponsiveSizeAppWidget(
                appWidgetManager,
                appWidgetId,
                listOf(50 x 50, 75 x 75, 120 x 140),
            ) { (width, height) ->
                remoteViews { setTextViewText(R.id.text, "$width x $height") }
            }

        assertThat(rv.landscape).isNull()
        assertThat(rv.portrait).isNull()
        val view = rv.apply(context, FrameLayout(context))
        assertThat(view.findViewById<TextView>(R.id.text).text.toString()).isEqualTo("75.0 x 75.0")
    }

    @Test
    fun responsive_multipleSizesFromLauncher_bestFitIsSameForBoth_shouldChooseBestFitAsSingle() {
        setWidgetSize(landscape = 200 x 100, portrait = 110 x 190)

        val rv =
            createResponsiveSizeAppWidget(
                appWidgetManager,
                appWidgetId,
                listOf(50 x 50, 75 x 75, 120 x 140),
            ) { (width, height) ->
                remoteViews { setTextViewText(R.id.text, "$width x $height") }
            }

        assertThat(rv.landscape).isNull()
        assertThat(rv.portrait).isNull()
        val view = rv.apply(context, FrameLayout(context))
        assertThat(view.findViewById<TextView>(R.id.text).text.toString()).isEqualTo("75.0 x 75.0")
    }

    @Test
    fun responsive_multipleSizesFromLauncher_bestFitIsDifferent_shouldChooseBestFitsAsLandPort() {
        setWidgetSize(landscape = 200 x 100, portrait = 110 x 190)

        val rv =
            createResponsiveSizeAppWidget(
                appWidgetManager,
                appWidgetId,
                listOf(50 x 50, 75 x 75, 120 x 100),
            ) { (width, height) ->
                remoteViews { setTextViewText(R.id.text, "$width x $height") }
            }

        assertThat(rv.landscape).isNotNull()
        assertThat(rv.portrait).isNotNull()
        val landView = rv.apply(landscapeContext, FrameLayout(landscapeContext))
        assertThat(landView.findViewById<TextView>(R.id.text).text.toString())
            .isEqualTo("120.0 x 100.0")
        val portView = rv.apply(portraitContext, FrameLayout(portraitContext))
        assertThat(portView.findViewById<TextView>(R.id.text).text.toString())
            .isEqualTo("75.0 x 75.0")
    }

    @Test
    fun responsive_noSizes_shouldThrow() {
        setWidgetSize(landscape = 200 x 100, portrait = 110 x 190)

        assertFailsWith<IllegalArgumentException> {
            createResponsiveSizeAppWidget(appWidgetManager, appWidgetId, emptyList()) {
                (width, height) ->
                remoteViews { setTextViewText(R.id.text, "$width x $height") }
            }
        }
    }

    @Test
    fun responsive_tooManySizes_shouldThrow() {
        setWidgetSize(landscape = 200 x 100, portrait = 110 x 190)

        assertFailsWith<IllegalArgumentException> {
            createResponsiveSizeAppWidget(appWidgetManager, appWidgetId, (1..17).map { it x it }) {
                (width, height) ->
                remoteViews { setTextViewText(R.id.text, "$width x $height") }
            }
        }
    }

    @Test
    fun responsive_invalidWidget_shouldThrow() {
        assertFailsWith<IllegalArgumentException> {
            createResponsiveSizeAppWidget(appWidgetManager, appWidgetId, listOf(100 x 100)) {
                (width, height) ->
                remoteViews { setTextViewText(R.id.text, "$width x $height") }
            }
        }
    }

    private fun setWidgetSize(landscape: SizeFCompat, portrait: SizeFCompat? = null) {
        shadowOf(appWidgetManager).addBoundWidget(appWidgetId, AppWidgetProviderInfo())
        appWidgetManager.updateAppWidgetOptions(
            appWidgetId,
            Bundle().apply {
                putInt(OPTION_APPWIDGET_MAX_WIDTH, landscape.width.toInt())
                putInt(OPTION_APPWIDGET_MIN_HEIGHT, landscape.height.toInt())

                putInt(
                    OPTION_APPWIDGET_MIN_WIDTH,
                    portrait?.width?.toInt() ?: landscape.width.toInt(),
                )
                putInt(
                    OPTION_APPWIDGET_MAX_HEIGHT,
                    portrait?.height?.toInt() ?: landscape.height.toInt(),
                )
            },
        )
    }

    private infix fun Int.x(other: Int) = SizeFCompat(this.toFloat(), other.toFloat())

    private fun remoteViews(modifier: RemoteViews.() -> Unit = {}): RemoteViews {
        return RemoteViews(packageName, R.layout.remote_views).apply(modifier)
    }

    private val RemoteViews.landscape: RemoteViews?
        get() = ReflectionHelpers.getField(this, "mLandscape")

    private val RemoteViews.portrait: RemoteViews?
        get() = ReflectionHelpers.getField(this, "mPortrait")

    private fun configurationContext(modifier: Configuration.() -> Unit): Context {
        val configuration = Configuration()
        configuration.apply(modifier)
        return context.createConfigurationContext(configuration)
    }
}
