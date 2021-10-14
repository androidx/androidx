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
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.Quirk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quirk required to exclude certain supported surface sizes that are problematic. These sizes
 * are dependent on the device, camera and image format.
 * <p>
 * An example is the resolution size 4000x3000 which is supported on OnePlus 6, but causes a WYSIWYG
 * issue between preview and image capture. See b/157448499.
 *
 * <p>On Huawei P20 Lite, the Preview screen will become too bright when 400x400 or 720x720
 * Preview resolutions are used together with a large zoom in value. The same symptom happens on
 * ImageAnalysis. See https://issuetracker.google.com/192129158.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExcludedSupportedSizesQuirk implements Quirk {

    private static final String TAG = "ExcludedSupportedSizesQuirk";

    static boolean load() {
        return isOnePlus6() || isOnePlus6T() || isHuaweiP20Lite();
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
            return getHuaweiP20LiteExcludedSizes(cameraId, imageFormat);
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
    private List<Size> getHuaweiP20LiteExcludedSizes(@NonNull String cameraId, int imageFormat) {
        final List<Size> sizes = new ArrayList<>();
        if (cameraId.equals("0")
                && (imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                || imageFormat == ImageFormat.YUV_420_888)) {
            sizes.add(new Size(720, 720));
            sizes.add(new Size(400, 400));
        }
        return sizes;
    }
}
