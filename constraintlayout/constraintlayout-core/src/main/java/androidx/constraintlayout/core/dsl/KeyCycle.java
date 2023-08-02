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

public class KeyCycle extends KeyAttribute {
    private static final String TAG = "KeyCycle";
    private Wave mWaveShape = null;
    private float mWavePeriod = Float.NaN;
    private float mWaveOffset = Float.NaN;
    private float mWavePhase = Float.NaN;

    KeyCycle(int frame, String target) {
        super(frame, target);
        TYPE = "KeyCycle";
    }

    public enum Wave {
        SIN,
        SQUARE,
        TRIANGLE,
        SAW,
        REVERSE_SAW,
        COS
    }

    public Wave getShape() {
        return mWaveShape;
    }

    public void setShape(Wave waveShape) {
        mWaveShape = waveShape;
    }

    public float getPeriod() {
        return mWavePeriod;
    }

    public void setPeriod(float wavePeriod) {
        mWavePeriod = wavePeriod;
    }

    public float getOffset() {
        return mWaveOffset;
    }

    public void setOffset(float waveOffset) {
        mWaveOffset = waveOffset;
    }

    public float getPhase() {
        return mWavePhase;
    }

    public void setPhase(float wavePhase) {
        mWavePhase = wavePhase;
    }

    @Override
    protected void attributesToString(StringBuilder builder) {
        super.attributesToString(builder);

        if (mWaveShape != null) {
            builder.append("shape:'").append(mWaveShape).append("',\n");
        }
        append(builder, "period", mWavePeriod);
        append(builder, "offset", mWaveOffset);
        append(builder, "phase", mWavePhase);

    }

}
