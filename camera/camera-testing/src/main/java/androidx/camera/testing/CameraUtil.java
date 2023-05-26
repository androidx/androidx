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

package androidx.camera.testing;

import static android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE;

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.DoNotInline;
import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.Logger;
import androidx.camera.core.UseCase;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.fakes.FakeCameraCoordinator;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.core.util.Preconditions;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.AssumptionViolatedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/** Utility functions for obtaining instances of camera2 classes. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CameraUtil {
    private CameraUtil() {
    }

    private static final String LOG_TAG = "CameraUtil";

    /** Amount of time to wait before timing out when trying to open a {@link CameraDevice}. */
    private static final int CAMERA_OPEN_TIMEOUT_SECONDS = 2;

    /** The device debug property key for the tests to enable the camera pretest. */
    private static final String PRETEST_CAMERA_TAG = "PreTestCamera";

    /**
     * Gets a new instance of a {@link CameraDevice}.
     *
     * <p>This method attempts to open up a new camera. Since the camera api is asynchronous it
     * needs to wait for camera open
     *
     * <p>After the camera is no longer needed {@link #releaseCameraDevice(CameraDeviceHolder)}
     * should be called to clean up resources.
     *
     * @throws CameraAccessException if the device is unable to access the camera
     * @throws InterruptedException  if a {@link CameraDevice} can not be retrieved within a set
     *                               time
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    @NonNull
    public static CameraDeviceHolder getCameraDevice(
            @Nullable CameraDevice.StateCallback stateCallback)
            throws CameraAccessException, InterruptedException, TimeoutException,
            ExecutionException {
        // Use the first camera available.
        List<String> cameraIds = getBackwardCompatibleCameraIdListOrThrow();
        if (cameraIds.isEmpty()) {
            throw new CameraAccessException(
                    CameraAccessException.CAMERA_ERROR, "Device contains no cameras.");
        }
        String cameraName = cameraIds.get(0);

        return new CameraDeviceHolder(getCameraManager(), cameraName, stateCallback);
    }

    /**
     * Gets a new instance of a {@link CameraDevice} by given camera id.
     *
     * <p>This method attempts to open up a new camera. Since the camera api is asynchronous it
     * needs to wait for camera open
     *
     * <p>After the camera is no longer needed {@link #releaseCameraDevice(CameraDeviceHolder)}
     * should be called to clean up resources.
     *
     * @throws InterruptedException  if a {@link CameraDevice} can not be retrieved within a set
     *                               time
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    @NonNull
    public static CameraDeviceHolder getCameraDevice(
            @NonNull String cameraId,
            @Nullable CameraDevice.StateCallback stateCallback)
            throws InterruptedException, TimeoutException,
            ExecutionException {
        return new CameraDeviceHolder(getCameraManager(), cameraId, stateCallback);
    }

    /**
     * Returns physical camera ids of the specified camera id.
     */
    @NonNull
    public static List<String> getPhysicalCameraIds(@NonNull String cameraId) {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                return Collections.unmodifiableList(new ArrayList<>(Api28Impl.getPhysicalCameraId(
                        getCameraManager().getCameraCharacteristics(cameraId))));
            } else {
                return Collections.emptyList();
            }

        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @RequiresApi(28)
    private static class Api28Impl {
        @DoNotInline
        static Set<String> getPhysicalCameraId(CameraCharacteristics cameraCharacteristics) {
            return cameraCharacteristics.getPhysicalCameraIds();
        }
    }

    /**
     * A container class used to hold a {@link CameraDevice}.
     *
     * <p>This class should contain a valid {@link CameraDevice} that can be retrieved with
     * {@link #get()}, unless the device has been closed.
     *
     * <p>The camera device should always be closed with
     * {@link CameraUtil#releaseCameraDevice(CameraDeviceHolder)} once finished with the device.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static class CameraDeviceHolder {

        final Object mLock = new Object();

        @GuardedBy("mLock")
        CameraDevice mCameraDevice;
        final HandlerThread mHandlerThread;
        final Handler mHandler;
        private ListenableFuture<Void> mCloseFuture;
        CameraCaptureSessionHolder mCameraCaptureSessionHolder;

        @RequiresPermission(Manifest.permission.CAMERA)
        CameraDeviceHolder(@NonNull CameraManager cameraManager, @NonNull String cameraId,
                @Nullable CameraDevice.StateCallback stateCallback)
                throws InterruptedException, ExecutionException, TimeoutException {
            mHandlerThread = new HandlerThread(String.format("CameraThread-%s", cameraId));
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());

            ListenableFuture<Void> cameraOpenFuture = openCamera(cameraManager, cameraId,
                    stateCallback);

            // Wait for the open future to complete before continuing.
            cameraOpenFuture.get(CAMERA_OPEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        @RequiresPermission(Manifest.permission.CAMERA)
        // Should only be called once during initialization.
        private ListenableFuture<Void> openCamera(@NonNull CameraManager cameraManager,
                @NonNull String cameraId,
                @Nullable CameraDevice.StateCallback extraStateCallback) {
            return CallbackToFutureAdapter.getFuture(openCompleter -> {
                mCloseFuture = CallbackToFutureAdapter.getFuture(closeCompleter -> {
                    cameraManager.openCamera(cameraId,
                            new DeviceStateCallbackImpl(openCompleter, closeCompleter,
                                    extraStateCallback), mHandler);
                    return "Close[cameraId=" + cameraId + "]";
                });
                return "Open[cameraId=" + cameraId + "]";
            });
        }

        @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info
        final class DeviceStateCallbackImpl extends CameraDevice.StateCallback {

            private final CallbackToFutureAdapter.Completer<Void> mOpenCompleter;
            private final CallbackToFutureAdapter.Completer<Void> mCloseCompleter;
            @Nullable
            private final CameraDevice.StateCallback mExtraStateCallback;

            DeviceStateCallbackImpl(
                    @NonNull CallbackToFutureAdapter.Completer<Void> openCompleter,
                    @NonNull CallbackToFutureAdapter.Completer<Void> closeCompleter,
                    @Nullable CameraDevice.StateCallback extraStateCallback) {
                mOpenCompleter = openCompleter;
                mCloseCompleter = closeCompleter;
                mExtraStateCallback = extraStateCallback;
            }

            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                synchronized (mLock) {
                    Preconditions.checkState(mCameraDevice == null, "CameraDevice "
                            + "should not have been opened yet.");
                    mCameraDevice = cameraDevice;
                }
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onOpened(cameraDevice);
                }
                mOpenCompleter.set(null);
            }

            @Override
            public void onClosed(@NonNull CameraDevice cameraDevice) {
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onClosed(cameraDevice);
                }
                mCloseCompleter.set(null);
                mHandlerThread.quitSafely();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                synchronized (mLock) {
                    mCameraDevice = null;
                    mCameraCaptureSessionHolder = null;
                }
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onDisconnected(cameraDevice);
                }
                cameraDevice.close();
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int i) {
                boolean notifyOpenFailed = false;
                synchronized (mLock) {
                    if (mCameraDevice == null) {
                        notifyOpenFailed = true;
                    } else {
                        mCameraDevice = null;
                        mCameraCaptureSessionHolder = null;
                    }
                }
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onError(cameraDevice, i);
                }

                if (notifyOpenFailed) {
                    mOpenCompleter.setException(new RuntimeException("Failed to "
                            + "open camera device due to error code: " + i));
                }
                cameraDevice.close();

            }
        }

        /**
         * Blocks until the camera device has been closed.
         */
        void close() throws ExecutionException, InterruptedException, TimeoutException {
            CameraDevice cameraDevice;
            synchronized (mLock) {
                cameraDevice = mCameraDevice;
                mCameraDevice = null;
                mCameraCaptureSessionHolder = null;
            }

            if (cameraDevice != null) {
                cameraDevice.close();
            }

            mCloseFuture.get(10L, TimeUnit.SECONDS);
        }

        /**
         * Returns a ListenableFuture representing the closed state.
         */
        @NonNull
        public ListenableFuture<Void> getClosedFuture() {
            return Futures.nonCancellationPropagating(mCloseFuture);
        }

        /**
         * Returns the camera device if it opened successfully and has not been closed.
         */
        @Nullable
        public CameraDevice get() {
            synchronized (mLock) {
                return mCameraDevice;
            }
        }

        /**
         * Create a {@link CameraCaptureSession} by the hold CameraDevice
         *
         * @param surfaces the surfaces used to create CameraCaptureSession
         * @return the CameraCaptureSession holder
         */
        @NonNull
        public CameraCaptureSessionHolder createCaptureSession(@NonNull List<Surface> surfaces)
                throws ExecutionException, InterruptedException, TimeoutException {
            synchronized (mLock) {
                Preconditions.checkState(mCameraDevice != null, "Camera is closed.");
            }
            if (mCameraCaptureSessionHolder != null) {
                mCameraCaptureSessionHolder.close();
                mCameraCaptureSessionHolder = null;
            }
            mCameraCaptureSessionHolder = new CameraCaptureSessionHolder(this, surfaces, null);
            return mCameraCaptureSessionHolder;
        }
    }

    /**
     * A container class used to hold a {@link CameraCaptureSession}.
     *
     * <p>This class contains a valid {@link CameraCaptureSession} that can be retrieved with
     * {@link #get()}, unless the session has been closed.
     *
     * <p>The instance can be obtained via {@link CameraDeviceHolder#createCaptureSession}
     * and will be closed by creating another CameraCaptureSessionHolder. The latest instance will
     * be closed when the associated CameraDeviceHolder is released by
     * {@link CameraUtil#releaseCameraDevice(CameraDeviceHolder)}.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info
    public static class CameraCaptureSessionHolder {

        private final CameraDeviceHolder mCameraDeviceHolder;
        private CameraCaptureSession mCameraCaptureSession;
        private ListenableFuture<Void> mCloseFuture;

        CameraCaptureSessionHolder(@NonNull CameraDeviceHolder cameraDeviceHolder,
                @NonNull List<Surface> surfaces,
                @Nullable CameraCaptureSession.StateCallback stateCallback
        ) throws ExecutionException, InterruptedException, TimeoutException {
            mCameraDeviceHolder = cameraDeviceHolder;
            CameraDevice cameraDevice = Preconditions.checkNotNull(cameraDeviceHolder.get());
            ListenableFuture<CameraCaptureSession> openFuture = openCaptureSession(cameraDevice,
                    surfaces, stateCallback, cameraDeviceHolder.mHandler);

            mCameraCaptureSession = openFuture.get(5, TimeUnit.SECONDS);
        }

        @SuppressWarnings("deprecation")
        @NonNull
        private ListenableFuture<CameraCaptureSession> openCaptureSession(
                @NonNull CameraDevice cameraDevice,
                @NonNull List<Surface> surfaces,
                @Nullable CameraCaptureSession.StateCallback stateCallback,
                @NonNull Handler handler) {
            return CallbackToFutureAdapter.getFuture(
                    openCompleter -> {
                        mCloseFuture = CallbackToFutureAdapter.getFuture(
                                closeCompleter -> {
                                    cameraDevice.createCaptureSession(surfaces,
                                            new SessionStateCallbackImpl(
                                                    openCompleter, closeCompleter, stateCallback),
                                            handler);
                                    return "Close CameraCaptureSession";
                                });
                        return "Open CameraCaptureSession";
                    });
        }

        void close() throws ExecutionException, InterruptedException, TimeoutException {
            if (mCameraCaptureSession != null) {
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            mCloseFuture.get(10L, TimeUnit.SECONDS);
        }

        /**
         * A simplified method to start a repeating capture request.
         *
         * <p>For advance usage, use {@link #get} to obtain the CameraCaptureSession and then issue
         * repeating request.
         *
         * @param template one of the {@link CameraDevice} template.
         * @param surfaces the surfaces add to the repeating request
         * @param captureParams the pairs of {@link CaptureRequest.Key} and value
         * @param captureCallback the capture callback
         * @throws CameraAccessException if fail to issue the request
         */
        @SuppressWarnings("unchecked") // Cast to CaptureRequest.Key<Object>
        public void startRepeating(int template, @NonNull List<Surface> surfaces,
                @Nullable Map<CaptureRequest.Key<?>, Object> captureParams,
                @Nullable CameraCaptureSession.CaptureCallback captureCallback)
                throws CameraAccessException {
            checkSessionOrThrow();
            CameraDevice cameraDevice = mCameraDeviceHolder.get();
            Preconditions.checkState(cameraDevice != null, "CameraDevice is closed.");
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(template);
            for (Surface surface : surfaces) {
                builder.addTarget(surface);
            }
            if (captureParams != null) {
                for (Map.Entry<CaptureRequest.Key<?>, Object> entry : captureParams.entrySet()) {
                    builder.set((CaptureRequest.Key<Object>) entry.getKey(), entry.getValue());
                }
            }
            mCameraCaptureSession.setRepeatingRequest(builder.build(), captureCallback,
                    mCameraDeviceHolder.mHandler);
        }

        /**
         * Returns the camera capture session if it opened successfully and has not been closed.
         *
         * @throws IllegalStateException if the camera capture session is closed
         */
        @NonNull
        public CameraCaptureSession get() {
            checkSessionOrThrow();
            return mCameraCaptureSession;
        }

        private void checkSessionOrThrow() {
            Preconditions.checkState(mCameraCaptureSession != null,
                    "CameraCaptureSession is closed");
        }

        @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info
        private static class SessionStateCallbackImpl extends
                CameraCaptureSession.StateCallback {
            private final Completer<CameraCaptureSession> mOpenCompleter;
            private final CallbackToFutureAdapter.Completer<Void> mCloseCompleter;
            @Nullable
            private final CameraCaptureSession.StateCallback mExtraStateCallback;

            SessionStateCallbackImpl(
                    @NonNull Completer<CameraCaptureSession> openCompleter,
                    @NonNull Completer<Void> closeCompleter,
                    @Nullable CameraCaptureSession.StateCallback extraStateCallback) {
                mOpenCompleter = openCompleter;
                mCloseCompleter = closeCompleter;
                mExtraStateCallback = extraStateCallback;
            }

            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onConfigured(cameraCaptureSession);
                }
                mOpenCompleter.set(cameraCaptureSession);
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onConfigureFailed(cameraCaptureSession);
                }
                mOpenCompleter.setException(new RuntimeException("Failed to "
                        + "open CameraCaptureSession"));
                mCloseCompleter.set(null);
            }

            @Override
            public void onClosed(@NonNull CameraCaptureSession session) {
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onClosed(session);
                }
                mCloseCompleter.set(null);
            }
        }
    }

    /**
     * Cleans up resources that need to be kept around while the camera device is active.
     *
     * @param cameraDeviceHolder camera that was obtained via
     *                           {@link #getCameraDevice(CameraDevice.StateCallback)}
     */
    public static void releaseCameraDevice(@NonNull CameraDeviceHolder cameraDeviceHolder)
            throws ExecutionException, InterruptedException, TimeoutException {
        cameraDeviceHolder.close();
    }

    @NonNull
    public static CameraManager getCameraManager() {
        return (CameraManager)
                ApplicationProvider.getApplicationContext()
                        .getSystemService(Context.CAMERA_SERVICE);
    }


    /**
     * Creates the CameraUseCaseAdapter that would be created with the given CameraSelector.
     *
     * <p>This requires that {@link CameraXUtil#initialize(Context, CameraXConfig)} has been called
     * to properly initialize the cameras. {@link CameraXUtil#shutdown()} also needs to be
     * properly called by the caller class to release the created {@link CameraX} instance.
     *
     * <p>A new CameraUseCaseAdapter instance will be created every time this method is called.
     * UseCases previously attached to CameraUseCasesAdapters returned by this method or
     * {@link #createCameraAndAttachUseCase(Context, CameraSelector, UseCase...)}
     * will not be attached to the new CameraUseCaseAdapter returned by this method.
     *
     * @param context        The context used to initialize CameraX
     * @param cameraCoordinator The camera coordinator for concurrent cameras.
     * @param cameraSelector The selector to select cameras with.
     */
    @VisibleForTesting
    @NonNull
    public static CameraUseCaseAdapter createCameraUseCaseAdapter(
            @NonNull Context context,
            @NonNull CameraCoordinator cameraCoordinator,
            @NonNull CameraSelector cameraSelector) {
        try {
            CameraX cameraX = CameraXUtil.getOrCreateInstance(context, null).get(5000,
                    TimeUnit.MILLISECONDS);
            LinkedHashSet<CameraInternal> cameras =
                    cameraSelector.filter(cameraX.getCameraRepository().getCameras());
            return new CameraUseCaseAdapter(cameras,
                    cameraCoordinator,
                    cameraX.getCameraDeviceSurfaceManager(),
                    cameraX.getDefaultConfigFactory());
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException("Unable to retrieve CameraX instance");
        }
    }

    /**
     * Creates the CameraUseCaseAdapter that would be created with the given CameraSelector.
     *
     * <p>This requires that {@link CameraXUtil#initialize(Context, CameraXConfig)} has been called
     * to properly initialize the cameras. {@link CameraXUtil#shutdown()} also needs to be
     * properly called by the caller class to release the created {@link CameraX} instance.
     *
     * <p>A new CameraUseCaseAdapter instance will be created every time this method is called.
     * UseCases previously attached to CameraUseCasesAdapters returned by this method or
     * {@link #createCameraAndAttachUseCase(Context, CameraSelector, UseCase...)}
     * will not be attached to the new CameraUseCaseAdapter returned by this method.
     *
     * @param context        The context used to initialize CameraX
     * @param cameraSelector The selector to select cameras with.
     */
    @VisibleForTesting
    @NonNull
    public static CameraUseCaseAdapter createCameraUseCaseAdapter(
            @NonNull Context context,
            @NonNull CameraSelector cameraSelector) {
        return createCameraUseCaseAdapter(context, new FakeCameraCoordinator(), cameraSelector);
    }

    /**
     * Creates the CameraUseCaseAdapter that would be created with the given CameraSelector and
     * attaches the UseCases.
     *
     * <p>This requires that {@link CameraXUtil#initialize(Context, CameraXConfig)} has been called
     * to properly initialize the cameras. {@link CameraXUtil#shutdown()} also needs to be
     * properly called by the caller class to release the created {@link CameraX} instance.
     *
     * <p>A new CameraUseCaseAdapter instance will be created every time this method is called.
     * UseCases previously attached to CameraUseCasesAdapters returned by this method or
     * {@link #createCameraUseCaseAdapter(Context, CameraSelector)} will not be
     * attached to the new CameraUseCaseAdapter returned by this method.
     *
     * @param context        The context used to initialize CameraX
     * @param cameraSelector The selector to select cameras with.
     * @param useCases       The UseCases to attach to the CameraUseCaseAdapter.
     */
    @VisibleForTesting
    @NonNull
    public static CameraUseCaseAdapter createCameraAndAttachUseCase(
            @NonNull Context context,
            @NonNull CameraSelector cameraSelector,
            @NonNull UseCase... useCases) {
        CameraUseCaseAdapter cameraUseCaseAdapter = createCameraUseCaseAdapter(context,
                cameraSelector);

        // TODO(b/160249108) move off of main thread once UseCases can be attached on any
        //  thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                cameraUseCaseAdapter.addUseCases(Arrays.asList(useCases));
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException("Unable to attach use cases to camera.", e);
            }
        });

        return cameraUseCaseAdapter;
    }

    /**
     * Check if there is any camera in the device.
     *
     * <p>If there is no camera in the device, most tests will failed.
     *
     * @return false if no camera
     */
    @SuppressWarnings("deprecation")
    public static boolean deviceHasCamera() {
        // TODO Think about external camera case,
        //  especially no built in camera but there might be some external camera

        // It also could be checked by PackageManager's hasSystemFeature() with following:
        //     FEATURE_CAMERA, FEATURE_CAMERA_FRONT, FEATURE_CAMERA_ANY.
        // But its needed to consider one case that platform build with camera feature but there is
        // no built in camera or external camera.

        int numberOfCamera = 0;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            try {
                numberOfCamera = getBackwardCompatibleCameraIdListOrThrow().size();
            } catch (IllegalStateException e) {
                Logger.e(LOG_TAG, "Unable to check camera availability.", e);
            }
        } else {
            numberOfCamera = android.hardware.Camera.getNumberOfCameras();
        }

        return numberOfCamera > 0;
    }

    /**
     * Check if the specified lensFacing is supported by the device.
     *
     * @param lensFacing The desired camera lensFacing.
     * @return True if the device supports the lensFacing.
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    public static boolean hasCameraWithLensFacing(@CameraSelector.LensFacing int lensFacing) {
        return getCameraCharacteristics(lensFacing) != null;
    }

    /**
     * Check if the aspect ratio needs to be corrected.
     *
     * @param lensFacing The desired camera lensFacing.
     * @return True if the aspect ratio has been corrected.
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    public static boolean requiresCorrectedAspectRatio(@CameraSelector.LensFacing int lensFacing) {
        Integer hardwareLevelValue;
        CameraCharacteristics cameraCharacteristics = getCameraCharacteristics(lensFacing);
        if (cameraCharacteristics == null) {
            return false;
        }
        hardwareLevelValue = cameraCharacteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        // There is a bug because of a flipped scaling factor in the intermediate texture
        // transform matrix, and it was fixed in L MR1. If the device is LEGACY + Android 5.0,
        // then auto resolution will return the same aspect ratio as maximum JPEG resolution.
        return (Build.VERSION.SDK_INT == 21 && hardwareLevelValue != null && hardwareLevelValue
                == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
    }

    /**
     * Gets the camera id of the first camera with the given lensFacing.
     *
     * @param lensFacing The desired camera lensFacing.
     * @return Camera id of the first camera with the given lensFacing, null if there's no camera
     * has the lensFacing.
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    @Nullable
    public static String getCameraIdWithLensFacing(@CameraSelector.LensFacing int lensFacing) {
        @SupportedLensFacingInt
        int lensFacingInteger = getLensFacingIntFromEnum(lensFacing);
        for (String cameraId : getBackwardCompatibleCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing != null && cameraLensFacing == lensFacingInteger) {
                return cameraId;
            }
        }
        return null;
    }

    /**
     * Checks if the device has a flash unit with the specified lensFacing.
     *
     * @param lensFacing The desired camera lensFacing.
     * @return True if the device has flash unit with the specified LensFacing.
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    public static boolean hasFlashUnitWithLensFacing(@CameraSelector.LensFacing int lensFacing) {
        @SupportedLensFacingInt
        int lensFacingInteger = getLensFacingIntFromEnum(lensFacing);
        for (String cameraId : getBackwardCompatibleCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing == null || cameraLensFacing != lensFacingInteger) {
                continue;
            }
            Boolean hasFlashUnit = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (hasFlashUnit != null && hasFlashUnit) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the {@link CameraCharacteristics} by specified lens facing if possible.
     *
     * @return the camera characteristics for the given lens facing or {@code null} if it can't
     * be retrieved.
     */
    @Nullable
    public static CameraCharacteristics getCameraCharacteristics(
            @CameraSelector.LensFacing int lensFacing) {
        @SupportedLensFacingInt
        int lensFacingInteger = getLensFacingIntFromEnum(lensFacing);
        for (String cameraId : getBackwardCompatibleCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing != null && cameraLensFacing == lensFacingInteger) {
                return characteristics;
            }
        }
        return null;
    }

    /**
     * The current lens facing directions supported by CameraX, as defined the
     * {@link CameraMetadata}.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CameraMetadata.LENS_FACING_FRONT, CameraMetadata.LENS_FACING_BACK})
    @interface SupportedLensFacingInt {
    }


    /**
     * Converts a lens facing direction from a {@link CameraMetadata} integer to a lensFacing.
     *
     * @param lensFacingInteger The lens facing integer, as defined in {@link CameraMetadata}.
     * @return The lens facing enum.
     */
    @CameraSelector.LensFacing
    public static int getLensFacingEnumFromInt(
            @SupportedLensFacingInt int lensFacingInteger) {
        switch (lensFacingInteger) {
            case CameraMetadata.LENS_FACING_BACK:
                return CameraSelector.LENS_FACING_BACK;
            case CameraMetadata.LENS_FACING_FRONT:
                return CameraSelector.LENS_FACING_FRONT;
            default:
                throw new IllegalArgumentException(
                        "Unsupported lens facing integer: " + lensFacingInteger);
        }
    }

    /**
     * Gets if the sensor orientation of the given lens facing.
     *
     * @param lensFacing The desired camera lensFacing.
     * @return The sensor orientation degrees, or null if it's undefined.
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    @Nullable
    public static Integer getSensorOrientation(@CameraSelector.LensFacing int lensFacing) {
        @SupportedLensFacingInt
        int lensFacingInteger = getLensFacingIntFromEnum(lensFacing);
        for (String cameraId : getBackwardCompatibleCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing == null || cameraLensFacing != lensFacingInteger) {
                continue;
            }
            return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        }
        return null;
    }

    /**
     * Gets the camera id list or throw exception if the CAMERA permission is not currently granted.
     *
     * @return the camera id list
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    @NonNull
    public static List<String> getBackwardCompatibleCameraIdListOrThrow() {
        try {
            List<String> backwardCompatibleCameraIdList = new ArrayList<>();

            for (String cameraId : getCameraManager().getCameraIdList()) {
                int[] capabilities = getCameraCharacteristicsOrThrow(cameraId).get(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

                if (capabilities == null) {
                    continue;
                }

                for (int capability : capabilities) {
                    if (capability == REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) {
                        backwardCompatibleCameraIdList.add(cameraId);
                        break;
                    }
                }
            }

            return backwardCompatibleCameraIdList;
        } catch (CameraAccessException e) {
            throw new IllegalStateException("Unable to retrieve list of cameras on device.", e);
        }
    }

    /**
     * Converts a lens facing direction from a lensFacing to a {@link CameraMetadata} integer.
     *
     * @param lensFacing The lens facing enum, as defined in {@link CameraSelector}.
     * @return The lens facing integer.
     */
    @SupportedLensFacingInt
    private static int getLensFacingIntFromEnum(@CameraSelector.LensFacing int lensFacing) {
        switch (lensFacing) {
            case CameraSelector.LENS_FACING_BACK:
                return CameraMetadata.LENS_FACING_BACK;
            case CameraSelector.LENS_FACING_FRONT:
                return CameraMetadata.LENS_FACING_FRONT;
            default:
                throw new IllegalArgumentException("Unsupported lens facing enum: " + lensFacing);
        }
    }

    /**
     * Gets the {@link CameraCharacteristics} by specified camera id or throw exception if the
     * CAMERA permission is not currently granted.
     *
     * @return the camera id list
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    @NonNull
    private static CameraCharacteristics getCameraCharacteristicsOrThrow(@NonNull String cameraId) {
        try {
            return getCameraManager().getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            throw new IllegalStateException(
                    "Unable to retrieve info for camera with id " + cameraId + ".", e);
        }
    }

    /**
     * Check if the resource sufficient to recording a video.
     */
    @NonNull
    public static TestRule checkVideoRecordingResource() {
        return RuleChain.outerRule((base, description) -> new Statement() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void evaluate() throws Throwable {
                // The default resolution in VideoCapture is 1080P.
                assumeTrue(checkVideoRecordingResource(CamcorderProfile.QUALITY_1080P));
                base.evaluate();
            }
        });
    }

    /**
     * Check resource for video recording.
     *
     * <p> Tries to configure an video encoder to ensure current resource is sufficient to
     * recording a video.
     */
    @SuppressWarnings("deprecation")
    public static boolean checkVideoRecordingResource(int quality) {
        String videoMimeType = "video/avc";
        // Assume the device resource is sufficient.
        boolean checkResult = true;

        if (CamcorderProfile.hasProfile(quality)) {
            CamcorderProfile profile = CamcorderProfile.get(quality);
            MediaFormat format =
                    MediaFormat.createVideoFormat(
                            videoMimeType, profile.videoFrameWidth, profile.videoFrameHeight);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, profile.videoBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, profile.videoFrameRate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            MediaCodec codec = null;

            try {
                codec = MediaCodec.createEncoderByType(videoMimeType);
                codec.configure(
                        format, /*surface*/
                        null, /*crypto*/
                        null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
            } catch (MediaCodec.CodecException e) {
                Logger.i(LOG_TAG,
                        "Video encoder pre-test configured fail CodecException: " + e.getMessage());
                // Skip tests if a video encoder cannot be configured successfully.
                checkResult = false;
            } catch (IOException | IllegalArgumentException | IllegalStateException e) {
                Logger.i(LOG_TAG, "Video encoder pre-test configured fail: " + e.getMessage());
                checkResult = false;
            } finally {
                Logger.i(LOG_TAG, "codec.release()");
                if (codec != null) {
                    codec.release();
                }
            }
        }

        return checkResult;
    }

    /**
     * Retrieves the max high resolution output size if the camera has high resolution output sizes
     * with the specified lensFacing.
     *
     * @param lensFacing The desired camera lensFacing.
     * @return the max high resolution output size if the camera has high resolution output sizes
     * with the specified LensFacing. Returns null otherwise.
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    @Nullable
    public static Size getMaxHighResolutionOutputSizeWithLensFacing(
            @CameraSelector.LensFacing int lensFacing, int imageFormat) {
        @SupportedLensFacingInt
        int lensFacingInteger = getLensFacingIntFromEnum(lensFacing);
        for (String cameraId : getBackwardCompatibleCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing == null || cameraLensFacing != lensFacingInteger) {
                continue;
            }

            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @SuppressLint("ClassVerificationFailure")
                Size[] highResolutionOutputSizes = map.getHighResolutionOutputSizes(imageFormat);

                if (highResolutionOutputSizes == null || Arrays.asList(
                        highResolutionOutputSizes).isEmpty()) {
                    return null;
                }

                Arrays.sort(highResolutionOutputSizes, new CompareSizesByArea(true));
                return highResolutionOutputSizes[0];
            }
        }
        return null;
    }

    /**
     * Grant the camera permission and test the camera.
     *
     * <p>It will
     * (1) Grant the camera permission.
     * (2) Check if there is at least one camera on the device alive. If not, it will ignore
     * the test.
     * (3) Ensure the camera can be opened successfully before the test. If not, it will ignore
     * the test.
     */
    @NonNull
    public static TestRule grantCameraPermissionAndPreTest() {
        return grantCameraPermissionAndPreTest(new PreTestCamera(), new PreTestCameraIdList());
    }

    /**
     * Grant the camera permission and test the camera.
     *
     * <p>This method is mainly required to be used when running the test with
     * Camera2Config/CameraPipeConfig. Please create a PreTestCameraIdList with the CameraXConfig
     * that is used in the test.
     * If the test uses fake CameraXConfig or doesn't initialize CameraX, i.e. doesn't uses
     * {@link androidx.camera.lifecycle.ProcessCameraProvider} or {@link CameraXUtil#initialize} to
     * initialize CameraX for testing, you can use
     * {@link CameraUtil#grantCameraPermissionAndPreTest()} instead.
     */
    @NonNull
    public static TestRule grantCameraPermissionAndPreTest(
            @Nullable PreTestCameraIdList cameraIdListTestRule) {
        return grantCameraPermissionAndPreTest(new PreTestCamera(), cameraIdListTestRule);
    }

    /**
     * Grant the camera permission and test the camera.
     *
     * @param cameraTestRule       to check if camera can be opened.
     * @param cameraIdListTestRule to check if camera characteristic reports correct information
     *                             that includes the supported camera devices that shows in the
     *                             system.
     */
    @NonNull
    public static TestRule grantCameraPermissionAndPreTest(@Nullable PreTestCamera cameraTestRule,
            @Nullable PreTestCameraIdList cameraIdListTestRule) {
        RuleChain rule = RuleChain.outerRule(GrantPermissionRule.grant(Manifest.permission.CAMERA));
        rule = rule.around(new IgnoreProblematicDeviceRule());
        if (cameraIdListTestRule != null) {
            rule = rule.around(cameraIdListTestRule);
        }
        if (cameraTestRule != null) {
            rule = rule.around(cameraTestRule);
        }
        rule = rule.around((base, description) -> new Statement() {
            @Override
            public void evaluate() throws Throwable {
                assumeTrue(deviceHasCamera());
                base.evaluate();
            }
        });

        return rule;
    }

    /**
     * Test the camera device
     *
     * <p>Try to open the camera with the front and back lensFacing. It throws an exception when
     * the test is running in the CameraX lab, or ignore the test otherwise.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static class PreTestCamera implements TestRule {
        final boolean mThrowOnError = Log.isLoggable(PRETEST_CAMERA_TAG, Log.DEBUG);
        final AtomicReference<Boolean> mCanOpenCamera = new AtomicReference<>();

        @NonNull
        @Override
        public Statement apply(@NonNull Statement base, @NonNull Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (mCanOpenCamera.get() == null) {
                        boolean backStatus = true;
                        if (hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK)) {
                            RetryCameraOpener opener =
                                    new RetryCameraOpener(CameraSelector.LENS_FACING_BACK);
                            backStatus = opener.openWithRetry(5, 5000);
                            opener.shutdown();
                        }

                        boolean frontStatus = true;
                        if (hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
                            RetryCameraOpener opener =
                                    new RetryCameraOpener(CameraSelector.LENS_FACING_FRONT);
                            frontStatus = opener.openWithRetry(5, 5000);
                            opener.shutdown();
                        }
                        Logger.d(LOG_TAG,
                                "PreTest Open camera result " + backStatus + " " + frontStatus);
                        mCanOpenCamera.set(backStatus && frontStatus);
                    }

                    if (Boolean.TRUE.equals(mCanOpenCamera.get())) {
                        base.evaluate();
                    } else {
                        if (mThrowOnError) {
                            throw new RuntimeException(
                                    "CameraX_cannot_test_with_failed_camera, model:" + Build.MODEL);
                        }

                        // Ignore the test, throw the AssumptionViolatedException.
                        throw new AssumptionViolatedException("Ignore the test since the camera "
                                + "failed, on test " + description.getDisplayName());
                    }
                }
            };
        }
    }

    /**
     * Try to open the camera, and close it immediately.
     *
     * @param cameraId the id of the camera to test
     * @return true if the camera can be opened successfully
     */
    @SuppressLint("MissingPermission")
    public static boolean tryOpenCamera(@NonNull String cameraId) {
        CameraDeviceHolder deviceHolder = null;
        boolean ret = true;
        try {
            deviceHolder = new CameraDeviceHolder(getCameraManager(), cameraId, null);
            if (deviceHolder.get() == null) {
                ret = false;
            }
        } catch (Exception e) {
            ret = false;
        } finally {
            if (deviceHolder != null) {
                try {
                    releaseCameraDevice(deviceHolder);
                } catch (Exception e) {
                    Logger.e(LOG_TAG, "Cannot close cameraDevice.", e);
                }
            }
        }

        return ret;
    }

    /**
     * Helper to verify the camera can be opened or not.
     *
     * <p>Call {@link #openWithRetry(int, long)} to start the test on the camera.
     *
     * <p>Call {@link #shutdown()} after finish the test.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static class RetryCameraOpener {
        private static final int RETRY_DELAY_MS = 1000;
        private CameraAvailability mCameraAvailability;
        private HandlerThread mHandlerThread;
        @Nullable
        private String mCameraId;

        /**
         * @param lensFacing The camera lens facing to be tested.
         */
        public RetryCameraOpener(@CameraSelector.LensFacing int lensFacing) {
            mCameraId = getCameraIdWithLensFacing(lensFacing);
            Logger.d(LOG_TAG,
                    "PreTest init Camera lensFacing: " + lensFacing + " id: " + mCameraId);
            if (mCameraId == null) {
                return;
            }
            mCameraAvailability = new CameraAvailability(mCameraId);
            mHandlerThread = new HandlerThread(String.format("CameraThread-%s", mCameraId));
            mHandlerThread.start();

            getCameraManager().registerAvailabilityCallback(mCameraAvailability,
                    new Handler(mHandlerThread.getLooper()));
        }

        /**
         * Test to open the camera
         *
         * @param retryCount        the retry count when it cannot open camera.
         * @param waitCameraTimeout the time to wait if camera unavailable. In milliseconds.
         * @return true if camera can be opened, otherwise false.
         */
        public boolean openWithRetry(int retryCount, long waitCameraTimeout) {
            if (mCameraId == null) {
                return false;
            }

            // Try to open the camera at the first time and we can grab the camera from the lower
            // priority user.
            for (int i = 0; i < retryCount; i++) {
                if (tryOpenCamera(mCameraId)) {
                    return true;
                }
                Logger.d(LOG_TAG,
                        "Cannot open camera with camera id: " + mCameraId + " retry:" + i);
                if (!waitForCameraAvailable(waitCameraTimeout)) {
                    return false;
                }

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
            return false;
        }

        private boolean waitForCameraAvailable(long waitCameraTimeout) {
            try {
                mCameraAvailability.observeAvailable().get(waitCameraTimeout,
                        TimeUnit.MILLISECONDS);
                return true;
            } catch (Exception e) {
                Logger.e(LOG_TAG, "Wait for camera available timeout camera id:" + mCameraId);
                return false;
            }
        }

        /**
         * Close the opener and release resource.
         */
        public void shutdown() {
            if (mCameraId == null) {
                return;
            }
            mCameraId = null;
            getCameraManager().unregisterAvailabilityCallback(mCameraAvailability);
            mHandlerThread.quitSafely();
        }

        static class CameraAvailability extends CameraManager.AvailabilityCallback {
            private final Object mLock = new Object();
            private final String mCameraId;

            @GuardedBy("mLock")
            private boolean mCameraAvailable = false;
            @GuardedBy("mLock")
            private CallbackToFutureAdapter.Completer<Void> mCompleter;

            CameraAvailability(@NonNull String cameraId) {
                mCameraId = cameraId;
            }

            ListenableFuture<Void> observeAvailable() {
                synchronized (mLock) {
                    if (mCameraAvailable) {
                        return Futures.immediateFuture(null);
                    }
                    return CallbackToFutureAdapter.getFuture(
                            completer -> {
                                synchronized (mLock) {
                                    if (mCompleter != null) {
                                        mCompleter.setCancelled();
                                    }
                                    mCompleter = completer;
                                }
                                return "observeCameraAvailable_" + mCameraId;
                            });
                }
            }

            @Override
            public void onCameraAvailable(@NonNull String cameraId) {
                Logger.d(LOG_TAG, "Camera id " + cameraId + " onCameraAvailable callback");
                if (!mCameraId.equals(cameraId)) {
                    // Ignore availability for other cameras
                    return;
                }

                synchronized (mLock) {
                    Logger.d(LOG_TAG, "Camera id " + mCameraId + " onCameraAvailable");
                    mCameraAvailable = true;
                    if (mCompleter != null) {
                        mCompleter.set(null);
                    }
                }
            }

            @Override
            public void onCameraUnavailable(@NonNull String cameraId) {
                if (!mCameraId.equals(cameraId)) {
                    // Ignore availability for other cameras
                    return;
                }
                synchronized (mLock) {
                    Logger.d(LOG_TAG, "Camera id " + mCameraId + " onCameraUnavailable");
                    mCameraAvailable = false;
                }
            }
        }
    }

    /**
     * Check the camera lensFacing info is reported correctly
     *
     * <p>For b/167201193
     *
     * <P>Verify the lensFacing info is available in the CameraCharacteristic, or initialize
     * CameraX with the CameraXConfig if it is provided. Throws an exception to interrupt the
     * test if it detects incorrect info, or throws AssumptionViolatedException when it is not in
     * the CameraX lab.
     */
    public static class PreTestCameraIdList implements TestRule {
        final boolean mThrowOnError = Log.isLoggable("CameraXDumpIdList", Log.DEBUG);
        final AtomicReference<Boolean> mCameraIdListCorrect = new AtomicReference<>();

        @Nullable
        final CameraXConfig mCameraXConfig;

        public PreTestCameraIdList() {
            mCameraXConfig = null;
        }

        /**
         * Try to use the {@link CameraXConfig} to initialize CameraX when it fails to detect the
         * valid lens facing info from camera characteristics.
         */
        public PreTestCameraIdList(@NonNull CameraXConfig config) {
            mCameraXConfig = config;
        }

        @NonNull
        @Override
        public Statement apply(@NonNull Statement base, @NonNull Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (mCameraIdListCorrect.get() == null) {
                        if (isCameraLensFacingInfoAvailable()) {
                            mCameraIdListCorrect.set(true);
                        } else {
                            mCameraIdListCorrect.set(false);
                        }

                        // Always try to initialize CameraX if the CameraXConfig has been set.
                        if (mCameraXConfig != null) {
                            if (checkLensFacingByCameraXConfig(
                                    ApplicationProvider.getApplicationContext(), mCameraXConfig)) {
                                mCameraIdListCorrect.set(true);
                            } else {
                                mCameraIdListCorrect.set(false);
                            }
                        }
                    }

                    if (Boolean.TRUE.equals(mCameraIdListCorrect.get())) {
                        base.evaluate();
                    } else {
                        if (mThrowOnError) {
                            throw new IllegalArgumentException(
                                    "CameraIdList_incorrect:" + Build.MODEL);
                        }

                        // Ignore the test, throw the AssumptionViolatedException.
                        throw new AssumptionViolatedException("Ignore the test since the camera "
                                + "id list failed, on test " + description.getDisplayName());
                    }
                }
            };
        }
    }

    static boolean checkLensFacingByCameraXConfig(@NonNull Context context,
            @NonNull CameraXConfig config) {
        try {
            // Shutdown exist instances, if there is any
            CameraXUtil.shutdown().get(10, TimeUnit.SECONDS);

            CameraXUtil.initialize(context, config).get(10, TimeUnit.SECONDS);
            CameraX camerax = CameraXUtil.getOrCreateInstance(context, null).get(5,
                    TimeUnit.SECONDS);
            LinkedHashSet<CameraInternal> cameras = camerax.getCameraRepository().getCameras();

            PackageManager pm = context.getPackageManager();
            boolean backFeature = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
            boolean frontFeature = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
            if (backFeature) {
                CameraSelector.DEFAULT_BACK_CAMERA.select(cameras);
            }
            if (frontFeature) {
                CameraSelector.DEFAULT_FRONT_CAMERA.select(cameras);
            }
            Logger.i(LOG_TAG, "Successfully init CameraX");
            return true;
        } catch (Exception e) {
            Logger.w(LOG_TAG, "CameraX init fail", e);
        } finally {
            try {
                CameraXUtil.shutdown().get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Ignore all exceptions in the shutdown process.
            }
        }
        return false;
    }

    /**
     * Check the camera lensFacing info for b/167201193 debug.
     *
     * @return true if the front and main camera info exists in the camera characteristic.
     */
    @SuppressWarnings("ObjectToString")
    static boolean isCameraLensFacingInfoAvailable() {
        boolean error = false;
        Context context = ApplicationProvider.getApplicationContext();
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        String[] cameraIds = new String[0];
        try {
            cameraIds = manager.getCameraIdList();
            Logger.d(LOG_TAG, "ids: " + Arrays.toString(cameraIds));
        } catch (CameraAccessException e) {
            error = true;
            Logger.e(LOG_TAG, "Cannot find default camera id");
        }

        if (cameraIds != null && cameraIds.length > 0) {
            boolean hasFront = false;
            boolean hasBack = false;
            for (String id : cameraIds) {
                Logger.d(LOG_TAG, "++ Camera id: " + id);
                try {
                    CameraCharacteristics c = manager.getCameraCharacteristics(id);
                    Logger.d(LOG_TAG, id + " character: " + c);
                    if (c != null) {
                        Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                        Logger.d(LOG_TAG, id + " lensFacing: " + lensFacing);
                        if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                            hasBack = true;
                        }
                        if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                            hasFront = true;
                        }
                    }
                } catch (Throwable t) {
                    Logger.d(LOG_TAG, id + ", failed to get CameraCharacteristics", t);
                }
                Logger.d(LOG_TAG, "-- Camera id: " + id);
            }

            PackageManager pm = context.getPackageManager();
            boolean backFeature = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
            boolean frontFeature = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);

            // Pass when no such feature or it gets the camera from the camera characteristic.
            boolean backPass = !backFeature || hasBack;
            boolean frontPass = !frontFeature || hasFront;

            if (!backPass || !frontPass) {
                error = true;
                Logger.e(LOG_TAG,
                        "Missing front or back camera, has front camera: " + hasFront + ", has "
                                + "back camera: " + hasBack + " has main camera feature:"
                                + backFeature + " has front camera feature:" + frontFeature
                                + " ids: " + Arrays.toString(cameraIds));
            }
        } else {
            error = true;
            Logger.e(LOG_TAG, "cameraIds.length is zero");
        }

        return !error;
    }
}