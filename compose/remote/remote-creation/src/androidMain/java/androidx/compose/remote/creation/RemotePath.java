/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.creation;

import static androidx.compose.remote.core.operations.Utils.idFromNan;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.Build;

import androidx.compose.remote.core.operations.PathData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;

public class RemotePath {
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    int mMaxSize = DEFAULT_BUFFER_SIZE;
    float[] mPath = new float[mMaxSize];
    float mCx = 0, mCy = 0;
    int mSize = 0;

    RemotePath(int bufferSize) {
        mPath = new float[mMaxSize = bufferSize];
    }

    public RemotePath() {
        mPath = new float[mMaxSize];
    }

    /** Reset the path */
    public void reset() {
        mSize = 0;
    }

    public float getCurrentX() {
        return mCx;
    }

    public float getCurrentY() {
        return mCy;
    }

    private void resize(int need) {
        if (mSize + need >= mMaxSize) {
            mMaxSize = Math.max(mMaxSize * 2, mSize + need);
            mPath = Arrays.copyOf(mPath, mMaxSize);
        }
    }

    /**
     * reserve space TODO: Do we need this function?
     *
     * @param extraPtCount
     */
    public void incReserve(int extraPtCount) {
        mSize = 0;
    }

    private void add(int type) {
        resize(1);
        mPath[mSize++] = Utils.asNan(type);
    }

    private void addMove(int type, float a1, float a2) {

        resize(3);
        mPath[mSize++] = Utils.asNan(type);
        mPath[mSize++] = a1;
        mPath[mSize++] = a2;
    }

    private void add(int type, float a1, float a2) {

        resize(3);
        mPath[mSize++] = Utils.asNan(type);
        mSize += 2; // THIS IS FLAW in the encoding TODO FIX ON VERSIONING
        mPath[mSize++] = a1;
        mPath[mSize++] = a2;
    }

    private void add(int type, float a1, float a2, float a3, float a4) {

        resize(5);

        mPath[mSize++] = Utils.asNan(type);
        mSize += 2; // THIS IS FLAW in the encoding TODO FIX ON VERSIONING

        mPath[mSize++] = a1;
        mPath[mSize++] = a2;
        mPath[mSize++] = a3;
        mPath[mSize++] = a4;
    }

    private void add(int type, float a1, float a2, float a3, float a4, float a5) {

        resize(6);
        mPath[mSize++] = Utils.asNan(type);
        mSize += 2; // THIS IS FLAW in the encoding TODO FIX ON VERSIONING

        mPath[mSize++] = a1;
        mPath[mSize++] = a2;
        mPath[mSize++] = a3;
        mPath[mSize++] = a4;
        mPath[mSize++] = a5;
    }

    private void add(int type, float a1, float a2, float a3, float a4, float a5, float a6) {

        resize(7);
        mPath[mSize++] = Utils.asNan(type);
        mSize += 2; // THIS IS FLAW in the encoding TODO FIX ON VERSIONING

        mPath[mSize++] = a1;
        mPath[mSize++] = a2;
        mPath[mSize++] = a3;
        mPath[mSize++] = a4;
        mPath[mSize++] = a5;
        mPath[mSize++] = a6;
    }

    /**
     * Set the beginning of the next contour to the point (x,y).
     *
     * @param x The x-coordinate of the start of a new contour
     * @param y The y-coordinate of the start of a new contour
     */
    public void moveTo(float x, float y) {
        addMove(MOVE, x, y);
        mCx = x;
        mCy = y;
    }

    /**
     * Set the beginning of the next contour relative to the last point on the previous contour. If
     * there is no previous contour, this is treated the same as moveTo().
     *
     * @param dx The amount to add to the x-coordinate of the end of the previous contour, to
     *     specify the start of a new contour
     * @param dy The amount to add to the y-coordinate of the end of the previous contour, to
     *     specify the start of a new contour
     */
    public void rMoveTo(float dx, float dy) {
        add(MOVE, mCx = dx + mCx, mCy = dy + mCy);
    }

    /**
     * Add a quadratic bezier from the last point, approaching control point (x1,y1), and ending at
     * (x2,y2). If no moveTo() call has been made for this contour, the first point is automatically
     * set to (0,0).
     *
     * @param x1 The x-coordinate of the control point on a quadratic curve
     * @param y1 The y-coordinate of the control point on a quadratic curve
     * @param x2 The x-coordinate of the end point on a quadratic curve
     * @param y2 The y-coordinate of the end point on a quadratic curve
     */
    public void quadTo(float x1, float y1, float x2, float y2) {
        add(QUADRATIC, x1, y1, x2, y2);
        mCx = x2;
        mCy = y2;
    }

    /**
     * Same as quadTo, but the coordinates are considered relative to the last point on this
     * contour. If there is no previous point, then a moveTo(0,0) is inserted automatically.
     *
     * @param dx1 The amount to add to the x-coordinate of the last point on this contour, for the
     *     control point of a quadratic curve
     * @param dy1 The amount to add to the y-coordinate of the last point on this contour, for the
     *     control point of a quadratic curve
     * @param dx2 The amount to add to the x-coordinate of the last point on this contour, for the
     *     end point of a quadratic curve
     * @param dy2 The amount to add to the y-coordinate of the last point on this contour, for the
     *     end point of a quadratic curve
     */
    public void rQuadTo(float dx1, float dy1, float dx2, float dy2) {
        add(QUADRATIC, dx1 + mCx, dy1 + mCx, dx2 + mCx, dy2 + mCx);
        mCx += dx2;
        mCy += dy2;
    }

    /**
     * Add a quadratic bezier from the last point, approaching control point (x1,y1), and ending at
     * (x2,y2), weighted by <code>weight</code>. If no moveTo() call has been made for this contour,
     * the first point is automatically set to (0,0).
     *
     * <p>A weight of 1 is equivalent to calling {@link #quadTo(float, float, float, float)}. A
     * weight of 0 is equivalent to calling {@link #lineTo(float, float)} to <code>(x1, y1)</code>
     * followed by {@link #lineTo(float, float)} to <code>(x2, y2)</code>.
     *
     * @param x1 The x-coordinate of the control point on a conic curve
     * @param y1 The y-coordinate of the control point on a conic curve
     * @param x2 The x-coordinate of the end point on a conic curve
     * @param y2 The y-coordinate of the end point on a conic curve
     * @param weight The weight of the conic applied to the curve. A value of 1 is equivalent to a
     *     quadratic with the given control and anchor points and a value of 0 is equivalent to a
     *     line to the first and another line to the second point.
     */
    public void conicTo(float x1, float y1, float x2, float y2, float weight) {
        add(CONIC, x1, y1, x2, y2, weight);
        mCx = x2;
        mCy = y2;
    }

    /**
     * Same as conicTo, but the coordinates are considered relative to the last point on this
     * contour. If there is no previous point, then a moveTo(0,0) is inserted automatically.
     *
     * @param dx1 The amount to add to the x-coordinate of the last point on this contour, for the
     *     control point of a conic curve
     * @param dy1 The amount to add to the y-coordinate of the last point on this contour, for the
     *     control point of a conic curve
     * @param dx2 The amount to add to the x-coordinate of the last point on this contour, for the
     *     end point of a conic curve
     * @param dy2 The amount to add to the y-coordinate of the last point on this contour, for the
     *     end point of a conic curve
     * @param weight The weight of the conic applied to the curve. A value of 1 is equivalent to a
     *     quadratic with the given control and anchor points and a value of 0 is equivalent to a
     *     line to the first and another line to the second point.
     */
    public void rConicTo(float dx1, float dy1, float dx2, float dy2, float weight) {
        add(CONIC, dx1 + mCx, dy1 + mCy, dx2 + mCx, dy2 + mCy, weight);
        mCx += dx2;
        mCy += dy2;
    }

    /**
     * Add a line from the last point to the specified point (x,y). If no moveTo() call has been
     * made for this contour, the first point is automatically set to (0,0).
     *
     * @param x The x-coordinate of the end of a line
     * @param y The y-coordinate of the end of a line
     */
    public void lineTo(float x, float y) {
        add(LINE, x, y);
        mCx = x;
        mCy = y;
    }

    /**
     * Same as lineTo, but the coordinates are considered relative to the last point on this
     * contour. If there is no previous point, then a moveTo(0,0) is inserted automatically.
     *
     * @param dx The amount to add to the x-coordinate of the previous point on this contour, to
     *     specify a line
     * @param dy The amount to add to the y-coordinate of the previous point on this contour, to
     *     specify a line
     */
    public void rLineTo(float dx, float dy) {
        add(LINE, mCx = dx + mCx, mCy = dy + mCy);
    }

    /**
     * Add a cubic bezier from the last point, approaching control points (x1,y1) and (x2,y2), and
     * ending at (x3,y3). If no moveTo() call has been made for this contour, the first point is
     * automatically set to (0,0).
     *
     * @param x1 The x-coordinate of the 1st control point on a cubic curve
     * @param y1 The y-coordinate of the 1st control point on a cubic curve
     * @param x2 The x-coordinate of the 2nd control point on a cubic curve
     * @param y2 The y-coordinate of the 2nd control point on a cubic curve
     * @param x3 The x-coordinate of the end point on a cubic curve
     * @param y3 The y-coordinate of the end point on a cubic curve
     */
    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        add(CUBIC, x1, y1, x2, y2, x3, y3);
        mCx = x3;
        mCy = y3;
    }

    /**
     * Same as cubicTo, but the coordinates are considered relative to the current point on this
     * contour. If there is no previous point, then a moveTo(0,0) is inserted automatically.
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x3
     * @param y3
     */
    public void rCubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {

        add(CUBIC, x1 + mCx, y1 + mCy, x2 + mCx, y2 + mCy, x3 + mCx, y3 + mCy);
        mCx += x3;
        mCy += y3;
    }

    /**
     * Close the current contour. If the current point is not equal to the first point of the
     * contour, a line segment is automatically added.
     */
    public void close() {
        add(CLOSE);
    }

    /**
     * Rewinds the path: clears any lines and curves from the path but keeps the internal data
     * structure for faster reuse.
     */
    public void rewind() {
        mSize = 0;
    }

    /**
     * Returns true if the path is empty (contains no lines or curves)
     *
     * @return true if the path is empty (contains no lines or curves)
     */
    public boolean isEmpty() {
        return mSize == 0;
    }

    /**
     * Append the specified arc to the path as a new contour. If the start of the path is different
     * from the path's current last point, then an automatic lineTo() is added to connect the
     * current contour to the start of the arc. However, if the path is empty, then we call moveTo()
     * with the first point of the arc.
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     * @param startAngle
     * @param sweepAngle
     */
    public void addArc(
            float left, float top, float right, float bottom, float startAngle, float sweepAngle) {
        addArc(left, top, right, bottom, startAngle, sweepAngle, false);
    }

    /**
     * Append the specified arc to the path as a new contour. If the start of the path is different
     * from the path's current last point, then an automatic lineTo() is added to connect the
     * current contour to the start of the arc. However, if the path is empty, then we call moveTo()
     * with the first point of the arc.
     *
     * @param oval The bounds of oval defining shape and size of the arc
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise, treated mod 360.
     */
    public void addArc(@NonNull RectF oval, float startAngle, float sweepAngle) {
        addArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle);
    }

    /**
     * Append the specified arc to the path as a new contour. If the start of the path is different
     * from the path's current last point, then an automatic lineTo() is added to connect the
     * current contour to the start of the arc. However, if the path is empty, then we call moveTo()
     * with the first point of the arc.
     *
     * @param oval
     * @param startAngle
     * @param sweepAngle
     */
    public void arcTo(@NonNull RectF oval, float startAngle, float sweepAngle) {
        addArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, false);
    }

    /**
     * Append the specified arc to the path as a new contour. If the start of the path is different
     * from the path's current last point, then an automatic lineTo() is added to connect the
     * current contour to the start of the arc. However, if the path is empty, then we call moveTo()
     * with the first point of the arc.
     *
     * @param oval
     * @param startAngle
     * @param sweepAngle
     * @param forceMoveTo If true, always begin a new contour with the arc
     */
    public void arcTo(
            @NonNull RectF oval, float startAngle, float sweepAngle, boolean forceMoveTo) {
        addArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, forceMoveTo);
    }

    /**
     * Add the specified arc to the path as a new contour.
     *
     * @param left left most bounds of the oval
     * @param top top most bounds of the oval
     * @param right right most bounds of the oval
     * @param bottom lowest bound of the oval
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     * @param forceMoveTo If true, always begin a new contour with the arc
     */
    public void arcTo(
            float left,
            float top,
            float right,
            float bottom,
            float startAngle,
            float sweepAngle,
            boolean forceMoveTo) {
        addArc(left, top, right, bottom, startAngle, sweepAngle, forceMoveTo);
    }

    /**
     * Add the specified arc to the path as a new contour.
     *
     * @param left left most bounds of the oval
     * @param top top most bounds of the oval
     * @param right right most bounds of the oval
     * @param bottom lowest bound of the oval
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     * @param forceMoveTo
     */
    public void addArc(
            float left,
            float top,
            float right,
            float bottom,
            float startAngle,
            float sweepAngle,
            boolean forceMoveTo) {
        // ??? add(ARC, left, top, right, bottom, startAngle, sweepAngle);

    }

    /**
     * get the path version of this remote path
     *
     * @return the RemotePath data as a Android path
     */
    public @NonNull Path getPath() {
        Path path = new Path();
        genPath(path, mPath, mSize, Float.NaN, Float.NaN);
        return path;
    }

    Path mCachePath = new Path();
    PathMeasure mCacheMeasure = new PathMeasure();

    private void genPath(
            Path retPath, float[] floatPath, int length, float startSection, float stopSection) {
        int i = 0;
        mCachePath = (mCachePath == null) ? new Path() : mCachePath;

        while (i < length) {
            switch (idFromNan(floatPath[i])) {
                case PathData.MOVE:
                    i++;
                    mCachePath.moveTo(floatPath[i + 0], floatPath[i + 1]);
                    i += 2;
                    break;
                case PathData.LINE:
                    i += 3;
                    mCachePath.lineTo(floatPath[i + 0], floatPath[i + 1]);
                    i += 2;
                    break;
                case PathData.QUADRATIC:
                    i += 3;
                    mCachePath.quadTo(
                            floatPath[i + 0], floatPath[i + 1], floatPath[i + 2], floatPath[i + 3]);
                    i += 4;
                    break;
                case PathData.CONIC:
                    i += 3;
                    if (Build.VERSION.SDK_INT >= 34) { // REMOVE IN PLATFORM
                        mCachePath.conicTo(
                                floatPath[i + 0],
                                floatPath[i + 1],
                                floatPath[i + 2],
                                floatPath[i + 3],
                                floatPath[i + 4]);
                    }
                    i += 5;
                    break;
                case PathData.CUBIC:
                    i += 3;
                    mCachePath.cubicTo(
                            floatPath[i + 0], floatPath[i + 1],
                            floatPath[i + 2], floatPath[i + 3],
                            floatPath[i + 4], floatPath[i + 5]);
                    i += 6;
                    break;
                case PathData.CLOSE:
                    mCachePath.close();
                    i++;
                    break;
                case PathData.DONE:
                    i++;
                    break;
                default:
                    System.err.println("RemotePath Odd command " + idFromNan(floatPath[i]));
            }
        }

        retPath.reset();
        if (Float.isNaN(startSection) && Float.isNaN(stopSection)) {
            retPath.addPath(mCachePath);
            return;
        }
        float start = Float.isNaN(startSection) ? 0f : startSection;
        float stop = Float.isNaN(stopSection) ? 1f : stopSection;

        if (start > stop) {
            retPath.addPath(mCachePath);
            return;
        }
        mCacheMeasure = (mCacheMeasure == null) ? new PathMeasure() : mCacheMeasure;
        if (stop > 1) {
            float seg = Math.min(stop, 1);
            mCacheMeasure.setPath(mCachePath, false);
            float len = mCacheMeasure.getLength();
            float scaleStart = ((start + 1) % 1) * len;
            float scaleStop = ((seg + 1) % 1) * len; // TODO
            mCacheMeasure.getSegment(scaleStart, scaleStop, retPath, true);
            retPath.addPath(mCachePath);
            return;
        }

        mCacheMeasure.setPath(mCachePath, false);
        float len = mCacheMeasure.getLength();
        float scaleStart = Math.max(start, 0f) * len;
        float scaleStop = Math.min(stop, 1f) * len;
        mCacheMeasure.getSegment(scaleStart, scaleStop, retPath, true);
        retPath.addPath(mCachePath);
    }

    public RemotePath(@NonNull String pathData) {

        float[] cords = new float[6];

        String[] commands = pathData.split("(?=[MmZzLlHhVvCcSsQqTtAa])");
        for (String command : commands) {
            char cmd = command.charAt(0);
            String[] values = command.substring(1).trim().split("[,\\s]+");
            switch (cmd) {
                case 'M':
                    moveTo(Float.parseFloat(values[0]), Float.parseFloat(values[1]));
                    break;
                case 'L':
                    for (int i = 0; i < values.length; i += 2) {
                        lineTo(Float.parseFloat(values[i]), Float.parseFloat(values[i + 1]));
                    }
                    break;
                case 'H':
                    for (String value : values) {
                        lineTo(Float.parseFloat(value), cords[1]);
                    }
                    break;
                case 'C':
                    for (int i = 0; i < values.length; i += 6) {
                        cubicTo(
                                Float.parseFloat(values[i]), Float.parseFloat(values[i + 1]),
                                Float.parseFloat(values[i + 2]), Float.parseFloat(values[i + 3]),
                                Float.parseFloat(values[i + 4]), Float.parseFloat(values[i + 5]));
                    }

                    break;
                case 'S':
                    for (int i = 0; i < values.length; i += 4) {
                        cubicTo(
                                2 * cords[0] - cords[2],
                                2 * cords[1] - cords[3],
                                Float.parseFloat(values[i]),
                                Float.parseFloat(values[i + 1]),
                                Float.parseFloat(values[i + 2]),
                                Float.parseFloat(values[i + 3]));
                    }
                    break;
                case 'Z':
                    close();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported command: " + cmd);
            }
            if (cmd != 'Z' && cmd != 'H') {
                cords[0] = Float.parseFloat(values[values.length - 2]);
                cords[1] = Float.parseFloat(values[values.length - 1]);
                if (cmd == 'C' || cmd == 'S') {
                    cords[2] = Float.parseFloat(values[values.length - 4]);
                    cords[3] = Float.parseFloat(values[values.length - 3]);
                }
            }
        }
    }

    @Override
    public @NonNull String toString() {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        float[] floatPath = mPath;
        while (i < mSize) {
            System.err.println(idFromNan(floatPath[i]));
            switch (idFromNan(floatPath[i])) {
                case PathData.MOVE:
                    i++;
                    builder.append("moveTo(" + floatPath[i + 0] + ", " + floatPath[i + 1] + " )\n");
                    i += 2;
                    break;
                case PathData.LINE:
                    i += 3;
                    builder.append("lineTo(" + floatPath[i + 0] + ", " + floatPath[i + 1] + " )\n");
                    i += 2;
                    break;
                case PathData.QUADRATIC:
                    i += 3;
                    builder.append(
                            "quadTo("
                                    + floatPath[i + 0]
                                    + ", "
                                    + floatPath[i + 1]
                                    + ", "
                                    + floatPath[i + 2]
                                    + ", "
                                    + floatPath[i + 3]
                                    + " )\n");
                    i += 4;
                    break;
                case PathData.CONIC:
                    i += 3;
                    if (Build.VERSION.SDK_INT >= 34) { // REMOVE IN PLATFORM
                        builder.append(
                                "conicTo("
                                        + floatPath[i + 0]
                                        + ", "
                                        + floatPath[i + 1]
                                        + ", "
                                        + floatPath[i + 2]
                                        + ", "
                                        + floatPath[i + 3]
                                        + ", "
                                        + floatPath[i + 4]
                                        + " )\n");
                    }
                    i += 5;
                    break;
                case PathData.CUBIC:
                    i += 3;
                    builder.append(
                            "cubicTo( "
                                    + floatPath[i + 0]
                                    + ", "
                                    + floatPath[i + 1]
                                    + ", "
                                    + floatPath[i + 2]
                                    + ", "
                                    + floatPath[i + 3]
                                    + ", "
                                    + floatPath[i + 4]
                                    + ", "
                                    + floatPath[i + 5]
                                    + " )\n");
                    i += 6;
                    break;
                case PathData.CLOSE:
                    builder.append("close()\n");
                    i++;
                    break;
                case PathData.DONE:
                    builder.append("done()\n");
                    i++;
                    break;
                default:
                    System.err.println(" Odd command " + idFromNan(floatPath[i]));
                    return builder.toString();
            }
        }
        return builder.toString();
    }

    /**
     * creates a float Array of the same size as the mPath
     *
     * @return the array
     */
    public float @NonNull [] createFloatArray() {
        return Arrays.copyOf(mPath, mSize);
    }

    /**
     * Transform the array applying the matrix to each point
     *
     * @param matrix matrix to transform the array by
     */
    public void transform(@NonNull Matrix matrix) {
        float[] floatPath = mPath;
        int i = 0;
        while (i < mSize) {
            switch (idFromNan(floatPath[i])) {
                case PathData.MOVE:
                    i++;
                    matrix.mapPoints(floatPath, i, floatPath, i, 1);
                    i += 2;
                    break;
                case PathData.LINE:
                    i += 3;
                    matrix.mapPoints(floatPath, i, floatPath, i, 1);
                    i += 2;
                    break;
                case PathData.QUADRATIC:
                    i += 3;
                    matrix.mapPoints(floatPath, i, floatPath, i, 2);
                    i += 4;
                    break;
                case PathData.CONIC:
                    i += 3;
                    if (Build.VERSION.SDK_INT >= 34) { // REMOVE IN PLATFORM
                        matrix.mapPoints(floatPath, i, floatPath, i, 2);
                    }
                    i += 5;
                    break;
                case PathData.CUBIC:
                    i += 3;
                    matrix.mapPoints(floatPath, i, floatPath, i, 3);

                    i += 6;
                    break;
                case PathData.CLOSE:
                case PathData.DONE:
                    i++;
                    break;

                default:
                    System.err.println(" Odd command " + idFromNan(floatPath[i]));
            }
        }
    }

    public static final int MOVE = 10;
    public static final int LINE = 11;
    public static final int QUADRATIC = 12;
    public static final int CONIC = 13;
    public static final int CUBIC = 14;
    public static final int CLOSE = 15;
    public static final int DONE = 16;
    public static final float MOVE_NAN = Utils.asNan(MOVE);
    public static final float LINE_NAN = Utils.asNan(LINE);
    public static final float QUADRATIC_NAN = Utils.asNan(QUADRATIC);
    public static final float CONIC_NAN = Utils.asNan(CONIC);
    public static final float CUBIC_NAN = Utils.asNan(CUBIC);
    public static final float CLOSE_NAN = Utils.asNan(CLOSE);
    public static final float DONE_NAN = Utils.asNan(DONE);

    /*
       TODO make decisions on "missing" methods

        public void addArc(float left, float top, float right, float bottom, float startAngle,
         float sweepAngle, boolean forceMoveTo) {

        public void addRoundRect(RectF rect, float[] radii, Path.Direction dir) {
        public void addRect(RectF rect, Path.Direction dir) {
        public void setLastPoint(float dx, float dy) {
        public boolean isInterpolatable(Path otherPath) {
        public void addRoundRect(float left, float top, float right, float bottom,
         float[] radii, Path.Direction dir) {
        public boolean interpolate(Path otherPath, float t, Path interpolatedPath) {
        public void addOval(float left, float top, float right, float bottom, Path.Direction dir) {
        public void toggleInverseFillType() {
        public void addRoundRect(RectF rect, float rx, float ry, Path.Direction dir) {
        public boolean isRect(RectF rect) {
        public void addCircle(float x, float y, float radius, Path.Direction dir) {
        public float[] approximate(float acceptableError) {
        public void transform(Matrix matrix) {
        public boolean op(Path path1, Path path2, Path.Op op) {
        public void addRect(float left, float top, float right, float bottom, Path.Direction dir) {
        public void computeBounds(RectF bounds, boolean exact) {
        public int getGenerationId() {
        public void offset(float dx, float dy, Path dst) {
        public void addOval(RectF oval, Path.Direction dir) {
        public Path.FillType getFillType() {
        public void offset(float dx, float dy) {
        public boolean isInverseFillType() {

        public void addPath(Path src, float dx, float dy) {
        public void set(Path src) {
        public void setFillType(Path.FillType ft) {
        public void addPath(Path src, Matrix matrix) {
        public boolean op(Path path, Path.Op op) {
        public void addPath(Path src) {
        public void addRoundRect(float left, float top, float right, float bottom,
         float rx, float ry, Path.Direction dir) {
    */
    /** This is useful to create an approximate circle using remote float */
    @SuppressWarnings("FloatingPointLiteralPrecision")
    public static @NonNull RemotePath createCirclePath(
            @NonNull RemoteComposeWriter rc, float x, float y, float rad) {
        float k = 0.5522847498f;
        boolean clockwise = true;
        float c = rc.floatExpression(rad, k, AnimatedFloatExpression.MUL);
        RemotePath path = new RemotePath();
        float xc = rc.floatExpression(x, c, AnimatedFloatExpression.ADD);
        float yc = rc.floatExpression(y, c, AnimatedFloatExpression.ADD);
        float xr = rc.floatExpression(x, rad, AnimatedFloatExpression.ADD);
        float yr = rc.floatExpression(y, rad, AnimatedFloatExpression.ADD);

        float x_c = rc.floatExpression(x, c, AnimatedFloatExpression.SUB);
        float y_c = rc.floatExpression(y, c, AnimatedFloatExpression.SUB);
        float x_r = rc.floatExpression(x, rad, AnimatedFloatExpression.SUB);
        float y_r = rc.floatExpression(y, rad, AnimatedFloatExpression.SUB);
        path.moveTo(xr, y);
        if (clockwise) {
            path.cubicTo(xr, yc, xc, yr, x, yr);
            path.cubicTo(x_c, yr, x_r, yc, x_r, y);
            path.cubicTo(x_r, y_c, x_c, y_r, x, y_r);
            path.cubicTo(xc, y_r, xr, y_c, xr, y);
        } else {
            path.moveTo(xr, y);
            path.cubicTo(xr, y_c, xc, y_r, x, y_r);
            path.cubicTo(x_c, y_r, x_r, y_c, x_r, y);
            path.cubicTo(x_r, yc, x_c, yr, x, yr);
            path.cubicTo(xc, yr, xr, yc, xr, y);
        }

        path.close();
        return path;
    }
}
