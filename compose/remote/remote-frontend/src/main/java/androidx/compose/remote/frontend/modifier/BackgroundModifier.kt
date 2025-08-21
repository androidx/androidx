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

import androidx.compose.foundation.background
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.capture.shaders.RemoteBrush
import androidx.compose.remote.frontend.capture.shaders.RemoteSolidColor
import androidx.compose.remote.frontend.capture.shaders.solidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class BackgroundModifier(val brush: RemoteBrush) : RemoteModifier.Element {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return if (brush is RemoteSolidColor) {
            androidx.compose.remote.creation.modifiers.SolidBackgroundModifier(brush.color.toArgb())
        } else {
            // TODO specify
            androidx.compose.remote.creation.modifiers.BackgroundModifier(
                brush.createShader(Size(100f, 100f)),
                0,
            )
        }
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        return background(brush.toComposeUi())
    }
}

fun RemoteModifier.background(color: Color): RemoteModifier =
    this.then(BackgroundModifier(RemoteBrush.solidColor(color)))

fun RemoteModifier.background(brush: RemoteBrush): RemoteModifier =
    this.then(BackgroundModifier(brush))
