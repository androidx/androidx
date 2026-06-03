/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.core.platform;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Typeface;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/** Interface representing a resolved font instance. */
@RestrictTo(LIBRARY_GROUP)
public interface FontInstance {
    /** Returns the Typeface for this font instance. */
    @NonNull Typeface getTypeface();

    /**
     * Applies font variation settings to this font instance and returns the updated Typeface. If
     * variation settings are not supported, returns the current Typeface.
     */
    @NonNull Typeface applyVariationSettings(@NonNull String[] tags, float @NonNull [] values);

    /** Sets a listener to be notified when the font is loaded. */
    void setOnLoadedListener(@NonNull Runnable listener);
}
