/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.compose.remote.core.operations.paint;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.Utils;

import org.jspecify.annotations.NonNull;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class PaintPathEffects {
    protected int mType;
    protected int mDataLength;
    public static final int DASH = 1;
    public static final int DISCRETE_PATH = 2;
    public static final int PATH_DASH = 3;
    public static final int SUM = 4;
    public static final int COMPOSE = 5;

    /**
     * Interface to register a path effect
     */
    public interface Register {
        /**
         * Register a path effect
         *
         * @param id id of the path effect
         */
        void id(int id);
    }


    /**
     * Get the ids for the path effects
     *
     * @param data     array of ids
     * @param offset   offset into the array
     * @param register register the ids
     * @return the offset into the array after the ids have been processed
     */
    static int getIds(int @NonNull [] data, int offset, @NonNull Register register) {

        switch (data[offset++]) {
            case DASH:
                return Dash.gitIds(data, offset, register);
            case DISCRETE_PATH:
                return Discrete.gitIds(data, offset, register);
            case PATH_DASH:
                return PathDash.gitIds(data, offset, register);
            case SUM:
                return Sum.gitIds(data, offset, register);
            case COMPOSE:
                return Compose.gitIds(data, offset, register);
        }
        Utils.log("should not get here offset  " + (offset - 1) + "  data =" + data[offset - 1]);
        return -1;
    }

    abstract float @NonNull [] toFloatArray();

    /**
     * Parse a path effect from a float array
     */
    public static @NonNull PaintPathEffects parse(float @NonNull [] data, int offset) {
        switch (Float.floatToRawIntBits(data[offset++])) {
            case DASH:
                return Dash.decode(data, offset);
            case DISCRETE_PATH:
                return Discrete.decode(data, offset);
            case PATH_DASH:
                return PathDash.decode(data, offset);
            case SUM:
                return Sum.decode(data, offset);
            case COMPOSE:
                return Compose.decode(data, offset);
        }
        throw new RuntimeException("Unknown type of path effect");
    }

    /**
     * Get the type of path effect
     *
     * @return type of path effect
     */
    public int getType() {
        return mType;
    }

    /**
     * Convert a path effect to a float array
     *
     * @param pe path effect
     * @return float array
     */
    public static float @NonNull [] encode(@NonNull PaintPathEffects pe) {
        return pe.toFloatArray();
    }

    /**
     * Fuze two path effects together
     *
     * @param type type of path effect
     * @param a    first path effect
     * @param b    second path effect
     * @return float array
     */
    public static float @NonNull [] fuze(int type, float @NonNull [] a, float @NonNull [] b) {
        float[] ret = new float[a.length + b.length + 1];
        ret[0] = Float.intBitsToFloat(type);
        System.arraycopy(a, 0, ret, 1, a.length);
        for (int i = 0; i < b.length; i++) {
            ret[i + a.length + 1] = b[i];
        }
        return ret;
    }

    public static class Dash extends PaintPathEffects {
        public float mPhase;
        public float @NonNull [] mIntervals;

        /**
         * Create a dash path effect
         *
         * @param phase     phase offset
         * @param intervals dash intervals
         */
        public Dash(float phase, float @NonNull ... intervals) {
            this.mPhase = phase;
            this.mIntervals = intervals;
            this.mType = DASH;
        }

        /**
         * Parse a dash path effect from a float array
         *
         * @param data   float array
         * @param offset offset into the array
         */
        public static @NonNull PaintPathEffects decode(float @NonNull [] data, int offset) {
            float phase = data[offset];
            float[] intervals = new float[Float.floatToRawIntBits(data[offset + 1])];
            System.arraycopy(data, offset + 2, intervals, 0, intervals.length);
            Dash ret = new Dash(phase, intervals);
            ret.mDataLength = intervals.length + 2;
            return ret;
        }

        /**
         * Get the ids for the path effects
         *
         * @param data     array of ids
         * @param offset   offset into the array
         * @param register register the ids
         * @return the offset into the array after the ids have been processed
         */
        static int gitIds(int @NonNull [] data, int offset, @NonNull Register register) {
            registerIfId(data, offset, register);
            int count = data[offset + 1];
            for (int i = 0; i < count; i++) {
                registerIfId(data, offset + 2 + i, register);
            }
            return offset + 2 + count;
        }

        /**
         * Convert a dash path effect to a float array
         *
         * @return float array
         */
        @Override
        float @NonNull [] toFloatArray() {
            return dash(mPhase, mIntervals);
        }
    }

    /**
     * Register an id if it is a NaN id
     *
     * @param data     float array
     * @param offset   offset into the array
     * @param register register the id
     */
    private static void registerIfId(int @NonNull [] data, int offset, @NonNull Register register) {
        float v = Float.intBitsToFloat(data[offset]);
        if (Float.isNaN(v)) {
            register.id(offset);
        }
    }

    /**
     * Chop the path into lines of segmentLength, randomly deviating from the original path by
     */
    public static class Discrete extends PaintPathEffects {
        public float mSegmentLength;
        public float mDeviation;

        public Discrete(float segmentLength, float deviation) {
            this.mSegmentLength = segmentLength;
            this.mDeviation = deviation;
            this.mType = DISCRETE_PATH;
        }

        /**
         * Parse a discrete path effect from a float array
         *
         * @param data   float array
         * @param offset offset into the array
         * @return discrete path effect
         */
        public static @NonNull PaintPathEffects decode(float @NonNull [] data, int offset) {
            float segmentLength = data[offset];
            float deviation = data[offset + 1];
            Discrete ret = new Discrete(segmentLength, deviation);
            ret.mDataLength = 2;
            return ret;
        }

        /**
         * Get the ids for the path effects
         *
         * @param data     array of ids
         * @param offset   offset into the array
         * @param register register the ids
         * @return the offset into the array after the ids have been processed
         */
        public static int gitIds(int @NonNull [] data, int offset, @NonNull Register register) {
            registerIfId(data, offset, register);
            registerIfId(data, offset + 1, register);
            return offset + 2;
        }

        /**
         * Convert a discrete path effect to a float array
         *
         * @return float array
         */
        @Override
        float @NonNull [] toFloatArray() {
            return discrete(mSegmentLength, mDeviation);
        }
    }

    /**
     * class represents a dash path effect
     */
    public static class PathDash extends PaintPathEffects {
        public int mShapeId;
        public float mAdvance;
        public float mPhase;
        public int mStyle;

        /**
         * Create a dash path effect
         *
         * @param shapeId id of the shape to use
         * @param advance advance
         * @param phase   phase offset
         * @param style   how to transform the shape at each position as it is stamped
         */
        public PathDash(int shapeId, float advance, float phase, int style) {
            this.mShapeId = shapeId;
            this.mAdvance = advance;
            this.mPhase = phase;
            this.mStyle = style;
            this.mType = PATH_DASH;
        }

        /**
         * Parse a dash path effect from a float array
         *
         * @param data   float array
         * @param offset offset into the array
         * @return dash path effect
         */
        public static @NonNull PaintPathEffects decode(float @NonNull [] data, int offset) {
            int shapeId = Float.floatToRawIntBits(data[offset]);
            float advance = data[offset + 1];
            float phase = data[offset + 2];
            int style = Float.floatToRawIntBits(data[offset + 3]);
            PathDash ret = new PathDash(shapeId, advance, phase, style);
            ret.mDataLength = 4;
            return ret;
        }

        /**
         * Get the ids for the path effects
         *
         * @param data     array of ids
         * @param offset   offset into the array
         * @param register register the ids
         * @return the offset into the array after the ids have been processed
         */
        public static int gitIds(int @NonNull [] data, int offset, @NonNull Register register) {
            // offset is shapeId
            registerIfId(data, offset + 1, register); // advance
            registerIfId(data, offset + 2, register); // phase
            return offset + 4;
        }

        /**
         * Convert a dash path effect to a float array
         */
        @Override
        float @NonNull [] toFloatArray() {
            return pathDash(mShapeId, mAdvance, mPhase, mStyle);
        }
    }

    /**
     * Sum two path effects together
     */
    public static class Sum extends PaintPathEffects {
        public @NonNull PaintPathEffects mFirst;
        public @NonNull PaintPathEffects mSecond;

        /**
         * `Sum` two path effects together
         *
         * @param first  first path effect
         * @param second second path effect
         */
        public Sum(@NonNull PaintPathEffects first, @NonNull PaintPathEffects second) {
            this.mFirst = first;
            this.mSecond = second;
            this.mType = SUM;
        }

        /**
         * Parse a sum path effect from a float array
         *
         * @param data   float array
         * @param offset offset into the array
         * @return sum path effect
         */
        public static @NonNull PaintPathEffects decode(float @NonNull [] data, int offset) {
            PaintPathEffects first = parse(data, offset);
            PaintPathEffects second = parse(data, offset + first.mDataLength + 1);
            Sum ret = new Sum(first, second);
            ret.mDataLength = first.mDataLength + second.mDataLength + 1;
            return ret;
        }

        /**
         * Get the ids for the path effects
         *
         * @param data     array of ids
         * @param offset   offset into the array
         * @param register register the ids
         * @return the offset into the array after the ids have been processed
         */
        public static int gitIds(int @NonNull [] data, int offset, @NonNull Register register) {
            offset = PaintPathEffects.getIds(data, offset, register);
            offset = PaintPathEffects.getIds(data, offset, register);
            return offset;

        }

        /**
         * Convert a sum path effect to a float array
         *
         * @return float array
         */
        @Override
        float @NonNull [] toFloatArray() {
            float[] f = encode(mFirst);
            float[] s = encode(mSecond);
            return fuze(SUM, f, s);
        }
    }

    /**
     * Compose two path effects together
     */
    public static class Compose extends PaintPathEffects {
        public @NonNull PaintPathEffects mOuterPE;
        public @NonNull PaintPathEffects mInnerPE;

        /**
         * Compose two path effects together
         *
         * @param outerPE outer path effect
         * @param innerPE inner path effect
         */
        public Compose(@NonNull PaintPathEffects outerPE, @NonNull PaintPathEffects innerPE) {
            this.mOuterPE = outerPE;
            this.mInnerPE = innerPE;
            this.mType = COMPOSE;
        }

        /**
         * Parse a compose path effect from a float array
         *
         * @param data   float array
         * @param offset offset into the array
         * @return compose path effect
         */
        public static @NonNull PaintPathEffects decode(float @NonNull [] data, int offset) {
            PaintPathEffects outerPE = parse(data, offset);
            PaintPathEffects innerPE = parse(data, offset + outerPE.mDataLength + 1);
            Compose ret = new Compose(outerPE, innerPE);
            ret.mDataLength = outerPE.mDataLength + innerPE.mDataLength + 1;
            return ret;
        }

        /**
         * Get the ids for the path effects
         *
         * @param data     array of ids
         * @param offset   offset into the array
         * @param register register the ids
         * @return the offset into the array after the ids have been processed
         */
        public static int gitIds(int @NonNull [] data, int offset, @NonNull Register register) {
            return 0;
        }

        /**
         * Convert a compose path effect to a float array
         *
         * @return float array
         */
        @Override
        float @NonNull [] toFloatArray() {
            float[] f = encode(mOuterPE);
            float[] s = encode(mInnerPE);
            return fuze(COMPOSE, f, s);
        }

    }

    /**
     * Create a dash path effect
     *
     * @param phase     support phase offset
     * @param intervals support dash intervals
     * @return dash path effect as a float array
     */
    public static float @NonNull [] dash(float phase, float @NonNull ... intervals) {
        float[] ret = new float[intervals.length + 3];
        ret[0] = Float.intBitsToFloat(DASH);
        ret[1] = phase;
        ret[2] = Float.intBitsToFloat(intervals.length);
        for (int i = 0; i < intervals.length; i++) {
            ret[i + 3] = intervals[i];
        }
        return ret;
    }

    /**
     * Chop the path into lines of segmentLength, randomly deviating from the
     * original path by deviation.
     *
     * @param segmentLength segment length
     * @param deviation     deviation path
     */
    public static float @NonNull [] discrete(float segmentLength, float deviation) {
        float[] ret = new float[3];
        ret[0] = Float.intBitsToFloat(DISCRETE_PATH);
        ret[1] = segmentLength;
        ret[2] = deviation;
        return ret;
    }

    /**
     * Dash the drawn path by stamping it with the specified shape. This only
     * applies to drawings when the paint's style is STROKE or STROKE_AND_FILL.
     * If the paint's style is FILL, then this effect is ignored. The paint's
     * strokeWidth does not affect the results.
     *
     * @param shapeId The path to stamp along
     * @param advance spacing between each stamp of shape
     * @param phase   amount to offset before the first shape is stamped
     * @param style   how to transform the shape at each position as it is stamped
     */
    public static float @NonNull [] pathDash(int shapeId, float advance, float phase, int style) {
        float[] ret = new float[5];
        ret[0] = Float.intBitsToFloat(PATH_DASH);
        ret[1] = Float.intBitsToFloat(shapeId);
        ret[2] = advance;
        ret[3] = phase;
        ret[4] = Float.intBitsToFloat(style);
        return ret;
    }

    /**
     * Sum two path effects together
     */
    public static float @NonNull [] sum(float @NonNull [] first, float @NonNull [] second) {
        float[] ret = new float[first.length + second.length + 1];
        ret[0] = Float.intBitsToFloat(SUM);
        for (int i = 0; i < first.length; i++) {
            ret[i + 1] = first[i];
        }
        for (int i = 0; i < second.length; i++) {
            ret[i + first.length + 1] = second[i];
        }
        return ret;
    }

    /**
     * Compose two path effects together
     */
    public static float @NonNull [] compose(float @NonNull [] outerPE, float @NonNull [] innerPE) {
        float[] ret = new float[outerPE.length + innerPE.length + 1];
        ret[0] = Float.intBitsToFloat(COMPOSE);
        for (int i = 0; i < outerPE.length; i++) {
            ret[i + 1] = outerPE[i];
        }
        for (int i = 0; i < innerPE.length; i++) {
            ret[i + outerPE.length + 1] = innerPE[i];
        }
        return ret;
    }

}
