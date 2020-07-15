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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Utility functions for obtaining instances of camera2 classes. */
public final class CameraUtil {
    private CameraUtil() {
    }

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
     * A container class used to hold a {@link CameraDevice}.
     *
     * <p>This class should contain a valid {@link CameraDevice} that can be retrieved with
     * {@link #get()}, unless the device has been closed.
     *
     * <p>The camera device should always be closed with
     * {@link CameraUtil#releaseCameraDevice(CameraDeviceHolder)} once finished with the device.
     */
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
                    cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {

                        @Override
                        public void onOpened(@NonNull CameraDevice cameraDevice) {
                            synchronized (mLock) {
                                Preconditions.checkState(mCameraDevice == null, "CameraDevice "
                                        + "should not have been opened yet.");
                                mCameraDevice = cameraDevice;
                            }
                            if (extraStateCallback != null) {
                                extraStateCallback.onOpened(cameraDevice);
                            }
                            openCompleter.set(null);
                        }

                        @Override
                        public void onClosed(@NonNull CameraDevice cameraDevice) {
                            if (extraStateCallback != null) {
                                extraStateCallback.onClosed(cameraDevice);
                            }
                            closeCompleter.set(null);
                            mHandlerThread.quitSafely();
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                            synchronized (mLock) {
                                mCameraDevice = null;
                            }
                            if (extraStateCallback != null) {
                                extraStateCallback.onDisconnected(cameraDevice);
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
                            if (extraStateCallback != null) {
                                extraStateCallback.onError(cameraDevice, i);
                            }

                            if (notifyOpenFailed) {
                                openCompleter.setException(new RuntimeException("Failed to "
                                        + "open camera device due to error code: " + i));
                            }
                            cameraDevice.close();

                        }
                    }, new Handler(mHandlerThread.getLooper()));

                    return "Close[cameraId=" + cameraId + "]";
                });

                return "Open[cameraId=" + cameraId + "]";
            });
        }

        /**
         * Blocks until the camera device has been closed.
         */
        void close() throws ExecutionException, InterruptedException {
            CameraDevice cameraDevice;
            synchronized (mLock) {
                cameraDevice = mCameraDevice;
                mCameraDevice = null;
            }

            if (cameraDevice != null) {
                cameraDevice.close();
            }

            mCloseFuture.get();
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
     * {@link #getCameraDevice(CameraDevice.StateCallback)}
     */
    public static void releaseCameraDevice(@NonNull CameraDeviceHolder cameraDeviceHolder)
            throws ExecutionException, InterruptedException {
        cameraDeviceHolder.close();
    }

    public static CameraManager getCameraManager() {
        return (CameraManager)
                ApplicationProvider.getApplicationContext()
                        .getSystemService(Context.CAMERA_SERVICE);
    }


    /**
     * Retrieves the CameraUseCaseAdapter that would be created with the given CameraSelector.
     *
     * <p> This requires that {@link CameraX#initialize(Context, CameraXConfig)} has been called
     * to properly initialize the cameras.
     *
     * @param context The context used to initialize CameraX
     * @param cameraSelector The selector to select cameras with.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.TESTS)
    public static CameraUseCaseAdapter getCameraUseCaseAdapter(@NonNull Context context,
            @NonNull CameraSelector cameraSelector) {
        try {
            CameraX cameraX = CameraX.getOrCreateInstance(context).get(5000, TimeUnit.MILLISECONDS);
            LinkedHashSet<CameraInternal> cameras =
                    cameraSelector.filter(cameraX.getCameraRepository().getCameras());
            return new CameraUseCaseAdapter(cameras.iterator().next(), cameras,
                    cameraX.getCameraDeviceSurfaceManager());
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException("Unable to retrieve CameraX instance");
        }
    }

    /**
     * Retrieves the CameraUseCaseAdapter that would be created with the given CameraSelector and
     * attaches the UseCases.
     *
     * <p> This requires that {@link CameraX#initialize(Context, CameraXConfig)} has been called
     * to properly initialize the cameras.
     *
     * @param context The context used to initialize CameraX
     * @param cameraSelector The selector to select cameras with.
     * @param useCases The UseCases to attach to the CameraUseCaseAdapter.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.TESTS)
    public static CameraUseCaseAdapter getCameraAndAttachUseCase(@NonNull Context context,
            @NonNull CameraSelector cameraSelector, @NonNull UseCase ... useCases) {
        CameraUseCaseAdapter cameraUseCaseAdapter = getCameraUseCaseAdapter(context,
                cameraSelector);

        // TODO(b/160249108) move off of main thread once UseCases can be attached on any
        //  thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                cameraUseCaseAdapter.addUseCases(Arrays.asList(useCases));
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException("Unable to attach use cases to camera.");
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
                Log.e(CameraUtil.class.getSimpleName(), "Unable to check camera availability.", e);
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
     * Create a chained rule for the test cases that need to use the camera.
     *
     * <p>It will
     * (1) Grant the camera permission.
     * (2) Check if there is at least one camera on the device.
     * (3) Test the camera can be opened successfully.
     *
     * <p>The method will set PreTestCamera throw exception will set when the PRETEST_CAMERA_TAG
     * debug key for Log.isLoggable is enabled.
     */
    @NonNull
    public static RuleChain grantCameraPermissionAndPreTest() {
        return RuleChain.outerRule(GrantPermissionRule.grant(Manifest.permission.CAMERA)).around(
                (base, description) -> new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        assumeTrue(deviceHasCamera());
                        base.evaluate();
                    }
                }).around(
                new CameraUtil.PreTestCamera(Log.isLoggable(PRETEST_CAMERA_TAG, Log.DEBUG)));
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
    public static class PreTestCamera implements TestRule {
        final boolean mThrowOnError;

        public PreTestCamera(boolean throwOnError) {
            mThrowOnError = throwOnError;
        }

        @NonNull
        @Override
        public Statement apply(@NonNull Statement base, @NonNull Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    boolean backStatus = true;
                    if (hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK)) {
                        Log.w(CameraUtil.class.getSimpleName(), "Fail to open the back camera");
                        backStatus = tryOpenCamera(CameraSelector.LENS_FACING_BACK);
                    }

                    boolean frontStatus = true;
                    if (hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
                        Log.w(CameraUtil.class.getSimpleName(), "Fail to open the front camera");
                        frontStatus = tryOpenCamera(CameraSelector.LENS_FACING_FRONT);
                    }
                    boolean canOpenCamera = backStatus && frontStatus;

                    if (canOpenCamera) {
                        base.evaluate();
                    } else {
                        if (mThrowOnError) {
                            throw new RuntimeException(
                                    "CameraX_cannot_test_with_failed_camera, model:" + Build.MODEL);
                        } else {
                            // Ignore the test, so we only print a log without calling
                            Log.w(CameraUtil.class.getSimpleName(),
                                    "Camera fail, on test " + description.getDisplayName());
                            base.evaluate();
                        }
                    }
                }
            };
        }
    }

    /**
     * Try to open the camera, and close it immediately.
     *
     * @param lensFacing the lensFacing of the camera to test
     * @return true if the camera can be opened successfully
     */
    @SuppressLint("MissingPermission")
    public static boolean tryOpenCamera(@CameraSelector.LensFacing int lensFacing) {
        String cameraId = getCameraIdWithLensFacing(lensFacing);

        if (cameraId == null) {
            return false;
        }

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
                    Log.e(CameraUtil.class.getSimpleName(), "Cannot close cameraDevice.", e);
                }
            }
        }

        return ret;
    }
}