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

package androidx.compose.remote.integration.view.demos.dsl

import android.graphics.Color
import androidx.compose.remote.creation.dsl.*
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun SmallAnimatedPreview() {
    RemoteDocumentPreview(RemoteDocument(smallAnimated()))
}

@Suppress("RestrictedApiAndroidX")
fun smallAnimated(): ByteArray {
    return createRawRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val sec = continuousSeconds()
        val text = sec.format(2, 2, 0)

        applyPaint {
            setTextSize(120f)
            setColor(Color.WHITE)
        }
        drawTextAnchored(text, 0f, 0f, -1f, 1f, 0)
    }
}
