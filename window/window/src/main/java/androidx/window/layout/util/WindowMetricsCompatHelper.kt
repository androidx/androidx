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

import android.content.Context
import android.os.Build
import android.view.WindowManager
import android.view.WindowMetrics as AndroidWindowMetrics
import androidx.annotation.RequiresApi
import androidx.annotation.UiContext
import androidx.core.view.WindowInsetsCompat
import androidx.window.layout.WindowMetrics

/**
 * Provides compatibility behavior for functionality related to [AndroidWindowMetrics].
 */
@RequiresApi(Build.VERSION_CODES.R)
internal interface WindowMetricsCompatHelper {

    /**
     * Translates platform [AndroidWindowMetrics] to jetpack [WindowMetrics].
     */
    fun translateWindowMetrics(windowMetrics: AndroidWindowMetrics, density: Float): WindowMetrics

    /**
     * Returns the [WindowMetrics] associated with the provided [Context].
     */
    fun currentWindowMetrics(@UiContext context: Context): WindowMetrics

    companion object {
        fun getInstance(): WindowMetricsCompatHelper {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    WindowMetricsCompatHelperApi34Impl()
                else ->
                    WindowMetricsCompatHelperBaseImpl()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private open class WindowMetricsCompatHelperBaseImpl : WindowMetricsCompatHelper {

    override fun translateWindowMetrics(
        windowMetrics: AndroidWindowMetrics,
        density: Float
    ): WindowMetrics {
        return WindowMetrics(
            windowMetrics.bounds,
            WindowInsetsCompat.toWindowInsetsCompat(windowMetrics.windowInsets),
            density
        )
    }

    override fun currentWindowMetrics(@UiContext context: Context): WindowMetrics {
        val wm = context.getSystemService(WindowManager::class.java)
        val insets = WindowInsetsCompat.toWindowInsetsCompat(wm.currentWindowMetrics.windowInsets)
        val density = context.resources.displayMetrics.density
        return WindowMetrics(wm.currentWindowMetrics.bounds, insets, density)
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private open class WindowMetricsCompatHelperApi34Impl : WindowMetricsCompatHelperBaseImpl() {

    override fun translateWindowMetrics(
        windowMetrics: AndroidWindowMetrics,
        density: Float
    ): WindowMetrics {
        return WindowMetrics(
            windowMetrics.bounds,
            WindowInsetsCompat.toWindowInsetsCompat(windowMetrics.windowInsets),
            windowMetrics.density
        )
    }

    override fun currentWindowMetrics(@UiContext context: Context): WindowMetrics {
        val wm = context.getSystemService(WindowManager::class.java)
        return WindowMetrics(wm.currentWindowMetrics.bounds,
            WindowInsetsCompat.toWindowInsetsCompat(wm.currentWindowMetrics.windowInsets),
            wm.currentWindowMetrics.density)
    }
}
