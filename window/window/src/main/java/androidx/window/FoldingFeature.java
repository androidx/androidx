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
 * A feature that describes a fold in the flexible display
 * or a hinge between two physical display panels.
 */
public class FoldingFeature implements DisplayFeature {

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

    /**
     * The foldable device is completely open, the screen space that is presented to the user is
     * flat. See the
     * <a href="https://developer.android.com/guide/topics/ui/foldables#postures">Posture</a>
     * section in the official documentation for visual samples and references.
     */
    public static final int STATE_FLAT = 1;

    /**
     * The foldable device's hinge is in an intermediate position between opened and closed state,
     * there is a non-flat angle between parts of the flexible screen or between physical screen
     * panels. See the
     * <a href="https://developer.android.com/guide/topics/ui/foldables#postures">Posture</a>
     * section in the official documentation for visual samples and references.
     */
    public static final int STATE_HALF_OPENED = 2;

    /**
     * The foldable device is flipped with the flexible screen parts or physical screens facing
     * opposite directions. See the
     * <a href="https://developer.android.com/guide/topics/ui/foldables#postures">Posture</a>
     * section in the official documentation for visual samples and references.
     */
    public static final int STATE_FLIPPED = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            STATE_HALF_OPENED,
            STATE_FLAT,
            STATE_FLIPPED,
    })
    @interface State {}

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
     * The state of the feature.
     */
    @State
    private final int mState;

    public FoldingFeature(@NonNull Rect bounds, @Type int type, @State int state) {
        validateFeatureBounds(bounds, type);
        mBounds = new Rect(bounds);
        mType = type;
        mState = state;
    }

    @NonNull
    @Override
    public Rect getBounds() {
        return new Rect(mBounds);
    }

    @Type
    public int getType() {
        return mType;
    }

    @State
    public int getState() {
        return mState;
    }

    /**
     * Verifies the bounds of the folding feature.
     */
    private static void validateFeatureBounds(@NonNull Rect bounds, int type) {
        if (bounds.width() == 0 && bounds.height() == 0) {
            throw new IllegalArgumentException("Bounds must be non zero");
        }
        if (type == TYPE_FOLD) {
            if (bounds.width() != 0 && bounds.height() != 0) {
                throw new IllegalArgumentException("Bounding rectangle must be either zero-wide "
                        + "or zero-high for features of type " + typeToString(type));
            }

            if ((bounds.width() != 0 && bounds.left != 0)
                    || (bounds.height() != 0 && bounds.top != 0)) {
                throw new IllegalArgumentException("Bounding rectangle must span the entire "
                        + "window space for features of type " + typeToString(type));
            }
        } else if (type == TYPE_HINGE) {
            if (bounds.left != 0 && bounds.top != 0) {
                throw new IllegalArgumentException("Bounding rectangle must span the entire "
                        + "window space for features of type " + typeToString(type));
            }
        }
    }

    @NonNull
    private static String typeToString(int type) {
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
    private static String stateToString(int state) {
        switch (state) {
            case STATE_FLAT:
                return "FLAT";
            case STATE_FLIPPED:
                return "FLIPPED";
            case STATE_HALF_OPENED:
                return "HALF_OPENED";
            default:
                return "Unknown feature state (" + state + ")";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return FoldingFeature.class.getSimpleName() + " { " + mBounds + ", type="
                + typeToString(getType()) + ", state=" + stateToString(mState) + " }";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FoldingFeature)) return false;
        FoldingFeature that = (FoldingFeature) o;
        return mType == that.mType
            && mState == that.mState
            && mBounds.equals(that.mBounds);
    }

    @Override
    public int hashCode() {
        int result = mBounds.hashCode();
        result = 31 * result + mType;
        result = 31 * result + mState;
        return result;
    }
}
