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
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
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
import androidx.camera.extensions.internal.compat.workaround.AvailableKeysRetriever;
import androidx.camera.extensions.internal.compat.workaround.ExtensionDisabledValidator;
import androidx.camera.extensions.internal.sessionprocessor.BasicExtenderSessionProcessor;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Basic vendor interface implementation
 */
@RequiresApi(23) // TODO(b/200306659): Remove and replace with annotation on package-info.java
// replaceImageFormatIfMissing accesses ImageFormat#PRIVATE which is public since API level 23.
public class BasicVendorExtender implements VendorExtender {
    private static final String TAG = "BasicVendorExtender";
    private final ExtensionDisabledValidator mExtensionDisabledValidator =
            new ExtensionDisabledValidator();
    private PreviewExtenderImpl mPreviewExtenderImpl = null;
    private ImageCaptureExtenderImpl mImageCaptureExtenderImpl = null;
    private CameraInfo mCameraInfo;
    private String mCameraId;
    private CameraCharacteristics mCameraCharacteristics;
    private AvailableKeysRetriever mAvailableKeysRetriever = new AvailableKeysRetriever();

    static final List<CaptureRequest.Key> sBaseSupportedKeys = new ArrayList<>(Arrays.asList(
            CaptureRequest.SCALER_CROP_REGION,
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_TRIGGER,
            CaptureRequest.CONTROL_AF_REGIONS,
            CaptureRequest.CONTROL_AE_REGIONS,
            CaptureRequest.CONTROL_AWB_REGIONS,
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
            CaptureRequest.FLASH_MODE,
            CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
    ));
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            sBaseSupportedKeys.add(CaptureRequest.CONTROL_ZOOM_RATIO);
        }
    }

    public BasicVendorExtender(@ExtensionMode.Mode int mode) {
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
            Logger.e(TAG, "OEM implementation for extension mode " + mode + "does not exist!");
        }
    }

    @VisibleForTesting
    BasicVendorExtender(ImageCaptureExtenderImpl imageCaptureExtenderImpl,
            PreviewExtenderImpl previewExtenderImpl) {
        mPreviewExtenderImpl = previewExtenderImpl;
        mImageCaptureExtenderImpl = imageCaptureExtenderImpl;
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> characteristicsMap) {

        if (mExtensionDisabledValidator.shouldDisableExtension()) {
            return false;
        }

        // Returns false if implementation classes do not exist.
        if (mPreviewExtenderImpl == null || mImageCaptureExtenderImpl == null) {
            return false;
        }

        CameraCharacteristics cameraCharacteristics = characteristicsMap.get(cameraId);
        return mPreviewExtenderImpl.isExtensionAvailable(cameraId, cameraCharacteristics)
                && mImageCaptureExtenderImpl.isExtensionAvailable(cameraId, cameraCharacteristics);
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @Override
    public void init(@NonNull CameraInfo cameraInfo) {
        mCameraInfo = cameraInfo;

        if (mPreviewExtenderImpl == null || mImageCaptureExtenderImpl == null) {
            return;
        }

        mCameraId = Camera2CameraInfo.from(cameraInfo).getCameraId();
        mCameraCharacteristics =
                Camera2CameraInfo.extractCameraCharacteristics(cameraInfo);
        mPreviewExtenderImpl.init(mCameraId, mCameraCharacteristics);
        mImageCaptureExtenderImpl.init(mCameraId, mCameraCharacteristics);

        Logger.d(TAG, "PreviewExtender processorType= " + mPreviewExtenderImpl.getProcessorType());
        Logger.d(TAG, "ImageCaptureExtender processor= "
                + mImageCaptureExtenderImpl.getCaptureProcessor());
    }

    @Nullable
    @Override
    public Range<Long> getEstimatedCaptureLatencyRange(@Nullable Size size) {
        Preconditions.checkNotNull(mCameraInfo, "VendorExtender#init() must be called first");
        if (mImageCaptureExtenderImpl != null && ExtensionVersion.getRuntimeVersion().compareTo(
                Version.VERSION_1_2) >= 0) {
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

    private int getCaptureInputImageFormat() {
        if (mImageCaptureExtenderImpl != null
                && mImageCaptureExtenderImpl.getCaptureProcessor() != null) {
            return ImageFormat.YUV_420_888;
        } else {
            return ImageFormat.JPEG;
        }
    }

    private int getPreviewInputImageFormat() {
        if (mPreviewExtenderImpl != null
                && mPreviewExtenderImpl.getProcessorType()
                == PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR) {
            return ImageFormat.YUV_420_888;
        } else {
            return ImageFormat.PRIVATE;
        }
    }

    @NonNull
    @Override
    public List<Pair<Integer, Size[]>> getSupportedPreviewOutputResolutions() {
        Preconditions.checkNotNull(mCameraInfo, "VendorExtender#init() must be called first");

        if (mPreviewExtenderImpl != null && ExtensionVersion.getRuntimeVersion().compareTo(
                Version.VERSION_1_1) >= 0) {
            try {
                List<Pair<Integer, Size[]>> result =
                        mPreviewExtenderImpl.getSupportedResolutions();
                if (result != null) {
                    // Ensure the PRIVATE format is in the list.
                    // PreviewExtenderImpl.getSupportedResolutions() returns the supported size
                    // for input surface. We need to ensure output surface format is supported.
                    return replaceImageFormatIfMissing(result,
                            ImageFormat.YUV_420_888 /* formatToBeReplaced */,
                            ImageFormat.PRIVATE /* newFormat */);
                }
            } catch (NoSuchMethodError e) {
            }
        }

        // Returns output sizes from stream configuration map if OEM returns null or OEM does not
        // implement the function. BasicVendorExtender's SessionProcessor will always output
        // to PRIVATE surface, but the input image which connect to the camera could be
        // either YUV or PRIVATE. Since the input image from input surface is guaranteed to be
        // able to output to the output surface, therefore we fetch the sizes from the
        // input image format for the output format.
        int inputImageFormat = getPreviewInputImageFormat();
        return Arrays.asList(new Pair<>(ImageFormat.PRIVATE, getOutputSizes(inputImageFormat)));
    }


    @NonNull
    @Override
    public List<Pair<Integer, Size[]>> getSupportedCaptureOutputResolutions() {
        Preconditions.checkNotNull(mCameraInfo, "VendorExtender#init() must be called first");
        if (mImageCaptureExtenderImpl != null && ExtensionVersion.getRuntimeVersion().compareTo(
                Version.VERSION_1_1) >= 0) {
            try {
                List<Pair<Integer, Size[]>> result =
                        mImageCaptureExtenderImpl.getSupportedResolutions();
                if (result != null) {
                    // Ensure the JPEG format is in the list.
                    // ImageCaptureExtenderImpl.getSupportedResolutions() returns the supported
                    // size for input surface. We need to ensure output surface format is supported.
                    return replaceImageFormatIfMissing(result,
                            ImageFormat.YUV_420_888 /* formatToBeReplaced */,
                            ImageFormat.JPEG /* newFormat */);
                }
            } catch (NoSuchMethodError e) {
            }
        }

        // Returns output sizes from stream configuration map if OEM returns null or OEM does not
        // implement the function. BasicVendorExtender's SessionProcessor will always output
        // JPEG Images, but the input image which connect to the camera could be either YUV or
        // JPEG. Since the input image from input surface is guaranteed to be able to output to
        // the output surface, therefore we fetch the sizes from the input image format for the
        // output format.
        int inputImageFormat = getCaptureInputImageFormat();
        return Arrays.asList(new Pair<>(ImageFormat.JPEG, getOutputSizes(inputImageFormat)));
    }

    private List<Pair<Integer, Size[]>> replaceImageFormatIfMissing(
            List<Pair<Integer, Size[]>> input, int formatToBeReplaced, int newFormat) {
        for (Pair<Integer, Size[]> pair : input) {
            if (pair.first == newFormat) {
                return input;
            }
        }

        List<Pair<Integer, Size[]>> output = new ArrayList<>();
        boolean formatFound = false;
        for (Pair<Integer, Size[]> pair : input) {
            if (pair.first == formatToBeReplaced) {
                formatFound = true;
                output.add(new Pair<>(newFormat, pair.second));
            } else {
                output.add(pair);
            }
        }

        if (!formatFound) {
            throw new IllegalArgumentException(
                    "Supported resolution should contain " + newFormat + " format.");
        }
        return output;
    }

    @NonNull
    @Override
    public Size[] getSupportedYuvAnalysisResolutions() {
        Preconditions.checkNotNull(mCameraInfo, "VendorExtender#init() must be called first");
        return getOutputSizes(ImageFormat.YUV_420_888);
    }

    @NonNull
    private List<CaptureRequest.Key> getSupportedParameterKeys(Context context) {
        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_3) >= 0) {
            try {
                List<CaptureRequest.Key> keys =
                        Collections.unmodifiableList(
                                mAvailableKeysRetriever.getAvailableCaptureRequestKeys(
                                        mImageCaptureExtenderImpl,
                                        mCameraId,
                                        mCameraCharacteristics,
                                        context));
                if (keys == null) {
                    keys = Collections.emptyList();
                }
                return keys;
            } catch (Exception e) {
                // it could crash on some OEMs.
                Logger.e(TAG, "ImageCaptureExtenderImpl.getAvailableCaptureRequestKeys "
                        + "throws exceptions", e);
                return Collections.emptyList();
            }
        } else {
            // For Basic Extender implementing v1.2 or below, we assume zoom/tap-to-focus/flash/EC
            // are supported for compatibility reason.
            return Collections.unmodifiableList(sBaseSupportedKeys);
        }
    }

    @Nullable
    @Override
    public SessionProcessor createSessionProcessor(@NonNull Context context) {
        Preconditions.checkNotNull(mCameraInfo, "VendorExtender#init() must be called first");
        return new BasicExtenderSessionProcessor(
                mPreviewExtenderImpl, mImageCaptureExtenderImpl,
                getSupportedParameterKeys(context),
                context);
    }
}
