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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

/**
 * The {@link CameraControl} provides various asynchronous operations like zoom, focus and
 * metering which affects output of all {@link UseCase}s currently bound to that camera.
 *
 * <p>The application can retrieve the {@link CameraControl} instance via
 * {@link Camera#getCameraControl()}. {@link CameraControl} is ready to start operations
 * immediately after {@link Camera} is retrieved and {@link UseCase}s are bound to that camera.
 * When all {@link UseCase}s are unbound, or when camera is closing or closed because
 * lifecycle onStop happens, the {@link CameraControl} will reject all operations.
 *
 * <p>Each method Of {@link CameraControl} returns a {@link ListenableFuture} which apps can use to
 * check the asynchronous result. If the operation is not allowed in current state, the returned
 * {@link ListenableFuture} will fail immediately with
 * {@link CameraControl.OperationCanceledException}.
 */
public interface CameraControl {
    /**
     * Enable the torch or disable the torch.
     *
     * <p>{@link CameraInfo#getTorchState()} can be used to query the torch state.
     * If the camera doesn't have a flash unit (see {@link CameraInfo#hasFlashUnit()}), then the
     * call will do nothing, the returned {@link ListenableFuture} will complete immediately with
     * a failed result and the torch state will be {@link TorchState#OFF}.
     *
     * <p>When the torch is enabled, the torch will remain enabled during photo capture regardless
     * of the flashMode setting. When the torch is disabled, flash will function as the flash mode
     * set by either {@link ImageCapture#setFlashMode(int)} or
     * {@link ImageCapture.Builder#setFlashMode(int)}.
     *
     * @param torch true to turn on the torch, false to turn it off.
     * @return A {@link ListenableFuture} which is successful when the torch was changed to the
     * value specified. It fails when it is unable to change the torch state. Cancellation of
     * this future is a no-op.
     */
    @NonNull
    ListenableFuture<Void> enableTorch(boolean torch);

    /**
     * Starts a focus and metering action configured by the {@link FocusMeteringAction}.
     *
     * <p>It will trigger an auto focus action and enable AF/AE/AWB metering regions. The action
     * is configured by a {@link FocusMeteringAction} which contains the configuration of
     * multiple AF/AE/AWB {@link MeteringPoint}s and an auto-cancel duration. See
     * {@link FocusMeteringAction} for more details.
     *
     * <p>Only one {@link FocusMeteringAction} is allowed to run at a time. If multiple
     * {@link FocusMeteringAction} are executed in a row, only the latest one will work and
     * other actions will be cancelled.
     *
     * <p>If the {@link FocusMeteringAction} specifies more AF/AE/AWB points than what is
     * supported on the current device, only the first point and then in order up to the number of
     * points supported by the device will be enabled.
     *
     * <p>If none of the points with either AF/AE/AWB can be supported on the device or none of
     * the points generates valid metering rectangles, the returned {@link ListenableFuture} in
     * {@link CameraControl#startFocusAndMetering(FocusMeteringAction)} will fail immediately.
     *
     * @see FocusMeteringAction
     *
     * @param action the {@link FocusMeteringAction} to be executed.
     * @return A {@link ListenableFuture} which completes with {@link FocusMeteringAction} when
     * auto focus is done and AF/AE/AWB regions are updated. In case AF points are not added,
     * auto focus will not be triggered and this {@link ListenableFuture} completes when
     * AE/AWB regions are updated. Cancellation of this future is a no-op.
     */
    @NonNull
    ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action);

    /**
     * Cancels current {@link FocusMeteringAction} and clears AF/AE/AWB regions.
     *
     * <p>Clear the AF/AE/AWB regions and update current AF mode to continuous AF (if
     * supported). If current {@link FocusMeteringAction} has not completed, the returned
     * {@link ListenableFuture} in {@link #startFocusAndMetering} will fail with
     * {@link OperationCanceledException}.
     *
     * @return A {@link ListenableFuture} which completes when the AF/AE/AWB regions is clear and AF
     * mode is set to continuous focus (if supported). Cancellation of this future is a no-op.
     */
    @NonNull
    ListenableFuture<Void> cancelFocusAndMetering();

    /**
     * Sets current zoom by ratio.
     *
     * <p>It modifies both current zoomRatio and linearZoom so if apps are observing
     * zoomRatio or linearZoom, they will get the update as well. If the ratio is
     * smaller than {@link ZoomState#getMinZoomRatio()} or larger than
     * {@link ZoomState#getMaxZoomRatio()}, the returned {@link ListenableFuture} will fail with
     * {@link IllegalArgumentException} and it won't modify current zoom ratio. It is the
     * applications' duty to clamp the ratio.
     *
     * @return a {@link ListenableFuture} which is finished when current repeating request
     * result contains the requested zoom ratio. It fails with
     * {@link OperationCanceledException} if there is newer value being set or camera is closed. If
     * the ratio is out of range, it fails with {@link IllegalArgumentException}. Cancellation of
     * this future is a no-op.
     */
    @NonNull
    ListenableFuture<Void> setZoomRatio(float ratio);

    /**
     * Sets current zoom by a linear zoom value ranging from 0f to 1.0f. LinearZoom 0f represents
     * the minimum zoom while linearZoom 1.0f represents the maximum zoom. The advantage of
     * linearZoom is that it ensures the field of view (FOV) varies linearly with the linearZoom
     * value, for use with slider UI elements (while {@link #setZoomRatio(float)} works well
     * for pinch-zoom gestures).
     *
     * <p>It modifies both current zoomRatio and linearZoom so if apps are observing
     * zoomRatio or linearZoom, they will get the update as well. If the linearZoom is not in
     * the range [0..1], the returned {@link ListenableFuture} will fail with
     * {@link IllegalArgumentException} and it won't modify current linearZoom and zoomRatio. It is
     * application's duty to clamp the linearZoom within [0..1].
     *
     * @return a {@link ListenableFuture} which is finished when current repeating request
     * result contains the requested linearZoom. It fails with
     * {@link OperationCanceledException} if there is newer value being set or camera is closed.
     * If linearZoom is not in range [0..1], it fails with {@link IllegalArgumentException}.
     * Cancellation of this future is a no-op.
     */
    @NonNull
    ListenableFuture<Void> setLinearZoom(@FloatRange(from = 0f, to = 1f) float linearZoom);

    /**
     * Set the exposure compensation value for the camera.
     *
     * <p>Only one {@link #setExposureCompensationIndex} is allowed to run at the same time. If
     * multiple {@link #setExposureCompensationIndex} are executed in a row, only the latest one
     * setting will be kept in the camera. The other actions will be cancelled and the
     * ListenableFuture will fail with the {@link OperationCanceledException}. After all the
     * previous actions is cancelled, the camera device will adjust the brightness according to
     * the latest setting.
     *
     * @param value the exposure compensation value to set on the camera which must be within
     *              the range of ExposureState#getExposureCompensationRange(). If the exposure
     *              compensation value is not in the range defined above, the returned
     *              {@link ListenableFuture} will fail with {@link IllegalArgumentException} and
     *              the value from ExposureState#getExposureCompensationIndex will not change.
     * @return a {@link ListenableFuture} which is finished when the camera reaches the newly
     * requested exposure target. Cancellation of this future is a no-op. The result of the
     * ListenableFuture is the new target exposure value, or cancelled with the following
     * exceptions,
     * <ul>
     * <li>{@link OperationCanceledException} when the camera is closed or a
     * new {@link #setExposureCompensationIndex} is called.
     * <li>{@link IllegalArgumentException} while the exposure compensation value to ranging
     * within {@link ExposureState#getExposureCompensationRange}.
     * </ul>
     */
    @NonNull
    ListenableFuture<Integer> setExposureCompensationIndex(int value);

    /**
     * An exception representing a failure that the operation is canceled which might be caused by
     * a new value is set or camera is closed.
     *
     * <p>This is different from {@link CancellationException}. While
     * {@link CancellationException} means the {@link ListenableFuture} was cancelled by
     * {@link Future#cancel(boolean)}, {@link OperationCanceledException} occurs when there is
     * something wrong inside CameraControl and it has to cancel the operation.
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
