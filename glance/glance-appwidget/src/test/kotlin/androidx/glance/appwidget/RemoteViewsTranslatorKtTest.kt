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
import android.content.res.Configuration
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.glance.GlanceInternalApi
import androidx.glance.Modifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.absolutePadding
import androidx.glance.layout.padding
import androidx.glance.unit.Dp
import androidx.glance.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.math.floor

@OptIn(GlanceInternalApi::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RemoteViewsTranslatorKtTest {

    private lateinit var fakeCoroutineScope: TestCoroutineScope
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val displayMetrics = context.resources.displayMetrics

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun canTranslateBox() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate { Box {} }
        val view = applyRemoteViews(rv)

        assertThat(view).isInstanceOf(RelativeLayout::class.java)
        require(view is RelativeLayout)
        assertThat(view.childCount).isEqualTo(0)
    }

    @Test
    fun canTranslateBoxWithAlignment() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Box(contentAlignment = Alignment.BottomEnd) { }
        }
        val view = applyRemoteViews(rv)

        assertThat(view).isInstanceOf(RelativeLayout::class.java)
        require(view is RelativeLayout)
        assertThat(view.gravity).isEqualTo(Gravity.BOTTOM or Gravity.END)
    }

    @Test
    fun canTranslateBoxWithChildren() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Box {
                Box(contentAlignment = Alignment.Center) {}
                Box(contentAlignment = Alignment.BottomEnd) {}
            }
        }
        val view = applyRemoteViews(rv)

        assertThat(view).isInstanceOf(RelativeLayout::class.java)
        require(view is RelativeLayout)
        assertThat(view.childCount).isEqualTo(2)
        assertThat(view.getChildAt(0)).isInstanceOf(RelativeLayout::class.java)
        assertThat(view.getChildAt(1)).isInstanceOf(RelativeLayout::class.java)
        val child1 = view.getChildAt(0) as RelativeLayout
        assertThat(child1.gravity).isEqualTo(Gravity.CENTER)
        val child2 = view.getChildAt(1) as RelativeLayout
        assertThat(child2.gravity).isEqualTo(Gravity.BOTTOM or Gravity.END)
    }

    @Test
    fun canTranslateMultipleNodes() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Box(contentAlignment = Alignment.Center) {}
            Box(contentAlignment = Alignment.BottomEnd) {}
        }
        val view = applyRemoteViews(rv)

        assertThat(view).isInstanceOf(RelativeLayout::class.java)
        require(view is RelativeLayout)
        assertThat(view.childCount).isEqualTo(2)
        assertThat(view.getChildAt(0)).isInstanceOf(RelativeLayout::class.java)
        assertThat(view.getChildAt(1)).isInstanceOf(RelativeLayout::class.java)
        val child1 = view.getChildAt(0) as RelativeLayout
        assertThat(child1.gravity).isEqualTo(Gravity.CENTER)
        val child2 = view.getChildAt(1) as RelativeLayout
        assertThat(child2.gravity).isEqualTo(Gravity.BOTTOM or Gravity.END)
    }

    @Test
    fun canTranslatePaddingModifier() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslate {
            Box(
                modifier = Modifier.padding(
                    start = 4.dp,
                    end = 5.dp,
                    top = 6.dp,
                    bottom = 7.dp,
                )
            ) { }
        }
        val view = applyRemoteViews(rv)

        assertThat(view).isInstanceOf(RelativeLayout::class.java)
        assertThat(view.paddingLeft).isEqualTo(dpToPixel(4.dp))
        assertThat(view.paddingRight).isEqualTo(dpToPixel(5.dp))
        assertThat(view.paddingTop).isEqualTo(dpToPixel(6.dp))
        assertThat(view.paddingBottom).isEqualTo(dpToPixel(7.dp))
    }

    @Test
    fun canTranslatePaddingRTL() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslateInRtl {
            Box(
                modifier = Modifier.padding(
                    start = 4.dp,
                    end = 5.dp,
                    top = 6.dp,
                    bottom = 7.dp,
                )
            ) { }
        }
        val view = applyRemoteViews(rv)

        assertThat(view).isInstanceOf(RelativeLayout::class.java)
        assertThat(view.paddingLeft).isEqualTo(dpToPixel(5.dp))
        assertThat(view.paddingRight).isEqualTo(dpToPixel(4.dp))
        assertThat(view.paddingTop).isEqualTo(dpToPixel(6.dp))
        assertThat(view.paddingBottom).isEqualTo(dpToPixel(7.dp))
    }

    @Test
    fun canTranslateAbsolutePaddingRTL() = fakeCoroutineScope.runBlockingTest {
        val rv = runAndTranslateInRtl {
            Box(
                modifier = Modifier.absolutePadding(
                    left = 4.dp,
                    right = 5.dp,
                    top = 6.dp,
                    bottom = 7.dp,
                )
            ) { }
        }
        val view = applyRemoteViews(rv)

        assertThat(view).isInstanceOf(RelativeLayout::class.java)
        assertThat(view.paddingLeft).isEqualTo(dpToPixel(4.dp))
        assertThat(view.paddingRight).isEqualTo(dpToPixel(5.dp))
        assertThat(view.paddingTop).isEqualTo(dpToPixel(6.dp))
        assertThat(view.paddingBottom).isEqualTo(dpToPixel(7.dp))
    }

    private suspend fun runAndTranslate(
        context: Context = this.context,
        content: @Composable () -> Unit
    ): RemoteViews {
        val root = runTestingComposition(content)
        return translateComposition(context, root)
    }

    private suspend fun runAndTranslateInRtl(content: @Composable () -> Unit): RemoteViews {
        val rtlLocale = Locale.getAvailableLocales().first {
            TextUtils.getLayoutDirectionFromLocale(it) == View.LAYOUT_DIRECTION_RTL
        }
        val rtlContext = context.createConfigurationContext(
            Configuration(context.resources.configuration).also {
                it.setLayoutDirection(rtlLocale)
            }
        )
        return runAndTranslate(rtlContext, content)
    }

    private fun applyRemoteViews(rv: RemoteViews) =
        rv.apply(context, FrameLayout(context))

    private fun dpToPixel(dp: Dp) =
        floor(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.value, displayMetrics))
            .toInt()
}