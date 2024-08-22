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

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.Iterator;

/**
 * This class contains a list of historical {@link MotionEvent.PointerCoords} for a given time
 *
 */
@RestrictTo(LIBRARY)
public class BatchedMotionEvent {
    /**
     * Historical pointer coordinate data as per {@link MotionEvent#getPointerCoords}, that occurred
     * between this event and the previous event for the given pointer. Only applies to ACTION_MOVE
     * events.
     */
    public final MotionEvent.PointerCoords[] coords;
    /**
     * The time this event occurred in the {@link android.os.SystemClock#uptimeMillis} time base.
     */
    public long timeMs;

    public BatchedMotionEvent(int pointerCount) {
        coords = new MotionEvent.PointerCoords[pointerCount];
        for (int i = 0; i < pointerCount; ++i) {
            coords[i] = new MotionEvent.PointerCoords();
        }
    }

    /**
     * This method creates an {@link Iterable} that will iterate over the historical {@link
     * MotionEvent}s.
     */
    public static @NonNull IterableMotionEvent iterate(@NonNull MotionEvent ev) {
        return new IterableMotionEvent(ev);
    }

    /** An {@link Iterable} list of {@link BatchedMotionEvent} objects. */
    public static class IterableMotionEvent implements Iterable<BatchedMotionEvent> {
        private final int mPointerCount;
        private final MotionEvent mMotionEvent;

        IterableMotionEvent(@NonNull MotionEvent motionEvent) {
            mMotionEvent = motionEvent;
            mPointerCount = motionEvent.getPointerCount();
        }

        public @NonNull MotionEvent getMotionEvent() {
            return mMotionEvent;
        }

        public int getPointerCount() {
            return mPointerCount;
        }

        @Override
        @NonNull
        public Iterator<BatchedMotionEvent> iterator() {
            return new Iterator<BatchedMotionEvent>() {
                private int mHistoryId = 0;

                @Override
                public boolean hasNext() {
                    return mHistoryId < (getMotionEvent().getHistorySize() + 1);
                }

                @Override
                public BatchedMotionEvent next() {
                    MotionEvent motionEvent = getMotionEvent();
                    int pointerCount = getPointerCount();

                    if (mHistoryId > motionEvent.getHistorySize()) {
                        return null;
                    }
                    BatchedMotionEvent batchedEvent = new BatchedMotionEvent(pointerCount);
                    if (mHistoryId < motionEvent.getHistorySize()) {
                        for (int pointerId = 0; pointerId < pointerCount; ++pointerId) {
                            motionEvent.getHistoricalPointerCoords(
                                    pointerId, mHistoryId, batchedEvent.coords[pointerId]);
                        }
                        batchedEvent.timeMs = motionEvent.getHistoricalEventTime(mHistoryId);
                    } else { // (mHistoryId == mMotionEvent.getHistorySize()) {
                        for (int pointerId = 0; pointerId < pointerCount; ++pointerId) {
                            motionEvent.getPointerCoords(
                                    pointerId, batchedEvent.coords[pointerId]);
                        }
                        batchedEvent.timeMs = motionEvent.getEventTime();
                    }
                    mHistoryId++;
                    return batchedEvent;
                }
            };
        }
    }
}
