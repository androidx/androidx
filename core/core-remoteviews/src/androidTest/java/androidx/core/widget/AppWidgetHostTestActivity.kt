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

package androidx.core.widget

import android.annotation.TargetApi
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_SIZES
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.remoteviews.test.R
import org.junit.Assert.fail

/** Test activity that contains an [AppWidgetHost]. */
@TargetApi(29)
public class AppWidgetHostTestActivity : Activity() {
    private var mHost: AppWidgetHost? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.app_widget_host_activity)

        mHost = AppWidgetHost(this, 1).also { it.startListening() }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHost?.stopListening()
        mHost?.deleteHost()
        mHost = null
    }

    public fun bindAppWidget(): AppWidgetHostView {
        val host = mHost ?: error("App widgets can only be bound while the activity is created")

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val appWidgetId = host.allocateAppWidgetId()
        val componentName = ComponentName(this, TestAppWidgetProvider::class.java)

        val wasBound = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, componentName)
        if (!wasBound) {
            fail("Failed to bind the app widget")
        }

        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        val hostView = host.createView(this, appWidgetId, info)
        val contentFrame = findViewById<FrameLayout>(R.id.content)
        contentFrame.addView(
            hostView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        fun pxToDp(px: Int): Int {
            val density = resources.displayMetrics.density
            return (px / density).toInt()
        }

        val width = pxToDp(contentFrame.width)
        val height = pxToDp(contentFrame.height)
        val optionsBundle = Bundle()
        optionsBundle.putInt(OPTION_APPWIDGET_MIN_WIDTH, width)
        optionsBundle.putInt(OPTION_APPWIDGET_MAX_WIDTH, width)
        optionsBundle.putInt(OPTION_APPWIDGET_MIN_HEIGHT, height)
        optionsBundle.putInt(OPTION_APPWIDGET_MAX_HEIGHT, height)
        if (Build.VERSION.SDK_INT >= 31) {
            optionsBundle.putParcelableArrayList(
                OPTION_APPWIDGET_SIZES,
                arrayListOf(SizeF(width.toFloat(), height.toFloat()))
            )
        }

        appWidgetManager.updateAppWidgetOptions(appWidgetId, optionsBundle)

        return hostView
    }
}
