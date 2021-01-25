/*
 * Copyright 2020 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.RestrictTo;

/**
 * Assorted common utilities.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public final class CommonUtils {
    /** Tag to use for logging in the library. */
    public static final String TAG = "CarApp";

    /** Tag to use for host validation */
    public static final String TAG_HOST_VALIDATION = "CarApp.Val";

    private CommonUtils() {
    }
}
