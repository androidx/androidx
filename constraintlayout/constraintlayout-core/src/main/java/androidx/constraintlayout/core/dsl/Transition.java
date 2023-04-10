/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.dsl;

/**
 * Create a Transition Object.
 * Transition objects reference the start and end Constraints
 */
public class Transition {
    private OnSwipe mOnSwipe = null;
    final int UNSET = -1;
    private final int DEFAULT_DURATION = 400;
    private final float DEFAULT_STAGGER = 0;
    private String mId = null;
    private String mConstraintSetEnd = null;
    private String mConstraintSetStart = null;
    @SuppressWarnings("unused") private int mDefaultInterpolator = 0;
    @SuppressWarnings("unused") private String mDefaultInterpolatorString = null;
    @SuppressWarnings("unused") private int mDefaultInterpolatorID = -1;
    private int mDuration = DEFAULT_DURATION;
    private float mStagger = DEFAULT_STAGGER;

    private KeyFrames mKeyFrames = new KeyFrames();

    public void setOnSwipe(OnSwipe onSwipe) {
        mOnSwipe = onSwipe;
    }

    public void setKeyFrames(@SuppressWarnings("HiddenTypeParameter") Keys keyFrames) {
        mKeyFrames.add(keyFrames);
    }

    public Transition(String from, String to) {
        mId = "default";
        mConstraintSetStart = from;
        mConstraintSetEnd = to;
    }

    public Transition(String id, String from, String to) {
        mId = id;
        mConstraintSetStart = from;
        mConstraintSetEnd = to;
    }

    String toJson() {
        return toString();
    }

    public void setId(String id) {
        mId = id;
    }

    public void setTo(String constraintSetEnd) {
        mConstraintSetEnd = constraintSetEnd;
    }

    public void setFrom(String constraintSetStart) {
        mConstraintSetStart = constraintSetStart;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    public void setStagger(float stagger) {
        mStagger = stagger;
    }

    @Override
    public String toString() {
        String ret = mId + ":{\n"
                + "from:'" + mConstraintSetStart + "',\n"
                + "to:'" + mConstraintSetEnd + "',\n";
        if (mDuration != DEFAULT_DURATION) {
            ret += "duration:" + mDuration + ",\n";
        }
        if (mStagger != DEFAULT_STAGGER) {
            ret += "stagger:" + mStagger + ",\n";
        }
        if (mOnSwipe != null) {
            ret += mOnSwipe.toString();
        }

        ret += mKeyFrames.toString();


        ret += "},\n";

        return ret;
    }

    public String getId() {
        return mId;
    }
}
