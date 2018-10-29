package androidx.ui.semantics

import androidx.ui.VoidCallback
import androidx.ui.engine.text.TextDirection
import androidx.ui.services.text_editing.TextSelection
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Describes the semantic information associated with the owning
 * [RenderObject].
 *
 * The information provided in the configuration is used to to generate the
 * semantics tree.
 */
class SemanticsConfiguration {
    // SEMANTIC BOUNDARY BEHAVIOR

    /**
     * Whether the [RenderObject] owner of this configuration wants to own its
     * own [SemanticsNode].
     *
     * When set to true semantic information associated with the [RenderObject]
     * owner of this configuration or any of its descendants will not leak into
     * parents. The [SemanticsNode] generated out of this configuration will
     * act as a boundary.
     *
     * Whether descendants of the owning [RenderObject] can add their semantic
     * information to the [SemanticsNode] introduced by this configuration
     * is controlled by [explicitChildNodes].
     *
     * This has to be true if [isMergingDescendantsIntoOneNode] is also true.
     */
    var isSemanticBoundary: Boolean = false
        set(value) {
            assert(!isMergingSemanticsOfDescendants || value)
            field = value
        }

    /**
     * Whether the configuration forces all children of the owning [RenderObject]
     * that want to contribute semantic information to the semantics tree to do
     * so in the form of explicit [SemanticsNode]s.
     *
     * When set to false children of the owning [RenderObject] are allowed to
     * annotate [SemanticNode]s of their parent with the semantic information
     * they want to contribute to the semantic tree.
     * When set to true the only way for children of the owning [RenderObject]
     * to contribute semantic information to the semantic tree is to introduce
     * new explicit [SemanticNode]s to the tree.
     *
     * This setting is often used in combination with [isSemanticBoundary] to
     * create semantic boundaries that are either writable or not for children.
     */
    var explicitChildNodes = false

    /**
     * Whether the owning [RenderObject] makes other [RenderObject]s previously
     * painted within the same semantic boundary unreachable for accessibility
     * purposes.
     *
     * If set to true, the semantic information for all siblings and cousins of
     * this node, that are earlier in a depth-first pre-order traversal, are
     * dropped from the semantics tree up until a semantic boundary (as defined
     * by [isSemanticBoundary]) is reached.
     *
     * If [isSemanticBoundary] and [isBlockingSemanticsOfPreviouslyPaintedNodes]
     * is set on the same node, all previously painted siblings and cousins up
     * until the next ancestor that is a semantic boundary are dropped.
     *
     * Paint order as established by [visitChildrenForSemantics] is used to
     * determine if a node is previous to this one.
     */
    var isBlockingSemanticsOfPreviouslyPaintedNodes = false

    // SEMANTIC ANNOTATIONS
    // These will end up on [SemanticNode]s generated from
    // [SemanticsConfiguration]s.

    /**
     * Whether this configuration is empty.
     *
     * An empty configuration doesn't contain any semantic information that it
     * wants to contribute to the semantics tree.
     */
    var hasBeenAnnotated: Boolean = false

    /**
     * The actions (with associated action handlers) that this configuration
     * would like to contribute to the semantics tree.
     *
     * See also:
     *
     * * [addAction] to add an action.
     */
    internal val _actions: MutableMap<SemanticsAction, _SemanticsActionHandler> = mutableMapOf()

    internal var _actionsAsBits: Int = 0

    /**
     * Adds an `action` to the semantics tree.
     *
     * The provided `handler` is called to respond to the user triggered
     * `action`.
     */
    fun _addAction(action: SemanticsAction, handler: _SemanticsActionHandler) {
        _actions[action] = handler
        _actionsAsBits = _actionsAsBits or action.index
        hasBeenAnnotated = true
    }

    /**
     * Adds an `action` to the semantics tree, whose `handler` does not expect
     * any arguments.
     *
     * The provided `handler` is called to respond to the user triggered
     * `action`.
     */
    // TODO(Migration/ryanmentley): Should be private, but is internal to avoid a synthetic accessor
    internal fun _addArgumentlessAction(action: SemanticsAction, handler: VoidCallback) {
        _addAction(action) { args: Any? ->
            assert(args == null)
            handler()
        }
    }

    /**
     * A property that holds a [VoidCallback] and calls [_addArgumentlessAction] when
     * it is set
     */
    private class ArgumentlessActionProperty(
        val action: SemanticsAction
    ) : ReadWriteProperty<SemanticsConfiguration, VoidCallback?> {
        var handler: VoidCallback? = null

        override fun getValue(
            thisRef: SemanticsConfiguration,
            property: KProperty<*>
        ): VoidCallback? {
            return handler
        }

        override fun setValue(
            thisRef: SemanticsConfiguration,
            property: KProperty<*>,
            value: VoidCallback?
        ) {
            thisRef._addArgumentlessAction(action, value!!)
            handler = value
        }
    }

    /**
     * A property that can only be set to non-null values (though the default values may be null)
     */
    // TODO(Migration/ryanmentley): The nullability of this class is really strange. Can we improve?
    private class NonNullAnnotationProperty<T>(initialValue: T) :
        ReadWriteProperty<SemanticsConfiguration, T> {
        private var value = initialValue

        override fun getValue(thisRef: SemanticsConfiguration, property: KProperty<*>): T {
            return value
        }

        override fun setValue(thisRef: SemanticsConfiguration, property: KProperty<*>, value: T) {
            assert(value != null)
            this.value = value
            thisRef.hasBeenAnnotated = true
        }
    }

    /**
     * The handler for [SemanticsAction.tap].
     *
     * This is the semantic equivalent of a user briefly tapping the screen with
     * the finger without moving it. For example, a button should implement this
     * action.
     *
     * VoiceOver users on iOS and TalkBack users on Android can trigger this
     * action by double-tapping the screen while an element is focused.
     *
     * On Android prior to Android Oreo a double-tap on the screen while an
     * element with an [onTap] handler is focused will not call the registered
     * handler. Instead, Android will simulate a pointer down and up event at the
     * center of the focused element. Those pointer events will get dispatched
     * just like a regular tap with TalkBack disabled would: The events will get
     * processed by any [GestureDetector] listening for gestures in the center of
     * the focused element. Therefore, to ensure that [onTap] handlers work
     * properly on Android versions prior to Oreo, a [GestureDetector] with an
     * onTap handler should always be wrapping an element that defines a
     * semantic [onTap] handler. By default a [GestureDetector] will register its
     * own semantic [onTap] handler that follows this principle.
     */
    var onTap: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.tap)

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
    var onLongPress: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.longPress)

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
    var onScrollLeft: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.scrollLeft)

    /**
     * The handler for [SemanticsAction.dismiss].
     *
     * This is a request to dismiss the currently focused node.
     *
     * TalkBack users on Android can trigger this action in the local context
     * menu, and VoiceOver users on iOS can trigger this action with a standard
     * gesture or menu option.
     */
    var onDismiss: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.dismiss)

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
     * [onScrollRight] share the same gesture. Therefore, only one of them should
     * be provided.
     */
    var onScrollRight: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.scrollRight)

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
     * [onScrollLeft] share the same gesture. Therefore, only one of them should
     * be provided.
     */
    var onScrollUp: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.scrollUp)

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
     * [onScrollRight] share the same gesture. Therefore, only one of them should
     * be provided.
     */
    var onScrollDown: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.scrollDown)

    /**
     * The handler for [SemanticsAction.increase].
     *
     * This is a request to increase the value represented by the widget. For
     * example, this action might be recognized by a slider control.
     *
     * If a [value] is set, [increasedValue] must also be provided and
     * [onIncrease] must ensure that [value] will be set to [increasedValue].
     *
     * VoiceOver users on iOS can trigger this action by swiping up with one
     * finger. TalkBack users on Android can trigger this action by pressing the
     * volume up button.
     */
    var onIncrease: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.increase)

    /**
     * The handler for [SemanticsAction.decrease].
     *
     * This is a request to decrease the value represented by the widget. For
     * example, this action might be recognized by a slider control.
     *
     * If a [value] is set, [decreasedValue] must also be provided and
     * [onDecrease] must ensure that [value] will be set to [decreasedValue].
     *
     * VoiceOver users on iOS can trigger this action by swiping down with one
     * finger. TalkBack users on Android can trigger this action by pressing the
     * volume down button.
     */
    var onDecrease: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.decrease)

    /**
     * The handler for [SemanticsAction.copy].
     *
     * This is a request to copy the current selection to the clipboard.
     *
     * TalkBack users on Android can trigger this action from the local context
     * menu of a text field, for example.
     */
    var onCopy: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.copy)

    /**
     * The handler for [SemanticsAction.cut].
     *
     * This is a request to cut the current selection and place it in the
     * clipboard.
     *
     * TalkBack users on Android can trigger this action from the local context
     * menu of a text field, for example.
     */
    var onCut: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.cut)

    /**
     * The handler for [SemanticsAction.paste].
     *
     * This is a request to paste the current content of the clipboard.
     *
     * TalkBack users on Android can trigger this action from the local context
     * menu of a text field, for example.
     */
    var onPaste: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.paste)

    /**
     * The handler for [SemanticsAction.showOnScreen].
     *
     * A request to fully show the semantics node on screen. For example, this
     * action might be send to a node in a scrollable list that is partially off
     * screen to bring it on screen.
     *
     * For elements in a scrollable list the framework provides a default
     * implementation for this action and it is not advised to provide a
     * custom one via this setter.
     */
    var onShowOnScreen: VoidCallback? by ArgumentlessActionProperty(SemanticsAction.showOnScreen)

    /**
     * The handler for [SemanticsAction.onMoveCursorForwardByCharacter].
     *
     * This handler is invoked when the user wants to move the cursor in a
     * text field forward by one character.
     *
     * TalkBack users can trigger this by pressing the volume up key while the
     * input focus is in a text field.
     */
    var onMoveCursorForwardByCharacter: MoveCursorHandler? = null
        set(value) {
            assert(value != null)
            _addAction(SemanticsAction.moveCursorForwardByCharacter) { args: Any? ->
                val extentSelection = args as Boolean
                value!!(extentSelection)
            }
            field = value
        }

    /**
     * The handler for [SemanticsAction.onMoveCursorBackwardByCharacter].
     *
     * This handler is invoked when the user wants to move the cursor in a
     * text field backward by one character.
     *
     * TalkBack users can trigger this by pressing the volume down key while the
     * input focus is in a text field.
     */
    var onMoveCursorBackwardByCharacter: MoveCursorHandler? = null
        set(value) {
            assert(value != null)
            _addAction(SemanticsAction.moveCursorBackwardByCharacter) { args: Any? ->
                val extentSelection = args as Boolean
                value!!(extentSelection)
            }
            field = value
        }

    /**
     * The handler for [SemanticsAction.setSelection].
     *
     * This handler is invoked when the user either wants to change the currently
     * selected text in a text field or change the position of the cursor.
     *
     * TalkBack users can trigger this handler by selecting "Move cursor to
     * beginning/end" or "Select all" from the local context menu.
     */
    var onSetSelection: SetSelectionHandler? = null
        set(value) {
            assert(value != null)
            _addAction(SemanticsAction.setSelection) { args: Any? ->
                val selection = args as Map<String, Int>
                assert(selection["base"] != null && selection["extent"] != null)
                value!!(
                    TextSelection(
                        baseOffset = selection["base"]!!,
                        extentOffset = selection["extent"]!!
                    )
                )
            }
            field = value
        }

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
    var onDidGainAccessibilityFocus
            by ArgumentlessActionProperty(SemanticsAction.didGainAccessibilityFocus)

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
    var onDidLoseAccessibilityFocus
            by ArgumentlessActionProperty(SemanticsAction.didLoseAccessibilityFocus)

    /**
     * Returns the action handler registered for [action] or null if none was
     * registered.
     *
     * See also:
     *
     *  * [addAction] to add an action.
     */
    fun getActionHandler(action: SemanticsAction): _SemanticsActionHandler? = _actions[action]

    /**
     * Determines the position of this node among its siblings in the traversal
     * sort order.
     *
     * This is used to describe the order in which the semantic node should be
     * traversed by the accessibility services on the platform (e.g. VoiceOver
     * on iOS and TalkBack on Android).
     *
     * Whether this sort key has an effect on the [SemanticsNode] sort order is
     * subject to how this configuration is used. For example, the [absorb]
     * method may decide to not use this key when it combines multiple
     * [SemanticsConfiguration] objects.
     */
    var sortKey: SemanticsSortKey? by NonNullAnnotationProperty(null)

    /**
     * Whether the semantic information provided by the owning [RenderObject] and
     * all of its descendants should be treated as one logical entity.
     *
     * If set to true, the descendants of the owning [RenderObject]'s
     * [SemanticsNode] will merge their semantic information into the
     * [SemanticsNode] representing the owning [RenderObject].
     *
     * Setting this to true requires that [isSemanticBoundary] is also true.
     */
    var isMergingSemanticsOfDescendants: Boolean = false
        set(value) {
            assert(isSemanticBoundary)
            field = value
            hasBeenAnnotated = true
        }

    /**
     * The handlers for each supported [CustomSemanticsAction].
     *
     * Whenever a custom accessibility action is added to a node, the action
     * [SemanticAction.customAction] is automatically added. A handler is
     * created which uses the passed argument to lookup the custom action
     * handler from this map and invoke it, if present.
     */
    var customSemanticsActions: MutableMap<CustomSemanticsAction, VoidCallback> = mutableMapOf()
        set(value) {
            hasBeenAnnotated = true
            _actionsAsBits = _actionsAsBits or SemanticsAction.customAction.index
            field = value
            _actions[SemanticsAction.customAction] = ::_onCustomSemanticsAction
        }

    //
    private fun _onCustomSemanticsAction(args: Any?) {
        // TODO(Migration/ryanmentley): This casting is not remotely type-safe - can we do better?
        val action: CustomSemanticsAction? = CustomSemanticsAction.getAction(args as Int) ?: return
        val callback: VoidCallback = customSemanticsActions[action] ?: return
        callback()
    }

    /**
     * A textual description of the owning [RenderObrject].
     *
     * On iOS this is used for the `accessibilityLabel` property defined in the
     * `UIAccessibility` Protocol. On Android it is concatenated together with
     * [value] and [hint] in the following order: [value], [label], [hint].
     * The concatenated value is then used as the `Text` description.
     *
     * The reading direction is given by [textDirection].
     */
    var label by NonNullAnnotationProperty("")

    /**
     * A textual description for the current value of the owning [RenderObject].
     *
     * On iOS this is used for the `accessibilityValue` property defined in the
     * `UIAccessibility` Protocol. On Android it is concatenated together with
     * [label] and [hint] in the following order: [value], [label], [hint].
     * The concatenated value is then used as the `Text` description.
     *
     * The reading direction is given by [textDirection].
     *
     * See also:
     *
     *  * [decreasedValue], describes what [value] will be after performing
     *    [SemanticsAction.decrease]
     *  * [increasedValue], describes what [value] will be after performing
     *    [SemanticsAction.increase]
     */
    var value by NonNullAnnotationProperty("")

    /**
     * The value that [value] will have after performing a
     * [SemanticsAction.decrease] action.
     *
     * This must be set if a handler for [SemanticsAction.decrease] is provided
     * and [value] is set.
     *
     * The reading direction is given by [textDirection].
     */
    var decreasedValue by NonNullAnnotationProperty("")

    /**
     * The value that [value] will have after performing a
     * [SemanticsAction.increase] action.
     *
     * This must be set if a handler for [SemanticsAction.increase] is provided
     * and [value] is set.
     *
     * The reading direction is given by [textDirection].
     */
    var increasedValue by NonNullAnnotationProperty("")

    /**
     * A brief description of the result of performing an action on this node.
     *
     * On iOS this is used for the `accessibilityHint` property defined in the
     * `UIAccessibility` Protocol. On Android it is concatenated together with
     * [label] and [value] in the following order: [value], [label], [hint].
     * The concatenated value is then used as the `Text` description.
     *
     * The reading direction is given by [textDirection].
     */
    var hint by NonNullAnnotationProperty("")

    /**
     * Provides hint values which override the default hints on supported
     * platforms.
     */
    var hintOverrides: SemanticsHintOverrides? = null
        set(value) {
            // TODO(Migration/ryanmentley): This could be a NonNullAnnotationProperty except that
            // this *ignores* attempts to set it to null rather than asserting, which seems like an
            // extremely unintuitive way to write a setter.  We should change this - setters should
            // not ignore values passed into them.
            if (value == null) {
                return
            }

            field = value
            hasBeenAnnotated = true
        }

    /**
     * Whether the semantics node is the root of a subtree for which values
     * should be announced.
     *
     * See also:
     *  * [SemanticsFlag.scopesRoute], for a full description of route scoping.
     */
    var scopesRoute: Boolean by SimpleFlagProperty(SemanticsFlag.scopesRoute)

    /**
     * Whether the semantics node contains the label of a route.
     *
     * See also:
     *  * [SemanticsFlag.namesRoute], for a full description of route naming.
     */
    var namesRoute: Boolean by SimpleFlagProperty(SemanticsFlag.namesRoute)

    /** Whether the semantics node represents an image. */
    var isImage: Boolean by SimpleFlagProperty(SemanticsFlag.isImage)

    /**
     * Whether the semantics node is a live region.
     *
     * On Android, when a live region semantics node is first created TalkBack
     * will make a polite announcement of the current label. This announcement
     * occurs even if the node is not focused. Subsequent polite announcements
     * can be made by sending a [UpdateLiveRegionEvent] semantics event. The
     * announcement will only be made if the node's label has changed since the
     * last update.
     *
     * An example of a live region is the [Snackbar] widget. When it appears
     * on the screen it may be difficult to focus to read the label. A live
     * region causes an initial polite announcement to be generated
     * automatically.
     *
     * See also:
     *
     *   * [SemanticsFlag.isLiveRegion], the semantics flag that this setting controls.
     */
    var liveRegion: Boolean by SimpleFlagProperty(SemanticsFlag.isLiveRegion)

    /**
     * The reading direction for the text in [label], [value], [hint],
     * [increasedValue], and [decreasedValue].
     */
    var textDirection: TextDirection? = null
        set(value) {
            field = value
            hasBeenAnnotated = true
        }

    /** Whether the owning [RenderObject] is selected (true) or not (false). */
    var isSelected: Boolean by SimpleFlagProperty(SemanticsFlag.isSelected)

    /**
     * Whether the owning [RenderObject] is currently enabled.
     *
     * A disabled object does not respond to user interactions. Only objects that
     * usually respond to user interactions, but which currently do not (like a
     * disabled button) should be marked as disabled.
     *
     * The setter should not be called for objects (like static text) that never
     * respond to user interactions.
     *
     * The getter will return null if the owning [RenderObject] doesn't support
     * the concept of being enabled/disabled.
     */
    var isEnabled: Boolean?
        get() {
            return if (_hasFlag(SemanticsFlag.hasEnabledState)) {
                _hasFlag(SemanticsFlag.isEnabled)
            } else {
                null
            }
        }
        set(value) {
            _setFlag(SemanticsFlag.hasEnabledState, true)
            _setFlag(SemanticsFlag.isEnabled, value!!)
        }

    /**
     * If this node has Boolean state that can be controlled by the user, whether
     * that state is checked or unchecked, corresponding to true and false,
     * respectively.
     *
     * Do not call the setter for this field if the owning [RenderObject] doesn't
     * have checked/unchecked state that can be controlled by the user.
     *
     * The getter returns null if the owning [RenderObject] does not have
     * checked/unchecked state.
     */
    var isChecked: Boolean?
        get() {
            return if (_hasFlag(SemanticsFlag.hasCheckedState)) {
                _hasFlag(SemanticsFlag.isChecked)
            } else {
                null
            }
        }
        set(value) {
            _setFlag(SemanticsFlag.hasCheckedState, true)
            _setFlag(SemanticsFlag.isChecked, value!!)
        }

    /**
     * If this node has Boolean state that can be controlled by the user, whether
     * that state is on or off, corresponding to true and false, respectively.
     *
     * Do not call the setter for this field if the owning [RenderObject] doesn't
     * have on/off state that can be controlled by the user.
     *
     * The getter returns null if the owning [RenderObject] does not have
     * on/off state.
     */
    var isToggled: Boolean?
        get() {
            return if (_hasFlag(SemanticsFlag.hasToggledState)) {
                _hasFlag(SemanticsFlag.isToggled)
            } else {
                null
            }
        }
        set(value) {
            _setFlag(SemanticsFlag.hasToggledState, true)
            _setFlag(SemanticsFlag.isToggled, value!!)
        }

    /**
     * Whether the owning RenderObject corresponds to UI that allows the user to
     * pick one of several mutually exclusive options.
     *
     * For example, a [Radio] button is in a mutually exclusive group because
     * only one radio button in that group can be marked as [isChecked].
     */
    var isInMutuallyExclusiveGroup: Boolean
            by SimpleFlagProperty(SemanticsFlag.isInMutuallyExclusiveGroup)

    /** Whether the owning [RenderObject] currently holds the user's focus. */
    var isFocused: Boolean by SimpleFlagProperty(SemanticsFlag.isFocused)

    /** Whether the owning [RenderObject] is a button (true) or not (false). */
    var isButton: Boolean by SimpleFlagProperty(SemanticsFlag.isButton)

    /** Whether the owning [RenderObject] is a header (true) or not (false). */
    var isHeader: Boolean by SimpleFlagProperty(SemanticsFlag.isHeader)

    /**
     * Whether the owning [RenderObject] is considered hidden.
     *
     * Hidden elements are currently not visible on screen. They may be covered
     * by other elements or positioned outside of the visible area of a viewport.
     *
     * Hidden elements cannot gain accessibility focus though regular touch. The
     * only way they can be focused is by moving the focus to them via linear
     * navigation.
     *
     * Platforms are free to completely ignore hidden elements and new platforms
     * are encouraged to do so.
     *
     * Instead of marking an element as hidden it should usually be excluded from
     * the semantics tree altogether. Hidden elements are only included in the
     * semantics tree to work around platform limitations and they are mainly
     * used to implement accessibility scrolling on iOS.
     */
    var isHidden: Boolean by SimpleFlagProperty(SemanticsFlag.isHidden)

    /** Whether the owning [RenderObject] is a text field. */
    var isTextField: Boolean by SimpleFlagProperty(SemanticsFlag.isTextField)

    /**
     * Whether the [value] should be obscured.
     *
     * This option is usually set in combination with [textField] to indicate
     * that the text field contains a password (or other sensitive information).
     * Doing so instructs screen readers to not read out the [value].
     */
    var isObscured: Boolean by SimpleFlagProperty(SemanticsFlag.isObscured)

    /**
     * Whether the platform can scroll the semantics node when the user attempts
     * to move focus to an offscreen child.
     *
     * For example, a [ListView] widget has implicit scrolling so that users can
     * easily move to the next visible set of children. A [TabBar] widget does
     * not have implicit scrolling, so that users can navigate into the tab
     * body when reaching the end of the tab bar.
     */
    var hasImplicitScrolling: Boolean by SimpleFlagProperty(SemanticsFlag.hasImplicitScrolling)

    /**
     * The currently selected text (or the position of the cursor) within [value]
     * if this node represents a text field.
     */
    var textSelection: TextSelection? by NonNullAnnotationProperty(null)

    /**
     * Indicates the current scrolling position in logical pixels if the node is
     * scrollable.
     *
     * The properties [scrollExtentMin] and [scrollExtentMax] indicate the valid
     * in-range values for this property. The value for [scrollPosition] may
     * (temporarily) be outside that range, e.g. during an overscroll.
     *
     * See also:
     *
     *  * [ScrollPosition.pixels], from where this value is usually taken.
     */
    var scrollPosition: Double? by NonNullAnnotationProperty(null)

    /**
     * Indicates the maximum in-range value for [scrollPosition] if the node is
     * scrollable.
     *
     * This value may be infinity if the scroll is unbound.
     *
     * See also:
     *
     *  * [ScrollPosition.maxScrollExtent], from where this value is usually taken.
     */
    var scrollExtentMax: Double? by NonNullAnnotationProperty(null)

    /**
     * Indicates the minimum in-range value for [scrollPosition] if the node is
     * scrollable.
     *
     * This value may be infinity if the scroll is unbound.
     *
     * See also:
     *
     *  * [ScrollPosition.minScrollExtent], from where this value is usually taken.
     */
    var scrollExtentMin: Double? by NonNullAnnotationProperty(null)

    // TAGS

    /**
     * The set of tags that this configuration wants to add to all child
     * [SemanticsNode]s.
     *
     * See also:
     *
     *  * [addTagForChildren] to add a tag and for more information about their
     *    usage.
     */
    var tagsForChildren: MutableSet<SemanticsTag>? = null

    /**
     * Specifies a [SemanticsTag] that this configuration wants to apply to all
     * child [SemanticsNode]s.
     *
     * The tag is added to all [SemanticsNode] that pass through the
     * [RenderObject] owning this configuration while looking to be attached to a
     * parent [SemanticsNode].
     *
     * Tags are used to communicate to a parent [SemanticsNode] that a child
     * [SemanticsNode] was passed through a particular [RenderObject]. The parent
     * can use this information to determine the shape of the semantics tree.
     *
     * See also:
     *
     *  * [RenderSemanticsGestureHandler.excludeFromScrolling] for an example of
     *    how tags are used.
     */
    fun addTagForChildren(tag: SemanticsTag) {
        var tags = tagsForChildren
        if (tags == null) {
            tags = mutableSetOf()
            tagsForChildren = tags
        }

        tags.add(tag)
    }

    // INTERNAL FLAG MANAGEMENT

    internal var _flags = 0

    // TODO(Migration/ryanmentley): Should be private, but is internal to avoid a synthetic accessor
    internal fun _setFlag(flag: SemanticsFlag, value: Boolean) {
        _flags = if (value) {
            _flags or flag.index
        } else {
            _flags and flag.index.inv()
        }
        hasBeenAnnotated = true
    }

    // TODO(Migration/ryanmentley): Should be private, but is internal to avoid a synthetic accessor
    internal fun _hasFlag(flag: SemanticsFlag): Boolean = (_flags and flag.index) != 0

    /**
     * A property that provides an abstraction over [_setFlag] and [_hasFlag]
     */
    private class SimpleFlagProperty(
        val flag: SemanticsFlag
    ) : ReadWriteProperty<SemanticsConfiguration, Boolean> {
        override fun getValue(
            thisRef: SemanticsConfiguration,
            property: KProperty<*>
        ): Boolean {
            return thisRef._hasFlag(flag)
        }

        override fun setValue(
            thisRef: SemanticsConfiguration,
            property: KProperty<*>,
            value: Boolean
        ) {
            thisRef._setFlag(flag, value)
        }
    }

    // CONFIGURATION COMBINATION LOGIC

    /**
     * Whether this configuration is compatible with the provided `other`
     * configuration.
     *
     * Two configurations are said to be compatible if they can be added to the
     * same [SemanticsNode] without losing any semantics information.
     */
    fun isCompatibleWith(other: SemanticsConfiguration): Boolean {
        if (!other.hasBeenAnnotated || !hasBeenAnnotated)
            return true
        if (_actionsAsBits and other._actionsAsBits != 0)
            return false
        if ((_flags and other._flags) != 0)
            return false
        if (value.isNotEmpty() && other.value.isNotEmpty())
            return false
        return true
    }

    /**
     * Absorb the semantic information from `other` into this configuration.
     *
     * This adds the semantic information of both configurations and saves the
     * result in this configuration.
     *
     * Only configurations that have [explicitChildNodes] set to false can
     * absorb other configurations and it is recommended to only absorb compatible
     * configurations as determined by [isCompatibleWith].
     */
    fun absorb(other: SemanticsConfiguration) {
        assert(!explicitChildNodes)

        if (!other.hasBeenAnnotated)
            return

        _actions.putAll(other._actions)
        customSemanticsActions.putAll(other.customSemanticsActions)
        _actionsAsBits = _actionsAsBits or other._actionsAsBits
        _flags = _flags or other._flags
        textSelection = textSelection ?: other.textSelection
        scrollPosition = scrollPosition ?: other.scrollPosition
        scrollExtentMax = scrollExtentMax ?: other.scrollExtentMax
        scrollExtentMin = scrollExtentMin ?: other.scrollExtentMin
        hintOverrides = hintOverrides ?: other.hintOverrides

        textDirection = textDirection ?: other.textDirection
        sortKey = sortKey ?: other.sortKey
        label = _concatStrings(
            thisString = label,
            thisTextDirection = textDirection!!,
            otherString = other.label,
            otherTextDirection = other.textDirection!!
        )
        if (decreasedValue.isNullOrEmpty()) {
            decreasedValue = other.decreasedValue
        }
        if (value.isNullOrEmpty()) {
            value = other.value
        }
        if (increasedValue.isNullOrEmpty()) {
            increasedValue = other.increasedValue
        }

        hint = _concatStrings(
            thisString = hint,
            thisTextDirection = textDirection!!,
            otherString = other.hint,
            otherTextDirection = other.textDirection!!
        )

        hasBeenAnnotated = hasBeenAnnotated or other.hasBeenAnnotated
    }

    /** Returns an exact copy of this configuration. */
    fun copy(): SemanticsConfiguration {
        val copy = SemanticsConfiguration()
        copy.isSemanticBoundary = isSemanticBoundary
        copy.explicitChildNodes = explicitChildNodes
        copy.isBlockingSemanticsOfPreviouslyPaintedNodes =
                isBlockingSemanticsOfPreviouslyPaintedNodes
        copy.hasBeenAnnotated = hasBeenAnnotated
        copy.isMergingSemanticsOfDescendants = isMergingSemanticsOfDescendants
        copy.textDirection = textDirection
        copy.sortKey = sortKey
        copy.label = label
        copy.increasedValue = increasedValue
        copy.value = value
        copy.decreasedValue = decreasedValue
        copy.hint = hint
        copy.hintOverrides = hintOverrides
        copy._flags = _flags
        copy.tagsForChildren = tagsForChildren
        copy.textSelection = textSelection
        copy.scrollPosition = scrollPosition
        copy.scrollExtentMax = scrollExtentMax
        copy.scrollExtentMin = scrollExtentMin
        copy._actionsAsBits = _actionsAsBits
        copy._actions.putAll(_actions)
        copy.customSemanticsActions.putAll(customSemanticsActions)
        return copy
    }
}