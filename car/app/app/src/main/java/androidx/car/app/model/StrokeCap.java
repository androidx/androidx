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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines the treatment applied to the beginning and end of drawn line segments or progress bar
 * tracks. When drawing a path with a stroke, the cap type/style dictates whether the ends are
 * rounded off, squared, etc. This class contains the allowed types of stroke caps.
 */
@RequiresCarApi(9)
@ExperimentalCarApi
@CarProtocol
@KeepFields
public final class StrokeCap {
    /**
     * Cap types for {@link StrokeCap}.
     */
    @RestrictTo(LIBRARY)
    @IntDef({
            DEFAULT,
            ROUND,
            SQUARE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StrokeCapType {
    }

    /** The default stroke cap style defined by the system. */
    public static final int DEFAULT = 0;

    /** Begin and end contours with a semicircle extension. */
    public static final int ROUND = 1;

    /** Begin and end contours with a half square extension. */
    public static final int SQUARE = 2;

    private StrokeCap() {
    }
}
