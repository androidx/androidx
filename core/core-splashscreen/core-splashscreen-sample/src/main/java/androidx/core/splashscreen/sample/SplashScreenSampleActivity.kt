/*
 * Copyright (C) 2020 The Android Open Source Project
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
package androidx.core.splashscreen.sample

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.core.view.WindowCompat
import androidx.core.view.postDelayed
import androidx.interpolator.view.animation.FastOutLinearInInterpolator

@RequiresApi(21)
class SplashScreenSampleActivity : AppCompatActivity() {

    private var appReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This activity will be handling the splash screen transition
        val splashScreen = installSplashScreen()

        // The splashscreen goes edge to edge, so for a smooth transition to our app, we also
        // want to draw edge to edge.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // The content view needs to set before calling setOnExitAnimationListener
        // to ensure that the SplashScreenView is attach to the right view root.
        setContentView(R.layout.main_activity)

        // (Optional) We can keep the splash screen visible until our app is ready.
        splashScreen.setKeepVisibleCondition { !appReady }

        // (Optional) Setting an OnExitAnimationListener on the SplashScreen indicates
        // to the system that the application will handle the exit animation.
        // The listener will be called once the app is ready.
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            onSplashScreenExit(splashScreenViewProvider)
        }

        /* The code below is only for demo purposes */
        // Create some artificial delay to simulate some local database fetch for example
        Handler(Looper.getMainLooper())
            .postDelayed({ appReady = true }, (MOCK_DELAY).toLong())

        // Just a convenient button in our App to kill its process so we can play with the
        // splashscreen again and again.
        setupKillButton()
    }

    /**
     * Handles the transition from the splash screen to the application
     */
    private fun onSplashScreenExit(splashScreenViewProvider: SplashScreenViewProvider) {
        val accelerateInterpolator = FastOutLinearInInterpolator()
        val splashScreenView = splashScreenViewProvider.view
        val iconView = splashScreenViewProvider.iconView

        // We'll change the alpha of the main view
        val alpha = ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f)
        alpha.duration = SPLASHSCREEN_ALPHA_ANIMATION_DURATION.toLong()
        alpha.interpolator = accelerateInterpolator

        // And we translate the icon down
        val translationY = ObjectAnimator.ofFloat(
            iconView,
            View.TRANSLATION_Y,
            iconView.translationY,
            splashScreenView.height.toFloat()
        )
        translationY.duration = SPLASHSCREEN_TY_ANIMATION_DURATION.toLong()
        translationY.interpolator = accelerateInterpolator

        // To get fancy, we'll also animate our content
        val marginAnimator = createContentAnimation()

        // And we play all of the animation together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(translationY, alpha, marginAnimator)

        // Once the application is finished, we remove the splash screen from our view
        // hierarchy.
        animatorSet.doOnEnd { splashScreenViewProvider.remove() }

        if (WAIT_FOR_AVD_TO_FINISH) {
            waitForAnimatedIconToFinish(splashScreenViewProvider, splashScreenView, animatorSet)
        } else {
            animatorSet.start()
        }
    }

    /**
     * Wait until the AVD animation is finished before starting the splash screen dismiss animation
     */
    private fun waitForAnimatedIconToFinish(
        splashScreenViewProvider: SplashScreenViewProvider,
        view: View,
        animatorSet: AnimatorSet
    ) {
        // If we want to wait for our Animated Vector Drawable to finish animating, we can compute
        // the remaining time to delay the start of the exit animation
        val delayMillis: Long = (
            splashScreenViewProvider.iconAnimationStartMillis +
                splashScreenViewProvider.iconAnimationDurationMillis
            ) - System.currentTimeMillis()
        view.postDelayed(delayMillis) { animatorSet.start() }
    }

    /**
     * Animates the content of the app in sync with the splash screen
     */
    private fun createContentAnimation(): ValueAnimator {
        val marginStart = resources.getDimension(R.dimen.content_animation_margin_start)
        val marginEnd = resources.getDimension(R.dimen.content_animation_margin_end)
        val marginAnimator = ValueAnimator.ofFloat(marginStart, marginEnd)
        marginAnimator.addUpdateListener { valueAnimator: ValueAnimator ->
            val container = findViewById<LinearLayout>(R.id.container)
            val marginTop = (valueAnimator.animatedValue as Float)
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                child.translationY = marginTop * (i + 1)
            }
        }
        marginAnimator.interpolator = DecelerateInterpolator()
        marginAnimator.duration = MARGIN_ANIMATION_DURATION.toLong()
        return marginAnimator
    }

    private fun setupKillButton() {
        findViewById<Button>(R.id.close_app).setOnClickListener {
            finishAndRemoveTask()

            // Don't do that in real life.
            // For the sake of this demo app, we kill the process so the next time the app is
            // launched, it will be a cold start and the splash screen will be visible.
            Process.killProcess(Process.myPid())
        }
    }

    private companion object {
        const val MOCK_DELAY = 200
        const val MARGIN_ANIMATION_DURATION = 800
        const val SPLASHSCREEN_ALPHA_ANIMATION_DURATION = 500
        const val SPLASHSCREEN_TY_ANIMATION_DURATION = 500
        const val WAIT_FOR_AVD_TO_FINISH = false
    }
}