/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.FocusMeteringAction.OnAutoFocusListener;

/**
 * An interface for controlling camera's zoom, focus and metering across all use cases.
 *
 * <p>Applications can retrieve the interface via CameraX.getCameraControl.
 */
public interface CameraControl {
    /**
     * Starts a focus and metering action by the {@link FocusMeteringAction}.
     *
     * The {@link FocusMeteringAction} contains the configuration of multiple 3A
     * {@link MeteringPoint}s, auto-cancel duration and{@link OnAutoFocusListener} to receive the
     * auto-focus result. Check {@link FocusMeteringAction} for more details.
     *
     * @param action the {@link FocusMeteringAction} to be executed.
     */
    void startFocusAndMetering(@NonNull FocusMeteringAction action);

    /**
     * Cancels current {@link FocusMeteringAction}.
     *
     * <p>It clears the 3A regions and update current AF mode to CONTINOUS AF (if supported).
     * If auto-focus does not completes, it will notify the {@link OnAutoFocusListener} with
     * isFocusLocked set to false.
     */
    void cancelFocusAndMetering();

    /**
     * An exception thrown when the argument is out of range.
     */
    final class ArgumentOutOfRangeException extends Exception {
        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public ArgumentOutOfRangeException(@NonNull String message) {
            super(message);
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public ArgumentOutOfRangeException(@NonNull String message, @NonNull Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * An exception representing a failure that the operation is canceled which might be caused by
     * a new value is set or camera is closed.
     */
    final class OperationCanceledException extends Exception {
        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public OperationCanceledException(@NonNull String message) {
            super(message);
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public OperationCanceledException(@NonNull String message, @NonNull Throwable cause) {
            super(message, cause);
        }
    }
}
