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

package androidx.camera.camera2.impl;

import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;
import android.util.Rational;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.camera2.impl.annotation.CameraExecutor;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.MeteringPoint;
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
 * <p>It is intended to be used within {@link Camera2CameraControl} to implement the
 * functionality of {@link Camera2CameraControl#startFocusAndMetering(FocusMeteringAction)} and
 * {@link Camera2CameraControl#cancelFocusAndMetering()}. This class depends on
 * {@link Camera2CameraControl} to provide some low-level methods such as updateSessionConfig,
 * triggerAfInternal and cancelAfAeTriggerInternal to achieve the focus and metering functions.
 *
 * <p>To wait for the auto-focus lock, it calls
 * {@link Camera2CameraControl#addCaptureResultListener(Camera2CameraControl.CaptureResultListener)}
 * to monitor the capture result. It also requires {@link ScheduledExecutorService} to schedule the
 * auto-cancel event and {@link Executor} to ensure all the methods within this class are called
 * in the same thread as the Camera2CameraControl.
 *
 * <p>The {@link Camera2CameraControl} calls {@link FocusMeteringControl#addFocusMeteringOptions} to
 * construct the 3A regions and append them to all repeating requests and single requests.
 */
class FocusMeteringControl {
    private static final String TAG = "FocusMeteringControl";
    private final Camera2CameraControl mCameraControl;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @CameraExecutor
    final Executor mExecutor;
    private final ScheduledExecutorService mScheduler;

    private volatile boolean mIsActive = false;

    //******************** Should only be accessed by executor (WorkThread) ****************//
    private FocusMeteringAction mCurrentFocusMeteringAction;
    private boolean mIsInAfAutoMode = false;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    Integer mCurrentAfState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
    private ScheduledFuture<?> mAutoCancelHandle;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    long mFocusTimeoutCounter = 0;
    private Camera2CameraControl.CaptureResultListener mSessionListenerForFocus = null;
    private MeteringRectangle[] mAfRects = new MeteringRectangle[]{};
    private MeteringRectangle[] mAeRects = new MeteringRectangle[]{};
    private MeteringRectangle[] mAwbRects = new MeteringRectangle[]{};
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
    FocusMeteringControl(@NonNull Camera2CameraControl cameraControl,
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
    void setActive(boolean isActive) {
        if (isActive == mIsActive) {
            return;
        }

        mIsActive = isActive;

        if (!mIsActive) {
            mExecutor.execute(() -> cancelFocusAndMetering());
        }
    }

    /**
     * Called by {@link Camera2CameraControl} to append the 3A regions to the shared options. It
     * applies to all repeating requests and single requests.
     */
    @WorkerThread
    void addFocusMeteringOptions(@NonNull Camera2ImplConfig.Builder configBuilder) {
        int afMode = mIsInAfAutoMode
                ? CaptureRequest.CONTROL_AF_MODE_AUTO
                : CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;

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

    @WorkerThread
    private PointF getFovAdjustedPoint(@NonNull MeteringPoint meteringPoint,
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

    @WorkerThread
    private MeteringRectangle getMeteringRect(MeteringPoint meteringPoint, PointF adjustedPoint,
            Rect cropRegion) {
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

    @WorkerThread
    private int rangeLimit(int val, int max, int min) {
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

    @WorkerThread
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

        if (mCurrentFocusMeteringAction != null) {
            cancelFocusAndMetering();
        }
        mCurrentFocusMeteringAction = action;

        Rect cropSensorRegion = mCameraControl.getCropSensorRegion();
        Rational cropRegionAspectRatio = new Rational(cropSensorRegion.width(),
                cropSensorRegion.height());

        if (defaultAspectRatio == null) {
            defaultAspectRatio = cropRegionAspectRatio;
        }

        List<MeteringRectangle> meteringRectanglesListAF = new ArrayList<>();
        List<MeteringRectangle> meteringRectanglesListAE = new ArrayList<>();
        List<MeteringRectangle> meteringRectanglesListAWB = new ArrayList<>();

        for (MeteringPoint meteringPoint : action.getMeteringPointsAf()) {
            PointF adjustedPoint = getFovAdjustedPoint(meteringPoint, cropRegionAspectRatio,
                    defaultAspectRatio);
            MeteringRectangle meteringRectangle = getMeteringRect(meteringPoint, adjustedPoint,
                    cropSensorRegion);
            meteringRectanglesListAF.add(meteringRectangle);
        }

        for (MeteringPoint meteringPoint : action.getMeteringPointsAe()) {
            PointF adjustedPoint = getFovAdjustedPoint(meteringPoint, cropRegionAspectRatio,
                    defaultAspectRatio);
            MeteringRectangle meteringRectangle = getMeteringRect(meteringPoint, adjustedPoint,
                    cropSensorRegion);
            meteringRectanglesListAE.add(meteringRectangle);
        }

        for (MeteringPoint meteringPoint : action.getMeteringPointsAwb()) {
            PointF adjustedPoint = getFovAdjustedPoint(meteringPoint, cropRegionAspectRatio,
                    defaultAspectRatio);
            MeteringRectangle meteringRectangle = getMeteringRect(meteringPoint, adjustedPoint,
                    cropSensorRegion);
            meteringRectanglesListAWB.add(meteringRectangle);
        }

        executeMeteringAction(completer,
                meteringRectanglesListAF.toArray(new MeteringRectangle[0]),
                meteringRectanglesListAE.toArray(new MeteringRectangle[0]),
                meteringRectanglesListAWB.toArray(new MeteringRectangle[0]),
                action
        );
    }

    @WorkerThread
    private int getDefaultTemplate() {
        return CameraDevice.TEMPLATE_PREVIEW;
    }

    @WorkerThread
    void triggerAf() {
        if (!mIsActive) {
            return;
        }

        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        builder.setTemplateType(getDefaultTemplate());
        builder.setUseRepeatingSurface(true);
        Camera2ImplConfig.Builder configBuilder = new Camera2ImplConfig.Builder();
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        builder.addImplementationOptions(configBuilder.build());
        mCameraControl.submitCaptureRequestsInternal(Collections.singletonList(builder.build()));
    }

    @WorkerThread
    void triggerAePrecapture() {
        if (!mIsActive) {
            return;
        }

        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        builder.setTemplateType(getDefaultTemplate());
        builder.setUseRepeatingSurface(true);
        Camera2ImplConfig.Builder configBuilder = new Camera2ImplConfig.Builder();
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        builder.addImplementationOptions(configBuilder.build());
        mCameraControl.submitCaptureRequestsInternal(Collections.singletonList(builder.build()));
    }

    @WorkerThread
    void cancelAfAeTrigger(final boolean cancelAfTrigger,
            final boolean cancelAePrecaptureTrigger) {
        if (!mIsActive) {
            return;
        }

        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        builder.setUseRepeatingSurface(true);
        builder.setTemplateType(getDefaultTemplate());

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


    @WorkerThread
    private void disableAutoCancel() {
        if (mAutoCancelHandle != null) {
            mAutoCancelHandle.cancel(/*mayInterruptIfRunning=*/true);
            mAutoCancelHandle = null;
        }
    }

    @WorkerThread
    private void executeMeteringAction(
            @NonNull Completer<FocusMeteringResult> completer,
            @Nullable MeteringRectangle[] afRects,
            @Nullable MeteringRectangle[] aeRects,
            @Nullable MeteringRectangle[] awbRects,
            FocusMeteringAction focusMeteringAction) {
        mCameraControl.removeCaptureResultListener(mSessionListenerForFocus);

        disableAutoCancel();

        if (afRects == null) {
            mAfRects = new MeteringRectangle[]{};
        } else {
            mAfRects = afRects;
        }

        if (aeRects == null) {
            mAeRects = new MeteringRectangle[]{};
        } else {
            mAeRects = aeRects;
        }

        if (awbRects == null) {
            mAwbRects = new MeteringRectangle[]{};
        } else {
            mAwbRects = awbRects;
        }

        // Trigger AF scan if any AF points are added.
        if (shouldTriggerAF()) {
            mCurrentAfState = CaptureResult.CONTROL_AF_STATE_INACTIVE;
            // Will be called on mExecutor since mSessionCallback was created with mExecutor
            mSessionListenerForFocus =
                    result -> {
                        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                        if (afState == null) {
                            return false;
                        }

                        if (mCurrentAfState == CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN) {
                            if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                                completer.set(FocusMeteringResult.create(true));
                                return true; // finished
                            } else if (afState
                                    == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                                completer.set(FocusMeteringResult.create(false));
                                return true; // finished
                            }
                        }
                        if (!mCurrentAfState.equals(afState)) {
                            mCurrentAfState = afState;
                        }
                        return false; // continue checking
                    };

            mCameraControl.addCaptureResultListener(mSessionListenerForFocus);

            mIsInAfAutoMode = true;
            mCameraControl.updateSessionConfig();
            triggerAf();
        } else {
            completer.set(FocusMeteringResult.create(false));
            mCameraControl.updateSessionConfig();
        }

        if (focusMeteringAction.isAutoCancelEnabled()) {
            final long timeoutId = ++mFocusTimeoutCounter;
            final Runnable autoCancelRunnable = () -> mExecutor.execute(() -> {
                if (timeoutId == mFocusTimeoutCounter) {
                    cancelFocusAndMetering();
                }
            });

            mAutoCancelHandle = mScheduler.schedule(autoCancelRunnable,
                    focusMeteringAction.getAutoCancelDurationInMillis(),
                    TimeUnit.MILLISECONDS);
        }
    }

    @WorkerThread
    private boolean shouldTriggerAF() {
        return mAfRects.length > 0;
    }

    @WorkerThread
    void cancelFocusAndMetering() {
        mCameraControl.removeCaptureResultListener(mSessionListenerForFocus);

        disableAutoCancel();

        if (shouldTriggerAF()) {
            cancelAfAeTrigger(true, false);
        }
        mAfRects = new MeteringRectangle[]{};
        mAeRects = new MeteringRectangle[]{};
        mAwbRects = new MeteringRectangle[]{};

        mIsInAfAutoMode = false;
        mCameraControl.updateSessionConfig();
        mCurrentFocusMeteringAction = null;
    }
}
