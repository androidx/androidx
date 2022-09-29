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

package androidx.constraintlayout.core.motion.utils;

/**
 * This contains the class to provide the logic for an animation to come to a stop.
 * The setup defines a series of velocity gradients that gets to the desired position
 * ending at 0 velocity.
 * The path is computed such that the velocities are continuous
 *
 *
 */
public class StopLogicEngine implements StopEngine {
    // the velocity at the start of each period
    private float mStage1Velocity, mStage2Velocity, mStage3Velocity;
    private float mStage1Duration, mStage2Duration, mStage3Duration; // the time for each period
    private float mStage1EndPosition, mStage2EndPosition, mStage3EndPosition; // ending position
    private int mNumberOfStages;
    private String mType;
    private boolean mBackwards = false;
    private float mStartPosition;
    private float mLastPosition;
    @SuppressWarnings("unused")
    private boolean mDone = false;
    private static final float EPSILON = 0.00001f;

    /**
     * Debugging logic to log the state.
     *
     * @param desc Description to pre append
     * @param time Time during animation
     * @return string useful for debugging the state of the StopLogic
     */
    @Override
    public String debug(String desc, float time) {
        String ret = desc + " ===== " + mType + "\n";
        ret += desc + (mBackwards ? "backwards" : "forward ")
                + " time = " + time + "  stages " + mNumberOfStages + "\n";
        ret += desc + " dur " + mStage1Duration + " vel "
                + mStage1Velocity + " pos " + mStage1EndPosition + "\n";

        if (mNumberOfStages > 1) {
            ret += desc + " dur " + mStage2Duration + " vel "
                    + mStage2Velocity + " pos " + mStage2EndPosition + "\n";

        }
        if (mNumberOfStages > 2) {
            ret += desc + " dur " + mStage3Duration + " vel "
                    + mStage3Velocity + " pos " + mStage3EndPosition + "\n";
        }

        if (time <= mStage1Duration) {
            ret += desc + "stage 0" + "\n";
            return ret;
        }
        if (mNumberOfStages == 1) {
            ret += desc + "end stage 0" + "\n";
            return ret;
        }
        time -= mStage1Duration;
        if (time < mStage2Duration) {

            ret += desc + " stage 1" + "\n";
            return ret;
        }
        if (mNumberOfStages == 2) {
            ret += desc + "end stage 1" + "\n";
            return ret;
        }
        time -= mStage2Duration;
        if (time < mStage3Duration) {

            ret += desc + " stage 2" + "\n";
            return ret;
        }
        ret += desc + " end stage 2" + "\n";
        return ret;
    }

    // @TODO: add description
    @Override
    public float getVelocity(float x) {
        if (x <= mStage1Duration) {
            return mStage1Velocity + (mStage2Velocity - mStage1Velocity) * x / mStage1Duration;
        }
        if (mNumberOfStages == 1) {
            return 0;
        }
        x -= mStage1Duration;
        if (x < mStage2Duration) {

            return mStage2Velocity + (mStage3Velocity - mStage2Velocity) * x / mStage2Duration;
        }
        if (mNumberOfStages == 2) {
            return mStage2EndPosition;
        }
        x -= mStage2Duration;
        if (x < mStage3Duration) {

            return mStage3Velocity - mStage3Velocity * x / mStage3Duration;
        }
        return mStage3EndPosition;
    }

    private float calcY(float time) {
        mDone = false;
        if (time <= mStage1Duration) {
            return mStage1Velocity * time + (mStage2Velocity - mStage1Velocity)
                    * time * time / (2 * mStage1Duration);
        }
        if (mNumberOfStages == 1) {
            return mStage1EndPosition;
        }
        time -= mStage1Duration;
        if (time < mStage2Duration) {

            return mStage1EndPosition + mStage2Velocity * time
                    + (mStage3Velocity - mStage2Velocity) * time * time / (2 * mStage2Duration);
        }
        if (mNumberOfStages == 2) {
            return mStage2EndPosition;
        }
        time -= mStage2Duration;
        if (time <= mStage3Duration) {

            return mStage2EndPosition + mStage3Velocity
                    * time - mStage3Velocity * time * time / (2 * mStage3Duration);
        }
        mDone = true;
        return mStage3EndPosition;
    }

    // @TODO: add description
    public void config(float currentPos, float destination, float currentVelocity,
            float maxTime, float maxAcceleration, float maxVelocity) {
        mDone = false;
        mStartPosition = currentPos;
        mBackwards = (currentPos > destination);
        if (mBackwards) {
            setup(-currentVelocity, currentPos - destination,
                    maxAcceleration, maxVelocity, maxTime);
        } else {
            setup(currentVelocity, destination - currentPos, maxAcceleration, maxVelocity, maxTime);
        }
    }

    // @TODO: add description
    @Override
    public float getInterpolation(float v) {
        float y = calcY(v);
        mLastPosition = v;
        return mBackwards ? mStartPosition - y : mStartPosition + y;
    }

    @Override
    public float getVelocity() {
        return mBackwards ? -getVelocity(mLastPosition) : getVelocity(mLastPosition);
    }

    @Override
    public boolean isStopped() {
        return getVelocity() < EPSILON && Math.abs(mStage3EndPosition - mLastPosition) < EPSILON;
    }

    private void setup(float velocity, float distance, float maxAcceleration, float maxVelocity,
            float maxTime) {
        mDone = false;
        if (velocity == 0) {
            velocity = 0.0001f;
        }
        this.mStage1Velocity = velocity;
        float min_time_to_stop = velocity / maxAcceleration;
        float stopDistance = min_time_to_stop * velocity / 2;

        if (velocity < 0) { // backward
            float timeToZeroVelocity = -velocity / maxAcceleration;
            float reversDistanceTraveled = timeToZeroVelocity * velocity / 2;
            float totalDistance = distance - reversDistanceTraveled;
            float peak_v = (float) Math.sqrt(maxAcceleration * totalDistance);
            if (peak_v < maxVelocity) { // accelerate then decelerate
                mType = "backward accelerate, decelerate";
                this.mNumberOfStages = 2;
                this.mStage1Velocity = velocity;
                this.mStage2Velocity = peak_v;
                this.mStage3Velocity = 0;
                this.mStage1Duration = (peak_v - velocity) / maxAcceleration;
                this.mStage2Duration = peak_v / maxAcceleration;
                this.mStage1EndPosition = (velocity + peak_v) * this.mStage1Duration / 2;
                this.mStage2EndPosition = distance;
                this.mStage3EndPosition = distance;
                return;
            }
            mType = "backward accelerate cruse decelerate";
            this.mNumberOfStages = 3;
            this.mStage1Velocity = velocity;
            this.mStage2Velocity = maxVelocity;
            this.mStage3Velocity = maxVelocity;

            this.mStage1Duration = (maxVelocity - velocity) / maxAcceleration;
            this.mStage3Duration = maxVelocity / maxAcceleration;
            float accDist = (velocity + maxVelocity) * this.mStage1Duration / 2;
            float decDist = (maxVelocity * this.mStage3Duration) / 2;
            this.mStage2Duration = (distance - accDist - decDist) / maxVelocity;
            this.mStage1EndPosition = accDist;
            this.mStage2EndPosition = (distance - decDist);
            this.mStage3EndPosition = distance;
            return;
        }

        if (stopDistance >= distance) { // we cannot make it hit the breaks.
            // we do a force hard stop
            mType = "hard stop";
            float time = 2 * distance / velocity;
            this.mNumberOfStages = 1;
            this.mStage1Velocity = velocity;
            this.mStage2Velocity = 0;
            this.mStage1EndPosition = distance;
            this.mStage1Duration = time;
            return;
        }

        float distance_before_break = distance - stopDistance;
        float cruseTime = distance_before_break / velocity; // do we just Cruse then stop?
        if (cruseTime + min_time_to_stop < maxTime) { // close enough maintain v then break
            mType = "cruse decelerate";
            this.mNumberOfStages = 2;
            this.mStage1Velocity = velocity;
            this.mStage2Velocity = velocity;
            this.mStage3Velocity = 0;
            this.mStage1EndPosition = distance_before_break;
            this.mStage2EndPosition = distance;
            this.mStage1Duration = cruseTime;
            this.mStage2Duration = velocity / maxAcceleration;
            return;
        }

        float peak_v = (float) Math.sqrt(maxAcceleration * distance + velocity * velocity / 2);
        this.mStage1Duration = (peak_v - velocity) / maxAcceleration;
        this.mStage2Duration = peak_v / maxAcceleration;
        if (peak_v < maxVelocity) { // accelerate then decelerate
            mType = "accelerate decelerate";
            this.mNumberOfStages = 2;
            this.mStage1Velocity = velocity;
            this.mStage2Velocity = peak_v;
            this.mStage3Velocity = 0;
            this.mStage1Duration = (peak_v - velocity) / maxAcceleration;
            this.mStage2Duration = peak_v / maxAcceleration;
            this.mStage1EndPosition = (velocity + peak_v) * this.mStage1Duration / 2;
            this.mStage2EndPosition = distance;

            return;
        }
        mType = "accelerate cruse decelerate";
        // accelerate, cruse then decelerate
        this.mNumberOfStages = 3;
        this.mStage1Velocity = velocity;
        this.mStage2Velocity = maxVelocity;
        this.mStage3Velocity = maxVelocity;

        this.mStage1Duration = (maxVelocity - velocity) / maxAcceleration;
        this.mStage3Duration = maxVelocity / maxAcceleration;
        float accDist = (velocity + maxVelocity) * this.mStage1Duration / 2;
        float decDist = (maxVelocity * this.mStage3Duration) / 2;

        this.mStage2Duration = (distance - accDist - decDist) / maxVelocity;
        this.mStage1EndPosition = accDist;
        this.mStage2EndPosition = (distance - decDist);
        this.mStage3EndPosition = distance;
    }

    // Support the simple Decelerate use case
    public static class Decelerate implements StopEngine {
        private float mDestination;
        private float mInitialVelocity;
        private float mAcceleration;
        private float mLastVelocity;
        private float mDuration;
        private float mInitialPos;
        private boolean mDone = false;

        @Override
        public String debug(String desc, float time) {
            return mDuration + " " + mLastVelocity;
        }

        @Override
        public float getVelocity(float time) {
            if (time > mDuration) {
                return 0;
            }
            return mLastVelocity = mInitialVelocity + mAcceleration * time;
        }

        @Override
        public float getInterpolation(float time) {
            if (time > mDuration) {
                mDone = true;
                return mDestination;
            }
            getVelocity(time);
            return mInitialPos + (mInitialVelocity + mAcceleration * time / 2) * time;
        }

        @Override
        public float getVelocity() {
            return mLastVelocity;
        }

        @Override
        public boolean isStopped() {
            return mDone;
        }

        /**
         * Configure simple deceleration controller
         *
         * @param currentPos      the current position
         * @param destination     the destination position
         * @param currentVelocity the currentVelocity change in pos / second
         */
        public void config(float currentPos, float destination, float currentVelocity) {
            mDone = false;
            mDestination = destination;
            mInitialVelocity = currentVelocity;
            mInitialPos = currentPos;
            float distance = mDestination - currentPos;
            mDuration = distance / (currentVelocity / 2);
            mAcceleration = -currentVelocity / mDuration;
        }
    }
}
