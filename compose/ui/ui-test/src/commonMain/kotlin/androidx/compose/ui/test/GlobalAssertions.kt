/*
 * Copyright 2023 The Android Open Source Project
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
@file:JvmName("GlobalAssertions")

package androidx.compose.ui.test

import kotlin.jvm.JvmName

/**
 * Adds a named assertion to the collection of assertions to be executed before test actions.
 *
 * This API is intended to be invoked by assertion frameworks to register assertions that must hold
 * on the entire application whenever it's fully loaded and ready to interact with. They will be
 * invoked upon common actions such as `performClick`, and they always verify every element on the
 * screen, not just the element the action is performed on.
 *
 * This is particularly useful to automatically catch accessibility problems such as contrast ratio,
 * minimum touch-target size, etc.
 *
 * @param name An identifier for the assertion. It can subsequently be used to deactivate the
 *   assertion with [removeGlobalAssertion].
 * @param assertion A function to be executed.
 */
@ExperimentalTestApi
@Deprecated(
    message =
        "This API has been removed and its intended usage to run accessibility checks can now be done via `ComposeUiTest.enableAccessibilityChecks()` and `ComposeTestRule.enableAccessibilityChecks()`",
    level = DeprecationLevel.ERROR,
)
@Suppress("UNUSED_PARAMETER")
fun addGlobalAssertion(name: String, assertion: (SemanticsNodeInteraction) -> Unit) {}

/**
 * Removes a named assertion from the collection of assertions to be executed before test actions.
 *
 * @param name An identifier that was previously used in a call to [addGlobalAssertion].
 */
@ExperimentalTestApi
@Deprecated(
    message =
        "This API has been removed and its intended usage to run accessibility checks can now be done via `ComposeUiTest.disableAccessibilityChecks()` and `ComposeTestRule.disableAccessibilityChecks()`",
    level = DeprecationLevel.ERROR,
)
@Suppress("UNUSED_PARAMETER")
fun removeGlobalAssertion(name: String) {}

/**
 * Executes all of the assertions registered by [addGlobalAssertion]. This may be useful in a custom
 * test action.
 *
 * @return the [SemanticsNodeInteraction] that is the receiver of this method
 */
@ExperimentalTestApi
@Deprecated(
    message =
        "This API has been removed and its intended usage to run accessibility checks can now be done via `SemanticsNodeInteraction.tryPerformAccessibilityChecks()`",
    level = DeprecationLevel.ERROR,
    replaceWith =
        ReplaceWith(
            "tryPerformAccessibilityChecks()",
            "androidx.compose.ui.test.tryPerformAccessibilityChecks"
        )
)
fun SemanticsNodeInteraction.invokeGlobalAssertions(): SemanticsNodeInteraction {
    tryPerformAccessibilityChecks()
    return this
}

/**
 * Executes all of the assertions registered by [addGlobalAssertion], each of which will receive the
 * first node of this collection. This may be useful in a custom test action.
 *
 * @return the [SemanticsNodeInteractionCollection] that is the receiver of this method
 */
@ExperimentalTestApi
@Deprecated(
    message =
        "This API has been removed and its intended usage to run accessibility checks can now be done via `SemanticsNodeInteraction.tryPerformAccessibilityChecks()`",
    level = DeprecationLevel.ERROR,
    replaceWith =
        ReplaceWith(
            "onFirst().tryPerformAccessibilityChecks()",
            "androidx.compose.ui.test.tryPerformAccessibilityChecks, androidx.compose.ui.test.onFirst"
        )
)
fun SemanticsNodeInteractionCollection.invokeGlobalAssertions():
    SemanticsNodeInteractionCollection {
    onFirst().tryPerformAccessibilityChecks()
    return this
}
