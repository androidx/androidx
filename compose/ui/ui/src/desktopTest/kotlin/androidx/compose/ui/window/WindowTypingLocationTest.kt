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

package androidx.compose.ui.window

import androidx.compose.ui.focusedInputMethodRequests
import androidx.compose.ui.sendCharTypedEvents
import java.awt.Rectangle
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

@RunWith(Theories::class)
class WindowTypingLocationTest: BaseWindowTextFieldTest() {
    @Theory
    internal fun `input methods text location going right when typing`(
        textFieldKind: TextFieldKind<*>,
    ) = runTextFieldTest(
        textFieldKind = textFieldKind,
        name = "input methods text location going right when typing"
    ) {
        val location0 = window.focusedInputMethodRequests()!!.getTextLocation(null)

        window.sendCharTypedEvents('a')
        awaitIdle()
        val location1 = window.focusedInputMethodRequests()!!.getTextLocation(null)

        window.sendCharTypedEvents('a')
        awaitIdle()
        val location2 = window.focusedInputMethodRequests()!!.getTextLocation(null)

        fun assertMovedRight(before: Rectangle, after: Rectangle) {
            assert(after.x > before.x) {
                "InputMethods X text location after typing (${after.x} should be greater than" +
                    " before (${before.x})), but isn't"
            }
            assert(after.y == before.y) {
                "InputMethods Y text location after typing (${after.y} should be the same as" +
                    " before (${before.y})), but isn't"
            }
        }

        assertMovedRight(location0, location1)
        assertMovedRight(location1, location2)
    }

    @Theory
    internal fun `input methods text location is inside window`(
        textFieldKind: TextFieldKind<*>,
    ) = runTextFieldTest(
        textFieldKind = textFieldKind,
        name = "input methods text location inside window",
    ) {
        val windowLocation = window.contentPane.locationOnScreen
        val windowSize = window.contentPane.size

        val location0 = window.focusedInputMethodRequests()!!.getTextLocation(null)

        window.sendCharTypedEvents('a')
        awaitIdle()
        val location1 = window.focusedInputMethodRequests()!!.getTextLocation(null)

        window.sendCharTypedEvents('a')
        awaitIdle()
        val location2 = window.focusedInputMethodRequests()!!.getTextLocation(null)

        assert(location0.x in windowLocation.x..windowLocation.x + windowSize.width)
        assert(location1.x in windowLocation.x..windowLocation.x + windowSize.width)
        assert(location2.x in windowLocation.x..windowLocation.x + windowSize.width)
        assert(location0.y in windowLocation.y..windowLocation.y + windowSize.height)
        assert(location1.y in windowLocation.y..windowLocation.y + windowSize.height)
        assert(location2.y in windowLocation.y..windowLocation.y + windowSize.height)
    }
}
