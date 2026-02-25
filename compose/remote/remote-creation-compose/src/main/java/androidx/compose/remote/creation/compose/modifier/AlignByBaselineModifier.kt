/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext.FIRST_BASELINE
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.modifiers.AlignByModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier

/**
 * A [RemoteModifier.Element] that aligns a layout by its baseline.
 *
 * This is the remote version of the `Modifier.alignByBaseline()` modifier.
 *
 * This modifier can be used to align a layout with other layouts in the same parent by their
 * baselines.
 */
internal class AlignByBaselineModifier() : RemoteModifier.Element {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return AlignByModifier(FIRST_BASELINE)
    }
}

/**
 * Aligns this layout with sibling layouts in a `Row` by their shared baseline.
 *
 * This modifier is used to align layouts within a `Row` based on their first baseline. It is
 * particularly useful for aligning text-based elements to ensure their baselines match up, creating
 * a visually consistent layout.
 *
 * This is the remote equivalent of the `Modifier.alignByBaseline()` modifier in Jetpack Compose.
 *
 * Example usage within a remote `Row`:
 * ```
 * RemoteRow(
 *     modifier = RemoteModifier.fillMaxWidth()
 * ) {
 *     RemoteText(
 *         text = "First",
 *         modifier = RemoteModifier.alignByBaseline()
 *     )
 *     RemoteText(
 *         text = "Second",
 *         modifier = RemoteModifier.alignByBaseline()
 *     )
 * }
 * ```
 */
public fun RemoteModifier.alignByBaseline(): RemoteModifier = then(AlignByBaselineModifier())
