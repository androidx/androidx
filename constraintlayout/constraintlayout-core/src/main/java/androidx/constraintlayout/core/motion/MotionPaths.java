/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.constraintlayout.core.motion;

import static androidx.constraintlayout.core.motion.MotionWidget.UNSET;

import androidx.constraintlayout.core.motion.key.MotionKeyPosition;
import androidx.constraintlayout.core.motion.utils.Easing;
import androidx.constraintlayout.core.motion.utils.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

/**
 * This is used to capture and play back path of the layout.
 * It is used to set the bounds of the view (view.layout(l, t, r, b))
 */
public class MotionPaths implements Comparable<MotionPaths> {
    public static final String TAG = "MotionPaths";
    public static final boolean DEBUG = false;
    public static final boolean OLD_WAY = false; // the computes the positions the old way
    static final int OFF_POSITION = 0;
    static final int OFF_X = 1;
    static final int OFF_Y = 2;
    static final int OFF_WIDTH = 3;
    static final int OFF_HEIGHT = 4;
    static final int OFF_PATH_ROTATE = 5;

    // mode and type have same numbering scheme
    public static final int PERPENDICULAR = MotionKeyPosition.TYPE_PATH;
    public static final int CARTESIAN = MotionKeyPosition.TYPE_CARTESIAN;
    public static final int SCREEN = MotionKeyPosition.TYPE_SCREEN;
    static String[] sNames = {"position", "x", "y", "width", "height", "pathRotate"};
    public String mId;
    Easing mKeyFrameEasing;
    int mDrawPath = 0;
    float mTime;
    float mPosition;
    float mX;
    float mY;
    float mWidth;
    float mHeight;
    float mPathRotate = Float.NaN;
    float mProgress = Float.NaN;
    int mPathMotionArc = UNSET;
    String mAnimateRelativeTo = null;
    float mRelativeAngle = Float.NaN;
    Motion mRelativeToController = null;

    HashMap<String, CustomVariable> mCustomAttributes = new HashMap<>();
    int mMode = 0; // how was this point computed 1=perpendicular 2=deltaRelative
    int mAnimateCircleAngleTo; // since angles loop there are 4 ways we can pic direction

    public MotionPaths() {
    }

    /**
     * set up with Cartesian
     */
    void initCartesian(MotionKeyPosition c, MotionPaths startTimePoint, MotionPaths endTimePoint) {
        float position = c.mFramePosition / 100f;
        MotionPaths point = this;
        point.mTime = position;

        mDrawPath = c.mDrawPath;
        float scaleWidth = Float.isNaN(c.mPercentWidth) ? position : c.mPercentWidth;
        float scaleHeight = Float.isNaN(c.mPercentHeight) ? position : c.mPercentHeight;
        float scaleX = endTimePoint.mWidth - startTimePoint.mWidth;
        float scaleY = endTimePoint.mHeight - startTimePoint.mHeight;

        point.mPosition = point.mTime;

        float path = position; // the position on the path

        float startCenterX = startTimePoint.mX + startTimePoint.mWidth / 2;
        float startCenterY = startTimePoint.mY + startTimePoint.mHeight / 2;
        float endCenterX = endTimePoint.mX + endTimePoint.mWidth / 2;
        float endCenterY = endTimePoint.mY + endTimePoint.mHeight / 2;
        float pathVectorX = endCenterX - startCenterX;
        float pathVectorY = endCenterY - startCenterY;
        point.mX = (int) (startTimePoint.mX + pathVectorX * path - scaleX * scaleWidth / 2);
        point.mY = (int) (startTimePoint.mY + pathVectorY * path - scaleY * scaleHeight / 2);
        point.mWidth = (int) (startTimePoint.mWidth + scaleX * scaleWidth);
        point.mHeight = (int) (startTimePoint.mHeight + scaleY * scaleHeight);

        float dxdx = Float.isNaN(c.mPercentX) ? position : c.mPercentX;
        float dydx = Float.isNaN(c.mAltPercentY) ? 0 : c.mAltPercentY;
        float dydy = Float.isNaN(c.mPercentY) ? position : c.mPercentY;
        float dxdy = Float.isNaN(c.mAltPercentX) ? 0 : c.mAltPercentX;
        point.mMode = MotionPaths.CARTESIAN;
        point.mX = (int) (startTimePoint.mX + pathVectorX * dxdx
                + pathVectorY * dxdy - scaleX * scaleWidth / 2);
        point.mY = (int) (startTimePoint.mY + pathVectorX * dydx
                + pathVectorY * dydy - scaleY * scaleHeight / 2);

        point.mKeyFrameEasing = Easing.getInterpolator(c.mTransitionEasing);
        point.mPathMotionArc = c.mPathMotionArc;
    }

    /**
     * takes the new keyPosition
     */
    public MotionPaths(int parentWidth,
            int parentHeight,
            MotionKeyPosition c,
            MotionPaths startTimePoint,
            MotionPaths endTimePoint) {
        if (startTimePoint.mAnimateRelativeTo != null) {
            initPolar(parentWidth, parentHeight, c, startTimePoint, endTimePoint);
            return;
        }
        switch (c.mPositionType) {
            case MotionKeyPosition.TYPE_SCREEN:
                initScreen(parentWidth, parentHeight, c, startTimePoint, endTimePoint);
                return;
            case MotionKeyPosition.TYPE_PATH:
                initPath(c, startTimePoint, endTimePoint);
                return;
            default:
            case MotionKeyPosition.TYPE_CARTESIAN:
                initCartesian(c, startTimePoint, endTimePoint);
                return;
        }
    }

    void initPolar(int parentWidth,
            int parentHeight,
            MotionKeyPosition c,
            MotionPaths s,
            MotionPaths e) {
        float position = c.mFramePosition / 100f;
        this.mTime = position;
        mDrawPath = c.mDrawPath;
        this.mMode = c.mPositionType; // mode and type have same numbering scheme
        float scaleWidth = Float.isNaN(c.mPercentWidth) ? position : c.mPercentWidth;
        float scaleHeight = Float.isNaN(c.mPercentHeight) ? position : c.mPercentHeight;
        float scaleX = e.mWidth - s.mWidth;
        float scaleY = e.mHeight - s.mHeight;
        this.mPosition = this.mTime;
        mWidth = (int) (s.mWidth + scaleX * scaleWidth);
        mHeight = (int) (s.mHeight + scaleY * scaleHeight);
        @SuppressWarnings("unused") float startfactor = 1 - position;
        @SuppressWarnings("unused") float endfactor = position;

        switch (c.mPositionType) {
            case MotionKeyPosition.TYPE_SCREEN:
                this.mX = Float.isNaN(c.mPercentX) ? (position * (e.mX - s.mX) + s.mX)
                        : c.mPercentX * Math.min(scaleHeight, scaleWidth);
                this.mY = Float.isNaN(c.mPercentY)
                        ? (position * (e.mY - s.mY) + s.mY) : c.mPercentY;
                break;

            case MotionKeyPosition.TYPE_PATH:
                this.mX = (Float.isNaN(c.mPercentX)
                        ? position : c.mPercentX) * (e.mX - s.mX) + s.mX;
                this.mY = (Float.isNaN(c.mPercentY)
                        ? position : c.mPercentY) * (e.mY - s.mY) + s.mY;
                break;
            default:
            case MotionKeyPosition.TYPE_CARTESIAN:
                this.mX = (Float.isNaN(c.mPercentX)
                        ? position : c.mPercentX) * (e.mX - s.mX) + s.mX;
                this.mY = (Float.isNaN(c.mPercentY)
                        ? position : c.mPercentY) * (e.mY - s.mY) + s.mY;
                break;
        }

        this.mAnimateRelativeTo = s.mAnimateRelativeTo;
        this.mKeyFrameEasing = Easing.getInterpolator(c.mTransitionEasing);
        this.mPathMotionArc = c.mPathMotionArc;
    }

    // @TODO: add description
    public void setupRelative(Motion mc, MotionPaths relative) {
        double dx = mX + mWidth / 2 - relative.mX - relative.mWidth / 2;
        double dy = mY + mHeight / 2 - relative.mY - relative.mHeight / 2;
        mRelativeToController = mc;

        mX = (float) Math.hypot(dy, dx);
        if (Float.isNaN(mRelativeAngle)) {
            mY = (float) (Math.atan2(dy, dx) + Math.PI / 2);
        } else {
            mY = (float) Math.toRadians(mRelativeAngle);
        }
    }

    void initScreen(int parentWidth,
            int parentHeight,
            MotionKeyPosition c,
            MotionPaths startTimePoint,
            MotionPaths endTimePoint) {
        float position = c.mFramePosition / 100f;
        MotionPaths point = this;
        point.mTime = position;

        mDrawPath = c.mDrawPath;
        float scaleWidth = Float.isNaN(c.mPercentWidth) ? position : c.mPercentWidth;
        float scaleHeight = Float.isNaN(c.mPercentHeight) ? position : c.mPercentHeight;

        float scaleX = endTimePoint.mWidth - startTimePoint.mWidth;
        float scaleY = endTimePoint.mHeight - startTimePoint.mHeight;

        point.mPosition = point.mTime;

        float path = position; // the position on the path

        float startCenterX = startTimePoint.mX + startTimePoint.mWidth / 2;
        float startCenterY = startTimePoint.mY + startTimePoint.mHeight / 2;
        float endCenterX = endTimePoint.mX + endTimePoint.mWidth / 2;
        float endCenterY = endTimePoint.mY + endTimePoint.mHeight / 2;
        float pathVectorX = endCenterX - startCenterX;
        float pathVectorY = endCenterY - startCenterY;
        point.mX = (int) (startTimePoint.mX + pathVectorX * path - scaleX * scaleWidth / 2);
        point.mY = (int) (startTimePoint.mY + pathVectorY * path - scaleY * scaleHeight / 2);
        point.mWidth = (int) (startTimePoint.mWidth + scaleX * scaleWidth);
        point.mHeight = (int) (startTimePoint.mHeight + scaleY * scaleHeight);

        point.mMode = MotionPaths.SCREEN;
        if (!Float.isNaN(c.mPercentX)) {
            parentWidth -= (int) point.mWidth;
            point.mX = (int) (c.mPercentX * parentWidth);
        }
        if (!Float.isNaN(c.mPercentY)) {
            parentHeight -= (int) point.mHeight;
            point.mY = (int) (c.mPercentY * parentHeight);
        }

        point.mAnimateRelativeTo = mAnimateRelativeTo;
        point.mKeyFrameEasing = Easing.getInterpolator(c.mTransitionEasing);
        point.mPathMotionArc = c.mPathMotionArc;
    }

    void initPath(MotionKeyPosition c, MotionPaths startTimePoint, MotionPaths endTimePoint) {

        float position = c.mFramePosition / 100f;
        MotionPaths point = this;
        point.mTime = position;

        mDrawPath = c.mDrawPath;
        float scaleWidth = Float.isNaN(c.mPercentWidth) ? position : c.mPercentWidth;
        float scaleHeight = Float.isNaN(c.mPercentHeight) ? position : c.mPercentHeight;

        float scaleX = endTimePoint.mWidth - startTimePoint.mWidth;
        float scaleY = endTimePoint.mHeight - startTimePoint.mHeight;

        point.mPosition = point.mTime;

        float path = Float.isNaN(c.mPercentX) ? position : c.mPercentX; // the position on the path

        float startCenterX = startTimePoint.mX + startTimePoint.mWidth / 2;
        float startCenterY = startTimePoint.mY + startTimePoint.mHeight / 2;
        float endCenterX = endTimePoint.mX + endTimePoint.mWidth / 2;
        float endCenterY = endTimePoint.mY + endTimePoint.mHeight / 2;
        float pathVectorX = endCenterX - startCenterX;
        float pathVectorY = endCenterY - startCenterY;
        point.mX = (int) (startTimePoint.mX + pathVectorX * path - scaleX * scaleWidth / 2);
        point.mY = (int) (startTimePoint.mY + pathVectorY * path - scaleY * scaleHeight / 2);
        point.mWidth = (int) (startTimePoint.mWidth + scaleX * scaleWidth);
        point.mHeight = (int) (startTimePoint.mHeight + scaleY * scaleHeight);
        float perpendicular = Float.isNaN(c.mPercentY)
                ? 0 : c.mPercentY; // the position on the path
        float perpendicularX = -pathVectorY;
        float perpendicularY = pathVectorX;

        float normalX = perpendicularX * perpendicular;
        float normalY = perpendicularY * perpendicular;
        point.mMode = MotionPaths.PERPENDICULAR;
        point.mX = (int) (startTimePoint.mX + pathVectorX * path - scaleX * scaleWidth / 2);
        point.mY = (int) (startTimePoint.mY + pathVectorY * path - scaleY * scaleHeight / 2);
        point.mX += normalX;
        point.mY += normalY;

        point.mAnimateRelativeTo = mAnimateRelativeTo;
        point.mKeyFrameEasing = Easing.getInterpolator(c.mTransitionEasing);
        point.mPathMotionArc = c.mPathMotionArc;
    }

    private static float xRotate(float sin, float cos, float cx, float cy, float x, float y) {
        x = x - cx;
        y = y - cy;
        return x * cos - y * sin + cx;
    }

    private static float yRotate(float sin, float cos, float cx, float cy, float x, float y) {
        x = x - cx;
        y = y - cy;
        return x * sin + y * cos + cy;
    }

    private boolean diff(float a, float b) {
        if (Float.isNaN(a) || Float.isNaN(b)) {
            return Float.isNaN(a) != Float.isNaN(b);
        }
        return Math.abs(a - b) > 0.000001f;
    }

    void different(MotionPaths points, boolean[] mask, String[] custom, boolean arcMode) {
        int c = 0;
        boolean diffx = diff(mX, points.mX);
        boolean diffy = diff(mY, points.mY);
        mask[c++] |= diff(mPosition, points.mPosition);
        mask[c++] |= diffx || diffy || arcMode;
        mask[c++] |= diffx || diffy || arcMode;
        mask[c++] |= diff(mWidth, points.mWidth);
        mask[c++] |= diff(mHeight, points.mHeight);

    }

    void getCenter(double p, int[] toUse, double[] data, float[] point, int offset) {
        float v_x = mX;
        float v_y = mY;
        float v_width = mWidth;
        float v_height = mHeight;
        float translationX = 0, translationY = 0;
        for (int i = 0; i < toUse.length; i++) {
            float value = (float) data[i];

            switch (toUse[i]) {
                case OFF_X:
                    v_x = value;
                    break;
                case OFF_Y:
                    v_y = value;
                    break;
                case OFF_WIDTH:
                    v_width = value;
                    break;
                case OFF_HEIGHT:
                    v_height = value;
                    break;
            }
        }
        if (mRelativeToController != null) {
            float[] pos = new float[2];
            float[] vel = new float[2];

            mRelativeToController.getCenter(p, pos, vel);
            float rx = pos[0];
            float ry = pos[1];
            float radius = v_x;
            float angle = v_y;
            // TODO Debug angle
            v_x = (float) (rx + radius * Math.sin(angle) - v_width / 2);
            v_y = (float) (ry - radius * Math.cos(angle) - v_height / 2);
        }

        point[offset] = v_x + v_width / 2 + translationX;
        point[offset + 1] = v_y + v_height / 2 + translationY;
    }

    void getCenter(double p,
            int[] toUse,
            double[] data,
            float[] point,
            double[] vdata,
            float[] velocity) {
        float v_x = mX;
        float v_y = mY;
        float v_width = mWidth;
        float v_height = mHeight;
        float dv_x = 0;
        float dv_y = 0;
        float dv_width = 0;
        float dv_height = 0;

        float translationX = 0, translationY = 0;
        for (int i = 0; i < toUse.length; i++) {
            float value = (float) data[i];
            float dvalue = (float) vdata[i];

            switch (toUse[i]) {
                case OFF_X:
                    v_x = value;
                    dv_x = dvalue;
                    break;
                case OFF_Y:
                    v_y = value;
                    dv_y = dvalue;
                    break;
                case OFF_WIDTH:
                    v_width = value;
                    dv_width = dvalue;
                    break;
                case OFF_HEIGHT:
                    v_height = value;
                    dv_height = dvalue;
                    break;
            }
        }
        float dpos_x = dv_x + dv_width / 2;
        float dpos_y = dv_y + dv_height / 2;

        if (mRelativeToController != null) {
            float[] pos = new float[2];
            float[] vel = new float[2];
            mRelativeToController.getCenter(p, pos, vel);
            float rx = pos[0];
            float ry = pos[1];
            float radius = v_x;
            float angle = v_y;
            float dradius = dv_x;
            float dangle = dv_y;
            float drx = vel[0];
            float dry = vel[1];
            // TODO Debug angle
            v_x = (float) (rx + radius * Math.sin(angle) - v_width / 2);
            v_y = (float) (ry - radius * Math.cos(angle) - v_height / 2);
            dpos_x = (float) (drx + dradius * Math.sin(angle) + Math.cos(angle) * dangle);
            dpos_y = (float) (dry - dradius * Math.cos(angle) + Math.sin(angle) * dangle);
        }

        point[0] = v_x + v_width / 2 + translationX;
        point[1] = v_y + v_height / 2 + translationY;
        velocity[0] = dpos_x;
        velocity[1] = dpos_y;
    }

    void getCenterVelocity(double p, int[] toUse, double[] data, float[] point, int offset) {
        float v_x = mX;
        float v_y = mY;
        float v_width = mWidth;
        float v_height = mHeight;
        float translationX = 0, translationY = 0;
        for (int i = 0; i < toUse.length; i++) {
            float value = (float) data[i];

            switch (toUse[i]) {
                case OFF_X:
                    v_x = value;
                    break;
                case OFF_Y:
                    v_y = value;
                    break;
                case OFF_WIDTH:
                    v_width = value;
                    break;
                case OFF_HEIGHT:
                    v_height = value;
                    break;
            }
        }
        if (mRelativeToController != null) {
            float[] pos = new float[2];
            float[] vel = new float[2];
            mRelativeToController.getCenter(p, pos, vel);
            float rx = pos[0];
            float ry = pos[1];
            float radius = v_x;
            float angle = v_y;
            // TODO Debug angle
            v_x = (float) (rx + radius * Math.sin(angle) - v_width / 2);
            v_y = (float) (ry - radius * Math.cos(angle) - v_height / 2);
        }

        point[offset] = v_x + v_width / 2 + translationX;
        point[offset + 1] = v_y + v_height / 2 + translationY;
    }

    void getBounds(int[] toUse, double[] data, float[] point, int offset) {
        @SuppressWarnings("unused") float v_x = mX;
        @SuppressWarnings("unused") float v_y = mY;
        float v_width = mWidth;
        float v_height = mHeight;
        for (int i = 0; i < toUse.length; i++) {
            float value = (float) data[i];

            switch (toUse[i]) {
                case OFF_X:
                    v_x = value;
                    break;
                case OFF_Y:
                    v_y = value;
                    break;
                case OFF_WIDTH:
                    v_width = value;
                    break;
                case OFF_HEIGHT:
                    v_height = value;
                    break;
            }
        }
        point[offset] = v_width;
        point[offset + 1] = v_height;
    }

    double[] mTempValue = new double[18];
    double[] mTempDelta = new double[18];

    // Called on the start Time Point
    void setView(float position,
            MotionWidget view,
            int[] toUse,
            double[] data,
            double[] slope,
            double[] cycle) {
        float v_x = mX;
        float v_y = mY;
        float v_width = mWidth;
        float v_height = mHeight;
        float dv_x = 0;
        float dv_y = 0;
        float dv_width = 0;
        float dv_height = 0;
        @SuppressWarnings("unused") float delta_path = 0;
        float path_rotate = Float.NaN;
        @SuppressWarnings("unused") String mod;

        if (toUse.length != 0 && mTempValue.length <= toUse[toUse.length - 1]) {
            int scratch_data_length = toUse[toUse.length - 1] + 1;
            mTempValue = new double[scratch_data_length];
            mTempDelta = new double[scratch_data_length];
        }
        Arrays.fill(mTempValue, Double.NaN);
        for (int i = 0; i < toUse.length; i++) {
            mTempValue[toUse[i]] = data[i];
            mTempDelta[toUse[i]] = slope[i];
        }

        for (int i = 0; i < mTempValue.length; i++) {
            if (Double.isNaN(mTempValue[i]) && (cycle == null || cycle[i] == 0.0)) {
                continue;
            }
            double deltaCycle = (cycle != null) ? cycle[i] : 0.0;
            float value = (float) (Double.isNaN(mTempValue[i])
                    ? deltaCycle : mTempValue[i] + deltaCycle);
            float dvalue = (float) mTempDelta[i];

            switch (i) {
                case OFF_POSITION:
                    delta_path = value;
                    break;
                case OFF_X:
                    v_x = value;
                    dv_x = dvalue;

                    break;
                case OFF_Y:
                    v_y = value;
                    dv_y = dvalue;
                    break;
                case OFF_WIDTH:
                    v_width = value;
                    dv_width = dvalue;
                    break;
                case OFF_HEIGHT:
                    v_height = value;
                    dv_height = dvalue;
                    break;
                case OFF_PATH_ROTATE:
                    path_rotate = value;
                    break;
            }
        }

        if (mRelativeToController != null) {
            float[] pos = new float[2];
            float[] vel = new float[2];

            mRelativeToController.getCenter(position, pos, vel);
            float rx = pos[0];
            float ry = pos[1];
            float radius = v_x;
            float angle = v_y;
            float dradius = dv_x;
            float dangle = dv_y;
            float drx = vel[0];
            float dry = vel[1];

            // TODO Debug angle
            float pos_x = (float) (rx + radius * Math.sin(angle) - v_width / 2);
            float pos_y = (float) (ry - radius * Math.cos(angle) - v_height / 2);
            float dpos_x = (float) (drx + dradius * Math.sin(angle)
                    + radius * Math.cos(angle) * dangle);
            float dpos_y = (float) (dry - dradius * Math.cos(angle)
                    + radius * Math.sin(angle) * dangle);
            dv_x = dpos_x;
            dv_y = dpos_y;
            v_x = pos_x;
            v_y = pos_y;
            if (slope.length >= 2) {
                slope[0] = dpos_x;
                slope[1] = dpos_y;
            }
            if (!Float.isNaN(path_rotate)) {
                float rot = (float) (path_rotate + Math.toDegrees(Math.atan2(dv_y, dv_x)));
                view.setRotationZ(rot);
            }

        } else {

            if (!Float.isNaN(path_rotate)) {
                float rot = 0;
                float dx = dv_x + dv_width / 2;
                float dy = dv_y + dv_height / 2;
                if (DEBUG) {
                    Utils.log(TAG, "dv_x       =" + dv_x);
                    Utils.log(TAG, "dv_y       =" + dv_y);
                    Utils.log(TAG, "dv_width   =" + dv_width);
                    Utils.log(TAG, "dv_height  =" + dv_height);
                }
                rot += (float) (path_rotate + Math.toDegrees(Math.atan2(dy, dx)));
                view.setRotationZ(rot);
                if (DEBUG) {
                    Utils.log(TAG, "Rotated " + rot + "  = " + dx + "," + dy);
                }
            }
        }

        // Todo: develop a concept of Float layout in MotionWidget widget.layout(float ...)
        int l = (int) (0.5f + v_x);
        int t = (int) (0.5f + v_y);
        int r = (int) (0.5f + v_x + v_width);
        int b = (int) (0.5f + v_y + v_height);
        int i_width = r - l;
        int i_height = b - t;
        if (OLD_WAY) { // This way may produce more stable with and height but risk gaps
            l = (int) v_x;
            t = (int) v_y;
            i_width = (int) v_width;
            i_height = (int) v_height;
            r = l + i_width;
            b = t + i_height;
        }

        // MotionWidget must do Android View measure if layout changes
        view.layout(l, t, r, b);
        if (DEBUG) {
            if (toUse.length > 0) {
                Utils.log(TAG, "setView " + mod);
            }
        }
    }

    void getRect(int[] toUse, double[] data, float[] path, int offset) {
        float v_x = mX;
        float v_y = mY;
        float v_width = mWidth;
        float v_height = mHeight;
        @SuppressWarnings("unused") float delta_path = 0;
        float rotation = 0;
        @SuppressWarnings("unused") float alpha = 0;
        @SuppressWarnings("unused") float rotationX = 0;
        @SuppressWarnings("unused") float rotationY = 0;
        float scaleX = 1;
        float scaleY = 1;
        float pivotX = Float.NaN;
        float pivotY = Float.NaN;
        float translationX = 0;
        float translationY = 0;

        @SuppressWarnings("unused") String mod;

        for (int i = 0; i < toUse.length; i++) {
            float value = (float) data[i];

            switch (toUse[i]) {
                case OFF_POSITION:
                    delta_path = value;
                    break;
                case OFF_X:
                    v_x = value;
                    break;
                case OFF_Y:
                    v_y = value;
                    break;
                case OFF_WIDTH:
                    v_width = value;
                    break;
                case OFF_HEIGHT:
                    v_height = value;
                    break;
            }
        }

        if (mRelativeToController != null) {
            float rx = mRelativeToController.getCenterX();
            float ry = mRelativeToController.getCenterY();
            float radius = v_x;
            float angle = v_y;
            // TODO Debug angle
            v_x = (float) (rx + radius * Math.sin(angle) - v_width / 2);
            v_y = (float) (ry - radius * Math.cos(angle) - v_height / 2);
        }

        float x1 = v_x;
        float y1 = v_y;

        float x2 = v_x + v_width;
        float y2 = y1;

        float x3 = x2;
        float y3 = v_y + v_height;

        float x4 = x1;
        float y4 = y3;

        float cx = x1 + v_width / 2;
        float cy = y1 + v_height / 2;

        if (!Float.isNaN(pivotX)) {
            cx = x1 + (x2 - x1) * pivotX;
        }
        if (!Float.isNaN(pivotY)) {

            cy = y1 + (y3 - y1) * pivotY;
        }
        if (scaleX != 1) {
            float midx = (x1 + x2) / 2;
            x1 = (x1 - midx) * scaleX + midx;
            x2 = (x2 - midx) * scaleX + midx;
            x3 = (x3 - midx) * scaleX + midx;
            x4 = (x4 - midx) * scaleX + midx;
        }
        if (scaleY != 1) {
            float midy = (y1 + y3) / 2;
            y1 = (y1 - midy) * scaleY + midy;
            y2 = (y2 - midy) * scaleY + midy;
            y3 = (y3 - midy) * scaleY + midy;
            y4 = (y4 - midy) * scaleY + midy;
        }
        if (rotation != 0) {
            float sin = (float) Math.sin(Math.toRadians(rotation));
            float cos = (float) Math.cos(Math.toRadians(rotation));
            float tx1 = xRotate(sin, cos, cx, cy, x1, y1);
            float ty1 = yRotate(sin, cos, cx, cy, x1, y1);
            float tx2 = xRotate(sin, cos, cx, cy, x2, y2);
            float ty2 = yRotate(sin, cos, cx, cy, x2, y2);
            float tx3 = xRotate(sin, cos, cx, cy, x3, y3);
            float ty3 = yRotate(sin, cos, cx, cy, x3, y3);
            float tx4 = xRotate(sin, cos, cx, cy, x4, y4);
            float ty4 = yRotate(sin, cos, cx, cy, x4, y4);
            x1 = tx1;
            y1 = ty1;
            x2 = tx2;
            y2 = ty2;
            x3 = tx3;
            y3 = ty3;
            x4 = tx4;
            y4 = ty4;
        }

        x1 += translationX;
        y1 += translationY;
        x2 += translationX;
        y2 += translationY;
        x3 += translationX;
        y3 += translationY;
        x4 += translationX;
        y4 += translationY;

        path[offset++] = x1;
        path[offset++] = y1;
        path[offset++] = x2;
        path[offset++] = y2;
        path[offset++] = x3;
        path[offset++] = y3;
        path[offset++] = x4;
        path[offset++] = y4;
    }

    /**
     * mAnchorDpDt
     */
    void setDpDt(float locationX,
            float locationY,
            float[] mAnchorDpDt,
            int[] toUse,
            double[] deltaData,
            double[] data) {

        float d_x = 0;
        float d_y = 0;
        float d_width = 0;
        float d_height = 0;

        float deltaScaleX = 0;
        float deltaScaleY = 0;

        @SuppressWarnings("unused") float mPathRotate = Float.NaN;
        float deltaTranslationX = 0;
        float deltaTranslationY = 0;

        String mod = " dd = ";
        for (int i = 0; i < toUse.length; i++) {
            float deltaV = (float) deltaData[i];
            if (DEBUG) {
                mod += " , D" + sNames[toUse[i]] + "/Dt= " + deltaV;
            }
            switch (toUse[i]) {
                case OFF_POSITION:
                    break;
                case OFF_X:
                    d_x = deltaV;
                    break;
                case OFF_Y:
                    d_y = deltaV;
                    break;
                case OFF_WIDTH:
                    d_width = deltaV;
                    break;
                case OFF_HEIGHT:
                    d_height = deltaV;
                    break;

            }
        }
        if (DEBUG) {
            if (toUse.length > 0) {
                Utils.log(TAG, "setDpDt " + mod);
            }
        }

        float deltaX = d_x - deltaScaleX * d_width / 2;
        float deltaY = d_y - deltaScaleY * d_height / 2;
        float deltaWidth = d_width * (1 + deltaScaleX);
        float deltaHeight = d_height * (1 + deltaScaleY);
        float deltaRight = deltaX + deltaWidth;
        float deltaBottom = deltaY + deltaHeight;
        if (DEBUG) {
            if (toUse.length > 0) {

                Utils.log(TAG, "D x /dt           =" + d_x);
                Utils.log(TAG, "D y /dt           =" + d_y);
                Utils.log(TAG, "D width /dt       =" + d_width);
                Utils.log(TAG, "D height /dt      =" + d_height);
                Utils.log(TAG, "D deltaScaleX /dt =" + deltaScaleX);
                Utils.log(TAG, "D deltaScaleY /dt =" + deltaScaleY);
                Utils.log(TAG, "D deltaX /dt      =" + deltaX);
                Utils.log(TAG, "D deltaY /dt      =" + deltaY);
                Utils.log(TAG, "D deltaWidth /dt  =" + deltaWidth);
                Utils.log(TAG, "D deltaHeight /dt =" + deltaHeight);
                Utils.log(TAG, "D deltaRight /dt  =" + deltaRight);
                Utils.log(TAG, "D deltaBottom /dt =" + deltaBottom);
                Utils.log(TAG, "locationX         =" + locationX);
                Utils.log(TAG, "locationY         =" + locationY);
                Utils.log(TAG, "deltaTranslationX =" + deltaTranslationX);
                Utils.log(TAG, "deltaTranslationX =" + deltaTranslationX);
            }
        }

        mAnchorDpDt[0] = deltaX * (1 - locationX) + deltaRight * locationX + deltaTranslationX;
        mAnchorDpDt[1] = deltaY * (1 - locationY) + deltaBottom * locationY + deltaTranslationY;
    }

    void fillStandard(double[] data, int[] toUse) {
        float[] set = {mPosition, mX, mY, mWidth, mHeight, mPathRotate};
        int c = 0;
        for (int i = 0; i < toUse.length; i++) {
            if (toUse[i] < set.length) {
                data[c++] = set[toUse[i]];
            }
        }
    }

    boolean hasCustomData(String name) {
        return mCustomAttributes.containsKey(name);
    }

    int getCustomDataCount(String name) {
        CustomVariable a = mCustomAttributes.get(name);
        if (a == null) {
            return 0;
        }
        return a.numberOfInterpolatedValues();
    }

    int getCustomData(String name, double[] value, int offset) {
        CustomVariable a = mCustomAttributes.get(name);
        if (a == null) {
            return 0;
        } else if (a.numberOfInterpolatedValues() == 1) {
            value[offset] = a.getValueToInterpolate();
            return 1;
        } else {
            int n = a.numberOfInterpolatedValues();
            float[] f = new float[n];
            a.getValuesToInterpolate(f);
            for (int i = 0; i < n; i++) {
                value[offset++] = f[i];
            }
            return n;
        }
    }

    void setBounds(float x, float y, float w, float h) {
        this.mX = x;
        this.mY = y;
        mWidth = w;
        mHeight = h;
    }

    @Override
    public int compareTo(MotionPaths o) {
        return Float.compare(mPosition, o.mPosition);
    }

    // @TODO: add description
    public void applyParameters(MotionWidget c) {
        MotionPaths point = this;
        point.mKeyFrameEasing = Easing.getInterpolator(c.mMotion.mTransitionEasing);
        point.mPathMotionArc = c.mMotion.mPathMotionArc;
        point.mAnimateRelativeTo = c.mMotion.mAnimateRelativeTo;
        point.mPathRotate = c.mMotion.mPathRotate;
        point.mDrawPath = c.mMotion.mDrawPath;
        point.mAnimateCircleAngleTo = c.mMotion.mAnimateCircleAngleTo;
        point.mProgress = c.mPropertySet.mProgress;
        if (c.mWidgetFrame != null && c.mWidgetFrame.widget != null) {
            point.mRelativeAngle = c.mWidgetFrame.widget.mCircleConstraintAngle;
        }

        Set<String> at = c.getCustomAttributeNames();
        for (String s : at) {
            CustomVariable attr = c.getCustomAttribute(s);
            if (attr != null && attr.isContinuous()) {
                this.mCustomAttributes.put(s, attr);
            }
        }
    }

    // @TODO: add description
    public void configureRelativeTo(Motion toOrbit) {
        @SuppressWarnings("unused") double[] p = toOrbit.getPos(mProgress); // get the position
        // in the orbit
    }
}
