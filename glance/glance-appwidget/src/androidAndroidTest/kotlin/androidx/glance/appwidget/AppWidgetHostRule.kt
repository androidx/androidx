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

package androidx.glance.appwidget

import android.Manifest
import android.appwidget.AppWidgetHostView
import android.view.ViewTreeObserver
import androidx.glance.unit.DpSize
import androidx.glance.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SdkSuppress(minSdkVersion = 29)
class AppWidgetHostRule(private val defaultSize: DpSize = DpSize(200.dp, 300.dp)) : TestRule {

    private val mUiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val activityRule: ActivityScenarioRule<AppWidgetHostTestActivity> =
        ActivityScenarioRule(AppWidgetHostTestActivity::class.java)

    private var hostStarted = false
    lateinit var hostView: TestAppWidgetHostView
    var appWidgetId = 0

    val scenario: ActivityScenario<AppWidgetHostTestActivity>
        get() = activityRule.scenario

    override fun apply(base: Statement, description: Description) = object : Statement() {

        override fun evaluate() {
            activityRule.apply(base, description).evaluate()
            stopHost()
        }

        private fun stopHost() {
            if (hostStarted) {
                mUiAutomation.dropShellPermissionIdentity()
            }
        }
    }

    fun startHost() {
        mUiAutomation.adoptShellPermissionIdentity(Manifest.permission.BIND_APPWIDGET)
        hostStarted = true

        activityRule.scenario.onActivity { activity ->
            hostView = activity.bindAppWidget(defaultSize)
        }

        runAndWaitForChildren {
            appWidgetId = hostView.appWidgetId
            hostView.waitForRemoteViews()
        }
    }

    private fun runAndWaitForChildren(action: () -> Unit) {
        val latch = CountDownLatch(1)
        val onDrawListener = ViewTreeObserver.OnDrawListener {
            if (hostView.childCount > 0) latch.countDown()
        }
        activityRule.scenario.onActivity {
            hostView.viewTreeObserver.addOnDrawListener(onDrawListener)
        }

        action()

        val countedDown = latch.await(5, TimeUnit.SECONDS)
        activityRule.scenario.onActivity {
            hostView.viewTreeObserver.removeOnDrawListener(onDrawListener)
        }
        if (!countedDown && hostView.childCount == 0) {
            Assert.fail("Expected new children on HostView within 5 seconds")
        }
    }

    fun onHostActivity(block: (AppWidgetHostTestActivity) -> Unit) {
        scenario.onActivity(block)
    }

    fun onHostView(block: (AppWidgetHostView) -> Unit) {
        onHostActivity { block(hostView) }
    }
}