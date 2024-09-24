/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.input.motionprediction.kalman;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.input.motionprediction.common.Configuration;
import androidx.input.motionprediction.kalman.matrix.DVector2;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 */
@RestrictTo(LIBRARY)
public class SinglePointerPredictor implements KalmanPredictor {
    private static final String TAG = "SinglePointerPredictor";

    // Influence of jank during each prediction sample
    private static final float JANK_INFLUENCE = 0.1f;

    // Influence of acceleration during each prediction sample
    private static final float ACCELERATION_INFLUENCE = 0.5f;

    // Influence of velocity during each prediction sample
    private static final float VELOCITY_INFLUENCE = 1.0f;

    // Range of jank values to expect.
    // Low value will use maximum prediction, high value will use no prediction.
    private static final float LOW_JANK = 0.02f;
    private static final float HIGH_JANK = 0.2f;
    private static final float ACCURATE_LOW_JANK = 0.1f;
    private static final float ACCURATE_HIGH_JANK = 0.2f;

    // Range of pen speed to expect (in dp / ms).
    // Low value will not use prediction, high value will use full prediction.
    private static final float LOW_SPEED = 0.0f;
    private static final float HIGH_SPEED = 2.0f;
    private static final float ACCURATE_LOW_SPEED = 0.0f;
    private static final float ACCURATE_HIGH_SPEED = 0.0f;

    private static final int EVENT_TIME_IGNORED_THRESHOLD_MS = 20;

    // Minimum number of Kalman filter samples needed for predicting the next point
    private static final int MIN_KALMAN_FILTER_ITERATIONS = 4;

    // The Kalman filter is tuned to smooth noise while maintaining fast reaction to direction
    // changes. The stronger the filter, the smoother the prediction result will be, at the
    // cost of possible prediction errors.
    private final PointerKalmanFilter mKalman = new PointerKalmanFilter(0.01, 1.0);

    private final DVector2 mLastPosition = new DVector2();
    private long mLastSeenEventTime;
    private double mLastPredictEventTime;
    private long mDownEventTime;
    private List<Double> mReportRates = new LinkedList<>();
    private int mExpectedPredictionSampleSize = -1;
    private double mReportRateMs = 0;

    private final DVector2 mPosition = new DVector2();
    private final DVector2 mVelocity = new DVector2();
    private final DVector2 mAcceleration = new DVector2();
    private final DVector2 mJank = new DVector2();

    /* pointer of the gesture that requires prediction */
    private int mPointerId;

    /* tool type of the gesture that requires prediction */
    private int mToolType;

    private double mPressure = 0;
    private double mLastOrientation = 0;
    private double mLastTilt = 0;

    private final boolean mPredictLift;
    private final int mStrategy;

    /**
     * Kalman based predictor, predicting the location of the pen `predictionTarget`
     * milliseconds into the future.
     *
     * <p>This filter can provide solid prediction up to 25ms into the future. If you are not
     * achieving close-to-zero latency, prediction errors can be more visible and the target should
     * be reduced to 20ms.
     */
    public SinglePointerPredictor(int strategy, int pointerId, int toolType) {
        mStrategy = strategy;
        mKalman.reset();
        mLastSeenEventTime = 0;
        mLastPredictEventTime = 0;
        mDownEventTime = 0;
        mPointerId = pointerId;
        mToolType = toolType;
        mPredictLift = Configuration.getInstance().predictLift();
    }

    private void update(float x, float y, float pressure, float orientation,
            float tilt, long eventTime) {
        if (x == mLastPosition.a1
                && y == mLastPosition.a2
                && (eventTime <= (mLastSeenEventTime + EVENT_TIME_IGNORED_THRESHOLD_MS))) {
            // Reduce Kalman filter jank by ignoring input event with similar coordinates
            // and eventTime as previous input event.
            // This is particularly useful when multiple pointer are on screen as in this case the
            // application will receive simultaneously multiple ACTION_MOVE MotionEvent
            // where position on screen and eventTime is unchanged.
            // This behavior that happens only in ARC++ and is likely due to Chrome Aura
            // implementation.
            return;
        }

        mKalman.update(x, y, pressure);
        mLastPosition.a1 = x;
        mLastPosition.a2 = y;
        mLastOrientation = orientation;
        mLastTilt = tilt;

        // Calculate average report rate over the first 20 samples. Most sensors will not
        // provide reliable timestamps and do not report at an even interval, so this is just
        // to be used as an estimate.
        if (mReportRates != null && mReportRates.size() < 20) {
            if (mLastSeenEventTime > 0) {
                double dt = eventTime - mLastSeenEventTime;
                mReportRates.add(dt);
                double sum = 0;
                for (double rate : mReportRates) {
                    sum += rate;
                }
                mReportRateMs = sum / mReportRates.size();
            }
        }
        mLastSeenEventTime = eventTime;
    }

    @Override
    public void setReportRate(int reportRateMs) {
        if (reportRateMs <= 0) {
            throw new IllegalArgumentException(
                    "reportRateMs should always be a strictly" + "positive number");
        }
        mReportRateMs = reportRateMs;
        mReportRates = null;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            mKalman.reset();
            mLastSeenEventTime = 0;
            mLastPredictEventTime = 0;
            return false;
        }
        int pointerIndex = event.findPointerIndex(mPointerId);

        if (pointerIndex == -1) {
            Log.i(
                    TAG,
                    String.format(
                            Locale.ROOT,
                            "onTouchEvent: Cannot find pointerId=%d in motionEvent=%s",
                            mPointerId,
                            event));
            return false;
        }

        mDownEventTime = event.getDownTime();

        for (BatchedMotionEvent ev : BatchedMotionEvent.iterate(event)) {
            MotionEvent.PointerCoords pointerCoords = ev.coords[pointerIndex];
            update(pointerCoords.x, pointerCoords.y, pointerCoords.pressure,
                    pointerCoords.orientation,
                    pointerCoords.getAxisValue(MotionEvent.AXIS_TILT), ev.timeMs);
        }
        return true;
    }

    @Override
    public @Nullable MotionEvent predict(int predictionTargetMs) {
        if (mReportRates == null) {
            mExpectedPredictionSampleSize = (int) Math.ceil(predictionTargetMs / mReportRateMs);
        }

        if (mExpectedPredictionSampleSize == -1
                && mKalman.getNumIterations() < MIN_KALMAN_FILTER_ITERATIONS) {
            return null;
        }

        mPosition.set(mLastPosition);
        mVelocity.set(mKalman.getVelocity());
        mAcceleration.set(mKalman.getAcceleration());
        mJank.set(mKalman.getJank());

        mPressure = mKalman.getPressure();
        double pressureChange = mKalman.getPressureChange();

        // Adjust prediction distance based on confidence of mKalman filter as well as movement
        // speed.
        double speedAbs = mVelocity.magnitude() / mReportRateMs;
        float lowSpeed, highSpeed, lowJank, highJank;
        if (usingAccurateTool()) {
            lowSpeed = ACCURATE_LOW_SPEED;
            highSpeed = ACCURATE_HIGH_SPEED;
            lowJank = ACCURATE_LOW_JANK;
            highJank = ACCURATE_HIGH_JANK;
        } else {
            lowSpeed = LOW_SPEED;
            highSpeed = HIGH_SPEED;
            lowJank = LOW_JANK;
            highJank = HIGH_JANK;
        }
        double speedFactor = normalizeRange(speedAbs, lowSpeed, highSpeed);
        double jankAbs = mJank.magnitude();
        double jankFactor = 1.0 - normalizeRange(jankAbs, lowJank, highJank);
        double confidenceFactor = speedFactor * jankFactor;

        if (mStrategy == Configuration.STRATEGY_AGGRESSIVE) {
            // We are very confident
            confidenceFactor = 1;
        }

        MotionEvent predictedEvent = null;
        final MotionEvent.PointerProperties[] pointerProperties =
                new MotionEvent.PointerProperties[1];
        pointerProperties[0] = new MotionEvent.PointerProperties();
        pointerProperties[0].id = mPointerId;
        pointerProperties[0].toolType = mToolType;

        // Project physical state of the pen into the future.
        int predictionTargetInSamples =
                (int) Math.ceil(predictionTargetMs / mReportRateMs * confidenceFactor);

        if (mExpectedPredictionSampleSize != -1) {
            // Normally this should always be false as confidenceFactor should be less than 1.0
            if (predictionTargetInSamples > mExpectedPredictionSampleSize) {
                predictionTargetInSamples = mExpectedPredictionSampleSize;
            }
        }

        if (mStrategy == Configuration.STRATEGY_SAFE) {
            // Just a single prediction step is very accurate
            predictionTargetInSamples = Math.max(predictionTargetInSamples, 1);
        }

        double predictedEventTime = mLastSeenEventTime;
        double nextPredictedEventTime = mLastSeenEventTime + mReportRateMs;
        for (int i = 0;
                i < predictionTargetInSamples || (nextPredictedEventTime <= mLastPredictEventTime);
                i++) {
            mAcceleration.a1 += mJank.a1 * JANK_INFLUENCE;
            mAcceleration.a2 += mJank.a2 * JANK_INFLUENCE;
            mVelocity.a1 += mAcceleration.a1 * ACCELERATION_INFLUENCE;
            mVelocity.a2 += mAcceleration.a2 * ACCELERATION_INFLUENCE;
            mPosition.a1 += mVelocity.a1 * VELOCITY_INFLUENCE;
            mPosition.a2 += mVelocity.a2 * VELOCITY_INFLUENCE;
            mPressure += pressureChange;

            // Ensure it's in the valid range
            if (mPressure < 0) {
                mPressure = 0;
            } else if (mPressure > 1) {
                mPressure = 1;
            }

            // Abort prediction if the pen is to be lifted.
            if (mPredictLift
                    && mPressure < 0.1
                    && nextPredictedEventTime > mLastPredictEventTime) {
                //TODO: Should we generate ACTION_UP MotionEvent instead of ACTION_MOVE?
                break;
            }

            MotionEvent.PointerCoords[] coords = {new MotionEvent.PointerCoords()};
            coords[0].x = (float) mPosition.a1;
            coords[0].y = (float) mPosition.a2;
            coords[0].pressure = (float) mPressure;
            coords[0].orientation = (float) mLastOrientation;
            coords[0].setAxisValue(MotionEvent.AXIS_TILT, (float) mLastTilt);
            if (predictedEvent == null) {
                predictedEvent =
                        MotionEvent.obtain(
                                mDownEventTime /* downTime */,
                                (long) nextPredictedEventTime /* eventTime */,
                                MotionEvent.ACTION_MOVE /* action */,
                                1 /* pointerCount */,
                                pointerProperties /* pointer properties */,
                                coords /* pointerCoords */,
                                0 /* metaState */,
                                0 /* button state */,
                                1.0f /* xPrecision */,
                                1.0f /* yPrecision */,
                                0 /* deviceId */,
                                0 /* edgeFlags */,
                                0 /* source */,
                                0 /* flags */);
            } else {
                predictedEvent.addBatch((long) nextPredictedEventTime, coords, 0);
            }
            // Keep track of the last predicted time
            predictedEventTime = nextPredictedEventTime;

            // Prepare for next iteration
            nextPredictedEventTime += mReportRateMs;
        }

        // Store the last predicted time
        mLastPredictEventTime = predictedEventTime;

        return predictedEvent;
    }

    private boolean usingAccurateTool() {
        return (mToolType != MotionEvent.TOOL_TYPE_FINGER);
    }

    private double normalizeRange(double x, double min, double max) {
        double normalized = (x - min) / (max - min);
        return Math.min(1.0, Math.max(normalized, 0.0));
    }

    /**
     * Append predicted event with samples where position and pressure are constant if predictor
     * consumer expect more samples
     *
     * @param predictedEvent
     */
    protected @Nullable MotionEvent appendPredictedEvent(@Nullable MotionEvent predictedEvent) {
        int predictedEventSize = (predictedEvent == null) ? 0 : predictedEvent.getHistorySize();
        for (int i = predictedEventSize; i < mExpectedPredictionSampleSize; i++) {
            MotionEvent.PointerCoords[] coords = {new MotionEvent.PointerCoords()};
            coords[0].x = (float) mPosition.a1;
            coords[0].y = (float) mPosition.a2;
            coords[0].pressure = (float) mPressure;
            if (predictedEvent == null) {
                final MotionEvent.PointerProperties[] pointerProperties =
                        new MotionEvent.PointerProperties[1];
                pointerProperties[0] = new MotionEvent.PointerProperties();
                pointerProperties[0].id = mPointerId;
                pointerProperties[0].toolType = mToolType;
                predictedEvent =
                        MotionEvent.obtain(
                                0 /* downTime */,
                                0 /* eventTime */,
                                MotionEvent.ACTION_MOVE /* action */,
                                1 /* pointerCount */,
                                pointerProperties /* pointer properties */,
                                coords /* pointerCoords */,
                                0 /* metaState */,
                                0 /* buttonState */,
                                1.0f /* xPrecision */,
                                1.0f /* yPrecision */,
                                0 /* deviceId */,
                                0 /* edgeFlags */,
                                0 /* source */,
                                0 /* flags */);
            } else {
                predictedEvent.addBatch(0, coords, 0);
            }
        }
        return predictedEvent;
    }
}
