/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Size
import android.view.Display
import android.view.WindowManager
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDisplay
import org.robolectric.shadows.ShadowDisplayManager
import org.robolectric.shadows.ShadowDisplayManager.removeDisplay
import org.robolectric.util.ReflectionHelpers

@Suppress("DEPRECATION") // getRealSize
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class DisplayInfoManagerTest {
    private val displayInfoManager = DisplayInfoManager(ApplicationProvider.getApplicationContext())

    private fun addDisplay(width: Int, height: Int, state: Int = Display.STATE_ON): Int {
        val displayStr = String.format("w%ddp-h%ddp", width, height)
        val displayId = ShadowDisplayManager.addDisplay(displayStr)

        if (state != Display.STATE_ON) {
            val displayManager =
                (ApplicationProvider.getApplicationContext() as Context).getSystemService(
                    Context.DISPLAY_SERVICE
                ) as DisplayManager
            (Shadow.extract(displayManager.getDisplay(displayId)) as ShadowDisplay).setState(state)
        }

        return displayId
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun classSetUp() {
            DisplayInfoManager.invalidateLazyFields()
        }
    }

    @After
    fun tearDown() {
        val displayManager =
            (ApplicationProvider.getApplicationContext() as Context).getSystemService(
                Context.DISPLAY_SERVICE
            ) as DisplayManager?

        displayManager?.let {
            for (display in it.displays) {
                removeDisplay(display.displayId)
            }
        }
    }

    @Test
    fun defaultDisplayIsDeviceDisplay_whenOneDisplay() {
        // Arrange
        val displayManager =
            (ApplicationProvider.getApplicationContext() as Context).getSystemService(
                Context.DISPLAY_SERVICE
            ) as DisplayManager
        val currentDisplaySize = Point()
        displayManager.displays[0].getRealSize(currentDisplaySize)

        // Act
        val size = Point()
        displayInfoManager.defaultDisplay.getRealSize(size)

        // Assert
        assertEquals(currentDisplaySize, size)
    }

    @Test
    fun defaultDisplayIsMaxSizeDisplay_whenMultipleDisplay() {
        // Arrange
        addDisplay(2000, 3000)
        addDisplay(480, 640)

        // Act
        val size = Point()
        displayInfoManager.defaultDisplay.getRealSize(size)

        // Assert
        assertEquals(Point(2000, 3000), size)
    }

    @Test
    fun defaultDisplayIsMaxSizeDisplay_whenPreviousMaxDisplayRemoved() {
        // Arrange
        val id = addDisplay(2000, 3000)
        addDisplay(480, 640)
        removeDisplay(id)

        // Act
        val size = Point()
        displayInfoManager.defaultDisplay.getRealSize(size)

        // Assert
        assertEquals(Point(480, 640), size)
    }

    @Test
    fun defaultDisplayIsMaxSizeDisplay_whenNewMaxDisplayAddedAfterGettingPrevious() {
        // Arrange
        addDisplay(480, 640)

        // Act
        displayInfoManager.defaultDisplay
        addDisplay(2000, 3000)

        val size = Point()
        displayInfoManager.defaultDisplay.getRealSize(size)

        // Assert
        assertEquals(Point(2000, 3000), size)
    }

    @Test
    fun defaultDisplayIsMaxSizeInNotOffState_whenMultipleDisplayWithSomeOffState() {
        // Arrange
        addDisplay(2000, 3000, Display.STATE_OFF)
        addDisplay(480, 640)
        addDisplay(240, 320)
        addDisplay(200, 300, Display.STATE_OFF)

        // Act
        val size = Point()
        displayInfoManager.defaultDisplay.getRealSize(size)

        // Assert
        assertEquals(Point(480, 640), size)
    }

    @Test
    fun defaultDisplayIsMaxSizeInNotOffState_whenMultipleDisplayWithNoOnState() {
        // Arrange
        addDisplay(2000, 3000, Display.STATE_OFF)
        addDisplay(480, 640, Display.STATE_UNKNOWN)
        addDisplay(240, 320, Display.STATE_UNKNOWN)
        addDisplay(200, 300, Display.STATE_OFF)

        // Act
        val size = Point()
        displayInfoManager.defaultDisplay.getRealSize(size)

        // Assert
        assertEquals(Point(480, 640), size)
    }

    @Test
    fun defaultDisplayIsMaxSizeInOffState_whenMultipleDisplayWithAllOffState() {
        // Arrange
        addDisplay(2000, 3000, Display.STATE_OFF)
        addDisplay(480, 640, Display.STATE_OFF)
        addDisplay(200, 300, Display.STATE_OFF)
        removeDisplay(0)

        // Act
        val size = Point()
        displayInfoManager.defaultDisplay.getRealSize(size)

        // Assert
        assertEquals(Point(2000, 3000), size)
    }

    @Test(expected = IllegalStateException::class)
    fun throwsCorrectExceptionForDefaultDisplay_whenNoDisplay() {
        // Arrange
        removeDisplay(0)

        // Act
        val size = Point()
        displayInfoManager.defaultDisplay.getRealSize(size)
    }

    @Test
    fun previewSizeIsProperSize_whenDisplaySmallerThan1080P() {
        // Arrange
        addDisplay(480, 640)

        // Act & Assert
        assertEquals(Size(640, 480), displayInfoManager.getPreviewSize())
    }

    @Test
    fun previewSizeIsMaxPreviewSize_whenDisplayLargerThan1080P() {
        // Arrange
        addDisplay(2000, 3000)

        // Act & Assert
        assertEquals(Size(1920, 1080), displayInfoManager.getPreviewSize())
    }

    @Test
    fun previewSizeIsUpdated_whenNewDisplayAddedAfterPreviousUse() {
        // Arrange
        addDisplay(480, 640)

        // Act
        displayInfoManager.getPreviewSize()
        addDisplay(2000, 3000)
        displayInfoManager.refresh()

        // Assert
        assertEquals(Size(1920, 1080), displayInfoManager.getPreviewSize())
    }

    @Test
    fun canReturnFallbackPreviewSize640x480_displaySmallerThan320x240() {
        // Arrange
        val windowManager =
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Shadows.shadowOf(windowManager.defaultDisplay).setRealWidth(16)
        Shadows.shadowOf(windowManager.defaultDisplay).setRealHeight(16)

        // Act & Assert
        val displayInfoManager = DisplayInfoManager(ApplicationProvider.getApplicationContext())
        assertThat(displayInfoManager.getPreviewSize()).isEqualTo(Size(640, 480))
    }

    @Test
    fun canReturnCorrectPreviewSize_fromDisplaySizeCorrector() {
        // Arrange
        val windowManager =
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Shadows.shadowOf(windowManager.defaultDisplay).setRealWidth(16)
        Shadows.shadowOf(windowManager.defaultDisplay).setRealHeight(16)

        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-A127F")

        // Act & Assert
        val displayInfoManager = DisplayInfoManager(ApplicationProvider.getApplicationContext())
        assertThat(displayInfoManager.getPreviewSize()).isEqualTo(Size(1600, 720))
    }
}
