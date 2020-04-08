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

package androidx.window;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Contains the list of {@link DisplayFeature}-s located within the window. For example, a hinge or
 * display fold can go across the window, in which case it might make sense to separate the
 * visual content and interactive elements into two groups, e.g. master-detail or view-controls.
 * <p>Only the features that are present within the current window bounds are reported. Their
 * positions and sizes can change if the window is moved or resized on screen.
 * @see WindowManager#registerLayoutChangeCallback(Executor, Consumer) for tracking changes in
 * display feature list and positions.
 */
public final class WindowLayoutInfo {
    private final List<DisplayFeature> mDisplayFeatures;

    WindowLayoutInfo(@NonNull List<DisplayFeature> displayFeatures) {
        mDisplayFeatures = new ArrayList<>();
        mDisplayFeatures.addAll(displayFeatures);
    }

    /**
     * Gets the list of physical display features within the window.
     */
    @NonNull
    public List<DisplayFeature> getDisplayFeatures() {
        return mDisplayFeatures;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WindowLayoutInfo{ DisplayFeatures[");
        for (int i = 0; i < mDisplayFeatures.size(); i++) {
            DisplayFeature feature = mDisplayFeatures.get(i);
            sb.append(feature);
            if (i < mDisplayFeatures.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("] }");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WindowLayoutInfo that = (WindowLayoutInfo) o;

        return mDisplayFeatures.equals(that.mDisplayFeatures);
    }

    @Override
    public int hashCode() {
        return mDisplayFeatures.hashCode();
    }

    /**
     * Builder for {@link WindowLayoutInfo} objects.
     */
    public static class Builder {
        private List<DisplayFeature> mDisplayFeatures = new ArrayList<>();

        /**
         * Creates an initially empty builder.
         */
        public Builder() {
        }

        /**
         * Sets the display features for the {@link WindowLayoutInfo} instance.
         */
        @NonNull
        public Builder setDisplayFeatures(@NonNull List<DisplayFeature> displayFeatures) {
            mDisplayFeatures.clear();
            mDisplayFeatures.addAll(displayFeatures);
            return this;
        }

        /**
         * Creates a {@link WindowLayoutInfo} instance with the specified fields.
         * @return A WindowLayoutInfo instance.
         */
        @NonNull
        public WindowLayoutInfo build() {
            return new WindowLayoutInfo(mDisplayFeatures);
        }
    }
}
