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

import androidx.annotation.RestrictTo;

/**
 * Utils for calculating scale and zoom operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ZoomUtils {
    private ZoomUtils() {
    }

    /** Returns the zoom that would fit the inner rect into the outer rect. */
    public static float calculateZoomToFit(
            float outerWidth, float outerHeight, float innerWidth, float innerHeight) {
        if (innerWidth == 0 || innerHeight == 0) {
            return 1;
        }
        if (RectUtils.widthIsLimitingDimension(outerWidth, outerHeight, innerWidth, innerHeight)) {
            return outerWidth / innerWidth;
        } else {
            return outerHeight / innerHeight;
        }
    }
}
