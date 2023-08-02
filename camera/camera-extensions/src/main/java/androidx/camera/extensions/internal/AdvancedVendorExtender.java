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
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.Logger;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Advanced vendor interface implementation
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AdvancedVendorExtender implements VendorExtender {
    private static final String TAG = "AdvancedVendorExtender";
    private final ExtensionDisabledValidator mExtensionDisabledValidator =
            new ExtensionDisabledValidator();
    private final AdvancedExtenderImpl mAdvancedExtenderImpl;
    private String mCameraId;

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
        } catch (NoClassDefFoundError e) {
            throw new IllegalArgumentException("AdvancedExtenderImpl does not exist");
        }
    }

    @VisibleForTesting
    AdvancedVendorExtender(AdvancedExtenderImpl advancedExtenderImpl) {
        mAdvancedExtenderImpl = advancedExtenderImpl;
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @Override
    public void init(@NonNull CameraInfo cameraInfo) {
        mCameraId = Camera2CameraInfo.from(cameraInfo).getCameraId();

        Map<String, CameraCharacteristics> cameraCharacteristicsMap =
                Camera2CameraInfo.from(cameraInfo).getCameraCharacteristicsMap();

        mAdvancedExtenderImpl.init(mCameraId, cameraCharacteristicsMap);
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> characteristicsMap) {

        if (mExtensionDisabledValidator.shouldDisableExtension()) {
            return false;
        }

        return mAdvancedExtenderImpl.isExtensionAvailable(cameraId, characteristicsMap);
    }

    @Nullable
    @Override
    public Range<Long> getEstimatedCaptureLatencyRange(@Nullable Size size) {
        Preconditions.checkNotNull(mCameraId, "VendorExtender#init() must be called first");

        // CameraX only uses JPEG output in Advanced Extender implementation.
        return mAdvancedExtenderImpl.getEstimatedCaptureLatencyRange(mCameraId, size,
                ImageFormat.JPEG);
    }

    @NonNull
    @Override
    public List<Pair<Integer, Size[]>> getSupportedPreviewOutputResolutions() {
        Preconditions.checkNotNull(mCameraId, "VendorExtender#init() must be called first");
        return convertResolutionMapToList(
                mAdvancedExtenderImpl.getSupportedPreviewOutputResolutions(mCameraId));
    }

    @NonNull
    @Override
    public List<Pair<Integer, Size[]>> getSupportedCaptureOutputResolutions() {
        Preconditions.checkNotNull(mCameraId, "VendorExtender#init() must be called first");
        return convertResolutionMapToList(
                mAdvancedExtenderImpl.getSupportedCaptureOutputResolutions(mCameraId));
    }

    @NonNull
    private List<Pair<Integer, Size[]>> convertResolutionMapToList(
            @NonNull Map<Integer, List<Size>> map) {
        List<Pair<Integer, Size[]>> result = new ArrayList<>();
        for (Integer imageFormat : map.keySet()) {
            Size[] sizeArray = map.get(imageFormat).toArray(new Size[0]);
            result.add(new Pair<>(imageFormat, sizeArray));
        }
        return Collections.unmodifiableList(result);
    }

    @NonNull
    @Override
    public Size[] getSupportedYuvAnalysisResolutions() {
        Preconditions.checkNotNull(mCameraId, "VendorExtender#init() must be called first");
        List<Size> yuvList = mAdvancedExtenderImpl.getSupportedYuvAnalysisResolutions(mCameraId);
        return yuvList == null ? new Size[0] : yuvList.toArray(new Size[0]);
    }

    @NonNull
    private List<CaptureRequest.Key> getSupportedParameterKeys() {
        List<CaptureRequest.Key> keys = Collections.emptyList();
        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_3) >= 0) {
            try {
                keys = Collections.unmodifiableList(
                        mAdvancedExtenderImpl.getAvailableCaptureRequestKeys());
            } catch (Exception e) {
                Logger.e(TAG, "AdvancedExtenderImpl.getAvailableCaptureRequestKeys "
                        + "throws exceptions", e);
            }
        }
        return keys;
    }

    @Nullable
    @Override
    public SessionProcessor createSessionProcessor(@NonNull Context context) {
        Preconditions.checkNotNull(mCameraId, "VendorExtender#init() must be called first");
        return new AdvancedSessionProcessor(
                mAdvancedExtenderImpl.createSessionProcessor(),
                getSupportedParameterKeys(),
                context);
    }
}
