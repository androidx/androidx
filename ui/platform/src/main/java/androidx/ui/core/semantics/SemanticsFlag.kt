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

// TODO(ryanmentley): b/129900044: Could this be an inline class and eliminate a lot of the
// properties on SemanticsConfiguration?
/** A Boolean value that can be associated with a [SemanticsNode]. */
enum class SemanticsFlag {
    /**
     * The semantics node has the quality of either being "checked" or "unchecked".
     *
     * This flag is mutually exclusive with [HasToggledState].
     *
     * For example, a checkbox or a radio button widget has checked state.
     *
     * See also:
     *
     *   * [SemanticsFlag.IsChecked], which controls whether the node is "checked" or "unchecked".
     */
    HasCheckedState,

    /**
     * Whether a semantics node that [HasCheckedState] is checked.
     *
     * If true, the semantics node is "checked". If false, the semantics node is
     * "unchecked".
     *
     * For example, if a checkbox has a visible checkmark, [IsChecked] is true.
     *
     * See also:
     *
     *   * [SemanticsFlag.HasCheckedState], which enables a checked state.
     */
    IsChecked,

    /**
     * Whether a semantics node is selected.
     *
     * If true, the semantics node is "selected". If false, the semantics node is
     * "unselected".
     *
     * For example, the active tab in a tab bar has [IsSelected] set to true.
     */
    IsSelected,

    /**
     * Whether the semantic node represents a button.
     *
     * Some platforms have special handling for buttons. For example, iOS's VoiceOver provides an
     * additional hint when the focused object is a button.
     */
    // TODO(ryanmentley): Should we remove this?
    IsButton,

    /**
     * Whether the semantic node represents a text field.
     *
     * Text fields are announced as such and allow text input via accessibility
     * affordances.
     */
    IsTextField,

    /**
     * Whether the semantic node currently holds the user's focus.
     *
     * The focused element is usually the current receiver of keyboard inputs.
     */
    IsFocused,

    /**
     * The semantics node has the quality of either being "enabled" or
     * "disabled".
     *
     * For example, a button can be enabled or disabled and therefore has an
     * "enabled" state. Static text is usually neither enabled nor disabled and
     * therefore does not have an "enabled" state.
     */
    HasEnabledState,

    /**
     * Whether a semantic node that [HasEnabledState] is currently enabled.
     *
     * A disabled element does not respond to user interaction. For example, a
     * button that currently does not respond to user interaction should be
     * marked as disabled.
     */
    IsEnabled,

    /**
     * Whether a semantic node is in a mutually exclusive group.
     *
     * For example, a radio button is in a mutually exclusive group because
     * only one radio button in that group can be marked as [IsChecked].
     */
    IsInMutuallyExclusiveGroup,

    /**
     * Whether a semantic node is a header that divides content into sections.
     *
     * For example, headers can be used to divide a list of alphabetically
     * sorted words into the sections A, B, C, etc. as can be found in many
     * address book applications.
     */
    IsHeader,

    /**
     * Whether the value of the semantics node is obscured.
     *
     * This is usually used for text fields to indicate that its content
     * is a password or contains other sensitive information.
     */
    IsObscured,

    /**
     * Whether the semantics node is the root of a subtree for which a route name
     * should be announced.
     *
     * When a node with this flag is removed from the semantics tree, the
     * framework will select the last in depth-first, paint order node with this
     * flag.  When a node with this flag is added to the semantics tree, it is
     * selected automatically, unless there were multiple nodes with this flag
     * added.  In this case, the last added node in depth-first, paint order
     * will be selected.
     *
     * From this selected node, the framework will search in depth-first, paint
     * order for the first node with a [NamesRoute] flag and a non-null,
     * non-empty label. The [NamesRoute] and [ScopesRoute] flags may be on the
     * same node. The label of the found node will be announced as an edge
     * transition. If no non-empty, non-null label is found then:
     *
     *   * VoiceOver will make a chime announcement.
     *   * TalkBack will make no announcement
     *
     * Semantic nodes annotated with this flag are generally not a11y focusable.
     *
     * This is used in widgets such as Routes, Drawers, and Dialogs to
     * communicate significant changes in the visible screen.
     */
    ScopesRoute,

    /**
     * Whether the semantics node label is the name of a visually distinct
     * route.
     *
     * This is used by certain widgets like Drawers and Dialogs, to indicate
     * that the node's semantic label can be used to announce an edge triggered
     * semantics update.
     *
     * Semantic nodes annotated with this flag will still recieve a11y focus.
     *
     * Updating this label within the same active route subtree will not cause
     * additional announcements.
     */
    NamesRoute,

    /**
     * Whether the semantics node is considered hidden.
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
    IsHidden,

    /**
     * Whether the semantics node represents an image.
     *
     * Both TalkBack and VoiceOver will inform the user the the semantics node
     * represents an image.
     */
    IsImage,

    /**
     * Whether the semantics node is a live region.
     *
     * A live region indicates that updates to semantics node are important.
     * Platforms may use this information to make polite announcements to the
     * user to inform them of updates to this node.
     *
     * An example of a live region is a [SnackBar] widget. On Android, A live
     * region causes a polite announcement to be generated automatically, even
     * if the user does not have focus of the widget.
     */
    IsLiveRegion,

    /**
     * The semantics node has the quality of either being "on" or "off".
     *
     * This flag is mutually exclusive with [hasCheckedState].
     *
     * For example, a switch has toggled state.
     *
     * See also:
     *
     *    * [SemanticsFlag.isToggled], which controls whether the node is "on" or "off".
     */
    HasToggledState,

    /**
     * If true, the semantics node is "on". If false, the semantics node is
     * "off".
     *
     * For example, if a switch is in the on position, [IsToggled] is true.
     *
     * See also:
     *
     *   * [SemanticsFlag.HasToggledState], which enables a toggled state.
     */
    IsToggled,

    /**
     * Whether the platform can scroll the semantics node when the user attempts
     * to move focus to an offscreen child.
     *
     * For example, a [ListView] widget has implicit scrolling so that users can
     * easily move to the next visible set of children. A [TabBar] widget does
     * not have implicit scrolling, so that users can navigate into the tab
     * body when reaching the end of the tab bar.
     */
    HasImplicitScrolling;

    companion object {

        /**
         * The possible semantics flags.
         *
         * The map's key is the [bitmask] of the flag and the value is the flag itself.
         */
        val values: Map<Int, SemanticsFlag>

        init {
            this.values = mutableMapOf()
            for (flag in SemanticsFlag.values()) {
                values[flag.bitmask] = flag
            }
        }
    }

    /**
     * The numerical value for this flag.
     *
     * Each flag has one bit set in this bit field.
     */
    internal val bitmask: Int
        get() = 1 shl ordinal

    override fun toString(): String {
        return "SemanticsFlag.$name"
    }
}