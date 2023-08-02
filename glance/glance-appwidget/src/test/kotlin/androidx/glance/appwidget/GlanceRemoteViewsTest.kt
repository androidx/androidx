/*
 * Copyright 2022 The Android Open Source Project
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
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.text.Text
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalGlanceRemoteViewsApi::class)
@RunWith(RobolectricTestRunner::class)
class GlanceRemoteViewsTest {

    private var fakeCoroutineScope: TestScope = TestScope()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun createEmptyUi() = fakeCoroutineScope.runTest {
        val composer = GlanceRemoteViews()

        val rv = composer.compose(context = context, size = DpSize(40.dp, 50.dp)) {
        }.remoteViews

        val view = context.applyRemoteViews(rv)
        assertIs<FrameLayout>(view)
        Truth.assertThat(view.childCount).isEqualTo(0)
    }

    @Test
    fun createUiWithSize() = fakeCoroutineScope.runTest {
        val composer = GlanceRemoteViews()

        val rv = composer.compose(context = context, size = DpSize(40.dp, 50.dp)) {
            val size = LocalSize.current
            Text("${size.width} x ${size.height}")
        }.remoteViews

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        Truth.assertThat(view.text).isEqualTo("40.0.dp x 50.0.dp")
    }

    @Test
    fun createUiFromOptionBundle() = fakeCoroutineScope.runTest {
        val composer = GlanceRemoteViews()
        val bundle = Bundle()
        bundle.putString("StringKey", "FOUND")

        val rv = composer.compose(
            context,
            DpSize(40.dp, 50.dp),
            appWidgetOptions = bundle
        ) {
            val options = LocalAppWidgetOptions.current
            Text(options.getString("StringKey", "<NOT FOUND>"))
        }.remoteViews

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        Truth.assertThat(view.text).isEqualTo("FOUND")
    }

    @Test
    fun createUiFromGlanceId() = fakeCoroutineScope.runTest {
        val composer = GlanceRemoteViews()

        val rv = composer.compose(context, DpSize(40.dp, 50.dp)) {
            LocalGlanceId.current

            Text("No error thrown")
        }.remoteViews

        val view = context.applyRemoteViews(rv)
        assertIs<TextView>(view)
        Truth.assertThat(view.text).isEqualTo("No error thrown")
    }
}