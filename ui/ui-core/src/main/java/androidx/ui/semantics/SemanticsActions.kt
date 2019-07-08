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

package androidx.ui.core.semantics

import androidx.ui.text.TextSelection

private const val INDEX_TAP = 1 shl 0
private const val INDEX_LONG_PRESS = 1 shl 1
private const val INDEX_SCROLL_LEFT = 1 shl 2
private const val INDEX_SCROLL_RIGHT = 1 shl 3
private const val INDEX_SCROLL_UP = 1 shl 4
private const val INDEX_SCROLL_DOWN = 1 shl 5
private const val INDEX_INCREASE = 1 shl 6
private const val INDEX_DECREASE = 1 shl 7
private const val INDEX_SHOW_ON_SCREEN = 1 shl 8
private const val INDEX_MOVE_CURSOR_FORWARD_BY_CHARACTER = 1 shl 9
private const val INDEX_MOVE_CURSOR_BACKWARD_BY_CHARACTER = 1 shl 10
private const val INDEX_SET_SELECTION = 1 shl 11
private const val INDEX_COPY = 1 shl 12
private const val INDEX_CUT = 1 shl 13
private const val INDEX_PASTE = 1 shl 14
private const val INDEX_DID_GAIN_ACCESSIBILITY_FOCUS = 1 shl 15
private const val INDEX_DID_LOSE_ACCESSIBILITY_FOCUS = 1 shl 16
private const val INDEX_CUSTOM_ACTION = 1 shl 17
private const val INDEX_DISMISS = 1 shl 18
private const val INDEX_MOVE_CURSOR_FORWARD_BY_WORD = 1 shl 19
private const val INDEX_MOVE_CURSOR_BACKWARD_BY_WORD = 1 shl 20

private typealias VoidCallback = () -> Unit

/**
 * Signature for [SemanticsActionType]s that move the cursor.
 *
 * If `extendSelection` is set to true the cursor movement should extend the
 * current selection or (if nothing is currently selected) start a selection.
 */
typealias MoveCursorHandler = (extendSelection: Boolean) -> Unit

/**
 * Signature for the [SemanticsActionType.SetSelection] handlers to change the
 * text selection (or re-position the cursor) to `selection`.
 */
typealias SetSelectionHandler = (selection: TextSelection) -> Unit

data class SemanticsAction<T : Any>(val type: SemanticsActionType<T>, val handler: T) {
    fun invokeHandler(args: Any?) {
        @Suppress("UNCHECKED_CAST")
        when (type.numArguments) {
            0 -> (handler as VoidCallback)()
            1 -> (handler as (Any?) -> Unit)(args)
            else -> throw IllegalStateException("Invalid number of arguments: ${type.numArguments}")
        }
    }
}

/**
 * The possible actions that can be conveyed from the operating system
 * accessibility APIs to a semantics node.
 */
class SemanticsActionType<T> private constructor(
    /**
     * The name of the action.
     */
    private val name: String,
    /**
     * The numerical value for this action.
     *
     * Each action has one bit set in this bit field.
     */
    // TODO(ryanmentley): this should be internal, but can't because we need it from other packages
    val bitmask: Int,
    internal val numArguments: Int
) {
    companion object {
        /**
         * The equivalent of a user briefly tapping the screen with the finger
         * without moving it.
         */
        val Tap = SemanticsActionType<VoidCallback>("Tap", INDEX_TAP, 0)

        /**
         * The equivalent of a user pressing and holding the screen with the finger
         * for a few seconds without moving it.
         */
        val LongPress = SemanticsActionType<VoidCallback>("LongPress", INDEX_LONG_PRESS, 0)

        /**
         * The equivalent of a user moving their finger across the screen from right
         * to left.
         *
         * This action should be recognized by controls that are horizontally
         * scrollable.
         */
        val ScrollLeft = SemanticsActionType<VoidCallback>("ScrollLeft", INDEX_SCROLL_LEFT, 0)

        /**
         * The equivalent of a user moving their finger across the screen from left
         * to right.
         *
         * This action should be recognized by controls that are horizontally
         * scrollable.
         */
        val ScrollRight = SemanticsActionType<VoidCallback>("ScrollRight", INDEX_SCROLL_RIGHT, 0)

        /**
         * The equivalent of a user moving their finger across the screen from
         * bottom to top.
         *
         * This action should be recognized by controls that are vertically
         * scrollable.
         */
        val ScrollUp = SemanticsActionType<VoidCallback>("ScrollUp", INDEX_SCROLL_UP, 0)

        /**
         * The equivalent of a user moving their finger across the screen from top
         * to bottom.
         *
         * This action should be recognized by controls that are vertically
         * scrollable.
         */
        val ScrollDown = SemanticsActionType<VoidCallback>("ScrollDown", INDEX_SCROLL_DOWN, 0)

        /**
         * A request to increase the value represented by the semantics node.
         *
         * For example, this action might be recognized by a slider control.
         */
        val Increase = SemanticsActionType<VoidCallback>("Increase", INDEX_INCREASE, 0)

        /**
         * A request to decrease the value represented by the semantics node.
         *
         * For example, this action might be recognized by a slider control.
         */
        val Decrease = SemanticsActionType<VoidCallback>("Decrease", INDEX_DECREASE, 0)

        /**
         * A request to fully show the semantics node on screen.
         *
         * For example, this action might be send to a node in a scrollable list that
         * is partially off screen to bring it on screen.
         */
        val ShowOnScreen = SemanticsActionType<VoidCallback>(
            "ShowOnScreen", INDEX_SHOW_ON_SCREEN, 0
        )

        /**
         * Move the cursor forward by one character.
         *
         * This is for example used by the cursor control in text fields.
         *
         * The action includes a boolean argument, which indicates whether the cursor
         * movement should extend (or start) a selection.
         */
        val MoveCursorForwardByCharacter = SemanticsActionType<MoveCursorHandler>(
            "MoveCursorForwardByCharacter", INDEX_MOVE_CURSOR_FORWARD_BY_CHARACTER, 1
        )

        /**
         * Move the cursor backward by one character.
         *
         * This is for example used by the cursor control in text fields.
         *
         * The action includes a boolean argument, which indicates whether the cursor
         * movement should extend (or start) a selection.
         */
        val MoveCursorBackwardByCharacter = SemanticsActionType<MoveCursorHandler>(
            "MoveCursorBackwardByCharacter", INDEX_MOVE_CURSOR_BACKWARD_BY_CHARACTER, 1
        )

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
        val SetSelection = SemanticsActionType<SetSelectionHandler>(
            "SetSelection", INDEX_SET_SELECTION, 1
        )

        /** Copy the current selection to the clipboard. */
        val Copy = SemanticsActionType<VoidCallback>("Copy", INDEX_COPY, 0)

        /** Cut the current selection and place it in the clipboard. */
        val Cut = SemanticsActionType<VoidCallback>("Cut", INDEX_CUT, 0)

        /** Paste the current content of the clipboard. */
        val Paste = SemanticsActionType<VoidCallback>("Paste", INDEX_PASTE, 0)

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
        val DidGainAccessibilityFocus = SemanticsActionType<VoidCallback>(
            "DidGainAccessibilityFocus", INDEX_DID_GAIN_ACCESSIBILITY_FOCUS, 0
        )

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
        val DidLoseAccessibilityFocus = SemanticsActionType<VoidCallback>(
            "DidLoseAccessibilityFocus", INDEX_DID_LOSE_ACCESSIBILITY_FOCUS, 0
        )

        /**
         * Indicates that the user has invoked a custom accessibility action.
         *
         * This handler is added automatically whenever a custom accessibility
         * action is added to a semantics node.
         */
        // TODO(ryanmentley): this needs parameters
        val CustomAction = SemanticsActionType<VoidCallback>(
            "CustomAction", INDEX_CUSTOM_ACTION, 0
        )

        /**
         * A request that the node should be dismissed.
         *
         * A [Snackbar], for example, may have a dismiss action to indicate to the
         * user that it can be removed after it is no longer relevant. On Android,
         * (with TalkBack) special hint text is spoken when focusing the node and
         * a custom action is availible in the local context menu. On iOS,
         * (with VoiceOver) users can perform a standard gesture to dismiss it.
         */
        val Dismiss = SemanticsActionType<VoidCallback>("Dismiss", INDEX_DISMISS, 0)

        /**
         * Move the cursor forward by one word.
         *
         * This is for example used by the cursor control in text fields.
         *
         * The action includes a boolean argument, which indicates whether the cursor
         * movement should extend (or start) a selection.
         */
        val MoveCursorForwardByWord = SemanticsActionType<MoveCursorHandler>(
            "MoveCursorForwardByWord", INDEX_MOVE_CURSOR_FORWARD_BY_WORD, 1
        )

        /**
         * Move the cursor backward by one word.
         *
         * This is for example used by the cursor control in text fields.
         *
         * The action includes a boolean argument, which indicates whether the cursor
         * movement should extend (or start) a selection.
         */
        val MoveCursorBackwardByWord = SemanticsActionType<MoveCursorHandler>(
            "MoveCursorBackwardByWord", INDEX_MOVE_CURSOR_BACKWARD_BY_WORD, 1
        )

        /**
         * The possible semantics actions.
         *
         * The map's key is the [bitmask] of the action and the value is the action
         * itself.
         */
        // TODO(ryanmentley): maybe unneeded, remove if not needed in final impl
        private val values: Map<Int, SemanticsActionType<*>> = mapOf(
            INDEX_TAP to Tap,
            INDEX_LONG_PRESS to LongPress,
            INDEX_SCROLL_LEFT to ScrollLeft,
            INDEX_SCROLL_RIGHT to ScrollRight,
            INDEX_SCROLL_UP to ScrollUp,
            INDEX_SCROLL_DOWN to ScrollDown,
            INDEX_INCREASE to Increase,
            INDEX_DECREASE to Decrease,
            INDEX_SHOW_ON_SCREEN to ShowOnScreen,
            INDEX_MOVE_CURSOR_FORWARD_BY_CHARACTER to MoveCursorForwardByCharacter,
            INDEX_MOVE_CURSOR_BACKWARD_BY_CHARACTER to MoveCursorBackwardByCharacter,
            INDEX_SET_SELECTION to SetSelection,
            INDEX_COPY to Copy,
            INDEX_CUT to Cut,
            INDEX_PASTE to Paste,
            INDEX_DID_GAIN_ACCESSIBILITY_FOCUS to DidGainAccessibilityFocus,
            INDEX_DID_LOSE_ACCESSIBILITY_FOCUS to DidLoseAccessibilityFocus,
            INDEX_CUSTOM_ACTION to CustomAction,
            INDEX_DISMISS to Dismiss,
            INDEX_MOVE_CURSOR_FORWARD_BY_WORD to MoveCursorForwardByWord,
            INDEX_MOVE_CURSOR_BACKWARD_BY_WORD to MoveCursorBackwardByWord
        )
    }

    override fun toString(): String {
        return "SemanticsActionType.$name"
    }
}