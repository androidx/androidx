/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.transition;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.core.content.res.TypedArrayUtils;

import org.xmlpull.v1.XmlPullParser;

/**
 * A PathMotion that generates a curved path along an arc on an imaginary circle containing
 * the two points. If the horizontal distance between the points is less than the vertical
 * distance, then the circle's center point will be horizontally aligned with the end point. If the
 * vertical distance is less than the horizontal distance then the circle's center point
 * will be vertically aligned with the end point.
 * <p>
 * When the two points are near horizontal or vertical, the curve of the motion will be
 * small as the center of the circle will be far from both points. To force curvature of
 * the path, {@link #setMinimumHorizontalAngle(float)} and
 * {@link #setMinimumVerticalAngle(float)} may be used to set the minimum angle of the
 * arc between two points.
 * </p>
 * <p>This may be used in XML as an element inside a transition.</p>
 * <pre>{@code
 * <changeBounds>
 *   <arcMotion android:minimumHorizontalAngle="15"
 *              android:minimumVerticalAngle="0"
 *              android:maximumAngle="90"/>
 * </changeBounds>}
 * </pre>
 */
public class ArcMotion extends PathMotion {

    private static final float DEFAULT_MIN_ANGLE_DEGREES = 0;
    private static final float DEFAULT_MAX_ANGLE_DEGREES = 70;
    private static final float DEFAULT_MAX_TANGENT = (float)
            Math.tan(Math.toRadians(DEFAULT_MAX_ANGLE_DEGREES / 2));

    private float mMinimumHorizontalAngle = 0;
    private float mMinimumVerticalAngle = 0;
    private float mMaximumAngle = DEFAULT_MAX_ANGLE_DEGREES;
    private float mMinimumHorizontalTangent = 0;
    private float mMinimumVerticalTangent = 0;
    private float mMaximumTangent = DEFAULT_MAX_TANGENT;

    public ArcMotion() {
    }

    public ArcMotion(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, Styleable.ARC_MOTION);
        XmlPullParser parser = (XmlPullParser) attrs;
        float minimumVerticalAngle = TypedArrayUtils.getNamedFloat(a, parser,
                "minimumVerticalAngle", Styleable.ArcMotion.MINIMUM_VERTICAL_ANGLE,
                DEFAULT_MIN_ANGLE_DEGREES);
        setMinimumVerticalAngle(minimumVerticalAngle);
        float minimumHorizontalAngle = TypedArrayUtils.getNamedFloat(a, parser,
                "minimumHorizontalAngle", Styleable.ArcMotion.MINIMUM_HORIZONTAL_ANGLE,
                DEFAULT_MIN_ANGLE_DEGREES);
        setMinimumHorizontalAngle(minimumHorizontalAngle);
        float maximumAngle = TypedArrayUtils.getNamedFloat(a, parser, "maximumAngle",
                Styleable.ArcMotion.MAXIMUM_ANGLE, DEFAULT_MAX_ANGLE_DEGREES);
        setMaximumAngle(maximumAngle);
        a.recycle();
    }

    /**
     * Sets the minimum arc along the circle between two points aligned near horizontally.
     * When start and end points are close to horizontal, the calculated center point of the
     * circle will be far from both points, giving a near straight path between the points.
     * By setting a minimum angle, this forces the center point to be closer and give an
     * exaggerated curve to the path.
     * <p>The default value is 0.</p>
     *
     * @param angleInDegrees The minimum angle of the arc on a circle describing the Path
     *                       between two nearly horizontally-separated points.
     */
    public void setMinimumHorizontalAngle(float angleInDegrees) {
        mMinimumHorizontalAngle = angleInDegrees;
        mMinimumHorizontalTangent = toTangent(angleInDegrees);
    }

    /**
     * Returns the minimum arc along the circle between two points aligned near horizontally.
     * When start and end points are close to horizontal, the calculated center point of the
     * circle will be far from both points, giving a near straight path between the points.
     * By setting a minimum angle, this forces the center point to be closer and give an
     * exaggerated curve to the path.
     * <p>The default value is 0.</p>
     *
     * @return The minimum arc along the circle between two points aligned near horizontally.
     */
    public float getMinimumHorizontalAngle() {
        return mMinimumHorizontalAngle;
    }

    /**
     * Sets the minimum arc along the circle between two points aligned near vertically.
     * When start and end points are close to vertical, the calculated center point of the
     * circle will be far from both points, giving a near straight path between the points.
     * By setting a minimum angle, this forces the center point to be closer and give an
     * exaggerated curve to the path.
     * <p>The default value is 0.</p>
     *
     * @param angleInDegrees The minimum angle of the arc on a circle describing the Path
     *                       between two nearly vertically-separated points.
     */
    public void setMinimumVerticalAngle(float angleInDegrees) {
        mMinimumVerticalAngle = angleInDegrees;
        mMinimumVerticalTangent = toTangent(angleInDegrees);
    }

    /**
     * Returns the minimum arc along the circle between two points aligned near vertically.
     * When start and end points are close to vertical, the calculated center point of the
     * circle will be far from both points, giving a near straight path between the points.
     * By setting a minimum angle, this forces the center point to be closer and give an
     * exaggerated curve to the path.
     * <p>The default value is 0.</p>
     *
     * @return The minimum angle of the arc on a circle describing the Path
     * between two nearly vertically-separated points.
     */
    public float getMinimumVerticalAngle() {
        return mMinimumVerticalAngle;
    }

    /**
     * Sets the maximum arc along the circle between two points. When start and end points
     * have close to equal x and y differences, the curve between them is large. This forces
     * the curved path to have an arc of at most the given angle.
     * <p>The default value is 70 degrees.</p>
     *
     * @param angleInDegrees The maximum angle of the arc on a circle describing the Path
     *                       between the start and end points.
     */
    public void setMaximumAngle(float angleInDegrees) {
        mMaximumAngle = angleInDegrees;
        mMaximumTangent = toTangent(angleInDegrees);
    }

    /**
     * Returns the maximum arc along the circle between two points. When start and end points
     * have close to equal x and y differences, the curve between them is large. This forces
     * the curved path to have an arc of at most the given angle.
     * <p>The default value is 70 degrees.</p>
     *
     * @return The maximum angle of the arc on a circle describing the Path
     * between the start and end points.
     */
    public float getMaximumAngle() {
        return mMaximumAngle;
    }

    private static float toTangent(float arcInDegrees) {
        if (arcInDegrees < 0 || arcInDegrees > 90) {
            throw new IllegalArgumentException("Arc must be between 0 and 90 degrees");
        }
        return (float) Math.tan(Math.toRadians(arcInDegrees / 2));
    }

    @NonNull
    @Override
    public Path getPath(float startX, float startY, float endX, float endY) {
        // Here's a little ascii art to show how this is calculated:
        // c---------- b
        //  \        / |
        //    \     d  |
        //      \  /   e
        //        a----f
        // This diagram assumes that the horizontal distance is less than the vertical
        // distance between The start point (a) and end point (b).
        // d is the midpoint between a and b. c is the center point of the circle with
        // This path is formed by assuming that start and end points are in
        // an arc on a circle. The end point is centered in the circle vertically
        // and start is a point on the circle.

        // Triangles bfa and bde form similar right triangles. The control points
        // for the cubic Bezier arc path are the midpoints between a and e and e and b.

        Path path = new Path();
        path.moveTo(startX, startY);

        float ex;
        float ey;
        float deltaX = endX - startX;
        float deltaY = endY - startY;

        // hypotenuse squared.
        float h2 = deltaX * deltaX + deltaY * deltaY;

        // Midpoint between start and end
        float dx = (startX + endX) / 2;
        float dy = (startY + endY) / 2;

        // Distance squared between end point and mid point is (1/2 hypotenuse)^2
        float midDist2 = h2 * 0.25f;

        float minimumArcDist2;

        boolean isMovingUpwards = startY > endY;

        if ((Math.abs(deltaX) < Math.abs(deltaY))) {
            // Similar triangles bfa and bde mean that (ab/fb = eb/bd)
            // Therefore, eb = ab * bd / fb
            // ab = hypotenuse
            // bd = hypotenuse/2
            // fb = deltaY
            float eDistY = Math.abs(h2 / (2 * deltaY));
            if (isMovingUpwards) {
                ey = endY + eDistY;
                ex = endX;
            } else {
                ey = startY + eDistY;
                ex = startX;
            }

            minimumArcDist2 = midDist2 * mMinimumVerticalTangent
                    * mMinimumVerticalTangent;
        } else {
            // Same as above, but flip X & Y and account for negative eDist
            float eDistX = h2 / (2 * deltaX);
            if (isMovingUpwards) {
                ex = startX + eDistX;
                ey = startY;
            } else {
                ex = endX - eDistX;
                ey = endY;
            }

            minimumArcDist2 = midDist2 * mMinimumHorizontalTangent
                    * mMinimumHorizontalTangent;
        }
        float arcDistX = dx - ex;
        float arcDistY = dy - ey;
        float arcDist2 = arcDistX * arcDistX + arcDistY * arcDistY;

        float maximumArcDist2 = midDist2 * mMaximumTangent * mMaximumTangent;

        float newArcDistance2 = 0;
        if (arcDist2 < minimumArcDist2) {
            newArcDistance2 = minimumArcDist2;
        } else if (arcDist2 > maximumArcDist2) {
            newArcDistance2 = maximumArcDist2;
        }
        if (newArcDistance2 != 0) {
            float ratio2 = newArcDistance2 / arcDist2;
            float ratio = (float) Math.sqrt(ratio2);
            ex = dx + (ratio * (ex - dx));
            ey = dy + (ratio * (ey - dy));
        }
        float control1X = (startX + ex) / 2;
        float control1Y = (startY + ey) / 2;
        float control2X = (ex + endX) / 2;
        float control2Y = (ey + endY) / 2;
        path.cubicTo(control1X, control1Y, control2X, control2Y, endX, endY);
        return path;
    }

}
