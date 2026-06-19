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

package androidx.compose.remote.player.compose.custom

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CustomContext
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.ui.graphics.Canvas

/**
 * CustomContext extension for Jetpack Compose environments, allowing injection of the active
 * Android Context, Compose Canvas, and RemoteContext for native View hosting.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalRemotePlayerApi::class)
public interface ComposeCustomContext : CustomContext {
    /** Sets the active Android Context for view instantiation. */
    public fun setContext(context: Context?)

    /** Sets the active Compose Canvas for custom drawing. */
    public fun setCanvas(canvas: Canvas?)

    /** Sets the active RemoteContext. */
    public fun setRemoteContext(remoteContext: RemoteContext?)
}
