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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.core.PassThroughLayout
import androidx.ui.core.gesture.TapGestureDetector
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.enabled
import androidx.ui.semantics.onClick

/**
 * Combines [TapGestureDetector] and [Semantics] for the clickable
 * components like Button.
 *
 * @sample androidx.ui.foundation.samples.ClickableSample
 *
 * @param onClick will be called when user clicked on the button
 * @param modifier allows to provide a modifier to be added before the gesture detector, for
 * example Ripple should be added at this point. this will be easier once we migrate this
 * function to a Modifier
 * @param enabled Controls the enabled state. When `false`, this component will not be
 * clickable
 */
@Composable
fun Clickable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.None,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    children: @Composable() () -> Unit
) {
    Semantics(
        container = true,
        properties = {
            this.enabled = enabled
            if (enabled) {
                onClick(action = onClick, label = onClickLabel)
            }
        }
    ) {
        // TODO(b/150706555): This layout is temporary and should be removed once Semantics
        //  is implemented with modifiers.
        val tap = if (enabled) {
            TapGestureDetector(onClick)
        } else {
            Modifier.None
        }
        @Suppress("DEPRECATION")
        PassThroughLayout(modifier + tap, children)
    }
}