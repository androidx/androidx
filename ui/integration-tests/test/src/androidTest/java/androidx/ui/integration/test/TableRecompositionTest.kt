/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.integration.test

import androidx.test.filters.MediumTest
import androidx.ui.unit.dp
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.Table
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Surface
import androidx.ui.test.createComposeRule
import androidx.ui.test.doFramesUntilNoChangesPending
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Assert table recompositions.
 */
@MediumTest
@RunWith(Parameterized::class)
class TableRecompositionTest(private val numberOfCells: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun initParameters(): Array<Any> = arrayOf(1, 10)
    }

    @get:Rule
    val composeRule = createComposeRule(disableTransitions = true)

    @Test
    fun testTable_recomposition() {
        composeRule.forGivenContent {
            MaterialTheme {
                Surface {
                    Table(columns = numberOfCells) {
                        tableDecoration(overlay = false) { }
                        repeat(numberOfCells) {
                            tableRow {
                                ColoredRect(
                                    color = Color.Black,
                                    width = 100.dp,
                                    height = 50.dp,
                                    modifier = LayoutPadding(2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }.performTestWithEventsControl {
            // We expect more than 1 frame because Table decorations are relying on measureWithSize
            // which we don't have on the first frame.
            doFramesUntilNoChangesPending(2)
        }
    }
}
