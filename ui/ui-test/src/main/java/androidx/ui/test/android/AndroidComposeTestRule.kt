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

import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.SparseArray
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.Composable
import androidx.compose.Recomposer
import androidx.test.rule.ActivityTestRule
import androidx.ui.animation.transitionsEnabled
import androidx.ui.core.setContent
import androidx.ui.input.textInputServiceFactory
import androidx.ui.test.AnimationClockTestRule
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.ComposeTestCaseSetup
import androidx.ui.test.ComposeTestRule
import androidx.ui.test.TextInputServiceForTests
import androidx.ui.test.isOnUiThread
import androidx.ui.test.runOnUiThread
import androidx.ui.test.waitForIdle
import androidx.ui.unit.Density
import org.junit.runner.Description
import org.junit.runners.model.Statement

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
inline fun <reified T : ComponentActivity> AndroidComposeTestRule(
    recomposer: Recomposer? = null,
    disableTransitions: Boolean = false
): AndroidComposeTestRule<T> {
    // TODO(b/138993381): By launching custom activities we are losing control over what content is
    // already there. This is issue in case the user already set some compose content and decides
    // to set it again via our API. In such case we won't be able to dispose the old composition.
    // Other option would be to provide a smaller interface that does not expose these methods.
    return AndroidComposeTestRule(ActivityTestRule(T::class.java), recomposer, disableTransitions)
}

/**
 * Android specific implementation of [ComposeTestRule].
 *
 * If [recomposer] is `null` the thread-specific [Recomposer.current] will be used when
 * [setContent] is called.
 */
class AndroidComposeTestRule<T : ComponentActivity>(
    // TODO(b/153623653): Remove activityTestRule from arguments when AndroidComposeTestRule can
    //  work with any kind of Activity launcher.
    val activityTestRule: ActivityTestRule<T>,
    val recomposer: Recomposer? = null,
    private val disableTransitions: Boolean = false
) : ComposeTestRule {

    override val clockTestRule = AnimationClockTestRule()

    private val handler: Handler = Handler(Looper.getMainLooper())
    private var disposeContentHook: (() -> Unit)? = null

    override val density: Density get() = Density(activityTestRule.activity)

    override val displayMetrics: DisplayMetrics get() =
        activityTestRule.activity.resources.displayMetrics

    override fun apply(base: Statement, description: Description?): Statement {
        val activityTestRuleStatement = activityTestRule.apply(base, description)
        val composeTestRuleStatement = AndroidComposeStatement(activityTestRuleStatement)
        return clockTestRule.apply(composeTestRuleStatement, description)
    }

    /**
     * @throws IllegalStateException if called more than once per test.
     */
    @SuppressWarnings("SyntheticAccessor")
    override fun setContent(composable: @Composable () -> Unit) {
        check(disposeContentHook == null) {
            "Cannot call setContent twice per test!"
        }

        runOnUiThread {
            val composition = activityTestRule.activity.setContent(
                recomposer ?: Recomposer.current(),
                composable
            )
            val contentViewGroup =
                activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)
            // AndroidComposeView is postponing the composition till the saved state is restored.
            // We will emulate the restoration of the empty state to trigger the real composition.
            contentViewGroup.getChildAt(0).restoreHierarchyState(SparseArray())
            disposeContentHook = {
                composition.dispose()
            }
        }

        if (!isOnUiThread()) {
            // Only wait for idleness if not on the UI thread. If we are on the UI thread, the
            // caller clearly wants to keep tight control over execution order, so don't go
            // executing future tasks on the main thread.
            waitForIdle()
        }
    }

    override fun forGivenContent(composable: @Composable () -> Unit): ComposeTestCaseSetup {
        return forGivenTestCase(object : ComposeTestCase {
            @Composable
            override fun emitContent() {
                composable()
            }
        })
    }

    override fun forGivenTestCase(testCase: ComposeTestCase): ComposeTestCaseSetup {
        return AndroidComposeTestCaseSetup(
            testCase,
            activityTestRule.activity
        )
    }

    inner class AndroidComposeStatement(
        private val base: Statement
    ) : Statement() {
        override fun evaluate() {
            val oldTextInputFactory = textInputServiceFactory
            beforeEvaluate()
            try {
                base.evaluate()
            } finally {
                afterEvaluate()
                textInputServiceFactory = oldTextInputFactory
            }
        }

        private fun beforeEvaluate() {
            transitionsEnabled = !disableTransitions
            AndroidOwnerRegistry.setupRegistry()
            FirstDrawRegistry.setupRegistry()
            registerComposeWithEspresso()
            textInputServiceFactory = {
                TextInputServiceForTests(it)
            }
        }

        private fun afterEvaluate() {
            transitionsEnabled = true
            AndroidOwnerRegistry.tearDownRegistry()
            FirstDrawRegistry.tearDownRegistry()
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
