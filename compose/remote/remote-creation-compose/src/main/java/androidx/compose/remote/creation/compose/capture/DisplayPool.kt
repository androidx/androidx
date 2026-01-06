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

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.SurfaceView
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.CreationDisplayInfo

/** API for managing a pool of [VirtualDisplay] objects. The current implementation does not */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object DisplayPool {
    public fun allocate(
        context: Context,
        creationDisplayInfo: CreationDisplayInfo,
    ): VirtualDisplay {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return displayManager.createVirtualDisplay(
            "Projection",
            creationDisplayInfo.width,
            creationDisplayInfo.height,
            creationDisplayInfo.densityDpi,
            SurfaceView(context).holder.surface,
            0,
        )
    }

    public fun release(virtualDisplay: VirtualDisplay) {
        virtualDisplay.release()
    }
}
