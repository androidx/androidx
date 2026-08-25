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

package androidx.compose.remote.creation.compose.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], shadows = [RemoteComposeWidgetTest.TestShadowAppWidgetManager::class])
class RemoteComposeWidgetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Implements(AppWidgetManager::class)
    class TestShadowAppWidgetManager {
        companion object {
            val updatedWidgets = mutableMapOf<Int, RemoteViews>()

            fun reset() {
                updatedWidgets.clear()
            }
        }

        @Implementation
        fun updateAppWidget(appWidgetId: Int, views: RemoteViews?) {
            if (views != null) {
                updatedWidgets[appWidgetId] = views
            }
        }
    }

    class TestCounterWidget : RemoteComposeWidget() {
        companion object {
            var counter: Int = 0
        }

        @RemoteComposable
        @Composable
        override fun Content(context: Context, widgetId: Int) {
            val count = counter
            RemoteBox {
                RemoteText("Count: $count".rs)
                RemoteBox(modifier = RemoteModifier.onClick { counter++ }) { RemoteText("+".rs) }
            }
        }
    }

    @Test
    fun testOnUpdate_createsRemoteView() {
        TestShadowAppWidgetManager.reset()
        TestCounterWidget.counter = 0
        val widget = TestCounterWidget()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetId = 42

        widget.onUpdate(context, appWidgetManager, intArrayOf(widgetId))

        // WidgetLambdaAction counter should reflect the button in Content
        assertThat(WidgetLambdaAction.counter).isEqualTo(1)
        // Verify updateAppWidget was called
        assertThat(TestShadowAppWidgetManager.updatedWidgets).containsKey(widgetId)
    }

    @Test
    fun testOnReceive_executesCallbackAndUpdateWidget() {
        TestShadowAppWidgetManager.reset()
        TestCounterWidget.counter = 0
        val widget = TestCounterWidget()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetId = 42

        // Initial update
        widget.onUpdate(context, appWidgetManager, intArrayOf(widgetId))
        assertThat(TestCounterWidget.counter).isEqualTo(0)
        assertThat(TestShadowAppWidgetManager.updatedWidgets).containsKey(widgetId)

        // Clear sAppWidgetIds to simulate process recreation before broadcast
        RemoteComposeWidget.sAppWidgetIds = null

        // Simulate click broadcast intent
        val clickIntent =
            Intent(context, TestCounterWidget::class.java).apply {
                action = AbstractRCWidget.ACTION
                putExtra("id", 1000 * widgetId + 0)
                putExtra("widgetId", widgetId)
            }

        widget.onReceive(context, clickIntent)

        // The callback should have executed and incremented counter
        assertThat(TestCounterWidget.counter).isEqualTo(1)
        assertThat(TestShadowAppWidgetManager.updatedWidgets).containsKey(widgetId)

        // Second click
        widget.onReceive(context, clickIntent)
        assertThat(TestCounterWidget.counter).isEqualTo(2)
    }
}
