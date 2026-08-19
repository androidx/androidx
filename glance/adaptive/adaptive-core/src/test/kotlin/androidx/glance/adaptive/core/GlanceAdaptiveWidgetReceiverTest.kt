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

package androidx.glance.adaptive.core

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class GlanceAdaptiveWidgetReceiverTest {

    private class TestReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetId: String = "test_widget_id"

        val onUpdateCalled = CompletableDeferred<Boolean>()

        override suspend fun onUpdate(context: Context) {
            onUpdateCalled.complete(true)
        }
    }

    private class DefaultReceiver : GlanceAdaptiveWidgetReceiver() {
        override val widgetId: String = "default_widget_id"
    }

    @Test
    fun widgetId_returnsConfiguredValue() {
        val receiver = TestReceiver()
        assertThat(receiver.widgetId).isEqualTo("test_widget_id")
    }

    @Test
    fun onUpdate_invokesSuspendOnUpdateWhenAppWidgetUpdateReceived() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = TestReceiver()
        val appWidgetIds = intArrayOf(1, 2, 3)

        val intent =
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }

        receiver.onReceive(context, intent)

        val called = receiver.onUpdateCalled.await()
        assertThat(called).isTrue()
    }

    @Test
    fun defaultReceiver_onUpdateDoesNotThrow() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = DefaultReceiver()
        receiver.onUpdate(context)
    }

    @Test
    fun onUpdate_directInvocationTriggersSuspendOnUpdate() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = TestReceiver()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = intArrayOf(10, 20)

        receiver.onUpdate(context, appWidgetManager, appWidgetIds)

        val called = receiver.onUpdateCalled.await()
        assertThat(called).isTrue()
    }
}
