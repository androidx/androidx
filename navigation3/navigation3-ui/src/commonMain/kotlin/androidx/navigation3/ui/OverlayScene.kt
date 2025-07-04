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

package androidx.navigation3.ui

import androidx.navigation3.runtime.NavEntry

/**
 * A specific scene to render 1 or more [NavEntry] instances as an overlay.
 *
 * It is expected that the [content] is rendered in one or more separate windows (e.g., a dialog,
 * popup window, etc.) that are visible above any additional [Scene] instances calculated from the
 * [overlaidEntries].
 *
 * When processing [overlaidEntries], expect processing of each [SceneStrategy] to restart from the
 * first strategy. This may result in multiple instances of the same [OverlayScene] to be shown
 * simultaneously, making a unique [key] even more important.
 */
public interface OverlayScene<T : Any> : Scene<T> {

    /**
     * The [NavEntry]s that should be handled by another [Scene] that sits below this Scene.
     *
     * This *must* always be a non-empty list to correctly display entries below the overlay.
     */
    public val overlaidEntries: List<NavEntry<T>>
}
