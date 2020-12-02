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

package androidx.camera.core.internal.compat.quirk;

import android.os.Build;

import androidx.camera.core.impl.Quirk;

/**
 * Quirk that displays a notification when an image in {@link android.provider.MediaStore} has
 * been deleted.
 *
 * <p> When triggering an image capture, the {@link androidx.camera.core.ImageCapture}
 * use case validates the save location before starting the image capture pipeline. Validating a
 * save location that is a {@link android.net.Uri} in {@link android.provider.MediaStore} involves
 * creating a new row in the user defined table, retrieving an output {@link android.net.Uri}
 * pointing to it, then attempting to open an {@link java.io.OutputStream} to it. The newly
 * created row is deleted at the end of the verification. On Huawei devices, this last step
 * results in the system displaying a notification informing the user that a photo has been
 * deleted. In order to avoid this, validating the image capture save location to
 * {@link android.provider.MediaStore} is skipped on Huawei devices. See b/169497925.
 */
public class HuaweiMediaStoreLocationValidationQuirk implements Quirk {

    static boolean load() {
        return "HUAWEI".equals(Build.BRAND.toUpperCase())
                || "HONOR".equals(Build.BRAND.toUpperCase());
    }

    /**
     * Always skip checking if the image capture save destination in
     * {@link android.provider.MediaStore} is valid.
     */
    public boolean canSaveToMediaStore() {
        return true;
    }
}
