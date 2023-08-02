/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core;

import android.graphics.Matrix;
import android.media.ImageReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.TagBundle;

/**
 * An ImageReaderProxy implementation that allows to modify the ImageInfo data of the images
 * retrieved.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class ModifiableImageReaderProxy extends AndroidImageReaderProxy {
    private volatile TagBundle mTagBundle = null;
    private volatile Long mTimestamp = null;
    private volatile Integer mRotationDegrees = null;
    private volatile Matrix mSensorToBufferTransformMatrix = null;

    ModifiableImageReaderProxy(@NonNull ImageReader imageReader) {
        super(imageReader);
    }

    void setImageTagBundle(@NonNull TagBundle tagBundle) {
        mTagBundle = tagBundle;
    }

    void setImageTimeStamp(long timestamp) {
        mTimestamp = timestamp;
    }

    void setImageRotationDegrees(int rotationDegrees) {
        mRotationDegrees = rotationDegrees;
    }

    void setImageSensorToBufferTransformaMatrix(@NonNull Matrix matrix) {
        mSensorToBufferTransformMatrix = matrix;
    }

    @Nullable
    @Override
    public ImageProxy acquireLatestImage() {
        return modifyImage(super.acquireNextImage());
    }

    @Nullable
    @Override
    public ImageProxy acquireNextImage() {
        return modifyImage(super.acquireNextImage());
    }

    private ImageProxy modifyImage(ImageProxy imageProxy) {
        ImageInfo origin = imageProxy.getImageInfo();
        ImageInfo  imageInfo = ImmutableImageInfo.create(
                mTagBundle != null ? mTagBundle : origin.getTagBundle(),
                mTimestamp != null ? mTimestamp.longValue() : origin.getTimestamp(),
                mRotationDegrees != null ? mRotationDegrees.intValue() :
                        origin.getRotationDegrees(),
                mSensorToBufferTransformMatrix != null ? mSensorToBufferTransformMatrix :
                        origin.getSensorToBufferTransformMatrix());
        return new SettableImageProxy(imageProxy, imageInfo);
    }
}
