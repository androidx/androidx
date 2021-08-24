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
import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import androidx.glance.GlanceInternalApi
import androidx.glance.LocalSize
import androidx.glance.layout.Text
import androidx.glance.unit.DpSize
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

@OptIn(GlanceInternalApi::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GlanceAppWidgetTest {

    private lateinit var fakeCoroutineScope: TestCoroutineScope
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        fakeCoroutineScope = TestCoroutineScope()
    }

    @Test
    fun createEmptyUI() = fakeCoroutineScope.runBlockingTest {
        val composer = SampleGlanceAppWidget { }

        val rv = composer.compose(context, 1, Bundle(), DpSize(40.dp, 50.dp))

        val view = context.applyRemoteViews(rv)
        assertIs<RelativeLayout>(view)
        assertThat(view.childCount).isEqualTo(0)
    }

    @Test
    fun createUiWithSize() = fakeCoroutineScope.runBlockingTest {
        val composer = SampleGlanceAppWidget {
            val size = LocalSize.current
            Text("${size.width} x ${size.height}")
        }

        val rv = composer.compose(context, 1, Bundle(), DpSize(40.dp, 50.dp))

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
        val rv = composer.compose(context, 1, bundle, DpSize(40.dp, 50.dp))

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
        val rv = composer.compose(context, 1, bundle, DpSize(40.dp, 50.dp))

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        assertThat(view.text).isEqualTo("AppWidgetId(appWidgetId=1)")
    }

    private class SampleGlanceAppWidget(val ui: @Composable () -> Unit) : GlanceAppWidget() {
        @Composable
        override fun Content() {
            ui()
        }
    }
}