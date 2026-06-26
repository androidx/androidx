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

package androidx.compose.remote.player.compose.embedded

import android.graphics.drawable.Drawable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Pluggable image loader so the embedded player doesn't depend on any specific image-loading
 * library.
 */
@Stable
public fun interface RcImageLoader {
    /** A reactive holder for the [Drawable] of [bitmapId]; `null` until/unless one is available. */
    public fun loadImage(bitmapId: Int): State<Drawable?>
}

/** The active [RcImageLoader] ProvidableCompositionLocal. */
public val LocalRcImageLoader: ProvidableCompositionLocal<RcImageLoader> =
    staticCompositionLocalOf {
        error("No RcImageLoader provided")
    }
