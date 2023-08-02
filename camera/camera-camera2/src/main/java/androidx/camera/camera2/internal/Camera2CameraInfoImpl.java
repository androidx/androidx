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

import static android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING;
import static android.hardware.camera2.CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME;
import static android.hardware.camera2.CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN;

import static androidx.camera.camera2.internal.ZslUtil.isCapabilitySupported;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.os.Build;
import android.util.Pair;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.FloatRange;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.internal.compat.StreamConfigurationMapCompat;
import androidx.camera.camera2.internal.compat.params.DynamicRangesCompat;
import androidx.camera.camera2.internal.compat.quirk.CameraQuirks;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.ZslDisablerQuirk;
import androidx.camera.camera2.internal.compat.workaround.FlashAvailabilityChecker;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraState;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.Logger;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.ImageOutputConfig.RotationValue;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.Timebase;
import androidx.camera.core.impl.utils.CameraOrientationUtil;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Implementation of the {@link CameraInfoInternal} interface that exposes parameters through
 * camera2.
 *
 * <p>Construction consists of two stages. The constructor creates a implementation without a
 * {@link Camera2CameraControlImpl} and will return default values for camera control related
 * states like zoom/exposure/torch. After {@link #linkWithCameraControl} is called,
 * zoom/exposure/torch API will reflect the states in the {@link Camera2CameraControlImpl}. Any
 * CameraCaptureCallbacks added before this link will also be added
 * to the {@link Camera2CameraControlImpl}.
 */
@OptIn(markerClass = ExperimentalCamera2Interop.class)
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class Camera2CameraInfoImpl implements CameraInfoInternal {

    private static final String TAG = "Camera2CameraInfo";
    private final String mCameraId;
    private final CameraCharacteristicsCompat mCameraCharacteristicsCompat;
    private final Camera2CameraInfo mCamera2CameraInfo;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    @Nullable
    private Camera2CameraControlImpl mCamera2CameraControlImpl;
    @GuardedBy("mLock")
    @Nullable
    private RedirectableLiveData<Integer> mRedirectTorchStateLiveData = null;
    @GuardedBy("mLock")
    @Nullable
    private RedirectableLiveData<ZoomState> mRedirectZoomStateLiveData = null;
    @NonNull
    private final RedirectableLiveData<CameraState> mCameraStateLiveData;
    @GuardedBy("mLock")
    @Nullable
    private List<Pair<CameraCaptureCallback, Executor>> mCameraCaptureCallbacks = null;

    @NonNull
    private final Quirks mCameraQuirks;
    @NonNull
    private final EncoderProfilesProvider mCamera2EncoderProfilesProvider;
    @NonNull
    private final CameraManagerCompat mCameraManager;

    /**
     * Constructs an instance. Before {@link #linkWithCameraControl(Camera2CameraControlImpl)} is
     * called, camera control related API (torch/exposure/zoom) will return default values.
     */
    public Camera2CameraInfoImpl(@NonNull String cameraId,
            @NonNull CameraManagerCompat cameraManager) throws CameraAccessExceptionCompat {
        mCameraId = Preconditions.checkNotNull(cameraId);
        mCameraManager = cameraManager;

        mCameraCharacteristicsCompat = cameraManager.getCameraCharacteristicsCompat(mCameraId);
        mCamera2CameraInfo = new Camera2CameraInfo(this);
        mCameraQuirks = CameraQuirks.get(cameraId, mCameraCharacteristicsCompat);
        mCamera2EncoderProfilesProvider = new Camera2EncoderProfilesProvider(cameraId);
        mCameraStateLiveData = new RedirectableLiveData<>(
                CameraState.create(CameraState.Type.CLOSED));
    }

    /**
     * Links with a {@link Camera2CameraControlImpl}. After the link, zoom/torch/exposure
     * operations of CameraControl will modify the states in this Camera2CameraInfoImpl.
     * Also, any CameraCaptureCallbacks added before this link will be added to the
     * {@link Camera2CameraControlImpl}.
     */
    void linkWithCameraControl(@NonNull Camera2CameraControlImpl camera2CameraControlImpl) {
        synchronized (mLock) {
            mCamera2CameraControlImpl = camera2CameraControlImpl;

            if (mRedirectZoomStateLiveData != null) {
                mRedirectZoomStateLiveData.redirectTo(
                        mCamera2CameraControlImpl.getZoomControl().getZoomState());
            }

            if (mRedirectTorchStateLiveData != null) {
                mRedirectTorchStateLiveData.redirectTo(
                        mCamera2CameraControlImpl.getTorchControl().getTorchState());
            }

            if (mCameraCaptureCallbacks != null) {
                for (Pair<CameraCaptureCallback, Executor> pair :
                        mCameraCaptureCallbacks) {
                    mCamera2CameraControlImpl.addSessionCameraCaptureCallback(pair.second,
                            pair.first);
                }
                mCameraCaptureCallbacks = null;
            }
        }
        logDeviceInfo();
    }

    /**
     * Sets the source of the {@linkplain CameraState camera states} that will be exposed. When
     * called more than once, the previous camera state source is overridden.
     */
    void setCameraStateSource(@NonNull LiveData<CameraState> cameraStateSource) {
        mCameraStateLiveData.redirectTo(cameraStateSource);
    }

    @NonNull
    @Override
    public String getCameraId() {
        return mCameraId;
    }

    @NonNull
    public CameraCharacteristicsCompat getCameraCharacteristicsCompat() {
        return mCameraCharacteristicsCompat;
    }

    @CameraSelector.LensFacing
    @Override
    public int getLensFacing() {
        Integer lensFacing = mCameraCharacteristicsCompat.get(CameraCharacteristics.LENS_FACING);
        Preconditions.checkArgument(lensFacing != null, "Unable to get the lens facing of the "
                + "camera.");
        return LensFacingUtil.getCameraSelectorLensFacing(lensFacing);
    }

    @Override
    public int getSensorRotationDegrees(@RotationValue int relativeRotation) {
        int sensorOrientation = getSensorOrientation();
        int relativeRotationDegrees =
                CameraOrientationUtil.surfaceRotationToDegrees(relativeRotation);
        // Currently this assumes that a back-facing camera is always opposite to the screen.
        // This may not be the case for all devices, so in the future we may need to handle that
        // scenario.
        final int lensFacing = getLensFacing();
        boolean isOppositeFacingScreen = CameraSelector.LENS_FACING_BACK == lensFacing;
        return CameraOrientationUtil.getRelativeImageRotation(
                relativeRotationDegrees,
                sensorOrientation,
                isOppositeFacingScreen);
    }

    int getSensorOrientation() {
        Integer sensorOrientation =
                mCameraCharacteristicsCompat.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Preconditions.checkNotNull(sensorOrientation);
        return sensorOrientation;
    }

    int getSupportedHardwareLevel() {
        Integer deviceLevel =
                mCameraCharacteristicsCompat.get(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        Preconditions.checkNotNull(deviceLevel);
        return deviceLevel;
    }

    @Override
    public int getSensorRotationDegrees() {
        return getSensorRotationDegrees(Surface.ROTATION_0);
    }

    private void logDeviceInfo() {
        // Extend by adding logging here as needed.
        logDeviceLevel();
    }

    private void logDeviceLevel() {
        String levelString;

        int deviceLevel = getSupportedHardwareLevel();
        switch (deviceLevel) {
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                levelString = "INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY";
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL:
                levelString = "INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL";
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                levelString = "INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED";
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                levelString = "INFO_SUPPORTED_HARDWARE_LEVEL_FULL";
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                levelString = "INFO_SUPPORTED_HARDWARE_LEVEL_3";
                break;
            default:
                levelString = "Unknown value: " + deviceLevel;
                break;
        }
        Logger.i(TAG, "Device Level: " + levelString);
    }

    @Override
    public boolean hasFlashUnit() {
        return FlashAvailabilityChecker.isFlashAvailable(mCameraCharacteristicsCompat::get);
    }

    @NonNull
    @Override
    public LiveData<Integer> getTorchState() {
        synchronized (mLock) {
            if (mCamera2CameraControlImpl == null) {
                if (mRedirectTorchStateLiveData == null) {
                    mRedirectTorchStateLiveData =
                            new RedirectableLiveData<>(TorchControl.DEFAULT_TORCH_STATE);
                }
                return mRedirectTorchStateLiveData;
            }

            // if RedirectableLiveData exists,  use it directly.
            if (mRedirectTorchStateLiveData != null) {
                return mRedirectTorchStateLiveData;
            }

            return mCamera2CameraControlImpl.getTorchControl().getTorchState();
        }
    }

    @NonNull
    @Override
    public LiveData<ZoomState> getZoomState() {
        synchronized (mLock) {
            if (mCamera2CameraControlImpl == null) {
                if (mRedirectZoomStateLiveData == null) {
                    mRedirectZoomStateLiveData = new RedirectableLiveData<>(
                            ZoomControl.getDefaultZoomState(mCameraCharacteristicsCompat));
                }
                return mRedirectZoomStateLiveData;
            }

            // if RedirectableLiveData exists,  use it directly.
            if (mRedirectZoomStateLiveData != null) {
                return mRedirectZoomStateLiveData;
            }

            return mCamera2CameraControlImpl.getZoomControl().getZoomState();
        }
    }

    @NonNull
    @Override
    public ExposureState getExposureState() {
        synchronized (mLock) {
            if (mCamera2CameraControlImpl == null) {
                return ExposureControl.getDefaultExposureState(mCameraCharacteristicsCompat);
            }
            return mCamera2CameraControlImpl.getExposureControl().getExposureState();
        }
    }

    @NonNull
    @Override
    public LiveData<CameraState> getCameraState() {
        return mCameraStateLiveData;
    }

    /**
     * {@inheritDoc}
     *
     * <p>When the CameraX configuration is {@link androidx.camera.camera2.Camera2Config}, the
     * return value depends on whether the device is legacy
     * ({@link CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL} {@code ==
     * }{@link CameraMetadata#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY}).
     *
     * @return {@link #IMPLEMENTATION_TYPE_CAMERA2_LEGACY} if the device is legacy, otherwise
     * {@link #IMPLEMENTATION_TYPE_CAMERA2}.
     */
    @NonNull
    @Override
    public String getImplementationType() {
        final int hardwareLevel = getSupportedHardwareLevel();
        return hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
                ? IMPLEMENTATION_TYPE_CAMERA2_LEGACY : IMPLEMENTATION_TYPE_CAMERA2;
    }

    @FloatRange(from = 0, fromInclusive = false)
    @Override
    public float getIntrinsicZoomRatio() {
        final Integer lensFacing =
                mCameraCharacteristicsCompat.get(CameraCharacteristics.LENS_FACING);
        if (lensFacing == null) {
            return INTRINSIC_ZOOM_RATIO_UNKNOWN;
        }

        int fovDegrees;
        int defaultFovDegrees;
        try {
            fovDegrees =
                    FovUtil.focalLengthToViewAngleDegrees(
                            FovUtil.getDefaultFocalLength(mCameraCharacteristicsCompat),
                            FovUtil.getSensorHorizontalLength(mCameraCharacteristicsCompat));
            defaultFovDegrees = FovUtil.getDeviceDefaultViewAngleDegrees(mCameraManager,
                    lensFacing);
        } catch (Exception e) {
            Logger.e(TAG, "The camera is unable to provide necessary information to resolve its "
                    + "intrinsic zoom ratio with error: " + e);
            return INTRINSIC_ZOOM_RATIO_UNKNOWN;
        }

        return ((float) defaultFovDegrees) / fovDegrees;
    }

    @Override
    public boolean isFocusMeteringSupported(@NonNull FocusMeteringAction action) {
        synchronized (mLock) {
            if (mCamera2CameraControlImpl == null) {
                return false;
            }
            return mCamera2CameraControlImpl.getFocusMeteringControl().isFocusMeteringSupported(
                    action);
        }
    }

    @Override
    public boolean isZslSupported() {
        return Build.VERSION.SDK_INT >= 23 && isPrivateReprocessingSupported()
                && (DeviceQuirks.get(ZslDisablerQuirk.class) == null);
    }

    @Override
    public boolean isPrivateReprocessingSupported() {
        return isCapabilitySupported(mCameraCharacteristicsCompat,
                REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING);
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public EncoderProfilesProvider getEncoderProfilesProvider() {
        return mCamera2EncoderProfilesProvider;
    }

    @NonNull
    @Override
    public Timebase getTimebase() {
        Integer timeSource = mCameraCharacteristicsCompat.get(
                CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE);
        Preconditions.checkNotNull(timeSource);
        switch (timeSource) {
            case SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME:
                return Timebase.REALTIME;
            case SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN:
            default:
                return Timebase.UPTIME;
        }
    }

    @NonNull
    @Override
    public List<Size> getSupportedResolutions(int format) {
        StreamConfigurationMapCompat mapCompat =
                mCameraCharacteristicsCompat.getStreamConfigurationMapCompat();
        Size[] size = mapCompat.getOutputSizes(format);
        return size != null ? Arrays.asList(size) : Collections.emptyList();
    }

    @NonNull
    @Override
    public List<Size> getSupportedHighResolutions(int format) {
        StreamConfigurationMapCompat mapCompat =
                mCameraCharacteristicsCompat.getStreamConfigurationMapCompat();
        Size[] size = mapCompat.getHighResolutionOutputSizes(format);
        return size != null ? Arrays.asList(size) : Collections.emptyList();
    }

    @NonNull
    @Override
    public Set<DynamicRange> getSupportedDynamicRanges() {
        DynamicRangesCompat dynamicRangesCompat = DynamicRangesCompat.fromCameraCharacteristics(
                mCameraCharacteristicsCompat);

        return dynamicRangesCompat.getSupportedDynamicRanges();
    }

    @Override
    public void addSessionCaptureCallback(@NonNull Executor executor,
            @NonNull CameraCaptureCallback callback) {
        synchronized (mLock) {
            if (mCamera2CameraControlImpl == null) {
                if (mCameraCaptureCallbacks == null) {
                    mCameraCaptureCallbacks = new ArrayList<>();
                }
                mCameraCaptureCallbacks.add(new Pair<>(callback, executor));
                return;
            }

            mCamera2CameraControlImpl.addSessionCameraCaptureCallback(executor, callback);
        }
    }

    @Override
    public void removeSessionCaptureCallback(@NonNull CameraCaptureCallback callback) {
        synchronized (mLock) {
            if (mCamera2CameraControlImpl == null) {
                if (mCameraCaptureCallbacks == null) {
                    return;
                }
                Iterator<Pair<CameraCaptureCallback, Executor>> it =
                        mCameraCaptureCallbacks.iterator();
                while (it.hasNext()) {
                    Pair<CameraCaptureCallback, Executor> pair = it.next();
                    if (pair.first == callback) {
                        it.remove();
                    }
                }
                return;
            }
            mCamera2CameraControlImpl.removeSessionCameraCaptureCallback(callback);
        }
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public Quirks getCameraQuirks() {
        return mCameraQuirks;
    }

    @NonNull
    @Override
    public Set<Range<Integer>> getSupportedFrameRateRanges() {
        Range<Integer>[] availableTargetFpsRanges =
                mCameraCharacteristicsCompat.get(
                        CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if (availableTargetFpsRanges != null) {
            return new HashSet<>(Arrays.asList(availableTargetFpsRanges));
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Gets the implementation of {@link Camera2CameraInfo}.
     */
    @NonNull
    public Camera2CameraInfo getCamera2CameraInfo() {
        return mCamera2CameraInfo;
    }

    /**
     * Returns a map consisting of the camera ids and the {@link CameraCharacteristics}s.
     *
     * <p>For every camera, the map contains at least the CameraCharacteristics for the camera id.
     * If the camera is logical camera, it will also contain associated physical camera ids and
     * their CameraCharacteristics.
     *
     */
    @NonNull
    public Map<String, CameraCharacteristics> getCameraCharacteristicsMap() {
        LinkedHashMap<String, CameraCharacteristics> map = new LinkedHashMap<>();

        map.put(mCameraId, mCameraCharacteristicsCompat.toCameraCharacteristics());

        for (String physicalCameraId : mCameraCharacteristicsCompat.getPhysicalCameraIds()) {
            if (Objects.equals(physicalCameraId, mCameraId)) {
                continue;
            }
            try {
                map.put(physicalCameraId,
                        mCameraManager.getCameraCharacteristicsCompat(physicalCameraId)
                                .toCameraCharacteristics());
            } catch (CameraAccessExceptionCompat e) {
                Logger.e(TAG,
                        "Failed to get CameraCharacteristics for cameraId " + physicalCameraId, e);
            }
        }
        return map;
    }

    /**
     * A {@link LiveData} which can be redirected to another {@link LiveData}. If no redirection
     * is set, initial value will be used.
     */
    static class RedirectableLiveData<T> extends MediatorLiveData<T> {
        private LiveData<T> mLiveDataSource;
        private final T mInitialValue;

        RedirectableLiveData(T initialValue) {
            mInitialValue = initialValue;
        }

        void redirectTo(@NonNull LiveData<T> liveDataSource) {
            if (mLiveDataSource != null) {
                super.removeSource(mLiveDataSource);
            }
            mLiveDataSource = liveDataSource;
            super.addSource(liveDataSource, this::setValue);
        }

        @Override
        public <S> void addSource(@NonNull LiveData<S> source,
                @NonNull Observer<? super S> onChanged) {
            throw new UnsupportedOperationException();
        }

        // Overrides getValue() to reflect the correct value from source. This is required to ensure
        // getValue() is correct when observe() or observeForever() is not called.
        @Override
        public T getValue() {
            // Returns initial value if source is not set.
            return mLiveDataSource == null ? mInitialValue : mLiveDataSource.getValue();
        }
    }

}
