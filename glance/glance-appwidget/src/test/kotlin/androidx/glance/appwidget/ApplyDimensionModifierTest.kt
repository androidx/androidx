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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.glance.Modifier
import androidx.glance.appwidget.ViewSubject.Companion.assertThat
import androidx.glance.appwidget.test.R
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Text
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentWidth
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ApplyDimensionModifierTest {

    private lateinit var fakeCoroutineScope: TestCoroutineScope
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun normalResourceWidth() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text(
                "content",
                modifier = Modifier.width(R.dimen.standard_dimension)
            )
        }
        val view = context.applyRemoteViews(rv)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertIs<TextView>(view)
            assertThat(view.layoutParams.width)
                .isEqualTo(context.resources.getDimensionPixelSize(R.dimen.standard_dimension))
        } else {
            val textView = view.findView<ViewGroup> { it.id == R.id.sizeView }?.getChildAt(0)
            assertNotNull(textView)
            assertIs<TextView>(textView)
            val targetWidth = context.resources.getDimensionPixelSize(R.dimen.standard_dimension)
            assertThat(textView.minWidth).isEqualTo(targetWidth)
            assertThat(textView.maxWidth).isEqualTo(targetWidth)
        }
    }

    @Test
    fun fillWidth() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text("content", modifier = Modifier.fillMaxWidth())
        }
        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.MATCH_PARENT)
    }

    @Test
    fun wrapWidth() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text("content", modifier = Modifier.wrapContentWidth())
        }
        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @Test
    fun expandWidth() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Row {
                Text("content", modifier = Modifier.defaultWeight())
            }
        }
        val view = context.applyRemoteViews(rv)
        assertIs<LinearLayout>(view)
        val child = assertIs<TextView>(view.getChildAt(0))
        val layoutParam = assertIs<LinearLayout.LayoutParams>(child.layoutParams)
        assertThat(layoutParam.width).isEqualTo(0)
        assertThat(layoutParam.weight).isEqualTo(1f)
    }

    @Test
    fun fillResourceWidth() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text("content", modifier = Modifier.width(R.dimen.fill_dimension))
        }
        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.MATCH_PARENT)
    }

    @Test
    fun wrapResourceWidth() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text("content", modifier = Modifier.width(R.dimen.wrap_dimension))
        }
        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.layoutParams.width).isEqualTo(ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @Test
    fun normalResourceHeight() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text(
                "content",
                modifier = Modifier.height(R.dimen.standard_dimension)
            )
        }
        val view = context.applyRemoteViews(rv)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertIs<TextView>(view)
            assertThat(view.layoutParams.height)
                .isEqualTo(context.resources.getDimensionPixelSize(R.dimen.standard_dimension))
        } else {
            val textView = view.findView<ViewGroup> { it.id == R.id.sizeView }?.getChildAt(0)
            assertNotNull(textView)
            assertIs<TextView>(textView)
            val targetHeight = context.resources.getDimensionPixelSize(R.dimen.standard_dimension)
            assertThat(textView.minHeight).isEqualTo(targetHeight)
            assertThat(textView.maxHeight).isEqualTo(targetHeight)
        }
    }

    @Test
    fun fillHeight() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text("content", modifier = Modifier.fillMaxHeight())
        }
        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.layoutParams.height).isEqualTo(ViewGroup.LayoutParams.MATCH_PARENT)
    }

    @Test
    fun wrapHeight() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text("content", modifier = Modifier.wrapContentHeight())
        }
        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.layoutParams.height).isEqualTo(ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @Test
    fun expandHeight() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Column {
                Text("content", modifier = Modifier.defaultWeight())
            }
        }
        val view = context.applyRemoteViews(rv)
        assertIs<LinearLayout>(view)
        val child = assertIs<TextView>(view.getChildAt(0))
        val layoutParam = assertIs<LinearLayout.LayoutParams>(child.layoutParams)
        assertThat(layoutParam.height).isEqualTo(0)
        assertThat(layoutParam.weight).isEqualTo(1f)
    }

    @Test
    fun fillResourceHeight() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text("content", modifier = Modifier.height(R.dimen.fill_dimension))
        }
        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view).hasLayoutParamsHeight(ViewGroup.LayoutParams.MATCH_PARENT)
    }

    @Test
    fun wrapResourceHeight() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text("content", modifier = Modifier.height(R.dimen.wrap_dimension))
        }
        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view).hasLayoutParamsHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @Test
    fun wrapWidth_fillHeight() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text("content", modifier = Modifier.wrapContentWidth().fillMaxHeight())
        }

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view).hasLayoutParamsWidth(ViewGroup.LayoutParams.WRAP_CONTENT)
        assertThat(view).hasLayoutParamsHeight(ViewGroup.LayoutParams.MATCH_PARENT)
    }

    @Test
    fun fillWidth_wrapHeight() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text("content", modifier = Modifier.fillMaxWidth().wrapContentHeight())
        }

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view).hasLayoutParamsWidth(ViewGroup.LayoutParams.MATCH_PARENT)
        assertThat(view).hasLayoutParamsHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @Test
    fun fillWidth_fixedHeight() = fakeCoroutineScope.runBlockingTest {
        val rv = context.runAndTranslate {
            Text("content", modifier = Modifier.fillMaxWidth().height(50.dp))
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

    private fun getSizingView(view: View): TextView {
        val sizeView = view.findView<ViewGroup> { it.id == R.id.sizeView }?.getChildAt(0)
        assertNotNull(sizeView)
        assertIs<TextView>(sizeView)
        return sizeView
    }
}