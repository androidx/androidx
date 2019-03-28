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

import androidx.test.filters.MediumTest
import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.baseui.selection.ToggleableState.Checked
import androidx.ui.baseui.selection.ToggleableState.Unchecked
import androidx.ui.core.CraneWrapper
import androidx.ui.core.TestTag
import androidx.ui.material.Checkbox
import androidx.ui.material.MaterialTheme
import androidx.ui.test.android.AndroidUiTestRunner
import androidx.ui.test.assertSemanticsIsEqualTo
import androidx.ui.test.copyWith
import androidx.ui.test.createFullSemantics
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import com.google.r4a.Model
import com.google.r4a.composer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Model
class CheckboxState(var value: ToggleableState = Checked) {
    fun toggle() {
        value = if (value == Checked) Unchecked else Checked
    }
}

@MediumTest
@RunWith(JUnit4::class)
class CheckboxUiTest : AndroidUiTestRunner() {
    // TODO(b/126881459): this should be the default semantic for checkbox
    private val defaultCheckboxSemantics = createFullSemantics(
        enabled = false,
        checked = false
    )

    @Test
    fun checkBoxTest_defaultSemantics() {
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <TestTag tag="myCheckbox">
                        <Checkbox value=Unchecked />
                    </TestTag>
                </MaterialTheme>
            </CraneWrapper>
        }

        findByTag("myCheckbox")
            .assertSemanticsIsEqualTo(
                defaultCheckboxSemantics
            )
    }

    @Test
    fun checkBoxTest_toggle() {
        val state = CheckboxState(value = Unchecked)

        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <TestTag tag="myCheckbox">
                        <Checkbox
                            value=state.value
                            onToggle={
                                state.toggle()
                            } />
                    </TestTag>
                </MaterialTheme>
            </CraneWrapper>
        }

        findByTag("myCheckbox")
            .doClick()
            .assertSemanticsIsEqualTo(
                defaultCheckboxSemantics.copyWith { checked = true }
            )
    }

    @Test
    fun checkBoxTest_toggle_twice() {
        val state = CheckboxState(value = Unchecked)

        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <TestTag tag="myCheckbox">
                        <Checkbox
                            value=state.value
                            onToggle={
                                state.toggle()
                            } />
                    </TestTag>
                </MaterialTheme>
            </CraneWrapper>
        }

        findByTag("myCheckbox")
            .doClick()
            .doClick()
            .assertSemanticsIsEqualTo(
                defaultCheckboxSemantics
            )
    }
}