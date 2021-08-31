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

import android.hardware.camera2.CameraDevice;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.interop.CaptureRequestOptions;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.DeferrableSurfaces;
import androidx.camera.core.impl.OutputSurface;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.core.impl.SessionProcessorSurface;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A CaptureSession that uses a {@link SessionProcessor} to process/transform the stream
 * configuration, repeating request and still-capture request.
 *
 * <p>This ProcessingCaptureSession works just like a normal {@link CaptureSession} excepts that
 * there are some following restrictions:
 * <pre>
 * (1) Target surfaces specified in {@link #setSessionConfig} and
 * {@link #issueCaptureRequests(List)} are ignored. Target surfaces can only be set by
 * {@link SessionProcessor}.
 * (2) After {@link #setSessionConfig(SessionConfig)} is invoked,
 * {@link SessionConfig#getRepeatingCameraCaptureCallbacks()} will be invoked but the
 * {@link CameraCaptureResult} doesn't contain camera2
 * {@link android.hardware.camera2.CaptureResult}.
 * (3) {@link #issueCaptureRequests(List)} can only execute {@link CaptureConfig} with
 * CameraDevice.TEMPLATE_STILL_CAPTURE. Others captureConfigs will be cancelled immediately.
 * {@link CaptureConfig#getCameraCaptureCallbacks()} will be invoked but the
 * {@link CameraCaptureResult} doesn't contain camera2
 * {@link android.hardware.camera2.CaptureResult}.
 * </pre>
 * <p>This class is not thread-safe. All methods must be executed sequentially.
 */
final class ProcessingCaptureSession implements CaptureSessionInterface {
    private static final String TAG = "ProcessingCaptureSession";
    private final SessionProcessor mSessionProcessor;
    private final Camera2CameraInfoImpl mCamera2CameraInfoImpl;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Executor mExecutor;
    private final ScheduledExecutorService mScheduledExecutorService;
    private final CaptureSession mCaptureSession;
    private List<DeferrableSurface> mOutputSurfaces = new ArrayList<>();
    @Nullable
    private SessionConfig mSessionConfig;
    @Nullable
    private Camera2RequestProcessor mRequestProcessor;
    @Nullable
    private SessionConfig mProcessorSessionConfig;
    private boolean mIsRepeatingRequestStarted = false;

    private static final long TIMEOUT_GET_SURFACE_IN_MS = 5000L;
    private ProcessorState mProcessorState;
    private static List<DeferrableSurface> sHeldProcessorSurfaces = new ArrayList<>();
    @Nullable
    private volatile CaptureConfig mPendingCaptureConfig = null;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    volatile boolean mIsExecutingStillCaptureRequest = false;
    private final SessionProcessorCaptureCallback mSessionProcessorCaptureCallback;
    private CaptureRequestOptions mSessionOptions = new CaptureRequestOptions.Builder().build();
    private CaptureRequestOptions mStillCaptureOptions =
            new CaptureRequestOptions.Builder().build();


    private enum ProcessorState {
        UNINITIALIZED,
        SESSION_INITIALIZED,
        ON_CAPTURE_SESSION_STARTED,
        ON_CAPTURE_SESSION_ENDED,
        CLOSED
    }

    // For debugging only
    private static int sNextInstanceId = 0;
    private int mInstanceId = 0;

    ProcessingCaptureSession(@NonNull SessionProcessor sessionProcessor,
            @NonNull Camera2CameraInfoImpl camera2CameraInfoImpl, @NonNull Executor executor,
            @NonNull ScheduledExecutorService scheduledExecutorService) {
        mCaptureSession = new CaptureSession();
        mSessionProcessor = sessionProcessor;
        mCamera2CameraInfoImpl = camera2CameraInfoImpl;
        mExecutor = executor;
        mScheduledExecutorService = scheduledExecutorService;
        mProcessorState = ProcessorState.UNINITIALIZED;
        mSessionProcessorCaptureCallback = new SessionProcessorCaptureCallback(mExecutor);

        mInstanceId = sNextInstanceId++;
        Logger.d(TAG, "New ProcessingCaptureSession (id=" + mInstanceId + ")");
    }

    @NonNull
    @Override
    public ListenableFuture<Void> open(@NonNull SessionConfig sessionConfig,
            @NonNull CameraDevice cameraDevice, @NonNull SynchronizedCaptureSessionOpener opener) {
        Preconditions.checkArgument(mProcessorState == ProcessorState.UNINITIALIZED,
                "Invalid state state:" + mProcessorState);
        Preconditions.checkArgument(!sessionConfig.getSurfaces().isEmpty(),
                "SessionConfig contains no surfaces");

        Logger.d(TAG, "open (id=" + mInstanceId + ")");
        mOutputSurfaces = sessionConfig.getSurfaces();
        ListenableFuture<Void> future =
                FutureChain.from(DeferrableSurfaces.surfaceListWithTimeout(
                        mOutputSurfaces, false,
                        TIMEOUT_GET_SURFACE_IN_MS, mExecutor, mScheduledExecutorService))
                        .transformAsync(surfaceList -> {
                            Logger.d(TAG,
                                    "-- getSurfaces done, start init (id=" + mInstanceId + ")");
                            if (mProcessorState == ProcessorState.CLOSED) {
                                return Futures.immediateFailedFuture(new IllegalStateException(
                                        "SessionProcessorCaptureSession is closed."));
                            }

                            // Containing null means some DeferrableSurface was closed and we
                            // need to propagate the SurfaceClosedException in order to recreate
                            // the surfaces.
                            if (surfaceList.contains(null)) {
                                DeferrableSurface deferrableSurface =
                                        sessionConfig.getSurfaces().get(surfaceList.indexOf(null));
                                return Futures.immediateFailedFuture(
                                        new DeferrableSurface.SurfaceClosedException(
                                                "Surface closed", deferrableSurface));
                            }

                            try {
                                DeferrableSurfaces.incrementAll(mOutputSurfaces);
                            } catch (DeferrableSurface.SurfaceClosedException e) {
                                return Futures.immediateFailedFuture(e);
                            }

                            OutputSurface previewOutputSurface = null;
                            OutputSurface captureOutputSurface = null;
                            OutputSurface analysisOutputSurface = null;

                            for (int i = 0; i < sessionConfig.getSurfaces().size(); i++) {
                                DeferrableSurface dSurface = sessionConfig.getSurfaces().get(i);
                                if (Objects.equals(dSurface.getContainerClass(),
                                        Preview.class)) {
                                    previewOutputSurface = OutputSurface.create(
                                            dSurface.getSurface().get(),
                                            new Size(dSurface.getPrescribedSize().getWidth(),
                                                    dSurface.getPrescribedSize().getHeight()),
                                            dSurface.getPrescribedStreamFormat());
                                } else if (Objects.equals(dSurface.getContainerClass(),
                                        ImageCapture.class)) {
                                    captureOutputSurface = OutputSurface.create(
                                            dSurface.getSurface().get(),
                                            new Size(dSurface.getPrescribedSize().getWidth(),
                                                    dSurface.getPrescribedSize().getHeight()),
                                            dSurface.getPrescribedStreamFormat());
                                } else if (Objects.equals(dSurface.getContainerClass(),
                                        ImageAnalysis.class)) {
                                    analysisOutputSurface = OutputSurface.create(
                                            dSurface.getSurface().get(),
                                            new Size(dSurface.getPrescribedSize().getWidth(),
                                                    dSurface.getPrescribedSize().getHeight()),
                                            dSurface.getPrescribedStreamFormat());
                                }
                            }

                            mProcessorState = ProcessorState.SESSION_INITIALIZED;
                            Logger.w(TAG, "== initSession (id=" + mInstanceId + ")");
                            mProcessorSessionConfig = mSessionProcessor.initSession(
                                    mCamera2CameraInfoImpl,
                                    previewOutputSurface,
                                    captureOutputSurface,
                                    analysisOutputSurface
                            );

                            // DecrementAll the output surfaces when ProcessorSurface
                            // terminates.
                            mProcessorSessionConfig.getSurfaces().get(0).getTerminationFuture()
                                    .addListener(() -> {
                                        DeferrableSurfaces.decrementAll(mOutputSurfaces);
                                    }, CameraXExecutors.directExecutor());

                            // Holding the Processor surfaces in case they are GCed
                            for (DeferrableSurface surface :
                                    mProcessorSessionConfig.getSurfaces()) {
                                sHeldProcessorSurfaces.add(surface);
                                surface.getTerminationFuture().addListener(() -> {
                                    sHeldProcessorSurfaces.remove(surface);
                                }, mExecutor);
                            }

                            SessionConfig.ValidatingBuilder validatingBuilder =
                                    new SessionConfig.ValidatingBuilder();
                            validatingBuilder.add(sessionConfig);
                            validatingBuilder.clearSurfaces(); // remove origin surfaces.
                            validatingBuilder.add(mProcessorSessionConfig);
                            Preconditions.checkArgument(validatingBuilder.isValid(),
                                    "Cannot transform the SessionConfig");
                            SessionConfig transformedConfig = validatingBuilder.build();
                            ListenableFuture<Void> openSessionFuture =
                                    mCaptureSession.open(transformedConfig,
                                            Preconditions.checkNotNull(cameraDevice),
                                            opener);
                            Futures.addCallback(openSessionFuture, new FutureCallback<Void>() {
                                @Override
                                public void onSuccess(@Nullable Void result) {
                                    // do nothing
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    // Close() will invoke appropriate SessionProcessor methods
                                    // to clear up and mark this session as CLOSED.
                                    Logger.e(TAG, "open session failed ", t);
                                    close();
                                }
                            }, mExecutor);
                            return openSessionFuture;
                        }, mExecutor)
                        .transform(v -> {
                            // Using transform instead of addListener because we want to ensure
                            // SessionProcessor#onCaptureSessionStarted is called when the future
                            // completes. Using future.addListener cannot guarantee that.
                            onConfigured(mCaptureSession);
                            return null;
                        }, mExecutor);

        return future;
    }

    private static void cancelRequests(@NonNull List<CaptureConfig> captureConfigs) {
        for (CaptureConfig captureConfig : captureConfigs) {
            for (CameraCaptureCallback cameraCaptureCallback :
                    captureConfig.getCameraCaptureCallbacks()) {
                cameraCaptureCallback.onCaptureCancelled();
            }
        }
    }

    private boolean isStillCapture(@NonNull List<CaptureConfig> captureConfigs) {
        if (captureConfigs.isEmpty()) {
            return false;
        }
        for (CaptureConfig captureConfig : captureConfigs) {
            // Don't need to consider TEMPLATE_VIDEO_SNAPSHOT case since extensions does not
            // support Video Capture yet
            if (captureConfig.getTemplateType() != CameraDevice.TEMPLATE_STILL_CAPTURE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Submit a still capture request via
     * {@link SessionProcessor#startCapture(SessionProcessor.CaptureCallback)}.
     *
     * <p>The method is more restrictive than {@link CaptureSession#issueCaptureRequests(List)}.
     * Only one @link CaptureConfig} with {@link CameraDevice#TEMPLATE_STILL_CAPTURE} template is
     * allowed. If the captureConfigs contain multiple {@link CaptureConfig}s or the contained
     * {@link CaptureConfig} does not use {@link CameraDevice#TEMPLATE_STILL_CAPTURE}, all
     * captureConfigs will be cancelled immediately.
     *
     * <p>Camera2 capture options in {@link CaptureConfig#getImplementationOptions()} will be
     * merged with the options in {@link SessionConfig#getImplementationOptions()} set by
     * {@link #setSessionConfig(SessionConfig)}. The merged parameters set will be passed to
     * {@link SessionProcessor#setParameters(Config)} but it is up to the implementation of the
     * {@link SessionProcessor} to determine which options to apply.
     *
     * <p>{@link CaptureConfig#getCameraCaptureCallbacks()} ()} will be invoked but it is unable
     * to invoke callbacks of {@link CaptureCallbackContainer} type due to lack of the access to
     * the camera2 {@link android.hardware.camera2.CameraCaptureSession.CaptureCallback}.
     *
     * <p>Still capture requests are expected to arrive one at a time sequentially by upper layer.
     * Capture requests will be cancelled if previous request have not finished.
     */
    @Override
    public void issueCaptureRequests(@NonNull List<CaptureConfig> captureConfigs) {
        if (captureConfigs.isEmpty()) {
            return;
        }
        if (captureConfigs.size() > 1 || !isStillCapture(captureConfigs)) {
            cancelRequests(captureConfigs);
            return;
        }
        // Only allows one capture config at a time.
        if (mPendingCaptureConfig != null || mIsExecutingStillCaptureRequest) {
            cancelRequests(captureConfigs);
            return;
        }

        // captureConfigs should contain exactly one CaptureConfig.
        CaptureConfig captureConfig = captureConfigs.get(0);

        Logger.d(TAG, "issueCaptureRequests (id=" + mInstanceId + ") + state =" + mProcessorState);

        switch (mProcessorState) {
            case UNINITIALIZED:
            case SESSION_INITIALIZED:
                mPendingCaptureConfig = captureConfig;

                break;
            case ON_CAPTURE_SESSION_STARTED:
                mIsExecutingStillCaptureRequest = true;
                mStillCaptureOptions =
                        CaptureRequestOptions.Builder.from(captureConfig.getImplementationOptions())
                                .build();
                updateParameters(mSessionOptions, mStillCaptureOptions);
                mSessionProcessor.startCapture(new SessionProcessor.CaptureCallback() {
                    @Override
                    public void onCaptureStarted(
                            int captureSequenceId, long timestamp) {
                    }

                    @Override
                    public void onCaptureProcessStarted(
                            int captureSequenceId) {
                    }

                    @Override
                    public void onCaptureFailed(
                            int captureSequenceId) {
                        mExecutor.execute(() -> {
                            for (CameraCaptureCallback cameraCaptureCallback :
                                    captureConfig.getCameraCaptureCallbacks()) {
                                cameraCaptureCallback.onCaptureFailed(new CameraCaptureFailure(
                                        CameraCaptureFailure.Reason.ERROR));
                            }
                            mIsExecutingStillCaptureRequest = false;
                        });
                    }

                    @Override
                    public void onCaptureSequenceCompleted(int captureSequenceId) {
                        mExecutor.execute(() -> {
                            for (CameraCaptureCallback cameraCaptureCallback :
                                    captureConfig.getCameraCaptureCallbacks()) {
                                cameraCaptureCallback.onCaptureCompleted(
                                        new CameraCaptureResult.EmptyCameraCaptureResult());
                            }
                            mIsExecutingStillCaptureRequest = false;
                        });
                    }

                    @Override
                    public void onCaptureSequenceAborted(int captureSequenceId) {
                    }
                });
                break;
            case ON_CAPTURE_SESSION_ENDED:
            case CLOSED:
                Logger.d(TAG, "Run issueCaptureRequests in wrong state, state = "
                        + mProcessorState);
                cancelRequests(captureConfigs);
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public ListenableFuture<Void> release(boolean abortInFlightCaptures) {
        Preconditions.checkState(mProcessorState == ProcessorState.CLOSED,
                "release() can only be called in CLOSED state");
        Logger.d(TAG, "release (id=" + mInstanceId + ")");
        return mCaptureSession.release(abortInFlightCaptures);
    }

    private static List<SessionProcessorSurface> getSessionProcessorSurfaceList(
            List<DeferrableSurface> deferrableSurfaceList) {
        ArrayList<SessionProcessorSurface> outputSurfaceList = new ArrayList<>();
        for (DeferrableSurface deferrableSurface : deferrableSurfaceList) {
            Preconditions.checkArgument(deferrableSurface instanceof SessionProcessorSurface,
                    "Surface must be SessionProcessorSurface");
            outputSurfaceList.add((SessionProcessorSurface) deferrableSurface);
        }
        return outputSurfaceList;
    }

    void onConfigured(@NonNull CaptureSession captureSession) {
        Preconditions.checkArgument(mProcessorState == ProcessorState.SESSION_INITIALIZED,
                "Invalid state state:" + mProcessorState);

        mRequestProcessor = new Camera2RequestProcessor(captureSession,
                getSessionProcessorSurfaceList(mProcessorSessionConfig.getSurfaces()));
        mSessionProcessor.onCaptureSessionStart(mRequestProcessor);
        mProcessorState = ProcessorState.ON_CAPTURE_SESSION_STARTED;

        if (mSessionConfig != null) {
            setSessionConfig(mSessionConfig);
        }

        if (mPendingCaptureConfig != null) {
            List<CaptureConfig> pendingCaptureConfigList = Arrays.asList(mPendingCaptureConfig);
            mPendingCaptureConfig = null;
            issueCaptureRequests(pendingCaptureConfigList);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public SessionConfig getSessionConfig() {
        return mSessionConfig;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public List<CaptureConfig> getCaptureConfigs() {
        return mPendingCaptureConfig != null ? Arrays.asList(mPendingCaptureConfig)
                : Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelIssuedCaptureRequests() {
        Logger.d(TAG, "cancelIssuedCaptureRequests (id=" + mInstanceId + ")");
        if (mPendingCaptureConfig != null) {
            for (CameraCaptureCallback cameraCaptureCallback :
                    mPendingCaptureConfig.getCameraCaptureCallbacks()) {
                cameraCaptureCallback.onCaptureCancelled();
            }
            mPendingCaptureConfig = null;
        }
    }

    /**
     * Invokes appropriate SessionProcessor methods to clear up and mark this session as CLOSED.
     */
    @Override
    public void close() {
        Logger.d(TAG, "close (id=" + mInstanceId + ") state=" + mProcessorState);

        switch (mProcessorState) {
            case ON_CAPTURE_SESSION_STARTED:
                mSessionProcessor.onCaptureSessionEnd();
                if (mRequestProcessor != null) {
                    mRequestProcessor.close();
                }
                mProcessorState = ProcessorState.ON_CAPTURE_SESSION_ENDED;
                // fall through
            case ON_CAPTURE_SESSION_ENDED:
            case SESSION_INITIALIZED:
                mSessionProcessor.deInitSession();
                break;
            case UNINITIALIZED:
                // do nothing
                break;
            case CLOSED:
                return; // already closed, no need to close again.
        }

        mProcessorState = ProcessorState.CLOSED;
        mCaptureSession.close();
    }

    /**
     * Set active session config for repeating request.
     *
     * <p> Surfaces contained in the {@link SessionConfig} will be ignored since the target
     * surface of repeating request is determined by {@link SessionProcessor}.
     * {@link SessionProcessor#setParameters(Config)} will be called to update the request
     * parameters retrieved from {@link SessionConfig#getImplementationOptions()}. It will also
     * invoke {@link SessionProcessor#startRepeating(SessionProcessor.CaptureCallback)} if it is not
     * started yet. {@link SessionConfig#getRepeatingCameraCaptureCallbacks()} will be invoked
     * but it is unable to invoke callbacks of {@link CaptureCallbackContainer} type.
     *
     * @param sessionConfig has the configuration that will currently active in issuing capture
     *                      request.
     */
    @Override
    public void setSessionConfig(@Nullable SessionConfig sessionConfig) {
        Logger.d(TAG, "setSessionConfig (id=" + mInstanceId + ")");

        mSessionConfig = sessionConfig;
        if (sessionConfig == null) {
            return;
        }

        mSessionProcessorCaptureCallback
                .setCameraCaptureCallbacks(sessionConfig.getRepeatingCameraCaptureCallbacks());

        if (mProcessorState == ProcessorState.ON_CAPTURE_SESSION_STARTED) {
            mSessionOptions =
                    CaptureRequestOptions.Builder.from(sessionConfig.getImplementationOptions())
                            .build();
            updateParameters(mSessionOptions, mStillCaptureOptions);
            if (!mIsRepeatingRequestStarted) {
                mSessionProcessor.startRepeating(mSessionProcessorCaptureCallback);
                mIsRepeatingRequestStarted = true;
            }
        }
    }

    private void updateParameters(@NonNull CaptureRequestOptions sessionOptions,
            @NonNull CaptureRequestOptions stillCaptureOptions) {
        Camera2ImplConfig.Builder builder = new Camera2ImplConfig.Builder();
        builder.insertAllOptions(sessionOptions);
        builder.insertAllOptions(stillCaptureOptions);
        mSessionProcessor.setParameters(builder.build());
    }

    private static class SessionProcessorCaptureCallback
            implements SessionProcessor.CaptureCallback {
        private List<CameraCaptureCallback> mCameraCaptureCallbacks = Collections.emptyList();
        private final Executor mExecutor;
        SessionProcessorCaptureCallback(@NonNull Executor executor) {
            mExecutor = executor;
        }

        public void setCameraCaptureCallbacks(
                @NonNull List<CameraCaptureCallback> cameraCaptureCallbacks) {
            mCameraCaptureCallbacks = cameraCaptureCallbacks;
        }

        @Override
        public void onCaptureStarted(int captureSequenceId, long timestamp) {
        }

        @Override
        public void onCaptureProcessStarted(int captureSequenceId) {
        }

        @Override
        public void onCaptureFailed(int captureSequenceId) {
            mExecutor.execute(() -> {
                for (CameraCaptureCallback cameraCaptureCallback : mCameraCaptureCallbacks) {
                    cameraCaptureCallback.onCaptureFailed(new CameraCaptureFailure(
                            CameraCaptureFailure.Reason.ERROR));
                }
            });
        }

        @Override
        public void onCaptureSequenceCompleted(int captureSequenceId) {
            mExecutor.execute(() -> {
                for (CameraCaptureCallback cameraCaptureCallback : mCameraCaptureCallbacks) {
                    cameraCaptureCallback.onCaptureCompleted(
                            CameraCaptureResult.EmptyCameraCaptureResult.create());
                }
            });
        }

        @Override
        public void onCaptureSequenceAborted(int captureSequenceId) {
        }
    }
}
