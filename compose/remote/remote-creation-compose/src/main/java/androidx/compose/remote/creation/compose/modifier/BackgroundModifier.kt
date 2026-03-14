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
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.painter.painterRemoteColor
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.SolidBackgroundModifier
import androidx.compose.ui.graphics.Color

internal data class BackgroundModifier(val color: RemoteColor) : RemoteModifier.Element {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return SolidBackgroundModifier(
            color.red.floatId,
            color.green.floatId,
            color.blue.floatId,
            color.alpha.floatId,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.background(color: Color): RemoteModifier =
    this.then(BackgroundModifier(color.rc))

/**
 * Draws a solid [color] background behind the content.
 *
 * @param color The [RemoteColor] to use for the background.
 */
public fun RemoteModifier.background(color: RemoteColor): RemoteModifier =
    if (color.hasConstantValue) {
        this.background(color.constantValue)
    } else {
        this.drawWithContent {
            with(painterRemoteColor(color)) { onDraw() }
            drawContent()
        }
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.background(brush: RemoteBrush): RemoteModifier =
    this.drawWithContent {
        drawRect(paint = RemotePaint { applyRemoteBrush(brush, size) })
        drawContent()
    }

/**
 * Draws a [remotePainter] behind the content.
 *
 * @param remotePainter The [RemotePainter] to use for the background.
 */
public fun RemoteModifier.background(remotePainter: RemotePainter): RemoteModifier =
    this.drawWithContent {
        with(remotePainter) { onDraw() }
        drawContent()
    }
