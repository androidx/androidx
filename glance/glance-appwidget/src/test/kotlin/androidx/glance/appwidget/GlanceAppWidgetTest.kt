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
import android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.SizeF
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.glance.GlanceId
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.text.Text
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GlanceAppWidgetTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val displayMetrics = context.resources.displayMetrics
    private val glanceId = AppWidgetId(1)
    // Use for tests that do not add bound widget information to AppWidgetManager, so that the
    // underlying AppWidgetSession does not attempt to use AppWidgetManager.
    private val fakeId = createFakeAppWidgetId()

    @Test
    fun createEmptyUi() = runTest {
        val rv: RemoteViews = TestWidget {}.compose(context, fakeId)
        val view = context.applyRemoteViews(rv)
        assertIs<FrameLayout>(view)
        assertThat(view.childCount).isEqualTo(0)
    }

    @Test
    fun createUiWithSize() = runTest {
        val rv =
            TestWidget {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
                .compose(
                    context,
                    fakeId,
                    size = DpSize(40.dp, 50.dp),
                )

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.text.toString()).isEqualTo("40.0.dp x 50.0.dp")
    }

    @Test
    fun createUiFromOptionBundle() = runTest {
        val rv =
            TestWidget {
                    val options = LocalAppWidgetOptions.current

                    Text(options.getString("StringKey", "<NOT FOUND>"))
                }
                .compose(
                    context,
                    fakeId,
                    bundleOf("StringKey" to "FOUND"),
                )

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.text.toString()).isEqualTo("FOUND")
    }

    @Test
    fun createUiFromGlanceId() = runTest {
        val rv = TestWidget { Text(LocalGlanceId.current.toString()) }.compose(context, fakeId)

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.text.toString()).isEqualTo(fakeId.toString())
    }

    @Test
    fun createUiWithSingleMode() = runTest {
        val appWidgetManager =
            Shadows.shadowOf(
                context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
            )
        appWidgetManager.addBoundWidget(
            glanceId.appWidgetId,
            appWidgetProviderInfo {
                minWidth = 50
                minHeight = 50
                minResizeWidth = 40
                minResizeHeight = 60
                resizeMode = AppWidgetProviderInfo.RESIZE_BOTH
            }
        )
        val rv =
            TestWidget {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
                .compose(context, glanceId)

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.text.toString()).isEqualTo("40.0.dp x 50.0.dp")
    }

    @Config(sdk = [30])
    @Test
    fun createUiWithExactModePreS() = runTest {
        val options = optionsBundleOf(listOf(DpSize(100.dp, 50.dp), DpSize(50.dp, 100.dp)))
        val rv =
            TestWidget(SizeMode.Exact) {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
                .compose(context, fakeId, options)

        val portraitView = createPortraitContext().applyRemoteViews(rv)
        assertIs<TextView>(portraitView)
        assertThat(portraitView.text.toString()).isEqualTo("50.0.dp x 100.0.dp")

        val landscapeView = createLandscapeContext().applyRemoteViews(rv)
        assertIs<TextView>(landscapeView)
        assertThat(landscapeView.text.toString()).isEqualTo("100.0.dp x 50.0.dp")
    }

    @Config(sdk = [30])
    @Test
    fun createUiWithResponsiveModePreS() = runTest {
        val sizes =
            setOf(
                DpSize(60.dp, 80.dp),
                DpSize(100.dp, 70.dp),
                DpSize(120.dp, 100.dp),
            )
        // Note: Landscape fits the 60x80 and 100x70, portrait doesn't fit anything
        val options = optionsBundleOf(listOf(DpSize(125.dp, 90.dp), DpSize(40.0.dp, 120.dp)))
        val rv =
            TestWidget(SizeMode.Responsive(sizes)) {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
                .compose(context, fakeId, options)

        val portraitView = createPortraitContext().applyRemoteViews(rv)
        assertIs<TextView>(portraitView)
        assertThat(portraitView.text.toString()).isEqualTo("60.0.dp x 80.0.dp")

        val landscapeView = createLandscapeContext().applyRemoteViews(rv)
        assertIs<TextView>(landscapeView)
        assertThat(landscapeView.text.toString()).isEqualTo("100.0.dp x 70.0.dp")
    }

    @Test
    fun createUiWithExactMode_noSizeFallsBackToUnique() = runTest {
        val appWidgetManager =
            Shadows.shadowOf(
                context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
            )
        appWidgetManager.addBoundWidget(
            glanceId.appWidgetId,
            appWidgetProviderInfo {
                minWidth = 50
                minHeight = 50
                minResizeWidth = 40
                minResizeHeight = 60
                resizeMode = AppWidgetProviderInfo.RESIZE_BOTH
            }
        )
        val rv =
            TestWidget(SizeMode.Exact) {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
                .compose(context, glanceId)

        val portraitView = createPortraitContext().applyRemoteViews(rv)
        assertIs<TextView>(portraitView)
        assertThat(portraitView.text.toString()).isEqualTo("40.0.dp x 50.0.dp")

        val landscapeView = createLandscapeContext().applyRemoteViews(rv)
        assertIs<TextView>(landscapeView)
        assertThat(landscapeView.text.toString()).isEqualTo("40.0.dp x 50.0.dp")
    }

    @Config(sdk = [30])
    @Test
    fun createUiWithResponsiveMode_noSizeUseMinSize() = runTest {
        val sizes =
            setOf(
                DpSize(60.dp, 80.dp),
                DpSize(100.dp, 70.dp),
                DpSize(120.dp, 100.dp),
            )
        val rv =
            TestWidget(SizeMode.Responsive(sizes)) {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
                .compose(context, fakeId)

        val portraitView = createPortraitContext().applyRemoteViews(rv)
        assertIs<TextView>(portraitView)
        assertThat(portraitView.text.toString()).isEqualTo("60.0.dp x 80.0.dp")

        val landscapeView = createLandscapeContext().applyRemoteViews(rv)
        assertIs<TextView>(landscapeView)
        assertThat(landscapeView.text.toString()).isEqualTo("60.0.dp x 80.0.dp")
    }

    @Test
    fun appWidgetMinSize_noResizing() {
        val appWidgetManager =
            mock<AppWidgetManager> {
                on { getAppWidgetInfo(1) }
                    .thenReturn(
                        appWidgetProviderInfo {
                            minWidth = 50
                            minHeight = 50
                            minResizeWidth = 40
                            minResizeHeight = 30
                            resizeMode = AppWidgetProviderInfo.RESIZE_NONE
                        }
                    )
            }

        assertThat(appWidgetMinSize(displayMetrics, appWidgetManager, 1))
            .isEqualTo(DpSize(50.dp, 50.dp))
    }

    @Test
    fun appWidgetMinSize_horizontalResizing() {
        val appWidgetManager =
            mock<AppWidgetManager> {
                on { getAppWidgetInfo(1) }
                    .thenReturn(
                        appWidgetProviderInfo {
                            minWidth = 50
                            minHeight = 50
                            minResizeWidth = 40
                            minResizeHeight = 30
                            resizeMode = AppWidgetProviderInfo.RESIZE_HORIZONTAL
                        }
                    )
            }

        assertThat(appWidgetMinSize(displayMetrics, appWidgetManager, 1))
            .isEqualTo(DpSize(40.dp, 50.dp))
    }

    @Test
    fun appWidgetMinSize_verticalResizing() {
        val appWidgetManager =
            mock<AppWidgetManager> {
                on { getAppWidgetInfo(1) }
                    .thenReturn(
                        appWidgetProviderInfo {
                            minWidth = 50
                            minHeight = 50
                            minResizeWidth = 40
                            minResizeHeight = 30
                            resizeMode = AppWidgetProviderInfo.RESIZE_VERTICAL
                        }
                    )
            }

        assertThat(appWidgetMinSize(displayMetrics, appWidgetManager, 1))
            .isEqualTo(DpSize(50.dp, 30.dp))
    }

    @Test
    fun appWidgetMinSize_bigMinResize() {
        val appWidgetManager =
            mock<AppWidgetManager> {
                on { getAppWidgetInfo(1) }
                    .thenReturn(
                        appWidgetProviderInfo {
                            minWidth = 50
                            minHeight = 50
                            minResizeWidth = 80
                            minResizeHeight = 70
                            resizeMode = AppWidgetProviderInfo.RESIZE_BOTH
                        }
                    )
            }

        assertThat(appWidgetMinSize(displayMetrics, appWidgetManager, 1))
            .isEqualTo(DpSize(50.dp, 50.dp))
    }

    @Test
    fun findBestSize_onlyFitting() {
        assertThat(
                findBestSize(
                    DpSize(10.dp, 10.dp),
                    setOf(DpSize(15.dp, 15.dp), DpSize(50.dp, 50.dp))
                )
            )
            .isNull()

        val sizes =
            setOf(
                DpSize(90.dp, 90.dp),
                DpSize(180.dp, 180.dp),
                DpSize(300.dp, 300.dp),
                DpSize(180.dp, 48.dp),
                DpSize(300.dp, 48.dp),
                DpSize(48.dp, 180.dp),
                DpSize(48.dp, 300.dp),
            )
        assertThat(findBestSize(DpSize(48.dp, 91.dp), sizes)).isNull()
    }

    @Test
    fun findBestSize_smallestFitting() {
        val sizes =
            setOf(
                DpSize(90.dp, 90.dp),
                DpSize(180.dp, 180.dp),
                DpSize(300.dp, 300.dp),
                DpSize(180.dp, 48.dp),
                DpSize(300.dp, 48.dp),
                DpSize(48.dp, 180.dp),
                DpSize(48.dp, 300.dp),
            )
        assertThat(findBestSize(DpSize(140.dp, 500.dp), sizes)).isEqualTo(DpSize(48.dp, 300.dp))
        assertThat(findBestSize(DpSize(90.dp, 91.dp), sizes)).isEqualTo(DpSize(90.dp, 90.dp))
        assertThat(findBestSize(DpSize(200.dp, 200.dp), sizes)).isEqualTo(DpSize(180.dp, 180.dp))
    }

    // Testing on pre-S and post-S to test both when OPTION_APPWIDGET_SIZES is present or not.
    @Test
    @Config(sdk = [Build.VERSION_CODES.Q, Build.VERSION_CODES.S])
    fun extractAllSizes_shouldExtractSizesWhenPresent() {
        val bundle = optionsBundleOf(listOf(DpSize(140.dp, 110.dp), DpSize(100.dp, 150.dp)))
        assertThat(bundle.extractAllSizes { DpSize.Zero })
            .containsExactly(DpSize(140.dp, 110.dp), DpSize(100.dp, 150.dp))
    }

    @Test
    fun extractAllSizes_emptyAppWidgetSizes_shouldExtractFromMinMax() {
        val bundle = optionsBundleOf(listOf(DpSize(140.dp, 110.dp), DpSize(100.dp, 150.dp)))
        bundle.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, ArrayList<SizeF>())
        assertThat(bundle.extractAllSizes { DpSize.Zero })
            .containsExactly(DpSize(140.dp, 110.dp), DpSize(100.dp, 150.dp))
    }

    @Test
    fun cancellingProvideContentEmitsNullContent() = runTest {
        val widget =
            object : GlanceAppWidget() {
                override suspend fun provideGlance(context: Context, id: GlanceId) {
                    coroutineScope {
                        val provideContentJob = launch { provideContent { Text("") } }
                        delay(100)
                        provideContentJob.cancel()
                    }
                }
            }
        widget.runGlance(context, AppWidgetId(0)).take(2).collectIndexed { index, content ->
            when (index) {
                // Initial content
                0 -> assertThat(content).isNotNull()
                // Content is null again when provideContent is cancelled
                1 -> assertThat(content).isNull()
                else -> throw Error("Invalid index $index")
            }
        }
    }

    @Test
    fun composeForPreview() = runTest {
        val rv =
            TestWidget.forPreview { widgetCategory -> Text("$widgetCategory") }
                .composeForPreview(context, WIDGET_CATEGORY_HOME_SCREEN)

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.text.toString()).isEqualTo(WIDGET_CATEGORY_HOME_SCREEN.toString())
    }

    @Test
    fun composeForPreview_sizeModeSingle_noInfo() = runTest {
        val rv =
            TestWidget.forPreview {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
                .composeForPreview(context, WIDGET_CATEGORY_HOME_SCREEN)

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.text.toString()).isEqualTo("0.0.dp x 0.0.dp")
    }

    @Test
    fun composeForPreview_sizeModeSingle_withInfo() = runTest {
        val rv =
            TestWidget.forPreview {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
                .composeForPreview(
                    context,
                    WIDGET_CATEGORY_HOME_SCREEN,
                    appWidgetProviderInfo {
                        minWidth = 40
                        minResizeWidth = 40
                        minHeight = 50
                        minResizeHeight = 50
                    }
                )

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.text.toString()).isEqualTo("40.0.dp x 50.0.dp")
    }

    @Config(minSdk = 31)
    @Test
    fun composeForPreview_sizeModeResponsive() = runTest {
        val sizes =
            setOf(
                DpSize(60.dp, 80.dp),
                DpSize(100.dp, 70.dp),
                DpSize(120.dp, 100.dp),
            )
        val rv =
            TestWidget.forPreview(SizeMode.Responsive(sizes)) {
                    val size = LocalSize.current
                    Text("${size.width} x ${size.height}")
                }
                .composeForPreview(context, WIDGET_CATEGORY_HOME_SCREEN)

        sizes.forEach { size ->
            val view =
                context.applyRemoteViews(
                    rv,
                    LayoutParams(
                        size.width.plus(5.dp).toPixels(context),
                        size.height.plus(5.dp).toPixels(context),
                    ),
                )
            assertIs<TextView>(view)
            assertThat(view.text.toString()).isEqualTo("${size.width} x ${size.height}")
        }
    }

    private fun createPortraitContext() = makeOrientationContext(Configuration.ORIENTATION_PORTRAIT)

    private fun createLandscapeContext() =
        makeOrientationContext(Configuration.ORIENTATION_LANDSCAPE)

    private fun makeOrientationContext(orientation: Int): Context {
        val config = context.resources.configuration
        config.orientation = orientation
        return context.createConfigurationContext(config)
    }
}
