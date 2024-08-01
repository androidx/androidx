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

package androidx.camera.view

import android.content.Context
import android.os.Build
import android.os.Looper.getMainLooper
import android.view.Window
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.ScreenFlash
import androidx.camera.core.ImageCapture.ScreenFlashListener
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowWindow

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ScreenFlashViewTest {
    private val noOpListener = ScreenFlashListener {
        // no-op
    }

    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var screenFlashView: ScreenFlashView
    private lateinit var window: Window

    @Before
    fun setUp() {
        screenFlashView = ScreenFlashView(appContext)
        createWindow()
    }

    private fun createWindow() {
        try {
            window = ShadowWindow.create(appContext)
        } catch (e: ClassNotFoundException) {
            Assume.assumeTrue("Failed to create shadow window", false)
        }
    }

    private fun getScreenFlashAfterSettingWindow(assumeNoFailure: Boolean): ScreenFlash? {
        screenFlashView.setScreenFlashWindow(window)
        val screenFlash = screenFlashView.screenFlash
        if (assumeNoFailure) {
            Assume.assumeTrue("Failed to create ScreenFlash", screenFlash != null)
        }
        return screenFlash
    }

    @Test
    fun isTransparentByDefault() {
        assertThat(screenFlashView.alpha).isEqualTo(0f)
    }

    @Test
    fun canProvideValidScreenFlash() {
        val screenFlash = getScreenFlashAfterSettingWindow(false)
        assertThat(screenFlash).isNotNull()
    }

    @Test
    fun providesSameScreenFlashInstanceIfSameWindowSetAgain() {
        val prevScreenFlash = getScreenFlashAfterSettingWindow(false)
        val newScreenFlash = getScreenFlashAfterSettingWindow(false)
        assertThat(newScreenFlash).isEqualTo(prevScreenFlash)
    }

    @Test
    fun providesNewScreenFlashIfNewWindowSet() {
        val prevScreenFlash = getScreenFlashAfterSettingWindow(false)
        createWindow()
        val newScreenFlash = getScreenFlashAfterSettingWindow(false)
        assertThat(newScreenFlash).isNotEqualTo(prevScreenFlash)
    }

    @Test
    fun isNotVisibleImmediately_whenScreenFlashApplyInvoked() {
        val screenFlash = getScreenFlashAfterSettingWindow(true)
        screenFlash!!.apply(
            System.currentTimeMillis() +
                TimeUnit.SECONDS.toMillis(ImageCapture.SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS),
            noOpListener,
        )
        assertThat(screenFlashView.alpha).isEqualTo(0f)
    }

    @Test
    fun isFullyVisibleAfterAnimationDuration_whenScreenFlashApplyInvoked() = runBlocking {
        val screenFlash = getScreenFlashAfterSettingWindow(true)
        screenFlash!!.apply(
            System.currentTimeMillis() +
                TimeUnit.SECONDS.toMillis(ImageCapture.SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS),
            noOpListener,
        )
        shadowOf(getMainLooper())
            .idleFor(
                screenFlashView.visibilityRampUpAnimationDurationMillis + 1,
                TimeUnit.MILLISECONDS
            )
        assertThat(screenFlashView.alpha).isEqualTo(1f)
    }

    @Test
    fun windowBrightnessMaximized_whenScreenFlashApplyInvoked() {
        val screenFlash = getScreenFlashAfterSettingWindow(true)
        screenFlash!!.apply(
            System.currentTimeMillis() +
                TimeUnit.SECONDS.toMillis(ImageCapture.SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS),
            noOpListener,
        )
        assertThat(window.attributes.screenBrightness).isEqualTo(1f)
    }

    @Test
    fun isTransparent_whenScreenFlashUiClearedAfterApply() {
        val screenFlash = getScreenFlashAfterSettingWindow(true)
        screenFlash!!.apply(
            System.currentTimeMillis() +
                TimeUnit.SECONDS.toMillis(ImageCapture.SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS),
            noOpListener,
        )
        screenFlash.clear()
        assertThat(screenFlashView.alpha).isEqualTo(0f)
    }

    @Test
    fun windowBrightnessRestored_whenScreenFlashUiClearedAfterApply() {
        val initialBrightness = 0.5f
        val layoutParam = window.attributes
        layoutParam.screenBrightness = initialBrightness
        window.setAttributes(layoutParam)
        val screenFlash = getScreenFlashAfterSettingWindow(true)
        screenFlash!!.apply(
            System.currentTimeMillis() +
                TimeUnit.SECONDS.toMillis(ImageCapture.SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS),
            noOpListener,
        )
        screenFlash.clear()
        assertThat(window.attributes.screenBrightness).isEqualTo(initialBrightness)
    }

    @Test
    fun validScreenFlashSetToCameraController_whenWindowSetAndThenControllerSet() {
        val cameraController = LifecycleCameraController(appContext)

        screenFlashView.setScreenFlashWindow(window)
        screenFlashView.setController(cameraController)

        assertThat(cameraController.screenFlashUiInfoByPriority?.screenFlash).isNotNull()
    }

    @Test
    fun validScreenFlashSetToCameraController_whenControllerSetAndThenWindowSet() {
        val cameraController = LifecycleCameraController(appContext)

        screenFlashView.setController(cameraController)
        screenFlashView.setScreenFlashWindow(window)

        assertThat(cameraController.screenFlashUiInfoByPriority?.screenFlash).isNotNull()
    }

    @Test
    fun nullScreenFlashInstanceSetToCameraController_whenControllerSetButNoWindowSet() {
        val cameraController = LifecycleCameraController(appContext)

        screenFlashView.setController(cameraController)

        assertThat(cameraController.screenFlashUiInfoByPriority?.screenFlash).isNull()
    }

    @Test
    fun throwException_whenControllerSetWithScreenFlashModeButNoWindowSet() {
        val cameraController = LifecycleCameraController(appContext)
        cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        cameraController.imageCaptureFlashMode = ImageCapture.FLASH_MODE_SCREEN

        Assert.assertThrows(IllegalStateException::class.java) {
            screenFlashView.setController(cameraController)
        }
    }
}
