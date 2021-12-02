/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.RequestProcessor

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
interface GraphListener {
    /**
     * Used to indicate that the graph has been initialized and is ready to actively process
     * requests using the provided [RequestProcessor] interface.
     */
    fun onGraphStarted(requestProcessor: RequestProcessor)

    /**
     * Used to indicate that a previously initialized [RequestProcessor] is no longer available.
     */
    fun onGraphStopped(requestProcessor: RequestProcessor)

    /**
     * Used to indicate that the internal state of the [RequestProcessor] has changed. This is
     * a signal that previously queued requests may now succeed if they previously failed.
     */
    fun onGraphModified(requestProcessor: RequestProcessor)
}