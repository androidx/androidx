/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplay
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 31)
class WindowInfoDeviceChangeTest {
    @Test
    @Config(minSdk = 33)
    fun noUpdateOnApplicationChange() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        var containerSize = IntSize.Zero
        lateinit var application: Application
        val composeView =
            activity.runOnUiThreadBlocking {
                ComposeView(activity).also {
                    it.setContent {
                        application = LocalContext.current.applicationContext as Application
                        containerSize = LocalWindowInfo.current.containerSize
                    }
                }
            }
        activity.runOnUiThreadBlocking { activity.setContentView(composeView) }
        ShadowLooper.shadowMainLooper().runToEndOfTasks()
        val initialContainerSize = containerSize
        val shadowDisplay = Shadows.shadowOf(ShadowDisplay.getDefaultDisplay())

        shadowDisplay.setWidth(1000)
        shadowDisplay.setHeight(2000)
        val configuration = Configuration(activity.applicationContext.resources.configuration)
        configuration.screenWidthDp =
            (1000 / activity.resources.displayMetrics.density).roundToInt()
        configuration.screenHeightDp =
            (2000 / activity.resources.displayMetrics.density).roundToInt()
        configuration.smallestScreenWidthDp = 1000
        configuration.setWindowSize(1000, 2000)

        val displayMetrics = DisplayMetrics()
        displayMetrics.setTo(activity.resources.displayMetrics)
        displayMetrics.widthPixels = 1000
        displayMetrics.heightPixels = 2000

        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(configuration, displayMetrics)

        val activityInfo =
            RuntimeEnvironment.getApplication()
                .packageManager
                .getActivityInfo(activity.componentName, 0)
        activityInfo.configChanges =
            activityInfo.configChanges or
                ActivityInfo.CONFIG_SCREEN_SIZE or
                ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE or
                ActivityInfo.CONFIG_SCREEN_LAYOUT or
                ActivityInfo.CONFIG_ORIENTATION or
                ActivityInfo.CONFIG_DENSITY
        val shadowPackageManager =
            Shadows.shadowOf(RuntimeEnvironment.getApplication().packageManager)
        shadowPackageManager.addOrUpdateActivity(activityInfo)

        activity.resources.configuration.updateFrom(configuration)
        application.resources.configuration.updateFrom(configuration)

        application.onConfigurationChanged(configuration)
        ShadowLooper.shadowMainLooper().runToEndOfTasks()
        activity.runOnUiThreadBlocking { assertThat(containerSize).isEqualTo(initialContainerSize) }
    }

    @Test
    fun containerSizeUpdatesWhenDeviceSizeChanges() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()

        var containerSize = IntSize.Zero
        val composeView =
            activity.runOnUiThreadBlocking {
                ComposeView(activity).also {
                    it.setContent { containerSize = LocalWindowInfo.current.containerSize }
                }
            }
        activity.runOnUiThreadBlocking { activity.setContentView(composeView) }

        activity.runOnUiThreadBlocking { assertThat(containerSize).isNotEqualTo(IntSize.Zero) }

        val shadowDisplay = Shadows.shadowOf(ShadowDisplay.getDefaultDisplay())

        shadowDisplay.setWidth(1000)
        shadowDisplay.setHeight(2000)
        val configuration = Configuration(activity.applicationContext.resources.configuration)
        configuration.screenWidthDp =
            (1000 / activity.resources.displayMetrics.density).roundToInt()
        configuration.screenHeightDp =
            (2000 / activity.resources.displayMetrics.density).roundToInt()
        configuration.smallestScreenWidthDp = 1000
        configuration.setWindowSize(1000, 2000)

        val displayMetrics = DisplayMetrics()
        displayMetrics.setTo(activity.resources.displayMetrics)
        displayMetrics.widthPixels = 1000
        displayMetrics.heightPixels = 2000

        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(configuration, displayMetrics)

        val activityInfo =
            RuntimeEnvironment.getApplication()
                .packageManager
                .getActivityInfo(activity.componentName, 0)
        activityInfo.configChanges =
            activityInfo.configChanges or
                ActivityInfo.CONFIG_SCREEN_SIZE or
                ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE or
                ActivityInfo.CONFIG_SCREEN_LAYOUT or
                ActivityInfo.CONFIG_ORIENTATION or
                ActivityInfo.CONFIG_DENSITY
        val shadowPackageManager =
            Shadows.shadowOf(RuntimeEnvironment.getApplication().packageManager)
        shadowPackageManager.addOrUpdateActivity(activityInfo)

        activity.resources.configuration.updateFrom(configuration)
        activity.application.resources.configuration.updateFrom(configuration)
        val windowManager = activity.applicationContext.getSystemService(WindowManager::class.java)

        controller.configurationChange(configuration, displayMetrics)
        ShadowLooper.shadowMainLooper().runToEndOfTasks()

        activity.runOnUiThreadBlocking { assertThat(containerSize).isEqualTo(IntSize(1000, 2000)) }
    }

    inline fun <T> Activity.runOnUiThreadBlocking(crossinline block: () -> T): T {
        val latch = CountDownLatch(1)
        var result: T? = null
        runOnUiThread {
            result = block()
            latch.countDown()
        }
        latch.await()
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /**
     * There's no public API to change the window size set in a Configuration, so we have to use
     * reflection to get access to it.
     */
    fun Configuration.setWindowSize(width: Int, height: Int) {
        val windowConfigField = Configuration::class.java.getDeclaredField("windowConfiguration")
        windowConfigField.isAccessible = true
        val windowConfiguration = windowConfigField.get(this)
        val windowConfigurationClass = Class.forName("android.app.WindowConfiguration")
        val setBoundsMethod =
            windowConfigurationClass.getDeclaredMethod("setBounds", Rect::class.java)
        val bounds = Rect(0, 0, width, height)
        setBoundsMethod.invoke(windowConfiguration, bounds)
        val setAppBoundsMethod =
            windowConfigurationClass.getDeclaredMethod("setAppBounds", Rect::class.java)
        setAppBoundsMethod.invoke(windowConfiguration, bounds)
        val setMaxBoundsMethod =
            windowConfigurationClass.getDeclaredMethod("setMaxBounds", Rect::class.java)
        setMaxBoundsMethod.invoke(windowConfiguration, bounds)
    }
}
