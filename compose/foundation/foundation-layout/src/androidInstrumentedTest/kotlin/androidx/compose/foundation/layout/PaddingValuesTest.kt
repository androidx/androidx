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
package androidx.compose.foundation.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PaddingValuesTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun zeroPaddingValues() {
        var width = 0
        var height = 0
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                val sizeDp = with(LocalDensity.current) { 100.toDp() }
                Box(
                    Modifier.size(sizeDp).padding(PaddingValues.Zero).onPlaced {
                        width = it.size.width
                        height = it.size.height
                    }
                )
            }
        }

        rule.runOnIdle {
            assertThat(width).isEqualTo(100)
            assertThat(height).isEqualTo(100)
        }
    }
}
