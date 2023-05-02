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

package androidx.window.layout

import android.view.WindowMetrics as AndroidWindowMetrics
import android.app.Activity
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.Display
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiContext
import androidx.core.view.WindowInsetsCompat

/**
 * An interface to calculate the [WindowMetrics] for an [Activity] or a [UiContext].
 */
interface WindowMetricsCalculator {

    /**
     * Computes the size and position of the area the window would occupy with
     * [MATCH_PARENT][android.view.WindowManager.LayoutParams.MATCH_PARENT] width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     *
     * For example, [android.view.WindowManager.LayoutParams.layoutInDisplayCutoutMode] set to
     * [android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS] or the
     * [android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS] flag set.
     *
     * The value returned from this method may be different from platform API(s) used to determine
     * the size and position of the visible area a given context occupies. For example:
     *
     *  * [Display.getSize] can be used to determine the size of the visible area
     * a window occupies, but may be subtracted to exclude certain system decorations that
     * always appear on screen, notably the navigation bar.
     *  * The decor view's [android.view.View#getWidth] and [android.view.View@getHeight] can be
     *  used to determine the size of the top level view in the view hierarchy, but this size is
     * determined through a combination of [android.view.WindowManager.LayoutParams]
     * flags and may not represent the true window size. For example, a window that does not
     * indicate it can be displayed behind a display cutout will have the size of the decor
     * view offset to exclude this region unless this region overlaps with the status bar, while
     * the value returned from this method will include this region.
     *
     * The value returned from this method is guaranteed to be correct on platforms
     * [Q][Build.VERSION_CODES.Q] and above. For older platforms the value may be invalid if
     * the activity is in multi-window mode or if the navigation bar offset can not be accounted
     * for, though a best effort is made to ensure the returned value is as close as possible to
     * the true value. See [.computeWindowBoundsP] and
     * [.computeWindowBoundsN].
     *
     * Note: The value of this is based on the last windowing state reported to the client.
     *
     * @see android.view.WindowManager.getCurrentWindowMetrics
     * @see android.view.WindowMetrics.getBounds
     */
    fun computeCurrentWindowMetrics(activity: Activity): WindowMetrics

    /**
     * Computes the size and position of the area the window would occupy with
     * [MATCH_PARENT][android.view.WindowManager.LayoutParams.MATCH_PARENT] width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     *
     * On [Build.VERSION_CODES.Q] and older, a [UiContext] is either an [Activity] or an
     * [InputMethodService]. On [Build.VERSION_CODES.R] and newer, a [UiContext] can also be one
     * created via the [Context.createWindowContext] APIs.
     *
     * @see [computeCurrentWindowMetrics]
     * @throws NotImplementedError if not implemented. The default implementation from [getOrCreate]
     * is guaranteed to implement this method.
     */
    fun computeCurrentWindowMetrics(@UiContext context: Context): WindowMetrics {
        throw NotImplementedError("Must override computeCurrentWindowMetrics(context) and" +
            " provide an implementation.")
    }

    /**
     * Computes the maximum size and position of the area the window can expect with
     * [MATCH_PARENT][android.view.WindowManager.LayoutParams.MATCH_PARENT] width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     *
     * The value returned from this method will always match [Display.getRealSize] on
     * [Android 10][Build.VERSION_CODES.Q] and below.
     *
     * @see android.view.WindowManager.getMaximumWindowMetrics
     */
    fun computeMaximumWindowMetrics(activity: Activity): WindowMetrics

    /**
     * Computes the maximum size and position of the area the window can expect with
     * [MATCH_PARENT][android.view.WindowManager.LayoutParams.MATCH_PARENT] width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     *
     * The value returned from this method will always match [Display.getRealSize] on
     * [Android 10][Build.VERSION_CODES.Q] and below.
     *
     * On [Build.VERSION_CODES.Q] and older, a [UiContext] is either an [Activity] or an
     * [InputMethodService]. On [Build.VERSION_CODES.R] and newer, a [UiContext] can also be one
     * created via the [Context.createWindowContext] APIs.
     *
     * @see [computeMaximumWindowMetrics]
     * @throws NotImplementedError if not implemented. The default implementation from [getOrCreate]
     * is guaranteed to implement this method.
     */
    fun computeMaximumWindowMetrics(@UiContext context: Context): WindowMetrics {
        throw NotImplementedError("Must override computeMaximumWindowMetrics(context) and" +
            " provide an implementation.")
    }

    companion object {

        private var decorator: (WindowMetricsCalculator) -> WindowMetricsCalculator =
            { it }

        @JvmStatic
        fun getOrCreate(): WindowMetricsCalculator {
            return decorator(WindowMetricsCalculatorCompat)
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun overrideDecorator(overridingDecorator: WindowMetricsCalculatorDecorator) {
            decorator = overridingDecorator::decorate
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun reset() {
            decorator = { it }
        }

        /**
         * Converts [Android API WindowMetrics][AndroidWindowMetrics] to
         * [Jetpack version WindowMetrics][WindowMetrics]
         */
        @Suppress("ClassVerificationFailure")
        @RequiresApi(Build.VERSION_CODES.R)
        internal fun translateWindowMetrics(windowMetrics: AndroidWindowMetrics): WindowMetrics =
            WindowMetrics(
                windowMetrics.bounds,
                WindowInsetsCompat.toWindowInsetsCompat(windowMetrics.windowInsets)
            )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface WindowMetricsCalculatorDecorator {

    /**
     * Returns an instance of [WindowMetricsCalculator]
     */
    fun decorate(calculator: WindowMetricsCalculator): WindowMetricsCalculator
}