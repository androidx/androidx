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
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
@RunWith(AndroidJUnit4::class)
class BasicTextHoverTest {
    @get:Rule val rule = createComposeRule()

    @Suppress("DEPRECATION")
    @Test
    fun whenSelectableText_andDefaultIcon_inBoxWithDefaultIcon_textIconIsUsed() =
        runSelectableTest(
            selectionContainerIconModifier = Modifier,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIconModifier = Modifier,
            expectedTextIcon = TYPE_TEXT
        )

    @Suppress("DEPRECATION")
    @Test
    fun whenSelectableText_andSetIcon_inBoxWithDefaultIcon_textIconIsUsed() =
        runSelectableTest(
            selectionContainerIconModifier = Modifier,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIconModifier = Modifier.pointerHoverIcon(PointerIcon.Crosshair),
            expectedTextIcon = TYPE_TEXT
        )

    @Suppress("DEPRECATION")
    @Test
    fun whenSelectableText_andSetIcon_withOverride_inBoxWithDefaultIcon_setIconIsUsed() =
        runSelectableTest(
            selectionContainerIconModifier = Modifier,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIconModifier =
                Modifier.pointerHoverIcon(icon = PointerIcon.Crosshair, overrideDescendants = true),
            expectedTextIcon = TYPE_CROSSHAIR
        )

    @Test
    fun whenSelectableText_andDefaultIcon_inBoxWithSetIcon_textIconIsUsed() =
        runSelectableTest(
            selectionContainerIconModifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            expectedSelectionContainerIcon = TYPE_HAND,
            textIconModifier = Modifier,
            expectedTextIcon = TYPE_TEXT
        )

    @Test
    fun whenSelectableText_andSetIcon_inBoxWithSetIcon_textIconIsUsed() =
        runSelectableTest(
            selectionContainerIconModifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            expectedSelectionContainerIcon = TYPE_HAND,
            textIconModifier = Modifier.pointerHoverIcon(PointerIcon.Crosshair),
            expectedTextIcon = TYPE_TEXT
        )

    @Test
    fun whenSelectableText_andSetIcon_withOverride_inBoxWithSetIcon_setIconIsUsed() =
        runSelectableTest(
            selectionContainerIconModifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            expectedSelectionContainerIcon = TYPE_HAND,
            textIconModifier =
                Modifier.pointerHoverIcon(icon = PointerIcon.Crosshair, overrideDescendants = true),
            expectedTextIcon = TYPE_CROSSHAIR
        )

    private fun runSelectableTest(
        selectionContainerIconModifier: Modifier,
        expectedSelectionContainerIcon: Int,
        textIconModifier: Modifier,
        expectedTextIcon: Int,
    ) =
        runTest(
            selectionContainerIconModifier,
            expectedSelectionContainerIcon,
            textIconModifier,
            expectedTextIcon,
        ) { containerTag: String, textTag: String, boxModifier: Modifier, textModifier: Modifier ->
            SelectionContainer {
                Box(
                    modifier =
                        Modifier.requiredSize(200.dp)
                            .then(boxModifier)
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .testTag(containerTag)
                ) {
                    BasicText(
                        text = "this is selectable text",
                        modifier = Modifier.then(textModifier).testTag(textTag)
                    )
                }
            }
        }

    @Suppress("DEPRECATION")
    @Test
    fun whenNonSelectableText_andDefaultIcon_inBoxWithDefaultIcon_textIconIsUsed() =
        runNonSelectableTest(
            selectionContainerIconModifier = Modifier,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIconModifier = Modifier,
            expectedTextIcon = TYPE_DEFAULT
        )

    @Suppress("DEPRECATION")
    @Test
    fun whenNonSelectableText_andSetIcon_inBoxWithDefaultIcon_setIconIsUsed() =
        runNonSelectableTest(
            selectionContainerIconModifier = Modifier,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIconModifier = Modifier.pointerHoverIcon(PointerIcon.Crosshair),
            expectedTextIcon = TYPE_CROSSHAIR
        )

    @Suppress("DEPRECATION")
    @Test
    fun whenNonSelectableText_andSetIcon_withOverride_inBoxWithDefaultIcon_setIconIsUsed() =
        runNonSelectableTest(
            selectionContainerIconModifier = Modifier,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIconModifier =
                Modifier.pointerHoverIcon(icon = PointerIcon.Crosshair, overrideDescendants = true),
            expectedTextIcon = TYPE_CROSSHAIR
        )

    @Test
    fun whenNonSelectableText_andDefaultIcon_inBoxWithSetIcon_textIconIsUsed() =
        runNonSelectableTest(
            selectionContainerIconModifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            expectedSelectionContainerIcon = TYPE_HAND,
            textIconModifier = Modifier,
            expectedTextIcon = TYPE_HAND
        )

    @Test
    fun whenNonSelectableText_andSetIcon_inBoxWithSetIcon_setIconIsUsed() =
        runNonSelectableTest(
            selectionContainerIconModifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            expectedSelectionContainerIcon = TYPE_HAND,
            textIconModifier = Modifier.pointerHoverIcon(PointerIcon.Crosshair),
            expectedTextIcon = TYPE_CROSSHAIR
        )

    @Test
    fun whenNonSelectableText_andSetIcon_withOverride_inBoxWithSetIcon_setIconIsUsed() =
        runNonSelectableTest(
            selectionContainerIconModifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            expectedSelectionContainerIcon = TYPE_HAND,
            textIconModifier =
                Modifier.pointerHoverIcon(icon = PointerIcon.Crosshair, overrideDescendants = true),
            expectedTextIcon = TYPE_CROSSHAIR
        )

    private fun runNonSelectableTest(
        selectionContainerIconModifier: Modifier,
        expectedSelectionContainerIcon: Int,
        textIconModifier: Modifier,
        expectedTextIcon: Int,
    ) =
        runTest(
            selectionContainerIconModifier,
            expectedSelectionContainerIcon,
            textIconModifier,
            expectedTextIcon,
        ) { containerTag: String, textTag: String, boxModifier: Modifier, textModifier: Modifier ->
            Box(
                modifier =
                    Modifier.requiredSize(200.dp)
                        .then(boxModifier)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(containerTag)
            ) {
                BasicText(
                    text = "this is selectable text",
                    modifier = Modifier.then(textModifier).testTag(textTag)
                )
            }
        }

    @Suppress("DEPRECATION")
    @Test
    fun whenDisabledSelectionText_andDefaultIcon_inBoxWithDefaultIcon_textIconIsUsed() =
        runDisabledSelectionText(
            selectionContainerIconModifier = Modifier,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIconModifier = Modifier,
            expectedTextIcon = TYPE_DEFAULT
        )

    @Suppress("DEPRECATION")
    @Test
    fun whenDisabledSelectionText_andSetIcon_inBoxWithDefaultIcon_setIconIsUsed() =
        runDisabledSelectionText(
            selectionContainerIconModifier = Modifier,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIconModifier = Modifier.pointerHoverIcon(PointerIcon.Crosshair),
            expectedTextIcon = TYPE_CROSSHAIR
        )

    @Suppress("DEPRECATION")
    @Test
    fun whenDisabledSelectionText_andSetIcon_withOverride_inBoxWithDefaultIcon_setIconIsUsed() =
        runDisabledSelectionText(
            selectionContainerIconModifier = Modifier,
            expectedSelectionContainerIcon = TYPE_DEFAULT,
            textIconModifier =
                Modifier.pointerHoverIcon(icon = PointerIcon.Crosshair, overrideDescendants = true),
            expectedTextIcon = TYPE_CROSSHAIR
        )

    @Test
    fun whenDisabledSelectionText_andDefaultIcon_inBoxWithSetIcon_textIconIsUsed() =
        runDisabledSelectionText(
            selectionContainerIconModifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            expectedSelectionContainerIcon = TYPE_HAND,
            textIconModifier = Modifier,
            expectedTextIcon = TYPE_HAND
        )

    @Test
    fun whenDisabledSelectionText_andSetIcon_inBoxWithSetIcon_setIconIsUsed() =
        runDisabledSelectionText(
            selectionContainerIconModifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            expectedSelectionContainerIcon = TYPE_HAND,
            textIconModifier = Modifier.pointerHoverIcon(PointerIcon.Crosshair),
            expectedTextIcon = TYPE_CROSSHAIR
        )

    @Test
    fun whenDisabledSelectionText_andSetIcon_withOverride_inBoxWithSetIcon_setIconIsUsed() =
        runDisabledSelectionText(
            selectionContainerIconModifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            expectedSelectionContainerIcon = TYPE_HAND,
            textIconModifier =
                Modifier.pointerHoverIcon(icon = PointerIcon.Crosshair, overrideDescendants = true),
            expectedTextIcon = TYPE_CROSSHAIR
        )

    private fun runDisabledSelectionText(
        selectionContainerIconModifier: Modifier,
        expectedSelectionContainerIcon: Int,
        textIconModifier: Modifier,
        expectedTextIcon: Int,
    ) =
        runTest(
            selectionContainerIconModifier,
            expectedSelectionContainerIcon,
            textIconModifier,
            expectedTextIcon,
        ) { containerTag: String, textTag: String, boxModifier: Modifier, textModifier: Modifier ->
            SelectionContainer {
                Box(
                    modifier =
                        Modifier.requiredSize(200.dp)
                            .then(boxModifier)
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .testTag(containerTag)
                ) {
                    DisableSelection {
                        BasicText(
                            text = "this is selectable text",
                            modifier = Modifier.then(textModifier).testTag(textTag)
                        )
                    }
                }
            }
        }

    private fun runTest(
        selectionContainerIconModifier: Modifier,
        expectedSelectionContainerIcon: Int,
        textIconModifier: Modifier,
        expectedTextIcon: Int,
        contentBlock:
            @Composable
            (
                containerTag: String, textTag: String, boxModifier: Modifier, textModifier: Modifier
            ) -> Unit,
    ) =
        with(PointerIconTestScope(rule)) {
            val selectionContainerTag = "container"
            val textTag = "text"

            setContent {
                contentBlock(
                    selectionContainerTag,
                    textTag,
                    selectionContainerIconModifier,
                    textIconModifier,
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
            @Suppress("DEPRECATION") assertIcon(TYPE_DEFAULT)
        }
}
