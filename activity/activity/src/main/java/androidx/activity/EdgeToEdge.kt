/*
 * Copyright 2023 The Android Open Source Project
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
@file:JvmName("EdgeToEdge")

package androidx.activity

import android.app.UiModeManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

// The light scrim color used in the platform API 29+
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/com/android/internal/policy/DecorView.java;drc=6ef0f022c333385dba2c294e35b8de544455bf19;l=142
@VisibleForTesting
internal val DefaultLightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

// The dark scrim color used in the platform.
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/res/color/system_bar_background_semi_transparent.xml
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/remote_color_resources_res/values/colors.xml;l=67
@VisibleForTesting
internal val DefaultDarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

private var Impl: EdgeToEdgeImpl? = null

/**
 * Enables the edge-to-edge display for this [ComponentActivity].
 *
 * To set it up with the default style, call this method in your Activity's onCreate method:
 * ```
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         enableEdgeToEdge()
 *         super.onCreate(savedInstanceState)
 *         ...
 *     }
 * ```
 *
 * The default style configures the system bars with a transparent background when contrast can be
 * enforced by the system (API 29 or above). On older platforms (which only have 3-button/2-button
 * navigation modes), an equivalent scrim is applied to ensure contrast with the system bars.
 *
 * See [SystemBarStyle] for more customization options.
 *
 * @param statusBarStyle The [SystemBarStyle] for the status bar.
 * @param navigationBarStyle The [SystemBarStyle] for the navigation bar.
 */
@JvmName("enable")
@JvmOverloads
fun ComponentActivity.enableEdgeToEdge(
    statusBarStyle: SystemBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
    navigationBarStyle: SystemBarStyle = SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim)
) {
    val view = window.decorView
    val statusBarIsDark = statusBarStyle.isDark(view.resources)
    val navigationBarIsDark = navigationBarStyle.isDark(view.resources)
    val impl = Impl ?: if (Build.VERSION.SDK_INT >= 29) {
        EdgeToEdgeApi29()
    } else if (Build.VERSION.SDK_INT >= 26) {
        EdgeToEdgeApi26()
    } else if (Build.VERSION.SDK_INT >= 23) {
        EdgeToEdgeApi23()
    } else if (Build.VERSION.SDK_INT >= 21) {
        EdgeToEdgeApi21()
    } else {
        EdgeToEdgeBase()
    }.also { Impl = it }
    impl.setUp(
        statusBarStyle, navigationBarStyle, window, view, statusBarIsDark, navigationBarIsDark
    )
}

/**
 * The style for the status bar or the navigation bar used in [enableEdgeToEdge].
 */
class SystemBarStyle private constructor(
    private val lightScrim: Int,
    internal val darkScrim: Int,
    internal val nightMode: Int
) {

    companion object {

        /**
         * Creates a new instance of [SystemBarStyle]. This style detects the dark mode
         * automatically.
         * - On API level 29 and above, the bar will be transparent in the gesture navigation mode.
         *   If this is used for the navigation bar, it will have the scrim automatically applied
         *   by the system in the 3-button navigation mode. _Note that neither of the specified
         *   colors are used_. If you really want a custom color on these API levels, use [dark] or
         *   [light].
         * - On API level 28 and below, the bar will be one of the specified scrim colors depending
         *   on the dark mode.
         * @param lightScrim The scrim color to be used for the background when the app is in light
         * mode.
         * @param darkScrim The scrim color to be used for the background when the app is in dark
         * mode. This is also used on devices where the system icon color is always light.
         */
        @JvmStatic
        fun auto(@ColorInt lightScrim: Int, @ColorInt darkScrim: Int): SystemBarStyle {
            return SystemBarStyle(lightScrim, darkScrim, UiModeManager.MODE_NIGHT_AUTO)
        }

        /**
         * Creates a new instance of [SystemBarStyle]. This style consistently applies the specified
         * scrim color regardless of the system navigation mode.
         *
         * @param scrim The scrim color to be used for the background. It is expected to be dark
         * for the contrast against the light system icons.
         */
        @JvmStatic
        fun dark(@ColorInt scrim: Int): SystemBarStyle {
            return SystemBarStyle(scrim, scrim, UiModeManager.MODE_NIGHT_YES)
        }

        /**
         * Creates a new instance of [SystemBarStyle]. This style consistently applies the specified
         * scrim color regardless of the system navigation mode.
         *
         * @param scrim The scrim color to be used for the background. It is expected to be light
         * for the contrast against the dark system icons.
         * @param darkScrim The scrim color to be used for the background on devices where the
         * system icon color is always light. It is expected to be dark.
         */
        @JvmStatic
        fun light(@ColorInt scrim: Int, @ColorInt darkScrim: Int): SystemBarStyle {
            return SystemBarStyle(scrim, darkScrim, UiModeManager.MODE_NIGHT_NO)
        }
    }

    internal fun isDark(resources: Resources): Boolean {
        return when (nightMode) {
            UiModeManager.MODE_NIGHT_YES -> true
            UiModeManager.MODE_NIGHT_NO -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }
    }

    internal fun getScrim(isDark: Boolean) = if (isDark) darkScrim else lightScrim

    internal fun getScrimWithEnforcedContrast(isDark: Boolean): Int {
        return when {
            nightMode == UiModeManager.MODE_NIGHT_AUTO -> Color.TRANSPARENT
            isDark -> darkScrim
            else -> lightScrim
        }
    }
}

private interface EdgeToEdgeImpl {

    fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean
    )
}

private class EdgeToEdgeBase : EdgeToEdgeImpl {

    override fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean
    ) {
        // No edge-to-edge before SDK 21.
    }
}

@RequiresApi(21)
private class EdgeToEdgeApi21 : EdgeToEdgeImpl {

    @Suppress("DEPRECATION")
    @DoNotInline
    override fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    }
}

@RequiresApi(23)
private class EdgeToEdgeApi23 : EdgeToEdgeImpl {

    @DoNotInline
    override fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = statusBarStyle.getScrim(statusBarIsDark)
        window.navigationBarColor = navigationBarStyle.darkScrim
        WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !statusBarIsDark
    }
}

@RequiresApi(26)
private class EdgeToEdgeApi26 : EdgeToEdgeImpl {

    @DoNotInline
    override fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = statusBarStyle.getScrim(statusBarIsDark)
        window.navigationBarColor = navigationBarStyle.getScrim(navigationBarIsDark)
        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = !statusBarIsDark
            isAppearanceLightNavigationBars = !navigationBarIsDark
        }
    }
}

@RequiresApi(29)
private class EdgeToEdgeApi29 : EdgeToEdgeImpl {

    @DoNotInline
    override fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = statusBarStyle.getScrimWithEnforcedContrast(statusBarIsDark)
        window.navigationBarColor =
            navigationBarStyle.getScrimWithEnforcedContrast(navigationBarIsDark)
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced =
            navigationBarStyle.nightMode == UiModeManager.MODE_NIGHT_AUTO
        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = !statusBarIsDark
            isAppearanceLightNavigationBars = !navigationBarIsDark
        }
    }
}
