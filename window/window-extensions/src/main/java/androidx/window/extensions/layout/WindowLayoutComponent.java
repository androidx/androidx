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

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Consumer;

/**
 * The interface definition that will be used by the WindowManager library to get custom
 * OEM-provided information about the window that isn't covered by platform APIs. Exposes methods
 * to listen to changes in the {@link WindowLayoutInfo}. A {@link WindowLayoutInfo} contains a list
 * of {@link DisplayFeature}s.
 *
 * Currently {@link FoldingFeature} is the only {@link DisplayFeature}. A {@link FoldingFeature}
 * exposes the state of a hinge and the relative bounds within the window. Developers can
 * optimize their UI to support a {@link FoldingFeature} by avoiding it and placing content in
 * relevant logical areas.
 *
 * <p>This interface should be implemented by OEM and deployed to the target devices.
 *
 */
public class WindowLayoutComponent {

    private WindowLayoutComponent() {}

    /**
     * Adds a listener interested in receiving updates to {@link WindowLayoutInfo}
     * @param activity hosting a {@link android.view.Window}
     * @param consumer interested in receiving updates to {@link WindowLayoutInfo}
     */
    public void addWindowLayoutInfoListener(@NonNull Activity activity,
            @NonNull Consumer<WindowLayoutInfo> consumer) {
        throw new UnsupportedOperationException("Stub, replace with implementation.");
    }

    /**
     * Removes a listener no longer interested in receiving updates.
     * @param consumer no longer interested in receiving updates to {@link WindowLayoutInfo}
     */
    public void removeWindowLayoutInfoListener(
            @NonNull Consumer<WindowLayoutInfo> consumer) {
        throw new UnsupportedOperationException("Stub, replace with implementation.");
    }

    /**
     * Returns the OEM implementation of {@link WindowLayoutComponent} if it is supported on the
     * device. The implementation must match the API level reported in
     * {@link androidx.window.extensions.WindowLibraryInfo}. A {@code null} value may be returned
     * if the device does not support {@link WindowLayoutInfo}. If {@code null} is returned the
     * core library will ignore all features related to {@link WindowLayoutComponent}. Currently
     * the developer will receive a single callback with empty data where possible. For
     * synchronous methods we will return a default value or {@code null}.
     * @return the OEM implementation of {@link WindowLayoutComponent}
     */
    @Nullable
    public static WindowLayoutComponent getInstance(@NonNull Context context) {
        throw new UnsupportedOperationException("Stub, replace with implementation.");
    }
}
