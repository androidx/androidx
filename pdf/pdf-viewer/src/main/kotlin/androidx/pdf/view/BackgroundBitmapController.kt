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

package androidx.pdf.view

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.annotation.MainThread

/**
 * Releases page bitmaps when the system reports memory pressure while the app is in the background,
 * and fetches them again when [PdfView]'s window becomes visible.
 *
 * Bitmaps are released on [onTrimMemory] at [ComponentCallbacks2.TRIM_MEMORY_BACKGROUND] or above.
 * These levels are only sent once the app's process has no visible UI and is in the background LRU
 * list, so releasing immediately never blanks pages the user can see.
 * [ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN] alone does not release bitmaps, as it is sent every
 * time the UI is hidden regardless of memory pressure. Other page data, e.g. text and links, is
 * never released by this class.
 *
 * This class is not thread-safe and must only be used from the main thread. It must be registered
 * via [android.content.Context.registerComponentCallbacks] on the application context to receive
 * [onTrimMemory].
 */
@MainThread
internal class BackgroundBitmapController(
    // Releases all bitmaps.
    private val releaseBitmaps: () -> Unit,
    // Fetches bitmaps again for the visible portion of the document.
    private val restoreBitmaps: () -> Unit,
) : ComponentCallbacks2 {

    // True if bitmaps were released and should be restored once the window is visible again.
    internal var areBitmapsReleased = false
        private set

    override fun onTrimMemory(level: Int) {
        if (level < ComponentCallbacks2.TRIM_MEMORY_BACKGROUND || areBitmapsReleased) return
        releaseBitmaps()
        areBitmapsReleased = true
    }

    fun onWindowVisibilityChanged(isVisible: Boolean) {
        if (isVisible && areBitmapsReleased) {
            areBitmapsReleased = false
            restoreBitmaps()
        }
    }

    fun reset() {
        areBitmapsReleased = false
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // No-op
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onLowMemory() {
        // No-op; superseded by onTrimMemory
    }
}
