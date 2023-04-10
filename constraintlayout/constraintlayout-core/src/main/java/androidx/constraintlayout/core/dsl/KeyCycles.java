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

/**
 * Provides the API for creating a KeyCycle Object for use in the Core
 * ConstraintLayout & MotionLayout system
 * This allows multiple KeyCycle positions to defined in one object.
 */
public class KeyCycles extends KeyAttributes {


    public enum Wave {
        SIN,
        SQUARE,
        TRIANGLE,
        SAW,
        REVERSE_SAW,
        COS
    }

    private KeyCycles.Wave mWaveShape = null;
    private float[] mWavePeriod = null;
    private float[] mWaveOffset = null;
    private float[] mWavePhase = null;

    KeyCycles(int numOfFrames, String... targets) {
        super(numOfFrames, targets);
        TYPE = "KeyCycle";
    }

    public KeyCycles.Wave getWaveShape() {
        return mWaveShape;
    }

    public void setWaveShape(KeyCycles.Wave waveShape) {
        mWaveShape = waveShape;
    }

    public float[] getWavePeriod() {
        return mWavePeriod;
    }

    public void setWavePeriod(float... wavePeriod) {
        mWavePeriod = wavePeriod;
    }

    public float[] getWaveOffset() {
        return mWaveOffset;
    }

    public void setWaveOffset(float... waveOffset) {
        mWaveOffset = waveOffset;
    }

    public float[] getWavePhase() {
        return mWavePhase;
    }

    public void setWavePhase(float... wavePhase) {
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
