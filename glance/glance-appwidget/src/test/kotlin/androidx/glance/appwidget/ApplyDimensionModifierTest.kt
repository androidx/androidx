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

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.ViewSubject.Companion.assertThat
import androidx.glance.appwidget.test.R
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentSize
import androidx.glance.layout.wrapContentWidth
import androidx.glance.text.Text
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class ApplyDimensionModifierTest {

    private lateinit var fakeCoroutineScope: TestScope
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
        ShadowLog.stream = System.out
    }

    @Test
    fun normalResourceWidth() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Text("content", modifier = GlanceModifier.width(R.dimen.standard_dimension))
                }
            val view = context.applyRemoteViews(rv)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                assertIs<TextView>(view)
                assertThat(view.layoutParams.width)
                    .isEqualTo(context.resources.getDimensionPixelSize(R.dimen.standard_dimension))
            } else {
                val textView = getSizingView(view)
                assertNotNull(textView)
                assertIs<TextView>(textView)
                val targetWidth =
                    context.resources.getDimensionPixelSize(R.dimen.standard_dimension)
                assertThat(textView.minWidth).isEqualTo(targetWidth)
                assertThat(textView.maxWidth).isEqualTo(targetWidth)
            }
        }

    @Test
    fun fillWidth() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Text("content", modifier = GlanceModifier.fillMaxWidth())
                }
            val view = context.applyRemoteViews(rv)
            assertIs<TextView>(view)
            assertThat(view.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.MATCH_PARENT)
        }

    @Test
    fun wrapWidth() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Text("content", modifier = GlanceModifier.wrapContentWidth())
                }
            val view = context.applyRemoteViews(rv)
            assertIs<TextView>(view)
            assertThat(view.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    @Test
    fun expandWidth() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Row { Text("content", modifier = GlanceModifier.defaultWeight()) }
                }
            val view = context.applyRemoteViews(rv)
            assertIs<LinearLayout>(view)
            val child = assertIs<TextView>(view.nonGoneChildren.single())
            val layoutParam = assertIs<LinearLayout.LayoutParams>(child.layoutParams)
            assertThat(layoutParam.width).isEqualTo(0)
            assertThat(layoutParam.weight).isEqualTo(1f)
        }

    @Test
    fun fillResourceWidth() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Text("content", modifier = GlanceModifier.width(R.dimen.fill_dimension))
                }
            val view = context.applyRemoteViews(rv)
            assertIs<TextView>(view)
            assertThat(view.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.MATCH_PARENT)
        }

    @Test
    fun wrapResourceWidth() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Text("content", modifier = GlanceModifier.width(R.dimen.wrap_dimension))
                }
            val view = context.applyRemoteViews(rv)
            assertIs<TextView>(view)
            assertThat(view.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    @Test
    fun normalResourceHeight() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Text("content", modifier = GlanceModifier.height(R.dimen.standard_dimension))
                }
            val view = context.applyRemoteViews(rv)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                assertIs<TextView>(view)
                assertThat(view.layoutParams.height)
                    .isEqualTo(context.resources.getDimensionPixelSize(R.dimen.standard_dimension))
            } else {
                val textView = getSizingView(view)
                assertNotNull(textView)
                assertIs<TextView>(textView)
                val targetHeight =
                    context.resources.getDimensionPixelSize(R.dimen.standard_dimension)
                assertThat(textView.minHeight).isEqualTo(targetHeight)
                assertThat(textView.maxHeight).isEqualTo(targetHeight)
            }
        }

    @Test
    fun fillHeight() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Text("content", modifier = GlanceModifier.fillMaxHeight())
                }
            val view = context.applyRemoteViews(rv)
            assertIs<TextView>(view)
            assertThat(view.layoutParams.height).isEqualTo(ViewGroup.LayoutParams.MATCH_PARENT)
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun wrapHeight() =
        fakeCoroutineScope.runMediumTest {
            measureTime {
                    val rv =
                        context.runAndTranslate {
                            Text("content", modifier = GlanceModifier.wrapContentHeight())
                        }
                    measureTime {
                            val view = context.applyRemoteViews(rv)
                            assertIs<TextView>(view)
                            assertThat(view.layoutParams.height)
                                .isEqualTo(ViewGroup.LayoutParams.WRAP_CONTENT)
                        }
                        .also {
                            println(
                                "[ApplyDimensionModifierTest.wrapHeight] applyRemoteViews took: $it"
                            )
                        }
                }
                .also { println("[ApplyDimensionModifierTest.wrapHeight] testBody took: $it") }
        }

    @Test
    fun expandHeight() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Column { Text("content", modifier = GlanceModifier.defaultWeight()) }
                }
            val view = context.applyRemoteViews(rv)
            assertIs<LinearLayout>(view)
            val child = assertIs<TextView>(view.nonGoneChildren.single())
            val layoutParam = assertIs<LinearLayout.LayoutParams>(child.layoutParams)
            assertThat(layoutParam.height).isEqualTo(0)
            assertThat(layoutParam.weight).isEqualTo(1f)
        }

    @Test
    fun fillResourceHeight() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Text("content", modifier = GlanceModifier.height(R.dimen.fill_dimension))
                }
            val view = context.applyRemoteViews(rv)
            assertIs<TextView>(view)
            assertThat(view).hasLayoutParamsHeight(ViewGroup.LayoutParams.MATCH_PARENT)
        }

    @Test
    fun wrapResourceHeight() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Text("content", modifier = GlanceModifier.height(R.dimen.wrap_dimension))
                }
            val view = context.applyRemoteViews(rv)
            assertIs<TextView>(view)
            assertThat(view).hasLayoutParamsHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    @Test
    fun wrapWidth_fillHeight() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Text("content", modifier = GlanceModifier.wrapContentWidth().fillMaxHeight())
                }

            val view = context.applyRemoteViews(rv)
            assertIs<TextView>(view)
            assertThat(view).hasLayoutParamsWidth(ViewGroup.LayoutParams.WRAP_CONTENT)
            assertThat(view).hasLayoutParamsHeight(ViewGroup.LayoutParams.MATCH_PARENT)
        }

    @Test
    fun fillWidth_wrapHeight() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Text("content", modifier = GlanceModifier.fillMaxWidth().wrapContentHeight())
                }

            val view = context.applyRemoteViews(rv)
            assertIs<TextView>(view)
            assertThat(view).hasLayoutParamsWidth(ViewGroup.LayoutParams.MATCH_PARENT)
            assertThat(view).hasLayoutParamsHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        }

    @Test
    fun fillWidth_fixedHeight() =
        fakeCoroutineScope.runMediumTest {
            val rv =
                context.runAndTranslate {
                    Text("content", modifier = GlanceModifier.fillMaxWidth().height(50.dp))
                }

            val view = context.applyRemoteViews(rv)
            assertThat(view).hasLayoutParamsWidth(ViewGroup.LayoutParams.MATCH_PARENT)

            val targetHeight = 50.dp.toPixels(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                assertIs<TextView>(view)
                assertThat(view).hasLayoutParamsHeight(targetHeight)
            } else {
                val sizeView = getSizingView(view)
                assertThat(sizeView.minHeight).isEqualTo(targetHeight)
                assertThat(sizeView.maxHeight).isEqualTo(targetHeight)
            }
        }

    @Test
    fun wrappedFitImageAdjustsSize() =
        fakeCoroutineScope.runMediumTest {
            // Any dimension set to wrap should cause the view bounds to be adjusted
            setOf(
                    GlanceModifier.wrapContentSize(),
                    GlanceModifier.wrapContentWidth().fillMaxHeight(),
                    GlanceModifier.fillMaxWidth().wrapContentHeight(),
                    GlanceModifier.width(100.dp).wrapContentHeight(),
                    GlanceModifier.wrapContentWidth().height(100.dp),
                )
                .forEach { sizeModifier ->
                    val imageView = getSizedAndTranslatedImageView(sizeModifier, ContentScale.Fit)
                    assertThat(imageView?.adjustViewBounds).isEqualTo(true)
                }
        }

    @Test
    fun cropImageDoesntAdjustsSize() =
        fakeCoroutineScope.runMediumTest {
            // If the contentScale is crop, never adjust the view bounds
            setOf(
                    GlanceModifier.wrapContentSize(),
                    GlanceModifier.wrapContentWidth().fillMaxHeight(),
                    GlanceModifier.fillMaxWidth().wrapContentHeight(),
                    GlanceModifier.width(100.dp).wrapContentHeight(),
                    GlanceModifier.wrapContentWidth().height(100.dp),
                    GlanceModifier.fillMaxSize(),
                    GlanceModifier.width(100.dp).height(100.dp),
                    GlanceModifier.fillMaxWidth().height(100.dp),
                    GlanceModifier.width(100.dp).fillMaxHeight(),
                )
                .forEach { sizeModifier ->
                    val imageView = getSizedAndTranslatedImageView(sizeModifier, ContentScale.Crop)
                    assertThat(imageView?.adjustViewBounds).isEqualTo(false)
                }
        }

    @Test
    fun fillImageDoesntAdjustsSize() =
        fakeCoroutineScope.runMediumTest {
            // Image with FillBounds contentScale should never set adjust view bounds
            setOf(
                    GlanceModifier.wrapContentSize(),
                    GlanceModifier.wrapContentWidth().fillMaxHeight(),
                    GlanceModifier.fillMaxWidth().wrapContentHeight(),
                    GlanceModifier.width(100.dp).wrapContentHeight(),
                    GlanceModifier.wrapContentWidth().height(100.dp),
                    GlanceModifier.fillMaxSize(),
                    GlanceModifier.width(100.dp).height(100.dp),
                    GlanceModifier.fillMaxWidth().height(100.dp),
                    GlanceModifier.width(100.dp).fillMaxHeight(),
                )
                .forEach { sizeModifier ->
                    val imageView =
                        getSizedAndTranslatedImageView(sizeModifier, ContentScale.FillBounds)
                    assertThat(imageView?.adjustViewBounds).isEqualTo(false)
                }
        }

    @Test
    fun nonwrappedFitImageDoesntAdjustsSize() =
        fakeCoroutineScope.runMediumTest {
            // No dimension set to wrap should not cause the view bounds to be adjusted
            setOf(
                    GlanceModifier.fillMaxSize(),
                    GlanceModifier.width(100.dp).fillMaxHeight(),
                    GlanceModifier.fillMaxWidth().height(100.dp),
                    GlanceModifier.width(100.dp).height(100.dp),
                )
                .forEach { sizeModifier ->
                    val imageView = getSizedAndTranslatedImageView(sizeModifier, ContentScale.Fit)
                    assertThat(imageView?.adjustViewBounds).isEqualTo(false)
                }
        }

    private suspend fun getSizedAndTranslatedImageView(
        modifier: GlanceModifier,
        contentScale: ContentScale,
    ): ImageView? {
        val rv =
            context.runAndTranslate {
                Image(
                    provider = ImageProvider(resId = R.drawable.glance_button_outline),
                    contentDescription = "TEST",
                    modifier = modifier,
                    contentScale = contentScale,
                )
            }

        return context.applyRemoteViews(rv).findViewByType()
    }

    private fun getSizingView(view: View): TextView {
        val sizeView =
            view.findView<ViewGroup> { it.id == R.id.sizeView }?.nonGoneChildren?.single()
        assertNotNull(sizeView)
        assertIs<TextView>(sizeView)
        return sizeView
    }
}
