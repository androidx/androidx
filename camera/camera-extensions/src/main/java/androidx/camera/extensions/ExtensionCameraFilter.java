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

package androidx.camera.extensions;

import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.Identifier;
import androidx.camera.extensions.internal.VendorExtender;
import androidx.camera.extensions.internal.compat.quirk.DeviceQuirks;
import androidx.camera.extensions.internal.compat.quirk.ExtensionDisabledQuirk;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A filter that filters camera based on extender implementation. If the implementation is
 * unavailable, the camera will be considered available.
 */
final class ExtensionCameraFilter implements CameraFilter {
    private final Identifier mId;
    private final VendorExtender mVendorExtender;
    private final int mMode;

    ExtensionCameraFilter(
            @NonNull String filterId, @NonNull VendorExtender vendorExtender, int mode) {
        mId = Identifier.create(filterId);
        mVendorExtender = vendorExtender;
        mMode = mode;
    }

    @Override
    public @NonNull Identifier getIdentifier() {
        return mId;
    }

    @Override
    public @NonNull List<CameraInfo> filter(@NonNull List<CameraInfo> cameraInfos) {
        List<CameraInfo> result = new ArrayList<>();
        for (CameraInfo cameraInfo : cameraInfos) {
            Preconditions.checkArgument(cameraInfo instanceof CameraInfoInternal,
                    "The camera info doesn't contain internal implementation.");
            String cameraId = ((CameraInfoInternal) cameraInfo).getCameraId();

            ExtensionDisabledQuirk quirk = DeviceQuirks.get(ExtensionDisabledQuirk.class);
            if (quirk != null && quirk.shouldDisableExtension(cameraId, mMode)) {
                continue;
            }

            if (mVendorExtender.isExtensionAvailable(cameraInfo)) {
                result.add(cameraInfo);
            }
        }

        return result;
    }
}
