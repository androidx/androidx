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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Build.VERSION.PREVIEW_SDK_INT
import android.os.Build.VERSION.SDK_INT
import android.util.TypedValue
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.ImageView
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.KeepOnScreenCondition

/**
 * Compatibly class for the SplashScreen API introduced in API 31.
 *
 * On API 31+ (Android 12+) this class calls the platform methods.
 *
 * Prior API 31, the platform behavior is replicated with the exception of the Animated Vector
 * Drawable support on the launch screen.
 *
 * To use this class, the theme of the starting Activity needs set [R.style.Theme_SplashScreen] as
 * its parent and the [R.attr.windowSplashScreenAnimatedIcon] and [R.attr.postSplashScreenTheme]`
 * attribute need to be set.
 */
@SuppressLint("CustomSplashScreen")
public class SplashScreen private constructor(activity: Activity) {

    @SuppressLint("NewApi") // TODO(188897399) Remove once "S" is finalized
    private val impl = when {
        SDK_INT >= 31 -> Impl31(activity)
        SDK_INT == 30 && PREVIEW_SDK_INT > 0 -> Impl31(activity)
        else -> Impl(activity)
    }

    public companion object {

        private const val MASK_FACTOR = 2 / 3f

        /**
         * Creates a [SplashScreen] instance associated with this [Activity] and handles
         * setting the theme to [R.attr.postSplashScreenTheme].
         *
         * This needs to be called before [Activity.setContentView] or other view operation on
         * the root view (e.g setting flags).
         *
         * Alternatively, if a [SplashScreen] instance is not required, the them can manually be
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
     * The splash will stay visible until the condition isn't met anymore.
     * The condition is evaluated before each request to draw the application, so it needs to be
     * fast to avoid blocking the UI.
     *
     * @param condition The condition evaluated to decide whether to keep the splash screen on
     * screen
     */
    public fun setKeepVisibleCondition(condition: KeepOnScreenCondition) {
        impl.setKeepVisibleCondition(condition)
    }

    /**
     * Sets a listener that will be called when the splashscreen is ready to be removed.
     *
     * If a listener is set, the splashscreen won't be automatically removed and the application
     * needs to manually call [SplashScreenViewProvider.remove].
     *
     * IF no listener is set, the splashscreen will be automatically removed once the app is
     * ready to draw.
     *
     * The listener will be called on the ui thread.
     *
     * @param listener The [OnExitAnimationListener] that will be called when the splash screen
     * is ready to be dismissed.
     *
     * @see setKeepVisibleCondition
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
     * reference to a [SplashScreenViewProvider] that can be used to customize the exit
     * animation of the splash screen.
     */
    public fun interface OnExitAnimationListener {

        /**
         * Callback called when the splash screen is ready to be dismissed. The caller is
         * responsible for animating and removing splash screen using the provided
         * [splashScreenViewProvider].
         *
         * The caller **must** call [SplashScreenViewProvider.remove] once it's done with the
         * splash screen.
         *
         * @param splashScreenViewProvider An object holding a reference to the displayed splash
         * screen.
         */
        @MainThread
        public fun onSplashScreenExit(splashScreenViewProvider: SplashScreenViewProvider)
    }

    /**
     * Condition evaluated to check if the splash screen should remain on screen
     *
     * The splash screen will stay visible until the condition isn't met anymore.
     * The condition is evaluated before each request to draw the application, so it needs to be
     * fast to avoid blocking the UI.
     */
    public fun interface KeepOnScreenCondition {

        /**
         * Callback evaluated before every requests to draw the Activity. If it returns `true`, the
         * splash screen will be kept visible to hide the Activity below.
         *
         * This callback is evaluated in the main thread.
         */
        @MainThread
        public fun shouldKeepOnScreen(): Boolean
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
            if (currentTheme.resolveAttribute(
                    R.attr.windowSplashScreenBackground,
                    typedValue,
                    true
                )
            ) {
                backgroundResId = typedValue.resourceId
                backgroundColor = typedValue.data
            }
            if (currentTheme.resolveAttribute(
                    R.attr.windowSplashScreenAnimatedIcon,
                    typedValue,
                    true
                )
            ) {
                icon = currentTheme.getDrawable(typedValue.resourceId)
            }

            if (currentTheme.resolveAttribute(R.attr.splashScreenIconSize, typedValue, true)) {
                hasBackground =
                    typedValue.resourceId == R.dimen.splashscreen_icon_size_with_background
            }
            setPostSplashScreenTheme(currentTheme, typedValue)
        }

        protected fun setPostSplashScreenTheme(
            currentTheme: Resources.Theme,
            typedValue: TypedValue
        ) {
            if (currentTheme.resolveAttribute(R.attr.postSplashScreenTheme, typedValue, true)) {
                finalThemeId = typedValue.resourceId
                if (finalThemeId != 0) {
                    activity.setTheme(finalThemeId)
                }
            } else {
                throw Resources.NotFoundException(
                    "Cannot set AppTheme. No theme value defined for attribute " +
                        activity.resources.getResourceName(R.attr.postSplashScreenTheme)
                )
            }
        }

        open fun setKeepVisibleCondition(keepOnScreenCondition: KeepOnScreenCondition) {
            splashScreenWaitPredicate = keepOnScreenCondition
            val contentView = activity.findViewById<View>(android.R.id.content)
            val observer = contentView.viewTreeObserver
            observer.addOnPreDrawListener(object : OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (splashScreenWaitPredicate.shouldKeepOnScreen()) {
                        return false
                    }
                    contentView.viewTreeObserver.removeOnPreDrawListener(this)
                    mSplashScreenViewProvider?.let(::dispatchOnExitAnimation)
                    return true
                }
            })
        }

        open fun setOnExitAnimationListener(exitAnimationListener: OnExitAnimationListener) {
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
                        oldBottom: Int
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
                })
        }

        private fun displaySplashScreenIcon(splashScreenView: View, icon: Drawable) {
            val iconView = splashScreenView.findViewById<ImageView>(R.id.splashscreen_icon_view)
            iconView.apply {
                val maskSize: Float
                if (hasBackground) {
                    // If the splash screen has an icon background we need to mask both the
                    // background and foreground.
                    val iconBackgroundDrawable = context.getDrawable(R.drawable.icon_background)

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

        fun dispatchOnExitAnimation(splashScreenViewProvider: SplashScreenViewProvider) {
            val finalListener = animationListener ?: return
            animationListener = null
            splashScreenViewProvider.view.postOnAnimation {
                finalListener.onSplashScreenExit(splashScreenViewProvider)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private class Impl31(activity: Activity) : Impl(activity) {
        var preDrawListener: OnPreDrawListener? = null

        override fun install() {
            setPostSplashScreenTheme(activity.theme, TypedValue())
        }

        override fun setKeepVisibleCondition(keepOnScreenCondition: KeepOnScreenCondition) {
            splashScreenWaitPredicate = keepOnScreenCondition
            val contentView = activity.findViewById<View>(android.R.id.content)
            val observer = contentView.viewTreeObserver

            if (preDrawListener != null && observer.isAlive) {
                observer.removeOnPreDrawListener(preDrawListener)
            }
            preDrawListener = object : OnPreDrawListener {
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

        override fun setOnExitAnimationListener(
            exitAnimationListener: OnExitAnimationListener
        ) {
            activity.splashScreen.setOnExitAnimationListener {
                val splashScreenViewProvider = SplashScreenViewProvider(it, activity)
                exitAnimationListener.onSplashScreenExit(splashScreenViewProvider)
            }
        }
    }
}