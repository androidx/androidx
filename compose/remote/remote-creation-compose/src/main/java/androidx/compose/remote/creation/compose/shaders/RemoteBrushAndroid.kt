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

package androidx.compose.remote.creation.compose.shaders

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteBrush.Companion.fromComposeUi(brush: Brush): RemoteBrush {
    return when (brush) {
        is SolidColor -> RemoteBrush.solidColor(brush.value.rc)
        else -> {
            println("RemoteBrush.fromComposeUi not implemented for $brush")
            RemoteBrush.solidColor(Color.Transparent.rc)
        }
    }
}
