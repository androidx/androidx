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

package androidx.camera.camera2.internal

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Size
import android.view.Display
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDisplayManager
import org.robolectric.shadows.ShadowDisplay

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@Suppress("DEPRECATION") // getRealSize
class DisplayInfoManagerTest {

    private fun addDisplay(width: Int, height: Int, state: Int = Display.STATE_ON) {
        val displayStr = String.format("w%ddp-h%ddp", width, height)
        val displayId = ShadowDisplayManager.addDisplay(displayStr)
        if (state != Display.STATE_ON) {
            val displayManager = (ApplicationProvider.getApplicationContext() as Context)
                .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            (Shadow.extract(displayManager.getDisplay(displayId)) as ShadowDisplay).setState(state)
        }
    }

    @After
    fun tearDown() {
        DisplayInfoManager.releaseInstance()
    }

    @Test
    fun canReturnMaxSizeDisplay_oneDisplay() {
        // Arrange
        val displayManager = (ApplicationProvider.getApplicationContext() as Context)
            .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val currentDisplaySize = Point()
        displayManager.displays.get(0).getRealSize(currentDisplaySize)

        // Act
        val displayInfoManager = DisplayInfoManager
            .getInstance(ApplicationProvider.getApplicationContext())
        val size = Point()
        displayInfoManager.maxSizeDisplay.getRealSize(size)

        // Assert
        assertThat(size).isEqualTo(currentDisplaySize)
    }

    @Test
    fun canReturnMaxSizeDisplay_multipleDisplay() {
        // Arrange
        addDisplay(2000, 3000)
        addDisplay(480, 640)

        // Act
        val displayInfoManager = DisplayInfoManager
            .getInstance(ApplicationProvider.getApplicationContext())
        val size = Point()
        displayInfoManager.maxSizeDisplay.getRealSize(size)

        // Assert
        assertThat(size).isEqualTo(Point(2000, 3000))
    }

    @Test
    fun canReturnMaxSizeDisplay_offState() {
        // Arrange
        addDisplay(2000, 3000, Display.STATE_OFF)
        addDisplay(480, 640)
        addDisplay(200, 300, Display.STATE_OFF)

        // Act
        val displayInfoManager = DisplayInfoManager
            .getInstance(ApplicationProvider.getApplicationContext())
        val size = Point()
        displayInfoManager.maxSizeDisplay.getRealSize(size)

        // Assert
        assertThat(size).isEqualTo(Point(480, 640))
    }

    @Test
    fun canReturnPreviewSize_displaySmallerThan1080P() {
        // Arrange
        addDisplay(480, 640)

        // Act & Assert
        val displayInfoManager = DisplayInfoManager
            .getInstance(ApplicationProvider.getApplicationContext())
        assertThat(displayInfoManager.previewSize).isEqualTo(Size(640, 480))
    }

    @Test
    fun canReturnPreviewSize_displayLargerThan1080P() {
        // Arrange
        addDisplay(2000, 3000)

        // Act & Assert
        val displayInfoManager = DisplayInfoManager
            .getInstance(ApplicationProvider.getApplicationContext())
        assertThat(displayInfoManager.previewSize).isEqualTo(Size(1920, 1080))
    }

    @Test
    fun canReturnDifferentPreviewSize_refreshIsCalled() {
        // Arrange
        val displayInfoManager = spy(
            DisplayInfoManager
                .getInstance(ApplicationProvider.getApplicationContext())
        )

        // Act
        displayInfoManager.previewSize
        displayInfoManager.refresh()
        displayInfoManager.previewSize

        // Assert
        verify(displayInfoManager, times(2)).maxSizeDisplay
    }
}