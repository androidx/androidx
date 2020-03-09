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

package androidx.window.extensions;

import android.graphics.Rect;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Description of a physical feature on the display.
 */
public class ExtensionDisplayFeature {
    /**
     * The bounding rectangle of the feature within the application window in the window
     * coordinate space.
     */
    @NonNull
    private final Rect mBounds;

    /**
     * The physical type of the feature.
     */
    @Type
    private final int mType;

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

    public ExtensionDisplayFeature(@NonNull Rect bounds, @Type int type) {
        mBounds = new Rect(bounds);
        mType = type;
    }

    /** Gets the bounding rect of the display feature in window coordinate space. */
    @NonNull
    public Rect getBounds() {
        return mBounds;
    }

    /** Gets the type of the display feature. */
    @Type
    public int getType() {
        return mType;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ExtensionDisplayFeature)) {
            return false;
        }
        final ExtensionDisplayFeature
                other = (ExtensionDisplayFeature) obj;
        if (mType != other.mType) {
            return false;
        }
        return mBounds.equals(other.mBounds);
    }

    @Override
    public int hashCode() {
        int result = mType;
        result = 31 * result + mBounds.centerX() + mBounds.centerY();
        return result;
    }
}
