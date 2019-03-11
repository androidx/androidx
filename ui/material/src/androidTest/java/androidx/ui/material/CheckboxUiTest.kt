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
package androidx.ui.material.test

import androidx.test.filters.SmallTest
import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.baseui.selection.ToggleableState.CHECKED
import androidx.ui.baseui.selection.ToggleableState.INDETERMINATE
import androidx.ui.baseui.selection.ToggleableState.UNCHECKED
import androidx.ui.core.Constraints
import androidx.ui.core.CraneWrapper
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.div
import androidx.ui.material.Checkbox
import androidx.ui.material.MaterialTheme
import androidx.ui.painting.Color
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import androidx.ui.core.times
import androidx.ui.material.RadioButtonWrapper
import androidx.ui.material.SwitchWrapper
import androidx.ui.test.android.AndroidUiTestRunner
import androidx.ui.test.assertIsChecked
import androidx.ui.test.assertIsNotChecked
import androidx.ui.test.assertIsVisible
import androidx.ui.test.assertSemanticsIsEqualTo
import androidx.ui.test.copyWith
import androidx.ui.test.createFullSemantics
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.Model
import com.google.r4a.composer
import org.junit.Ignore

@Model
class CheckboxState(
    var color: Color? = null,
    var value: ToggleableState = CHECKED
) {
    fun toggle() {
        value = if (value == CHECKED) UNCHECKED else CHECKED
    }
}

@SmallTest
@RunWith(JUnit4::class)
class CheckboxUiTest : AndroidUiTestRunner() {
    private val state = CheckboxState(value=CHECKED)
    private var state0 = CheckboxState(value=CHECKED)
    private val state1 = CheckboxState(value=CHECKED)
    // TODO(b/126881459): this should be the default semantic for checkbox
    private val defaultCheckboxSemantics = createFullSemantics(
        enabled = false,
        checked = false
    )

    @Test
    @Ignore("Check this after recomposition problems are solved")
    fun checkBoxTestDemo() {
        // TODO(pavlis): AndroidUiTestRunner should wrap with CraneWrapper automatically once
        // meta-data are fixed
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Checkbox
                        testTag="myCheckbox"
                        value=state.value
                        onToggle={
                            state.toggle()
                        }/>
                </MaterialTheme>
            </CraneWrapper>
        }

        // TODO(pavlis): Improve this section
        // This is just an initial demo of testing API.

        // The API below is what developers will be using (still WIP). And what we will use
        // to verify interactions in our widgets. E.g. clickButton -> assertIsChecked. However there
        // should  be also an initial test for each widget that assert the whole semantics, see few
        // lines below.
        // FYI: This is a real working test now
        findByTag("myCheckbox")
            .assertIsVisible()
            .doClick()

        // TODO(catalintudor): eliminate the need for a second call that recreates
        // the SemanticsTreeQuery cache
        findByTag("myCheckbox")
            .assertIsNotChecked()

        // Every widget should have test that verifies that its default semantics (when the widget
        // is initialized) is correct. E.g. button has clickable set, or editText is focusable.
        // For that we don't want to use the APIs above as it is easy to forget to verify some
        // property. So because of that we are introducing the assertion below where all fields will
        // be mandatory in the future. That will enforce each widget owner to make assumptions
        // about what their widgets need to provide.
        findByTag("myCheckbox")
            .assertSemanticsIsEqualTo(
                defaultCheckboxSemantics.copyWith { checked = false }
            )
    }

    @Test
    fun checkBoxTest_defaultSemantics() {
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Checkbox testTag="myCheckbox" />
                </MaterialTheme>
            </CraneWrapper>
        }

        findByTag("myCheckbox")
            .assertSemanticsIsEqualTo(
                defaultCheckboxSemantics
            )
    }

    // TODO: Crashes on java.lang.IllegalStateException: Current layout is not an ancestor of the
    // providedchild layout
    @Test
    @Ignore("Check this after recomposition problems are solved")
    fun checkBoxTest_fillgrid() {
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <FillGrid2 horizontalGridCount=4>
                        <Checkbox
                            value=state0.value
                            onToggle={ state0.toggle() }
                            testTag="myCheckbox"/>
                        <Checkbox value=UNCHECKED />
                        <Checkbox />
                        <Checkbox value=INDETERMINATE />
                        <SwitchWrapper checked=true />
                        <SwitchWrapper checked=false />
                        <SwitchWrapper checked=true/>
                        <SwitchWrapper checked=false/>
                        <RadioButtonWrapper checked=true />
                        <RadioButtonWrapper checked=false />
                        <RadioButtonWrapper checked=true />
                        <RadioButtonWrapper checked=false/>
                    </FillGrid2>
                </MaterialTheme>
            </CraneWrapper>
        }

        findByTag("myCheckbox")
            .assertIsVisible()
            .doClick()

        // TODO(catalintudor): eliminate the need for a second call that recreates
        // the SemanticsTreeQuery cache
        findByTag("myCheckbox")
            .assertIsNotChecked()
    }

    // TODO: Crashes because after recompose checkbox becomes invisible
    @Test
    @Ignore("b/124640800")
    fun checkBoxTest_ClickTwice() {
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Checkbox value=state1.value
                        onToggle={ state1.toggle() }
                        testTag="myCheckbox" />
                </MaterialTheme>
            </CraneWrapper>
        }

        findByTag("myCheckbox")
            .assertIsVisible()
            .doClick()

        Thread.sleep(3000) // TODO(catalintudor): delete this after test passes

        // NOTE: This is where we crash: checkbox becomes invisible. There is a sleep so you can
        // observe on screen.
        findByTag("myCheckbox")
            .assertIsVisible()
            .doClick()

        findByTag("myCheckbox")
            .assertIsChecked()
    }
}

@Composable
fun FillGrid2(horizontalGridCount: Int, @Children children: () -> Unit) {
    <MeasureBox> constraints ->
        val measurables = collect(children)
        val verticalGrid = (measurables.size + horizontalGridCount - 1) / horizontalGridCount
        val cellW = constraints.maxWidth / horizontalGridCount
        val cellH = constraints.maxHeight / verticalGrid
        val c = Constraints.tightConstraints(cellW, cellH)
        layout(constraints.maxWidth, constraints.maxHeight) {
            measurables
                .map { it.measure(c) }
                .forEachIndexed { index, placeable ->
                    val x = index % horizontalGridCount * cellW
                    val y = cellH * (index / horizontalGridCount)
                    placeable.place(x, y)
                }
        }
    </MeasureBox>
}
