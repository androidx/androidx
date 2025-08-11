/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.runUIKitInstrumentedTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WindowInsetsPaddingTest {
    @Test
    fun composableDoesNotRecomposeOnWindowInsetsImeChange() = runUIKitInstrumentedTest {
        var compositionCount = 0

        setContent {
            val focusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()){
                Spacer(modifier = Modifier.weight(1f))
                TextField(
                    "",
                    {},
                    Modifier.focusRequester(focusRequester)
                )
                compositionCount++
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        assertEquals(1, compositionCount)
    }
}