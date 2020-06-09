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

package androidx.ui.semantics

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * General semantics properties, mainly used for accessibility.
 */
object SemanticsProperties {
    /**
     * Developer-set content description of the semantics node. If this is not set, accessibility
     * services will present the text of this node as content part.
     *
     * @see SemanticsPropertyReceiver.accessibilityLabel
     */
    val AccessibilityLabel = object : SemanticsPropertyKey<String>("AccessibilityLabel") {
        override fun merge(existingValue: String, newValue: String): String {
            // TODO(b/138173613): Needs TextDirection, probably needs to pass both nodes
            //  to retrieve it
            return existingValue + "\n" + newValue
        }
    }

    /**
     * Developer-set state description of the semantics node. For example: on/off. If this not
     * set, accessibility services will derive the state from other semantics properties, like
     * [AccessibilityRangeInfo], but it is not guaranteed and the format will be decided by
     * accessibility services.
     *
     * @see SemanticsPropertyReceiver.accessibilityValue
     */
    val AccessibilityValue = SemanticsPropertyKey<String>("AccessibilityValue")

    /**
     * The node is a range with current value.
     *
     * @see SemanticsPropertyReceiver.accessibilityValueRange
     */
    val AccessibilityRangeInfo =
        SemanticsPropertyKey<AccessibilityRangeInfo>("AccessibilityRangeInfo")

    /**
     * Whether this semantics node is enabled.
     *
     * @see SemanticsPropertyReceiver.enabled
     */
    val Enabled = SemanticsPropertyKey<Boolean>("Enabled")

    /**
     * Whether this semantics node is hidden.
     *
     * @see SemanticsPropertyReceiver.hidden
     */
    val Hidden = SemanticsPropertyKey<Boolean>("Hidden")

    /**
     * Whether this semantics node represents a Popup. Not to be confused with if this node is
     * _part of_ a Popup.
     *
     * @see SemanticsPropertyReceiver.popup
     */
    val IsPopup = SemanticsPropertyKey<Boolean>("IsPopup")

    // TODO(b/151228491): TextDirection needs to be in core for platform use
    // val TextDirection = SemanticsPropertyKey<TextDirection>("TextDirection")

    // TODO(b/138172781): Move to FoundationSemanticsProperties
    /**
     * Test tag attached to this semantics node.
     *
     * @see SemanticsPropertyReceiver.testTag
     */
    val TestTag = SemanticsPropertyKey<String>("TestTag")
}

/**
 * Ths object defines keys of the actions which can be set in semantics and performed on the
 * semantics node.
 */
object SemanticsActions {
    /**
     * Action to be performed when the node is clicked.
     *
     * @see SemanticsPropertyReceiver.onClick
     */
    val OnClick = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("OnClick")

    /**
     * Action to scroll to a specified position.
     *
     * @see SemanticsPropertyReceiver.ScrollTo
     */
    val ScrollTo =
        SemanticsPropertyKey<AccessibilityAction<(x: Float, y: Float) -> Boolean>>("ScrollTo")

    /**
     * Action to scroll the content forward.
     *
     * @see SemanticsPropertyReceiver.scrollForward
     */
    @Deprecated("Use scroll up/down/left/right instead. Need more discussion")
    // TODO(b/157692376): remove scroll forward/backward api together with slider scroll action.
    val ScrollForward =
        SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("ScrollForward")

    /**
     * Action to scroll the content backward.
     *
     * @see SemanticsPropertyReceiver.scrollBackward
     */
    @Deprecated("Use scroll up/down/left/right instead. Need more discussion.")
    // TODO(b/157692376): remove scroll forward/backward api together with slider scroll action.
    val ScrollBackward =
        SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("ScrollForward")

    /**
     * Action to set slider progress.
     *
     * @see SemanticsPropertyReceiver.setProgress
     */
    val SetProgress =
        SemanticsPropertyKey<AccessibilityAction<(progress: Float) -> Boolean>>("SetProgress")

    /**
     * Custom actions which are defined by app developers.
     *
     * @see SemanticsPropertyReceiver.customActions
     */
    val CustomActions =
        SemanticsPropertyKey<List<CustomAccessibilityAction>>("CustomActions")
}

open class SemanticsPropertyKey<T>(
    /**
     * The name of the property.  Should be the same as the constant from which it is accessed.
     */
    val name: String
) :
    ReadWriteProperty<SemanticsPropertyReceiver, T> {
    /**
     * Subclasses that wish to implement merging should override this to output the merged value
     *
     * This implementation always throws IllegalStateException.  It should be overridden for
     * properties that can be merged.
     */
    open fun merge(existingValue: T, newValue: T): T {
        throw IllegalStateException(
            "merge function called on unmergeable property $name. " +
                    "Existing value: $existingValue, new value: $newValue. " +
                    "You may need to add a semantic boundary."
        )
    }

    /**
     * Throws [UnsupportedOperationException].  Should not be called.
     */
    // noinspection DeprecatedCallableAddReplaceWith
    // TODO(KT-32770): Re-deprecate this
    // @Deprecated(
    //     message = "You cannot retrieve a semantics property directly - " +
    //             "use one of the SemanticsConfiguration.getOr* methods instead",
    //     level = DeprecationLevel.ERROR
    // )
    // TODO(KT-6519): Remove this getter entirely
    final override fun getValue(thisRef: SemanticsPropertyReceiver, property: KProperty<*>): T {
        throw UnsupportedOperationException(
            "You cannot retrieve a semantics property directly - " +
                    "use one of the SemanticsConfiguration.getOr* methods instead"
        )
    }

    final override fun setValue(
        thisRef: SemanticsPropertyReceiver,
        property: KProperty<*>,
        value: T
    ) {
        thisRef[this] = value
    }

    override fun toString(): String {
        return "SemanticsPropertyKey: $name"
    }
}

/**
 * Data class for standard accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should return
 * a boolean result indicating whether the action is successfully handled. For example, a scroll
 * forward action should return false if the widget is not enabled or has reached the end of the
 * list.
 */
data class AccessibilityAction<T : Function<Boolean>>(val label: CharSequence?, val action: T) {
    // TODO(b/145951226): Workaround for a bytecode issue, remove this
    override fun hashCode(): Int {
        var result = label?.hashCode() ?: 0
        // (action as Any) is the workaround
        result = 31 * result + (action as Any).hashCode()
        return result
    }
}

/**
 * Data class for custom accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should have no
 * arguments and return a boolean result indicating whether the action is successfully handled.
 */
data class CustomAccessibilityAction(val label: CharSequence, val action: () -> Boolean) {
    // TODO(b/145951226): Workaround for a bytecode issue, remove this
    override fun hashCode(): Int {
        var result = label.hashCode()
        // (action as Any) is the workaround
        result = 31 * result + (action as Any).hashCode()
        return result
    }
}

data class AccessibilityRangeInfo(
    val current: Float,
    val range: ClosedFloatingPointRange<Float>
)

interface SemanticsPropertyReceiver {
    operator fun <T> set(key: SemanticsPropertyKey<T>, value: T)
}

/**
 * Developer-set content description of the semantics node. If this is not set, accessibility
 * services will present the text of this node as content part.
 *
 * @see SemanticsProperties.AccessibilityLabel
 */
var SemanticsPropertyReceiver.accessibilityLabel by SemanticsProperties.AccessibilityLabel

/**
 * Developer-set state description of the semantics node. For example: on/off. If this not
 * set, accessibility services will derive the state from other semantics properties, like
 * [AccessibilityRangeInfo], but it is not guaranteed and the format will be decided by
 * accessibility services.
 *
 * @see SemanticsProperties.AccessibilityValue
 */
var SemanticsPropertyReceiver.accessibilityValue by SemanticsProperties.AccessibilityValue

/**
 * The node is a range with current value.
 *
 * @see SemanticsProperties.AccessibilityRangeInfo
 */
var SemanticsPropertyReceiver.accessibilityValueRange by SemanticsProperties.AccessibilityRangeInfo

/**
 * Whether this semantics node is enabled.
 *
 * @see SemanticsProperties.Enabled
 */
var SemanticsPropertyReceiver.enabled by SemanticsProperties.Enabled

/**
 * Whether this semantics node is hidden.
 *
 * @See SemanticsProperties.Hidden
 */
var SemanticsPropertyReceiver.hidden by SemanticsProperties.Hidden

/**
 * Whether this semantics node represents a Popup. Not to be confused with if this node is
 * _part of_ a Popup.
 *
 * @See SemanticsProperties.IsPopup
 */
var SemanticsPropertyReceiver.popup by SemanticsProperties.IsPopup

// TODO(b/138172781): Move to FoundationSemanticsProperties.kt
/**
 * Test tag attached to this semantics node.
 *
 * @see SemanticsPropertyReceiver.testTag
 */
var SemanticsPropertyReceiver.testTag by SemanticsProperties.TestTag

// var SemanticsPropertyReceiver.textDirection by SemanticsProperties.TextDirection

/**
 * Custom actions which are defined by app developers.
 *
 * @see SemanticsPropertyReceiver.customActions
 */
var SemanticsPropertyReceiver.customActions by SemanticsActions.CustomActions

/**
 * Action to be performed when the node is clicked.
 *
 * @see SemanticsActions.OnClick
 */
var SemanticsPropertyReceiver.onClick by SemanticsActions.OnClick

/**
 * Action to scroll to a specified position.
 *
 * @see SemanticsActions.ScrollTo
 */
var SemanticsPropertyReceiver.ScrollTo by SemanticsActions.ScrollTo

/**
 * Action to scroll the content forward.
 *
 * @see SemanticsActions.ScrollForward
 */
@Deprecated("Use scroll up/down/left/right instead")
@Suppress("DEPRECATION")
var SemanticsPropertyReceiver.scrollForward by SemanticsActions.ScrollForward

/**
 * Action to scroll the content backward.
 *
 * @see SemanticsActions.ScrollBackward
 */
@Deprecated("Use scroll up/down/left/right instead")
@Suppress("DEPRECATION")
var SemanticsPropertyReceiver.scrollBackward by SemanticsActions.ScrollBackward

/**
 * Action to set slider progress.
 *
 * @see SemanticsActions.SetProgress
 */
var SemanticsPropertyReceiver.setProgress by SemanticsActions.SetProgress

/**
 * This function adds the [SemanticsActions.OnClick] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnClick] is called.
 */
fun SemanticsPropertyReceiver.onClick(label: String? = null, action: () -> Boolean) {
    this[SemanticsActions.OnClick] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.ScrollTo] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.ScrollTo] is called.
 */
fun SemanticsPropertyReceiver.ScrollTo(
    label: String? = null,
    action: (x: Float, y: Float) -> Boolean
) {
    this[SemanticsActions.ScrollTo] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.ScrollForward] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.ScrollForward] is called.
 */
// TODO(b/157692376): remove scroll forward/backward api together with slider scroll action.
@Deprecated("Use scroll up/down/left/right instead")
fun SemanticsPropertyReceiver.scrollForward(label: String? = null, action: () -> Boolean) {
    @Suppress("DEPRECATION")
    this[SemanticsActions.ScrollForward] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.ScrollBackward] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.ScrollBackward] is called.
 */
// TODO(b/157692376): remove scroll forward/backward api together with slider scroll action.
@Deprecated("Use scroll up/down/left/right instead")
fun SemanticsPropertyReceiver.scrollBackward(label: String? = null, action: () -> Boolean) {
    @Suppress("DEPRECATION")
    this[SemanticsActions.ScrollBackward] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.SetProgress] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetProgress] is called.
 */
fun SemanticsPropertyReceiver.setProgress(
    label: String? = null,
    action: (progress: Float) -> Boolean
) {
    this[SemanticsActions.SetProgress] = AccessibilityAction(label, action)
}

// TODO(b/138173613): Use this for merging labels
/*

    /**
    * U+202A LEFT-TO-RIGHT EMBEDDING
    *
    * Treat the following text as embedded left-to-right.
    *
    * Use [PDF] to end the embedding.
    */
    private const val LRE = "\u202A"

    /**
     * U+202B RIGHT-TO-LEFT EMBEDDING
     *
     * Treat the following text as embedded right-to-left.
     *
     * Use [PDF] to end the embedding.
     */
    private const val RLE = "\u202B"

    /**
     * U+202C POP DIRECTIONAL FORMATTING
     *
     * End the scope of the last [LRE], [RLE], [RLO], or [LRO].
     */
    private const val PDF = "\u202C"

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
            TextDirection.Rtl -> "${RLE}$nestedLabel${PDF}"
            TextDirection.Ltr -> "${LRE}$nestedLabel${PDF}"
        }
    }
    if (thisString.isNullOrEmpty())
        return nestedLabel
    return "$thisString\n$nestedLabel"
}
*/
