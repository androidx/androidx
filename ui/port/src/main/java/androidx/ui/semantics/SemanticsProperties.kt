package androidx.ui.semantics

import androidx.ui.VoidCallback
import androidx.ui.foundation.diagnostics.DiagnosticableTree
import androidx.ui.engine.text.TextDirection

/**
 * Contains properties used by assistive technologies to make the application
 * more accessible.
 *
 * The properties of this class are used to generate a [SemanticsNode]s in the
 * semantics tree.
 */
// @immutable
class SemanticsProperties(
    /**
     * If non-null, indicates that this subtree represents something that can be
     * in an enabled or disabled state.
     *
     * For example, a button that a user can currently interact with would set
     * this field to true. A button that currently does not respond to user
     * interactions would set this field to false.
     */
    val enabled: Boolean? = null,
    /**
     * If non-null, indicates that this subtree represents a checkbox
     * or similar widget with a "checked" state, and what its current
     * state is.
     *
     * This is mutually exclusive with [toggled].
     */
    val checked: Boolean? = null,
    /**
     * If non-null indicates that this subtree represents something that can be
     * in a selected or unselected state, and what its current state is.
     *
     * The active tab in a tab bar for example is considered "selected", whereas
     * all other tabs are unselected.
     */
    val selected: Boolean? = null,
    /**
     * If non-null, indicates that this subtree represents a toggle switch
     * or similar widget with an "on" state, and what its current
     * state is.
     *
     * This is mutually exclusive with [checked].
     */
    val toggled: Boolean? = null,
    /**
     * If non-null, indicates that this subtree represents a button.
     *
     * TalkBack/VoiceOver provides users with the hint "button" when a button
     * is focused.
     */
    val button: Boolean? = null,
    /**
     * If non-null, indicates that this subtree represents a header.
     *
     * A header divides into sections. For example, an address book application
     * might define headers A, B, C, etc. to divide the list of alphabetically
     * sorted contacts into sections.
     */
    val header: Boolean? = null,
    /**
     * If non-null, indicates that this subtree represents a text field.
     *
     * TalkBack/VoiceOver provide special affordances to enter text into a
     * text field.
     */
    val textField: Boolean? = null,
    /**
     * If non-null, whether the node currently holds input focus.
     *
     * At most one node in the tree should hold input focus at any point in time.
     *
     * Input focus (indicates that the node will receive keyboard events) is not
     * to be confused with accessibility focus. Accessibility focus is the
     * green/black rectangular that TalkBack/VoiceOver on the screen and is
     * separate from input focus.
     */
    val focused: Boolean? = null,
    /**
     * If non-null, whether a semantic node is in a mutually exclusive group.
     *
     * For example, a radio button is in a mutually exclusive group because only
     * one radio button in that group can be marked as [checked].
     */
    val inMutuallyExclusiveGroup: Boolean? = null,
    /**
     * If non-null, whether the node is considered hidden.
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
    val hidden: Boolean? = null,
    /**
     * If non-null, whether [value] should be obscured.
     *
     * This option is usually set in combination with [textField] to indicate
     * that the text field contains a password (or other sensitive information).
     * Doing so instructs screen readers to not read out the [value].
     */
    val obscured: Boolean? = null,
    /**
     * If non-null, whether the node corresponds to the root of a subtree for
     * which a route name should be announced.
     *
     * Generally, this is set in combination with [explicitChildNodes], since
     * nodes with this flag are not considered focusable by Android or iOS.
     *
     * See also:
     *
     *  * [SemanticsFlag.scopesRoute] for a description of how the announced
     *    value is selected.
     */
    val scopesRoute: Boolean? = null,
    /**
     * If non-null, whether the node contains the semantic label for a route.
     *
     * See also:
     *
     *  * [SemanticsFlag.namesRoute] for a description of how the name is used.
     */
    val namesRoute: Boolean? = null,
    /**
     * If non-null, whether the node represents an image.
     *
     * See also:
     *
     *   * [SemanticsFlag.image], for the flag this setting controls.
     */
    val image: Boolean? = null,
    /**
     * If non-null, whether the node should be considered a live region.
     *
     * On Android, when a live region semantics node is first created TalkBack
     * will make a polite announcement of the current label. This announcement
     * occurs even if the node is not focused. Subsequent polite announcements
     * can be made by sending a [UpdateLiveRegionEvent] semantics event. The
     * announcement will only be made if the node's label has changed since the
     * last update.
     *
     * On iOS, no announcements are made but the node is marked as
     * `UIAccessibilityTraitUpdatesFrequently`.
     *
     * An example of a live region is the [Snackbar] widget. When it appears
     * on the screen it may be difficult to focus to read the label. A live
     * region causes an initial polite announcement to be generated
     * automatically.
     *
     * See also:
     *   * [SemanticsFlag.liveRegion], the semantics flag this setting controls.
     *   * [SemanticsConfiguration.liveRegion], for a full description of a live region.
     *   * [UpdateLiveRegionEvent], to trigger a polite announcement of a live region.
     */
    val liveRegion: Boolean? = null,
    /**
     * Provides a textual description of the widget.
     *
     * If a label is provided, there must either by an ambient [Directionality]
     * or an explicit [textDirection] should be provided.
     *
     * See also:
     *
     *  * [SemanticsConfiguration.label] for a description of how this is exposed
     *    in TalkBack and VoiceOver.
     */
    val label: String? = null,
    /**
     * Provides a textual description of the value of the widget.
     *
     * If a value is provided, there must either by an ambient [Directionality]
     * or an explicit [textDirection] should be provided.
     *
     * See also:
     *
     *  * [SemanticsConfiguration.value] for a description of how this is exposed
     *    in TalkBack and VoiceOver.
     */
    val value: String? = null,
    /**
     * The value that [value] will become after a [SemanticsAction.increase]
     * action has been performed on this widget.
     *
     * If a value is provided, [onIncrease] must also be set and there must
     * either be an ambient [Directionality] or an explicit [textDirection]
     * must be provided.
     *
     * See also:
     *
     *  * [SemanticsConfiguration.increasedValue] for a description of how this
     *    is exposed in TalkBack and VoiceOver.
     */
    val increasedValue: String? = null,
    /**
     * The value that [value] will become after a [SemanticsAction.decrease]
     * action has been performed on this widget.
     *
     * If a value is provided, [onDecrease] must also be set and there must
     * either be an ambient [Directionality] or an explicit [textDirection]
     * must be provided.
     *
     * See also:
     *
     *  * [SemanticsConfiguration.decreasedValue] for a description of how this
     *    is exposed in TalkBack and VoiceOver.
     */
    val decreasedValue: String? = null,
    /**
     * Provides a brief textual description of the result of an action performed
     * on the widget.
     *
     * If a hint is provided, there must either be an ambient [Directionality]
     * or an explicit [textDirection] should be provided.
     *
     * See also:
     *
     *  * [SemanticsConfiguration.hint] for a description of how this is exposed
     *    in TalkBack and VoiceOver.
     */
    val hint: String? = null,
    /**
     * Provides hint values which override the default hints on supported
     * platforms.
     *
     * On Android, If no hint overrides are used then default [hint] will be
     * combined with the [label]. Otherwise, the [hint] will be ignored as long
     * as there as at least one non-null hint override.
     *
     * On iOS, these are always ignored and the default [hint] is used instead.
     */
    val hintOverrides: SemanticsHintOverrides? = null,
    /**
     * The reading direction of the [label], [value], [hint], [increasedValue],
     * and [decreasedValue].
     *
     * Defaults to the ambient [Directionality].
     */
    val textDirection: TextDirection? = null,
    /**
     * Determines the position of this node among its siblings in the traversal
     * sort order.
     *
     * This is used to describe the order in which the semantic node should be
     * traversed by the accessibility services on the platform (e.g. VoiceOver
     * on iOS and TalkBack on Android).
     */
    val sortKey: SemanticsSortKey? = null,
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
    val onTap: VoidCallback? = null,
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
    val onLongPress: VoidCallback? = null,
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
    val onScrollLeft: VoidCallback? = null,
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
    val onScrollRight: VoidCallback? = null,
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
    val onScrollUp: VoidCallback? = null,
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
    val onScrollDown: VoidCallback? = null,
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
    val onIncrease: VoidCallback? = null,
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
    val onDecrease: VoidCallback? = null,
    /**
     * The handler for [SemanticsAction.copy].
     *
     * This is a request to copy the current selection to the clipboard.
     *
     * TalkBack users on Android can trigger this action from the local context
     * menu of a text field, for example.
     */
    val onCopy: VoidCallback? = null,
    /**
     * The handler for [SemanticsAction.cut].
     *
     * This is a request to cut the current selection and place it in the
     * clipboard.
     *
     * TalkBack users on Android can trigger this action from the local context
     * menu of a text field, for example.
     */
    val onCut: VoidCallback? = null,
    /**
     * The handler for [SemanticsAction.paste].
     *
     * This is a request to paste the current content of the clipboard.
     *
     * TalkBack users on Android can trigger this action from the local context
     * menu of a text field, for example.
     */
    val onPaste: VoidCallback? = null,
    /**
     * The handler for [SemanticsAction.onMoveCursorForwardByCharacter].
     *
     * This handler is invoked when the user wants to move the cursor in a
     * text field forward by one character.
     *
     * TalkBack users can trigger this by pressing the volume up key while the
     * input focus is in a text field.
     */
    val onMoveCursorForwardByCharacter: MoveCursorHandler? = null,
    /**
     * The handler for [SemanticsAction.onMoveCursorBackwardByCharacter].
     *
     * This handler is invoked when the user wants to move the cursor in a
     * text field backward by one character.
     *
     * TalkBack users can trigger this by pressing the volume down key while the
     * input focus is in a text field.
     */
    val onMoveCursorBackwardByCharacter: MoveCursorHandler? = null,
    /**
     * The handler for [SemanticsAction.setSelection].
     *
     * This handler is invoked when the user either wants to change the currently
     * selected text in a text field or change the position of the cursor.
     *
     * TalkBack users can trigger this handler by selecting "Move cursor to
     * beginning/end" or "Select all" from the local context menu.
     */
    val onSetSelection: SetSelectionHandler? = null,
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
    val onDidGainAccessibilityFocus: VoidCallback? = null,
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
    val onDidLoseAccessibilityFocus: VoidCallback? = null,
    /**
     * The handler for [SemanticsAction.dismiss].
     *
     * This is a request to dismiss the currently focused node.
     *
     * TalkBack users on Android can trigger this action in the local context
     * menu, and VoiceOver users on iOS can trigger this action with a standard
     * gesture or menu option.
     */
    val onDismiss: VoidCallback? = null
) : DiagnosticableTree {
//
//  @override
//  void debugFillProperties(DiagnosticPropertiesBuilder properties) {
//    super.debugFillProperties(properties);
//    properties.add(new DiagnosticsProperty<Boolean>('checked', checked, defaultValue: null));
//    properties.add(new DiagnosticsProperty<Boolean>('selected', selected, defaultValue: null));
//    properties.add(new StringProperty('label', label, defaultValue: ''));
//    properties.add(new StringProperty('value', value));
//    properties.add(new StringProperty('hint', hint));
//    properties.add(new EnumProperty<TextDirection>('textDirection', textDirection, defaultValue: null));
//    properties.add(new DiagnosticsProperty<SemanticsSortKey>('sortKey', sortKey, defaultValue: null));
//    properties.add(new DiagnosticsProperty<SemanticsHintOverrides>('hintOverrides', hintOverrides));
//  }
//
//  @override
//  String toStringShort() => '$runtimeType'; // the hashCode isn't important since we're immutable
}