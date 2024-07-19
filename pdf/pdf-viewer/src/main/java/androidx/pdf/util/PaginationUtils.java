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

package androidx.pdf.util;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Utils class for [PaginatedView]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PaginationUtils {
    private PaginationUtils() {
    }

    /** {@link View#setElevation(float)} value for PDF Pages (API 21+). */
    public static final int PAGE_ELEVATION_DP = 2;

    /** The spacing added before and after each page, in dip. */
    private static final int PAGE_SPACING_DP = 4;

    /** Converts a value given in dp to pixels, based on the screen density. */
    public static int getPageElevationInPixels(@NonNull Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (PAGE_ELEVATION_DP * density);
    }

    /** Converts a value given in dp to pixels, based on the screen density. */
    public static int getPageSpacingInPixels(@NonNull Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (PAGE_SPACING_DP * density);
    }
}
