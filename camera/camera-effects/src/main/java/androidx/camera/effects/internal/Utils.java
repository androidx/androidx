/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.effects.internal;

import android.graphics.Canvas;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Utility methods for overlay processing.
 */
public class Utils {

    private Utils() {
    }

    /**
     * Locks the Canvas on the given Surface.
     *
     * <p>This method calls {@link Surface#lockCanvas} or {@link Surface#lockHardwareCanvas()}
     * depending on the API level.
     */
    @NonNull
    public static Canvas lockCanvas(@NonNull Surface surface) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Api23Impl.lockHardwareCanvas(surface);
        } else {
            return surface.lockCanvas(null);
        }
    }

    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static Canvas lockHardwareCanvas(Surface surface) {
            return surface.lockHardwareCanvas();
        }
    }
}
