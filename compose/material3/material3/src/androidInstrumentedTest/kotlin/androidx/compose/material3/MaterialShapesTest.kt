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

package androidx.compose.material3

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class MaterialShapesTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun roundedPolygon_outlineSize() {
        var outline: Outline? = null
        rule.setContent {
            // For the sake of this test, we pick the Clover4Leaf that should have the same bounds
            // as the actual size given to it. Other shapes have smaller bounds.
            val shape = MaterialShapes.Clover4Leaf.toShape()
            outline =
                shape.createOutline(
                    Size(100f, 100f),
                    LocalLayoutDirection.current,
                    density = LocalDensity.current,
                )
        }
        assertThat(outline).isNotNull()
        assertThat(outline!!.bounds).isEqualTo(Rect(Offset.Zero, Size(100f, 100f)))
    }

    @Test
    fun roundedPolygon_multipleCreateOutlineCalls() {
        var outline1: Outline? = null
        var outline2: Outline? = null
        rule.setContent {
            val shape = MaterialShapes.Clover4Leaf.toShape()
            outline1 =
                shape.createOutline(
                    Size(100f, 100f),
                    LocalLayoutDirection.current,
                    density = LocalDensity.current,
                )
            outline2 =
                shape.createOutline(
                    Size(50f, 50f),
                    LocalLayoutDirection.current,
                    density = LocalDensity.current,
                )
        }
        assertThat(outline1).isNotNull()
        assertThat(outline2).isNotNull()
        // Ensure that the two outlines have different bounds.
        assertThat(outline1!!.bounds).isNotEqualTo(outline2!!.bounds)
    }
}
