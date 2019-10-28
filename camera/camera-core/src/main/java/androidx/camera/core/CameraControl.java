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

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.FocusMeteringAction.OnAutoFocusListener;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * An interface for controlling camera's zoom, focus and metering across all use cases.
 *
 * <p>Applications can retrieve the interface via CameraX.getCameraControl.
 */
public interface CameraControl {
    /**
     * Starts a focus and metering action by the {@link FocusMeteringAction}.
     *
     * <p>The {@link FocusMeteringAction} contains the configuration of multiple 3A
     * {@link MeteringPoint}s, auto-cancel duration and{ @link OnAutoFocusListener} to receive the
     * auto-focus result. Check {@link FocusMeteringAction} for more details.
     *
     * @param action the {@link FocusMeteringAction} to be executed.
     */
    void startFocusAndMetering(@NonNull FocusMeteringAction action);

    /**
     * Cancels current {@link FocusMeteringAction}.
     *
     * <p>It clears the 3A regions and update current AF mode to CONTINOUS AF (if supported).
     * If auto-focus does not complete, it will notify the {@link OnAutoFocusListener} with
     * isFocusLocked set to false.
     */
    void cancelFocusAndMetering();

    /**
     * Sets current zoom by ratio.
     *
     * <p>It modifies both current zoom ratio and zoom percentage so if apps are observing
     * zoomRatio or zoomPercentage, they will get the update as well. If the ratio is
     * smaller than {@link CameraInfo#getMinZoomRatio()} or larger than
     * {@link CameraInfo#getMaxZoomRatio()}, it won't modify current zoom ratio. It is
     * applications' duty to clamp the ratio.
     *
     * @return a {@link ListenableFuture} which is finished when current repeating request
     *     result contains the requested zoom ratio. It fails with
     *     {@link OperationCanceledException} if there is newer value being set or camera is closed.
     *     If ratio is out of range, it fails with
     *     {@link CameraControl.ArgumentOutOfRangeException}.
     */
    @NonNull
    ListenableFuture<Void> setZoomRatio(float ratio);

    /**
     * Sets current zoom by percentage ranging from 0f to 1.0f. Percentage 0f represents the
     * minimum zoom while percentage 1.0f represents the maximum zoom. One advantage of zoom
     * percentage is that it ensures FOV varies linearly with the percentage value.
     *
     * <p>It modifies both current zoom ratio and zoom percentage so if apps are observing
     * zoomRatio or zoomPercentage, they will get the update as well. If the percentage is not in
     * the range [0..1], it won't modify current zoom percentage and zoom ratio. It is
     * applications' duty to clamp the zoomPercentage within [0..1].
     *
     * @return a {@link ListenableFuture} which is finished when current repeating request
     *     result contains the requested zoom percentage. It fails with
     *     {@link OperationCanceledException} if there is newer value being set or camera is closed.
     *     If percentage is out of range, it fails with
     *     {@link CameraControl.ArgumentOutOfRangeException}.
     */
    @NonNull
    ListenableFuture<Void> setZoomPercentage(@FloatRange(from = 0f, to = 1f) float percentage);

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
