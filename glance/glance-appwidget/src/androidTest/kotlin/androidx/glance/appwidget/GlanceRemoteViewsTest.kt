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

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.children
import androidx.glance.text.Text
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@MediumTest
class GlanceRemoteViewsTest {
    @get:Rule
    val mActivityRule: ActivityScenarioRule<AppWidgetHostTestActivity> =
        ActivityScenarioRule(AppWidgetHostTestActivity::class.java)

    @Test
    fun createSimpleRemoteViews() {
        mActivityRule.scenario.onActivity { context ->
            val hostView = NonWidgetAppWidgetHostView(context)
            val glanceRemoteViews = GlanceRemoteViews()

            runBlocking {
                val remoteViews =
                    glanceRemoteViews.compose(context = context, size = DpSize(100.dp, 50.dp)) {
                        Text("text content")
                    }.remoteViews
                hostView.updateAppWidget(remoteViews)
            }

            val textView = hostView.getUnboxedView<TextView>()
            assertThat(textView.text.toString()).isEqualTo("text content")
        }
    }

    @Test
    fun composeMultipleTimes() {
        mActivityRule.scenario.onActivity { context ->
            val hostView = NonWidgetAppWidgetHostView(context)
            val glanceRemoteViews = GlanceRemoteViews()

            runBlocking {
                hostView.updateAppWidget(
                    glanceRemoteViews.compose(
                        context = context,
                        size = DpSize(100.dp, 50.dp)
                    ) {
                        Text("first")
                    }.remoteViews
                )
                hostView.updateAppWidget(
                    glanceRemoteViews.compose(
                        context = context,
                        size = DpSize(100.dp, 50.dp)
                    ) {
                        Text("second")
                    }.remoteViews
                )
            }

            // verify that update on a same hostView twice works
            val textView = hostView.getUnboxedView<TextView>()
            assertThat(textView.text.toString()).isEqualTo("second")
        }
    }
}

class NonWidgetAppWidgetHostView(context: Context) : AppWidgetHostView(context) {
    private var view: View? = null

    override fun updateAppWidget(remoteViews: RemoteViews) {
        val content: View = remoteViews.apply(context, this) ?: return
        prepareView(content)
        addView(content)
        if (view !== content && view != null) {
            removeView(view)
        }
        view = content
    }

    inline fun <reified T : View> getUnboxedView(): T {
        val boxingView = assertIs<ViewGroup>(getChildAt(0))
        return boxingView.children.single().getTargetView()
    }
}
