/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.dsl;

public abstract class Guideline extends Helper {
    private int mStart = Integer.MIN_VALUE;
    private int mEnd = Integer.MIN_VALUE;
    private float mPercent = Float.NaN;

    Guideline(String name) {
        super(name, new HelperType(""));
    }

    /**
     * Get the start position
     *
     * @return start position
     */
    public int getStart() {
        return mStart;
    }

    /**
     * Set the start position
     *
     * @param start the start position
     */
    public void setStart(int start) {
        mStart = start;
        configMap.put("start", String.valueOf(mStart));
    }

    /**
     * Get the end position
     *
     * @return end position
     */
    public int getEnd() {
        return mEnd;
    }

    /**
     * Set the end position
     *
     * @param end the end position
     */
    public void setEnd(int end) {
        mEnd = end;
        configMap.put("end", String.valueOf(mEnd));
    }

    /**
     * Get the position in percent
     *
     * @return position in percent
     */
    public float getPercent() {
        return mPercent;
    }

    /**
     * Set the position in percent
     *
     * @param percent the position in percent
     */
    public void setPercent(float percent) {
        mPercent = percent;
        configMap.put("percent", String.valueOf(mPercent));
    }
}
