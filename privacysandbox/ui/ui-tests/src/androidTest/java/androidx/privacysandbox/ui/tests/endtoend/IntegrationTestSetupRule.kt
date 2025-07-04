/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.tests.endtoend

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.BackwardCompatUtil
import androidx.privacysandbox.ui.integration.testingutils.TestEventListener
import androidx.privacysandbox.ui.tests.util.TestSessionManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class IntegrationTestSetupRule(val invokeBackwardsCompatFlow: Boolean) : TestRule {

    companion object {
        const val INITIAL_HEIGHT = 100
        const val INITIAL_WIDTH = 100
    }

    val context: Context = InstrumentationRegistry.getInstrumentation().context

    lateinit var activityScenario: ActivityScenario<MainActivity>
    lateinit var view: SandboxedSdkView
    lateinit var eventListener: TestEventListener
    lateinit var linearLayout: LinearLayout
    lateinit var sessionManager: TestSessionManager

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (!invokeBackwardsCompatFlow) {
                    // Device needs to support remote provider to invoke non-backward-compat flow.
                    assumeTrue(BackwardCompatUtil.canProviderBeRemote())
                }

                sessionManager = TestSessionManager(context, invokeBackwardsCompatFlow)
                activityScenario = ActivityScenario.launch(MainActivity::class.java)
                activityScenario.onActivity { activity ->
                    view = SandboxedSdkView(context)
                    eventListener = TestEventListener()
                    view.setEventListener(eventListener)

                    linearLayout = LinearLayout(context)
                    linearLayout.layoutParams =
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    linearLayout.setBackgroundColor(Color.RED)

                    activity.setContentView(linearLayout)
                    view.layoutParams = LinearLayout.LayoutParams(INITIAL_WIDTH, INITIAL_HEIGHT)
                    linearLayout.addView(view)
                }

                base.evaluate()
            }
        }
    }
}
