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

package androidx.compose.ui.platform

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.Display
import android.view.DisplayCutout
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.WindowInfoImpl.Companion.GlobalKeyboardModifiers
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toSize
import java.lang.reflect.InvocationTargetException

/**
 * WindowInfo that only calculates [containerSize] if the property has been read, to avoid expensive
 * size calculation when no one is reading the value.
 */
internal class LazyWindowInfo : WindowInfo {
    private var onInitializeContainerSize: (() -> DerivedSize)? = null
    private var _containerSize: MutableState<DerivedSize>? = null

    override var isWindowFocused: Boolean by mutableStateOf(false)

    override var keyboardModifiers: PointerKeyboardModifiers
        get() = GlobalKeyboardModifiers.value
        set(value) {
            GlobalKeyboardModifiers.value = value
        }

    inline fun updateContainerSizeIfObserved(calculateContainerSize: () -> DerivedSize) {
        _containerSize?.let { it.value = calculateContainerSize() }
    }

    fun setOnInitializeContainerSize(onInitializeContainerSize: (() -> DerivedSize)?) {
        // If we have already initialized, no need to set a listener here
        if (_containerSize == null) {
            this.onInitializeContainerSize = onInitializeContainerSize
        }
    }

    override val containerSize: IntSize
        get() {
            if (_containerSize == null) {
                val initialSize = onInitializeContainerSize?.invoke() ?: DerivedSize.Zero
                _containerSize = mutableStateOf(initialSize)
                onInitializeContainerSize = null
            }
            return _containerSize!!.value.pxSize
        }

    override val containerDpSize: DpSize
        get() {
            if (_containerSize == null) {
                val initialSize = onInitializeContainerSize?.invoke() ?: DerivedSize.Zero
                _containerSize = mutableStateOf(initialSize)
                onInitializeContainerSize = null
            }
            return _containerSize!!.value.dpSize
        }
}

/**
 * TODO: b/369334429 Temporary fork of WindowMetricsCalculator logic until b/360934048 and
 *   b/369170239 are resolved
 */
internal fun calculateWindowSize(androidComposeView: AndroidComposeView): DerivedSize {
    val context = androidComposeView.context
    val activity = context.findActivity()
    val density = Density(context)
    if (activity != null) {
        val bounds = BoundsHelper.getInstance().currentWindowBounds(activity)
        return DerivedSize.fromPxSize(
            pxSize = IntSize(bounds.width(), bounds.height()),
            density = density,
        )
    } else {
        // Fallback behavior for views created with an applicationContext / other non-Activity host
        val configuration = context.resources.configuration
        return DerivedSize.fromDpSize(
            dpSize = DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp),
            density = density,
        )
    }
}

internal data class DerivedSize(val pxSize: IntSize, val dpSize: DpSize) {
    companion object {
        val Zero = DerivedSize(IntSize.Zero, DpSize.Zero)

        fun fromPxSize(pxSize: IntSize, density: Density) =
            DerivedSize(pxSize, with(density) { pxSize.toSize().toDpSize() })

        fun fromDpSize(dpSize: DpSize, density: Density) =
            DerivedSize(with(density) { dpSize.toSize().toIntSize() }, dpSize)
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> this.baseContext.findActivity()
        else -> null
    }

private interface BoundsHelper {
    /** Compute the current bounds for the given [Activity]. */
    fun currentWindowBounds(activity: Activity): Rect

    companion object {
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
                    // If reflection fails for some reason default to the P implementation which
                    // still has the ability to account for display cutouts.
                    bounds = BoundsHelperApi28Impl.currentWindowBounds(activity)
                }
                else -> throw e
            }
        }
        return bounds
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
            if (activity.isInMultiWindowMode) {
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

        if (!activity.isInMultiWindowMode) {
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
                !activity.isInMultiWindowMode
        ) {
            // If the corrected bounds are not the same as the display size and the activity is
            // not in multi-window mode it is possible there are unreported cutouts inset-ing
            // the window depending on the layoutInCutoutMode. Check for them here by getting
            // the cutout from the display itself.
            val displayCutout = getCutoutForDisplay(currentDisplay)
            if (displayCutout != null) {
                if (bounds.left == displayCutout.safeInsetLeft) {
                    bounds.left = 0
                }
                if (realDisplaySize.x - bounds.right == displayCutout.safeInsetRight) {
                    bounds.right += displayCutout.safeInsetRight
                }
                if (bounds.top == displayCutout.safeInsetTop) {
                    bounds.top = 0
                }
                if (realDisplaySize.y - bounds.bottom == displayCutout.safeInsetBottom) {
                    bounds.bottom += displayCutout.safeInsetBottom
                }
            }
        }
        return bounds
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
        if (!activity.isInMultiWindowMode) {
            // The activity is not in multi-window mode. Check if the addition of the
            // navigation bar size to Display#getSize() results in the real display size and
            // if so return this value. If not, return the result of Display#getSize().
            val realDisplaySize = Point()
            @Suppress("DEPRECATION") defaultDisplay.getRealSize(realDisplaySize)
            val navigationBarHeight = getNavigationBarHeight(activity)
            if (bounds.bottom + navigationBarHeight == realDisplaySize.y) {
                bounds.bottom += navigationBarHeight
            } else if (bounds.right + navigationBarHeight == realDisplaySize.x) {
                bounds.right += navigationBarHeight
            }
        }
        return bounds
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
        val realDisplaySize = Point()
        @Suppress("DEPRECATION") defaultDisplay.getRealSize(realDisplaySize)
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
            is InstantiationException -> {}
            else -> throw e
        }
    }
    return displayCutout
}
