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

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.foundation.layout.RowScope
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.modifiers.AlignByModifierOperation
import androidx.compose.remote.player.compose.embedded.lineReflection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun Modifier.alignBy(op: AlignByModifierOperation, rowScope: RowScope? = null): Modifier {
    val line = op.lineReflection
    return if (Utils.isVariable(line)) {
        when (Utils.idFromNan(line)) {
            AlignByModifierOperation.ID_FIRST_BASELINE -> with(rowScope!!) { alignByBaseline() }
            AlignByModifierOperation.ID_LAST_BASELINE -> with(rowScope!!) { alignByBaseline() }
            else -> this
        }
    } else {
        // If it's a literal value, we don't have a direct mapping to standard baselines
        // but we could theoretically use a custom HorizontalAlignmentLine if needed.
        this
    }
}
