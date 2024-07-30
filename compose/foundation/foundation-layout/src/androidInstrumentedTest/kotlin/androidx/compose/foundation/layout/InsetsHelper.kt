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

package androidx.compose.foundation.layout

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class InsetsHelperTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun dpRectToAndroidXInsetsConverts() {
        lateinit var androidXInsets: androidx.core.graphics.Insets
        lateinit var density: Density

        rule.setContent {
            androidXInsets = DpRect(5.dp, 6.dp, 7.dp, 8.dp).roundToAndroidXInsets()
            density = LocalDensity.current
        }

        assertEquals(with(density) { 5.dp.roundToPx() }, androidXInsets.left)
        assertEquals(with(density) { 6.dp.roundToPx() }, androidXInsets.top)
        assertEquals(with(density) { 7.dp.roundToPx() }, androidXInsets.right)
        assertEquals(with(density) { 8.dp.roundToPx() }, androidXInsets.bottom)
    }
}
