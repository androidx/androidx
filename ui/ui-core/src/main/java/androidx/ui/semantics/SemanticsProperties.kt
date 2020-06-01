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
 * This class defines the APIs for accessibility properties.
 */
object SemanticsProperties {
    // content description of the semantics node
    val AccessibilityLabel = object : SemanticsPropertyKey<String>("AccessibilityLabel") {
        override fun merge(existingValue: String, newValue: String): String {
            // TODO(b/138173613): Needs TextDirection, probably needs to pass both nodes
            //  to retrieve it
            return existingValue + "\n" + newValue
        }
    }

    // state description of the semantics node
    val AccessibilityValue = SemanticsPropertyKey<String>("AccessibilityValue")

    // a data structure which contains the current value and range of values
    val AccessibilityRangeInfo =
        SemanticsPropertyKey<AccessibilityRangeInfo>("AccessibilityRangeInfo")

    // whether this semantics node is enabled
    val Enabled = SemanticsPropertyKey<Boolean>("Enabled")

    // whether this semantics node is hidden
    val Hidden = SemanticsPropertyKey<Boolean>("Hidden")

    /**
     * Whether this semantics node represents a Popup. Not to be confused with if this node is
     * _part of_ a Popup.
     */
    val IsPopup = SemanticsPropertyKey<Boolean>("IsPopup")

    // TODO(b/151228491): TextDirection needs to be in core for platform use
    // val TextDirection = SemanticsPropertyKey<TextDirection>("TextDirection")

    // TODO(b/138172781): Move to FoundationSemanticsProperties
    val TestTag = SemanticsPropertyKey<String>("TestTag")
}

/**
 * Ths class defines keys of the actions which can be set in semantics and performed on the
 * semantics node.
 */
class SemanticsActions {
    companion object {
        // action to be performed when the node is clicked
        val OnClick = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("OnClick")

        // action to scroll to a specified position
        val ScrollTo =
            SemanticsPropertyKey<AccessibilityAction<(x: Float, y: Float) -> Boolean>>("ScrollTo")

        // custom actions which are defined by app developers
        val CustomActions =
            SemanticsPropertyKey<List<CustomAccessibilityAction>>("CustomActions")
    }
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

var SemanticsPropertyReceiver.accessibilityLabel by SemanticsProperties.AccessibilityLabel

var SemanticsPropertyReceiver.accessibilityValue by SemanticsProperties.AccessibilityValue

var SemanticsPropertyReceiver.accessibilityValueRange
        by SemanticsProperties.AccessibilityRangeInfo

var SemanticsPropertyReceiver.enabled by SemanticsProperties.Enabled

var SemanticsPropertyReceiver.hidden by SemanticsProperties.Hidden

/**
 * Whether this semantics node represents a Popup. Not to be confused with if this node is
 * _part of_ a Popup.
 */
var SemanticsPropertyReceiver.popup by SemanticsProperties.IsPopup

// var SemanticsPropertyReceiver.textDirection by SemanticsProperties.TextDirection

var SemanticsPropertyReceiver.onClick by SemanticsActions.OnClick

var SemanticsPropertyReceiver.ScrollTo by SemanticsActions.ScrollTo

fun SemanticsPropertyReceiver.onClick(label: String? = null, action: () -> Boolean) {
    this[SemanticsActions.OnClick] = AccessibilityAction(label, action)
}

fun SemanticsPropertyReceiver.ScrollTo(
    label: String? = null,
    action: (x: Float, y: Float) -> Boolean
) {
    this[SemanticsActions.ScrollTo] = AccessibilityAction(label, action)
}

var SemanticsPropertyReceiver.customActions by SemanticsActions.CustomActions

// TODO(b/138172781): Move to FoundationSemanticsProperties.kt
var SemanticsPropertyReceiver.testTag by SemanticsProperties.TestTag

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
