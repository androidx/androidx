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

import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraState;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.TorchState;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.ImageOutputConfig.RotationValue;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.Timebase;
import androidx.camera.core.impl.utils.CameraOrientationUtil;
import androidx.camera.core.internal.ImmutableZoomState;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
 * Information for a fake camera.
 *
 * <p>This camera info can be constructed with fake values.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class FakeCameraInfoInternal implements CameraInfoInternal {
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
    private final Map<Integer, List<Size>> mSupportedHighResolutionMap = new HashMap<>();
    private MutableLiveData<CameraState> mCameraStateLiveData;

    private final Set<DynamicRange> mSupportedDynamicRanges = new HashSet<>(DEFAULT_DYNAMIC_RANGES);
    private String mImplementationType = IMPLEMENTATION_TYPE_FAKE;

    // Leave uninitialized to support camera-core:1.0.0 dependencies.
    // Can be initialized during class init once there are no more pinned dependencies on
    // camera-core:1.0.0
    private EncoderProfilesProvider mEncoderProfilesProvider;

    private boolean mIsPrivateReprocessingSupported = false;
    private float mIntrinsicZoomRatio = 1.0F;

    private boolean mIsFocusMeteringSupported = false;

    private ExposureState mExposureState = new FakeExposureState();
    @NonNull
    private final List<Quirk> mCameraQuirks = new ArrayList<>();

    private Timebase mTimebase = Timebase.UPTIME;

    public FakeCameraInfoInternal() {
        this(/*sensorRotation=*/ 0, /*lensFacing=*/ CameraSelector.LENS_FACING_BACK);
    }

    public FakeCameraInfoInternal(@NonNull String cameraId) {
        this(cameraId, 0, CameraSelector.LENS_FACING_BACK);
    }

    public FakeCameraInfoInternal(int sensorRotation, @CameraSelector.LensFacing int lensFacing) {
        this("0", sensorRotation, lensFacing);
    }

    public FakeCameraInfoInternal(@NonNull String cameraId, int sensorRotation,
            @CameraSelector.LensFacing int lensFacing) {
        mCameraId = cameraId;
        mSensorRotation = sensorRotation;
        mLensFacing = lensFacing;
        mZoomLiveData = new MutableLiveData<>(ImmutableZoomState.create(1.0f, 4.0f, 1.0f, 0.0f));
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

    @NonNull
    @Override
    public String getCameraId() {
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

    @NonNull
    @Override
    public LiveData<Integer> getTorchState() {
        return mTorchState;
    }

    @NonNull
    @Override
    public LiveData<ZoomState> getZoomState() {
        return mZoomLiveData;
    }

    @NonNull
    @Override
    public ExposureState getExposureState() {
        return mExposureState;
    }

    @NonNull
    @Override
    public LiveData<CameraState> getCameraState() {
        if (mCameraStateLiveData == null) {
            mCameraStateLiveData = new MutableLiveData<>(
                    CameraState.create(CameraState.Type.CLOSED));
        }
        return mCameraStateLiveData;
    }

    @NonNull
    @Override
    public String getImplementationType() {
        return mImplementationType;
    }

    @NonNull
    @Override
    public EncoderProfilesProvider getEncoderProfilesProvider() {
        return mEncoderProfilesProvider == null ? EncoderProfilesProvider.EMPTY :
                mEncoderProfilesProvider;
    }

    @NonNull
    @Override
    public Timebase getTimebase() {
        return mTimebase;
    }

    @NonNull
    @Override
    public List<Size> getSupportedResolutions(int format) {
        List<Size> resolutions = mSupportedResolutionMap.get(format);
        return resolutions != null ? resolutions : Collections.emptyList();
    }

    @NonNull
    @Override
    public List<Size> getSupportedHighResolutions(int format) {
        List<Size> resolutions = mSupportedHighResolutionMap.get(format);
        return resolutions != null ? resolutions : Collections.emptyList();
    }

    @NonNull
    @Override
    public Set<DynamicRange> getSupportedDynamicRanges() {
        return mSupportedDynamicRanges;
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

    @NonNull
    @Override
    public Quirks getCameraQuirks() {
        return new Quirks(mCameraQuirks);
    }

    @NonNull
    @Override
    public Set<Range<Integer>> getSupportedFrameRateRanges() {
        return FAKE_FPS_RANGES;
    }

    @Override
    public boolean isFocusMeteringSupported(@NonNull FocusMeteringAction action) {
        return mIsFocusMeteringSupported;
    }

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

    /** Adds a quirk to the list of this camera's quirks. */
    @SuppressWarnings("unused")
    public void addCameraQuirk(@NonNull final Quirk quirk) {
        mCameraQuirks.add(quirk);
    }

    /**
     * Set the implementation type for testing
     */
    public void setImplementationType(@NonNull @ImplementationType String implementationType) {
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

    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
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

        @NonNull
        @Override
        public Range<Integer> getExposureCompensationRange() {
            return mRange;
        }

        @NonNull
        @Override
        public Rational getExposureCompensationStep() {
            return mStep;
        }

        @Override
        public boolean isExposureCompensationSupported() {
            return mIsSupported;
        }
    }
}
