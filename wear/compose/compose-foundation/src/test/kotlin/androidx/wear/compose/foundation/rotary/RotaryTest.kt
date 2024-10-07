/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.rotary

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ThresholdHandlerTest {

    @Test
    fun testMinVelocityThreshold() {
        val itemHeight = 100f
        val minThresholdDivider = 1f
        val maxThresholdDivider = 2f
        val thresholdHandler =
            ThresholdHandler(
                minThresholdDivider = minThresholdDivider,
                maxThresholdDivider = maxThresholdDivider,
                averageItemSize = { itemHeight },
            )

        thresholdHandler.startThresholdTracking(0L)
        // Simulate very slow scroll
        thresholdHandler.updateTracking(100L, 1f)
        val result = thresholdHandler.calculateSnapThreshold()

        // Threshold should be equal to the height of an item divided by minThresholdDivider
        assertEquals(itemHeight / minThresholdDivider, result, 0.01f)
    }

    @Test
    fun testMaxVelocityThreshold() {
        val itemHeight = 100f
        val minThresholdDivider = 1f
        val maxThresholdDivider = 2f
        val thresholdHandler =
            ThresholdHandler(
                minThresholdDivider = minThresholdDivider,
                maxThresholdDivider = maxThresholdDivider,
                averageItemSize = { itemHeight },
            )

        thresholdHandler.startThresholdTracking(0L)
        // Simulate very fast scroll
        thresholdHandler.updateTracking(1L, 100f)
        val result = thresholdHandler.calculateSnapThreshold()

        // Threshold should be equal to the height of an item divided by maxThresholdDivider
        assertEquals(itemHeight / maxThresholdDivider, result, 0.01f)
    }
}
