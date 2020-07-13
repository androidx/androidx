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

package androidx.ui.foundation.selection

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.core.composed
import androidx.ui.core.semantics.semantics
import androidx.ui.foundation.Indication
import androidx.ui.foundation.IndicationAmbient
import androidx.ui.foundation.Interaction
import androidx.ui.foundation.InteractionState
import androidx.ui.foundation.Strings
import androidx.ui.foundation.clickable
import androidx.ui.foundation.semantics.inMutuallyExclusiveGroup
import androidx.ui.foundation.semantics.selected
import androidx.ui.semantics.accessibilityValue
import androidx.ui.semantics.onClick

/**
 * Configure component to be selectable, usually as a part of a mutually exclusive group, where
 * only one item can be selected at any point in time. A typical example of mutually exclusive set
 * is [androidx.ui.material.RadioGroup]
 *
 * If you want to make an item support on/off capabilities without being part of a set, consider
 * using [Modifier.toggleable]
 *
 * @sample androidx.ui.foundation.samples.SelectableSample
 *
 * @param selected whether or not this item is selected in a mutually exclusion set
 * @param onClick callback to invoke when this item is clicked
 * @param enabled whether or not this [selectable] will handle input events
 * and appear enabled from a semantics perspective
 * @param inMutuallyExclusiveGroup whether or not this item is a part of mutually exclusive
 * group, meaning that only one of these items can be selected at any point of time
 * @param interactionState [InteractionState] that will be updated when this element is
 * pressed, using [Interaction.Pressed]
 * @param indication indication to be shown when the modified element is pressed. By default,
 * the indication from [IndicationAmbient] will be used. Set to `null` to show no indication
 */
@Composable
fun Modifier.selectable(
    selected: Boolean,
    enabled: Boolean = true,
    inMutuallyExclusiveGroup: Boolean = true,
    interactionState: InteractionState = remember { InteractionState() },
    indication: Indication? = IndicationAmbient.current(),
    onClick: () -> Unit
) = composed {
    Modifier.clickable(
        enabled = enabled,
        interactionState = interactionState,
        indication = indication,
        onClick = onClick
    ).semantics {
        this.inMutuallyExclusiveGroup = inMutuallyExclusiveGroup
        this.selected = selected
        this.accessibilityValue = if (selected) Strings.Selected else Strings.NotSelected
    }
}