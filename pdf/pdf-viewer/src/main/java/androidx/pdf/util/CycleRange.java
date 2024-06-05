/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Cycles through a range of numbers from [0 to size-1], but starting at any number between the two.
 * Stops once it has been around the cycle exactly once. If the start is not between 0 and size - 1,
 * it is fixed using the same type of modulo arithmetic: so start = -1 means start at (size - 1).
 *
 * <p>For example: with start = 2, size = 5, backwards = false, produces: [2, 3, 4, 0, 1] Or with
 * backwards = true produces: [2, 1, 0, 4, 3]
 *
 * <p>Start values of 7 or -3 would produce the same results.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CycleRange implements Iterable<Integer> {
    public final int start;
    public final int size;
    public final Direction direction;

    /** Directions to iterate around a cycle. */
    public enum Direction {
        FORWARDS(1),
        BACKWARDS(-1),
        /**
         * Outwards is an interleaved mix of forwards and backwards - it iterates outwards in both
         * directions, nearest neighbours first and finishing on the furthest point in the cycle.
         */
        OUTWARDS(0);

        public final int sign;

        Direction(int sign) {
            this.sign = sign;
        }
    }

    /** Static factory. */
    @NonNull
    public static CycleRange of(int start, int size, @NonNull Direction direction) {
        return new CycleRange(start, size, direction);
    }

    private CycleRange(int start, int size, Direction direction) {
        Preconditions.checkArgument(size > 0, "size must be > 0");
        this.start = ((start % size) + size) % size;
        this.size = size;
        this.direction = direction;
    }

    @NonNull
    @Override
    public Iterator iterator() {
        return new Iterator();
    }

    /** CycleRange is an {@link Iterable}, this is its {@link java.util.Iterator}. */
    public class Iterator implements java.util.Iterator<Integer> {
        private int mI = 0;

        @Override
        public boolean hasNext() {
            return mI != size;
        }

        /**
         *
         */
        @NonNull
        public Integer peekNext() {
            return (start + getAddend() + size) % size;
        }

        @Override
        public Integer next() {
            int next = peekNext();
            mI++;
            return next;
        }

        private int getAddend() {
            switch (direction) {
                case FORWARDS:
                    return mI;
                case BACKWARDS:
                    return -mI;
                case OUTWARDS:
                    return (mI % 2 == 1) ? ((mI + 1) / 2) : -(mI / 2);
                default:
                    throw new RuntimeException("Never happens");
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove not supported");
        }

        @NonNull
        public Direction getDirection() {
            return direction;
        }
    }
}
