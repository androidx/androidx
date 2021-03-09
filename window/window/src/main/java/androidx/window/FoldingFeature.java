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

    /**
     * The {@link FoldingFeature} does not occlude the content in any way. One example is a flat
     * continuous fold where content can stretch across the fold. Another example is a hinge that
     * has width or height equal to 0. In this case the content is physically split across both
     * displays, but fully visible.
     */
    public static final int OCCLUSION_NONE = 0;

    /**
     * The {@link FoldingFeature} occludes all content. One example is a hinge that is considered to
     * be part of the window, so that part of the UI is not visible to the user. Any content shown
     * in the same area as the hinge may not be accessible in any way. Fully occluded areas should
     * always be avoided when placing interactive UI elements and text.
     */
    public static final int OCCLUSION_FULL = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            OCCLUSION_NONE,
            OCCLUSION_FULL
    })
    @interface OcclusionType {}

    /**
     * The height of the {@link FoldingFeature} is greater than or equal to the width.
     */
    public static final int ORIENTATION_VERTICAL = 0;

    /**
     * The width of the {@link FoldingFeature} is greater than the height.
     */
    public static final int ORIENTATION_HORIZONTAL = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            ORIENTATION_HORIZONTAL,
            ORIENTATION_VERTICAL
    })
    @interface Orientation {}

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
        validateState(state);
        validateType(type);
        validateFeatureBounds(bounds);
        mBounds = new Rect(bounds);
        mType = type;
        mState = state;
    }

    @NonNull
    @Override
    public Rect getBounds() {
        return new Rect(mBounds);
    }

    /**
     * Returns type that is either {@link FoldingFeature#TYPE_FOLD} or
     * {@link FoldingFeature#TYPE_HINGE}
     * @deprecated visibility will be reduced.
     */
    @Type
    @Deprecated
    public int getType() {
        return mType;
    }

    @State
    public int getState() {
        return mState;
    }

    /**
     * Calculates if a {@link FoldingFeature} should be thought of as splitting the window into
     * multiple physical areas that can be seen by users as logically separate. Display panels
     * connected by a hinge are always separated. Folds on flexible screens should be treated as
     * separating when they are not {@link FoldingFeature#STATE_FLAT}.
     *
     * Apps may use this to determine if content should lay out around the {@link FoldingFeature}.
     * Developers should consider the placement of interactive elements. Similar to the case of
     * {@link FoldingFeature#OCCLUSION_FULL}, when a feature is separating then consider laying
     * out the controls around the {@link FoldingFeature}.
     *
     * An example use case is to determine if the UI should be split into two logical areas. A media
     * app where there is some auxiliary content, such as comments or description of a video, may
     * need to adapt the layout. The media can be put on one side of the {@link FoldingFeature} and
     * the auxiliary content can be placed on the other side.
     *
     * @return {@code true} if the feature splits the display into two areas, {@code false}
     * otherwise.
     */
    public boolean isSeparating() {
        if (mType == TYPE_HINGE) {
            return true;
        }
        if (mType == TYPE_FOLD && (mState == STATE_FLIPPED || mState == STATE_HALF_OPENED)) {
            return true;
        }
        return false;
    }

    /**
     * Calculates the occlusion mode to determine if a {@link FoldingFeature} occludes a part of
     * the window. This flag is useful for determining if UI elements need to be moved
     * around so that the user can access them. For some devices occluded elements can not be
     * accessed by the user at all.
     *
     * For occlusion type {@link FoldingFeature#OCCLUSION_NONE} the feature can be treated as a
     * guideline. One example would be for a continuously folding screen. For occlusion type
     * {@link FoldingFeature#OCCLUSION_FULL} the feature should be avoided completely since content
     * will not be visible or touchable, like a hinge device with two displays.
     *
     * The occlusion mode is useful to determine if the UI needs to adapt to the
     * {@link FoldingFeature}. For example, full screen games should consider avoiding anything in
     * the occluded region if it negatively affects the gameplay.  The user can not tap
     * on the occluded interactive UI elements nor can they see important information.
     *
     * @return {@link FoldingFeature#OCCLUSION_NONE} if the {@link FoldingFeature} has empty
     * bounds.
     */
    @OcclusionType
    public int getOcclusionMode() {
        if (mBounds.width() == 0 || mBounds.height() == 0) {
            return OCCLUSION_NONE;
        }
        return OCCLUSION_FULL;
    }

    /**
     * Returns {@link FoldingFeature#ORIENTATION_HORIZONTAL} if the width is greater than the
     * height, {@link FoldingFeature#ORIENTATION_VERTICAL} otherwise.
     */
    @Orientation
    public int getOrientation() {
        return mBounds.width() > mBounds.height()
                ? ORIENTATION_HORIZONTAL
                : ORIENTATION_VERTICAL;
    }

    static String occlusionTypeToString(@OcclusionType int type) {
        switch (type) {
            case OCCLUSION_NONE:
                return "OCCLUSION_NONE";
            case OCCLUSION_FULL:
                return "OCCLUSION_FULL";
            default:
                return "UNKNOWN";
        }
    }

    static String orientationToString(@Orientation int direction) {
        switch (direction) {
            case ORIENTATION_HORIZONTAL:
                return "ORIENTATION_HORIZONTAL";
            case ORIENTATION_VERTICAL:
                return "ORIENTATION_VERTICAL";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Verifies the state is {@link FoldingFeature#STATE_FLAT},
     * {@link FoldingFeature#STATE_HALF_OPENED} or {@link FoldingFeature#STATE_FLIPPED}.
     */
    private static void validateState(int state) {
        if (state != STATE_FLAT && state != STATE_HALF_OPENED && state != STATE_FLIPPED) {
            throw new IllegalArgumentException("State must be either " + stateToString(STATE_FLAT)
                    + ", " + stateToString(STATE_HALF_OPENED) + ", or "
                    + stateToString(STATE_FLIPPED));
        }
    }

    /**
     * Verifies the type is either {@link FoldingFeature#TYPE_HINGE} or
     * {@link FoldingFeature#TYPE_FOLD}
     */
    private static void validateType(int type) {
        if (type != TYPE_FOLD && type != TYPE_HINGE) {
            throw new IllegalArgumentException("Type must be either " + typeToString(TYPE_FOLD)
                    + " or " + typeToString(TYPE_HINGE));
        }
    }

    /**
     * Verifies the bounds of the folding feature.
     */
    private static void validateFeatureBounds(@NonNull Rect bounds) {
        if (bounds.width() == 0 && bounds.height() == 0) {
            throw new IllegalArgumentException("Bounds must be non zero");
        }
        if (bounds.left != 0 && bounds.top != 0) {
            throw new IllegalArgumentException("Bounding rectangle must start at the top or "
                    + "left window edge for folding features");
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
                + typeToString(mType) + ", state=" + stateToString(mState) + " }";
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
