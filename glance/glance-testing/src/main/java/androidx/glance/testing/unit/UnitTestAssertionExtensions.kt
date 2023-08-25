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

package androidx.glance.testing.unit

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.semantics.SemanticsProperties
import androidx.glance.testing.GlanceNodeAssertion

// This file contains common convenience assertion shorthands for unit tests that delegate calls to
// "assert(matchers)".

internal typealias UnitTestAssertion = GlanceNodeAssertion<MappedNode, GlanceMappedNode>

/**
 * Asserts that a given node is annotated by the given test tag.
 *
 * @param testTag value to match against the free form string specified in the `testTag` semantics
 *                modifier on the Glance composable nodes.
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertHasTestTag(testTag: String): UnitTestAssertion {
    return assert(hasTestTag(testTag))
}

/**
 * Asserts that a given node matches content description with the provided [value]
 *
 * @param value value to match as one of the items in the list of content descriptions.
 * @param substring whether to use substring matching.
 * @param ignoreCase whether case should be ignored.
 *
 * @see SemanticsProperties.ContentDescription
 *
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
@JvmOverloads
fun UnitTestAssertion.assertHasContentDescription(
    value: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false
): UnitTestAssertion {
    return assert(hasContentDescription(value, substring, ignoreCase))
}

/**
 * Asserts that a given node doesn't have a clickable modifier or `onClick` set.
 *
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertHasNoClickAction(): UnitTestAssertion {
    return assert(hasNoClickAction())
}

/**
 * Asserts that a given node has a clickable modifier or `onClick` set.
 *
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun UnitTestAssertion.assertHasClickAction(): UnitTestAssertion {
    return assert(hasClickAction())
}

/**
 * Asserts that a given node has a clickable set with action that starts an activity.
 *
 * This can be passed in "onNode" and "onNodeAll" functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param activityClass class of the activity that is expected to have been passed in the
 *                      `actionStartActivity` method call
 * @param parameters the parameters associated with the action that are expected to have been passed
 *                      in the `actionStartActivity` method call
 * @param activityOptions Additional options built from an [android.app.ActivityOptions] that are
 *                        expected to have been passed in the `actionStartActivity` method call
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
@JvmOverloads
fun <T : Activity> UnitTestAssertion.assertHasStartActivityClickAction(
    activityClass: Class<T>,
    parameters: ActionParameters = actionParametersOf(),
    activityOptions: Bundle? = null
): UnitTestAssertion {
    return assert(hasStartActivityClickAction(activityClass, parameters, activityOptions))
}

/**
 * Asserts that a given node has a clickable set with action that starts an activity.
 *
 * @param componentName component of the activity that is expected to have been passed in the
 *                      `actionStartActivity` method call
 * @param parameters the parameters associated with the action that are expected to have been passed
 *                      in the `actionStartActivity` method call
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
@JvmOverloads
fun UnitTestAssertion.assertHasStartActivityClickAction(
    componentName: ComponentName,
    parameters: ActionParameters = actionParametersOf()
): UnitTestAssertion {
    return assert(hasStartActivityClickAction(componentName, parameters))
}
