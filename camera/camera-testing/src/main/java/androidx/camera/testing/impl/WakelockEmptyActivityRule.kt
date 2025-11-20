/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.camera.core.Logger
import androidx.camera.testing.impl.Api27Impl.setShowWhenLocked
import androidx.camera.testing.impl.Api27Impl.setTurnScreenOn
import androidx.camera.testing.impl.CoreAppTestUtil.clearDeviceUI
import androidx.camera.testing.impl.activity.EmptyActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A rule that opens an empty Activity and wakes the device to prevent test failures.
 *
 * By default, all brands will be enabled (brandsToEnable == null). Caller can specify the brand
 * list to enable the rule via the [brandsToEnable] parameter.
 */
public class WakelockEmptyActivityRule(private val brandsToEnable: List<String>? = null) :
    TestRule {
    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                if (
                    brandsToEnable != null &&
                        !brandsToEnable.any { Build.BRAND.equals(it, ignoreCase = true) }
                ) {
                    base.evaluate()
                    return
                }

                val instrumentation = InstrumentationRegistry.getInstrumentation()
                clearDeviceUI(instrumentation)
                var activityRef: EmptyActivity? = null
                try {
                    activityRef =
                        CoreAppTestUtil.launchActivity(
                                instrumentation,
                                EmptyActivity::class.java,
                                Intent(Intent.ACTION_MAIN).apply {
                                    setClassName(
                                        ApplicationProvider.getApplicationContext<Context>()
                                            .packageName,
                                        EmptyActivity::class.java.name,
                                    )
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                            ?.also { activity ->
                                instrumentation.runOnMainSync {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                        activity.setShowWhenLocked()
                                        activity.setTurnScreenOn()
                                        activity.window.addFlags(
                                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                        )
                                    } else {
                                        @Suppress("DEPRECATION")
                                        activity.window.addFlags(
                                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                                        )
                                    }
                                }
                            }
                } catch (_: Exception) {
                    Logger.w("WakelockEmptyActivityRule", "Fail to open Activity + wakelock")
                }

                try {
                    base.evaluate()
                } finally {
                    if (activityRef != null) {
                        instrumentation.runOnMainSync { activityRef.finish() }
                        instrumentation.waitForIdleSync()
                    }
                }
            }
        }
}

@RequiresApi(Build.VERSION_CODES.O_MR1)
public object Api27Impl {
    public fun Activity.setShowWhenLocked() {
        setShowWhenLocked(true)
    }

    public fun Activity.setTurnScreenOn() {
        setTurnScreenOn(true)
    }
}
