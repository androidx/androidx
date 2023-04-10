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

package androidx.camera.core;

import android.graphics.Matrix;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.ExifData;

import com.google.auto.value.AutoValue;

/**
 * @hide
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ImmutableImageInfo implements ImageInfo {

    /**
     * Creates an instance of {@link ImmutableImageInfo}.
     */
    @NonNull
    public static ImageInfo create(@NonNull TagBundle tag, long timestamp,
            int rotationDegrees, @NonNull Matrix sensorToBufferTransformMatrix) {
        return new AutoValue_ImmutableImageInfo(
                tag,
                timestamp,
                rotationDegrees,
                sensorToBufferTransformMatrix);
    }

    @Override
    @NonNull
    public abstract TagBundle getTagBundle();

    @Override
    public abstract long getTimestamp();

    @Override
    public abstract int getRotationDegrees();

    @NonNull
    @Override
    public abstract Matrix getSensorToBufferTransformMatrix();

    @Override
    public void populateExifData(@NonNull ExifData.Builder exifBuilder) {
        // Only have access to orientation information.
        exifBuilder.setOrientationDegrees(getRotationDegrees());
    }
}
