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

package androidx.compose.ui.semantics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import kotlin.reflect.KProperty

/**
 * General semantics properties, mainly used for accessibility and testing.
 *
 * Each of these is intended to be set by the respective SemanticsPropertyReceiver extension
 * instead of used directly.
 */
/*@VisibleForTesting*/
object SemanticsProperties {
    /**
     * @see SemanticsPropertyReceiver.contentDescription
     */
    val ContentDescription = AccessibilityKey<List<String>>(
        name = "ContentDescription",
        mergePolicy = { parentValue, childValue ->
            parentValue?.toMutableList()?.also { it.addAll(childValue) } ?: childValue
        }
    )

    /**
     * @see SemanticsPropertyReceiver.stateDescription
     */
    val StateDescription = AccessibilityKey<String>("StateDescription")

    /**
     * @see SemanticsPropertyReceiver.progressBarRangeInfo
     */
    val ProgressBarRangeInfo =
        AccessibilityKey<ProgressBarRangeInfo>("ProgressBarRangeInfo")

    /**
     * @see SemanticsPropertyReceiver.paneTitle
     */
    val PaneTitle = AccessibilityKey<String>(
        name = "PaneTitle",
        mergePolicy = { _, _ ->
            throw IllegalStateException(
                "merge function called on unmergeable property PaneTitle."
            )
        }
    )

    /** @see SemanticsPropertyReceiver.selectableGroup */
    val SelectableGroup = AccessibilityKey<Unit>("SelectableGroup")

    /** @see SemanticsPropertyReceiver.collectionInfo */
    val CollectionInfo = AccessibilityKey<CollectionInfo>("CollectionInfo")

    /** @see SemanticsPropertyReceiver.collectionItemInfo */
    val CollectionItemInfo = AccessibilityKey<CollectionItemInfo>("CollectionItemInfo")

    /**
     * @see SemanticsPropertyReceiver.heading
     */
    val Heading = AccessibilityKey<Unit>("Heading")

    /**
     * @see SemanticsPropertyReceiver.disabled
     */
    val Disabled = AccessibilityKey<Unit>("Disabled")

    /**
     * @see SemanticsPropertyReceiver.liveRegion
     */
    val LiveRegion = AccessibilityKey<LiveRegionMode>("LiveRegion")

    /**
     * @see SemanticsPropertyReceiver.focused
     */
    val Focused = AccessibilityKey<Boolean>("Focused")

    /**
     * @see SemanticsPropertyReceiver.isContainer
     */
    @Deprecated("Use `isTraversalGroup` and `isOpaque` instead.")
    val IsContainer: SemanticsPropertyKey<Boolean>
        get() = IsTraversalGroup

    /**
     * @see SemanticsPropertyReceiver.isTraversalGroup
     */
    val IsTraversalGroup = SemanticsPropertyKey<Boolean>("IsTraversalGroup")

    /**
     * @see SemanticsPropertyReceiver.isOpaque
     */
    val IsOpaque = AccessibilityKey<Unit>(
        name = "IsOpaque",
        mergePolicy = { parentValue, _ ->
            parentValue
        }
    )

    /**
     * @see SemanticsPropertyReceiver.invisibleToUser
     */
    @ExperimentalComposeUiApi
    val InvisibleToUser = SemanticsPropertyKey<Unit>(
        name = "InvisibleToUser",
        mergePolicy = { parentValue, _ ->
            parentValue
        }
    )

    /**
     * @see SemanticsPropertyReceiver.traversalIndex
     */
    val TraversalIndex = SemanticsPropertyKey<Float>(
        name = "TraversalIndex",
        mergePolicy = { parentValue, _ ->
            // Never merge traversal indices
            parentValue
        }
    )

    /**
     * @see SemanticsPropertyReceiver.horizontalScrollAxisRange
     */
    val HorizontalScrollAxisRange =
        AccessibilityKey<ScrollAxisRange>("HorizontalScrollAxisRange")

    /**
     * @see SemanticsPropertyReceiver.verticalScrollAxisRange
     */
    val VerticalScrollAxisRange =
        AccessibilityKey<ScrollAxisRange>("VerticalScrollAxisRange")

    /**
     * @see SemanticsPropertyReceiver.popup
     */
    val IsPopup = AccessibilityKey<Unit>(
        name = "IsPopup",
        mergePolicy = { _, _ ->
            throw IllegalStateException(
                "merge function called on unmergeable property IsPopup. " +
                    "A popup should not be a child of a clickable/focusable node."
            )
        }
    )

    /**
     * @see SemanticsPropertyReceiver.dialog
     */
    val IsDialog = AccessibilityKey<Unit>(
        name = "IsDialog",
        mergePolicy = { _, _ ->
            throw IllegalStateException(
                "merge function called on unmergeable property IsDialog. " +
                    "A dialog should not be a child of a clickable/focusable node."
            )
        }
    )

    /**
     * The type of user interface element. Accessibility services might use this to describe the
     * element or do customizations. Most roles can be automatically resolved by the semantics
     * properties of this element. But some elements with subtle differences need an exact role. If
     * an exact role is not listed in [Role], this property should not be set and the framework will
     * automatically resolve it.
     *
     * @see SemanticsPropertyReceiver.role
     */
    val Role = AccessibilityKey<Role>("Role") { parentValue, _ -> parentValue }

    /**
     * @see SemanticsPropertyReceiver.testTag
     */
    val TestTag = SemanticsPropertyKey<String>(
        name = "TestTag",
        isImportantForAccessibility = false,
        mergePolicy = { parentValue, _ ->
            // Never merge TestTags, to avoid leaking internal test tags to parents.
            parentValue
        }
    )

    /**
     * @see SemanticsPropertyReceiver.text
     */
    val Text = AccessibilityKey<List<AnnotatedString>>(
        name = "Text",
        mergePolicy = { parentValue, childValue ->
            parentValue?.toMutableList()?.also { it.addAll(childValue) } ?: childValue
        }
    )

    /**
     * @see SemanticsPropertyReceiver.textSubstitution
     */
    val TextSubstitution = SemanticsPropertyKey<AnnotatedString>(name = "TextSubstitution")

    /**
     * @see SemanticsPropertyReceiver.isShowingTextSubstitution
     */
    val IsShowingTextSubstitution = SemanticsPropertyKey<Boolean>("IsShowingTextSubstitution")

    /**
     * @see SemanticsPropertyReceiver.editableText
     */
    val EditableText = AccessibilityKey<AnnotatedString>(name = "EditableText")

    /**
     * @see SemanticsPropertyReceiver.textSelectionRange
     */
    val TextSelectionRange = AccessibilityKey<TextRange>("TextSelectionRange")

    /**
     * @see SemanticsPropertyReceiver.onImeAction
     */
    val ImeAction = AccessibilityKey<ImeAction>("ImeAction")

    /**
     * @see SemanticsPropertyReceiver.selected
     */
    val Selected = AccessibilityKey<Boolean>("Selected")

    /**
     * @see SemanticsPropertyReceiver.toggleableState
     */
    val ToggleableState = AccessibilityKey<ToggleableState>("ToggleableState")

    /**
     * @see SemanticsPropertyReceiver.password
     */
    val Password = AccessibilityKey<Unit>("Password")

    /**
     * @see SemanticsPropertyReceiver.error
     */
    val Error = AccessibilityKey<String>("Error")

    /**
     * @see SemanticsPropertyReceiver.indexForKey
     */
    val IndexForKey = SemanticsPropertyKey<(Any) -> Int>("IndexForKey")

    /**
     * @see SemanticsPropertyReceiver.editable
     */
    val Editable = SemanticsPropertyKey<Unit>("Editable")
}

/**
 * Ths object defines keys of the actions which can be set in semantics and performed on the
 * semantics node.
 *
 * Each of these is intended to be set by the respective SemanticsPropertyReceiver extension
 * instead of used directly.
 */
/*@VisibleForTesting*/
object SemanticsActions {
    /**
     * @see SemanticsPropertyReceiver.getTextLayoutResult
     */
    val GetTextLayoutResult =
        ActionPropertyKey<(MutableList<TextLayoutResult>) -> Boolean>("GetTextLayoutResult")

    /**
     * @see SemanticsPropertyReceiver.onClick
     */
    val OnClick = ActionPropertyKey<() -> Boolean>("OnClick")

    /**
     * @see SemanticsPropertyReceiver.onLongClick
     */
    val OnLongClick = ActionPropertyKey<() -> Boolean>("OnLongClick")

    /**
     * @see SemanticsPropertyReceiver.scrollBy
     */
    val ScrollBy = ActionPropertyKey<(x: Float, y: Float) -> Boolean>("ScrollBy")

    /**
     * @see SemanticsPropertyReceiver.scrollToIndex
     */
    val ScrollToIndex = ActionPropertyKey<(Int) -> Boolean>("ScrollToIndex")

    /**
     * @see SemanticsPropertyReceiver.setProgress
     */
    val SetProgress = ActionPropertyKey<(progress: Float) -> Boolean>("SetProgress")

    /**
     * @see SemanticsPropertyReceiver.setSelection
     */
    val SetSelection = ActionPropertyKey<(Int, Int, Boolean) -> Boolean>("SetSelection")

    /**
     * @see SemanticsPropertyReceiver.setText
     */
    val SetText = ActionPropertyKey<(AnnotatedString) -> Boolean>("SetText")

    /**
     * @see SemanticsPropertyReceiver.setTextSubstitution
     */
    val SetTextSubstitution = ActionPropertyKey<(AnnotatedString) -> Boolean>("SetTextSubstitution")

    /**
     * @see SemanticsPropertyReceiver.showTextSubstitution
     */
    val ShowTextSubstitution = ActionPropertyKey<(Boolean) -> Boolean>("ShowTextSubstitution")

    /**
     * @see SemanticsPropertyReceiver.clearTextSubstitution
     */
    val ClearTextSubstitution = ActionPropertyKey<() -> Boolean>("ClearTextSubstitution")

    /**
     * @see SemanticsPropertyReceiver.insertTextAtCursor
     */
    val InsertTextAtCursor = ActionPropertyKey<(AnnotatedString) -> Boolean>("InsertTextAtCursor")

    /**
     * @see SemanticsPropertyReceiver.onImeAction
     */
    val OnImeAction = ActionPropertyKey<() -> Boolean>("PerformImeAction")

    // b/322269946
    @Suppress("unused")
    @Deprecated(
        message = "Maintained for binary compatibility. " +
            "Use `SemanticsActions.OnImeAction` instead.",
        level = DeprecationLevel.HIDDEN,
    )
    val PerformImeAction = ActionPropertyKey<() -> Boolean>("PerformImeAction")

    /**
     * @see SemanticsPropertyReceiver.copyText
     */
    val CopyText = ActionPropertyKey<() -> Boolean>("CopyText")

    /**
     * @see SemanticsPropertyReceiver.cutText
     */
    val CutText = ActionPropertyKey<() -> Boolean>("CutText")

    /**
     * @see SemanticsPropertyReceiver.pasteText
     */
    val PasteText = ActionPropertyKey<() -> Boolean>("PasteText")

    /**
     * @see SemanticsPropertyReceiver.expand
     */
    val Expand = ActionPropertyKey<() -> Boolean>("Expand")

    /**
     * @see SemanticsPropertyReceiver.collapse
     */
    val Collapse = ActionPropertyKey<() -> Boolean>("Collapse")

    /**
     * @see SemanticsPropertyReceiver.dismiss
     */
    val Dismiss = ActionPropertyKey<() -> Boolean>("Dismiss")

    /**
     * @see SemanticsPropertyReceiver.requestFocus
     */
    val RequestFocus = ActionPropertyKey<() -> Boolean>("RequestFocus")

    /**
     * @see SemanticsPropertyReceiver.customActions
     */
    val CustomActions =
        AccessibilityKey<List<CustomAccessibilityAction>>("CustomActions")

    /**
     * @see SemanticsPropertyReceiver.pageUp
     */
    val PageUp = ActionPropertyKey<() -> Boolean>("PageUp")

    /**
     * @see SemanticsPropertyReceiver.pageLeft
     */
    val PageLeft = ActionPropertyKey<() -> Boolean>("PageLeft")

    /**
     * @see SemanticsPropertyReceiver.pageDown
     */
    val PageDown = ActionPropertyKey<() -> Boolean>("PageDown")

    /**
     * @see SemanticsPropertyReceiver.pageRight
     */
    val PageRight = ActionPropertyKey<() -> Boolean>("PageRight")
}

/**
 * SemanticsPropertyKey is the infrastructure for setting key/value pairs inside semantics blocks
 * in a type-safe way.  Each key has one particular statically defined value type T.
 */
class SemanticsPropertyKey<T>(
    /**
     * The name of the property.  Should be the same as the constant from which it is accessed.
     */
    val name: String,
    internal val mergePolicy: (T?, T) -> T? = { parentValue, childValue ->
        parentValue ?: childValue
    }
) {
    /**
     * Whether this type of property provides information relevant to accessibility services.
     *
     * Most built-in semantics properties are relevant to accessibility, but a very common
     * exception is testTag. Nodes with only a testTag still need to be included
     * in the AccessibilityNodeInfo tree because UIAutomator tests rely on
     * that, but we mark them `isImportantForAccessibility = false` on the AccessibilityNodeInfo
     * to inform accessibility services that they are best ignored.
     *
     * The default value is false and it is not exposed as a public API. That's because
     * it is impossible in the first place for `SemanticsPropertyKey`s
     * defined outside the UI package to be relevant to accessibility, because
     * for each accessibility-relevant SemanticsProperty type to get plumbed into the
     * AccessibilityNodeInfo, the private `createNodeInfo` implementation must also have
     * a line of code.
     */
    internal var isImportantForAccessibility = false
        private set

    internal constructor(
        name: String,
        isImportantForAccessibility: Boolean,
    ) : this(name) {
        this.isImportantForAccessibility = isImportantForAccessibility
    }

    internal constructor(
        name: String,
        isImportantForAccessibility: Boolean,
        mergePolicy: (T?, T) -> T?
    ) : this(name, mergePolicy) {
        this.isImportantForAccessibility = isImportantForAccessibility
    }

    /**
     * Method implementing the semantics merge policy of a particular key.
     *
     * When mergeDescendants is set on a semantics node, then this function will called for each
     * descendant node of a given key in depth-first-search order.  The parent
     * value accumulates the result of merging the values seen so far, similar to reduce().
     *
     * The default implementation returns the parent value if one exists, otherwise uses the
     * child element.  This means by default, a SemanticsNode with mergeDescendants = true
     * winds up with the first value found for each key in its subtree in depth-first-search order.
     */
    fun merge(parentValue: T?, childValue: T): T? {
        return mergePolicy(parentValue, childValue)
    }

    /**
     * Throws [UnsupportedOperationException].  Should not be called.
     */
    // TODO(KT-6519): Remove this getter
    // TODO(KT-32770): Cannot deprecate this either as the getter is considered called by "by"
    final operator fun getValue(thisRef: SemanticsPropertyReceiver, property: KProperty<*>): T {
        return throwSemanticsGetNotSupported()
    }

    final operator fun setValue(
        thisRef: SemanticsPropertyReceiver,
        property: KProperty<*>,
        value: T
    ) {
        thisRef[this] = value
    }

    override fun toString(): String {
        return "AccessibilityKey: $name"
    }
}

private fun <T> throwSemanticsGetNotSupported(): T {
    throw UnsupportedOperationException(
        "You cannot retrieve a semantics property directly - " +
            "use one of the SemanticsConfiguration.getOr* methods instead"
    )
}

internal fun <T> AccessibilityKey(
    name: String
) =
    SemanticsPropertyKey<T>(
        name = name,
        isImportantForAccessibility = true
    )

internal fun <T> AccessibilityKey(
    name: String,
    mergePolicy: (T?, T) -> T?
) =
    SemanticsPropertyKey<T>(
        name = name,
        isImportantForAccessibility = true,
        mergePolicy = mergePolicy
    )

/**
 * Standard accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should return
 * a boolean result indicating whether the action is successfully handled. For example, a scroll
 * forward action should return false if the widget is not enabled or has reached the end of the
 * list. If multiple semantics blocks with the same AccessibilityAction are provided, the
 * resulting AccessibilityAction's label/action will be the label/action of the outermost
 * modifier with this key and nonnull label/action, or null if no nonnull label/action is found.
 */
class AccessibilityAction<T : Function<Boolean>>(val label: String?, val action: T?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccessibilityAction<*>) return false

        if (label != other.label) return false
        if (action != other.action) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label?.hashCode() ?: 0
        result = 31 * result + action.hashCode()
        return result
    }

    override fun toString(): String {
        return "AccessibilityAction(label=$label, action=$action)"
    }
}

@Suppress("NOTHING_TO_INLINE")
// inline to break static initialization cycle issue
private inline fun <T : Function<Boolean>> ActionPropertyKey(
    name: String
) =
    AccessibilityKey<AccessibilityAction<T>>(
        name = name,
        mergePolicy = { parentValue, childValue ->
            AccessibilityAction(
                parentValue?.label ?: childValue.label,
                parentValue?.action ?: childValue.action
            )
        }
    )

/**
 * Custom accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should have no
 * arguments and return a boolean result indicating whether the action is successfully handled.
 */
class CustomAccessibilityAction(val label: String, val action: () -> Boolean) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomAccessibilityAction) return false

        if (label != other.label) return false
        if (action != other.action) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + action.hashCode()
        return result
    }

    override fun toString(): String {
        return "CustomAccessibilityAction(label=$label, action=$action)"
    }
}

/**
 * Accessibility range information, to represent the status of a progress bar or
 * seekable progress bar.
 *
 * @param current current value in the range. Must not be NaN.
 * @param range range of this node
 * @param steps if greater than `0`, specifies the number of discrete values, evenly distributed
 * between across the whole value range. If `0`, any value from the range specified can be chosen.
 * Cannot be less than `0`.
 */
class ProgressBarRangeInfo(
    val current: Float,
    val range: ClosedFloatingPointRange<Float>,
    /*@IntRange(from = 0)*/
    val steps: Int = 0
) {
    init {
        require(!current.isNaN()) { "current must not be NaN" }
    }

    companion object {
        /**
         * Accessibility range information to present indeterminate progress bar
         */
        val Indeterminate = ProgressBarRangeInfo(0f, 0f..0f)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProgressBarRangeInfo) return false

        if (current != other.current) return false
        if (range != other.range) return false
        if (steps != other.steps) return false

        return true
    }

    override fun hashCode(): Int {
        var result = current.hashCode()
        result = 31 * result + range.hashCode()
        result = 31 * result + steps
        return result
    }

    override fun toString(): String {
        return "ProgressBarRangeInfo(current=$current, range=$range, steps=$steps)"
    }
}

/**
 * Information about the collection.
 *
 * A collection of items has [rowCount] rows and [columnCount] columns.
 * For example, a vertical list is a collection with one column, as many rows as the list items
 * that are important for accessibility; A table is a collection with several rows and several
 * columns.
 *
 * @param rowCount the number of rows in the collection, or -1 if unknown
 * @param columnCount the number of columns in the collection, or -1 if unknown
 */
class CollectionInfo(val rowCount: Int, val columnCount: Int)

/**
 * Information about the item of a collection.
 *
 * A collection item is contained in a collection, it starts at a given [rowIndex] and
 * [columnIndex] in the collection, and spans one or more rows and columns. For example, a header
 * of two related table columns starts at the first row and the first column, spans one row and
 * two columns.
 *
 * @param rowIndex the index of the row at which item is located
 * @param rowSpan the number of rows the item spans
 * @param columnIndex the index of the column at which item is located
 * @param columnSpan the number of columns the item spans
 */
class CollectionItemInfo(
    val rowIndex: Int,
    val rowSpan: Int,
    val columnIndex: Int,
    val columnSpan: Int
)

/**
 * The scroll state of one axis if this node is scrollable.
 *
 * @param value current 0-based scroll position value (either in pixels, or lazy-item count)
 * @param maxValue maximum bound for [value], or [Float.POSITIVE_INFINITY] if still unknown
 * @param reverseScrolling for horizontal scroll, when this is `true`, 0 [value] will mean right,
 * when`false`, 0 [value] will mean left. For vertical scroll, when this is `true`, 0 [value] will
 * mean bottom, when `false`, 0 [value] will mean top
 */
class ScrollAxisRange(
    val value: () -> Float,
    val maxValue: () -> Float,
    val reverseScrolling: Boolean = false
) {
    override fun toString(): String =
        "ScrollAxisRange(value=${value()}, maxValue=${maxValue()}, " +
            "reverseScrolling=$reverseScrolling)"
}

/**
 * The type of user interface element. Accessibility services might use this to describe the
 * element or do customizations. Most roles can be automatically resolved by the semantics
 * properties of this element. But some elements with subtle differences need an exact role. If an
 * exact role is not listed, [SemanticsPropertyReceiver.role] should not be set and the framework
 * will automatically resolve it.
 */
@Immutable
@kotlin.jvm.JvmInline
value class Role private constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * This element is a button control. Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsActions.OnClick]
         */
        val Button = Role(0)

        /**
         * This element is a Checkbox which is a component that represents two states (checked /
         * unchecked). Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
         * [SemanticsActions.OnClick]
         */
        val Checkbox = Role(1)

        /**
         * This element is a Switch which is a two state toggleable component that provides on/off
         * like options. Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
         * [SemanticsActions.OnClick]
         */
        val Switch = Role(2)

        /**
         * This element is a RadioButton which is a component to represent two states, selected and not
         * selected. Associated semantics properties for accessibility: [SemanticsProperties.Disabled],
         * [SemanticsProperties.StateDescription], [SemanticsActions.OnClick]
         */
        val RadioButton = Role(3)

        /**
         * This element is a Tab which represents a single page of content using a text label and/or
         * icon. A Tab also has two states: selected and not selected. Associated semantics properties
         * for accessibility: [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
         * [SemanticsActions.OnClick]
         */
        val Tab = Role(4)

        /**
         * This element is an image. Associated semantics properties for accessibility:
         * [SemanticsProperties.ContentDescription]
         */
        val Image = Role(5)

        /**
         * This element is associated with a drop down menu.
         * Associated semantics properties for accessibility:
         * [SemanticsActions.OnClick]
         */
        val DropdownList = Role(6)
    }

    override fun toString() = when (this) {
        Button -> "Button"
        Checkbox -> "Checkbox"
        Switch -> "Switch"
        RadioButton -> "RadioButton"
        Tab -> "Tab"
        Image -> "Image"
        DropdownList -> "DropdownList"
        else -> "Unknown"
    }
}

/**
 * The mode of live region. Live region indicates to accessibility services they should
 * automatically notify the user about changes to the node's content description or text, or to
 * the content descriptions or text of the node's children (where applicable).
 */
@Immutable
@kotlin.jvm.JvmInline
value class LiveRegionMode private constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * Live region mode specifying that accessibility services should announce
         * changes to this node.
         */
        val Polite = LiveRegionMode(0)

        /**
         * Live region mode specifying that accessibility services should interrupt
         * ongoing speech to immediately announce changes to this node.
         */
        val Assertive = LiveRegionMode(1)
    }

    override fun toString() = when (this) {
        Polite -> "Polite"
        Assertive -> "Assertive"
        else -> "Unknown"
    }
}

/**
 * SemanticsPropertyReceiver is the scope provided by semantics {} blocks, letting you set
 * key/value pairs primarily via extension functions.
 */
interface SemanticsPropertyReceiver {
    operator fun <T> set(key: SemanticsPropertyKey<T>, value: T)

    /**
     * Unset an individual property.
     *
     * Note: this can only unset properties originally on the same modifier chain, not properties
     * merged from children (for those, use [clearAndSetSemantics] instead). And because the
     * semantics system processes modifier chains back-to-front, the unset must be ordered earlier
     * on the modifier chain if it's in a separate `semantics {}` block.

     * 1. Examples of correct uses (resulting in empty semantics):
     *
     *   ```
     *   Modifier.semantics { heading(); unset(SemanticsProperties.Heading) }
     *   Modifier.semantics { unset(SemanticsProperties.Heading) }.semantics { heading() }
     *   ```
     *
     * 2. Examples of ineffective, no-op uses (where the heading remains instead of being unset):
     *
     *   ```
     *   Modifier.semantics { unset(SemanticsProperties.Heading); heading() } // order
     *   Modifier.semantics { heading() }.semantics { unset(SemanticsProperties.Heading) } // order
     *   Box(Modifier.semantics(mergeDescendants = true) { unset(SemanticsProperties.Heading) }) {
     *       Box(Modifier.semantics { heading() }) // not originally on the same modifier chain`
     *   }
     *
     * 3. Examples of complex cases where there is more than one set:
     *
     *   ```
     *   // Result is empty semantics:
     *   Modifier.semantics { unset(SemanticsProperties.TestTag) }.testTag("b").testTag("a")
     *
     *   // Result is testTag = "b":`
     *   Modifier.testTag("b").semantics { unset(SemanticsProperties.TestTag) }.testTag("a")
     *   ```
     */
    @ExperimentalComposeUiApi
    fun <T> unset(key: SemanticsPropertyKey<T>)
}

/**
 * Developer-set content description of the semantics node.
 *
 * If this is not set, accessibility services will present the [text][SemanticsProperties.Text] of
 * this node as the content.
 *
 * This typically should not be set directly by applications, because some screen readers will
 * cease presenting other relevant information when this property is present. This is intended
 * to be used via Foundation components which are inherently intractable to automatically
 * describe, such as Image, Icon, and Canvas.
 */
var SemanticsPropertyReceiver.contentDescription: String
    get() = throwSemanticsGetNotSupported()
    set(value) {
        set(SemanticsProperties.ContentDescription, listOf(value))
    }

/**
 * Developer-set state description of the semantics node.
 *
 * For example: on/off. If this not set, accessibility services will derive the state from
 * other semantics properties, like [ProgressBarRangeInfo], but it is not guaranteed and the format
 * will be decided by accessibility services.
 */
var SemanticsPropertyReceiver.stateDescription by SemanticsProperties.StateDescription

/**
 * The semantics represents a range of possible values with a current value.
 * For example, when used on a slider control, this will allow screen readers to communicate
 * the slider's state.
 */
var SemanticsPropertyReceiver.progressBarRangeInfo by SemanticsProperties.ProgressBarRangeInfo

/**
 * The node is marked as heading for accessibility.
 *
 * @see SemanticsProperties.Heading
 */
fun SemanticsPropertyReceiver.heading() {
    this[SemanticsProperties.Heading] = Unit
}

/**
 * Accessibility-friendly title for a screen's pane. For accessibility purposes, a pane is a
 * visually distinct portion of a window, such as the contents of a open drawer. In order for
 * accessibility services to understand a pane's window-like behavior, you should give
 * descriptive titles to your app's panes. Accessibility services can then provide more granular
 * information to users when a pane's appearance or content changes.
 *
 * @see SemanticsProperties.PaneTitle
 */
var SemanticsPropertyReceiver.paneTitle by SemanticsProperties.PaneTitle

/**
 * Whether this semantics node is disabled. Note that proper [SemanticsActions] should still
 * be added when this property is set.
 *
 * @see SemanticsProperties.Disabled
 */
fun SemanticsPropertyReceiver.disabled() {
    this[SemanticsProperties.Disabled] = Unit
}

/**
 * This node is marked as live region for accessibility. This indicates to accessibility services
 * they should automatically notify the user about changes to the node's content description or
 * text, or to the content descriptions or text of the node's children (where applicable). It
 * should be used with caution, especially with assertive mode which immediately stops the
 * current audio and the user does not hear the rest of the content. An example of proper use is
 * a Snackbar which is marked as [LiveRegionMode.Polite].
 *
 * @see SemanticsProperties.LiveRegion
 * @see LiveRegionMode
 */
var SemanticsPropertyReceiver.liveRegion by SemanticsProperties.LiveRegion

/**
 * Whether this semantics node is focused. The presence of this property indicates this node is
 * focusable
 *
 * @see SemanticsProperties.Focused
 */
var SemanticsPropertyReceiver.focused by SemanticsProperties.Focused

/**
 * Whether this semantics node is a container. This is defined as a node whose function
 * is to serve as a boundary or border in organizing its children.
 *
 * @see SemanticsProperties.IsContainer
 */
@Deprecated("Use `isTraversalGroup` and `isOpaque` instead.")
@get:Deprecated("Use `isTraversalGroup` and `isOpaque` instead.")
@set:Deprecated("Use `isTraversalGroup` and `isOpaque` instead.")
@OptIn(ExperimentalComposeUiApi::class)
var SemanticsPropertyReceiver.isContainer: Boolean
    get() = throwSemanticsGetNotSupported()
    set(bool) {
        isTraversalGroup = bool
        if (bool) {
            this[SemanticsProperties.IsOpaque] = Unit
        } else {
            unset(SemanticsProperties.IsOpaque)
        }
    }

/**
 * Whether this semantics node is a traversal group.
 *
 * See https://developer.android.com/jetpack/compose/accessibility#modify-traversal-order
 *
 * @see SemanticsProperties.IsTraversalGroup
 */
var SemanticsPropertyReceiver.isTraversalGroup by SemanticsProperties.IsTraversalGroup

/**
 * Non-mergeable property used to mark that whether a node is semantically opaque.
 *
 * In other words, whether nodes fully covered by it ought to be pruned from the a11y tree.  (Note
 * that most semantic properties other than testTag also have this effect, so it should be rarely
 * needed.)
 *
 * If true, then a11y nodes behind will be pruned unless this node's graphics alpha is 0.
 *
 * @see SemanticsProperties.IsOpaque
 */
fun SemanticsPropertyReceiver.isOpaque() {
    this[SemanticsProperties.IsOpaque] = Unit
}

/**
 * Whether this node is specially known to be invisible to the user.
 *
 * For example, if the node is currently occluded by a dark semitransparent
 * pane above it, then for all practical purposes the node is invisible to the user,
 * but the system cannot automatically determine that.  To make the screen reader linear
 * navigation skip over this type of invisible node, this property can be set.
 *
 * If looking for a way to hide semantics of small items from screen readers because they're
 * redundant with semantics of their parent, consider [SemanticsModifier.clearAndSetSemantics]
 * instead.
 */
@ExperimentalComposeUiApi
fun SemanticsPropertyReceiver.invisibleToUser() {
    this[SemanticsProperties.InvisibleToUser] = Unit
}

/**
 * A value to manually control screenreader traversal order.
 *
 * This API can be used to customize TalkBack traversal order. When the `traversalIndex` property is
 * set on a traversalGroup or on a screenreader-focusable node, then the sorting algorithm will
 * prioritize nodes with smaller `traversalIndex`s earlier. The default traversalIndex value is
 * zero, and traversalIndices are compared at a peer level.
 *
 * For example,` traversalIndex = -1f` can be used to force a top bar to be ordered earlier, and
 * `traversalIndex = 1f` to make a bottom bar ordered last, in the edge cases where this does not
 * happen by default.  As another example, if you need to reorder two Buttons within a Row, then
 * you can set `isTraversalGroup = true` on the Row, and set `traversalIndex` on one of the Buttons.
 *
 * Note that if `traversalIndex` seems to have no effect, be sure to set `isTraversalGroup = true`
 * as well.
 */
var SemanticsPropertyReceiver.traversalIndex by SemanticsProperties.TraversalIndex

/**
 * The horizontal scroll state of this node if this node is scrollable.
 */
var SemanticsPropertyReceiver.horizontalScrollAxisRange
    by SemanticsProperties.HorizontalScrollAxisRange

/**
 * The vertical scroll state of this node if this node is scrollable.
 */
var SemanticsPropertyReceiver.verticalScrollAxisRange
    by SemanticsProperties.VerticalScrollAxisRange

/**
 * Whether this semantics node represents a Popup. Not to be confused with if this node is
 * _part of_ a Popup.
 */
fun SemanticsPropertyReceiver.popup() {
    this[SemanticsProperties.IsPopup] = Unit
}

/**
 * Whether this element is a Dialog. Not to be confused with if this element is _part of_ a Dialog.
 */
fun SemanticsPropertyReceiver.dialog() {
    this[SemanticsProperties.IsDialog] = Unit
}

/**
 * The type of user interface element. Accessibility services might use this to describe the
 * element or do customizations. Most roles can be automatically resolved by the semantics
 * properties of this element. But some elements with subtle differences need an exact role. If
 * an exact role is not listed in [Role], this property should not be set and the framework will
 * automatically resolve it.
 */
var SemanticsPropertyReceiver.role by SemanticsProperties.Role

/**
 * Test tag attached to this semantics node.
 *
 * This can be used to find nodes in testing frameworks:
 * - In Compose's built-in unit test framework, use with
 * [onNodeWithTag][androidx.compose.ui.test.onNodeWithTag].
 * - For newer AccessibilityNodeInfo-based integration test frameworks, it can be matched in the
 * extras with key "androidx.compose.ui.semantics.testTag"
 * - For legacy AccessibilityNodeInfo-based integration tests, it's optionally exposed as the
 * resource id if [testTagsAsResourceId] is true (for matching with 'By.res' in UIAutomator).
 */
var SemanticsPropertyReceiver.testTag by SemanticsProperties.TestTag

/**
 * Text of the semantics node. It must be real text instead of developer-set content description.
 *
 * @see SemanticsPropertyReceiver.editableText
 */
var SemanticsPropertyReceiver.text: AnnotatedString
    get() = throwSemanticsGetNotSupported()
    set(value) {
        set(SemanticsProperties.Text, listOf(value))
    }

/**
 * Text substitution of the semantics node. This property is only available after calling
 * [SemanticsActions.SetTextSubstitution].
 */
var SemanticsPropertyReceiver.textSubstitution by SemanticsProperties.TextSubstitution

/**
 * Whether this element is showing the text substitution. This property is only available after
 * calling [SemanticsActions.SetTextSubstitution].
 */
var SemanticsPropertyReceiver.isShowingTextSubstitution
    by SemanticsProperties.IsShowingTextSubstitution

/**
 * Input text of the text field with visual transformation applied to it. It must be a real text
 * entered by the user with visual transformation applied on top of the input text instead of a
 * developer-set content description.
 */
var SemanticsPropertyReceiver.editableText by SemanticsProperties.EditableText

/**
 * Text selection range for the text field.
 */
var SemanticsPropertyReceiver.textSelectionRange by SemanticsProperties.TextSelectionRange

/**
 * Contains the IME action provided by the node.
 *
 * For example, "go to next form field" or "submit".
 *
 * A node that specifies an action should also specify a callback to perform the action via
 * [onImeAction].
 */
@Deprecated("Pass the ImeAction to onImeAction instead.")
@get:Deprecated("Pass the ImeAction to onImeAction instead.")
@set:Deprecated("Pass the ImeAction to onImeAction instead.")
var SemanticsPropertyReceiver.imeAction by SemanticsProperties.ImeAction

/**
 * Whether this element is selected (out of a list of possible selections).
 *
 * The presence of this property indicates that the element is selectable.
 */
var SemanticsPropertyReceiver.selected by SemanticsProperties.Selected

/**
 * This semantics marks node as a collection and provides the required information.
 *
 * @see collectionItemInfo
 */
var SemanticsPropertyReceiver.collectionInfo by SemanticsProperties.CollectionInfo

/**
 * This semantics marks node as an items of a collection and provides the required information.
 *
 * If you mark items of a collection, you should also be marking the collection with
 * [collectionInfo].
 */
var SemanticsPropertyReceiver.collectionItemInfo by SemanticsProperties.CollectionItemInfo

/**
 * The state of a toggleable component.
 *
 * The presence of this property indicates that the element is toggleable.
 */
var SemanticsPropertyReceiver.toggleableState by SemanticsProperties.ToggleableState

/**
 * The node is marked as a password.
 */
fun SemanticsPropertyReceiver.password() {
    this[SemanticsProperties.Password] = Unit
}

/**
 * Mark semantics node that contains invalid input or error.
 *
 * @param [description] a localized description explaining an error to the accessibility user
 */
fun SemanticsPropertyReceiver.error(description: String) {
    this[SemanticsProperties.Error] = description
}

/**
 * The index of an item identified by a given key. The key is usually defined during the creation
 * of the container. If the key did not match any of the items' keys, the [mapping] must return -1.
 */
fun SemanticsPropertyReceiver.indexForKey(mapping: (Any) -> Int) {
    this[SemanticsProperties.IndexForKey] = mapping
}

/**
 * Whether this semantics node is editable, e.g. an editable text field.
 */
fun SemanticsPropertyReceiver.editable() {
    this[SemanticsProperties.Editable] = Unit
}

/**
 * The node is marked as a collection of horizontally or vertically stacked selectable elements.
 *
 * Unlike [collectionInfo] which marks a collection of any elements and asks developer to
 * provide all the required information like number of elements etc., this semantics will
 * populate the number of selectable elements automatically. Note that if you use this semantics
 * with lazy collections, it won't get the number of elements in the collection.
 *
 * @see SemanticsPropertyReceiver.selected
 */
fun SemanticsPropertyReceiver.selectableGroup() {
    this[SemanticsProperties.SelectableGroup] = Unit
}

/**
 * Custom actions which are defined by app developers.
 */
var SemanticsPropertyReceiver.customActions by SemanticsActions.CustomActions

/**
 * Action to get a Text/TextField node's [TextLayoutResult]. The result is the first element
 * of layout (the argument of the AccessibilityAction).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.GetTextLayoutResult] is called.
 */
fun SemanticsPropertyReceiver.getTextLayoutResult(
    label: String? = null,
    action: ((MutableList<TextLayoutResult>) -> Boolean)?
) {
    this[SemanticsActions.GetTextLayoutResult] = AccessibilityAction(label, action)
}

/**
 * Action to be performed when the node is clicked (single-tapped).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnClick] is called.
 */
fun SemanticsPropertyReceiver.onClick(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.OnClick] = AccessibilityAction(label, action)
}

/**
 * Action to be performed when the node is long clicked (long-pressed).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnLongClick] is called.
 */
fun SemanticsPropertyReceiver.onLongClick(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.OnLongClick] = AccessibilityAction(label, action)
}

/**
 * Action to scroll by a specified amount.
 *
 * Expected to be used in conjunction with verticalScrollAxisRange/horizontalScrollAxisRange.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.ScrollBy] is called.
 */
fun SemanticsPropertyReceiver.scrollBy(
    label: String? = null,
    action: ((x: Float, y: Float) -> Boolean)?
) {
    this[SemanticsActions.ScrollBy] = AccessibilityAction(label, action)
}

/**
 * Action to scroll a container to the index of one of its items.
 *
 * The [action] should throw an [IllegalArgumentException] if the index is out of bounds.
 */
fun SemanticsPropertyReceiver.scrollToIndex(
    label: String? = null,
    action: (Int) -> Boolean
) {
    this[SemanticsActions.ScrollToIndex] = AccessibilityAction(label, action)
}

/**
 * Action to set the current value of the progress bar.
 *
 * Expected to be used in conjunction with progressBarRangeInfo.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetProgress] is called.
 */
fun SemanticsPropertyReceiver.setProgress(label: String? = null, action: ((Float) -> Boolean)?) {
    this[SemanticsActions.SetProgress] = AccessibilityAction(label, action)
}

/**
 * Action to set the text contents of this node.
 *
 * Expected to be used on editable text fields.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.SetText] is called.
 */
fun SemanticsPropertyReceiver.setText(
    label: String? = null,
    action: ((AnnotatedString) -> Boolean)?
) {
    this[SemanticsActions.SetText] = AccessibilityAction(label, action)
}

/**
 * Action to set the text substitution of this node.
 *
 * Expected to be used on non-editable text.
 *
 * Note, this action doesn't show the text substitution. Please call
 * [SemanticsPropertyReceiver.showTextSubstitution] to show the text substitution.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.SetTextSubstitution] is called.
 */
fun SemanticsPropertyReceiver.setTextSubstitution(
    label: String? = null,
    action: ((AnnotatedString) -> Boolean)?
) {
    this[SemanticsActions.SetTextSubstitution] = AccessibilityAction(label, action)
}

/**
 * Action to show or hide the text substitution of this node.
 *
 * Expected to be used on non-editable text.
 *
 * Note, this action only takes effect when the node has the text substitution.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.ShowTextSubstitution] is called.
 */
fun SemanticsPropertyReceiver.showTextSubstitution(
    label: String? = null,
    action: ((Boolean) -> Boolean)?
) {
    this[SemanticsActions.ShowTextSubstitution] = AccessibilityAction(label, action)
}

/**
 * Action to clear the text substitution of this node.
 *
 * Expected to be used on non-editable text.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.ClearTextSubstitution] is called.
 */
fun SemanticsPropertyReceiver.clearTextSubstitution(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.ClearTextSubstitution] = AccessibilityAction(label, action)
}

/**
 * Action to insert text into this node at the current cursor position, or replacing the selection
 * if text is selected.
 *
 * Expected to be used on editable text fields.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.InsertTextAtCursor] is called.
 */
fun SemanticsPropertyReceiver.insertTextAtCursor(
    label: String? = null,
    action: ((AnnotatedString) -> Boolean)?
) {
    this[SemanticsActions.InsertTextAtCursor] = AccessibilityAction(label, action)
}

/**
 * Action to invoke the IME action handler configured on the node, as well as specify the type of
 * IME action provided by the node.
 *
 * Expected to be used on editable text fields.
 *
 * @param imeActionType The IME type, such as [ImeAction.Next] or [ImeAction.Search]
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.OnImeAction] is called.
 *
 * @see SemanticsProperties.ImeAction
 * @see SemanticsActions.OnImeAction
 */
fun SemanticsPropertyReceiver.onImeAction(
    imeActionType: ImeAction,
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsProperties.ImeAction] = imeActionType
    this[SemanticsActions.OnImeAction] = AccessibilityAction(label, action)
}

// b/322269946
@Suppress("unused")
@Deprecated(
    message = "Maintained for binary compatibility. " +
        "Use `SemanticsPropertyReceiver.onImeAction` instead.",
    level = DeprecationLevel.HIDDEN,
)
fun SemanticsPropertyReceiver.performImeAction(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.OnImeAction] = AccessibilityAction(label, action)
}

/**
 * Action to set text selection by character index range.
 *
 * If this action is provided, the selection data must be provided
 * using [textSelectionRange].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetSelection] is called. The
 * parameters to the action are: `startIndex`, `endIndex`, and whether the indices are relative
 * to the original text or the transformed text (when a `VisualTransformation` is applied).
 */
fun SemanticsPropertyReceiver.setSelection(
    label: String? = null,
    action: ((startIndex: Int, endIndex: Int, relativeToOriginalText: Boolean) -> Boolean)?
) {
    this[SemanticsActions.SetSelection] = AccessibilityAction(label, action)
}

/**
 * Action to copy the text to the clipboard.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.CopyText] is called.
 */
fun SemanticsPropertyReceiver.copyText(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.CopyText] = AccessibilityAction(label, action)
}

/**
 * Action to cut the text and copy it to the clipboard.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.CutText] is called.
 */
fun SemanticsPropertyReceiver.cutText(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.CutText] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.PasteText] to the [SemanticsPropertyReceiver].
 * Use it to indicate that element is open for accepting paste data from the clipboard. There is
 * no need to check if the clipboard data available as this is done by the framework.
 * For this action to be triggered, the element must also have the [SemanticsProperties.Focused]
 * property set.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PasteText] is called.
 *
 * @see focused
 */
fun SemanticsPropertyReceiver.pasteText(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.PasteText] = AccessibilityAction(label, action)
}

/**
 * Action to expand an expandable node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Expand] is called.
 */
fun SemanticsPropertyReceiver.expand(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.Expand] = AccessibilityAction(label, action)
}

/**
 * Action to collapse an expandable node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Collapse] is called.
 */
fun SemanticsPropertyReceiver.collapse(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.Collapse] = AccessibilityAction(label, action)
}

/**
 * Action to dismiss a dismissible node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Dismiss] is called.
 */
fun SemanticsPropertyReceiver.dismiss(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.Dismiss] = AccessibilityAction(label, action)
}

/**
 * Action that gives input focus to this node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.RequestFocus] is called.
 */
fun SemanticsPropertyReceiver.requestFocus(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.RequestFocus] = AccessibilityAction(label, action)
}

/**
 * Action to page up.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PageUp] is called.
 */
fun SemanticsPropertyReceiver.pageUp(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.PageUp] = AccessibilityAction(label, action)
}

/**
 * Action to page down.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PageDown] is called.
 */
fun SemanticsPropertyReceiver.pageDown(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.PageDown] = AccessibilityAction(label, action)
}

/**
 * Action to page left.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PageLeft] is called.
 */
fun SemanticsPropertyReceiver.pageLeft(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.PageLeft] = AccessibilityAction(label, action)
}

/**
 * Action to page right.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PageRight] is called.
 */
fun SemanticsPropertyReceiver.pageRight(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.PageRight] = AccessibilityAction(label, action)
}
