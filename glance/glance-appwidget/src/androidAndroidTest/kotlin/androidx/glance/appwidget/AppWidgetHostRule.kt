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
import android.app.Activity
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import org.hamcrest.Matcher
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.fail

@SdkSuppress(minSdkVersion = 29)
class AppWidgetHostRule(
    private var mPortraitSize: DpSize = DpSize(200.dp, 300.dp),
    private var mLandscapeSize: DpSize = DpSize(300.dp, 200.dp)
) : TestRule {

    val portraitSize: DpSize
        get() = mPortraitSize
    val landscapeSize: DpSize
        get() = mLandscapeSize

    private val mInstrumentation = InstrumentationRegistry.getInstrumentation()
    private val mUiAutomation = mInstrumentation.uiAutomation

    private val mActivityRule: ActivityScenarioRule<AppWidgetHostTestActivity> =
        ActivityScenarioRule(AppWidgetHostTestActivity::class.java)

    private val mUiDevice = UiDevice.getInstance(mInstrumentation)

    // Ensure the screen starts in portrait and restore the orientation on leaving
    private val mOrientationRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                mUiDevice.freezeRotation()
                mUiDevice.setOrientationNatural()
                base.evaluate()
                mUiDevice.unfreezeRotation()
            }
        }
    }

    private val mInnerRules = RuleChain.outerRule(mActivityRule).around(mOrientationRule)

    private var mHostStarted = false
    lateinit var mHostView: TestAppWidgetHostView
    private var mAppWidgetId = 0
    private val mScenario: ActivityScenario<AppWidgetHostTestActivity>
        get() = mActivityRule.scenario
    private val mContext = ApplicationProvider.getApplicationContext<Context>()

    override fun apply(base: Statement, description: Description) = object : Statement() {

        override fun evaluate() {
            mInnerRules.apply(base, description).evaluate()
            stopHost()
        }

        private fun stopHost() {
            if (mHostStarted) {
                mUiAutomation.dropShellPermissionIdentity()
            }
        }
    }

    /** Start the host and bind the app widget. */
    fun startHost() {
        mUiAutomation.adoptShellPermissionIdentity(Manifest.permission.BIND_APPWIDGET)
        mHostStarted = true

        mActivityRule.scenario.onActivity { activity ->
            mHostView = activity.bindAppWidget(mPortraitSize, mLandscapeSize)
        }

        runAndWaitForChildren {
            mAppWidgetId = mHostView.appWidgetId
            mHostView.waitForRemoteViews()
        }
    }

    fun onHostActivity(block: (AppWidgetHostTestActivity) -> Unit) {
        mScenario.onActivity(block)
    }

    fun onHostView(block: (AppWidgetHostView) -> Unit) {
        onHostActivity { block(mHostView) }
    }

    /** Change the orientation to landscape.*/
    fun setLandscapeOrientation() {
        onView(isRoot()).perform(orientationLandscape())
    }

    /** Change the orientation to portrait.*/
    fun setPortraitOrientation() {
        onView(isRoot()).perform(orientationPortrait())
    }

    /**
     * Set the sizes for portrait and landscape for the host view.
     *
     * If specified, the options bundle for the AppWidget is updated and the code waits for the
     * new RemoteViews from the provider.
     */
    fun setSizes(portraitSize: DpSize, landscapeSize: DpSize, updateRemoteViews: Boolean = true) {
        mLandscapeSize = landscapeSize
        mPortraitSize = portraitSize
        mScenario.onActivity {
            mHostView.setSizes(portraitSize, landscapeSize)
        }

        if (updateRemoteViews) {
            runAndWaitForChildren {
                mHostView.resetRemoteViewsLatch()
                AppWidgetManager.getInstance(mContext).updateAppWidgetOptions(
                    mAppWidgetId,
                    optionsBundleOf(listOf(portraitSize, landscapeSize))
                )
                mHostView.waitForRemoteViews()
            }
        }
    }

    fun runAndObserveUntilDraw(
        condition: String = "Expected condition to be met within 5 seconds",
        run: () -> Unit = {},
        test: () -> Boolean
    ) {
        val latch = CountDownLatch(1)
        val onDrawListener = ViewTreeObserver.OnDrawListener {
            if (mHostView.childCount > 0 && test()) latch.countDown()
        }
        mActivityRule.scenario.onActivity {
            mHostView.viewTreeObserver.addOnDrawListener(onDrawListener)
        }

        run()

        try {
            if (test()) return
            val interval = 200L
            for (timeout in 0..5000L step interval) {
                val countedDown = latch.await(interval, TimeUnit.MILLISECONDS)
                if (countedDown || test()) return
            }
            fail(condition)
        } finally {
            latch.countDown() // make sure it's released in all conditions
            mActivityRule.scenario.onActivity {
                mHostView.viewTreeObserver.removeOnDrawListener(onDrawListener)
            }
        }
    }

    private fun runAndWaitForChildren(action: () -> Unit) {
        runAndObserveUntilDraw("Expected new children on HostView within 5 seconds", action) {
            mHostView.childCount > 0
        }
    }

    private inner class OrientationChangeAction constructor(private val orientation: Int) :
        ViewAction {
        override fun getConstraints(): Matcher<View> = isRoot()

        override fun getDescription() = "change orientation to $orientationName"

        private val orientationName: String
            get() =
                if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    "landscape"
                } else {
                    "portrait"
                }

        override fun perform(uiController: UiController, view: View) {
            uiController.loopMainThreadUntilIdle()
            mActivityRule.scenario.onActivity { it.requestedOrientation = orientation }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // Somehow, before Android S, changing the orientation doesn't trigger the
                // onConfigurationChange
                uiController.loopMainThreadUntilIdle()
                mScenario.onActivity {
                    it.updateAllSizes(it.resources.configuration.orientation)
                    it.reapplyRemoteViews()
                }
            }
            val resumedActivities: Collection<Activity> =
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            if (resumedActivities.isEmpty()) {
                throw RuntimeException("Could not change orientation")
            }
        }
    }

    private fun orientationLandscape(): ViewAction {
        return OrientationChangeAction(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }

    private fun orientationPortrait(): ViewAction {
        return OrientationChangeAction(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }
}
