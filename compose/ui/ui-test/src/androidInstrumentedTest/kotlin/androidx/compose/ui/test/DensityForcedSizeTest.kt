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

package androidx.compose.ui.test

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class DensityForcedSizeTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun wrapsRequestedSize_smallPortraitAspectRatio() {
        lateinit var density: Density
        var layoutCoordinates: LayoutCoordinates? = null

        rule.setContent {
            density = LocalDensity.current
            DensityForcedSize(
                size = DpSize(30.dp, 40.dp),
                modifier = Modifier.onPlaced { layoutCoordinates = it }
            ) {
                Spacer(modifier = Modifier.fillMaxSize())
            }
        }

        // The size should be within 0.5 pixels of the specified size
        // Due to rounding, we can't expect to have the DensityForcedSize take exactly the requested
        // size which is true in normal Compose code as well
        assertEquals(with(density) { 30.dp.toPx() }, layoutCoordinates!!.size.width.toFloat(), 0.5f)
        assertEquals(
            with(density) { 40.dp.toPx() },
            layoutCoordinates!!.size.height.toFloat(),
            0.5f
        )
    }

    @Test
    fun wrapsRequestedSize_smallLandscapeAspectRatio() {
        lateinit var density: Density
        var layoutCoordinates: LayoutCoordinates? = null

        rule.setContent {
            density = LocalDensity.current
            DensityForcedSize(
                size = DpSize(40.dp, 30.dp),
                modifier = Modifier.onPlaced { layoutCoordinates = it }
            ) {
                Spacer(modifier = Modifier.fillMaxSize())
            }
        }

        // The size should be within 0.5 pixels of the specified size
        // Due to rounding, we can't expect to have the DensityForcedSize take exactly the requested
        // size which is true in normal Compose code as well
        assertEquals(with(density) { 40.dp.toPx() }, layoutCoordinates!!.size.width.toFloat(), 0.5f)
        assertEquals(
            with(density) { 30.dp.toPx() },
            layoutCoordinates!!.size.height.toFloat(),
            0.5f
        )
    }
}
