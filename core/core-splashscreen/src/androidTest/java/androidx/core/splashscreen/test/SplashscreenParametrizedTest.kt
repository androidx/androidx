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

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

@LargeTest
@RunWith(Parameterized::class)
public class SplashscreenParametrizedTest(
    public val name: String,
    public val activityClass: KClass<out SplashScreenTestControllerHolder>
) {

    private lateinit var device: UiDevice

    public companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        public fun data(): Iterable<Array<Any>> {
            return listOf(
                arrayOf("Platform", SplashScreenWithIconBgTestActivity::class),
                arrayOf("AppCompat", SplashScreenAppCompatTestActivity::class)
            )
        }
    }

    @Before
    public fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    public fun compatAttributePopulated() {
        val activity = startActivityWithSplashScreen()
        assertEquals(1234, activity.duration)
        assertEquals(R.color.bg_launcher, activity.splashscreenBackgroundId)
        val expectedTheme =
            if (activity.isCompatActivity) R.style.Theme_Test_AppCompat else R.style.Theme_Test
        assertEquals(expectedTheme, activity.finalAppTheme)
        assertEquals(R.drawable.android, activity.splashscreenIconId)
    }

    @Test
    public fun exitAnimationListenerCalled() {
        val activity = startActivityWithSplashScreen {
            // Clear out any previous instances
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            it.putExtra(EXTRA_ANIMATION_LISTENER, true)
        }
        assertTrue(activity.exitAnimationListenerLatch.await(2, TimeUnit.SECONDS))
    }

    @Test
    public fun splashScreenWaited() {
        val activity = startActivityWithSplashScreen {
            // Clear out any previous instances
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            it.putExtra(EXTRA_SPLASHSCREEN_WAIT, true)
        }
        assertTrue(
            "Waiting condition was never checked",
            activity.waitedLatch.await(2, TimeUnit.SECONDS)
        )
        assertFalse(
            "Activity should not have been drawn", activity.hasDrawn
        )
        activity.waitBarrier.set(false)
        assertTrue(
            "Activity was never drawn",
            activity.drawnLatch.await(2, TimeUnit.SECONDS)
        )
    }

    @Test
    public fun exitAnimationListenerCalledAfterWait() {
        val activity = startActivityWithSplashScreen {
            // Clear out any previous instances
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            it.putExtra(EXTRA_SPLASHSCREEN_WAIT, true)
            it.putExtra(EXTRA_ANIMATION_LISTENER, true)
        }
        activity.waitBarrier.set(false)
        assertTrue(
            "Activity was never drawn",
            activity.drawnLatch.await(2, TimeUnit.SECONDS)
        )
        assertTrue(activity.exitAnimationListenerLatch.await(2, TimeUnit.SECONDS))
    }

    @Test
    public fun splashScreenViewRemoved() {
        val activity = startActivityWithSplashScreen {
            // Clear out any previous instances
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            it.putExtra(EXTRA_ANIMATION_LISTENER, true)
        }
        activity.exitAnimationListenerLatch.await(2, TimeUnit.SECONDS)
        assertNull(
            "Splash screen view was not removed from its parent",
            activity.splashScreenView!!.parent
        )
    }

    // The vector drawable of the starting window isn't scaled
    // correctly pre 23
    @SdkSuppress(minSdkVersion = 23)
    @Test
    public fun splashscreenViewScreenshotComparison() {
        val activity = startActivityWithSplashScreen {
            // Clear out any previous instances
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            it.putExtra(EXTRA_SPLASHSCREEN_WAIT, true)
            it.putExtra(EXTRA_ANIMATION_LISTENER, true)
            it.putExtra(EXTRA_SPLASHSCREEN_VIEW_SCREENSHOT, true)
        }
        assertTrue(activity.waitedLatch.await(2, TimeUnit.SECONDS))
        activity.waitBarrier.set(false)
        activity.exitAnimationListenerLatch.await(2, TimeUnit.SECONDS)

        compareBitmaps(activity.splashScreenScreenshot!!, activity.splashScreenViewScreenShot!!)
    }

    private fun compareBitmaps(
        beforeScreenshot: Bitmap,
        afterScreenshot: Bitmap
    ) {
        val beforeBuffer = IntArray(beforeScreenshot.width * beforeScreenshot.height)
        beforeScreenshot.getPixels(
            beforeBuffer, 0, beforeScreenshot.width, 0, 0,
            beforeScreenshot.width, beforeScreenshot.height
        )

        val afterBuffer = IntArray(afterScreenshot.width * afterScreenshot.height)
        afterScreenshot.getPixels(
            afterBuffer, 0, afterScreenshot.width, 0, 0,
            afterScreenshot.width, afterScreenshot.height
        )

        val matcher = MSSIMMatcher(0.99).compareBitmaps(
            beforeBuffer, afterBuffer, afterScreenshot.width,
            afterScreenshot.height
        )

        if (!matcher.matches) {
            val bundle = Bundle()
            val diff = matcher.diff?.writeToDevice("diff.png")
            bundle.putString("splashscreen_diff", diff?.absolutePath)
            bundle.putString(
                "splashscreen_before",
                beforeScreenshot.writeToDevice("before.png").absolutePath
            )
            bundle.putString(
                "splashscreen_after",
                afterScreenshot.writeToDevice("after.png").absolutePath
            )
            val path = diff?.parentFile?.path
            InstrumentationRegistry.getInstrumentation().sendStatus(2, bundle)
            fail(
                "SplashScreenView and SplashScreen don't match\n${matcher.comparisonStatistics}" +
                    "\nResult saved at $path"
            )
        }
    }

    private fun Bitmap.writeToDevice(name: String): File {
        return writeToDevice(
            {
                compress(Bitmap.CompressFormat.PNG, 0 /*ignored for png*/, it)
            },
            name
        )
    }

    private fun writeToDevice(
        writeAction: (FileOutputStream) -> Unit,
        name: String
    ): File {
        val deviceOutputDirectory = File(
            InstrumentationRegistry.getInstrumentation().context.externalCacheDir,
            "splashscreen_test"
        )
        if (!deviceOutputDirectory.exists() && !deviceOutputDirectory.mkdir()) {
            throw IOException("Could not create folder.")
        }

        val file = File(deviceOutputDirectory, name)
        try {
            FileOutputStream(file).use {
                writeAction(it)
            }
        } catch (e: Exception) {
            throw IOException(
                "Could not write file to storage (path: ${file.absolutePath}). " +
                    " Stacktrace: " + e.stackTrace
            )
        }
        return file
    }

    private fun startActivityWithSplashScreen(
        intentModifier: ((Intent) -> Unit)? = null
    ): SplashScreenTestController {
        return startActivityWithSplashScreen(
            activityClass, device, intentModifier
        )
    }
}