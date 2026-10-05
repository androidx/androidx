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
import androidx.annotation.IntRange;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

/**
 * Provides asynchronous camera operations, such as zoom, focus and metering, torch, and exposure
 * compensation, that affect the output of all {@link UseCase}s currently bound to the camera.
 *
 * <p>The application can retrieve the {@link CameraControl} instance via
 * {@link Camera#getCameraControl()}. {@link CameraControl} is ready to start operations
 * immediately after {@link Camera} is retrieved and {@link UseCase}s are bound to that camera.
 *
 * <p>Settings applied through this {@code CameraControl} remain in effect while
 * {@link UseCase}s are bound to the camera, including when only some of the bound
 * {@link UseCase}s are unbound. When all {@link UseCase}s are unbound from the camera, or when
 * the camera is closed because the lifecycle it is bound to is stopped, pending operations fail
 * with {@link CameraControl.OperationCanceledException}, and the zoom, torch, low-light boost,
 * focus and metering, and exposure compensation settings are reset to their default values.
 *
 * <p>Each method of {@code CameraControl} returns a {@link ListenableFuture} which apps can use to
 * check the asynchronous result. If an operation is not allowed in the current state, for example
 * when the camera is closed, the returned {@link ListenableFuture} fails immediately with
 * {@link CameraControl.OperationCanceledException}. If an operation is not supported by the
 * current session configuration, for example when a CameraX extension is enabled, the returned
 * {@link ListenableFuture} fails with {@link IllegalStateException}.
 */
public interface CameraControl {
    /**
     * Enables or disables the torch.
     *
     * <p>{@link CameraInfo#getTorchState()} can be used to query or observe the torch state. When
     * called while the camera is open and has a flash unit, {@link CameraInfo#getTorchState()} is
     * updated without waiting for the camera to apply the change.
     *
     * <p>The returned {@link ListenableFuture} fails in the following cases:
     * <ul>
     * <li>{@link IllegalStateException} if the camera doesn't have a flash unit (see
     * {@link CameraInfo#hasFlashUnit()}) or torch is not supported by the current session
     * configuration. The call does nothing, the future fails immediately, and the torch state
     * remains {@link TorchState#OFF}.
     * <li>{@link OperationCanceledException} if a newer call is made or the camera is closed
     * before completion.
     * </ul>
     *
     * <p>Cancellation of the returned future is a no-op.
     *
     * <p>When the torch is enabled, the torch will remain enabled during photo capture regardless
     * of the flashMode setting. When the torch is disabled, flash will function as the flash mode
     * set by either {@link ImageCapture#setFlashMode(int)} or
     * {@link ImageCapture.Builder#setFlashMode(int)}.
     *
     * <p>Torch and low-light boost (see {@link #enableLowLightBoostAsync(boolean)}) are mutually
     * exclusive and follow a "last setting wins" policy. Enabling the torch will automatically
     * turn off low-light boost if it is active. Disabling the torch will not restore low-light
     * boost.
     *
     * @param torch {@code true} to turn on the torch, {@code false} to turn it off.
     * @return a {@link ListenableFuture} that completes when the torch state has been updated on
     * the camera.
     */
    @NonNull ListenableFuture<Void> enableTorch(boolean torch);

    /**
     * Enables or disables low-light boost mode.
     *
     * <p>Devices running Android 15 or higher can provide support for low-light boost. This
     * feature can automatically adjust the brightness of the preview, video or image analysis
     * streams in low-light conditions. This is different from how the night mode camera
     * extension creates still images, because night mode combines a burst of photos to create a
     * single, enhanced image. While night mode works very well for creating a still image, it
     * can't create a continuous stream of frames, but Low Light Boost can. Thus, Low Light Boost
     * enables new camera capabilities, such as the following:
     *
     * <ul>
     * <li>Providing an enhanced image preview, so users can better frame their low-light pictures.
     * <li>Recording brighter videos in low-light conditions.
     * <li>Scanning QR codes in low-light conditions.
     * </ul>
     *
     * <p>Applications can query the low-light boost availability via
     * {@link CameraInfo#isLowLightBoostSupported()}. If you enable Low Light Boost, it
     * automatically turns on when there's a low light level, and turns off when there's more light.
     *
     * <p>If the camera device does not support low-light boost, the value obtained via
     * {@link CameraInfo#getLowLightBoostState()} will always be {@link LowLightBoostState#OFF}.
     *
     * <p>Note that this mode may interact with other configurations:
     *
     * <ul>
     * <li>Low-light boost and torch (see {@link #enableTorch(boolean)}) are mutually exclusive
     * and follow a "last setting wins" policy. Enabling low-light boost will automatically turn
     * off the torch if it is active, and enabling the torch will automatically turn off low-light
     * boost.
     * <li>When capturing a picture with {@link ImageCapture} while low-light boost is active,
     * flash (if enabled by {@link ImageCapture#setFlashMode(int)}) is allowed to fire for the
     * capture, and low-light boost will remain active for preview.
     * <li>When frame rate configuration results in an FPS exceeding 30, low-light boost will be
     * disabled and the state will always be {@link LowLightBoostState#OFF}.
     * </ul>
     *
     * <p>To ensure low-light boost mode functions correctly, the frame rate must not exceed 30 FPS.
     *
     * <p>The returned {@link ListenableFuture} fails in the following cases:
     * <ul>
     * <li>{@link IllegalStateException} if low-light boost is not available because the device
     * does not support it or there is a settings conflict. The failure reason is provided in the
     * exception's message.
     * <li>{@link OperationCanceledException} if a newer value is set or the camera is closed
     * before completion.
     * </ul>
     *
     * <p>Cancellation of the returned future is a no-op.
     *
     * <p>Use {@link CameraInfo#getLowLightBoostState()} to observe whether low-light boost is
     * active.
     *
     * @param lowLightBoost {@code true} to turn on low-light boost mode, {@code false} to turn it
     *                      off.
     * @return a {@link ListenableFuture} that completes when low-light boost mode has been updated
     * on the camera.
     * @see CameraInfo#isLowLightBoostSupported()
     */
    default @NonNull ListenableFuture<Void> enableLowLightBoostAsync(boolean lowLightBoost) {
        return Futures.immediateFailedFuture(new OperationCanceledException("Not supported!"));
    }

    /**
     * Starts a focus and metering action configured by the {@link FocusMeteringAction}.
     *
     * <p>By default, it triggers an autofocus scan and updates the AF/AE/AWB metering regions. The
     * action is configured by a {@link FocusMeteringAction} which contains the configuration of
     * multiple AF/AE/AWB {@link MeteringPoint}s, the 3A locking mode, and an auto-cancel duration.
     * See {@link FocusMeteringAction} for more details.
     *
     * <p>Only one {@link FocusMeteringAction} is allowed to run at a time. If multiple
     * {@link FocusMeteringAction} are executed in a row, only the latest one will work and
     * other actions will be cancelled. However, starting a new action does not unlock 3A
     * components (AF, AE, or AWB) that were already locked by a previous action if they are not
     * included in the new action's {@link FocusMeteringAction.Builder#setLockingMode(int) locking
     * mode}. 3A locks are released and continuous autofocus is restored only when
     * {@link #cancelFocusAndMetering()} is called or the latest action's auto-cancel duration is
     * reached.
     *
     * <p>If the {@link FocusMeteringAction} specifies more AF/AE/AWB points than what is
     * supported on the current device, only the first point and then in order up to the number of
     * points supported by the device will be enabled.
     *
     * <p>When autofocus completes and AF/AE/AWB regions are updated, the returned
     * {@link ListenableFuture} completes with {@link FocusMeteringResult}. If autofocus does not
     * converge within a time limit, the future completes with
     * {@link FocusMeteringResult#isFocusSuccessful()} set to {@code false}, and continuous
     * autofocus remains disabled until the action is cancelled. If no AF points are added,
     * autofocus is not triggered and {@link FocusMeteringResult#isFocusSuccessful()} is
     * {@code false}. If {@link FocusMeteringAction#FLAG_AF} is excluded from the locking mode,
     * autofocus is not triggered and the future completes once the metering regions and any
     * requested AE/AWB locks are updated.
     *
     * <p>The returned {@link ListenableFuture} fails in the following cases:
     * <ul>
     * <li>{@link IllegalArgumentException} if none of the specified AF/AE/AWB
     * {@link MeteringPoint}s are supported on the device or none of the points generate valid
     * metering rectangles. The future fails immediately.
     * <li>{@link IllegalStateException} if focus and metering is not supported by the current
     * session configuration.
     * <li>{@link OperationCanceledException} if a newer action is started,
     * {@link #cancelFocusAndMetering()} is called, the auto-cancel duration elapses, or the camera
     * is closed before completion.
     * </ul>
     *
     * <p>Cancellation of the returned future is a no-op.
     *
     * @param action the {@link FocusMeteringAction} to be executed.
     * @return a {@link ListenableFuture} that completes with the {@link FocusMeteringResult} when
     * the focus and metering action finishes.
     * @see FocusMeteringAction
     */
    @NonNull ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action);

    /**
     * Cancels current {@link FocusMeteringAction} and clears AF/AE/AWB regions.
     *
     * <p>Clears the AF/AE/AWB regions, unlocks any 3A (AF, AE, AWB) locks acquired by
     * {@link #startFocusAndMetering(FocusMeteringAction)}, and updates current AF mode to
     * continuous AF (if supported). If current {@link FocusMeteringAction} has not completed, the
     * returned {@link ListenableFuture} in {@link #startFocusAndMetering} will fail with
     * {@link OperationCanceledException}.
     *
     * <p>The returned {@link ListenableFuture} fails with {@link OperationCanceledException} if a
     * newer focus and metering operation is started or the camera is closed before completion.
     *
     * <p>Cancellation of the returned future is a no-op.
     *
     * @return a {@link ListenableFuture} that completes when the AF/AE/AWB regions are cleared,
     * 3A locks are unlocked, and AF mode is set to continuous focus (if supported).
     */
    @NonNull ListenableFuture<Void> cancelFocusAndMetering();

    /**
     * Sets current zoom by ratio.
     *
     * <p>It modifies both current {@code zoomRatio} and {@code linearZoom} in
     * {@link CameraInfo#getZoomState()}, so if apps are observing {@code zoomRatio} or
     * {@code linearZoom}, they will get the update as well. When a valid ratio is provided, the
     * {@link ZoomState} in {@link CameraInfo#getZoomState()} is updated immediately without
     * waiting for the camera to apply the zoom, while the actual camera zoom adjustment is
     * performed asynchronously.
     *
     * <p>The returned {@link ListenableFuture} fails in the following cases:
     * <ul>
     * <li>{@link IllegalArgumentException} if the ratio is smaller than
     * {@link ZoomState#getMinZoomRatio()} or larger than {@link ZoomState#getMaxZoomRatio()}. The
     * current zoom ratio is not modified. It is the application's duty to clamp the ratio.
     * <li>{@link IllegalStateException} if zoom is not supported by the current session
     * configuration.
     * <li>{@link OperationCanceledException} if a newer zoom value is set or the camera is closed
     * before completion.
     * </ul>
     *
     * <p>Cancellation of the returned future is a no-op.
     *
     * @param ratio the zoom ratio to set, from {@link ZoomState#getMinZoomRatio()} to
     *              {@link ZoomState#getMaxZoomRatio()} inclusive.
     * @return a {@link ListenableFuture} that completes when the camera has applied the requested
     * zoom.
     */
    @NonNull ListenableFuture<Void> setZoomRatio(float ratio);

    /**
     * Sets current zoom by a linear zoom value ranging from 0f to 1.0f. LinearZoom 0f represents
     * the minimum zoom while linearZoom 1.0f represents the maximum zoom. The advantage of
     * linearZoom is that it ensures the field of view (FOV) varies linearly with the linearZoom
     * value, for use with slider UI elements (while {@link #setZoomRatio(float)} works well
     * for pinch-zoom gestures).
     *
     * <p>It modifies both current {@code zoomRatio} and {@code linearZoom} in
     * {@link CameraInfo#getZoomState()}, so if apps are observing {@code zoomRatio} or
     * {@code linearZoom}, they will get the update as well. When a valid {@code linearZoom} is
     * provided, the {@link ZoomState} in {@link CameraInfo#getZoomState()} is updated immediately
     * without waiting for the camera to apply the zoom, while the actual camera zoom adjustment is
     * performed asynchronously.
     *
     * <p>The returned {@link ListenableFuture} fails in the following cases:
     * <ul>
     * <li>{@link IllegalArgumentException} if the linearZoom is not in the range {@code [0..1]}.
     * The current linearZoom and zoomRatio are not modified. It is the application's duty to
     * clamp the linearZoom within {@code [0..1]}.
     * <li>{@link IllegalStateException} if zoom is not supported by the current session
     * configuration.
     * <li>{@link OperationCanceledException} if a newer zoom value is set or the camera is closed
     * before completion.
     * </ul>
     *
     * <p>Cancellation of the returned future is a no-op.
     *
     * @param linearZoom the linear zoom value to set, in the range {@code [0..1]}.
     * @return a {@link ListenableFuture} that completes when the camera has applied the requested
     * zoom.
     */
    @NonNull ListenableFuture<Void> setLinearZoom(@FloatRange(from = 0f, to = 1f) float linearZoom);

    /**
     * Sets the exposure compensation index for the camera.
     *
     * <p>When a valid exposure compensation index is provided while the camera is open,
     * {@link ExposureState#getExposureCompensationIndex()} (via
     * {@link CameraInfo#getExposureState()}) is updated immediately, while the returned
     * {@link ListenableFuture} completes asynchronously with the new target exposure index once
     * auto-exposure converges to the target exposure.
     *
     * <p>Only one {@link #setExposureCompensationIndex} is allowed to run at the same time. If
     * multiple {@link #setExposureCompensationIndex} are executed in a row, only the latest one
     * setting will be kept in the camera. The other actions will be cancelled and their
     * {@link ListenableFuture}s will fail with {@link OperationCanceledException}. After all the
     * previous actions are cancelled, the camera device will adjust the brightness according to
     * the latest setting.
     *
     * <p>The returned {@link ListenableFuture} fails in the following cases:
     * <ul>
     * <li>{@link IllegalArgumentException} if exposure compensation is not supported on the
     * camera (see {@link ExposureState#isExposureCompensationSupported()}) or {@code value} is
     * outside {@link ExposureState#getExposureCompensationRange()}.
     * <li>{@link IllegalStateException} if exposure compensation is not supported by the current
     * session configuration.
     * <li>{@link OperationCanceledException} if the camera is closed, or a newer
     * {@link #setExposureCompensationIndex} call is made before the camera reaches the requested
     * exposure target.
     * </ul>
     *
     * <p>When the future fails with {@link IllegalArgumentException} or
     * {@link IllegalStateException}, it fails immediately without changing
     * {@link ExposureState#getExposureCompensationIndex()}.
     *
     * <p>Cancellation of the returned future is a no-op.
     *
     * @param value the exposure compensation index to set on the camera, within
     *              {@link ExposureState#getExposureCompensationRange()}.
     * @return a {@link ListenableFuture} that completes with the new target exposure compensation
     * index when the camera reaches the requested exposure target.
     */
    @NonNull ListenableFuture<Integer> setExposureCompensationIndex(int value);

    /**
     * Sets torch strength level.
     *
     * <p>The torch strength level only applies when the torch is turned on by
     * {@link #enableTorch(boolean)} and doesn't affect other usages of the flash unit.
     *
     * <p>Use the value returned by {@link CameraInfo#getMaxTorchStrengthLevel()} to set the maximum
     * level the device can provide and use {@code 1} to set the minimum level. When a valid level
     * is provided, {@link CameraInfo#getTorchStrengthLevel()} is updated without waiting for the
     * camera to apply it. This method can also be called when the torch is currently
     * {@link TorchState#OFF}, in which case the returned {@link ListenableFuture} completes
     * immediately and the new strength level will take effect the next time
     * {@link #enableTorch(boolean)} is called with {@code true}.
     *
     * <p>The returned {@link ListenableFuture} fails in the following cases:
     * <ul>
     * <li>{@link IllegalArgumentException} if the level is greater than
     * {@link CameraInfo#getMaxTorchStrengthLevel()} or less than {@code 1}. The torch strength is
     * not modified.
     * <li>{@link UnsupportedOperationException} if the device doesn't have a flash unit or
     * doesn't support configuring torch strength level (see
     * {@link CameraInfo#isTorchStrengthSupported()}).
     * <li>{@link OperationCanceledException} if a newer value is set or the camera is closed
     * before the strength is applied.
     * </ul>
     *
     * <p>Cancellation of the returned future is a no-op.
     *
     * @param torchStrengthLevel the desired torch strength level, from {@code 1} to
     *                           {@link CameraInfo#getMaxTorchStrengthLevel()}.
     * @return a {@link ListenableFuture} that completes when the torch strength level has been
     * applied.
     */
    @SuppressWarnings("AsyncSuffixFuture")
    default @NonNull ListenableFuture<Void> setTorchStrengthLevel(
            @IntRange(from = 1) int torchStrengthLevel) {
        return Futures.immediateFailedFuture(new UnsupportedOperationException(
                "Setting torch strength is not supported on the device."));
    }

    /**
     * Applies interoperability configuration to this camera control.
     *
     * <p>To configure Camera2 options, use {@code Camera2Interop.forCameraControl(configurator)}
     * (from the {@code camera-camera2} artifact) to create a configurator, then pass it to this
     * method.
     *
     * <p>All parameters set within a single {@code configurator} are applied together atomically in
     * a single repeating capture request update. Subsequent calls to {@code applyInteropAsync} add
     * to or update the existing parameters incrementally without clearing previously set keys,
     * unless explicitly cleared via methods such as
     * {@code CameraControlCamera2Interop.clearCaptureRequestOption} or
     * {@code CameraControlCamera2Interop.clearAllCaptureRequestOptions}. This overwrites options
     * set with {@code SessionConfigInterop} via {@link SessionConfig.Builder#setInterop}.
     *
     * <p>The returned {@link ListenableFuture} fails with
     * {@link CameraControl.OperationCanceledException} if a newer configuration is applied before
     * this operation takes effect or if the camera is closed.
     *
     * <p>Cancellation of the returned future is a no-op.
     *
     * <p><b>Note:</b> Using Camera2 interop options can override internal CameraX
     * configurations. If an option configured via interop conflicts with options required by
     * CameraX internally, the option from Camera2Interop will override, which may result in
     * unexpected behavior or interfere with 3A routines and camera control APIs.
     *
     * <p><b>Warning:</b> Callbacks configured via interop receive raw
     * {@link android.hardware.camera2.CameraCaptureSession} instances. Directly invoking
     * state-altering methods on these raw objects (such as
     * {@link android.hardware.camera2.CameraCaptureSession#close()} or
     * {@link android.hardware.camera2.CameraCaptureSession#abortCaptures()}) bypasses CameraX
     * pipeline management and may cause state desynchronization, stream interruption, or
     * application crashes.
     *
     * @param configurator the configurator that sets the interoperability options.
     * @return a {@link ListenableFuture} that completes with {@code null} when the
     * interoperability options have been applied.
     */
    default @NonNull ListenableFuture<Void> applyInteropAsync(
            @NonNull InteropConfigurator<? super CameraControl> configurator) {
        return Futures.immediateFailedFuture(new OperationCanceledException("Not supported!"));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    default @NonNull MutableConfig getInteropMutableConfig() {
        return MutableOptionsBundle.create();
    }
    /**
     * An exception indicating that a {@link CameraControl} operation was canceled, for example
     * because a newer value was set or the camera was closed.
     *
     * <p>This is different from {@link CancellationException}. While
     * {@link CancellationException} means the {@link ListenableFuture} was cancelled by
     * {@link Future#cancel(boolean)}, {@link OperationCanceledException} means that
     * {@link CameraControl} canceled the operation itself.
     */
    final class OperationCanceledException extends Exception {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public OperationCanceledException(@NonNull String message) {
            super(message);
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public OperationCanceledException(@NonNull String message, @NonNull Throwable cause) {
            super(message, cause);
        }
    }
}
