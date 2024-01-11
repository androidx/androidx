/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.extensions.internal;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.RestrictedCameraControl;
import androidx.camera.core.impl.SessionProcessor;
import androidx.camera.extensions.CameraExtensionsControl;
import androidx.core.util.Preconditions;

/**
 * A {@link CameraExtensionsControl} implementation that wraps a {@link RestrictedCameraControl} to
 * provide extensions-specific functions.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CameraExtensionsControlImpl implements CameraExtensionsControl {
    @Nullable
    private final SessionProcessor mSessionProcessor;

    public CameraExtensionsControlImpl(
            @NonNull RestrictedCameraControl restrictedCameraControl) {
        mSessionProcessor = restrictedCameraControl.getSessionProcessor();
    }

    @Override
    public void setExtensionStrength(@IntRange(from = 0, to = 100) int strength) {
        Preconditions.checkArgumentInRange(strength, 0, 100, "strength");
        // Do nothing when extension strength is not supported.
        if (mSessionProcessor == null || !mSessionProcessor.isExtensionStrengthAvailable()) {
            return;
        }

        mSessionProcessor.setExtensionStrength(strength);
    }
}
