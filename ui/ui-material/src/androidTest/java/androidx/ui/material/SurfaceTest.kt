/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.material

import android.os.Build
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.Modifier
import androidx.ui.core.semantics.semantics
import androidx.ui.core.testTag
import androidx.ui.core.zIndex
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredSize
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SurfaceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun zOrderingBasedOnElevationIsApplied() {
        composeTestRule.setMaterialContent {
            Stack(Modifier
                .preferredSize(10.dp, 10.dp)
                .semantics(mergeAllDescendants = true)
                .testTag("stack")) {
                Surface(color = Color.Yellow, elevation = 2.dp) {
                    Box(Modifier.fillMaxSize())
                }
                Surface(color = Color.Green) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        findByTag("stack")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = RectangleShape,
                shapeColor = Color.Yellow,
                backgroundColor = Color.White
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun originalOrderingWhenTheDefaultElevationIsUsed() {
        composeTestRule.setMaterialContent {
            Stack(Modifier
                .preferredSize(10.dp, 10.dp)
                .semantics(mergeAllDescendants = true)
                .testTag("stack")) {
                Surface(color = Color.Yellow) {
                    Box(Modifier.fillMaxSize())
                }
                Surface(color = Color.Green) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        findByTag("stack")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = RectangleShape,
                shapeColor = Color.Green,
                backgroundColor = Color.White
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun elevationRawValueIsUsedAsZIndex_drawsBelow() {
        composeTestRule.setMaterialContent {
            Stack(Modifier
                .preferredSize(10.dp, 10.dp)
                .semantics(mergeAllDescendants = true)
                .testTag("stack")) {
                Box(Modifier.fillMaxSize().drawBackground(Color.Green).zIndex(3f))
                Surface(color = Color.Yellow, elevation = 2.dp) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        findByTag("stack")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = RectangleShape,
                shapeColor = Color.Green,
                backgroundColor = Color.Green
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun elevationRawValueIsUsedAsZIndex_drawsAbove() {
        composeTestRule.setMaterialContent {
            Stack(Modifier
                .preferredSize(10.dp, 10.dp)
                .semantics(mergeAllDescendants = true)
                .testTag("stack")) {
                Box(Modifier.fillMaxSize().drawBackground(Color.Green).zIndex(1f))
                Surface(color = Color.Yellow, elevation = 2.dp) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        findByTag("stack")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = RectangleShape,
                shapeColor = Color.Yellow,
                backgroundColor = Color.Yellow
            )
    }
}
