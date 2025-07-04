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

package androidx.compose.material3.adaptive.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified

/**
 * The class is used to create a scrim when a levitated pane is shown, to block the user interaction
 * from the underlying layout. See [AdaptStrategy.Levitate] for more detailed info.
 *
 * @sample androidx.compose.material3.adaptive.samples.levitateAdaptStrategySample
 * @param color the color of scrim, by default if [Color.Unspecified] is provided, the pane scaffold
 *   implementation will use a translucent black color.
 * @param onClick the on-click listener of the scrim; usually used to dismiss the levitated pane;
 *   i.e. remove the pane from the top of the destination history.
 */
@Immutable
class Scrim(val color: Color = Color.Unspecified, val onClick: (() -> Unit)? = null) {
    @Composable
    internal fun Content(defaultColor: Color, enabled: Boolean) {
        val color = if (this.color.isSpecified) this.color else defaultColor
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(color)
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                interactionSource = null,
                                indication = null,
                                enabled = enabled,
                                onClick = onClick,
                            )
                        } else {
                            Modifier
                        }
                    )
        )
    }

    override fun toString() = "Scrim[color=$color, onClick=$onClick]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Scrim) return false
        if (color != other.color) return false
        if (onClick !== other.onClick) return false
        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + onClick.hashCode()
        return result
    }
}
