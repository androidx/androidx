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

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalContext
import androidx.xr.compose.subspace.layout.CoreMainPanelEntity
import androidx.xr.scenecore.Session

/**
 * A composition local that provides the current Jetpack XR [Session].
 *
 * In non-XR environments, this composition local will return `null`.
 */
public val LocalSession: ProvidableCompositionLocal<Session?> =
    compositionLocalWithComputedDefaultOf {
        if (SpatialConfiguration.hasXrSpatialFeature(LocalContext.currentValue)) {
            Session.create(LocalContext.currentValue.getActivity())
        } else {
            null
        }
    }

private val mainPanelEntityMap: MutableMap<Session, CoreMainPanelEntity> = mutableMapOf()

/**
 * The [CoreMainPanelEntity] compose wrapper that represents the main panel for this [Session].
 *
 * In order to react to the main panel's size changes we need to add a listener to the main view.
 * Tracking the instance of CoreMainPanelEntity allows us to limit the number of listeners added and
 * makes it so we don't have to worry about disposing the instance every time it is used.
 */
internal val Session.coreMainPanelEntity: CoreMainPanelEntity
    get() = mainPanelEntityMap.getOrPut(this) { CoreMainPanelEntity(this) }
