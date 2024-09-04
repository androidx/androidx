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

package androidx.core.splashscreen.test

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.R as SR
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * If true, sets an [androidx.core.splashscreen.SplashScreen.OnExitAnimationListener] on the
 * Activity
 */
internal const val EXTRA_ANIMATION_LISTENER = "AnimationListener"

/**
 * If true, sets a [androidx.core.splashscreen.SplashScreen.KeepOnScreenCondition] waiting until
 * [SplashScreenTestController.waitBarrier] is set to `false`. Activity
 */
internal const val EXTRA_SPLASHSCREEN_WAIT = "splashscreen_wait"

/**
 * If set to true, takes a screenshot of the splash screen and saves it in
 * [SplashScreenTestController.splashScreenScreenshot]
 */
internal const val EXTRA_SPLASHSCREEN_SCREENSHOT = "SplashScreenScreenShot"

public interface SplashScreenTestControllerHolder {
    public var controller: SplashScreenTestController
}

public class SplashScreenTestController(internal val activity: Activity) {

    public var splashScreenScreenshot: Bitmap? = null
    public var splashscreenIconId: Int = 0
    public var splashscreenBackgroundId: Int = 0
    public var splashscreenIconBackgroundId: Int = 0
    public var finalAppTheme: Int = 0
    public var duration: Int = 0
    public var exitAnimationListenerLatch: CountDownLatch = CountDownLatch(1)
    public var splashScreenView: View? = null
    public var splashScreenIconView: View? = null
    public var splashScreenIconViewBackground: Drawable? = null
    public var drawnLatch: CountDownLatch = CountDownLatch(1)
    public val isCompatActivity: Boolean
        get() = activity is AppCompatActivity

    // Wait for at least 3 passes to reduce flakiness
    public var waitedLatch: CountDownLatch = CountDownLatch(3)
    public val waitBarrier: AtomicBoolean = AtomicBoolean(true)
    public var hasDrawn: Boolean = false

    private var onExitAnimationListener: (SplashScreenViewProvider) -> Boolean = { false }

    /**
     * Call [onExitAnimation] when the
     * [androidx.core.splashscreen.SplashScreen.OnExitAnimationListener] is called. This requires
     * [EXTRA_ANIMATION_LISTENER] to be set to true. If [onExitAnimation] returns true,
     * [SplashScreenViewProvider] won't be removed and the OnExitAnimationListener returns
     * immediately.
     */
    fun doOnExitAnimation(onExitAnimation: (SplashScreenViewProvider) -> Boolean) {
        onExitAnimationListener = onExitAnimation
    }

    public fun onCreate() {
        val intent = activity.intent
        val theme = activity.theme
        val extras = intent.extras ?: Bundle.EMPTY

        val useListener = extras.getBoolean(EXTRA_ANIMATION_LISTENER)
        val takeScreenShot = extras.getBoolean(EXTRA_SPLASHSCREEN_SCREENSHOT)
        val waitForSplashscreen = extras.getBoolean(EXTRA_SPLASHSCREEN_WAIT)

        val tv = TypedValue()
        theme.resolveAttribute(SR.attr.windowSplashScreenAnimatedIcon, tv, true)
        splashscreenIconId = tv.resourceId

        theme.resolveAttribute(SR.attr.windowSplashScreenBackground, tv, true)
        splashscreenBackgroundId = tv.resourceId

        theme.resolveAttribute(SR.attr.windowSplashScreenIconBackgroundColor, tv, true)
        splashscreenIconBackgroundId = tv.resourceId

        theme.resolveAttribute(SR.attr.postSplashScreenTheme, tv, true)
        finalAppTheme = tv.resourceId

        theme.resolveAttribute(SR.attr.windowSplashScreenAnimationDuration, tv, true)
        duration = tv.data

        val splashScreen = activity.installSplashScreen()

        activity.setContentView(R.layout.main_activity)

        if (waitForSplashscreen) {
            splashScreen.setKeepOnScreenCondition {
                waitedLatch.countDown()
                val shouldWait = waitBarrier.get() || waitedLatch.count > 0L
                if (!shouldWait && takeScreenShot && splashScreenScreenshot == null) {
                    splashScreenScreenshot = getInstrumentation().uiAutomation.takeScreenshot()
                }
                shouldWait
            }
        }

        if (useListener) {
            splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
                splashScreenView = splashScreenViewProvider.view
                splashScreenIconView = splashScreenViewProvider.iconView
                splashScreenIconViewBackground = splashScreenViewProvider.iconView.background
                if (onExitAnimationListener(splashScreenViewProvider)) {
                    return@setOnExitAnimationListener
                }
                if (!takeScreenShot) {
                    splashScreenViewProvider.remove()
                    exitAnimationListenerLatch.countDown()
                }
            }
        }

        val view = activity.findViewById<TestView>(R.id.container)
        view.doOnDraw = { drawnLatch.countDown() }
    }
}
