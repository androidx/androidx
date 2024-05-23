/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.core.impl.utils;

import android.util.Size;

import androidx.annotation.NonNull;

import java.util.Comparator;

/** Comparator based on area of the given {@link Size} objects. */
public final class CompareSizesByArea implements Comparator<Size> {
    private boolean mReverse = false;

    /** Creates a comparator with standard total ordering. */
    public CompareSizesByArea() {
        this(false);
    }

    /** Creates a comparator which can reverse the total ordering. */
    public CompareSizesByArea(boolean reverse) {
        mReverse = reverse;
    }

    @Override
    public int compare(@NonNull Size lhs, @NonNull Size rhs) {
        // We cast here to ensure the multiplications won't overflow
        int result =
                Long.signum(
                        (long) lhs.getWidth() * lhs.getHeight()
                                - (long) rhs.getWidth() * rhs.getHeight());

        if (mReverse) {
            result *= -1;
        }

        return result;
    }
}
