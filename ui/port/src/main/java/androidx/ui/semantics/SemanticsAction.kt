/*
 * Copyright 2018 The Android Open Source Project
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

// part of dart.ui;

package androidx.ui.semantics

private const val _kTapIndex = 1 shl 0
private const val _kLongPressIndex = 1 shl 1
private const val _kScrollLeftIndex = 1 shl 2
private const val _kScrollRightIndex = 1 shl 3
private const val _kScrollUpIndex = 1 shl 4
private const val _kScrollDownIndex = 1 shl 5
private const val _kIncreaseIndex = 1 shl 6
private const val _kDecreaseIndex = 1 shl 7
private const val _kShowOnScreenIndex = 1 shl 8
private const val _kMoveCursorForwardByCharacterIndex = 1 shl 9
private const val _kMoveCursorBackwardByCharacterIndex = 1 shl 10
private const val _kSetSelectionIndex = 1 shl 11
private const val _kCopyIndex = 1 shl 12
private const val _kCutIndex = 1 shl 13
private const val _kPasteIndex = 1 shl 14
private const val _kDidGainAccessibilityFocusIndex = 1 shl 15
private const val _kDidLoseAccessibilityFocusIndex = 1 shl 16
private const val _kCustomAction = 1 shl 17
private const val _kDismissIndex = 1 shl 18
private const val _kMoveCursorForwardByWordIndex = 1 shl 19
private const val _kMoveCursorBackwardByWordIndex = 1 shl 20

/**
 * The possible actions that can be conveyed from the operating system
 * accessibility APIs to a semantics node.
 */
enum class SemanticsAction(
    /**
     * The numerical value for this action.
     *
     * Each action has one bit set in this bit field.
     */
    val index: Int
) {

    /**
     * The equivalent of a user briefly tapping the screen with the finger
     * without moving it.
     */
    tap(_kTapIndex),

    /**
     * The equivalent of a user pressing and holding the screen with the finger
     * for a few seconds without moving it.
     */
    longPress(_kLongPressIndex),

    /**
     * The equivalent of a user moving their finger across the screen from right
     * to left.
     *
     * This action should be recognized by controls that are horizontally
     * scrollable.
     */
    scrollLeft(_kScrollLeftIndex),

    /**
     * The equivalent of a user moving their finger across the screen from left
     * to right.
     *
     * This action should be recognized by controls that are horizontally
     * scrollable.
     */
    scrollRight(_kScrollRightIndex),

    /**
     * The equivalent of a user moving their finger across the screen from
     * bottom to top.
     *
     * This action should be recognized by controls that are vertically
     * scrollable.
     */
    scrollUp(_kScrollUpIndex),

    /**
     * The equivalent of a user moving their finger across the screen from top
     * to bottom.
     *
     * This action should be recognized by controls that are vertically
     * scrollable.
     */
    scrollDown(_kScrollDownIndex),

    /**
     * A request to increase the value represented by the semantics node.
     *
     * For example, this action might be recognized by a slider control.
     */
    increase(_kIncreaseIndex),

    /**
     * A request to decrease the value represented by the semantics node.
     *
     * For example, this action might be recognized by a slider control.
     */
    decrease(_kDecreaseIndex),

    /**
     * A request to fully show the semantics node on screen.
     *
     * For example, this action might be send to a node in a scrollable list that
     * is partially off screen to bring it on screen.
     */
    showOnScreen(_kShowOnScreenIndex),

    /**
     * Move the cursor forward by one character.
     *
     * This is for example used by the cursor control in text fields.
     *
     * The action includes a boolean argument, which indicates whether the cursor
     * movement should extend (or start) a selection.
     */
    moveCursorForwardByCharacter(_kMoveCursorForwardByCharacterIndex),

    /**
     * Move the cursor backward by one character.
     *
     * This is for example used by the cursor control in text fields.
     *
     * The action includes a boolean argument, which indicates whether the cursor
     * movement should extend (or start) a selection.
     */
    moveCursorBackwardByCharacter(_kMoveCursorBackwardByCharacterIndex),

    /**
     * Set the text selection to the given range.
     *
     * The provided argument is a Map<String, int> which includes the keys `base`
     * and `extent` indicating where the selection within the `value` of the
     * semantics node should start and where it should end. Values for both
     * keys can range from 0 to length of `value` (inclusive).
     *
     * Setting `base` and `extent` to the same value will move the cursor to
     * that position (without selecting anything).
     */
    setSelection(_kSetSelectionIndex),

    /** Copy the current selection to the clipboard. */
    copy(_kCopyIndex),

    /** Cut the current selection and place it in the clipboard. */
    cut(_kCutIndex),

    /** Paste the current content of the clipboard. */
    paste(_kPasteIndex),

    /**
     * Indicates that the nodes has gained accessibility focus.
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
     */
    didGainAccessibilityFocus(_kDidGainAccessibilityFocusIndex),

    /**
     * Indicates that the nodes has lost accessibility focus.
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
     */
    didLoseAccessibilityFocus(_kDidLoseAccessibilityFocusIndex),

    /**
     * Indicates that the user has invoked a custom accessibility action.
     *
     * This handler is added automatically whenever a custom accessibility
     * action is added to a semantics node.
     */
    customAction(_kCustomAction),

    /**
     * A request that the node should be dismissed.
     *
     * A [Snackbar], for example, may have a dismiss action to indicate to the
     * user that it can be removed after it is no longer relevant. On Android,
     * (with TalkBack) special hint text is spoken when focusing the node and
     * a custom action is availible in the local context menu. On iOS,
     * (with VoiceOver) users can perform a standard gesture to dismiss it.
     */
    dismiss(_kDismissIndex),

    /**
     * Move the cursor forward by one word.
     *
     * This is for example used by the cursor control in text fields.
     *
     * The action includes a boolean argument, which indicates whether the cursor
     * movement should extend (or start) a selection.
     */
    moveCursorForwardByWord(_kMoveCursorForwardByWordIndex),

    /**
     * Move the cursor backward by one word.
     *
     * This is for example used by the cursor control in text fields.
     *
     * The action includes a boolean argument, which indicates whether the cursor
     * movement should extend (or start) a selection.
     */
    moveCursorBackwardByWord(_kMoveCursorBackwardByWordIndex);

    companion object {
        /**
         * The possible semantics actions.
         *
         * The map's key is the [index] of the action and the value is the action
         * itself.
         */
        val values: Map<Int, SemanticsAction> = mapOf(
                _kTapIndex to tap,
                _kLongPressIndex to longPress,
                _kScrollLeftIndex to scrollLeft,
                _kScrollRightIndex to scrollRight,
                _kScrollUpIndex to scrollUp,
                _kScrollDownIndex to scrollDown,
                _kIncreaseIndex to increase,
                _kDecreaseIndex to decrease,
                _kShowOnScreenIndex to showOnScreen,
                _kMoveCursorForwardByCharacterIndex to moveCursorForwardByCharacter,
                _kMoveCursorBackwardByCharacterIndex to moveCursorBackwardByCharacter,
                _kSetSelectionIndex to setSelection,
                _kCopyIndex to copy,
                _kCutIndex to cut,
                _kPasteIndex to paste,
                _kDidGainAccessibilityFocusIndex to didGainAccessibilityFocus,
                _kDidLoseAccessibilityFocusIndex to didLoseAccessibilityFocus,
                _kCustomAction to customAction,
                _kDismissIndex to dismiss,
                _kMoveCursorForwardByWordIndex to moveCursorForwardByWord,
                _kMoveCursorBackwardByWordIndex to moveCursorBackwardByWord
        )
    }

    override fun toString(): String {
        return "SemanticsAction.$name"
    }
}