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

import android.view.Surface;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Configuration containing options for configuring the output image data of a pipeline.
 */
public interface Rotation {
    /**
     * Valid integer rotation values.
     */
    @IntDef({Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270})
    @Retention(RetentionPolicy.SOURCE)
    @interface RotationValue {
    }

    /**
     * Valid integer rotation degrees values.
     */
    @IntDef({0, 90, 180, 270})
    @Retention(RetentionPolicy.SOURCE)
    @interface RotationDegreesValue {
    }
}
