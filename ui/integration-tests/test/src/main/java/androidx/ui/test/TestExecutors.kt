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

package androidx.ui.test

/**
 * Runs the given test case and toggle its state using the given lambda. Asserts that recomposition
 * happens and has changes and also asserts that the no more compositions is needed.
 */
fun runComposeTestWithStateToggleAndAssertRecompositions(
    testCase: ComposeTestCase,
    toggleState: () -> Unit
) {
    testCase.runSetup()

    testCase.assertMeasureSizeIsPositive()

    testCase.recomposeSyncAssertNoChanges()

    // Change state
    toggleState.invoke()

    // Recompose our changes
    testCase.recomposeSyncAssertHadChanges()

    // No other compositions should be pending
    testCase.recomposeSyncAssertNoChanges()
}