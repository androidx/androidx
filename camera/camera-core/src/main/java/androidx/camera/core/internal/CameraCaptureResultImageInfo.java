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

package androidx.camera.core.internal;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.ExifData;

/** An ImageInfo that is created by a {@link CameraCaptureResult}. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CameraCaptureResultImageInfo implements ImageInfo {
    private final CameraCaptureResult mCameraCaptureResult;

    /** Create an {@link ImageInfo} using data from {@link CameraCaptureResult}. */
    public CameraCaptureResultImageInfo(@NonNull CameraCaptureResult cameraCaptureResult) {
        mCameraCaptureResult = cameraCaptureResult;
    }

    @Override
    @NonNull
    public TagBundle getTagBundle() {
        return mCameraCaptureResult.getTagBundle();
    }

    @Override
    public long getTimestamp() {
        return mCameraCaptureResult.getTimestamp();
    }

    @Override
    public int getRotationDegrees() {
        return 0;
    }

    @Override
    public void populateExifData(@NonNull ExifData.Builder exifBuilder) {
        mCameraCaptureResult.populateExifData(exifBuilder);
    }

    @NonNull
    public CameraCaptureResult getCameraCaptureResult() {
        return mCameraCaptureResult;
    }
}
