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
import android.graphics.Rect
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.test.R
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.fail

private const val TAG = "AppWidgetHostTestActivity"

@RequiresApi(26)
class AppWidgetHostTestActivity : Activity() {
    private var mHost: AppWidgetHost? = null
    private val mHostViews = mutableListOf<TestAppWidgetHostView>()
    private var mConfigurationChanged: CountDownLatch? = null
    private var mLastConfiguration: Configuration? = null
    val lastConfiguration: Configuration
        get() = synchronized(this) { mLastConfiguration!! }

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
        } catch (ex: Throwable) {
            Log.w(TAG, "Error stopping listening", ex)
        }
        try {
            mHost?.deleteHost()
        } catch (t: Throwable) {
            Log.w(TAG, "Error deleting Host", t)
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
            optionsBundleOf(listOf(portraitSize, landscapeSize))
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
        val contentFrame = findViewById<FrameLayout>(R.id.content)
        contentFrame.addView(hostView)
        hostView.setSizes(portraitSize, landscapeSize)
        hostView.setBackgroundColor(Color.WHITE)
        mHostViews += hostView
        return hostView
    }

    fun deleteAppWidget(hostView: TestAppWidgetHostView) {
        val appWidgetId = hostView.appWidgetId
        mHost?.deleteAppWidgetId(appWidgetId)
        mHostViews.remove(hostView)
        val contentFrame = findViewById<FrameLayout>(R.id.content)
        contentFrame.removeView(hostView)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mHostViews.forEach {
            it.updateSize(newConfig.orientation)
            it.reapplyRemoteViews()
        }
        synchronized(this) {
            mLastConfiguration = newConfig
            mConfigurationChanged?.countDown()
        }
    }

    fun resetConfigurationChangedLatch() {
       synchronized(this) {
           mConfigurationChanged = CountDownLatch(1)
           mLastConfiguration = null
       }
    }

    // This should not be called from the main thread, so that it does not block
    // onConfigurationChanged from being called.
    fun waitForConfigurationChange() {
        val result = mConfigurationChanged?.await(5, TimeUnit.SECONDS)!!
        require(result) { "Timeout before getting configuration" }
    }
}

@RequiresApi(26)
class TestAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView = TestAppWidgetHostView(context)

    override fun onProviderChanged(appWidgetId: Int, appWidget: AppWidgetProviderInfo?) {
        // In tests, we aren't testing anything specific to how widget behaves on PACKAGE_CHANGED.
        // In a few SDK versions (http://shortn/_PpxiDuRnvb, http://shortn/_TysXctaGMI),
        // onProviderChange resets the widget with null value - which happens in middle of test
        // in-deterministically. For example, in local emulators it doesn't get called sometimes.
        // So we override this method to prevent reset.
        // TODO: Make this conditional or find a way to avoid PACKAGE_CHANGED in middle of the test.
        Log.w(TAG, "Ignoring onProviderChanged for $appWidgetId.")
    }
}

@RequiresApi(26)
class TestAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    init {
        // Prevent asynchronous inflation of the App Widget
        setExecutor(null)
        layoutDirection = View.LAYOUT_DIRECTION_LOCALE
    }

    private var mLatch: CountDownLatch? = null
    var mRemoteViews: RemoteViews? = null
        private set
    private var mPortraitSize: DpSize = DpSize(0.dp, 0.dp)
    private var mLandscapeSize: DpSize = DpSize(0.dp, 0.dp)

    /**
     * Wait for the new remote views to be received. If a remote views was already received, return
     * immediately.
     */
    fun waitForRemoteViews() {
        synchronized(this) {
            mRemoteViews?.let { return }
            mLatch = CountDownLatch(1)
        }
        val result = mLatch?.await(30, TimeUnit.SECONDS)!!
        require(result) { "Timeout before getting RemoteViews" }
    }

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        if (VERBOSE_LOG) {
            Log.d(RECEIVER_TEST_TAG, "updateAppWidget() called with: $remoteViews")
        }

        super.updateAppWidget(remoteViews)
        synchronized(this) {
            mRemoteViews = remoteViews
            if (remoteViews != null) {
                mLatch?.countDown()
            }
        }
    }

    override fun prepareView(view: View?) {
        if (VERBOSE_LOG) {
            Log.d(RECEIVER_TEST_TAG, "prepareView() called with: view = $view")
        }

        super.prepareView(view)
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

        // The widget host applies a default padding that is difficult to remove. Make the outer
        // host view bigger by the default padding amount, so that the inner view that we care about
        // matches the provided size.
        val hostViewPadding = Rect()
        val testComponent =
            ComponentName(context.applicationContext, TestGlanceAppWidgetReceiver::class.java)
        getDefaultPaddingForWidget(context, testComponent, hostViewPadding)
        val paddedWidth = width + hostViewPadding.left + hostViewPadding.right
        val paddedHeight = height + hostViewPadding.top + hostViewPadding.bottom

        layoutParams = LayoutParams(paddedWidth, paddedHeight, Gravity.CENTER)
        requestLayout()
    }

    fun reapplyRemoteViews() {
        mRemoteViews?.let { super.updateAppWidget(it) }
    }
}
