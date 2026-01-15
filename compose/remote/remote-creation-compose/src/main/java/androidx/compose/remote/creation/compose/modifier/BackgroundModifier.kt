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
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.painter.painterRemoteColor
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.SolidBackgroundModifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class BackgroundModifier(val color: RemoteColor) : RemoteModifier.Element {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return SolidBackgroundModifier(color.red.id, color.green.id, color.blue.id, color.alpha.id)
    }
}

public fun RemoteModifier.background(color: Color): RemoteModifier =
    this.then(BackgroundModifier(color.rc))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun RemoteModifier.background(color: RemoteColor): RemoteModifier =
    this.drawWithContent {
        with(painterRemoteColor(color)) { drawScope.onDraw() }
        drawContent()
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun RemoteModifier.background(brush: RemoteBrush): RemoteModifier =
    this.drawWithContent {
        drawScope.drawRect(paint = RemotePaint().apply { remoteBrush = brush })
        drawContent()
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
public fun RemoteModifier.background(remotePainter: RemotePainter): RemoteModifier =
    this.drawWithContent {
        with(remotePainter) { drawScope.onDraw() }
        drawContent()
    }
