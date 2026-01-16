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

package androidx.compose.remote.creation.compose.v2

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteImageV2(
    bitmap: ImageBitmap,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: RemoteFloat = RemoteFloat(1f),
) {
    RemoteComposeNode(
        factory = { RemoteImageNodeV2() },
        update = {
            set(modifier) { nodeModifier -> this.modifier = nodeModifier }
            set(bitmap) { this.image = it }
            set(contentScale) { this.contentScale = it }
            set(alpha) { this.alpha = it }
        },
    )
}

@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteImageV2(
    remoteBitmap: RemoteBitmap,
    modifier: RemoteModifier = RemoteModifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: RemoteFloat = RemoteFloat(1f),
) {
    RemoteComposeNode(
        factory = { RemoteImageNodeV2() },
        update = {
            set(modifier) { nodeModifier -> this.modifier = nodeModifier }
            set(remoteBitmap) { this.remoteBitmap = it }
            set(contentScale) { this.contentScale = it }
            set(alpha) { this.alpha = it }
        },
    )
}
