/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.quirk;

import android.graphics.ImageFormat;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.Quirk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * <p>QuirkSummary
 *     Bug Id: b/157448499, b/192129158, b/245495234
 *     Description: Quirk required to exclude certain supported surface sizes that are
 *                  problematic. These sizes are dependent on the device, camera and image format.
 *                  An example is the resolution size 4000x3000 which is supported on OnePlus 6,
 *                  but causes a WYSIWYG issue between preview and image capture. Another example
 *                  is on Huawei P20 Lite, the Preview screen will become too bright when 400x400
 *                  or 720x720 Preview resolutions are used together with a large zoom in value.
 *                  The same symptom happens on ImageAnalysis. On Samsung J7 Prime (SM-G610M) or
 *                  J7 (SM-J710MN) API 27 devices, the Preview images will be stretched if
 *                  1920x1080 resolution is used.
 *     Device(s): OnePlus 6, OnePlus 6T, Huawei P20, Samsung J7 Prime (SM-G610M) API 27, Samsung
 *     J7 (SM-J710MN) API 27
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExcludedSupportedSizesQuirk implements Quirk {

    private static final String TAG = "ExcludedSupportedSizesQuirk";
    private static final int UNKNOWN_IMAGE_FORMAT = -1;

    static boolean load() {
        return isOnePlus6() || isOnePlus6T() || isHuaweiP20Lite() || isSamsungJ7PrimeApi27Above()
                || isSamsungJ7Api27Above();
    }

    private static boolean isOnePlus6() {
        return "OnePlus".equalsIgnoreCase(Build.BRAND) && "OnePlus6".equalsIgnoreCase(Build.DEVICE);
    }

    private static boolean isOnePlus6T() {
        return "OnePlus".equalsIgnoreCase(Build.BRAND) && "OnePlus6T".equalsIgnoreCase(
                Build.DEVICE);
    }

    private static boolean isHuaweiP20Lite() {
        return "HUAWEI".equalsIgnoreCase(Build.BRAND) && "HWANE".equalsIgnoreCase(Build.DEVICE);
    }

    private static boolean isSamsungJ7PrimeApi27Above() {
        return "SAMSUNG".equalsIgnoreCase(Build.BRAND.toUpperCase(Locale.US))
                && "ON7XELTE".equalsIgnoreCase(Build.DEVICE.toUpperCase(Locale.US))
                && Build.VERSION.SDK_INT >= 27;
    }

    private static boolean isSamsungJ7Api27Above() {
        return "SAMSUNG".equalsIgnoreCase(Build.BRAND.toUpperCase(Locale.US))
                && "J7XELTE".equalsIgnoreCase(Build.DEVICE.toUpperCase(Locale.US))
                && Build.VERSION.SDK_INT >= 27;
    }

    /**
     * Retrieves problematic supported surface sizes that have to be excluded on the current
     * device, for the given camera id and image format.
     */
    @NonNull
    public List<Size> getExcludedSizes(@NonNull String cameraId, int imageFormat) {
        if (isOnePlus6()) {
            return getOnePlus6ExcludedSizes(cameraId, imageFormat);
        }
        if (isOnePlus6T()) {
            return getOnePlus6TExcludedSizes(cameraId, imageFormat);
        }
        if (isHuaweiP20Lite()) {
            return getHuaweiP20LiteExcludedSizes(cameraId, imageFormat, null);
        }
        if (isSamsungJ7PrimeApi27Above()) {
            return getSamsungJ7PrimeApi27AboveExcludedSizes(cameraId, imageFormat, null);
        }
        if (isSamsungJ7Api27Above()) {
            return getSamsungJ7Api27AboveExcludedSizes(cameraId, imageFormat, null);
        }
        Logger.w(TAG, "Cannot retrieve list of supported sizes to exclude on this device.");
        return Collections.emptyList();
    }

    /**
     * Retrieves problematic supported surface sizes that have to be excluded on the current
     * device, for the given camera id and class type.
     */
    @NonNull
    public List<Size> getExcludedSizes(@NonNull String cameraId, @NonNull Class<?> klass) {
        if (isHuaweiP20Lite()) {
            return getHuaweiP20LiteExcludedSizes(cameraId, UNKNOWN_IMAGE_FORMAT, klass);
        }
        if (isSamsungJ7PrimeApi27Above()) {
            return getSamsungJ7PrimeApi27AboveExcludedSizes(cameraId, UNKNOWN_IMAGE_FORMAT, klass);
        }
        if (isSamsungJ7Api27Above()) {
            return getSamsungJ7Api27AboveExcludedSizes(cameraId, UNKNOWN_IMAGE_FORMAT, klass);
        }
        Logger.w(TAG, "Cannot retrieve list of supported sizes to exclude on this device.");
        return Collections.emptyList();
    }

    @NonNull
    private List<Size> getOnePlus6ExcludedSizes(@NonNull String cameraId, int imageFormat) {
        final List<Size> sizes = new ArrayList<>();
        if (cameraId.equals("0") && imageFormat == ImageFormat.JPEG) {
            sizes.add(new Size(4160, 3120));
            sizes.add(new Size(4000, 3000));
        }
        return sizes;
    }

    @NonNull
    private List<Size> getOnePlus6TExcludedSizes(@NonNull String cameraId, int imageFormat) {
        final List<Size> sizes = new ArrayList<>();
        if (cameraId.equals("0") && imageFormat == ImageFormat.JPEG) {
            sizes.add(new Size(4160, 3120));
            sizes.add(new Size(4000, 3000));
        }
        return sizes;
    }

    @NonNull
    private List<Size> getHuaweiP20LiteExcludedSizes(@NonNull String cameraId, int imageFormat,
            @Nullable Class<?> klass) {
        final List<Size> sizes = new ArrayList<>();
        // When klass is not null, the list for PRIVATE format should be returned.
        if (cameraId.equals("0") && (
                imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                        || imageFormat == ImageFormat.YUV_420_888 || klass != null)) {
            sizes.add(new Size(720, 720));
            sizes.add(new Size(400, 400));
        }
        return sizes;
    }

    @NonNull
    private List<Size> getSamsungJ7PrimeApi27AboveExcludedSizes(@NonNull String cameraId,
            int imageFormat, @Nullable Class<?> klass) {
        final List<Size> sizes = new ArrayList<>();

        // When klass is not null, the list for PRIVATE format should be returned.
        if (cameraId.equals("0")) {
            if (imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                    || klass != null) {
                sizes.add(new Size(4128, 3096));
                sizes.add(new Size(4128, 2322));
                sizes.add(new Size(3088, 3088));
                sizes.add(new Size(3264, 2448));
                sizes.add(new Size(3264, 1836));
                sizes.add(new Size(2048, 1536));
                sizes.add(new Size(2048, 1152));
                sizes.add(new Size(1920, 1080));
            } else if (imageFormat == ImageFormat.YUV_420_888) {
                sizes.add(new Size(4128, 2322));
                sizes.add(new Size(3088, 3088));
                sizes.add(new Size(3264, 2448));
                sizes.add(new Size(3264, 1836));
                sizes.add(new Size(2048, 1536));
                sizes.add(new Size(2048, 1152));
                sizes.add(new Size(1920, 1080));
            }
        } else if (cameraId.equals("1")) {
            if (imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                    || imageFormat == ImageFormat.YUV_420_888 || klass != null) {
                sizes.add(new Size(3264, 2448));
                sizes.add(new Size(3264, 1836));
                sizes.add(new Size(2448, 2448));
                sizes.add(new Size(1920, 1920));
                sizes.add(new Size(2048, 1536));
                sizes.add(new Size(2048, 1152));
                sizes.add(new Size(1920, 1080));
            }
        }

        return sizes;
    }

    @NonNull
    private List<Size> getSamsungJ7Api27AboveExcludedSizes(@NonNull String cameraId,
            int imageFormat, @Nullable Class<?> klass) {
        final List<Size> sizes = new ArrayList<>();

        // When klass is not null, the list for PRIVATE format should be returned.
        if (cameraId.equals("0")) {
            if (imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                    || klass != null) {
                sizes.add(new Size(4128, 3096));
                sizes.add(new Size(4128, 2322));
                sizes.add(new Size(3088, 3088));
                sizes.add(new Size(3264, 2448));
                sizes.add(new Size(3264, 1836));
                sizes.add(new Size(2048, 1536));
                sizes.add(new Size(2048, 1152));
                sizes.add(new Size(1920, 1080));
            } else if (imageFormat == ImageFormat.YUV_420_888) {
                sizes.add(new Size(2048, 1536));
                sizes.add(new Size(2048, 1152));
                sizes.add(new Size(1920, 1080));
            }
        } else if (cameraId.equals("1")) {
            if (imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                    || imageFormat == ImageFormat.YUV_420_888 || klass != null) {
                sizes.add(new Size(2576, 1932));
                sizes.add(new Size(2560, 1440));
                sizes.add(new Size(1920, 1920));
                sizes.add(new Size(2048, 1536));
                sizes.add(new Size(2048, 1152));
                sizes.add(new Size(1920, 1080));
            }
        }

        return sizes;
    }
}
