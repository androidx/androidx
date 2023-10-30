/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.extensions.embedding;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.window.extensions.RequiresVendorApiLevel;

/**
 * Attributes used to update the layout of an {@link ActivityStack}.
 */
@RequiresVendorApiLevel(level = 5)
public class ActivityStackAttributes {

    @NonNull
    private final Rect mRelativeBounds;

    ActivityStackAttributes(@NonNull Rect relativeBounds) {
        mRelativeBounds = relativeBounds;
    }

    /**
     * Returns the requested bounds of an {@link ActivityStack} which relative to its parent
     * container.
     * <p>
     * {@link Rect#isEmpty() Empty} bounds mean that this {@link ActivityStack} should fill its
     * parent container bounds.
     */
    @NonNull
    public Rect getRelativeBounds() {
        return mRelativeBounds;
    }

    @Override
    public int hashCode() {
        return mRelativeBounds.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ActivityStackAttributes)) return false;
        final ActivityStackAttributes attrs = (ActivityStackAttributes) obj;
        return mRelativeBounds.equals(attrs.mRelativeBounds);
    }

    @NonNull
    @Override
    public String toString() {
        return ActivityStackAttributes.class.getSimpleName() + ": {"
                + " , relativeBounds=" + mRelativeBounds
                + "}";
    }

    /** The builder class of {@link ActivityStackAttributes} */
    public static final class Builder {

        @NonNull
        private final Rect mRelativeBounds = new Rect();

        /**
         * Sets the requested relative bounds of the {@link ActivityStack}. If this value is
         * not specified, {@link #getRelativeBounds()} defaults to {@link Rect#isEmpty() empty}
         * bounds, which means to follow the parent container bounds.
         *
         * @param relativeBounds The requested relative bounds
         * @return the builder class
         */
        @NonNull
        public Builder setRelativeBounds(@NonNull Rect relativeBounds) {
            mRelativeBounds.set(relativeBounds);
            return this;
        }

        /**
         * Builds an {@link ActivityStackAttributes} instance.
         */
        @NonNull
        public ActivityStackAttributes build() {
            return new ActivityStackAttributes(mRelativeBounds);
        }
    }
}
