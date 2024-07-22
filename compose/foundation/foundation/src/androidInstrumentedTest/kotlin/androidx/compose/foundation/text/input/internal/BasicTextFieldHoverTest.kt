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

package androidx.compose.foundation.text.input.internal

import android.os.Build
import android.view.PointerIcon.TYPE_CROSSHAIR
import android.view.PointerIcon.TYPE_DEFAULT
import android.view.PointerIcon.TYPE_HAND
import android.view.PointerIcon.TYPE_TEXT
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.PointerIconTestScope
import androidx.compose.foundation.text.input.rememberTextFieldState
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
class BasicTextFieldHoverTest {
    @get:Rule val rule = createComposeRule()

    @Suppress("DEPRECATION")
    @Test
    fun whenDefaultIcon_inBoxWithDefaultIcon_textIconIsUsed() =
        runTest(
            boxIconModifier = Modifier,
            expectedBoxIcon = TYPE_DEFAULT,
            textFieldIconModifier = Modifier,
            expectedTextIcon = TYPE_TEXT
        )

    @Suppress("DEPRECATION")
    @Test
    fun whenSetIcon_inBoxWithDefaultIcon_textIconIsUsed() =
        runTest(
            boxIconModifier = Modifier,
            expectedBoxIcon = TYPE_DEFAULT,
            textFieldIconModifier = Modifier.pointerHoverIcon(PointerIcon.Crosshair),
            expectedTextIcon = TYPE_TEXT
        )

    @Suppress("DEPRECATION")
    @Test
    fun whenSetIcon_withOverride_inBoxWithDefaultIcon_setIconIsUsed() =
        runTest(
            boxIconModifier = Modifier,
            expectedBoxIcon = TYPE_DEFAULT,
            textFieldIconModifier =
                Modifier.pointerHoverIcon(icon = PointerIcon.Crosshair, overrideDescendants = true),
            expectedTextIcon = TYPE_CROSSHAIR
        )

    @Test
    fun whenDefaultIcon_inBoxWithSetIcon_textIconIsUsed() =
        runTest(
            boxIconModifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            expectedBoxIcon = TYPE_HAND,
            textFieldIconModifier = Modifier,
            expectedTextIcon = TYPE_TEXT
        )

    @Test
    fun whenSetIcon_inBoxWithSetIcon_textIconIsUsed() =
        runTest(
            boxIconModifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            expectedBoxIcon = TYPE_HAND,
            textFieldIconModifier = Modifier.pointerHoverIcon(PointerIcon.Crosshair),
            expectedTextIcon = TYPE_TEXT
        )

    @Test
    fun whenSetIcon_withOverride_inBoxWithSetIcon_setIconIsUsed() =
        runTest(
            boxIconModifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            expectedBoxIcon = TYPE_HAND,
            textFieldIconModifier =
                Modifier.pointerHoverIcon(icon = PointerIcon.Crosshair, overrideDescendants = true),
            expectedTextIcon = TYPE_CROSSHAIR
        )

    private fun runTest(
        boxIconModifier: Modifier,
        expectedBoxIcon: Int,
        textFieldIconModifier: Modifier,
        expectedTextIcon: Int,
    ) =
        with(PointerIconTestScope(rule)) {
            val boxTag = "myParentIcon"
            val textFieldTag = "myCoreTextField"

            setContent {
                val tfs = rememberTextFieldState("initial text")
                Box(
                    modifier =
                        Modifier.requiredSize(200.dp)
                            .then(boxIconModifier)
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .testTag(boxTag)
                ) {
                    BasicTextField(
                        state = tfs,
                        modifier =
                            Modifier.requiredSize(50.dp)
                                .then(textFieldIconModifier)
                                .testTag(textFieldTag)
                    )
                }
            }

            // Hover over CoreTextField
            rule.onNodeWithTag(textFieldTag).performMouseInput { enter(bottomRight) }
            assertIcon(expectedTextIcon)

            // Move cursor to hover over portion of the parent box not covered by any descendants
            rule.onNodeWithTag(boxTag).performMouseInput { moveTo(bottomRight) }
            assertIcon(expectedBoxIcon)

            // Exit hovering over element
            rule.onNodeWithTag(boxTag).performMouseInput { exit() }
            @Suppress("DEPRECATION") assertIcon(TYPE_DEFAULT)
        }
}
