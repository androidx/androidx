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

package androidx.glance.appwidget.macrobenchmark

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.util.DisplayMetrics
import android.util.Log
import android.util.SizeF
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.fail

@RequiresApi(26)
class AppWidgetHostTestActivity : Activity() {
    private var mHost: AppWidgetHost? = null
    private val mHostViews = mutableListOf<TestAppWidgetHostView>()

    companion object {
        const val EXTRA_TARGET_RECEIVER = "targetReceiver"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(android.R.layout.list_content)

        mHost = TestAppWidgetHost(this, 1025).also {
            it.appWidgetIds.forEach(it::deleteAppWidgetId)
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

    private fun rootView() =
        this.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as ViewGroup

    fun bindAppWidget(portraitSize: DpSize, landscapeSize: DpSize): TestAppWidgetHostView? {
        val host = mHost ?: error("App widgets can only be bound while the activity is created")

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val appWidgetId = host.allocateAppWidgetId()

        @Suppress("DEPRECATION")
        val componentName = intent.getParcelableExtra<ComponentName>(EXTRA_TARGET_RECEIVER)!!

        val wasBound = appWidgetManager.bindAppWidgetIdIfAllowed(
            appWidgetId,
            componentName,
            optionsBundleOf(listOf(portraitSize, landscapeSize))
        )
        if (!wasBound) {
            fail("Failed to bind the app widget")
            mHost?.deleteHost()
            mHost = null
            return null
        }

        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        val locale = Locale.getDefault()
        val config = resources.configuration
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        val context = this.createConfigurationContext(config)

        val hostView = host.createView(context, appWidgetId, info) as TestAppWidgetHostView
        hostView.setPadding(0, 0, 0, 0)
        rootView().addView(hostView)
        hostView.setSizes(portraitSize, landscapeSize)
        hostView.setBackgroundColor(Color.WHITE)
        mHostViews += hostView
        return hostView
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
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
        layoutDirection = View.LAYOUT_DIRECTION_LOCALE
    }

    private val remoteViews = MutableStateFlow<RemoteViews?>(null)
    private var mPortraitSize: DpSize = DpSize(0.dp, 0.dp)
    private var mLandscapeSize: DpSize = DpSize(0.dp, 0.dp)

    /**
     * Wait for the new remote views to be received. If a remote views was already received, return
     * immediately.
     */
    suspend fun waitForRemoteViews() {
        remoteViews.first { it != null }
    }

    suspend fun runAndWaitForRemoteViews(block: () -> Unit) {
        remoteViews.value = null
        block()
        waitForRemoteViews()
    }

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        super.updateAppWidget(remoteViews)
        this.remoteViews.value = remoteViews
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

    private fun Dp.toPixels(displayMetrics: DisplayMetrics) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics).toInt()

    fun reapplyRemoteViews() {
        remoteViews.value?.let { super.updateAppWidget(it) }
    }
}

fun optionsBundleOf(sizes: List<DpSize>): Bundle {
    require(sizes.isNotEmpty()) { "There must be at least one size" }
    val (minSize, maxSize) = sizes.fold(sizes[0] to sizes[0]) { acc, s ->
        DpSize(min(acc.first.width, s.width), min(acc.first.height, s.height)) to
            DpSize(max(acc.second.width, s.width), max(acc.second.height, s.height))
    }
    return Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minSize.width.value.toInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minSize.height.value.toInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxSize.width.value.toInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxSize.height.value.toInt())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sizeList = ArrayList<SizeF>(sizes.map { SizeF(it.width.value, it.height.value) })
            putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizeList)
        }
    }
}
