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

package androidx.camera.view.preview.transform.transformation;


import androidx.annotation.RestrictTo;

/**
 * A {@link Transformation} used to correct the camera preview.
 *
 * It only allows setting the scale on the x and y axes and the rotation, since these are the
 * operations involved when correcting the preview.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PreviewCorrectionTransformation extends Transformation {

    /**
     * Creates a {@link Transformation} that corrects the preview. It doesn't translate the
     * preview, keeping it at its initial position of (0, 0).
     */
    public PreviewCorrectionTransformation(float scaleX, float scaleY, float rotation) {
        super(scaleX, scaleY, 0, 0, rotation);
    }
}
