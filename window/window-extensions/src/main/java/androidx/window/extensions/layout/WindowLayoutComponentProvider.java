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

package androidx.window.extensions.layout;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.window.extensions.WindowLibraryInfo;

/**
 * A class to provide instances of {@link WindowLayoutComponent}. An OEM must implement
 * {@link WindowLayoutComponentProvider#isWindowLayoutComponentAvailable()} and
 * {@link WindowLayoutComponentProvider#getWindowLayoutComponent(Context)} for the core window
 * library to support {@link DisplayFeature}s. Any implementation of
 * {@link WindowLayoutComponent} must support the methods up to
 * {@link WindowLibraryInfo#getApiLevel()}
 */
public class WindowLayoutComponentProvider {

    private WindowLayoutComponentProvider() {}

    /**
     * Returns {@code true} if {@link WindowLayoutComponent} is present on the device,
     * {@code false} otherwise.
     */
    public static boolean isWindowLayoutComponentAvailable() {
        throw new UnsupportedOperationException("Stub, replace with implementation.");
    }

    /**
     * Returns the OEM implementation of {@link WindowLayoutComponent} if it is supported on the
     * device. The implementation must match the API level reported in
     * {@link androidx.window.extensions.WindowLibraryInfo}. An
     * {@link UnsupportedOperationException} will be thrown if the device does not support
     * {@link WindowLayoutInfo}. Use
     * {@link WindowLayoutComponentProvider#isWindowLayoutComponentAvailable()} to determine if
     * {@link WindowLayoutComponent} is present.
     * @return the OEM implementation of {@link WindowLayoutComponent}
     */
    @NonNull
    public static WindowLayoutComponent getWindowLayoutComponent(@NonNull Context context) {
        throw new UnsupportedOperationException("Stub, replace with implementation.");
    }
}
