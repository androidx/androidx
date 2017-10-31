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
package android.support.graphics.drawable;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.lang.Math.abs;
import static java.lang.Math.min;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.support.annotation.RestrictTo;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v4.graphics.PathParser;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.animation.Interpolator;

import org.xmlpull.v1.XmlPullParser;

/**
 * An interpolator that can traverse a Path that extends from <code>Point</code>
 * <code>(0, 0)</code> to <code>(1, 1)</code>. The x coordinate along the <code>Path</code>
 * is the input value and the output is the y coordinate of the line at that point.
 * This means that the Path must conform to a function <code>y = f(x)</code>.
 *
 * <p>The <code>Path</code> must not have gaps in the x direction and must not
 * loop back on itself such that there can be two points sharing the same x coordinate.
 * It is alright to have a disjoint line in the vertical direction:</p>
 * <p><blockquote><pre>
 *     Path path = new Path();
 *     path.lineTo(0.25f, 0.25f);
 *     path.moveTo(0.25f, 0.5f);
 *     path.lineTo(1f, 1f);
 * </pre></blockquote></p>
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class PathInterpolatorCompat implements Interpolator {

    // This governs how accurate the approximation of the Path is.
    private static final float PRECISION = 0.002f;
    public static final int MAX_NUM_POINTS = 3000;
    public static final double EPSILON = 0.00001;

    private float[] mX; // x coordinates in the line

    private float[] mY; // y coordinates in the line

    public PathInterpolatorCompat(Context context, AttributeSet attrs, XmlPullParser parser) {
        this(context.getResources(), context.getTheme(), attrs, parser);
    }

    public PathInterpolatorCompat(Resources res, Resources.Theme theme, AttributeSet attrs,
            XmlPullParser parser) {
        TypedArray a = TypedArrayUtils.obtainAttributes(res, theme,
                attrs, AndroidResources.STYLEABLE_PATH_INTERPOLATOR);
        parseInterpolatorFromTypeArray(a, parser);
        a.recycle();
    }

    private void parseInterpolatorFromTypeArray(TypedArray a, XmlPullParser parser) {
        // If there is pathData defined in the xml file, then the controls points
        // will be all coming from pathData.
        if (TypedArrayUtils.hasAttribute(parser, "pathData")) {
            String pathData = TypedArrayUtils.getNamedString(a, parser, "pathData",
                    AndroidResources.STYLEABLE_PATH_INTERPOLATOR_PATH_DATA);
            Path path = PathParser.createPathFromPathData(pathData);
            if (path == null) {
                throw new InflateException("The path is null, which is created"
                        + " from " + pathData);
            }
            initPath(path);
        } else {
            if (!TypedArrayUtils.hasAttribute(parser, "controlX1")) {
                throw new InflateException("pathInterpolator requires the controlX1 attribute");
            } else if (!TypedArrayUtils.hasAttribute(parser, "controlY1")) {
                throw new InflateException("pathInterpolator requires the controlY1 attribute");
            }
            float x1 = TypedArrayUtils.getNamedFloat(a, parser, "controlX1",
                    AndroidResources.STYLEABLE_PATH_INTERPOLATOR_CONTROL_X_1, 0);
            float y1 = TypedArrayUtils.getNamedFloat(a, parser, "controlY1",
                    AndroidResources.STYLEABLE_PATH_INTERPOLATOR_CONTROL_Y_1, 0);

            boolean hasX2 = TypedArrayUtils.hasAttribute(parser, "controlX2");
            boolean hasY2 = TypedArrayUtils.hasAttribute(parser, "controlY2");

            if (hasX2 != hasY2) {
                throw new InflateException("pathInterpolator requires both controlX2 and"
                        + " controlY2 for cubic Beziers.");
            }

            if (!hasX2) {
                initQuad(x1, y1);
            } else {
                float x2 = TypedArrayUtils.getNamedFloat(a, parser, "controlX2",
                        AndroidResources.STYLEABLE_PATH_INTERPOLATOR_CONTROL_X_2, 0);
                float y2 = TypedArrayUtils.getNamedFloat(a, parser, "controlY2",
                        AndroidResources.STYLEABLE_PATH_INTERPOLATOR_CONTROL_Y_2, 0);
                initCubic(x1, y1, x2, y2);
            }
        }
    }

    private void initQuad(float controlX, float controlY) {
        Path path = new Path();
        path.moveTo(0, 0);
        path.quadTo(controlX, controlY, 1f, 1f);
        initPath(path);
    }

    private void initCubic(float x1, float y1, float x2, float y2) {
        Path path = new Path();
        path.moveTo(0, 0);
        path.cubicTo(x1, y1, x2, y2, 1f, 1f);
        initPath(path);
    }

    private void initPath(Path path) {
        final PathMeasure pathMeasure = new PathMeasure(path, false /* forceClosed */);

        final float pathLength = pathMeasure.getLength();
        final int numPoints = min(MAX_NUM_POINTS, (int) (pathLength / PRECISION) + 1);

        if (numPoints <= 0) {
            throw new IllegalArgumentException("The Path has a invalid length " + pathLength);
        }

        mX = new float[numPoints];
        mY = new float[numPoints];

        final float[] position = new float[2];
        for (int i = 0; i < numPoints; ++i) {
            final float distance = (i * pathLength) / (numPoints - 1);
            pathMeasure.getPosTan(distance, position, null /* tangent */);

            mX[i] = position[0];
            mY[i] = position[1];
        }

        if (abs(mX[0]) > EPSILON || abs(mY[0]) > EPSILON || abs(mX[numPoints - 1] - 1) > EPSILON
                || abs(mY[numPoints - 1] - 1) > EPSILON) {
            throw new IllegalArgumentException("The Path must start at (0,0) and end at (1,1)"
                    + " start: " + mX[0] + "," + mY[0] + " end:" + mX[numPoints - 1] + ","
                    + mY[numPoints - 1]);

        }

        float prevX = 0;
        int componentIndex = 0;
        for (int i = 0; i < numPoints; i++) {
            float x = mX[componentIndex++];
            if (x < prevX) {
                throw new IllegalArgumentException("The Path cannot loop back on itself, x :" + x);
            }
            mX[i] = x;
            prevX = x;
        }

        if (pathMeasure.nextContour()) {
            throw new IllegalArgumentException("The Path should be continuous,"
                    + " can't have 2+ contours");
        }
    }

    /**
     * Using the line in the Path in this interpolator that can be described as
     * <code>y = f(x)</code>, finds the y coordinate of the line given <code>t</code>
     * as the x coordinate. Values less than 0 will always return 0 and values greater
     * than 1 will always return 1.
     *
     * @param t Treated as the x coordinate along the line.
     * @return The y coordinate of the Path along the line where x = <code>t</code>.
     * @see Interpolator#getInterpolation(float)
     */
    @Override
    public float getInterpolation(float t) {
        if (t <= 0) {
            return 0;
        } else if (t >= 1) {
            return 1;
        }
        // Do a binary search for the correct x to interpolate between.
        int startIndex = 0;
        int endIndex = mX.length - 1;

        while (endIndex - startIndex > 1) {
            int midIndex = (startIndex + endIndex) / 2;
            if (t < mX[midIndex]) {
                endIndex = midIndex;
            } else {
                startIndex = midIndex;
            }
        }

        float xRange = mX[endIndex] - mX[startIndex];
        if (xRange == 0) {
            return mY[startIndex];
        }

        float tInRange = t - mX[startIndex];
        float fraction = tInRange / xRange;

        float startY = mY[startIndex];
        float endY = mY[endIndex];
        return startY + (fraction * (endY - startY));
    }
}
