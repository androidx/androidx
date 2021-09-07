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

package androidx.window.extensions;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * A class to return global information about the library. From this class you can get the
 * API level supported by the library.
 *
 * @see WindowLibraryInfo#getVendorApiLevel() ()
 */
public class WindowLibraryInfo {

    private WindowLibraryInfo() {}

    /**
     * Returns the API level of the vendor library on the device. If the returned version is not
     * supported by the WindowManager library, then some functions may not be available or replaced
     * with stub implementations.
     *
     * The expected use case is for the WindowManager library to determine which APIs are
     * available and wrap the API so that app developers do not need to deal with the complexity.
     * @return the API level supported by the library.
     */
    public int getVendorApiLevel() {
        return 1;
    }

    @NonNull
    public static WindowLibraryInfo getInstance(@NonNull Context context) {
        return new WindowLibraryInfo();
    }
}
