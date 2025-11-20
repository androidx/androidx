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

package androidx.privacysandbox.ui.provider

import android.view.SurfaceControlViewHost
import androidx.privacysandbox.ui.core.IRemoteSessionController

/**
 * Wrapper interface to perform check on client version before delegating call to
 * [androidx.privacysandbox.ui.core.IRemoteSessionClient]
 */

// TODO(b/414583457): Ensure any api change in
// [androidx.privacysandbox.ui.core.IRemoteSessionClient] is updated in wrapper interface as well
internal interface IRemoteSessionClient {
    fun onRemoteSessionOpened(
        surfacePackage: SurfaceControlViewHost.SurfacePackage,
        remoteSessionController: IRemoteSessionController,
        isZOrderOnTop: Boolean,
        signalOptions: List<String>,
    )

    fun onRemoteSessionError(exception: String?)

    fun onResizeRequested(width: Int, height: Int)

    fun onSessionUiFetched(surfacePackage: SurfaceControlViewHost.SurfacePackage)
}
