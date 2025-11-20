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

package androidx.camera.core.impl;


import org.jspecify.annotations.NonNull;

/**
 * An exception thrown by an {@link InternalCameraPresenceListener} to indicate that it
 * failed to process an update to the set of available cameras.
 *
 * <p>This signals to the caller that the update transaction should be rolled back.
 */
public final class CameraUpdateException extends Exception {

    public CameraUpdateException(@NonNull String message) {
        super(message);
    }

    public CameraUpdateException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }

    public CameraUpdateException(@NonNull Throwable cause) {
        super(cause);
    }
}
