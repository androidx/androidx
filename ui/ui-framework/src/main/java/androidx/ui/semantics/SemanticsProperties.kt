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

import androidx.ui.text.style.TextDirection
import androidx.ui.unit.Px

object SemanticsProperties {
    val AccessibilityLabel = object : SemanticsPropertyKey<String>("AccessibilityLabel") {
        override fun merge(existingValue: String, newValue: String): String {
            // TODO(b/138173613): Needs TextDirection, probably needs to pass both nodes
            //  to retrieve it
            return existingValue + "\n" + newValue
        }
    }

    val AccessibilityValue = SemanticsPropertyKey<String>("AccessibilityValue")

    val AccessibilityRangeInfo =
        SemanticsPropertyKey<AccessibilityRangeInfo>("AccessibilityRangeInfo")

    val Enabled = SemanticsPropertyKey<Boolean>("Enabled")

    val Hidden = SemanticsPropertyKey<Boolean>("Hidden")

    val TextDirection = SemanticsPropertyKey<TextDirection>("TextDirection")

    // TODO(b/138172781): Move to FoundationSemanticsProperties
    val TestTag = SemanticsPropertyKey<String>("TestTag")
}

class SemanticsActions {
    companion object {
        val OnClick = SemanticsPropertyKey<AccessibilityAction<() -> Unit>>("OnClick")

        val ScrollTo =
            SemanticsPropertyKey<AccessibilityAction<(x: Px, y: Px) -> Unit>>("ScrollTo")

        val CustomActions =
            SemanticsPropertyKey<List<AccessibilityAction<() -> Unit>>>("CustomActions")
    }
}

var SemanticsPropertyReceiver.accessibilityLabel by SemanticsProperties.AccessibilityLabel

var SemanticsPropertyReceiver.accessibilityValue by SemanticsProperties.AccessibilityValue

var SemanticsPropertyReceiver.accessibilityValueRange
        by SemanticsProperties.AccessibilityRangeInfo

var SemanticsPropertyReceiver.enabled by SemanticsProperties.Enabled

var SemanticsPropertyReceiver.hidden by SemanticsProperties.Hidden

var SemanticsPropertyReceiver.textDirection by SemanticsProperties.TextDirection

var SemanticsPropertyReceiver.onClick by SemanticsActions.OnClick

var SemanticsPropertyReceiver.ScrollTo by SemanticsActions.ScrollTo

fun SemanticsPropertyReceiver.onClick(label: String? = null, action: () -> Unit) {
    this[SemanticsActions.OnClick] = AccessibilityAction(label, action)
}

fun SemanticsPropertyReceiver.ScrollTo(label: String? = null, action: (x: Px, y: Px) -> Unit) {
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
