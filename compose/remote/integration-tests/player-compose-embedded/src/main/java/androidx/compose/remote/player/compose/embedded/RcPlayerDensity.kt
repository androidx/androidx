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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.core.CoreDocument
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Resolves a **raw** (unscaled) RemoteCompose dimension to a Compose [Dp], honoring the document's
 * density behavior (the `Header.DOC_DENSITY_BEHAVIOR` property, surfaced as
 * [CoreDocument.getDensityBehavior]).
 *
 * This is for values the player reads *directly* off the op rather than from a core-resolved field
 * — the `spacedBy` gap of `RowLayout`/`ColumnLayout`/`FlowLayout`, `MarqueeModifierOperation`
 * spacing, and `BorderModifierOperation` width. remote-core scales each of these by the display
 * density only under [CoreDocument.DENSITY_BEHAVIOR_DP] (in a layout/paint-time local that is never
 * stored back); under [CoreDocument.DENSITY_BEHAVIOR_PIXELS] and
 * [CoreDocument.DENSITY_BEHAVIOR_LEGACY] the value is used as raw pixels. Since a Compose [Dp] is
 * re-multiplied by [density], a DP-mode value maps to itself and a pixel-mode value is divided by
 * [density] — reproducing the View player's pixels.
 *
 * Ops that instead read a *core-resolved* field (padding via `getLeft`, `widthIn`/`heightIn` via
 * `getMin`/`getMax`) need no behavior logic: remote-core's `updateVariables` already folded the
 * density into that field, so the reader simply converts pixels→dp by dividing by [density].
 */
internal fun rawDimensionDp(value: Float, behavior: Int, density: Float): Dp =
    if (behavior == CoreDocument.DENSITY_BEHAVIOR_DP) value.dp else (value / density).dp
