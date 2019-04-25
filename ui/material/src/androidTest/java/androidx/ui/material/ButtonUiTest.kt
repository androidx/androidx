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

package androidx.ui.material

import androidx.test.filters.MediumTest
import androidx.ui.core.CraneWrapper
import androidx.ui.core.TestTag
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.test.android.AndroidUiTestRunner
import androidx.ui.test.assertSemanticsIsEqualTo
import androidx.ui.test.createFullSemantics
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import com.google.common.truth.Truth
import com.google.r4a.composer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class ButtonUiTest : AndroidUiTestRunner() {

    private val defaultButtonSemantics = createFullSemantics(
        enabled = true,
        button = true
    )

    @Test
    fun buttonTest_defaultSemantics() {
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Center>
                        <TestTag tag="myButton">
                            <Button onClick= {} text="myButton"/>
                        </TestTag>
                    </Center>
                </MaterialTheme>
            </CraneWrapper>
        }

        findByTag("myButton")
            .assertSemanticsIsEqualTo(defaultButtonSemantics)
    }

    @Test
    fun buttonTest_findByTextAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "myButton"

        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Center>
                        <Button onClick text/>
                    </Center>
                </MaterialTheme>
            </CraneWrapper>
        }

        // TODO(b/129400818): this actually finds the text, not the button as
        // merge semantics aren't implemented yet
        findByText(text)
            .doClick()

        Truth
            .assertThat(counter)
            .isEqualTo(1)
    }

    @Test
    fun buttonTest_ClickIsIndependentBetweenButtons() {
        var button1Counter = 0
        val button1OnClick: () -> Unit = { ++button1Counter }
        val button1Tag = "button1"

        var button2Counter = 0
        val button2OnClick: () -> Unit = { ++button2Counter }
        val button2Tag = "button2"

        val text = "myButton"

        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Column>
                        <TestTag tag=button1Tag>
                            <Button onClick=button1OnClick text/>
                        </TestTag>
                        <TestTag tag=button2Tag>
                            <Button onClick=button2OnClick text/>
                        </TestTag>
                    </Column>
                </MaterialTheme>
            </CraneWrapper>
        }

        findByTag(button1Tag)
            .doClick()

        Truth
            .assertThat(button1Counter)
            .isEqualTo(1)

        Truth
            .assertThat(button2Counter)
            .isEqualTo(0)

        findByTag(button2Tag)
            .doClick()

        Truth
            .assertThat(button1Counter)
            .isEqualTo(1)

        Truth
            .assertThat(button2Counter)
            .isEqualTo(1)
    }
}
