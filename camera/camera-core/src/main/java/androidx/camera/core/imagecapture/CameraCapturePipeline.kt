/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.imagecapture

import androidx.annotation.RestrictTo
import com.google.common.util.concurrent.ListenableFuture

/**
 * Interface to provide capture pipeline functionalities based on camera details.
 *
 * The camera framework specific implementation layer decides exactly what functionalities to
 * provide. For example, 3A precapture may be triggered as part of pre-capture tasks if a flash mode
 * is being used.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraCapturePipeline {
    /** Invokes pre-capture tasks based on camera details. */
    public fun invokePreCapture(): ListenableFuture<Void?>

    /** Invokes post-capture tasks based on camera details. */
    public fun invokePostCapture(): ListenableFuture<Void?>
}
