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

package androidx.ui.rendering.proxybox

import androidx.ui.VoidCallback
import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.obj.RenderObjectVisitor
import androidx.ui.semantics.MoveCursorHandler
import androidx.ui.semantics.SemanticsConfiguration
import androidx.ui.semantics.SemanticsSortKey
import androidx.ui.semantics.SetSelectionHandler
import androidx.ui.services.text_editing.TextSelection
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private class InvalidatingProperty<T>(private var value: T) :
    ReadWriteProperty<RenderObject, T> {
    override fun getValue(thisRef: RenderObject, property: KProperty<*>): T {
        return value
    }

    override fun setValue(
        thisRef: RenderObject,
        property: KProperty<*>,
        value: T
    ) {
        if (this.value == value) {
            return
        }
        this.value = value
        thisRef.markNeedsSemanticsUpdate()
    }
}

private class InvalidatingCallbackProperty<T>(private var value: T) :
    ReadWriteProperty<RenderObject, T> {
    override fun getValue(thisRef: RenderObject, property: KProperty<*>): T {
        return value
    }

    override fun setValue(
        thisRef: RenderObject,
        property: KProperty<*>,
        value: T
    ) {
        if (this.value == value) {
            return
        }
        val hadValue = this.value != null
        this.value = value
        if ((value != null) != hadValue) {
            thisRef.markNeedsSemanticsUpdate()
        }
    }
}

/**
 * Add annotations to the [SemanticsNode] for this subtree.
 *
 * If the [label] is not null, the [textDirection] must also not be null.
 */
class RenderSemanticsAnnotations(
    child: RenderBox? = null,
    container: Boolean = false,
    explicitChildNodes: Boolean = false,
    enabled: Boolean? = null,
    checked: Boolean? = null,
    selected: Boolean? = null,
    button: Boolean? = null,
    header: Boolean? = null,
    textField: Boolean? = null,
    focused: Boolean? = null,
    inMutuallyExclusiveGroup: Boolean? = null,
    obscured: Boolean? = null,
    scopesRoute: Boolean? = null,
    namesRoute: Boolean? = null,
    hidden: Boolean? = null,
    label: String? = null,
    value: String? = null,
    increasedValue: String? = null,
    decreasedValue: String? = null,
    hint: String? = null,
    textDirection: TextDirection? = null,
    sortKey: SemanticsSortKey? = null,
    onTap: VoidCallback? = null,
    onLongPress: VoidCallback? = null,
    onScrollLeft: VoidCallback? = null,
    onScrollRight: VoidCallback? = null,
    onScrollUp: VoidCallback? = null,
    onScrollDown: VoidCallback? = null,
    onIncrease: VoidCallback? = null,
    onDecrease: VoidCallback? = null,
    onCopy: VoidCallback? = null,
    onCut: VoidCallback? = null,
    onPaste: VoidCallback? = null,
    onMoveCursorForwardByCharacter: MoveCursorHandler? = null,
    onMoveCursorBackwardByCharacter: MoveCursorHandler? = null,
    onSetSelection: SetSelectionHandler? = null,
    onDidGainAccessibilityFocus: VoidCallback? = null,
    onDidLoseAccessibilityFocus: VoidCallback? = null
) : RenderProxyBox(child) {

    init {
        assert(container != null)
    }

    /**
     * If 'container' is true, this [RenderObject] will introduce a new
     * node in the semantics tree. Otherwise, the semantics will be
     * merged with the semantics of any ancestors.
     *
     * Whether descendants of this [RenderObject] can add their semantic information
     * to the [SemanticsNode] introduced by this configuration is controlled by
     * [explicitChildNodes].
     */
    var container: Boolean by InvalidatingProperty(container)

    /**
     * Whether descendants of this [RenderObject] are allowed to add semantic
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
    var explicitChildNodes: Boolean by InvalidatingProperty(explicitChildNodes)

    /**
     * If non-null, sets the [SemanticsNode.hasCheckedState] semantic to true and
     * the [SemanticsNode.isChecked] semantic to the given value.
     */
    var checked: Boolean? by InvalidatingProperty(checked)

    /**
     * If non-null, sets the [SemanticsNode.hasEnabledState] semantic to true and
     * the [SemanticsNode.isEnabled] semantic to the given value.
     */
    var enabled: Boolean? by InvalidatingProperty(enabled)

    /**
     * If non-null, sets the [SemanticsNode.isSelected] semantic to the given
     * value.
     */
    var selected: Boolean? by InvalidatingProperty(selected)

    /** If non-null, sets the [SemanticsNode.isButton] semantic to the given value. */
    var button: Boolean? by InvalidatingProperty(button)

    /** If non-null, sets the [SemanticsNode.isHeader] semantic to the given value. */
    var header: Boolean? by InvalidatingProperty(header)

    /** If non-null, sets the [SemanticsNode.isTextField] semantic to the given value. */
    var textField: Boolean? by InvalidatingProperty(textField)

    /** If non-null, sets the [SemanticsNode.isFocused] semantic to the given value. */
    var focused: Boolean? by InvalidatingProperty(focused)

    /**
     * If non-null, sets the [SemanticsNode.isInMutuallyExclusiveGroup] semantic
     * to the given value.
     */
    var inMutuallyExclusiveGroup: Boolean? by InvalidatingProperty(inMutuallyExclusiveGroup)

    /**
     * If non-null, sets the [SemanticsNode.isObscured] semantic to the given
     * value.
     */
    var obscured: Boolean? by InvalidatingProperty(obscured)

    /** If non-null, sets the [SemanticsNode.scopesRoute] semantic to the give value. */
    var scopesRoute: Boolean? by InvalidatingProperty(scopesRoute)

    /** If non-null, sets the [SemanticsNode.namesRoute] semantic to the give value. */
    var namesRoute: Boolean? by InvalidatingProperty(namesRoute)

    /**
     * If non-null, sets the [SemanticsNode.isHidden] semantic to the given
     * value.
     */
    var hidden: Boolean? by InvalidatingProperty(hidden)

    /**
     * If non-null, sets the [SemanticsNode.label] semantic to the given value.
     *
     * The reading direction is given by [textDirection].
     */
    var label: String? by InvalidatingProperty(label)

    /**
     * If non-null, sets the [SemanticsNode.value] semantic to the given value.
     *
     * The reading direction is given by [textDirection].
     */
    var value: String? by InvalidatingProperty(value)

    /**
     * If non-null, sets the [SemanticsNode.increasedValue] semantic to the given
     * value.
     *
     * The reading direction is given by [textDirection].
     */
    var increasedValue: String? by InvalidatingProperty(increasedValue)

    /**
     * If non-null, sets the [SemanticsNode.decreasedValue] semantic to the given
     * value.
     *
     * The reading direction is given by [textDirection].
     */
    var decreasedValue: String? by InvalidatingProperty(decreasedValue)

    /**
     * If non-null, sets the [SemanticsNode.hint] semantic to the given value.
     *
     * The reading direction is given by [textDirection].
     */
    var hint: String? by InvalidatingProperty(hint)

    /**
     * If non-null, sets the [SemanticsNode.textDirection] semantic to the given value.
     *
     * This must not be null if [label], [hint], [value], [increasedValue], or
     * [decreasedValue] are not null.
     */
    var textDirection: TextDirection? by InvalidatingProperty(textDirection)

    /**
     * Sets the [SemanticsNode.sortKey] to the given value.
     *
     * This defines how this node is sorted among the sibling semantics nodes
     * to determine the order in which they are traversed by the accessibility
     * services on the platform (e.g. VoiceOver on iOS and TalkBack on Android).
     */
    var sortKey: SemanticsSortKey? by InvalidatingProperty(sortKey)

    /**
     * The handler for [SemanticsAction.tap].
     *
     * This is the semantic equivalent of a user briefly tapping the screen with
     * the finger without moving it. For example, a button should implement this
     * action.
     *
     * VoiceOver users on iOS and TalkBack users on Android can trigger this
     * action by double-tapping the screen while an element is focused.
     */
    var onTap: VoidCallback? by InvalidatingCallbackProperty(onTap)

    /**
     * The handler for [SemanticsAction.longPress].
     *
     * This is the semantic equivalent of a user pressing and holding the screen
     * with the finger for a few seconds without moving it.
     *
     * VoiceOver users on iOS and TalkBack users on Android can trigger this
     * action by double-tapping the screen without lifting the finger after the
     * second tap.
     */
    var onLongPress: VoidCallback? by InvalidatingCallbackProperty(onLongPress)

    /**
     * The handler for [SemanticsAction.scrollLeft].
     *
     * This is the semantic equivalent of a user moving their finger across the
     * screen from right to left. It should be recognized by controls that are
     * horizontally scrollable.
     *
     * VoiceOver users on iOS can trigger this action by swiping left with three
     * fingers. TalkBack users on Android can trigger this action by swiping
     * right and then left in one motion path. On Android, [onScrollUp] and
     * [onScrollLeft] share the same gesture. Therefore, only on of them should
     * be provided.
     */
    var onScrollLeft: VoidCallback? by InvalidatingCallbackProperty(onScrollLeft)

    /**
     * The handler for [SemanticsAction.scrollRight].
     *
     * This is the semantic equivalent of a user moving their finger across the
     * screen from left to right. It should be recognized by controls that are
     * horizontally scrollable.
     *
     * VoiceOver users on iOS can trigger this action by swiping right with three
     * fingers. TalkBack users on Android can trigger this action by swiping
     * left and then right in one motion path. On Android, [onScrollDown] and
     * [onScrollRight] share the same gesture. Therefore, only on of them should
     * be provided.
     */
    var onScrollRight: VoidCallback? by InvalidatingCallbackProperty(onScrollRight)

    /**
     * The handler for [SemanticsAction.scrollUp].
     *
     * This is the semantic equivalent of a user moving their finger across the
     * screen from bottom to top. It should be recognized by controls that are
     * vertically scrollable.
     *
     * VoiceOver users on iOS can trigger this action by swiping up with three
     * fingers. TalkBack users on Android can trigger this action by swiping
     * right and then left in one motion path. On Android, [onScrollUp] and
     * [onScrollLeft] share the same gesture. Therefore, only on of them should
     * be provided.
     */
    var onScrollUp: VoidCallback? by InvalidatingCallbackProperty(onScrollUp)

    /**
     * The handler for [SemanticsAction.scrollDown].
     *
     * This is the semantic equivalent of a user moving their finger across the
     * screen from top to bottom. It should be recognized by controls that are
     * vertically scrollable.
     *
     * VoiceOver users on iOS can trigger this action by swiping down with three
     * fingers. TalkBack users on Android can trigger this action by swiping
     * left and then right in one motion path. On Android, [onScrollDown] and
     * [onScrollRight] share the same gesture. Therefore, only on of them should
     * be provided.
     */
    var onScrollDown: VoidCallback? by InvalidatingCallbackProperty(onScrollDown)

    /**
     * The handler for [SemanticsAction.increase].
     *
     * This is a request to increase the value represented by the widget. For
     * example, this action might be recognized by a slider control.
     *
     * VoiceOver users on iOS can trigger this action by swiping up with one
     * finger. TalkBack users on Android can trigger this action by pressing the
     * volume up button.
     */
    var onIncrease: VoidCallback? by InvalidatingCallbackProperty(onIncrease)

    /**
     * The handler for [SemanticsAction.decrease].
     *
     * This is a request to decrease the value represented by the widget. For
     * example, this action might be recognized by a slider control.
     *
     * VoiceOver users on iOS can trigger this action by swiping down with one
     * finger. TalkBack users on Android can trigger this action by pressing the
     * volume down button.
     */
    var onDecrease: VoidCallback? by InvalidatingCallbackProperty(onDecrease)

    /**
     * The handler for [SemanticsAction.copy].
     *
     * This is a request to copy the current selection to the clipboard.
     *
     * TalkBack users on Android can trigger this action from the local context
     * menu of a text field, for example.
     */
    var onCopy: VoidCallback? by InvalidatingCallbackProperty(onCopy)

    /**
     * The handler for [SemanticsAction.cut].
     *
     * This is a request to cut the current selection and place it in the
     * clipboard.
     *
     * TalkBack users on Android can trigger this action from the local context
     * menu of a text field, for example.
     */
    var onCut: VoidCallback? by InvalidatingCallbackProperty(onCut)

    /**
     * The handler for [SemanticsAction.paste].
     *
     * This is a request to paste the current content of the clipboard.
     *
     * TalkBack users on Android can trigger this action from the local context
     * menu of a text field, for example.
     */
    var onPaste: VoidCallback? by InvalidatingCallbackProperty(onPaste)

    /**
     * The handler for [SemanticsAction.onMoveCursorForwardByCharacter].
     *
     * This handler is invoked when the user wants to move the cursor in a
     * text field forward by one character.
     *
     * TalkBack users can trigger this by pressing the volume up key while the
     * input focus is in a text field.
     */
    var onMoveCursorForwardByCharacter: MoveCursorHandler? by InvalidatingCallbackProperty(
        onMoveCursorForwardByCharacter
    )

    /**
     * The handler for [SemanticsAction.onMoveCursorBackwardByCharacter].
     *
     * This handler is invoked when the user wants to move the cursor in a
     * text field backward by one character.
     *
     * TalkBack users can trigger this by pressing the volume down key while the
     * input focus is in a text field.
     */
    var onMoveCursorBackwardByCharacter: MoveCursorHandler? by InvalidatingCallbackProperty(
        onMoveCursorBackwardByCharacter
    )

    /**
     * The handler for [SemanticsAction.setSelection].
     *
     * This handler is invoked when the user either wants to change the currently
     * selected text in a text field or change the position of the cursor.
     *
     * TalkBack users can trigger this handler by selecting "Move cursor to
     * beginning/end" or "Select all" from the local context menu.
     */
    var onSetSelection: SetSelectionHandler? by InvalidatingCallbackProperty(onSetSelection)

    /**
     * The handler for [SemanticsAction.didGainAccessibilityFocus].
     *
     * This handler is invoked when the node annotated with this handler gains
     * the accessibility focus. The accessibility focus is the
     * green (on Android with TalkBack) or black (on iOS with VoiceOver)
     * rectangle shown on screen to indicate what element an accessibility
     * user is currently interacting with.
     *
     * The accessibility focus is different from the input focus. The input focus
     * is usually held by the element that currently responds to keyboard inputs.
     * Accessibility focus and input focus can be held by two different nodes!
     *
     * See also:
     *
     *  * [onDidLoseAccessibilityFocus], which is invoked when the accessibility
     *    focus is removed from the node
     *  * [FocusNode], [FocusScope], [FocusManager], which manage the input focus
     */
    var onDidGainAccessibilityFocus: VoidCallback? by InvalidatingCallbackProperty(
        onDidGainAccessibilityFocus
    )

    /**
     * The handler for [SemanticsAction.didLoseAccessibilityFocus].
     *
     * This handler is invoked when the node annotated with this handler
     * loses the accessibility focus. The accessibility focus is
     * the green (on Android with TalkBack) or black (on iOS with VoiceOver)
     * rectangle shown on screen to indicate what element an accessibility
     * user is currently interacting with.
     *
     * The accessibility focus is different from the input focus. The input focus
     * is usually held by the element that currently responds to keyboard inputs.
     * Accessibility focus and input focus can be held by two different nodes!
     *
     * See also:
     *
     *  * [onDidGainAccessibilityFocus], which is invoked when the node gains
     *    accessibility focus
     *  * [FocusNode], [FocusScope], [FocusManager], which manage the input focus
     */
    var onDidLoseAccessibilityFocus: VoidCallback? by InvalidatingCallbackProperty(
        onDidLoseAccessibilityFocus
    )

    override fun describeSemanticsConfiguration(config: SemanticsConfiguration) {
        super.describeSemanticsConfiguration(config)
        super.describeSemanticsConfiguration(config)
        config.isSemanticBoundary = container
        config.explicitChildNodes = explicitChildNodes
        assert((scopesRoute == true && explicitChildNodes) || scopesRoute != true) {
            "explicitChildNodes must be set to true if scopes route is true"
        }

        enabled?.let {
            config.isEnabled = it
        }
        checked?.let {
            config.isChecked = it
        }
        selected?.let {
            config.isSelected = it
        }
        button?.let {
            config.isButton = it
        }
        header?.let {
            config.isHeader = it
        }
        textField?.let {
            config.isTextField = it
        }
        focused?.let {
            config.isFocused = it
        }
        inMutuallyExclusiveGroup?.let {
            config.isInMutuallyExclusiveGroup = it
        }
        obscured?.let {
            config.isObscured = it
        }
        hidden?.let {
            config.isHidden = it
        }
        label?.let {
            config.label = it
        }
        value?.let {
            config.value = it
        }
        increasedValue?.let {
            config.increasedValue = it
        }
        decreasedValue?.let {
            config.decreasedValue = it
        }
        hint?.let {
            config.hint = it
        }
        scopesRoute?.let {
            config.scopesRoute = it
        }
        namesRoute?.let {
            config.namesRoute = it
        }
        textDirection?.let {
            config.textDirection = it
        }
        sortKey?.let {
            config.sortKey = it
        }
        // Registering _perform* as action handlers instead of the user provided
        // ones to ensure that changing a user provided handler from a non-null to
        // another non-null value doesn't require a semantics update.
        onTap?.let {
            config.onTap = ::_performTap
        }
        onLongPress?.let {
            config.onLongPress = ::_performLongPress
        }
        onScrollLeft?.let {
            config.onScrollLeft = ::_performScrollLeft
        }
        onScrollRight?.let {
            config.onScrollRight = ::_performScrollRight
        }
        onScrollUp?.let {
            config.onScrollUp = ::_performScrollUp
        }
        onScrollDown?.let {
            config.onScrollDown = ::_performScrollDown
        }
        onIncrease?.let {
            config.onIncrease = ::_performIncrease
        }
        onDecrease?.let {
            config.onDecrease = ::_performDecrease
        }
        onCopy?.let {
            config.onCopy = ::_performCopy
        }
        onCut?.let {
            config.onCut = ::_performCut
        }
        onPaste?.let {
            config.onPaste = ::_performPaste
        }
        onMoveCursorForwardByCharacter?.let {
            config.onMoveCursorForwardByCharacter = ::_performMoveCursorForwardByCharacter
        }
        onMoveCursorBackwardByCharacter?.let {
            config.onMoveCursorBackwardByCharacter = ::_performMoveCursorBackwardByCharacter
        }
        onSetSelection?.let {
            config.onSetSelection = ::_performSetSelection
        }
        onDidGainAccessibilityFocus?.let {
            config.onDidGainAccessibilityFocus = ::_performDidGainAccessibilityFocus
        }
        onDidLoseAccessibilityFocus?.let {
            config.onDidLoseAccessibilityFocus = ::_performDidLoseAccessibilityFocus
        }
    }

    fun _performTap() {
        onTap?.invoke()
    }

    fun _performLongPress() {
        onLongPress?.invoke()
    }

    fun _performScrollLeft() {
        onScrollLeft?.invoke()
    }

    fun _performScrollRight() {
        onScrollRight?.invoke()
    }

    fun _performScrollUp() {
        onScrollUp?.invoke()
    }

    fun _performScrollDown() {
        onScrollDown?.invoke()
    }

    fun _performIncrease() {
        onIncrease?.invoke()
    }

    fun _performDecrease() {
        onDecrease?.invoke()
    }

    fun _performCopy() {
        onCopy?.invoke()
    }

    fun _performCut() {
        onCut?.invoke()
    }

    fun _performPaste() {
        onPaste?.invoke()
    }

    fun _performMoveCursorForwardByCharacter(extendSelection: Boolean) {
        onMoveCursorForwardByCharacter?.invoke(extendSelection)
    }

    fun _performMoveCursorBackwardByCharacter(extendSelection: Boolean) {
        onMoveCursorBackwardByCharacter?.invoke(extendSelection)
    }

    fun _performSetSelection(selection: TextSelection) {
        onSetSelection?.invoke(selection)
    }

    fun _performDidGainAccessibilityFocus() {
        onDidGainAccessibilityFocus?.invoke()
    }

    fun _performDidLoseAccessibilityFocus() {
        onDidLoseAccessibilityFocus?.invoke()
    }
}

/**
 * Causes the semantics of all earlier (in paint order) render objects below the same semantic
 * boundary to be dropped.
 *
 * This is useful in a stack where an opaque mask should prevent interactions
 * with the render objects painted below the mask.
 */
class RenderBlockSemantics(
    child: RenderBox? = null,
    /**
     * See [RenderBlockSemantics.blocking]
     */
    blocking: Boolean = true
) : RenderProxyBox(child) {

    /**
     * Whether this render object is blocking semantics of previously painted
     * [RenderObject]s below a common semantics boundary from the semantic tree.
     */
    var blocking: Boolean by InvalidatingProperty(blocking)

    override fun describeSemanticsConfiguration(config: SemanticsConfiguration) {
        super.describeSemanticsConfiguration(config)
        config.isBlockingSemanticsOfPreviouslyPaintedNodes = blocking
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("blocking", blocking))
    }
}

/**
 * Causes the semantics of all descendants to be merged into this
 * node such that the entire subtree becomes a single leaf in the
 * semantics tree.
 *
 * Useful for combining the semantics of multiple render objects that
 * form part of a single conceptual widget, e.g. a checkbox, a label,
 * and the gesture detector that goes with them.
 */
class RenderMergeSemantics(child: RenderBox? = null) : RenderProxyBox(child) {

    override fun describeSemanticsConfiguration(config: SemanticsConfiguration) {
        super.describeSemanticsConfiguration(config)
        config.also {
            it.isSemanticBoundary = true
            it.isMergingSemanticsOfDescendants = true
        }
    }
}

/**
 * Excludes this subtree from the semantic tree.
 *
 * When [excluding] is true, this render object (and its subtree) is excluded
 * from the semantic tree.
 *
 * Useful e.g. for hiding text that is redundant with other text next
 * to it (e.g. text included only for the visual effect).
 */
class RenderExcludeSemantics(
    child: RenderBox? = null,
    /** See [RenderExcludeSemantics.excluding] */
    excluding: Boolean = true
) : RenderProxyBox(child) {

    /** Whether this render object is excluded from the semantic tree. */
    var excluding: Boolean by InvalidatingProperty(excluding)

    override fun visitChildren(visitor: RenderObjectVisitor) {
        if (excluding) {
            return
        }
        super.visitChildren(visitor)
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("excluding", excluding))
    }
}
