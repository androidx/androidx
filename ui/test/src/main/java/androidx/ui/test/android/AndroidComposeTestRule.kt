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
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.compose.Compose
import androidx.compose.composer
import androidx.test.rule.ActivityTestRule
import androidx.ui.animation.transitionsEnabled
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Density
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

    val activityTestRule = ActivityTestRule<DefaultTestActivity>(DefaultTestActivity::class.java)

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
     * <p>
     * Please note that you need to add the following activity
     * [androidx.ui.test.android.DefaultTestActivity] to you tests manifest in order to use this.
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
                setContentInternal(composable)
                val contentViewGroup =
                    activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)
                contentViewGroup.viewTreeObserver.addOnGlobalLayoutListener(listener)
            }
        }
        activityTestRule.runOnUiThread(runnable)
        drawLatch.await(1, TimeUnit.SECONDS)
    }

    private fun setContentInternal(composable: @Composable() () -> Unit) {
        activityTestRule.activity.setContentView(FrameLayout(activityTestRule.activity).apply {
            Compose.composeInto(this, null, composable = @Composable {
                CraneWrapper {
                    composable()
                }
            })
        })
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

class DefaultTestActivity : Activity() {
    var hasFocusLatch = CountDownLatch(1)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hasFocusLatch.countDown()
        }
    }
}
