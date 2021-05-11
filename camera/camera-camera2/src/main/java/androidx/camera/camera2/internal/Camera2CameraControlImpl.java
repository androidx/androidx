/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import static androidx.camera.core.ImageCapture.FLASH_MODE_AUTO;
import static androidx.camera.core.ImageCapture.FLASH_MODE_OFF;
import static androidx.camera.core.ImageCapture.FLASH_MODE_ON;

import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.util.ArrayMap;
import android.util.Rational;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.workaround.AeFpsRange;
import androidx.camera.camera2.internal.compat.workaround.AutoFlashAEModeDisabler;
import androidx.camera.camera2.interop.Camera2CameraControl;
import androidx.camera.camera2.interop.CaptureRequestOptions;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.ExperimentalExposureCompensation;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A Camera2 implementation for CameraControlInternal interface
 *
 * <p>There are 2 states in the control, use count and active boolean. Use count controls
 * whether the user can submit new requests, it can be increased or decreased via
 * {@link #incrementUseCount()} and {@link #decrementUseCount()}. Before sending the request to
 * the control, it must increase use count, otherwise the request will be dropped. Active state
 * controls whether the requests are sent to the camera. It can be set via
 * {@link #setActive(boolean)}. The transition of active boolean from {@code true} to {@code
 * false} may also reset state.
 *
 * <p>There are 4 possible state combinations when processing a request.
 *
 * <ul>
 * <li>Use count >= 1 but active boolean == false: the control can accept new requests for
 * changing parameters, but won't attempt to send them to the camera device yet. New requests can
 * be either cached and replace old requests, or may end with {@code ImmediateFailedFuture}
 * directly, depending on whether the type of request needs to be cached reasonably.</li>
 * <li>Use count >= 1 and active boolean is true: the control now sends cached requests to the
 * camera. Any new requests are also sent directly to the camera.</li>
 * <li>Use count == 0 and active boolean is true: This state may not be possible or may be very
 * short lived depending on how we want to use it. the control does not accept new requests;
 * all requests end in {@code ImmediateFailedFuture}. Previously cached requests may continue
 * processing.</li>
 * <li>Use count == 0 and active boolean is false: the control does not accept new requests; all
 * requests end in {@code ImmediateFailedFuture}. Any cached requests are dropped.</li>
 * </ul>
 */
@OptIn(markerClass = ExperimentalCamera2Interop.class)
public class Camera2CameraControlImpl implements CameraControlInternal {
    private static final String TAG = "Camera2CameraControlImp";
    private static final int DEFAULT_TEMPLATE = CameraDevice.TEMPLATE_PREVIEW;
    @VisibleForTesting
    final CameraControlSessionCallback mSessionCallback;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @CameraExecutor
    final Executor mExecutor;
    private final Object mLock = new Object();
    private final CameraCharacteristicsCompat mCameraCharacteristics;
    private final ControlUpdateCallback mControlUpdateCallback;

    private final SessionConfig.Builder mSessionConfigBuilder = new SessionConfig.Builder();
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    volatile Rational mPreviewAspectRatio = null;
    private final FocusMeteringControl mFocusMeteringControl;
    private final ZoomControl mZoomControl;
    private final TorchControl mTorchControl;
    private final ExposureControl mExposureControl;
    private final Camera2CameraControl mCamera2CameraControl;
    private final AeFpsRange mAeFpsRange;
    @GuardedBy("mLock")
    private int mUseCount = 0;
    // use volatile modifier to make these variables in sync in all threads.
    private volatile boolean mIsTorchOn = false;
    @ImageCapture.FlashMode
    private volatile int mFlashMode = FLASH_MODE_OFF;
    private final AutoFlashAEModeDisabler mAutoFlashAEModeDisabler = new AutoFlashAEModeDisabler();
    private int mTemplate = DEFAULT_TEMPLATE;

    //******************** Should only be accessed by executor *****************************//
    private final CameraCaptureCallbackSet mCameraCaptureCallbackSet =
            new CameraCaptureCallbackSet();
    //**************************************************************************************//

    @VisibleForTesting
    Camera2CameraControlImpl(@NonNull CameraCharacteristicsCompat cameraCharacteristics,
            @NonNull ScheduledExecutorService scheduler,
            @NonNull @CameraExecutor Executor executor,
            @NonNull ControlUpdateCallback controlUpdateCallback) {
        this(cameraCharacteristics, scheduler, executor, controlUpdateCallback,
                new Quirks(new ArrayList<>()));
    }

    /**
     * Constructor for a Camera2CameraControlImpl.
     *
     * <p>All {@code controlUpdateListener} invocations will be on the provided {@code executor}.
     *
     * <p>All tasks scheduled by {@code scheduler} will be immediately executed by {@code executor}.
     *
     * @param cameraCharacteristics Characteristics for the camera being controlled.
     * @param scheduler             Scheduler used for scheduling tasks in the future.
     * @param executor              Camera executor for synchronizing and offloading all commands.
     * @param controlUpdateCallback Listener which will be notified of control changes.
     * @param cameraQuirks          Camera-related quirks of the camera being controlled
     */
    Camera2CameraControlImpl(@NonNull CameraCharacteristicsCompat cameraCharacteristics,
            @NonNull ScheduledExecutorService scheduler,
            @NonNull @CameraExecutor Executor executor,
            @NonNull ControlUpdateCallback controlUpdateCallback,
            @NonNull final Quirks cameraQuirks) {
        mCameraCharacteristics = cameraCharacteristics;
        mControlUpdateCallback = controlUpdateCallback;
        mExecutor = executor;
        mSessionCallback = new CameraControlSessionCallback(mExecutor);
        mSessionConfigBuilder.setTemplateType(mTemplate);
        mSessionConfigBuilder.addRepeatingCameraCaptureCallback(
                CaptureCallbackContainer.create(mSessionCallback));
        // Adding a callback via SessionConfigBuilder requires a expensive updateSessionConfig
        // call. mCameraCaptureCallbackset is for enabling dynamically add/remove
        // CameraCaptureCallback efficiently.
        mSessionConfigBuilder.addRepeatingCameraCaptureCallback(mCameraCaptureCallbackSet);

        mExposureControl = new ExposureControl(this, mCameraCharacteristics, mExecutor);
        mFocusMeteringControl = new FocusMeteringControl(this, scheduler, mExecutor);
        mZoomControl = new ZoomControl(this, mCameraCharacteristics, mExecutor);
        mTorchControl = new TorchControl(this, mCameraCharacteristics, mExecutor);
        mAeFpsRange = new AeFpsRange(cameraQuirks);
        mCamera2CameraControl = new Camera2CameraControl(this, mExecutor);
        mExecutor.execute(
                () -> addCaptureResultListener(mCamera2CameraControl.getCaptureRequestListener()));
    }

    /** Increments the use count of the control. */
    void incrementUseCount() {
        synchronized (mLock) {
            mUseCount++;
        }
    }

    /**
     * Decrements the use count of the control.
     *
     * @throws IllegalStateException if try to decrement the use count to less than zero
     */
    void decrementUseCount() {
        synchronized (mLock) {
            if (mUseCount == 0) {
                throw new IllegalStateException("Decrementing use count occurs more times than "
                        + "incrementing");
            }
            mUseCount--;
        }
    }

    /**
     * Returns the use count of the control.
     *
     * <p>Use count can be increased and decreased via {@link #incrementUseCount()} and
     * {@link #decrementUseCount()}. Camera control only accepts requests when the use count is
     * greater than 0.
     */
    @VisibleForTesting
    int getUseCount() {
        synchronized (mLock) {
            return mUseCount;
        }
    }

    @NonNull
    public ZoomControl getZoomControl() {
        return mZoomControl;
    }

    @NonNull
    public TorchControl getTorchControl() {
        return mTorchControl;
    }

    @NonNull
    public ExposureControl getExposureControl() {
        return mExposureControl;
    }

    @NonNull
    public Camera2CameraControl getCamera2CameraControl() {
        return mCamera2CameraControl;
    }

    @Override
    public void addInteropConfig(@NonNull Config config) {
        ListenableFuture<Void> future = mCamera2CameraControl.addCaptureRequestOptions(
                CaptureRequestOptions.Builder.from(config).build());
        future.addListener(() -> {
        }, CameraXExecutors.directExecutor());
    }

    @Override
    public void clearInteropConfig() {
        ListenableFuture<Void> future = mCamera2CameraControl.clearCaptureRequestOptions();
        future.addListener(() -> {
        }, CameraXExecutors.directExecutor());
    }

    @NonNull
    @Override
    public Config getInteropConfig() {
        return mCamera2CameraControl.getCamera2ImplConfig();
    }

    /**
     * Set current active state. Set active if it is ready to trigger camera control operation.
     *
     * <p>Most operations during inactive state do nothing. Some states are reset to default
     * once it is changed to inactive state.
     */
    @ExecutedBy("mExecutor")
    void setActive(boolean isActive) {
        mFocusMeteringControl.setActive(isActive);
        mZoomControl.setActive(isActive);
        mTorchControl.setActive(isActive);
        mExposureControl.setActive(isActive);
        mCamera2CameraControl.setActive(isActive);
    }

    @ExecutedBy("mExecutor")
    public void setPreviewAspectRatio(@Nullable Rational previewAspectRatio) {
        mPreviewAspectRatio = previewAspectRatio;
    }

    /**
     * Sets a {@link CaptureRequest.Builder} to get the default capture request parameters in order
     * to compare the 3A regions in CaptureResult in FocusMeteringControl.
     */
    public void setDefaultRequestBuilder(@NonNull CaptureRequest.Builder builder) {
        mFocusMeteringControl.setDefaultRequestBuilder(builder);
    }

    @NonNull
    @Override
    public ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action) {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(
                mFocusMeteringControl.startFocusAndMetering(action, mPreviewAspectRatio));
    }

    @NonNull
    @Override
    public ListenableFuture<Void> cancelFocusAndMetering() {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(mFocusMeteringControl.cancelFocusAndMetering());
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setZoomRatio(float ratio) {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(mZoomControl.setZoomRatio(ratio));
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setLinearZoom(float linearZoom) {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(mZoomControl.setLinearZoom(linearZoom));
    }

    @ImageCapture.FlashMode
    @Override
    public int getFlashMode() {
        return mFlashMode;
    }

    /** {@inheritDoc} */
    @Override
    public void setFlashMode(@ImageCapture.FlashMode int flashMode) {
        if (!isControlInUse()) {
            Logger.w(TAG, "Camera is not active.");
            return;
        }
        // update mFlashMode immediately so that following getFlashMode() returns correct value.
        mFlashMode = flashMode;

        updateSessionConfig();
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public ListenableFuture<Void> enableTorch(final boolean torch) {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(mTorchControl.enableTorch(torch));
    }

    /**
     * Issues a {@link CaptureRequest#CONTROL_AF_TRIGGER_START} request to start auto focus scan.
     *
     * @return a {@link ListenableFuture} which completes when the request is completed.
     * Cancelling the ListenableFuture is a no-op.
     */
    @Override
    @NonNull
    public ListenableFuture<CameraCaptureResult> triggerAf() {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(CallbackToFutureAdapter.getFuture(
                completer -> {
                    mExecutor.execute(() -> mFocusMeteringControl.triggerAf(completer));
                    return "triggerAf";
                }));
    }

    /**
     * Issues a {@link CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER_START} request to start auto
     * exposure scan.
     *
     * @return a {@link ListenableFuture} which completes when the request is completed.
     * Cancelling the ListenableFuture is a no-op.
     */
    @Override
    @NonNull
    public ListenableFuture<CameraCaptureResult> triggerAePrecapture() {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(CallbackToFutureAdapter.getFuture(
                completer -> {
                    mExecutor.execute(() -> mFocusMeteringControl.triggerAePrecapture(completer));
                    return "triggerAePrecapture";
                }));
    }

    /**
     * Issues {@link CaptureRequest#CONTROL_AF_TRIGGER_CANCEL} or {@link
     * CaptureRequest#CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL} request to cancel auto focus or auto
     * exposure scan.
     */
    @Override
    public void cancelAfAeTrigger(final boolean cancelAfTrigger,
            final boolean cancelAePrecaptureTrigger) {
        if (!isControlInUse()) {
            Logger.w(TAG, "Camera is not active.");
            return;
        }
        mExecutor.execute(() -> mFocusMeteringControl.cancelAfAeTrigger(cancelAfTrigger,
                cancelAePrecaptureTrigger));
    }

    @NonNull
    @Override
    @ExperimentalExposureCompensation
    public ListenableFuture<Integer> setExposureCompensationIndex(int exposure) {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return mExposureControl.setExposureCompensationIndex(exposure);
    }

    /** {@inheritDoc} */
    @Override
    public void submitCaptureRequests(@NonNull final List<CaptureConfig> captureConfigs) {
        if (!isControlInUse()) {
            Logger.w(TAG, "Camera is not active.");
            return;
        }
        mExecutor.execute(() -> submitCaptureRequestsInternal(captureConfigs));
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public SessionConfig getSessionConfig() {
        mSessionConfigBuilder.setTemplateType(mTemplate);
        mSessionConfigBuilder.setImplementationOptions(getSessionOptions());
        Object tag = mCamera2CameraControl.getCamera2ImplConfig().getCaptureRequestTag(null);
        if (tag != null && tag instanceof Integer) {
            mSessionConfigBuilder.addTag(Camera2CameraControl.TAG_KEY, tag);
        }
        return mSessionConfigBuilder.build();
    }

    @ExecutedBy("mExecutor")
    int getTemplate() {
        return mTemplate;
    }

    @ExecutedBy("mExecutor")
    void setTemplate(int template) {
        mTemplate = template;

        mFocusMeteringControl.setTemplate(mTemplate);
    }

    @ExecutedBy("mExecutor")
    void resetTemplate() {
        setTemplate(DEFAULT_TEMPLATE);
    }

    private boolean isControlInUse() {
        return getUseCount() > 0;
    }

    /**
     * Triggers an update to the session.
     */
    public void updateSessionConfig() {
        mExecutor.execute(this::updateSessionConfigSynchronous);
    }

    @ExecutedBy("mExecutor")
    void updateSessionConfigSynchronous() {
        mControlUpdateCallback.onCameraControlUpdateSessionConfig();
    }

    @ExecutedBy("mExecutor")
    @NonNull
    Rect getCropSensorRegion() {
        return mZoomControl.getCropSensorRegion();
    }

    @Override
    @ExecutedBy("mExecutor")
    @NonNull
    public Rect getSensorRect() {
        return Preconditions.checkNotNull(
                mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE));
    }

    @ExecutedBy("mExecutor")
    void removeCaptureResultListener(@NonNull CaptureResultListener listener) {
        mSessionCallback.removeListener(listener);
    }

    @ExecutedBy("mExecutor")
    void addCaptureResultListener(@NonNull CaptureResultListener listener) {
        mSessionCallback.addListener(listener);
    }

    /** Adds a session {@link CameraCaptureCallback dynamically */
    void addSessionCameraCaptureCallback(@NonNull Executor executor,
            @NonNull CameraCaptureCallback cameraCaptureCallback) {
        mExecutor.execute(() -> {
            mCameraCaptureCallbackSet.addCaptureCallback(executor, cameraCaptureCallback);
        });
    }

    /** Removes the {@link CameraCaptureCallback} that was added previously */
    void removeSessionCameraCaptureCallback(@NonNull CameraCaptureCallback cameraCaptureCallback) {
        mExecutor.execute(() -> {
            mCameraCaptureCallbackSet.removeCaptureCallback(cameraCaptureCallback);
        });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void enableTorchInternal(boolean torch) {
        mIsTorchOn = torch;
        if (!torch) {
            // Send capture request with AE_MODE_ON + FLASH_MODE_OFF to turn off torch.
            CaptureConfig.Builder singleRequestBuilder = new CaptureConfig.Builder();
            singleRequestBuilder.setTemplateType(mTemplate);
            singleRequestBuilder.setUseRepeatingSurface(true);
            Camera2ImplConfig.Builder configBuilder = new Camera2ImplConfig.Builder();
            configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE,
                    getSupportedAeMode(CaptureRequest.CONTROL_AE_MODE_ON));
            configBuilder.setCaptureRequestOption(CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_OFF);
            singleRequestBuilder.addImplementationOptions(configBuilder.build());
            submitCaptureRequestsInternal(
                    Collections.singletonList(singleRequestBuilder.build()));
        }
        updateSessionConfigSynchronous();
    }


    @ExecutedBy("mExecutor")
    void submitCaptureRequestsInternal(final List<CaptureConfig> captureConfigs) {
        mControlUpdateCallback.onCameraControlCaptureRequests(captureConfigs);
    }

    /**
     * Gets session options by current status.
     *
     * <p>The session options are based on the current torch status, flash mode, focus area, crop
     * area, etc... They should be appended to the repeat request.
     */
    @VisibleForTesting
    @ExecutedBy("mExecutor")
    Config getSessionOptions() {
        Camera2ImplConfig.Builder builder = new Camera2ImplConfig.Builder();
        builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

        // AF Mode is assigned in mFocusMeteringControl.
        mFocusMeteringControl.addFocusMeteringOptions(builder);

        mAeFpsRange.addAeFpsRangeOptions(builder);

        mZoomControl.addZoomOption(builder);

        int aeMode = CaptureRequest.CONTROL_AE_MODE_ON;
        if (mIsTorchOn) {
            builder.setCaptureRequestOption(CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_TORCH);
        } else {
            switch (mFlashMode) {
                case FLASH_MODE_OFF:
                    aeMode = CaptureRequest.CONTROL_AE_MODE_ON;
                    break;
                case FLASH_MODE_ON:
                    aeMode = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
                    break;
                case FLASH_MODE_AUTO:
                    aeMode = mAutoFlashAEModeDisabler.getCorrectedAeMode(
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
            }
        }
        builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, getSupportedAeMode(aeMode));

        builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                getSupportedAwbMode(CaptureRequest.CONTROL_AWB_MODE_AUTO));

        mExposureControl.setCaptureRequestOption(builder);

        Config currentConfig = mCamera2CameraControl.getCamera2ImplConfig();
        for (Config.Option<?> option : currentConfig.listOptions()) {
            @SuppressWarnings("unchecked")
            Config.Option<Object> objectOpt = (Config.Option<Object>) option;
            builder.getMutableConfig().insertOption(objectOpt,
                    Config.OptionPriority.ALWAYS_OVERRIDE,
                    currentConfig.retrieveOption(objectOpt));
        }

        return builder.build();
    }

    /**
     * Returns a supported AF mode which will be preferredMode if it is supported.
     *
     * <p><pre>If preferredMode is not supported, fallback with the following priority (highest to
     * lowest).
     * 1) {@link CaptureRequest#CONTROL_AF_MODE_CONTINUOUS_PICTURE}
     * 2) {@link CaptureRequest#CONTROL_AF_MODE_AUTO)}
     * 3) {@link CaptureRequest#CONTROL_AF_MODE_OFF}
     * </pre>
     */
    @ExecutedBy("mExecutor")
    int getSupportedAfMode(int preferredMode) {
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (modes == null) {
            return CaptureRequest.CONTROL_AF_MODE_OFF;
        }

        // if preferredMode is supported, use it
        if (isModeInList(preferredMode, modes)) {
            return preferredMode;
        }

        // if not found, priority is CONTINUOUS_PICTURE > AUTO > OFF
        if (isModeInList(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, modes)) {
            return CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
        } else if (isModeInList(CaptureRequest.CONTROL_AF_MODE_AUTO, modes)) {
            return CaptureRequest.CONTROL_AF_MODE_AUTO;
        }

        return CaptureRequest.CONTROL_AF_MODE_OFF;
    }

    /**
     * Returns a supported AE mode which will be preferredMode if it is supported.
     *
     * <p><pre>If preferredMode is not supported, fallback with the following priority (highest to
     * lowest).
     * 1) {@link CaptureRequest#CONTROL_AE_MODE_ON}
     * 2) {@link CaptureRequest#CONTROL_AE_MODE_OFF)}
     * </pre>
     */
    @ExecutedBy("mExecutor")
    private int getSupportedAeMode(int preferredMode) {
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);

        if (modes == null) {
            return CaptureRequest.CONTROL_AE_MODE_OFF;
        }

        // if preferredMode is supported, use it
        if (isModeInList(preferredMode, modes)) {
            return preferredMode;
        }

        // if not found, priority is AE_ON > AE_OFF
        if (isModeInList(CaptureRequest.CONTROL_AE_MODE_ON, modes)) {
            return CaptureRequest.CONTROL_AE_MODE_ON;
        }

        return CaptureRequest.CONTROL_AE_MODE_OFF;
    }

    /**
     * Returns a supported AWB mode which will be preferredMode if it is supported.
     *
     * <p><pre>If preferredMode is not supported, fallback with the following priority (highest to
     * lowest).
     * 1) {@link CaptureRequest#CONTROL_AWB_MODE_AUTO}
     * 2) {@link CaptureRequest#CONTROL_AWB_MODE_OFF)}
     * </pre>
     */
    @ExecutedBy("mExecutor")
    private int getSupportedAwbMode(int preferredMode) {
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);

        if (modes == null) {
            return CaptureRequest.CONTROL_AWB_MODE_OFF;
        }

        // if preferredMode is supported, use it
        if (isModeInList(preferredMode, modes)) {
            return preferredMode;
        }

        // if not found, priority is AWB_AUTO > AWB_OFF
        if (isModeInList(CaptureRequest.CONTROL_AWB_MODE_AUTO, modes)) {
            return CaptureRequest.CONTROL_AWB_MODE_AUTO;
        }

        return CaptureRequest.CONTROL_AWB_MODE_OFF;
    }

    @ExecutedBy("mExecutor")
    private boolean isModeInList(int mode, int[] modeList) {
        for (int m : modeList) {
            if (mode == m) {
                return true;
            }
        }
        return false;
    }

    int getMaxAfRegionCount() {
        Integer count = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        return count == null ? 0 : count;
    }

    int getMaxAeRegionCount() {
        Integer count = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        return count == null ? 0 : count;
    }

    int getMaxAwbRegionCount() {
        Integer count = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB);
        return count == null ? 0 : count;
    }

    /** An interface to listen to camera capture results. */
    public interface CaptureResultListener {
        /**
         * Callback to handle camera capture results.
         *
         * @param captureResult camera capture result.
         * @return true to finish listening, false to continue listening.
         */
        boolean onCaptureResult(@NonNull TotalCaptureResult captureResult);
    }

    static final class CameraControlSessionCallback extends CaptureCallback {

        /* synthetic accessor */final Set<CaptureResultListener> mResultListeners = new HashSet<>();
        @CameraExecutor
        private final Executor mExecutor;

        CameraControlSessionCallback(@NonNull @CameraExecutor Executor executor) {
            mExecutor = executor;
        }

        @ExecutedBy("mExecutor")
        void addListener(@NonNull CaptureResultListener listener) {
            mResultListeners.add(listener);
        }

        @ExecutedBy("mExecutor")
        void removeListener(@NonNull CaptureResultListener listener) {
            mResultListeners.remove(listener);
        }

        @Override
        public void onCaptureCompleted(
                @NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request,
                @NonNull final TotalCaptureResult result) {

            mExecutor.execute(() -> {
                Set<CaptureResultListener> removeSet = new HashSet<>();
                for (CaptureResultListener listener : mResultListeners) {
                    boolean isFinished = listener.onCaptureResult(result);
                    if (isFinished) {
                        removeSet.add(listener);
                    }
                }

                if (!removeSet.isEmpty()) {
                    mResultListeners.removeAll(removeSet);
                }
            });
        }
    }

    /**
     * A set of {@link CameraCaptureCallback}s which is capable of adding/removing callbacks
     * dynamically.
     */
    static final class CameraCaptureCallbackSet extends CameraCaptureCallback {
        Set<CameraCaptureCallback> mCallbacks = new HashSet<>();
        Map<CameraCaptureCallback, Executor> mCallbackExecutors = new ArrayMap<>();

        @ExecutedBy("mExecutor")
        void addCaptureCallback(@NonNull Executor executor,
                @NonNull CameraCaptureCallback callback) {
            mCallbacks.add(callback);
            mCallbackExecutors.put(callback, executor);
        }

        @ExecutedBy("mExecutor")
        void removeCaptureCallback(@NonNull CameraCaptureCallback callback) {
            mCallbacks.remove(callback);
            mCallbackExecutors.remove(callback);
        }

        @ExecutedBy("mExecutor")
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureResult cameraCaptureResult) {
            for (CameraCaptureCallback callback : mCallbacks) {
                try {
                    mCallbackExecutors.get(callback).execute(() -> {
                        callback.onCaptureCompleted(cameraCaptureResult);
                    });
                } catch (RejectedExecutionException e) {
                    Logger.e(TAG, "Executor rejected to invoke onCaptureCompleted.", e);
                }
            }
        }

        @ExecutedBy("mExecutor")
        @Override
        public void onCaptureFailed(@NonNull CameraCaptureFailure failure) {
            for (CameraCaptureCallback callback : mCallbacks) {
                try {
                    mCallbackExecutors.get(callback).execute(() -> {
                        callback.onCaptureFailed(failure);
                    });
                } catch (RejectedExecutionException e) {
                    Logger.e(TAG, "Executor rejected to invoke onCaptureFailed.", e);
                }
            }
        }

        @ExecutedBy("mExecutor")
        @Override
        public void onCaptureCancelled() {
            for (CameraCaptureCallback callback : mCallbacks) {
                try {
                    mCallbackExecutors.get(callback).execute(() -> {
                        callback.onCaptureCancelled();
                    });
                } catch (RejectedExecutionException e) {
                    Logger.e(TAG, "Executor rejected to invoke onCaptureCancelled.", e);
                }
            }
        }
    }
}
