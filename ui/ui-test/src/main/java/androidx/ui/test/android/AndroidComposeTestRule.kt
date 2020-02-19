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
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.compose.Composable
import androidx.compose.Compose
import androidx.test.rule.ActivityTestRule
import androidx.ui.animation.transitionsEnabled
import androidx.ui.core.AndroidComposeView
import androidx.ui.core.setContent
import androidx.ui.geometry.Rect
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.ComposeTestCaseSetup
import androidx.ui.test.ComposeTestRule
import androidx.ui.unit.Density
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Factory method to provide implementation of [AndroidComposeTestRule].
 *
 * This method is useful for tests that require a custom Activity. This is usually the case for
 * app tests. Make sure that you add the provided activity into your app's manifest file (usually
 * in main/AndroidManifest.xml).
 *
 * If you don't care about specific activity and just want to test composables in general, see
 * [AndroidComposeTestRule].
 */
inline fun <reified T : Activity> AndroidComposeTestRule(
    disableTransitions: Boolean = false
): AndroidComposeTestRule<T> {
    // TODO(b/138993381): By launching custom activities we are losing control over what content is
    // already there. This is issue in case the user already set some compose content and decides
    // to set it again via our API. In such case we won't be able to dispose the old composition.
    // Other option would be to provide a smaller interface that does not expose these methods.
    return AndroidComposeTestRule(T::class.java, disableTransitions)
}

/**
 * Android specific implementation of [ComposeTestRule].
 */
class AndroidComposeTestRule<T : Activity>(
    activityClass: Class<T>,
    private val disableTransitions: Boolean = false
) : ComposeTestRule {

    val activityTestRule = ActivityTestRule<T>(activityClass)

    private val handler: Handler = Handler(Looper.getMainLooper())
    private var disposeContentHook: (() -> Unit)? = null

    override val density: Density get() = Density(activityTestRule.activity)

    override val displayMetrics: DisplayMetrics get() =
        activityTestRule.activity.resources.displayMetrics

    override fun apply(base: Statement, description: Description?): Statement {
        return activityTestRule.apply(AndroidComposeStatement(base), description)
    }

    override fun <T> runOnUiThread(action: () -> T): T {
        // Workaround for lambda bug in IR
        var result: T? = null
        activityTestRule.runOnUiThread(object : Runnable {
            override fun run() {
                result = action.invoke()
            }
        })
        return result!!
    }

    override fun <T> runOnIdleCompose(action: () -> T): T {
        // Method below make sure that compose is idle.
        SynchronizedTreeCollector.waitForIdle()
        // Execute the action on ui thread in a blocking way.
        return runOnUiThread(action)
    }

    /**
     * @throws IllegalStateException if called more than once per test.
     */
    @SuppressWarnings("SyntheticAccessor")
    override fun setContent(composable: @Composable() () -> Unit) {
        check(disposeContentHook == null) {
            "Cannot call setContent twice per test!"
        }

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
                val view = findComposeView(activityTestRule.activity)
                disposeContentHook = {
                    Compose.disposeComposition((view as AndroidComposeView).root,
                        activityTestRule.activity, null)
                }
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
        SynchronizedTreeCollector.waitForIdle()
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
            ComposeIdlingResource.registerSelfIntoEspresso()
            try {
                base.evaluate()
            } finally {
                transitionsEnabled = true
                // Dispose the content
                if (disposeContentHook != null) {
                    runOnUiThread {
                        // NOTE: currently, calling dispose after an exception that happened during
                        // composition is not a safe call. Compose runtime should fix this, and then
                        // this call will be okay. At the moment, however, calling this could
                        // itself produce an exception which will then obscure the original
                        // exception. To fix this, we will just wrap this call in a try/catch of
                        // its own
                        try {
                            disposeContentHook!!()
                        } catch (e: Exception) {
                            // ignore
                        }
                        disposeContentHook = null
                    }
                }
            }
        }
    }

    // TODO(pavlis): These methods are only needed because we don't have an API to purge all
    //  compositions from the app. Remove them once we have the option.
    private fun findComposeView(activity: Activity): AndroidComposeView? {
        return findComposeView(activity.findViewById(android.R.id.content) as ViewGroup)
    }

    private fun findComposeView(view: View): AndroidComposeView? {
        if (view is AndroidComposeView) {
            return view
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val composeView = findComposeView(view.getChildAt(i))
                if (composeView != null) {
                    return composeView
                }
            }
        }
        return null
    }
}