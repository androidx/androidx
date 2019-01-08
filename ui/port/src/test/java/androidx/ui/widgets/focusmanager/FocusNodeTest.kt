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

package androidx.ui.widgets.focusmanager

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FocusNodeTest {

    @Test
    fun `FocusNode consumeKeyboardToken`() {
        val node = FocusNode()

        // By default, hasKeyboardToken is false and consumeKeyboardToken return false
        assertThat(node.consumeKeyboardToken()).isFalse()

        // If hasKeyboardToken is true, consumeKeyboardToken flips token and returns true.
        node.hasKeyboardToken = true
        assertThat(node.consumeKeyboardToken()).isTrue()
        assertThat(node.hasKeyboardToken).isFalse()
    }

    @Test
    fun `FocusNode no focus by default`() {
        val node = FocusNode()

        // By default, the node doesn't have focus
        assertThat(node.hasFocus).isFalse()
    }
}