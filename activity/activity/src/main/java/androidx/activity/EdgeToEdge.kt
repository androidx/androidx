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
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
import androidx.activity.SystemBarStyle.Companion.dark
import androidx.activity.SystemBarStyle.Companion.light
import androidx.annotation.ColorInt
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.insets.ColorProtection
import androidx.core.view.insets.ProtectionLayout

// The light scrim color used in the platform API 29+
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/com/android/internal/policy/DecorView.java;drc=6ef0f022c333385dba2c294e35b8de544455bf19;l=142
@VisibleForTesting internal val DefaultLightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

// The dark scrim color used in the platform.
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/res/color/system_bar_background_semi_transparent.xml
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/remote_color_resources_res/values/colors.xml;l=67
@VisibleForTesting internal val DefaultDarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

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
    navigationBarStyle: SystemBarStyle = SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim),
) {
    val view = window.decorView
    val statusBarIsDark = statusBarStyle.detectDarkMode(view.resources)
    val navigationBarIsDark = navigationBarStyle.detectDarkMode(view.resources)
    val impl =
        Impl
            ?: (if (Build.VERSION.SDK_INT >= 35) {
                    EdgeToEdgeApi35()
                } else if (Build.VERSION.SDK_INT >= 30) {
                    EdgeToEdgeApi30()
                } else if (Build.VERSION.SDK_INT >= 29) {
                    EdgeToEdgeApi29()
                } else if (Build.VERSION.SDK_INT >= 28) {
                    EdgeToEdgeApi28()
                } else if (Build.VERSION.SDK_INT >= 26) {
                    EdgeToEdgeApi26()
                } else {
                    EdgeToEdgeApi23()
                })
                .also { Impl = it }
    impl.setUp(
        statusBarStyle,
        navigationBarStyle,
        window,
        view,
        statusBarIsDark,
        navigationBarIsDark,
    )
    impl.adjustLayoutInDisplayCutoutMode(window)
}

/** The style for the status bar or the navigation bar used in [enableEdgeToEdge]. */
class SystemBarStyle
private constructor(
    private val lightScrim: Int,
    internal val darkScrim: Int,
    internal val nightMode: Int,
    internal val detectDarkMode: (Resources) -> Boolean,
) {

    companion object {

        /**
         * Creates a new instance of [SystemBarStyle]. This style detects the dark mode
         * automatically and applies the recommended style for each of the status bar and the
         * navigation bar. If this style doesn't work for your app, consider using either [dark] or
         * [light].
         * - On API level 29 and above, both the status bar and the navigation bar will be
         *   transparent. However, the navigation bar with 3 or 2 buttons will have a translucent
         *   scrim. This scrim color is provided by the platform and *cannot be customized*.
         * - On API level 28 and below, the status bar will be transparent, and the navigation bar
         *   will have one of the specified scrim colors depending on the dark mode.
         *
         * @param lightScrim The scrim color to be used for the background when the app is in light
         *   mode. Note that this is used only on API level 28 and below.
         * @param darkScrim The scrim color to be used for the background when the app is in dark
         *   mode. This is also used on devices where the system icon color is always light. Note
         *   that this is used only on API level 28 and below.
         * @param detectDarkMode Optional. Detects whether UI currently uses dark mode or not. The
         *   default implementation can detect any of the standard dark mode features from the
         *   platform, appcompat, and Jetpack Compose.
         */
        @JvmStatic
        @JvmOverloads
        fun auto(
            @ColorInt lightScrim: Int,
            @ColorInt darkScrim: Int,
            detectDarkMode: (Resources) -> Boolean = { resources ->
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            },
        ): SystemBarStyle {
            return SystemBarStyle(
                lightScrim = lightScrim,
                darkScrim = darkScrim,
                nightMode = UiModeManager.MODE_NIGHT_AUTO,
                detectDarkMode = detectDarkMode,
            )
        }

        /**
         * Creates a new instance of [SystemBarStyle]. This style consistently applies the specified
         * scrim color regardless of the system navigation mode.
         *
         * @param scrim The scrim color to be used for the background. It is expected to be dark for
         *   the contrast against the light system icons.
         */
        @JvmStatic
        fun dark(@ColorInt scrim: Int): SystemBarStyle {
            return SystemBarStyle(
                lightScrim = scrim,
                darkScrim = scrim,
                nightMode = UiModeManager.MODE_NIGHT_YES,
                detectDarkMode = { _ -> true },
            )
        }

        /**
         * Creates a new instance of [SystemBarStyle]. This style consistently applies the specified
         * scrim color regardless of the system navigation mode.
         *
         * @param scrim The scrim color to be used for the background. It is expected to be light
         *   for the contrast against the dark system icons.
         * @param darkScrim The scrim color to be used for the background on devices where the
         *   system icon color is always light. It is expected to be dark.
         */
        @JvmStatic
        fun light(@ColorInt scrim: Int, @ColorInt darkScrim: Int): SystemBarStyle {
            return SystemBarStyle(
                lightScrim = scrim,
                darkScrim = darkScrim,
                nightMode = UiModeManager.MODE_NIGHT_NO,
                detectDarkMode = { _ -> false },
            )
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
        navigationBarIsDark: Boolean,
    )

    fun adjustLayoutInDisplayCutoutMode(window: Window)
}

private abstract class EdgeToEdgeBase : EdgeToEdgeImpl {

    override fun adjustLayoutInDisplayCutoutMode(window: Window) {
        // No display cutout before SDK 28.
    }
}

private class EdgeToEdgeApi23 : EdgeToEdgeBase() {

    @Suppress("DEPRECATION")
    @DoNotInline
    override fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean,
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = statusBarStyle.getScrim(statusBarIsDark)
        window.navigationBarColor = navigationBarStyle.darkScrim
        WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !statusBarIsDark
    }
}

@RequiresApi(26)
private open class EdgeToEdgeApi26 : EdgeToEdgeBase() {

    @Suppress("DEPRECATION")
    @DoNotInline
    override fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean,
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

@RequiresApi(28)
private open class EdgeToEdgeApi28 : EdgeToEdgeApi26() {

    @DoNotInline
    override fun adjustLayoutInDisplayCutoutMode(window: Window) {
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
}

@RequiresApi(29)
private open class EdgeToEdgeApi29 : EdgeToEdgeApi28() {

    @Suppress("DEPRECATION")
    @DoNotInline
    override fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean,
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

@RequiresApi(30)
private open class EdgeToEdgeApi30 : EdgeToEdgeApi29() {

    @DoNotInline
    override fun adjustLayoutInDisplayCutoutMode(window: Window) {
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
}

@RequiresApi(35)
private class EdgeToEdgeApi35 : EdgeToEdgeApi30() {

    @Suppress("DEPRECATION")
    @DoNotInline
    override fun setUp(
        statusBarStyle: SystemBarStyle,
        navigationBarStyle: SystemBarStyle,
        window: Window,
        view: View,
        statusBarIsDark: Boolean,
        navigationBarIsDark: Boolean,
    ) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // TODO(b/438675320): Remove the attributes check after `activity` depends on a version of
        //                    `core` which doesn't have b/434987937.
        // The attributes check is safe because when the window size is WRAP_CONTENT, DecorView
        // would consume the system window insets, and the protection is not necessary.
        val attrs = window.attributes
        if (
            (attrs.flags and FLAG_LAYOUT_IN_SCREEN) != 0 ||
                attrs.width != WRAP_CONTENT ||
                attrs.height != WRAP_CONTENT
        ) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            val statusBarColor = statusBarStyle.getScrimWithEnforcedContrast(statusBarIsDark)
            val navBarColor = navigationBarStyle.getScrimWithEnforcedContrast(navigationBarIsDark)
            (view as ViewGroup).addView(
                ProtectionLayout(
                    view.context,
                    listOf(
                        ColorProtection(WindowInsetsCompat.Side.TOP, statusBarColor),
                        ColorProtection(WindowInsetsCompat.Side.LEFT, navBarColor),
                        ColorProtection(WindowInsetsCompat.Side.RIGHT, navBarColor),
                        ColorProtection(WindowInsetsCompat.Side.BOTTOM, navBarColor),
                    ),
                )
            )
        }
        window.isNavigationBarContrastEnforced =
            navigationBarStyle.nightMode == UiModeManager.MODE_NIGHT_AUTO
        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = !statusBarIsDark
            isAppearanceLightNavigationBars = !navigationBarIsDark
        }
    }
}
