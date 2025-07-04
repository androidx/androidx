/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.impl.perception;

import android.util.Log;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A Plane is a type of Trackable that maps to a real world plane (e.g. a floor, a wall, or a table)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Plane implements Trackable {
    private static final String TAG = "PerceptionPlane";
    ArrayList<Anchor> mAttachedAnchors = new ArrayList<>();
    Long mPlaneId = 0L;
    @PerceptionLibraryConstants.OpenXrSpaceType int mReferenceSpaceType = 0;

    public Plane(
            @SuppressWarnings("AutoBoxing") @NonNull Long planeId,
            @PerceptionLibraryConstants.OpenXrSpaceType int referenceSpaceType) {
        mPlaneId = planeId;
        mReferenceSpaceType = referenceSpaceType;
    }

    /**
     * Creates an anchor on the trackable for a point in world space relative to the trackable.
     *
     * @param pose Specifies the pose relative to the center point of the trackable.
     * @param timeNs The monotonic time retrieved using the SystemClock's uptimeMillis() or
     *     uptimeNanos() at which to get the pose, in nanoseconds. If time is null or negative, the
     *     current time will be used.
     */
    @Override
    public @Nullable Anchor createAnchor(
            @NonNull Pose pose, @SuppressWarnings("AutoBoxing") @Nullable Long timeNs) {
        Anchor.AnchorData anchorData =
                createAnchorOnPlane(mPlaneId, pose, timeNs == null ? -1 : timeNs);
        if (anchorData == null) {
            Log.i(TAG, "Failed to create an anchor.");
            return null;
        }
        Log.i(TAG, "Creating an anchor result:" + anchorData.mAnchorToken);
        Anchor anchor = new Anchor(anchorData);
        mAttachedAnchors.add(anchor);
        return anchor;
    }

    /**
     * Retrieves data associated with the plane.
     *
     * @param timeNs The monotonic time retrieved using the SystemClock's uptimeMillis() or
     *     uptimeNanos() at which to get the pose, in nanoseconds. If time is null or negative, the
     *     current time will be used.
     */
    public @Nullable PlaneData getData(@SuppressWarnings("AutoBoxing") @Nullable Long timeNs) {
        return getPlaneData(mPlaneId, mReferenceSpaceType, timeNs == null ? -1 : timeNs);
    }

    /** Returns all anchors attached to this trackable. */
    @Override
    public @NonNull List<Anchor> getAnchors() {
        return mAttachedAnchors;
    }

    private native PlaneData getPlaneData(
            long planeId, int referenceSpaceType, long monotonicTimeNs);

    private native Anchor.AnchorData createAnchorOnPlane(
            long planeId, Pose pose, long monotonicTimeNs);

    /**
     * The direction of the plane. These enum values must be kept in sync with OpenXR
     * XrPlaneTypeANDROID.
     */
    public enum Type {
        /** A horizontal plane facing downward (e.g. a ceiling). */
        HORIZONTAL_DOWNWARD_FACING(0),
        /** A horizontal plane facing upward (e.g. floor or tabletop). */
        HORIZONTAL_UPWARD_FACING(1),
        /** A vertical plane (e.g. a wall). */
        VERTICAL(2),
        /** Any plane type. */
        ARBITRARY(3);

        public final int intValue;

        Type(int intValue) {
            this.intValue = intValue;
        }

        // Converts a native code to the corresponding enum value.
        static Type forNumber(int intValue) {
            for (Type type : values()) {
                if (type.intValue == intValue) {
                    return type;
                }
            }
            return ARBITRARY;
        }
    }

    /**
     * Data on the plane that can be used to determine the type of object that it is. These enum
     * values must be kept in sync with OpenXR XrPlaneLabelANDROID.
     */
    public enum Label {
        /** It was not possible to label the plane. */
        UNKNOWN(0),
        /** The plane is a wall. */
        WALL(1),
        /** The plane is a floor. */
        FLOOR(2),
        /** The plane is a ceiling */
        CEILING(3),
        /** The plane is a table */
        TABLE(4);

        public final int intValue;

        Label(int intValue) {
            this.intValue = intValue;
        }

        // Converts a native code to the corresponding enum value.
        static Label forNumber(int intValue) {
            for (Label label : values()) {
                if (label.intValue == intValue) {
                    return label;
                }
            }
            return UNKNOWN;
        }
    }

    /** Data returned from the native OpenXR to describe the plane. */
    public static class PlaneData {
        /**
         * The pose of the center of the plane. The positive y-axis of the pose will point
         * perpendicular out of the plane's surface.
         */
        public final @NonNull Pose centerPose;

        /** The width of the plane. */
        public final float extentWidth;

        /** The height of the plane. */
        public final float extentHeight;

        /** The direction of the plane. */
        public final Plane.@NonNull Type type;

        /** A label that can be used to determine the type of object that the plane is. */
        public final Plane.@NonNull Label label;

        public PlaneData(
                @NonNull Pose centerPose,
                float extentWidth,
                float extentHeight,
                int type,
                int label) {
            this.centerPose = centerPose;
            this.extentWidth = extentWidth;
            this.extentHeight = extentHeight;
            this.type = Plane.Type.forNumber(type);
            this.label = Plane.Label.forNumber(label);
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof PlaneData) {
                PlaneData that = (PlaneData) object;
                return this.centerPose.equals(that.centerPose)
                        && this.extentWidth == that.extentWidth
                        && this.extentHeight == that.extentHeight
                        && this.type == that.type
                        && this.label == that.label;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + centerPose.hashCode();
            result = 31 * result + Float.floatToIntBits(extentWidth);
            result = 31 * result + Float.floatToIntBits(extentHeight);
            result = 31 * result + type.hashCode();
            result = 31 * result + label.hashCode();
            return result;
        }
    }
}
