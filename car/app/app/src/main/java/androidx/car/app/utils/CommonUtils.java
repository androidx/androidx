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

package androidx.car.app.utils;

import static android.content.pm.PackageManager.FEATURE_AUTOMOTIVE;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.util.Objects.requireNonNull;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Assorted common utilities.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class CommonUtils {

    /**
     * Returns whether the {@code context} is for an Automotive OS device.
     *
     * @throws NullPointerException if {@code context} is {@code null}
     */
    public static boolean isAutomotiveOS(@NonNull Context context) {
        return requireNonNull(context).getPackageManager().hasSystemFeature(FEATURE_AUTOMOTIVE);
    }

    private CommonUtils() {}
}
