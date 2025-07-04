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

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.CoreMainPanelEntity
import androidx.xr.compose.subspace.layout.OpaqueEntity

/**
 * A CompositionLocal that holds the current [OpaqueEntity] acting as the parent for any containing
 * composed UI.
 */
@PublishedApi
internal val LocalOpaqueEntity: ProvidableCompositionLocal<OpaqueEntity?> = compositionLocalOf {
    null
}

/**
 * A CompositionLocal that holds the current [CoreEntity] acting as the parent for any containing
 * composed UI.
 */
internal val LocalCoreEntity: CompositionLocal<CoreEntity?> =
    compositionLocalWithComputedDefaultOf {
        LocalOpaqueEntity.currentValue as CoreEntity?
    }

internal val LocalCoreMainPanelEntity: CompositionLocal<CoreMainPanelEntity?> =
    compositionLocalWithComputedDefaultOf {
        LocalComposeXrOwners.currentValue?.coreMainPanelEntity
            ?: LocalSession.currentValue?.let { CoreMainPanelEntity(it) }
    }
