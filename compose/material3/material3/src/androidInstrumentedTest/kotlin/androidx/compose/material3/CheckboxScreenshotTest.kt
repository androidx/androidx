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
package androidx.compose.material3

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class CheckboxScreenshotTest(
    private val scheme: ColorSchemeWrapper,
    private val isCheckboxStyleM3FixEnabled: Boolean,
) {

    @OptIn(ExperimentalMaterial3Api::class)
    @Before
    fun setUp() {
        isCheckboxStylingFixEnabled = isCheckboxStyleM3FixEnabled
    }

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @After
    fun resetTouchMode() =
        with(InstrumentationRegistry.getInstrumentation()) {
            if (SDK_INT < 33) setInTouchMode(true) else resetInTouchMode()
        }

    val wrap = Modifier.wrapContentSize(Alignment.TopStart)

    // TODO: this test tag as well as Boxes inside test are temporary, remove then b/157687898
    //  is fixed
    private val wrapperTestTag = "checkboxWrapper"

    @Test
    fun checkBox_checked() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { Checkbox(checked = true, onCheckedChange = {}) }
        }
        assertToggeableAgainstGolden("checkBox_${getParametersName()}_checked")
    }

    @Test
    fun checkBox_unchecked() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(modifier = wrap, checked = false, onCheckedChange = {})
            }
        }
        assertToggeableAgainstGolden("checkBox_${getParametersName()}_unchecked")
    }

    @Test
    @Ignore("b/355413615")
    fun checkBox_pressed() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(modifier = wrap, checked = false, onCheckedChange = {})
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isToggleable()).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertToggeableAgainstGolden("checkBox_${getParametersName()}_pressed")
    }

    @Test
    fun checkBox_indeterminate() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                TriStateCheckbox(
                    state = ToggleableState.Indeterminate,
                    modifier = wrap,
                    onClick = {},
                )
            }
        }
        assertToggeableAgainstGolden("checkBox_${getParametersName()}_indeterminate")
    }

    @Test
    fun checkBox_disabled_checked() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(modifier = wrap, checked = true, enabled = false, onCheckedChange = {})
            }
        }
        assertToggeableAgainstGolden("checkBox_${getParametersName()}_disabled_checked")
    }

    @Test
    fun checkBox_disabled_unchecked() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(modifier = wrap, checked = false, enabled = false, onCheckedChange = {})
            }
        }
        assertToggeableAgainstGolden("checkBox_${getParametersName()}_disabled_unchecked")
    }

    @Test
    fun checkBox_disabled_indeterminate() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                TriStateCheckbox(
                    state = ToggleableState.Indeterminate,
                    enabled = false,
                    modifier = wrap,
                    onClick = {},
                )
            }
        }
        assertToggeableAgainstGolden("checkBox_${getParametersName()}_disabled_indeterminate")
    }

    @Test
    fun checkBox_unchecked_animateToChecked() {
        val isChecked = mutableStateOf(false)
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(
                    modifier = wrap,
                    checked = isChecked.value,
                    onCheckedChange = { isChecked.value = it },
                )
            }
        }

        rule.mainClock.autoAdvance = false

        // Because Ripples are drawn on the RenderThread, it is hard to synchronize them with
        // Compose animations, so instead just manually change the value instead of triggering
        // and trying to screenshot a ripple
        rule.runOnIdle { isChecked.value = true }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 100)

        assertToggeableAgainstGolden("checkBox_${getParametersName()}_unchecked_animateToChecked")
    }

    @Test
    fun checkBox_checked_animateToUnchecked() {
        val isChecked = mutableStateOf(true)
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(
                    modifier = wrap,
                    checked = isChecked.value,
                    onCheckedChange = { isChecked.value = it },
                )
            }
        }

        rule.mainClock.autoAdvance = false

        // Because Ripples are drawn on the RenderThread, it is hard to synchronize them with
        // Compose animations, so instead just manually change the value instead of triggering
        // and trying to screenshot a ripple
        rule.runOnIdle { isChecked.value = false }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 100)

        assertToggeableAgainstGolden("checkBox_${getParametersName()}_checked_animateToUnchecked")
    }

    @Test
    fun checkBox_hover() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(modifier = wrap, checked = true, onCheckedChange = {})
            }
        }

        rule.onNode(isToggleable()).performMouseInput { enter(center) }

        rule.waitForIdle()

        assertToggeableAgainstGolden("checkBox_${getParametersName()}_hover")
    }

    @Test
    fun checkBox_focus() {
        val focusRequester = FocusRequester()
        var localInputModeManager: InputModeManager? = null

        rule.setMaterialContent(scheme.colorScheme) {
            localInputModeManager = LocalInputModeManager.current
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(
                    modifier = wrap.focusRequester(focusRequester),
                    checked = true,
                    onCheckedChange = {},
                )
            }
        }

        rule.runOnIdle {
            localInputModeManager!!.requestInputMode(InputMode.Keyboard)
            focusRequester.requestFocus()
        }

        rule.waitForIdle()

        assertToggeableAgainstGolden("checkBox_${getParametersName()}_focus")
    }

    @Test
    fun checkBox_customColors() {
        rule.setMaterialContent(scheme.colorScheme) {
            Column(wrap.testTag(wrapperTestTag), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val colors =
                    CheckboxDefaults.colors(
                        checkedColor = Color.Red,
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.Green,
                        disabledCheckedColor = Color.Red.copy(alpha = 0.38f),
                        disabledUncheckedColor = Color.Gray.copy(alpha = 0.38f),
                        disabledIndeterminateColor = Color.Magenta.copy(alpha = 0.38f),
                    )
                Checkboxes(colors = colors)
            }
        }

        rule.waitForIdle()

        assertToggeableAgainstGolden("checkBox_${getParametersName()}_customColors")
    }

    @Test
    fun checkBox_customCheckboxColorsConstruct() {
        rule.setMaterialContent(scheme.colorScheme) {
            Column(wrap.testTag(wrapperTestTag), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val colors =
                    CheckboxColors(
                        checkedCheckmarkColor = Color.Black,
                        // Irrelevant for the test, as this color only appears when the check mark
                        // transitions from checked to unchecked.
                        uncheckedCheckmarkColor = Color.Transparent,
                        disabledCheckmarkColor = Color.White,
                        checkedBoxColor = Color.Green,
                        uncheckedBoxColor = Color.Yellow,
                        disabledCheckedBoxColor = Color.Green.copy(alpha = 0.38f),
                        disabledUncheckedBoxColor = Color.Yellow.copy(alpha = 0.38f),
                        disabledIndeterminateBoxColor = Color.Magenta.copy(alpha = 0.38f),
                        checkedBorderColor = Color.Red,
                        uncheckedBorderColor = Color.Black,
                        disabledBorderColor = Color.Red.copy(alpha = 0.38f),
                        disabledUncheckedBorderColor = Color.Blue,
                        disabledIndeterminateBorderColor = Color.LightGray,
                    )
                Checkboxes(colors = colors)
            }
        }

        rule.waitForIdle()

        assertToggeableAgainstGolden(
            "checkBox_${getParametersName()}_customCheckboxColorsConstruct"
        )
    }

    @Test
    fun checkBox_customStroke_checked() {
        rule.setMaterialContent(scheme.colorScheme) {
            val stroke =
                Stroke(
                    width = with(LocalDensity.current) { CheckboxDefaults.StrokeWidth.toPx() },
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(
                    modifier = wrap,
                    checked = true,
                    onCheckedChange = {},
                    checkmarkStroke = stroke,
                    outlineStroke = stroke,
                )
            }
        }
        assertToggeableAgainstGolden("checkBox_${getParametersName()}_customStroke_checked")
    }

    @Test
    fun checkBox_customStroke_unchecked() {
        rule.setMaterialContent(scheme.colorScheme) {
            // Have the stroke thinner so we can verify it's being applied to the rounded box.
            val stroke =
                Stroke(
                    width = with(LocalDensity.current) { CheckboxDefaults.StrokeWidth.toPx() / 4 },
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
            Box(wrap.testTag(wrapperTestTag)) {
                Checkbox(
                    modifier = wrap,
                    checked = false,
                    onCheckedChange = {},
                    checkmarkStroke = stroke,
                    outlineStroke = stroke,
                )
            }
        }
        assertToggeableAgainstGolden("checkBox_${getParametersName()}_customStroke_unchecked")
    }

    @Test
    fun checkBox_customStroke_indeterminate() {
        rule.setMaterialContent(scheme.colorScheme) {
            val stroke =
                Stroke(
                    width = with(LocalDensity.current) { CheckboxDefaults.StrokeWidth.toPx() },
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
            Box(wrap.testTag(wrapperTestTag)) {
                TriStateCheckbox(
                    state = ToggleableState.Indeterminate,
                    checkmarkStroke = stroke,
                    outlineStroke = stroke,
                    modifier = wrap,
                    onClick = {},
                )
            }
        }
        assertToggeableAgainstGolden("checkBox_${getParametersName()}_customStroke_indeterminate")
    }

    @Composable
    private fun Checkboxes(colors: CheckboxColors) {
        TriStateCheckbox(state = ToggleableState.Off, onClick = {}, colors = colors)
        TriStateCheckbox(
            state = ToggleableState.Off,
            onClick = {},
            enabled = false,
            colors = colors,
        )
        TriStateCheckbox(state = ToggleableState.On, onClick = {}, colors = colors)
        TriStateCheckbox(state = ToggleableState.On, onClick = {}, enabled = false, colors = colors)
        TriStateCheckbox(state = ToggleableState.Indeterminate, onClick = {}, colors = colors)
        TriStateCheckbox(
            state = ToggleableState.Indeterminate,
            onClick = {},
            enabled = false,
            colors = colors,
        )
        Checkbox(checked = false, onCheckedChange = {}, colors = colors)
        Checkbox(checked = false, onCheckedChange = {}, enabled = false, colors = colors)
        Checkbox(checked = true, onCheckedChange = {}, colors = colors)
        Checkbox(checked = true, onCheckedChange = {}, enabled = false, colors = colors)
    }

    private fun assertToggeableAgainstGolden(goldenName: String) {
        // TODO: replace with find(isToggeable()) after b/157687898 is fixed
        rule
            .onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    private fun getParametersName(): String {
        return "${scheme.name}_checkBoxStyleM3FixEnabled-${isCheckboxStyleM3FixEnabled}"
    }

    // Provides the ColorScheme and their name parameter in a ColorSchemeWrapper plus feature flag
    // param enabling checkbox styling tokens.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}_checkBoxStyleM3FixEnabled-{1}")
        fun parameters(): Collection<Array<Any>> {
            val colorSchemes =
                listOf(
                    ColorSchemeWrapper("lightTheme", lightColorScheme()),
                    ColorSchemeWrapper("darkTheme", darkColorScheme()),
                )
            val flagStates = listOf(true, false)
            val params = mutableListOf<Array<Any>>()

            for (scheme in colorSchemes) {
                for (flagState in flagStates) {
                    params.add(arrayOf(scheme, flagState))
                }
            }
            return params
        }
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}
