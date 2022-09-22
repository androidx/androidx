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

import androidx.constraintlayout.core.motion.key.MotionConstraintSet;
import androidx.constraintlayout.core.motion.key.MotionKey;
import androidx.constraintlayout.core.motion.key.MotionKeyAttributes;
import androidx.constraintlayout.core.motion.key.MotionKeyCycle;
import androidx.constraintlayout.core.motion.key.MotionKeyPosition;
import androidx.constraintlayout.core.motion.key.MotionKeyTimeCycle;
import androidx.constraintlayout.core.motion.key.MotionKeyTrigger;
import androidx.constraintlayout.core.motion.utils.CurveFit;
import androidx.constraintlayout.core.motion.utils.DifferentialInterpolator;
import androidx.constraintlayout.core.motion.utils.Easing;
import androidx.constraintlayout.core.motion.utils.FloatRect;
import androidx.constraintlayout.core.motion.utils.KeyCache;
import androidx.constraintlayout.core.motion.utils.KeyCycleOscillator;
import androidx.constraintlayout.core.motion.utils.KeyFrameArray;
import androidx.constraintlayout.core.motion.utils.Rect;
import androidx.constraintlayout.core.motion.utils.SplineSet;
import androidx.constraintlayout.core.motion.utils.TimeCycleSplineSet;
import androidx.constraintlayout.core.motion.utils.TypedBundle;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.constraintlayout.core.motion.utils.Utils;
import androidx.constraintlayout.core.motion.utils.VelocityMatrix;
import androidx.constraintlayout.core.motion.utils.ViewState;

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
public class Motion implements TypedValues {
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
    MotionWidget mView;
    public String mId;
    String mConstraintTag;
    private int mCurveFitType = CurveFit.SPLINE;
    private MotionPaths mStartMotionPath = new MotionPaths();
    private MotionPaths mEndMotionPath = new MotionPaths();

    private MotionConstrainedPoint mStartPoint = new MotionConstrainedPoint();
    private MotionConstrainedPoint mEndPoint = new MotionConstrainedPoint();

    // spline 0 is the generic one that process all the standard attributes
    private CurveFit[] mSpline;
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
    private float[] mValuesBuff = new float[mMaxDimension];
    private ArrayList<MotionPaths> mMotionPaths = new ArrayList<>();
    private float[] mVelocity = new float[1]; // used as a temp buffer to return values

    private ArrayList<MotionKey> mKeyList = new ArrayList<>(); // List of key frame items

    // splines to calculate for use TimeCycles
    private HashMap<String, TimeCycleSplineSet> mTimeCycleAttributesMap;
    private HashMap<String, SplineSet> mAttributesMap; // splines to calculate values of attributes

    // splines to calculate values of attributes
    private HashMap<String, KeyCycleOscillator> mCycleMap;
    private MotionKeyTrigger[] mKeyTriggers; // splines to calculate values of attributes
    private int mPathMotionArc = UNSET;

    // if set, pivot point is maintained as the other object
    private int mTransformPivotTarget = UNSET;

    // if set, pivot point is maintained as the other object
    private MotionWidget mTransformPivotView = null;
    private int mQuantizeMotionSteps = UNSET;
    private float mQuantizeMotionPhase = Float.NaN;
    private DifferentialInterpolator mQuantizeMotionInterpolator = null;
    private boolean mNoMovement = false;
    Motion mRelativeMotion;
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

    /**
     * provides access to MotionPath objects
     */
    public MotionPaths getKeyFrame(int i) {
        return mMotionPaths.get(i);
    }

    public Motion(MotionWidget view) {
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
    public String getAnimateRelativeTo() {
        return mStartMotionPath.mAnimateRelativeTo;
    }

    /**
     * set up the motion to be relative to this other motionController
     */
    public void setupRelative(Motion motionController) {
        mRelativeMotion = motionController;
    }

    private void setupRelative() {
        if (mRelativeMotion == null) {
            return;
        }
        mStartMotionPath.setupRelative(mRelativeMotion, mRelativeMotion.mStartMotionPath);
        mEndMotionPath.setupRelative(mRelativeMotion, mRelativeMotion.mEndMotionPath);
    }

    public float getCenterX() {
        return mCurrentCenterX;
    }

    public float getCenterY() {
        return mCurrentCenterY;
    }

    // @TODO: add description
    public void getCenter(double p, float[] pos, float[] vel) {
        double[] position = new double[4];
        double[] velocity = new double[4];
        mSpline[0].getPos(p, position);
        mSpline[0].getSlope(p, velocity);
        Arrays.fill(vel, 0);
        mStartMotionPath.getCenter(p, mInterpolateVariables, position, pos, velocity, vel);
    }

    /**
     * fill the array point with the center coordinates point[0] is filled with the
     * x coordinate of "time" 0.0 mPoints[point.length-1] is filled with the y coordinate of "time"
     * 1.0
     *
     * @param points array to fill (should be 2x the number of mPoints
     */
    public void buildPath(float[] points, int pointCount) {
        float mils = 1.0f / (pointCount - 1);
        SplineSet trans_x =
                (mAttributesMap == null) ? null : mAttributesMap.get(MotionKey.TRANSLATION_X);
        SplineSet trans_y =
                (mAttributesMap == null) ? null : mAttributesMap.get(MotionKey.TRANSLATION_Y);
        KeyCycleOscillator osc_x =
                (mCycleMap == null) ? null : mCycleMap.get(MotionKey.TRANSLATION_X);
        KeyCycleOscillator osc_y =
                (mCycleMap == null) ? null : mCycleMap.get(MotionKey.TRANSLATION_Y);

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
            mStartMotionPath.getCenter(p, mInterpolateVariables, mInterpolateData, points, i * 2);

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
     * @param bounds array to fill (should be 2x the number of mPoints
     * @return number of key frames
     */
    void buildBounds(float[] bounds, int pointCount) {
        float mils = 1.0f / (pointCount - 1);
        @SuppressWarnings("unused") SplineSet trans_x =
                (mAttributesMap == null) ? null : mAttributesMap.get(MotionKey.TRANSLATION_X);
        @SuppressWarnings("unused") SplineSet trans_y =
                (mAttributesMap == null) ? null : mAttributesMap.get(MotionKey.TRANSLATION_Y);
        @SuppressWarnings("unused") KeyCycleOscillator osc_x =
                (mCycleMap == null) ? null : mCycleMap.get(MotionKey.TRANSLATION_X);
        @SuppressWarnings("unused") KeyCycleOscillator osc_y =
                (mCycleMap == null) ? null : mCycleMap.get(MotionKey.TRANSLATION_Y);

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

    MotionKeyPosition getPositionKeyframe(int layoutWidth, int layoutHeight, float x, float y) {
        FloatRect start = new FloatRect();
        start.left = mStartMotionPath.mX;
        start.top = mStartMotionPath.mY;
        start.right = start.left + mStartMotionPath.mWidth;
        start.bottom = start.top + mStartMotionPath.mHeight;
        FloatRect end = new FloatRect();
        end.left = mEndMotionPath.mX;
        end.top = mEndMotionPath.mY;
        end.right = end.left + mEndMotionPath.mWidth;
        end.bottom = end.top + mEndMotionPath.mHeight;
        for (MotionKey key : mKeyList) {
            if (key instanceof MotionKeyPosition) {
                if (((MotionKeyPosition) key).intersects(layoutWidth,
                        layoutHeight, start, end, x, y)) {
                    return (MotionKeyPosition) key;
                }
            }
        }
        return null;
    }

    // @TODO: add description
    public int buildKeyFrames(float[] keyFrames, int[] mode, int[] pos) {
        if (keyFrames != null) {
            int count = 0;
            double[] time = mSpline[0].getTimePoints();
            if (mode != null) {
                for (MotionPaths keyFrame : mMotionPaths) {
                    mode[count++] = keyFrame.mMode;
                }
                count = 0;
            }
            if (pos != null) {
                for (MotionPaths keyFrame : mMotionPaths) {
                    pos[count++] = (int) (100 * keyFrame.mPosition);
                }
                count = 0;
            }
            for (int i = 0; i < time.length; i++) {
                mSpline[0].getPos(time[i], mInterpolateData);
                mStartMotionPath.getCenter(time[i],
                        mInterpolateVariables, mInterpolateData, keyFrames, count);
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
                mStartMotionPath.getBounds(mInterpolateVariables,
                        mInterpolateData, keyBounds, count);
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

    // @TODO: add description
    public void buildRect(float p, float[] path, int offset) {
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
        MotionPaths redundant = null;
        for (MotionPaths p : mMotionPaths) {
            if (point.mPosition == p.mPosition) {
                redundant = p;
            }
        }
        if (redundant != null) {
            mMotionPaths.remove(redundant);
        }
        int pos = Collections.binarySearch(mMotionPaths, point);
        if (pos == 0) {
            Utils.loge(TAG, " KeyPath position \"" + point.mPosition + "\" outside of range");
        }
        mMotionPaths.add(-pos - 1, point);
    }

    void addKeys(ArrayList<MotionKey> list) {
        mKeyList.addAll(list);
        if (DEBUG) {
            for (MotionKey key : mKeyList) {
                Utils.log(TAG, " ################ set = " + key.getClass().getSimpleName());
            }
        }
    }

    // @TODO: add description
    public void addKey(MotionKey key) {
        mKeyList.add(key);
        if (DEBUG) {
            Utils.log(TAG, " ################ addKey = " + key.getClass().getSimpleName());
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
        @SuppressWarnings({"unused", "ModifiedButNotUsed"})
        HashSet<String> springAttributes = new HashSet<>();
        // attributes we need to interpolate
        HashSet<String> timeCycleAttributes = new HashSet<>(); // attributes we need to interpolate
        HashSet<String> splineAttributes = new HashSet<>(); // attributes we need to interpolate
        HashSet<String> cycleAttributes = new HashSet<>(); // attributes we need to oscillate
        HashMap<String, Integer> interpolation = new HashMap<>();
        ArrayList<MotionKeyTrigger> triggerList = null;

        setupRelative();
        if (DEBUG) {
            if (mKeyList == null) {
                Utils.log(TAG, ">>>>>>>>>>>>>>> mKeyList==null");

            } else {
                Utils.log(TAG, ">>>>>>>>>>>>>>> mKeyList for " + mView.getName());

            }
        }

        if (mPathMotionArc != UNSET && mStartMotionPath.mPathMotionArc == UNSET) {
            mStartMotionPath.mPathMotionArc = mPathMotionArc;
        }

        mStartPoint.different(mEndPoint, splineAttributes);
        if (DEBUG) {
            HashSet<String> attr = new HashSet<>();
            mStartPoint.different(mEndPoint, attr);
            Utils.log(TAG, ">>>>>>>>>>>>>>> MotionConstrainedPoint found "
                    + Arrays.toString(attr.toArray()));
        }
        if (mKeyList != null) {
            for (MotionKey key : mKeyList) {
                if (key instanceof MotionKeyPosition) {
                    MotionKeyPosition keyPath = (MotionKeyPosition) key;
                    insertKey(new MotionPaths(parentWidth, parentHeight,
                            keyPath, mStartMotionPath, mEndMotionPath));
                    if (keyPath.mCurveFit != UNSET) {
                        mCurveFitType = keyPath.mCurveFit;
                    }
                } else if (key instanceof MotionKeyCycle) {
                    key.getAttributeNames(cycleAttributes);
                } else if (key instanceof MotionKeyTimeCycle) {
                    key.getAttributeNames(timeCycleAttributes);
                } else if (key instanceof MotionKeyTrigger) {
                    if (triggerList == null) {
                        triggerList = new ArrayList<>();
                    }
                    triggerList.add((MotionKeyTrigger) key);
                } else {
                    key.setInterpolation(interpolation);
                    key.getAttributeNames(splineAttributes);
                }
            }
        }

        //--------------------------- trigger support --------------------

        if (triggerList != null) {
            mKeyTriggers = triggerList.toArray(new MotionKeyTrigger[0]);
        }

        //--------------------------- splines support --------------------
        if (!splineAttributes.isEmpty()) {
            mAttributesMap = new HashMap<>();
            for (String attribute : splineAttributes) {
                SplineSet splineSets;
                if (attribute.startsWith("CUSTOM,")) {
                    KeyFrameArray.CustomVar attrList = new KeyFrameArray.CustomVar();
                    String customAttributeName = attribute.split(",")[1];
                    for (MotionKey key : mKeyList) {
                        if (key.mCustom == null) {
                            continue;
                        }
                        CustomVariable customAttribute = key.mCustom.get(customAttributeName);
                        if (customAttribute != null) {
                            attrList.append(key.mFramePosition, customAttribute);
                        }
                    }
                    splineSets = SplineSet.makeCustomSplineSet(attribute, attrList);
                } else {
                    splineSets = SplineSet.makeSpline(attribute, currentTime);
                }
                if (splineSets == null) {
                    continue;
                }
                splineSets.setType(attribute);
                mAttributesMap.put(attribute, splineSets);
            }
            if (mKeyList != null) {
                for (MotionKey key : mKeyList) {
                    if ((key instanceof MotionKeyAttributes)) {
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

                SplineSet splineSets = null;
                if (attribute.startsWith("CUSTOM,")) {
                    KeyFrameArray.CustomVar attrList = new KeyFrameArray.CustomVar();
                    String customAttributeName = attribute.split(",")[1];
                    for (MotionKey key : mKeyList) {
                        if (key.mCustom == null) {
                            continue;
                        }
                        CustomVariable customAttribute = key.mCustom.get(customAttributeName);
                        if (customAttribute != null) {
                            attrList.append(key.mFramePosition, customAttribute);
                        }
                    }
                    splineSets = SplineSet.makeCustomSplineSet(attribute, attrList);
                } else {
                    splineSets = SplineSet.makeSpline(attribute, currentTime);
                }
                if (splineSets == null) {
                    continue;
                }
                splineSets.setType(attribute);
//                mTimeCycleAttributesMap.put(attribute, splineSets);
            }

            if (mKeyList != null) {
                for (MotionKey key : mKeyList) {
                    if (key instanceof MotionKeyTimeCycle) {
                        ((MotionKeyTimeCycle) key).addTimeValues(mTimeCycleAttributesMap);
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
        if (mMotionPaths.size() > 0 && mCurveFitType == MotionKey.UNSET) {
            mCurveFitType = CurveFit.SPLINE;
        }
        for (MotionPaths point : mMotionPaths) {
            points[count++] = point;
        }

        // -----  setup custom attributes which must be in the start and end constraint sets
        int variables = 18;
        HashSet<String> attributeNameSet = new HashSet<>();
        for (String s : mEndMotionPath.mCustomAttributes.keySet()) {
            if (mStartMotionPath.mCustomAttributes.containsKey(s)) {
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
                if (points[j].mCustomAttributes.containsKey(attributeName)) {
                    CustomVariable attribute = points[j].mCustomAttributes.get(attributeName);
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
                @SuppressWarnings("unused") String s =
                        MotionPaths.sNames[mInterpolateVariables[j]] + " [";
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

        // Spline for positions
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
                KeyCycleOscillator cycle = KeyCycleOscillator.makeWidgetCycle(attribute);
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
            for (MotionKey key : mKeyList) {
                if (key instanceof MotionKeyCycle) {
                    ((MotionKeyCycle) key).addCycleValues(mCycleMap);
                }
            }
            for (KeyCycleOscillator cycle : mCycleMap.values()) {
                cycle.setup(distance);
            }
        }

        if (DEBUG) {
            Utils.log(TAG, "Animation of splineAttributes "
                    + Arrays.toString(splineAttributes.toArray()));
            Utils.log(TAG, "Animation of cycle " + Arrays.toString(mCycleMap.keySet().toArray()));
            if (mAttributesMap != null) {
                Utils.log(TAG, " splines = " + Arrays.toString(mAttributesMap.keySet().toArray()));
                for (String s : mAttributesMap.keySet()) {
                    Utils.log(TAG, s + " = " + mAttributesMap.get(s));
                }
            }
            Utils.log(TAG, " ---------------------------------------- ");
        }

        //--------------------------- end cycle support ----------------
    }

    /**
     * Debug string
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

    public void setView(MotionWidget view) {
        mView = view;
    }

    public MotionWidget getView() {
        return mView;
    }

    // @TODO: add description
    public void setStart(MotionWidget mw) {
        mStartMotionPath.mTime = 0;
        mStartMotionPath.mPosition = 0;
        mStartMotionPath.setBounds(mw.getX(), mw.getY(), mw.getWidth(), mw.getHeight());
        mStartMotionPath.applyParameters(mw);
        mStartPoint.setState(mw);
        TypedBundle p = mw.getWidgetFrame().getMotionProperties();
        if (p != null) {
            p.applyDelta(this);
        }
    }

    // @TODO: add description
    public void setEnd(MotionWidget mw) {
        mEndMotionPath.mTime = 1;
        mEndMotionPath.mPosition = 1;
        readView(mEndMotionPath);
        mEndMotionPath.setBounds(mw.getLeft(), mw.getTop(), mw.getWidth(), mw.getHeight());
        mEndMotionPath.applyParameters(mw);
        mEndPoint.setState(mw);
    }

    // @TODO: add description
    public void setStartState(ViewState rect,
            MotionWidget v,
            int rotation,
            int preWidth,
            int preHeight) {
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

            case MotionConstraintSet.ROTATE_PORTRATE_OF_LEFT:
                cx = rect.left + rect.right;
                cy = rect.top + rect.bottom;
                out.left = preHeight - (cy + rect.width()) / 2;
                out.top = (cx - rect.height()) / 2;
                out.right = out.left + rect.width();
                out.bottom = out.top + rect.height();
                break;
            case MotionConstraintSet.ROTATE_PORTRATE_OF_RIGHT:
                cx = rect.left + rect.right;
                cy = rect.top + rect.bottom;
                out.left = (cy - rect.width()) / 2;
                out.top = preWidth - (cx + rect.height()) / 2;
                out.right = out.left + rect.width();
                out.bottom = out.top + rect.height();
                break;
            case MotionConstraintSet.ROTATE_LEFT_OF_PORTRATE:
                cx = rect.left + rect.right;
                cy = rect.bottom + rect.top;
                out.left = preHeight - (cy + rect.width()) / 2;
                out.top = (cx - rect.height()) / 2;
                out.right = out.left + rect.width();
                out.bottom = out.top + rect.height();
                break;
            case MotionConstraintSet.ROTATE_RIGHT_OF_PORTRATE:
                cx = rect.left + rect.right;
                cy = rect.top + rect.bottom;
                out.left = rect.height() / 2 + rect.top - cx / 2;
                out.top = preWidth - (cx + rect.height()) / 2;
                out.right = out.left + rect.width();
                out.bottom = out.top + rect.height();
                break;
        }
    }

    // Todo : Implement  QuantizeMotion scene rotate
    //    void setStartState(Rect cw, ConstraintSet constraintSet,
    //                      int parentWidth, int parentHeight) {
    //        int rotate = constraintSet.mRotate; // for rotated frames
    //        if (rotate != 0) {
    //            rotate(cw, mTempRect, rotate, parentWidth, parentHeight);
    //        }
    //        mStartMotionPath.time = 0;
    //        mStartMotionPath.position = 0;
    //        readView(mStartMotionPath);
    //        mStartMotionPath.setBounds(cw.left, cw.top, cw.width(), cw.height());
    //        ConstraintSet.Constraint constraint = constraintSet.getParameters(mId);
    //        mStartMotionPath.applyParameters(constraint);
    //        mMotionStagger = constraint.motion.mMotionStagger;
    //        mStartPoint.setState(cw, constraintSet, rotate, mId);
    //        mTransformPivotTarget = constraint.transform.transformPivotTarget;
    //        mQuantizeMotionSteps = constraint.motion.mQuantizeMotionSteps;
    //        mQuantizeMotionPhase = constraint.motion.mQuantizeMotionPhase;
    //        mQuantizeMotionInterpolator = getInterpolator(mView.getContext(),
    //                constraint.motion.mQuantizeInterpolatorType,
    //                constraint.motion.mQuantizeInterpolatorString,
    //                constraint.motion.mQuantizeInterpolatorID
    //        );
    //    }

    static final int EASE_IN_OUT = 0;
    static final int EASE_IN = 1;
    static final int EASE_OUT = 2;
    static final int LINEAR = 3;
    static final int BOUNCE = 4;
    static final int OVERSHOOT = 5;
    private static final int SPLINE_STRING = -1;
    @SuppressWarnings("unused") private static final int INTERPOLATOR_REFERENCE_ID = -2;
    @SuppressWarnings("unused") private static final int INTERPOLATOR_UNDEFINED = -3;

    private static DifferentialInterpolator getInterpolator(int type,
            String interpolatorString,
            @SuppressWarnings("unused") int id) {
        switch (type) {
            case SPLINE_STRING:
                final Easing easing = Easing.getInterpolator(interpolatorString);
                return new DifferentialInterpolator() {
                    float mX;

                    @Override
                    public float getInterpolation(float x) {
                        mX = x;
                        return (float) easing.get(x);
                    }

                    @Override
                    public float getVelocity() {
                        return (float) easing.getDiff(mX);
                    }
                };

        }
        return null;
    }

//    void setEndState(Rect cw, ConstraintSet constraintSet, int parentWidth, int parentHeight) {
//        int rotate = constraintSet.mRotate; // for rotated frames
//        if (rotate != 0) {
//            rotate(cw, mTempRect, rotate, parentWidth, parentHeight);
//            cw = mTempRect;
//        }
//        mEndMotionPath.time = 1;
//        mEndMotionPath.position = 1;
//        readView(mEndMotionPath);
//        mEndMotionPath.setBounds(cw.left, cw.top, cw.width(), cw.height());
//        mEndMotionPath.applyParameters(constraintSet.getParameters(mId));
//        mEndPoint.setState(cw, constraintSet, rotate, mId);
//    }

    void setBothStates(MotionWidget v) {
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
//        if ("button".equals(Debug.getName(mView)))
//            if (mKeyTriggers != null) {
//                for (int i = 0; i < mKeyTriggers.length; i++) {
//                    mKeyTriggers[i].conditionallyFire(start ? -100 : 100, mView);
//                }
//            }
    }
    //##############################################################################################
    //$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%
    //$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%
    //$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%
    //$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%$%
    //##############################################################################################

    /**
     * The main driver of interpolation
     *
     * @return do you need to keep animating
     */
    public boolean interpolate(MotionWidget child,
            float globalPosition,
            long time,
            KeyCache keyCache) {
        @SuppressWarnings("unused")  boolean timeAnimation = false;
        float position = getAdjustedPosition(globalPosition, null);
        // This quantize the position into steps e.g. 4 steps = 0-0.25,0.25-0.50 etc
        if (mQuantizeMotionSteps != UNSET) {
            @SuppressWarnings("unused") float pin = position;
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
        // MotionKeyTimeCycle.PathRotate timePathRotate = null;
        if (mAttributesMap != null) {
            for (SplineSet aSpline : mAttributesMap.values()) {
                aSpline.setProperty(child, position);
            }
        }

        //       TODO add KeyTimeCycle
        //        if (mTimeCycleAttributesMap != null) {
        //            for (ViewTimeCycle aSpline : mTimeCycleAttributesMap.values()) {
        //                if (aSpline instanceof ViewTimeCycle.PathRotate) {
        //                    timePathRotate = (ViewTimeCycle.PathRotate) aSpline;
        //                    continue;
        //                }
        //                timeAnimation |= aSpline.setProperty(child, position, time, keyCache);
        //            }
        //        }

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
                        mInterpolateVariables, mInterpolateData, mInterpolateVelocity, null);
            }
            if (mTransformPivotTarget != UNSET) {
                if (mTransformPivotView == null) {
                    MotionWidget layout = (MotionWidget) child.getParent();
                    mTransformPivotView = layout.findViewById(mTransformPivotTarget);
                }
                if (mTransformPivotView != null) {
                    float cy =
                            (mTransformPivotView.getTop() + mTransformPivotView.getBottom()) / 2.0f;
                    float cx =
                            (mTransformPivotView.getLeft() + mTransformPivotView.getRight()) / 2.0f;
                    if (child.getRight() - child.getLeft() > 0
                            && child.getBottom() - child.getTop() > 0) {
                        float px = (cx - child.getLeft());
                        float py = (cy - child.getTop());
                        child.setPivotX(px);
                        child.setPivotY(py);
                    }
                }
            }

            //       TODO add support for path rotate
            //            if (mAttributesMap != null) {
            //                for (SplineSet aSpline : mAttributesMap.values()) {
            //                    if (aSpline instanceof ViewSpline.PathRotate
            //                          && mInterpolateVelocity.length > 1)
            //                        ((ViewSpline.PathRotate) aSpline).setPathRotate(child,
            //                        position, mInterpolateVelocity[0], mInterpolateVelocity[1]);
            //                }
            //
            //            }
            //            if (timePathRotate != null) {
            //                timeAnimation |= timePathRotate.setPathRotate(child, keyCache,
            //                  position, time, mInterpolateVelocity[0], mInterpolateVelocity[1]);
            //            }

            for (int i = 1; i < mSpline.length; i++) {
                CurveFit spline = mSpline[i];
                spline.getPos(position, mValuesBuff);
                //interpolated here
                mStartMotionPath.mCustomAttributes
                        .get(mAttributeNames[i - 1])
                        .setInterpolatedValue(child, mValuesBuff);
            }
            if (mStartPoint.mVisibilityMode == MotionWidget.VISIBILITY_MODE_NORMAL) {
                if (position <= 0.0f) {
                    child.setVisibility(mStartPoint.mVisibility);
                } else if (position >= 1.0f) {
                    child.setVisibility(mEndPoint.mVisibility);
                } else if (mEndPoint.mVisibility != mStartPoint.mVisibility) {
                    child.setVisibility(MotionWidget.VISIBLE);
                }
            }

            if (mKeyTriggers != null) {
                for (int i = 0; i < mKeyTriggers.length; i++) {
                    mKeyTriggers[i].conditionallyFire(position, child);
                }
            }
        } else {
            // do the interpolation

            float float_l =
                    (mStartMotionPath.mX + (mEndMotionPath.mX - mStartMotionPath.mX) * position);
            float float_t =
                    (mStartMotionPath.mY + (mEndMotionPath.mY - mStartMotionPath.mY) * position);
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
            // widget is responsible to call measure
            child.layout(l, t, r, b);
        }

        // TODO add pathRotate KeyCycles
        if (mCycleMap != null) {
            for (KeyCycleOscillator osc : mCycleMap.values()) {
                if (osc instanceof KeyCycleOscillator.PathRotateSet) {
                    ((KeyCycleOscillator.PathRotateSet) osc).setPathRotate(child, position,
                            mInterpolateVelocity[0], mInterpolateVelocity[1]);
                } else {
                    osc.setProperty(child, position);
                }
            }
        }
        //   When we support TimeCycle return true if repaint is needed
        //        return timeAnimation;
        return false;
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
    public void getDpDt(float position, float locationX, float locationY, float[] mAnchorDpDt) {
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
            Utils.log(TAG, " position= " + position
                    + " location= " + locationX + " , " + locationY);
        }
        position = getAdjustedPosition(position, mVelocity);

        SplineSet trans_x =
                (mAttributesMap == null) ? null : mAttributesMap.get(MotionKey.TRANSLATION_X);
        SplineSet trans_y =
                (mAttributesMap == null) ? null : mAttributesMap.get(MotionKey.TRANSLATION_Y);
        SplineSet rotation =
                (mAttributesMap == null) ? null : mAttributesMap.get(MotionKey.ROTATION);
        SplineSet scale_x = (mAttributesMap == null) ? null : mAttributesMap.get(MotionKey.SCALE_X);
        SplineSet scale_y = (mAttributesMap == null) ? null : mAttributesMap.get(MotionKey.SCALE_Y);

        KeyCycleOscillator osc_x =
                (mCycleMap == null) ? null : mCycleMap.get(MotionKey.TRANSLATION_X);
        KeyCycleOscillator osc_y =
                (mCycleMap == null) ? null : mCycleMap.get(MotionKey.TRANSLATION_Y);
        KeyCycleOscillator osc_r = (mCycleMap == null) ? null : mCycleMap.get(MotionKey.ROTATION);
        KeyCycleOscillator osc_sx = (mCycleMap == null) ? null : mCycleMap.get(MotionKey.SCALE_X);
        KeyCycleOscillator osc_sy = (mCycleMap == null) ? null : mCycleMap.get(MotionKey.SCALE_Y);

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

    // @TODO: add description
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

        return mView.getName();
    }

    void positionKeyframe(MotionWidget view,
            MotionKeyPosition key,
            float x,
            float y,
            String[] attribute,
            float[] value) {
        FloatRect start = new FloatRect();
        start.left = mStartMotionPath.mX;
        start.top = mStartMotionPath.mY;
        start.right = start.left + mStartMotionPath.mWidth;
        start.bottom = start.top + mStartMotionPath.mHeight;
        FloatRect end = new FloatRect();
        end.left = mEndMotionPath.mX;
        end.top = mEndMotionPath.mY;
        end.right = end.left + mEndMotionPath.mWidth;
        end.bottom = end.top + mEndMotionPath.mHeight;
        key.positionAttributes(view, start, end, x, y, attribute, value);
    }

    /**
     * Get the keyFrames for the view controlled by this MotionController
     *
     * @param type is position(0-100) + 1000
     *             * mType(1=Attributes, 2=Position, 3=TimeCycle 4=Cycle 5=Trigger
     * @param pos  the x&y position of the keyFrame along the path
     * @return Number of keyFrames found
     */
    public int getKeyFramePositions(int[] type, float[] pos) {
        int i = 0;
        int count = 0;
        for (MotionKey key : mKeyList) {
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
        for (MotionKey key : mKeyList) {
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
            if (key instanceof MotionKeyPosition) {
                MotionKeyPosition kp = (MotionKeyPosition) key;
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

    @Override
    public boolean setValue(int id, int value) {
        switch (id) {
            case TypedValues.PositionType.TYPE_PATH_MOTION_ARC:
                setPathMotionArc(value);
                return true;
            case TypedValues.MotionType.TYPE_QUANTIZE_MOTIONSTEPS:
                mQuantizeMotionSteps = value;
                return true;
            case TypedValues.TransitionType.TYPE_AUTO_TRANSITION:
                // TODO add support for auto transitions mAutoTransition = value;
                return true;
        }
        return false;
    }

    @Override
    public boolean setValue(int id, float value) {
        if (MotionType.TYPE_QUANTIZE_MOTION_PHASE == id) {
            mQuantizeMotionPhase = value;
            return true;
        }
        if (MotionType.TYPE_STAGGER == id) {
            mMotionStagger = value;
            return true;
        }
        return false;
    }

    @Override
    public boolean setValue(int id, String value) {
        if (TransitionType.TYPE_INTERPOLATOR == id
                || MotionType.TYPE_QUANTIZE_INTERPOLATOR_TYPE == id) {
            mQuantizeMotionInterpolator = getInterpolator(SPLINE_STRING, value, 0);
            return true;
        }
        if (MotionType.TYPE_ANIMATE_RELATIVE_TO == id) {
            mStartMotionPath.mAnimateRelativeTo = value;
            return true;
        }
        return false;
    }

    @Override
    public boolean setValue(int id, boolean value) {
        return false;
    }

    @Override
    public int getId(String name) {
        return 0;
    }

    /**
     * Set stagger scale
     */
    public void setStaggerScale(float staggerScale) {
        mStaggerScale = staggerScale;
    }

    /**
     * set the offset used in calculating stagger launches
     *
     * @param staggerOffset fraction of progress before this controller runs
     */
    public void setStaggerOffset(float staggerOffset) {
        mStaggerOffset = staggerOffset;
    }

    /**
     * The values set in
     * motion: {
     * stagger: '2'
     * }
     *
     * @return value from motion: { stagger: ? } or NaN if not set
     */
    public float getMotionStagger() {
        return mMotionStagger;
    }

    public void setIdString(String stringId) {
        mId = stringId;
        mStartMotionPath.mId = mId;
    }
}
