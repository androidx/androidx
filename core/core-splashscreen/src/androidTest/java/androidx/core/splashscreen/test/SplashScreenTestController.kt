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
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.R as SR
import androidx.test.runner.screenshot.Screenshot
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

internal const val EXTRA_ANIMATION_LISTENER = "AnimationListener"
internal const val EXTRA_SPLASHSCREEN_WAIT = "splashscreen_wait"
internal const val EXTRA_SPLASHSCREEN_VIEW_SCREENSHOT = "SplashScreenViewScreenShot"

public interface SplashScreenTestControllerHolder {
    public var controller: SplashScreenTestController
}

public class SplashScreenTestController(private val activity: Activity) {

    public var splashScreenViewScreenShot: Bitmap? = null
    public var splashScreenScreenshot: Bitmap? = null
    public var splashscreenIconId: Int = 0
    public var splashscreenBackgroundId: Int = 0
    public var finalAppTheme: Int = 0
    public var duration: Int = 0
    public var exitAnimationListenerLatch: CountDownLatch = CountDownLatch(1)
    public var drawnLatch: CountDownLatch = CountDownLatch(1)
    public val isCompatActivity: Boolean
        get() = activity is AppCompatActivity

    // Wait for at least 3 passes to reduce flakiness
    public var waitedLatch: CountDownLatch = CountDownLatch(3)
    public val waitBarrier: AtomicBoolean = AtomicBoolean(true)
    public var hasDrawn: Boolean = false

    public fun onCreate() {
        val intent = activity.intent
        val theme = activity.theme

        val useListener = intent.extras?.getBoolean(EXTRA_ANIMATION_LISTENER) ?: false
        val takeScreenShot =
            intent.extras?.getBoolean(EXTRA_SPLASHSCREEN_VIEW_SCREENSHOT) ?: false
        val waitForSplashscreen = intent.extras?.getBoolean(EXTRA_SPLASHSCREEN_WAIT) ?: false

        val tv = TypedValue()
        theme.resolveAttribute(SR.attr.windowSplashScreenAnimatedIcon, tv, true)
        splashscreenIconId = tv.resourceId

        theme.resolveAttribute(SR.attr.windowSplashScreenBackground, tv, true)
        splashscreenBackgroundId = tv.resourceId

        theme.resolveAttribute(SR.attr.postSplashScreenTheme, tv, true)
        finalAppTheme = tv.resourceId

        theme.resolveAttribute(SR.attr.windowSplashScreenAnimationDuration, tv, true)
        duration = tv.data

        val splashScreen = activity.installSplashScreen()
        activity.setContentView(R.layout.main_activity)

        if (waitForSplashscreen) {
            splashScreen.setKeepVisibleCondition {
                waitedLatch.countDown()
                val shouldWait = waitBarrier.get() || waitedLatch.count > 0L
                if (!shouldWait && takeScreenShot && splashScreenScreenshot == null) {
                    splashScreenScreenshot = Screenshot.capture().bitmap
                }
                shouldWait
            }
        }

        if (useListener) {
            splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
                if (takeScreenShot) {
                    splashScreenViewScreenShot = Screenshot.capture().bitmap
                }
                exitAnimationListenerLatch.countDown()
                splashScreenViewProvider.remove()
            }
        }

        val view = activity.findViewById<TestView>(R.id.container)
        view.doOnDraw = { drawnLatch.countDown() }
    }
}
