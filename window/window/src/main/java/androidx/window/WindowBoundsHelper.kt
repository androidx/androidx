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
package androidx.window

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Build.VERSION_CODES
import android.util.Log
import android.view.Display
import android.view.DisplayCutout
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.annotation.VisibleForTesting
import androidx.window.ActivityCompatHelperApi24.isInMultiWindowMode
import androidx.window.ActivityCompatHelperApi30.currentWindowBounds
import androidx.window.ActivityCompatHelperApi30.maximumWindowBounds
import androidx.window.DisplayCompatHelperApi17.getRealSize
import androidx.window.DisplayCompatHelperApi28.safeInsetBottom
import androidx.window.DisplayCompatHelperApi28.safeInsetLeft
import androidx.window.DisplayCompatHelperApi28.safeInsetRight
import androidx.window.DisplayCompatHelperApi28.safeInsetTop
import java.lang.reflect.InvocationTargetException

/**
 * Helper class used to compute window bounds across Android versions. Obtain an instance with
 * [.getInstance].
 */
internal open class WindowBoundsHelper {
    /**
     * Computes the size and position of the area the window would occupy with
     * [MATCH_PARENT][android.view.WindowManager.LayoutParams.MATCH_PARENT] width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     *
     *
     * For example, [android.view.WindowManager.LayoutParams.layoutInDisplayCutoutMode] set to
     * [android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS] or the
     * [android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS] flag set.
     *
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
     *
     *
     * The value returned from this method is guaranteed to be correct on platforms
     * [Q][Build.VERSION_CODES.Q] and above. For older platforms the value may be invalid if
     * the activity is in multi-window mode or if the navigation bar offset can not be accounted
     * for, though a best effort is made to ensure the returned value is as close as possible to
     * the true value. See [.computeWindowBoundsP] and
     * [.computeWindowBoundsN].
     *
     *
     * Note: The value of this is based on the last windowing state reported to the client.
     *
     * @see android.view.WindowManager.getCurrentWindowMetrics
     * @see android.view.WindowMetrics.getBounds
     */
    @RestrictTo(LIBRARY)
    public open fun computeCurrentWindowBounds(activity: Activity): Rect {
        return if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
            currentWindowBounds(activity)
        } else if (Build.VERSION.SDK_INT >= VERSION_CODES.Q) {
            computeWindowBoundsQ(activity)
        } else if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
            computeWindowBoundsP(activity)
        } else if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
            computeWindowBoundsN(activity)
        } else {
            computeWindowBoundsIceCreamSandwich(activity)
        }
    }

    /**
     * Computes the maximum size and position of the area the window can expect with
     * [MATCH_PARENT][android.view.WindowManager.LayoutParams.MATCH_PARENT] width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     *
     *
     * The value returned from this method will always match [Display.getRealSize] on
     * [Android 10][Build.VERSION_CODES.Q] and below.
     *
     * @see android.view.WindowManager.getMaximumWindowMetrics
     */
    @RestrictTo(LIBRARY_GROUP)
    public open fun computeMaximumWindowBounds(activity: Activity): Rect {
        return if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
            maximumWindowBounds(activity)
        } else {
            // [WindowManager#getDefaultDisplay] is deprecated but we have this for
            // compatibility with older versions
            @Suppress("DEPRECATION")
            val display = activity.windowManager.defaultDisplay
            val displaySize = getRealSizeForDisplay(display)
            Rect(0, 0, displaySize.x, displaySize.y)
        }
    }

    internal companion object {
        private const val TAG = "WindowBoundsHelper"
        private val globalInstance = WindowBoundsHelper()
        private var testInstance: WindowBoundsHelper? = null
        @JvmStatic
        val instance: WindowBoundsHelper
            get() = testInstance ?: globalInstance

        @VisibleForTesting
        fun setForTesting(helper: WindowBoundsHelper?) {
            testInstance = helper
        }

        /** Computes the window bounds for [Build.VERSION_CODES.Q].  */
        @SuppressLint("BanUncheckedReflection")
        @RequiresApi(VERSION_CODES.Q)
        internal fun computeWindowBoundsQ(activity: Activity): Rect {
            var bounds: Rect
            val config = activity.resources.configuration
            try {
                val windowConfigField =
                    Configuration::class.java.getDeclaredField("windowConfiguration")
                windowConfigField.isAccessible = true
                val windowConfig = windowConfigField[config]
                val getBoundsMethod = windowConfig.javaClass.getDeclaredMethod("getBounds")
                bounds = Rect(getBoundsMethod.invoke(windowConfig) as Rect)
            } catch (e: NoSuchFieldException) {
                Log.w(TAG, e)
                // If reflection fails for some reason default to the P implementation which still
                // has the ability to account for display cutouts.
                bounds = computeWindowBoundsP(activity)
            } catch (e: NoSuchMethodException) {
                Log.w(TAG, e)
                bounds = computeWindowBoundsP(activity)
            } catch (e: IllegalAccessException) {
                Log.w(TAG, e)
                bounds = computeWindowBoundsP(activity)
            } catch (e: InvocationTargetException) {
                Log.w(TAG, e)
                bounds = computeWindowBoundsP(activity)
            }
            return bounds
        }

        /**
         * Computes the window bounds for [Build.VERSION_CODES.P].
         *
         *
         * NOTE: This method may result in incorrect values if the [android.content.res.Resources]
         * value stored at 'navigation_bar_height' does not match the true navigation bar inset on
         * the window.
         *
         */
        @SuppressLint("BanUncheckedReflection")
        @RequiresApi(VERSION_CODES.P)
        internal fun computeWindowBoundsP(activity: Activity): Rect {
            val bounds = Rect()
            val config = activity.resources.configuration
            try {
                val windowConfigField =
                    Configuration::class.java.getDeclaredField("windowConfiguration")
                windowConfigField.isAccessible = true
                val windowConfig = windowConfigField[config]

                // In multi-window mode we'll use the WindowConfiguration#mBounds property which
                // should match the window size. Otherwise we'll use the mAppBounds property and
                // will adjust it below.
                if (isInMultiWindowMode(activity)) {
                    val getAppBounds = windowConfig.javaClass.getDeclaredMethod("getBounds")
                    bounds.set((getAppBounds.invoke(windowConfig) as Rect))
                } else {
                    val getAppBounds = windowConfig.javaClass.getDeclaredMethod("getAppBounds")
                    bounds.set((getAppBounds.invoke(windowConfig) as Rect))
                }
            } catch (e: NoSuchFieldException) {
                Log.w(TAG, e)
                getRectSizeFromDisplay(activity, bounds)
            } catch (e: NoSuchMethodException) {
                Log.w(TAG, e)
                getRectSizeFromDisplay(activity, bounds)
            } catch (e: IllegalAccessException) {
                Log.w(TAG, e)
                getRectSizeFromDisplay(activity, bounds)
            } catch (e: InvocationTargetException) {
                Log.w(TAG, e)
                getRectSizeFromDisplay(activity, bounds)
            }
            val platformWindowManager = activity.windowManager

            // [WindowManager#getDefaultDisplay] is deprecated but we have this for
            // compatibility with older versions
            @Suppress("DEPRECATION")
            val currentDisplay = platformWindowManager.defaultDisplay
            val realDisplaySize = Point()
            // [Display#getRealSize] is deprecated but we have this for
            // compatibility with older versions
            @Suppress("DEPRECATION")
            getRealSize(display = currentDisplay, point = realDisplaySize)
            if (!isInMultiWindowMode(activity)) {
                // The activity is not in multi-window mode. Check if the addition of the
                // navigation bar size to mAppBounds results in the real display size and if so
                // assume the nav bar height should be added to the result.
                val navigationBarHeight = getNavigationBarHeight(activity)
                if (bounds.bottom + navigationBarHeight == realDisplaySize.y) {
                    bounds.bottom += navigationBarHeight
                } else if (bounds.right + navigationBarHeight == realDisplaySize.x) {
                    bounds.right += navigationBarHeight
                } else if (bounds.left == navigationBarHeight) {
                    bounds.left = 0
                }
            }
            if ((bounds.width() < realDisplaySize.x || bounds.height() < realDisplaySize.y) &&
                !isInMultiWindowMode(activity)
            ) {
                // If the corrected bounds are not the same as the display size and the activity is
                // not in multi-window mode it is possible there are unreported cutouts inset-ing
                // the window depending on the layoutInCutoutMode. Check for them here by getting
                // the cutout from the display itself.
                val displayCutout = getCutoutForDisplay(currentDisplay)
                if (displayCutout != null) {
                    if (bounds.left == safeInsetLeft(displayCutout)) {
                        bounds.left = 0
                    }
                    if (realDisplaySize.x - bounds.right == safeInsetRight(displayCutout)) {
                        bounds.right += safeInsetRight(displayCutout)
                    }
                    if (bounds.top == safeInsetTop(displayCutout)) {
                        bounds.top = 0
                    }
                    if (realDisplaySize.y - bounds.bottom == safeInsetBottom(displayCutout)) {
                        bounds.bottom += safeInsetBottom(displayCutout)
                    }
                }
            }
            return bounds
        }

        private fun getRectSizeFromDisplay(activity: Activity, bounds: Rect) {
            // [WindowManager#getDefaultDisplay] is deprecated but we have this for
            // compatibility with older versions
            @Suppress("DEPRECATION")
            val defaultDisplay = activity.windowManager.defaultDisplay
            // [Display#getRectSize] is deprecated but we have this for
            // compatibility with older versions
            @Suppress("DEPRECATION")
            defaultDisplay.getRectSize(bounds)
        }

        /**
         * Computes the window bounds for platforms between [Build.VERSION_CODES.N]
         * and [Build.VERSION_CODES.O_MR1], inclusive.
         *
         *
         * NOTE: This method may result in incorrect values under the following conditions:
         *
         *  * If the activity is in multi-window mode the origin of the returned bounds will
         * always be anchored at (0, 0).
         *  * If the [android.content.res.Resources] value stored at 'navigation_bar_height' does
         *  not match the true navigation bar size the returned bounds will not take into account
         *  the navigation
         * bar.
         *
         */
        @RequiresApi(VERSION_CODES.N)
        internal fun computeWindowBoundsN(activity: Activity): Rect {
            val bounds = Rect()
            // [WindowManager#getDefaultDisplay] is deprecated but we have this for
            // compatibility with older versions
            @Suppress("DEPRECATION")
            val defaultDisplay = activity.windowManager.defaultDisplay
            // [Display#getRectSize] is deprecated but we have this for
            // compatibility with older versions
            @Suppress("DEPRECATION")
            defaultDisplay.getRectSize(bounds)
            if (!isInMultiWindowMode(activity)) {
                // The activity is not in multi-window mode. Check if the addition of the
                // navigation bar size to Display#getSize() results in the real display size and
                // if so return this value. If not, return the result of Display#getSize().
                val realDisplaySize = getRealSizeForDisplay(defaultDisplay)
                val navigationBarHeight = getNavigationBarHeight(activity)
                if (bounds.bottom + navigationBarHeight == realDisplaySize.y) {
                    bounds.bottom += navigationBarHeight
                } else if (bounds.right + navigationBarHeight == realDisplaySize.x) {
                    bounds.right += navigationBarHeight
                }
            }
            return bounds
        }

        /**
         * Computes the window bounds for platforms between [Build.VERSION_CODES.JELLY_BEAN]
         * and [Build.VERSION_CODES.M], inclusive.
         *
         *
         * Given that multi-window mode isn't supported before N we simply return the real display
         * size which should match the window size of a full-screen app.
         */
        @RequiresApi(VERSION_CODES.ICE_CREAM_SANDWICH)
        internal fun computeWindowBoundsIceCreamSandwich(activity: Activity): Rect {
            // [WindowManager#getDefaultDisplay] is deprecated but we have this for
            // compatibility with older versions
            @Suppress("DEPRECATION")
            val defaultDisplay = activity.windowManager.defaultDisplay
            val realDisplaySize = getRealSizeForDisplay(defaultDisplay)
            val bounds = Rect()
            if (realDisplaySize.x == 0 || realDisplaySize.y == 0) {
                // [Display#getRectSize] is deprecated but we have this for
                // compatibility with older versions
                @Suppress("DEPRECATION")
                defaultDisplay.getRectSize(bounds)
            } else {
                bounds.right = realDisplaySize.x
                bounds.bottom = realDisplaySize.y
            }
            return bounds
        }

        /**
         * Returns the full (real) size of the display, in pixels, without subtracting any window
         * decor or applying any compatibility scale factors.
         *
         *
         * The size is adjusted based on the current rotation of the display.
         *
         * @return a point representing the real display size in pixels.
         *
         * @see Display.getRealSize
         */
        @VisibleForTesting
        @RequiresApi(VERSION_CODES.ICE_CREAM_SANDWICH)
        fun getRealSizeForDisplay(display: Display): Point {
            val size = Point()
            if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                getRealSize(display, size)
            } else {
                try {
                    val getRealSizeMethod = Display::class.java.getDeclaredMethod(
                        "getRealSize",
                        Point::class.java
                    )
                    getRealSizeMethod.isAccessible = true
                    getRealSizeMethod.invoke(display, size)
                } catch (e: NoSuchMethodException) {
                    Log.w(TAG, e)
                } catch (e: IllegalAccessException) {
                    Log.w(TAG, e)
                } catch (e: InvocationTargetException) {
                    Log.w(TAG, e)
                }
            }
            return size
        }

        /**
         * Returns the [android.content.res.Resources] value stored as 'navigation_bar_height'.
         *
         *
         * Note: This is error-prone and is **not** the recommended way to determine the size
         * of the overlapping region between the navigation bar and a given window. The best
         * approach is to acquire the [android.view.WindowInsets].
         */
        private fun getNavigationBarHeight(context: Context): Int {
            val resources = context.resources
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId)
            } else 0
        }

        /**
         * Returns the [DisplayCutout] for the given display. Note that display cutout returned
         * here is for the display and the insets provided are in the display coordinate system.
         *
         * @return the display cutout for the given display.
         */
        @SuppressLint("BanUncheckedReflection")
        @RequiresApi(VERSION_CODES.P)
        private fun getCutoutForDisplay(display: Display): DisplayCutout? {
            var displayCutout: DisplayCutout? = null
            try {
                val displayInfoClass = Class.forName("android.view.DisplayInfo")
                val displayInfoConstructor = displayInfoClass.getConstructor()
                displayInfoConstructor.isAccessible = true
                val displayInfo = displayInfoConstructor.newInstance()
                val getDisplayInfoMethod = display.javaClass.getDeclaredMethod(
                    "getDisplayInfo", displayInfo.javaClass
                )
                getDisplayInfoMethod.isAccessible = true
                getDisplayInfoMethod.invoke(display, displayInfo)
                val displayCutoutField = displayInfo.javaClass.getDeclaredField("displayCutout")
                displayCutoutField.isAccessible = true
                val cutout = displayCutoutField[displayInfo]
                if (cutout is DisplayCutout) {
                    displayCutout = cutout
                }
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, e)
            } catch (e: NoSuchMethodException) {
                Log.w(TAG, e)
            } catch (e: NoSuchFieldException) {
                Log.w(TAG, e)
            } catch (e: IllegalAccessException) {
                Log.w(TAG, e)
            } catch (e: InvocationTargetException) {
                Log.w(TAG, e)
            } catch (e: InstantiationException) {
                Log.w(TAG, e)
            }
            return displayCutout
        }
    }
}
