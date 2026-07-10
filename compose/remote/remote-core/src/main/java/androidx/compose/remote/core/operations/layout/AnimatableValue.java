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
package androidx.compose.remote.core.operations.layout;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation;
import androidx.compose.remote.core.operations.utilities.easing.GeneralEasing;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

/** Value animation for layouts */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnimatableValue implements Serializable {
    boolean mIsVariable = false;
    int mId = 0;
    float mValue = 0f;

    boolean mAnimateValueChanges = true;
    boolean mAnimate = false;
    long mAnimateTargetTime = 0;
    float mAnimateDuration = 300f;
    float mTargetRotationX;
    float mStartRotationX;
    long mLastUpdate = 0L;

    int mMotionEasingType = GeneralEasing.CUBIC_STANDARD;
    FloatAnimation mMotionEasing;

    /**
     * Value to animate
     *
     * @param value value
     * @param animateValueChanges animate the change of values
     */
    public AnimatableValue(float value, boolean animateValueChanges) {
        mAnimateValueChanges = animateValueChanges;
        if (Utils.isVariable(value)) {
            mId = Utils.idFromNan(value);
            mIsVariable = true;
        } else {
            mValue = value;
        }
    }

    /**
     * Value to animate.
     *
     * @param value value
     */
    public AnimatableValue(float value) {
        this(value, true);
    }

    /**
     * Get the value
     *
     * @return the value
     */
    public float getValue() {
        return mValue;
    }

    /**
     * Evaluate going through FloatAnimation if needed
     *
     * @param context the paint context
     * @return the current value
     */
    public float evaluate(@NonNull PaintContext context) {
        if (!mIsVariable) {
            return mValue;
        }
        float value = context.getContext().mRemoteComposeState.getFloat(mId);
        long timeMillis = context.getClock().millis();
        if (value != mValue) {
            long lastUpdate = timeMillis;
            long interval = lastUpdate - mLastUpdate;
            if (interval > mAnimateDuration && mLastUpdate != 0L) {
                mAnimateValueChanges = true;
            } else {
                mAnimateValueChanges = false;
            }
            mLastUpdate = lastUpdate;
        }
        if (!mAnimateValueChanges) {
            mValue = value;
        } else {
            if (value != mValue && !mAnimate) {
                // animate
                mStartRotationX = mValue;
                mTargetRotationX = value;
                mAnimate = true;
                mAnimateTargetTime = timeMillis;
                mMotionEasing =
                        new FloatAnimation(
                                mMotionEasingType, mAnimateDuration / 1000f, null, 0f, Float.NaN);
                mMotionEasing.setTargetValue(1f);
            }
            if (mAnimate) {
                float elapsed = timeMillis - mAnimateTargetTime;
                float p = mMotionEasing.get(elapsed / mAnimateDuration);
                mValue = (1 - p) * mStartRotationX + p * mTargetRotationX;
                if (p >= 1f) {
                    mAnimate = false;
                }
            } else {
                mValue = mTargetRotationX;
            }
        }
        return mValue;
    }

    @Override
    public String toString() {
        return "AnimatableValue{mId=" + mId + "}";
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType("AnimatableValue").add("id", mId);
    }
}
