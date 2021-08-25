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

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RemoteViews
import org.junit.Assert.fail
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import androidx.glance.appwidget.test.R
import androidx.glance.unit.DpSize

class AppWidgetHostTestActivity : Activity() {
    private var mHost: AppWidgetHost? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.app_widget_host_activity)

        mHost = TestAppWidgetHost(this, 1025).also {
            it.startListening()
        }
    }

    override fun onDestroy() {
        try {
            mHost?.stopListening()
            mHost?.deleteHost()
        } catch (ex: Throwable) {
            Log.w("AppWidgetHostTestActivity", "Error stopping the AppWidget Host", ex)
        }
        mHost = null
        super.onDestroy()
    }

    fun bindAppWidget(size: DpSize): TestAppWidgetHostView {
        val host = mHost ?: error("App widgets can only be bound while the activity is created")

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val appWidgetId = host.allocateAppWidgetId()
        val componentName = ComponentName(this, TestGlanceAppWidgetReceiver::class.java)

        val wasBound = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, componentName)
        if (!wasBound) {
            fail("Failed to bind the app widget")
        }

        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        val hostView = host.createView(this, appWidgetId, info) as TestAppWidgetHostView
        val contentFrame = findViewById<FrameLayout>(R.id.content)
        val displayMetrics = resources.displayMetrics
        val width = size.width.toPixels(displayMetrics)
        val height = size.height.toPixels(displayMetrics)
        contentFrame.addView(hostView, FrameLayout.LayoutParams(width, height, Gravity.CENTER))
        hostView.setBackgroundColor(Color.WHITE)

        return hostView
    }
}

class TestAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView = TestAppWidgetHostView(context)
}

class TestAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    val latch = AtomicReference<CountDownLatch?>(null)

    fun waitForRemoteViews() {
        latch.set(CountDownLatch(1))
        val result = latch.get()?.await(5, TimeUnit.SECONDS)!!
        latch.set(null)
        require(result) { "Timeout before getting RemoteViews" }
    }

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        super.updateAppWidget(remoteViews)
        if (remoteViews != null) {
            latch.get()?.countDown()
        }
    }
}
