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
import android.view.Window
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.ScreenFlashUiCompleter
import androidx.camera.core.ImageCapture.ScreenFlashUiControl
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowWindow

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ScreenFlashViewTest {
    private val noOpUiCompleter = ScreenFlashUiCompleter {}

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

    private fun getScreenFlashUiControlAfterSettingWindow(
        assumeNoFailure: Boolean
    ): ScreenFlashUiControl? {
        screenFlashView.setScreenFlashWindow(window)
        val uiControl = screenFlashView.screenFlashUiControl
        if (assumeNoFailure) {
            Assume.assumeTrue("Failed to create ScreenFlashUiControl", uiControl != null)
        }
        return uiControl
    }

    @Test
    fun isTransparentByDefault() {
        assertThat(screenFlashView.alpha).isEqualTo(0f)
    }

    @Test
    fun canProvideValidScreenFlashUiControl() {
        val uiControl = getScreenFlashUiControlAfterSettingWindow(false)
        assertThat(uiControl).isNotNull()
    }

    @Test
    fun providesSameScreenFlashUiControlIfSameWindowSetAgain() {
        val prevUiControl = getScreenFlashUiControlAfterSettingWindow(false)
        val newUiControl = getScreenFlashUiControlAfterSettingWindow(false)
        assertThat(newUiControl).isEqualTo(prevUiControl)
    }

    @Test
    fun providesNewScreenFlashUiControlIfNewWindowSet() {
        val prevUiControl = getScreenFlashUiControlAfterSettingWindow(false)
        createWindow()
        val newUiControl = getScreenFlashUiControlAfterSettingWindow(false)
        assertThat(newUiControl).isNotEqualTo(prevUiControl)
    }

    @Test
    fun isFullyVisible_whenApplyScreenFlashUiInvoked() {
        val uiControl = getScreenFlashUiControlAfterSettingWindow(true)
        uiControl!!.applyScreenFlashUi(noOpUiCompleter)
        assertThat(screenFlashView.alpha).isEqualTo(1f)
    }

    @Test
    fun windowBrightnessMaximized_whenApplyScreenFlashUiInvoked() {
        val uiControl = getScreenFlashUiControlAfterSettingWindow(true)
        uiControl!!.applyScreenFlashUi(noOpUiCompleter)
        assertThat(window.attributes.screenBrightness).isEqualTo(1f)
    }

    @Test
    fun isTransparent_whenScreenFlashUiClearedAfterApply() {
        val uiControl = getScreenFlashUiControlAfterSettingWindow(true)
        uiControl!!.applyScreenFlashUi(noOpUiCompleter)
        uiControl.clearScreenFlashUi()
        assertThat(screenFlashView.alpha).isEqualTo(0f)
    }

    @Test
    fun windowBrightnessRestored_whenScreenFlashUiClearedAfterApply() {
        val initialBrightness = 0.5f
        val layoutParam = window.attributes
        layoutParam.screenBrightness = initialBrightness
        window.setAttributes(layoutParam)
        val uiControl = getScreenFlashUiControlAfterSettingWindow(true)
        uiControl!!.applyScreenFlashUi(noOpUiCompleter)
        uiControl.clearScreenFlashUi()
        assertThat(window.attributes.screenBrightness).isEqualTo(initialBrightness)
    }

    @Test
    fun validScreenFlashUiControlSetToCameraController_whenWindowSetAndThenControllerSet() {
        val cameraController = LifecycleCameraController(appContext)

        screenFlashView.setScreenFlashWindow(window)
        screenFlashView.setController(cameraController)

        assertThat(cameraController.screenFlashUiInfoByPriority?.screenFlashUiControl).isNotNull()
    }

    @Test
    fun validScreenFlashUiControlSetToCameraController_whenControllerSetAndThenWindowSet() {
        val cameraController = LifecycleCameraController(appContext)

        screenFlashView.setController(cameraController)
        screenFlashView.setScreenFlashWindow(window)

        assertThat(cameraController.screenFlashUiInfoByPriority?.screenFlashUiControl).isNotNull()
    }

    @Test
    fun nullScreenFlashUiControlSetToCameraController_whenControllerSetButNoWindowSet() {
        val cameraController = LifecycleCameraController(appContext)

        screenFlashView.setController(cameraController)

        assertThat(cameraController.screenFlashUiInfoByPriority?.screenFlashUiControl).isNull()
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
