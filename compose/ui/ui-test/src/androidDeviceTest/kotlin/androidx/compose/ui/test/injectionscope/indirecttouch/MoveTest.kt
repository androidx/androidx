/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.indirecttouch

import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.test.IndirectPointerInjectionScope
import androidx.compose.ui.test.injectionscope.indirecttouch.Common.performIndirectPointerInput
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.ClickableTestBox.defaultTag
import androidx.compose.ui.unit.IntSize
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests the error states of [IndirectPointerInjectionScope.move] that are not tested in
 * [MoveToTest] and [MoveByTest]
 */
@MediumTest
class MoveTest {
    companion object {
        private val downPosition1 = Offset(10f, 10f)
        private val inputDeviceSize = IntSize(3082, 616)
    }

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Before
    fun setUp() {
        rule.setContent { ClickableTestBox() }
        rule.onNodeWithTag(defaultTag).requestFocus()
    }

    @Test
    fun move_withoutDown() {
        expectError<IllegalStateException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                move()
            }
        }
    }

    @Test
    fun move_afterUp() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(downPosition1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            up()
        }
        expectError<IllegalStateException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                move()
            }
        }
    }

    @Test
    fun move_afterCancel() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(downPosition1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            cancel()
        }
        expectError<IllegalStateException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                move()
            }
        }
    }
}
