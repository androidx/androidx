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
import android.util.SparseArray;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.Locale;

/**
 */
@RestrictTo(LIBRARY)
public class MultiPointerPredictor implements KalmanPredictor {
    private static final String TAG = "MultiPointerPredictor";
    private static final boolean DEBUG_PREDICTION = Log.isLoggable(TAG, Log.DEBUG);

    private final SparseArray<SinglePointerPredictor> mPredictorMap = new SparseArray<>();
    private int mReportRateMs = 0;

    public MultiPointerPredictor() {}

    @Override
    public void setReportRate(int reportRateMs) {
        if (reportRateMs <= 0) {
            throw new IllegalArgumentException(
                    "reportRateMs should always be a strictly" + "positive number");
        }
        mReportRateMs = reportRateMs;

        for (int i = 0; i < mPredictorMap.size(); ++i) {
            mPredictorMap.valueAt(i).setReportRate(mReportRateMs);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            SinglePointerPredictor predictor = new SinglePointerPredictor(
                    pointerId,
                    event.getToolType(actionIndex)
            );
            if (mReportRateMs > 0) {
                predictor.setReportRate(mReportRateMs);
            }
            predictor.onTouchEvent(event);
            mPredictorMap.put(pointerId, predictor);
        } else if (action == MotionEvent.ACTION_UP) {
            SinglePointerPredictor predictor = mPredictorMap.get(pointerId);
            if (predictor != null) {
                mPredictorMap.remove(pointerId);
                predictor.onTouchEvent(event);
            }
            mPredictorMap.clear();
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            SinglePointerPredictor predictor = mPredictorMap.get(pointerId);
            if (predictor != null) {
                mPredictorMap.remove(pointerId);
                predictor.onTouchEvent(event);
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            mPredictorMap.clear();
        } else if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < mPredictorMap.size(); ++i) {
                mPredictorMap.valueAt(i).onTouchEvent(event);
            }
        } else {
            // ignore other events
            return false;
        }
        return true;
    }

    /** Support eventTime */
    @Override
    public @Nullable MotionEvent predict(int predictionTargetMs) {
        final int pointerCount = mPredictorMap.size();
        // Shortcut for likely case where only zero or one pointer is on the screen
        // this logic exists only to make sure logic when one pointer is on screen then
        // there is no performance degradation of using MultiPointerPredictor vs
        // SinglePointerPredictor
        // TODO: verify performance is not degraded by removing this shortcut logic.
        if (pointerCount == 0) {
            if (DEBUG_PREDICTION) {
                Log.d(TAG, "predict() -> null: no pointer on screen");
            }
            return null;
        }
        if (pointerCount == 1) {
            SinglePointerPredictor predictor = mPredictorMap.valueAt(0);
            MotionEvent predictedEv = predictor.predict(predictionTargetMs);
            if (DEBUG_PREDICTION) {
                Log.d(TAG, "predict() -> MotionEvent: " + predictedEv);
            }
            return predictedEv;
        }

        // Predict MotionEvent for each pointer
        int[] pointerIds = new int[pointerCount];
        MotionEvent[] singlePointerEvents = new MotionEvent[pointerCount];
        for (int i = 0; i < pointerCount; ++i) {
            pointerIds[i] = mPredictorMap.keyAt(i);
            SinglePointerPredictor predictor = mPredictorMap.valueAt(i);
            singlePointerEvents[i] = predictor.predict(predictionTargetMs);
        }

        // Compute minimal history size for every predicted single pointer MotionEvent
        boolean foundNullPrediction = false;
        int minHistorySize = Integer.MAX_VALUE;
        for (MotionEvent ev : singlePointerEvents) {
            if (ev == null) {
                foundNullPrediction = true;
                break;
            }
            if (ev.getHistorySize() < minHistorySize) {
                minHistorySize = ev.getHistorySize();
            }
        }

        if (foundNullPrediction) {
            for (MotionEvent ev : singlePointerEvents) {
                if (ev != null) {
                    ev.recycle();
                }
            }
            return null;
        }

        // Take into account the current event of each predicted MotionEvent
        minHistorySize += 1;

        // Merge single pointer MotionEvent into a single MotionEvent
        MotionEvent.PointerCoords[][] pointerCoords =
                new MotionEvent.PointerCoords[minHistorySize][pointerCount];
        for (int pointerIndex = 0; pointerIndex < pointerCount; pointerIndex++) {
            int historyIndex = 0;
            for (BatchedMotionEvent ev :
                    BatchedMotionEvent.iterate(singlePointerEvents[pointerIndex])) {
                pointerCoords[historyIndex][pointerIndex] = ev.coords[0];
                if (minHistorySize <= ++historyIndex) {
                    break;
                }
            }
        }

        // Recycle single pointer predicted MotionEvent
        for (MotionEvent ev : singlePointerEvents) {
            ev.recycle();
        }

        // Generate predicted multi-pointer MotionEvent
        final MotionEvent.PointerProperties[] pointerProperties =
                new MotionEvent.PointerProperties[pointerCount];
        for (int i = 0; i < pointerCount; i++) {
            pointerProperties[i] = new MotionEvent.PointerProperties();
            pointerProperties[i].id = pointerIds[i];
        }
        MotionEvent multiPointerEvent =
                MotionEvent.obtain(
                        0 /* down time */,
                        0 /* event time */,
                        MotionEvent.ACTION_MOVE /* action */,
                        pointerCount /* pointer count */,
                        pointerProperties /* pointer properties */,
                        pointerCoords[0] /* pointer coordinates */,
                        0 /* meta state */,
                        0 /* button state */,
                        1.0f /* x */,
                        1.0f /* y */,
                        0 /* device ID */,
                        0 /* edge flags */,
                        0 /* source */,
                        0 /* flags */);
        for (int historyIndex = 1; historyIndex < minHistorySize; historyIndex++) {
            multiPointerEvent.addBatch(0, pointerCoords[historyIndex], 0);
        }
        if (DEBUG_PREDICTION) {
            final StringBuilder builder =
                    new StringBuilder(
                            String.format(
                                    Locale.ROOT,
                                    "predict() -> MotionEvent: (pointerCount=%d, historySize=%d);",
                                    multiPointerEvent.getPointerCount(),
                                    multiPointerEvent.getHistorySize()));
            for (BatchedMotionEvent motionEvent : BatchedMotionEvent.iterate(multiPointerEvent)) {
                builder.append("      ");
                for (MotionEvent.PointerCoords coord : motionEvent.coords) {
                    builder.append(String.format(Locale.ROOT, "(%f, %f)", coord.x, coord.y));
                }
                builder.append("\n");
            }
            Log.d(TAG, builder.toString());
        }
        return multiPointerEvent;
    }
}
