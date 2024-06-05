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

package androidx.pdf.util;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility to configure system gesture exclusion rects for disabling the back gesture on the left
 * and right sides of the screen when using gesture navigation.
 *
 * <p>Gesture navigation was introduced in Q.
 *
 * <p>As of Q release, exclusion rects are limited to 200dp total per side of the screen. When this
 * limit is exceeded the exclusion rects will be honored starting from the bottom of the screen
 * until the total dp for that side reaches the limit. Additional rects past this limit will be
 * ignored.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SystemGestureExclusionHelper {

    private SystemGestureExclusionHelper() {
    }

    /**
     * Creates {@link Rect}s to cover the left and right system gesture areas for {@code
     * bufferDistancePx} above and {@code bufferDistancePx} below any corner of {@code rect} that
     * lies in within {@code bufferDistancePx} of the system gesture area or within the gesture area
     * itself.
     *
     * <p>{@code rect} is in unscaled screen coordinates.
     */
    @NonNull
    public static List<Rect> createExclusionRectsForCorners(
            @NonNull Rect rect, int systemGestureInsetsWidthPx, int bufferDistancePx,
            int screenWidthPx) {
        List<Rect> exclusionRects = new ArrayList<>();

        if (needsLeftSideExclusionRect(rect.left, systemGestureInsetsWidthPx, bufferDistancePx)) {
            exclusionRects.add(
                    createLeftSideExclusionRect(rect.top, systemGestureInsetsWidthPx,
                            bufferDistancePx));
            exclusionRects.add(
                    createLeftSideExclusionRect(rect.bottom, systemGestureInsetsWidthPx,
                            bufferDistancePx));
        }
        if (needsRightSideExclusionRect(
                rect.right, systemGestureInsetsWidthPx, bufferDistancePx, screenWidthPx)) {
            exclusionRects.add(
                    createRightSideExclusionRect(
                            rect.top, systemGestureInsetsWidthPx, bufferDistancePx, screenWidthPx));
            exclusionRects.add(
                    createRightSideExclusionRect(
                            rect.bottom, systemGestureInsetsWidthPx, bufferDistancePx,
                            screenWidthPx));
        }
        return exclusionRects;
    }

    /**
     * Creates a {@link Rect} to cover the left system gesture area from {@code reservedDistancePx}
     * above {@code yCoordinatePx} to {@code reservedDistancePx} below {@code yCoordinatePx}.
     *
     * <p>{@code yCoordinatePx} is in unscaled screen coordinates.
     *
     * @throws IllegalArgumentException if {@code reservedDistancePx} <= 0.
     */
    @NonNull
    public static Rect createLeftSideExclusionRect(
            int yCoordinatePx, int systemGestureInsetsWidthPx, int reservedDistancePx) {
        Preconditions.checkArgument(
                reservedDistancePx > 0,
                String.format("Invalid reservedDistancePx: %d.", reservedDistancePx));
        return new Rect(
                0,
                yCoordinatePx - reservedDistancePx,
                systemGestureInsetsWidthPx,
                yCoordinatePx + reservedDistancePx);
    }

    /**
     * Creates a {@link Rect} to cover the right system gesture area from {@code reservedDistancePx}
     * above {@code yCoordinatePx} to {@code reservedDistancePx} below {@code yCoordinatePx}.
     *
     * <p>{@code yCoordinatePx} is in unscaled screen coordinates.
     *
     * @throws IllegalArgumentException if {@code reservedDistancePx} <= 0.
     */
    @NonNull
    public static Rect createRightSideExclusionRect(
            int yCoordinatePx,
            int systemGestureInsetsWidthPx,
            int reservedDistancePx,
            int screenWidthPx) {
        Preconditions.checkArgument(
                reservedDistancePx > 0,
                String.format("Invalid reservedDistancePx: %d.", reservedDistancePx));
        return new Rect(
                screenWidthPx - systemGestureInsetsWidthPx,
                yCoordinatePx - reservedDistancePx,
                screenWidthPx,
                yCoordinatePx + reservedDistancePx);
    }

    /**
     * True if a) there is a non-zero system gesture area on the left side of the screen
     * <i>and</i> b) the provided {@code xCoordinatePx} falls within this area <i>or</i> {@code
     * bufferDistancePx} to the left of {@code xCoordinatePx} falls within this area.
     *
     * <p>To check only if the {@code xCoordinatePx} falls within the left system gesture area, call
     * with {@code bufferDistancePx} == 0.
     *
     * <p>{@code xCoordinatePx} is in unscaled screen coordinates.
     */
    public static boolean needsLeftSideExclusionRect(
            int xCoordinatePx, int systemGestureInsetsWidthPx, int bufferDistancePx) {
        Preconditions.checkArgument(xCoordinatePx >= 0, "Negative xCoordinatePx.");
        return systemGestureInsetsWidthPx > 0
                && xCoordinatePx < (systemGestureInsetsWidthPx + bufferDistancePx);
    }

    /** Same as {@link #needsLeftSideExclusionRect}, but for the right side. */
    public static boolean needsRightSideExclusionRect(
            int xCoordinatePx, int systemGestureInsetsWidthPx, int bufferDistancePx,
            int screenWidthPx) {
        Preconditions.checkArgument(xCoordinatePx <= screenWidthPx,
                "xCoordinatePx > screenWidthPx.");
        return systemGestureInsetsWidthPx > 0
                && xCoordinatePx > (screenWidthPx - systemGestureInsetsWidthPx - bufferDistancePx);
    }

    /**
     * Sets the {@code exclusionRects} on the {@code view}.
     *
     * @return true if the {@code exclusionRects} were set, false otherwise.
     */
    public static boolean setSystemGestureExclusionRects(@NonNull View view,
            @NonNull List<Rect> exclusionRects) {
        if (view == null || exclusionRects == null) {
            return false;
        }
        view.setSystemGestureExclusionRects(exclusionRects);
        return true;
    }
}
