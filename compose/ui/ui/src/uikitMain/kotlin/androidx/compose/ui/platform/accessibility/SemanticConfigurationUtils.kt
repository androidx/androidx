/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.platform.accessibility

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import kotlin.math.roundToInt
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.UIKit.UIAccessibilityCustomAction
import platform.UIKit.UIAccessibilityTraitAdjustable
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIAccessibilityTraitHeader
import platform.UIKit.UIAccessibilityTraitImage
import platform.UIKit.UIAccessibilityTraitNone
import platform.UIKit.UIAccessibilityTraitNotEnabled
import platform.UIKit.UIAccessibilityTraitSelected
import platform.UIKit.UIAccessibilityTraitStaticText
import platform.UIKit.UIAccessibilityTraitToggleButton
import platform.UIKit.UIAccessibilityTraitUpdatesFrequently
import platform.UIKit.UIAccessibilityTraits

// Private accessibility trait for text fields
internal val CMPAccessibilityTraitTextView: UIAccessibilityTraits = (1UL shl 18) or (1UL shl 47)
internal val CMPAccessibilityTraitIsEditing: UIAccessibilityTraits = 1UL shl 21

internal fun SemanticsConfiguration.accessibilityTraits(): UIAccessibilityTraits {
    var result = UIAccessibilityTraitNone

    if (contains(SemanticsProperties.LiveRegion)) {
        // TODO: LiveRegionMode in the config is currently ignored.
        //  the default behavior due this flag set will actually do `Polite` announcements
        //  to do `Assertive` announcements, we need to post a notification explicitly on each change
        //  which we need to track manually
        result = result or UIAccessibilityTraitUpdatesFrequently
    }

    if (contains(SemanticsProperties.Disabled)) {
        result = result or UIAccessibilityTraitNotEnabled
    }

    getOrNull(SemanticsProperties.Selected)?.let { selected ->
        if (selected) {
            result = result or UIAccessibilityTraitSelected
        }
    }

    if (contains(SemanticsProperties.Heading)) {
        result = result or UIAccessibilityTraitHeader
    }

    getOrNull(SemanticsProperties.ToggleableState)?.let { state ->
        when (state) {
            ToggleableState.On -> {
                result = result or UIAccessibilityTraitSelected
            }

            ToggleableState.Off, ToggleableState.Indeterminate -> {
                // Do nothing
            }
        }
    }

    if (contains(SemanticsProperties.ProgressBarRangeInfo)) {
        if (contains(SemanticsActions.SetProgress)) {
            result = result or UIAccessibilityTraitAdjustable
        }
    }

    if (contains(SemanticsProperties.EditableText)) {
        result = result or CMPAccessibilityTraitTextView
        if (getOrNull(SemanticsProperties.Focused) == true) {
            result = result or CMPAccessibilityTraitIsEditing
        }
    } else if (contains(SemanticsActions.OnClick)) {
        result = result or UIAccessibilityTraitButton
    }

    getOrNull(SemanticsProperties.Role)?.let { role ->
        when (role) {
            Role.Button -> {
                result = result or UIAccessibilityTraitButton
            }

            Role.DropdownList -> {
                result = result or UIAccessibilityTraitAdjustable
            }

            Role.Image -> {
                result = result or UIAccessibilityTraitImage
            }

            Role.Switch -> {
                if (available(OS.Ios to OSVersion(major = 17))) {
                    result = result or UIAccessibilityTraitToggleButton
                }
            }
        }
    }

    if (result == UIAccessibilityTraitNone &&
        contains(SemanticsProperties.Text) &&
        contains(SemanticsActions.GetTextLayoutResult) &&
        contains(SemanticsActions.ShowTextSubstitution)
    ) {
        result = result or UIAccessibilityTraitStaticText
    }

    return result
}

internal fun SemanticsConfiguration.accessibilityValue(): String? {
    getOrNull(SemanticsProperties.StateDescription)?.takeIf { it.isNotBlank() }?.let {
        return it
    }

    if (contains(SemanticsProperties.EditableText)) {
        getOrNull(SemanticsProperties.EditableText)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it.text }

        getOrNull(SemanticsProperties.Text)
            ?.joinToString("\n")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
    }

    return getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.let {
        return if (it.range.endInclusive > it.range.start) {
            val fraction = (it.current - it.range.start) /
                (it.range.endInclusive - it.range.start)
            "${(fraction * 100f).roundToInt()}%"
        } else {
            null
        }
    }
}

internal fun SemanticsConfiguration.accessibilityCustomActions():
    List<UIAccessibilityCustomAction> {
    return getOrNull(SemanticsActions.CustomActions)?.let { actions ->
        actions.map {
            UIAccessibilityCustomAction(
                name = it.label,
                actionHandler = { _ ->
                    if (contains(SemanticsProperties.Disabled)) {
                        false
                    } else {
                        it.action.invoke()
                    }
                }
            )
        }
    } ?: emptyList()
}