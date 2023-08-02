/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core;

import androidx.annotation.RestrictTo;

/**
 * Interface for injecting a {@link ImageProxy}-based post-processing effect into CameraX.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ImageEffect extends CameraEffect {

    /**
     * Bitmask option to indicate that CameraX applies this effect to {@link ImageCapture}.
     */
    int IMAGE_CAPTURE = 1 << 2;

    // TODO(b/229629890): create the public interface for post-processing images.
}
