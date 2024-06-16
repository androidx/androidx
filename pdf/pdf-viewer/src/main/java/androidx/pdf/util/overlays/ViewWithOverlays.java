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

package androidx.pdf.util.overlays;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Interface to define views which support overlays for comments.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ViewWithOverlays {

    /**
     * Adds the comment overlay to the view with a key identifying the type.
     */
    void addOverlay(@NonNull String key, @NonNull Drawable overlay);
}
