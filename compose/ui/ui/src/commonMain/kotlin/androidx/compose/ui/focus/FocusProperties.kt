/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Properties that are applied to [focusTarget] that is the first child of the
 * [FocusPropertiesModifierNode] that sets these properties.
 *
 * @see [focusProperties]
 */
interface FocusProperties {
    /**
     * When set to false, indicates that the [focusTarget] that this is applied to can no longer
     * take focus. If the [focusTarget] is currently focused, setting this property to false will
     * end up clearing focus.
     */
    var canFocus: Boolean

    /**
     * A custom item to be used when the user requests the focus to move to the "next" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var next: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user requests the focus to move to the "previous" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var previous: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user moves focus "up".
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var up: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user moves focus "down".
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var down: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user requests a focus moves to the "left" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var left: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user requests a focus moves to the "right" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var right: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user requests a focus moves to the "left" in LTR mode and
     * "right" in RTL mode.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var start: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user requests a focus moves to the "right" in LTR mode and
     * "left" in RTL mode.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var end: FocusRequester
        get() = FocusRequester.Default
        set(_) {}

    /**
     * A custom item to be used when the user requests focus to move focus in
     * ([FocusDirection.Enter]). An automatic [Enter][FocusDirection.Enter]" can be triggered when
     * we move focus to a focus group that is not itself focusable. In this case, users can use the
     * the focus direction that triggered the move in to determine the next item to be focused on.
     *
     * When you set the [enter] property, provide a lambda that takes the FocusDirection that
     * triggered the enter as an input, and provides a [FocusRequester] as an output. You can return
     * a custom destination by providing a [FocusRequester] attached to that destination, a
     * [Cancel][FocusRequester.Cancel] to cancel the focus enter or
     * [Default][FocusRequester.Default] to use the default focus enter behavior.
     *
     * @sample androidx.compose.ui.samples.CustomFocusEnterSample
     */
    @ExperimentalComposeUiApi
    @set:Deprecated("Use onEnter instead", ReplaceWith("onEnter"))
    var enter: (FocusDirection) -> FocusRequester
        get() = { FocusRequester.Default }
        set(value) {
            onEnter = value.toUsingEnterExitScope()
        }

    /**
     * A custom item to be used when the user requests focus to move focus in
     * ([FocusDirection.Enter]). An automatic [Enter][FocusDirection.Enter]" can be triggered when
     * we move focus to a focus group that is not itself focusable. In this case, users can use the
     * the focus direction that triggered the move in to determine the next item to be focused on.
     *
     * When you set the [onEnter] property, provide a lambda with the [FocusEnterExitScope] scope,
     * having the [FocusEnterExitScope.requestedFocusDirection] that triggered the enter as an
     * input. If redirection is required, use [FocusRequester.requestFocus] and if the focus change
     * should be canceled, use [FocusEnterExitScope.cancelFocusChange].
     *
     * @sample androidx.compose.ui.samples.CustomFocusEnterSample
     */
    var onEnter: FocusEnterExitScope.() -> Unit
        get() = {}
        set(_) {}

    /**
     * A custom item to be used when the user requests focus to move out ([FocusDirection.Exit]). An
     * automatic [Exit][FocusDirection.Exit] can be triggered when we move focus outside the edge of
     * a parent. In this case, users can use the focus direction that triggered the move out to
     * determine the next focus destination.
     *
     * When you set the [exit] property, provide a lambda that takes the FocusDirection that
     * triggered the exit as an input, and provides a [FocusRequester] as an output. You can return
     * a custom destination by providing a [FocusRequester] attached to that destination, a
     * [Cancel][FocusRequester.Cancel] to cancel the focus exit or [Default][FocusRequester.Default]
     * to use the default focus exit behavior.
     *
     * @sample androidx.compose.ui.samples.CustomFocusExitSample
     */
    @ExperimentalComposeUiApi
    @set:Deprecated("Use onExit instead", ReplaceWith("onExit"))
    var exit: (FocusDirection) -> FocusRequester
        get() = { FocusRequester.Default }
        set(value) {
            onExit = value.toUsingEnterExitScope()
        }

    /**
     * A custom item to be used when the user requests focus to move out ([FocusDirection.Exit]). An
     * automatic [Exit][FocusDirection.Exit] can be triggered when we move focus outside the edge of
     * a parent. In this case, users can use the focus direction that triggered the move out to
     * determine the next focus destination.
     *
     * When you set the [onExit] property, provide a lambda with the [FocusEnterExitScope] scope,
     * having the [FocusEnterExitScope.requestedFocusDirection] that triggered the exit as an input.
     * If redirection is required, use [FocusRequester.requestFocus] and if the focus change should
     * be canceled, use [FocusEnterExitScope.cancelFocusChange].
     *
     * @sample androidx.compose.ui.samples.CustomFocusExitSample
     */
    var onExit: FocusEnterExitScope.() -> Unit
        get() = {}
        set(_) {}
}

/**
 * A utility to use when upgrading from [FocusProperties.enter] and [FocusProperties.exit] to
 * [FocusProperties.onEnter] and [FocusProperties.onExit].
 */
private fun ((FocusDirection) -> FocusRequester).toUsingEnterExitScope():
    FocusEnterExitScope.() -> Unit = {
    val focusRequester = invoke(requestedFocusDirection)
    if (focusRequester === FocusRequester.Cancel) {
        cancelFocusChange()
    } else if (focusRequester !== FocusRequester.Default) {
        focusRequester.requestFocus()
    }
}

/**
 * Receiver scope for [FocusProperties.onEnter] and [FocusProperties.onExit]. Developers can change
 * focus with [FocusRequester.requestFocus] to change the focus or [cancelFocusChange] to stop the
 * focus from changing.
 */
sealed interface FocusEnterExitScope {
    /**
     * The direction used to get into (with [FocusProperties.onEnter]) or leave (with
     * [FocusProperties.onExit]) focus.
     */
    val requestedFocusDirection: FocusDirection

    /** Stop focus from changing. */
    fun cancelFocusChange()

    @ExperimentalComposeUiApi
    @Deprecated("Use cancelFocusChange instead", replaceWith = ReplaceWith("cancelFocusChange"))
    fun cancelFocus() = cancelFocusChange()
}

internal class CancelIndicatingFocusBoundaryScope(
    override val requestedFocusDirection: FocusDirection
) : FocusEnterExitScope {
    var isCanceled = false
        private set

    override fun cancelFocusChange() {
        isCanceled = true
    }
}

internal class FocusPropertiesImpl : FocusProperties {
    override var canFocus: Boolean = true
    override var next: FocusRequester = FocusRequester.Default
    override var previous: FocusRequester = FocusRequester.Default
    override var up: FocusRequester = FocusRequester.Default
    override var down: FocusRequester = FocusRequester.Default
    override var left: FocusRequester = FocusRequester.Default
    override var right: FocusRequester = FocusRequester.Default
    override var start: FocusRequester = FocusRequester.Default
    override var end: FocusRequester = FocusRequester.Default
    override var onEnter: FocusEnterExitScope.() -> Unit = {}
    override var onExit: FocusEnterExitScope.() -> Unit = {}
}

/**
 * This modifier allows you to specify properties that are accessible to [focusTarget]s further down
 * the modifier chain or on child layout nodes.
 *
 * @sample androidx.compose.ui.samples.FocusPropertiesSample
 */
fun Modifier.focusProperties(scope: FocusProperties.() -> Unit): Modifier =
    this then FocusPropertiesElement(scope)

private data class FocusPropertiesElement(val scope: FocusPropertiesScope) :
    ModifierNodeElement<FocusPropertiesNode>() {
    override fun create() = FocusPropertiesNode(scope)

    override fun update(node: FocusPropertiesNode) {
        node.focusPropertiesScope = scope
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "focusProperties"
        properties["scope"] = scope
    }
}

private class FocusPropertiesNode(var focusPropertiesScope: FocusPropertiesScope) :
    FocusPropertiesModifierNode, Modifier.Node() {

    override fun applyFocusProperties(focusProperties: FocusProperties) {
        focusPropertiesScope.apply(focusProperties)
    }
}
