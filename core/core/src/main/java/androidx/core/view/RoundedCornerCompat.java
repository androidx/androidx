/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.view;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Point;
import android.view.RoundedCorner;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a rounded corner of the display.
 *
 * <p>Note: The center coordinates and radius are an approximation of the actual display corner
 * (based on the display's rounded corner shape).</p>
 *
 * <p>{@link RoundedCornerCompat} is immutable.</p>
 */
public final class RoundedCornerCompat {

    /** The rounded corner is at the top-left of the screen. */
    public static final int POSITION_TOP_LEFT = 0;

    /** The rounded corner is at the top-right of the screen. */
    public static final int POSITION_TOP_RIGHT = 1;

    /** The rounded corner is at the bottom-right of the screen. */
    public static final int POSITION_BOTTOM_RIGHT = 2;

    /** The rounded corner is at the bottom-left of the screen. */
    public static final int POSITION_BOTTOM_LEFT = 3;

    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            POSITION_TOP_LEFT,
            POSITION_TOP_RIGHT,
            POSITION_BOTTOM_RIGHT,
            POSITION_BOTTOM_LEFT
    })
    public @interface Position {}

    private final @Position int mPosition;
    private final int mRadius;
    private final @NonNull Point mCenter;

    /**
     * Creates a {@link RoundedCornerCompat}.
     *
     * <p>Note that this is only useful for tests. For production code, developers should always
     * use a {@link RoundedCornerCompat} obtained from the system via
     * {@link WindowInsetsCompat#getRoundedCorner} or {@link DisplayCompat#getRoundedCorner}.</p>
     *
     * @param position the position of the rounded corner.
     * @param radius the radius of the rounded corner.
     * @param centerX the x of center point of the rounded corner.
     * @param centerY the y of center point of the rounded corner.
     */
    @RestrictTo(LIBRARY_GROUP)
    public RoundedCornerCompat(@Position int position, int radius, int centerX, int centerY) {
        mPosition = position;
        mRadius = radius;
        mCenter = new Point(centerX, centerY);
    }

    /**
     * Same as {@link #RoundedCornerCompat(int,int,int,int)} but accepting a {@link Point} as the
     * center.
     */
    private RoundedCornerCompat(@Position int position, int radius, @NonNull Point center) {
        this(position, radius, center.x, center.y);
    }

    private static int toCompatPosition(int position) {
        switch (position) {
            case 0:
                return POSITION_TOP_LEFT;
            case 1:
                return POSITION_TOP_RIGHT;
            case 2:
                return POSITION_BOTTOM_RIGHT;
            case 3:
                return POSITION_BOTTOM_LEFT;
        }
        throw new IllegalArgumentException("Invalid position: " + position);
    }

    /**
     * Converts a platform {@link RoundedCorner} into a {@link RoundedCornerCompat}.
     *
     * @param rc the given rounded corner.
     * @return the compatible version of rounded corner.
     */
    @RequiresApi(31)
    static @Nullable RoundedCornerCompat toRoundedCornerCompat(@Nullable RoundedCorner rc) {
        return rc != null
                ? new RoundedCornerCompat(
                        toCompatPosition(rc.getPosition()),
                        rc.getRadius(),
                        rc.getCenter())
                : null;
    }

    static int toPlatformPosition(@Position int position) {
        switch (position) {
            case POSITION_TOP_LEFT:
                return 0;
            case POSITION_TOP_RIGHT:
                return 1;
            case POSITION_BOTTOM_RIGHT:
                return 2;
            case POSITION_BOTTOM_LEFT:
                return 3;
        }
        throw new IllegalArgumentException("Invalid position: " + position);
    }

    /**
     * Converts a {@link RoundedCornerCompat} into a platform {@link RoundedCorner}.
     *
     * @param rcc the given compatible rounded corner.
     * @return the platform version of rounded corner.
     */
    @RequiresApi(31)
    static @Nullable RoundedCorner toPlatformRoundedCorner(@Nullable RoundedCornerCompat rcc) {
        return rcc != null
                ? new RoundedCorner(
                        toPlatformPosition(rcc.getPosition()),
                        rcc.getRadius(),
                        rcc.getCenterX(),
                        rcc.getCenterY())
                : null;
    }

    /**
     * Get the position of this {@link RoundedCornerCompat}.
     *
     * @see #POSITION_TOP_LEFT
     * @see #POSITION_TOP_RIGHT
     * @see #POSITION_BOTTOM_RIGHT
     * @see #POSITION_BOTTOM_LEFT
     */
    public @Position int getPosition() {
        return mPosition;
    }

    /**
     * Returns the radius of a quarter circle approximation of this {@link RoundedCornerCompat}.
     *
     * @return the rounded corner radius of this {@link RoundedCornerCompat}. Returns 0 if there is
     *         no rounded corner.
     */
    public int getRadius() {
        return mRadius;
    }

    /**
     * Returns the circle center of a quarter circle approximation of this
     * {@link RoundedCornerCompat}.
     *
     * @return the center point of this {@link RoundedCornerCompat} in the coordinates of the
     *         window.
     */
    public @NonNull Point getCenter() {
        return new Point(mCenter);
    }

    /**
     * Returns the x-coordinate of the circle center of a quarter circle approximation of this
     * {@link RoundedCornerCompat}.
     *
     * @return the x-coordinate of the circle center.
     */
    public int getCenterX() {
        return mCenter.x;
    }

    /**
     * Returns the y-coordinate of the circle center of a quarter circle approximation of this
     * {@link RoundedCornerCompat}.
     *
     * @return the y-coordinate of the circle center.
     */
    public int getCenterY() {
        return mCenter.y;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof RoundedCornerCompat) {
            RoundedCornerCompat r = (RoundedCornerCompat) o;
            return mPosition == r.mPosition && mRadius == r.mRadius
                    && mCenter.equals(r.mCenter);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + mPosition;
        result = 31 * result + mRadius;
        result = 31 * result + mCenter.hashCode();
        return result;
    }

    private String getPositionString(@Position int position) {
        switch (position) {
            case POSITION_TOP_LEFT:
                return "TopLeft";
            case POSITION_TOP_RIGHT:
                return "TopRight";
            case POSITION_BOTTOM_RIGHT:
                return "BottomRight";
            case POSITION_BOTTOM_LEFT:
                return "BottomLeft";
            default:
                return "Invalid";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "RoundedCornerCompat{"
                + "position=" + getPositionString(mPosition)
                + ", radius=" + mRadius
                + ", center=" + mCenter
                + '}';
    }
}
