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
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.ViewTreeObserver
import androidx.glance.unit.DpSize
import androidx.glance.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
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

    private val mUiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val mActivityRule: ActivityScenarioRule<AppWidgetHostTestActivity> =
        ActivityScenarioRule(AppWidgetHostTestActivity::class.java)

    // Ensure the screen starts in portrait and restore the orientation on leaving
    private val mOrientationRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                var orientation: Int = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                mScenario.onActivity {
                    orientation = it.resources.configuration.orientation.toActivityInfoOrientation()
                    it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                base.evaluate()
                mScenario.onActivity {
                    it.requestedOrientation = orientation
                }
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

    /** Change the orientation to landscape using [setOrientation] .*/
    fun setLandscapeOrientation() {
        setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }

    /** Change the orientation to portrait using [setOrientation] .*/
    fun setPortraitOrientation() {
        setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    /**
     * Change the orientation of the screen, then update the view sizes and reapply the RemoteViews.
     */
    private fun setOrientation(orientation: Int) {
        mScenario.onActivity { it.requestedOrientation = orientation }
        onIdle()
        mScenario.onActivity {
            it.updateAllSizes()
            it.reapplyRemoteViews()
        }
        onIdle()
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
                    optionsBundleOf(portraitSize, landscapeSize)
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
            if (mHostView.childCount > 0) latch.countDown()
        }
        mActivityRule.scenario.onActivity {
            mHostView.viewTreeObserver.addOnDrawListener(onDrawListener)
        }

        run()

        val countedDown = latch.await(5, TimeUnit.SECONDS)
        mActivityRule.scenario.onActivity {
            mHostView.viewTreeObserver.removeOnDrawListener(onDrawListener)
        }
        if (!countedDown && !test()) fail(condition)
    }

    private fun runAndWaitForChildren(action: () -> Unit) {
        runAndObserveUntilDraw("Expected new children on HostView within 5 seconds", action) {
            mHostView.childCount > 0
        }
    }
}

private fun Int.toActivityInfoOrientation(): Int =
    if (this == Configuration.ORIENTATION_PORTRAIT) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }