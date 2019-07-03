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
package androidx.ui.core

import androidx.ui.core.semantics.SemanticsAction
import androidx.ui.text.style.TextDirection
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.unaryPlus

// TODO(ryanmentley): This is the wrong package, move it as a standalone CL

@Composable
@Suppress("PLUGIN_ERROR")
fun Semantics(
    /**
     * If 'container' is true, this component will introduce a new
     * node in the semantics tree. Otherwise, the semantics will be
     * merged with the semantics of any ancestors.
     *
     * Whether descendants of this component can add their semantic information
     * to the [SemanticsNode] introduced by this configuration is controlled by
     * [explicitChildNodes].
     */
    container: Boolean = false,
    /**
     * Whether descendants of this component are allowed to add semantic
     * information to the [SemanticsNode] annotated by this widget.
     *
     * When set to false descendants are allowed to annotate [SemanticNode]s of
     * their parent with the semantic information they want to contribute to the
     * semantic tree.
     * When set to true the only way for descendants to contribute semantic
     * information to the semantic tree is to introduce new explicit
     * [SemanticNode]s to the tree.
     *
     * This setting is often used in combination with [isSemanticBoundary] to
     * create semantic boundaries that are either writable or not for children.
     */
    explicitChildNodes: Boolean = false,
    /**
     * Whether the component represented by this configuration is currently enabled.
     *
     * A disabled object does not respond to user interactions. Only objects that
     * usually respond to user interactions, but which currently do not (like a
     * disabled button) should be marked as disabled.
     *
     * The corresponding getter on [SemanticsConfiguration] will return null if the component
     * doesn't support the concept of being enabled/disabled.
     */
    enabled: Boolean? = null,
    /**
     * If this node has Boolean state that can be controlled by the user, whether
     * that state is checked or unchecked, corresponding to true and false,
     * respectively.
     *
     * Do not set this field if the component doesn't have checked/unchecked state that can be
     * controlled by the user.
     *
     * The corresponding getter on [SemanticsConfiguration] returns null if the component does not
     * have checked/unchecked state.
     */
    checked: Boolean? = null,
    /** Whether the component represented by this configuration is selected (true) or not (false). */
    selected: Boolean? = null,
    /** Whether the component represented by this configuration is a button (true) or not (false). */
    button: Boolean? = null,
//    header: Boolean? = null,
//    textField: Boolean? = null,
//    focused: Boolean? = null,
    /**
     * Whether this component corresponds to UI that allows the user to
     * pick one of several mutually exclusive options.
     *
     * For example, a [Radio] button is in a mutually exclusive group because
     * only one radio button in that group can be marked as [isChecked].
     */
    inMutuallyExclusiveGroup: Boolean? = null,
//    obscured: Boolean? = null,
//    scopesRoute: Boolean? = null,
//    namesRoute: Boolean? = null,
    hidden: Boolean? = null,
    /**
     * A textual description of the component
     *
     * On iOS this is used for the `accessibilityLabel` property defined in the
     * `UIAccessibility` Protocol. On Android it is concatenated together with
     * [value] and [hint] in the following order: [value], [label], [hint].
     * The concatenated value is then used as the `Text` description.
     *
     * The reading direction is given by [textDirection].
     */
    label: String? = null,
    /**
     * A textual description for the current value of the owning component.
     *
     * On iOS this is used for the `accessibilityValue` property defined in the
     * `UIAccessibility` Protocol. On Android it is concatenated together with
     * [label] and [hint] in the following order: [value], [label], [hint].
     * The concatenated value is then used as the `Text` description.
     *
     * The reading direction is given by [textDirection].
     */
    value: String? = null,
//    hint: String? = null,
    /**
     * The reading direction for the text in [label], [value],  and [hint]
     */
    textDirection: TextDirection? = null,
    testTag: String? = null,
    actions: List<SemanticsAction<*>> = emptyList(),
    @Children children: @Composable() () -> Unit
) {
    val providedTestTag = +ambient(TestTagAmbient)
    <SemanticsComponentNode
        container
        explicitChildNodes
        enabled
        checked
        selected
        button
        header=null
        textField=null
        focused=null
        inMutuallyExclusiveGroup
        obscured=null
        scopesRoute=null
        namesRoute=null
        hidden
        label
        value
        hint=null
        textDirection
        testTag=(testTag ?: providedTestTag)
        actions>
        TestTag(tag=DefaultTestTag) {
            children()
        }
    </SemanticsComponentNode>
}
