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
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.test.core.app.takeScreenshot
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.test.uiautomator.UiDevice
import androidx.testutils.PollingCheck
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
public class SplashscreenParametrizedTest(
    public val name: String,
    public val activityClass: KClass<out SplashScreenTestControllerHolder>,
) {

    private lateinit var device: UiDevice

    public companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        public fun data(): Iterable<Array<Any>> {
            return listOf(
                arrayOf("Platform", SplashScreenWithIconBgTestActivity::class),
                arrayOf("AppCompat", SplashScreenAppCompatTestActivity::class),
            )
        }

        const val TAG = "SplashscreenParameterizedTest"
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
            activity.waitedLatch.await(2, TimeUnit.SECONDS),
        )
        assertFalse("Activity should not have been drawn", activity.hasDrawn)
        activity.waitBarrier.set(false)
        assertTrue("Activity was never drawn", activity.drawnLatch.await(2, TimeUnit.SECONDS))
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
        assertTrue("Activity was never drawn", activity.drawnLatch.await(2, TimeUnit.SECONDS))
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
            activity.splashScreenView!!.parent,
        )
    }

    // The vector drawable of the starting window isn't scaled
    // correctly pre 23
    @Test
    public fun splashscreenViewScreenshotComparison() {
        val controller = startActivityWithSplashScreen {
            // Clear out any previous instances
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            it.putExtra(EXTRA_SPLASHSCREEN_WAIT, true)
            it.putExtra(EXTRA_ANIMATION_LISTENER, true)
            it.putExtra(EXTRA_SPLASHSCREEN_SCREENSHOT, true)
        }

        var splashScreenViewScreenShot: Bitmap? = null

        controller.doOnExitAnimation {
            // b/355716686
            // During the transition from the splash screen of system starting window to the
            // activity, there may be a moment that `PhoneWindowManager`'s
            // `mTopFullscreenOpaqueWindowState` would be `null`, which might lead to the flicker of
            // status bar (b/64291272,
            // https://android.googlesource.com/platform/frameworks/base/+/c0c9324fcb03c85ef7bed2d997c441119823d31c%5E%21/)
            val topFullscreenWinState = "mTopFullscreenOpaqueWindowState"

            // We should take the screenshot when `mTopFullscreenOpaqueWindowState` is window of the
            // activity
            val topFullscreenWinStateBelongsToActivity =
                Regex(
                    topFullscreenWinState +
                        "=Window\\{.*" +
                        controller.activity.componentName.className +
                        "\\}"
                )

            val isTopFullscreenWinStateReady: () -> Boolean = {
                val dumpedWindowPolicy =
                    InstrumentationRegistry.getInstrumentation()
                        .uiAutomation
                        .executeShellCommand("dumpsys window p")
                        .use { FileInputStream(it.fileDescriptor).reader().readText() }

                !dumpedWindowPolicy.contains(topFullscreenWinState) ||
                    dumpedWindowPolicy.contains(topFullscreenWinStateBelongsToActivity)
            }

            PollingCheck.waitFor(2000, isTopFullscreenWinStateReady)
            if (!isTopFullscreenWinStateReady())
                fail("$topFullscreenWinState is not ready, cannot take screenshot")

            splashScreenViewScreenShot =
                InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            it.remove()
            controller.exitAnimationListenerLatch.countDown()
            true
        }

        assertTrue(controller.waitedLatch.await(2, TimeUnit.SECONDS))
        controller.waitBarrier.set(false)
        controller.exitAnimationListenerLatch.await(2, TimeUnit.SECONDS)

        compareBitmaps(controller.splashScreenScreenshot!!, splashScreenViewScreenShot!!)
    }

    /**
     * Checks that activity and especially the system bars, have the same appearance whether we set
     * an OnExitAnimationListener or not. This allows us to check that the system ui is stable
     * before and after the removal of the SplashScreenView.
     */
    @Test
    @Ignore // b/213634077
    fun endStateStableWithAndWithoutListener() {
        // Take a screenshot of the activity when no OnExitAnimationListener is set.
        // This is our reference.
        var controller =
            startActivityWithSplashScreen(SplashScreenStability1::class, device) {
                // Clear out any previous instances
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                it.putExtra(EXTRA_ANIMATION_LISTENER, false)
            }
        assertTrue(controller.drawnLatch.await(2, TimeUnit.SECONDS))
        Thread.sleep(500)
        val withoutListener = takeScreenshot()

        // Take a screenshot of the container view while the splash screen view is invisible but
        // not removed
        controller =
            startActivityWithSplashScreen(SplashScreenStability1::class, device) {
                // Clear out any previous instances
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                it.putExtra(EXTRA_ANIMATION_LISTENER, true)
                it.putExtra(EXTRA_SPLASHSCREEN_WAIT, true)
            }
        val withListener = screenshotContainerInExitListener(controller)

        compareBitmaps(withoutListener, withListener, 0.999)

        // Execute the same steps as above but with another set of theme attributes to check.
        controller =
            startActivityWithSplashScreen(SplashScreenStability2::class, device) {
                // Clear out any previous instances
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                it.putExtra(EXTRA_ANIMATION_LISTENER, false)
            }
        controller.waitForActivityDrawn()
        Thread.sleep(500)
        val withoutListener2 = takeScreenshot()

        controller =
            startActivityWithSplashScreen(SplashScreenStability2::class, device) {
                // Clear out any previous instances
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                it.putExtra(EXTRA_ANIMATION_LISTENER, true)
                it.putExtra(EXTRA_SPLASHSCREEN_WAIT, true)
            }
        val withListener2 = screenshotContainerInExitListener(controller)
        compareBitmaps(withListener2, withoutListener2)
    }

    private fun screenshotContainerInExitListener(controller: SplashScreenTestController): Bitmap {
        lateinit var contentViewInListener: Bitmap
        controller.doOnExitAnimation {
            it.view.visibility = View.INVISIBLE
            it.view.postDelayed(
                {
                    contentViewInListener = takeScreenshot()
                    it.remove()
                    controller.exitAnimationListenerLatch.countDown()
                },
                100,
            )
            true
        }
        controller.waitBarrier.set(false)
        controller.waitSplashScreenViewRemoved()
        controller.activity.finishAndRemoveTask()
        return contentViewInListener
    }

    /**
     * The splash screen is drawn full screen. On Android 12, this is achieved using
     * [Window.setDecorFitsSystemWindow(false)].
     */
    @Test
    public fun decorFitSystemStableContentView() {
        val activityController = startActivityWithSplashScreen {
            // Clear out any previous instances
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            it.putExtra(EXTRA_ANIMATION_LISTENER, true)
            it.putExtra(EXTRA_SPLASHSCREEN_WAIT, true)
        }

        // We wait for 2 draw passes and check that out content view's height is stable
        val drawLatch = CountDownLatch(2)
        val container = activityController.activity.findViewById<View>(R.id.container)
        val contentViewHeights = mutableListOf<Int>()
        var splashScreenViewProvider: SplashScreenViewProvider? = null

        val onDrawListener =
            ViewTreeObserver.OnDrawListener {
                contentViewHeights.add(container.height)
                drawLatch.countDown()
                if (drawLatch.count == 1L) {
                    splashScreenViewProvider!!.remove()
                }
            }

        activityController.doOnExitAnimation {
            splashScreenViewProvider = it
            container.viewTreeObserver.addOnDrawListener(onDrawListener)
            activityController.splashScreenView!!.alpha = 0f
            true
        }

        activityController.waitBarrier.set(false)
        assertTrue("Missing ${drawLatch.count} draw passes.", drawLatch.await(2, TimeUnit.SECONDS))
        assertTrue(
            "Content view height must be stable but was ${
                contentViewHeights.joinToString(",")
            }",
            contentViewHeights.all { it == contentViewHeights.first() },
        )
    }

    private fun compareBitmaps(
        beforeScreenshot: Bitmap,
        afterScreenshot: Bitmap,
        threshold: Double = 0.99,
    ) {
        val beforeBuffer = IntArray(beforeScreenshot.width * beforeScreenshot.height)
        beforeScreenshot.getPixels(
            beforeBuffer,
            0,
            beforeScreenshot.width,
            0,
            0,
            beforeScreenshot.width,
            beforeScreenshot.height,
        )

        val afterBuffer = IntArray(afterScreenshot.width * afterScreenshot.height)
        afterScreenshot.getPixels(
            afterBuffer,
            0,
            afterScreenshot.width,
            0,
            0,
            afterScreenshot.width,
            afterScreenshot.height,
        )

        val matcher =
            MSSIMMatcher(threshold)
                .compareBitmaps(
                    beforeBuffer,
                    afterBuffer,
                    afterScreenshot.width,
                    afterScreenshot.height,
                )

        if (!matcher.matches) {
            // Serialize the screenshots and output them through Logcat so as to gather more details
            // for debugging.
            logLongMessage(Log::e, TAG, "before", beforeScreenshot.toBase64String())
            logLongMessage(Log::e, TAG, "after", afterScreenshot.toBase64String())
            matcher.diff?.let { logLongMessage(Log::e, TAG, "diff", it.toBase64String()) }

            val bundle = Bundle()
            val diff = matcher.diff?.writeToDevice("diff.png")
            bundle.putString("splashscreen_diff", diff?.absolutePath)
            bundle.putString(
                "splashscreen_before",
                beforeScreenshot.writeToDevice("before.png").absolutePath,
            )
            bundle.putString(
                "splashscreen_after",
                afterScreenshot.writeToDevice("after.png").absolutePath,
            )
            val path = diff?.parentFile?.path
            InstrumentationRegistry.getInstrumentation().sendStatus(2, bundle)
            fail(
                "SplashScreenView and SplashScreen don't match\n${matcher.comparisonStatistics}" +
                    "\nResult saved at $path"
            )
        }
    }

    /**
     * A log message has a maximum of 4096 bytes, where date / time, tag, process, etc. included.
     *
     * Therefore, we should chunk a large message into some smaller ones.
     */
    private fun logLongMessage(
        logger: (tag: String, msg: String) -> Int,
        tag: String,
        title: String,
        msg: String,
    ) {
        val chunks = msg.chunked(4000)
        logger(tag, "$title ${chunks.size}")

        for ((i, chunk) in chunks.withIndex()) {
            logger(tag, title + " $i/${chunks.size} " + chunk)
        }
    }

    /**
     * Serialize a bitmap into a string in Base64 encoding so that we could output it through logs
     * when comparisons fail.
     */
    private fun Bitmap.toBase64String(): String {
        val scaledBitmap =
            Bitmap.createScaledBitmap(
                this,
                // Reduce the size of the bitmap
                width * 3 shr 2,
                height * 3 shr 2,
                false,
            )
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)

        val bytes = outputStream.toByteArray()
        val str =
            Base64.encodeToString(
                bytes,
                Base64.NO_WRAP, // Not to wrap here as we are going to wrap on our own later
            )
        return str
    }

    private fun Bitmap.writeToDevice(name: String): File {
        return writeToDevice(
            { compress(Bitmap.CompressFormat.PNG, 0 /*ignored for png*/, it) },
            name,
        )
    }

    private fun writeToDevice(writeAction: (FileOutputStream) -> Unit, name: String): File {
        val deviceOutputDirectory =
            File(
                InstrumentationRegistry.getInstrumentation().context.externalCacheDir,
                "splashscreen_test",
            )
        if (!deviceOutputDirectory.exists() && !deviceOutputDirectory.mkdir()) {
            throw IOException("Could not create folder.")
        }

        val file = File(deviceOutputDirectory, name)
        try {
            FileOutputStream(file).use { writeAction(it) }
        } catch (e: Exception) {
            throw IOException(
                "Could not write file to storage (path: ${file.absolutePath}). " +
                    " Stacktrace: " +
                    e.stackTrace
            )
        }
        return file
    }

    private fun startActivityWithSplashScreen(
        intentModifier: ((Intent) -> Unit)? = null
    ): SplashScreenTestController {
        return startActivityWithSplashScreen(activityClass, device, intentModifier)
    }

    private fun SplashScreenTestController.waitForActivityDrawn() {
        assertTrue("Activity was never drawn", drawnLatch.await(2, TimeUnit.SECONDS))
    }

    private fun SplashScreenTestController.waitSplashScreenViewRemoved() {
        assertTrue(
            "Exit animation listener was not called",
            exitAnimationListenerLatch.await(2, TimeUnit.SECONDS),
        )
    }
}
