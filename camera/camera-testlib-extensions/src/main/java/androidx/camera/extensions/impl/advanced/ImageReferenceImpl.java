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

package androidx.camera.extensions.impl.advanced;

import android.annotation.SuppressLint;
import android.media.Image;

/**
 * A Image reference container that enables the Image sharing between Camera2/CameraX and OEM
 * using reference counting. The wrapped Image will be closed once the reference count
 * reaches 0.
 *
 * <p>Implemented by Camera2/CameraX.
 */
@SuppressLint("UnknownNullness")
public interface ImageReferenceImpl {

    /**
     * Increment the reference count. Returns true if the value was incremented.
     * (returns false if the reference count has already reached zero.)
     */
    boolean increment();

    /**
     * Decrement the reference count. Image will be closed if reference count reaches 0.
     * Returns true if the value was decremented (returns false if the reference count has
     * already reached zero)
     */
    boolean decrement();

    /**
     * Return the Android image. This object MUST not be closed directly.
     * Returns null when the reference count is zero.
     */
    Image get();
}
