/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.constraintlayout.core;

/**
 * Utility to draw an histogram
 */
public class HistogramCounter {
    long[] mCalls = new long[256];
    final String mName;

    public void inc(int value) {
        if (value < 255) {
            mCalls[value]++;
        } else {
            mCalls[255]++;
        }
    }

    public HistogramCounter(String name) {
        this.mName = name;
    }

    public void reset() {
        mCalls = new long[256];
    }

    private String print(long n) {
        String ret = "";
        for (int i = 0; i < n; i++) {
            ret += "X";
        }
        return ret;
    }

    @Override
    public String toString() {
        String ret = mName + " :\n";
        int lastValue = 255;
        for (int i = 255; i >= 0; i--) {
            if (mCalls[i] != 0) {
                lastValue = i;
                break;
            }
        }
        int total = 0;
        for (int i = 0; i <= lastValue; i++) {
            ret += "[" + i + "] = " + mCalls[i] + " -> " + print(mCalls[i]) + "\n";
            total += mCalls[i];
        }
        ret += "Total calls " + total;
        return ret;
    }
}
