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

package androidx.xr.scenecore.spatial.core

import android.view.SurfaceControlViewHost
import android.view.View
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(SurfaceControlViewHost::class)
class ShadowSurfaceControlViewHost {
    private var _view: View? = null

    @Implementation
    fun setView(view: View, width: Int, height: Int) {
        _view = view
    }

    @Implementation fun getView(): View? = _view

    @Implementation fun relayout(width: Int, height: Int) {}

    @Implementation
    fun release() {
        _view = null
    }
}
