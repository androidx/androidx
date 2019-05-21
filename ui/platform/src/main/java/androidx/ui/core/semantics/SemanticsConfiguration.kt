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

import androidx.ui.core.Unicode
import androidx.ui.engine.text.TextDirection
import androidx.ui.services.text_editing.TextSelection
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Describes the semantic information associated with the owning component
 *
 * The information provided in the configuration is used to to generate the
 * semantics tree.
 */
class SemanticsConfiguration {
    // SEMANTIC BOUNDARY BEHAVIOR

    /**
     * Whether the owner of this configuration wants to own its
     * own [SemanticsNode].
     *
     * When set to true semantic information associated with the
     * owner of this configuration or any of its descendants will not leak into
     * parents. The [SemanticsNode] generated out of this configuration will
     * act as a boundary.
     *
     * Whether descendants of the owning component can add their semantic
     * information to the [SemanticsNode] introduced by this configuration
     * is controlled by [explicitChildNodes].
     *
     * This has to be true if [isMergingSemanticsOfDescendants] is also true.
     */
    var isSemanticBoundary: Boolean = false
        set(value) {
            assert(!isMergingSemanticsOfDescendants || value)
            field = value
        }

    /**
     * Whether the configuration forces all children of the owning component
     * that want to contribute semantic information to the semantics tree to do
     * so in the form of explicit [SemanticsNode]s.
     *
     * When set to false children of the owning component are allowed to
     * annotate [SemanticNode]s of their parent with the semantic information
     * they want to contribute to the semantic tree.
     * When set to true the only way for children of the owning component
     * to contribute semantic information to the semantic tree is to introduce
     * new explicit [SemanticNode]s to the tree.
     *
     * This setting is often used in combination with [isSemanticBoundary] to
     * create semantic boundaries that are either writable or not for children.
     */
    var explicitChildNodes = false

    /**
     * Whether the owning component makes other components previously
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
    // These will end up on [SemanticNode]s generated from [SemanticsConfiguration]s.

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
    internal var _actions: MutableMap<SemanticsActionType<*>,
            SemanticsAction<*>> = mutableMapOf()

    var actions: List<SemanticsAction<*>>
        get() = _actions.values.toList()
        set(value) {
            // TODO(ryanmentley): This is naive, we could be smarter
            actionsAsBits = 0
            _actions.clear()
            for (action in value) {
                addAction(action)
            }
        }

    private var actionsAsBits: Int = 0

    /**
     * Adds an `action` to the semantics tree.
     *
     * The provided `handler` is called to respond to the user triggered
     * `action`.
     */
    private fun addAction(action: SemanticsAction<*>) {
        _actions[action.type] = action
        actionsAsBits = actionsAsBits or action.type.bitmask
        hasBeenAnnotated = true
    }

    /**
     * A property that marks the configuration as having been annotated
     * (i.e., containing information)
     */
    private class AnnotationProperty<T>(initialValue: T) :
        ReadWriteProperty<SemanticsConfiguration, T> {
        private var value = initialValue

        override fun getValue(thisRef: SemanticsConfiguration, property: KProperty<*>): T {
            return value
        }

        override fun setValue(thisRef: SemanticsConfiguration, property: KProperty<*>, value: T) {
            this.value = value
            thisRef.hasBeenAnnotated = true
        }
    }

    /**
     * Whether the semantic information provided by the owning component and
     * all of its descendants should be treated as one logical entity.
     *
     * If set to true, the descendants of the owning component's
     * [SemanticsNode] will merge their semantic information into the
     * [SemanticsNode] representing the owning component.
     *
     * Setting this to true requires that [isSemanticBoundary] is also true.
     */
    var isMergingSemanticsOfDescendants: Boolean = false
        set(value) {
            // TODO(Migration/ryanmentley): Changed this, confirm it's correct
            if (value) {
                assert(isSemanticBoundary)
            }

            field = value
            hasBeenAnnotated = true
        }

    var label: String? by AnnotationProperty("")

    var value: String? by AnnotationProperty("")

    // TODO(ryanmentley): Added in API 26 and backported via androidx, integrate this
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
    var hint: String? by AnnotationProperty("")

    /**
     * Provides hint values which override the default hints on supported
     * platforms.
     */
    var hintOverrides: SemanticsHintOverrides? by AnnotationProperty(null)

    /**
     * Whether the semantics node is the root of a subtree for which values
     * should be announced.
     *
     * See also:
     *  * [SemanticsFlag.ScopesRoute], for a full description of route scoping.
     */
    var scopesRoute: Boolean by SimpleFlagProperty(SemanticsFlag.ScopesRoute)

    /**
     * Whether the semantics node contains the label of a route.
     *
     * See also:
     *  * [SemanticsFlag.NamesRoute], for a full description of route naming.
     */
    var namesRoute: Boolean by SimpleFlagProperty(SemanticsFlag.NamesRoute)

    /** Whether the semantics node represents an image. */
    var isImage: Boolean by SimpleFlagProperty(SemanticsFlag.IsImage)

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
     *   * [SemanticsFlag.IsLiveRegion], the semantics flag that this setting controls.
     */
    var liveRegion: Boolean by SimpleFlagProperty(SemanticsFlag.IsLiveRegion)

    var textDirection: TextDirection? by AnnotationProperty(null)

    var isSelected: Boolean by SimpleFlagProperty(
        SemanticsFlag.IsSelected
    )

    var isEnabled: Boolean?
        get() {
            return if (hasFlag(SemanticsFlag.HasEnabledState)) {
                hasFlag(SemanticsFlag.IsEnabled)
            } else {
                null
            }
        }
        set(value) {
            setFlag(SemanticsFlag.HasEnabledState, value != null)
            setFlag(SemanticsFlag.IsEnabled, value ?: false)
        }

    var isChecked: Boolean?
        get() {
            return if (hasFlag(SemanticsFlag.HasCheckedState)) {
                hasFlag(SemanticsFlag.IsChecked)
            } else {
                null
            }
        }
        set(value) {
            setFlag(SemanticsFlag.HasCheckedState, value != null)
            setFlag(SemanticsFlag.IsChecked, value ?: false)
        }

    /**
     * If this node has Boolean state that can be controlled by the user, whether
     * that state is on or off, corresponding to true and false, respectively.
     *
     * Do not call the setter for this field if the owning component doesn't
     * have on/off state that can be controlled by the user.
     *
     * The getter returns null if the owning component does not have
     * on/off state.
     */
    var isToggled: Boolean?
        get() {
            return if (hasFlag(SemanticsFlag.HasToggledState)) {
                hasFlag(SemanticsFlag.IsToggled)
            } else {
                null
            }
        }
        set(value) {
            setFlag(SemanticsFlag.HasToggledState, value != null)
            setFlag(SemanticsFlag.IsToggled, value ?: false)
        }

    var isInMutuallyExclusiveGroup: Boolean
            by SimpleFlagProperty(SemanticsFlag.IsInMutuallyExclusiveGroup)

    /** Whether the component represented by this configuration currently holds the user's focus. */
    var isFocused: Boolean by SimpleFlagProperty(SemanticsFlag.IsFocused)

    var isButton: Boolean by SimpleFlagProperty(SemanticsFlag.IsButton)

    /** Whether the component represented by this configuration is a header (true) or not (false). */
    var isHeader: Boolean by SimpleFlagProperty(SemanticsFlag.IsHeader)

    /**
     * Whether the the component represented by this configuration is considered hidden.
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
    var isHidden: Boolean by SimpleFlagProperty(SemanticsFlag.IsHidden)

    /** Whether the owning component is a text field. */
    var isTextField: Boolean by SimpleFlagProperty(SemanticsFlag.IsTextField)

    /**
     * Whether the [value] should be obscured.
     *
     * This option is usually set in combination with [isTextField] to indicate
     * that the text field contains a password (or other sensitive information).
     * Doing so instructs screen readers to not read out the [value].
     */
    var isObscured: Boolean by SimpleFlagProperty(SemanticsFlag.IsObscured)

    /**
     * Whether the platform can scroll the semantics node when the user attempts
     * to move focus to an offscreen child.
     *
     * For example, a [ListView] widget has implicit scrolling so that users can
     * easily move to the next visible set of children. A [TabBar] widget does
     * not have implicit scrolling, so that users can navigate into the tab
     * body when reaching the end of the tab bar.
     */
    var hasImplicitScrolling: Boolean by SimpleFlagProperty(SemanticsFlag.HasImplicitScrolling)

    /**
     * The currently selected text (or the position of the cursor) within [value]
     * if this node represents a text field.
     */
    var textSelection: TextSelection? by AnnotationProperty(null)

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
    var scrollPosition: Float? by AnnotationProperty(null)

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
    var scrollExtentMax: Float? by AnnotationProperty(null)

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
    var scrollExtentMin: Float? by AnnotationProperty(null)

    var testTag: String? by AnnotationProperty(null)

    // INTERNAL FLAG MANAGEMENT

    private var flags = 0

    private fun setFlag(flag: SemanticsFlag, value: Boolean) {
        flags = if (value) {
            flags or flag.bitmask
        } else {
            flags and flag.bitmask.inv()
        }
        hasBeenAnnotated = true
    }

    private fun hasFlag(flag: SemanticsFlag): Boolean = (flags and flag.bitmask) != 0

    /**
     * A property that provides an abstraction over [setFlag] and [hasFlag]
     */
    private class SimpleFlagProperty(
        val flag: SemanticsFlag
    ) : ReadWriteProperty<SemanticsConfiguration, Boolean> {
        override fun getValue(
            thisRef: SemanticsConfiguration,
            property: KProperty<*>
        ): Boolean {
            return thisRef.hasFlag(flag)
        }

        override fun setValue(
            thisRef: SemanticsConfiguration,
            property: KProperty<*>,
            value: Boolean
        ) {
            thisRef.setFlag(flag, value)
        }
    }

    // CONFIGURATION COMBINATION LOGIC

    /**
     * Whether this configuration is compatible with the provided `other` configuration.
     *
     * Two configurations are said to be compatible if they can be added to the same [SemanticsNode]
     * without losing any semantics information.
     */
    fun isCompatibleWith(other: SemanticsConfiguration?): Boolean {
        if (other == null || !other.hasBeenAnnotated || !hasBeenAnnotated) {
            return true
        }
        if (actionsAsBits and other.actionsAsBits != 0) {
            return false
        }
        if ((flags and other.flags) != 0) {
            return false
        }
        if (!value.isNullOrEmpty() && !value.isNullOrEmpty()) {
            return false
        }
        return true
    }

    /**
     * Absorb the semantic information from `other` into this configuration.
     *
     * This adds the semantic information of both configurations and saves the result in this
     * configuration.
     *
     * Only configurations that have [explicitChildNodes] set to false can absorb other
     * configurations.  The [other] configuration must be compatible as determined by
     * [isCompatibleWith].
     */
    internal fun absorb(other: SemanticsConfiguration) {
        assert(!explicitChildNodes)
        assert(isCompatibleWith((other)))

        if (!other.hasBeenAnnotated) {
            return
        }

        _actions.putAll(other._actions)
        actionsAsBits = actionsAsBits or other.actionsAsBits
        flags = flags or other.flags

        textSelection = textSelection ?: other.textSelection
        scrollPosition = scrollPosition ?: other.scrollPosition
        scrollExtentMax = scrollExtentMax ?: other.scrollExtentMax
        scrollExtentMin = scrollExtentMin ?: other.scrollExtentMin
        hintOverrides = hintOverrides ?: other.hintOverrides

        textDirection = textDirection ?: other.textDirection

        label = concatStrings(
            thisString = label,
            thisTextDirection = textDirection,
            otherString = other.label,
            otherTextDirection = other.textDirection
        )
        if (value.isNullOrEmpty()) {
            value = other.value
        }

        hint = concatStrings(
            thisString = hint,
            thisTextDirection = textDirection,
            otherString = other.hint,
            otherTextDirection = other.textDirection
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
        copy.isMergingSemanticsOfDescendants = isMergingSemanticsOfDescendants
        copy.textDirection = textDirection
        copy.label = label
        copy.value = value
        copy.hint = hint
        copy.hintOverrides = hintOverrides
        copy.flags = flags
        copy.textSelection = textSelection
        copy.scrollPosition = scrollPosition
        copy.scrollExtentMax = scrollExtentMax
        copy.scrollExtentMin = scrollExtentMin
        copy.actionsAsBits = actionsAsBits
        copy._actions.putAll(_actions)
        // Do this last so it's not overwritten by setting the other properties
        copy.hasBeenAnnotated = hasBeenAnnotated
        return copy
    }

    /**
     * Checks that all properties are the same and that the set of actions (though not necessarily
     * the exact functions implementing them) are the same
     */
    internal fun isSemanticallyDifferentFrom(other: SemanticsConfiguration): Boolean {
        return this.label != other.label ||
                this.hint != other.hint ||
                this.value != other.value ||
                this.flags != other.flags ||
                this.textDirection != other.textDirection ||
                this.textSelection != other.textSelection ||
                this.scrollPosition != other.scrollPosition ||
                this.scrollExtentMax != other.scrollExtentMax ||
                this.scrollExtentMin != other.scrollExtentMin ||
                this.actionsAsBits != other.actionsAsBits
    }

    internal fun updateWith(sourceConfig: SemanticsConfiguration) {
        this.label = sourceConfig.label
        this.value = sourceConfig.value
        this.hint = sourceConfig.hint
        this.flags = sourceConfig.flags
        this.textDirection = sourceConfig.textDirection
        this._actions = sourceConfig._actions
        this.actionsAsBits = sourceConfig.actionsAsBits
        this.textSelection = sourceConfig.textSelection
        this.scrollPosition = sourceConfig.scrollPosition
        this.scrollExtentMax = sourceConfig.scrollExtentMax
        this.scrollExtentMin = sourceConfig.scrollExtentMin
    }
}

private fun concatStrings(
    thisString: String?,
    otherString: String?,
    thisTextDirection: TextDirection?,
    otherTextDirection: TextDirection?
): String? {
    if (otherString.isNullOrEmpty())
        return thisString
    var nestedLabel = otherString
    if (thisTextDirection != otherTextDirection && otherTextDirection != null) {
        nestedLabel = when (otherTextDirection) {
            TextDirection.Rtl -> "${Unicode.RLE}$nestedLabel${Unicode.PDF}"
            TextDirection.Ltr -> "${Unicode.LRE}$nestedLabel${Unicode.PDF}"
        }
    }
    if (thisString.isNullOrEmpty())
        return nestedLabel
    return "$thisString\n$nestedLabel"
}