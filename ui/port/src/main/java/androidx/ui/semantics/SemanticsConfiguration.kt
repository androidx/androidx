package androidx.ui.semantics

// / Describes the semantic information associated with the owning
// / [RenderObject].
// /
// / The information provided in the configuration is used to to generate the
// / semantics tree.
class SemanticsConfiguration {
//
//  // SEMANTIC BOUNDARY BEHAVIOR
//
//  /// Whether the [RenderObject] owner of this configuration wants to own its
//  /// own [SemanticsNode].
//  ///
//  /// When set to true semantic information associated with the [RenderObject]
//  /// owner of this configuration or any of its descendants will not leak into
//  /// parents. The [SemanticsNode] generated out of this configuration will
//  /// act as a boundary.
//  ///
//  /// Whether descendants of the owning [RenderObject] can add their semantic
//  /// information to the [SemanticsNode] introduced by this configuration
//  /// is controlled by [explicitChildNodes].
//  ///
//  /// This has to be true if [isMergingDescendantsIntoOneNode] is also true.
    val isSemanticBoundary: Boolean get() {
        // TODO(Migration/Andrey): Mocking it for a hello world
        return false
    }
//  bool get isSemanticBoundary => _isSemanticBoundary;
//  bool _isSemanticBoundary = false;
//  set isSemanticBoundary(bool value) {
//    assert(!isMergingSemanticsOfDescendants || value);
//    _isSemanticBoundary = value;
//  }
//
//  /// Whether the configuration forces all children of the owning [RenderObject]
//  /// that want to contribute semantic information to the semantics tree to do
//  /// so in the form of explicit [SemanticsNode]s.
//  ///
//  /// When set to false children of the owning [RenderObject] are allowed to
//  /// annotate [SemanticNode]s of their parent with the semantic information
//  /// they want to contribute to the semantic tree.
//  /// When set to true the only way for children of the owning [RenderObject]
//  /// to contribute semantic information to the semantic tree is to introduce
//  /// new explicit [SemanticNode]s to the tree.
//  ///
//  /// This setting is often used in combination with [isSemanticBoundary] to
//  /// create semantic boundaries that are either writable or not for children.
//  bool explicitChildNodes = false;
//
//  /// Whether the owning [RenderObject] makes other [RenderObject]s previously
//  /// painted within the same semantic boundary unreachable for accessibility
//  /// purposes.
//  ///
//  /// If set to true, the semantic information for all siblings and cousins of
//  /// this node, that are earlier in a depth-first pre-order traversal, are
//  /// dropped from the semantics tree up until a semantic boundary (as defined
//  /// by [isSemanticBoundary]) is reached.
//  ///
//  /// If [isSemanticBoundary] and [isBlockingSemanticsOfPreviouslyPaintedNodes]
//  /// is set on the same node, all previously painted siblings and cousins up
//  /// until the next ancestor that is a semantic boundary are dropped.
//  ///
//  /// Paint order as established by [visitChildrenForSemantics] is used to
//  /// determine if a node is previous to this one.
    val isBlockingSemanticsOfPreviouslyPaintedNodes: Boolean get() {
        // TODO(Migration/Andrey): Mocking it for now
        return false
    }
//  bool isBlockingSemanticsOfPreviouslyPaintedNodes = false;
//
//  // SEMANTIC ANNOTATIONS
//  // These will end up on [SemanticNode]s generated from
//  // [SemanticsConfiguration]s.
//
//  /// Whether this configuration is empty.
//  ///
//  /// An empty configuration doesn't contain any semantic information that it
//  /// wants to contribute to the semantics tree.
//  bool get hasBeenAnnotated => _hasBeenAnnotated;
//  bool _hasBeenAnnotated = false;
//
//  /// The actions (with associated action handlers) that this configuration
//  /// would like to contribute to the semantics tree.
//  ///
//  /// See also:
//  ///
//  /// * [addAction] to add an action.
//  final Map<SemanticsAction, _SemanticsActionHandler> _actions = <SemanticsAction, _SemanticsActionHandler>{};
//
//  int _actionsAsBits = 0;
//
//  /// Adds an `action` to the semantics tree.
//  ///
//  /// The provided `handler` is called to respond to the user triggered
//  /// `action`.
//  void _addAction(SemanticsAction action, _SemanticsActionHandler handler) {
//    assert(handler != null);
//    _actions[action] = handler;
//    _actionsAsBits |= action.index;
//    _hasBeenAnnotated = true;
//  }
//
//  /// Adds an `action` to the semantics tree, whose `handler` does not expect
//  /// any arguments.
//  ///
//  /// The provided `handler` is called to respond to the user triggered
//  /// `action`.
//  void _addArgumentlessAction(SemanticsAction action, VoidCallback handler) {
//    assert(handler != null);
//    _addAction(action, (dynamic args) {
//      assert(args == null);
//      handler();
//    });
//  }
//
//  /// The handler for [SemanticsAction.tap].
//  ///
//  /// This is the semantic equivalent of a user briefly tapping the screen with
//  /// the finger without moving it. For example, a button should implement this
//  /// action.
//  ///
//  /// VoiceOver users on iOS and TalkBack users on Android can trigger this
//  /// action by double-tapping the screen while an element is focused.
//  ///
//  /// On Android prior to Android Oreo a double-tap on the screen while an
//  /// element with an [onTap] handler is focused will not call the registered
//  /// handler. Instead, Android will simulate a pointer down and up event at the
//  /// center of the focused element. Those pointer events will get dispatched
//  /// just like a regular tap with TalkBack disabled would: The events will get
//  /// processed by any [GestureDetector] listening for gestures in the center of
//  /// the focused element. Therefore, to ensure that [onTap] handlers work
//  /// properly on Android versions prior to Oreo, a [GestureDetector] with an
//  /// onTap handler should always be wrapping an element that defines a
//  /// semantic [onTap] handler. By default a [GestureDetector] will register its
//  /// own semantic [onTap] handler that follows this principle.
//  VoidCallback get onTap => _onTap;
//  VoidCallback _onTap;
//  set onTap(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.tap, value);
//    _onTap = value;
//  }
//
//  /// The handler for [SemanticsAction.longPress].
//  ///
//  /// This is the semantic equivalent of a user pressing and holding the screen
//  /// with the finger for a few seconds without moving it.
//  ///
//  /// VoiceOver users on iOS and TalkBack users on Android can trigger this
//  /// action by double-tapping the screen without lifting the finger after the
//  /// second tap.
//  VoidCallback get onLongPress => _onLongPress;
//  VoidCallback _onLongPress;
//  set onLongPress(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.longPress, value);
//    _onLongPress = value;
//  }
//
//  /// The handler for [SemanticsAction.scrollLeft].
//  ///
//  /// This is the semantic equivalent of a user moving their finger across the
//  /// screen from right to left. It should be recognized by controls that are
//  /// horizontally scrollable.
//  ///
//  /// VoiceOver users on iOS can trigger this action by swiping left with three
//  /// fingers. TalkBack users on Android can trigger this action by swiping
//  /// right and then left in one motion path. On Android, [onScrollUp] and
//  /// [onScrollLeft] share the same gesture. Therefore, only on of them should
//  /// be provided.
//  VoidCallback get onScrollLeft => _onScrollLeft;
//  VoidCallback _onScrollLeft;
//  set onScrollLeft(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.scrollLeft, value);
//    _onScrollLeft = value;
//  }
//
//  /// The handler for [SemanticsAction.dismiss].
//  ///
//  /// This is a request to dismiss the currently focused node.
//  ///
//  /// TalkBack users on Android can trigger this action in the local context
//  /// menu, and VoiceOver users on iOS can trigger this action with a standard
//  /// gesture or menu option.
//  VoidCallback get onDismiss => _onDismiss;
//  VoidCallback _onDismiss;
//  set onDismiss(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.dismiss, value);
//    _onDismiss = value;
//  }
//
//  /// The handler for [SemanticsAction.scrollRight].
//  ///
//  /// This is the semantic equivalent of a user moving their finger across the
//  /// screen from left to right. It should be recognized by controls that are
//  /// horizontally scrollable.
//  ///
//  /// VoiceOver users on iOS can trigger this action by swiping right with three
//  /// fingers. TalkBack users on Android can trigger this action by swiping
//  /// left and then right in one motion path. On Android, [onScrollDown] and
//  /// [onScrollRight] share the same gesture. Therefore, only on of them should
//  /// be provided.
//  VoidCallback get onScrollRight => _onScrollRight;
//  VoidCallback _onScrollRight;
//  set onScrollRight(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.scrollRight, value);
//    _onScrollRight = value;
//  }
//
//  /// The handler for [SemanticsAction.scrollUp].
//  ///
//  /// This is the semantic equivalent of a user moving their finger across the
//  /// screen from bottom to top. It should be recognized by controls that are
//  /// vertically scrollable.
//  ///
//  /// VoiceOver users on iOS can trigger this action by swiping up with three
//  /// fingers. TalkBack users on Android can trigger this action by swiping
//  /// right and then left in one motion path. On Android, [onScrollUp] and
//  /// [onScrollLeft] share the same gesture. Therefore, only on of them should
//  /// be provided.
//  VoidCallback get onScrollUp => _onScrollUp;
//  VoidCallback _onScrollUp;
//  set onScrollUp(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.scrollUp, value);
//    _onScrollUp = value;
//  }
//
//  /// The handler for [SemanticsAction.scrollDown].
//  ///
//  /// This is the semantic equivalent of a user moving their finger across the
//  /// screen from top to bottom. It should be recognized by controls that are
//  /// vertically scrollable.
//  ///
//  /// VoiceOver users on iOS can trigger this action by swiping down with three
//  /// fingers. TalkBack users on Android can trigger this action by swiping
//  /// left and then right in one motion path. On Android, [onScrollDown] and
//  /// [onScrollRight] share the same gesture. Therefore, only on of them should
//  /// be provided.
//  VoidCallback get onScrollDown => _onScrollDown;
//  VoidCallback _onScrollDown;
//  set onScrollDown(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.scrollDown, value);
//    _onScrollDown = value;
//  }
//
//  /// The handler for [SemanticsAction.increase].
//  ///
//  /// This is a request to increase the value represented by the widget. For
//  /// example, this action might be recognized by a slider control.
//  ///
//  /// If a [value] is set, [increasedValue] must also be provided and
//  /// [onIncrease] must ensure that [value] will be set to [increasedValue].
//  ///
//  /// VoiceOver users on iOS can trigger this action by swiping up with one
//  /// finger. TalkBack users on Android can trigger this action by pressing the
//  /// volume up button.
//  VoidCallback get onIncrease => _onIncrease;
//  VoidCallback _onIncrease;
//  set onIncrease(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.increase, value);
//    _onIncrease = value;
//  }
//
//  /// The handler for [SemanticsAction.decrease].
//  ///
//  /// This is a request to decrease the value represented by the widget. For
//  /// example, this action might be recognized by a slider control.
//  ///
//  /// If a [value] is set, [decreasedValue] must also be provided and
//  /// [onDecrease] must ensure that [value] will be set to [decreasedValue].
//  ///
//  /// VoiceOver users on iOS can trigger this action by swiping down with one
//  /// finger. TalkBack users on Android can trigger this action by pressing the
//  /// volume down button.
//  VoidCallback get onDecrease => _onDecrease;
//  VoidCallback _onDecrease;
//  set onDecrease(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.decrease, value);
//    _onDecrease = value;
//  }
//
//  /// The handler for [SemanticsAction.copy].
//  ///
//  /// This is a request to copy the current selection to the clipboard.
//  ///
//  /// TalkBack users on Android can trigger this action from the local context
//  /// menu of a text field, for example.
//  VoidCallback get onCopy => _onCopy;
//  VoidCallback _onCopy;
//  set onCopy(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.copy, value);
//    _onCopy = value;
//  }
//
//  /// The handler for [SemanticsAction.cut].
//  ///
//  /// This is a request to cut the current selection and place it in the
//  /// clipboard.
//  ///
//  /// TalkBack users on Android can trigger this action from the local context
//  /// menu of a text field, for example.
//  VoidCallback get onCut => _onCut;
//  VoidCallback _onCut;
//  set onCut(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.cut, value);
//    _onCut = value;
//  }
//
//  /// The handler for [SemanticsAction.paste].
//  ///
//  /// This is a request to paste the current content of the clipboard.
//  ///
//  /// TalkBack users on Android can trigger this action from the local context
//  /// menu of a text field, for example.
//  VoidCallback get onPaste => _onPaste;
//  VoidCallback _onPaste;
//  set onPaste(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.paste, value);
//    _onPaste = value;
//  }
//
//  /// The handler for [SemanticsAction.showOnScreen].
//  ///
//  /// A request to fully show the semantics node on screen. For example, this
//  /// action might be send to a node in a scrollable list that is partially off
//  /// screen to bring it on screen.
//  ///
//  /// For elements in a scrollable list the framework provides a default
//  /// implementation for this action and it is not advised to provide a
//  /// custom one via this setter.
//  VoidCallback get onShowOnScreen => _onShowOnScreen;
//  VoidCallback _onShowOnScreen;
//  set onShowOnScreen(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.showOnScreen, value);
//    _onShowOnScreen = value;
//  }
//
//  /// The handler for [SemanticsAction.onMoveCursorForwardByCharacter].
//  ///
//  /// This handler is invoked when the user wants to move the cursor in a
//  /// text field forward by one character.
//  ///
//  /// TalkBack users can trigger this by pressing the volume up key while the
//  /// input focus is in a text field.
//  MoveCursorHandler get onMoveCursorForwardByCharacter => _onMoveCursorForwardByCharacter;
//  MoveCursorHandler _onMoveCursorForwardByCharacter;
//  set onMoveCursorForwardByCharacter(MoveCursorHandler value) {
//    assert(value != null);
//    _addAction(SemanticsAction.moveCursorForwardByCharacter, (dynamic args) {
//      final bool extentSelection = args;
//      assert(extentSelection != null);
//      value(extentSelection);
//    });
//    _onMoveCursorForwardByCharacter = value;
//  }
//
//  /// The handler for [SemanticsAction.onMoveCursorBackwardByCharacter].
//  ///
//  /// This handler is invoked when the user wants to move the cursor in a
//  /// text field backward by one character.
//  ///
//  /// TalkBack users can trigger this by pressing the volume down key while the
//  /// input focus is in a text field.
//  MoveCursorHandler get onMoveCursorBackwardByCharacter => _onMoveCursorBackwardByCharacter;
//  MoveCursorHandler _onMoveCursorBackwardByCharacter;
//  set onMoveCursorBackwardByCharacter(MoveCursorHandler value) {
//    assert(value != null);
//    _addAction(SemanticsAction.moveCursorBackwardByCharacter, (dynamic args) {
//      final bool extentSelection = args;
//      assert(extentSelection != null);
//      value(extentSelection);
//    });
//    _onMoveCursorBackwardByCharacter = value;
//  }
//
//  /// The handler for [SemanticsAction.setSelection].
//  ///
//  /// This handler is invoked when the user either wants to change the currently
//  /// selected text in a text field or change the position of the cursor.
//  ///
//  /// TalkBack users can trigger this handler by selecting "Move cursor to
//  /// beginning/end" or "Select all" from the local context menu.
//  SetSelectionHandler get onSetSelection => _onSetSelection;
//  SetSelectionHandler _onSetSelection;
//  set onSetSelection(SetSelectionHandler value) {
//    assert(value != null);
//    _addAction(SemanticsAction.setSelection, (dynamic args) {
//      final Map<String, int> selection = args;
//      assert(selection != null && selection['base'] != null && selection['extent'] != null);
//      value(new TextSelection(
//        baseOffset: selection['base'],
//        extentOffset: selection['extent'],
//      ));
//    });
//    _onSetSelection = value;
//  }
//
//  /// The handler for [SemanticsAction.didGainAccessibilityFocus].
//  ///
//  /// This handler is invoked when the node annotated with this handler gains
//  /// the accessibility focus. The accessibility focus is the
//  /// green (on Android with TalkBack) or black (on iOS with VoiceOver)
//  /// rectangle shown on screen to indicate what element an accessibility
//  /// user is currently interacting with.
//  ///
//  /// The accessibility focus is different from the input focus. The input focus
//  /// is usually held by the element that currently responds to keyboard inputs.
//  /// Accessibility focus and input focus can be held by two different nodes!
//  ///
//  /// See also:
//  ///
//  ///  * [onDidLoseAccessibilityFocus], which is invoked when the accessibility
//  ///    focus is removed from the node
//  ///  * [FocusNode], [FocusScope], [FocusManager], which manage the input focus
//  VoidCallback get onDidGainAccessibilityFocus => _onDidGainAccessibilityFocus;
//  VoidCallback _onDidGainAccessibilityFocus;
//  set onDidGainAccessibilityFocus(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.didGainAccessibilityFocus, value);
//    _onDidGainAccessibilityFocus = value;
//  }
//
//  /// The handler for [SemanticsAction.didLoseAccessibilityFocus].
//  ///
//  /// This handler is invoked when the node annotated with this handler
//  /// loses the accessibility focus. The accessibility focus is
//  /// the green (on Android with TalkBack) or black (on iOS with VoiceOver)
//  /// rectangle shown on screen to indicate what element an accessibility
//  /// user is currently interacting with.
//  ///
//  /// The accessibility focus is different from the input focus. The input focus
//  /// is usually held by the element that currently responds to keyboard inputs.
//  /// Accessibility focus and input focus can be held by two different nodes!
//  ///
//  /// See also:
//  ///
//  ///  * [onDidGainAccessibilityFocus], which is invoked when the node gains
//  ///    accessibility focus
//  ///  * [FocusNode], [FocusScope], [FocusManager], which manage the input focus
//  VoidCallback get onDidLoseAccessibilityFocus => _onDidLoseAccessibilityFocus;
//  VoidCallback _onDidLoseAccessibilityFocus;
//  set onDidLoseAccessibilityFocus(VoidCallback value) {
//    _addArgumentlessAction(SemanticsAction.didLoseAccessibilityFocus, value);
//    _onDidLoseAccessibilityFocus = value;
//  }
//
//  /// Returns the action handler registered for [action] or null if none was
//  /// registered.
//  ///
//  /// See also:
//  ///
//  ///  * [addAction] to add an action.
//  _SemanticsActionHandler getActionHandler(SemanticsAction action) => _actions[action];
//
//  /// Determines the position of this node among its siblings in the traversal
//  /// sort order.
//  ///
//  /// This is used to describe the order in which the semantic node should be
//  /// traversed by the accessibility services on the platform (e.g. VoiceOver
//  /// on iOS and TalkBack on Android).
//  ///
//  /// Whether this sort key has an effect on the [SemanticsNode] sort order is
//  /// subject to how this configuration is used. For example, the [absorb]
//  /// method may decide to not use this key when it combines multiple
//  /// [SemanticsConfiguration] objects.
//  androidx.ui.semantics.SemanticsSortKey get sortKey => _sortKey;
//  androidx.ui.semantics.SemanticsSortKey _sortKey;
//  set sortKey(androidx.ui.semantics.SemanticsSortKey value) {
//    assert(value != null);
//    _sortKey = value;
//    _hasBeenAnnotated = true;
//  }
//
//  /// Whether the semantic information provided by the owning [RenderObject] and
//  /// all of its descendants should be treated as one logical entity.
//  ///
//  /// If set to true, the descendants of the owning [RenderObject]'s
//  /// [SemanticsNode] will merge their semantic information into the
//  /// [SemanticsNode] representing the owning [RenderObject].
//  ///
//  /// Setting this to true requires that [isSemanticBoundary] is also true.
//  bool get isMergingSemanticsOfDescendants => _isMergingSemanticsOfDescendants;
//  bool _isMergingSemanticsOfDescendants = false;
//  set isMergingSemanticsOfDescendants(bool value) {
//    assert(isSemanticBoundary);
//    _isMergingSemanticsOfDescendants = value;
//    _hasBeenAnnotated = true;
//  }
//
//  /// The handlers for each supported [androidx.ui.semantics.CustomSemanticsAction].
//  ///
//  /// Whenever a custom accessibility action is added to a node, the action
//  /// [SemanticAction.customAction] is automatically added. A handler is
//  /// created which uses the passed argument to lookup the custom action
//  /// handler from this map and invoke it, if present.
//  Map<androidx.ui.semantics.CustomSemanticsAction, VoidCallback> get customSemanticsActions => _customSemanticsActions;
//  Map<androidx.ui.semantics.CustomSemanticsAction, VoidCallback> _customSemanticsActions = <androidx.ui.semantics.CustomSemanticsAction, VoidCallback>{};
//  set customSemanticsActions(Map<androidx.ui.semantics.CustomSemanticsAction, VoidCallback> value) {
//    _hasBeenAnnotated = true;
//    _actionsAsBits |= SemanticsAction.customAction.index;
//    _customSemanticsActions = value;
//    _actions[SemanticsAction.customAction] = _onCustomSemanticsAction;
//  }
//
//  void _onCustomSemanticsAction(dynamic args) {
//    final androidx.ui.semantics.CustomSemanticsAction action = androidx.ui.semantics.CustomSemanticsAction.getAction(args);
//    if (action == null)
//      return;
//    final VoidCallback callback = _customSemanticsActions[action];
//    if (callback != null)
//      callback();
//  }
//
//  /// A textual description of the owning [RenderObject].
//  ///
//  /// On iOS this is used for the `accessibilityLabel` property defined in the
//  /// `UIAccessibility` Protocol. On Android it is concatenated together with
//  /// [value] and [hint] in the following order: [value], [label], [hint].
//  /// The concatenated value is then used as the `Text` description.
//  ///
//  /// The reading direction is given by [textDirection].
//  String get label => _label;
//  String _label = '';
//  set label(String label) {
//    assert(label != null);
//    _label = label;
//    _hasBeenAnnotated = true;
//  }
//
//  /// A textual description for the current value of the owning [RenderObject].
//  ///
//  /// On iOS this is used for the `accessibilityValue` property defined in the
//  /// `UIAccessibility` Protocol. On Android it is concatenated together with
//  /// [label] and [hint] in the following order: [value], [label], [hint].
//  /// The concatenated value is then used as the `Text` description.
//  ///
//  /// The reading direction is given by [textDirection].
//  ///
//  /// See also:
//  ///
//  ///  * [decreasedValue], describes what [value] will be after performing
//  ///    [SemanticsAction.decrease]
//  ///  * [increasedValue], describes what [value] will be after performing
//  ///    [SemanticsAction.increase]
//  String get value => _value;
//  String _value = '';
//  set value(String value) {
//    assert(value != null);
//    _value = value;
//    _hasBeenAnnotated = true;
//  }
//
//  /// The value that [value] will have after performing a
//  /// [SemanticsAction.decrease] action.
//  ///
//  /// This must be set if a handler for [SemanticsAction.decrease] is provided
//  /// and [value] is set.
//  ///
//  /// The reading direction is given by [textDirection].
//  String get decreasedValue => _decreasedValue;
//  String _decreasedValue = '';
//  set decreasedValue(String decreasedValue) {
//    assert(decreasedValue != null);
//    _decreasedValue = decreasedValue;
//    _hasBeenAnnotated = true;
//  }
//
//  /// The value that [value] will have after performing a
//  /// [SemanticsAction.increase] action.
//  ///
//  /// This must be set if a handler for [SemanticsAction.increase] is provided
//  /// and [value] is set.
//  ///
//  /// The reading direction is given by [textDirection].
//  String get increasedValue => _increasedValue;
//  String _increasedValue = '';
//  set increasedValue(String increasedValue) {
//    assert(increasedValue != null);
//    _increasedValue = increasedValue;
//    _hasBeenAnnotated = true;
//  }
//
//  /// A brief description of the result of performing an action on this node.
//  ///
//  /// On iOS this is used for the `accessibilityHint` property defined in the
//  /// `UIAccessibility` Protocol. On Android it is concatenated together with
//  /// [label] and [value] in the following order: [value], [label], [hint].
//  /// The concatenated value is then used as the `Text` description.
//  ///
//  /// The reading direction is given by [textDirection].
//  String get hint => _hint;
//  String _hint = '';
//  set hint(String hint) {
//    assert(hint != null);
//    _hint = hint;
//    _hasBeenAnnotated = true;
//  }
//
//  /// Provides hint values which override the default hints on supported
//  /// platforms.
//  androidx.ui.semantics.SemanticsHintOverrides get hintOverrides => _hintOverrides;
//  androidx.ui.semantics.SemanticsHintOverrides _hintOverrides;
//  set hintOverrides(androidx.ui.semantics.SemanticsHintOverrides value) {
//    if (value == null)
//      return;
//    _hintOverrides = value;
//    _hasBeenAnnotated = true;
//  }
//
//  /// Whether the semantics node is the root of a subtree for which values
//  /// should be announced.
//  ///
//  /// See also:
//  ///  * [SemanticsFlag.scopesRoute], for a full description of route scoping.
//  bool get scopesRoute => _hasFlag(SemanticsFlag.scopesRoute);
//  set scopesRoute(bool value) {
//    _setFlag(SemanticsFlag.scopesRoute, value);
//  }
//
//  /// Whether the semantics node contains the label of a route.
//  ///
//  /// See also:
//  ///  * [SemanticsFlag.namesRoute], for a full description of route naming.
//  bool get namesRoute => _hasFlag(SemanticsFlag.namesRoute);
//  set namesRoute(bool value) {
//    _setFlag(SemanticsFlag.namesRoute, value);
//  }
//
//  /// Whether the semantics node represents an image.
//  bool get isImage => _hasFlag(SemanticsFlag.isImage);
//  set isImage(bool value) {
//    _setFlag(SemanticsFlag.isImage, value);
//  }
//
//  /// Whether the semantics node is a live region.
//  ///
//  /// On Android, when a live region semantics node is first created TalkBack
//  /// will make a polite announcement of the current label. This announcement
//  /// occurs even if the node is not focused. Subsequent polite announcements
//  /// can be made by sending a [UpdateLiveRegionEvent] semantics event. The
//  /// announcement will only be made if the node's label has changed since the
//  /// last update.
//  ///
//  /// An example of a live region is the [Snackbar] widget. When it appears
//  /// on the screen it may be difficult to focus to read the label. A live
//  /// region causes an initial polite announcement to be generated
//  /// automatically.
//  ///
//  /// See also:
//  ///
//  ///   * [SemanticsFlag.isLiveRegion], the semantics flag that this setting controls.
//  bool get liveRegion => _hasFlag(SemanticsFlag.isLiveRegion);
//  set liveRegion(bool value) {
//    _setFlag(SemanticsFlag.isLiveRegion, value);
//  }
//
//  /// The reading direction for the text in [label], [value], [hint],
//  /// [increasedValue], and [decreasedValue].
//  TextDirection get textDirection => _textDirection;
//  TextDirection _textDirection;
//  set textDirection(TextDirection textDirection) {
//    _textDirection = textDirection;
//    _hasBeenAnnotated = true;
//  }
//
//  /// Whether the owning [RenderObject] is selected (true) or not (false).
//  bool get isSelected => _hasFlag(SemanticsFlag.isSelected);
//  set isSelected(bool value) {
//    _setFlag(SemanticsFlag.isSelected, value);
//  }
//
//  /// Whether the owning [RenderObject] is currently enabled.
//  ///
//  /// A disabled object does not respond to user interactions. Only objects that
//  /// usually respond to user interactions, but which currently do not (like a
//  /// disabled button) should be marked as disabled.
//  ///
//  /// The setter should not be called for objects (like static text) that never
//  /// respond to user interactions.
//  ///
//  /// The getter will return null if the owning [RenderObject] doesn't support
//  /// the concept of being enabled/disabled.
//  bool get isEnabled => _hasFlag(SemanticsFlag.hasEnabledState) ? _hasFlag(SemanticsFlag.isEnabled) : null;
//  set isEnabled(bool value) {
//    _setFlag(SemanticsFlag.hasEnabledState, true);
//    _setFlag(SemanticsFlag.isEnabled, value);
//  }
//
//  /// If this node has Boolean state that can be controlled by the user, whether
//  /// that state is checked or unchecked, corresponding to true and false,
//  /// respectively.
//  ///
//  /// Do not call the setter for this field if the owning [RenderObject] doesn't
//  /// have checked/unchecked state that can be controlled by the user.
//  ///
//  /// The getter returns null if the owning [RenderObject] does not have
//  /// checked/unchecked state.
//  bool get isChecked => _hasFlag(SemanticsFlag.hasCheckedState) ? _hasFlag(SemanticsFlag.isChecked) : null;
//  set isChecked(bool value) {
//    _setFlag(SemanticsFlag.hasCheckedState, true);
//    _setFlag(SemanticsFlag.isChecked, value);
//  }
//
//  /// If this node has Boolean state that can be controlled by the user, whether
//  /// that state is on or off, corresponding to true and false, respectively.
//  ///
//  /// Do not call the setter for this field if the owning [RenderObject] doesn't
//  /// have on/off state that can be controlled by the user.
//  ///
//  /// The getter returns null if the owning [RenderObject] does not have
//  /// on/off state.
//  bool get isToggled => _hasFlag(SemanticsFlag.hasToggledState) ? _hasFlag(SemanticsFlag.isToggled) : null;
//  set isToggled(bool value) {
//    _setFlag(SemanticsFlag.hasToggledState, true);
//    _setFlag(SemanticsFlag.isToggled, value);
//  }
//
//  /// Whether the owning RenderObject corresponds to UI that allows the user to
//  /// pick one of several mutually exclusive options.
//  ///
//  /// For example, a [Radio] button is in a mutually exclusive group because
//  /// only one radio button in that group can be marked as [isChecked].
//  bool get isInMutuallyExclusiveGroup => _hasFlag(SemanticsFlag.isInMutuallyExclusiveGroup);
//  set isInMutuallyExclusiveGroup(bool value) {
//    _setFlag(SemanticsFlag.isInMutuallyExclusiveGroup, value);
//  }
//
//  /// Whether the owning [RenderObject] currently holds the user's focus.
//  bool get isFocused => _hasFlag(SemanticsFlag.isFocused);
//  set isFocused(bool value) {
//    _setFlag(SemanticsFlag.isFocused, value);
//  }
//
//  /// Whether the owning [RenderObject] is a button (true) or not (false).
//  bool get isButton => _hasFlag(SemanticsFlag.isButton);
//  set isButton(bool value) {
//    _setFlag(SemanticsFlag.isButton, value);
//  }
//
//  /// Whether the owning [RenderObject] is a header (true) or not (false).
//  bool get isHeader => _hasFlag(SemanticsFlag.isHeader);
//  set isHeader(bool value) {
//    _setFlag(SemanticsFlag.isHeader, value);
//  }
//
//  /// Whether the owning [RenderObject] is considered hidden.
//  ///
//  /// Hidden elements are currently not visible on screen. They may be covered
//  /// by other elements or positioned outside of the visible area of a viewport.
//  ///
//  /// Hidden elements cannot gain accessibility focus though regular touch. The
//  /// only way they can be focused is by moving the focus to them via linear
//  /// navigation.
//  ///
//  /// Platforms are free to completely ignore hidden elements and new platforms
//  /// are encouraged to do so.
//  ///
//  /// Instead of marking an element as hidden it should usually be excluded from
//  /// the semantics tree altogether. Hidden elements are only included in the
//  /// semantics tree to work around platform limitations and they are mainly
//  /// used to implement accessibility scrolling on iOS.
//  bool get isHidden => _hasFlag(SemanticsFlag.isHidden);
//  set isHidden(bool value) {
//    _setFlag(SemanticsFlag.isHidden, value);
//  }
//
//  /// Whether the owning [RenderObject] is a text field.
//  bool get isTextField => _hasFlag(SemanticsFlag.isTextField);
//  set isTextField(bool value) {
//    _setFlag(SemanticsFlag.isTextField, value);
//  }
//
//  /// Whether the [value] should be obscured.
//  ///
//  /// This option is usually set in combination with [textField] to indicate
//  /// that the text field contains a password (or other sensitive information).
//  /// Doing so instructs screen readers to not read out the [value].
//  bool get isObscured => _hasFlag(SemanticsFlag.isObscured);
//  set isObscured(bool value) {
//    _setFlag(SemanticsFlag.isObscured, value);
//  }
//
//  /// Whether the platform can scroll the semantics node when the user attempts
//  /// to move focus to an offscreen child.
//  ///
//  /// For example, a [ListView] widget has implicit scrolling so that users can
//  /// easily move to the next visible set of children. A [TabBar] widget does
//  /// not have implicit scrolling, so that users can navigate into the tab
//  /// body when reaching the end of the tab bar.
//  bool get hasImplicitScrolling => _hasFlag(SemanticsFlag.hasImplicitScrolling);
//  set hasImplicitScrolling(bool value) {
//    _setFlag(SemanticsFlag.hasImplicitScrolling, value);
//  }
//
//  /// The currently selected text (or the position of the cursor) within [value]
//  /// if this node represents a text field.
//  TextSelection get textSelection => _textSelection;
//  TextSelection _textSelection;
//  set textSelection(TextSelection value) {
//    assert(value != null);
//    _textSelection = value;
//    _hasBeenAnnotated = true;
//  }
//
//  /// Indicates the current scrolling position in logical pixels if the node is
//  /// scrollable.
//  ///
//  /// The properties [scrollExtentMin] and [scrollExtentMax] indicate the valid
//  /// in-range values for this property. The value for [scrollPosition] may
//  /// (temporarily) be outside that range, e.g. during an overscroll.
//  ///
//  /// See also:
//  ///
//  ///  * [ScrollPosition.pixels], from where this value is usually taken.
//  double get scrollPosition => _scrollPosition;
//  double _scrollPosition;
//  set scrollPosition(double value) {
//    assert(value != null);
//    _scrollPosition = value;
//    _hasBeenAnnotated = true;
//  }
//
//  /// Indicates the maximum in-range value for [scrollPosition] if the node is
//  /// scrollable.
//  ///
//  /// This value may be infinity if the scroll is unbound.
//  ///
//  /// See also:
//  ///
//  ///  * [ScrollPosition.maxScrollExtent], from where this value is usually taken.
//  double get scrollExtentMax => _scrollExtentMax;
//  double _scrollExtentMax;
//  set scrollExtentMax(double value) {
//    assert(value != null);
//    _scrollExtentMax = value;
//    _hasBeenAnnotated = true;
//  }
//
//  /// Indicates the minimum in-range value for [scrollPosition] if the node is
//  /// scrollable.
//  ///
//  /// This value may be infinity if the scroll is unbound.
//  ///
//  /// See also:
//  ///
//  ///  * [ScrollPosition.minScrollExtent], from where this value is usually taken.
//  double get scrollExtentMin => _scrollExtentMin;
//  double _scrollExtentMin;
//  set scrollExtentMin(double value) {
//    assert(value != null);
//    _scrollExtentMin = value;
//    _hasBeenAnnotated = true;
//  }
//
//  // TAGS
//
//  /// The set of tags that this configuration wants to add to all child
//  /// [SemanticsNode]s.
//  ///
//  /// See also:
//  ///
//  ///  * [addTagForChildren] to add a tag and for more information about their
//  ///    usage.
//  Iterable<androidx.ui.semantics.SemanticsTag> get tagsForChildren => _tagsForChildren;
//  Set<androidx.ui.semantics.SemanticsTag> _tagsForChildren;
//
//  /// Specifies a [androidx.ui.semantics.SemanticsTag] that this configuration wants to apply to all
//  /// child [SemanticsNode]s.
//  ///
//  /// The tag is added to all [SemanticsNode] that pass through the
//  /// [RenderObject] owning this configuration while looking to be attached to a
//  /// parent [SemanticsNode].
//  ///
//  /// Tags are used to communicate to a parent [SemanticsNode] that a child
//  /// [SemanticsNode] was passed through a particular [RenderObject]. The parent
//  /// can use this information to determine the shape of the semantics tree.
//  ///
//  /// See also:
//  ///
//  ///  * [RenderSemanticsGestureHandler.excludeFromScrolling] for an example of
//  ///    how tags are used.
//  void addTagForChildren(androidx.ui.semantics.SemanticsTag tag) {
//    _tagsForChildren ??= new Set<androidx.ui.semantics.SemanticsTag>();
//    _tagsForChildren.add(tag);
//  }
//
//  // INTERNAL FLAG MANAGEMENT
//
//  int _flags = 0;
//  void _setFlag(SemanticsFlag flag, bool value) {
//    if (value) {
//      _flags |= flag.index;
//    } else {
//      _flags &= ~flag.index;
//    }
//    _hasBeenAnnotated = true;
//  }
//
//  bool _hasFlag(SemanticsFlag flag) => (_flags & flag.index) != 0;
//
//  // CONFIGURATION COMBINATION LOGIC
//
//  /// Whether this configuration is compatible with the provided `other`
//  /// configuration.
//  ///
//  /// Two configurations are said to be compatible if they can be added to the
//  /// same [SemanticsNode] without losing any semantics information.
//  bool isCompatibleWith(SemanticsConfiguration other) {
//    if (other == null || !other.hasBeenAnnotated || !hasBeenAnnotated)
//      return true;
//    if (_actionsAsBits & other._actionsAsBits != 0)
//      return false;
//    if ((_flags & other._flags) != 0)
//      return false;
//    if (_value != null && _value.isNotEmpty && other._value != null && other._value.isNotEmpty)
//      return false;
//    return true;
//  }
//
//  /// Absorb the semantic information from `other` into this configuration.
//  ///
//  /// This adds the semantic information of both configurations and saves the
//  /// result in this configuration.
//  ///
//  /// Only configurations that have [explicitChildNodes] set to false can
//  /// absorb other configurations and it is recommended to only absorb compatible
//  /// configurations as determined by [isCompatibleWith].
//  void absorb(SemanticsConfiguration other) {
//    assert(!explicitChildNodes);
//
//    if (!other.hasBeenAnnotated)
//      return;
//
//    _actions.addAll(other._actions);
//    _customSemanticsActions.addAll(other._customSemanticsActions);
//    _actionsAsBits |= other._actionsAsBits;
//    _flags |= other._flags;
//    _textSelection ??= other._textSelection;
//    _scrollPosition ??= other._scrollPosition;
//    _scrollExtentMax ??= other._scrollExtentMax;
//    _scrollExtentMin ??= other._scrollExtentMin;
//    _hintOverrides ??= other._hintOverrides;
//
//    textDirection ??= other.textDirection;
//    _sortKey ??= other._sortKey;
//    _label = _concatStrings(
//      thisString: _label,
//      thisTextDirection: textDirection,
//      otherString: other._label,
//      otherTextDirection: other.textDirection,
//    );
//    if (_decreasedValue == '' || _decreasedValue == null)
//      _decreasedValue = other._decreasedValue;
//    if (_value == '' || _value == null)
//      _value = other._value;
//    if (_increasedValue == '' || _increasedValue == null)
//      _increasedValue = other._increasedValue;
//    _hint = _concatStrings(
//      thisString: _hint,
//      thisTextDirection: textDirection,
//      otherString: other._hint,
//      otherTextDirection: other.textDirection,
//    );
//
//    _hasBeenAnnotated = _hasBeenAnnotated || other._hasBeenAnnotated;
//  }
//
//  /// Returns an exact copy of this configuration.
//  SemanticsConfiguration copy() {
//    return new SemanticsConfiguration()
//      .._isSemanticBoundary = _isSemanticBoundary
//      ..explicitChildNodes = explicitChildNodes
//      ..isBlockingSemanticsOfPreviouslyPaintedNodes = isBlockingSemanticsOfPreviouslyPaintedNodes
//      .._hasBeenAnnotated = _hasBeenAnnotated
//      .._isMergingSemanticsOfDescendants = _isMergingSemanticsOfDescendants
//      .._textDirection = _textDirection
//      .._sortKey = _sortKey
//      .._label = _label
//      .._increasedValue = _increasedValue
//      .._value = _value
//      .._decreasedValue = _decreasedValue
//      .._hint = _hint
//      .._hintOverrides = _hintOverrides
//      .._flags = _flags
//      .._tagsForChildren = _tagsForChildren
//      .._textSelection = _textSelection
//      .._scrollPosition = _scrollPosition
//      .._scrollExtentMax = _scrollExtentMax
//      .._scrollExtentMin = _scrollExtentMin
//      .._actionsAsBits = _actionsAsBits
//      .._actions.addAll(_actions)
//      .._customSemanticsActions.addAll(_customSemanticsActions);
//  }
}