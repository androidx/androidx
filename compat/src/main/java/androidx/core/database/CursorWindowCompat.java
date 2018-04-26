/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.database;

import android.database.CursorWindow;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BuildCompat;

/**
 * Helper for accessing features in {@link android.database.CursorWindow}
 */
public final class CursorWindowCompat {

    private CursorWindowCompat() {
        /* Hide constructor */
    }

    /**
     * Creates a CursorWindow of the specified size.
     * <p>
     * Prior to Android P, this method will return a CursorWindow of size defined by the platform.
     */
    @NonNull
    public CursorWindow create(@Nullable String name, long windowSizeBytes) {
        if (BuildCompat.isAtLeastP()) {
            return new CursorWindow(name, windowSizeBytes);
        } else if (Build.VERSION.SDK_INT >= 15) {
            return new CursorWindow(name);
        } else {
            //noinspection deprecation
            return new CursorWindow(false);
        }
    }
}
