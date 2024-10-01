/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.window.layout.util

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.WindowManager
import android.view.WindowMetrics as AndroidWindowMetrics
import androidx.annotation.RequiresApi
import androidx.annotation.UiContext
import androidx.window.core.Bounds
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.util.ContextCompatHelper.unwrapUiContext
import androidx.window.layout.util.DisplayHelper.getRealSizeForDisplay

/** Provides compatibility behavior for functionality related to [WindowMetricsCalculator]. */
internal interface WindowMetricsCompatHelper {

    /** Translates platform [AndroidWindowMetrics] to jetpack [WindowMetrics]. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun translateWindowMetrics(windowMetrics: AndroidWindowMetrics, density: Float): WindowMetrics

    /** Returns the [WindowMetrics] associated with the provided [Context]. */
    fun currentWindowMetrics(
        @UiContext context: Context,
        densityCompatHelper: DensityCompatHelper
    ): WindowMetrics

    /** Returns the [WindowMetrics] associated with the provided [Activity]. */
    fun currentWindowMetrics(
        activity: Activity,
        densityCompatHelper: DensityCompatHelper
    ): WindowMetrics

    /** Returns the maximum [WindowMetrics] for a given [UiContext]. */
    fun maximumWindowMetrics(
        @UiContext context: Context,
        densityCompatHelper: DensityCompatHelper
    ): WindowMetrics

    companion object {
        fun getInstance(): WindowMetricsCompatHelper {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    WindowMetricsCompatHelperApi34Impl
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> WindowMetricsCompatHelperApi30Impl
                else -> WindowMetricsCompatHelperBaseImpl
            }
        }
    }
}

internal object WindowMetricsCompatHelperBaseImpl : WindowMetricsCompatHelper {

    override fun translateWindowMetrics(
        windowMetrics: AndroidWindowMetrics,
        density: Float
    ): WindowMetrics {
        throw UnsupportedOperationException("translateWindowMetrics not available before API30")
    }

    override fun currentWindowMetrics(
        @UiContext context: Context,
        densityCompatHelper: DensityCompatHelper
    ): WindowMetrics {
        when (val unwrappedContext = unwrapUiContext(context)) {
            is Activity -> {
                return currentWindowMetrics(unwrappedContext, densityCompatHelper)
            }
            is InputMethodService -> {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                // On older SDK levels, the app and IME could show up on different displays.
                // However, there isn't a way for us to figure this out from the application
                // layer. But, this should be good enough for now given the small likelihood of
                // IMEs showing up on non-primary displays on these SDK levels.
                @Suppress("DEPRECATION") val displaySize = getRealSizeForDisplay(wm.defaultDisplay)

                // IME occupies the whole display bounds.
                val imeBounds = Rect(0, 0, displaySize.x, displaySize.y)
                return WindowMetrics(imeBounds, density = densityCompatHelper.density(context))
            }
            else -> {
                throw IllegalArgumentException("$context is not a UiContext")
            }
        }
    }

    override fun currentWindowMetrics(
        activity: Activity,
        densityCompatHelper: DensityCompatHelper
    ): WindowMetrics {
        // TODO (b/233899790): compute insets for other platform versions below R
        return WindowMetrics(
            Bounds(BoundsHelper.getInstance().currentWindowBounds(activity)),
            densityCompatHelper.density(activity)
        )
    }

    override fun maximumWindowMetrics(
        @UiContext context: Context,
        densityCompatHelper: DensityCompatHelper
    ): WindowMetrics {
        // TODO (b/233899790): compute insets for other platform versions below Rs
        return WindowMetrics(
            Bounds(BoundsHelper.getInstance().maximumWindowBounds(context)),
            densityCompatHelper.density(context)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.R)
internal object WindowMetricsCompatHelperApi30Impl : WindowMetricsCompatHelper {

    override fun translateWindowMetrics(
        windowMetrics: AndroidWindowMetrics,
        density: Float
    ): WindowMetrics {
        return WindowMetrics(windowMetrics.bounds, density)
    }

    override fun currentWindowMetrics(
        @UiContext context: Context,
        densityCompatHelper: DensityCompatHelper
    ): WindowMetrics {
        val wm = context.getSystemService(WindowManager::class.java)
        val density = context.resources.displayMetrics.density
        return WindowMetrics(wm.currentWindowMetrics.bounds, density)
    }

    override fun currentWindowMetrics(
        activity: Activity,
        densityCompatHelper: DensityCompatHelper
    ): WindowMetrics {
        return WindowMetrics(
            Bounds(BoundsHelper.getInstance().currentWindowBounds(activity)),
            densityCompatHelper.density(activity)
        )
    }

    override fun maximumWindowMetrics(
        @UiContext context: Context,
        densityCompatHelper: DensityCompatHelper
    ): WindowMetrics {
        return WindowMetrics(
            Bounds(BoundsHelper.getInstance().maximumWindowBounds(context)),
            densityCompatHelper.density(context)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal object WindowMetricsCompatHelperApi34Impl : WindowMetricsCompatHelper {

    override fun translateWindowMetrics(
        windowMetrics: AndroidWindowMetrics,
        density: Float
    ): WindowMetrics {
        return WindowMetrics(windowMetrics.bounds, windowMetrics.density)
    }

    override fun currentWindowMetrics(
        @UiContext context: Context,
        densityCompatHelper: DensityCompatHelper
    ): WindowMetrics {
        val wm = context.getSystemService(WindowManager::class.java)
        return WindowMetrics(wm.currentWindowMetrics.bounds, wm.currentWindowMetrics.density)
    }

    override fun currentWindowMetrics(
        activity: Activity,
        densityCompatHelper: DensityCompatHelper
    ): WindowMetrics {
        return WindowMetricsCompatHelperApi30Impl.currentWindowMetrics(
            activity,
            densityCompatHelper
        )
    }

    override fun maximumWindowMetrics(
        @UiContext context: Context,
        densityCompatHelper: DensityCompatHelper
    ): WindowMetrics {
        return WindowMetricsCompatHelperApi30Impl.maximumWindowMetrics(context, densityCompatHelper)
    }
}
