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

package androidx.core.splashscreen.test

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.hamcrest.core.IsNull
import org.junit.Assert
import kotlin.reflect.KClass

private const val SPLASH_SCREEN_STYLE_ICON = 1
private const val KEY_SPLASH_SCREEN_STYLE: String = "android.activity.splashScreenStyle"
private const val BASIC_SAMPLE_PACKAGE: String = "androidx.core.splashscreen.test"
private const val LAUNCH_TIMEOUT: Long = 5000

/**
 * Start an activity simulating a launch from the launcher
 * to ensure the splash screen is shown
 */
fun startActivityWithSplashScreen(
    activityClass: KClass<out SplashScreenTestControllerHolder>,
    device: UiDevice,
    intentModifier: ((Intent) -> Unit)? = null
): SplashScreenTestController {
    // Start from the home screen
    device.pressHome()

    // Wait for launcher
    val launcherPackage: String = device.launcherPackageName
    Assert.assertThat(launcherPackage, IsNull.notNullValue())
    device.wait(
        Until.hasObject(By.pkg(launcherPackage).depth(0)),
        LAUNCH_TIMEOUT
    )

    // Launch the app
    val context = ApplicationProvider.getApplicationContext<Context>()
    val baseIntent = context.packageManager.getLaunchIntentForPackage(
        BASIC_SAMPLE_PACKAGE
    )
    val intent = Intent(baseIntent).apply {
        component = ComponentName(BASIC_SAMPLE_PACKAGE, activityClass.qualifiedName!!)
        intentModifier?.invoke(this)
    }

    val monitor = object : Instrumentation.ActivityMonitor(
        activityClass.qualifiedName!!,
        Instrumentation.ActivityResult(0, Intent()), false
    ) {
        override fun onStartActivity(intent: Intent?): Instrumentation.ActivityResult? {
            return if (intent?.component?.packageName == BASIC_SAMPLE_PACKAGE) {
                Instrumentation.ActivityResult(0, Intent())
            } else {
                null
            }
        }
    }
    InstrumentationRegistry.getInstrumentation().addMonitor(monitor)

    context.startActivity(
        intent,
        // Force the splash screen to be shown with an icon
        Bundle().apply { putInt(KEY_SPLASH_SCREEN_STYLE, SPLASH_SCREEN_STYLE_ICON) }
    )
    Assert.assertTrue(
        device.wait(
            Until.hasObject(By.pkg(BASIC_SAMPLE_PACKAGE).depth(0)),
            LAUNCH_TIMEOUT
        )
    )
    val splashScreenTestActivity =
        monitor.waitForActivityWithTimeout(LAUNCH_TIMEOUT) as SplashScreenTestControllerHolder?
    if (splashScreenTestActivity == null) {
        Assert.fail(
            activityClass.simpleName!! + " was not launched after " +
                "$LAUNCH_TIMEOUT ms"
        )
    }
    return splashScreenTestActivity!!.controller
}
