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

package androidx.xr.compose.platform

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.mutableStateOf

/** Tracks the Elevated Status of SpatialDialog */
internal interface DialogManager {
    /** `true` when a SpatialDialog is triggered, `false` otherwise. */
    public val isSpatialDialogActive: MutableState<Boolean>
}

/** Default implementation of [DialogManager]. */
internal class DefaultDialogManager : DialogManager {
    override val isSpatialDialogActive: MutableState<Boolean> = mutableStateOf(false)
}

internal val LocalDialogManager: ProvidableCompositionLocal<DialogManager> =
    compositionLocalWithComputedDefaultOf {
        LocalComposeXrOwners.currentValue?.dialogManager ?: DefaultDialogManager()
    }
