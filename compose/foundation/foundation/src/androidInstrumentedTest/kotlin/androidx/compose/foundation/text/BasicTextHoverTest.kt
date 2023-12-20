/*
 * Copyright 2023 The Android Open Source Project
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

import android.os.Build
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@OptIn(ExperimentalTestApi::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
@RunWith(AndroidJUnit4::class)
class BasicTextHoverTest {
    @get:Rule
    val rule = createComposeRule()
    private val defaultDisableSelectionIcon = android.view.PointerIcon.TYPE_DEFAULT
    private val defaultSelectableIcon = android.view.PointerIcon.TYPE_TEXT

    /**
     * Verifies the default [PointerIcon] for selectable and non-selectable text.
     */
    @Test
    fun IBeamDefaults() {
        val selectionContainerTag = "mySelectionContainer"
        val disableSelectableTextTag = "myDisableSelection"
        lateinit var view: View

        rule.setContent {
            view = LocalView.current
            Box(
                modifier = Modifier
                    .requiredSize(200.dp)
                    .border(BorderStroke(2.dp, SolidColor(Color.Red)))
            ) {
                Column {
                    SelectionContainer {
                        Column {
                            BasicText(
                                text = "this is selectable text",
                                modifier = Modifier.testTag(selectionContainerTag)
                            )
                            DisableSelection {
                                BasicText(
                                    text = "not selectable",
                                    modifier = Modifier.testTag(disableSelectableTextTag)
                                )
                            }
                        }
                    }
                }
            }
        }
        // Hover over selectable text
        rule.onNodeWithTag(selectionContainerTag).performMouseInput {
            enter(bottomRight)
        }
        // Verify the current icon is the default selectable icon
        verifyIcon(defaultSelectableIcon, view)
        // Move cursor to hover over DisableSelection text
        rule.onNodeWithTag(disableSelectableTextTag).performMouseInput {
            moveTo(bottomRight)
        }
        // Verify the current icon is the default arrow icon
        verifyIcon(defaultDisableSelectionIcon, view)
        // Exit hovering over element
        rule.onNodeWithTag(disableSelectableTextTag).performMouseInput {
            exit()
        }
    }

    private fun verifyIcon(type: Int, view: View) {
        rule.runOnIdle {
            assertThat(view.pointerIcon).isEqualTo(
                android.view.PointerIcon.getSystemIcon(
                    view.context,
                    type
                )
            )
        }
    }
}
