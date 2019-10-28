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

package androidx.ui.foundation.semantics

import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.semantics.SemanticsPropertyKey
import androidx.ui.semantics.SemanticsPropertyReceiver

/**
 * Semantics properties that apply to the Compose Foundation UI elements.  Used for making
 * assertions in testing.
 */
object FoundationSemanticsProperties {
    // TODO(ryanmentley): Is this useful?
    val InMutuallyExclusiveGroup = SemanticsPropertyKey<Boolean>("InMutuallyExclusiveGroup")

    val Selected = SemanticsPropertyKey<Boolean>("Selected")

    // TODO(ryanmentley): Can we think of a better name?
    val ToggleableState = SemanticsPropertyKey<ToggleableState>("ToggleableState")
}

// TODO(ryanmentley): should these be public?  is that confusing?

// TODO(ryanmentley): This one is kind of weird...it's sort of nonsense if this one is ever false
var SemanticsPropertyReceiver.inMutuallyExclusiveGroup
        by FoundationSemanticsProperties.InMutuallyExclusiveGroup

var SemanticsPropertyReceiver.selected by FoundationSemanticsProperties.Selected

var SemanticsPropertyReceiver.toggleableState
        by FoundationSemanticsProperties.ToggleableState
