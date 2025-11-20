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

package androidx.camera.extensions.internal;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.os.Build;
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.impl.advanced.AdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.AutoAdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.BeautyAdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.BokehAdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.HdrAdvancedExtenderImpl;
import androidx.camera.extensions.impl.advanced.NightAdvancedExtenderImpl;
import androidx.camera.extensions.internal.compat.workaround.ExtensionDisabledValidator;
import androidx.camera.extensions.internal.sessionprocessor.AdvancedSessionProcessor;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Advanced vendor interface implementation
 */
public class AdvancedVendorExtender implements VendorExtender {
    private static final String TAG = "AdvancedVendorExtender";
    private final ExtensionDisabledValidator mExtensionDisabledValidator =
            new ExtensionDisabledValidator();
    private final AdvancedExtenderImpl mAdvancedExtenderImpl;
    private String mCameraId;
    private final @ExtensionMode.Mode int mMode;

    public AdvancedVendorExtender(@ExtensionMode.Mode int mode) {
        try {
            switch (mode) {
                case ExtensionMode.BOKEH:
                    mAdvancedExtenderImpl = new BokehAdvancedExtenderImpl();
                    break;
                case ExtensionMode.HDR:
                    mAdvancedExtenderImpl = new HdrAdvancedExtenderImpl();
                    break;
                case ExtensionMode.NIGHT:
                    mAdvancedExtenderImpl = new NightAdvancedExtenderImpl();
                    break;
                case ExtensionMode.FACE_RETOUCH:
                    mAdvancedExtenderImpl = new BeautyAdvancedExtenderImpl();
                    break;
                case ExtensionMode.AUTO:
                    mAdvancedExtenderImpl = new AutoAdvancedExtenderImpl();
                    break;
                case ExtensionMode.NONE:
                default:
                    throw new IllegalArgumentException("Should not active ExtensionMode.NONE");
            }
            mMode = mode;
        } catch (NoClassDefFoundError e) {
            throw new IllegalArgumentException("AdvancedExtenderImpl does not exist");
        }
    }

    @VisibleForTesting
    AdvancedVendorExtender(AdvancedExtenderImpl advancedExtenderImpl) {
        mAdvancedExtenderImpl = advancedExtenderImpl;
        mMode = ExtensionMode.NONE;
    }

    @Override
    public void init(@NonNull CameraInfo cameraInfo) {
        CameraInfoInternal cameraInfoInternal = (CameraInfoInternal) cameraInfo;
        mCameraId = cameraInfoInternal.getCameraId();

        Map<String, CameraCharacteristics> cameraCharacteristicsMap =
                ExtensionsUtils.getCameraCharacteristicsMap(cameraInfoInternal);

        mAdvancedExtenderImpl.init(mCameraId, cameraCharacteristicsMap);
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> characteristicsMap) {

        if (mExtensionDisabledValidator.shouldDisableExtension(cameraId)) {
            return false;
        }

        return mAdvancedExtenderImpl.isExtensionAvailable(cameraId, characteristicsMap);
    }

    @Override
    public @Nullable Range<Long> getEstimatedCaptureLatencyRange(@Nullable Size size) {
        Preconditions.checkNotNull(mCameraId, "VendorExtender#init() must be called first");

        // CameraX only uses JPEG output in Advanced Extender implementation.
        try {
            return mAdvancedExtenderImpl.getEstimatedCaptureLatencyRange(mCameraId, size,
                    ImageFormat.JPEG);
        } catch (Throwable throwable) {
            Logger.e(TAG, "AdvancedExtenderImpl.getEstimatedCaptureLatencyRange "
                    + "throws exceptions", throwable);
        }
        return null;
    }

    @Override
    public @NonNull List<Pair<Integer, Size[]>> getSupportedPreviewOutputResolutions() {
        Preconditions.checkNotNull(mCameraId, "VendorExtender#init() must be called first");
        try {
            return convertResolutionMapToList(
                    mAdvancedExtenderImpl.getSupportedPreviewOutputResolutions(mCameraId));
        } catch (Throwable throwable) {
            Logger.e(TAG, "AdvancedExtenderImpl.getSupportedPreviewOutputResolutions "
                    + "throws exceptions", throwable);
        }
        return Collections.emptyList();
    }

    @Override
    public @NonNull List<Pair<Integer, Size[]>> getSupportedCaptureOutputResolutions() {
        Preconditions.checkNotNull(mCameraId, "VendorExtender#init() must be called first");
        try {
            return convertResolutionMapToList(
                    mAdvancedExtenderImpl.getSupportedCaptureOutputResolutions(mCameraId));
        } catch (Throwable throwable) {
            Logger.e(TAG, "AdvancedExtenderImpl.getSupportedCaptureOutputResolutions "
                    + "throws exceptions", throwable);
        }
        return Collections.emptyList();
    }

    private @NonNull List<Pair<Integer, Size[]>> convertResolutionMapToList(
            @NonNull Map<Integer, List<Size>> map) {
        List<Pair<Integer, Size[]>> result = new ArrayList<>();
        for (Integer imageFormat : map.keySet()) {
            Size[] sizeArray = map.get(imageFormat).toArray(new Size[0]);
            result.add(new Pair<>(imageFormat, sizeArray));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Size @NonNull [] getSupportedYuvAnalysisResolutions() {
        Preconditions.checkNotNull(mCameraId, "VendorExtender#init() must be called first");
        // Disable ImageAnalysis
        return new Size[0];
    }

    private @NonNull List<CaptureRequest.Key<?>> getSupportedParameterKeys() {
        List<CaptureRequest.Key<?>> keys = new ArrayList<>();
        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_3) >= 0) {
            try {
                for (CaptureRequest.Key<?> key :
                        mAdvancedExtenderImpl.getAvailableCaptureRequestKeys()) {
                    keys.add(key);
                }
            } catch (Throwable throwable) {
                Logger.e(TAG, "Failed to retrieve available characteristics key-values!",
                        throwable);
            }
        }
        return Collections.unmodifiableList(keys);
    }

    @Override
    public @NonNull List<CaptureResult.Key> getSupportedCaptureResultKeys() {
        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_3) >= 0) {
            try {
                return Collections.unmodifiableList(
                        mAdvancedExtenderImpl.getAvailableCaptureResultKeys());
            } catch (Throwable throwable) {
                Logger.e(TAG, "AdvancedExtenderImpl.getAvailableCaptureResultKeys "
                        + "throws exceptions", throwable);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public @NonNull Map<Integer, List<Size>> getSupportedPostviewResolutions(
            @NonNull Size captureSize) {
        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)) {
            try {
                return Collections.unmodifiableMap(
                        mAdvancedExtenderImpl.getSupportedPostviewResolutions(captureSize));
            } catch (Throwable throwable) {
                Logger.e(TAG, "AdvancedExtenderImpl.getSupportedPostviewResolutions "
                        + "throws exceptions", throwable);
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public boolean isPostviewAvailable() {
        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)) {
            try {
                return mAdvancedExtenderImpl.isPostviewAvailable();
            } catch (Throwable throwable) {
                Logger.e(TAG, "AdvancedExtenderImpl.isPostviewAvailable throws exceptions",
                        throwable);
            }
        }
        return false;
    }

    @Override
    public boolean isCaptureProcessProgressAvailable() {
        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)) {
            try {
                return mAdvancedExtenderImpl.isCaptureProcessProgressAvailable();
            } catch (Throwable throwable) {
                Logger.e(TAG, "AdvancedExtenderImpl.isCaptureProcessProgressAvailable "
                        + "throws exceptions", throwable);
            }
        }
        return false;
    }

    @Override
    public boolean isExtensionStrengthAvailable() {
        // EXTENSION_STRENGTH is supported since API level 34
        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return getSupportedParameterKeys().contains(CaptureRequest.EXTENSION_STRENGTH);
        } else {
            return false;
        }
    }

    @Override
    public boolean isCurrentExtensionModeAvailable() {
        // EXTENSION_CURRENT_TYPE is supported since API level 34
        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return getSupportedCaptureResultKeys().contains(CaptureResult.EXTENSION_CURRENT_TYPE);
        } else {
            return false;
        }
    }

    @Override
    public @NonNull List<Pair<CameraCharacteristics.Key, Object>>
        getAvailableCharacteristicsKeyValues() {
        if (ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_5)
                && ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_5)) {
            List<Pair<CameraCharacteristics.Key, Object>> result = null;
            try {
                result = mAdvancedExtenderImpl.getAvailableCharacteristicsKeyValues();
            } catch (Throwable throwable) {
                Logger.e(TAG, "Failed to retrieve available characteristics key-values!",
                        throwable);
            }
            // In case OEMs implements it incorrectly by returning a null.
            if (result == null) {
                return Collections.emptyList();
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public @Nullable SessionProcessor createSessionProcessor(@NonNull Context context) {
        Preconditions.checkNotNull(mCameraId, "VendorExtender#init() must be called first");
        return new AdvancedSessionProcessor(
                mAdvancedExtenderImpl.createSessionProcessor(),
                getSupportedParameterKeys(),
                this,
                context,
                mMode);
    }
}
