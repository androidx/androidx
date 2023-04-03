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

package androidx.glance.appwidget.macrobenchmark

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Trace
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@RequiresApi(Build.VERSION_CODES.Q)
class AppWidgetHostRule(
    private var mPortraitSize: DpSize = DpSize(200.dp, 300.dp),
    private var mLandscapeSize: DpSize = DpSize(300.dp, 200.dp),
) : TestRule {
    private val mInstrumentation = InstrumentationRegistry.getInstrumentation()
    private val mUiAutomation = mInstrumentation.uiAutomation
    private val targetComponent = ComponentName(
        "androidx.glance.appwidget.macrobenchmark.target",
        "androidx.glance.appwidget.macrobenchmark.target.BasicAppWidgetReceiver",
    )

    private val mActivityRule: ActivityScenarioRule<AppWidgetHostTestActivity> =
        ActivityScenarioRule(
            Intent()
                .setComponent(
                    ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        AppWidgetHostTestActivity::class.java,
                    )
                ).putExtra(AppWidgetHostTestActivity.EXTRA_TARGET_RECEIVER, targetComponent)
        )

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
    private lateinit var hostView: TestAppWidgetHostView
    private val appWidgetId: Int get() = hostView.appWidgetId
    private val mContext = ApplicationProvider.getApplicationContext<Context>()

    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            mInnerRules.apply(base, description).evaluate()
            if (mHostStarted) {
                mUiAutomation.dropShellPermissionIdentity()
            }
        }
    }

    /**
     * Start the host and bind the app widget.
     * Measures time from binding an app widget to receiving the first RemoteViews.
     */
    suspend fun startHost() {
        mUiAutomation.adoptShellPermissionIdentity(Manifest.permission.BIND_APPWIDGET)
        mHostStarted = true

        Trace.beginSection("appWidgetInitialUpdate")
        mActivityRule.scenario.onActivity { activity ->
            hostView = checkNotNull(activity.bindAppWidget(mPortraitSize, mLandscapeSize)) {
                "Failed to bind widget and create host view"
            }
        }
        hostView.waitForRemoteViews()
        Trace.endSection()
    }

    /**
     * Measures time from sending APPWIDGET_UPDATE broadcast to receiving RemoteViews.
     */
    suspend fun updateAppWidget() {
        val intent = Intent(GlanceAppWidgetReceiver.ACTION_DEBUG_UPDATE)
            .setPackage("androidx.glance.appwidget.macrobenchmark.target")
            .setComponent(targetComponent)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        Trace.beginSection("appWidgetUpdate")
        hostView.runAndWaitForRemoteViews {
            mContext.sendBroadcast(intent)
        }
        Trace.endSection()
    }
}
