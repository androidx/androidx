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
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.impl.AutoImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.AutoPreviewExtenderImpl;
import androidx.camera.extensions.impl.BeautyImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BeautyPreviewExtenderImpl;
import androidx.camera.extensions.impl.BokehImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.BokehPreviewExtenderImpl;
import androidx.camera.extensions.impl.HdrImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.HdrPreviewExtenderImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.NightPreviewExtenderImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.core.util.Preconditions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Basic vendor interface implementation
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class BasicVendorExtender implements VendorExtender {
    private static final String TAG = "BasicVendorExtender";
    private final @ExtensionMode.Mode int mMode;
    private final PreviewExtenderImpl mPreviewExtenderImpl;
    private final ImageCaptureExtenderImpl mImageCaptureExtenderImpl;
    private CameraInfo mCameraInfo;

    public BasicVendorExtender(@ExtensionMode.Mode int mode) {
        mMode = mode;
        try {
            switch (mode) {
                case ExtensionMode.BOKEH:
                    mPreviewExtenderImpl = new BokehPreviewExtenderImpl();
                    mImageCaptureExtenderImpl = new BokehImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.HDR:
                    mPreviewExtenderImpl = new HdrPreviewExtenderImpl();
                    mImageCaptureExtenderImpl = new HdrImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.NIGHT:
                    mPreviewExtenderImpl = new NightPreviewExtenderImpl();
                    mImageCaptureExtenderImpl = new NightImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.FACE_RETOUCH:
                    mPreviewExtenderImpl = new BeautyPreviewExtenderImpl();
                    mImageCaptureExtenderImpl = new BeautyImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.AUTO:
                    mPreviewExtenderImpl = new AutoPreviewExtenderImpl();
                    mImageCaptureExtenderImpl = new AutoImageCaptureExtenderImpl();
                    break;
                case ExtensionMode.NONE:
                default:
                    throw new IllegalArgumentException("Should not activate ExtensionMode.NONE");
            }
        } catch (NoClassDefFoundError e) {
            throw new IllegalArgumentException("Extension mode does not exist: " + mode);
        }
    }

    /**
     * Return the {@link PreviewExtenderImpl} instance. This method will be removed once the
     * existing basic extender implementation is migrated to the unified vendor extender.
     */
    @NonNull
    public PreviewExtenderImpl getPreviewExtenderImpl() {
        return mPreviewExtenderImpl;
    }

    /**
     * Return the {@link ImageCaptureExtenderImpl} instance. This method will be removed once the
     * existing basic extender implementation is migrated to the unified vendor extender.
     */
    @NonNull
    public ImageCaptureExtenderImpl getImageCaptureExtenderImpl() {
        return mImageCaptureExtenderImpl;
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> characteristicsMap) {
        CameraCharacteristics cameraCharacteristics = characteristicsMap.get(cameraId);
        return mPreviewExtenderImpl.isExtensionAvailable(cameraId, cameraCharacteristics)
                && mImageCaptureExtenderImpl.isExtensionAvailable(cameraId, cameraCharacteristics);
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @Override
    public void init(@NonNull CameraInfo cameraInfo) {
        mCameraInfo = cameraInfo;
        String cameraId = Camera2CameraInfo.from(cameraInfo).getCameraId();
        CameraCharacteristics cameraCharacteristics =
                Camera2CameraInfo.extractCameraCharacteristics(cameraInfo);
        mPreviewExtenderImpl.init(cameraId, cameraCharacteristics);
        mImageCaptureExtenderImpl.init(cameraId, cameraCharacteristics);

        Logger.d(TAG, "Extension init Mode = " + mMode);
        Logger.d(TAG, "PreviewExtender processorType= " + mPreviewExtenderImpl.getProcessorType());
        Logger.d(TAG, "ImageCaptureExtender processor= "
                + mImageCaptureExtenderImpl.getCaptureProcessor());
    }

    @Nullable
    @Override
    public Range<Long> getEstimatedCaptureLatencyRange(@Nullable Size size) {
        Preconditions.checkNotNull(mCameraInfo, "VendorExtender#init() must be called first");
        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_2) >= 0) {
            try {
                return mImageCaptureExtenderImpl.getEstimatedCaptureLatencyRange(size);
            } catch (NoSuchMethodError e) {
            }
        }
        return null;
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private Size[] getOutputSizes(int imageFormat) {
        StreamConfigurationMap map = Camera2CameraInfo.from(mCameraInfo)
                .getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        return map.getOutputSizes(imageFormat);
    }

    private int getPreviewInputImageFormat() {
        if (mPreviewExtenderImpl.getProcessorType()
                == PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR) {
            return ImageFormat.YUV_420_888;
        } else {
            return ImageFormat.PRIVATE;
        }
    }

    private int getCaptureInputImageFormat() {
        if (mImageCaptureExtenderImpl.getCaptureProcessor() != null) {
            return ImageFormat.YUV_420_888;
        } else {
            return ImageFormat.JPEG;
        }
    }

    @NonNull
    @Override
    public List<Pair<Integer, Size[]>> getSupportedPreviewOutputResolutions() {
        Preconditions.checkNotNull(mCameraInfo, "VendorExtender#init() must be called first");

        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_1) >= 0) {
            try {
                List<Pair<Integer, Size[]>> result =
                        mPreviewExtenderImpl.getSupportedResolutions();
                if (result != null) {
                    return result;
                }
            } catch (NoSuchMethodError e) {
            }
        }

        // Returns output sizes from stream configuration map if OEM returns null or OEM does not
        // implement the function. It is required to return all supported sizes so it must fetch
        // all sizes from the stream configuration map here.
        int imageformat = getPreviewInputImageFormat();
        return Arrays.asList(new Pair<>(imageformat, getOutputSizes(imageformat)));
    }


    @NonNull
    @Override
    public List<Pair<Integer, Size[]>> getSupportedCaptureOutputResolutions() {
        Preconditions.checkNotNull(mCameraInfo, "VendorExtender#init() must be called first");
        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_1) >= 0) {
            try {
                List<Pair<Integer, Size[]>> result =
                        mImageCaptureExtenderImpl.getSupportedResolutions();
                if (result != null) {
                    return result;
                }
            } catch (NoSuchMethodError e) {
            }
        }

        // Returns output sizes from stream configuration map if OEM returns null or OEM does not
        // implement the function. It is required to return all supported sizes so it must fetch
        // all sizes from the stream configuration map here.
        int imageFormat = getCaptureInputImageFormat();
        return Arrays.asList(new Pair<>(imageFormat, getOutputSizes(imageFormat)));
    }

    @NonNull
    @Override
    public Size[] getSupportedYuvAnalysisResolutions() {
        Preconditions.checkNotNull(mCameraInfo, "VendorExtender#init() must be called first");
        return getOutputSizes(ImageFormat.YUV_420_888);
    }

    @Nullable
    @Override
    public SessionProcessor createSessionProcessor(@NonNull Context context) {
        Preconditions.checkNotNull(mCameraInfo, "VendorExtender#init() must be called first");
        /* Return null to keep using existing flow for basic extender to ensure compatibility for
         * now. We will switch to SessionProcessor implementation once compatibility is ensured.
         */
        return null;
    }
}
