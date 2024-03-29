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
 * Declares the log tags to use in the library.
 *
 */
@RestrictTo(LIBRARY)
public final class LogTags {
    /** Tag to use for logging in the library. */
    public static final String TAG = "CarApp";

    /** Tag to use for IPC dispatching */
    public static final String TAG_DISPATCH = TAG + ".Dispatch";

    /** Tag to use for host validation */
    public static final String TAG_HOST_VALIDATION = TAG + ".Val";

    /** Tag to use for navigation manager. */
    public static final String TAG_NAVIGATION_MANAGER = TAG + ".Nav";

    /** Tag to use for serialization. */
    public static final String TAG_BUNDLER = TAG + ".Bun";

    /** Tag to use for checking connection to a car head unit. */
    public static final String TAG_CONNECTION_TO_CAR = TAG + ".Conn";

    /** Tag to use for car hardware related issues. */
    public static final String TAG_CAR_HARDWARE = TAG + ".Hardware";

    private LogTags() {
    }
}
