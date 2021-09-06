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

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.Logger;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter;
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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
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
        List<String> cameraIds = getCameraIdListOrThrow();
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
     * @throws CameraAccessException if the device is unable to access the camera
     * @throws InterruptedException  if a {@link CameraDevice} can not be retrieved within a set
     *                               time
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    @NonNull
    public static CameraDeviceHolder getCameraDevice(
            @NonNull String cameraId,
            @Nullable CameraDevice.StateCallback stateCallback)
            throws CameraAccessException, InterruptedException, TimeoutException,
            ExecutionException {
        return new CameraDeviceHolder(getCameraManager(), cameraId, stateCallback);
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
        private ListenableFuture<Void> mCloseFuture;

        @RequiresPermission(Manifest.permission.CAMERA)
        CameraDeviceHolder(@NonNull CameraManager cameraManager, @NonNull String cameraId,
                @Nullable CameraDevice.StateCallback stateCallback)
                throws InterruptedException, ExecutionException, TimeoutException {
            mHandlerThread = new HandlerThread(String.format("CameraThread-%s", cameraId));
            mHandlerThread.start();

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
                                    extraStateCallback), new Handler(mHandlerThread.getLooper()));
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
            }

            if (cameraDevice != null) {
                cameraDevice.close();
            }

            mCloseFuture.get(10L, TimeUnit.SECONDS);
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
     * <p> This requires that {@link CameraX#initialize(Context, CameraXConfig)} has been called
     * to properly initialize the cameras.
     *
     * <p>A new CameraUseCaseAdapter instance will be created every time this method is called.
     * UseCases previously attached to CameraUseCasesAdapters returned by this method or
     * {@link #createCameraAndAttachUseCase(Context, CameraSelector, UseCase...)} will not be
     * attached to the new CameraUseCaseAdapter returned by this method.
     *
     * @param context        The context used to initialize CameraX
     * @param cameraSelector The selector to select cameras with.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.TESTS)
    @NonNull
    public static CameraUseCaseAdapter createCameraUseCaseAdapter(@NonNull Context context,
            @NonNull CameraSelector cameraSelector) {
        try {
            CameraX cameraX = CameraX.getOrCreateInstance(context, null).get(5000,
                    TimeUnit.MILLISECONDS);
            LinkedHashSet<CameraInternal> cameras =
                    cameraSelector.filter(cameraX.getCameraRepository().getCameras());
            return new CameraUseCaseAdapter(cameras,
                    cameraX.getCameraDeviceSurfaceManager(), cameraX.getDefaultConfigFactory());
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException("Unable to retrieve CameraX instance");
        }
    }

    /**
     * Creates the CameraUseCaseAdapter that would be created with the given CameraSelector and
     * attaches the UseCases.
     *
     * <p> This requires that {@link CameraX#initialize(Context, CameraXConfig)} has been called
     * to properly initialize the cameras.
     *
     * <p>A new CameraUseCaseAdapter instance will be created every time this method is called.
     * UseCases previously attached to CameraUseCasesAdapters returned by this method or
     * {@link #createCameraUseCaseAdapter(Context, CameraSelector)} will not be
     * attached to the new CameraUseCaseAdapter returned by this method.
     *
     * @param context        The context used to initialize CameraX
     * @param cameraSelector The selector to select cameras with.
     * @param useCases       The UseCases to attach to the CameraUseCaseAdapter.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.TESTS)
    @NonNull
    public static CameraUseCaseAdapter createCameraAndAttachUseCase(@NonNull Context context,
            @NonNull CameraSelector cameraSelector, @NonNull UseCase... useCases) {
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
                numberOfCamera = getCameraIdListOrThrow().size();
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
        for (String cameraId : getCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing != null && cameraLensFacing.intValue() == lensFacingInteger) {
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
        for (String cameraId : getCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing == null || cameraLensFacing.intValue() != lensFacingInteger) {
                continue;
            }
            Boolean hasFlashUnit = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (hasFlashUnit != null && hasFlashUnit.booleanValue()) {
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
        for (String cameraId : getCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing != null && cameraLensFacing.intValue() == lensFacingInteger) {
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
        for (String cameraId : getCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing == null || cameraLensFacing.intValue() != lensFacingInteger) {
                continue;
            }
            return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        }
        return null;
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
     * Gets the camera id list or throw exception if the CAMERA permission is not currently granted.
     *
     * @return the camera id list
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    private static List<String> getCameraIdListOrThrow() {
        try {
            return Arrays.asList(getCameraManager().getCameraIdList());
        } catch (CameraAccessException e) {
            throw new IllegalStateException("Unable to retrieve list of cameras on device.", e);
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
        RuleChain rule = RuleChain.outerRule((base, description) -> new Statement() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void evaluate() throws Throwable {
                // The default resolution in VideoCapture is 1080P.
                assumeTrue(checkVideoRecordingResource(CamcorderProfile.QUALITY_1080P));
                base.evaluate();
            }
        });

        return rule;
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
                codec.release();
            }
        }

        return checkResult;
    }

    /**
     * Create a chained rule for the test cases that need to use the camera.
     *
     * <p>It will
     * (1) Grant the camera permission.
     * (2) Check if there is at least one camera on the device.
     * (3) Test the camera can be opened successfully.
     *
     * <p>This method will set {@link PreTestCamera} to throw an exception when the camera is
     * unavailable if PRETEST_CAMERA_TAG is loggable at the debug level (see Log#isLoggable).
     */
    @NonNull
    public static TestRule grantCameraPermissionAndPreTest() {
        return grantCameraPermissionAndPreTest(new PreTestCamera());
    }

    /**
     * Grant the camera permission and test the camera.
     *
     * @param cameraTestRule the PreTestCamera rule to execute the camera test.
     */
    @NonNull
    public static TestRule grantCameraPermissionAndPreTest(@NonNull PreTestCamera cameraTestRule) {
        RuleChain rule =
                RuleChain.outerRule(GrantPermissionRule.grant(Manifest.permission.CAMERA)).around(
                        (base, description) -> new Statement() {
                            @Override
                            public void evaluate() throws Throwable {
                                dumpCameraLensFacingInfo();
                                assumeTrue(deviceHasCamera());
                                base.evaluate();
                            }
                        }).around(cameraTestRule);
        return rule;
    }

    /**
     * Pretest the camera device
     *
     * <p>Try to open the camera with the front and back lensFacing. It throws exception when
     * camera is unavailable and mThrowOnError is true.
     *
     * <p>Passing false into the constructor {@link #PreTestCamera(boolean)}
     * will never throw the exception when the camera is unavailable.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static class PreTestCamera implements TestRule {
        final boolean mThrowOnError;
        final AtomicReference<Boolean> mCanOpenCamera = new AtomicReference<>();

        public PreTestCamera() {
            mThrowOnError = Log.isLoggable(PRETEST_CAMERA_TAG, Log.DEBUG);
        }

        public PreTestCamera(boolean throwOnError) {
            mThrowOnError = throwOnError;
        }

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

                    if (mCanOpenCamera.get()) {
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
     * Log the camera lensFacing info for b/167201193 debug.
     * Throw exception with a specific message if the Camera doesn't have both front/back lens
     * facing in the daily testing.
     */
    @SuppressWarnings("ObjectToString")
    static void dumpCameraLensFacingInfo() {
        boolean error = false;

        CameraManager manager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);
        String[] cameraIds = new String[0];
        try {
            cameraIds = manager.getCameraIdList();
            Logger.d(LOG_TAG, "ids: " + Arrays.toString(cameraIds));
        } catch (CameraAccessException e) {
            error = true;
            Logger.e(LOG_TAG, "Cannot find default camera id");
        }

        if (cameraIds != null && cameraIds.length > 0) {
            List<String> ids = Arrays.asList(cameraIds);
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

            if (!ids.contains("0") || !ids.contains("1")) {
                error = true;
                Logger.e(LOG_TAG,
                        "Camera Id 0 or 1 is missing,  ids: " + Arrays.toString(cameraIds));
            }

            if (!hasFront || !hasBack) {
                Logger.e(LOG_TAG,
                        "Missing front or back camera, has front camera: " + hasFront + ", has "
                                + "back camera: " + hasBack);
            }
        } else {
            error = true;
            Logger.e(LOG_TAG, "cameraIds.length is zero");
        }

        if (error && Log.isLoggable("CameraXDumpIdList", Log.DEBUG)) {
            throw new IllegalArgumentException("CameraIdList_incorrect:" + Build.MODEL);
        }

    }
}