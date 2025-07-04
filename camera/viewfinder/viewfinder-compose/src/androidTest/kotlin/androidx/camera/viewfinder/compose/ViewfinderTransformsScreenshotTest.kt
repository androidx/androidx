/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.viewfinder.compose

import android.util.Size
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import com.google.common.truth.TruthJUnit.assume
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Tests for [Viewfinder] with various output transforms via [ContentScale] and [Alignment] */
@SdkSuppress(minSdkVersion = 33) // Required for screenshot tests
@LargeTest
@RunWith(Parameterized::class)
class ViewfinderTransformsScreenshotTest(
    private val implementationMode: ImplementationMode,
    private val contentScale: ContentScale,
    private val alignment: Alignment,
    private val name: String,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "impl={0},type={3}")
        fun data() =
            setOf(ImplementationMode.EMBEDDED, ImplementationMode.EXTERNAL).flatMap { impl ->
                setOf(
                        arrayOf(ContentScale.Crop, Alignment.Center, "crop_center"),
                        arrayOf(ContentScale.Crop, Alignment.TopStart, "crop_start"),
                        arrayOf(ContentScale.Crop, Alignment.BottomEnd, "crop_end"),
                        arrayOf(ContentScale.Fit, Alignment.Center, "fit_center"),
                        arrayOf(ContentScale.Fit, Alignment.TopStart, "fit_start"),
                        arrayOf(ContentScale.Fit, Alignment.BottomEnd, "fit_end"),
                        arrayOf(ContentScale.FillBounds, Alignment.Center, "fill_bounds_center"),
                        arrayOf(ContentScale.None, Alignment.Center, "none_center"),
                    )
                    .map { args -> arrayOf(impl, *args) }
            }
    }

    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_CAMERA_VIEWFINDER_COMPOSE)

    @Test
    fun viewfinderTransformsCorrectly() = runBlocking {
        assume()
            .withMessage("ComposeTest cannot yet capture SurfaceView content to Image.")
            .that(implementationMode == ImplementationMode.EMBEDDED)
            .isTrue()

        val testParams =
            ViewfinderTestParams(
                viewfinderSize = DpSize(360.dp, 640.dp),
                sourceResolution = Size(720, 540),
                implementationMode = implementationMode,
                alignment = alignment,
                contentScale = contentScale,
            )

        drawAndAssertAgainstGolden(
            composeTestRule = composeTestRule,
            screenshotRule = screenshotRule,
            testParams = testParams,
            goldenIdentifier = "upright_face_with_mapped_touch_point_$name",
        )
    }
}
