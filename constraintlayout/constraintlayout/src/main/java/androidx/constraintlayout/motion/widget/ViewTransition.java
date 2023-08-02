/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import androidx.constraintlayout.core.motion.utils.Easing;
import androidx.constraintlayout.core.motion.utils.KeyCache;
import androidx.constraintlayout.widget.ConstraintAttribute;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Provides a support for <ViewTransition> tag
 * it Parses tag
 * it implement the transition
 * it will update ConstraintSet or sets
 * For asynchronous it will create and drive a MotionController.
 */
public class ViewTransition {
    private static final String TAG = "ViewTransition";
    ConstraintSet mSet;
    public static final String VIEW_TRANSITION_TAG = "ViewTransition";
    public static final String KEY_FRAME_SET_TAG = "KeyFrameSet";
    public static final String CONSTRAINT_OVERRIDE = "ConstraintOverride";
    public static final String CUSTOM_ATTRIBUTE = "CustomAttribute";
    public static final String CUSTOM_METHOD = "CustomMethod";

    private static final int UNSET = -1;
    private int mId;
    // Transition can be up or down of manually fired
    public static final int ONSTATE_ACTION_DOWN = 1;
    public static final int ONSTATE_ACTION_UP = 2;
    public static final int ONSTATE_ACTION_DOWN_UP = 3;
    public static final int ONSTATE_SHARED_VALUE_SET = 4;
    public static final int ONSTATE_SHARED_VALUE_UNSET = 5;

    private int mOnStateTransition = UNSET;
    private boolean mDisabled = false;
    private int mPathMotionArc = 0;
    int mViewTransitionMode;
    static final int VIEWTRANSITIONMODE_CURRENTSTATE = 0;
    static final int VIEWTRANSITIONMODE_ALLSTATES = 1;
    static final int VIEWTRANSITIONMODE_NOSTATE = 2;
    KeyFrames mKeyFrames;
    ConstraintSet.Constraint mConstraintDelta;
    private int mDuration = UNSET;
    private int mUpDuration = UNSET;

    private int mTargetId;
    private String mTargetString;

    // interpolator code
    private static final int SPLINE_STRING = -1;
    private static final int INTERPOLATOR_REFERENCE_ID = -2;
    private int mDefaultInterpolator = 0;
    private String mDefaultInterpolatorString = null;
    private int mDefaultInterpolatorID = -1;
    static final int EASE_IN_OUT = 0;
    static final int EASE_IN = 1;
    static final int EASE_OUT = 2;
    static final int LINEAR = 3;
    static final int BOUNCE = 4;
    static final int OVERSHOOT = 5;
    static final int ANTICIPATE = 6;

    Context mContext;
    private int mSetsTag = UNSET;
    private int mClearsTag = UNSET;
    private int mIfTagSet = UNSET;
    private int mIfTagNotSet = UNSET;

    // shared value management. mSharedValueId is the key we are watching,
    // mSharedValueCurrent the current value for that key, and mSharedValueTarget
    // is the target we are waiting for to trigger.
    private int mSharedValueTarget = UNSET;
    private int mSharedValueID = UNSET;
    private int mSharedValueCurrent = UNSET;

    public int getSharedValueCurrent() {
        return mSharedValueCurrent;
    }

    public void setSharedValueCurrent(int sharedValueCurrent) {
        this.mSharedValueCurrent = sharedValueCurrent;
    }

    /**
     * Gets the type of transition to listen to.
     *
     * @return ONSTATE_TRANSITION_*
     */
    public int getStateTransition() {
        return mOnStateTransition;
    }

    /**
     * Sets the type of transition to listen to.
     *
     * @param stateTransition
     */
    public void setStateTransition(int stateTransition) {
        this.mOnStateTransition = stateTransition;
    }

    /**
     * Gets the SharedValue it will be listening for.
     *
     * @return
     */
    public int getSharedValue() {
        return mSharedValueTarget;
    }

    /**
     * sets the SharedValue it will be listening for.
     */
    public void setSharedValue(int sharedValue) {
        this.mSharedValueTarget = sharedValue;
    }

    /**
     * Gets the ID of the SharedValue it will be listening for.
     *
     * @return the id of the shared value
     */
    public int getSharedValueID() {
        return mSharedValueID;
    }

    /**
     * sets the ID of the SharedValue it will be listening for.
     */
    public void setSharedValueID(int sharedValueID) {
        this.mSharedValueID = sharedValueID;
    }

    /**
     * debug string for a ViewTransition
     * @return
     */
    @Override
    public String toString() {
        return "ViewTransition(" + Debug.getName(mContext, mId) + ")";
    }

    Interpolator getInterpolator(Context context) {
        switch (mDefaultInterpolator) {
            case SPLINE_STRING:
                final Easing easing = Easing.getInterpolator(mDefaultInterpolatorString);
                return new Interpolator() {
                    @Override
                    public float getInterpolation(float v) {
                        return (float) easing.get(v);
                    }
                };
            case INTERPOLATOR_REFERENCE_ID:
                return AnimationUtils.loadInterpolator(context,
                        mDefaultInterpolatorID);
            case EASE_IN_OUT:
                return new AccelerateDecelerateInterpolator();
            case EASE_IN:
                return new AccelerateInterpolator();
            case EASE_OUT:
                return new DecelerateInterpolator();
            case LINEAR:
                return null;
            case ANTICIPATE:
                return new AnticipateInterpolator();
            case OVERSHOOT:
                return new OvershootInterpolator();
            case BOUNCE:
                return new BounceInterpolator();
        }
        return null;
    }

    ViewTransition(Context context, XmlPullParser parser) {
        mContext = context;
        try {
            for (int eventType = parser.getEventType();
                    eventType != XmlResourceParser.END_DOCUMENT;
                    eventType = parser.next()) {
                switch (eventType) {
                    case XmlResourceParser.START_DOCUMENT:
                    case XmlResourceParser.TEXT:
                        break;
                    case XmlResourceParser.START_TAG:
                        String tagName = parser.getName();
                        switch (tagName) {
                            case VIEW_TRANSITION_TAG:
                                parseViewTransitionTags(context, parser);
                                break;
                            case KEY_FRAME_SET_TAG:
                                mKeyFrames = new KeyFrames(context, parser);
                                break;
                            case CONSTRAINT_OVERRIDE:
                                mConstraintDelta = ConstraintSet.buildDelta(context, parser);
                                break;
                            case CUSTOM_ATTRIBUTE:
                            case CUSTOM_METHOD:
                                ConstraintAttribute.parse(context, parser,
                                        mConstraintDelta.mCustomConstraints);
                                break;
                            default:
                                Log.e(TAG, Debug.getLoc() + " unknown tag " + tagName);
                                Log.e(TAG, ".xml:" + parser.getLineNumber());
                        }

                        break;
                    case XmlResourceParser.END_TAG:
                        if (VIEW_TRANSITION_TAG.equals(parser.getName())) {
                            return;
                        }
                        break;
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Error parsing XML resource", e);
        } catch (IOException e) {
            Log.e(TAG, "Error parsing XML resource", e);
        }
    }

    private void parseViewTransitionTags(Context context, XmlPullParser parser) {
        AttributeSet attrs = Xml.asAttributeSet(parser);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewTransition);
        final int count = a.getIndexCount();
        for (int i = 0; i < count; i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.ViewTransition_android_id) {
                mId = a.getResourceId(attr, mId);
            } else if (attr == R.styleable.ViewTransition_motionTarget) {
                if (MotionLayout.IS_IN_EDIT_MODE) {
                    mTargetId = a.getResourceId(attr, mTargetId);
                    if (mTargetId == -1) {
                        mTargetString = a.getString(attr);
                    }
                } else {
                    if (a.peekValue(attr).type == TypedValue.TYPE_STRING) {
                        mTargetString = a.getString(attr);
                    } else {
                        mTargetId = a.getResourceId(attr, mTargetId);
                    }
                }
            } else if (attr == R.styleable.ViewTransition_onStateTransition) {
                mOnStateTransition = a.getInt(attr, mOnStateTransition);
            } else if (attr == R.styleable.ViewTransition_transitionDisable) {
                mDisabled = a.getBoolean(attr, mDisabled);
            } else if (attr == R.styleable.ViewTransition_pathMotionArc) {
                mPathMotionArc = a.getInt(attr, mPathMotionArc);
            } else if (attr == R.styleable.ViewTransition_duration) {
                mDuration = a.getInt(attr, mDuration);
            } else if (attr == R.styleable.ViewTransition_upDuration) {
                mUpDuration = a.getInt(attr, mUpDuration);
            } else if (attr == R.styleable.ViewTransition_viewTransitionMode) {
                mViewTransitionMode = a.getInt(attr, mViewTransitionMode);
            } else if (attr == R.styleable.ViewTransition_motionInterpolator) {
                TypedValue type = a.peekValue(attr);
                if (type.type == TypedValue.TYPE_REFERENCE) {
                    mDefaultInterpolatorID = a.getResourceId(attr, -1);
                    if (mDefaultInterpolatorID != UNSET) {
                        mDefaultInterpolator = INTERPOLATOR_REFERENCE_ID;
                    }
                } else if (type.type == TypedValue.TYPE_STRING) {
                    mDefaultInterpolatorString = a.getString(attr);
                    if (mDefaultInterpolatorString != null
                            && mDefaultInterpolatorString.indexOf("/") > 0) {
                        mDefaultInterpolatorID = a.getResourceId(attr, UNSET);
                        mDefaultInterpolator = INTERPOLATOR_REFERENCE_ID;
                    } else {
                        mDefaultInterpolator = SPLINE_STRING;
                    }
                } else {
                    mDefaultInterpolator = a.getInteger(attr, mDefaultInterpolator);
                }
            } else if (attr == R.styleable.ViewTransition_setsTag) {
                mSetsTag = a.getResourceId(attr, mSetsTag);
            } else if (attr == R.styleable.ViewTransition_clearsTag) {
                mClearsTag = a.getResourceId(attr, mClearsTag);
            } else if (attr == R.styleable.ViewTransition_ifTagSet) {
                mIfTagSet = a.getResourceId(attr, mIfTagSet);
            } else if (attr == R.styleable.ViewTransition_ifTagNotSet) {
                mIfTagNotSet = a.getResourceId(attr, mIfTagNotSet);
            } else if (attr == R.styleable.ViewTransition_SharedValueId) {
                mSharedValueID = a.getResourceId(attr, mSharedValueID);
            } else if (attr == R.styleable.ViewTransition_SharedValue) {
                mSharedValueTarget = a.getInteger(attr, mSharedValueTarget);
            }
        }
        a.recycle();
    }

    void applyIndependentTransition(ViewTransitionController controller,
                                    MotionLayout motionLayout,
                                    View view) {
        MotionController motionController = new MotionController(view);
        motionController.setBothStates(view);
        mKeyFrames.addAllFrames(motionController);
        motionController.setup(motionLayout.getWidth(), motionLayout.getHeight(),
                mDuration, System.nanoTime());
        new Animate(controller, motionController,
                mDuration, mUpDuration, mOnStateTransition,
                getInterpolator(motionLayout.getContext()), mSetsTag, mClearsTag);
    }

    static class Animate {
        private final int mSetsTag;
        private final int mClearsTag;
        long mStart;
        MotionController mMC;
        int mDuration;
        int mUpDuration;
        KeyCache mCache = new KeyCache();
        ViewTransitionController mVtController;
        Interpolator mInterpolator;
        boolean mReverse = false;
        float mPosition;
        float mDpositionDt;
        long mLastRender;
        Rect mTempRec = new Rect();
        boolean mHoldAt100 = false;

        Animate(ViewTransitionController controller,
                MotionController motionController,
                int duration, int upDuration, int mode,
                Interpolator interpolator, int setTag, int clearTag) {
            mVtController = controller;
            mMC = motionController;
            mDuration = duration;
            mUpDuration = upDuration;
            mStart = System.nanoTime();
            mLastRender = mStart;
            mVtController.addAnimation(this);
            mInterpolator = interpolator;
            mSetsTag = setTag;
            mClearsTag = clearTag;
            if (mode == ONSTATE_ACTION_DOWN_UP) {
                mHoldAt100 = true;
            }
            mDpositionDt = (duration == 0) ? Float.MAX_VALUE : 1f / duration;
            mutate();
        }

        void reverse(boolean dir) {
            mReverse = dir;
            if (mReverse && mUpDuration != UNSET) {
                mDpositionDt = (mUpDuration == 0) ? Float.MAX_VALUE : 1f / mUpDuration;
            }
            mVtController.invalidate();
            mLastRender = System.nanoTime();
        }

        void mutate() {
            if (mReverse) {
                mutateReverse();
            } else {
                mutateForward();
            }
        }

        void mutateReverse() {
            long current = System.nanoTime();
            long elapse = current - mLastRender;
            mLastRender = current;

            mPosition -= ((float) (elapse * 1E-6)) * mDpositionDt;
            if (mPosition < 0.0f) {
                mPosition = 0.0f;
            }

            float ipos = (mInterpolator == null) ? mPosition
                    : mInterpolator.getInterpolation(mPosition);
            boolean repaint = mMC.interpolate(mMC.mView, ipos, current, mCache);

            if (mPosition <= 0) {
                if (mSetsTag != UNSET) {
                    mMC.getView().setTag(mSetsTag, System.nanoTime());
                }
                if (mClearsTag != UNSET) {
                    mMC.getView().setTag(mClearsTag, null);
                }
                mVtController.removeAnimation(this);
            }
            if (mPosition > 0f || repaint) {
                mVtController.invalidate();
            }
        }

        void mutateForward() {

            long current = System.nanoTime();
            long elapse = current - mLastRender;
            mLastRender = current;

            mPosition += ((float) (elapse * 1E-6)) * mDpositionDt;
            if (mPosition >= 1.0f) {
                mPosition = 1.0f;
            }

            float ipos = (mInterpolator == null) ? mPosition
                    : mInterpolator.getInterpolation(mPosition);
            boolean repaint = mMC.interpolate(mMC.mView, ipos, current, mCache);

            if (mPosition >= 1) {
                if (mSetsTag != UNSET) {
                    mMC.getView().setTag(mSetsTag, System.nanoTime());
                }
                if (mClearsTag != UNSET) {
                    mMC.getView().setTag(mClearsTag, null);
                }
                if (!mHoldAt100) {
                    mVtController.removeAnimation(this);
                }
            }
            if (mPosition < 1f || repaint) {
                mVtController.invalidate();
            }
        }

        public void reactTo(int action, float x, float y) {
            switch (action) {
                case MotionEvent.ACTION_UP:
                    if (!mReverse) {
                        reverse(true);
                    }
                    return;
                case MotionEvent.ACTION_MOVE:
                    View view = mMC.getView();
                    view.getHitRect(mTempRec);
                    if (!mTempRec.contains((int) x, (int) y)) {
                        if (!mReverse) {
                            reverse(true);
                        }
                    }
            }
        }
    }

    void applyTransition(ViewTransitionController controller,
                         MotionLayout layout,
                         int fromId,
                         ConstraintSet current,
                         View... views) {
        if (mDisabled) {
            return;
        }
        if (mViewTransitionMode == VIEWTRANSITIONMODE_NOSTATE) {
            applyIndependentTransition(controller, layout, views[0]);
            return;
        }
        if (mViewTransitionMode == VIEWTRANSITIONMODE_ALLSTATES) {
            int[] ids = layout.getConstraintSetIds();
            for (int i = 0; i < ids.length; i++) {
                int id = ids[i];
                if (id == fromId) {
                    continue;
                }
                ConstraintSet cSet = layout.getConstraintSet(id);
                for (View view : views) {
                    ConstraintSet.Constraint constraint = cSet.getConstraint(view.getId());
                    if (mConstraintDelta != null) {
                        mConstraintDelta.applyDelta(constraint);
                        constraint.mCustomConstraints.putAll(mConstraintDelta.mCustomConstraints);
                    }
                }
            }
        }

        ConstraintSet transformedState = new ConstraintSet();
        transformedState.clone(current);
        for (View view : views) {
            ConstraintSet.Constraint constraint = transformedState.getConstraint(view.getId());
            if (mConstraintDelta != null) {
                mConstraintDelta.applyDelta(constraint);
                constraint.mCustomConstraints.putAll(mConstraintDelta.mCustomConstraints);
            }
        }

        layout.updateState(fromId, transformedState);
        layout.updateState(R.id.view_transition, current);
        layout.setState(R.id.view_transition, -1, -1);
        MotionScene.Transition tmpTransition =
                new MotionScene.Transition(-1, layout.mScene, R.id.view_transition, fromId);
        for (View view : views) {
            updateTransition(tmpTransition, view);
        }
        layout.setTransition(tmpTransition);
        layout.transitionToEnd(() -> {
            if (mSetsTag != UNSET) {
                for (View view : views) {
                    view.setTag(mSetsTag, System.nanoTime());
                }
            }
            if (mClearsTag != UNSET) {
                for (View view : views) {
                    view.setTag(mClearsTag, null);
                }
            }
        });
    }

    private void updateTransition(MotionScene.Transition transition, View view) {
        if (mDuration != -1) {
            transition.setDuration(mDuration);
        }
        transition.setPathMotionArc(mPathMotionArc);
        transition.setInterpolatorInfo(mDefaultInterpolator,
                mDefaultInterpolatorString, mDefaultInterpolatorID);
        int id = view.getId();
        if (mKeyFrames != null) {
            ArrayList<Key> keys = mKeyFrames.getKeyFramesForView(KeyFrames.UNSET);
            KeyFrames keyFrames = new KeyFrames();
            for (Key key : keys) {
                keyFrames.addKey(key.clone().setViewId(id));
            }

            transition.addKeyFrame(keyFrames);
        }
    }

    int getId() {
        return mId;
    }

    void setId(int id) {
        this.mId = id;
    }

    boolean matchesView(View view) {
        if (view == null) {
            return false;
        }
        if (mTargetId == -1 && mTargetString == null) {
            return false;
        }
        if (!checkTags(view)) {
            return false;
        }
        if (view.getId() == mTargetId) {
            return true;
        }
        if (mTargetString == null) {
            return false;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof ConstraintLayout.LayoutParams) {
            String tag = ((ConstraintLayout.LayoutParams) view.getLayoutParams()).constraintTag;
            if (tag != null && tag.matches(mTargetString)) {
                return true;
            }
        }
        return false;
    }

    boolean supports(int action) {
        if (mOnStateTransition == ONSTATE_ACTION_DOWN) {
            return action == MotionEvent.ACTION_DOWN;
        }
        if (mOnStateTransition == ONSTATE_ACTION_UP) {
            return action == MotionEvent.ACTION_UP;
        }
        if (mOnStateTransition == ONSTATE_ACTION_DOWN_UP) {
            return action == MotionEvent.ACTION_DOWN;
        }
        return false;
    }

    boolean isEnabled() {
        return !mDisabled;
    }

    void setEnabled(boolean enable) {
        this.mDisabled = !enable;
    }

    boolean checkTags(View view) {

        boolean set = (mIfTagSet == UNSET) ? true : (null != view.getTag(mIfTagSet));
        boolean notSet = (mIfTagNotSet == UNSET) ? true : null == view.getTag(mIfTagNotSet);
        return set && notSet;
    }
}
