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
import android.util.Xml;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.R;
import androidx.core.widget.NestedScrollView;

import org.xmlpull.v1.XmlPullParser;

import java.util.Arrays;

/**
 * This class is used to manage Touch behaviour
 *
 *
 */

class TouchResponse {
    private static final String TAG = "TouchResponse";
    private static final boolean DEBUG = false;
    private int mTouchAnchorSide = 0;
    private int mTouchSide = 0;
    private int mOnTouchUp = 0;
    private int mTouchAnchorId = MotionScene.UNSET;
    private int mTouchRegionId = MotionScene.UNSET;
    private int mLimitBoundsTo = MotionScene.UNSET;
    private float mTouchAnchorY = 0.5f;
    private float mTouchAnchorX = 0.5f;
    float mRotateCenterX = 0.5f;
    float mRotateCenterY = 0.5f;
    private int mRotationCenterId = MotionScene.UNSET;
    boolean mIsRotateMode = false;
    private float mTouchDirectionX = 0;
    private float mTouchDirectionY = 1;
    private boolean mDragStarted = false;
    private float[] mAnchorDpDt = new float[2];
    private int[] mTempLoc = new int[2];
    private float mLastTouchX, mLastTouchY;
    private final MotionLayout mMotionLayout;
    private static final int SEC_TO_MILLISECONDS = 1000;
    private static final float EPSILON = 0.0000001f;

    private static final float[][] TOUCH_SIDES = {
            {0.5f, 0.0f}, // top
            {0.0f, 0.5f}, // left
            {1.0f, 0.5f}, // right
            {0.5f, 1.0f}, // bottom
            {0.5f, 0.5f}, // middle
            {0.0f, 0.5f}, // start (dynamically updated)
            {1.0f, 0.5f}, // end  (dynamically updated)
    };
    private static final float[][] TOUCH_DIRECTION = {
            {0.0f, -1.0f}, // up
            {0.0f, 1.0f}, // down
            {-1.0f, 0.0f}, // left
            {1.0f, 0.0f}, // right
            {-1.0f, 0.0f}, // start (dynamically updated)
            {1.0f, 0.0f}, // end  (dynamically updated)
    };
    @SuppressWarnings("unused")
    private static final int TOUCH_UP = 0;
    @SuppressWarnings("unused")
    private static final int TOUCH_DOWN = 1;
    private static final int TOUCH_LEFT = 2;
    private static final int TOUCH_RIGHT = 3;
    private static final int TOUCH_START = 4;
    private static final int TOUCH_END = 5;

    @SuppressWarnings("unused")
    private static final int SIDE_TOP = 0;
    private static final int SIDE_LEFT = 1;
    private static final int SIDE_RIGHT = 2;
    @SuppressWarnings("unused")
    private static final int SIDE_BOTTOM = 3;
    @SuppressWarnings("unused")
    private static final int SIDE_MIDDLE = 4;
    private static final int SIDE_START = 5;
    private static final int SIDE_END = 6;

    private float mMaxVelocity = 4;
    private float mMaxAcceleration = 1.2f;
    private boolean mMoveWhenScrollAtTop = true;
    private float mDragScale = 1f;
    private int mFlags = 0;
    static final int FLAG_DISABLE_POST_SCROLL = 1;
    static final int FLAG_DISABLE_SCROLL = 2;
    static final int FLAG_SUPPORT_SCROLL_UP = 4;

    private float mDragThreshold = 10;
    private float mSpringDamping = 10;
    private float mSpringMass = 1;
    private float mSpringStiffness = Float.NaN;
    private float mSpringStopThreshold = Float.NaN;
    private int mSpringBoundary = 0;
    private int mAutoCompleteMode = COMPLETE_MODE_CONTINUOUS_VELOCITY;
    public static final int COMPLETE_MODE_CONTINUOUS_VELOCITY = 0;
    public static final int COMPLETE_MODE_SPRING = 1;

    TouchResponse(Context context, MotionLayout layout, XmlPullParser parser) {
        mMotionLayout = layout;
        fillFromAttributeList(context, Xml.asAttributeSet(parser));
    }

    TouchResponse(MotionLayout layout, OnSwipe onSwipe) {
        mMotionLayout = layout;
        mTouchAnchorId = onSwipe.getTouchAnchorId();
        mTouchAnchorSide = onSwipe.getTouchAnchorSide();
        if (mTouchAnchorSide != -1) {
            mTouchAnchorX = TOUCH_SIDES[mTouchAnchorSide][0];
            mTouchAnchorY = TOUCH_SIDES[mTouchAnchorSide][1];
        }
        mTouchSide = onSwipe.getDragDirection();
        if (mTouchSide < TOUCH_DIRECTION.length) {
            mTouchDirectionX = TOUCH_DIRECTION[mTouchSide][0];
            mTouchDirectionY = TOUCH_DIRECTION[mTouchSide][1];
        } else {
            mTouchDirectionX = mTouchDirectionY = Float.NaN;
            mIsRotateMode = true;
        }
        mMaxVelocity = onSwipe.getMaxVelocity();
        mMaxAcceleration = onSwipe.getMaxAcceleration();
        mMoveWhenScrollAtTop = onSwipe.getMoveWhenScrollAtTop();
        mDragScale = onSwipe.getDragScale();
        mDragThreshold = onSwipe.getDragThreshold();
        mTouchRegionId = onSwipe.getTouchRegionId();
        mOnTouchUp = onSwipe.getOnTouchUp();
        mFlags = onSwipe.getNestedScrollFlags();
        mLimitBoundsTo = onSwipe.getLimitBoundsTo();
        mRotationCenterId = onSwipe.getRotationCenterId();
        mSpringBoundary = onSwipe.getSpringBoundary();
        mSpringDamping = onSwipe.getSpringDamping();
        mSpringMass = onSwipe.getSpringMass();
        mSpringStiffness = onSwipe.getSpringStiffness();
        mSpringStopThreshold = onSwipe.getSpringStopThreshold();
        mAutoCompleteMode = onSwipe.getAutoCompleteMode();
    }

    public void setRTL(boolean rtl) {
        if (rtl) {
            TOUCH_DIRECTION[TOUCH_START] = TOUCH_DIRECTION[TOUCH_RIGHT];
            TOUCH_DIRECTION[TOUCH_END] = TOUCH_DIRECTION[TOUCH_LEFT];
            TOUCH_SIDES[SIDE_START] = TOUCH_SIDES[SIDE_RIGHT];
            TOUCH_SIDES[SIDE_END] = TOUCH_SIDES[SIDE_LEFT];
        } else {
            TOUCH_DIRECTION[TOUCH_START] = TOUCH_DIRECTION[TOUCH_LEFT];
            TOUCH_DIRECTION[TOUCH_END] = TOUCH_DIRECTION[TOUCH_RIGHT];
            TOUCH_SIDES[SIDE_START] = TOUCH_SIDES[SIDE_LEFT];
            TOUCH_SIDES[SIDE_END] = TOUCH_SIDES[SIDE_RIGHT];
        }

        mTouchAnchorX = TOUCH_SIDES[mTouchAnchorSide][0];
        mTouchAnchorY = TOUCH_SIDES[mTouchAnchorSide][1];
        if (mTouchSide >= TOUCH_DIRECTION.length) {
            return;
        }
        mTouchDirectionX = TOUCH_DIRECTION[mTouchSide][0];
        mTouchDirectionY = TOUCH_DIRECTION[mTouchSide][1];
    }

    private void fillFromAttributeList(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OnSwipe);
        fill(a);
        a.recycle();
    }

    private void fill(TypedArray a) {
        final int count = a.getIndexCount();
        for (int i = 0; i < count; i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.OnSwipe_touchAnchorId) {
                mTouchAnchorId = a.getResourceId(attr, mTouchAnchorId);
            } else if (attr == R.styleable.OnSwipe_touchAnchorSide) {
                mTouchAnchorSide = a.getInt(attr, mTouchAnchorSide);
                mTouchAnchorX = TOUCH_SIDES[mTouchAnchorSide][0];
                mTouchAnchorY = TOUCH_SIDES[mTouchAnchorSide][1];
            } else if (attr == R.styleable.OnSwipe_dragDirection) {
                mTouchSide = a.getInt(attr, mTouchSide);
                if (mTouchSide < TOUCH_DIRECTION.length) {
                    mTouchDirectionX = TOUCH_DIRECTION[mTouchSide][0];
                    mTouchDirectionY = TOUCH_DIRECTION[mTouchSide][1];
                } else {
                    mTouchDirectionX = mTouchDirectionY = Float.NaN;
                    mIsRotateMode = true;
                }
            } else if (attr == R.styleable.OnSwipe_maxVelocity) {
                mMaxVelocity = a.getFloat(attr, mMaxVelocity);
            } else if (attr == R.styleable.OnSwipe_maxAcceleration) {
                mMaxAcceleration = a.getFloat(attr, mMaxAcceleration);
            } else if (attr == R.styleable.OnSwipe_moveWhenScrollAtTop) {
                mMoveWhenScrollAtTop = a.getBoolean(attr, mMoveWhenScrollAtTop);
            } else if (attr == R.styleable.OnSwipe_dragScale) {
                mDragScale = a.getFloat(attr, mDragScale);
            } else if (attr == R.styleable.OnSwipe_dragThreshold) {
                mDragThreshold = a.getFloat(attr, mDragThreshold);
            } else if (attr == R.styleable.OnSwipe_touchRegionId) {
                mTouchRegionId = a.getResourceId(attr, mTouchRegionId);
            } else if (attr == R.styleable.OnSwipe_onTouchUp) {
                mOnTouchUp = a.getInt(attr, mOnTouchUp);
            } else if (attr == R.styleable.OnSwipe_nestedScrollFlags) {
                mFlags = a.getInteger(attr, 0);
            } else if (attr == R.styleable.OnSwipe_limitBoundsTo) {
                mLimitBoundsTo = a.getResourceId(attr, 0);
            } else if (attr == R.styleable.OnSwipe_rotationCenterId) {
                mRotationCenterId = a.getResourceId(attr, mRotationCenterId);
            } else if (attr == R.styleable.OnSwipe_springDamping) {
                mSpringDamping = a.getFloat(attr, mSpringDamping);
            } else if (attr == R.styleable.OnSwipe_springMass) {
                mSpringMass = a.getFloat(attr, mSpringMass);
            } else if (attr == R.styleable.OnSwipe_springStiffness) {
                mSpringStiffness = a.getFloat(attr, mSpringStiffness);
            } else if (attr == R.styleable.OnSwipe_springStopThreshold) {
                mSpringStopThreshold = a.getFloat(attr, mSpringStopThreshold);
            } else if (attr == R.styleable.OnSwipe_springBoundary) {
                mSpringBoundary = a.getInt(attr, mSpringBoundary);
            } else if (attr == R.styleable.OnSwipe_autoCompleteMode) {
                mAutoCompleteMode = a.getInt(attr, mAutoCompleteMode);
            }

        }
    }

    void setUpTouchEvent(float lastTouchX, float lastTouchY) {
        mLastTouchX = lastTouchX;
        mLastTouchY = lastTouchY;
        mDragStarted = false;
    }

    /**
     * @param event
     * @param velocityTracker
     * @param currentState
     * @param motionScene
     */
    void processTouchRotateEvent(MotionEvent event,
                                 MotionLayout.MotionTracker velocityTracker,
                                 int currentState,
                                 MotionScene motionScene) {
        velocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                mLastTouchX = event.getRawX();
                mLastTouchY = event.getRawY();

                mDragStarted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                @SuppressWarnings("unused")
                float dy = event.getRawY() - mLastTouchY;
                @SuppressWarnings("unused")
                float dx = event.getRawX() - mLastTouchX;

                float drag;

                float rcx = mMotionLayout.getWidth() / 2.0f;
                float rcy = mMotionLayout.getHeight() / 2.0f;
                if (mRotationCenterId != MotionScene.UNSET) {
                    View v = mMotionLayout.findViewById(mRotationCenterId);
                    mMotionLayout.getLocationOnScreen(mTempLoc);
                    rcx = mTempLoc[0] + (v.getLeft() + v.getRight()) / 2.0f;
                    rcy = mTempLoc[1] + (v.getTop() + v.getBottom()) / 2.0f;
                } else if (mTouchAnchorId != MotionScene.UNSET) {
                    MotionController mc = mMotionLayout.getMotionController(mTouchAnchorId);
                    View v = mMotionLayout.findViewById(mc.getAnimateRelativeTo());
                    if (v == null) {
                        Log.e(TAG, "could not find view to animate to");
                    } else {
                        mMotionLayout.getLocationOnScreen(mTempLoc);
                        rcx = mTempLoc[0] + (v.getLeft() + v.getRight()) / 2.0f;
                        rcy = mTempLoc[1] + (v.getTop() + v.getBottom()) / 2.0f;
                    }
                }
                float relativePosX = event.getRawX() - rcx;
                float relativePosY = event.getRawY() - rcy;

                double angle1 = Math.atan2(event.getRawY() - rcy, event.getRawX() - rcx);
                double angle2 = Math.atan2(mLastTouchY - rcy, mLastTouchX - rcx);
                drag = (float) ((angle1 - angle2) * 180.0f / Math.PI);
                if (drag > 330) {
                    drag -= 360;
                } else if (drag < -330) {
                    drag += 360;
                }

                if (Math.abs(drag) > 0.01 || mDragStarted) {
                    float pos = mMotionLayout.getProgress();
                    if (!mDragStarted) {
                        mDragStarted = true;
                        mMotionLayout.setProgress(pos);
                    }
                    if (mTouchAnchorId != MotionScene.UNSET) {
                        mMotionLayout.getAnchorDpDt(mTouchAnchorId, pos,
                                mTouchAnchorX, mTouchAnchorY, mAnchorDpDt);
                        mAnchorDpDt[1] = (float) Math.toDegrees(mAnchorDpDt[1]);
                    } else {
                        mAnchorDpDt[1] = 360;
                    }
                    float change = drag * mDragScale / mAnchorDpDt[1];

                    pos = Math.max(Math.min(pos + change, 1), 0);
                    float current = mMotionLayout.getProgress();

                    if (pos != current) {
                        if (current == 0.0f || current == 1.0f) {
                            mMotionLayout.endTrigger(current == 0.0f);
                        }
                        mMotionLayout.setProgress(pos);
                        velocityTracker.computeCurrentVelocity(SEC_TO_MILLISECONDS);
                        float tvx = velocityTracker.getXVelocity();
                        float tvy = velocityTracker.getYVelocity();
                        float angularVelocity = // v*sin(angle)/r
                                (float) (Math.hypot(tvy, tvx)
                                        * Math.sin(Math.atan2(tvy, tvx) - angle1)
                                        / Math.hypot(relativePosX, relativePosY));
                        mMotionLayout.mLastVelocity = (float) Math.toDegrees(angularVelocity);
                    } else {
                        mMotionLayout.mLastVelocity = 0;
                    }
                    mLastTouchX = event.getRawX();
                    mLastTouchY = event.getRawY();
                }

                break;
            case MotionEvent.ACTION_UP:
                mDragStarted = false;
                velocityTracker.computeCurrentVelocity(16);

                float tvx = velocityTracker.getXVelocity();
                float tvy = velocityTracker.getYVelocity();
                float currentPos = mMotionLayout.getProgress();
                float pos = currentPos;
                rcx = mMotionLayout.getWidth() / 2.0f;
                rcy = mMotionLayout.getHeight() / 2.0f;
                if (mRotationCenterId != MotionScene.UNSET) {
                    View v = mMotionLayout.findViewById(mRotationCenterId);
                    mMotionLayout.getLocationOnScreen(mTempLoc);
                    rcx = mTempLoc[0] + (v.getLeft() + v.getRight()) / 2.0f;
                    rcy = mTempLoc[1] + (v.getTop() + v.getBottom()) / 2.0f;
                } else if (mTouchAnchorId != MotionScene.UNSET) {
                    MotionController mc = mMotionLayout.getMotionController(mTouchAnchorId);
                    View v = mMotionLayout.findViewById(mc.getAnimateRelativeTo());
                    mMotionLayout.getLocationOnScreen(mTempLoc);
                    rcx = mTempLoc[0] + (v.getLeft() + v.getRight()) / 2.0f;
                    rcy = mTempLoc[1] + (v.getTop() + v.getBottom()) / 2.0f;
                }
                relativePosX = event.getRawX() - rcx;
                relativePosY = event.getRawY() - rcy;
                angle1 = Math.toDegrees(Math.atan2(relativePosY, relativePosX));

                if (mTouchAnchorId != MotionScene.UNSET) {
                    mMotionLayout.getAnchorDpDt(mTouchAnchorId, pos,
                            mTouchAnchorX, mTouchAnchorY, mAnchorDpDt);
                    mAnchorDpDt[1] = (float) Math.toDegrees(mAnchorDpDt[1]);
                } else {
                    mAnchorDpDt[1] = 360;
                }
                angle2 = Math.toDegrees(Math.atan2(tvy + relativePosY, tvx + relativePosX));
                drag = (float) (angle2 - angle1);
                float velocity_tweek = SEC_TO_MILLISECONDS / 16f;
                float angularVelocity = drag * velocity_tweek;
                if (!Float.isNaN(angularVelocity)) {
                    pos += 3 * angularVelocity * mDragScale / mAnchorDpDt[1]; // TODO calibrate vel
                }
                if (pos != 0.0f && pos != 1.0f && mOnTouchUp != MotionLayout.TOUCH_UP_STOP) {
                    angularVelocity = (float) angularVelocity * mDragScale / mAnchorDpDt[1];
                    float target = (pos < 0.5) ? 0.0f : 1.0f;

                    if (mOnTouchUp == MotionLayout.TOUCH_UP_NEVER_TO_START) {
                        if (currentPos + angularVelocity < 0) {
                            angularVelocity = Math.abs(angularVelocity);
                        }
                        target = 1;
                    }
                    if (mOnTouchUp == MotionLayout.TOUCH_UP_NEVER_TO_END) {
                        if (currentPos + angularVelocity > 1) {
                            angularVelocity = -Math.abs(angularVelocity);
                        }
                        target = 0;
                    }
                    mMotionLayout.touchAnimateTo(mOnTouchUp, target ,
                            3 * angularVelocity);
                    if (0.0f >= currentPos || 1.0f <= currentPos) {
                        mMotionLayout.setState(MotionLayout.TransitionState.FINISHED);
                    }
                } else if (0.0f >= pos || 1.0f <= pos) {
                    mMotionLayout.setState(MotionLayout.TransitionState.FINISHED);
                }
                break;
        }

    }

    /**
     * Process touch events
     *
     * @param event        The event coming from the touch
     * @param currentState
     * @param motionScene  The relevant MotionScene
     */
    void processTouchEvent(MotionEvent event,
                           MotionLayout.MotionTracker velocityTracker,
                           int currentState,
                           MotionScene motionScene) {
        if (DEBUG) {
            Log.v(TAG, Debug.getLocation() + " best processTouchEvent For ");
        }
        if (mIsRotateMode) {
            processTouchRotateEvent(event, velocityTracker, currentState, motionScene);
            return;
        }
        velocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = event.getRawX();
                mLastTouchY = event.getRawY();
                mDragStarted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = event.getRawY() - mLastTouchY;
                float dx = event.getRawX() - mLastTouchX;
                float drag = dx * mTouchDirectionX + dy * mTouchDirectionY;
                if (DEBUG) {
                    Log.v(TAG, "# dx = " + dx + " = " + event.getRawX() + " - " + mLastTouchX);
                    Log.v(TAG, "# drag = " + drag);
                }
                if (Math.abs(drag) > mDragThreshold || mDragStarted) {
                    if (DEBUG) {
                        Log.v(TAG, "# ACTION_MOVE  mDragStarted  ");
                    }
                    float pos = mMotionLayout.getProgress();
                    if (!mDragStarted) {
                        mDragStarted = true;
                        mMotionLayout.setProgress(pos);
                        if (DEBUG) {
                            Log.v(TAG, "# ACTION_MOVE  progress <- " + pos);
                        }
                    }
                    if (mTouchAnchorId != MotionScene.UNSET) {

                        mMotionLayout.getAnchorDpDt(mTouchAnchorId, pos, mTouchAnchorX,
                                mTouchAnchorY, mAnchorDpDt);
                        if (DEBUG) {
                            Log.v(TAG, Debug.getLocation() + " mAnchorDpDt "
                                    + Arrays.toString(mAnchorDpDt));
                        }
                    } else {
                        if (DEBUG) {
                            Log.v(TAG, Debug.getLocation() + " NO ANCHOR ");
                        }
                        float minSize = Math.min(mMotionLayout.getWidth(),
                                mMotionLayout.getHeight());
                        mAnchorDpDt[1] = minSize * mTouchDirectionY;
                        mAnchorDpDt[0] = minSize * mTouchDirectionX;
                    }

                    float movmentInDir = mTouchDirectionX * mAnchorDpDt[0]
                            + mTouchDirectionY * mAnchorDpDt[1];
                    if (DEBUG) {
                        Log.v(TAG, "# ACTION_MOVE  movmentInDir <- " + movmentInDir + " ");

                        Log.v(TAG, "# ACTION_MOVE  mAnchorDpDt  = " + mAnchorDpDt[0]
                                + " ,  " + mAnchorDpDt[1]);
                        Log.v(TAG, "# ACTION_MOVE  mTouchDir  = " + mTouchDirectionX
                                + " , " + mTouchDirectionY);

                    }
                    movmentInDir *= mDragScale;

                    if (Math.abs(movmentInDir) < 0.01) {
                        mAnchorDpDt[0] = .01f;
                        mAnchorDpDt[1] = .01f;

                    }
                    float change;
                    if (mTouchDirectionX != 0) {
                        change = dx / mAnchorDpDt[0];
                    } else {
                        change = dy / mAnchorDpDt[1];
                    }
                    if (DEBUG) {
                        Log.v(TAG, "# ACTION_MOVE      CHANGE  = " + change);
                    }

                    pos = Math.max(Math.min(pos + change, 1), 0);

                    if (mOnTouchUp == MotionLayout.TOUCH_UP_NEVER_TO_START) {
                        pos = Math.max(pos, 0.01f);
                    }
                    if (mOnTouchUp == MotionLayout.TOUCH_UP_NEVER_TO_END) {
                        pos = Math.min(pos, 0.99f);
                    }

                    float current = mMotionLayout.getProgress();
                    if (pos != current) {
                        if (current == 0.0f || current == 1.0f) {
                            mMotionLayout.endTrigger(current == 0.0f);
                        }
                        mMotionLayout.setProgress(pos);
                        if (DEBUG) {
                            Log.v(TAG, "# ACTION_MOVE progress <- " + pos);
                        }
                        velocityTracker.computeCurrentVelocity(SEC_TO_MILLISECONDS);
                        float tvx = velocityTracker.getXVelocity();
                        float tvy = velocityTracker.getYVelocity();
                        float velocity = (mTouchDirectionX != 0) ? tvx / mAnchorDpDt[0]
                                : tvy / mAnchorDpDt[1];
                        mMotionLayout.mLastVelocity = velocity;
                    } else {
                        mMotionLayout.mLastVelocity = 0;
                    }
                    mLastTouchX = event.getRawX();
                    mLastTouchY = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_UP:
                mDragStarted = false;
                velocityTracker.computeCurrentVelocity(SEC_TO_MILLISECONDS);
                float tvx = velocityTracker.getXVelocity();
                float tvy = velocityTracker.getYVelocity();
                float currentPos = mMotionLayout.getProgress();
                float pos = currentPos;

                if (DEBUG) {
                    Log.v(TAG, "# ACTION_UP progress  = " + pos);
                }
                if (mTouchAnchorId != MotionScene.UNSET) {
                    mMotionLayout.getAnchorDpDt(mTouchAnchorId, pos,
                            mTouchAnchorX, mTouchAnchorY, mAnchorDpDt);
                } else {
                    float minSize = Math.min(mMotionLayout.getWidth(), mMotionLayout.getHeight());
                    mAnchorDpDt[1] = minSize * mTouchDirectionY;
                    mAnchorDpDt[0] = minSize * mTouchDirectionX;
                }
                @SuppressWarnings("unused")
                float movmentInDir = mTouchDirectionX * mAnchorDpDt[0]
                        + mTouchDirectionY * mAnchorDpDt[1];
                float velocity;
                if (mTouchDirectionX != 0) {
                    velocity = tvx / mAnchorDpDt[0];
                } else {
                    velocity = tvy / mAnchorDpDt[1];
                }
                if (DEBUG) {
                    Log.v(TAG, "# ACTION_UP               tvy = " + tvy);
                    Log.v(TAG, "# ACTION_UP mTouchDirectionX  = " + mTouchDirectionX);
                    Log.v(TAG, "# ACTION_UP         velocity  = " + velocity);
                }

                if (!Float.isNaN(velocity)) {
                    pos += velocity / 3; // TODO calibration & animation speed based on velocity
                }
                if (pos != 0.0f && pos != 1.0f && mOnTouchUp != MotionLayout.TOUCH_UP_STOP) {
                    float target = (pos < 0.5) ? 0.0f : 1.0f;

                    if (mOnTouchUp == MotionLayout.TOUCH_UP_NEVER_TO_START) {
                        if (currentPos + velocity < 0) {
                            velocity = Math.abs(velocity);
                        }
                        target = 1;
                    }
                    if (mOnTouchUp == MotionLayout.TOUCH_UP_NEVER_TO_END) {
                        if (currentPos + velocity > 1) {
                            velocity = -Math.abs(velocity);
                        }
                        target = 0;
                    }

                    mMotionLayout.touchAnimateTo(mOnTouchUp, target, velocity);
                    if (0.0f >= currentPos || 1.0f <= currentPos) {
                        mMotionLayout.setState(MotionLayout.TransitionState.FINISHED);
                    }
                } else if (0.0f >= pos || 1.0f <= pos) {
                    mMotionLayout.setState(MotionLayout.TransitionState.FINISHED);

                }
                break;
        }
    }

    void setDown(float lastTouchX, float lastTouchY) {
        mLastTouchX = lastTouchX;
        mLastTouchY = lastTouchY;
    }

    /**
     * Calculate if a drag in this direction results in an increase or decrease in progress.
     *
     * @param dx drag direction in x
     * @param dy drag direction in y
     * @return the change in progress given that dx and dy
     */
    float getProgressDirection(float dx, float dy) {
        float pos = mMotionLayout.getProgress();
        mMotionLayout.getAnchorDpDt(mTouchAnchorId, pos, mTouchAnchorX, mTouchAnchorY, mAnchorDpDt);
        float velocity;
        if (mTouchDirectionX != 0) {
            if (mAnchorDpDt[0] == 0) {
                mAnchorDpDt[0] = EPSILON;
            }
            velocity = dx * mTouchDirectionX / mAnchorDpDt[0];
        } else {
            if (mAnchorDpDt[1] == 0) {
                mAnchorDpDt[1] = EPSILON;
            }
            velocity = dy * mTouchDirectionY / mAnchorDpDt[1];
        }
        return velocity;
    }

    void scrollUp(float dx, float dy) {
        mDragStarted = false;

        float pos = mMotionLayout.getProgress();
        mMotionLayout.getAnchorDpDt(mTouchAnchorId, pos, mTouchAnchorX, mTouchAnchorY, mAnchorDpDt);
        @SuppressWarnings("unused")
        float movmentInDir = mTouchDirectionX * mAnchorDpDt[0] + mTouchDirectionY * mAnchorDpDt[1];
        float velocity;
        if (mTouchDirectionX != 0) {
            velocity = dx * mTouchDirectionX / mAnchorDpDt[0];
        } else {
            velocity = dy * mTouchDirectionY / mAnchorDpDt[1];
        }
        if (!Float.isNaN(velocity)) {
            pos += velocity / 3; // TODO calibration & animation speed based on velocity
        }
        if (pos != 0.0f && pos != 1.0f && mOnTouchUp != MotionLayout.TOUCH_UP_STOP) {
            mMotionLayout.touchAnimateTo(mOnTouchUp, (pos < 0.5) ? 0.0f : 1.0f, velocity);
        }
    }

    void scrollMove(float dx, float dy) {
        @SuppressWarnings("unused")
        float drag = dx * mTouchDirectionX + dy * mTouchDirectionY;
        if (true) { // Todo evaluate || Math.abs(drag) > 10 || mDragStarted) {
            float pos = mMotionLayout.getProgress();
            if (!mDragStarted) {
                mDragStarted = true;
                mMotionLayout.setProgress(pos);
            }
            mMotionLayout.getAnchorDpDt(mTouchAnchorId, pos,
                    mTouchAnchorX, mTouchAnchorY, mAnchorDpDt);
            float movmentInDir = mTouchDirectionX * mAnchorDpDt[0]
                    + mTouchDirectionY * mAnchorDpDt[1];

            if (Math.abs(movmentInDir) < 0.01) {
                mAnchorDpDt[0] = .01f;
                mAnchorDpDt[1] = .01f;

            }
            float change;
            if (mTouchDirectionX != 0) {
                change = dx * mTouchDirectionX / mAnchorDpDt[0];
            } else {
                change = dy * mTouchDirectionY / mAnchorDpDt[1];
            }
            pos = Math.max(Math.min(pos + change, 1), 0);

            if (pos != mMotionLayout.getProgress()) {
                mMotionLayout.setProgress(pos);
                if (DEBUG) {
                    Log.v(TAG, "# ACTION_UP        progress <- " + pos);
                }
            }

        }
    }

    void setupTouch() {

        View view = null;
        if (mTouchAnchorId != -1) {
            view = mMotionLayout.findViewById(mTouchAnchorId);
            if (view == null) {
                Log.e(TAG, "cannot find TouchAnchorId @id/"
                        + Debug.getName(mMotionLayout.getContext(), mTouchAnchorId));
            }
        }
        if (view instanceof NestedScrollView) {
            final NestedScrollView sv = (NestedScrollView) view;
            sv.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return false;
                }
            });
            sv.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {

                @Override
                public void onScrollChange(NestedScrollView v,
                                           int scrollX,
                                           int scrollY,
                                           int oldScrollX,
                                           int oldScrollY) {

                }
            });
        }
    }

    /**
     * set the id of the anchor
     *
     * @param id
     */
    public void setAnchorId(int id) {
        mTouchAnchorId = id;
    }

    /**
     * Get the view being used as anchor
     *
     * @return
     */
    public int getAnchorId() {
        return mTouchAnchorId;
    }

    /**
     * Set the location in the view to be the touch anchor
     *
     * @param x location in x 0 = left, 1 = right
     * @param y location in y 0 = top, 1 = bottom
     */
    public void setTouchAnchorLocation(float x, float y) {
        mTouchAnchorX = x;
        mTouchAnchorY = y;
    }

    /**
     * Sets the maximum velocity allowed on touch up.
     * Velocity is the rate of change in "progress" per second.
     *
     * @param velocity in progress per second 1 = one second to do the entire animation
     */
    public void setMaxVelocity(float velocity) {
        mMaxVelocity = velocity;
    }

    /**
     * set the maximum Acceleration allowed for a motion.
     * Acceleration is the rate of change velocity per second.
     *
     * @param acceleration
     */
    public void setMaxAcceleration(float acceleration) {
        mMaxAcceleration = acceleration;
    }

    float getMaxAcceleration() {
        return mMaxAcceleration;
    }

    /**
     * Gets the maximum velocity allowed on touch up.
     * Velocity is the rate of change in "progress" per second.
     *
     * @return
     */
    public float getMaxVelocity() {
        return mMaxVelocity;
    }

    boolean getMoveWhenScrollAtTop() {
        return mMoveWhenScrollAtTop;
    }

    /**
     * Get how the drag progress will return to the start or end state on touch up.
     * Can be ether COMPLETE_MODE_CONTINUOUS_VELOCITY (default) or COMPLETE_MODE_SPRING
     * @return
     */
    public int getAutoCompleteMode() {
        return mAutoCompleteMode;
    }
    /**
     * set how the drag progress will return to the start or end state on touch up.
     *
     *
     * @return
     */
    void setAutoCompleteMode(int autoCompleteMode) {
        mAutoCompleteMode = autoCompleteMode;
    }

    /**
     * This calculates the bounds of the mTouchRegionId view.
     * This reuses rect for efficiency as this class will be called many times.
     *
     * @param layout The layout containing the view (findViewId)
     * @param rect   the rectangle to fill provided so this function does not have to create memory
     * @return the rect or null
     */
    RectF getTouchRegion(ViewGroup layout, RectF rect) {
        if (mTouchRegionId == MotionScene.UNSET) {
            return null;
        }
        View view = layout.findViewById(mTouchRegionId);
        if (view == null) {
            return null;
        }
        rect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        return rect;
    }

    int getTouchRegionId() {
        return mTouchRegionId;
    }

    /**
     * This calculates the bounds of the mTouchRegionId view.
     * This reuses rect for efficiency as this class will be called many times.
     *
     * @param layout The layout containing the view (findViewId)
     * @param rect   the rectangle to fill provided for memory efficiency
     * @return the rect or null
     */
    RectF getLimitBoundsTo(ViewGroup layout, RectF rect) {
        if (mLimitBoundsTo == MotionScene.UNSET) {
            return null;
        }
        View view = layout.findViewById(mLimitBoundsTo);
        if (view == null) {
            return null;
        }
        rect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        return rect;
    }

    int getLimitBoundsToId() {
        return mLimitBoundsTo;
    }

    float dot(float dx, float dy) {
        return dx * mTouchDirectionX + dy * mTouchDirectionY;
    }

    @Override
    public String toString() {
        return Float.isNaN(mTouchDirectionX) ? "rotation"
                : (mTouchDirectionX + " , " + mTouchDirectionY);
    }

    /**
     * flags to control
     *
     * @return
     */
    public int getFlags() {
        return mFlags;
    }

    public void setTouchUpMode(int touchUpMode) {
        mOnTouchUp = touchUpMode;
    }

    /**
     * the stiffness of the spring if using spring
     *  K in "a = (-k*x-c*v)/m" equation for the acceleration of a spring
     * @return NaN if not set
     */
    public float getSpringStiffness() {
        return mSpringStiffness;
    }

    /**
     * the Mass of the spring if using spring
     *  m in "a = (-k*x-c*v)/m" equation for the acceleration of a spring
     * @return default is 1
     */
    public float getSpringMass() {
        return mSpringMass;
    }

    /**
     * the damping of the spring if using spring
     * c in "a = (-k*x-c*v)/m" equation for the acceleration of a spring
     * @return NaN if not set
     */
    public float getSpringDamping() {
        return mSpringDamping;
    }

    /**
     * The threshold below
     * @return NaN if not set
     */
    public float getSpringStopThreshold() {
        return mSpringStopThreshold;
    }

    /**
     * The spring's behaviour when it hits 0 or 1. It can be made ot overshoot or bounce
     * overshoot = 0
     * bounceStart = 1
     * bounceEnd = 2
     * bounceBoth = 3
     * @return Bounce mode
     */
    public int getSpringBoundary() {
        return mSpringBoundary;
    }

    boolean isDragStarted() {
        return mDragStarted;
    }

}
