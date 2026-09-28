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

package androidx.compose.remote.integration.view.demos.blog

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.plus
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * A circle whose radius breathes, written in the Remote Compose DSL.
 *
 * The document is authored once and never rewritten. `continuousSeconds()` is a *dynamic
 * reference*, not a value read at creation time, so every arithmetic operation applied to it builds
 * up an expression graph that is shipped inside the document and re-evaluated by the player on each
 * frame. There is no recomposition, no host callback, and no per-frame traffic.
 *
 * Layout values work the same way: `componentWidth()` and `componentHeight()` resolve on the player
 * against the real surface, so the circle is centered and sized correctly at any size without the
 * creator knowing the dimensions.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslAnimatedRadius(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 400),
        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 400),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Animated Radius"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(0xFFFFFFFF.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val centerX = w * 0.5f
            val centerY = h * 0.5f
            val minDimension = min(w, h)

            val radius = minDimension * 0.2f
            // Time in seconds is a dynamic reference, not a snapshot.
            val time = continuousSeconds()
            // Radius cycles between 0.85x and 1.15x about every 2 seconds.
            // Under the hood this creates an expression graph.
            val animatedRadius = radius * (1f + (sin(time * 3f) * 0.15f))

            paint { color(0xFF34A853.toInt()) }
            drawCircle(centerX, centerY, animatedRadius)
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DslAnimatedRadiusPreview() {
    RemoteDocumentPreview(RemoteDocument(dslAnimatedRadius()))
}
