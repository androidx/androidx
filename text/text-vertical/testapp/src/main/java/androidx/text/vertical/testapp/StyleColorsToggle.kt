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

package androidx.text.vertical.testapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/** The test tag of the floating button that [StyleColorsToggle] shows. */
internal const val STYLE_COLORS_TOGGLE_TAG = "style_colors_toggle"

/**
 * Shows [content] with a floating button that turns the style colors on and off.
 *
 * The toggle state lives in the demo and survives rotation through [rememberSaveable].
 *
 * @param modifier the [Modifier] applied to the outer [Box]
 * @param initiallyEnabled whether the style colors are on when the demo first shows
 * @param content the demo. It gets whether the style colors are on.
 */
@Composable
fun StyleColorsToggle(
    modifier: Modifier = Modifier,
    initiallyEnabled: Boolean = false,
    content: @Composable (styleColorsEnabled: Boolean) -> Unit,
) {
    var enabled by rememberSaveable { mutableStateOf(initiallyEnabled) }
    Box(modifier.fillMaxSize()) {
        content(enabled)
        ExtendedFloatingActionButton(
            text = { Text("Style Colors") },
            icon = {
                Icon(
                    painter =
                        painterResource(
                            if (enabled) R.drawable.checkbox else R.drawable.checkbox_outline_blank
                        ),
                    contentDescription = null,
                )
            },
            onClick = { enabled = !enabled },
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag(STYLE_COLORS_TOGGLE_TAG)
                    .semantics { stateDescription = if (enabled) "On" else "Off" },
        )
    }
}
