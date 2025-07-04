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

package androidx.privacysandbox.ui.client

import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import androidx.privacysandbox.ui.core.IMotionEventTransferCallback

/**
 * Wrapper interface to perform check on provider version before delegating call to
 * [androidx.privacysandbox.ui.core.IRemoteSessionController]
 */

// TODO(b/414583457): Ensure any api change in
// [androidx.privacysandbox.ui.core.IRemoteSessionController]
// is updated in this interface as well
internal interface IRemoteSessionController {
    fun close()

    fun notifyConfigurationChanged(configuration: Configuration)

    fun notifyResized(width: Int, height: Int)

    fun notifyZOrderChanged(isZOrderOnTop: Boolean)

    fun notifyFetchUiForSession()

    fun notifyUiChanged(uiContainerInfo: Bundle)

    fun notifySessionRendered(supportedSignalOptions: List<String>)

    fun notifyMotionEvent(
        motionEvent: MotionEvent,
        eventTargetFrameTime: Long,
        eventTransferCallback: IMotionEventTransferCallback?,
    )
}
