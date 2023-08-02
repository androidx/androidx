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

package androidx.camera.viewfinder.internal.transform;

import android.graphics.Rect;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

/**
 * Transformation associated the viewfinder output.
 */
@AutoValue
public abstract class TransformationInfo {

    /**
     * Returns the crop rect rectangle.
     */
    @NonNull
    public abstract Rect getCropRect();

    /**
     * Returns the rotation needed to transform the output from sensor to the target
     * rotation.
     */
    @Rotation.RotationDegreesValue
    public abstract int getRotationDegrees();

    /**
     * Returns the target rotation.
     */
    @Rotation.RotationValue
    public abstract int getTargetRotation();

    /**
     * Creates new {@link TransformationInfo}.
     */
    @NonNull
    public static TransformationInfo of(@NonNull Rect cropRect,
            @Rotation.RotationDegreesValue int rotationDegrees,
            @Rotation.RotationValue int targetRotation) {
        return new AutoValue_TransformationInfo(cropRect, rotationDegrees,
                targetRotation);
    }

    TransformationInfo() {
    }
}
