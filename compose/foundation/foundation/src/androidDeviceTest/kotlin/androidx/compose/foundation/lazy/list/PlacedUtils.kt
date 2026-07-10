/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.lazy.list

import androidx.compose.ui.test.SemanticsNodeInteraction

/**
 * Asserts that the current semantics node is placed.
 *
 * Throws [AssertionError] if the node is not placed.
 */
internal fun SemanticsNodeInteraction.assertIsPlaced(): SemanticsNodeInteraction {
    val errorMessageOnFail = "Assert failed: The component is not placed!"
    if (!fetchSemanticsNode(errorMessageOnFail).layoutInfo.isPlaced) {
        throw AssertionError(errorMessageOnFail)
    }
    return this
}

/**
 * Asserts that the current semantics node is not placed.
 *
 * Throws [AssertionError] if the node is placed.
 */
internal fun SemanticsNodeInteraction.assertIsNotPlaced() {
    // TODO(b/187188981): We don't have a non-throwing API to check whether an item exists.
    //  So until this bug is fixed, we are going to catch the assertion error and then check
    //  whether the node is placed or not.
    try {
        // If the node does not exist, it implies that it is also not placed.
        assertDoesNotExist()
    } catch (e: AssertionError) {
        // If the node exists, we need to assert that it is not placed.
        val errorMessageOnFail = "Assert failed: The component is placed!"
        if (fetchSemanticsNode().layoutInfo.isPlaced) {
            throw AssertionError(errorMessageOnFail)
        }
    }
}
