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

package androidx.core.splashscreen

import android.R.attr
import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.TypedValue
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.WindowInsets
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.window.SplashScreenView
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreen.KeepOnScreenCondition

/**
 * Provides control over the splash screen once the application is started.
 *
 * On API 31+ (Android 12+) this class calls the platform methods.
 *
 * Prior API 31, the platform behavior is replicated with the exception of the Animated Vector
 * Drawable support on the launch screen.
 *
 * # Usage of the `core-splashscreen` library:
 *
 * To replicate the splash screen behavior from Android 12 on older APIs the following steps need to
 * be taken:
 * 1. Create a new Theme (e.g `Theme.App.Starting`) and set its parent to `Theme.SplashScreen` or
 *    `Theme.SplashScreen.IconBackground`
 * 2. In your manifest, set the `theme` attribute of the whole `<application>` or just the starting
 *    `<activity>` to `Theme.App.Starting`
 * 3. In the `onCreate` method the starting activity, call [installSplashScreen] just before
 *    `super.onCreate()`. You also need to make sure that `postSplashScreenTheme` is set to the
 *    application's theme. Alternatively, this call can be replaced by [Activity#setTheme] if a
 *    [SplashScreen] instance isn't needed.
 *
 * ## Themes
 *
 * The library provides two themes: [R.style.Theme_SplashScreen] and
 * [R.style.Theme_SplashScreen_IconBackground]. If you wish to display a background right under your
 * icon, the later needs to be used. This ensure that the scale and masking of the icon are similar
 * to the Android 12 Splash Screen.
 *
 * `windowSplashScreenAnimatedIcon`: The splash screen icon. On API 31+ it can be an animated vector
 * drawable.
 *
 * `windowSplashScreenAnimationDuration`: Duration of the Animated Icon Animation. The value needs
 * to be > 0 if the icon is animated.
 *
 * **Note:** This has no impact on the time during which the splash screen is displayed and is only
 * used in [SplashScreenViewProvider.iconAnimationDurationMillis]. If you need to display the splash
 * screen for a longer time, you can use [SplashScreen.setKeepOnScreenCondition]
 *
 * `windowSplashScreenIconBackgroundColor`: _To be used in with
 * `Theme.SplashScreen.IconBackground`_. Sets a background color under the splash screen icon.
 *
 * `windowSplashScreenBackground`: Background color of the splash screen. Defaults to the theme's
 * `?attr/colorBackground`.
 *
 * `postSplashScreenTheme`* Theme to apply to the Activity when [installSplashScreen] is called.
 *
 * **Known incompatibilities:**
 * - On API < 31, `windowSplashScreenAnimatedIcon` cannot be animated. If you want to provide an
 *   animated icon for API 31+ and a still icon for API <31, you can do so by overriding the still
 *   icon with an animated vector drawable in `res/drawable-v31`.
 * - On API < 31, if the value of `windowSplashScreenAnimatedIcon` is an
 *   [adaptive icon](http://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive)
 *   , it will be cropped and scaled. The workaround is to respectively assign
 *   `windowSplashScreenAnimatedIcon` and `windowSplashScreenIconBackgroundColor` to the values of
 *   the adaptive icon `foreground` and `background`.
 *
 * # Design
 * The splash screen icon uses the same specifications as
 * [Adaptive Icons](https://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive)
 * . This means that the icon needs to fit within a circle whose diameter is 2/3 the size of the
 * icon. The actual values don't really matter if you use a vector icon.
 *
 * ## Specs
 * - With icon background (`Theme.SplashScreen.IconBackground`)
 *     + Image Size: 240x240 dp
 *     + Inner Circle diameter: 160 dp
 * - Without icon background (`Theme.SplashScreen`)
 *         + Image size: 288x288 dp
 *         + Inner circle diameter: 192 dp
 *
 * _Example:_ if the full size of the image is 300dp*300dp, the icon needs to fit within a circle
 * with a diameter of 200dp. Everything outside the circle will be invisible (masked).
 */
@SuppressLint("CustomSplashScreen")
public class SplashScreen private constructor(activity: Activity) {

    private val impl =
        when {
            SDK_INT >= 31 -> Impl31(activity)
            else -> Impl(activity)
        }

    public companion object {

        private const val MASK_FACTOR = 2 / 3f

        /**
         * Creates a [SplashScreen] instance associated with this [Activity] and handles setting the
         * theme to [R.attr.postSplashScreenTheme].
         *
         * This needs to be called before [Activity.setContentView] or other view operations on the
         * root view (e.g setting flags).
         *
         * Alternatively, if a [SplashScreen] instance is not required, the theme can manually be
         * set using [Activity.setTheme].
         */
        @JvmStatic
        public fun Activity.installSplashScreen(): SplashScreen {
            val splashScreen = SplashScreen(this)
            splashScreen.install()
            return splashScreen
        }
    }

    /**
     * Sets the condition to keep the splash screen visible.
     *
     * The splash will stay visible until the condition isn't met anymore. The condition is
     * evaluated before each request to draw the application, so it needs to be fast to avoid
     * blocking the UI.
     *
     * @param condition The condition evaluated to decide whether to keep the splash screen on
     *   screen
     */
    public fun setKeepOnScreenCondition(condition: KeepOnScreenCondition) {
        impl.setKeepOnScreenCondition(condition)
    }

    /**
     * Sets a listener that will be called when the splashscreen is ready to be removed.
     *
     * If a listener is set, the splashscreen won't be automatically removed and the application
     * needs to manually call [SplashScreenViewProvider.remove].
     *
     * IF no listener is set, the splashscreen will be automatically removed once the app is ready
     * to draw.
     *
     * The listener will be called on the ui thread.
     *
     * @param listener The [OnExitAnimationListener] that will be called when the splash screen is
     *   ready to be dismissed.
     * @see setKeepOnScreenCondition
     * @see OnExitAnimationListener
     * @see SplashScreenViewProvider
     */
    @SuppressWarnings("ExecutorRegistration") // Always runs on the MainThread
    public fun setOnExitAnimationListener(listener: OnExitAnimationListener) {
        impl.setOnExitAnimationListener(listener)
    }

    private fun install() {
        impl.install()
    }

    /**
     * Listener to be passed in [SplashScreen.setOnExitAnimationListener].
     *
     * The listener will be called once the splash screen is ready to be removed and provides a
     * reference to a [SplashScreenViewProvider] that can be used to customize the exit animation of
     * the splash screen.
     */
    public fun interface OnExitAnimationListener {

        /**
         * Callback called when the splash screen is ready to be dismissed. The caller is
         * responsible for animating and removing splash screen using the provided
         * [splashScreenViewProvider].
         *
         * The caller **must** call [SplashScreenViewProvider.remove] once it's done with the splash
         * screen.
         *
         * @param splashScreenViewProvider An object holding a reference to the displayed splash
         *   screen.
         */
        @MainThread
        public fun onSplashScreenExit(splashScreenViewProvider: SplashScreenViewProvider)
    }

    /**
     * Condition evaluated to check if the splash screen should remain on screen
     *
     * The splash screen will stay visible until the condition isn't met anymore. The condition is
     * evaluated before each request to draw the application, so it needs to be fast to avoid
     * blocking the UI.
     */
    public fun interface KeepOnScreenCondition {

        /**
         * Callback evaluated before every requests to draw the Activity. If it returns `true`, the
         * splash screen will be kept visible to hide the Activity below.
         *
         * This callback is evaluated in the main thread.
         */
        @MainThread public fun shouldKeepOnScreen(): Boolean
    }

    private open class Impl(val activity: Activity) {
        var finalThemeId: Int = 0
        var backgroundResId: Int? = null
        var backgroundColor: Int? = null
        var icon: Drawable? = null
        var hasBackground: Boolean = false

        var splashScreenWaitPredicate = KeepOnScreenCondition { false }
        private var animationListener: OnExitAnimationListener? = null
        private var mSplashScreenViewProvider: SplashScreenViewProvider? = null

        open fun install() {
            val typedValue = TypedValue()
            val currentTheme = activity.theme
            if (
                currentTheme.resolveAttribute(R.attr.windowSplashScreenBackground, typedValue, true)
            ) {
                backgroundResId = typedValue.resourceId
                backgroundColor = typedValue.data
            }
            if (
                currentTheme.resolveAttribute(
                    R.attr.windowSplashScreenAnimatedIcon,
                    typedValue,
                    true,
                )
            ) {
                icon = AppCompatResources.getDrawable(activity, typedValue.resourceId)
            }

            if (currentTheme.resolveAttribute(R.attr.splashScreenIconSize, typedValue, true)) {
                hasBackground =
                    typedValue.resourceId == R.dimen.splashscreen_icon_size_with_background
            }
            setPostSplashScreenTheme(currentTheme, typedValue)
        }

        protected fun setPostSplashScreenTheme(
            currentTheme: Resources.Theme,
            typedValue: TypedValue,
        ) {
            if (currentTheme.resolveAttribute(R.attr.postSplashScreenTheme, typedValue, true)) {
                finalThemeId = typedValue.resourceId
                if (finalThemeId != 0) {
                    activity.setTheme(finalThemeId)
                }
            }
        }

        public open fun setKeepOnScreenCondition(keepOnScreenCondition: KeepOnScreenCondition) {
            splashScreenWaitPredicate = keepOnScreenCondition
            val contentView = activity.findViewById<View>(android.R.id.content)
            val observer = contentView.viewTreeObserver
            observer.addOnPreDrawListener(
                object : OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        if (splashScreenWaitPredicate.shouldKeepOnScreen()) {
                            return false
                        }
                        contentView.viewTreeObserver.removeOnPreDrawListener(this)
                        mSplashScreenViewProvider?.let(::dispatchOnExitAnimation)
                        return true
                    }
                }
            )
        }

        public open fun setOnExitAnimationListener(exitAnimationListener: OnExitAnimationListener) {
            animationListener = exitAnimationListener

            val splashScreenViewProvider = SplashScreenViewProvider(activity)
            val finalBackgroundResId = backgroundResId
            val finalBackgroundColor = backgroundColor
            val splashScreenView = splashScreenViewProvider.view

            if (finalBackgroundResId != null && finalBackgroundResId != Resources.ID_NULL) {
                splashScreenView.setBackgroundResource(finalBackgroundResId)
            } else if (finalBackgroundColor != null) {
                splashScreenView.setBackgroundColor(finalBackgroundColor)
            } else {
                splashScreenView.background = activity.window.decorView.background
            }

            icon?.let { displaySplashScreenIcon(splashScreenView, it) }

            splashScreenView.addOnLayoutChangeListener(
                object : OnLayoutChangeListener {
                    override fun onLayoutChange(
                        view: View,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int,
                    ) {
                        if (!view.isAttachedToWindow) {
                            return
                        }

                        view.removeOnLayoutChangeListener(this)
                        if (!splashScreenWaitPredicate.shouldKeepOnScreen()) {
                            dispatchOnExitAnimation(splashScreenViewProvider)
                        } else {
                            mSplashScreenViewProvider = splashScreenViewProvider
                        }
                    }
                }
            )
        }

        private fun displaySplashScreenIcon(splashScreenView: View, icon: Drawable) {
            val iconView = splashScreenView.findViewById<ImageView>(R.id.splashscreen_icon_view)
            iconView.apply {
                val maskSize: Float
                if (hasBackground) {
                    // If the splash screen has an icon background we need to mask both the
                    // background and foreground.
                    val iconBackgroundDrawable =
                        AppCompatResources.getDrawable(context, R.drawable.icon_background)

                    val iconSize =
                        resources.getDimension(R.dimen.splashscreen_icon_size_with_background)
                    maskSize = iconSize * MASK_FACTOR

                    if (iconBackgroundDrawable != null) {
                        background = MaskedDrawable(iconBackgroundDrawable, maskSize)
                    }
                } else {
                    val iconSize =
                        resources.getDimension(R.dimen.splashscreen_icon_size_no_background)
                    maskSize = iconSize * MASK_FACTOR
                }
                setImageDrawable(MaskedDrawable(icon, maskSize))
            }
        }

        public fun dispatchOnExitAnimation(splashScreenViewProvider: SplashScreenViewProvider) {
            val finalListener = animationListener ?: return
            animationListener = null
            splashScreenViewProvider.view.postOnAnimation {
                splashScreenViewProvider.view.bringToFront()
                finalListener.onSplashScreenExit(splashScreenViewProvider)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private class Impl31(activity: Activity) : Impl(activity) {
        var preDrawListener: OnPreDrawListener? = null
        var mDecorFitWindowInsets = true

        val hierarchyListener =
            object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {

                    if (child is SplashScreenView) {
                        /*
                         * On API 31, the SplashScreenView sets window.setDecorFitsSystemWindows(false)
                         * when an OnExitAnimationListener is used. This also affects the application
                         * content that will be pushed up under the status bar even though it didn't
                         * requested it. And once the SplashScreenView is removed, the whole layout
                         * jumps back below the status bar. Fortunately, this happens only after the
                         * view is attached, so we have time to record the value of
                         * window.setDecorFitsSystemWindows() before the splash screen modifies it and
                         * reapply the correct value to the window.
                         */
                        mDecorFitWindowInsets = computeDecorFitsWindow(child)
                        (activity.window.decorView as ViewGroup).setOnHierarchyChangeListener(null)
                    }
                }

                override fun onChildViewRemoved(parent: View?, child: View?) {
                    // no-op
                }
            }

        fun computeDecorFitsWindow(child: SplashScreenView): Boolean {
            val inWindowInsets = WindowInsets.Builder().build()
            val outLocalInsets = Rect(Int.MIN_VALUE, Int.MIN_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

            // If setDecorFitWindowInsets is set to false, computeSystemWindowInsets
            // will return the same instance of WindowInsets passed in its parameter and
            // will set outLocalInsets to empty, so we check that both conditions are
            // filled to extrapolate the value of setDecorFitWindowInsets
            return !(inWindowInsets ===
                child.rootView.computeSystemWindowInsets(inWindowInsets, outLocalInsets) &&
                outLocalInsets.isEmpty)
        }

        override fun install() {
            setPostSplashScreenTheme(activity.theme, TypedValue())
            if (SDK_INT < 33) {
                (activity.window.decorView as ViewGroup).setOnHierarchyChangeListener(
                    hierarchyListener
                )
            }
        }

        override fun setKeepOnScreenCondition(keepOnScreenCondition: KeepOnScreenCondition) {
            splashScreenWaitPredicate = keepOnScreenCondition
            val contentView = activity.findViewById<View>(android.R.id.content)
            val observer = contentView.viewTreeObserver

            if (preDrawListener != null && observer.isAlive) {
                observer.removeOnPreDrawListener(preDrawListener)
            }
            preDrawListener =
                object : OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        if (splashScreenWaitPredicate.shouldKeepOnScreen()) {
                            return false
                        }
                        contentView.viewTreeObserver.removeOnPreDrawListener(this)
                        return true
                    }
                }
            observer.addOnPreDrawListener(preDrawListener)
        }

        override fun setOnExitAnimationListener(exitAnimationListener: OnExitAnimationListener) {
            activity.splashScreen.setOnExitAnimationListener { splashScreenView ->
                if (SDK_INT < 33) {
                    applyAppSystemUiTheme()
                }
                val splashScreenViewProvider = SplashScreenViewProvider(splashScreenView, activity)
                exitAnimationListener.onSplashScreenExit(splashScreenViewProvider)
            }
        }

        /**
         * Apply the system ui related theme attribute defined in the application to override the
         * ones set on the [SplashScreenView]
         *
         * On API 31, if an OnExitAnimationListener is set, the Window layout params are only
         * applied only when the [SplashScreenView] is removed. This lead to some flickers.
         *
         * To fix this, we apply these attributes as soon as the [SplashScreenView] is visible.
         */
        @Suppress("DEPRECATION")
        private fun applyAppSystemUiTheme() {
            val tv = TypedValue()
            val theme = activity.theme
            val window = activity.window

            if (theme.resolveAttribute(attr.statusBarColor, tv, true)) {
                window.statusBarColor = tv.data
            }

            if (theme.resolveAttribute(attr.navigationBarColor, tv, true)) {
                window.navigationBarColor = tv.data
            }

            if (theme.resolveAttribute(attr.windowDrawsSystemBarBackgrounds, tv, true)) {
                if (tv.data != 0) {
                    window.addFlags(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                } else {
                    window.clearFlags(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                }
            }

            val decorView = window.decorView as ViewGroup
            ThemeUtils.Api31.applyThemesSystemBarAppearance(theme, decorView, tv)

            // Fix setDecorFitsSystemWindows being overridden by the SplashScreenView
            decorView.setOnHierarchyChangeListener(null)
            window.setDecorFitsSystemWindows(mDecorFitWindowInsets)
        }
    }
}
