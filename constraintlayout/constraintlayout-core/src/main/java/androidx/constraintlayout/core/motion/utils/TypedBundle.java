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

import java.util.Arrays;

public class TypedBundle {

    private static final int INITIAL_BOOLEAN = 4;
    private static final int INITIAL_INT = 10;
    private static final int INITIAL_FLOAT = 10;
    private static final int INITIAL_STRING = 5;

    int[] mTypeInt = new int[INITIAL_INT];
    int[] mValueInt = new int[INITIAL_INT];
    int mCountInt = 0;
    int[] mTypeFloat = new int[INITIAL_FLOAT];
    float[] mValueFloat = new float[INITIAL_FLOAT];
    int mCountFloat = 0;
    int[] mTypeString = new int[INITIAL_STRING];
    String[] mValueString = new String[INITIAL_STRING];
    int mCountString = 0;
    int[] mTypeBoolean = new int[INITIAL_BOOLEAN];
    boolean[] mValueBoolean = new boolean[INITIAL_BOOLEAN];
    int mCountBoolean = 0;

    // @TODO: add description
    public int getInteger(int type) {
        for (int i = 0; i < mCountInt; i++) {
            if (mTypeInt[i] == type) {
                return mValueInt[i];
            }
        }
        return -1;
    }

    // @TODO: add description
    public void add(int type, int value) {
        if (mCountInt >= mTypeInt.length) {
            mTypeInt = Arrays.copyOf(mTypeInt, mTypeInt.length * 2);
            mValueInt = Arrays.copyOf(mValueInt, mValueInt.length * 2);
        }
        mTypeInt[mCountInt] = type;
        mValueInt[mCountInt++] = value;
    }

    // @TODO: add description
    public void add(int type, float value) {
        if (mCountFloat >= mTypeFloat.length) {
            mTypeFloat = Arrays.copyOf(mTypeFloat, mTypeFloat.length * 2);
            mValueFloat = Arrays.copyOf(mValueFloat, mValueFloat.length * 2);
        }
        mTypeFloat[mCountFloat] = type;
        mValueFloat[mCountFloat++] = value;
    }

    // @TODO: add description
    public void addIfNotNull(int type, String value) {
        if (value != null) {
            add(type, value);
        }
    }

    // @TODO: add description
    public void add(int type, String value) {
        if (mCountString >= mTypeString.length) {
            mTypeString = Arrays.copyOf(mTypeString, mTypeString.length * 2);
            mValueString = Arrays.copyOf(mValueString, mValueString.length * 2);
        }
        mTypeString[mCountString] = type;
        mValueString[mCountString++] = value;
    }

    // @TODO: add description
    public void add(int type, boolean value) {
        if (mCountBoolean >= mTypeBoolean.length) {
            mTypeBoolean = Arrays.copyOf(mTypeBoolean, mTypeBoolean.length * 2);
            mValueBoolean = Arrays.copyOf(mValueBoolean, mValueBoolean.length * 2);
        }
        mTypeBoolean[mCountBoolean] = type;
        mValueBoolean[mCountBoolean++] = value;
    }

    // @TODO: add description
    public void applyDelta(TypedValues values) {
        for (int i = 0; i < mCountInt; i++) {
            values.setValue(mTypeInt[i], mValueInt[i]);
        }
        for (int i = 0; i < mCountFloat; i++) {
            values.setValue(mTypeFloat[i], mValueFloat[i]);
        }
        for (int i = 0; i < mCountString; i++) {
            values.setValue(mTypeString[i], mValueString[i]);
        }
        for (int i = 0; i < mCountBoolean; i++) {
            values.setValue(mTypeBoolean[i], mValueBoolean[i]);
        }
    }

    // @TODO: add description
    public void applyDelta(TypedBundle values) {
        for (int i = 0; i < mCountInt; i++) {
            values.add(mTypeInt[i], mValueInt[i]);
        }
        for (int i = 0; i < mCountFloat; i++) {
            values.add(mTypeFloat[i], mValueFloat[i]);
        }
        for (int i = 0; i < mCountString; i++) {
            values.add(mTypeString[i], mValueString[i]);
        }
        for (int i = 0; i < mCountBoolean; i++) {
            values.add(mTypeBoolean[i], mValueBoolean[i]);
        }
    }

    // @TODO: add description
    public void clear() {
        mCountBoolean = 0;
        mCountString = 0;
        mCountFloat = 0;
        mCountInt = 0;
    }

    @Override
    public String toString() {
        return "TypedBundle{" +
                "mCountInt=" + mCountInt +
                ", mCountFloat=" + mCountFloat +
                ", mCountString=" + mCountString +
                ", mCountBoolean=" + mCountBoolean +
                '}';
    }
}
