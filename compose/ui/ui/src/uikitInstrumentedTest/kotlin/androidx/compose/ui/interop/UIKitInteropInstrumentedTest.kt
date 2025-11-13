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

package androidx.compose.ui.interop

import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.runUIKitInstrumentedTest

internal fun runUIKitInstrumentedTestWithInterop(
    testBlock: UIKitInstrumentedTest.(Boolean) -> Unit
) {
    runUIKitInstrumentedTest {
        println("Debug: Interop view placed as overlay: false")
        testBlock(false)
    }
    runUIKitInstrumentedTest {
        println("Debug: Interop view placed as overlay: true")
        testBlock(true)
    }
}

internal fun runUIKitInstrumentedTestWithInterop(
    ignoreIf: Boolean,
    ignoreNotes: String,
    testBlock: UIKitInstrumentedTest.(Boolean) -> Unit
) {
    runUIKitInstrumentedTest(ignoreIf = ignoreIf, ignoreNotes = ignoreNotes) {
        println("Debug: Interop view placed as overlay: false")
        testBlock(false)
    }
    runUIKitInstrumentedTest(ignoreIf = ignoreIf, ignoreNotes = ignoreNotes) {
        println("Debug: Interop view placed as overlay: true")
        testBlock(true)
    }
}
