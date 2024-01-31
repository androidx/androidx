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

package androidx.compose.material3

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DividerTest {

    @get:Rule
    val rule = createComposeRule()

    private val defaultThickness = 1.dp

    @Test
    fun horizontalDivider_defaultSize() {
        rule
            .setMaterialContentForSizeAssertions {
                HorizontalDivider()
            }
            .assertHeightIsEqualTo(defaultThickness)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun horizontalDivider_customSize() {
        val thickness = 20.dp
        rule
            .setMaterialContentForSizeAssertions {
                HorizontalDivider(thickness = thickness)
            }
            .assertWidthIsEqualTo(rule.rootWidth())
            .assertHeightIsEqualTo(thickness)
    }

    @Test
    fun verticalDivider_defaultSize() {
        rule
            .setMaterialContentForSizeAssertions {
                VerticalDivider()
            }
            .assertHeightIsEqualTo(rule.rootHeight())
            .assertWidthIsEqualTo(defaultThickness)
    }

    @Test
    fun verticalDivider_customSize() {
        val thickness = 20.dp
        rule
            .setMaterialContentForSizeAssertions {
                VerticalDivider(thickness = thickness)
            }
            .assertWidthIsEqualTo(thickness)
            .assertHeightIsEqualTo(rule.rootHeight())
    }

    @Test
    fun divider_withIndent_doesNotChangeSize() {
        val indent = 75.dp
        val thickness = 21.dp

        rule
            .setMaterialContentForSizeAssertions {
                HorizontalDivider(
                    modifier = Modifier.padding(start = indent),
                    thickness = thickness
                )
            }
            .assertHeightIsEqualTo(thickness)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun divider_hairlineThickness() {
        var heightPx = 0
        rule.setMaterialContent(lightColorScheme()) {
            HorizontalDivider(
                modifier = Modifier.onGloballyPositioned { heightPx = it.size.height },
                thickness = Dp.Hairline,
            )
        }

        assertThat(heightPx).isEqualTo(0)
    }
}
