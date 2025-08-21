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

import androidx.compose.foundation.layout.padding
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class PaddingModifier(
    val left: RemoteFloat,
    val top: RemoteFloat,
    val right: RemoteFloat,
    val bottom: RemoteFloat,
) : RemoteLayoutModifier {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.PaddingModifier(
            left.internalAsFloat(),
            top.internalAsFloat(),
            right.internalAsFloat(),
            bottom.internalAsFloat(),
        )
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        // TODO how to get the value of a RemoteFloat in preview mode
        // TODO LTR
        return with(LocalDensity.current) {
            padding(
                left.toFloat().toDp(),
                top.toFloat().toDp(),
                right.toFloat().toDp(),
                bottom.toFloat().toDp(),
            )
        }
    }
}

fun RemoteModifier.padding(
    left: RemoteFloat,
    top: RemoteFloat,
    right: RemoteFloat,
    bottom: RemoteFloat,
): RemoteModifier = then(PaddingModifier(left, top, right, bottom))

fun RemoteModifier.padding(all: RemoteFloat): RemoteModifier =
    this.then(PaddingModifier(all, all, all, all))

@Composable
fun RemoteModifier.padding(value: Dp): RemoteModifier {
    val dimension = (LocalDensity.current.density * value.value)
    return padding(RemoteFloat(dimension))
}

@Composable
fun RemoteModifier.padding(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp,
): RemoteModifier {
    return with(LocalDensity.current) {
        padding(
            RemoteFloat(start.toPx()),
            RemoteFloat(top.toPx()),
            RemoteFloat(end.toPx()),
            RemoteFloat(bottom.toPx()),
        )
    }
}
