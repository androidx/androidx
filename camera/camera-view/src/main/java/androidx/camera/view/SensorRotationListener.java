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

package androidx.camera.view;

import android.content.Context;
import android.view.OrientationEventListener;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Listens to motion sensor reading and converts the orientation degrees to {@link Surface}
 * rotation.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class SensorRotationListener extends OrientationEventListener {

    public static final int INVALID_SURFACE_ROTATION = -1;

    private int mRotation = INVALID_SURFACE_ROTATION;

    public SensorRotationListener(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            // Short-circuit if orientation is unknown. Unknown rotation can't be handled so it
            // shouldn't be sent.
            return;
        }

        int newRotation;
        if (orientation >= 315 || orientation < 45) {
            newRotation = Surface.ROTATION_0;
        } else if (orientation >= 225) {
            newRotation = Surface.ROTATION_90;
        } else if (orientation >= 135) {
            newRotation = Surface.ROTATION_180;
        } else {
            newRotation = Surface.ROTATION_270;
        }
        if (mRotation != newRotation) {
            mRotation = newRotation;
            onRotationChanged(newRotation);
        }
    }

    /**
     * Invoked when rotation changes.
     *
     * <p> The output rotation is defined as the UI Surface rotation, or what the Surface rotation
     * should be if the app's orientation is not locked.
     */
    public abstract void onRotationChanged(int rotation);
}
