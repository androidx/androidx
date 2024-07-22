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

import androidx.annotation.RestrictTo;

/**
 * Utils for calculating scale and zoom operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ZoomUtils {
    private ZoomUtils() {
    }

    /** Returns the zoom that would fit the inner rect into the outer rect. */
    public static float calculateZoomToFit(
            float outerWidth, float outerHeight, float innerWidth, float innerHeight) {
        if (innerWidth == 0 || innerHeight == 0) {
            return 1;
        }
        if (RectUtils.widthIsLimitingDimension(outerWidth, outerHeight, innerWidth, innerHeight)) {
            return outerWidth / innerWidth;
        } else {
            return outerHeight / innerHeight;
        }
    }

    /**
     * Used to convert the zoom view coordinates to the content coordinates using the current
     * zoom and scroll values.
     *
     * @param zoomViewCoordinate coordinate for either the x or y-axis in the [ZoomView] viewport.
     * @param zoom               current zoom factor.
     * @param scroll             current scroll position.
     * @return content coordinates converted from the viewport coordinates.
     */
    public static float toContentCoordinate(float zoomViewCoordinate, float zoom, int scroll) {
        Preconditions.checkArgument(zoom > 0, "Zoom factor must be positive.");
        return (zoomViewCoordinate + scroll) / zoom;
    }

    /**
     * Used to convert the content coordinates to the view coordinates using the current
     * zoom and scroll values.
     *
     * @param contentCoordinate coordinate for either the x or y-axis in the [ZoomView] viewport.
     * @param zoom              current zoom factor.
     * @param scroll            current scroll position.
     * @return view coordinates converted from the content coordinates.
     */
    public static float toZoomViewCoordinate(float contentCoordinate, float zoom, int scroll) {
        Preconditions.checkArgument(zoom > 0, "Zoom factor must be positive.");
        return (contentCoordinate * zoom) - scroll;
    }

    /**
     * Used to find the delta between the view port pivot and the pivot after the zoom in/out is
     * done. Delta is positive in case of zooming in, negative in case it has been zoomed out and
     * 0 if no change.
     *
     * @param oldZoom   previous zoom factor.
     * @param newZoom   current zoom factor.
     * @param zoomPivot pivot point from zoom.
     * @param scroll    scroll position.
     * @return delta between the view port and zoomed in/out pivot.
     */
    public static int scrollDeltaNeededForZoomChange(
            float oldZoom, float newZoom, float zoomPivot, int scroll) {
        // Find where the given pivot point would move to when we change the zoom, and return the
        // delta.
        float contentPivot = ZoomUtils.toContentCoordinate(zoomPivot, oldZoom, scroll);
        float movedZoomViewPivot = ZoomUtils.toZoomViewCoordinate(contentPivot, newZoom,
                scroll);
        return (int) (movedZoomViewPivot - zoomPivot);
    }

    /**
     * Used to constrain the coordinate using the current zoom factor and the scroll position
     * based on the content raw size and the view port size. In case of adjusting the x
     * coordinate, the content and view port dimension will be the width while in case of
     * y-coordinate it will be the width. Lower and upper bound is the left and right of the
     * content for x-axis and is top and bottom for y-axis.
     *
     * @param zoom                current zoom factor.
     * @param scroll              current scroll position.
     * @param contentRawDimension raw dimension (height/width) for the content
     * @param viewportDimension   viewport dimension
     * @return scaled coordinate or 0 if no adjustment is needed.
     */
    public static int constrainCoordinate(float zoom, int scroll, int contentRawDimension,
            int viewportDimension) {
        float lowerBound = ZoomUtils.toZoomViewCoordinate(0, zoom, scroll);
        float upperBound = ZoomUtils.toZoomViewCoordinate(contentRawDimension, zoom, scroll);

        if (lowerBound <= 0 && upperBound >= viewportDimension) {
            // Content too large for viewport and no dead margins: no adjustment needed.
            return 0;
        }

        float scaledContentSize = upperBound - lowerBound;
        if (scaledContentSize <= viewportDimension) {
            // Content fits in viewport: keep in the center.
            return (int) ((upperBound + lowerBound - viewportDimension) / 2);
        } else {
            // Content doesn't fit in viewport: eliminate dead margins.
            if (lowerBound > 0) { // Dead margin on the left.
                return (int) lowerBound;
            } else if (upperBound < viewportDimension) { // Dead margin on the right.
                return (int) (upperBound - viewportDimension);
            }
        }
        return 0;
    }
}
