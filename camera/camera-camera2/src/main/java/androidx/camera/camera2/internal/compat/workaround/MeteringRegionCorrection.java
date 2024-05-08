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

import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.quirk.AfRegionFlipHorizontallyQuirk;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.impl.Quirks;

/**
 * Correct the metering point if necessary. For some devices, Af region coordinates are flipped
 * horizontally by OEM, hence we should flip it to fix the issue.
 */
public class MeteringRegionCorrection {
    private final Quirks mCameraQuirks;
    public MeteringRegionCorrection(@NonNull Quirks cameraQuirks) {
        mCameraQuirks = cameraQuirks;
    }

    /**
     * Return corrected normalized point by given MeteringPoint and MeteringMode.
     */
    @NonNull
    public PointF getCorrectedPoint(@NonNull MeteringPoint meteringPoint,
            @FocusMeteringAction.MeteringMode int meteringMode) {
        if (meteringMode == FocusMeteringAction.FLAG_AF
                && mCameraQuirks.contains(AfRegionFlipHorizontallyQuirk.class)) {
            return new PointF(1f - meteringPoint.getX(), meteringPoint.getY());
        }

        return new PointF(meteringPoint.getX(), meteringPoint.getY());
    }
}
