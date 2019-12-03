/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test.android

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.compose.Composable
import androidx.test.rule.ActivityTestRule
import androidx.ui.animation.transitionsEnabled
import androidx.ui.core.Density
import androidx.ui.core.setContent
import androidx.ui.engine.geometry.Rect
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.ComposeTestCaseSetup
import androidx.ui.test.ComposeTestRule
import androidx.ui.test.throwOnRecomposeTimeout
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Android specific implementation of [ComposeTestRule].
 */
class AndroidComposeTestRule(
    private val disableTransitions: Boolean = false,
    private val shouldThrowOnRecomposeTimeout: Boolean = false
) : ComposeTestRule {

    val activityTestRule = ActivityTestRule<Activity>(Activity::class.java)

    private val handler: Handler = Handler(Looper.getMainLooper())

    override val density: Density get() = Density(activityTestRule.activity)

    override val displayMetrics: DisplayMetrics get() =
        activityTestRule.activity.resources.displayMetrics

    override fun apply(base: Statement, description: Description?): Statement {
        return activityTestRule.apply(AndroidComposeStatement(base), description)
    }

    override fun runOnUiThread(action: () -> Unit) {
        // Workaround for lambda bug in IR
        activityTestRule.activity.runOnUiThread(object : Runnable {
            override fun run() {
                action.invoke()
            }
        })
    }

    /**
     * Use this in your tests to setup the UI content to be tested. This should be called exactly
     * once per test.
     */
    @SuppressWarnings("SyntheticAccessor")
    override fun setContent(composable: @Composable() () -> Unit) {
        val drawLatch = CountDownLatch(1)
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                drawLatch.countDown()
                val contentViewGroup =
                    activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)
                contentViewGroup.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        }
        val runnable: Runnable = object : Runnable {
            override fun run() {
                activityTestRule.activity.setContent(composable)
                val contentViewGroup =
                    activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)
                contentViewGroup.viewTreeObserver.addOnGlobalLayoutListener(listener)
            }
        }
        activityTestRule.runOnUiThread(runnable)
        drawLatch.await(1, TimeUnit.SECONDS)
    }

    override fun forGivenContent(composable: @Composable() () -> Unit): ComposeTestCaseSetup {
        val testCase = object : ComposeTestCase {
            @Composable
            override fun emitContent() {
                composable()
            }
        }
        return AndroidComposeTestCaseSetup(
            this,
            testCase,
            activityTestRule.activity
        )
    }

    override fun forGivenTestCase(testCase: ComposeTestCase): ComposeTestCaseSetup {
        return AndroidComposeTestCaseSetup(
            this,
            testCase,
            activityTestRule.activity
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun captureScreenOnIdle(): Bitmap {
        AndroidSemanticsTreeInteraction(throwOnRecomposeTimeOut = true, selector = { true })
            .waitForIdleCompose()
        val contentView = activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)

        val screenRect = Rect.fromLTWH(
            0f,
            0f,
            contentView.width.toFloat(),
            contentView.height.toFloat()
        )
        return captureRegionToBitmap(screenRect, handler, activityTestRule.activity.window)
    }

    inner class AndroidComposeStatement(
        private val base: Statement
    ) : Statement() {
        override fun evaluate() {
            transitionsEnabled = !disableTransitions
            throwOnRecomposeTimeout = shouldThrowOnRecomposeTimeout
            try {
                base.evaluate()
            } finally {
                transitionsEnabled = true
                throwOnRecomposeTimeout = false
            }
        }
    }
}
