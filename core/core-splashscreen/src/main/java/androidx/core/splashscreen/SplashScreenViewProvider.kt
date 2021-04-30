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
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.window.SplashScreenView
import androidx.annotation.RequiresApi

/**
 * Contains a copy of the splash screen used to create a custom animation from the splash screen
 * to the application.
 *
 * The splashscreen is accessible using [SplashScreenViewProvider.view] and the view
 * containing the icon using [SplashScreenViewProvider.iconView].
 *
 * This class also contains time information about the animated icon (for API 31+).
 *
 * The application always needs to call [SplashScreenViewProvider.remove] once it's done
 * with it.
 */
@SuppressLint("ViewConstructor")
public class SplashScreenViewProvider internal constructor(ctx: Activity) {

    @RequiresApi(31)
    internal constructor(platformView: SplashScreenView, ctx: Activity) : this(ctx) {
        (impl as ViewImpl31).platformView = platformView
    }

    @SuppressLint("NewApi") // TODO(188897399) Remove once "S" is finalized
    private val impl: ViewImpl = when {
        Build.VERSION.SDK_INT >= 31 -> ViewImpl31(ctx)
        Build.VERSION.SDK_INT == 30 && Build.VERSION.PREVIEW_SDK_INT > 0 -> ViewImpl31(ctx)
        else -> ViewImpl(ctx)
    }

    /**
     * The splash screen view, copied into this application process.
     *
     * This view can be used to create custom animation from the splash screen to the application
     */
    public val view: View get() = impl.splashScreenView

    /**
     * The view containing the splashscreen icon as defined by
     * [R.attr.windowSplashScreenAnimatedIcon]
     */
    public val iconView: View get() = impl.iconView

    /**
     * Start time of the icon animation.
     *
     * On API 31+, returns the number of millisecond since the Epoch time (1970-1-1T00:00:00Z)
     *
     * Below API 31, returns 0 because the icon cannot be animated.
     */
    public val iconAnimationStartMillis: Long get() = impl.iconAnimationStartMillis

    /**
     * Duration of the icon animation as provided in [R.attr.
     */
    public val iconAnimationDurationMillis: Long get() = impl.iconAnimationDurationMillis

    /**
     * Remove the SplashScreen's view from the view hierarchy.
     *
     * This always needs to be called when an
     * [androidx.core.splashscreen.SplashScreen.OnExitAnimationListener]
     * is set.
     */
    public fun remove(): Unit = impl.remove()

    private open class ViewImpl(val activity: Activity) {

        private val _splashScreenView: ViewGroup by lazy {
            FrameLayout.inflate(
                activity,
                R.layout.splash_screen_view,
                null
            ) as ViewGroup
        }

        init {
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            content.addView(_splashScreenView)
        }

        open val splashScreenView: ViewGroup get() = _splashScreenView
        open val iconView: View get() = splashScreenView.findViewById(R.id.splashscreen_icon_view)
        open val iconAnimationStartMillis: Long get() = 0
        open val iconAnimationDurationMillis: Long get() = 0
        open fun remove() =
            activity.findViewById<ViewGroup>(android.R.id.content).removeView(splashScreenView)
    }

    @RequiresApi(31)
    private class ViewImpl31(activity: Activity) : ViewImpl(activity) {
        lateinit var platformView: SplashScreenView

        override val splashScreenView get() = platformView

        override val iconView get() = platformView.iconView!!

        override val iconAnimationStartMillis: Long
            get() = platformView.iconAnimationStart?.toEpochMilli() ?: 0

        override val iconAnimationDurationMillis: Long
            get() = platformView.iconAnimationDuration?.toMillis() ?: 0

        override fun remove() = platformView.remove()
    }
}