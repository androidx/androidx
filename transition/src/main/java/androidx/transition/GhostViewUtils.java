/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.transition;

import android.graphics.Matrix;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class GhostViewUtils {

    @Nullable
    static GhostView addGhost(@NonNull View view, @NonNull ViewGroup viewGroup,
            @Nullable Matrix matrix) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // Use the platform implementation on P as we can't backport the shadows drawing.
            return GhostViewPlatform.addGhost(view, viewGroup, matrix);
        } else {
            return GhostViewPort.addGhost(view, viewGroup, matrix);
        }
    }

    static void removeGhost(View view) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // Use the platform implementation on P as we can't backport the shadows drawing.
            GhostViewPlatform.removeGhost(view);
        } else {
            GhostViewPort.removeGhost(view);
        }
    }

    private GhostViewUtils() {
    }
}
