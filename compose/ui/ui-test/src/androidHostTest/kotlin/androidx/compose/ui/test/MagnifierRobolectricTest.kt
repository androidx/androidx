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

package androidx.compose.ui.test

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Robolectric can't create the surface that the framework Magnifier draws into, so showing one
 * throws or dismissing one crashes, depending on the API level.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MagnifierRobolectricTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun doubleClickTextField_doesNotCrash() {
        composeRule.setContent { BasicTextField(rememberTextFieldState("Hello world")) }

        // Selects a word, which shows the magnifier until the pointer goes up.
        composeRule.onNode(hasSetTextAction()).performTouchInput { doubleClick() }
        composeRule.waitForIdle()
    }
}
