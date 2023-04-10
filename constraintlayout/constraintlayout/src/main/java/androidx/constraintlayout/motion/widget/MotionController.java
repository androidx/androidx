/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.constraintlayout.motion.widget;

import static androidx.constraintlayout.motion.widget.Key.UNSET;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import androidx.constraintlayout.core.motion.utils.CurveFit;
import androidx.constraintlayout.core.motion.utils.Easing;
import androidx.constraintlayout.core.motion.utils.KeyCache;
import androidx.constraintlayout.core.motion.utils.SplineSet;
import androidx.constraintlayout.core.motion.utils.VelocityMatrix;
import androidx.constraintlayout.motion.utils.CustomSupport;
import androidx.constraintlayout.motion.utils.ViewOscillator;
import androidx.constraintlayout.motion.utils.ViewSpline;
import androidx.constraintlayout.motion.utils.ViewState;
import androidx.constraintlayout.motion.utils.ViewTimeCycle;
import androidx.constraintlayout.widget.ConstraintAttribute;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This contains the picture of a view through the a transition and is used to interpolate it
 * During an transition every view has a MotionController which drives its position.
 * <p>
 * All parameter which affect a views motion are added to MotionController and then setup()
 * builds out the splines that control the view.
 *
 *
 */
public class MotionController {
    public static final int PATH_PERCENT = 0;
    public static final int PATH_PERPENDICULAR = 1;
    public static final int HORIZONTAL_PATH_X = 2;
    public static final int HORIZONTAL_PATH_Y = 3;
    public static final int VERTICAL_PATH_X = 4;
    public static final int VERTICAL_PATH_Y = 5;
    public static final int DRAW_PATH_NONE = 0;
    public static final int DRAW_PATH_BASIC = 1;
    public static final int DRAW_PATH_RELATIVE = 2;
    public static final int DRAW_PATH_CARTESIAN = 3;
    public static final int DRAW_PATH_AS_CONFIGURED = 4;
    public static final int DRAW_PATH_RECTANGLE = 5;
    public static final int DRAW_PATH_SCREEN = 6;


    public static final int ROTATION_RIGHT = 1;
    public static final int ROTATION_LEFT = 2;
    Rect mTempRect = new Rect(); // for efficiency

    private static final String TAG = "MotionController";
    private static final boolean DEBUG = false;
    private static final boolean FAVOR_FIXED_SIZE_VIEWS = false;
    View mView;
    int mId;
    boolean mForceMeasure = false;
    String mConstraintTag;
    private int mCurveFitType = KeyFrames.UNSET;
    private MotionPaths mStartMotionPath = new MotionPaths();
    private MotionPaths mEndMotionPath = new MotionPaths();

    private MotionConstrainedPoint mStartPoint = new MotionConstrainedPoint();
    private MotionConstrainedPoint mEndPoint = new MotionConstrainedPoint();

    private CurveFit[] mSpline; // spline 0 is the one that process all the standard attributes
    private CurveFit mArcSpline;
    float mMotionStagger = Float.NaN;
    float mStaggerOffset = 0;
    float mStaggerScale = 1.0f;
    float mCurrentCenterX, mCurrentCenterY;
    private int[] mInterpolateVariables;
    private double[] mInterpolateData; // scratch data created during setup
    private double[] mInterpolateVelocity; // scratch data created during setup

    private String[] mAttributeNames;  // the names of the custom attributes
    private int[] mAttributeInterpolatorCount; // how many interpolators for each custom attribute
    private int mMaxDimension = 4;
    private float []mValuesBuff = new float[mMaxDimension];
    private ArrayList<MotionPaths> mMotionPaths = new ArrayList<>();
    private float[] mVelocity = new float[1]; // used as a temp buffer to return values

    private ArrayList<Key> mKeyList = new ArrayList<>(); // List of key frame items
    private HashMap<String, ViewTimeCycle> mTimeCycleAttributesMap; // splines for use TimeCycles
    private HashMap<String, ViewSpline> mAttributesMap; // splines to calculate values of attributes
    private HashMap<String, ViewOscillator> mCycleMap; // splines to calculate values of attributes
    private KeyTrigger[] mKeyTriggers; // splines to calculate values of attributes
    private int mPathMotionArc = UNSET;
    private int mTransformPivotTarget = UNSET; // if set, pivot point is set to the other object
    private View mTransformPivotView = null; // if set, pivot point is set to the other object
    private int mQuantizeMotionSteps = UNSET;
    private float mQuantizeMotionPhase = Float.NaN;
    private Interpolator mQuantizeMotionInterpolator = null;
    private boolean mNoMovement = false;

    /**
     * Get the view to pivot around
     *
     * @return id of view or UNSET if not set
     */
    public int getTransformPivotTarget() {
        return mTransformPivotTarget;
    }

    /**
     * Set a view to pivot around
     *
     * @param transformPivotTarget id of view
     */
    public void setTransformPivotTarget(int transformPivotTarget) {
        mTransformPivotTarget = transformPivotTarget;
        mTransformPivotView = null;
    }

    MotionPaths getKeyFrame(int i) {
        return mMotionPaths.get(i);
    }

    MotionController(View view) {
        setView(view);
    }

    /**
     * get the left most position of the widget at the start of the movement.
     *
     * @return the left most position
     */
    public float getStartX() {
        return mStartMotionPath.mX;
    }

    /**
     * get the top most position of the widget at the start of the movement.
     * Positive is down.
     *
     * @return the top most position
     */
    public float getStartY() {
        return mStartMotionPath.mY;
    }

    /**
     * get the left most position of the widget at the end of the movement.
     *
     * @return the left most position
     */
    public float getFinalX() {
        return mEndMotionPath.mX;
    }

    /**
     * get the top most position of the widget at the end of the movement.
     * Positive is down.
     *
     * @return the top most position
     */
    public float getFinalY() {
        return mEndMotionPath.mY;
    }

    /**
     * get the width of the widget at the start of the movement.
     *
     * @return the width at the start
     */
    public float getStartWidth() {
        return mStartMotionPath.mWidth;
    }

    /**
     * get the width of the widget at the start of the movement.
     *
     * @return the height at the start
     */
    public float getStartHeight() {
        return mStartMotionPath.mHeight;
    }

    /**
     * get the width of the widget at the end of the movement.
     *
     * @return the width at the end
     */
    public float getFinalWidth() {
        return mEndMotionPath.mWidth;
    }

    /**
     * get the width of the widget at the end of the movement.
     *
     * @return the height at the end
     */
    public float getFinalHeight() {
        return mEndMotionPath.mHeight;
    }

    /**
     * Will return the id of the view to move relative to
     * The position at the start and then end will be viewed relative to this view
     * -1 is the return value if NOT in polar mode
     *
     * @return the view id of the view this is in polar mode to or -1 if not in polar
     */
    public int getAnimateRelativeTo() {
        return mStartMotionPath.mAnimateRelativeTo;
    }

    /**
     * This ties one motionController to another to allow relative pathes
     * @param motionController
     */
    public void setupRelative(MotionController motionController) {
        mStartMotionPath.setupRelative(motionController, motionController.mStartMotionPath);
        mEndMotionPath.setupRelative(motionController, motionController.mEndMotionPath);
    }

    /**
     * Get the center X of the motion at the current progress
     * @return
     */
    public float getCenterX() {
        return mCurrentCenterX;
    }

    /**
     * Get the center Y of the motion at the current progress
     * @return
     */
    public float getCenterY() {
        return mCurrentCenterY;
    }

    /**
     * Get a center and velocities at the position p
     * @param p
     * @param pos
     * @param vel
     */
    public void getCenter(double p, float[] pos, float[] vel) {
        double[] position = new double[4];
        double[] velocity = new double[4];
        @SuppressWarnings("unused") int[] temp = new int[4];
        mSpline[0].getPos(p, position);
        mSpline[0].getSlope(p, velocity);
        Arrays.fill(vel, 0);
        mStartMotionPath.getCenter(p, mInterpolateVariables, position, pos, velocity, vel);
    }

    /**
     * During the next layout call measure then layout
     */
    public void remeasure() {
        mForceMeasure = true;
    }

    /**
     * fill the array point with the center coordinates point[0] is filled with the
     * x coordinate of "time" 0.0 mPoints[point.length-1] is filled with the y coordinate of "time"
     * 1.0
     *
     * @param points     array to fill (should be 2x the number of mPoints
     * @param pointCount
     * @return number of key frames
     */
    void buildPath(float[] points, int pointCount) {
        float mils = 1.0f / (pointCount - 1);
        SplineSet trans_x = (mAttributesMap == null) ? null : mAttributesMap.get(Key.TRANSLATION_X);
        SplineSet trans_y = (mAttributesMap == null) ? null : mAttributesMap.get(Key.TRANSLATION_Y);
        ViewOscillator osc_x = (mCycleMap == null) ? null : mCycleMap.get(Key.TRANSLATION_X);
        ViewOscillator osc_y = (mCycleMap == null) ? null : mCycleMap.get(Key.TRANSLATION_Y);

        for (int i = 0; i < pointCount; i++) {
            float position = i * mils;
            if (mStaggerScale != 1.0f) {
                if (position < mStaggerOffset) {
                    position = 0;
                }
                if (position > mStaggerOffset && position < 1.0) {
                    position -= mStaggerOffset;
                    position *= mStaggerScale;
                    position = Math.min(position, 1.0f);
                }
            }
            double p = position;

            Easing easing = mStartMotionPath.mKeyFrameEasing;
            float start = 0;
            float end = Float.NaN;
            for (MotionPaths frame : mMotionPaths) {
                if (frame.mKeyFrameEasing != null) { // this frame has an easing
                    if (frame.mTime < position) {  // frame with easing is before the current pos
                        easing = frame.mKeyFrameEasing; // this is the candidate
                        start = frame.mTime; // this is also the starting time
                    } else { // frame with easing is past the pos
                        if (Float.isNaN(end)) { // we never ended the time line
                            end = frame.mTime;
                        }
                    }
                }
            }

            if (easing != null) {
                if (Float.isNaN(end)) {
                    end = 1.0f;
                }
                float offset = (position - start) / (end - start);
                offset = (float) easing.get(offset);
                p = offset * (end - start) + start;

            }

            mSpline[0].getPos(p, mInterpolateData);
            if (mArcSpline != null) {
                if (mInterpolateData.length > 0) {
                    mArcSpline.getPos(p, mInterpolateData);
                }
            }
            mStartMotionPath.getCenter(p, mInterpolateVariables, mInterpolateData,
                    points, i * 2);

            if (osc_x != null) {
                points[i * 2] += osc_x.get(position);
            } else if (trans_x != null) {
                points[i * 2] += trans_x.get(position);
            }
            if (osc_y != null) {
                points[i * 2 + 1] += osc_y.get(position);
            } else if (trans_y != null) {
                points[i * 2 + 1] += trans_y.get(position);
            }
        }
    }

    double[] getPos(double position) {
        mSpline[0].getPos(position, mInterpolateData);
        if (mArcSpline != null) {
            if (mInterpolateData.length > 0) {
                mArcSpline.getPos(position, mInterpolateData);
            }
        }
        return mInterpolateData;
    }

    /**
     * fill the array point with the center coordinates point[0] is filled with the
     * x coordinate of "time" 0.0 mPoints[point.length-1] is filled with the y coordinate of "time"
     * 1.0
     *
     * @param bounds     array to fill (should be 2x the number of mPoints
     * @param pointCount
     * @return number of key frames
     */
    void buildBounds(float[] bounds, int pointCount) {
        float mils = 1.0f / (pointCount - 1);
        @SuppressWarnings("unused")
        SplineSet trans_x = (mAttributesMap == null) ? null : mAttributesMap.get(Key.TRANSLATION_X);
        @SuppressWarnings("unused")
        SplineSet trans_y = (mAttributesMap == null) ? null : mAttributesMap.get(Key.TRANSLATION_Y);
        @SuppressWarnings("unused")
        ViewOscillator osc_x = (mCycleMap == null) ? null : mCycleMap.get(Key.TRANSLATION_X);
        @SuppressWarnings("unused")
        ViewOscillator osc_y = (mCycleMap == null) ? null : mCycleMap.get(Key.TRANSLATION_Y);

        for (int i = 0; i < pointCount; i++) {
            float position = i * mils;
            if (mStaggerScale != 1.0f) {
                if (position < mStaggerOffset) {
                    position = 0;
                }
                if (position > mStaggerOffset && position < 1.0) {
                    position -= mStaggerOffset;
                    position *= mStaggerScale;
                    position = Math.min(position, 1.0f);
                }
            }
            double p = position;

            Easing easing = mStartMotionPath.mKeyFrameEasing;
            float start = 0;
            float end = Float.NaN;
            for (MotionPaths frame : mMotionPaths) {
                if (frame.mKeyFrameEasing != null) { // this frame has an easing
                    if (frame.mTime < position) {  // frame with easing is before the current pos
                        easing = frame.mKeyFrameEasing; // this is the candidate
                        start = frame.mTime; // this is also the starting time
                    } else { // frame with easing is past the pos
                        if (Float.isNaN(end)) { // we never ended the time line
                            end = frame.mTime;
                        }
                    }
                }
            }

            if (easing != null) {
                if (Float.isNaN(end)) {
                    end = 1.0f;
                }
                float offset = (position - start) / (end - start);
                offset = (float) easing.get(offset);
                p = offset * (end - start) + start;

            }

            mSpline[0].getPos(p, mInterpolateData);
            if (mArcSpline != null) {
                if (mInterpolateData.length > 0) {
                    mArcSpline.getPos(p, mInterpolateData);
                }
            }
            mStartMotionPath.getBounds(mInterpolateVariables, mInterpolateData, bounds, i * 2);
        }
    }

    private float getPreCycleDistance() {
        int pointCount = 100;
        float[] points = new float[2];
        float sum = 0;
        float mils = 1.0f / (pointCount - 1);
        double x = 0, y = 0;
        for (int i = 0; i < pointCount; i++) {
            float position = i * mils;

            double p = position;

            Easing easing = mStartMotionPath.mKeyFrameEasing;
            float start = 0;
            float end = Float.NaN;
            for (MotionPaths frame : mMotionPaths) {
                if (frame.mKeyFrameEasing != null) { // this frame has an easing
                    if (frame.mTime < position) {  // frame with easing is before the current pos
                        easing = frame.mKeyFrameEasing; // this is the candidate
                        start = frame.mTime; // this is also the starting time
                    } else { // frame with easing is past the pos
                        if (Float.isNaN(end)) { // we never ended the time line
                            end = frame.mTime;
                        }
                    }
                }
            }

            if (easing != null) {
                if (Float.isNaN(end)) {
                    end = 1.0f;
                }
                float offset = (position - start) / (end - start);
                offset = (float) easing.get(offset);
                p = offset * (end - start) + start;

            }

            mSpline[0].getPos(p, mInterpolateData);
            mStartMotionPath.getCenter(p, mInterpolateVariables, mInterpolateData, points, 0);
            if (i > 0) {
                sum += (float) Math.hypot(y - points[1], x - points[0]);
            }
            x = points[0];
            y = points[1];
        }
        return sum;
    }

    KeyPositionBase getPositionKeyframe(int layoutWidth, int layoutHeight, float x, float y) {
        RectF start = new RectF();
        start.left = mStartMotionPath.mX;
        start.top = mStartMotionPath.mY;
        start.right = start.left + mStartMotionPath.mWidth;
        start.bottom = start.top + mStartMotionPath.mHeight;
        RectF end = new RectF();
        end.left = mEndMotionPath.mX;
        end.top = mEndMotionPath.mY;
        end.right = end.left + mEndMotionPath.mWidth;
        end.bottom = end.top + mEndMotionPath.mHeight;
        for (Key key : mKeyList) {
            if (key instanceof KeyPositionBase) {
                if (((KeyPositionBase) key).intersects(layoutWidth, layoutHeight,
                        start, end, x, y)) {
                    return (KeyPositionBase) key;
                }
            }
        }
        return null;
    }

    int buildKeyFrames(float[] keyFrames, int[] mode) {
        if (keyFrames != null) {
            int count = 0;
            double[] time = mSpline[0].getTimePoints();
            if (mode != null) {
                for (MotionPaths keyFrame : mMotionPaths) {
                    mode[count++] = keyFrame.mMode;
                }
                count = 0;
            }

            for (int i = 0; i < time.length; i++) {
                mSpline[0].getPos(time[i], mInterpolateData);
                mStartMotionPath.getCenter(time[i], mInterpolateVariables, mInterpolateData,
                        keyFrames, count);
                count += 2;
            }
            return count / 2;
        }
        return 0;
    }

    int buildKeyBounds(float[] keyBounds, int[] mode) {
        if (keyBounds != null) {
            int count = 0;
            double[] time = mSpline[0].getTimePoints();
            if (mode != null) {
                for (MotionPaths keyFrame : mMotionPaths) {
                    mode[count++] = keyFrame.mMode;
                }
                count = 0;
            }

            for (int i = 0; i < time.length; i++) {
                mSpline[0].getPos(time[i], mInterpolateData);
                mStartMotionPath.getBounds(mInterpolateVariables, mInterpolateData,
                        keyBounds, count);
                count += 2;
            }
            return count / 2;
        }
        return 0;
    }

    String[] mAttributeTable;

    int getAttributeValues(String attributeType, float[] points, int pointCount) {
        @SuppressWarnings("unused") float mils = 1.0f / (pointCount - 1);
        SplineSet spline = mAttributesMap.get(attributeType);
        if (spline == null) {
            return -1;
        }
        for (int j = 0; j < points.length; j++) {
            points[j] = spline.get(j / (points.length - 1));
        }
        return points.length;
    }

    void buildRect(float p, float[] path, int offset) {
        p = getAdjustedPosition(p, null);
        mSpline[0].getPos(p, mInterpolateData);
        mStartMotionPath.getRect(mInterpolateVariables, mInterpolateData, path, offset);
    }

    void buildRectangles(float[] path, int pointCount) {
        float mils = 1.0f / (pointCount - 1);
        for (int i = 0; i < pointCount; i++) {
            float position = i * mils;
            position = getAdjustedPosition(position, null);
            mSpline[0].getPos(position, mInterpolateData);
            mStartMotionPath.getRect(mInterpolateVariables, mInterpolateData, path, i * 8);
        }
    }

    float getKeyFrameParameter(int type, float x, float y) {

        float dx = mEndMotionPath.mX - mStartMotionPath.mX;
        float dy = mEndMotionPath.mY - mStartMotionPath.mY;
        float startCenterX = mStartMotionPath.mX + mStartMotionPath.mWidth / 2;
        float startCenterY = mStartMotionPath.mY + mStartMotionPath.mHeight / 2;
        float hypotenuse = (float) Math.hypot(dx, dy);
        if (hypotenuse < 0.0000001) {
            return Float.NaN;
        }

        float vx = x - startCenterX;
        float vy = y - startCenterY;
        float distFromStart = (float) Math.hypot(vx, vy);
        if (distFromStart == 0) {
            return 0;
        }
        float pathDistance = (vx * dx + vy * dy);

        switch (type) {
            case PATH_PERCENT:
                return pathDistance / hypotenuse;
            case PATH_PERPENDICULAR:
                return (float) Math.sqrt(hypotenuse * hypotenuse - pathDistance * pathDistance);
            case HORIZONTAL_PATH_X:
                return vx / dx;
            case HORIZONTAL_PATH_Y:
                return vy / dx;
            case VERTICAL_PATH_X:
                return vx / dy;
            case VERTICAL_PATH_Y:
                return vy / dy;
        }
        return 0;
    }

    private void insertKey(MotionPaths point) {
        int pos = Collections.binarySearch(mMotionPaths, point);
        if (pos == 0) {
            Log.e(TAG, " KeyPath position \"" + point.mPosition + "\" outside of range");
        }
        mMotionPaths.add(-pos - 1, point);
    }

    void addKeys(ArrayList<Key> list) {
        mKeyList.addAll(list);
        if (DEBUG) {
            for (Key key : mKeyList) {
                Log.v(TAG, " ################ set = " + key.getClass().getSimpleName());
            }
        }
    }

    /**
     * Add a key to the MotionController
     * @param key
     */
    public void addKey(Key key) {
        mKeyList.add(key);
        if (DEBUG) {
            Log.v(TAG, " ################ addKey = " + key.getClass().getSimpleName());
        }
    }

    public void setPathMotionArc(int arc) {
        mPathMotionArc = arc;
    }

    /**
     * Called after all TimePoints & Cycles have been added;
     * Spines are evaluated
     */
    public void setup(int parentWidth,
                      int parentHeight,
                      float transitionDuration,
                      long currentTime) {
        HashSet<String> springAttributes = new HashSet<>(); // attributes we need to interpolate
        HashSet<String> timeCycleAttributes = new HashSet<>(); // attributes we need to interpolate
        HashSet<String> splineAttributes = new HashSet<>(); // attributes we need to interpolate
        HashSet<String> cycleAttributes = new HashSet<>(); // attributes we need to oscillate
        HashMap<String, Integer> interpolation = new HashMap<>();
        ArrayList<KeyTrigger> triggerList = null;
        if (DEBUG) {
            if (mKeyList == null) {
                Log.v(TAG, ">>>>>>>>>>>>>>> mKeyList==null");

            } else {
                Log.v(TAG, ">>>>>>>>>>>>>>> mKeyList for " + Debug.getName(mView));

            }
        }

        if (mPathMotionArc != UNSET) {
            mStartMotionPath.mPathMotionArc = mPathMotionArc;
        }

        mStartPoint.different(mEndPoint, splineAttributes);
        if (DEBUG) {
            HashSet<String> attr = new HashSet<>();
            mStartPoint.different(mEndPoint, attr);
            Log.v(TAG, ">>>>>>>>>>>>>>> MotionConstrainedPoint found "
                    + Arrays.toString(attr.toArray()));
        }
        if (mKeyList != null) {
            for (Key key : mKeyList) {
                if (key instanceof KeyPosition) {
                    KeyPosition keyPath = (KeyPosition) key;
                    insertKey(new MotionPaths(parentWidth, parentHeight, keyPath,
                            mStartMotionPath, mEndMotionPath));
                    if (keyPath.mCurveFit != UNSET) {
                        mCurveFitType = keyPath.mCurveFit;
                    }
                } else if (key instanceof KeyCycle) {
                    key.getAttributeNames(cycleAttributes);
                } else if (key instanceof KeyTimeCycle) {
                    key.getAttributeNames(timeCycleAttributes);
                } else if (key instanceof KeyTrigger) {
                    if (triggerList == null) {
                        triggerList = new ArrayList<>();
                    }
                    triggerList.add((KeyTrigger) key);
                } else {
                    key.setInterpolation(interpolation);
                    key.getAttributeNames(splineAttributes);
                }
            }
        }

        //--------------------------- trigger support --------------------

        if (triggerList != null) {
            mKeyTriggers = triggerList.toArray(new KeyTrigger[0]);
        }

        if (DEBUG) {
            if (!cycleAttributes.isEmpty()) {
                Log.v(TAG, ">>>>>>>>>>>>>>>>  found cycleA"
                        + Debug.getName(mView) + " cycles     "
                        + Arrays.toString(cycleAttributes.toArray()));
            }
            if (!splineAttributes.isEmpty()) {
                Log.v(TAG, ">>>>>>>>>>>>>>>>  found spline "
                        + Debug.getName(mView) + " attrs      "
                        + Arrays.toString(splineAttributes.toArray()));
            }
            if (!timeCycleAttributes.isEmpty()) {
                Log.v(TAG, ">>>>>>>>>>>>>>>>  found timeCycle "
                        + Debug.getName(mView) + " attrs      "
                        + Arrays.toString(timeCycleAttributes.toArray()));
            }
            if (!springAttributes.isEmpty()) {
                Log.v(TAG, ">>>>>>>>>>>>>>>>  found springs "
                        + Debug.getName(mView) + " attrs      "
                        + Arrays.toString(springAttributes.toArray()));
            }

        }

        //--------------------------- splines support --------------------
        if (!splineAttributes.isEmpty()) {
            mAttributesMap = new HashMap<>();
            for (String attribute : splineAttributes) {
                ViewSpline splineSets;
                if (attribute.startsWith("CUSTOM,")) {
                    SparseArray<ConstraintAttribute> attrList = new SparseArray<>();
                    String customAttributeName = attribute.split(",")[1];
                    for (Key key : mKeyList) {
                        if (key.mCustomConstraints == null) {
                            continue;
                        }
                        ConstraintAttribute customAttribute =
                                key.mCustomConstraints.get(customAttributeName);
                        if (customAttribute != null) {
                            attrList.append(key.mFramePosition, customAttribute);
                        }
                    }
                    splineSets = ViewSpline.makeCustomSpline(attribute, attrList);
                } else {
                    splineSets = ViewSpline.makeSpline(attribute);
                }
                if (splineSets == null) {
                    continue;
                }
                splineSets.setType(attribute);
                mAttributesMap.put(attribute, splineSets);
            }
            if (mKeyList != null) {
                for (Key key : mKeyList) {
                    if ((key instanceof KeyAttributes)) {
                        key.addValues(mAttributesMap);
                    }
                }
            }
            mStartPoint.addValues(mAttributesMap, 0);
            mEndPoint.addValues(mAttributesMap, 100);

            for (String spline : mAttributesMap.keySet()) {
                int curve = CurveFit.SPLINE; // default is SPLINE
                if (interpolation.containsKey(spline)) {
                    Integer boxedCurve = interpolation.get(spline);
                    if (boxedCurve != null) {
                        curve = boxedCurve;
                    }
                }
                SplineSet splineSet = mAttributesMap.get(spline);
                if (splineSet != null) {
                    splineSet.setup(curve);
                }
            }
        }

        //--------------------------- timeCycle support --------------------
        if (!timeCycleAttributes.isEmpty()) {
            if (mTimeCycleAttributesMap == null) {
                mTimeCycleAttributesMap = new HashMap<>();
            }
            for (String attribute : timeCycleAttributes) {
                if (mTimeCycleAttributesMap.containsKey(attribute)) {
                    continue;
                }

                ViewTimeCycle splineSets = null;
                if (attribute.startsWith("CUSTOM,")) {
                    SparseArray<ConstraintAttribute> attrList = new SparseArray<>();
                    String customAttributeName = attribute.split(",")[1];
                    for (Key key : mKeyList) {
                        if (key.mCustomConstraints == null) {
                            continue;
                        }
                        ConstraintAttribute customAttribute =
                                key.mCustomConstraints.get(customAttributeName);
                        if (customAttribute != null) {
                            attrList.append(key.mFramePosition, customAttribute);
                        }
                    }
                    splineSets = ViewTimeCycle.makeCustomSpline(attribute, attrList);
                } else {
                    splineSets = ViewTimeCycle.makeSpline(attribute, currentTime);

                }
                if (splineSets == null) {
                    continue;
                }
                splineSets.setType(attribute);
                mTimeCycleAttributesMap.put(attribute, splineSets);
            }

            if (mKeyList != null) {
                for (Key key : mKeyList) {
                    if (key instanceof KeyTimeCycle) {
                        ((KeyTimeCycle) key).addTimeValues(mTimeCycleAttributesMap);
                    }
                }
            }

            for (String spline : mTimeCycleAttributesMap.keySet()) {
                int curve = CurveFit.SPLINE; // default is SPLINE
                if (interpolation.containsKey(spline)) {
                    curve = interpolation.get(spline);
                }
                mTimeCycleAttributesMap.get(spline).setup(curve);
            }
        }

        //--------------------------------- end new key frame 2

        MotionPaths[] points = new MotionPaths[2 + mMotionPaths.size()];
        int count = 1;
        points[0] = mStartMotionPath;
        points[points.length - 1] = mEndMotionPath;
        if (mMotionPaths.size() > 0 && mCurveFitType == KeyFrames.UNSET) {
            mCurveFitType = CurveFit.SPLINE;
        }
        for (MotionPaths point : mMotionPaths) {
            points[count++] = point;
        }

        // -----  setup custom attributes which must be in the start and end constraint sets
        int variables = 18;
        HashSet<String> attributeNameSet = new HashSet<>();
        for (String s : mEndMotionPath.mAttributes.keySet()) {
            if (mStartMotionPath.mAttributes.containsKey(s)) {
                if (!splineAttributes.contains("CUSTOM," + s)) {
                    attributeNameSet.add(s);
                }
            }
        }

        mAttributeNames = attributeNameSet.toArray(new String[0]);
        mAttributeInterpolatorCount = new int[mAttributeNames.length];
        for (int i = 0; i < mAttributeNames.length; i++) {
            String attributeName = mAttributeNames[i];
            mAttributeInterpolatorCount[i] = 0;
            for (int j = 0; j < points.length; j++) {
                if (points[j].mAttributes.containsKey(attributeName)) {
                    ConstraintAttribute attribute = points[j].mAttributes.get(attributeName);
                    if (attribute != null) {
                        mAttributeInterpolatorCount[i] += attribute.numberOfInterpolatedValues();
                        break;
                    }
                }
            }
        }
        boolean arcMode = points[0].mPathMotionArc != UNSET;
        boolean[] mask = new boolean[variables + mAttributeNames.length]; // defaults to false
        for (int i = 1; i < points.length; i++) {
            points[i].different(points[i - 1], mask, mAttributeNames, arcMode);
        }

        count = 0;
        for (int i = 1; i < mask.length; i++) {
            if (mask[i]) {
                count++;
            }
        }

        mInterpolateVariables = new int[count];
        int varLen = Math.max(2, count);
        mInterpolateData = new double[varLen];
        mInterpolateVelocity = new double[varLen];

        count = 0;
        for (int i = 1; i < mask.length; i++) {
            if (mask[i]) {
                mInterpolateVariables[count++] = i;
            }
        }

        double[][] splineData = new double[points.length][mInterpolateVariables.length];
        double[] timePoint = new double[points.length];

        for (int i = 0; i < points.length; i++) {
            points[i].fillStandard(splineData[i], mInterpolateVariables);
            timePoint[i] = points[i].mTime;
        }

        for (int j = 0; j < mInterpolateVariables.length; j++) {
            int interpolateVariable = mInterpolateVariables[j];
            if (interpolateVariable < MotionPaths.sNames.length) {
                @SuppressWarnings("unused")
                String s = MotionPaths.sNames[mInterpolateVariables[j]] + " [";
                for (int i = 0; i < points.length; i++) {
                    s += splineData[i][j];
                }
            }
        }
        mSpline = new CurveFit[1 + mAttributeNames.length];

        for (int i = 0; i < mAttributeNames.length; i++) {
            int pointCount = 0;
            double[][] splinePoints = null;
            double[] timePoints = null;
            String name = mAttributeNames[i];

            for (int j = 0; j < points.length; j++) {
                if (points[j].hasCustomData(name)) {
                    if (splinePoints == null) {
                        timePoints = new double[points.length];
                        splinePoints =
                                new double[points.length][points[j].getCustomDataCount(name)];
                    }
                    timePoints[pointCount] = points[j].mTime;
                    points[j].getCustomData(name, splinePoints[pointCount], 0);
                    pointCount++;
                }
            }
            timePoints = Arrays.copyOf(timePoints, pointCount);
            splinePoints = Arrays.copyOf(splinePoints, pointCount);
            mSpline[i + 1] = CurveFit.get(mCurveFitType, timePoints, splinePoints);
        }

        mSpline[0] = CurveFit.get(mCurveFitType, timePoint, splineData);
        // --------------------------- SUPPORT ARC MODE --------------
        if (points[0].mPathMotionArc != UNSET) {
            int size = points.length;
            int[] mode = new int[size];
            double[] time = new double[size];
            double[][] values = new double[size][2];
            for (int i = 0; i < size; i++) {
                mode[i] = points[i].mPathMotionArc;
                time[i] = points[i].mTime;
                values[i][0] = points[i].mX;
                values[i][1] = points[i].mY;
            }

            mArcSpline = CurveFit.getArc(mode, time, values);
        }

        //--------------------------- Cycle support --------------------
        float distance = Float.NaN;
        mCycleMap = new HashMap<>();
        if (mKeyList != null) {
            for (String attribute : cycleAttributes) {
                ViewOscillator cycle = ViewOscillator.makeSpline(attribute);
                if (cycle == null) {
                    continue;
                }

                if (cycle.variesByPath()) {
                    if (Float.isNaN(distance)) {
                        distance = getPreCycleDistance();
                    }
                }
                cycle.setType(attribute);
                mCycleMap.put(attribute, cycle);
            }
            for (Key key : mKeyList) {
                if (key instanceof KeyCycle) {
                    ((KeyCycle) key).addCycleValues(mCycleMap);
                }
            }
            for (ViewOscillator cycle : mCycleMap.values()) {
                cycle.setup(distance);
            }
        }

        if (DEBUG) {
            Log.v(TAG, "Animation of splineAttributes "
                    + Arrays.toString(splineAttributes.toArray()));
            Log.v(TAG, "Animation of cycle " + Arrays.toString(mCycleMap.keySet().toArray()));
            if (mAttributesMap != null) {
                Log.v(TAG, " splines = "
                        + Arrays.toString(mAttributesMap.keySet().toArray()));
                for (String s : mAttributesMap.keySet()) {
                    Log.v(TAG, s + " = " + mAttributesMap.get(s));
                }
            }
            Log.v(TAG, " ---------------------------------------- ");
        }

        //--------------------------- end cycle support ----------------
    }

    /**
     * Debug string
     *
     * @return
     */
    @Override
    public String toString() {
        return " start: x: " + mStartMotionPath.mX + " y: " + mStartMotionPath.mY
                + " end: x: " + mEndMotionPath.mX + " y: " + mEndMotionPath.mY;
    }

    private void readView(MotionPaths motionPaths) {
        motionPaths.setBounds((int) mView.getX(), (int) mView.getY(),
                mView.getWidth(), mView.getHeight());
    }

    // @TODO: add description

    /**
     *
     * @param view
     */
    public void setView(View view) {
        mView = view;
        mId = view.getId();
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof ConstraintLayout.LayoutParams) {
            mConstraintTag = ((ConstraintLayout.LayoutParams) lp).getConstraintTag();
        }
    }

    /**
     * Get the view that is being controlled
     * @return
     */
    public View getView() {
        return mView;
    }

    void setStartCurrentState(View v) {
        mStartMotionPath.mTime = 0;
        mStartMotionPath.mPosition = 0;
        mStartMotionPath.setBounds(v.getX(), v.getY(), v.getWidth(), v.getHeight());
        mStartPoint.setState(v);
    }

    /**
     * configure the position of the view
     * @param rect
     * @param v
     * @param rotation
     * @param preWidth
     * @param preHeight
     */
    public void setStartState(ViewState rect, View v, int rotation, int preWidth, int preHeight) {
        mStartMotionPath.mTime = 0;
        mStartMotionPath.mPosition = 0;
        int cx, cy;
        Rect r = new Rect();
        switch (rotation) {
            case 2:
                cx = rect.left + rect.right;
                cy = rect.top + rect.bottom;
                r.left = preHeight - (cy + rect.width()) / 2;
                r.top = (cx - rect.height()) / 2;
                r.right = r.left + rect.width();
                r.bottom = r.top + rect.height();
                break;
            case 1:
                cx = rect.left + rect.right;
                cy = rect.top + rect.bottom;
                r.left = (cy - rect.width()) / 2;
                r.top = preWidth - (cx + rect.height()) / 2;
                r.right = r.left + rect.width();
                r.bottom = r.top + rect.height();
                break;
        }
        mStartMotionPath.setBounds(r.left, r.top, r.width(), r.height());
        mStartPoint.setState(r, v, rotation, rect.rotation);
    }

    void rotate(Rect rect, Rect out, int rotation, int preHeight, int preWidth) {
        int cx, cy;
        switch (rotation) {

            case ConstraintSet.ROTATE_PORTRATE_OF_LEFT:
                cx = rect.left + rect.right;
                cy = rect.top + rect.bottom;
                out.left = preHeight - (cy + rect.width()) / 2;
                out.top = (cx - rect.height()) / 2;
                out.right = out.left + rect.width();
                out.bottom = out.top + rect.height();
                break;
            case ConstraintSet.ROTATE_PORTRATE_OF_RIGHT:
                cx = rect.left + rect.right;
                cy = rect.top + rect.bottom;
                out.left = (cy - rect.width()) / 2;
                out.top = preWidth - (cx + rect.height()) / 2;
                out.right = out.left + rect.width();
                out.bottom = out.top + rect.height();
                break;
            case ConstraintSet.ROTATE_LEFT_OF_PORTRATE:
                cx = rect.left + rect.right;
                cy = rect.bottom + rect.top;
                out.left = preHeight - (cy + rect.width()) / 2;
                out.top = (cx - rect.height()) / 2;
                out.right = out.left + rect.width();
                out.bottom = out.top + rect.height();
                break;
            case ConstraintSet.ROTATE_RIGHT_OF_PORTRATE:
                cx = rect.left + rect.right;
                cy = rect.top + rect.bottom;
                out.left = rect.height() / 2 + rect.top - cx / 2;
                out.top = preWidth - (cx + rect.height()) / 2;
                out.right = out.left + rect.width();
                out.bottom = out.top + rect.height();
                break;
        }
    }

    void setStartState(Rect cw, ConstraintSet constraintSet, int parentWidth, int parentHeight) {
        int rotate = constraintSet.mRotate; // for rotated frames
        if (rotate != 0) {
            rotate(cw, mTempRect, rotate, parentWidth, parentHeight);
        }
        mStartMotionPath.mTime = 0;
        mStartMotionPath.mPosition = 0;
        readView(mStartMotionPath);
        mStartMotionPath.setBounds(cw.left, cw.top, cw.width(), cw.height());
        ConstraintSet.Constraint constraint = constraintSet.getParameters(mId);
        mStartMotionPath.applyParameters(constraint);
        mMotionStagger = constraint.motion.mMotionStagger;
        mStartPoint.setState(cw, constraintSet, rotate, mId);
        mTransformPivotTarget = constraint.transform.transformPivotTarget;
        mQuantizeMotionSteps = constraint.motion.mQuantizeMotionSteps;
        mQuantizeMotionPhase = constraint.motion.mQuantizeMotionPhase;
        mQuantizeMotionInterpolator = getInterpolator(mView.getContext(),
                constraint.motion.mQuantizeInterpolatorType,
                constraint.motion.mQuantizeInterpolatorString,
                constraint.motion.mQuantizeInterpolatorID
        );
    }

    static final int EASE_IN_OUT = 0;
    static final int EASE_IN = 1;
    static final int EASE_OUT = 2;
    static final int LINEAR = 3;
    static final int BOUNCE = 4;
    static final int OVERSHOOT = 5;
    private static final int SPLINE_STRING = -1;
    private static final int INTERPOLATOR_REFERENCE_ID = -2;
    private static final int INTERPOLATOR_UNDEFINED = -3;

    private static Interpolator getInterpolator(Context context,
                                                int type,
                                                String interpolatorString,
                                                int id) {
        switch (type) {
            case SPLINE_STRING:
                final Easing easing = Easing.getInterpolator(interpolatorString);
                return new Interpolator() {
                    @Override
                    public float getInterpolation(float v) {
                        return (float) easing.get(v);
                    }
                };
            case INTERPOLATOR_REFERENCE_ID:
                return AnimationUtils.loadInterpolator(context, id);
            case EASE_IN_OUT:
                return new AccelerateDecelerateInterpolator();
            case EASE_IN:
                return new AccelerateInterpolator();
            case EASE_OUT:
                return new DecelerateInterpolator();
            case LINEAR:
                return null;
            case BOUNCE:
                return new BounceInterpolator();
            case OVERSHOOT:
                return new OvershootInterpolator();
            case INTERPOLATOR_UNDEFINED:
                return null;
        }
        return null;
    }

    void setEndState(Rect cw, ConstraintSet constraintSet, int parentWidth, int parentHeight) {
        int rotate = constraintSet.mRotate; // for rotated frames
        if (rotate != 0) {
            rotate(cw, mTempRect, rotate, parentWidth, parentHeight);
            cw = mTempRect;
        }
        mEndMotionPath.mTime = 1;
        mEndMotionPath.mPosition = 1;
        readView(mEndMotionPath);
        mEndMotionPath.setBounds(cw.left, cw.top, cw.width(), cw.height());
        mEndMotionPath.applyParameters(constraintSet.getParameters(mId));
        mEndPoint.setState(cw, constraintSet, rotate, mId);
    }

    void setBothStates(View v) {
        mStartMotionPath.mTime = 0;
        mStartMotionPath.mPosition = 0;
        mNoMovement = true;
        mStartMotionPath.setBounds(v.getX(), v.getY(), v.getWidth(), v.getHeight());
        mEndMotionPath.setBounds(v.getX(), v.getY(), v.getWidth(), v.getHeight());
        mStartPoint.setState(v);
        mEndPoint.setState(v);
    }

    /**
     * Calculates the adjusted (and optional velocity)
     * Note if requesting velocity staggering is not considered
     *
     * @param position position pre stagger
     * @param velocity return velocity
     * @return actual position accounting for easing and staggering
     */
    private float getAdjustedPosition(float position, float[] velocity) {
        if (velocity != null) {
            velocity[0] = 1;
        } else if (mStaggerScale != 1.0) {
            if (position < mStaggerOffset) {
                position = 0;
            }
            if (position > mStaggerOffset && position < 1.0) {
                position -= mStaggerOffset;
                position *= mStaggerScale;
                position = Math.min(position, 1.0f);
            }
        }

        // adjust the position based on the easing curve
        float adjusted = position;
        Easing easing = mStartMotionPath.mKeyFrameEasing;
        float start = 0;
        float end = Float.NaN;
        for (MotionPaths frame : mMotionPaths) {
            if (frame.mKeyFrameEasing != null) { // this frame has an easing
                if (frame.mTime < position) {  // frame with easing is before the current pos
                    easing = frame.mKeyFrameEasing; // this is the candidate
                    start = frame.mTime; // this is also the starting time
                } else { // frame with easing is past the pos
                    if (Float.isNaN(end)) { // we never ended the time line
                        end = frame.mTime;
                    }
                }
            }
        }

        if (easing != null) {
            if (Float.isNaN(end)) {
                end = 1.0f;
            }
            float offset = (position - start) / (end - start);
            float new_offset = (float) easing.get(offset);
            adjusted = new_offset * (end - start) + start;
            if (velocity != null) {
                velocity[0] = (float) easing.getDiff(offset);
            }
        }
        return adjusted;
    }

    void endTrigger(boolean start) {
        if ("button".equals(Debug.getName(mView))) {
            if (mKeyTriggers != null) {
                for (int i = 0; i < mKeyTriggers.length; i++) {
                    mKeyTriggers[i].conditionallyFire(start ? -100 : 100, mView);
                }
            }
        }
    }

    /**
     * The main driver of interpolation
     *
     * @param child
     * @param globalPosition
     * @param time
     * @param keyCache
     * @return do you need to keep animating
     */
    boolean interpolate(View child, float globalPosition, long time, KeyCache keyCache) {
        boolean timeAnimation = false;
        float position = getAdjustedPosition(globalPosition, null);
        // This quantize the position into steps e.g. 4 steps = 0-0.25,0.25-0.50 etc
        if (mQuantizeMotionSteps != UNSET) {
            float steps = 1.0f / mQuantizeMotionSteps; // the length of a step
            float jump = (float) Math.floor(position / steps) * steps; // step jumps
            float section = (position % steps) / steps; // float from 0 to 1 in a step

            if (!Float.isNaN(mQuantizeMotionPhase)) {
                section = (section + mQuantizeMotionPhase) % 1;
            }
            if (mQuantizeMotionInterpolator != null) {
                section = mQuantizeMotionInterpolator.getInterpolation(section);
            } else {
                section = section > 0.5 ? 1 : 0;
            }
            position = section * steps + jump;
        }
        ViewTimeCycle.PathRotate timePathRotate = null;
        if (mAttributesMap != null) {
            for (ViewSpline aSpline : mAttributesMap.values()) {
                aSpline.setProperty(child, position);
            }
        }

        if (mTimeCycleAttributesMap != null) {
            for (ViewTimeCycle aSpline : mTimeCycleAttributesMap.values()) {
                if (aSpline instanceof ViewTimeCycle.PathRotate) {
                    timePathRotate = (ViewTimeCycle.PathRotate) aSpline;
                    continue;
                }
                timeAnimation |= aSpline.setProperty(child, position, time, keyCache);
            }
        }

        if (mSpline != null) {
            mSpline[0].getPos(position, mInterpolateData);
            mSpline[0].getSlope(position, mInterpolateVelocity);
            if (mArcSpline != null) {
                if (mInterpolateData.length > 0) {
                    mArcSpline.getPos(position, mInterpolateData);
                    mArcSpline.getSlope(position, mInterpolateVelocity);
                }
            }

            if (!mNoMovement) {
                mStartMotionPath.setView(position, child,
                        mInterpolateVariables, mInterpolateData, mInterpolateVelocity,
                        null, mForceMeasure);
                mForceMeasure = false;
            }
            if (mTransformPivotTarget != UNSET) {
                if (mTransformPivotView == null) {
                    View layout = (View) child.getParent();
                    mTransformPivotView = layout.findViewById(mTransformPivotTarget);
                }
                if (mTransformPivotView != null) {
                    float cy = (mTransformPivotView.getTop()
                            + mTransformPivotView.getBottom()) / 2.0f;
                    float cx = (mTransformPivotView.getLeft()
                            + mTransformPivotView.getRight()) / 2.0f;
                    if (child.getRight() - child.getLeft() > 0
                            && child.getBottom() - child.getTop() > 0) {
                        float px = (cx - child.getLeft());
                        float py = (cy - child.getTop());
                        child.setPivotX(px);
                        child.setPivotY(py);
                    }
                }
            }

            if (mAttributesMap != null) {
                for (SplineSet aSpline : mAttributesMap.values()) {
                    if (aSpline instanceof ViewSpline.PathRotate
                            && mInterpolateVelocity.length > 1) {
                        ((ViewSpline.PathRotate) aSpline).setPathRotate(child, position,
                                mInterpolateVelocity[0], mInterpolateVelocity[1]);
                    }
                }

            }
            if (timePathRotate != null) {
                timeAnimation |= timePathRotate.setPathRotate(child, keyCache, position, time,
                        mInterpolateVelocity[0], mInterpolateVelocity[1]);
            }

            for (int i = 1; i < mSpline.length; i++) {
                CurveFit spline = mSpline[i];
                spline.getPos(position, mValuesBuff);
                CustomSupport.setInterpolatedValue(mStartMotionPath
                        .mAttributes.get(mAttributeNames[i - 1]), child, mValuesBuff);

            }
            if (mStartPoint.mVisibilityMode == ConstraintSet.VISIBILITY_MODE_NORMAL) {
                if (position <= 0.0f) {
                    child.setVisibility(mStartPoint.mVisibility);
                } else if (position >= 1.0f) {
                    child.setVisibility(mEndPoint.mVisibility);
                } else if (mEndPoint.mVisibility != mStartPoint.mVisibility) {
                    child.setVisibility(View.VISIBLE);
                }
            }

            if (mKeyTriggers != null) {
                for (int i = 0; i < mKeyTriggers.length; i++) {
                    mKeyTriggers[i].conditionallyFire(position, child);
                }
            }
        } else {
            // do the interpolation

            float float_l = (mStartMotionPath.mX
                    + (mEndMotionPath.mX - mStartMotionPath.mX) * position);
            float float_t = (mStartMotionPath.mY
                    + (mEndMotionPath.mY - mStartMotionPath.mY) * position);
            float float_width = (mStartMotionPath.mWidth
                    + (mEndMotionPath.mWidth - mStartMotionPath.mWidth) * position);
            float float_height = (mStartMotionPath.mHeight
                    + (mEndMotionPath.mHeight - mStartMotionPath.mHeight) * position);
            int l = (int) (0.5f + float_l);
            int t = (int) (0.5f + float_t);
            int r = (int) (0.5f + float_l + float_width);
            int b = (int) (0.5f + float_t + float_height);
            int width = r - l;
            int height = b - t;

            if (FAVOR_FIXED_SIZE_VIEWS) {
                l = (int) (mStartMotionPath.mX
                        + (mEndMotionPath.mX - mStartMotionPath.mX) * position);
                t = (int) (mStartMotionPath.mY
                        + (mEndMotionPath.mY - mStartMotionPath.mY) * position);
                width = (int) (mStartMotionPath.mWidth
                        + (mEndMotionPath.mWidth - mStartMotionPath.mWidth) * position);
                height = (int) (mStartMotionPath.mHeight
                        + (mEndMotionPath.mHeight - mStartMotionPath.mHeight) * position);
                r = l + width;
                b = t + height;
            }
            if (mEndMotionPath.mWidth != mStartMotionPath.mWidth
                    || mEndMotionPath.mHeight != mStartMotionPath.mHeight || mForceMeasure) {
                int widthMeasureSpec =
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
                int heightMeasureSpec =
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
                child.measure(widthMeasureSpec, heightMeasureSpec);
                mForceMeasure = false;
            }
            child.layout(l, t, r, b);
        }

        if (mCycleMap != null) {
            for (ViewOscillator osc : mCycleMap.values()) {
                if (osc instanceof ViewOscillator.PathRotateSet) {
                    ((ViewOscillator.PathRotateSet) osc).setPathRotate(child, position,
                            mInterpolateVelocity[0], mInterpolateVelocity[1]);
                } else {
                    osc.setProperty(child, position);
                }
            }
        }
        return timeAnimation;
    }

    /**
     * This returns the differential with respect to the animation layout position (Progress)
     * of a point on the view (post layout effects are not computed)
     *
     * @param position    position in time
     * @param locationX   the x location on the view (0 = left edge, 1 = right edge)
     * @param locationY   the y location on the view (0 = top, 1 = bottom)
     * @param mAnchorDpDt returns the differential of the motion with respect to the position
     */
    void getDpDt(float position, float locationX, float locationY, float[] mAnchorDpDt) {
        position = getAdjustedPosition(position, mVelocity);

        if (mSpline != null) {
            mSpline[0].getSlope(position, mInterpolateVelocity);
            mSpline[0].getPos(position, mInterpolateData);
            float v = mVelocity[0];
            for (int i = 0; i < mInterpolateVelocity.length; i++) {
                mInterpolateVelocity[i] *= v;
            }

            if (mArcSpline != null) {
                if (mInterpolateData.length > 0) {
                    mArcSpline.getPos(position, mInterpolateData);
                    mArcSpline.getSlope(position, mInterpolateVelocity);
                    mStartMotionPath.setDpDt(locationX, locationY, mAnchorDpDt,
                            mInterpolateVariables, mInterpolateVelocity, mInterpolateData);
                }
                return;
            }
            mStartMotionPath.setDpDt(locationX, locationY, mAnchorDpDt,
                    mInterpolateVariables, mInterpolateVelocity, mInterpolateData);
            return;
        }
        // do the interpolation
        float dleft = (mEndMotionPath.mX - mStartMotionPath.mX);
        float dTop = (mEndMotionPath.mY - mStartMotionPath.mY);
        float dWidth = (mEndMotionPath.mWidth - mStartMotionPath.mWidth);
        float dHeight = (mEndMotionPath.mHeight - mStartMotionPath.mHeight);
        float dRight = dleft + dWidth;
        float dBottom = dTop + dHeight;
        mAnchorDpDt[0] = dleft * (1 - locationX) + dRight * locationX;
        mAnchorDpDt[1] = dTop * (1 - locationY) + dBottom * locationY;
    }

    /**
     * This returns the differential with respect to the animation post layout transform
     * of a point on the view
     *
     * @param position    position in time
     * @param width       width of the view
     * @param height      height of the view
     * @param locationX   the x location on the view (0 = left edge, 1 = right edge)
     * @param locationY   the y location on the view (0 = top, 1 = bottom)
     * @param mAnchorDpDt returns the differential of the motion with respect to the position
     */
    void getPostLayoutDvDp(float position,
                           int width,
                           int height,
                           float locationX,
                           float locationY,
                           float[] mAnchorDpDt) {
        if (DEBUG) {
            Log.v(TAG, " position= " + position + " location= "
                    + locationX + " , " + locationY);
        }
        position = getAdjustedPosition(position, mVelocity);

        SplineSet trans_x = (mAttributesMap == null) ? null : mAttributesMap.get(Key.TRANSLATION_X);
        SplineSet trans_y = (mAttributesMap == null) ? null : mAttributesMap.get(Key.TRANSLATION_Y);
        SplineSet rotation = (mAttributesMap == null) ? null : mAttributesMap.get(Key.ROTATION);
        SplineSet scale_x = (mAttributesMap == null) ? null : mAttributesMap.get(Key.SCALE_X);
        SplineSet scale_y = (mAttributesMap == null) ? null : mAttributesMap.get(Key.SCALE_Y);

        ViewOscillator osc_x = (mCycleMap == null) ? null : mCycleMap.get(Key.TRANSLATION_X);
        ViewOscillator osc_y = (mCycleMap == null) ? null : mCycleMap.get(Key.TRANSLATION_Y);
        ViewOscillator osc_r = (mCycleMap == null) ? null : mCycleMap.get(Key.ROTATION);
        ViewOscillator osc_sx = (mCycleMap == null) ? null : mCycleMap.get(Key.SCALE_X);
        ViewOscillator osc_sy = (mCycleMap == null) ? null : mCycleMap.get(Key.SCALE_Y);

        VelocityMatrix vmat = new VelocityMatrix();
        vmat.clear();
        vmat.setRotationVelocity(rotation, position);
        vmat.setTranslationVelocity(trans_x, trans_y, position);
        vmat.setScaleVelocity(scale_x, scale_y, position);
        vmat.setRotationVelocity(osc_r, position);
        vmat.setTranslationVelocity(osc_x, osc_y, position);
        vmat.setScaleVelocity(osc_sx, osc_sy, position);
        if (mArcSpline != null) {
            if (mInterpolateData.length > 0) {
                mArcSpline.getPos(position, mInterpolateData);
                mArcSpline.getSlope(position, mInterpolateVelocity);
                mStartMotionPath.setDpDt(locationX, locationY, mAnchorDpDt,
                        mInterpolateVariables, mInterpolateVelocity, mInterpolateData);
            }
            vmat.applyTransform(locationX, locationY, width, height, mAnchorDpDt);
            return;
        }
        if (mSpline != null) {
            position = getAdjustedPosition(position, mVelocity);
            mSpline[0].getSlope(position, mInterpolateVelocity);
            mSpline[0].getPos(position, mInterpolateData);
            float v = mVelocity[0];
            for (int i = 0; i < mInterpolateVelocity.length; i++) {
                mInterpolateVelocity[i] *= v;
            }
            mStartMotionPath.setDpDt(locationX, locationY, mAnchorDpDt,
                    mInterpolateVariables, mInterpolateVelocity, mInterpolateData);
            vmat.applyTransform(locationX, locationY, width, height, mAnchorDpDt);
            return;
        }

        // do the interpolation
        float dleft = (mEndMotionPath.mX - mStartMotionPath.mX);
        float dTop = (mEndMotionPath.mY - mStartMotionPath.mY);
        float dWidth = (mEndMotionPath.mWidth - mStartMotionPath.mWidth);
        float dHeight = (mEndMotionPath.mHeight - mStartMotionPath.mHeight);
        float dRight = dleft + dWidth;
        float dBottom = dTop + dHeight;
        mAnchorDpDt[0] = dleft * (1 - locationX) + dRight * locationX;
        mAnchorDpDt[1] = dTop * (1 - locationY) + dBottom * locationY;

        vmat.clear();
        vmat.setRotationVelocity(rotation, position);
        vmat.setTranslationVelocity(trans_x, trans_y, position);
        vmat.setScaleVelocity(scale_x, scale_y, position);
        vmat.setRotationVelocity(osc_r, position);
        vmat.setTranslationVelocity(osc_x, osc_y, position);
        vmat.setScaleVelocity(osc_sx, osc_sy, position);
        vmat.applyTransform(locationX, locationY, width, height, mAnchorDpDt);
        return;
    }

    /**
     * returns the draw path mode
     * @return
     */
    public int getDrawPath() {
        int mode = mStartMotionPath.mDrawPath;
        for (MotionPaths keyFrame : mMotionPaths) {
            mode = Math.max(mode, keyFrame.mDrawPath);
        }
        mode = Math.max(mode, mEndMotionPath.mDrawPath);
        return mode;
    }

    public void setDrawPath(int debugMode) {
        mStartMotionPath.mDrawPath = debugMode;
    }

    String name() {
        Context context = mView.getContext();
        return context.getResources().getResourceEntryName(mView.getId());
    }

    void positionKeyframe(View view,
                          KeyPositionBase key,
                          float x,
                          float y,
                          String[] attribute,
                          float[] value) {
        RectF start = new RectF();
        start.left = mStartMotionPath.mX;
        start.top = mStartMotionPath.mY;
        start.right = start.left + mStartMotionPath.mWidth;
        start.bottom = start.top + mStartMotionPath.mHeight;
        RectF end = new RectF();
        end.left = mEndMotionPath.mX;
        end.top = mEndMotionPath.mY;
        end.right = end.left + mEndMotionPath.mWidth;
        end.bottom = end.top + mEndMotionPath.mHeight;
        key.positionAttributes(view, start, end, x, y, attribute, value);
    }

    /**
     * Get the keyFrames for the view controlled by this MotionController
     *
     * @param type is position(0-100) + 1000*mType(1=Attr, 2=Pos, 3=TimeCycle 4=Cycle 5=Trigger
     * @param pos  the x&y position of the keyFrame along the path
     * @return Number of keyFrames found
     */
    public int getKeyFramePositions(int[] type, float[] pos) {
        int i = 0;
        int count = 0;
        for (Key key : mKeyList) {
            type[i++] = key.mFramePosition + 1000 * key.mType;
            float time = key.mFramePosition / 100.0f;
            mSpline[0].getPos(time, mInterpolateData);
            mStartMotionPath.getCenter(time, mInterpolateVariables, mInterpolateData, pos, count);
            count += 2;
        }

        return i;
    }

    /**
     * Get the keyFrames for the view controlled by this MotionController
     * the info data structure is of the form
     * 0 length if your are at index i the [i+len+1] is the next entry
     * 1 type  1=Attributes, 2=Position, 3=TimeCycle 4=Cycle 5=Trigger
     * 2 position
     * 3 x location
     * 4 y location
     * 5
     * ...
     * length
     *
     * @param info is a data structure array of int that holds info on each keyframe
     * @return Number of keyFrames found
     */
    public int getKeyFrameInfo(int type, int[] info) {
        int count = 0;
        int cursor = 0;
        float[] pos = new float[2];
        int len;
        for (Key key : mKeyList) {
            if (key.mType != type && type == -1) {
                continue;
            }
            len = cursor;
            info[cursor] = 0;

            info[++cursor] = key.mType;
            info[++cursor] = key.mFramePosition;

            float time = key.mFramePosition / 100.0f;
            mSpline[0].getPos(time, mInterpolateData);
            mStartMotionPath.getCenter(time, mInterpolateVariables, mInterpolateData, pos, 0);
            info[++cursor] = Float.floatToIntBits(pos[0]);
            info[++cursor] = Float.floatToIntBits(pos[1]);
            if (key instanceof KeyPosition) {
                KeyPosition kp = (KeyPosition) key;
                info[++cursor] = kp.mPositionType;

                info[++cursor] = Float.floatToIntBits(kp.mPercentX);
                info[++cursor] = Float.floatToIntBits(kp.mPercentY);
            }
            cursor++;
            info[len] = cursor - len;
            count++;
        }

        return count;
    }
}
