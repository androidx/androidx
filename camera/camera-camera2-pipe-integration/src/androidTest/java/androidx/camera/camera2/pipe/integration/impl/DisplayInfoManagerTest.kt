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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@Suppress("DEPRECATION") // getRealSize
@RunWith(AndroidJUnit4::class)
class DisplayInfoManagerTest {
    private val displayInfoManager =
        DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext())

    @After
    fun tearDown() {
        DisplayInfoManager.releaseInstance()
    }

    @Test
    fun defaultDisplayIsDeviceDisplay_whenOneDisplay() {
        // Arrange
        val displayManager =
            (ApplicationProvider.getApplicationContext() as Context).getSystemService(
                Context.DISPLAY_SERVICE
            ) as DisplayManager

        Assume.assumeTrue(displayManager.displays.size == 1)

        val currentDisplaySize = Point()
        displayManager.displays[0].getRealSize(currentDisplaySize)

        // Act
        val size = Point()
        displayInfoManager.getMaxSizeDisplay().getRealSize(size)

        // Assert
        assertEquals(currentDisplaySize, size)
    }

    @Test
    fun previewSizeAreaIsWithinMaxPreviewArea() {
        // Act & Assert
        val previewSize = displayInfoManager.getPreviewSize()
        assertTrue(
            "$previewSize has larger area than 1920 * 1080",
            previewSize.width * previewSize.height <= 1920 * 1080,
        )
    }
}
