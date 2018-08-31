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

package androidx.ui.semantics

private const val _kHasCheckedStateIndex = 1 shl 0
private const val _kIsCheckedIndex = 1 shl 1
private const val _kIsSelectedIndex = 1 shl 2
private const val _kIsButtonIndex = 1 shl 3
private const val _kIsTextFieldIndex = 1 shl 4
private const val _kIsFocusedIndex = 1 shl 5
private const val _kHasEnabledStateIndex = 1 shl 6
private const val _kIsEnabledIndex = 1 shl 7
private const val _kIsInMutuallyExclusiveGroupIndex = 1 shl 8
private const val _kIsHeaderIndex = 1 shl 9
private const val _kIsObscuredIndex = 1 shl 10
private const val _kScopesRouteIndex = 1 shl 11
private const val _kNamesRouteIndex = 1 shl 12
private const val _kIsHiddenIndex = 1 shl 13
private const val _kIsImageIndex = 1 shl 14
private const val _kIsLiveRegionIndex = 1 shl 15
private const val _kHasToggledStateIndex = 1 shl 16
private const val _kIsToggledIndex = 1 shl 17
private const val _kHasImplicitScrollingIndex = 1 shl 18

// / A Boolean value that can be associated with a semantics node.
enum class SemanticsFlag(
    // / The numerical value for this flag.
    // /
    // / Each flag has one bit set in this bit field.
    val index: Int
) {
    // / The semantics node has the quality of either being "checked" or "unchecked".
    // /
    // / This flag is mutually exclusive with [hasToggledState].
    // /
    // / For example, a checkbox or a radio button widget has checked state.
    // /
    // / See also:
    // /
    // /   * [SemanticsFlag.isChecked], which controls whether the node is "checked" or "unchecked".
    hasCheckedState(_kHasCheckedStateIndex),

    // / Whether a semantics node that [hasCheckedState] is checked.
    // /
    // / If true, the semantics node is "checked". If false, the semantics node is
    // / "unchecked".
    // /
    // / For example, if a checkbox has a visible checkmark, [isChecked] is true.
    // /
    // / See also:
    // /
    // /   * [SemanticsFlag.hasCheckedState], which enables a checked state.
    isChecked(_kIsCheckedIndex),

    // / Whether a semantics node is selected.
    // /
    // / If true, the semantics node is "selected". If false, the semantics node is
    // / "unselected".
    // /
    // / For example, the active tab in a tab bar has [isSelected] set to true.
    isSelected(_kIsSelectedIndex),

    // / Whether the semantic node represents a button.
    // /
    // / Platforms has special handling for buttons, for example Android's TalkBack
    // / and iOS's VoiceOver provides an additional hint when the focused object is
    // / a button.
    isButton(_kIsButtonIndex),

    // / Whether the semantic node represents a text field.
    // /
    // / Text fields are announced as such and allow text input via accessibility
    // / affordances.
    isTextField(_kIsTextFieldIndex),

    // / Whether the semantic node currently holds the user's focus.
    // /
    // / The focused element is usually the current receiver of keyboard inputs.
    isFocused(_kIsFocusedIndex),

    // / The semantics node has the quality of either being "enabled" or
    // / "disabled".
    // /
    // / For example, a button can be enabled or disabled and therefore has an
    // / "enabled" state. Static text is usually neither enabled nor disabled and
    // / therefore does not have an "enabled" state.
    hasEnabledState(_kHasEnabledStateIndex),

    // / Whether a semantic node that [hasEnabledState] is currently enabled.
    // /
    // / A disabled element does not respond to user interaction. For example, a
    // / button that currently does not respond to user interaction should be
    // / marked as disabled.
    isEnabled(_kIsEnabledIndex),

    // / Whether a semantic node is in a mutually exclusive group.
    // /
    // / For example, a radio button is in a mutually exclusive group because
    // / only one radio button in that group can be marked as [isChecked].
    isInMutuallyExclusiveGroup(_kIsInMutuallyExclusiveGroupIndex),

    // / Whether a semantic node is a header that divides content into sections.
    // /
    // / For example, headers can be used to divide a list of alphabetically
    // / sorted words into the sections A, B, C, etc. as can be found in many
    // / address book applications.
    isHeader(_kIsHeaderIndex),

    // / Whether the value of the semantics node is obscured.
    // /
    // / This is usually used for text fields to indicate that its content
    // / is a password or contains other sensitive information.
    isObscured(_kIsObscuredIndex),

    // / Whether the semantics node is the root of a subtree for which a route name
    // / should be announced.
    // /
    // / When a node with this flag is removed from the semantics tree, the
    // / framework will select the last in depth-first, paint order node with this
    // / flag.  When a node with this flag is added to the semantics tree, it is
    // / selected automatically, unless there were multiple nodes with this flag
    // / added.  In this case, the last added node in depth-first, paint order
    // / will be selected.
    // /
    // / From this selected node, the framework will search in depth-first, paint
    // / order for the first node with a [namesRoute] flag and a non-null,
    // / non-empty label. The [namesRoute] and [scopesRoute] flags may be on the
    // / same node. The label of the found node will be announced as an edge
    // / transition. If no non-empty, non-null label is found then:
    // /
    // /   * VoiceOver will make a chime announcement.
    // /   * TalkBack will make no announcement
    // /
    // / Semantic nodes annotated with this flag are generally not a11y focusable.
    // /
    // / This is used in widgets such as Routes, Drawers, and Dialogs to
    // / communicate significant changes in the visible screen.
    scopesRoute(_kScopesRouteIndex),

    // / Whether the semantics node label is the name of a visually distinct
    // / route.
    // /
    // / This is used by certain widgets like Drawers and Dialogs, to indicate
    // / that the node's semantic label can be used to announce an edge triggered
    // / semantics update.
    // /
    // / Semantic nodes annotated with this flag will still recieve a11y focus.
    // /
    // / Updating this label within the same active route subtree will not cause
    // / additional announcements.
    namesRoute(_kNamesRouteIndex),

    // / Whether the semantics node is considered hidden.
    // /
    // / Hidden elements are currently not visible on screen. They may be covered
    // / by other elements or positioned outside of the visible area of a viewport.
    // /
    // / Hidden elements cannot gain accessibility focus though regular touch. The
    // / only way they can be focused is by moving the focus to them via linear
    // / navigation.
    // /
    // / Platforms are free to completely ignore hidden elements and new platforms
    // / are encouraged to do so.
    // /
    // / Instead of marking an element as hidden it should usually be excluded from
    // / the semantics tree altogether. Hidden elements are only included in the
    // / semantics tree to work around platform limitations and they are mainly
    // / used to implement accessibility scrolling on iOS.
    isHidden(_kIsHiddenIndex),

    // / Whether the semantics node represents an image.
    // /
    // / Both TalkBack and VoiceOver will inform the user the the semantics node
    // / represents an image.
    isImage(_kIsImageIndex),

    // / Whether the semantics node is a live region.
    // /
    // / A live region indicates that updates to semantics node are important.
    // / Platforms may use this information to make polite announcements to the
    // / user to inform them of updates to this node.
    // /
    // / An example of a live region is a [SnackBar] widget. On Android, A live
    // / region causes a polite announcement to be generated automatically, even
    // / if the user does not have focus of the widget.
    isLiveRegion(_kIsLiveRegionIndex),

    // / The semantics node has the quality of either being "on" or "off".
    // /
    // / This flag is mutually exclusive with [hasCheckedState].
    // /
    // / For example, a switch has toggled state.
    // /
    // / See also:
    // /
    // /    * [SemanticsFlag.isToggled], which controls whether the node is "on" or "off".
    hasToggledState(_kHasToggledStateIndex),

    // / If true, the semantics node is "on". If false, the semantics node is
    // / "off".
    // /
    // / For example, if a switch is in the on position, [isToggled] is true.
    // /
    // / See also:
    // /
    // /   * [SemanticsFlag.hasToggledState], which enables a toggled state.
    isToggled(_kIsToggledIndex),

    // / Whether the platform can scroll the semantics node when the user attempts
    // / to move focus to an offscreen child.
    // /
    // / For example, a [ListView] widget has implicit scrolling so that users can
    // / easily move to the next visible set of children. A [TabBar] widget does
    // / not have implicit scrolling, so that users can navigate into the tab
    // / body when reaching the end of the tab bar.
    hasImplicitScrolling(_kHasImplicitScrollingIndex);

    companion object {
        // / The possible semantics flags.
        // /
        // / The map's key is the [index] of the flag and the value is the flag itself.
        val values: Map<Int, SemanticsFlag> = mapOf(
            _kHasCheckedStateIndex to hasCheckedState,
            _kIsCheckedIndex to isChecked,
            _kIsSelectedIndex to isSelected,
            _kIsButtonIndex to isButton,
            _kIsTextFieldIndex to isTextField,
            _kIsFocusedIndex to isFocused,
            _kHasEnabledStateIndex to hasEnabledState,
            _kIsEnabledIndex to isEnabled,
            _kIsInMutuallyExclusiveGroupIndex to isInMutuallyExclusiveGroup,
            _kIsHeaderIndex to isHeader,
            _kIsObscuredIndex to isObscured,
            _kScopesRouteIndex to scopesRoute,
            _kNamesRouteIndex to namesRoute,
            _kIsHiddenIndex to isHidden,
            _kIsImageIndex to isImage,
            _kIsLiveRegionIndex to isLiveRegion,
            _kHasToggledStateIndex to hasToggledState,
            _kIsToggledIndex to isToggled,
            _kHasImplicitScrollingIndex to hasImplicitScrolling
        )
    }

    override fun toString(): String {
        return "SemanticsFlag.$name"
    }
}