/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.core.os.bundleOf
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.text.Text
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GlanceAppWidgetTest {

    private lateinit var fakeCoroutineScope: TestCoroutineScope
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val displayMetrics = context.resources.displayMetrics

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun createEmptyUi() = fakeCoroutineScope.runBlockingTest {
        val composer = SampleGlanceAppWidget { }

        val rv = composer.composeForSize(
            context,
            1,
            composer.stateDefinition,
            Bundle(),
            DpSize(40.dp, 50.dp),
            LayoutConfiguration.create(context, 1),
        )

        val view = context.applyRemoteViews(rv)
        assertIs<FrameLayout>(view)
        assertThat(view.childCount).isEqualTo(0)
    }

    @Test
    fun createUiWithSize() = fakeCoroutineScope.runBlockingTest {
        val composer = SampleGlanceAppWidget {
            val size = LocalSize.current
            Text("${size.width} x ${size.height}")
        }

        val rv = composer.composeForSize(
            context,
            1,
            composer.stateDefinition,
            Bundle(),
            DpSize(40.dp, 50.dp),
            LayoutConfiguration.create(context, 1),
        )

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.text).isEqualTo("40.0.dp x 50.0.dp")
    }

    @Test
    fun createUiFromOptionBundle() = fakeCoroutineScope.runBlockingTest {
        val composer = SampleGlanceAppWidget {
            val options = LocalAppWidgetOptions.current

            Text(options.getString("StringKey", "<NOT FOUND>"))
        }

        val bundle = Bundle()
        bundle.putString("StringKey", "FOUND")
        val rv = composer.composeForSize(
            context,
            1,
            composer.stateDefinition,
            bundle,
            DpSize(40.dp, 50.dp),
            LayoutConfiguration.create(context, 1),
        )

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.text).isEqualTo("FOUND")
    }

    @Test
    fun createUiFromGlanceId() = fakeCoroutineScope.runBlockingTest {
        val composer = SampleGlanceAppWidget {
            val glanceId = LocalGlanceId.current

            Text(glanceId.toString())
        }

        val bundle = bundleOf("StringKey" to "FOUND")
        val rv = composer.composeForSize(
            context,
            1,
            composer.stateDefinition,
            bundle,
            DpSize(40.dp, 50.dp),
            LayoutConfiguration.create(context, 1),
        )

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.text).isEqualTo("AppWidgetId(appWidgetId=1)")
    }

    @Test
    fun createUiWithUniqueMode() = fakeCoroutineScope.runBlockingTest {
        val composer = SampleGlanceAppWidget {
            val size = LocalSize.current
            Text("${size.width} x ${size.height}")
        }
        val appWidgetManager = mock<AppWidgetManager> {
            on { getAppWidgetInfo(1) }.thenReturn(
                appWidgetProviderInfo {
                    minWidth = 50
                    minHeight = 50
                    minResizeWidth = 40
                    minResizeHeight = 60
                    resizeMode = AppWidgetProviderInfo.RESIZE_BOTH
                }
            )
        }

        val rv = composer.compose(
            context,
            appWidgetManager,
            appWidgetId = 1,
            composer.stateDefinition,
            options = Bundle(),
            LayoutConfiguration.create(context, 2),
        )

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.text).isEqualTo("40.0.dp x 50.0.dp")
    }

    @Config(sdk = [30])
    @Test
    fun createUiWithExactModePreS() = fakeCoroutineScope.runBlockingTest {
        val composer = SampleGlanceAppWidget(SizeMode.Exact) {
            val size = LocalSize.current
            Text("${size.width} x ${size.height}")
        }
        val options = optionsBundleOf(listOf(DpSize(100.dp, 50.dp), DpSize(50.dp, 100.dp)))
        val appWidgetManager = mock<AppWidgetManager> {
            on { getAppWidgetInfo(1) }.thenThrow(RuntimeException("This should not be called"))
        }
        val rv = composer.compose(
            context,
            appWidgetManager,
            appWidgetId = 1,
            composer.stateDefinition,
            options = options,
            LayoutConfiguration.create(context, 1),
        )

        val portraitView = createPortraitContext().applyRemoteViews(rv)
        assertIs<TextView>(portraitView)
        assertThat(portraitView.text).isEqualTo("50.0.dp x 100.0.dp")

        val landscapeView = createLandscapeContext().applyRemoteViews(rv)
        assertIs<TextView>(landscapeView)
        assertThat(landscapeView.text).isEqualTo("100.0.dp x 50.0.dp")
    }

    @Config(sdk = [30])
    @Test
    fun createUiWithResponsiveModePreS() = fakeCoroutineScope.runBlockingTest {
        val sizes = setOf(
            DpSize(60.dp, 80.dp),
            DpSize(100.dp, 70.dp),
            DpSize(120.dp, 100.dp),
        )
        val composer = SampleGlanceAppWidget(SizeMode.Responsive(sizes)) {
            val size = LocalSize.current
            Text("${size.width} x ${size.height}")
        }
        // Note: Landscape fits the 60x80 and 100x70, portrait doesn't fit anything
        val options =
            optionsBundleOf(listOf(DpSize(125.dp, 90.dp), DpSize(40.0.dp, 120.dp)))
        val appWidgetManager = mock<AppWidgetManager> {
            on { getAppWidgetInfo(1) }.thenThrow(RuntimeException("This should not be called"))
        }
        val rv = composer.compose(
            context,
            appWidgetManager,
            appWidgetId = 1,
            composer.stateDefinition,
            options = options,
            LayoutConfiguration.create(context, 1),
        )

        val portraitView = createPortraitContext().applyRemoteViews(rv)
        assertIs<TextView>(portraitView)
        assertThat(portraitView.text).isEqualTo("60.0.dp x 80.0.dp")

        val landscapeView = createLandscapeContext().applyRemoteViews(rv)
        assertIs<TextView>(landscapeView)
        assertThat(landscapeView.text).isEqualTo("100.0.dp x 70.0.dp")
    }

    @Test
    fun createUiWithExactMode_noSizeFallsBackToUnique() {
        runBlocking {
            val composer = SampleGlanceAppWidget(SizeMode.Exact) {
                val size = LocalSize.current
                Text("${size.width} x ${size.height}")
            }
            val appWidgetManager = mock<AppWidgetManager> {
                on { getAppWidgetInfo(1) }.thenReturn(
                    appWidgetProviderInfo {
                        minWidth = 50
                        minHeight = 50
                        minResizeWidth = 40
                        minResizeHeight = 60
                        resizeMode = AppWidgetProviderInfo.RESIZE_BOTH
                    }
                )
            }
            val rv = composer.compose(
                context,
                appWidgetManager,
                appWidgetId = 1,
                composer.stateDefinition,
                options = Bundle(),
                LayoutConfiguration.create(context, 1),
            )

            val portraitView = createPortraitContext().applyRemoteViews(rv)
            assertIs<TextView>(portraitView)
            assertThat(portraitView.text).isEqualTo("40.0.dp x 50.0.dp")

            val landscapeView = createLandscapeContext().applyRemoteViews(rv)
            assertIs<TextView>(landscapeView)
            assertThat(landscapeView.text).isEqualTo("40.0.dp x 50.0.dp")
        }
    }

    @Config(sdk = [30])
    @Test
    fun createUiWithResponsiveMode_noSizeUseMinSize() = fakeCoroutineScope.runBlockingTest {
        val sizes = setOf(
            DpSize(60.dp, 80.dp),
            DpSize(100.dp, 70.dp),
            DpSize(120.dp, 100.dp),
        )
        val composer = SampleGlanceAppWidget(SizeMode.Responsive(sizes)) {
            val size = LocalSize.current
            Text("${size.width} x ${size.height}")
        }
        val appWidgetManager = mock<AppWidgetManager> {
            on { getAppWidgetInfo(1) }.thenThrow(RuntimeException("This should not be called"))
        }
        val rv = composer.compose(
            context,
            appWidgetManager,
            appWidgetId = 1,
            composer.stateDefinition,
            options = Bundle(),
            LayoutConfiguration.create(context, 1),
        )

        val portraitView = createPortraitContext().applyRemoteViews(rv)
        assertIs<TextView>(portraitView)
        assertThat(portraitView.text).isEqualTo("60.0.dp x 80.0.dp")

        val landscapeView = createLandscapeContext().applyRemoteViews(rv)
        assertIs<TextView>(landscapeView)
        assertThat(landscapeView.text).isEqualTo("60.0.dp x 80.0.dp")
    }

    @Test
    fun appWidgetMinSize_noResizing() {
        val composer = SampleGlanceAppWidget { }
        val appWidgetManager = mock<AppWidgetManager> {
            on { getAppWidgetInfo(1) }.thenReturn(
                appWidgetProviderInfo {
                    minWidth = 50
                    minHeight = 50
                    minResizeWidth = 40
                    minResizeHeight = 30
                    resizeMode = AppWidgetProviderInfo.RESIZE_NONE
                }
            )
        }

        assertThat(composer.appWidgetMinSize(displayMetrics, appWidgetManager, 1))
            .isEqualTo(DpSize(50.dp, 50.dp))
    }

    @Test
    fun appWidgetMinSize_horizontalResizing() {
        val composer = SampleGlanceAppWidget { }
        val appWidgetManager = mock<AppWidgetManager> {
            on { getAppWidgetInfo(1) }.thenReturn(
                appWidgetProviderInfo {
                    minWidth = 50
                    minHeight = 50
                    minResizeWidth = 40
                    minResizeHeight = 30
                    resizeMode = AppWidgetProviderInfo.RESIZE_HORIZONTAL
                }
            )
        }

        assertThat(composer.appWidgetMinSize(displayMetrics, appWidgetManager, 1))
            .isEqualTo(DpSize(40.dp, 50.dp))
    }

    @Test
    fun appWidgetMinSize_verticalResizing() {
        val composer = SampleGlanceAppWidget { }
        val appWidgetManager = mock<AppWidgetManager> {
            on { getAppWidgetInfo(1) }.thenReturn(
                appWidgetProviderInfo {
                    minWidth = 50
                    minHeight = 50
                    minResizeWidth = 40
                    minResizeHeight = 30
                    resizeMode = AppWidgetProviderInfo.RESIZE_VERTICAL
                }
            )
        }

        assertThat(composer.appWidgetMinSize(displayMetrics, appWidgetManager, 1))
            .isEqualTo(DpSize(50.dp, 30.dp))
    }

    @Test
    fun appWidgetMinSize_bigMinResize() {
        val composer = SampleGlanceAppWidget { }
        val appWidgetManager = mock<AppWidgetManager> {
            on { getAppWidgetInfo(1) }.thenReturn(
                appWidgetProviderInfo {
                    minWidth = 50
                    minHeight = 50
                    minResizeWidth = 80
                    minResizeHeight = 70
                    resizeMode = AppWidgetProviderInfo.RESIZE_BOTH
                }
            )
        }

        assertThat(composer.appWidgetMinSize(displayMetrics, appWidgetManager, 1))
            .isEqualTo(DpSize(50.dp, 50.dp))
    }

    @Test
    fun findBestSize_onlyFitting() {
        assertThat(
            findBestSize(
                DpSize(10.dp, 10.dp),
                setOf(DpSize(15.dp, 15.dp), DpSize(50.dp, 50.dp))
            )
        ).isNull()

        val sizes = setOf(
            DpSize(90.dp, 90.dp),
            DpSize(180.dp, 180.dp),
            DpSize(300.dp, 300.dp),
            DpSize(180.dp, 48.dp),
            DpSize(300.dp, 48.dp),
            DpSize(48.dp, 180.dp),
            DpSize(48.dp, 300.dp),
        )
        assertThat(findBestSize(DpSize(48.dp, 91.dp), sizes))
            .isNull()
    }

    @Test
    fun findBestSize_smallestFitting() {
        val sizes = setOf(
            DpSize(90.dp, 90.dp),
            DpSize(180.dp, 180.dp),
            DpSize(300.dp, 300.dp),
            DpSize(180.dp, 48.dp),
            DpSize(300.dp, 48.dp),
            DpSize(48.dp, 180.dp),
            DpSize(48.dp, 300.dp),
        )
        assertThat(findBestSize(DpSize(140.dp, 500.dp), sizes))
            .isEqualTo(DpSize(48.dp, 300.dp))
        assertThat(findBestSize(DpSize(90.dp, 91.dp), sizes))
            .isEqualTo(DpSize(90.dp, 90.dp))
        assertThat(findBestSize(DpSize(200.dp, 200.dp), sizes))
            .isEqualTo(DpSize(180.dp, 180.dp))
    }

    // Testing on pre-S and post-S to test both when OPTION_APPWIDGET_SIZES is present or not.
    @Test
    @Config(sdk = [Build.VERSION_CODES.Q, Build.VERSION_CODES.S])
    fun extractAllSizes_shouldExtractSizesWhenPresent() {
        val bundle = optionsBundleOf(listOf(DpSize(140.dp, 110.dp), DpSize(100.dp, 150.dp)))
        assertThat(bundle.extractAllSizes { DpSize.Zero }).containsExactly(
            DpSize(140.dp, 110.dp),
            DpSize(100.dp, 150.dp)
        )
    }

    @Test
    fun extractAllSizes_emptyAppWidgetSizes_shouldExtractFromMinMax() {
        val bundle = optionsBundleOf(listOf(DpSize(140.dp, 110.dp), DpSize(100.dp, 150.dp)))
        bundle.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, ArrayList<SizeF>())
        assertThat(bundle.extractAllSizes { DpSize.Zero }).containsExactly(
            DpSize(140.dp, 110.dp),
            DpSize(100.dp, 150.dp)
        )
    }

    private fun optionsBundleOf(sizes: List<DpSize>): Bundle {
        require(sizes.isNotEmpty()) { "There must be at least one size" }
        val (minSize, maxSize) = sizes.fold(sizes[0] to sizes[0]) { acc, s ->
            DpSize(min(acc.first.width, s.width), min(acc.first.height, s.height)) to
                DpSize(max(acc.second.width, s.width), max(acc.second.height, s.height))
        }
        return Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minSize.width.value.toInt())
            putInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                minSize.height.value.toInt()
            )
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxSize.width.value.toInt())
            putInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                maxSize.height.value.toInt()
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val sizeList = sizes.map { it.toSizeF() }.toArrayList()
                putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizeList)
            }
        }
    }

    private fun createPortraitContext() =
        makeOrientationContext(Configuration.ORIENTATION_PORTRAIT)

    private fun createLandscapeContext() =
        makeOrientationContext(Configuration.ORIENTATION_LANDSCAPE)

    private fun makeOrientationContext(orientation: Int): Context {
        val config = context.resources.configuration
        config.orientation = orientation
        return context.createConfigurationContext(config)
    }

    private class SampleGlanceAppWidget(
        override val sizeMode: SizeMode = SizeMode.Single,
        val ui: @Composable () -> Unit,
    ) : GlanceAppWidget() {
        @Composable
        override fun Content() {
            ui()
        }
    }
}
