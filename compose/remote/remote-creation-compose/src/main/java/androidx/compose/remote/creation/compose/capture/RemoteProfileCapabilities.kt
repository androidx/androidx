/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("PrimitiveInCollection") // Profile.supportedOperations

package androidx.compose.remote.creation.compose.capture

import androidx.compose.remote.core.Operations
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.onTouchCancel
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.modifier.onTouchUp
import androidx.compose.runtime.Composable

/**
 * Whether the current creation [androidx.compose.remote.creation.profile.Profile] supports themed
 * (Android system) colors created with
 * [androidx.compose.remote.creation.compose.state.createThemedRemoteColor].
 *
 * Some profiles, such as `androidx.glance.wear.GlanceWearProfiles.wearWidgets`, do not include the
 * color theme operation. Design systems and components can check this before emitting themed colors
 * and fall back to named or literal colors when it is `false`.
 */
@Composable
@RemoteComposable
public fun currentProfileSupportsThemedColors(): Boolean =
    currentProfileSupports(Operations.COLOR_THEME)

/**
 * Whether the current creation [androidx.compose.remote.creation.profile.Profile] supports the
 * touch down, up and cancel modifiers ([onTouchDown], [onTouchUp], [onTouchCancel]), for example to
 * drive a press animation.
 *
 * Some profiles, such as `androidx.glance.wear.GlanceWearProfiles.wearWidgets`, do not include
 * those touch operations.
 */
@Composable
@RemoteComposable
public fun currentProfileSupportsTouchModifiers(): Boolean =
    currentProfileSupports(
        Operations.MODIFIER_TOUCH_DOWN,
        Operations.MODIFIER_TOUCH_UP,
        Operations.MODIFIER_TOUCH_CANCEL,
    )

/**
 * Whether the current creation [androidx.compose.remote.creation.profile.Profile] supports clipping
 * to an arbitrary path, for example with
 * [androidx.compose.remote.creation.compose.layout.RemoteDrawScope.clipPath].
 *
 * Without it, clipping is limited to the rectangle and rounded rectangle shapes supported by the
 * [clip] modifier, so components can check this before clipping drawn content, such as a background
 * image, to a custom shape.
 */
@Composable
@RemoteComposable
public fun currentProfileSupportsPathClip(): Boolean = currentProfileSupports(Operations.CLIP_PATH)

/**
 * Whether the current creation [androidx.compose.remote.creation.profile.Profile] supports all of
 * the given [operations].
 *
 * Prefer specific capability helpers such as [currentProfileSupportsThemedColors] or
 * [currentProfileSupportsTouchModifiers] when available. Operation IDs are not public API so must
 * be known ahead of time. Use with care.
 */
@Composable
@RemoteComposable
public fun currentProfileSupports(vararg operations: Int): Boolean {
    val supportedOperations = LocalRemoteComposeCreationState.current.profile.supportedOperations
    return operations.all { it in supportedOperations }
}
