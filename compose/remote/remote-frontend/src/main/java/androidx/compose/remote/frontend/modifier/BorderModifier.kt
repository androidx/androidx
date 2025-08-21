/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.frontend.modifier

import androidx.compose.foundation.border
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class BorderModifier(val width: RemoteFloat, val color: Color) : RemoteModifier.Element {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.BorderModifier(
            width.internalAsFloat(),
            0f,
            color.toArgb(),
            0,
        )
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        // TODO how to get the value of a RemoteFloat in preview mode
        return border(width = width.toFloat().dp, color = color)
    }
}

fun RemoteModifier.border(width: RemoteFloat, color: Color): RemoteModifier =
    then(BorderModifier(width, color))

@Composable
fun RemoteModifier.border(value: Dp, color: Color): RemoteModifier {
    return border(RemoteFloat(value.value), color)
}
