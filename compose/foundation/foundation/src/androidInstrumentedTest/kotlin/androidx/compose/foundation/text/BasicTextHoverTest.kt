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
import android.view.PointerIcon.TYPE_CROSSHAIR
import android.view.PointerIcon.TYPE_DEFAULT
import android.view.PointerIcon.TYPE_HAND
import android.view.PointerIcon.TYPE_TEXT
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
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

    @Test
    fun whenSelectableText_andDefaultIcon_inBoxWithDefaultIcon_textIconIsUsed() =
        runSelectableTest(
            selectionContainerIcon = null,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIcon = null,
            expectedTextIcon = TYPE_TEXT
        )

    @Test
    fun whenSelectableText_andSetIcon_inBoxWithDefaultIcon_setIconIsUsed() =
        runSelectableTest(
            selectionContainerIcon = null,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIcon = PointerIcon.Crosshair,
            expectedTextIcon = TYPE_CROSSHAIR
        )

    @Test
    fun whenSelectableText_andDefaultIcon_inBoxWithSetIcon_textIconIsUsed() =
        runSelectableTest(
            selectionContainerIcon = PointerIcon.Hand,
            expectedSelectionContainerIcon = TYPE_HAND,
            textIcon = null,
            expectedTextIcon = TYPE_TEXT
        )

    @Test
    fun whenSelectableText_andSetIcon_inBoxWithSetIcon_setIconIsUsed() =
        runSelectableTest(
            selectionContainerIcon = PointerIcon.Hand,
            expectedSelectionContainerIcon = TYPE_HAND,
            textIcon = PointerIcon.Crosshair,
            expectedTextIcon = TYPE_CROSSHAIR
        )

    private fun runSelectableTest(
        selectionContainerIcon: PointerIcon?,
        expectedSelectionContainerIcon: Int,
        textIcon: PointerIcon?,
        expectedTextIcon: Int,
    ) = runTest(
        selectionContainerIcon,
        expectedSelectionContainerIcon,
        textIcon,
        expectedTextIcon,
    ) { containerTag: String, textTag: String, boxModifier: Modifier, textModifier: Modifier ->
        SelectionContainer {
            Box(
                modifier = Modifier
                    .requiredSize(200.dp)
                    .then(boxModifier)
                    .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                    .testTag(containerTag)
            ) {
                BasicText(
                    text = "this is selectable text",
                    modifier = Modifier
                        .then(textModifier)
                        .testTag(textTag)
                )
            }
        }
    }

    @Test
    fun whenNonSelectableText_andDefaultIcon_inBoxWithDefaultIcon_textIconIsUsed() =
        runNonSelectableTest(
            selectionContainerIcon = null,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIcon = null,
            expectedTextIcon = TYPE_DEFAULT
        )

    @Test
    fun whenNonSelectableText_andSetIcon_inBoxWithDefaultIcon_setIconIsUsed() =
        runNonSelectableTest(
            selectionContainerIcon = null,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIcon = PointerIcon.Crosshair,
            expectedTextIcon = TYPE_CROSSHAIR
        )

    @Test
    fun whenNonSelectableText_andDefaultIcon_inBoxWithSetIcon_textIconIsUsed() =
        runNonSelectableTest(
            selectionContainerIcon = PointerIcon.Hand,
            expectedSelectionContainerIcon = TYPE_HAND,
            textIcon = null,
            expectedTextIcon = TYPE_HAND
        )

    @Test
    fun whenNonSelectableText_andSetIcon_inBoxWithSetIcon_setIconIsUsed() =
        runNonSelectableTest(
            selectionContainerIcon = PointerIcon.Hand,
            expectedSelectionContainerIcon = TYPE_HAND,
            textIcon = PointerIcon.Crosshair,
            expectedTextIcon = TYPE_CROSSHAIR
        )

    private fun runNonSelectableTest(
        selectionContainerIcon: PointerIcon?,
        expectedSelectionContainerIcon: Int,
        textIcon: PointerIcon?,
        expectedTextIcon: Int,
    ) = runTest(
        selectionContainerIcon,
        expectedSelectionContainerIcon,
        textIcon,
        expectedTextIcon,
    ) { containerTag: String, textTag: String, boxModifier: Modifier, textModifier: Modifier ->
        Box(
            modifier = Modifier
                .requiredSize(200.dp)
                .then(boxModifier)
                .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                .testTag(containerTag)
        ) {
            BasicText(
                text = "this is selectable text",
                modifier = Modifier
                    .then(textModifier)
                    .testTag(textTag)
            )
        }
    }

    @Test
    fun whenDisabledSelectionText_andDefaultIcon_inBoxWithDefaultIcon_textIconIsUsed() =
        runDisabledSelectionText(
            selectionContainerIcon = null,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIcon = null,
            expectedTextIcon = TYPE_DEFAULT
        )

    @Test
    fun whenDisabledSelectionText_andSetIcon_inBoxWithDefaultIcon_setIconIsUsed() =
        runDisabledSelectionText(
            selectionContainerIcon = null,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIcon = PointerIcon.Crosshair,
            expectedTextIcon = TYPE_CROSSHAIR
        )

    @Test
    fun whenDisabledSelectionText_andDefaultIcon_inBoxWithSetIcon_textIconIsUsed() =
        runDisabledSelectionText(
            selectionContainerIcon = PointerIcon.Hand,
            expectedSelectionContainerIcon = TYPE_HAND,
            textIcon = null,
            expectedTextIcon = TYPE_HAND
        )

    @Test
    fun whenDisabledSelectionText_andSetIcon_inBoxWithSetIcon_setIconIsUsed() =
        runDisabledSelectionText(
            selectionContainerIcon = PointerIcon.Hand,
            expectedSelectionContainerIcon = TYPE_HAND,
            textIcon = PointerIcon.Crosshair,
            expectedTextIcon = TYPE_CROSSHAIR
        )

    private fun runDisabledSelectionText(
        selectionContainerIcon: PointerIcon?,
        expectedSelectionContainerIcon: Int,
        textIcon: PointerIcon?,
        expectedTextIcon: Int,
    ) = runTest(
        selectionContainerIcon,
        expectedSelectionContainerIcon,
        textIcon,
        expectedTextIcon,
    ) { containerTag: String, textTag: String, boxModifier: Modifier, textModifier: Modifier ->
        SelectionContainer {
            Box(
                modifier = Modifier
                    .requiredSize(200.dp)
                    .then(boxModifier)
                    .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                    .testTag(containerTag)
            ) {
                DisableSelection {
                    BasicText(
                        text = "this is selectable text",
                        modifier = Modifier
                            .then(textModifier)
                            .testTag(textTag)
                    )
                }
            }
        }
    }

    private fun runTest(
        selectionContainerIcon: PointerIcon?,
        expectedSelectionContainerIcon: Int,
        textIcon: PointerIcon?,
        expectedTextIcon: Int,
        contentBlock: @Composable (
            containerTag: String,
            textTag: String,
            boxModifier: Modifier,
            textModifier: Modifier
        ) -> Unit,
    ) = with(PointerIconTestScope(rule)) {
        val selectionContainerTag = "container"
        val textTag = "text"

        fun testPointerHoverIcon(icon: PointerIcon?): Modifier =
            if (icon == null) Modifier else Modifier.pointerHoverIcon(icon)

        setContent {
            contentBlock(
                selectionContainerTag,
                textTag,
                testPointerHoverIcon(selectionContainerIcon),
                testPointerHoverIcon(textIcon),
            )
        }

        // Hover over text
        rule.onNodeWithTag(textTag).performMouseInput { enter(bottomRight) }
        assertIcon(expectedTextIcon)

        // Move cursor to hover over portion of the parent box not covered by any descendants
        rule.onNodeWithTag(selectionContainerTag).performMouseInput { moveTo(bottomRight) }
        assertIcon(expectedSelectionContainerIcon)

        // Exit hovering over element
        rule.onNodeWithTag(selectionContainerTag).performMouseInput { exit() }
        assertIcon(TYPE_DEFAULT)
    }
}
