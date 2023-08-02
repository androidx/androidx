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

package androidx.camera.camera2.internal.compat.workaround;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.quirk.ConfigureSurfaceToSecondarySessionFailQuirk;
import androidx.camera.camera2.internal.compat.quirk.PreviewOrientationIncorrectQuirk;
import androidx.camera.camera2.internal.compat.quirk.TextureViewIsClosedQuirk;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.Quirks;

import java.util.List;

/**
 * The workaround is used to close the {@link androidx.camera.core.impl.DeferrableSurface} when the
 * {@link android.hardware.camera2.CameraCaptureSession} is closed. The next CameraCaptureSession
 * will throw an exception when attempting to retrieve the underlying Surface.
 * The Camera now posts an error to the UseCase signaling the DeferrableSurface needs to be
 * recreated.
 *
 * @see TextureViewIsClosedQuirk
 * @see PreviewOrientationIncorrectQuirk
 * @see ConfigureSurfaceToSecondarySessionFailQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ForceCloseDeferrableSurface {
    private final boolean mHasTextureViewIsClosedQuirk;
    private final boolean mHasPreviewOrientationIncorrectQuirk;
    private final boolean mHasConfigureSurfaceToSecondarySessionFailQuirk;

    /** Constructor of the ForceCloseDeferrableSurface workaround */
    public ForceCloseDeferrableSurface(@NonNull Quirks cameraQuirks,
            @NonNull Quirks deviceQuirks) {
        mHasTextureViewIsClosedQuirk = deviceQuirks.contains(TextureViewIsClosedQuirk.class);
        mHasPreviewOrientationIncorrectQuirk = cameraQuirks.contains(
                PreviewOrientationIncorrectQuirk.class);
        mHasConfigureSurfaceToSecondarySessionFailQuirk =
                cameraQuirks.contains(ConfigureSurfaceToSecondarySessionFailQuirk.class);
    }

    /**
     * Return true if any of the TextureViewIsClosedQuirk, PreviewOrientationIncorrectQuirk,
     * ConfigureSurfaceToSecondarySessionFailQuirk is enabled.
     */
    public boolean shouldForceClose() {
        return mHasTextureViewIsClosedQuirk || mHasPreviewOrientationIncorrectQuirk
                || mHasConfigureSurfaceToSecondarySessionFailQuirk;
    }

    /**
     * Close the {@link DeferrableSurface} to force the {@link DeferrableSurface} to be recreated
     * in the new CameraCaptureSession.
     */
    public void onSessionEnd(@Nullable List<DeferrableSurface> deferrableSurfaces) {
        if (shouldForceClose() && deferrableSurfaces != null) {
            for (DeferrableSurface deferrableSurface : deferrableSurfaces) {
                deferrableSurface.close();
            }
            Logger.d("ForceCloseDeferrableSurface", "deferrableSurface closed");
        }
    }
}
