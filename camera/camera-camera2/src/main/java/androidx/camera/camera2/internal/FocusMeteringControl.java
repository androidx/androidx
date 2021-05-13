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

package androidx.camera.camera2.internal;

import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;
import android.util.Rational;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.core.CameraControl;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of focus and metering.
 *
 * <p>It is intended to be used within {@link Camera2CameraControlImpl} to implement the
 * functionality of {@link Camera2CameraControlImpl#startFocusAndMetering(FocusMeteringAction)} and
 * {@link Camera2CameraControlImpl#cancelFocusAndMetering()}. This class depends on
 * {@link Camera2CameraControlImpl} to provide some low-level methods such as updateSessionConfig,
 * triggerAfInternal and cancelAfAeTriggerInternal to achieve the focus and metering functions.
 *
 * <p>To wait for the auto-focus lock, it calls
 * {@link Camera2CameraControlImpl
 * #addCaptureResultListener(Camera2CameraControlImpl.CaptureResultListener)}
 * to monitor the capture result. It also requires {@link ScheduledExecutorService} to schedule the
 * auto-cancel event and {@link Executor} to ensure all the methods within this class are called
 * in the same thread as the Camera2CameraControlImpl.
 *
 * <p>The {@link Camera2CameraControlImpl} calls
 * {@link FocusMeteringControl#addFocusMeteringOptions} to construct the 3A regions and append
 * them to all repeating requests and single requests.
 */
class FocusMeteringControl {
    private static final String TAG = "FocusMeteringControl";
    private final Camera2CameraControlImpl mCameraControl;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @CameraExecutor
    final Executor mExecutor;
    private final ScheduledExecutorService mScheduler;

    private volatile boolean mIsActive = false;

    //******************** Should only be accessed by executor (WorkThread) ****************//
    private boolean mIsInAfAutoMode = false;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    Integer mCurrentAfState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
    private ScheduledFuture<?> mAutoCancelHandle;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mFocusTimeoutCounter = 0;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    boolean mIsAutoFocusCompleted = false;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    boolean mIsFocusSuccessful = false;
    private int mTemplate = CameraDevice.TEMPLATE_PREVIEW;

    private Camera2CameraControlImpl.CaptureResultListener mSessionListenerForFocus = null;
    private Camera2CameraControlImpl.CaptureResultListener mSessionListenerForCancel = null;
    private MeteringRectangle[] mAfRects = new MeteringRectangle[]{};
    private MeteringRectangle[] mAeRects = new MeteringRectangle[]{};
    private MeteringRectangle[] mAwbRects = new MeteringRectangle[]{};
    CallbackToFutureAdapter.Completer<FocusMeteringResult> mRunningActionCompleter = null;
    CallbackToFutureAdapter.Completer<Void> mRunningCancelCompleter = null;
    //**************************************************************************************//


    /**
     * Constructs a FocusMeteringControl.
     *
     * <p>All tasks scheduled by {@code scheduler} will be immediately executed by {@code executor}.
     *
     * @param cameraControl Camera control to which this FocusMeteringControl belongs.
     * @param scheduler     Scheduler used for scheduling tasks in the future.
     * @param executor      Camera executor used to run all tasks scheduled on {@code scheduler}.
     */
    FocusMeteringControl(@NonNull Camera2CameraControlImpl cameraControl,
            @NonNull ScheduledExecutorService scheduler,
            @NonNull @CameraExecutor Executor executor) {
        mCameraControl = cameraControl;
        mExecutor = executor;
        mScheduler = scheduler;
    }


    /**
     * Set current active state. Set active if it is ready to accept focus/metering operations.
     *
     * <p> In inactive state, startFocusAndMetering does nothing while cancelFocusAndMetering
     * still works to cancel current operation. cancelFocusAndMetering is performed automatically
     * when active state is changed to false.
     */
    @ExecutedBy("mExecutor")
    void setActive(boolean isActive) {
        if (isActive == mIsActive) {
            return;
        }

        mIsActive = isActive;

        if (!mIsActive) {
            cancelFocusAndMeteringWithoutAsyncResult();
        }
    }

    @ExecutedBy("mExecutor")
    void setTemplate(int template) {
        mTemplate = template;
    }

    /**
     * Called by {@link Camera2CameraControlImpl} to append the 3A regions to the shared options. It
     * applies to all repeating requests and single requests.
     */
    @ExecutedBy("mExecutor")
    void addFocusMeteringOptions(@NonNull Camera2ImplConfig.Builder configBuilder) {

        int afMode = mIsInAfAutoMode
                ? CaptureRequest.CONTROL_AF_MODE_AUTO
                : getDefaultAfMode();

        configBuilder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE, mCameraControl.getSupportedAfMode(afMode));

        if (mAfRects.length != 0) {
            configBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_REGIONS, mAfRects);
        }
        if (mAeRects.length != 0) {
            configBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_REGIONS, mAeRects);
        }
        if (mAwbRects.length != 0) {
            configBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_REGIONS, mAwbRects);
        }
    }

    private static boolean isValid(@NonNull final MeteringPoint pt) {
        return pt.getX() >= 0f && pt.getX() <= 1f && pt.getY() >= 0f && pt.getY() <= 1f;
    }

    private static PointF getFovAdjustedPoint(@NonNull MeteringPoint meteringPoint,
            @NonNull Rational cropRegionAspectRatio,
            @NonNull Rational defaultAspectRatio) {
        // Use default aspect ratio unless there is a custom aspect ratio in MeteringPoint.
        Rational fovAspectRatio = defaultAspectRatio;
        if (meteringPoint.getSurfaceAspectRatio() != null) {
            fovAspectRatio = meteringPoint.getSurfaceAspectRatio();
        }

        PointF adjustedPoint = new PointF(meteringPoint.getX(),
                meteringPoint.getY());
        if (!fovAspectRatio.equals(cropRegionAspectRatio)) {

            if (fovAspectRatio.compareTo(cropRegionAspectRatio) > 0) {
                // FOV is more narrow than crop region, top and down side of FOV is cropped.
                float heightOfCropRegion =
                        (float) (fovAspectRatio.doubleValue()
                                / cropRegionAspectRatio.doubleValue());
                float top_padding = (float) ((heightOfCropRegion - 1.0) / 2);
                adjustedPoint.y = (top_padding + adjustedPoint.y) * (1 / heightOfCropRegion);

            } else {
                // FOV is wider than crop region, left and right side of FOV is cropped.
                float widthOfCropRegion =
                        (float) (cropRegionAspectRatio.doubleValue()
                                / fovAspectRatio.doubleValue());
                float left_padding = (float) ((widthOfCropRegion - 1.0) / 2);
                adjustedPoint.x = (left_padding + adjustedPoint.x) * (1f / widthOfCropRegion);
            }
        }

        return adjustedPoint;
    }

    private static MeteringRectangle getMeteringRect(MeteringPoint meteringPoint,
            PointF adjustedPoint, Rect cropRegion) {
        int centerX = (int) (cropRegion.left + adjustedPoint.x * cropRegion.width());
        int centerY = (int) (cropRegion.top + adjustedPoint.y * cropRegion.height());

        int width = (int) (meteringPoint.getSize() * cropRegion.width());
        int height = (int) (meteringPoint.getSize() * cropRegion.height());

        Rect focusRect = new Rect(centerX - width / 2, centerY - height / 2, centerX + width / 2,
                centerY + height / 2);

        focusRect.left = rangeLimit(focusRect.left, cropRegion.right, cropRegion.left);
        focusRect.right = rangeLimit(focusRect.right, cropRegion.right, cropRegion.left);
        focusRect.top = rangeLimit(focusRect.top, cropRegion.bottom, cropRegion.top);
        focusRect.bottom = rangeLimit(focusRect.bottom, cropRegion.bottom, cropRegion.top);

        return new MeteringRectangle(focusRect, MeteringRectangle.METERING_WEIGHT_MAX);
    }

    private static int rangeLimit(int val, int max, int min) {
        return Math.min(Math.max(val, min), max);
    }

    ListenableFuture<FocusMeteringResult> startFocusAndMetering(@NonNull FocusMeteringAction action,
            @Nullable Rational defaultAspectRatio) {
        return CallbackToFutureAdapter.getFuture(completer -> {
            mExecutor.execute(
                    () -> startFocusAndMeteringInternal(completer, action, defaultAspectRatio));
            return "startFocusAndMetering";
        });
    }

    @ExecutedBy("mExecutor")
    void startFocusAndMeteringInternal(@NonNull Completer<FocusMeteringResult> completer,
            @NonNull FocusMeteringAction action,
            @Nullable Rational defaultAspectRatio) {
        if (!mIsActive) {
            completer.setException(
                    new CameraControl.OperationCanceledException("Camera is not active."));
            return;
        }

        if (action.getMeteringPointsAf().isEmpty()
                && action.getMeteringPointsAe().isEmpty()
                && action.getMeteringPointsAwb().isEmpty()) {
            completer.setException(
                    new IllegalArgumentException("No AF/AE/AWB MeteringPoints are added."));
            return;
        }

        int supportedAfCount = Math.min(action.getMeteringPointsAf().size(),
                mCameraControl.getMaxAfRegionCount());
        int supportedAeCount = Math.min(action.getMeteringPointsAe().size(),
                mCameraControl.getMaxAeRegionCount());
        int supportedAwbCount = Math.min(action.getMeteringPointsAwb().size(),
                mCameraControl.getMaxAwbRegionCount());
        int totalSupportedCount = supportedAfCount + supportedAeCount + supportedAwbCount;
        if (totalSupportedCount <= 0) {
            completer.setException(
                    new IllegalArgumentException("None of the specified AF/AE/AWB MeteringPoints "
                            + "is supported on this camera."));
            return;
        }

        List<MeteringPoint> meteringPointListAF = new ArrayList<>();
        List<MeteringPoint> meteringPointListAE = new ArrayList<>();
        List<MeteringPoint> meteringPointListAWB = new ArrayList<>();
        if (supportedAfCount > 0) {
            meteringPointListAF.addAll(action.getMeteringPointsAf().subList(0, supportedAfCount));
        }
        if (supportedAeCount > 0) {
            meteringPointListAE.addAll(action.getMeteringPointsAe().subList(0, supportedAeCount));
        }
        if (supportedAwbCount > 0) {
            meteringPointListAWB.addAll(action.getMeteringPointsAwb().subList(0,
                    supportedAwbCount));
        }

        Rect cropSensorRegion = mCameraControl.getCropSensorRegion();
        Rational cropRegionAspectRatio = new Rational(cropSensorRegion.width(),
                cropSensorRegion.height());

        if (defaultAspectRatio == null) {
            defaultAspectRatio = cropRegionAspectRatio;
        }

        List<MeteringRectangle> meteringRectanglesListAF = new ArrayList<>();
        List<MeteringRectangle> meteringRectanglesListAE = new ArrayList<>();
        List<MeteringRectangle> meteringRectanglesListAWB = new ArrayList<>();

        for (MeteringPoint meteringPoint : meteringPointListAF) {
            if (!isValid(meteringPoint)) {
                continue;
            }
            PointF adjustedPoint = getFovAdjustedPoint(meteringPoint, cropRegionAspectRatio,
                    defaultAspectRatio);
            MeteringRectangle meteringRectangle = getMeteringRect(meteringPoint, adjustedPoint,
                    cropSensorRegion);
            if (meteringRectangle.getWidth() == 0 || meteringRectangle.getHeight() == 0) {
                continue;
            }
            meteringRectanglesListAF.add(meteringRectangle);
        }

        for (MeteringPoint meteringPoint : meteringPointListAE) {
            if (!isValid(meteringPoint)) {
                continue;
            }
            PointF adjustedPoint = getFovAdjustedPoint(meteringPoint, cropRegionAspectRatio,
                    defaultAspectRatio);
            MeteringRectangle meteringRectangle = getMeteringRect(meteringPoint, adjustedPoint,
                    cropSensorRegion);
            if (meteringRectangle.getWidth() == 0 || meteringRectangle.getHeight() == 0) {
                continue;
            }
            meteringRectanglesListAE.add(meteringRectangle);
        }

        for (MeteringPoint meteringPoint : meteringPointListAWB) {
            if (!isValid(meteringPoint)) {
                continue;
            }
            PointF adjustedPoint = getFovAdjustedPoint(meteringPoint, cropRegionAspectRatio,
                    defaultAspectRatio);
            MeteringRectangle meteringRectangle = getMeteringRect(meteringPoint, adjustedPoint,
                    cropSensorRegion);
            if (meteringRectangle.getWidth() == 0 || meteringRectangle.getHeight() == 0) {
                continue;
            }
            meteringRectanglesListAWB.add(meteringRectangle);
        }

        if (meteringRectanglesListAF.isEmpty()
                && meteringRectanglesListAE.isEmpty()
                && meteringRectanglesListAWB.isEmpty()) {
            completer.setException(
                    new IllegalArgumentException("None of the specified AF/AE/AWB MeteringPoints "
                            + "are valid."));
            return;
        }

        failActionFuture("Cancelled by another startFocusAndMetering()");
        failCancelFuture("Cancelled by another startFocusAndMetering()");
        disableAutoCancel();
        mRunningActionCompleter = completer;

        executeMeteringAction(
                meteringRectanglesListAF.toArray(new MeteringRectangle[0]),
                meteringRectanglesListAE.toArray(new MeteringRectangle[0]),
                meteringRectanglesListAWB.toArray(new MeteringRectangle[0]),
                action
        );
    }

    /**
     * Trigger an AF scan.
     *
     * @param completer used to complete the associated {@link ListenableFuture} when the
     *                  operation succeeds or fails. Passing null to simply ignore the result.
     */
    @ExecutedBy("mExecutor")
    void triggerAf(@Nullable Completer<CameraCaptureResult> completer) {
        if (!mIsActive) {
            if (completer != null) {
                completer.setException(
                        new CameraControl.OperationCanceledException("Camera is not active."));
            }
            return;
        }

        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        builder.setTemplateType(mTemplate);
        builder.setUseRepeatingSurface(true);
        Camera2ImplConfig.Builder configBuilder = new Camera2ImplConfig.Builder();
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        builder.addImplementationOptions(configBuilder.build());
        builder.addCameraCaptureCallback(new CameraCaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureResult cameraCaptureResult) {
                if (completer != null) {
                    completer.set(cameraCaptureResult);
                }
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureFailure failure) {
                if (completer != null) {
                    completer.setException(
                            new CameraControlInternal.CameraControlException(failure));
                }
            }

            @Override
            public void onCaptureCancelled() {
                if (completer != null) {
                    completer.setException(
                            new CameraControl.OperationCanceledException("Camera is closed"));
                }
            }
        });

        mCameraControl.submitCaptureRequestsInternal(Collections.singletonList(builder.build()));
    }

    /**
     * Trigger an AE precapture sequence.
     *
     * @param completer used to complete the associated {@link ListenableFuture} when the
     *                  operation succeeds or fails. Passing null to simply ignore the result.
     */
    @ExecutedBy("mExecutor")
    void triggerAePrecapture(@Nullable Completer<CameraCaptureResult> completer) {
        if (!mIsActive) {
            if (completer != null) {
                completer.setException(
                        new CameraControl.OperationCanceledException("Camera is not active."));
            }
            return;
        }

        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        builder.setTemplateType(mTemplate);
        builder.setUseRepeatingSurface(true);
        Camera2ImplConfig.Builder configBuilder = new Camera2ImplConfig.Builder();
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        builder.addImplementationOptions(configBuilder.build());
        builder.addCameraCaptureCallback(new CameraCaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureResult cameraCaptureResult) {
                if (completer != null) {
                    completer.set(cameraCaptureResult);
                }
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureFailure failure) {
                if (completer != null) {
                    completer.setException(
                            new CameraControlInternal.CameraControlException(failure));
                }
            }

            @Override
            public void onCaptureCancelled() {
                if (completer != null) {
                    completer.setException(
                            new CameraControl.OperationCanceledException("Camera is closed"));
                }
            }
        });
        mCameraControl.submitCaptureRequestsInternal(Collections.singletonList(builder.build()));
    }

    @ExecutedBy("mExecutor")
    void cancelAfAeTrigger(final boolean cancelAfTrigger,
            final boolean cancelAePrecaptureTrigger) {
        if (!mIsActive) {
            return;
        }

        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        builder.setUseRepeatingSurface(true);
        builder.setTemplateType(mTemplate);

        Camera2ImplConfig.Builder configBuilder = new Camera2ImplConfig.Builder();
        if (cancelAfTrigger) {
            configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        }
        if (Build.VERSION.SDK_INT >= 23 && cancelAePrecaptureTrigger) {
            configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
        }
        builder.addImplementationOptions(configBuilder.build());
        mCameraControl.submitCaptureRequestsInternal(Collections.singletonList(builder.build()));
    }


    @ExecutedBy("mExecutor")
    private void disableAutoCancel() {
        if (mAutoCancelHandle != null) {
            mAutoCancelHandle.cancel(/*mayInterruptIfRunning=*/true);
            mAutoCancelHandle = null;
        }
    }

    @VisibleForTesting
    @ExecutedBy("mExecutor")
    int getDefaultAfMode() {
        switch (mTemplate) {
            case CameraDevice.TEMPLATE_RECORD:
                return CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
            case CameraDevice.TEMPLATE_PREVIEW:
            default:
                return CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
        }
    }

    private boolean isAfModeSupported() {
        return mCameraControl.getSupportedAfMode(CaptureRequest.CONTROL_AF_MODE_AUTO)
                == CaptureRequest.CONTROL_AF_MODE_AUTO;
    }

    @ExecutedBy("mExecutor")
    private void completeActionFuture(boolean isFocusSuccessful) {
        if (mRunningActionCompleter != null) {
            mRunningActionCompleter.set(FocusMeteringResult.create(isFocusSuccessful));
            mRunningActionCompleter = null;
        }
    }

    @ExecutedBy("mExecutor")
    private void failActionFuture(String message) {
        mCameraControl.removeCaptureResultListener(mSessionListenerForFocus);
        if (mRunningActionCompleter != null) {
            mRunningActionCompleter.setException(new CameraControl.OperationCanceledException(
                    message));
            mRunningActionCompleter = null;
        }
    }

    @ExecutedBy("mExecutor")
    private void failCancelFuture(String message) {
        mCameraControl.removeCaptureResultListener(mSessionListenerForCancel);
        if (mRunningCancelCompleter != null) {
            mRunningCancelCompleter.setException(
                    new CameraControl.OperationCanceledException(message));
            mRunningCancelCompleter = null;
        }
    }

    @ExecutedBy("mExecutor")
    private void completeCancelFuture() {
        if (mRunningCancelCompleter != null) {
            mRunningCancelCompleter.set(null);
            mRunningCancelCompleter = null;
        }
    }

    @ExecutedBy("mExecutor")
    private void executeMeteringAction(
            @NonNull MeteringRectangle[] afRects,
            @NonNull MeteringRectangle[] aeRects,
            @NonNull MeteringRectangle[] awbRects,
            FocusMeteringAction focusMeteringAction) {
        mCameraControl.removeCaptureResultListener(mSessionListenerForFocus);

        disableAutoCancel();

        mAfRects = afRects;
        mAeRects = aeRects;
        mAwbRects = awbRects;

        long sessionUpdateId;
        // Trigger AF scan if any AF points are added.
        if (shouldTriggerAF()) {
            mIsInAfAutoMode = true;
            mIsAutoFocusCompleted = false;
            mIsFocusSuccessful = false;
            sessionUpdateId = mCameraControl.updateSessionConfigSynchronous();
            triggerAf(null);
        } else {
            mIsInAfAutoMode = false;
            mIsAutoFocusCompleted = true; // Don't need to wait for auto-focus
            mIsFocusSuccessful = false;  // False because AF is not triggered.
            sessionUpdateId = mCameraControl.updateSessionConfigSynchronous();
        }

        mCurrentAfState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
        final boolean isAfModeSupported = isAfModeSupported();

        // Will be called on mExecutor since mSessionCallback was created with mExecutor
        mSessionListenerForFocus =
                result -> {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (shouldTriggerAF()) {
                        if (!isAfModeSupported || afState == null) {
                            // set isFocusSuccessful to true when camera does not support AF_AUTO.
                            mIsFocusSuccessful = true;
                            mIsAutoFocusCompleted = true;
                        } else if (mCurrentAfState == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN) {
                            if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                                mIsFocusSuccessful = true;
                                mIsAutoFocusCompleted = true;
                            } else if (afState
                                    == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                                mIsFocusSuccessful = false;
                                mIsAutoFocusCompleted = true;
                            }
                        }
                    }

                    // Check 3A regions
                    if (mIsAutoFocusCompleted) {
                        if (isSessionUpdated(result, sessionUpdateId)) {
                            completeActionFuture(mIsFocusSuccessful);
                            return true; // remove this listener
                        }
                    }

                    if (!mCurrentAfState.equals(afState) && afState != null) {
                        mCurrentAfState = afState;
                    }
                    return false; // continue checking
                };

        mCameraControl.addCaptureResultListener(mSessionListenerForFocus);

        if (focusMeteringAction.isAutoCancelEnabled()) {
            final long timeoutId = ++mFocusTimeoutCounter;
            final Runnable autoCancelRunnable = () -> mExecutor.execute(() -> {
                if (timeoutId == mFocusTimeoutCounter) {
                    cancelFocusAndMeteringWithoutAsyncResult();
                }
            });

            mAutoCancelHandle = mScheduler.schedule(autoCancelRunnable,
                    focusMeteringAction.getAutoCancelDurationInMillis(),
                    TimeUnit.MILLISECONDS);
        }
    }

    @ExecutedBy("mExecutor")
    private boolean shouldTriggerAF() {
        return mAfRects.length > 0;
    }

    ListenableFuture<Void> cancelFocusAndMetering() {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    mExecutor.execute(() -> cancelFocusAndMeteringInternal(completer));
                    return "cancelFocusAndMetering";
                });
    }

    @ExecutedBy("mExecutor")
    void cancelFocusAndMeteringWithoutAsyncResult() {
        cancelFocusAndMeteringInternal(null);
    }

    @ExecutedBy("mExecutor")
    void cancelFocusAndMeteringInternal(
            @Nullable CallbackToFutureAdapter.Completer<Void> completer) {
        failCancelFuture("Cancelled by another cancelFocusAndMetering()");
        failActionFuture("Cancelled by cancelFocusAndMetering()");
        mRunningCancelCompleter = completer;
        disableAutoCancel();

        if (shouldTriggerAF()) {
            cancelAfAeTrigger(true, false);
        }
        mAfRects = new MeteringRectangle[]{};
        mAeRects = new MeteringRectangle[]{};
        mAwbRects = new MeteringRectangle[]{};

        mIsInAfAutoMode = false;
        long sessionUpdateId = mCameraControl.updateSessionConfigSynchronous();

        if (mRunningCancelCompleter != null) {
            int targetAfMode = mCameraControl.getSupportedAfMode(getDefaultAfMode());
            mSessionListenerForCancel =
                    captureResult -> {
                        Integer afMode = captureResult.get(CaptureResult.CONTROL_AF_MODE);
                        if (afMode == targetAfMode
                                && isSessionUpdated(captureResult, sessionUpdateId)) {
                            completeCancelFuture();
                            return true; // remove this listener
                        }
                        return false;
                    };

            mCameraControl.addCaptureResultListener(mSessionListenerForCancel);
        }
    }

    private static boolean isSessionUpdated(@NonNull TotalCaptureResult captureResult,
            long sessionUpdateId) {
        if (captureResult.getRequest() == null) {
            return false;
        }
        Object tag = captureResult.getRequest().getTag();
        if (tag instanceof TagBundle) {
            Long tagLong =
                    (Long) ((TagBundle) tag).getTag(Camera2CameraControlImpl.TAG_SESSION_UPDATE_ID);
            if (tagLong == null) {
                return false;
            }
            long sessionUpdateIdInCaptureResult = tagLong.longValue();
            // Check if session update is already done.
            if (sessionUpdateIdInCaptureResult >= sessionUpdateId) {
                return true;
            }
        }

        return false;
    }
}
