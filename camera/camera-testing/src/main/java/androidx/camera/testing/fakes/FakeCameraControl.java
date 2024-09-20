/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.testing.fakes;

import static androidx.camera.core.ImageCapture.FLASH_MODE_OFF;
import static androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.ScreenFlash;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Logger;
import androidx.camera.core.imagecapture.CameraCapturePipeline;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.testing.impl.FakeCameraCapturePipeline;
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Pair;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * A fake implementation for the {@link CameraControlInternal} interface which is capable of
 * notifying submitted requests using the associated {@link CameraCaptureCallback} instances or
 * {@link ControlUpdateCallback}.
 */
public final class FakeCameraControl implements CameraControlInternal {
    private static final String TAG = "FakeCameraControl";
    private static final ControlUpdateCallback NO_OP_CALLBACK = new ControlUpdateCallback() {
        @Override
        public void onCameraControlUpdateSessionConfig() {
            // No-op
        }

        @Override
        public void onCameraControlCaptureRequests(
                @NonNull List<CaptureConfig> captureConfigs) {
            // No-op
        }
    };

    /**
     * The executor used to invoke any callback/listener which doesn't have a dedicated executor
     * for it.
     * <p> {@link CameraXExecutors#directExecutor} via default, unless some other executor is set
     * via {@link #FakeCameraControl(Executor, CameraControlInternal.ControlUpdateCallback)}.
     */
    @NonNull private final Executor mExecutor;
    private final ControlUpdateCallback mControlUpdateCallback;
    private final SessionConfig.Builder mSessionConfigBuilder = new SessionConfig.Builder();
    @ImageCapture.FlashMode
    private int mFlashMode = FLASH_MODE_OFF;
    private final ArrayList<CaptureConfig> mSubmittedCaptureRequests = new ArrayList<>();
    private Pair<Executor, OnNewCaptureRequestListener> mOnNewCaptureRequestListener;
    private MutableOptionsBundle mInteropConfig = MutableOptionsBundle.create();
    private final ArrayList<CallbackToFutureAdapter.Completer<Void>> mSubmittedCompleterList =
            new ArrayList<>();

    private boolean mIsZslDisabledByUseCaseConfig = false;
    private boolean mIsZslConfigAdded = false;
    private float mZoomRatio = -1;
    private float mLinearZoom = -1;
    private boolean mTorchEnabled = false;
    private int mExposureCompensation = -1;
    private ScreenFlash mScreenFlash;

    private final FakeCameraCapturePipeline mFakeCameraCapturePipeline =
            new FakeCameraCapturePipeline();

    @Nullable
    private FocusMeteringAction mLastSubmittedFocusMeteringAction = null;

    /**
     * Constructs an instance of {@link FakeCameraControl} with a no-op
     * {@link ControlUpdateCallback}.
     *
     * @see #FakeCameraControl(CameraControlInternal.ControlUpdateCallback)
     * @see #FakeCameraControl(Executor, CameraControlInternal.ControlUpdateCallback)
     */
    public FakeCameraControl() {
        this(NO_OP_CALLBACK);
    }

    /**
     * Constructs an instance of {@link FakeCameraControl} with the
     * provided {@link ControlUpdateCallback}.
     *
     * <p> Note that callbacks will be executed on the calling thread directly via
     * {@link CameraXExecutors#directExecutor}. To specify the execution thread, use
     * {@link #FakeCameraControl(Executor, CameraControlInternal.ControlUpdateCallback)}.
     *
     * @param controlUpdateCallback {@link ControlUpdateCallback} to notify events.
     */
    public FakeCameraControl(@NonNull ControlUpdateCallback controlUpdateCallback) {
        this(CameraXExecutors.directExecutor(), controlUpdateCallback);
    }

    /**
     * Constructs an instance of {@link FakeCameraControl} with the
     * provided {@link ControlUpdateCallback}.
     *
     * @param executor {@link Executor} used to invoke the {@code controlUpdateCallback}.
     * @param controlUpdateCallback {@link ControlUpdateCallback} to notify events.
     */
    public FakeCameraControl(@NonNull Executor executor,
            @NonNull ControlUpdateCallback controlUpdateCallback) {
        mExecutor = executor;
        mControlUpdateCallback = controlUpdateCallback;
    }

    /**
     * Notifies all submitted requests using {@link CameraCaptureCallback#onCaptureCancelled},
     * which is invoked in the thread denoted by {@link #mExecutor}.
     */
    public void notifyAllRequestsOnCaptureCancelled() {
        for (CaptureConfig captureConfig : mSubmittedCaptureRequests) {
            for (CameraCaptureCallback cameraCaptureCallback :
                    captureConfig.getCameraCaptureCallbacks()) {
                mExecutor.execute(() -> {
                    cameraCaptureCallback.onCaptureCancelled(captureConfig.getId());
                });
            }
        }
        for (CallbackToFutureAdapter.Completer<Void> completer : mSubmittedCompleterList) {
            completer.setException(
                    new ImageCaptureException(ImageCapture.ERROR_CAMERA_CLOSED, "Simulate "
                            + "capture cancelled", null));
        }
        mSubmittedCompleterList.clear();
        mSubmittedCaptureRequests.clear();
    }

    /**
     * Notifies all submitted requests using {@link CameraCaptureCallback#onCaptureFailed},
     * which is invoked in the thread denoted by {@link #mExecutor}.
     */
    public void notifyAllRequestsOnCaptureFailed() {
        for (CaptureConfig captureConfig : mSubmittedCaptureRequests) {
            for (CameraCaptureCallback cameraCaptureCallback :
                    captureConfig.getCameraCaptureCallbacks()) {
                mExecutor.execute(() -> cameraCaptureCallback.onCaptureFailed(
                        captureConfig.getId(),
                        new CameraCaptureFailure(CameraCaptureFailure.Reason.ERROR)));
            }
        }
        for (CallbackToFutureAdapter.Completer<Void> completer : mSubmittedCompleterList) {
            completer.setException(new ImageCaptureException(ImageCapture.ERROR_CAPTURE_FAILED,
                    "Simulate capture fail", null));
        }
        mSubmittedCompleterList.clear();
        mSubmittedCaptureRequests.clear();
    }

    /**
     * Notifies all submitted requests using {@link CameraCaptureCallback#onCaptureCompleted},
     * which is invoked in the thread denoted by {@link #mExecutor}.
     *
     * @param result The {@link CameraCaptureResult} which is notified to all the callbacks.
     */
    public void notifyAllRequestsOnCaptureCompleted(@NonNull CameraCaptureResult result) {
        for (CaptureConfig captureConfig : mSubmittedCaptureRequests) {
            for (CameraCaptureCallback cameraCaptureCallback :
                    captureConfig.getCameraCaptureCallbacks()) {
                mExecutor.execute(() -> cameraCaptureCallback.onCaptureCompleted(
                        captureConfig.getId(), result));
            }
        }
        for (CallbackToFutureAdapter.Completer<Void> completer : mSubmittedCompleterList) {
            completer.set(null);
        }
        mSubmittedCompleterList.clear();
        mSubmittedCaptureRequests.clear();
    }

    @ImageCapture.FlashMode
    @Override
    public int getFlashMode() {
        return mFlashMode;
    }

    @Override
    public void setFlashMode(@ImageCapture.FlashMode int flashMode) {
        mFlashMode = flashMode;
        Logger.d(TAG, "setFlashMode(" + mFlashMode + ")");
    }

    @Override
    public void setScreenFlash(@Nullable ScreenFlash screenFlash) {
        mScreenFlash = screenFlash;
        Logger.d(TAG, "setScreenFlash(" + mScreenFlash + ")");
    }

    @Nullable
    public ScreenFlash getScreenFlash() {
        return mScreenFlash;
    }

    @Override
    public void setZslDisabledByUserCaseConfig(boolean disabled) {
        mIsZslDisabledByUseCaseConfig = disabled;
    }

    @Override
    public boolean isZslDisabledByByUserCaseConfig() {
        return mIsZslDisabledByUseCaseConfig;
    }

    @Override
    public void addZslConfig(@NonNull SessionConfig.Builder sessionConfigBuilder) {
        // Override if Zero-Shutter Lag needs to add config to session config.
        mIsZslConfigAdded = true;
    }

    /**
     * Checks if {@link FakeCameraControl#addZslConfig(SessionConfig.Builder)} has been triggered.
     */
    public boolean isZslConfigAdded() {
        return mIsZslConfigAdded;
    }

    /**
     * Sets the torch status.
     *
     * @param torch The torch status is set as enabled if true, disabled if false.
     * @return Returns a {@link Futures#immediateFuture} which immediately contains a result.
     */
    @Override
    @NonNull
    public ListenableFuture<Void> enableTorch(boolean torch) {
        Logger.d(TAG, "enableTorch(" + torch + ")");
        mTorchEnabled = torch;
        return Futures.immediateFuture(null);
    }

    /** Returns if torch is set as enabled. */
    public boolean getTorchEnabled() {
        return mTorchEnabled;
    }

    /**
     * Sets the exposure compensation index.
     *
     * @param value The exposure compensation value to be set.
     * @return Returns a {@link Futures#immediateFuture} which immediately contains a result.
     */
    @NonNull
    @Override
    public ListenableFuture<Integer> setExposureCompensationIndex(int value) {
        mExposureCompensation = value;
        return Futures.immediateFuture(null);
    }

    /** Returns the exposure compensation index. */
    public int getExposureCompensationIndex() {
        return mExposureCompensation;
    }

    @NonNull
    @Override
    public ListenableFuture<List<Void>> submitStillCaptureRequests(
            @NonNull List<CaptureConfig> captureConfigs,
            int captureMode, int flashType) {
        mSubmittedCaptureRequests.addAll(captureConfigs);
        mExecutor.execute(
                () -> mControlUpdateCallback.onCameraControlCaptureRequests(captureConfigs));
        List<ListenableFuture<Void>> fakeFutures = new ArrayList<>();
        for (int i = 0; i < captureConfigs.size(); i++) {
            fakeFutures.add(CallbackToFutureAdapter.getFuture(completer -> {
                mSubmittedCompleterList.add(completer);
                return "fakeFuture";
            }));
        }

        if (mOnNewCaptureRequestListener != null) {
            Executor executor = Objects.requireNonNull(mOnNewCaptureRequestListener.first);
            OnNewCaptureRequestListener listener =
                    Objects.requireNonNull(mOnNewCaptureRequestListener.second);

            executor.execute(() -> listener.onNewCaptureRequests(captureConfigs));
        }
        return Futures.allAsList(fakeFutures);
    }

    @NonNull
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public ListenableFuture<CameraCapturePipeline> getCameraCapturePipelineAsync(int captureMode,
            int flashType) {
        return Futures.immediateFuture(mFakeCameraCapturePipeline);
    }

    @NonNull
    @Override
    public SessionConfig getSessionConfig() {
        return mSessionConfigBuilder.build();
    }

    /**
     * Returns a {@link Rect} corresponding to
     * {@link FakeCameraDeviceSurfaceManager#MAX_OUTPUT_SIZE}.
     */
    @NonNull
    @Override
    public Rect getSensorRect() {
        return new Rect(0, 0, MAX_OUTPUT_SIZE.getWidth(), MAX_OUTPUT_SIZE.getHeight());
    }

    /**
     * Stores the last submitted {@link FocusMeteringAction}.
     *
     * @param action The {@link FocusMeteringAction} to be used.
     * @return Returns a {@link Futures#immediateFuture} which immediately contains a empty
     * {@link FocusMeteringResult}.
     */
    @NonNull
    @Override
    public ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action) {
        mLastSubmittedFocusMeteringAction = action;
        return Futures.immediateFuture(FocusMeteringResult.emptyInstance());
    }

    /** Returns a {@link Futures#immediateFuture} which immediately contains a result. */
    @NonNull
    @Override
    public ListenableFuture<Void> cancelFocusAndMetering() {
        return Futures.immediateFuture(null);
    }

    /**
     * Sets a listener to be notified when there are new capture requests submitted.
     *
     * <p> Note that the listener will be executed on the calling thread directly using
     * {@link CameraXExecutors#directExecutor}. To specify the execution thread, use
     * {@link #setOnNewCaptureRequestListener(Executor, OnNewCaptureRequestListener)}.
     *
     * @param listener {@link OnNewCaptureRequestListener} that is notified with the submitted
     * {@link CaptureConfig} parameters when new capture requests are submitted.
     */
    public void setOnNewCaptureRequestListener(@NonNull OnNewCaptureRequestListener listener) {
        setOnNewCaptureRequestListener(CameraXExecutors.directExecutor(), listener);
    }

    /**
     * Sets a listener to be notified when there are new capture requests submitted.
     *
     * @param executor {@link Executor} used to notify the {@code listener}.
     * @param listener {@link OnNewCaptureRequestListener} that is notified with the submitted
     * {@link CaptureConfig} parameters when new capture requests are submitted.
     */
    public void setOnNewCaptureRequestListener(@NonNull Executor executor,
            @NonNull OnNewCaptureRequestListener listener) {
        mOnNewCaptureRequestListener = new Pair<>(executor, listener);
    }

    /**
     * Clears any listener set via {@link #setOnNewCaptureRequestListener}.
     */
    public void clearNewCaptureRequestListener() {
        mOnNewCaptureRequestListener = null;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setZoomRatio(float ratio) {
        mZoomRatio = ratio;
        return Futures.immediateFuture(null);
    }

    /** Gets the linear zoom value set with {@link #setZoomRatio}. */
    public float getZoomRatio() {
        return mZoomRatio;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> setLinearZoom(float linearZoom) {
        mLinearZoom = linearZoom;
        return Futures.immediateFuture(null);
    }

    /** Gets the linear zoom value set with {@link #setLinearZoom}. */
    public float getLinearZoom() {
        return mLinearZoom;
    }

    /** Gets the last focus metering action submitted with {@link #startFocusAndMetering}. */
    @Nullable
    public FocusMeteringAction getLastSubmittedFocusMeteringAction() {
        return mLastSubmittedFocusMeteringAction;
    }

    @Override
    public void addInteropConfig(@NonNull Config config) {
        for (Config.Option<?> option : config.listOptions()) {
            @SuppressWarnings("unchecked")
            Config.Option<Object> objectOpt = (Config.Option<Object>) option;
            mInteropConfig.insertOption(objectOpt, config.retrieveOption(objectOpt));
        }
    }

    @Override
    public void clearInteropConfig() {
        mInteropConfig = MutableOptionsBundle.create();
    }

    @NonNull
    @Override
    public Config getInteropConfig() {
        return MutableOptionsBundle.from(mInteropConfig);
    }

    /** A listener which is used to notify when there are new submitted capture requests */
    public interface OnNewCaptureRequestListener {
        /** Called when there are new submitted capture request */
        void onNewCaptureRequests(@NonNull List<CaptureConfig> captureConfigs);
    }
}
