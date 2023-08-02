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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.motion.utils.ViewSpline;
import androidx.constraintlayout.widget.ConstraintAttribute;
import androidx.constraintlayout.widget.R;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/**
 * Defines container for a key frame of for storing KeyAttributes.
 * KeyAttributes change post layout values of a view.
 *
 *
 */

public class KeyTrigger extends Key {
    public static final String VIEW_TRANSITION_ON_CROSS = "viewTransitionOnCross";
    public static final String VIEW_TRANSITION_ON_POSITIVE_CROSS = "viewTransitionOnPositiveCross";
    public static final String VIEW_TRANSITION_ON_NEGATIVE_CROSS = "viewTransitionOnNegativeCross";
    public static final String POST_LAYOUT = "postLayout";
    public static final String TRIGGER_SLACK = "triggerSlack";
    public static final String TRIGGER_COLLISION_VIEW = "triggerCollisionView";
    public static final String TRIGGER_COLLISION_ID = "triggerCollisionId";
    public static final String TRIGGER_ID = "triggerID";
    public static final String POSITIVE_CROSS = "positiveCross";
    public static final String NEGATIVE_CROSS = "negativeCross";
    public static final String TRIGGER_RECEIVER = "triggerReceiver";
    public static final String CROSS = "CROSS";
    public static final int KEY_TYPE = 5;
    static final String NAME = "KeyTrigger";
    private static final String TAG = "KeyTrigger";
    float mTriggerSlack = .1f;
    int mViewTransitionOnNegativeCross = UNSET;
    int mViewTransitionOnPositiveCross = UNSET;
    int mViewTransitionOnCross = UNSET;
    RectF mCollisionRect = new RectF();
    RectF mTargetRect = new RectF();
    HashMap<String, Method> mMethodHashMap = new HashMap<>();
    private int mCurveFit = -1;
    private String mCross = null;
    private int mTriggerReceiver = UNSET;
    private String mNegativeCross = null;
    private String mPositiveCross = null;
    private int mTriggerID = UNSET;
    private int mTriggerCollisionId = UNSET;
    private View mTriggerCollisionView = null;
    private boolean mFireCrossReset = true;
    private boolean mFireNegativeReset = true;
    private boolean mFirePositiveReset = true;
    private float mFireThreshold = Float.NaN;
    private float mFireLastPos;
    private boolean mPostLayout = false;

    {
        mType = KEY_TYPE;
        mCustomConstraints = new HashMap<>();
    }

    @Override
    public void load(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyTrigger);
        Loader.read(this, a, context);
    }

    /**
     * Gets the curve fit type this drives the interpolation
     *
     * @return
     */
    int getCurveFit() {
        return mCurveFit;
    }

    @Override
    public void getAttributeNames(HashSet<String> attributes) {
    }

    @Override
    public void addValues(HashMap<String, ViewSpline> splines) {
    }

    @Override
    public void setValue(String tag, Object value) {
        switch (tag) {
            case CROSS:
                mCross = value.toString();
                break;
            case TRIGGER_RECEIVER:
                mTriggerReceiver = toInt(value);
                break;
            case NEGATIVE_CROSS:
                mNegativeCross = value.toString();
                break;
            case POSITIVE_CROSS:
                mPositiveCross = value.toString();
                break;
            case TRIGGER_ID:
                mTriggerID = toInt(value);
                break;
            case TRIGGER_COLLISION_ID:
                mTriggerCollisionId = toInt(value);
                break;
            case TRIGGER_COLLISION_VIEW:
                mTriggerCollisionView = (View) value;
                break;
            case TRIGGER_SLACK:
                mTriggerSlack = toFloat(value);
                break;
            case POST_LAYOUT:
                mPostLayout = toBoolean(value);
                break;
            case VIEW_TRANSITION_ON_NEGATIVE_CROSS:
                mViewTransitionOnNegativeCross = toInt(value);
                break;
            case VIEW_TRANSITION_ON_POSITIVE_CROSS:
                mViewTransitionOnPositiveCross = toInt(value);
                break;
            case VIEW_TRANSITION_ON_CROSS:
                mViewTransitionOnCross = toInt(value);
                break;

        }
    }

    private void setUpRect(RectF rect, View child, boolean postLayout) {
        rect.top = child.getTop();
        rect.bottom = child.getBottom();
        rect.left = child.getLeft();
        rect.right = child.getRight();
        if (postLayout) {
            child.getMatrix().mapRect(rect);
        }
    }

    /**
     * This fires the keyTriggers associated with this view at that position
     *
     * @param pos   the progress
     * @param child the view
     */
    public void conditionallyFire(float pos, View child) {
        boolean fireCross = false;
        boolean fireNegative = false;
        boolean firePositive = false;

        if (mTriggerCollisionId != UNSET) {
            if (mTriggerCollisionView == null) {
                mTriggerCollisionView =
                        ((ViewGroup) child.getParent()).findViewById(mTriggerCollisionId);
            }

            setUpRect(mCollisionRect, mTriggerCollisionView, mPostLayout);
            setUpRect(mTargetRect, child, mPostLayout);
            boolean in = mCollisionRect.intersect(mTargetRect);
            // TODO scale by mTriggerSlack
            if (in) {
                if (mFireCrossReset) {
                    fireCross = true;
                    mFireCrossReset = false;
                }
                if (mFirePositiveReset) {
                    firePositive = true;
                    mFirePositiveReset = false;
                }
                mFireNegativeReset = true;
            } else {
                if (!mFireCrossReset) {
                    fireCross = true;
                    mFireCrossReset = true;
                }
                if (mFireNegativeReset) {
                    fireNegative = true;
                    mFireNegativeReset = false;
                }
                mFirePositiveReset = true;
            }

        } else {

            // Check for crossing
            if (mFireCrossReset) {

                float offset = pos - mFireThreshold;
                float lastOffset = mFireLastPos - mFireThreshold;

                if (offset * lastOffset < 0) { // just crossed the threshold
                    fireCross = true;
                    mFireCrossReset = false;
                }
            } else {
                if (Math.abs(pos - mFireThreshold) > mTriggerSlack) {
                    mFireCrossReset = true;
                }
            }

            // Check for negative crossing
            if (mFireNegativeReset) {
                float offset = pos - mFireThreshold;
                float lastOffset = mFireLastPos - mFireThreshold;
                if (offset * lastOffset < 0 && offset < 0) { // just crossed the threshold
                    fireNegative = true;
                    mFireNegativeReset = false;
                }
            } else {
                if (Math.abs(pos - mFireThreshold) > mTriggerSlack) {
                    mFireNegativeReset = true;
                }
            }
            // Check for positive crossing
            if (mFirePositiveReset) {
                float offset = pos - mFireThreshold;
                float lastOffset = mFireLastPos - mFireThreshold;
                if (offset * lastOffset < 0 && offset > 0) { // just crossed the threshold
                    firePositive = true;
                    mFirePositiveReset = false;
                }
            } else {
                if (Math.abs(pos - mFireThreshold) > mTriggerSlack) {
                    mFirePositiveReset = true;
                }
            }
        }
        mFireLastPos = pos;

        if (fireNegative || fireCross || firePositive) {
            ((MotionLayout) child.getParent()).fireTrigger(mTriggerID, firePositive, pos);
        }
        View call = (mTriggerReceiver == UNSET) ? child :
                ((MotionLayout) child.getParent()).findViewById(mTriggerReceiver);

        if (fireNegative) {
            if (mNegativeCross != null) {
                fire(mNegativeCross, call);
            }
            if (mViewTransitionOnNegativeCross != UNSET) {
                ((MotionLayout) child.getParent()).viewTransition(mViewTransitionOnNegativeCross,
                        call);
            }
        }
        if (firePositive) {
            if (mPositiveCross != null) {
                fire(mPositiveCross, call);
            }
            if (mViewTransitionOnPositiveCross != UNSET) {
                ((MotionLayout) child.getParent()).viewTransition(mViewTransitionOnPositiveCross,
                        call);
            }
        }
        if (fireCross) {
            if (mCross != null) {
                fire(mCross, call);
            }
            if (mViewTransitionOnCross != UNSET) {
                ((MotionLayout) child.getParent()).viewTransition(mViewTransitionOnCross, call);
            }
        }

    }

    private void fire(String str, View call) {
        if (str == null) {
            return;
        }
        if (str.startsWith(".")) {
            fireCustom(str, call);
            return;
        }
        Method method = null;
        if (mMethodHashMap.containsKey(str)) {
            method = mMethodHashMap.get(str);
            if (method == null) { // we looked up and did not find
                return;
            }
        }
        if (method == null) {
            try {
                method = call.getClass().getMethod(str);
                mMethodHashMap.put(str, method);
            } catch (NoSuchMethodException e) {
                mMethodHashMap.put(str, null); // record that we could not get this method
                Log.e(TAG, "Could not find method \"" + str + "\"" + "on class "
                        + call.getClass().getSimpleName() + " " + Debug.getName(call));
                return;
            }
        }
        try {
            method.invoke(call);
        } catch (Exception e) {
            Log.e(TAG, "Exception in call \"" + mCross + "\"" + "on class "
                    + call.getClass().getSimpleName() + " " + Debug.getName(call));
        }
    }

    private void fireCustom(String str, View view) {
        boolean callAll = str.length() == 1;
        if (!callAll) {
            str = str.substring(1).toLowerCase(Locale.ROOT);
        }
        for (String name : mCustomConstraints.keySet()) {
            String lowerCase = name.toLowerCase(Locale.ROOT);
            if (callAll || lowerCase.matches(str)) {
                ConstraintAttribute custom = mCustomConstraints.get(name);
                if (custom != null) {
                    custom.applyCustom(view);
                }
            }
        }
    }

    /**
     * Copy the key
     *
     * @param src to be copied
     * @return self
     */
    @Override
    public Key copy(Key src) {
        super.copy(src);
        KeyTrigger k = (KeyTrigger) src;
        mCurveFit = k.mCurveFit;
        mCross = k.mCross;
        mTriggerReceiver = k.mTriggerReceiver;
        mNegativeCross = k.mNegativeCross;
        mPositiveCross = k.mPositiveCross;
        mTriggerID = k.mTriggerID;
        mTriggerCollisionId = k.mTriggerCollisionId;
        mTriggerCollisionView = k.mTriggerCollisionView;
        mTriggerSlack = k.mTriggerSlack;
        mFireCrossReset = k.mFireCrossReset;
        mFireNegativeReset = k.mFireNegativeReset;
        mFirePositiveReset = k.mFirePositiveReset;
        mFireThreshold = k.mFireThreshold;
        mFireLastPos = k.mFireLastPos;
        mPostLayout = k.mPostLayout;
        mCollisionRect = k.mCollisionRect;
        mTargetRect = k.mTargetRect;
        mMethodHashMap = k.mMethodHashMap;
        return this;
    }

    /**
     * Clone this KeyAttributes
     *
     * @return
     */
    @Override
    public Key clone() {
        return new KeyTrigger().copy(this);
    }

    private static class Loader {
        private static final int NEGATIVE_CROSS = 1;
        private static final int POSITIVE_CROSS = 2;
        private static final int CROSS = 4;
        private static final int TRIGGER_SLACK = 5;
        private static final int TRIGGER_ID = 6;
        private static final int TARGET_ID = 7;
        private static final int FRAME_POS = 8;
        private static final int COLLISION = 9;
        private static final int POST_LAYOUT = 10;
        private static final int TRIGGER_RECEIVER = 11;
        private static final int VT_CROSS = 12;
        private static final int VT_NEGATIVE_CROSS = 13;
        private static final int VT_POSITIVE_CROSS = 14;

        private static SparseIntArray sAttrMap = new SparseIntArray();

        static {
            sAttrMap.append(R.styleable.KeyTrigger_framePosition, FRAME_POS);
            sAttrMap.append(R.styleable.KeyTrigger_onCross, CROSS);
            sAttrMap.append(R.styleable.KeyTrigger_onNegativeCross, NEGATIVE_CROSS);
            sAttrMap.append(R.styleable.KeyTrigger_onPositiveCross, POSITIVE_CROSS);
            sAttrMap.append(R.styleable.KeyTrigger_motionTarget, TARGET_ID);
            sAttrMap.append(R.styleable.KeyTrigger_triggerId, TRIGGER_ID);
            sAttrMap.append(R.styleable.KeyTrigger_triggerSlack, TRIGGER_SLACK);
            sAttrMap.append(R.styleable.KeyTrigger_motion_triggerOnCollision, COLLISION);
            sAttrMap.append(R.styleable.KeyTrigger_motion_postLayoutCollision, POST_LAYOUT);
            sAttrMap.append(R.styleable.KeyTrigger_triggerReceiver, TRIGGER_RECEIVER);
            sAttrMap.append(R.styleable.KeyTrigger_viewTransitionOnCross, VT_CROSS);
            sAttrMap.append(R.styleable.KeyTrigger_viewTransitionOnNegativeCross,
                    VT_NEGATIVE_CROSS);
            sAttrMap.append(R.styleable.KeyTrigger_viewTransitionOnPositiveCross,
                    VT_POSITIVE_CROSS);
        }

        public static void read(KeyTrigger c, TypedArray a,
                @SuppressWarnings("unused") Context context) {
            final int n = a.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);
                switch (sAttrMap.get(attr)) {
                    case FRAME_POS:
                        c.mFramePosition = a.getInteger(attr, c.mFramePosition);
                        c.mFireThreshold = (c.mFramePosition + .5f) / 100f;
                        break;
                    case TARGET_ID:
                        if (MotionLayout.IS_IN_EDIT_MODE) {
                            c.mTargetId = a.getResourceId(attr, c.mTargetId);
                            if (c.mTargetId == -1) {
                                c.mTargetString = a.getString(attr);
                            }
                        } else {
                            if (a.peekValue(attr).type == TypedValue.TYPE_STRING) {
                                c.mTargetString = a.getString(attr);
                            } else {
                                c.mTargetId = a.getResourceId(attr, c.mTargetId);
                            }
                        }
                        break;
                    case NEGATIVE_CROSS:
                        c.mNegativeCross = a.getString(attr);
                        break;
                    case POSITIVE_CROSS:
                        c.mPositiveCross = a.getString(attr);
                        break;
                    case CROSS:
                        c.mCross = a.getString(attr);
                        break;
                    case TRIGGER_SLACK:
                        c.mTriggerSlack = a.getFloat(attr, c.mTriggerSlack);
                        break;
                    case TRIGGER_ID:
                        c.mTriggerID = a.getResourceId(attr, c.mTriggerID);
                        break;
                    case COLLISION:
                        c.mTriggerCollisionId = a.getResourceId(attr, c.mTriggerCollisionId);
                        break;
                    case POST_LAYOUT:
                        c.mPostLayout = a.getBoolean(attr, c.mPostLayout);
                        break;
                    case TRIGGER_RECEIVER:
                        c.mTriggerReceiver = a.getResourceId(attr, c.mTriggerReceiver);
                        break;
                    case VT_NEGATIVE_CROSS:
                        c.mViewTransitionOnNegativeCross = a.getResourceId(attr,
                                c.mViewTransitionOnNegativeCross);
                        break;
                    case VT_POSITIVE_CROSS:
                        c.mViewTransitionOnPositiveCross = a.getResourceId(attr,
                                c.mViewTransitionOnPositiveCross);
                        break;
                    case VT_CROSS:
                        c.mViewTransitionOnCross = a.getResourceId(attr, c.mViewTransitionOnCross);
                        break;
                    default:
                        Log.e(NAME, "unused attribute 0x" + Integer.toHexString(attr)
                                + "   " + sAttrMap.get(attr));
                        break;
                }
            }
        }
    }
}
