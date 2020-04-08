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

import android.graphics.Rect;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Description of a physical feature on the display.
 *
 * <p>A display feature is a distinctive physical attribute located within the display panel of
 * the device. It can intrude into the application window space and create a visual distortion,
 * visual or touch discontinuity, make some area invisible or create a logical divider or separation
 * in the screen space.
 *
 * @see #TYPE_FOLD
 * @see #TYPE_HINGE
 */
public final class DisplayFeature {
    private final Rect mBounds;
    @Type
    private int mType;

    DisplayFeature(@NonNull Rect bounds, @Type int type) {
        if (bounds.height() == 0 && bounds.width() == 0) {
            throw new IllegalArgumentException("Bounding rectangle must not be empty: " + bounds);
        }
        mBounds = new Rect(bounds);
        mType = type;
    }

    /**
     * Gets bounding rectangle of the physical display feature in the coordinate space of the
     * window. The rectangle provides information about the location of the feature in the window
     * and its size.
     */
    @NonNull
    public Rect getBounds() {
        return mBounds;
    }

    /**
     * Gets type of the physical display feature.
     */
    @Type
    public int getType() {
        return mType;
    }

    /**
     * A fold in the flexible screen without a physical gap.
     */
    public static final int TYPE_FOLD = 1;

    /**
     * A physical separation with a hinge that allows two display panels to fold.
     */
    public static final int TYPE_HINGE = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            TYPE_FOLD,
            TYPE_HINGE,
    })
    @interface Type{}

    private String typeToString(@Type int type) {
        switch (type) {
            case TYPE_FOLD:
                return "FOLD";
            case TYPE_HINGE:
                return "HINGE";
            default:
                return "Unknown feature type (" + type + ")";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "DisplayFeature{ bounds=" + mBounds + ", type=" + typeToString(mType) + " }";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DisplayFeature that = (DisplayFeature) o;

        return mType == that.mType && mBounds.equals(that.mBounds);
    }

    @Override
    public int hashCode() {
        int result = mBounds.hashCode();
        result = 31 * result + mType;
        return result;
    }

    /**
     * Builder for {@link DisplayFeature} objects.
     */
    public static class Builder {
        private Rect mBounds = new Rect();
        @Type
        private int mType = TYPE_FOLD;

        /**
         * Creates an initially empty builder.
         */
        public Builder() {
        }

        /**
         * Sets the bounds for the {@link DisplayFeature} instance.
         */
        @NonNull
        public Builder setBounds(@NonNull Rect bounds) {
            mBounds = bounds;
            return this;
        }

        /**
         * Sets the type for the {@link DisplayFeature} instance.
         */
        @NonNull
        public Builder setType(@Type int type) {
            mType = type;
            return this;
        }

        /**
         * Creates a {@link DisplayFeature} instance with the specified fields.
         * @return A DisplayFeature instance.
         */
        @NonNull
        public DisplayFeature build() {
            return new DisplayFeature(mBounds, mType);
        }
    }
}
