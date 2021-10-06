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
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.glance.appwidget.test.R
import androidx.glance.unit.DpSize
import org.junit.Assert.fail
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RequiresApi(26)
class AppWidgetHostTestActivity : Activity() {
    private var mHost: AppWidgetHost? = null
    private val mHostViews = mutableListOf<TestAppWidgetHostView>()

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

    fun bindAppWidget(portraitSize: DpSize, landscapeSize: DpSize): TestAppWidgetHostView {
        val host = mHost ?: error("App widgets can only be bound while the activity is created")

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val appWidgetId = host.allocateAppWidgetId()
        val componentName = ComponentName(this, TestGlanceAppWidgetReceiver::class.java)

        val wasBound = appWidgetManager.bindAppWidgetIdIfAllowed(
            appWidgetId,
            componentName,
            optionsBundleOf(portraitSize, landscapeSize)
        )
        if (!wasBound) {
            fail("Failed to bind the app widget")
        }

        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        val locale = Locale.getDefault()
        val config = resources.configuration
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        val context = this.createConfigurationContext(config)

        val hostView = host.createView(context, appWidgetId, info) as TestAppWidgetHostView
        hostView.setPadding(0, 0, 0, 0)
        val contentFrame = findViewById<FrameLayout>(R.id.content)
        contentFrame.addView(hostView)
        hostView.setSizes(portraitSize, landscapeSize)
        hostView.setBackgroundColor(Color.WHITE)
        mHostViews += hostView
        return hostView
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.i("YOLO", "Changing configuration, orientation = ${newConfig.orientation}")
        super.onConfigurationChanged(newConfig)
        updateAllSizes(newConfig.orientation)
        reapplyRemoteViews()
    }

    fun updateAllSizes(orientation: Int) {
        mHostViews.forEach { it.updateSize(orientation) }
    }

    fun reapplyRemoteViews() {
        mHostViews.forEach { it.reapplyRemoteViews() }
    }
}

@RequiresApi(26)
class TestAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView = TestAppWidgetHostView(context)
}

@RequiresApi(26)
class TestAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    init {
        // Prevent asynchronous inflation of the App Widget
        setExecutor(null)
        layoutDirection = View.LAYOUT_DIRECTION_LOCALE
    }

    private var mLatch: CountDownLatch? = null
    private var mRemoteViews: RemoteViews? = null
    private lateinit var mPortraitSize: DpSize
    private lateinit var mLandscapeSize: DpSize

    /**
     * Wait for the new remote views to be received. If a remote views was already received, return
     * immediately.
     */
    fun waitForRemoteViews() {
        synchronized(this) {
            mRemoteViews?.let { return }
            mLatch = CountDownLatch(1)
        }
        val result = mLatch?.await(5, TimeUnit.SECONDS)!!
        require(result) { "Timeout before getting RemoteViews" }
    }

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        super.updateAppWidget(remoteViews)
        synchronized(this) {
            mRemoteViews = remoteViews
            if (remoteViews != null) {
                mLatch?.countDown()
            }
        }
    }

    /** Reset the latch used to detect the arrival of a new RemoteViews. */
    fun resetRemoteViewsLatch() {
        synchronized(this) {
            mRemoteViews = null
            mLatch = null
        }
    }

    fun setSizes(portraitSize: DpSize, landscapeSize: DpSize) {
        mPortraitSize = portraitSize
        mLandscapeSize = landscapeSize
        updateSize(resources.configuration.orientation)
    }

    fun updateSize(orientation: Int) {
        val size = when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> mLandscapeSize
            Configuration.ORIENTATION_PORTRAIT -> mPortraitSize
            else -> error("Unknown orientation ${context.resources.configuration.orientation}")
        }
        val displayMetrics = resources.displayMetrics
        val width = size.width.toPixels(displayMetrics)
        val height = size.height.toPixels(displayMetrics)
        layoutParams = LayoutParams(width, height, Gravity.CENTER)
        requestLayout()
    }

    fun reapplyRemoteViews() {
        mRemoteViews?.let { super.updateAppWidget(it) }
    }
}
