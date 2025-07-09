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

import static androidx.camera.core.DynamicRange.SDR;
import static androidx.camera.core.internal.StreamSpecsCalculator.NO_OP_STREAM_SPECS_CALCULATOR;
import static androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.FloatRange;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraState;
import androidx.camera.core.CameraUseCaseAdapterProvider;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.Logger;
import androidx.camera.core.TorchState;
import androidx.camera.core.UseCase;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.DynamicRanges;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.ImageOutputConfig.RotationValue;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.Timebase;
import androidx.camera.core.impl.utils.CameraOrientationUtil;
import androidx.camera.core.internal.ImmutableZoomState;
import androidx.camera.core.internal.StreamSpecsCalculator;
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ApplicationProvider;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Fake implementation for retrieving camera information of a fake camera.
 *
 * <p>This camera info can be constructed with fake values.
 */
public final class FakeCameraInfoInternal implements CameraInfoInternal {
    private static final String TAG = "FakeCameraInfoInternal";
    private static final Set<Range<Integer>> FAKE_FPS_RANGES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    new Range<>(12, 30),
                    new Range<>(30, 30),
                    new Range<>(60, 60))
            )
    );
    private static final Set<DynamicRange> DEFAULT_DYNAMIC_RANGES = Collections.singleton(SDR);
    private final String mCameraId;
    private final int mSensorRotation;
    @CameraSelector.LensFacing
    private final int mLensFacing;
    private final MutableLiveData<Integer> mTorchState = new MutableLiveData<>(TorchState.OFF);
    private final MutableLiveData<ZoomState> mZoomLiveData;
    private final Map<Integer, List<Size>> mSupportedResolutionMap = new HashMap<>();
    private final Map<Range<Integer>, List<Size>> mSupportedHighSpeedFpsToSizeMap = new HashMap<>();
    private final Map<Integer, List<Size>> mSupportedHighResolutionMap = new HashMap<>();
    private MutableLiveData<CameraState> mCameraStateMutableLiveData;

    private final Set<DynamicRange> mSupportedDynamicRanges = new HashSet<>(DEFAULT_DYNAMIC_RANGES);
    private final Set<Integer> mAvailableCapabilities = new HashSet<>();
    private String mImplementationType = IMPLEMENTATION_TYPE_FAKE;

    // Leave uninitialized to support camera-core:1.0.0 dependencies.
    // Can be initialized during class init once there are no more pinned dependencies on
    // camera-core:1.0.0
    private EncoderProfilesProvider mEncoderProfilesProvider;

    private boolean mIsPrivateReprocessingSupported = false;
    private float mIntrinsicZoomRatio = 1.0F;

    private boolean mIsFocusMeteringSupported = false;
    private boolean mIsHighSpeedSupported = false;

    private ExposureState mExposureState = new FakeExposureState();
    private final @NonNull List<Quirk> mCameraQuirks = new ArrayList<>();

    private Timebase mTimebase = Timebase.UPTIME;

    private @Nullable CameraManager mCameraManager;

    private final @NonNull StreamSpecsCalculator mStreamSpecsCalculator;

    private @Nullable CameraUseCaseAdapterProvider mCameraUseCaseAdapterProvider;

    public FakeCameraInfoInternal() {
        this(/*sensorRotation=*/ 0, /*lensFacing=*/ CameraSelector.LENS_FACING_BACK);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FakeCameraInfoInternal(@NonNull StreamSpecsCalculator streamSpecsCalculator) {
        this(/*cameraId=*/ "0", /*sensorRotation=*/ 0, CameraSelector.LENS_FACING_BACK,
                ApplicationProvider.getApplicationContext(), streamSpecsCalculator);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FakeCameraInfoInternal(@NonNull String cameraId,
            @NonNull StreamSpecsCalculator streamSpecsCalculator) {
        this(cameraId, /*sensorRotation=*/ 0, CameraSelector.LENS_FACING_BACK,
                ApplicationProvider.getApplicationContext(), streamSpecsCalculator);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FakeCameraInfoInternal(@NonNull String cameraId,
            @NonNull Context context) {
        this(cameraId, 0, CameraSelector.LENS_FACING_BACK, context);
    }

    public FakeCameraInfoInternal(@NonNull String cameraId) {
        this(cameraId, 0, CameraSelector.LENS_FACING_BACK,
                ApplicationProvider.getApplicationContext());
    }

    public FakeCameraInfoInternal(@NonNull String cameraId,
            @CameraSelector.LensFacing int lensFacing) {
        this(cameraId, 0, lensFacing,
                ApplicationProvider.getApplicationContext());
    }

    public FakeCameraInfoInternal(int sensorRotation, @CameraSelector.LensFacing int lensFacing) {
        this("0", sensorRotation, lensFacing,
                ApplicationProvider.getApplicationContext());
    }

    public FakeCameraInfoInternal(@NonNull String cameraId, int sensorRotation,
            @CameraSelector.LensFacing int lensFacing) {
        this(cameraId, sensorRotation, lensFacing,
                ApplicationProvider.getApplicationContext());
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FakeCameraInfoInternal(@NonNull String cameraId, int sensorRotation,
            @CameraSelector.LensFacing int lensFacing,
            @NonNull Context context) {
        this(cameraId, sensorRotation, lensFacing, context, NO_OP_STREAM_SPECS_CALCULATOR);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public FakeCameraInfoInternal(@NonNull String cameraId, int sensorRotation,
            @CameraSelector.LensFacing int lensFacing,
            @NonNull Context context, @NonNull StreamSpecsCalculator streamSpecsCalculator) {
        mCameraId = cameraId;
        mSensorRotation = sensorRotation;
        mLensFacing = lensFacing;
        mZoomLiveData = new MutableLiveData<>(ImmutableZoomState.create(1.0f, 4.0f, 1.0f, 0.0f));
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mStreamSpecsCalculator = streamSpecsCalculator;
    }

    /**
     * Sets the zoom parameter.
     */
    public void setZoom(float zoomRatio, float minZoomRatio, float maxZoomRatio, float linearZoom) {
        mZoomLiveData.postValue(ImmutableZoomState.create(
                zoomRatio, maxZoomRatio, minZoomRatio, linearZoom
        ));
    }

    /**
     * Sets the exposure compensation parameters.
     */
    public void setExposureState(int index, @NonNull Range<Integer> range,
            @NonNull Rational step, boolean isSupported) {
        mExposureState = new FakeExposureState(index, range, step, isSupported);
    }

    /**
     * Sets the torch state.
     */
    public void setTorch(int torchState) {
        mTorchState.postValue(torchState);
    }

    /**
     * Sets the return value for {@link #isFocusMeteringSupported(FocusMeteringAction)}.
     */
    public void setIsFocusMeteringSupported(boolean supported) {
        mIsFocusMeteringSupported = supported;
    }

    @Override
    public int getLensFacing() {
        return mLensFacing;
    }

    @Override
    public @NonNull String getCameraId() {
        return mCameraId;
    }

    @Override
    public int getSensorRotationDegrees(@RotationValue int relativeRotation) {
        int relativeRotationDegrees =
                CameraOrientationUtil.surfaceRotationToDegrees(relativeRotation);
        // Currently this assumes that a back-facing camera is always opposite to the screen.
        // This may not be the case for all devices, so in the future we may need to handle that
        // scenario.
        Integer lensFacing = getLensFacing();
        boolean isOppositeFacingScreen =
                lensFacing != null && (CameraSelector.LENS_FACING_BACK == getLensFacing());
        return CameraOrientationUtil.getRelativeImageRotation(
                relativeRotationDegrees,
                mSensorRotation,
                isOppositeFacingScreen);
    }

    @Override
    public int getSensorRotationDegrees() {
        return getSensorRotationDegrees(Surface.ROTATION_0);
    }

    @Override
    public boolean hasFlashUnit() {
        return true;
    }

    @Override
    public @NonNull LiveData<Integer> getTorchState() {
        return mTorchState;
    }

    @Override
    public @NonNull LiveData<ZoomState> getZoomState() {
        return mZoomLiveData;
    }

    @Override
    public @NonNull ExposureState getExposureState() {
        return mExposureState;
    }

    private MutableLiveData<CameraState> getCameraStateMutableLiveData() {
        if (mCameraStateMutableLiveData == null) {
            mCameraStateMutableLiveData = new MutableLiveData<>(
                    CameraState.create(CameraState.Type.CLOSED));
        }
        return mCameraStateMutableLiveData;
    }

    @Override
    public @NonNull LiveData<CameraState> getCameraState() {
        return getCameraStateMutableLiveData();
    }

    @Override
    public @NonNull String getImplementationType() {
        return mImplementationType;
    }

    @Override
    public @NonNull EncoderProfilesProvider getEncoderProfilesProvider() {
        return mEncoderProfilesProvider == null ? EncoderProfilesProvider.EMPTY :
                mEncoderProfilesProvider;
    }

    @Override
    public @NonNull Timebase getTimebase() {
        return mTimebase;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public @NonNull Set<Integer> getSupportedOutputFormats() {
        return mSupportedResolutionMap.keySet();
    }

    @Override
    public @NonNull List<Size> getSupportedResolutions(int format) {
        List<Size> resolutions = mSupportedResolutionMap.get(format);
        return resolutions != null ? resolutions : Collections.emptyList();
    }

    @Override
    public @NonNull List<Size> getSupportedHighResolutions(int format) {
        List<Size> resolutions = mSupportedHighResolutionMap.get(format);
        return resolutions != null ? resolutions : Collections.emptyList();
    }

    @Override
    public @NonNull Set<DynamicRange> getSupportedDynamicRanges() {
        return mSupportedDynamicRanges;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public boolean isHighSpeedSupported() {
        return mIsHighSpeedSupported;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public Set<Range<Integer>> getSupportedHighSpeedFrameRateRanges() {
        return mSupportedHighSpeedFpsToSizeMap.keySet();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public Set<Range<Integer>> getSupportedHighSpeedFrameRateRangesFor(@NonNull Size size) {
        Set<Range<Integer>> ranges = new HashSet<>();
        for (Map.Entry<Range<Integer>, List<Size>> entry :
                mSupportedHighSpeedFpsToSizeMap.entrySet()) {
            if (entry.getValue().contains(size)) {
                ranges.add(entry.getKey());
            }
        }
        return ranges;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public List<Size> getSupportedHighSpeedResolutions() {
        Set<Size> resolutions = new HashSet<>();
        for (List<Size> sizes : mSupportedHighSpeedFpsToSizeMap.values()) {
            resolutions.addAll(sizes);
        }
        return new ArrayList<>(resolutions);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public List<Size> getSupportedHighSpeedResolutionsFor(@NonNull Range<Integer> fpsRange) {
        List<Size> resolutions = mSupportedHighSpeedFpsToSizeMap.get(fpsRange);
        return resolutions != null ? resolutions : Collections.emptyList();
    }

    /**
     * Returns a {@link Rect} corresponding to
     * {@link FakeCameraDeviceSurfaceManager#MAX_OUTPUT_SIZE}.
     */
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Rect getSensorRect() {
        return new Rect(0, 0, MAX_OUTPUT_SIZE.getWidth(), MAX_OUTPUT_SIZE.getHeight());
    }

    /**
     * Returns the supported dynamic ranges of this camera from a set of candidate dynamic ranges.
     *
     * <p>The dynamic ranges which represent what the camera supports will come from the dynamic
     * ranges set on {@link #setSupportedDynamicRanges(Set)}, or will consist of {@code {SDR}} if
     * {@code setSupportedDynamicRanges(Set)} has not been called. In order to stay compliant
     * with the API contract of
     * {@link androidx.camera.core.CameraInfo#querySupportedDynamicRanges(Set)}, it is
     * required that the {@link Set} provided to {@code setSupportedDynamicRanges(Set)} should
     * always contain {@link DynamicRange#SDR} and should never contain under-specified dynamic
     * ranges, such as {@link DynamicRange#UNSPECIFIED} and
     * {@link DynamicRange#HDR_UNSPECIFIED_10_BIT}.
     *
     * @see androidx.camera.core.CameraInfo#querySupportedDynamicRanges(Set)
     */
    @Override
    public @NonNull Set<DynamicRange> querySupportedDynamicRanges(
            @NonNull Set<DynamicRange> candidateDynamicRanges) {
        return DynamicRanges.findAllPossibleMatches(
                candidateDynamicRanges, getSupportedDynamicRanges());
    }

    @Override
    public void addSessionCaptureCallback(@NonNull Executor executor,
            @NonNull CameraCaptureCallback callback) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public void removeSessionCaptureCallback(@NonNull CameraCaptureCallback callback) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public @NonNull Quirks getCameraQuirks() {
        return new Quirks(mCameraQuirks);
    }

    @Override
    public @NonNull Set<Range<Integer>> getSupportedFrameRateRanges() {
        return FAKE_FPS_RANGES;
    }

    @Override
    public boolean isFocusMeteringSupported(@NonNull FocusMeteringAction action) {
        return mIsFocusMeteringSupported;
    }

    @androidx.camera.core.ExperimentalZeroShutterLag
    @Override
    public boolean isZslSupported() {
        return false;
    }

    @Override
    public boolean isPrivateReprocessingSupported() {
        return mIsPrivateReprocessingSupported;
    }

    @FloatRange(from = 0, fromInclusive = false)
    @Override
    public float getIntrinsicZoomRatio() {
        return mIntrinsicZoomRatio;
    }

    @Override
    public boolean isPreviewStabilizationSupported() {
        return false;
    }

    @Override
    public boolean isVideoStabilizationSupported() {
        return false;
    }

    /** Adds a quirk to the list of this camera's quirks. */
    @SuppressWarnings("unused")
    public void addCameraQuirk(final @NonNull Quirk quirk) {
        mCameraQuirks.add(quirk);
    }

    /**
     * Updates the {@link CameraState} value to the {@code LiveData} provided by
     * {@link #getCameraState()}.
     *
     * @param cameraState the camera state value to set.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void updateCameraState(@NonNull CameraState cameraState) {
        getCameraStateMutableLiveData().postValue(cameraState);
    }

    /**
     * Set the implementation type for testing
     */
    public void setImplementationType(@ImplementationType @NonNull String implementationType) {
        mImplementationType = implementationType;
    }

    /** Set the EncoderProfilesProvider for testing */
    public void setEncoderProfilesProvider(
            @NonNull EncoderProfilesProvider encoderProfilesProvider) {
        mEncoderProfilesProvider = Preconditions.checkNotNull(encoderProfilesProvider);
    }

    /** Set the timebase for testing */
    public void setTimebase(@NonNull Timebase timebase) {
        mTimebase = timebase;
    }

    /** Set the supported resolutions for testing */
    public void setSupportedResolutions(int format, @NonNull List<Size> resolutions) {
        mSupportedResolutionMap.put(format, resolutions);
    }

    /** Set the supported high resolutions for testing */
    public void setSupportedHighResolutions(int format, @NonNull List<Size> resolutions) {
        mSupportedHighResolutionMap.put(format, resolutions);
    }

    /** Sets the return value for {@link #isHighSpeedSupported()}}. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setHighSpeedSupported(boolean supported) {
        mIsHighSpeedSupported = supported;
    }

    /** Set the supported high speed resolutions for testing */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setSupportedHighSpeedResolutions(@NonNull Range<Integer> fps,
            @NonNull List<Size> resolutions) {
        mSupportedHighSpeedFpsToSizeMap.put(fps, resolutions);
    }

    /** Set the isPrivateReprocessingSupported flag for testing */
    public void setPrivateReprocessingSupported(boolean supported) {
        mIsPrivateReprocessingSupported = supported;
    }

    /** Adds a available view angle for testing. */
    public void setIntrinsicZoomRatio(float zoomRatio) {
        mIntrinsicZoomRatio = zoomRatio;
    }

    /** Set the supported dynamic ranges for testing */
    public void setSupportedDynamicRanges(@NonNull Set<DynamicRange> dynamicRanges) {
        mSupportedDynamicRanges.clear();
        mSupportedDynamicRanges.addAll(dynamicRanges);
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Object getCameraCharacteristics() {
        try {
            return mCameraManager.getCameraCharacteristics(mCameraId);
        } catch (CameraAccessException e) {
            throw new IllegalStateException("can't get CameraCharacteristics", e);
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @Nullable Object getPhysicalCameraCharacteristics(@NonNull String physicalCameraId) {
        try {
            return mCameraManager.getCameraCharacteristics(physicalCameraId);
        } catch (CameraAccessException e) {
            throw new IllegalStateException("can't get CameraCharacteristics", e);
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isUseCaseCombinationSupported(@NonNull List<@NonNull UseCase> useCases,
            int cameraMode, boolean isFeatureComboInvocation, @NonNull CameraConfig cameraConfig) {
        try {
            StreamSpecsCalculator.Companion.calculateSuggestedStreamSpecsCompat(
                    mStreamSpecsCalculator,
                    cameraMode,
                    this,
                    useCases,
                    cameraConfig,
                    isFeatureComboInvocation
            );
        } catch (IllegalArgumentException e) {
            Logger.d(TAG, "isUseCaseCombinationSupported: calculateSuggestedStreamSpecs failed", e);
            return false;
        }

        return true;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public void setCameraUseCaseAdapterProvider(
            @NonNull CameraUseCaseAdapterProvider cameraUseCaseAdapterProvider) {
        CameraInfoInternal.super.setCameraUseCaseAdapterProvider(cameraUseCaseAdapterProvider);
        mCameraUseCaseAdapterProvider = cameraUseCaseAdapterProvider;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @Nullable CameraUseCaseAdapterProvider getCameraUseCaseAdapterProvider() {
        return mCameraUseCaseAdapterProvider;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public @NonNull Set<@NonNull Integer> getAvailableCapabilities() {
        return new HashSet<>(mAvailableCapabilities);
    }

    /** Sets the capabilities available for a camera. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setAvailableCapabilities(@NonNull Set<@NonNull Integer> availableCapabilities) {
        mAvailableCapabilities.clear();
        mAvailableCapabilities.addAll(availableCapabilities);
    }

    static final class FakeExposureState implements ExposureState {
        private int mIndex = 0;
        private Range<Integer> mRange = new Range<>(0, 0);
        private Rational mStep = Rational.ZERO;
        private boolean mIsSupported = true;

        FakeExposureState() {
        }
        FakeExposureState(int index, Range<Integer> range,
                Rational step, boolean isSupported) {
            mIndex = index;
            mRange = range;
            mStep = step;
            mIsSupported = isSupported;
        }

        @Override
        public int getExposureCompensationIndex() {
            return mIndex;
        }

        @Override
        public @NonNull Range<Integer> getExposureCompensationRange() {
            return mRange;
        }

        @Override
        public @NonNull Rational getExposureCompensationStep() {
            return mStep;
        }

        @Override
        public boolean isExposureCompensationSupported() {
            return mIsSupported;
        }
    }
}
