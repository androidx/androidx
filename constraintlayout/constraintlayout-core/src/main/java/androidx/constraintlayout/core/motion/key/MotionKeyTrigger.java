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
package androidx.constraintlayout.core.motion.key;

import androidx.constraintlayout.core.motion.CustomVariable;
import androidx.constraintlayout.core.motion.MotionWidget;
import androidx.constraintlayout.core.motion.utils.FloatRect;
import androidx.constraintlayout.core.motion.utils.SplineSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

public class MotionKeyTrigger extends MotionKey {
    private static final String TAG = "KeyTrigger";
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

    private int mCurveFit = -1;
    private String mCross = null;
    private int mTriggerReceiver = UNSET;
    private String mNegativeCross = null;
    private String mPositiveCross = null;
    private int mTriggerID = UNSET;
    private int mTriggerCollisionId = UNSET;
    //   TODO private MotionWidget mTriggerCollisionView = null;
    float mTriggerSlack = .1f;
    private boolean mFireCrossReset = true;
    private boolean mFireNegativeReset = true;
    private boolean mFirePositiveReset = true;
    private float mFireThreshold = Float.NaN;
    private float mFireLastPos;
    private boolean mPostLayout = false;
    int mViewTransitionOnNegativeCross = UNSET;
    int mViewTransitionOnPositiveCross = UNSET;
    int mViewTransitionOnCross = UNSET;

    public static final int TYPE_VIEW_TRANSITION_ON_CROSS = 301;
    public static final int TYPE_VIEW_TRANSITION_ON_POSITIVE_CROSS = 302;
    public static final int TYPE_VIEW_TRANSITION_ON_NEGATIVE_CROSS = 303;
    public static final int TYPE_POST_LAYOUT = 304;
    public static final int TYPE_TRIGGER_SLACK = 305;
    public static final int TYPE_TRIGGER_COLLISION_VIEW = 306;
    public static final int TYPE_TRIGGER_COLLISION_ID = 307;
    public static final int TYPE_TRIGGER_ID = 308;
    public static final int TYPE_POSITIVE_CROSS = 309;
    public static final int TYPE_NEGATIVE_CROSS = 310;
    public static final int TYPE_TRIGGER_RECEIVER = 311;
    public static final int TYPE_CROSS = 312;

    FloatRect mCollisionRect = new FloatRect();
    FloatRect mTargetRect = new FloatRect();
    public static final int KEY_TYPE = 5;

    {
        mType = KEY_TYPE;
        mCustom = new HashMap<>();
    }

    @Override
    public void getAttributeNames(HashSet<String> attributes) {

    }

    @Override
    public void addValues(HashMap<String, SplineSet> splines) {

    }

    @Override
    public int getId(String name) {
        switch (name) {
            case VIEW_TRANSITION_ON_CROSS:
                return TYPE_VIEW_TRANSITION_ON_CROSS;
            case VIEW_TRANSITION_ON_POSITIVE_CROSS:
                return TYPE_VIEW_TRANSITION_ON_POSITIVE_CROSS;
            case VIEW_TRANSITION_ON_NEGATIVE_CROSS:
                return TYPE_VIEW_TRANSITION_ON_NEGATIVE_CROSS;
            case POST_LAYOUT:
                return TYPE_POST_LAYOUT;
            case TRIGGER_SLACK:
                return TYPE_TRIGGER_SLACK;
            case TRIGGER_COLLISION_VIEW:
                return TYPE_TRIGGER_COLLISION_VIEW;
            case TRIGGER_COLLISION_ID:
                return TYPE_TRIGGER_COLLISION_ID;
            case TRIGGER_ID:
                return TYPE_TRIGGER_ID;
            case POSITIVE_CROSS:
                return TYPE_POSITIVE_CROSS;
            case NEGATIVE_CROSS:
                return TYPE_NEGATIVE_CROSS;
            case TRIGGER_RECEIVER:
                return TYPE_TRIGGER_RECEIVER;
        }
        return -1;
    }

    // @TODO: add description
    @Override
    public MotionKeyTrigger copy(MotionKey src) {
        super.copy(src);
        MotionKeyTrigger k = (MotionKeyTrigger) src;
        mCurveFit = k.mCurveFit;
        mCross = k.mCross;
        mTriggerReceiver = k.mTriggerReceiver;
        mNegativeCross = k.mNegativeCross;
        mPositiveCross = k.mPositiveCross;
        mTriggerID = k.mTriggerID;
        mTriggerCollisionId = k.mTriggerCollisionId;
        // TODO mTriggerCollisionView = k.mTriggerCollisionView;
        mTriggerSlack = k.mTriggerSlack;
        mFireCrossReset = k.mFireCrossReset;
        mFireNegativeReset = k.mFireNegativeReset;
        mFirePositiveReset = k.mFirePositiveReset;
        mFireThreshold = k.mFireThreshold;
        mFireLastPos = k.mFireLastPos;
        mPostLayout = k.mPostLayout;
        mCollisionRect = k.mCollisionRect;
        mTargetRect = k.mTargetRect;
        return this;
    }

    // @TODO: add description
    @Override
    public MotionKey clone() {
        return new MotionKeyTrigger().copy(this);
    }

    @SuppressWarnings("unused")
    private void fireCustom(String str, MotionWidget widget) {
        boolean callAll = str.length() == 1;
        if (!callAll) {
            str = str.substring(1).toLowerCase(Locale.ROOT);
        }
        for (String name : mCustom.keySet()) {
            String lowerCase = name.toLowerCase(Locale.ROOT);
            if (callAll || lowerCase.matches(str)) {
                CustomVariable custom = mCustom.get(name);
                if (custom != null) {
                    custom.applyToWidget(widget);
                }
            }
        }
    }

    // @TODO: add description
    public void conditionallyFire(float position, MotionWidget child) {
    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, int value) {
        switch (type) {
            case TriggerType.TYPE_TRIGGER_RECEIVER:
                mTriggerReceiver = value;
                break;
            case TriggerType.TYPE_TRIGGER_ID:
                mTriggerID = toInt(value);
                break;
            case TriggerType.TYPE_TRIGGER_COLLISION_ID:
                mTriggerCollisionId = value;
                break;
            case TriggerType.TYPE_VIEW_TRANSITION_ON_NEGATIVE_CROSS:
                mViewTransitionOnNegativeCross = value;
                break;
            case TriggerType.TYPE_VIEW_TRANSITION_ON_POSITIVE_CROSS:
                mViewTransitionOnPositiveCross = value;
                break;

            case TriggerType.TYPE_VIEW_TRANSITION_ON_CROSS:
                mViewTransitionOnCross = value;
                break;
            default:
                return super.setValue(type, value);
        }
        return true;
    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, float value) {
        switch (type) {
            case TriggerType.TYPE_TRIGGER_SLACK:
                mTriggerSlack = value;
                break;
            default:
                return super.setValue(type, value);
        }
        return true;
    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, String value) {
        switch (type) {
            case TriggerType.TYPE_CROSS:
                mCross = value;
                break;
            case TriggerType.TYPE_NEGATIVE_CROSS:
                mNegativeCross = value;
                break;
            case TriggerType.TYPE_POSITIVE_CROSS:
                mPositiveCross = value;
                break;
//                TODO
//            case TRIGGER_COLLISION_VIEW:
//                mTriggerCollisionView = (MotionWidget) value;
//                break;

            default:

                return super.setValue(type, value);
        }
        return true;
    }

    // @TODO: add description
    @Override
    public boolean setValue(int type, boolean value) {
        switch (type) {
            case TriggerType.TYPE_POST_LAYOUT:
                mPostLayout = value;
                break;
            default:
                return super.setValue(type, value);
        }
        return true;
    }


}
