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

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTextMinSizeTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun changingMinSizeConstraint_shrinksLayout() {
        var isBig by mutableStateOf(true)
        val widths = mutableListOf<Int>()
        rule.setContent {
            MinSizeChangeLayout(isBig = { isBig }, onMeasure = { widths += it }) { BasicText("A") }
        }
        rule.waitForIdle()
        isBig = !isBig
        rule.waitForIdle()
        val shrank = widths[0] - widths[1]
        assertThat(shrank).isGreaterThan(2) // fault if run with <4px layouts on bad devices
        assertThat(shrank).isEqualTo(widths[0] / 2)
    }

    @Test
    fun changingMinSizeConstraint_shrinksLayout_annotatedString() {
        var isBig by mutableStateOf(true)
        val widths = mutableListOf<Int>()
        rule.setContent {
            MinSizeChangeLayout(isBig = { isBig }, onMeasure = { widths += it }) {
                BasicText(AnnotatedString("A"))
            }
        }
        rule.waitForIdle()
        isBig = !isBig
        rule.waitForIdle()
        val shrank = widths[0] - widths[1]
        assertThat(shrank).isGreaterThan(2) // fault if run with <4px layouts on bad devices
        assertThat(shrank).isEqualTo(widths[0] / 2)
    }
}

@Composable
fun MinSizeChangeLayout(
    isBig: () -> Boolean,
    onMeasure: (Int) -> Unit,
    content: @Composable @UiComposable () -> Unit
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        measurePolicy = { measurables, constraints ->
            val newConstraints =
                Constraints(
                    minWidth =
                        if (isBig()) {
                            constraints.maxWidth
                        } else {
                            constraints.maxWidth / 2
                        },
                    maxWidth = constraints.maxWidth,
                    minHeight = 0,
                    maxHeight = constraints.maxHeight
                )
            val placeables = measurables.map { it.measure(newConstraints) }
            onMeasure(placeables.first().width)
            layout(constraints.minWidth, placeables[0].height) {
                placeables.forEachIndexed { _, it -> it.place(0, 0) }
            }
        },
        content = content
    )
}
