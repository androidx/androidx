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

package androidx.window.layout.util

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.WindowManager
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.UiContext
import androidx.core.view.WindowInsetsCompat
import androidx.window.layout.WindowMetrics

@RequiresApi(Build.VERSION_CODES.N)
internal object ContextCompatHelperApi24 {
    fun isInMultiWindowMode(activity: Activity): Boolean {
        return activity.isInMultiWindowMode
    }
}

@RequiresApi(Build.VERSION_CODES.R)
internal object ContextCompatHelperApi30 {

    fun currentWindowMetrics(@UiContext context: Context): WindowMetrics {
        val wm = context.getSystemService(WindowManager::class.java)
        val insets = WindowInsetsCompat.toWindowInsetsCompat(wm.currentWindowMetrics.windowInsets)
        return WindowMetrics(wm.currentWindowMetrics.bounds, insets)
    }

    fun currentWindowBounds(@UiContext context: Context): Rect {
        val wm = context.getSystemService(WindowManager::class.java)
        return wm.currentWindowMetrics.bounds
    }

    fun currentWindowInsets(@UiContext context: Context): WindowInsetsCompat {
        val wm = context.getSystemService(WindowManager::class.java)
        return WindowInsetsCompat.toWindowInsetsCompat(wm.currentWindowMetrics.windowInsets)
    }

    fun maximumWindowBounds(@UiContext context: Context): Rect {
        val wm = context.getSystemService(WindowManager::class.java)
        return wm.maximumWindowMetrics.bounds
    }

    /**
     * Computes the [WindowInsetsCompat] for platforms above [Build.VERSION_CODES.R], inclusive.
     * @DoNotInline required for implementation-specific class method to prevent it from being
     * inlined.
     *
     * @see androidx.window.layout.WindowMetrics.getWindowInsets
     */
    @DoNotInline
    fun currentWindowInsets(activity: Activity): WindowInsetsCompat {
        val platformInsets = activity.windowManager.currentWindowMetrics.windowInsets
        return WindowInsetsCompat.toWindowInsetsCompat(platformInsets)
    }
}