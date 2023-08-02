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
package androidx.constraintlayout.core.motion.utils;

import androidx.constraintlayout.core.motion.CustomAttribute;
import androidx.constraintlayout.core.motion.CustomVariable;

import java.util.Arrays;

public class KeyFrameArray {

    // =================================== CustomAttribute =================================
    public static class CustomArray {
        int[] mKeys = new int[101];
        CustomAttribute[] mValues = new CustomAttribute[101];
        int mCount;
        private static final int EMPTY = 999;

        public CustomArray() {
            clear();
        }

        // @TODO: add description
        public void clear() {
            Arrays.fill(mKeys, EMPTY);
            Arrays.fill(mValues, null);
            mCount = 0;
        }

        // @TODO: add description
        public void dump() {
            System.out.println("V: " + Arrays.toString(Arrays.copyOf(mKeys, mCount)));
            System.out.print("K: [");
            for (int i = 0; i < mCount; i++) {
                System.out.print((i == 0 ? "" : ", ") + valueAt(i));
            }
            System.out.println("]");
        }

        // @TODO: add description
        public int size() {
            return mCount;
        }

        // @TODO: add description
        public CustomAttribute valueAt(int i) {
            return mValues[mKeys[i]];
        }

        // @TODO: add description
        public int keyAt(int i) {
            return mKeys[i];
        }

        // @TODO: add description
        public void append(int position, CustomAttribute value) {
            if (mValues[position] != null) {
                remove(position);
            }
            mValues[position] = value;
            mKeys[mCount++] = position;
            Arrays.sort(mKeys);
        }

        // @TODO: add description
        public void remove(int position) {
            mValues[position] = null;
            for (int j = 0, i = 0; i < mCount; i++) {
                if (position == mKeys[i]) {
                    mKeys[i] = EMPTY;
                    j++;
                }
                if (i != j) {
                    mKeys[i] = mKeys[j];
                }
                j++;

            }
            mCount--;
        }
    }

    // =================================== CustomVar =================================
    public static class CustomVar {
        int[] mKeys = new int[101];
        CustomVariable[] mValues = new CustomVariable[101];
        int mCount;
        private static final int EMPTY = 999;

        public CustomVar() {
            clear();
        }

        // @TODO: add description
        public void clear() {
            Arrays.fill(mKeys, EMPTY);
            Arrays.fill(mValues, null);
            mCount = 0;
        }

        // @TODO: add description
        public void dump() {
            System.out.println("V: " + Arrays.toString(Arrays.copyOf(mKeys, mCount)));
            System.out.print("K: [");
            for (int i = 0; i < mCount; i++) {
                System.out.print((i == 0 ? "" : ", ") + valueAt(i));
            }
            System.out.println("]");
        }

        // @TODO: add description
        public int size() {
            return mCount;
        }

        // @TODO: add description
        public CustomVariable valueAt(int i) {
            return mValues[mKeys[i]];
        }

        // @TODO: add description
        public int keyAt(int i) {
            return mKeys[i];
        }

        // @TODO: add description
        public void append(int position, CustomVariable value) {
            if (mValues[position] != null) {
                remove(position);
            }
            mValues[position] = value;
            mKeys[mCount++] = position;
            Arrays.sort(mKeys);
        }

        // @TODO: add description
        public void remove(int position) {
            mValues[position] = null;
            for (int j = 0, i = 0; i < mCount; i++) {
                if (position == mKeys[i]) {
                    mKeys[i] = EMPTY;
                    j++;
                }
                if (i != j) {
                    mKeys[i] = mKeys[j];
                }
                j++;

            }
            mCount--;
        }
    }

    // =================================== FloatArray ======================================
    static class FloatArray {
        int[] mKeys = new int[101];
        float[][] mValues = new float[101][];
        int mCount;
        private static final int EMPTY = 999;

        FloatArray() {
            clear();
        }

        public void clear() {
            Arrays.fill(mKeys, EMPTY);
            Arrays.fill(mValues, null);
            mCount = 0;
        }

        public void dump() {
            System.out.println("V: " + Arrays.toString(Arrays.copyOf(mKeys, mCount)));
            System.out.print("K: [");
            for (int i = 0; i < mCount; i++) {
                System.out.print((i == 0 ? "" : ", ") + Arrays.toString(valueAt(i)));
            }
            System.out.println("]");
        }

        public int size() {
            return mCount;
        }

        public float[] valueAt(int i) {
            return mValues[mKeys[i]];
        }

        public int keyAt(int i) {
            return mKeys[i];
        }

        public void append(int position, float[] value) {
            if (mValues[position] != null) {
                remove(position);
            }
            mValues[position] = value;
            mKeys[mCount++] = position;
            Arrays.sort(mKeys);
        }

        public void remove(int position) {
            mValues[position] = null;
            for (int j = 0, i = 0; i < mCount; i++) {
                if (position == mKeys[i]) {
                    mKeys[i] = EMPTY;
                    j++;
                }
                if (i != j) {
                    mKeys[i] = mKeys[j];
                }
                j++;

            }
            mCount--;
        }
    }
}
