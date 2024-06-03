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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.DisplayCutout
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.annotation.UiContext
import androidx.window.layout.util.ActivityCompatHelperApi24.isInMultiWindowMode
import androidx.window.layout.util.BoundsHelper.Companion.TAG
import androidx.window.layout.util.DisplayCompatHelperApi28.safeInsetBottom
import androidx.window.layout.util.DisplayCompatHelperApi28.safeInsetLeft
import androidx.window.layout.util.DisplayCompatHelperApi28.safeInsetRight
import androidx.window.layout.util.DisplayCompatHelperApi28.safeInsetTop
import androidx.window.layout.util.DisplayHelper.getRealSizeForDisplay
import java.lang.reflect.InvocationTargetException

/** Provides compatibility behavior for calculating bounds of an activity. */
internal interface BoundsHelper {

    /** Compute the current bounds for the given [Activity]. */
    fun currentWindowBounds(activity: Activity): Rect

    fun maximumWindowBounds(@UiContext context: Context): Rect

    companion object {

        val TAG: String = BoundsHelper::class.java.simpleName

        fun getInstance(): BoundsHelper {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    BoundsHelperApi30Impl
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    BoundsHelperApi29Impl
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                    BoundsHelperApi28Impl
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                    BoundsHelperApi24Impl
                }
                else -> {
                    BoundsHelperApi16Impl
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private object BoundsHelperApi30Impl : BoundsHelper {
    override fun currentWindowBounds(activity: Activity): Rect {
        val wm = activity.getSystemService(WindowManager::class.java)
        return wm.currentWindowMetrics.bounds
    }

    override fun maximumWindowBounds(@UiContext context: Context): Rect {
        val wm = context.getSystemService(WindowManager::class.java)
        return wm.maximumWindowMetrics.bounds
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private object BoundsHelperApi29Impl : BoundsHelper {

    /** Computes the window bounds for [Build.VERSION_CODES.Q]. */
    @SuppressLint("BanUncheckedReflection", "BlockedPrivateApi")
    override fun currentWindowBounds(activity: Activity): Rect {
        var bounds: Rect
        val config = activity.resources.configuration
        try {
            val windowConfigField =
                Configuration::class.java.getDeclaredField("windowConfiguration")
            windowConfigField.isAccessible = true
            val windowConfig = windowConfigField[config]
            val getBoundsMethod = windowConfig.javaClass.getDeclaredMethod("getBounds")
            bounds = Rect(getBoundsMethod.invoke(windowConfig) as Rect)
        } catch (e: Exception) {
            when (e) {
                is NoSuchFieldException,
                is NoSuchMethodException,
                is IllegalAccessException,
                is InvocationTargetException -> {
                    Log.w(TAG, e)
                    // If reflection fails for some reason default to the P implementation which
                    // still has the ability to account for display cutouts.
                    bounds = BoundsHelperApi28Impl.currentWindowBounds(activity)
                }
                else -> throw e
            }
        }
        return bounds
    }

    override fun maximumWindowBounds(@UiContext context: Context): Rect {
        return BoundsHelperApi28Impl.maximumWindowBounds(context)
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private object BoundsHelperApi28Impl : BoundsHelper {

    /**
     * Computes the window bounds for [Build.VERSION_CODES.P].
     *
     * NOTE: This method may result in incorrect values if the [android.content.res.Resources] value
     * stored at 'navigation_bar_height' does not match the true navigation bar inset on the window.
     */
    @SuppressLint("BanUncheckedReflection", "BlockedPrivateApi")
    override fun currentWindowBounds(activity: Activity): Rect {
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
        } catch (e: Exception) {
            when (e) {
                is NoSuchFieldException,
                is NoSuchMethodException,
                is IllegalAccessException,
                is InvocationTargetException -> {
                    Log.w(TAG, e)
                    getRectSizeFromDisplay(activity, bounds)
                }
                else -> throw e
            }
        }

        val platformWindowManager = activity.windowManager

        // [WindowManager#getDefaultDisplay] is deprecated but we have this for
        // compatibility with older versions
        @Suppress("DEPRECATION") val currentDisplay = platformWindowManager.defaultDisplay
        val realDisplaySize = Point()
        @Suppress("DEPRECATION") currentDisplay.getRealSize(realDisplaySize)

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
        if (
            (bounds.width() < realDisplaySize.x || bounds.height() < realDisplaySize.y) &&
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

    override fun maximumWindowBounds(@UiContext context: Context): Rect {
        return BoundsHelperApi24Impl.maximumWindowBounds(context)
    }
}

@RequiresApi(Build.VERSION_CODES.N)
private object BoundsHelperApi24Impl : BoundsHelper {

    /**
     * Computes the window bounds for platforms between [Build.VERSION_CODES.N] and
     * [Build.VERSION_CODES.O_MR1], inclusive.
     *
     * NOTE: This method may result in incorrect values under the following conditions:
     * * If the activity is in multi-window mode the origin of the returned bounds will always be
     *   anchored at (0, 0).
     * * If the [android.content.res.Resources] value stored at 'navigation_bar_height' does not
     *   match the true navigation bar size the returned bounds will not take into account the
     *   navigation bar.
     */
    override fun currentWindowBounds(activity: Activity): Rect {
        val bounds = Rect()
        // [WindowManager#getDefaultDisplay] is deprecated but we have this for
        // compatibility with older versions
        @Suppress("DEPRECATION") val defaultDisplay = activity.windowManager.defaultDisplay
        // [Display#getRectSize] is deprecated but we have this for
        // compatibility with older versions
        @Suppress("DEPRECATION") defaultDisplay.getRectSize(bounds)
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

    override fun maximumWindowBounds(@UiContext context: Context): Rect {
        return BoundsHelperApi16Impl.maximumWindowBounds(context)
    }
}

private object BoundsHelperApi16Impl : BoundsHelper {

    /**
     * Computes the window bounds for platforms between [Build.VERSION_CODES.JELLY_BEAN] and
     * [Build.VERSION_CODES.M], inclusive.
     *
     * Given that multi-window mode isn't supported before N we simply return the real display size
     * which should match the window size of a full-screen app.
     */
    override fun currentWindowBounds(activity: Activity): Rect {
        // [WindowManager#getDefaultDisplay] is deprecated but we have this for
        // compatibility with older versions
        @Suppress("DEPRECATION") val defaultDisplay = activity.windowManager.defaultDisplay
        val realDisplaySize = getRealSizeForDisplay(defaultDisplay)
        val bounds = Rect()
        if (realDisplaySize.x == 0 || realDisplaySize.y == 0) {
            // [Display#getRectSize] is deprecated but we have this for
            // compatibility with older versions
            @Suppress("DEPRECATION") defaultDisplay.getRectSize(bounds)
        } else {
            bounds.right = realDisplaySize.x
            bounds.bottom = realDisplaySize.y
        }
        return bounds
    }

    override fun maximumWindowBounds(@UiContext context: Context): Rect {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // [WindowManager#getDefaultDisplay] is deprecated but we have this for
        // compatibility with older versions, as we can't reliably get the display associated
        // with a Context through public APIs either.
        @Suppress("DEPRECATION") val display = wm.defaultDisplay
        val displaySize = getRealSizeForDisplay(display)
        return Rect(0, 0, displaySize.x, displaySize.y)
    }
}

/**
 * Returns the [android.content.res.Resources] value stored as 'navigation_bar_height'.
 *
 * Note: This is error-prone and is **not** the recommended way to determine the size of the
 * overlapping region between the navigation bar and a given window. The best approach is to acquire
 * the [android.view.WindowInsets].
 */
private fun getNavigationBarHeight(context: Context): Int {
    val resources = context.resources
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else 0
}

private fun getRectSizeFromDisplay(activity: Activity, bounds: Rect) {
    // [WindowManager#getDefaultDisplay] is deprecated but we have this for
    // compatibility with older versions
    @Suppress("DEPRECATION") val defaultDisplay = activity.windowManager.defaultDisplay
    // [Display#getRectSize] is deprecated but we have this for
    // compatibility with older versions
    @Suppress("DEPRECATION") defaultDisplay.getRectSize(bounds)
}

/**
 * Returns the [DisplayCutout] for the given display. Note that display cutout returned here is for
 * the display and the insets provided are in the display coordinate system.
 *
 * @return the display cutout for the given display.
 */
@SuppressLint("BanUncheckedReflection")
@RequiresApi(Build.VERSION_CODES.P)
private fun getCutoutForDisplay(display: Display): DisplayCutout? {
    var displayCutout: DisplayCutout? = null
    try {
        val displayInfoClass = Class.forName("android.view.DisplayInfo")
        val displayInfoConstructor = displayInfoClass.getConstructor()
        displayInfoConstructor.isAccessible = true
        val displayInfo = displayInfoConstructor.newInstance()
        val getDisplayInfoMethod =
            display.javaClass.getDeclaredMethod("getDisplayInfo", displayInfo.javaClass)
        getDisplayInfoMethod.isAccessible = true
        getDisplayInfoMethod.invoke(display, displayInfo)
        val displayCutoutField = displayInfo.javaClass.getDeclaredField("displayCutout")
        displayCutoutField.isAccessible = true
        val cutout = displayCutoutField[displayInfo]
        if (cutout is DisplayCutout) {
            displayCutout = cutout
        }
    } catch (e: Exception) {
        when (e) {
            is ClassNotFoundException,
            is NoSuchMethodException,
            is NoSuchFieldException,
            is IllegalAccessException,
            is InvocationTargetException,
            is InstantiationException -> {
                Log.w(TAG, e)
            }
            else -> throw e
        }
    }
    return displayCutout
}
