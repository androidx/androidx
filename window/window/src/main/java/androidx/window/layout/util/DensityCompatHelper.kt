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
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.WindowMetrics as AndroidWindowMetrics
import androidx.annotation.RequiresApi
import androidx.annotation.UiContext

/** Provides compatibility behavior for functionality related to display density. */
internal interface DensityCompatHelper {

    /** Returns the logical density of the display associated with the [Context]. */
    fun density(context: Context): Float

    /**
     * Returns the logical density of the display associated with the [Configuration] or
     * [AndroidWindowMetrics], depending on the SDK level.
     */
    fun density(configuration: Configuration, windowMetrics: AndroidWindowMetrics): Float

    companion object {
        fun getInstance(): DensityCompatHelper {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    DensityCompatHelperApi34Impl
                else -> DensityCompatHelperBaseImpl
            }
        }
    }
}

private object DensityCompatHelperBaseImpl : DensityCompatHelper {
    override fun density(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    override fun density(configuration: Configuration, windowMetrics: AndroidWindowMetrics): Float {
        return configuration.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private object DensityCompatHelperApi34Impl : DensityCompatHelper {
    override fun density(@UiContext context: Context): Float {
        return context.getSystemService(WindowManager::class.java).currentWindowMetrics.density
    }

    override fun density(configuration: Configuration, windowMetrics: AndroidWindowMetrics): Float {
        return windowMetrics.density
    }
}
