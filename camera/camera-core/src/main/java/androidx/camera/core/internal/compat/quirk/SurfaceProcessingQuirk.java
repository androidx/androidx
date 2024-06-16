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

package androidx.camera.core.internal.compat.quirk;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.Quirks;

/**
 * A Quirk interface denotes devices have specific issue and can be workaround by enabling
 * surface processing (OpenGL) pipeline.
 *
 * <p>Subclasses of this quirk may contain device specific information.
 */
public interface SurfaceProcessingQuirk extends Quirk {

    /**
     * Returns if the device specific issue can be workaround by enabling surface processing
     * (OpenGL) pipeline
     */
    default boolean workaroundBySurfaceProcessing() {
        return true;
    }

    /**
     * Returns if input quirks contains at least one {@link SurfaceProcessingQuirk} which
     * {@link SurfaceProcessingQuirk#workaroundBySurfaceProcessing()} is true.
     */
    static boolean workaroundBySurfaceProcessing(@NonNull Quirks quirks) {
        for (SurfaceProcessingQuirk quirk : quirks.getAll(SurfaceProcessingQuirk.class)) {
            if (quirk.workaroundBySurfaceProcessing()) {
                return true;
            }
        }
        return false;
    }
}
