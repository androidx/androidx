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

package androidx.camera.camera2.pipe.compat

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.RequestProcessor

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal interface CameraController {
    /**
     * Tell the graph to start and initialize a [RequestProcessor] instances.
     */
    fun start()

    /**
     * Tell the [CameraController] to stop initialization and to tear down any existing
     * [RequestProcessor] instance.
     */
    fun stop()

    /**
     * Signals the [CameraController] that a [RequestProcessor] may need to be recreated.
     */
    fun restart()
}
