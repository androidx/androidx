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

package androidx.camera.camera2.internal;

import static androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY;
import static androidx.camera.core.ImageCapture.CaptureMode;
import static androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED;
import static androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED;
import static androidx.camera.core.ImageCapture.FLASH_MODE_AUTO;
import static androidx.camera.core.ImageCapture.FLASH_MODE_OFF;
import static androidx.camera.core.ImageCapture.FLASH_MODE_ON;
import static androidx.camera.core.ImageCapture.FLASH_MODE_SCREEN;
import static androidx.camera.core.ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH;
import static androidx.camera.core.ImageCapture.FlashMode;
import static androidx.camera.core.ImageCapture.FlashType;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.workaround.FlashAvailabilityChecker;
import androidx.camera.camera2.internal.compat.workaround.OverrideAeModeForStillCapture;
import androidx.camera.camera2.internal.compat.workaround.UseFlashModeTorchFor3aUpdate;
import androidx.camera.camera2.internal.compat.workaround.UseTorchAsFlash;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.core.imagecapture.CameraCapturePipeline;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraCaptureResults;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.ConvergenceUtils;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation detail of the submitStillCaptures method.
 */
class Camera2CapturePipeline {

    private static final String TAG = "Camera2CapturePipeline";

    @NonNull
    private final Camera2CameraControlImpl mCameraControl;

    @NonNull
    private final UseTorchAsFlash mUseTorchAsFlash;

    private final boolean mHasFlashUnit;

    @NonNull
    private final Quirks mCameraQuirk;

    @NonNull
    @CameraExecutor
    private final Executor mExecutor;

    @NonNull
    private final ScheduledExecutorService mScheduler;

    private final boolean mIsLegacyDevice;

    private int mTemplate = CameraDevice.TEMPLATE_PREVIEW;

    /**
     * Constructs a Camera2CapturePipeline for single capture use.
     */
    Camera2CapturePipeline(@NonNull Camera2CameraControlImpl cameraControl,
            @NonNull CameraCharacteristicsCompat cameraCharacteristics,
            @NonNull Quirks cameraQuirks,
            @CameraExecutor @NonNull Executor executor,
            @NonNull ScheduledExecutorService scheduler) {
        mCameraControl = cameraControl;
        Integer level =
                cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        mIsLegacyDevice = level != null
                && level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        mExecutor = executor;
        mScheduler = scheduler;
        mCameraQuirk = cameraQuirks;
        mUseTorchAsFlash = new UseTorchAsFlash(cameraQuirks);
        mHasFlashUnit = FlashAvailabilityChecker.isFlashAvailable(cameraCharacteristics::get);
    }

    @ExecutedBy("mExecutor")
    public void setTemplate(int template) {
        mTemplate = template;
    }

    /**
     * Submit a list of capture configs to the camera, it returns a ListenableFuture
     * which will be completed after all the captures were done.
     *
     * @return the future will be completed after all the captures are completed, It would
     * fail with a {@link androidx.camera.core.ImageCapture#ERROR_CAMERA_CLOSED} when the
     * capture was canceled, or {@link androidx.camera.core.ImageCapture#ERROR_CAPTURE_FAILED}
     * when the capture was failed.
     */
    @ExecutedBy("mExecutor")
    @NonNull
    public ListenableFuture<List<Void>> submitStillCaptures(
            @NonNull List<CaptureConfig> captureConfigs, @CaptureMode int captureMode,
            @FlashMode int flashMode, @FlashType int flashType) {

        Pipeline pipeline = createPipeline(captureMode, flashMode, flashType);
        return Futures.nonCancellationPropagating(
                pipeline.executeCapture(captureConfigs, flashMode));
    }

    /**
     * Creates a {@link Pipeline} for the current capture request based on the parameters.
     */
    @VisibleForTesting
    Pipeline createPipeline(@CaptureMode int captureMode, @FlashMode int flashMode,
            @FlashType int flashType) {
        OverrideAeModeForStillCapture aeQuirk = new OverrideAeModeForStillCapture(mCameraQuirk);
        Pipeline pipeline = new Pipeline(mTemplate, mExecutor, mScheduler, mCameraControl,
                mIsLegacyDevice, aeQuirk);

        if (captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY) {
            pipeline.addTask(new AfTask(mCameraControl));
        }

        if (flashMode == FLASH_MODE_SCREEN) {
            pipeline.addTask(new ScreenFlashTask(mCameraControl, mExecutor, mScheduler,
                    new UseFlashModeTorchFor3aUpdate(mCameraQuirk)));
        } else {
            if (mHasFlashUnit) {
                if (isTorchAsFlash(flashType)) {
                    // TODO: b/339846763 - Disable AE precap only for the quirks where AE precapture
                    //  is problematic, instead of all TorchAsFlash quirks.
                    boolean triggerAePrecapture = !mUseTorchAsFlash.shouldUseTorchAsFlash()
                            && !mCameraControl.isInVideoUsage();
                    pipeline.addTask(new TorchTask(mCameraControl, flashMode, mExecutor, mScheduler,
                            triggerAePrecapture));
                } else {
                    pipeline.addTask(new AePreCaptureTask(mCameraControl, flashMode, aeQuirk));
                }
            }
            // If there is no flash unit, skip the flash related task instead of failing the
            // pipeline.
        }

        Logger.d(TAG, "createPipeline: captureMode = " + captureMode + ", flashMode = " + flashMode
                + ", flashType = " + flashType + ", pipeline tasks = " + pipeline.mTasks);

        return pipeline;
    }

    @NonNull
    CameraCapturePipeline getCameraCapturePipeline(@CaptureMode int captureMode,
            @FlashMode int flashMode, @FlashType int flashType) {
        return new CameraCapturePipelineImpl(createPipeline(captureMode, flashMode, flashType),
                mExecutor, flashMode);
    }

    /**
     * The internal implementation for {@link CameraCapturePipeline}.
     */
    static class CameraCapturePipelineImpl implements CameraCapturePipeline {
        private final Executor mExecutor;
        private final Pipeline mPipelineDelegate;

        @FlashMode private int mFlashMode;

        CameraCapturePipelineImpl(Pipeline pipeline, Executor executor, @FlashMode int flashMode) {
            mPipelineDelegate = pipeline;
            mExecutor = executor;
            mFlashMode = flashMode;
        }

        @NonNull
        @Override
        public ListenableFuture<Void> invokePreCapture() {
            Logger.d(TAG, "invokePreCapture");
            return FutureChain.from(mPipelineDelegate.executePreCapture(mFlashMode)).transform(
                    result -> null, mExecutor);
        }

        @NonNull
        @Override
        public ListenableFuture<Void> invokePostCapture() {
            return CallbackToFutureAdapter.getFuture(completer -> {
                mPipelineDelegate.executePostCapture();
                completer.set(null);
                return "invokePostCaptureFuture";
            });
        }
    }

    /**
     * The pipeline for single capturing.
     */
    @VisibleForTesting
    static class Pipeline {
        private static final long CHECK_3A_TIMEOUT_IN_NS = TimeUnit.SECONDS.toNanos(1);
        private static final long CHECK_3A_WITH_FLASH_TIMEOUT_IN_NS = TimeUnit.SECONDS.toNanos(5);

        private final int mTemplate;
        private final Executor mExecutor;
        private final ScheduledExecutorService mScheduler;
        private final Camera2CameraControlImpl mCameraControl;
        private final OverrideAeModeForStillCapture mOverrideAeModeForStillCapture;
        private final boolean mIsLegacyDevice;
        private long mTimeout3A = CHECK_3A_TIMEOUT_IN_NS;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        final List<PipelineTask> mTasks = new ArrayList<>();

        private final PipelineTask mPipelineSubTask = new PipelineTask() {

            @NonNull
            @Override
            public ListenableFuture<Boolean> preCapture(
                    @Nullable TotalCaptureResult captureResult) {
                ArrayList<ListenableFuture<Boolean>> futures = new ArrayList<>();
                for (PipelineTask task : mTasks) {
                    futures.add(task.preCapture(captureResult));
                }
                return Futures.transform(Futures.allAsList(futures),
                        results -> results.contains(true), CameraXExecutors.directExecutor());
            }

            @Override
            public boolean isCaptureResultNeeded() {
                for (PipelineTask task : mTasks) {
                    if (task.isCaptureResultNeeded()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void postCapture() {
                for (PipelineTask task : mTasks) {
                    task.postCapture();
                }
            }
        };

        Pipeline(int template, @NonNull Executor executor,
                @NonNull ScheduledExecutorService scheduler,
                @NonNull Camera2CameraControlImpl cameraControl, boolean isLegacyDevice,
                @NonNull OverrideAeModeForStillCapture overrideAeModeForStillCapture) {
            mTemplate = template;
            mExecutor = executor;
            mScheduler = scheduler;
            mCameraControl = cameraControl;
            mIsLegacyDevice = isLegacyDevice;
            mOverrideAeModeForStillCapture = overrideAeModeForStillCapture;
        }

        /**
         * Add the AE/AF/Torch tasks if required.
         *
         * @param task implements the PipelineTask interface
         */
        void addTask(@NonNull PipelineTask task) {
            mTasks.add(task);
        }

        /**
         * Set the timeout for the 3A converge.
         *
         * @param timeout3A in nano seconds
         */
        @SuppressWarnings("SameParameterValue")
        private void setTimeout3A(long timeout3A) {
            mTimeout3A = timeout3A;
        }

        @SuppressWarnings("FutureReturnValueIgnored")
        @ExecutedBy("mExecutor")
        @NonNull
        ListenableFuture<List<Void>> executeCapture(@NonNull List<CaptureConfig> captureConfigs,
                @FlashMode int flashMode) {
            ListenableFuture<List<Void>> future = FutureChain.from(
                    executePreCapture(flashMode)
            ).transformAsync(v -> submitConfigsInternal(captureConfigs, flashMode), mExecutor);

            /* Always call postCapture(), it will unlock3A if it was locked in preCapture.*/
            future.addListener(this::executePostCapture, mExecutor);

            return future;
        }

        @NonNull
        public ListenableFuture<TotalCaptureResult> executePreCapture(int flashMode) {
            ListenableFuture<TotalCaptureResult> preCapture = Futures.immediateFuture(null);

            if (!mTasks.isEmpty()) {
                ListenableFuture<TotalCaptureResult> getResult =
                        mPipelineSubTask.isCaptureResultNeeded() ? waitForResult(mCameraControl,
                                null) : Futures.immediateFuture(null);

                preCapture = FutureChain.from(getResult).transformAsync(captureResult -> {
                    if (isFlashRequired(flashMode, captureResult)) {
                        setTimeout3A(CHECK_3A_WITH_FLASH_TIMEOUT_IN_NS);
                    }
                    return mPipelineSubTask.preCapture(captureResult);
                }, mExecutor).transformAsync(is3aConvergeRequired -> {
                    if (Boolean.TRUE.equals(is3aConvergeRequired)) {
                        return waitForResult(mTimeout3A, mScheduler, mCameraControl,
                                (result) -> is3AConverged(result, false));
                    }
                    return Futures.immediateFuture(null);
                }, mExecutor);
            }

            return preCapture;
        }

        public void executePostCapture() {
            mPipelineSubTask.postCapture();
        }

        @ExecutedBy("mExecutor")
        @NonNull
        ListenableFuture<List<Void>> submitConfigsInternal(
                @NonNull List<CaptureConfig> captureConfigs, @FlashMode int flashMode) {
            List<ListenableFuture<Void>> futureList = new ArrayList<>();
            List<CaptureConfig> configsToSubmit = new ArrayList<>();
            for (CaptureConfig captureConfig : captureConfigs) {
                CaptureConfig.Builder configBuilder = CaptureConfig.Builder.from(captureConfig);

                // Dequeue image from buffer and enqueue into image writer for reprocessing. If
                // succeeded, retrieve capture result and set into capture config.
                CameraCaptureResult cameraCaptureResult = null;
                if (captureConfig.getTemplateType() == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
                        && !mCameraControl.getZslControl().isZslDisabledByFlashMode()
                        && !mCameraControl.getZslControl().isZslDisabledByUserCaseConfig()) {
                    ImageProxy imageProxy =
                            mCameraControl.getZslControl().dequeueImageFromBuffer();
                    boolean isSuccess = imageProxy != null
                            && mCameraControl.getZslControl().enqueueImageToImageWriter(
                                        imageProxy);
                    if (isSuccess) {
                        cameraCaptureResult =
                                CameraCaptureResults.retrieveCameraCaptureResult(
                                        imageProxy.getImageInfo());
                    }
                }

                if (cameraCaptureResult != null) {
                    configBuilder.setCameraCaptureResult(cameraCaptureResult);
                } else {
                    // Apply still capture template type for regular still capture case
                    applyStillCaptureTemplate(configBuilder, captureConfig);
                }

                if (mOverrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(flashMode)) {
                    applyAeModeQuirk(configBuilder);
                }

                futureList.add(CallbackToFutureAdapter.getFuture(completer -> {
                    configBuilder.addCameraCaptureCallback(new CameraCaptureCallback() {
                        @Override
                        public void onCaptureCompleted(int captureConfigId,
                                @NonNull CameraCaptureResult result) {
                            completer.set(null);
                        }

                        @Override
                        public void onCaptureFailed(int captureConfigId,
                                @NonNull CameraCaptureFailure failure) {
                            String msg =
                                    "Capture request failed with reason " + failure.getReason();
                            completer.setException(
                                    new ImageCaptureException(ERROR_CAPTURE_FAILED, msg, null));
                        }

                        @Override
                        public void onCaptureCancelled(int captureConfigId) {
                            String msg = "Capture request is cancelled because camera is closed";
                            completer.setException(
                                    new ImageCaptureException(ERROR_CAMERA_CLOSED, msg, null));
                        }
                    });
                    return "submitStillCapture";
                }));
                configsToSubmit.add(configBuilder.build());
            }
            mCameraControl.submitCaptureRequestsInternal(configsToSubmit);

            return Futures.allAsList(futureList);
        }

        @ExecutedBy("mExecutor")
        private void applyStillCaptureTemplate(@NonNull CaptureConfig.Builder configBuilder,
                @NonNull CaptureConfig captureConfig) {
            int templateToModify = CaptureConfig.TEMPLATE_TYPE_NONE;
            if (mTemplate == CameraDevice.TEMPLATE_RECORD && !mIsLegacyDevice) {
                // Always override template by TEMPLATE_VIDEO_SNAPSHOT when
                // repeating template is TEMPLATE_RECORD. Note:
                // TEMPLATE_VIDEO_SNAPSHOT is not supported on legacy device.
                templateToModify = CameraDevice.TEMPLATE_VIDEO_SNAPSHOT;
            } else if (captureConfig.getTemplateType() == CaptureConfig.TEMPLATE_TYPE_NONE
                    || captureConfig.getTemplateType() == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG) {
                templateToModify = CameraDevice.TEMPLATE_STILL_CAPTURE;
            }

            if (templateToModify != CaptureConfig.TEMPLATE_TYPE_NONE) {
                configBuilder.setTemplateType(templateToModify);
            }
        }

        @ExecutedBy("mExecutor")
        @OptIn(markerClass = ExperimentalCamera2Interop.class)
        private void applyAeModeQuirk(@NonNull CaptureConfig.Builder configBuilder) {
            Camera2ImplConfig.Builder impBuilder = new Camera2ImplConfig.Builder();
            impBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            configBuilder.addImplementationOptions(impBuilder.build());
        }
    }

    /**
     * Waits, with a timeout, for a camera capture result satisfying some criteria defined with the
     * {@code checker} parameter.
     *
     * @param timeoutNanos             The timeout for waiting in nanoseconds.
     * @param scheduledExecutorService The executor service to enforce the timeout.
     * @param cameraControl            The {@link Camera2CameraControlImpl} instance used to
     *                                 listen for capture results.
     * @param checker                  Defines the criteria of camera capture result for which
     *                                 the returned future will be waiting.
     * @return A {@link ListenableFuture} providing the first capture result that satisfies the
     * {@code checker} parameter.
     */
    @ExecutedBy("mExecutor")
    @NonNull
    static ListenableFuture<TotalCaptureResult> waitForResult(long timeoutNanos,
            @NonNull ScheduledExecutorService scheduledExecutorService,
            @NonNull Camera2CameraControlImpl cameraControl,
            @Nullable ResultListener.Checker checker) {
        return Futures.makeTimeoutFuture(TimeUnit.NANOSECONDS.toMillis(timeoutNanos),
                scheduledExecutorService, null, true, waitForResult(cameraControl, checker));
    }

    /**
     * Waits indefinitely for a camera capture result satisfying some criteria defined with the
     * {@code checker} parameter.
     *
     * @param cameraControl The {@link Camera2CameraControlImpl} instance used to listen for
     *                      capture results.
     * @param checker       Defines the criteria of camera capture result for which the returned
     *                      future will be waiting.
     * @return A {@link ListenableFuture} providing the first capture result that satisfies the
     * {@code checker} parameter.
     */
    @ExecutedBy("mExecutor")
    @NonNull
    static ListenableFuture<TotalCaptureResult> waitForResult(
            @NonNull Camera2CameraControlImpl cameraControl,
            @Nullable ResultListener.Checker checker) {
        ResultListener resultListener = new ResultListener(checker);
        cameraControl.addCaptureResultListener(resultListener);

        ListenableFuture<TotalCaptureResult> future = resultListener.getFuture();
        future.addListener(() -> cameraControl.removeCaptureResultListener(resultListener),
                cameraControl.mExecutor);

        return  future;
    }

    static boolean is3AConverged(@Nullable TotalCaptureResult totalCaptureResult,
            boolean isTorchAsFlash) {
        if (totalCaptureResult == null) {
            return false;
        }

        Camera2CameraCaptureResult captureResult = new Camera2CameraCaptureResult(
                totalCaptureResult);
        return ConvergenceUtils.is3AConverged(captureResult, isTorchAsFlash);
    }

    interface PipelineTask {
        /**
         * @return A {@link ListenableFuture} that will be fulfilled with a Boolean result, the
         * result true if it needs to wait for 3A converge after the task is executed, otherwise
         * false.
         */
        @ExecutedBy("mExecutor")
        @NonNull
        ListenableFuture<Boolean> preCapture(@Nullable TotalCaptureResult captureResult);

        /**
         * @return true if the preCapture method requires a CaptureResult. When it return false,
         * that means the {@link #preCapture(TotalCaptureResult)} ()} can accept a null input, we
         * don't need to capture a CaptureResult for this task.
         */
        @ExecutedBy("mExecutor")
        boolean isCaptureResultNeeded();

        @ExecutedBy("mExecutor")
        void postCapture();
    }

    /**
     * Task to triggerAF preCapture if it is required
     */
    static class AfTask implements PipelineTask {

        private final Camera2CameraControlImpl mCameraControl;
        private boolean mIsExecuted = false;

        AfTask(@NonNull Camera2CameraControlImpl cameraControl) {
            mCameraControl = cameraControl;
        }

        @ExecutedBy("mExecutor")
        @NonNull
        @Override
        public ListenableFuture<Boolean> preCapture(@Nullable TotalCaptureResult captureResult) {
            // Always return true for this task since we always need to wait for the focused
            // signal after the task is executed.
            ListenableFuture<Boolean> ret = Futures.immediateFuture(true);

            if (captureResult == null) {
                return ret;
            }

            Integer afMode = captureResult.get(CaptureResult.CONTROL_AF_MODE);
            if (afMode == null) {
                return ret;
            }
            switch (afMode) {
                case CaptureResult.CONTROL_AF_MODE_AUTO:
                case CaptureResult.CONTROL_AF_MODE_MACRO:
                    Logger.d(TAG, "TriggerAf? AF mode auto");
                    Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState != null && afState == CaptureResult.CONTROL_AF_STATE_INACTIVE) {
                        Logger.d(TAG, "Trigger AF");

                        mIsExecuted = true;
                        mCameraControl.getFocusMeteringControl().triggerAf(null, false);
                        return ret;
                    }
                    break;
                default:
                    // fall out
            }

            return ret;
        }

        @ExecutedBy("mExecutor")
        @Override
        public boolean isCaptureResultNeeded() {
            return true;
        }

        @ExecutedBy("mExecutor")
        @Override
        public void postCapture() {
            if (mIsExecuted) {
                Logger.d(TAG, "cancel TriggerAF");
                mCameraControl.getFocusMeteringControl().cancelAfAeTrigger(true, false);
            }
        }
    }

    /**
     * Task to open the Torch if flash is required.
     */
    static class TorchTask implements PipelineTask {
        private static final long CHECK_3A_WITH_TORCH_TIMEOUT_IN_NS = TimeUnit.SECONDS.toNanos(2);

        private final Camera2CameraControlImpl mCameraControl;
        private final @FlashMode int mFlashMode;
        private boolean mIsExecuted = false;
        @CameraExecutor
        private final Executor mExecutor;
        private final ScheduledExecutorService mScheduler;
        private final boolean mTriggerAePrecapture;

        TorchTask(@NonNull Camera2CameraControlImpl cameraControl, @FlashMode int flashMode,
                @NonNull Executor executor, ScheduledExecutorService scheduler,
                boolean triggerAePrecapture) {
            mCameraControl = cameraControl;
            mFlashMode = flashMode;
            mExecutor = executor;
            mScheduler = scheduler;
            mTriggerAePrecapture = triggerAePrecapture;
        }

        @ExecutedBy("mExecutor")
        @NonNull
        @Override
        public ListenableFuture<Boolean> preCapture(@Nullable TotalCaptureResult captureResult) {
            boolean isFlashRequired = isFlashRequired(mFlashMode, captureResult);
            Logger.d(TAG, "TorchTask#preCapture: isFlashRequired = " + isFlashRequired);

            if (isFlashRequired(mFlashMode, captureResult)) {
                if (mCameraControl.isTorchOn()) {
                    Logger.d(TAG, "Torch already on, not turn on");
                } else {
                    Logger.d(TAG, "Turn on torch");
                    mIsExecuted = true;

                    ListenableFuture<Void> future = CallbackToFutureAdapter.getFuture(completer -> {
                        mCameraControl.getTorchControl().enableTorchInternal(completer, true);
                        return "TorchOn";
                    });
                    return FutureChain.from(future).transformAsync(
                            input -> {
                                if (mTriggerAePrecapture) {
                                    return mCameraControl.getFocusMeteringControl()
                                            .triggerAePrecapture();
                                }
                                return Futures.immediateFuture(null);
                            },
                            mExecutor
                    ).transformAsync(
                            input -> waitForResult(CHECK_3A_WITH_TORCH_TIMEOUT_IN_NS, mScheduler,
                                    mCameraControl, (result) -> is3AConverged(result, true)),
                            mExecutor).transform(input -> false, CameraXExecutors.directExecutor());
                }
            }

            return Futures.immediateFuture(false);
        }

        @ExecutedBy("mExecutor")
        @Override
        public boolean isCaptureResultNeeded() {
            return mFlashMode == FLASH_MODE_AUTO;
        }

        @ExecutedBy("mExecutor")
        @Override
        public void postCapture() {
            if (mIsExecuted) {
                mCameraControl.getTorchControl().enableTorchInternal(null, false);
                Logger.d(TAG, "Turning off torch");
                if (mTriggerAePrecapture) {
                    mCameraControl.getFocusMeteringControl().cancelAfAeTrigger(false, true);
                }
            }
        }
    }

    /**
     * Task to trigger AePreCapture if flash is required.
     */
    static class AePreCaptureTask implements PipelineTask {

        private final Camera2CameraControlImpl mCameraControl;
        private final OverrideAeModeForStillCapture mOverrideAeModeForStillCapture;
        private final @FlashMode int mFlashMode;
        private boolean mIsExecuted = false;

        AePreCaptureTask(@NonNull Camera2CameraControlImpl cameraControl, @FlashMode int flashMode,
                @NonNull OverrideAeModeForStillCapture overrideAeModeForStillCapture) {
            mCameraControl = cameraControl;
            mFlashMode = flashMode;
            mOverrideAeModeForStillCapture = overrideAeModeForStillCapture;
        }

        @ExecutedBy("mExecutor")
        @NonNull
        @Override
        public ListenableFuture<Boolean> preCapture(@Nullable TotalCaptureResult captureResult) {
            if (isFlashRequired(mFlashMode, captureResult)) {
                Logger.d(TAG, "Trigger AE");
                mIsExecuted = true;

                ListenableFuture<Void> future = CallbackToFutureAdapter.getFuture(completer -> {
                    mCameraControl.getFocusMeteringControl().triggerAePrecapture(completer);
                    mOverrideAeModeForStillCapture.onAePrecaptureStarted();
                    return "AePreCapture";
                });
                return FutureChain.from(future).transform(input -> true,
                        CameraXExecutors.directExecutor());
            }

            return Futures.immediateFuture(false);
        }

        @ExecutedBy("mExecutor")
        @Override
        public boolean isCaptureResultNeeded() {
            return mFlashMode == FLASH_MODE_AUTO;
        }

        @ExecutedBy("mExecutor")
        @Override
        public void postCapture() {
            if (mIsExecuted) {
                Logger.d(TAG, "cancel TriggerAePreCapture");
                mCameraControl.getFocusMeteringControl().cancelAfAeTrigger(false, true);
                mOverrideAeModeForStillCapture.onAePrecaptureFinished();
            }
        }
    }

    /**
     * Task to trigger ScreenFlashCallback and AePreCapture if screen flash is enabled.
     */
    static class ScreenFlashTask implements PipelineTask {
        private static final long CHECK_3A_WITH_SCREEN_FLASH_TIMEOUT_IN_NS =
                TimeUnit.SECONDS.toNanos(2);

        private final Camera2CameraControlImpl mCameraControl;
        private final Executor mExecutor;
        private final ScheduledExecutorService mScheduler;
        private final ImageCapture.ScreenFlash mScreenFlash;
        private final UseFlashModeTorchFor3aUpdate mUseFlashModeTorchFor3aUpdate;

        ScreenFlashTask(@NonNull Camera2CameraControlImpl cameraControl, @NonNull Executor executor,
                @NonNull ScheduledExecutorService scheduler,
                @NonNull UseFlashModeTorchFor3aUpdate useFlashModeTorchFor3aUpdate) {
            mCameraControl = cameraControl;
            mExecutor = executor;
            mScheduler = scheduler;
            mUseFlashModeTorchFor3aUpdate = useFlashModeTorchFor3aUpdate;

            mScreenFlash = Objects.requireNonNull(mCameraControl.getScreenFlash());
        }

        @ExecutedBy("mExecutor")
        @NonNull
        @Override
        public ListenableFuture<Boolean> preCapture(@Nullable TotalCaptureResult captureResult) {
            Logger.d(TAG, "ScreenFlashTask#preCapture");

            AtomicReference<ImageCapture.ScreenFlashListener> screenFlashListener =
                    new AtomicReference<>();

            ListenableFuture<Void> uiAppliedFuture = CallbackToFutureAdapter.getFuture(
                    completer -> {
                        screenFlashListener.set(() -> {
                            Logger.d(TAG, "ScreenFlashTask#preCapture: UI change applied");
                            completer.set(null);
                        });
                        return "OnScreenFlashUiApplied";
                    });

            ListenableFuture<Void> future = CallbackToFutureAdapter.getFuture(completer -> {
                CameraXExecutors.mainThreadExecutor().execute(() -> {
                    Logger.d(TAG, "ScreenFlashTask#preCapture: invoking applyScreenFlashUi");
                    mScreenFlash.apply(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(
                                    ImageCapture.SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS),
                            screenFlashListener.get());
                    completer.set(null);
                });
                return "OnScreenFlashStart";
            });

            return FutureChain.from(future).transformAsync(
                    input -> mCameraControl.getFocusMeteringControl().enableExternalFlashAeMode(
                            true),
                    mExecutor
            ).transformAsync(
                    input -> CallbackToFutureAdapter.getFuture(
                            completer -> {
                                if (!mUseFlashModeTorchFor3aUpdate.shouldUseFlashModeTorch()) {
                                    completer.set(null);
                                    return "EnableTorchInternal";
                                }
                                Logger.d(TAG, "ScreenFlashTask#preCapture: enable torch");
                                mCameraControl.enableTorchInternal(true);
                                completer.set(null);
                                return "EnableTorchInternal";
                            }),
                    mExecutor
            ).transformAsync(
                    input -> Futures.makeTimeoutFuture(
                            // Not using the previous timestamp here gives users a bit more grace
                            // time before CameraX stops waiting.
                            TimeUnit.SECONDS.toMillis(
                                    ImageCapture.SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS),
                            mScheduler, null, true, uiAppliedFuture),
                    mExecutor
            ).transformAsync(
                    input -> mCameraControl.getFocusMeteringControl().triggerAePrecapture(),
                    mExecutor
            ).transformAsync(
                    input -> waitForResult(CHECK_3A_WITH_SCREEN_FLASH_TIMEOUT_IN_NS, mScheduler,
                            mCameraControl, (result) -> is3AConverged(result, false)), mExecutor
            ).transform(input -> false, CameraXExecutors.directExecutor());
        }

        @ExecutedBy("mExecutor")
        @Override
        public boolean isCaptureResultNeeded() {
            return false;
        }

        @ExecutedBy("mExecutor")
        @Override
        public void postCapture() {
            Logger.d(TAG, "ScreenFlashTask#postCapture");
            if (mUseFlashModeTorchFor3aUpdate.shouldUseFlashModeTorch()) {
                mCameraControl.enableTorchInternal(false);
            }
            mCameraControl.getFocusMeteringControl().enableExternalFlashAeMode(false).addListener(
                    () -> Log.d(TAG, "enableExternalFlashAeMode disabled"), mExecutor
            );
            mCameraControl.getFocusMeteringControl().cancelAfAeTrigger(false, true);
            CameraXExecutors.mainThreadExecutor().execute(mScreenFlash::clear);
        }
    }

    static boolean isFlashRequired(@FlashMode int flashMode, @Nullable TotalCaptureResult result) {
        Logger.d(TAG, "isFlashRequired: flashMode = " + flashMode);

        switch (flashMode) {
            case FLASH_MODE_SCREEN:
            case FLASH_MODE_ON:
                return true;
            case FLASH_MODE_AUTO:
                Integer aeState = (result != null) ? result.get(CaptureResult.CONTROL_AE_STATE)
                        : null;
                Logger.d(TAG, "isFlashRequired: aeState = " + aeState);
                return aeState != null && aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED;
            case FLASH_MODE_OFF:
                return false;
        }
        throw new AssertionError(flashMode);
    }

    /**
     * A listener receives the result of the repeating request. The results will be sent to the
     * Checker to identify if the mFuture can be completed.
     */
    static class ResultListener implements Camera2CameraControlImpl.CaptureResultListener {

        /**
         * The totalCaptureResults will be sent to the Checker#check() method, return true in the
         * Checker#check() will complete the mFuture.
         */
        interface Checker {
            boolean check(@NonNull TotalCaptureResult totalCaptureResult);
        }

        private CallbackToFutureAdapter.Completer<TotalCaptureResult> mCompleter;
        private final ListenableFuture<TotalCaptureResult> mFuture =
                CallbackToFutureAdapter.getFuture(completer -> {
                    mCompleter = completer;
                    return "waitFor3AResult";
                });
        private final Checker mChecker;

        /**
         * @param checker     the checker to define the condition to complete the mFuture, set null
         *                    will complete the mFuture once it receives any totalCaptureResults.
         */
        ResultListener(@Nullable Checker checker) {
            mChecker = checker;
        }

        @NonNull
        public ListenableFuture<TotalCaptureResult> getFuture() {
            return mFuture;
        }

        @Override
        public boolean onCaptureResult(@NonNull TotalCaptureResult captureResult) {
            if (mChecker != null && !mChecker.check(captureResult)) {
                return false;
            }

            mCompleter.set(captureResult);
            return true;
        }
    }

    /** Whether torch flash should be used due to quirk or VideoCapture binding. */
    private boolean isTorchAsFlash(@FlashType int flashType) {
        return mUseTorchAsFlash.shouldUseTorchAsFlash() || mTemplate == CameraDevice.TEMPLATE_RECORD
                || flashType == FLASH_TYPE_USE_TORCH_AS_FLASH;
    }
}
