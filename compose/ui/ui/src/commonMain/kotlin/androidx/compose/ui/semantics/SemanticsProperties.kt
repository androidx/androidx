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
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.FillableData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import kotlin.reflect.KProperty

/**
 * General semantics properties, mainly used for accessibility and testing.
 *
 * Each of these is intended to be set by the respective SemanticsPropertyReceiver extension instead
 * of used directly.
 */
/*@VisibleForTesting*/
public object SemanticsProperties {
    /** @see SemanticsPropertyReceiver.contentDescription */
    public val ContentDescription: SemanticsPropertyKey<List<String>> =
        AccessibilityKey<List<String>>(
            name = "ContentDescription",
            mergePolicy = { parentValue, childValue ->
                parentValue?.toMutableList()?.also { it.addAll(childValue) } ?: childValue
            },
        )

    /** @see SemanticsPropertyReceiver.stateDescription */
    public val StateDescription: SemanticsPropertyKey<String> =
        AccessibilityKey<String>("StateDescription")

    /** @see SemanticsPropertyReceiver.progressBarRangeInfo */
    public val ProgressBarRangeInfo: SemanticsPropertyKey<ProgressBarRangeInfo> =
        AccessibilityKey<ProgressBarRangeInfo>("ProgressBarRangeInfo")

    /** @see SemanticsPropertyReceiver.paneTitle */
    public val PaneTitle: SemanticsPropertyKey<String> =
        AccessibilityKey<String>(
            name = "PaneTitle",
            mergePolicy = { _, _ ->
                throw IllegalStateException(
                    "merge function called on unmergeable property PaneTitle."
                )
            },
        )

    /** @see SemanticsPropertyReceiver.selectableGroup */
    public val SelectableGroup: SemanticsPropertyKey<Unit> =
        AccessibilityKey<Unit>("SelectableGroup")

    /** @see SemanticsPropertyReceiver.collectionInfo */
    public val CollectionInfo: SemanticsPropertyKey<CollectionInfo> =
        AccessibilityKey<CollectionInfo>("CollectionInfo")

    /** @see SemanticsPropertyReceiver.collectionItemInfo */
    public val CollectionItemInfo: SemanticsPropertyKey<CollectionItemInfo> =
        AccessibilityKey<CollectionItemInfo>("CollectionItemInfo")

    /** @see SemanticsPropertyReceiver.heading */
    public val Heading: SemanticsPropertyKey<Unit> = AccessibilityKey<Unit>("Heading")

    /** @see SemanticsPropertyReceiver.textEntryKey */
    public val TextEntryKey: SemanticsPropertyKey<Unit> = AccessibilityKey<Unit>("TextEntryKey")

    /** @see SemanticsPropertyReceiver.disabled */
    public val Disabled: SemanticsPropertyKey<Unit> = AccessibilityKey<Unit>("Disabled")

    /** @see SemanticsPropertyReceiver.liveRegion */
    public val LiveRegion: SemanticsPropertyKey<LiveRegionMode> =
        AccessibilityKey<LiveRegionMode>("LiveRegion")

    /** @see SemanticsPropertyReceiver.focused */
    public val Focused: SemanticsPropertyKey<Boolean> = AccessibilityKey<Boolean>("Focused")

    /** @see SemanticsPropertyReceiver.isContainer */
    @Deprecated("Use `isTraversalGroup` instead.", replaceWith = ReplaceWith("IsTraversalGroup"))
    // TODO(mnuzen): `isContainer` should not need to be an accessibility key after a new
    //  pruning API is added. See b/347038246 for more details.
    public val IsContainer: SemanticsPropertyKey<Boolean> = AccessibilityKey<Boolean>("IsContainer")

    /** @see SemanticsPropertyReceiver.isTraversalGroup */
    public val IsTraversalGroup: SemanticsPropertyKey<Boolean> =
        SemanticsPropertyKey<Boolean>("IsTraversalGroup")

    /** @see isSensitiveData */
    public val IsSensitiveData: SemanticsPropertyKey<Boolean> =
        SemanticsPropertyKey<Boolean>("IsSensitiveData")

    /** @see SemanticsPropertyReceiver.invisibleToUser */
    @Deprecated(
        "Use `hideFromAccessibility` instead.",
        replaceWith = ReplaceWith("HideFromAccessibility"),
    )
    // Retain for binary compatibility with aosp/3341487 in 1.7
    public val InvisibleToUser: SemanticsPropertyKey<Unit> =
        SemanticsPropertyKey<Unit>(
            name = "InvisibleToUser",
            mergePolicy = { parentValue, _ -> parentValue },
        )

    /** @see SemanticsPropertyReceiver.hideFromAccessibility */
    public val HideFromAccessibility: SemanticsPropertyKey<Unit> =
        SemanticsPropertyKey<Unit>(
            name = "HideFromAccessibility",
            mergePolicy = { parentValue, _ -> parentValue },
        )

    /** @see SemanticsPropertyReceiver.contentType */
    public val ContentType: SemanticsPropertyKey<ContentType> =
        SemanticsPropertyKey<ContentType>(
            name = "ContentType",
            mergePolicy = { parentValue, _ ->
                // Never merge autofill types
                parentValue
            },
        )

    /** @see SemanticsPropertyReceiver.contentDataType */
    public val ContentDataType: SemanticsPropertyKey<ContentDataType> =
        SemanticsPropertyKey<ContentDataType>(
            name = "ContentDataType",
            mergePolicy = { parentValue, _ ->
                // Never merge autofill data types
                parentValue
            },
        )

    /** @see SemanticsPropertyReceiver.fillableData */
    public val FillableData: SemanticsPropertyKey<FillableData> =
        SemanticsPropertyKey<FillableData>(
            name = "FillableData",
            mergePolicy = { parentValue, _ ->
                // Never merge autofill types
                parentValue
            },
        )

    /** @see SemanticsPropertyReceiver.traversalIndex */
    public val TraversalIndex: SemanticsPropertyKey<Float> =
        SemanticsPropertyKey<Float>(
            name = "TraversalIndex",
            mergePolicy = { parentValue, _ ->
                // Never merge traversal indices
                parentValue
            },
        )

    /** @see SemanticsPropertyReceiver.hintText */
    public val HintText: SemanticsPropertyKey<String> = AccessibilityKey<String>(name = "HintText")

    /** @see SemanticsPropertyReceiver.horizontalScrollAxisRange */
    public val HorizontalScrollAxisRange: SemanticsPropertyKey<ScrollAxisRange> =
        AccessibilityKey<ScrollAxisRange>("HorizontalScrollAxisRange")

    /** @see SemanticsPropertyReceiver.verticalScrollAxisRange */
    public val VerticalScrollAxisRange: SemanticsPropertyKey<ScrollAxisRange> =
        AccessibilityKey<ScrollAxisRange>("VerticalScrollAxisRange")

    /** @see SemanticsPropertyReceiver.popup */
    public val IsPopup: SemanticsPropertyKey<Unit> =
        AccessibilityKey<Unit>(
            name = "IsPopup",
            mergePolicy = { _, _ ->
                throw IllegalStateException(
                    "merge function called on unmergeable property IsPopup. " +
                        "A popup should not be a child of a clickable/focusable node."
                )
            },
        )

    /** @see SemanticsPropertyReceiver.dialog */
    public val IsDialog: SemanticsPropertyKey<Unit> =
        AccessibilityKey<Unit>(
            name = "IsDialog",
            mergePolicy = { _, _ ->
                throw IllegalStateException(
                    "merge function called on unmergeable property IsDialog. " +
                        "A dialog should not be a child of a clickable/focusable node."
                )
            },
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
    public val Role: SemanticsPropertyKey<Role> =
        AccessibilityKey<Role>("Role") { parentValue, _ -> parentValue }

    /** @see SemanticsPropertyReceiver.testTag */
    public val TestTag: SemanticsPropertyKey<String> =
        SemanticsPropertyKey<String>(
            name = "TestTag",
            isImportantForAccessibility = false,
            mergePolicy = { parentValue, _ ->
                // Never merge TestTags, to avoid leaking internal test tags to parents.
                parentValue
            },
        )

    /**
     * Marks a link within a text node (a link is represented by a
     * [androidx.compose.ui.text.LinkAnnotation]) for identification during automated testing. This
     * property is for internal use only and not intended for general use by developers.
     */
    public val LinkTestMarker: SemanticsPropertyKey<Unit> =
        SemanticsPropertyKey<Unit>(
            name = "LinkTestMarker",
            isImportantForAccessibility = false,
            mergePolicy = { parentValue, _ -> parentValue },
        )

    /** @see SemanticsPropertyReceiver.text */
    public val Text: SemanticsPropertyKey<List<AnnotatedString>> =
        AccessibilityKey<List<AnnotatedString>>(
            name = "Text",
            mergePolicy = { parentValue, childValue ->
                parentValue?.toMutableList()?.also { it.addAll(childValue) } ?: childValue
            },
        )

    /** @see SemanticsPropertyReceiver.textSubstitution */
    public val TextSubstitution: SemanticsPropertyKey<AnnotatedString> =
        SemanticsPropertyKey<AnnotatedString>(name = "TextSubstitution")

    /** @see SemanticsPropertyReceiver.isShowingTextSubstitution */
    public val IsShowingTextSubstitution: SemanticsPropertyKey<Boolean> =
        SemanticsPropertyKey<Boolean>("IsShowingTextSubstitution")

    /** @see SemanticsPropertyReceiver.inputText */
    public val InputText: SemanticsPropertyKey<AnnotatedString> =
        AccessibilityKey<AnnotatedString>(name = "InputText")

    /** @see SemanticsPropertyReceiver.editableText */
    public val EditableText: SemanticsPropertyKey<AnnotatedString> =
        AccessibilityKey<AnnotatedString>(name = "EditableText")

    /** @see SemanticsPropertyReceiver.textSelectionRange */
    public val TextSelectionRange: SemanticsPropertyKey<TextRange> =
        AccessibilityKey<TextRange>("TextSelectionRange")

    /** @see SemanticsPropertyReceiver.textCompositionRange */
    public val TextCompositionRange: SemanticsPropertyKey<TextRange?> =
        AccessibilityKey<TextRange?>("TextCompositionRange")

    /** @see SemanticsPropertyReceiver.onImeAction */
    public val ImeAction: SemanticsPropertyKey<ImeAction> = AccessibilityKey<ImeAction>("ImeAction")

    /** @see SemanticsPropertyReceiver.selected */
    public val Selected: SemanticsPropertyKey<Boolean> = AccessibilityKey<Boolean>("Selected")

    /** @see SemanticsPropertyReceiver.toggleableState */
    public val ToggleableState: SemanticsPropertyKey<ToggleableState> =
        AccessibilityKey<ToggleableState>("ToggleableState")

    /** @see SemanticsPropertyReceiver.inputTextSuggestionState */
    public val InputTextSuggestionState: SemanticsPropertyKey<InputTextSuggestionState> =
        AccessibilityKey<InputTextSuggestionState>("InputTextSuggestionState")

    /** @see SemanticsPropertyReceiver.password */
    public val Password: SemanticsPropertyKey<Unit> = AccessibilityKey<Unit>("Password")

    /** @see SemanticsPropertyReceiver.password */
    public val IsPasswordObfuscated: SemanticsPropertyKey<Boolean> =
        AccessibilityKey<Boolean>("IsPasswordObfuscated")

    /** @see SemanticsPropertyReceiver.error */
    public val Error: SemanticsPropertyKey<String> = AccessibilityKey<String>("Error")

    /** @see SemanticsPropertyReceiver.indexForKey */
    public val IndexForKey: SemanticsPropertyKey<(Any) -> Int> =
        SemanticsPropertyKey<(Any) -> Int>("IndexForKey")

    /** @see SemanticsPropertyReceiver.isEditable */
    public val IsEditable: SemanticsPropertyKey<Boolean> =
        SemanticsPropertyKey<Boolean>("IsEditable")

    /** @see SemanticsPropertyReceiver.maxTextLength */
    public val MaxTextLength: SemanticsPropertyKey<Int> = SemanticsPropertyKey<Int>("MaxTextLength")

    /** @see SemanticsPropertyReceiver.shape */
    public val Shape: SemanticsPropertyKey<Shape> =
        SemanticsPropertyKey<Shape>(
            name = "Shape",
            isImportantForAccessibility = false,
            mergePolicy = { parentValue, _ ->
                // Never merge shapes
                parentValue
            },
        )
}

/**
 * Ths object defines keys of the actions which can be set in semantics and performed on the
 * semantics node.
 *
 * Each of these is intended to be set by the respective SemanticsPropertyReceiver extension instead
 * of used directly.
 */
/*@VisibleForTesting*/
public object SemanticsActions {
    /** @see SemanticsPropertyReceiver.getTextLayoutResult */
    public val GetTextLayoutResult:
        SemanticsPropertyKey<AccessibilityAction<(MutableList<TextLayoutResult>) -> Boolean>> =
        ActionPropertyKey<(MutableList<TextLayoutResult>) -> Boolean>("GetTextLayoutResult")

    /** @see SemanticsPropertyReceiver.onClick */
    public val OnClick: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("OnClick")

    /** @see SemanticsPropertyReceiver.onLongClick */
    public val OnLongClick: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("OnLongClick")

    /** @see SemanticsPropertyReceiver.scrollBy */
    public val ScrollBy:
        SemanticsPropertyKey<AccessibilityAction<(x: Float, y: Float) -> Boolean>> =
        ActionPropertyKey<(x: Float, y: Float) -> Boolean>("ScrollBy")

    /** @see SemanticsPropertyReceiver.scrollByOffset */
    public val ScrollByOffset: SemanticsPropertyKey<suspend (offset: Offset) -> Offset> =
        SemanticsPropertyKey<suspend (offset: Offset) -> Offset>("ScrollByOffset")

    /** @see SemanticsPropertyReceiver.scrollToIndex */
    public val ScrollToIndex: SemanticsPropertyKey<AccessibilityAction<(Int) -> Boolean>> =
        ActionPropertyKey<(Int) -> Boolean>("ScrollToIndex")

    @Suppress("unused")
    @Deprecated(
        message = "Use `SemanticsActions.OnFillData` instead.",
        replaceWith =
            ReplaceWith("OnFillData", "androidx.compose.ui.semantics.SemanticsActions.OnFillData"),
        level = DeprecationLevel.WARNING,
    )
    public val OnAutofillText:
        SemanticsPropertyKey<AccessibilityAction<(AnnotatedString) -> Boolean>> =
        ActionPropertyKey<(AnnotatedString) -> Boolean>("OnAutofillText")

    /** @see SemanticsPropertyReceiver.onFillData */
    public val OnFillData: SemanticsPropertyKey<AccessibilityAction<(FillableData) -> Boolean>> =
        ActionPropertyKey<(FillableData) -> Boolean>("OnFillData")

    /** @see SemanticsPropertyReceiver.setProgress */
    public val SetProgress:
        SemanticsPropertyKey<AccessibilityAction<(progress: Float) -> Boolean>> =
        ActionPropertyKey<(progress: Float) -> Boolean>("SetProgress")

    /** @see SemanticsPropertyReceiver.setSelection */
    public val SetSelection:
        SemanticsPropertyKey<AccessibilityAction<(Int, Int, Boolean) -> Boolean>> =
        ActionPropertyKey<(Int, Int, Boolean) -> Boolean>("SetSelection")

    /** @see SemanticsPropertyReceiver.setText */
    public val SetText: SemanticsPropertyKey<AccessibilityAction<(AnnotatedString) -> Boolean>> =
        ActionPropertyKey<(AnnotatedString) -> Boolean>("SetText")

    /** @see SemanticsPropertyReceiver.setTextSubstitution */
    public val SetTextSubstitution:
        SemanticsPropertyKey<AccessibilityAction<(AnnotatedString) -> Boolean>> =
        ActionPropertyKey<(AnnotatedString) -> Boolean>("SetTextSubstitution")

    /** @see SemanticsPropertyReceiver.showTextSubstitution */
    public val ShowTextSubstitution:
        SemanticsPropertyKey<AccessibilityAction<(Boolean) -> Boolean>> =
        ActionPropertyKey<(Boolean) -> Boolean>("ShowTextSubstitution")

    /** @see SemanticsPropertyReceiver.clearTextSubstitution */
    public val ClearTextSubstitution: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("ClearTextSubstitution")

    /** @see SemanticsPropertyReceiver.insertTextAtCursor */
    public val InsertTextAtCursor:
        SemanticsPropertyKey<AccessibilityAction<(AnnotatedString) -> Boolean>> =
        ActionPropertyKey<(AnnotatedString) -> Boolean>("InsertTextAtCursor")

    /** @see SemanticsPropertyReceiver.onImeAction */
    public val OnImeAction: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("PerformImeAction")

    // b/322269946
    @Suppress("unused")
    @Deprecated(
        message = "Use `SemanticsActions.OnImeAction` instead.",
        replaceWith =
            ReplaceWith(
                "OnImeAction",
                "androidx.compose.ui.semantics.SemanticsActions.OnImeAction",
            ),
        level = DeprecationLevel.ERROR,
    )
    /** @see SemanticsPropertyReceiver.performImeAction */
    public val PerformImeAction: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("PerformImeAction")

    /** @see SemanticsPropertyReceiver.copyText */
    public val CopyText: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("CopyText")

    /** @see SemanticsPropertyReceiver.cutText */
    public val CutText: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("CutText")

    /** @see SemanticsPropertyReceiver.pasteText */
    public val PasteText: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("PasteText")

    /** @see SemanticsPropertyReceiver.expand */
    public val Expand: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("Expand")

    /** @see SemanticsPropertyReceiver.collapse */
    public val Collapse: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("Collapse")

    /** @see SemanticsPropertyReceiver.dismiss */
    public val Dismiss: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("Dismiss")

    /** @see SemanticsPropertyReceiver.requestFocus */
    public val RequestFocus: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("RequestFocus")

    /** @see SemanticsPropertyReceiver.customActions */
    public val CustomActions: SemanticsPropertyKey<List<CustomAccessibilityAction>> =
        AccessibilityKey<List<CustomAccessibilityAction>>(
            name = "CustomActions",
            mergePolicy = { parentValue, childValue -> parentValue.orEmpty() + childValue },
        )

    /** @see SemanticsPropertyReceiver.pageUp */
    public val PageUp: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("PageUp")

    /** @see SemanticsPropertyReceiver.pageLeft */
    public val PageLeft: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("PageLeft")

    /** @see SemanticsPropertyReceiver.pageDown */
    public val PageDown: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("PageDown")

    /** @see SemanticsPropertyReceiver.pageRight */
    public val PageRight: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>> =
        ActionPropertyKey<() -> Boolean>("PageRight")

    /** @see SemanticsPropertyReceiver.getScrollViewportLength */
    public val GetScrollViewportLength:
        SemanticsPropertyKey<AccessibilityAction<(MutableList<Float>) -> Boolean>> =
        ActionPropertyKey<(MutableList<Float>) -> Boolean>("GetScrollViewportLength")
}

/**
 * SemanticsPropertyKey is the infrastructure for setting key/value pairs inside semantics blocks in
 * a type-safe way. Each key has one particular statically defined value type T.
 */
public class SemanticsPropertyKey<T>(
    /** The name of the property. Should be the same as the constant from which it is accessed. */
    public val name: String,
    internal val mergePolicy: (T?, T) -> T? = { parentValue, childValue ->
        parentValue ?: childValue
    },
) {
    /**
     * Whether this type of property provides information relevant to accessibility services.
     *
     * Most built-in semantics properties are relevant to accessibility, but a very common exception
     * is testTag. Nodes with only a testTag still need to be included in the AccessibilityNodeInfo
     * tree because UIAutomator tests rely on that, but we mark them `isImportantForAccessibility =
     * false` on the AccessibilityNodeInfo to inform accessibility services that they are best
     * ignored.
     *
     * The default value is false and it is not exposed as a public API. That's because it is
     * impossible in the first place for `SemanticsPropertyKey`s defined outside the UI package to
     * be relevant to accessibility, because for each accessibility-relevant SemanticsProperty type
     * to get plumbed into the AccessibilityNodeInfo, the private `createNodeInfo` implementation
     * must also have a line of code.
     */
    internal var isImportantForAccessibility = false
        private set

    /**
     * If this value is non-null, this semantics property will be exposed as an accessibility extra
     * via AccessibilityNodeInfo.getExtras with this value used as the key for the extra.
     */
    internal var accessibilityExtraKey: String? = null

    internal constructor(name: String, isImportantForAccessibility: Boolean) : this(name) {
        this.isImportantForAccessibility = isImportantForAccessibility
    }

    internal constructor(
        name: String,
        isImportantForAccessibility: Boolean,
        mergePolicy: (T?, T) -> T?,
        accessibilityExtraKey: String? = null,
    ) : this(name, mergePolicy) {
        this.isImportantForAccessibility = isImportantForAccessibility
        this.accessibilityExtraKey = accessibilityExtraKey
    }

    /**
     * Method implementing the semantics merge policy of a particular key.
     *
     * When mergeDescendants is set on a semantics node, then this function will called for each
     * descendant node of a given key in depth-first-search order. The parent value accumulates the
     * result of merging the values seen so far, similar to reduce().
     *
     * The default implementation returns the parent value if one exists, otherwise uses the child
     * element. This means by default, a SemanticsNode with mergeDescendants = true winds up with
     * the first value found for each key in its subtree in depth-first-search order.
     */
    public fun merge(parentValue: T?, childValue: T): T? {
        return mergePolicy(parentValue, childValue)
    }

    /** Throws [UnsupportedOperationException]. Should not be called. */
    // TODO(KT-6519): Remove this getter
    // TODO(KT-32770): Cannot deprecate this either as the getter is considered called by "by"
    public final operator fun getValue(
        thisRef: SemanticsPropertyReceiver,
        property: KProperty<*>,
    ): T {
        return throwSemanticsGetNotSupported()
    }

    public final operator fun setValue(
        thisRef: SemanticsPropertyReceiver,
        property: KProperty<*>,
        value: T,
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

@Suppress("NOTHING_TO_INLINE")
// inline to avoid different static initialization order on different targets.
// See https://youtrack.jetbrains.com/issue/KT-65040 for more information.
internal inline fun <T> AccessibilityKey(name: String) =
    SemanticsPropertyKey<T>(name = name, isImportantForAccessibility = true)

@Suppress("NOTHING_TO_INLINE")
// inline to avoid different static initialization order on different targets
// See https://youtrack.jetbrains.com/issue/KT-65040 for more information.
internal inline fun <T> AccessibilityKey(name: String, noinline mergePolicy: (T?, T) -> T?) =
    SemanticsPropertyKey(name = name, isImportantForAccessibility = true, mergePolicy = mergePolicy)

/**
 * Standard accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should return a
 *   boolean result indicating whether the action is successfully handled. For example, a scroll
 *   forward action should return false if the widget is not enabled or has reached the end of the
 *   list. If multiple semantics blocks with the same AccessibilityAction are provided, the
 *   resulting AccessibilityAction's label/action will be the label/action of the outermost modifier
 *   with this key and nonnull label/action, or null if no nonnull label/action is found.
 */
public class AccessibilityAction<T : Function<Boolean>>(
    public val label: String?,
    public val action: T?,
) {
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
private inline fun <T : Function<Boolean>> ActionPropertyKey(name: String) =
    AccessibilityKey<AccessibilityAction<T>>(
        name = name,
        mergePolicy = { parentValue, childValue ->
            AccessibilityAction(
                parentValue?.label ?: childValue.label,
                parentValue?.action ?: childValue.action,
            )
        },
    )

/**
 * Custom accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should have no
 *   arguments and return a boolean result indicating whether the action is successfully handled.
 */
public class CustomAccessibilityAction(public val label: String, public val action: () -> Boolean) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomAccessibilityAction) return false

        if (label != other.label) return false
        if (action !== other.action) return false

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
 * Accessibility range information, to represent the status of a progress bar or seekable progress
 * bar.
 *
 * @param current current value in the range. Must not be NaN.
 * @param range range of this node
 * @param steps if greater than `0`, specifies the number of discrete values, evenly distributed
 *   between across the whole value range. If `0`, any value from the range specified can be chosen.
 *   Cannot be less than `0`.
 */
public class ProgressBarRangeInfo(
    public val current: Float,
    public val range: ClosedFloatingPointRange<Float>,
    /*@IntRange(from = 0)*/
    public val steps: Int = 0,
) {
    init {
        require(!current.isNaN()) { "current must not be NaN" }
    }

    public companion object {
        /** Accessibility range information to present indeterminate progress bar */
        public val Indeterminate: ProgressBarRangeInfo = ProgressBarRangeInfo(0f, 0f..0f)
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
 * A collection of items has [rowCount] rows and [columnCount] columns. For example, a vertical list
 * is a collection with one column, as many rows as the list items that are important for
 * accessibility; A table is a collection with several rows and several columns.
 *
 * @param rowCount the number of rows in the collection, or -1 if unknown
 * @param columnCount the number of columns in the collection, or -1 if unknown
 */
public class CollectionInfo(public val rowCount: Int, public val columnCount: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CollectionInfo) return false

        if (rowCount != other.rowCount) return false
        if (columnCount != other.columnCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rowCount.hashCode()
        result = 31 * result + columnCount.hashCode()
        return result
    }

    override fun toString(): String {
        return "CollectionInfo(rowCount=$rowCount, columnCount=$columnCount)"
    }
}

/**
 * Information about the item of a collection.
 *
 * A collection item is contained in a collection, it starts at a given [rowIndex] and [columnIndex]
 * in the collection, and spans one or more rows and columns. For example, a header of two related
 * table columns starts at the first row and the first column, spans one row and two columns.
 *
 * @param rowIndex the index of the row at which item is located
 * @param rowSpan the number of rows the item spans
 * @param columnIndex the index of the column at which item is located
 * @param columnSpan the number of columns the item spans
 */
public class CollectionItemInfo(
    public val rowIndex: Int,
    public val rowSpan: Int,
    public val columnIndex: Int,
    public val columnSpan: Int,
)

/**
 * The scroll state of one axis if this node is scrollable.
 *
 * @param value current 0-based scroll position value (either in pixels, or lazy-item count)
 * @param maxValue maximum bound for [value], or [Float.POSITIVE_INFINITY] if still unknown
 * @param reverseScrolling for horizontal scroll, when this is `true`, 0 [value] will mean right,
 *   when`false`, 0 [value] will mean left. For vertical scroll, when this is `true`, 0 [value] will
 *   mean bottom, when `false`, 0 [value] will mean top
 */
public class ScrollAxisRange(
    public val value: () -> Float,
    public val maxValue: () -> Float,
    public val reverseScrolling: Boolean = false,
) {
    override fun toString(): String =
        "ScrollAxisRange(value=${value()}, maxValue=${maxValue()}, " +
            "reverseScrolling=$reverseScrolling)"
}

/**
 * The state of an input text when suggestions are shown. This property specifies the different
 * available states the input text can be in when there are text suggestions available, typically
 * shown as a dialog window and when a user inputs a transliteration language specifically Chinese,
 * Japanese, Korean, and Vietnamese.
 *
 * On Android, this semantics property is only supported on SDK >= 37.
 *
 * @param isCommittedByInputMethodEditor whether the current text was committed by an input method
 *   editor done by the user, will stay false if the committed text was done programmatically, e.g.
 *   via Accessibility service.
 * @param isTransliterationSuggestionSelected whether a replacement text suggestion is selected to
 *   replace the transliterated text. If true, the text is from a transliteration language and is
 *   currently displaying one or multiple text suggestion replacements and that one of the
 *   suggestions is selected to replace the transliterated text. This does not indicate whether the
 *   text replacement suggestion has been committed. Will stay false for non-transliteration
 *   languages or if no suggestion is currently selected. If this were to be set to true for a
 *   non-transliteration language, it may affect accessibility services from announcing events
 *   correctly.
 */
public class InputTextSuggestionState(
    public val isCommittedByInputMethodEditor: Boolean = false,
    public val isTransliterationSuggestionSelected: Boolean = false,
) {
    override fun toString(): String =
        "InputTextSuggestionState(isCommittedByInputMethodEditor=$isCommittedByInputMethodEditor," +
            " suggestionSelected=$isTransliterationSuggestionSelected)"

    override fun hashCode(): Int {
        var result = isCommittedByInputMethodEditor.hashCode()
        result = 31 * result + isTransliterationSuggestionSelected.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InputTextSuggestionState) return false

        if (isCommittedByInputMethodEditor != other.isCommittedByInputMethodEditor) return false
        if (isTransliterationSuggestionSelected != other.isTransliterationSuggestionSelected)
            return false

        return true
    }

    @Suppress("unused")
    @Deprecated(
        message = "Use the new constructor that accepts the [isSuggestionSelected] parameter",
        level = DeprecationLevel.HIDDEN,
    )
    public constructor(
        isCommittedByInputMethodEditor: Boolean = false
    ) : this(isCommittedByInputMethodEditor, false)
}

/**
 * The type of user interface element. Accessibility services might use this to describe the element
 * or do customizations. Most roles can be automatically resolved by the semantics properties of
 * this element. But some elements with subtle differences need an exact role. If an exact role is
 * not listed, [SemanticsPropertyReceiver.role] should not be set and the framework will
 * automatically resolve it.
 */
@Immutable
@kotlin.jvm.JvmInline
public value class Role private constructor(@Suppress("unused") private val value: Int) {
    public companion object {
        /**
         * This element is a button control. Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsActions.OnClick]
         */
        public val Button: Role
            get() = Role(0)

        /**
         * This element is a Checkbox which is a component that represents two states (checked /
         * unchecked). Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
         * [SemanticsActions.OnClick]
         */
        public val Checkbox: Role
            get() = Role(1)

        /**
         * This element is a Switch which is a two state toggleable component that provides on/off
         * like options. Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
         * [SemanticsActions.OnClick]
         */
        public val Switch: Role
            get() = Role(2)

        /**
         * This element is a RadioButton which is a component to represent two states, selected and
         * not selected. Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
         * [SemanticsActions.OnClick]
         */
        public val RadioButton: Role
            get() = Role(3)

        /**
         * This element is a Tab which represents a single page of content using a text label and/or
         * icon. A Tab also has two states: selected and not selected. Associated semantics
         * properties for accessibility: [SemanticsProperties.Disabled],
         * [SemanticsProperties.StateDescription], [SemanticsActions.OnClick]
         */
        public val Tab: Role
            get() = Role(4)

        /**
         * This element is an image. Associated semantics properties for accessibility:
         * [SemanticsProperties.ContentDescription]
         */
        public val Image: Role
            get() = Role(5)

        /**
         * This element is associated with a drop down menu. Associated semantics properties for
         * accessibility: [SemanticsActions.OnClick]
         */
        public val DropdownList: Role
            get() = Role(6)

        /**
         * This element is a value picker. It should support the following accessibility actions to
         * enable selection of the next and previous values:
         *
         * [android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD]: Select the next
         * value.
         *
         * [android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD]: Select the
         * previous value.
         *
         * These actions allow accessibility services to interact with this node programmatically on
         * behalf of users, facilitating navigation within sets of selectable values.
         */
        public val ValuePicker: Role
            get() = Role(7)

        /**
         * This element is a Carousel. This means that even if Pager actions are added, this element
         * will behave like a regular List collection.
         *
         * Associated semantics properties for Pager accessibility actions:
         * [SemanticsActions.PageUp],[SemanticsActions.PageDown],[SemanticsActions.PageLeft],
         * [SemanticsActions.PageRight]
         */
        public val Carousel: Role
            get() = Role(8)
    }

    public override fun toString(): String =
        when (this) {
            Button -> "Button"
            Checkbox -> "Checkbox"
            Switch -> "Switch"
            RadioButton -> "RadioButton"
            Tab -> "Tab"
            Image -> "Image"
            DropdownList -> "DropdownList"
            ValuePicker -> "Picker"
            Carousel -> "Carousel"
            else -> "Unknown"
        }
}

/**
 * The mode of live region. Live region indicates to accessibility services they should
 * automatically notify the user about changes to the node's content description or text, or to the
 * content descriptions or text of the node's children (where applicable).
 */
@Immutable
@kotlin.jvm.JvmInline
public value class LiveRegionMode private constructor(@Suppress("unused") private val value: Int) {
    public companion object {
        /**
         * Live region mode specifying that accessibility services should announce changes to this
         * node.
         */
        public val Polite: LiveRegionMode
            get() = LiveRegionMode(0)

        /**
         * Live region mode specifying that accessibility services should interrupt ongoing speech
         * to immediately announce changes to this node.
         */
        public val Assertive: LiveRegionMode
            get() = LiveRegionMode(1)
    }

    public override fun toString(): String =
        when (this) {
            Polite -> "Polite"
            Assertive -> "Assertive"
            else -> "Unknown"
        }
}

/**
 * SemanticsPropertyReceiver is the scope provided by semantics {} blocks, letting you set key/value
 * pairs primarily via extension functions.
 */
public interface SemanticsPropertyReceiver {
    public operator fun <T> set(key: SemanticsPropertyKey<T>, value: T)
}

/**
 * Developer-set content description of the semantics node.
 *
 * If this is not set, accessibility services will present the [text][SemanticsProperties.Text] of
 * this node as the content.
 *
 * This typically should not be set directly by applications, because some screen readers will cease
 * presenting other relevant information when this property is present. This is intended to be used
 * via Foundation components which are inherently intractable to automatically describe, such as
 * Image, Icon, and Canvas.
 */
public var SemanticsPropertyReceiver.contentDescription: String
    get() = throwSemanticsGetNotSupported()
    set(value) {
        set(SemanticsProperties.ContentDescription, listOf(value))
    }

/**
 * The hint text for an editable text field. This is typically used to provide guidance to the user
 * about what to enter in the text field.
 *
 * @sample androidx.compose.ui.samples.HintTextSample
 */
public var SemanticsPropertyReceiver.hintText: String by SemanticsProperties.HintText

/**
 * Developer-set state description of the semantics node.
 *
 * For example: on/off. If this not set, accessibility services will derive the state from other
 * semantics properties, like [ProgressBarRangeInfo], but it is not guaranteed and the format will
 * be decided by accessibility services.
 */
public var SemanticsPropertyReceiver.stateDescription: String by
    SemanticsProperties.StateDescription

/**
 * The semantics represents a range of possible values with a current value. For example, when used
 * on a slider control, this will allow screen readers to communicate the slider's state.
 */
public var SemanticsPropertyReceiver.progressBarRangeInfo: ProgressBarRangeInfo by
    SemanticsProperties.ProgressBarRangeInfo

/**
 * The node is marked as heading for accessibility.
 *
 * @see SemanticsProperties.Heading
 */
public fun SemanticsPropertyReceiver.heading(): Unit {
    this[SemanticsProperties.Heading] = Unit
}

/**
 * The node is marked as a text entry key for accessibility. This is used to indicate that this
 * composable acts as a key within a text entry interface, such as a custom on-screen keyboard.
 * Accessibility services can use this information to provide a better experience for users
 * interacting with custom text input methods.
 *
 * See
 * [AccessibilityNodeInfo.setTextEntryKey](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo#setTextEntryKey(boolean))
 * for more details.
 *
 * @see SemanticsProperties.TextEntryKey
 */
public fun SemanticsPropertyReceiver.textEntryKey(): Unit {
    this[SemanticsProperties.TextEntryKey] = Unit
}

/**
 * Accessibility-friendly title for a screen's pane. For accessibility purposes, a pane is a
 * visually distinct portion of a window, such as the contents of a open drawer. In order for
 * accessibility services to understand a pane's window-like behavior, you should give descriptive
 * titles to your app's panes. Accessibility services can then provide more granular information to
 * users when a pane's appearance or content changes.
 *
 * @see SemanticsProperties.PaneTitle
 */
public var SemanticsPropertyReceiver.paneTitle: String by SemanticsProperties.PaneTitle

/**
 * Whether this semantics node is disabled. Note that proper [SemanticsActions] should still be
 * added when this property is set.
 *
 * @see SemanticsProperties.Disabled
 */
public fun SemanticsPropertyReceiver.disabled(): Unit {
    this[SemanticsProperties.Disabled] = Unit
}

/**
 * This node is marked as live region for accessibility. This indicates to accessibility services
 * they should automatically notify the user about changes to the node's content description or
 * text, or to the content descriptions or text of the node's children (where applicable). It should
 * be used with caution, especially with assertive mode which immediately stops the current audio
 * and the user does not hear the rest of the content. An example of proper use is a Snackbar which
 * is marked as [LiveRegionMode.Polite].
 *
 * @see SemanticsProperties.LiveRegion
 * @see LiveRegionMode
 */
public var SemanticsPropertyReceiver.liveRegion: LiveRegionMode by SemanticsProperties.LiveRegion

/**
 * Whether this semantics node is focused. The presence of this property indicates this node is
 * focusable
 *
 * @see SemanticsProperties.Focused
 */
public var SemanticsPropertyReceiver.focused: Boolean by SemanticsProperties.Focused

/**
 * Whether this semantics node is a container. This is defined as a node whose function is to serve
 * as a boundary or border in organizing its children.
 *
 * @see SemanticsProperties.IsContainer
 */
@Deprecated("Use `isTraversalGroup` instead.", replaceWith = ReplaceWith("isTraversalGroup"))
@Suppress("DEPRECATION")
public var SemanticsPropertyReceiver.isContainer: Boolean by SemanticsProperties.IsContainer

/**
 * Whether this semantics node is a traversal group.
 *
 * See https://developer.android.com/develop/ui/compose/accessibility/traversal
 *
 * @see SemanticsProperties.IsTraversalGroup
 */
public var SemanticsPropertyReceiver.isTraversalGroup: Boolean by
    SemanticsProperties.IsTraversalGroup

/**
 * Whether this semantics node should only allow interactions from
 * [android.accessibilityservice.AccessibilityService]s with the
 * [android.accessibilityservice.AccessibilityServiceInfo.isAccessibilityTool] property set to true.
 *
 * This property allows the node to remain visible and interactive to Accessibility Services
 * declared as accessibility tools that assist users with disabilities, while simultaneously hiding
 * this node and its generated AccessibilityEvents from other Accessibility Services that are not
 * declared as accessibility tools.
 *
 * If looking for a way to hide the node from all Accessibility Services then consider
 * [SemanticsProperties.HideFromAccessibility] instead.
 *
 * @see SemanticsProperties.IsSensitiveData
 */
public var SemanticsPropertyReceiver.isSensitiveData: Boolean by SemanticsProperties.IsSensitiveData

/**
 * Whether this node is specially known to be invisible to the user.
 *
 * For example, if the node is currently occluded by a dark semitransparent pane above it, then for
 * all practical purposes the node is invisible to the user, but the system cannot automatically
 * determine that. To make the screen reader linear navigation skip over this type of invisible
 * node, this property can be set.
 *
 * If looking for a way to hide semantics of small items from screen readers because they're
 * redundant with semantics of their parent, consider [SemanticsModifier.clearAndSetSemantics]
 * instead.
 */
@Deprecated(
    "Use `hideFromAccessibility()` instead.",
    replaceWith = ReplaceWith("hideFromAccessibility()"),
)
@Suppress("DEPRECATION")
// Retain for binary compatibility with aosp/3341487 in 1.7
public fun SemanticsPropertyReceiver.invisibleToUser(): Unit {
    this[SemanticsProperties.InvisibleToUser] = Unit
}

/**
 * If present, this node is considered hidden from accessibility services.
 *
 * For example, if the node is currently occluded by a dark semitransparent pane above it, then for
 * all practical purposes the node should not be announced to the user. Since the system cannot
 * automatically determine that, this property can be set to make the screen reader linear
 * navigation skip over this type of node.
 *
 * If looking for a way to clear semantics of small items from the UI tree completely because they
 * are redundant with semantics of their parent, consider [SemanticsModifier.clearAndSetSemantics]
 * instead.
 */
public fun SemanticsPropertyReceiver.hideFromAccessibility(): Unit {
    this[SemanticsProperties.HideFromAccessibility] = Unit
}

/**
 * Content field type information.
 *
 * This API can be used to indicate to Autofill services what _kind of field_ is associated with
 * this node. Not to be confused with the _data type_ to be entered into the field.
 *
 * @see SemanticsProperties.ContentType
 */
public var SemanticsPropertyReceiver.contentType: ContentType by SemanticsProperties.ContentType

/**
 * Content data type information.
 *
 * This API can be used to indicate to Autofill services what _kind of data_ is meant to be
 * suggested for this field. Not to be confused with the _type_ of the field.
 *
 * @see SemanticsProperties.ContentType
 */
public var SemanticsPropertyReceiver.contentDataType: ContentDataType by
    SemanticsProperties.ContentDataType

/**
 * The current value of a component that can be autofilled.
 *
 * This property is used to expose the component's current data *to* the autofill service. The
 * service can then read this value, for example, to save it for future autofill suggestions.
 *
 * This is the counterpart to the [onFillData] action, which is used to *receive* data from the
 * autofill service.
 *
 * @sample androidx.compose.ui.samples.AutofillableTextFieldWithFillableDataSemantics
 * @see SemanticsProperties.FillableData
 */
public var SemanticsPropertyReceiver.fillableData: FillableData by SemanticsProperties.FillableData

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
 * happen by default. As another example, if you need to reorder two Buttons within a Row, then you
 * can set `isTraversalGroup = true` on the Row, and set `traversalIndex` on one of the Buttons.
 *
 * Note that if `traversalIndex` seems to have no effect, be sure to set `isTraversalGroup = true`
 * as well.
 */
public var SemanticsPropertyReceiver.traversalIndex: Float by SemanticsProperties.TraversalIndex

/** The horizontal scroll state of this node if this node is scrollable. */
public var SemanticsPropertyReceiver.horizontalScrollAxisRange: ScrollAxisRange by
    SemanticsProperties.HorizontalScrollAxisRange

/** The vertical scroll state of this node if this node is scrollable. */
public var SemanticsPropertyReceiver.verticalScrollAxisRange: ScrollAxisRange by
    SemanticsProperties.VerticalScrollAxisRange

/**
 * Whether this semantics node represents a Popup. Not to be confused with if this node is _part of_
 * a Popup.
 */
public fun SemanticsPropertyReceiver.popup(): Unit {
    this[SemanticsProperties.IsPopup] = Unit
}

/**
 * Whether this element is a Dialog. Not to be confused with if this element is _part of_ a Dialog.
 */
public fun SemanticsPropertyReceiver.dialog(): Unit {
    this[SemanticsProperties.IsDialog] = Unit
}

/**
 * The type of user interface element. Accessibility services might use this to describe the element
 * or do customizations. Most roles can be automatically resolved by the semantics properties of
 * this element. But some elements with subtle differences need an exact role. If an exact role is
 * not listed in [Role], this property should not be set and the framework will automatically
 * resolve it.
 */
public var SemanticsPropertyReceiver.role: Role by SemanticsProperties.Role

/**
 * Test tag attached to this semantics node.
 *
 * This can be used to find nodes in testing frameworks:
 * - In Compose's built-in unit test framework, use with
 *   [onNodeWithTag][androidx.compose.ui.test.onNodeWithTag].
 * - For newer AccessibilityNodeInfo-based integration test frameworks, it can be matched in the
 *   extras with key "androidx.compose.ui.semantics.testTag"
 * - For legacy AccessibilityNodeInfo-based integration tests, it's optionally exposed as the
 *   resource id if [testTagsAsResourceId] is true (for matching with 'By.res' in UIAutomator).
 */
public var SemanticsPropertyReceiver.testTag: String by SemanticsProperties.TestTag

/**
 * Text of the semantics node. It must be real text instead of developer-set content description.
 *
 * @see SemanticsPropertyReceiver.editableText
 */
public var SemanticsPropertyReceiver.text: AnnotatedString
    get() = throwSemanticsGetNotSupported()
    set(value) {
        set(SemanticsProperties.Text, listOf(value))
    }

/**
 * Text substitution of the semantics node. This property is only available after calling
 * [SemanticsActions.SetTextSubstitution].
 */
public var SemanticsPropertyReceiver.textSubstitution: AnnotatedString by
    SemanticsProperties.TextSubstitution

/**
 * Whether this element is showing the text substitution. This property is only available after
 * calling [SemanticsActions.SetTextSubstitution].
 */
public var SemanticsPropertyReceiver.isShowingTextSubstitution: Boolean by
    SemanticsProperties.IsShowingTextSubstitution

/**
 * The raw value of the text field after input transformations have been applied.
 *
 * This is an actual user input of the fields, e.g. a real password, after any input transformations
 * that might change or reject that input have been applied. This value is not affected by visual
 * transformations.
 */
public var SemanticsPropertyReceiver.inputText: AnnotatedString by SemanticsProperties.InputText

/**
 * A visual value of the text field after output transformations that change the visual
 * representation of the field's state have been applied.
 *
 * This is the value displayed to the user, for example "*******" in a password field.
 */
public var SemanticsPropertyReceiver.editableText: AnnotatedString by
    SemanticsProperties.EditableText

/** Text selection range for the text field. */
public var SemanticsPropertyReceiver.textSelectionRange: TextRange by
    SemanticsProperties.TextSelectionRange

/** Text composition range for the text field. */
public var SemanticsPropertyReceiver.textCompositionRange: TextRange? by
    SemanticsProperties.TextCompositionRange

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
public var SemanticsPropertyReceiver.imeAction: ImeAction by SemanticsProperties.ImeAction

/**
 * Whether this element is selected (out of a list of possible selections).
 *
 * The presence of this property indicates that the element is selectable.
 */
public var SemanticsPropertyReceiver.selected: Boolean by SemanticsProperties.Selected

/**
 * This semantics marks node as a collection and provides the required information.
 *
 * @see collectionItemInfo
 */
public var SemanticsPropertyReceiver.collectionInfo: CollectionInfo by
    SemanticsProperties.CollectionInfo

/**
 * This semantics marks node as an items of a collection and provides the required information.
 *
 * If you mark items of a collection, you should also be marking the collection with
 * [collectionInfo].
 */
public var SemanticsPropertyReceiver.collectionItemInfo: CollectionItemInfo by
    SemanticsProperties.CollectionItemInfo

/**
 * The state of a toggleable component.
 *
 * The presence of this property indicates that the element is toggleable.
 */
public var SemanticsPropertyReceiver.toggleableState: ToggleableState by
    SemanticsProperties.ToggleableState

/**
 * This semantics provides the state of a text that has active suggestions. Text with suggestions
 * are typically associated with typing transliteration languages such as Chinese, Japanese, Korean
 * where multiple text replacement suggestions appear.
 *
 * It is used by accessibility services to determine what speech feedback should be announced as the
 * user is typing a transliteration text. For example, whether to announce that a replacement text
 * is selected.
 */
public var SemanticsPropertyReceiver.inputTextSuggestionState: InputTextSuggestionState by
    SemanticsProperties.InputTextSuggestionState

/** Whether this semantics node is editable, e.g. an editable text field. */
public var SemanticsPropertyReceiver.isEditable: Boolean by SemanticsProperties.IsEditable

/**
 * Marks this semantics node as a password field (e.g. for credentials and sensitive text input).
 *
 * Setting password semantics identifies the node as a credential field, ensuring appropriate
 * handling for credential security, autofill, and speech masking. If the password text is visually
 * revealed to the user, pass [isPasswordObfuscated] as `false` to allow screen readers (such as
 * TalkBack) to announce the revealed characters aloud.
 *
 * Note: [isPasswordObfuscated] is descriptive for accessibility services and tests; it does not
 * control the visual masking or rendering of the password text field itself (which is typically
 * managed by the text field's visual or codepoint transformation).
 *
 * @param isPasswordObfuscated whether the password in a secure text field is currently visually
 *   masked with obfuscation characters. When false (revealed), accessibility services such as
 *   TalkBack may announce the revealed character content aloud to the user instead of masking it.
 * @see SemanticsProperties.Password
 * @see SemanticsProperties.IsPasswordObfuscated
 */
public fun SemanticsPropertyReceiver.password(isPasswordObfuscated: Boolean = true): Unit {
    this[SemanticsProperties.Password] = Unit
    this[SemanticsProperties.IsPasswordObfuscated] = isPasswordObfuscated
}

@Deprecated(message = "Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun SemanticsPropertyReceiver.password(): Unit {
    password(isPasswordObfuscated = true)
}

/**
 * Mark semantics node that contains invalid input or error.
 *
 * @param [description] a localized description explaining an error to the accessibility user
 */
public fun SemanticsPropertyReceiver.error(description: String): Unit {
    this[SemanticsProperties.Error] = description
}

/**
 * The index of an item identified by a given key. The key is usually defined during the creation of
 * the container. If the key did not match any of the items' keys, the [mapping] must return -1.
 */
public fun SemanticsPropertyReceiver.indexForKey(mapping: (Any) -> Int): Unit {
    this[SemanticsProperties.IndexForKey] = mapping
}

/**
 * Limits the number of characters that can be entered, e.g. in an editable text field. By default
 * this value is -1, signifying there is no maximum text length limit.
 */
public var SemanticsPropertyReceiver.maxTextLength: Int by SemanticsProperties.MaxTextLength

/** The shape of the UI element. */
public var SemanticsPropertyReceiver.shape: Shape by SemanticsProperties.Shape

/**
 * The node is marked as a collection of horizontally or vertically stacked selectable elements.
 *
 * Unlike [collectionInfo] which marks a collection of any elements and asks developer to provide
 * all the required information like number of elements etc., this semantics will populate the
 * number of selectable elements automatically. Note that if you use this semantics with lazy
 * collections, it won't get the number of elements in the collection.
 *
 * @see SemanticsPropertyReceiver.selected
 */
public fun SemanticsPropertyReceiver.selectableGroup(): Unit {
    this[SemanticsProperties.SelectableGroup] = Unit
}

/** Custom actions which are defined by app developers. */
public var SemanticsPropertyReceiver.customActions: List<CustomAccessibilityAction> by
    SemanticsActions.CustomActions

/**
 * Action to get a Text/TextField node's [TextLayoutResult]. The result is the first element of
 * layout (the argument of the AccessibilityAction).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.GetTextLayoutResult] is called.
 */
public fun SemanticsPropertyReceiver.getTextLayoutResult(
    label: String? = null,
    action: ((MutableList<TextLayoutResult>) -> Boolean)?,
): Unit {
    this[SemanticsActions.GetTextLayoutResult] = AccessibilityAction(label, action)
}

/**
 * Action to be performed when the node is clicked (single-tapped).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnClick] is called.
 */
public fun SemanticsPropertyReceiver.onClick(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsActions.OnClick] = AccessibilityAction(label, action)
}

/**
 * Action to be performed when the node is long clicked (long-pressed).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnLongClick] is called.
 */
public fun SemanticsPropertyReceiver.onLongClick(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsActions.OnLongClick] = AccessibilityAction(label, action)
}

/**
 * Action to asynchronously scroll by a specified amount.
 *
 * [scrollByOffset] should be preferred in most cases, since it is synchronous and returns the
 * amount of scroll that was actually consumed.
 *
 * Expected to be used in conjunction with [verticalScrollAxisRange]/[horizontalScrollAxisRange].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.ScrollBy] is called.
 */
public fun SemanticsPropertyReceiver.scrollBy(
    label: String? = null,
    action: ((x: Float, y: Float) -> Boolean)?,
): Unit {
    this[SemanticsActions.ScrollBy] = AccessibilityAction(label, action)
}

/**
 * Action to scroll by a specified amount and return how much of the offset was actually consumed.
 * E.g. if the node can't scroll at all in the given direction, [Offset.Zero] should be returned.
 * The action should not return until the scroll operation has finished.
 *
 * Expected to be used in conjunction with [verticalScrollAxisRange]/[horizontalScrollAxisRange].
 *
 * Unlike [scrollBy], this action is synchronous, and returns the amount of scroll consumed.
 *
 * @param action Action to be performed when [SemanticsActions.ScrollByOffset] is called.
 */
public fun SemanticsPropertyReceiver.scrollByOffset(
    action: suspend (offset: Offset) -> Offset
): Unit {
    this[SemanticsActions.ScrollByOffset] = action
}

/**
 * Action to scroll a container to the index of one of its items.
 *
 * The [action] should throw an [IllegalArgumentException] if the index is out of bounds.
 */
public fun SemanticsPropertyReceiver.scrollToIndex(
    label: String? = null,
    action: (Int) -> Boolean,
): Unit {
    this[SemanticsActions.ScrollToIndex] = AccessibilityAction(label, action)
}

/**
 * Action to autofill a TextField.
 *
 * Expected to be used in conjunction with [contentType] and [contentDataType] properties.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnAutofillText] is called.
 */
@Deprecated(
    message = "Use onFillData instead",
    replaceWith = ReplaceWith("onFillData"),
    level = DeprecationLevel.WARNING,
)
public fun SemanticsPropertyReceiver.onAutofillText(
    label: String? = null,
    action: ((AnnotatedString) -> Boolean)?,
): Unit {
    @Suppress("DEPRECATION")
    this[SemanticsActions.OnAutofillText] = AccessibilityAction(label, action)
}

/**
 * Action that an autofill service can invoke to fill the component with data.
 *
 * The [action] will be called by the system, passing the [FillableData] that should be used to
 * update the component's state.
 *
 * This is the counterpart to the [fillableData] property, which is used to *provide* the
 * component's current data to the autofill service.
 *
 * @sample androidx.compose.ui.samples.AutofillableTextFieldWithFillableDataSemantics
 * @param label Optional label for this action.
 * @param action Action to be performed when [SemanticsActions.OnFillData] is called. The lambda
 *   receives the [FillableData] from the autofill service.
 */
public fun SemanticsPropertyReceiver.onFillData(
    label: String? = null,
    action: ((FillableData) -> Boolean)?,
): Unit {
    this[SemanticsActions.OnFillData] = AccessibilityAction(label, action)
}

/**
 * Action to set the current value of the progress bar.
 *
 * Expected to be used in conjunction with progressBarRangeInfo.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetProgress] is called.
 */
public fun SemanticsPropertyReceiver.setProgress(
    label: String? = null,
    action: ((Float) -> Boolean)?,
): Unit {
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
public fun SemanticsPropertyReceiver.setText(
    label: String? = null,
    action: ((AnnotatedString) -> Boolean)?,
): Unit {
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
public fun SemanticsPropertyReceiver.setTextSubstitution(
    label: String? = null,
    action: ((AnnotatedString) -> Boolean)?,
): Unit {
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
public fun SemanticsPropertyReceiver.showTextSubstitution(
    label: String? = null,
    action: ((Boolean) -> Boolean)?,
): Unit {
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
public fun SemanticsPropertyReceiver.clearTextSubstitution(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
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
public fun SemanticsPropertyReceiver.insertTextAtCursor(
    label: String? = null,
    action: ((AnnotatedString) -> Boolean)?,
): Unit {
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
 * @see SemanticsProperties.ImeAction
 * @see SemanticsActions.OnImeAction
 */
public fun SemanticsPropertyReceiver.onImeAction(
    imeActionType: ImeAction,
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsProperties.ImeAction] = imeActionType
    this[SemanticsActions.OnImeAction] = AccessibilityAction(label, action)
}

// b/322269946
@Suppress("unused")
@Deprecated(
    message = "Use `SemanticsPropertyReceiver.onImeAction` instead.",
    replaceWith =
        ReplaceWith(
            "onImeAction(imeActionType = ImeAction.Default, label = label, action = action)",
            "androidx.compose.ui.semantics.onImeAction",
            "androidx.compose.ui.text.input.ImeAction",
        ),
    level = DeprecationLevel.ERROR,
)
public fun SemanticsPropertyReceiver.performImeAction(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsActions.OnImeAction] = AccessibilityAction(label, action)
}

/**
 * Action to set text selection by character index range.
 *
 * If this action is provided, the selection data must be provided using [textSelectionRange].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetSelection] is called. The
 *   parameters to the action are: `startIndex`, `endIndex`, and whether the indices are relative to
 *   the original text or the transformed text (when a `VisualTransformation` is applied).
 */
public fun SemanticsPropertyReceiver.setSelection(
    label: String? = null,
    action: ((startIndex: Int, endIndex: Int, relativeToOriginalText: Boolean) -> Boolean)?,
): Unit {
    this[SemanticsActions.SetSelection] = AccessibilityAction(label, action)
}

/**
 * Action to copy the text to the clipboard.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.CopyText] is called.
 */
public fun SemanticsPropertyReceiver.copyText(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsActions.CopyText] = AccessibilityAction(label, action)
}

/**
 * Action to cut the text and copy it to the clipboard.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.CutText] is called.
 */
public fun SemanticsPropertyReceiver.cutText(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsActions.CutText] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.PasteText] to the [SemanticsPropertyReceiver]. Use it to
 * indicate that element is open for accepting paste data from the clipboard. There is no need to
 * check if the clipboard data available as this is done by the framework. For this action to be
 * triggered, the element must also have the [SemanticsProperties.Focused] property set.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PasteText] is called.
 * @see focused
 */
public fun SemanticsPropertyReceiver.pasteText(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsActions.PasteText] = AccessibilityAction(label, action)
}

/**
 * Action to expand an expandable node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Expand] is called.
 */
public fun SemanticsPropertyReceiver.expand(label: String? = null, action: (() -> Boolean)?): Unit {
    this[SemanticsActions.Expand] = AccessibilityAction(label, action)
}

/**
 * Action to collapse an expandable node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Collapse] is called.
 */
public fun SemanticsPropertyReceiver.collapse(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsActions.Collapse] = AccessibilityAction(label, action)
}

/**
 * Action to dismiss a dismissible node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Dismiss] is called.
 */
public fun SemanticsPropertyReceiver.dismiss(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsActions.Dismiss] = AccessibilityAction(label, action)
}

/**
 * Action that gives input focus to this node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.RequestFocus] is called.
 */
public fun SemanticsPropertyReceiver.requestFocus(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsActions.RequestFocus] = AccessibilityAction(label, action)
}

/**
 * Action to page up.
 *
 * Using [Role.Carousel] will prevent this action from being sent to accessibility services.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PageUp] is called.
 * @see [Role.Carousel] for more information.
 */
public fun SemanticsPropertyReceiver.pageUp(label: String? = null, action: (() -> Boolean)?): Unit {
    this[SemanticsActions.PageUp] = AccessibilityAction(label, action)
}

/**
 * Action to page down.
 *
 * Using [Role.Carousel] will prevent this action from being sent to accessibility services.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PageDown] is called.
 * @see [Role.Carousel] for more information.
 */
public fun SemanticsPropertyReceiver.pageDown(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsActions.PageDown] = AccessibilityAction(label, action)
}

/**
 * Action to page left.
 *
 * Using [Role.Carousel] will prevent this action from being sent to accessibility services.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PageLeft] is called.
 * @see [Role.Carousel] for more information.
 */
public fun SemanticsPropertyReceiver.pageLeft(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsActions.PageLeft] = AccessibilityAction(label, action)
}

/**
 * Action to page right.
 *
 * Using [Role.Carousel] will prevent this action from being sent to accessibility services.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PageRight] is called.
 * @see [Role.Carousel] for more information.
 */
public fun SemanticsPropertyReceiver.pageRight(
    label: String? = null,
    action: (() -> Boolean)?,
): Unit {
    this[SemanticsActions.PageRight] = AccessibilityAction(label, action)
}

/**
 * Action to get a scrollable's active view port amount for scrolling actions.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.GetScrollViewportLength] is
 *   called.
 */
public fun SemanticsPropertyReceiver.getScrollViewportLength(
    label: String? = null,
    action: (() -> Float?),
): Unit {
    this[SemanticsActions.GetScrollViewportLength] =
        AccessibilityAction(label) {
            val viewport = action.invoke()
            if (viewport == null) {
                false
            } else {
                it.add(viewport)
                true
            }
        }
}
