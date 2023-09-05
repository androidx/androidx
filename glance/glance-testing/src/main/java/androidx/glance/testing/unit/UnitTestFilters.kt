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
import androidx.glance.EmittableWithText
import androidx.glance.action.ActionModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.StartActivityClassAction
import androidx.glance.action.StartActivityComponentAction
import androidx.glance.action.actionParametersOf
import androidx.glance.semantics.SemanticsModifier
import androidx.glance.semantics.SemanticsProperties
import androidx.glance.semantics.SemanticsPropertyKey
import androidx.glance.testing.GlanceNode
import androidx.glance.testing.GlanceNodeAssertionsProvider
import androidx.glance.testing.GlanceNodeMatcher

// This file contains common filters that can be passed in "onNode", "onAllNodes" or
// "assert(matcher)". Surface specific filters can be found in UnitTestFilters.kt in surface
// specific lib project.

/**
 * Returns a matcher that matches if a node is annotated by the given test tag.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param testTag value to match against the free form string specified in the `testTag` semantics
 *                modifier on the Glance composable nodes.
 */
fun hasTestTag(testTag: String): GlanceNodeMatcher<MappedNode> =
    hasSemanticsPropertyValue(SemanticsProperties.TestTag, testTag)

private fun <T> hasSemanticsPropertyValue(
    key: SemanticsPropertyKey<T>,
    expectedValue: T
): GlanceNodeMatcher<MappedNode> {
    return GlanceNodeMatcher("${key.name} = '$expectedValue'") { node ->
        node.value.emittable.modifier.any {
            it is SemanticsModifier &&
                it.configuration.getOrElseNullable(key) { null } == expectedValue
        }
    }
}

/**
 * Returns whether the content description set directly on the node contains the provided [value].
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param value value that should be substring of the content description set directly on the node.
 * @param ignoreCase whether case should be ignored. Default is case sensitive.
 *
 * @see SemanticsProperties.ContentDescription
 */
@JvmOverloads
fun hasContentDescription(
    value: String,
    ignoreCase: Boolean = false
): GlanceNodeMatcher<MappedNode> =
    GlanceNodeMatcher(
        description =
            "${SemanticsProperties.ContentDescription.name} contains '$value'" +
                " (ignoreCase: '$ignoreCase') as substring"
    ) { node ->
        node.value.emittable.modifier.any {
            it is SemanticsModifier &&
                hasContentDescription(
                    semanticsModifier = it,
                    value = value,
                    substring = true,
                    ignoreCase = ignoreCase)
        }
    }

/**
 * Returns whether the content description set directly on the node is equal to the provided
 * [value].
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param value value that should match exactly with content description set directly on the node.
 * @param ignoreCase whether case should be ignored. Default is case sensitive.
 *
 * @see SemanticsProperties.ContentDescription
 */
@JvmOverloads
fun hasContentDescriptionEqualTo(
    value: String,
    ignoreCase: Boolean = false
): GlanceNodeMatcher<MappedNode> =
    GlanceNodeMatcher(
        description =
        "${SemanticsProperties.ContentDescription.name} == '$value' (ignoreCase: '$ignoreCase')"
    ) { node ->
        node.value.emittable.modifier.any {
            it is SemanticsModifier &&
                hasContentDescription(
                    semanticsModifier = it,
                    value = value,
                    substring = false,
                    ignoreCase = ignoreCase
                )
        }
    }

private fun hasContentDescription(
    semanticsModifier: SemanticsModifier,
    value: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false
): Boolean {
    val contentDescription =
        semanticsModifier.configuration.getOrNull(SemanticsProperties.ContentDescription)
            ?.joinToString()
            ?: return false
    return if (substring) {
        contentDescription.contains(value, ignoreCase)
    } else {
        contentDescription.equals(value, ignoreCase)
    }
}

/**
 * Returns a matcher that matches if text on node contains the provided text as its substring.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param text value that should be matched as a substring of the node's text.
 * @param ignoreCase whether to perform case insensitive matching. Defaults to case sensitive
 *                   matching.
 */
@JvmOverloads
fun hasText(
    text: String,
    ignoreCase: Boolean = false
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "contains text '$text' (ignoreCase: '$ignoreCase') as substring"
) { node ->
    val emittable = node.value.emittable
    emittable is EmittableWithText && emittable.text.contains(text, ignoreCase)
}

/**
 * Returns a matcher that matches if node is a text node and its text is equal to the provided text.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param text value that should exactly match the node's text.
 * @param ignoreCase whether to perform case insensitive matching. Defaults to case sensitive
 *                   matching.
 */
@JvmOverloads
fun hasTextEqualTo(
    text: String,
    ignoreCase: Boolean = false
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "text == '$text' (ignoreCase: '$ignoreCase')"
) { node ->
    val emittable = node.value.emittable
    emittable is EmittableWithText && emittable.text.equals(text, ignoreCase)
}

/**
 * Returns a matcher that matches if the given node has clickable modifier set.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 */
fun hasClickAction(): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "has click action"
) { node ->
    node.value.emittable.modifier.any {
        it is ActionModifier
    }
}

/**
 * Returns a matcher that matches if the given node doesn't have a clickable modifier or `onClick`
 * set.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 */
fun hasNoClickAction(): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "has no click action"
) { node ->
    node.value.emittable.modifier.all {
        it !is ActionModifier
    }
}

/**
 * Returns a matcher that matches if a given node has a clickable set with action that starts an
 * activity.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param activityClass class of the activity that is expected to have been passed in the
 *                      `actionStartActivity` method call
 * @param parameters the parameters associated with the action that are expected to have been passed
 *                      in the `actionStartActivity` method call
 * @param activityOptions Additional options built from an [android.app.ActivityOptions] that are
 *                        expected to have been passed in the `actionStartActivity` method call
 */
@JvmOverloads
fun <T : Activity> hasStartActivityClickAction(
    activityClass: Class<T>,
    parameters: ActionParameters = actionParametersOf(),
    activityOptions: Bundle? = null
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description =
    if (activityOptions != null) {
        "has start activity click action with activity: ${activityClass.name}, " +
            "parameters: $parameters and bundle: $activityOptions"
    } else {
        "has start activity click action with activity: ${activityClass.name} and " +
            "parameters: $parameters"
    }
) { node ->
    node.value.emittable.modifier.any {
        if (it is ActionModifier) {
            val action = it.action
            if (action is StartActivityClassAction) {
                var result = action.activityClass == activityClass &&
                    action.parameters == parameters
                if (activityOptions != null) {
                    result = result && activityOptions == action.activityOptions
                }
                return@any result
            }
        }
        false
    }
}

/**
 * Returns a matcher that matches if a given node has a descendant node somewhere in its
 * sub-hierarchy that the matches the provided matcher.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param matcher a matcher that needs to be satisfied for the descendant node to be matched
 */
fun hasAnyDescendant(matcher: GlanceNodeMatcher<MappedNode>): GlanceNodeMatcher<MappedNode> {

    fun checkIfSubtreeMatchesRecursive(
        matcher: GlanceNodeMatcher<MappedNode>,
        node: GlanceNode<MappedNode>
    ): Boolean {
        if (matcher.matchesAny(node.children())) {
            return true
        }

        return node.children().any { checkIfSubtreeMatchesRecursive(matcher, it) }
    }

    return GlanceNodeMatcher("hasAnyDescendantThat(${matcher.description})") {
        checkIfSubtreeMatchesRecursive(matcher, it)
    }
}

/**
 * Returns a matcher that matches if a given node has a clickable set with action that starts an
 * activity.
 *
 * This can be passed in [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] functions on assertion providers to filter out
 * matching node(s) or in assertions to validate that node(s) satisfy the condition.
 *
 * @param componentName component of the activity that is expected to have been passed in the
 *                      `actionStartActivity` method call
 * @param parameters the parameters associated with the action that are expected to have been passed
 *                      in the `actionStartActivity` method call
 */
@JvmOverloads
fun hasStartActivityClickAction(
    componentName: ComponentName,
    parameters: ActionParameters = actionParametersOf()
): GlanceNodeMatcher<MappedNode> = GlanceNodeMatcher(
    description = "has start activity click action with componentName: $componentName and " +
        "parameters: $parameters"
) { node ->
    node.value.emittable.modifier.any {
        if (it is ActionModifier) {
            val action = it.action
            if (action is StartActivityComponentAction) {
                return@any action.componentName == componentName &&
                    action.parameters == parameters
            }
        }
        false
    }
}
